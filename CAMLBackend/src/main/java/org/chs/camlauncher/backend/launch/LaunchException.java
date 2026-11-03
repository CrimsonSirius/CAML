package org.chs.camlauncher.backend.launch;

import java.io.IOException;
import java.util.Locale;

public final class LaunchException extends IOException {
    private final LaunchFailure failure;

    public LaunchException(LaunchFailure failure, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.failure = failure;
    }

    public LaunchFailure failure() {
        return failure;
    }

    public String userMessage() {
        return getMessage();
    }

    public static LaunchException from(IOException exception) {
        String message = exception.getMessage();
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (lower.contains("permission denied")
                || lower.contains("access denied")
                || lower.contains("error=13")) {
            return new LaunchException(
                    LaunchFailure.PERMISSION_DENIED,
                    "没有权限执行 Java 或访问游戏目录，请检查文件权限。",
                    exception
            );
        }
        if (lower.contains("no such file")
                || lower.contains("cannot find")
                || lower.contains("error=2")) {
            return new LaunchException(
                    LaunchFailure.EXECUTABLE_NOT_FOUND,
                    "找不到 Java 可执行文件，请重新选择 Java。",
                    exception
            );
        }
        if (isCommandTooLong(exception)) {
            return new LaunchException(
                    LaunchFailure.COMMAND_TOO_LONG,
                    "启动命令过长，请缩短游戏路径或减少类路径。",
                    exception
            );
        }
        return new LaunchException(
                LaunchFailure.PROCESS_START_FAILED,
                "启动游戏进程失败，请查看日志了解详情。",
                exception
        );
    }

    public static boolean isCommandTooLong(IOException exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("too long")
                || lower.contains("argument list")
                || lower.contains("error=206");
    }
}
