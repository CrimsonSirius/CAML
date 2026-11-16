package org.chs.camlauncher.backend.instance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 从本地 mod jar 读取版本号。 */
public final class InstanceModMetadataService {
    private static final Logger LOGGER = LogManager.getLogger(InstanceModMetadataService.class);
    private static final Pattern TOML_VERSION = Pattern.compile(
            "(?m)^\\s*version\\s*=\\s*[\"']([^\"']+)[\"']");

    private InstanceModMetadataService() {
    }

    public static String version(Path jar) {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            String version = jsonVersion(zip, "fabric.mod.json");
            if (version == null) {
                version = quiltVersion(zip);
            }
            if (version == null) {
                version = tomlVersion(zip, "META-INF/mods.toml");
            }
            if (version == null) {
                version = tomlVersion(zip, "META-INF/neoforge.mods.toml");
            }
            if (version == null) {
                version = mcmodInfoVersion(zip);
            }
            if (version == null) {
                version = manifestVersion(zip);
            }
            return version == null ? "" : version.trim();
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("读取 Mod 版本失败: {}", jar, e);
            return "";
        }
    }

    private static String jsonVersion(ZipFile zip, String path) throws IOException {
        JsonObject object = jsonObject(zip, path);
        return object == null ? null : primitive(object.get("version"));
    }

    private static String quiltVersion(ZipFile zip) throws IOException {
        JsonObject root = jsonObject(zip, "quilt.mod.json");
        if (root == null) {
            return null;
        }
        JsonElement loader = root.get("quilt_loader");
        if (loader == null || !loader.isJsonObject()) {
            return null;
        }
        JsonElement metadata = loader.getAsJsonObject().get("metadata");
        return metadata != null && metadata.isJsonObject()
                ? primitive(metadata.getAsJsonObject().get("version"))
                : null;
    }

    private static String tomlVersion(ZipFile zip, String path) throws IOException {
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            return null;
        }
        try (InputStream input = zip.getInputStream(entry)) {
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            var matcher = TOML_VERSION.matcher(text);
            return matcher.find() ? matcher.group(1) : null;
        }
    }

    private static String mcmodInfoVersion(ZipFile zip) throws IOException {
        JsonElement root = json(zip, "mcmod.info");
        if (root == null || !root.isJsonArray() || root.getAsJsonArray().isEmpty()) {
            return null;
        }
        JsonElement first = root.getAsJsonArray().get(0);
        return first.isJsonObject() ? primitive(first.getAsJsonObject().get("version")) : null;
    }

    private static String manifestVersion(ZipFile zip) throws IOException {
        ZipEntry entry = zip.getEntry("META-INF/MANIFEST.MF");
        if (entry == null) {
            return null;
        }
        try (InputStream input = zip.getInputStream(entry)) {
            return new Manifest(input).getMainAttributes().getValue("Implementation-Version");
        }
    }

    private static JsonObject jsonObject(ZipFile zip, String path) throws IOException {
        JsonElement root = json(zip, path);
        return root != null && root.isJsonObject() ? root.getAsJsonObject() : null;
    }

    private static JsonElement json(ZipFile zip, String path) throws IOException {
        ZipEntry entry = zip.getEntry(path);
        if (entry == null) {
            return null;
        }
        try (InputStream input = zip.getInputStream(entry)) {
            return JsonParser.parseString(new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private static String primitive(JsonElement value) {
        return value != null && value.isJsonPrimitive() ? value.getAsString() : null;
    }
}
