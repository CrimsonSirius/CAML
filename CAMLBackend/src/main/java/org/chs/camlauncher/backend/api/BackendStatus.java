package org.chs.camlauncher.backend.api;

import java.nio.file.Path;

public record BackendStatus(boolean ready, String version, Path dataDirectory, Path gameDirectory) {
}
