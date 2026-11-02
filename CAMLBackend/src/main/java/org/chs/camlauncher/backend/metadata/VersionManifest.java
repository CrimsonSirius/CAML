package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Objects;

public record VersionManifest(Latest latest, List<Version> versions) {
    public VersionManifest {
        Objects.requireNonNull(versions, "versions");
        versions = List.copyOf(versions);
    }

    public record Latest(
            @SerializedName("release") String release,
            @SerializedName("snapshot") String snapshot
    ) {
    }

    public record Version(
            @SerializedName("id") String id,
            @SerializedName("type") String type,
            @SerializedName("url") String url,
            @SerializedName("time") String time,
            @SerializedName("releaseTime") String releaseTime,
            @SerializedName("sha1") String sha1,
            @SerializedName("complianceLevel") Integer complianceLevel
    ) {
    }
}
