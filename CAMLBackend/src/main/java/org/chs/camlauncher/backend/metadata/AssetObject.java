package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

import java.util.Objects;

public record AssetObject(
        @SerializedName("hash") String hash,
        @SerializedName("size") Long size
) {
    public AssetObject {
        Objects.requireNonNull(hash, "hash");
    }
}
