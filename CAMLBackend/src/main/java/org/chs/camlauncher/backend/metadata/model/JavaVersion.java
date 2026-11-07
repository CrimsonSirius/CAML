package org.chs.camlauncher.backend.metadata.model;

import com.google.gson.annotations.SerializedName;

public record JavaVersion(
        @SerializedName("component") String component,
        @SerializedName("majorVersion") Integer majorVersion
) {
}
