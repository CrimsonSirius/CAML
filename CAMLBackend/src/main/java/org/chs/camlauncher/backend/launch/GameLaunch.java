package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.auth.AuthSession;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.launch.process.GameProcess;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.nio.file.Path;
import java.util.List;

public record GameLaunch(
        InstalledVersion installed,
        AuthSession auth,
        JavaRuntime java,
        LaunchPlan plan,
        List<Path> classpath,
        GameProcess process
) {
    public GameLaunch {
        classpath = List.copyOf(classpath);
    }
}
