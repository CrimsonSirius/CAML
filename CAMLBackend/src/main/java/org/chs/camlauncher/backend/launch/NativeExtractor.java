package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.metadata.DownloadInfo;
import org.chs.camlauncher.backend.metadata.Library;
import org.chs.camlauncher.backend.metadata.RuleEvaluator;
import org.chs.camlauncher.backend.runtime.JavaRuntimeParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class NativeExtractor {
    private final RuleEvaluator ruleEvaluator;
    private final String osKey;
    private final int bits;

    public NativeExtractor() {
        this(
                new RuleEvaluator(),
                RuleEvaluator.currentOsName(),
                JavaRuntimeParser.parseBits(
                        System.getProperty("sun.arch.data.model"),
                        RuleEvaluator.currentOsArch()
                )
        );
    }

    public NativeExtractor(RuleEvaluator ruleEvaluator, String osKey, int bits) {
        this.ruleEvaluator = Objects.requireNonNull(ruleEvaluator, "ruleEvaluator");
        this.osKey = Objects.requireNonNull(osKey, "osKey");
        if (bits <= 0) {
            throw new IllegalArgumentException("bits must be positive");
        }
        this.bits = bits;
    }

    public void extract(LaunchPlan plan) throws IOException {
        Path nativesDirectory = plan.nativesDirectory().toAbsolutePath().normalize();
        cleanDirectory(nativesDirectory);
        Files.createDirectories(nativesDirectory);

        Set<NativeJar> nativeJars = new LinkedHashSet<>();
        for (Library library : plan.manifest().libraries()) {
            if (ruleEvaluator.isAllowed(library.rules())) {
                nativeJars.addAll(nativeJars(plan.instance().librariesDirectory(), library));
            }
        }

        for (NativeJar jar : nativeJars) {
            if (Files.isRegularFile(jar.path())) {
                extractJar(jar.path(), nativesDirectory, jar.extract());
            }
        }
    }

    private List<NativeJar> nativeJars(Path librariesDirectory, Library library) {
        Library.Downloads downloads = library.downloads();
        if (downloads == null) {
            return List.of();
        }

        List<NativeJar> result = new ArrayList<>(1);
        String classifier = library.natives().get(osKey);
        if (classifier != null) {
            classifier = classifier.replace("${arch}", Integer.toString(bits));
            DownloadInfo nativeArtifact = downloads.classifiers().get(classifier);
            addArtifact(result, librariesDirectory, nativeArtifact, library.extract());
        } else if (isNativeOnly(library)) {
            addArtifact(result, librariesDirectory, downloads.artifact(), library.extract());
        }
        return List.copyOf(result);
    }

    private static void addArtifact(
            List<NativeJar> result,
            Path librariesDirectory,
            DownloadInfo info,
            Library.Extract extract
    ) {
        if (info != null && info.path() != null && !info.path().isBlank()) {
            result.add(new NativeJar(
                    librariesDirectory.resolve(info.path()).toAbsolutePath().normalize(),
                    extract
            ));
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

    private static void extractJar(Path jar, Path nativesDirectory, Library.Extract extract) throws IOException {
        try (ZipFile zipFile = new ZipFile(jar.toFile())) {
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }

                String name = entry.getName();
                if (isExcluded(name, extract)) {
                    continue;
                }

                Path target = nativesDirectory.resolve(name).normalize();
                if (!target.startsWith(nativesDirectory)) {
                    continue;
                }
                if (target.getParent() != null) {
                    Files.createDirectories(target.getParent());
                }
                try (InputStream input = zipFile.getInputStream(entry)) {
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static boolean isExcluded(String name, Library.Extract extract) {
        if (name.startsWith("META-INF/")) {
            return true;
        }
        if (extract == null) {
            return false;
        }
        for (String pattern : extract.exclude()) {
            if (pattern == null || pattern.isBlank()) {
                continue;
            }
            if (pattern.endsWith("/")) {
                if (name.startsWith(pattern)) {
                    return true;
                }
            } else if (name.equals(pattern) || name.startsWith(pattern + "/")) {
                return true;
            }
        }
        return false;
    }

    private static void cleanDirectory(Path directory) throws IOException {
        if (Files.notExists(directory)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private record NativeJar(Path path, Library.Extract extract) {
    }
}
