package org.chs.camlauncher.backend.launch.command;

import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.runtime.JavaLocator;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class LaunchCommandBuilder {
    private final JvmArgumentsBuilder jvmArgumentsBuilder;
    private final GameArgumentsBuilder gameArgumentsBuilder;
    private final JavaLocator javaLocator;

    public LaunchCommandBuilder() {
        this(new JvmArgumentsBuilder(), new GameArgumentsBuilder(), new JavaLocator());
    }

    public LaunchCommandBuilder(
            JvmArgumentsBuilder jvmArgumentsBuilder,
            GameArgumentsBuilder gameArgumentsBuilder,
            JavaLocator javaLocator
    ) {
        this.jvmArgumentsBuilder = Objects.requireNonNull(jvmArgumentsBuilder, "jvmArgumentsBuilder");
        this.gameArgumentsBuilder = Objects.requireNonNull(gameArgumentsBuilder, "gameArgumentsBuilder");
        this.javaLocator = Objects.requireNonNull(javaLocator, "javaLocator");
    }

    public LaunchCommand build(LaunchPlan plan, List<Path> classpath) {
        Objects.requireNonNull(plan, "plan");
        String mainClass = plan.manifest().mainClass();
        if (mainClass == null || mainClass.isBlank()) {
            throw new IllegalArgumentException("Version manifest has no mainClass");
        }

        return new LaunchCommand(
                javaLocator.launchExecutable(plan.java()),
                jvmArgumentsBuilder.build(plan, classpath),
                mainClass,
                gameArgumentsBuilder.build(plan, classpath),
                plan.gameDirectory(),
                plan.options().environmentVariables()
        );
    }
}
