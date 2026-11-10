package org.chs.camlauncher.backend.launch;

import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendErrorCarrier;

import java.io.IOException;
import java.util.Objects;
import java.util.Locale;

public final class LaunchException extends IOException implements BackendErrorCarrier {
    private final LaunchFailure failure;
    private final BackendError error;

    public LaunchException(LaunchFailure failure, BackendError error, Throwable cause) {
        super(Objects.requireNonNull(error, "error").code(), cause);
        this.failure = Objects.requireNonNull(failure, "failure");
        this.error = error;
    }

    public LaunchFailure failure() {
        return failure;
    }

    public String userMessage() {
        return getMessage();
    }

    @Override
    public BackendError error() {
        return error;
    }

    public static LaunchException from(IOException exception) {
        String message = exception.getMessage();
        String lower = message == null ? "" : message.toLowerCase(Locale.ROOT);

        if (lower.contains("permission denied")
                || lower.contains("access denied")
                || lower.contains("error=13")) {
            return new LaunchException(
                    LaunchFailure.PERMISSION_DENIED,
                    BackendError.of("launch.permissionDenied"),
                    exception
            );
        }
        if (lower.contains("no such file")
                || lower.contains("cannot find")
                || lower.contains("error=2")) {
            return new LaunchException(
                    LaunchFailure.EXECUTABLE_NOT_FOUND,
                    BackendError.of("launch.executableNotFound"),
                    exception
            );
        }
        if (isCommandTooLong(exception)) {
            return new LaunchException(
                    LaunchFailure.COMMAND_TOO_LONG,
                    BackendError.of("launch.commandTooLong"),
                    exception
            );
        }
        return new LaunchException(
                LaunchFailure.PROCESS_START_FAILED,
                BackendError.of("launch.processStartFailed"),
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
