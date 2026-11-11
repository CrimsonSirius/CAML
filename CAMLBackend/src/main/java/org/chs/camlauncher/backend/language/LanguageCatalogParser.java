package org.chs.camlauncher.backend.language;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** 语言 JSON 到扁平键值表的解析器。 */
final class LanguageCatalogParser {
    private LanguageCatalogParser() {
    }

    static Map<String, String> parse(byte[] content, String source) throws IOException {
        try {
            JsonObject object = JsonParser.parseString(new String(content, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    result.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
            return Map.copyOf(result);
        } catch (RuntimeException e) {
            throw new IOException("无法解析语言文件: " + source, e);
        }
    }
}
