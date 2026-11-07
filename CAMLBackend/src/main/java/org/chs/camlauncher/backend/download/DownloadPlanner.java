package org.chs.camlauncher.backend.download;

import org.chs.camlauncher.backend.metadata.AssetIndex;
import org.chs.camlauncher.backend.metadata.DownloadInfo;
import org.chs.camlauncher.backend.metadata.Library;
import org.chs.camlauncher.backend.metadata.LibraryArtifact;
import org.chs.camlauncher.backend.metadata.LoggingInfo;
import org.chs.camlauncher.backend.metadata.RuleEvaluator;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntimeParser;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class DownloadPlanner {
    private final List<DownloadSource> sources;
    private final RuleEvaluator ruleEvaluator;
    private final String osKey;
    private final int bits;

    public DownloadPlanner() {
        this(
                List.of(OfficialDownloadSource.INSTANCE),
                new RuleEvaluator(),
                RuleEvaluator.currentOsName(),
                JavaRuntimeParser.parseBits(
                        System.getProperty("sun.arch.data.model"),
                        RuleEvaluator.currentOsArch()
                )
        );
    }

    public DownloadPlanner(List<DownloadSource> sources, RuleEvaluator ruleEvaluator, String osKey, int bits) {
        this.sources = List.copyOf(sources);
        this.ruleEvaluator = Objects.requireNonNull(ruleEvaluator, "ruleEvaluator");
        this.osKey = Objects.requireNonNull(osKey, "osKey");
        if (bits <= 0) {
            throw new IllegalArgumentException("bits must be positive");
        }
        this.bits = bits;
    }

    public DownloadPlan plan(VersionInfo version, Path gameDirectory) {
        Map<Path, DownloadItem> items = new LinkedHashMap<>();
        String clientJarId = version.clientJarId();
        Path clientJarDirectory = gameDirectory.resolve("versions").resolve(clientJarId);

        DownloadInfo client = version.downloads().get("client");
        if (client != null) {
            add(items, "client " + clientJarId, client,
                    clientJarDirectory.resolve(clientJarId + ".jar"));
        }

        for (Library library : version.libraries()) {
            if (!ruleEvaluator.isAllowed(library.rules()) || !library.isClientRequired()) {
                continue;
            }

            Library.Downloads downloads = library.downloads();
            if (downloads == null) {
                addLegacyLibrary(items, gameDirectory, library);
                continue;
            }

            if (downloads.artifact() != null) {
                add(items, library.name(), downloads.artifact(),
                        libraryPath(gameDirectory, downloads.artifact()));
            }

            String classifier = library.natives().get(osKey);
            if (classifier != null) {
                classifier = classifier.replace("${arch}", Integer.toString(bits));
                DownloadInfo nativeArtifact = downloads.classifiers().get(classifier);
                if (nativeArtifact != null) {
                    add(items, library.name() + ":" + classifier, nativeArtifact,
                            libraryPath(gameDirectory, nativeArtifact));
                }
            }
        }

        AssetIndex assetIndex = version.assetIndex();
        if (assetIndex != null) {
            add(items, "asset-index " + assetIndex.id(),
                    new DownloadInfo(null, assetIndex.sha1(), assetIndex.size(), assetIndex.url()),
                    gameDirectory.resolve("assets").resolve("indexes").resolve(assetIndex.id() + ".json"));
        }

        LoggingInfo logging = version.logging().get("client");
        if (logging != null && logging.file() != null) {
            DownloadInfo file = logging.file();
            String fileName = file.id();
            if ((fileName == null || fileName.isBlank()) && file.url() != null) {
                fileName = fileNameFromUrl(file.url());
            }
            if (fileName != null && !fileName.isBlank()) {
                add(items, "log-config " + fileName, file,
                        gameDirectory.resolve("assets").resolve("log_configs").resolve(fileName));
            }
        }

        return new DownloadPlan(items.values().stream().toList());
    }

    private void addLegacyLibrary(Map<Path, DownloadItem> items, Path gameDirectory, Library library) {
        String path = LibraryArtifact.relativePath(library);
        String url = LibraryArtifact.downloadUrl(library);
        if (path == null || url == null) {
            return;
        }
        add(items, library.name(), new DownloadInfo(
                path,
                LibraryArtifact.sha1(library),
                LibraryArtifact.size(library),
                url
        ), gameDirectory.resolve("libraries").resolve(path));
    }

    private void add(Map<Path, DownloadItem> items, String label, DownloadInfo info, Path destination) {
        if (info == null || info.url() == null || info.url().isBlank() || destination == null) {
            return;
        }
        items.putIfAbsent(destination.toAbsolutePath().normalize(), new DownloadItem(
                label,
                DownloadSources.candidates(sources, info.url()),
                destination,
                info.sha1(),
                info.size() == null ? -1 : info.size()
        ));
    }

    private static Path libraryPath(Path gameDirectory, DownloadInfo info) {
        if (info.path() == null || info.path().isBlank()) {
            return null;
        }
        return gameDirectory.resolve("libraries").resolve(info.path());
    }

    private static String fileNameFromUrl(String url) {
        int slash = url.lastIndexOf('/');
        return slash >= 0 && slash + 1 < url.length() ? url.substring(slash + 1) : null;
    }
}
