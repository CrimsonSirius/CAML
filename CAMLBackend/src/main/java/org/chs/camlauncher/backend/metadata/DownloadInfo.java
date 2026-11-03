package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

public record DownloadInfo(
        @SerializedName("id") String id,
        @SerializedName("path") String path,
        @SerializedName("sha1") String sha1,
        @SerializedName("size") Long size,
        @SerializedName("url") String url
) {
    public DownloadInfo(String path, String sha1, Long size, String url) {
        this(null, path, sha1, size, url);
    }
}
