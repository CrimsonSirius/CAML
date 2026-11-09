package org.chs.camlauncher.backend.runtime;

import java.nio.file.Path;
import java.util.Objects;

/** 设置页展示的一个 Java 运行时候选。 */
public record JavaCandidate(
        Path executable,
        String version,
        int majorVersion,
        String vendor,
        String architecture,
        int bits,
        boolean jdk,
        boolean manual,
        boolean available
) {
    public JavaCandidate {
        Objects.requireNonNull(executable, "executable");
        version = version == null ? "" : version;
        vendor = vendor == null ? "" : vendor;
        architecture = architecture == null ? "" : architecture;
    }

    public static JavaCandidate from(JavaRuntime runtime, boolean manual) {
        return new JavaCandidate(
                runtime.executable(),
                runtime.version(),
                runtime.majorVersion(),
                runtime.vendor(),
                runtime.architecture(),
                runtime.bits(),
                runtime.jdk(),
                manual,
                true
        );
    }

    public static JavaCandidate unavailable(Path executable, boolean manual) {
        return new JavaCandidate(executable, "", 0, "", "", 0, false, manual, false);
    }

    public JavaCandidate withManual(boolean value) {
        return new JavaCandidate(
                executable, version, majorVersion, vendor, architecture, bits, jdk, value, available);
    }
}
