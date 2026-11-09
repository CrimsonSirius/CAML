package org.chs.camlauncher.backend.metadata.model;

import com.google.gson.annotations.SerializedName;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public record VersionManifest(Latest latest, List<Version> versions) {
    public VersionManifest {
        Objects.requireNonNull(versions, "versions");
        versions = List.copyOf(versions);
    }

    /** @return 只包含正式版，按发布时间从新到旧排列 */
    public List<Version> releasesNewestFirst() {
        return newestFirst(Set.of("release"));
    }

    /** @return 只包含快照版，按发布时间从新到旧排列 */
    public List<Version> snapshotsNewestFirst() {
        return newestFirst(Set.of("snapshot"));
    }

    /** @return 只包含远古版（Alpha/Beta），按发布时间从新到旧排列 */
    public List<Version> oldVersionsNewestFirst() {
        return newestFirst(Set.of("old_alpha", "old_beta"));
    }

    private List<Version> newestFirst(Set<String> types) {
        return versions.stream()
                .filter(version -> version.type() != null
                        && types.contains(version.type().toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparing(
                        Version::releaseTime,
                        Comparator.nullsLast(Comparator.<String>reverseOrder())
                ))
                .toList();
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
