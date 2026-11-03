package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.metadata.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.nio.file.Path;
import java.util.Objects;

public record LaunchPlan(
        InstalledVersion instance,
        VersionInfo manifest,
        AuthSession auth,
        JavaRuntime java,
        LaunchOptions options,
        Path gameDirectory,
        Path assetsDirectory,
        Path nativesDirectory
) {
    public LaunchPlan {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(auth, "auth");
        Objects.requireNonNull(java, "java");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        Objects.requireNonNull(assetsDirectory, "assetsDirectory");
        Objects.requireNonNull(nativesDirectory, "nativesDirectory");
    }

    public LaunchPlan withAssetsDirectory(Path assetsDirectory) {
        return new LaunchPlan(
                instance,
                manifest,
                auth,
                java,
                options,
                gameDirectory,
                assetsDirectory,
                nativesDirectory
        );
    }
}
