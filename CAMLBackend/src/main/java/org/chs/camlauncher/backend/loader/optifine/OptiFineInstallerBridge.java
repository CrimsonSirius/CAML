package org.chs.camlauncher.backend.loader.optifine;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.nio.file.Path;

/** 在独立类加载器中调用 OptiFine 官方 installer 的 doInstall(File)。 */
public final class OptiFineInstallerBridge {
    private OptiFineInstallerBridge() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("用法: OptiFineInstallerBridge <installer.jar> <gameDir>");
        }
        Path installer = Path.of(args[0]).toAbsolutePath().normalize();
        Path gameDirectory = Path.of(args[1]).toAbsolutePath().normalize();

        try (URLClassLoader loader = new URLClassLoader(
                new java.net.URL[]{installer.toUri().toURL()},
                ClassLoader.getPlatformClassLoader()
        )) {
            Class<?> type = Class.forName("optifine.Installer", true, loader);
            Method doInstall = type.getMethod("doInstall", File.class);
            try {
                doInstall.invoke(null, gameDirectory.toFile());
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause() == null ? e : e.getCause();
                cause.printStackTrace();
                System.exit(1);
            }
        }
    }
}
