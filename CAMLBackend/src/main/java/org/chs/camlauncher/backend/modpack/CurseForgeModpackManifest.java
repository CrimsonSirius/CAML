package org.chs.camlauncher.backend.modpack;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** CurseForge 整合包 ZIP 根目录中的 manifest.json。 */
public record CurseForgeModpackManifest(
        String name,
        String version,
        Map<String, String> dependencies,
        List<File> files,
        String overrides
) {
    public record File(long projectId, long fileId, boolean required) {
    }

    public static CurseForgeModpackManifest parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        String manifestType = string(root, "manifestType");
        if (manifestType != null && !manifestType.isBlank()
                && !"minecraftModpack".equalsIgnoreCase(manifestType)) {
            throw new IllegalArgumentException("unsupported manifestType: " + manifestType);
        }

        JsonObject minecraft = object(root, "minecraft");
        if (minecraft == null) {
            throw new IllegalArgumentException("minecraft section is missing");
        }
        String minecraftVersion = string(minecraft, "version");
        if (minecraftVersion == null || minecraftVersion.isBlank()) {
            throw new IllegalArgumentException("minecraft version is missing");
        }

        Map<String, String> dependencies = new LinkedHashMap<>();
        dependencies.put("minecraft", minecraftVersion);
        String loaderId = primaryLoaderId(minecraft);
        if (loaderId != null && !loaderId.isBlank()) {
            putLoader(dependencies, loaderId);
        }

        List<File> files = new ArrayList<>();
        JsonArray fileArray = root.getAsJsonArray("files");
        if (fileArray != null) {
            for (JsonElement element : fileArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject file = element.getAsJsonObject();
                files.add(new File(
                        longValue(file, "projectID"),
                        longValue(file, "fileID"),
                        !file.has("required") || file.get("required").getAsBoolean()
                ));
            }
        }

        return new CurseForgeModpackManifest(
                string(root, "name"),
                string(root, "version"),
                Map.copyOf(dependencies),
                List.copyOf(files),
                normalizeOverrides(string(root, "overrides"))
        );
    }

    private static String primaryLoaderId(JsonObject minecraft) {
        JsonArray loaders = minecraft.getAsJsonArray("modLoaders");
        if (loaders == null) {
            return null;
        }
        String first = null;
        for (JsonElement element : loaders) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject loader = element.getAsJsonObject();
            String id = string(loader, "id");
            if (id == null || id.isBlank()) {
                continue;
            }
            if (first == null) {
                first = id;
            }
            if (loader.has("primary") && loader.get("primary").getAsBoolean()) {
                return id;
            }
        }
        return first;
    }

    private static void putLoader(Map<String, String> dependencies, String loaderId) {
        int separator = loaderId.indexOf('-');
        if (separator <= 0 || separator == loaderId.length() - 1) {
            dependencies.put(loaderId, "");
            return;
        }
        String type = loaderId.substring(0, separator);
        String version = loaderId.substring(separator + 1);
        String key = switch (type) {
            case "fabric", "fabric-loader" -> "fabric-loader";
            case "quilt", "quilt-loader" -> "quilt-loader";
            case "neoforge", "neo-forge" -> "neoforge";
            default -> type;
        };
        dependencies.put(key, version);
    }

    private static String normalizeOverrides(String value) {
        String result = value == null ? "" : value.trim().replace('\\', '/');
        while (result.startsWith("/")) {
            result = result.substring(1);
        }
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        if (result.isBlank()) {
            return "overrides";
        }
        for (String segment : result.split("/")) {
            if (segment.isBlank() || ".".equals(segment) || "..".equals(segment)) {
                throw new IllegalArgumentException("invalid overrides path: " + value);
            }
        }
        return result;
    }

    private static JsonObject object(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static String string(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }

    private static long longValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || !value.isJsonPrimitive() ? 0 : value.getAsLong();
    }
}
