package org.chs.camlauncher.backend.modpack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.chs.camlauncher.backend.mod.model.ModFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/** 整合包实例标记与目录约定。 */
public final class ModpackLayout {
    public static final String MARKER_FILE = ".caml-modpack.json";

    private ModpackLayout() {
    }

    /** 版本目录中存在整合包标记时，启动必须使用完全隔离的游戏目录。 */
    public static boolean isModpack(Path versionDirectory) {
        return versionDirectory != null
                && Files.isRegularFile(versionDirectory.resolve(MARKER_FILE));
    }

    public static void writeMarker(Path instanceDirectory, ModrinthModpackIndex index, ModFile file)
            throws IOException {
        writeMarker(instanceDirectory, "modrinth", file,
                index.name(), index.versionId());
    }

    public static void writeMarker(
            Path instanceDirectory, CurseForgeModpackManifest manifest, ModFile file)
            throws IOException {
        writeMarker(instanceDirectory, "curseforge", file,
                manifest.name(), manifest.version());
    }

    private static void writeMarker(Path instanceDirectory, String source, ModFile file,
                                    String name, String version) throws IOException {
        JsonObject marker = new JsonObject();
        marker.addProperty("source", source);
        marker.addProperty("projectId", file.projectId());
        marker.addProperty("versionId", file.versionId());
        marker.addProperty("name", name == null || name.isBlank() ? file.name() : name);
        marker.addProperty("version", version == null ? "" : version);
        marker.addProperty("file", file.fileName());
        Files.writeString(
                instanceDirectory.resolve(MARKER_FILE),
                new Gson().toJson(marker),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
