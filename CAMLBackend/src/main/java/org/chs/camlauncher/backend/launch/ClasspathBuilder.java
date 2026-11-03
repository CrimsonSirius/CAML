package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.metadata.DownloadInfo;
import org.chs.camlauncher.backend.metadata.Library;
import org.chs.camlauncher.backend.metadata.RuleEvaluator;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class ClasspathBuilder {
    private final RuleEvaluator ruleEvaluator;

    public ClasspathBuilder() {
        this(new RuleEvaluator());
    }

    public ClasspathBuilder(RuleEvaluator ruleEvaluator) {
        this.ruleEvaluator = Objects.requireNonNull(ruleEvaluator, "ruleEvaluator");
    }

    public List<Path> build(LaunchPlan plan) {
        Objects.requireNonNull(plan, "plan");

        Path librariesDirectory = plan.instance().librariesDirectory();
        Set<Path> classpath = new LinkedHashSet<>();

        for (Library library : plan.manifest().libraries()) {
            if (!ruleEvaluator.isAllowed(library.rules()) || isNativeOnly(library)) {
                continue;
            }

            Library.Downloads downloads = library.downloads();
            if (downloads == null || downloads.artifact() == null) {
                continue;
            }

            Path artifact = artifactPath(librariesDirectory, library, downloads.artifact());
            if (artifact != null) {
                classpath.add(artifact);
            }
        }

        classpath.add(plan.instance().clientJar().toAbsolutePath().normalize());
        return List.copyOf(classpath);
    }

    public String join(List<Path> classpath) {
        return classpath.stream()
                .map(Path::toString)
                .collect(java.util.stream.Collectors.joining(File.pathSeparator));
    }

    private static Path artifactPath(Path librariesDirectory, Library library, DownloadInfo artifact) {
        String relativePath = artifact.path();
        if (relativePath == null || relativePath.isBlank()) {
            relativePath = mavenPath(library.name());
        }
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        return librariesDirectory.resolve(relativePath).toAbsolutePath().normalize();
    }

    private static boolean isNativeOnly(Library library) {
        String[] parts = library.name().split(":");
        if (parts.length < 4) {
            return false;
        }
        String classifier = parts[3].toLowerCase(Locale.ROOT);
        return classifier.startsWith("natives-") || classifier.contains("-natives-");
    }

    private static String mavenPath(String name) {
        String[] parts = name.split(":");
        if (parts.length < 3) {
            return null;
        }

        String group = parts[0].replace('.', '/');
        String artifact = parts[1];
        String version = parts[2];
        String classifier = parts.length >= 4 ? parts[3] : null;
        String extension = "jar";

        int extensionIndex = version.indexOf('@');
        if (extensionIndex >= 0) {
            extension = version.substring(extensionIndex + 1);
            version = version.substring(0, extensionIndex);
        }

        String fileName = artifact + "-" + version;
        if (classifier != null) {
            fileName += "-" + classifier;
        }
        fileName += "." + extension;
        return group + "/" + artifact + "/" + version + "/" + fileName;
    }
}
