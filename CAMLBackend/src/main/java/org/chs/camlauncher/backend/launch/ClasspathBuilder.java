package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.metadata.Library;
import org.chs.camlauncher.backend.metadata.LibraryArtifact;
import org.chs.camlauncher.backend.metadata.RuleEvaluator;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
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
        Set<String> coordinates = new LinkedHashSet<>();
        Set<Path> paths = new LinkedHashSet<>();

        for (Library library : plan.manifest().libraries()) {
            if (!ruleEvaluator.isAllowed(library.rules())
                    || !library.isClientRequired()
                    || isNativeOnly(library)) {
                continue;
            }

            if (!coordinates.add(LibraryArtifact.coordinateKey(library.name()))) {
                continue;
            }

            String relativePath = LibraryArtifact.relativePath(library);
            if (relativePath == null || relativePath.isBlank()) {
                continue;
            }
            Path artifact = librariesDirectory.resolve(relativePath).toAbsolutePath().normalize();
            if (paths.add(artifact)) {
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

    private static boolean isNativeOnly(Library library) {
        String[] parts = library.name().split(":");
        if (parts.length < 4) {
            return false;
        }
        String classifier = parts[3].toLowerCase(java.util.Locale.ROOT);
        return classifier.startsWith("natives-") || classifier.contains("-natives-");
    }
}
