package org.chs.camlauncher.backend.download;

/** 下载并发数约束。 */
public final class DownloadConcurrency {
    public static final int MIN = 1;
    public static final int MAX = 256;
    public static final int DEFAULT = 8;

    private DownloadConcurrency() {
    }

    public static int clamp(int value) {
        return Math.max(MIN, Math.min(MAX, value));
    }
}
