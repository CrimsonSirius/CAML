package org.chs.camlauncher.backend.launch.command;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LaunchCommand(
        Path javaExecutable,
        List<String> jvmArguments,
        String mainClass,
        List<String> gameArguments,
        Path workingDirectory,
        Map<String, String> environment
) {
    public LaunchCommand {
        Objects.requireNonNull(javaExecutable, "javaExecutable");
        jvmArguments = List.copyOf(jvmArguments);
        Objects.requireNonNull(mainClass, "mainClass");
        gameArguments = List.copyOf(gameArguments);
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        environment = Map.copyOf(environment);
    }

    public List<String> arguments() {
        List<String> arguments = new ArrayList<>(jvmArguments.size() + gameArguments.size() + 1);
        arguments.addAll(jvmArguments);
        arguments.add(mainClass);
        arguments.addAll(gameArguments);
        return List.copyOf(arguments);
    }

    public List<String> asList() {
        List<String> command = new ArrayList<>(jvmArguments.size() + gameArguments.size() + 2);
        command.add(javaExecutable.toString());
        command.addAll(arguments());
        return List.copyOf(command);
    }
}
