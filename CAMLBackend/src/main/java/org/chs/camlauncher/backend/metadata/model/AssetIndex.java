package org.chs.camlauncher.backend.metadata.model;

import com.google.gson.annotations.SerializedName;

public record AssetIndex(
        @SerializedName("id") String id,
        @SerializedName("sha1") String sha1,
        @SerializedName("size") Long size,
        @SerializedName("totalSize") Long totalSize,
        @SerializedName("url") String url
) {
}
