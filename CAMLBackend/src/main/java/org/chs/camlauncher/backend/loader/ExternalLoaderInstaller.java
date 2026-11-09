package org.chs.camlauncher.backend.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;
import org.chs.camlauncher.backend.download.source.SourceAwareHttpClient;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.runtime.JavaRequirement;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipFile;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 调用 Forge/NeoForge 官方 installer 的 headless client 安装。 */
public final class ExternalLoaderInstaller {
    private static final Logger LOGGER = LogManager.getLogger(ExternalLoaderInstaller.class);
    private static final long INSTALL_TIMEOUT_MINUTES = 20;
    private static final int OUTPUT_LIMIT = 64 * 1024;
    private static final int INSTALL_ATTEMPTS = 2;
    private static final Pattern SHA1_PATTERN = Pattern.compile("\\b[0-9a-fA-F]{40}\\b");

    private final VersionInstaller versionInstaller;
    private final JavaLocator javaLocator;
    private final FileDownloader fileDownloader;
    private final MetadataService metadata;
    private final URI manifestUri;
    private final Path manifestCacheFile;
    private final SourceAwareHttpClient shaClient = new SourceAwareHttpClient(defaultClient());
    private volatile List<DownloadSource> sources = List.of(OfficialDownloadSource.INSTANCE);

    public ExternalLoaderInstaller(
            VersionInstaller versionInstaller,
            JavaLocator javaLocator,
            FileDownloader fileDownloader,
            MetadataService metadata,
            URI manifestUri,
            Path manifestCacheFile
    ) {
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.javaLocator = Objects.requireNonNull(javaLocator, "javaLocator");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.manifestUri = Objects.requireNonNull(manifestUri, "manifestUri");
        this.manifestCacheFile = Objects.requireNonNull(manifestCacheFile, "manifestCacheFile");
    }

    public void setSources(List<DownloadSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
        shaClient.setSources(sources);
    }

    public InstalledVersion install(
            String loaderName,
            String gameVersion,
            String instanceId,
            URI installerUri,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(loaderName, "loaderName");
        Objects.requireNonNull(gameVersion, "gameVersion");
        Objects.requireNonNull(installerUri, "installerUri");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        String normalizedInstanceId = InstanceNameRules.validate(instanceId);
        if (normalizedInstanceId.equals(gameVersion)) {
            throw new IllegalArgumentException(
                    "安装 " + loaderName + " 时实例名不能与游戏版本号相同");
        }

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
                listener
        );
        JavaRuntime java = javaLocator.findSuitable(JavaRequirement.from(base.manifest()))
                .orElseThrow(() -> new IOException(
                        "找不到可用于运行 " + loaderName + " 安装器的 Java"));

        Path staging = Files.createTempDirectory(
                gameDirectory,
                ".caml-" + loaderName.toLowerCase() + "-install-"
        );
        try {
            prepareStaging(staging, gameVersion, base);
            Path installerJar = staging.resolve("installer.jar");
            String installerSha1 = resolveSha1(installerUri);
            fileDownloader.download(new DownloadItem(
                    loaderName + " installer",
                    DownloadSources.candidates(sources, installerUri.toString()),
                    installerJar,
                    installerSha1,
                    -1
            ), listener, ExternalLoaderInstaller::validateInstallerArchive);

            String installedVersionId = readInstallerVersionId(installerJar);
            Path installedVersionDirectory = staging.resolve("versions").resolve(installedVersionId);
            String installerOutput = "";
            for (int attempt = 0; attempt < INSTALL_ATTEMPTS; attempt++) {
                installerOutput = runInstaller(loaderName, java, installerJar, staging);
                if (Files.isDirectory(installedVersionDirectory)) {
                    break;
                }
            }
            if (!Files.isDirectory(installedVersionDirectory)) {
                throw new IOException(
                        loaderName + " 安装器没有生成版本目录: " + installedVersionDirectory
                                + "\n安装器输出:\n" + installerOutput);
            }

            mergeLibraries(staging.resolve("libraries"), gameDirectory.resolve("libraries"));
            moveVersionDirectory(
                    installedVersionDirectory,
                    targetVersionDirectory,
                    installedVersionId,
                    normalizedInstanceId
            );
        } finally {
            deleteRecursively(staging);
        }

