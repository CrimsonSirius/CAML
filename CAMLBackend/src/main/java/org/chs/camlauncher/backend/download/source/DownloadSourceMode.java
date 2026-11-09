package org.chs.camlauncher.backend.download.source;

/** 下载源模式。 */
public enum DownloadSourceMode {
    MIRROR,
    OFFICIAL,
    CUSTOM;

    public static DownloadSourceMode fromStored(String value) {
        if (value == null) {
            return MIRROR;
        }
        return switch (value.trim().toLowerCase()) {
            case "official" -> OFFICIAL;
            case "custom" -> CUSTOM;
            default -> MIRROR;
        };
    }

    public String storedName() {
        return name().toLowerCase();
    }
}
