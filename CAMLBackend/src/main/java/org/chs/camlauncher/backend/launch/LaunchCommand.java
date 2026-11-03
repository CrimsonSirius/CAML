package org.chs.camlauncher.backend.launch;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record LaunchCommand(
        Path javaExecutable,
        List<String> arguments,
        Path workingDirectory,
        Map<String, String> environment
) {
    public LaunchCommand {
        Objects.requireNonNull(javaExecutable, "javaExecutable");
        arguments = List.copyOf(arguments);
        Objects.requireNonNull(workingDirectory, "workingDirectory");
        environment = Map.copyOf(environment);
    }

    public List<String> asList() {
        List<String> command = new ArrayList<>(arguments.size() + 1);
        command.add(javaExecutable.toString());
        command.addAll(arguments);
        return List.copyOf(command);
    }
}
