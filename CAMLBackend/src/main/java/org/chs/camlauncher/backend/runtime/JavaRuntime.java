package org.chs.camlauncher.backend.runtime;

import java.nio.file.Path;
import java.util.Objects;

public record JavaRuntime(
        Path executable,
        String version,
        int majorVersion,
        String vendor,
        String architecture,
        int bits,
        boolean jdk
) {
    public JavaRuntime {
        Objects.requireNonNull(executable, "executable");
        Objects.requireNonNull(version, "version");
        Objects.requireNonNull(vendor, "vendor");
        Objects.requireNonNull(architecture, "architecture");
    }
}
