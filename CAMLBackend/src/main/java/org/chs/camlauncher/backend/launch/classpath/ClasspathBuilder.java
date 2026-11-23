package org.chs.camlauncher.backend.launch.classpath;

import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.metadata.model.Library;
import org.chs.camlauncher.backend.metadata.model.LibraryArtifact;
import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;
import org.chs.camlauncher.backend.io.SafePaths;

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
        Path deferredOptiFine = null;
        boolean legacyLaunchWrapper = "net.minecraft.launchwrapper.Launch"
                .equals(plan.manifest().mainClass());

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
            Path artifact = SafePaths.resolveUnder(librariesDirectory, relativePath, "library");
            if (paths.add(artifact)) {
                if (legacyLaunchWrapper && isOptiFinePatch(library)) {
                    deferredOptiFine = artifact;
                } else {
                    classpath.add(artifact);
                }
            }
        }

        if (!usesBootstrapLauncher(plan.manifest().mainClass())) {
            classpath.add(plan.instance().clientJar().toAbsolutePath().normalize());
        }
        // OptiFine 补丁 jar 自带一个 anb.class。LaunchWrapper 会先按 classpath
        // 找到它，Forge 再拿这个类去比对原版 binpatch 时就会因 Adler-32 不同而退出。
        // 把补丁 jar 放到原版 jar 之后，FML 才能读到干净的输入类。
        if (deferredOptiFine != null) {
            classpath.add(deferredOptiFine);
        }
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

    private static boolean usesBootstrapLauncher(String mainClass) {
        return mainClass != null
                && mainClass.startsWith("cpw.mods.bootstraplauncher.BootstrapLauncher");
    }

    private static boolean isOptiFinePatch(Library library) {
        String[] parts = library.name().split(":");
        return parts.length >= 2
                && "optifine".equals(parts[0])
                && "OptiFine".equals(parts[1]);
    }
}
