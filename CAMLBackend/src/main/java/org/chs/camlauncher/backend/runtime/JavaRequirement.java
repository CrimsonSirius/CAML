package org.chs.camlauncher.backend.runtime;

import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;

import java.util.Objects;

public record JavaRequirement(int majorVersion, String architecture, int bits) {
    public JavaRequirement {
        if (majorVersion <= 0) {
            throw new IllegalArgumentException("majorVersion must be positive");
        }
        Objects.requireNonNull(architecture, "architecture");
    }

    public static JavaRequirement from(VersionInfo version) {
        int majorVersion = version.javaVersion() != null && version.javaVersion().majorVersion() != null
                ? version.javaVersion().majorVersion()
                : 8;
        return new JavaRequirement(
                majorVersion,
                RuleEvaluator.currentOsArch(),
                currentBits()
        );
    }

    public boolean isSatisfiedBy(JavaRuntime runtime) {
        return runtime.majorVersion() >= majorVersion
                && runtime.architecture().equals(architecture)
                && (bits <= 0 || runtime.bits() == bits);
    }

    private static int currentBits() {
        return JavaRuntimeParser.parseBits(
                System.getProperty("sun.arch.data.model"),
                RuleEvaluator.currentOsArch()
        );
    }
}
