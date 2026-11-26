package org.chs.camlauncher.backend.loader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.DownloadStage;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.io.DownloadExecutor;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;
import org.chs.camlauncher.backend.download.source.SourceAwareHttpClient;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.InstanceInstallLock;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.SharedLibraryTransaction;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.runtime.JavaRequirement;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 调用 Forge/NeoForge 官方 installer 的 headless client 安装。 */
public final class ExternalLoaderInstaller {
    private static final Logger LOGGER = LogManager.getLogger(ExternalLoaderInstaller.class);
    private static final int INSTALL_ATTEMPTS = 2;
    private static final Duration INSTALLER_LIBRARY_RETRY_DELAY = Duration.ofSeconds(5);
    private static final Pattern SHA1_PATTERN = Pattern.compile("\\b[0-9a-fA-F]{40}\\b");

    private final VersionInstaller versionInstaller;
    private final JavaLocator javaLocator;
    private final FileDownloader fileDownloader;
    private final MetadataService metadata;
    private final URI manifestUri;
    private final Path manifestCacheFile;
    private final Duration installerLibraryRetryDelay;
    private final SourceAwareHttpClient shaClient = new SourceAwareHttpClient(defaultClient());
    private volatile List<DownloadSource> sources = List.of(OfficialDownloadSource.INSTANCE);
    private volatile int downloadConcurrency = DownloadConcurrency.DEFAULT;

    public ExternalLoaderInstaller(
            VersionInstaller versionInstaller,
            JavaLocator javaLocator,
            FileDownloader fileDownloader,
            MetadataService metadata,
            URI manifestUri,
            Path manifestCacheFile
    ) {
        this(versionInstaller, javaLocator, fileDownloader, metadata, manifestUri,
                manifestCacheFile, INSTALLER_LIBRARY_RETRY_DELAY);
    }

    ExternalLoaderInstaller(
            VersionInstaller versionInstaller,
            JavaLocator javaLocator,
            FileDownloader fileDownloader,
            MetadataService metadata,
            URI manifestUri,
            Path manifestCacheFile,
            Duration installerLibraryRetryDelay
    ) {
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.javaLocator = Objects.requireNonNull(javaLocator, "javaLocator");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.manifestUri = Objects.requireNonNull(manifestUri, "manifestUri");
        this.manifestCacheFile = Objects.requireNonNull(manifestCacheFile, "manifestCacheFile");
        this.installerLibraryRetryDelay = Objects.requireNonNull(
                installerLibraryRetryDelay, "installerLibraryRetryDelay");
        if (installerLibraryRetryDelay.isZero() || installerLibraryRetryDelay.isNegative()) {
            throw new IllegalArgumentException("installerLibraryRetryDelay must be positive");
        }
    }

    public void setSources(List<DownloadSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
        shaClient.setSources(sources);
    }

    public void setDownloadConcurrency(int downloadConcurrency) {
        this.downloadConcurrency = DownloadConcurrency.clamp(downloadConcurrency);
    }

    public InstalledVersion install(
            String loaderName,
            String gameVersion,
            String instanceId,
            URI installerUri,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return install(loaderName, gameVersion, instanceId, installerUri, gameDirectory, listener,
                new DownloadCancellationToken());
    }

    public InstalledVersion install(
            String loaderName,
            String gameVersion,
            String instanceId,
            URI installerUri,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        try (SharedLibraryTransaction libraryTransaction = SharedLibraryTransaction.begin(
                gameDirectory.resolve("libraries"))) {
            InstalledVersion installed = install(
                    loaderName, gameVersion, instanceId, installerUri, gameDirectory,
                    listener, token, libraryTransaction);
            libraryTransaction.commit();
            return installed;
        }
    }

    /** 把共享库变更纳入调用方事务；调用方负责最终 commit/rollback。 */
    public InstalledVersion install(
            String loaderName,
            String gameVersion,
            String instanceId,
            URI installerUri,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            SharedLibraryTransaction libraryTransaction
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(token, "token");
        Objects.requireNonNull(libraryTransaction, "libraryTransaction");
        token.throwIfCancelled();
        Objects.requireNonNull(loaderName, "loaderName");
        Objects.requireNonNull(gameVersion, "gameVersion");
        Objects.requireNonNull(installerUri, "installerUri");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        String normalizedInstanceId = InstanceNameRules.validate(instanceId);
        if (normalizedInstanceId.equals(gameVersion)) {
            throw new IllegalArgumentException(
                    "安装 " + loaderName + " 时实例名不能与游戏版本号相同");
        }

        try (InstanceInstallLock ignored = InstanceInstallLock.acquire(
                gameDirectory, normalizedInstanceId)) {
            InstanceNameRules.ensureNewInstance(gameDirectory, normalizedInstanceId);
            Path targetVersionDirectory = gameDirectory
                    .resolve("versions")
                    .resolve(normalizedInstanceId)
                    .normalize();

            InstalledVersion base = versionInstaller.install(
                    gameVersion,
                    gameVersion,
                    gameDirectory,
                    manifestUri,
                    manifestCacheFile,
                    listener,
                    token
            );
            DownloadListener.notifyStage(listener,
                    DownloadStage.of("loader.installer.prepare", loaderName));
            JavaRuntime java = findInstallerJava(base);

            Path preparedStaging = Files.createTempDirectory(
                    gameDirectory,
                    ".caml-" + loaderName.toLowerCase() + "-install-prepare-"
            );
            Path attemptStaging = null;
            try {
                LoaderInstallFiles.prepareStaging(preparedStaging, gameVersion, base);
                Path installerJar = preparedStaging.resolve("installer.jar");
                downloadInstaller(loaderName, installerUri, installerJar, listener, token);

                token.throwIfCancelled();
                String installedVersionId;
                try (InstallerArchive archive = new InstallerArchive(installerJar, metadata)) {
                    installedVersionId = archive.versionId();
                    new InstallerLibraryDownloader(
                            fileDownloader,
                            installerLibraryRetryDelay,
                            downloadConcurrency,
                            sources,
                            archive
                    ).download(loaderName, preparedStaging, listener, token);
                }

                DownloadListener.notifyStage(listener,
                        DownloadStage.of("loader.installer.run", loaderName));
                attemptStaging = runInstallerAttempts(
                        loaderName, gameDirectory, java, preparedStaging, installedVersionId, token);

                DownloadListener.notifyStage(listener,
                        DownloadStage.of("loader.files.merge", loaderName));
                token.throwIfCancelled();
                libraryTransaction.merge(attemptStaging.resolve("libraries"));
                LoaderInstallFiles.moveVersionDirectory(
                        attemptStaging.resolve("versions").resolve(installedVersionId),
                        targetVersionDirectory,
                        installedVersionId,
                        normalizedInstanceId
                );

                DownloadListener.notifyStage(listener,
                        DownloadStage.of("loader.verify", loaderName));
                InstalledVersion installed = versionInstaller.install(
                        normalizedInstanceId,
                        gameDirectory,
                        manifestUri,
                        manifestCacheFile,
                        listener,
                        token
                );
                InstalledLoaderVerifier.verifyInstalledLibraries(loaderName, installed);
                return installed;
            } catch (IOException | RuntimeException | InterruptedException e) {
                LoaderInstallFiles.deleteRecursively(targetVersionDirectory);
                throw e;
            } finally {
                LoaderInstallFiles.deleteRecursively(attemptStaging);
                LoaderInstallFiles.deleteRecursively(preparedStaging);
            }
        }
    }

