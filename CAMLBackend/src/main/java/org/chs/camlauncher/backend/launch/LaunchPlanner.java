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
        return create(instance, auth, java, options, instance.gameDirectory());
    }

    /**
     * 创建启动计划，并显式指定游戏数据目录。
     *
     * <p>启用版本隔离的版本需要把 {@code --gameDir} 指向
     * {@code versions/<id>}，才能读到原有的 {@code saves} 和 {@code options.txt}。
     * {@code libraries} 与 {@code assets} 仍然来自 {@code .minecraft} 根目录。</p>
     *
     * @param gameDataDirectory 实际作为游戏数据目录使用的路径
     */
    public LaunchPlan create(
            InstalledVersion instance,
            AuthSession auth,
            JavaRuntime java,
            LaunchOptions options,
            Path gameDataDirectory
    ) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(auth, "auth");
        Objects.requireNonNull(java, "java");
        Objects.requireNonNull(options, "options");
        Objects.requireNonNull(gameDataDirectory, "gameDataDirectory");

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
                gameDataDirectory,
                instance.assetsDirectory(),
                instance.versionDirectory().resolve("natives")
        );
    }
}