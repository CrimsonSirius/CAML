package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

import java.util.Map;

public record Rule(
        @SerializedName("action") Action action,
        @SerializedName("os") Os os,
        @SerializedName("features") Map<String, Boolean> features
) {
    public Rule {
        features = features == null ? Map.of() : Map.copyOf(features);
    }

    public enum Action {
        @SerializedName("allow")
        ALLOW,
        @SerializedName("disallow")
        DISALLOW
    }

    public record Os(
            @SerializedName("name") String name,
            @SerializedName("arch") String arch,
            @SerializedName("version") String version
    ) {
    }
}
