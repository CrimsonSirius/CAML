package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record VersionInfo(
        @SerializedName("id") String id,
        @SerializedName("type") String type,
        @SerializedName("inheritsFrom") String inheritsFrom,
        @SerializedName("mainClass") String mainClass,
        @SerializedName("assets") String assets,
        @SerializedName("assetIndex") AssetIndex assetIndex,
        @SerializedName("minecraftArguments") String minecraftArguments,
        @SerializedName("arguments") Arguments arguments,
        @SerializedName("downloads") Map<String, DownloadInfo> downloads,
        @SerializedName("javaVersion") JavaVersion javaVersion,
        @SerializedName("libraries") List<Library> libraries,
        @SerializedName("logging") Map<String, LoggingInfo> logging,
        @SerializedName("minimumLauncherVersion") Integer minimumLauncherVersion,
        @SerializedName("time") String time,
        @SerializedName("releaseTime") String releaseTime,
        @SerializedName("complianceLevel") Integer complianceLevel
) {
    public VersionInfo {
        Objects.requireNonNull(id, "id");
        arguments = arguments == null ? new Arguments(List.of(), List.of()) : arguments;
        downloads = downloads == null ? Map.of() : Map.copyOf(downloads);
        libraries = libraries == null ? List.of() : List.copyOf(libraries);
        logging = logging == null ? Map.of() : Map.copyOf(logging);
    }
}
