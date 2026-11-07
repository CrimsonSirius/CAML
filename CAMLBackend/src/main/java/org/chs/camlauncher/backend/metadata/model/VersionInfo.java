package org.chs.camlauncher.backend.metadata.model;

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
        @SerializedName("complianceLevel") Integer complianceLevel,
        @SerializedName("jar") String jar
) {
    public VersionInfo {
        Objects.requireNonNull(id, "id");
        arguments = arguments == null ? new Arguments(List.of(), List.of()) : arguments;
        downloads = downloads == null ? Map.of() : Map.copyOf(downloads);
        libraries = libraries == null ? List.of() : List.copyOf(libraries);
        logging = logging == null ? Map.of() : Map.copyOf(logging);
    }

    /**
     * 复制一份使用指定 id 的版本信息。
     *
     * <p>本地安装目录名就是启动时使用的版本 id。部分第三方启动器或整合包会保留
     * JSON 内部的原版 id 而重命名目录，因此需要把 id 统一为目录名，保证
     * {@code versions/<id>/<id>.jar} 等路径一致。</p>
     *
     * @param newId 新的版本 id，通常取自版本目录名
     * @return 替换 id 后的新实例；id 相同时直接返回自身
     */
    public VersionInfo withId(String newId) {
        Objects.requireNonNull(newId, "newId");
        if (newId.equals(id)) {
            return this;
        }
        return new VersionInfo(
                newId,
                type,
                inheritsFrom,
                mainClass,
                assets,
                assetIndex,
                minecraftArguments,
                arguments,
                downloads,
                javaVersion,
                libraries,
                logging,
                minimumLauncherVersion,
                time,
                releaseTime,
                complianceLevel,
                jar
        );
    }

    /** @return 客户端 jar 对应的版本 ID；未指定 {@code jar} 时使用当前实例 ID */
    public String clientJarId() {
        return jar == null || jar.isBlank() ? id : jar;
    }
}
