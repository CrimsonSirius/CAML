package org.chs.camlauncher.backend.modpack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.io.DownloadExecutor;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.instance.InstanceInstallLock;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.SharedLibraryTransaction;
import org.chs.camlauncher.backend.instance.VersionDirectoryStage;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.io.ArchiveExtractionGuard;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricInstaller;
import org.chs.camlauncher.backend.loader.quilt.QuiltInstaller;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 将 Modrinth .mrpack 安装为新的完全隔离实例。 */
public final class ModpackInstaller {
    private final VersionInstaller versionInstaller;
    private final FabricInstaller fabricInstaller;
    private final QuiltInstaller quiltInstaller;
    private final ExternalLoaderInstaller externalLoaderInstaller;
    private final FileDownloader fileDownloader;
    private final URI manifestUri;
    private final Path manifestCacheFile;
    private volatile int downloadConcurrency = DownloadConcurrency.DEFAULT;
    private volatile int archiveMaxEntries = ArchiveExtractionGuard.DEFAULT_MAX_ENTRIES;
    private volatile long archiveMaxEntryBytes = ArchiveExtractionGuard.DEFAULT_MAX_ENTRY_BYTES;
    private volatile long archiveMaxTotalBytes = ArchiveExtractionGuard.DEFAULT_MAX_TOTAL_BYTES;

    public ModpackInstaller(
            VersionInstaller versionInstaller,
            FabricInstaller fabricInstaller,
            QuiltInstaller quiltInstaller,
            ExternalLoaderInstaller externalLoaderInstaller,
            FileDownloader fileDownloader,
            URI manifestUri,
            Path manifestCacheFile
    ) {
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.fabricInstaller = Objects.requireNonNull(fabricInstaller, "fabricInstaller");
        this.quiltInstaller = Objects.requireNonNull(quiltInstaller, "quiltInstaller");
        this.externalLoaderInstaller = Objects.requireNonNull(
                externalLoaderInstaller, "externalLoaderInstaller");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
        this.manifestUri = Objects.requireNonNull(manifestUri, "manifestUri");
        this.manifestCacheFile = Objects.requireNonNull(manifestCacheFile, "manifestCacheFile");
    }

    public void setDownloadConcurrency(int downloadConcurrency) {
        this.downloadConcurrency = DownloadConcurrency.clamp(downloadConcurrency);
    }

    public void setArchiveExtractionLimits(int maxEntries, long maxEntryBytes, long maxTotalBytes) {
        ArchiveExtractionGuard guard = new ArchiveExtractionGuard(
                maxEntries, maxEntryBytes, maxTotalBytes);
        this.archiveMaxEntries = maxEntries;
        this.archiveMaxEntryBytes = maxEntryBytes;
        this.archiveMaxTotalBytes = maxTotalBytes;
    }

