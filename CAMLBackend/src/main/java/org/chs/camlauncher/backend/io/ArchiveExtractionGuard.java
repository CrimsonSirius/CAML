package org.chs.camlauncher.backend.io;

import org.chs.camlauncher.backend.download.DownloadCancellationToken;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/** 限制压缩包条目数量、单条目大小和累计解压大小。 */
public final class ArchiveExtractionGuard {
    public static final int DEFAULT_MAX_ENTRIES = 10_000;
    public static final long DEFAULT_MAX_ENTRY_BYTES = 512L * 1024 * 1024;
    public static final long DEFAULT_MAX_TOTAL_BYTES = 2L * 1024 * 1024 * 1024;

    private final int maxEntries;
    private final long maxEntryBytes;
    private final long maxTotalBytes;
    private int entries;
    private long totalBytes;

    public ArchiveExtractionGuard() {
        this(DEFAULT_MAX_ENTRIES, DEFAULT_MAX_ENTRY_BYTES, DEFAULT_MAX_TOTAL_BYTES);
    }

    public ArchiveExtractionGuard(int maxEntries, long maxEntryBytes, long maxTotalBytes) {
        if (maxEntries <= 0 || maxEntryBytes <= 0 || maxTotalBytes <= 0) {
            throw new IllegalArgumentException("压缩包限制必须为正数");
        }
        this.maxEntries = maxEntries;
        this.maxEntryBytes = maxEntryBytes;
        this.maxTotalBytes = maxTotalBytes;
    }

    /** 每个 ZIP 条目（包括目录）开始时调用。 */
    public void beginEntry() throws IOException {
        entries++;
        if (entries > maxEntries) {
            throw new IOException("压缩包条目数量超过限制: " + maxEntries);
        }
    }

    public void copy(InputStream input, OutputStream output) throws IOException {
        try {
            copy(input, output, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("压缩包解压被中断", e);
        }
    }

    public void copy(InputStream input, OutputStream output, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        byte[] buffer = new byte[8192];
        long entryBytes = 0;
        int read;
        while (true) {
            if (token != null) {
                token.throwIfCancelled();
            }
            read = input.read(buffer);
            if (read == -1) {
                return;
            }
            if (token != null) {
                token.throwIfCancelled();
            }
            entryBytes += read;
            totalBytes += read;
            if (entryBytes > maxEntryBytes) {
                throw new IOException("压缩包单条目超过限制: " + maxEntryBytes);
            }
            if (totalBytes > maxTotalBytes) {
                throw new IOException("压缩包解压总量超过限制: " + maxTotalBytes);
            }
            output.write(buffer, 0, read);
        }
    }

    public byte[] readAll(InputStream input) throws IOException {
        try {
            return readAll(input, null);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("压缩包读取被中断", e);
        }
    }

    public byte[] readAll(InputStream input, DownloadCancellationToken token)
            throws IOException, InterruptedException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        copy(input, output, token);
        return output.toByteArray();
    }
}