    /** 校验最终安装结果中的库文件是否存在。 */
    public static void verifyInstalledLibraries(String loaderName, InstalledVersion installed)
            throws IOException {
        InstalledLoaderVerifier.verifyInstalledLibraries(loaderName, installed);
    }

    private JavaRuntime findInstallerJava(InstalledVersion base) throws IOException {
        JavaRequirement requirement = JavaRequirement.from(base.manifest());
        return javaLocator.findSuitable(requirement)
                .orElseThrow(() -> new BackendException(BackendError.of(
                        "launch.javaRequirementNotFound",
                        requirement.majorVersion()
                )));
    }

    private void downloadInstaller(
            String loaderName,
            URI installerUri,
            Path installerJar,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        String installerSha1 = resolveSha1(installerUri);
        DownloadListener.notifyStage(listener,
                DownloadStage.of("loader.installer.download", loaderName));
        token.throwIfCancelled();
        DownloadExecutor.downloadSingle(new DownloadItem(
                        loaderName + " installer",
                        DownloadSources.candidates(sources, installerUri.toString()),
                        installerJar,
                        installerSha1,
                        -1
                ),
                listener,
                (item, itemListener) -> fileDownloader.download(
                        item, itemListener, InstallerArchive::validate, token),
                token);
    }

    private Path runInstallerAttempts(
            String loaderName,
            Path gameDirectory,
            JavaRuntime java,
            Path preparedStaging,
            String installedVersionId,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        IOException lastFailure = null;
        for (int attempt = 0; attempt < INSTALL_ATTEMPTS; attempt++) {
            token.throwIfCancelled();
            Path candidate = Files.createTempDirectory(
                    gameDirectory,
                    ".caml-" + loaderName.toLowerCase() + "-install-attempt-"
            );
            boolean keepCandidate = false;
            try {
                LoaderInstallFiles.copyTree(preparedStaging, candidate);
                String installerOutput = ExternalInstallerProcess.run(
                        loaderName, java, candidate.resolve("installer.jar"), candidate, token);
                Path installedVersionDirectory = candidate.resolve("versions")
                        .resolve(installedVersionId);
                if (!Files.isDirectory(installedVersionDirectory)) {
                    throw new IOException(
                            loaderName + " 安装器没有生成版本目录: " + installedVersionDirectory
                                    + "\n安装器输出:\n" + installerOutput);
                }
                InstalledLoaderVerifier.validateGeneratedVersion(
                        loaderName, installedVersionDirectory, installedVersionId, metadata);
                keepCandidate = true;
                return candidate;
            } catch (IOException e) {
                lastFailure = e;
                if (attempt + 1 < INSTALL_ATTEMPTS) {
                    LOGGER.warn("{} 安装器第 {} 次尝试失败，将使用全新 staging 重试",
                            loaderName, attempt + 1, e);
                }
            } finally {
                if (!keepCandidate) {
                    LoaderInstallFiles.deleteRecursively(candidate);
                }
            }
        }
        throw lastFailure == null
                ? new IOException(loaderName + " 安装器多次尝试后仍未生成版本目录")
                : lastFailure;
    }

    private String resolveSha1(URI installerUri) throws InterruptedException {
        try {
            Optional<String> sha1 = shaClient.getParsedText(
                    URI.create(installerUri + ".sha1"),
                    Duration.ofSeconds(15),
                    "text/plain",
                    text -> {
                        Matcher matcher = SHA1_PATTERN.matcher(text);
                        return matcher.find()
                                ? Optional.of(matcher.group().toLowerCase(Locale.ROOT))
                                : Optional.empty();
                    }
            );
            if (sha1.isPresent()) {
                return sha1.get();
            }
            LOGGER.warn("{} 的 .sha1 内容无效，改由 ZIP 结构校验", installerUri);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("无法获取 {} 的 .sha1，改由 ZIP 结构校验", installerUri, e);
        }
        return null;
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }
}
