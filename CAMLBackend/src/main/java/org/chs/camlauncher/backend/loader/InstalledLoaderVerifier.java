package org.chs.camlauncher.backend.loader;

import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.model.Library;
import org.chs.camlauncher.backend.metadata.model.LibraryArtifact;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** 校验外部 installer 产物与最终库文件。 */
final class InstalledLoaderVerifier {
    private InstalledLoaderVerifier() {
    }

    static void validateGeneratedVersion(
            String loaderName,
            Path versionDirectory,
            String installedVersionId,
            MetadataService metadata
    ) throws IOException {
        Path versionJson = versionDirectory.resolve(installedVersionId + ".json");
        if (!Files.isRegularFile(versionJson)) {
            throw new IOException(loaderName + " 安装器没有生成版本 JSON: " + versionJson);
        }
        VersionInfo version;
        try {
            version = metadata.parseVersionInfo(Files.readString(versionJson, StandardCharsets.UTF_8));
        } catch (RuntimeException e) {
            throw new IOException(loaderName + " 安装器生成的版本 JSON 无法解析: " + versionJson, e);
        }
        if (version == null || version.mainClass() == null || version.mainClass().isBlank()) {
            throw new IOException(loaderName + " 安装器生成的版本 JSON 缺少 mainClass: " + versionJson);
        }
    }

    static void verifyInstalledLibraries(String loaderName, InstalledVersion installed)
            throws IOException {
        RuleEvaluator evaluator = new RuleEvaluator();
        Set<String> coordinates = new LinkedHashSet<>();
        for (Library library : installed.manifest().libraries()) {
            if (!evaluator.isAllowed(library.rules())
                    || !library.isClientRequired()
                    || isNativeOnly(library)
                    || !coordinates.add(LibraryArtifact.coordinateKey(library.name()))) {
                continue;
            }
            String relativePath = LibraryArtifact.relativePath(library);
            if (relativePath == null || relativePath.isBlank()) {
                continue;
            }
            Path path = SafePaths.resolveUnder(
                    installed.librariesDirectory(), relativePath, "installed library");
            if (!Files.isRegularFile(path)) {
                throw new IOException(loaderName + " 安装结果缺少库文件: " + path);
            }
        }
    }

    private static boolean isNativeOnly(Library library) {
        String[] parts = library.name().split(":");
        if (parts.length < 4) {
            return false;
        }
        String classifier = parts[3].toLowerCase(Locale.ROOT);
        return classifier.startsWith("natives-") || classifier.contains("-natives-");
    }
}
