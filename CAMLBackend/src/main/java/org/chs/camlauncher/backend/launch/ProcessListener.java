package org.chs.camlauncher.backend.launch;

public interface ProcessListener {
    ProcessListener NONE = (line, error) -> {
    };

    void onLog(String line, boolean error);

    default void onExit(int exitCode) {
    }
}
