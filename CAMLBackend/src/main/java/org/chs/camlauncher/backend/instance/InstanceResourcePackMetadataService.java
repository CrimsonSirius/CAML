package org.chs.camlauncher.backend.instance;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** 从本地资源包读取 Minecraft 的 pack_format 版本号。 */
public final class InstanceResourcePackMetadataService {
    private static final Logger LOGGER =
            LogManager.getLogger(InstanceResourcePackMetadataService.class);
    private static final String PACK_META = "pack.mcmeta";

    private InstanceResourcePackMetadataService() {
    }

    public static String version(Path resourcePack) {
        try {
            if (Files.isDirectory(resourcePack)) {
                return version(Files.readString(resourcePack.resolve(PACK_META),
                        StandardCharsets.UTF_8));
            }
            try (ZipFile zip = new ZipFile(resourcePack.toFile())) {
                ZipEntry entry = zip.getEntry(PACK_META);
                if (entry == null) {
                    return "";
                }
                try (InputStream input = zip.getInputStream(entry)) {
                    return version(new String(input.readAllBytes(), StandardCharsets.UTF_8));
                }
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.debug("读取资源包版本失败: {}", resourcePack, e);
            return "";
        }
    }

    private static String version(String json) {
        JsonElement root = JsonParser.parseString(json);
        if (!root.isJsonObject()) {
            return "";
        }
        JsonElement pack = root.getAsJsonObject().get("pack");
        if (pack == null || !pack.isJsonObject()) {
            return "";
        }
        JsonObject metadata = pack.getAsJsonObject();
        JsonElement format = metadata.get("pack_format");
        if (format == null || !format.isJsonPrimitive()) {
            return "";
        }
        return format.getAsString().trim();
    }
}
