package org.chs.camlauncher.backend.launch;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LaunchExceptionTest {
    @Test
    void classifiesPermissionFailure() {
        LaunchException exception = LaunchException.from(
                new IOException("CreateProcess error=13, Access is denied"));

        assertEquals(LaunchFailure.PERMISSION_DENIED, exception.failure());
        assertFalse(exception.userMessage().isBlank());
    }

    @Test
    void classifiesMissingExecutable() {
        LaunchException exception = LaunchException.from(
                new IOException("Cannot run program \"java\": error=2, No such file or directory"));

        assertEquals(LaunchFailure.EXECUTABLE_NOT_FOUND, exception.failure());
    }

    @Test
    void classifiesTooLongCommand() {
        LaunchException exception = LaunchException.from(
                new IOException("error=7, Argument list too long"));

        assertEquals(LaunchFailure.COMMAND_TOO_LONG, exception.failure());
    }

    @Test
    void fallsBackToGenericProcessFailure() {
        LaunchException exception = LaunchException.from(new IOException("unexpected failure"));

        assertEquals(LaunchFailure.PROCESS_START_FAILED, exception.failure());
    }
}
