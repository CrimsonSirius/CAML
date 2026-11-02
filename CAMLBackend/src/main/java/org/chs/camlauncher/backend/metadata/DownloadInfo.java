package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

public record DownloadInfo(
        @SerializedName("path") String path,
        @SerializedName("sha1") String sha1,
        @SerializedName("size") Long size,
        @SerializedName("url") String url
) {
}
