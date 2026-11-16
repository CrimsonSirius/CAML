package org.chs.camlauncher.backend.system;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** 使用系统文件管理器打开目录，不依赖 JavaFX。 */
public final class DirectoryOpener {
    private static final Logger LOGGER = LogManager.getLogger(DirectoryOpener.class);

    private DirectoryOpener() {
    }

    public static void open(Path directory) throws IOException {
        Path normalized = Objects.requireNonNull(directory, "directory")
                .toAbsolutePath()
                .normalize();
        if (!Files.isDirectory(normalized)) {
            throw new IOException("目录不存在: " + normalized);
        }

        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        List<String> command;
        if (osName.contains("win")) {
            command = List.of("explorer.exe", normalized.toString());
        } else if (osName.contains("mac")) {
            command = List.of("open", normalized.toString());
        } else {
            command = List.of("xdg-open", normalized.toString());
        }

        new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD)
                .start();
        LOGGER.info("已打开实例文件夹: {}", normalized);
    }
}
