package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public record AssetIndexFile(
        @SerializedName("objects") Map<String, AssetObject> objects
) {
    public AssetIndexFile {
        objects = objects == null ? Map.of() : Map.copyOf(objects);
    }
}
