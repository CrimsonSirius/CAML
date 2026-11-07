package org.chs.camlauncher.backend.metadata.model;

import com.google.gson.annotations.SerializedName;

public record LoggingInfo(
        @SerializedName("argument") String argument,
        @SerializedName("file") DownloadInfo file,
        @SerializedName("type") String type
) {
}