        return versionInstaller.install(
                normalizedInstanceId,
                gameDirectory,
                manifestUri,
                manifestCacheFile,
                listener
        );
    }

    private String resolveSha1(URI installerUri) throws InterruptedException {
        try {
            String text = shaClient.getText(
                    URI.create(installerUri + ".sha1"),
                    Duration.ofSeconds(15),
                    "text/plain"
            );
            Matcher matcher = SHA1_PATTERN.matcher(text);
            if (matcher.find()) {
                return matcher.group().toLowerCase(Locale.ROOT);
            }
            LOGGER.warn("{} 的 .sha1 内容无效，改由 ZIP 结构校验", installerUri);
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("无法获取 {} 的 .sha1，改由 ZIP 结构校验", installerUri, e);
        }
        return null;
    }

    private static void validateInstallerArchive(Path installer) throws IOException {
        try (ZipFile zip = new ZipFile(installer.toFile())) {
            if (zip.getEntry("version.json") == null
                    && zip.getEntry("install_profile.json") == null) {
                throw new IOException("installer 不是有效的 Forge/NeoForge 归档: " + installer);
            }
        }
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private static void prepareStaging(Path staging, String gameVersion, InstalledVersion base)
            throws IOException {
        Path stagedVersionDirectory = staging.resolve("versions").resolve(gameVersion);
        Files.createDirectories(stagedVersionDirectory);
        Files.copy(base.versionJson(),
                stagedVersionDirectory.resolve(gameVersion + ".json"),
                StandardCopyOption.REPLACE_EXISTING);
        if (Files.isRegularFile(base.clientJar())) {
            Files.copy(base.clientJar(),
                    stagedVersionDirectory.resolve(gameVersion + ".jar"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Files.writeString(
                staging.resolve("launcher_profiles.json"),
                "{\"profiles\":{},\"settings\":{}}\n",
                StandardCharsets.UTF_8
        );
    }

    private static String runInstaller(
            String loaderName,
            JavaRuntime java,
            Path installerJar,
            Path staging
    ) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(java.executable().toString());
        command.add("-Djava.awt.headless=true");
        command.add("-jar");
        command.add(installerJar.toString());
        command.add("--installClient");
        command.add(staging.toString());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(staging.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> captureOutput(process, output),
                "caml-" + loaderName.toLowerCase() + "-installer-output");
        reader.setDaemon(true);
        reader.start();

        boolean finished;
        try {
            finished = process.waitFor(INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException(loaderName + " 安装器超时");
        }
        reader.join(5000);

        if (process.exitValue() != 0) {
            throw new IOException(loaderName + " 安装器失败，退出码 "
                    + process.exitValue() + "\n" + tail(output));
        }
        return tail(output);
    }

    private static void captureOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (output) {
                    output.append(line).append('\n');
                    if (output.length() > OUTPUT_LIMIT) {
                        output.delete(0, output.length() - OUTPUT_LIMIT);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static String tail(StringBuilder output) {
        synchronized (output) {
            return output.toString();
        }
    }

    private static String readInstallerVersionId(Path installerJar) throws IOException {
        try (ZipFile zip = new ZipFile(installerJar.toFile())) {
            var versionEntry = zip.getEntry("version.json");
            if (versionEntry != null) {
                try (var input = zip.getInputStream(versionEntry)) {
                    String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    return new MetadataService().parseVersionInfo(text).id();
                }
            }

            var profileEntry = zip.getEntry("install_profile.json");
            if (profileEntry != null) {
                try (var input = zip.getInputStream(profileEntry)) {
                    JsonObject profile = JsonParser.parseString(
                            new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    ).getAsJsonObject();
                    if (profile.has("version") && !profile.get("version").isJsonNull()) {
                        return profile.get("version").getAsString();
                    }
                }
            }
        }
        throw new IOException("安装器缺少 version.json 或 install_profile.version: " + installerJar);
    }

    private static void mergeLibraries(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        Path normalizedTarget = target.toAbsolutePath().normalize();
        try (Stream<Path> files = Files.walk(source)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Path destination = normalizedTarget.resolve(source.relativize(file)).normalize();
                if (!destination.startsWith(normalizedTarget)) {
                    throw new IOException("安装器库路径越界: " + file);
                }
                Files.createDirectories(destination.getParent());
                if (Files.isRegularFile(destination)
                        && Files.size(destination) == Files.size(file)) {
                    continue;
                }
                Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void moveVersionDirectory(
            Path source,
            Path target,
            String installedVersionId,
            String instanceId
    ) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }

        if (!installedVersionId.equals(instanceId)) {
            Path json = target.resolve(installedVersionId + ".json");
            if (!Files.isRegularFile(json)) {
                throw new IOException("安装器生成的版本 JSON 不存在: " + json);
            }
            Files.move(json, target.resolve(instanceId + ".json"),
                    StandardCopyOption.REPLACE_EXISTING);

            Path jar = target.resolve(installedVersionId + ".jar");
            if (Files.isRegularFile(jar)) {
                Files.move(jar, target.resolve(instanceId + ".jar"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
        }
    }

}
