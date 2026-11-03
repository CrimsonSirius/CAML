package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.metadata.RuleEvaluator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class ProcessLauncher {
    private final LaunchCommandBuilder commandBuilder;

    public ProcessLauncher() {
        this(new LaunchCommandBuilder());
    }

    public ProcessLauncher(LaunchCommandBuilder commandBuilder) {
        this.commandBuilder = Objects.requireNonNull(commandBuilder, "commandBuilder");
    }

    public GameProcess launch(LaunchPlan plan, List<Path> classpath, ProcessListener listener)
            throws IOException {
        LaunchCommand command = commandBuilder.build(plan, classpath);
        ProcessBuilder builder = new ProcessBuilder(command.asList());
        builder.directory(command.workingDirectory().toFile());
        builder.environment().putAll(command.environment());

        if (isWindows()) {
            Path appData = command.workingDirectory().toAbsolutePath().getParent();
            if (appData != null) {
                builder.environment().put("APPDATA", appData.toString());
            }
        }

        return new GameProcess(builder.start(), command.asList(), listener);
    }

    private static boolean isWindows() {
        return "windows".equals(RuleEvaluator.currentOsName());
    }
}
