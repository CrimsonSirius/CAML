package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

public record JavaVersion(
        @SerializedName("component") String component,
        @SerializedName("majorVersion") Integer majorVersion
) {
}
