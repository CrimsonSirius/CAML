package org.chs.camlauncher.backend.download;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DownloadCancellationTokenTest {
    @Test
    void cancelIsIdempotentAndBlocksFurtherWork() throws Exception {
        DownloadCancellationToken token = new DownloadCancellationToken();

        assertFalse(token.isCancelled());
        token.cancel();
        token.cancel();

        assertTrue(token.isCancelled());
        assertThrows(InterruptedException.class, token::throwIfCancelled);
    }
}
