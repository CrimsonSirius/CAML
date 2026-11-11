package org.chs.camlauncher.backend.launch.process;

import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.launch.LaunchException;
import org.chs.camlauncher.backend.launch.LaunchFailure;
import org.chs.camlauncher.backend.launch.LaunchPlan;
import org.chs.camlauncher.backend.launch.command.LaunchCommand;
import org.chs.camlauncher.backend.launch.command.LaunchCommandBuilder;
import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        try {
            return start(command.asList(), command.workingDirectory(), command.environment(), listener, null, plan);
        } catch (IOException firstFailure) {
            if (!LaunchException.isCommandTooLong(firstFailure)) {
                throw LaunchException.from(firstFailure);
            }

            IOException argFileFailure = null;
            if (plan.java().majorVersion() >= 9) {
                try {
                    return launchWithArgFile(plan, command, listener);
                } catch (IOException e) {
                    argFileFailure = e;
                }
            }

            try {
                return launchWithManifest(plan, command, classpath, listener);
            } catch (IOException manifestFailure) {
                if (argFileFailure != null) {
                    manifestFailure.addSuppressed(argFileFailure);
                }
                throw new LaunchException(
                        LaunchFailure.COMMAND_TOO_LONG,
                        BackendError.of("launch.commandTooLongAfterFallback"),
                        manifestFailure
                );
            }
        }

    }

    private GameProcess launchWithArgFile(LaunchPlan plan, LaunchCommand command, ProcessListener listener)
            throws IOException {
        Path argumentFile = ArgumentFile.write(command.jvmArguments());
        try {
            List<String> commandLine = new ArrayList<>();
            commandLine.add(command.javaExecutable().toString());
            commandLine.add("@" + argumentFile);
            commandLine.add(command.mainClass());
            commandLine.addAll(command.gameArguments());
            return start(
                    commandLine,
                    command.workingDirectory(),
                    command.environment(),
                    listener,
                    argumentFile,
                    plan
            );
        } catch (IOException e) {
            Files.deleteIfExists(argumentFile);
            throw e;
        }
    }

    private GameProcess launchWithManifest(
            LaunchPlan plan,
            LaunchCommand command,
            List<Path> classpath,
            ProcessListener listener
    ) throws IOException {
        Path manifestJar = ClasspathManifest.create(classpath);
        try {
            List<String> jvmArguments = new ArrayList<>();
            for (int i = 0; i < command.jvmArguments().size(); i++) {
                String argument = command.jvmArguments().get(i);
                if (argument.equals("-cp") || argument.equals("-classpath")) {
                    i++;
                    continue;
                }
                jvmArguments.add(argument);
            }
            jvmArguments.add("-cp");
            jvmArguments.add(manifestJar.toString());

            List<String> commandLine = new ArrayList<>();
            commandLine.add(command.javaExecutable().toString());
            commandLine.addAll(jvmArguments);
            commandLine.add(command.mainClass());
            commandLine.addAll(command.gameArguments());
            return start(
                    commandLine,
                    command.workingDirectory(),
                    command.environment(),
                    listener,
                    manifestJar,
                    plan
            );
        } catch (IOException e) {
            Files.deleteIfExists(manifestJar);
            throw e;
        }
    }

    private static GameProcess start(
            List<String> commandLine,
            Path workingDirectory,
            Map<String, String> environment,
            ProcessListener listener,
            Path cleanupFile,
            LaunchPlan plan
    ) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(commandLine);
        builder.directory(workingDirectory.toFile());
        builder.environment().putAll(environment);

        if (isWindows()) {
            // 启用版本隔离时 workingDirectory 是 versions/<id>,
            // 其父级会是 versions 目录，因此以 .minecraft 根目录为基准计算 APPDATA。
            Path appData = plan.instance().gameDirectory().toAbsolutePath().getParent();
            if (appData != null) {
                builder.environment().put("APPDATA", appData.toString());
            }
        }

        return new GameProcess(builder.start(), commandLine, listener, cleanupFile);

    }

    private static boolean isWindows() {
        return "windows".equals(RuleEvaluator.currentOsName());
    }

}
