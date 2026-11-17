package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.auth.injector.AuthlibInjector;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public record LaunchPlan(
        InstalledVersion instance,
        VersionInfo manifest,
        AuthSession auth,
        JavaRuntime java,
        LaunchOptions options,
        Path gameDirectory,
        Path assetsDirectory,
        Path nativesDirectory,
        Optional<AuthlibInjector> injector
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
        injector = injector == null ? Optional.empty() : injector;
    }

    /** 不注入 authlib-injector 的启动计划（离线账号或未启用第三方认证）。 */
    public LaunchPlan(
            InstalledVersion instance,
            VersionInfo manifest,
            AuthSession auth,
            JavaRuntime java,
            LaunchOptions options,
            Path gameDirectory,
            Path assetsDirectory,
            Path nativesDirectory
    ) {
        this(instance, manifest, auth, java, options, gameDirectory,
                assetsDirectory, nativesDirectory, Optional.empty());
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
                nativesDirectory,
                injector
        );
    }

    public LaunchPlan withInjector(Optional<AuthlibInjector> injector) {
        return new LaunchPlan(
                instance,
                manifest,
                auth,
                java,
                options,
                gameDirectory,
                assetsDirectory,
                nativesDirectory,
                injector
        );
    }
}