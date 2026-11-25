package org.chs.camlauncher.backend.modpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Modrinth .mrpack 中的 modrinth.index.json。 */
public record ModrinthModpackIndex(
        String name,
        String versionId,
        String summary,
        Map<String, String> dependencies,
        List<File> files
) {
    public record File(
            String path,
            List<URI> downloads,
            String sha1,
            long size,
            String clientEnvironment
    ) {
        public boolean clientUnsupported() {
            return "unsupported".equalsIgnoreCase(clientEnvironment);
        }
    }

    public static ModrinthModpackIndex parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, String> dependencies = new LinkedHashMap<>();
        if (root.has("dependencies") && root.get("dependencies").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry
                    : root.getAsJsonObject("dependencies").entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    dependencies.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        }

        List<File> files = new ArrayList<>();
        JsonArray fileArray = root.has("files") && root.get("files").isJsonArray()
                ? root.getAsJsonArray("files") : new JsonArray();
        for (JsonElement element : fileArray) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject file = element.getAsJsonObject();
            String path = string(file, "path");
            if (path == null || path.isBlank()) {
                throw new IllegalArgumentException("modpack file path is missing");
            }
            List<URI> downloads = new ArrayList<>();
            if (file.has("downloads") && file.get("downloads").isJsonArray()) {
                for (JsonElement download : file.getAsJsonArray("downloads")) {
                    if (download.isJsonPrimitive()) {
                        downloads.add(URI.create(download.getAsString()));
                    }
                }
            }
            String sha1 = null;
            if (file.has("hashes") && file.get("hashes").isJsonObject()) {
                sha1 = string(file.getAsJsonObject("hashes"), "sha1");
            }
            long size = file.has("fileSize") && file.get("fileSize").isJsonPrimitive()
                    ? file.get("fileSize").getAsLong() : 0L;
            String clientEnvironment = null;
            if (file.has("env") && file.get("env").isJsonObject()) {
                clientEnvironment = string(file.getAsJsonObject("env"), "client");
            }
            files.add(new File(path, List.copyOf(downloads), sha1, size, clientEnvironment));
        }

        return new ModrinthModpackIndex(
                string(root, "name"),
                string(root, "versionId"),
                string(root, "summary"),
                Map.copyOf(dependencies),
                List.copyOf(files)
        );
    }

    private static String string(JsonObject object, String key) {
        return object.has(key) && object.get(key).isJsonPrimitive()
                ? object.get(key).getAsString()
                : null;
    }
}
