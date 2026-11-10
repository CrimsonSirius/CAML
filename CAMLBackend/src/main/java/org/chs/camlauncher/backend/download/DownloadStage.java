package org.chs.camlauncher.backend.download;

import java.util.Objects;

/** 下载流程的稳定阶段标识，界面负责按当前语言翻译。 */
public record DownloadStage(String key, String detail) {
    public DownloadStage {
        key = Objects.requireNonNull(key, "key");
        if (key.isBlank()) {
            throw new IllegalArgumentException("stage key must not be blank");
        }
    }

    public static DownloadStage of(String key) {
        return new DownloadStage(key, null);
    }

    public static DownloadStage of(String key, String detail) {
        return new DownloadStage(key, detail);
    }
}
