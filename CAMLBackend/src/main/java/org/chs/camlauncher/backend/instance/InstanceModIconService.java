package org.chs.camlauncher.backend.instance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.io.SafePaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 从本地 mod jar 读取图标，不把 jar 解压到磁盘。 */
public final class InstanceModIconService {
    private static final Logger LOGGER = LogManager.getLogger(InstanceModIconService.class);
    private static final int MAX_ICON_BYTES = 2 * 1024 * 1024;
    private static final String[] COMMON_ICON_NAMES = {
            "icon.png", "logo.png", "pack.png", "icon.jpg", "logo.jpg"
    };

    private InstanceModIconService() {
    }

    public static byte[] load(Path gameDirectory, String instanceId, String fileName,
                              VersionIsolationMode isolationMode) {
        try {
            Path modsDirectory = InstanceInspector.dataDirectory(
                    gameDirectory, instanceId, isolationMode)
                    .resolve("mods");
            String baseName = fileName != null && fileName.endsWith(".disabled")
                    ? fileName.substring(0, fileName.length() - ".disabled".length())
                    : fileName;
            Path modFile = SafePaths.resolveChild(modsDirectory, baseName, "mod file");
            if (!java.nio.file.Files.isRegularFile(modFile)) {
                modFile = modsDirectory.resolve(baseName + ".disabled");
            }
            if (!java.nio.file.Files.isRegularFile(modFile)) {
                return new byte[0];
            }
            try (ZipFile zip = new ZipFile(modFile.toFile())) {
                String iconPath = findIconPath(zip);
                if (iconPath == null) {
                    return new byte[0];
                }
                ZipEntry entry = zip.getEntry(iconPath);
                if (entry == null) {
                    return new byte[0];
                }
                try (InputStream input = zip.getInputStream(entry)) {
                    byte[] bytes = input.readNBytes(MAX_ICON_BYTES + 1);
                    if (bytes.length == 0 || bytes.length > MAX_ICON_BYTES) {
                        return new byte[0];
                    }
                    return bytes;
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("读取本地 Mod 图标失败: {}", fileName, e);
            return new byte[0];
        }
    }

    private static String findIconPath(ZipFile zip) {
        String fabric = readIconFromJson(zip, "fabric.mod.json", root -> root);
        if (fabric != null) {
            return fabric;
        }
        String quilt = readIconFromJson(zip, "quilt.mod.json", root -> {
            JsonObject loader = object(root, "quilt_loader");
            return loader == null ? null : object(loader, "metadata");
        });
        if (quilt != null) {
            return quilt;
        }

        String forge = readLogoFileFromToml(zip, "META-INF/mods.toml");
        if (forge == null) {
            forge = readLogoFileFromToml(zip, "META-INF/neoforge.mods.toml");
        }
        if (forge != null) {
            return forge;
        }

        String legacy = readLegacyMcmodInfo(zip);
        if (legacy != null) {
            return legacy;
        }
        return findCommonIcon(zip);
    }

    private static String readIconFromJson(
            ZipFile zip, String metadataPath, JsonNodeSelector selector) {
        ZipEntry entry = zip.getEntry(metadataPath);
        if (entry == null) {
            return null;
        }
        try (InputStream input = zip.getInputStream(entry)) {
            JsonElement root = JsonParser.parseString(
                    new String(input.readAllBytes(), StandardCharsets.UTF_8));
            if (!root.isJsonObject()) {
                return null;
            }
            JsonObject target = selector.select(root.getAsJsonObject());
            return target == null ? null : iconValue(target.get("icon"));
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("解析 Mod 图标元数据失败: {}", metadataPath, e);
            return null;
        }
    }

    private static String readLogoFileFromToml(ZipFile zip, String path) {
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            return null;
        }
        try (InputStream input = zip.getInputStream(entry)) {
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var matcher = java.util.regex.Pattern
                    .compile("(?m)^\\s*logoFile\\s*=\\s*[\"']([^\"']+)[\"']")
                    .matcher(text);
            return matcher.find() ? normalizeIconPath(matcher.group(1)) : null;
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("解析 Mod logoFile 失败: {}", path, e);
            return null;
        }
    }

    private static String readLegacyMcmodInfo(ZipFile zip) {
        ZipEntry entry = zip.getEntry("mcmod.info");
        if (entry == null) {
            return null;
        }
        try (InputStream input = zip.getInputStream(entry)) {
            JsonElement root = JsonParser.parseString(
                    new String(input.readAllBytes(), StandardCharsets.UTF_8));
            if (!root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
                return null;
            }
            JsonElement first = root.getAsJsonArray().get(0);
            if (!first.isJsonObject()) {
                return null;
            }
            JsonElement logo = first.getAsJsonObject().get("logoFile");
            return logo == null || !logo.isJsonPrimitive() ? null : normalizeIconPath(logo.getAsString());
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("解析 mcmod.info 图标失败", e);
            return null;
        }
    }

    private static String findCommonIcon(ZipFile zip) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                continue;
            }
            String path = entry.getName().toLowerCase(Locale.ROOT);
            for (String name : COMMON_ICON_NAMES) {
                if (path.equals(name) || path.endsWith("/" + name)) {
                    return entry.getName();
                }
            }
        }
        return null;
    }

    private static String iconValue(JsonElement icon) {
        if (icon == null) {
            return null;
        }
        if (icon.isJsonPrimitive()) {
            return normalizeIconPath(icon.getAsString());
        }
        if (!icon.isJsonObject()) {
            return null;
        }
        return icon.getAsJsonObject().entrySet().stream()
                .filter(entry -> entry.getValue().isJsonPrimitive())
                .sorted(Comparator.comparingInt(entry -> sizeKey(entry.getKey())))
                .reduce((first, second) -> second)
                .map(Map.Entry::getValue)
                .map(JsonElement::getAsString)
                .map(InstanceModIconService::normalizeIconPath)
                .orElse(null);
    }

    private static int sizeKey(String key) {
        try {
            return Integer.parseInt(key);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static JsonObject object(JsonObject parent, String key) {
        JsonElement value = parent.get(key);
        return value != null && value.isJsonObject() ? value.getAsJsonObject() : null;
    }

    private static String normalizeIconPath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim().replace('\\', '/');
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized.isBlank() ? null : normalized;
    }

    @FunctionalInterface
    private interface JsonNodeSelector {
        JsonObject select(JsonObject root);
    }
}