    public ModDownloadResult install(
            ModFile file,
            Path gameDirectory,
            String instanceName,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(file, "file");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(token, "token");
        String instance = InstanceNameRules.validate(instanceName);
        Files.createDirectories(gameDirectory);

        Path archive = Files.createTempFile(gameDirectory, ".caml-modpack-", ".mrpack");
        String stagedInstance = ".caml-modpack-stage-" + UUID.randomUUID().toString().replace("-", "");
        Path stagedDirectory = gameDirectory.resolve("versions").resolve(stagedInstance);
        ArchiveExtractionGuard extractionGuard = new ArchiveExtractionGuard(
                archiveMaxEntries, archiveMaxEntryBytes, archiveMaxTotalBytes);
        try {
            try (InstanceInstallLock ignored = InstanceInstallLock.acquire(gameDirectory, instance)) {
                InstanceNameRules.ensureNewInstance(gameDirectory, instance);
                downloadArchive(file, archive, listener, token);
                ModrinthModpackIndex index = readIndex(archive, file.fileName(), extractionGuard);
                try (SharedLibraryTransaction libraryTransaction = SharedLibraryTransaction.begin(
                        gameDirectory.resolve("libraries"));
                     VersionDirectoryStage stage = VersionDirectoryStage.adopt(
                             gameDirectory, instance, stagedDirectory)) {
                    installLoader(index, stagedInstance, gameDirectory, listener, token,
                            libraryTransaction);
                    installContent(archive, stagedDirectory, index, file, listener, token,
                            extractionGuard);
                    finalizeStagedVersion(stagedDirectory, stagedInstance, instance);
                    libraryTransaction.commit();
                    Path finalDirectory = stage.commitDirectory();
                    return new ModDownloadResult(finalDirectory, file);
                }
            }
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    private void installContent(
            Path archive,
            Path instanceDirectory,
            ModrinthModpackIndex index,
            ModFile file,
            DownloadListener listener,
            DownloadCancellationToken token,
            ArchiveExtractionGuard extractionGuard
    ) throws IOException, InterruptedException {
        extractOverrides(archive, instanceDirectory, file.fileName(), extractionGuard);
        downloadFiles(index, instanceDirectory, file.fileName(), listener, token);
        ModpackLayout.writeMarker(instanceDirectory, index, file);
    }

    static void finalizeStagedVersion(
            Path versionDirectory, String stagedId, String finalId) throws IOException {
        Path stagedJson = SafePaths.resolveChild(
                versionDirectory, stagedId + ".json", "staged version json");
        if (!Files.isRegularFile(stagedJson)) {
            throw new IOException("整合包 staging 缺少版本 JSON: " + stagedJson);
        }

        JsonObject manifest;
        try {
            var parsed = JsonParser.parseString(Files.readString(stagedJson, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IOException("整合包版本 JSON 不是对象: " + stagedJson);
            }
            manifest = parsed.getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IOException("整合包版本 JSON 无法解析: " + stagedJson, e);
        }
        manifest.addProperty("id", finalId);
        var jar = manifest.get("jar");
        if (jar != null && jar.isJsonPrimitive() && stagedId.equals(jar.getAsString())) {
            manifest.addProperty("jar", finalId);
        }

        writeAtomically(stagedJson, new Gson().toJson(manifest));
        Path finalJson = SafePaths.resolveChild(
                versionDirectory, finalId + ".json", "final version json");
        move(stagedJson, finalJson);

        Path stagedJar = SafePaths.resolveChild(
                versionDirectory, stagedId + ".jar", "staged client jar");
        if (Files.isRegularFile(stagedJar)) {
            move(stagedJar, SafePaths.resolveChild(
                    versionDirectory, finalId + ".jar", "final client jar"));
        }
    }

    private static void writeAtomically(Path target, String text) throws IOException {
        Path parent = target.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "caml-modpack-json-", ".tmp");
        try {
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            try {
                Files.move(temporary, target,
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    private void downloadArchive(
            ModFile file,
            Path archive,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        if (!file.hasDownloadUri()) {
            throw new BackendException(BackendError.of(
                    "modpack.error.missingFileUrl", file.fileName()));
        }
        fileDownloader.download(new DownloadItem(
                file.name(),
                file.downloadUris(),
                archive,
                file.sha1(),
                file.size()
        ), listener, null, token);
    }

    private ModrinthModpackIndex readIndex(Path archive, String displayName,
                                           ArchiveExtractionGuard extractionGuard)
            throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            ZipEntry entry = zip.getEntry("modrinth.index.json");
            if (entry == null) {
                throw invalidArchive(displayName);
            }
            byte[] bytes;
            try (InputStream input = zip.getInputStream(entry)) {
                extractionGuard.beginEntry();
                bytes = extractionGuard.readAll(input);
            }
            return ModrinthModpackIndex.parse(new String(bytes, StandardCharsets.UTF_8));
        } catch (BackendException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw invalidArchive(displayName, e);
        }
    }

    private void installLoader(
            ModrinthModpackIndex index,
            String instance,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            SharedLibraryTransaction libraryTransaction
    ) throws IOException, InterruptedException {
        String minecraft = index.dependencies().get("minecraft");
        if (minecraft == null || minecraft.isBlank()) {
            throw new BackendException(BackendError.of("modpack.error.missingMinecraft"));
        }
        String fabric = index.dependencies().get("fabric-loader");
        String quilt = index.dependencies().get("quilt-loader");
        String forge = index.dependencies().get("forge");
        String neoforge = index.dependencies().get("neoforge");
        if (neoforge == null) {
            neoforge = index.dependencies().get("neo-forge");
        }
        for (String key : index.dependencies().keySet()) {
            if (!java.util.Set.of("minecraft", "fabric-loader", "quilt-loader",
                    "forge", "neoforge", "neo-forge").contains(key)) {
                throw new BackendException(BackendError.of(
                        "modpack.error.unsupportedLoader", key));
            }
        }
        if (fabric != null && !fabric.isBlank()) {
            if (libraryTransaction == null) {
                fabricInstaller.install(minecraft, instance, fabric, null,
                        gameDirectory, listener, token);
            } else {
                fabricInstaller.install(minecraft, instance, fabric, null,
                        gameDirectory, listener, token, libraryTransaction);
            }
        } else if (quilt != null && !quilt.isBlank()) {
            if (libraryTransaction == null) {
                quiltInstaller.install(minecraft, instance, quilt, null,
                        gameDirectory, listener, token);
            } else {
                quiltInstaller.install(minecraft, instance, quilt, null,
                        gameDirectory, listener, token, libraryTransaction);
            }
        } else if (neoforge != null && !neoforge.isBlank()) {
            installExternalLoader(
                    "neoforge", minecraft, instance, neoforgeInstaller(neoforge),
                    gameDirectory, listener, token, libraryTransaction);
        } else if (forge != null && !forge.isBlank()) {
            installExternalLoader(
                    "forge", minecraft, instance, forgeInstaller(minecraft, forge),
                    gameDirectory, listener, token, libraryTransaction);
        } else {
            versionInstaller.install(minecraft, instance, gameDirectory,
                    manifestUri, manifestCacheFile, listener, token);
        }
    }

    private void installExternalLoader(
            String loaderName,
            String minecraft,
            String instance,
            URI installerUri,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token,
            SharedLibraryTransaction libraryTransaction
    ) throws IOException, InterruptedException {
        if (libraryTransaction == null) {
            externalLoaderInstaller.install(
                    loaderName, minecraft, instance, installerUri,
                    gameDirectory, listener, token);
        } else {
            externalLoaderInstaller.install(
                    loaderName, minecraft, instance, installerUri,
                    gameDirectory, listener, token, libraryTransaction);
        }
    }

    private static URI forgeInstaller(String minecraft, String loader) {
        String full = loader.startsWith(minecraft + "-") ? loader : minecraft + "-" + loader;
        return URI.create("https://maven.minecraftforge.net/net/minecraftforge/forge/"
                + full + "/forge-" + full + "-installer.jar");
    }

    private static URI neoforgeInstaller(String loader) {
        return URI.create("https://maven.neoforged.net/releases/net/neoforged/neoforge/"
                + loader + "/neoforge-" + loader + "-installer.jar");
    }

    void extractOverrides(Path archive, Path instanceDirectory, String displayName,
                          ArchiveExtractionGuard extractionGuard)
            throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            Set<Path> extractedFiles = new HashSet<>();
            extractPrefix(zip, "overrides/", instanceDirectory, displayName,
                    extractedFiles, extractionGuard);
            extractPrefix(zip, "client-overrides/", instanceDirectory, displayName,
                    extractedFiles, extractionGuard);
        } catch (BackendException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw invalidArchive(displayName, e);
        }
    }

    private void extractPrefix(
            ZipFile zip,
            String prefix,
            Path instanceDirectory,
            String displayName,
            Set<Path> extractedFiles,
            ArchiveExtractionGuard extractionGuard
    ) throws IOException {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!name.startsWith(prefix)) {
                continue;
            }
            extractionGuard.beginEntry();
            String relative = name.substring(prefix.length());
            if (relative.isBlank()) {
                continue;
            }
            Path target;
            try {
                target = SafePaths.resolveUnder(instanceDirectory, relative, "modpack override");
            } catch (IllegalArgumentException e) {
                throw invalidArchive(displayName, e);
            }
            if (entry.isDirectory()) {
                Files.createDirectories(target);
            } else {
                if (!extractedFiles.add(target)) {
                    throw invalidArchive(displayName);
                }
                Files.createDirectories(target.getParent());
                try (InputStream input = zip.getInputStream(entry)) {
                    try (OutputStream output = Files.newOutputStream(target,
                            StandardOpenOption.CREATE,
                            StandardOpenOption.TRUNCATE_EXISTING,
                            StandardOpenOption.WRITE)) {
                        extractionGuard.copy(input, output);
                    }
                }
            }
        }
    }

