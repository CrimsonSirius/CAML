package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.runtime.JavaRequirement;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.nio.file.Path;
import java.util.Objects;

public final class LaunchPlanner {
    public LaunchPlan create(
            InstalledVersion instance,
            AuthSession auth,
            JavaRuntime java,
            LaunchOptions options
    ) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(auth, "auth");
        Objects.requireNonNull(java, "java");
        Objects.requireNonNull(options, "options");

        JavaRequirement requirement = JavaRequirement.from(instance.manifest());
        if (!requirement.isSatisfiedBy(java)) {
            throw new IllegalArgumentException(
                    "Java runtime does not satisfy version requirement: " + requirement
            );
        }

        return new LaunchPlan(
                instance,
                instance.manifest(),
                auth,
                java,
                options,
                instance.gameDirectory(),
                instance.assetsDirectory(),
                instance.versionDirectory().resolve("natives")
        );
    }
}
