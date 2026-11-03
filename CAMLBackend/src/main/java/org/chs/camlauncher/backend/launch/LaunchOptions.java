package org.chs.camlauncher.backend.launch;

import java.util.List;
import java.util.Map;

public record LaunchOptions(
        Integer minMemoryMb,
        Integer maxMemoryMb,
        Integer width,
        Integer height,
        boolean fullscreen,
        List<String> jvmArguments,
        List<String> gameArguments,
        Map<String, String> environmentVariables
) {
    public LaunchOptions {
        if (minMemoryMb != null && minMemoryMb < 0) {
            throw new IllegalArgumentException("minMemoryMb must not be negative");
        }
        if (maxMemoryMb != null && maxMemoryMb < 0) {
            throw new IllegalArgumentException("maxMemoryMb must not be negative");
        }
        if (minMemoryMb != null && maxMemoryMb != null && maxMemoryMb < minMemoryMb) {
            throw new IllegalArgumentException("maxMemoryMb must be greater than or equal to minMemoryMb");
        }
        if (width != null && width <= 0) {
            throw new IllegalArgumentException("width must be positive");
        }
        if (height != null && height <= 0) {
            throw new IllegalArgumentException("height must be positive");
        }
        jvmArguments = List.copyOf(jvmArguments);
        gameArguments = List.copyOf(gameArguments);
        environmentVariables = Map.copyOf(environmentVariables);
    }

    public static LaunchOptions defaults() {
        return new LaunchOptions(
                null,
                null,
                null,
                null,
                false,
                List.of(),
                List.of(),
                Map.of()
        );
    }
}