    void downloadFiles(
            ModrinthModpackIndex index,
            Path instanceDirectory,
            String displayName,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        List<DownloadItem> items = new ArrayList<>();
        Set<Path> destinations = new HashSet<>();
        for (ModrinthModpackIndex.File file : index.files()) {
            if (file.clientUnsupported()) {
                continue;
            }
            if (file.downloads().isEmpty()) {
                throw new BackendException(BackendError.of(
                        "modpack.error.missingFileUrl", file.path()));
            }
            if (!hasText(file.sha1()) || file.size() <= 0) {
                throw new BackendException(BackendError.of(
                        "modpack.error.invalidFile", file.path()));
            }
            Path target;
            try {
                target = SafePaths.resolveUnder(instanceDirectory, file.path(), "modpack file");
            } catch (IllegalArgumentException e) {
                throw new BackendException(BackendError.of(
                        "modpack.error.invalidFile", file.path()), e);
            }
            if (!destinations.add(target) || Files.exists(target)) {
                throw new BackendException(BackendError.of(
                        "modpack.error.invalidFile", file.path()));
            }
            items.add(new DownloadItem(
                    file.path(),
                    file.downloads(),
                    target,
                    file.sha1(),
                    file.size()
            ));
        }
        DownloadExecutor.download(items, downloadConcurrency, listener,
                (item, progress) -> fileDownloader.download(item, progress, null, token), token);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static BackendException invalidArchive(String displayName) {
        return new BackendException(BackendError.of("modpack.error.invalidArchive", displayName));
    }

    private static BackendException invalidArchive(String displayName, Throwable cause) {
        return new BackendException(BackendError.of("modpack.error.invalidArchive", displayName), cause);
    }

}
