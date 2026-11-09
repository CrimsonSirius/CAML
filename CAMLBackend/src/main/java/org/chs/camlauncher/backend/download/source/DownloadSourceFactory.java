package org.chs.camlauncher.backend.download.source;

import java.net.URI;
import java.util.List;

/** 根据设置创建按优先级排列的下载源。 */
public final class DownloadSourceFactory {
    private DownloadSourceFactory() {
    }

    public static List<DownloadSource> create(DownloadSourceMode mode, String customApi) {
        return switch (mode) {
            case OFFICIAL -> List.of(OfficialDownloadSource.INSTANCE);
            case MIRROR -> List.of(new MirrorDownloadSource(MirrorDownloadSource.BMCLAPI_ROOT));
            case CUSTOM -> customApi != null && isValidCustomApi(customApi)
                    ? List.of(new MirrorDownloadSource(customApi))
                    : List.of(OfficialDownloadSource.INSTANCE);
        };
    }

    public static boolean isValidCustomApi(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(value.trim());
            return ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
