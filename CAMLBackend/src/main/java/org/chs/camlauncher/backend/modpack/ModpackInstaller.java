package org.chs.camlauncher.backend.modpack;

import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadConcurrency;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.io.DownloadExecutor;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;
import org.chs.camlauncher.backend.loader.fabric.FabricInstaller;
import org.chs.camlauncher.backend.loader.quilt.QuiltInstaller;
import org.chs.camlauncher.backend.mod.model.ModDownloadResult;
import org.chs.camlauncher.backend.mod.model.ModFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        InstanceNameRules.ensureNewInstance(gameDirectory, instance);
        Files.createDirectories(gameDirectory);

        Path archive = Files.createTempFile(gameDirectory, ".caml-modpack-", ".mrpack");
        Path instanceDirectory = gameDirectory.resolve("versions").resolve(instance);
        try {
            downloadArchive(file, archive, listener, token);
            ModrinthModpackIndex index = readIndex(archive, file.fileName());
            installLoader(index, instance, gameDirectory, listener, token);
            extractOverrides(archive, instanceDirectory, file.fileName());
            downloadFiles(index, instanceDirectory, file.fileName(), listener, token);
            ModpackLayout.writeMarker(instanceDirectory, index, file);
            return new ModDownloadResult(instanceDirectory, file);
        } catch (IOException | RuntimeException | InterruptedException e) {
            deleteRecursively(instanceDirectory);
            throw e;
        } finally {
            Files.deleteIfExists(archive);
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

    private static ModrinthModpackIndex readIndex(Path archive, String displayName) throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            ZipEntry entry = zip.getEntry("modrinth.index.json");
            if (entry == null) {
                throw invalidArchive(displayName);
            }
            byte[] bytes;
            try (InputStream input = zip.getInputStream(entry)) {
                bytes = input.readAllBytes();
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
            DownloadCancellationToken token
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
            fabricInstaller.install(minecraft, instance, fabric, null, gameDirectory, listener, token);
        } else if (quilt != null && !quilt.isBlank()) {
            quiltInstaller.install(minecraft, instance, quilt, null, gameDirectory, listener, token);
        } else if (neoforge != null && !neoforge.isBlank()) {
            externalLoaderInstaller.install("neoforge", minecraft, instance,
                    neoforgeInstaller(neoforge), gameDirectory, listener, token);
        } else if (forge != null && !forge.isBlank()) {
            externalLoaderInstaller.install("forge", minecraft, instance,
                    forgeInstaller(minecraft, forge), gameDirectory, listener, token);
        } else {
            versionInstaller.install(minecraft, instance, gameDirectory,
                    manifestUri, manifestCacheFile, listener, token);
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

    private static void extractOverrides(Path archive, Path instanceDirectory, String displayName)
            throws IOException {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            extractPrefix(zip, "overrides/", instanceDirectory, displayName);
            extractPrefix(zip, "client-overrides/", instanceDirectory, displayName);
        } catch (BackendException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw invalidArchive(displayName, e);
        }
    }

    private static void extractPrefix(
            ZipFile zip,
            String prefix,
            Path instanceDirectory,
            String displayName
    ) throws IOException {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!name.startsWith(prefix)) {
                continue;
            }
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
                Files.createDirectories(target.getParent());
                try (InputStream input = zip.getInputStream(entry)) {
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private void downloadFiles(
            ModrinthModpackIndex index,
            Path instanceDirectory,
            String displayName,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        List<DownloadItem> items = new ArrayList<>();
        for (ModrinthModpackIndex.File file : index.files()) {
            if (file.clientUnsupported()) {
                continue;
            }
            if (file.downloads().isEmpty()) {
                throw new BackendException(BackendError.of(
                        "modpack.error.missingFileUrl", file.path()));
            }
            Path target;
            try {
                target = SafePaths.resolveUnder(instanceDirectory, file.path(), "modpack file");
            } catch (IllegalArgumentException e) {
                throw new BackendException(BackendError.of(
                        "modpack.error.invalidFile", file.path()), e);
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

    private static BackendException invalidArchive(String displayName) {
        return new BackendException(BackendError.of("modpack.error.invalidArchive", displayName));
    }

    private static BackendException invalidArchive(String displayName, Throwable cause) {
        return new BackendException(BackendError.of("modpack.error.invalidArchive", displayName), cause);
    }

    private static void deleteRecursively(Path root) {
        if (!Files.exists(root)) {
            return;
        }
        try (var paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException ignored) {
                    // 清理失败不覆盖原始安装异常。
                }
            });
        } catch (IOException ignored) {
            // 清理失败不覆盖原始安装异常。
        }
    }
}
