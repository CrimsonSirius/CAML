package org.chs.camlauncher.backend.metadata.model;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public record AssetIndexFile(
        @SerializedName("objects") Map<String, AssetObject> objects,
        @SerializedName("virtual") boolean virtual,
        @SerializedName("map_to_resources") boolean mapToResources
) {
    public AssetIndexFile {
        objects = objects == null ? Map.of() : Map.copyOf(objects);
    }

    public boolean isVirtual() {
        return virtual || mapToResources;
    }
}
