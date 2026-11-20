package org.chs.camlauncher.backend.mod.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.mod.model.ModDependency;
import org.chs.camlauncher.backend.mod.model.ModDependencyType;
import org.chs.camlauncher.backend.mod.model.ModContentType;
import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModLoader;
import org.chs.camlauncher.backend.mod.model.ModProjectDetail;
import org.chs.camlauncher.backend.mod.model.ModProjectSummary;
import org.chs.camlauncher.backend.mod.model.ModSearchPage;
import org.chs.camlauncher.backend.mod.model.ModSearchQuery;
import org.chs.camlauncher.backend.mod.model.ModSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/** CurseForge 官方 v1 API；需要 API Key。 */
public final class CurseForgeClient {
    private static final URI DEFAULT_BASE = URI.create("https://api.curseforge.com/v1/");
    private static final int MINECRAFT_GAME_ID = 432;
    private static final int FILE_PAGE_SIZE = 50;

    private static final String BUNDLED_KEY_RESOURCE = "/caml.properties";
    /** 构建期写入的编码 Key，见 CAMLBackend/build.gradle.kts 的 generateBuildProperties。 */
    static final String ENCODED_KEY_PROPERTY = "caml.curseforge.apikey.enc";
    /** 兼容手工把明文 Key 写进 caml.properties 的情况。 */
    private static final String PLAIN_KEY_PROPERTY = "caml.curseforge.apikey";
    /**
     * 解码内置 Key 的固定盐，必须与 CAMLBackend/build.gradle.kts 中的编码保持一致。
     *
     * <p>构建期按该盐对明文做 XOR，再做 Base64；它只用于避免发布包里出现可以被 grep
     * 直接命中的明文，不构成安全边界。
     */
    private static final byte[] BUNDLED_KEY_SALT =
            "caml-launcher/curseforge/2026".getBytes(StandardCharsets.UTF_8);

    private final URI base;
    private final ModApiHttpClient http;
    private final String apiKey;

    public CurseForgeClient() {
        this(defaultClient(), DEFAULT_BASE, configuredApiKey());
    }

    public CurseForgeClient(HttpClient httpClient, URI base, String apiKey) {
        this.base = base.toString().endsWith("/") ? base : URI.create(base + "/");
        this.http = new ModApiHttpClient(httpClient);
        this.apiKey = apiKey == null ? "" : apiKey.trim();
    }

    public boolean configured() {
        return !apiKey.isBlank();
    }

    /** CurseForge 文件 CDN 请求也需要 API Key。 */
    public Map<String, String> downloadHeaders() {
        return apiKey.isBlank() ? Map.of() : Map.of("x-api-key", apiKey);
    }

    public ModSearchPage search(ModSearchQuery query) throws IOException, InterruptedException {
        requireApiKey();
        int limit = Math.max(1, Math.min(50, query.pageSize()));
        int index = Math.max(0, query.pageIndex()) * limit;
        StringBuilder uri = new StringBuilder(base.resolve("mods/search").toString())
                .append("?gameId=").append(MINECRAFT_GAME_ID)
                .append("&classId=").append(query.type().curseForgeClassId())
                .append("&index=").append(index)
                .append("&pageSize=").append(limit)
                .append("&sortField=2&sortOrder=desc");
        if (query.text() != null && !query.text().isBlank()) {
            uri.append("&searchFilter=").append(encode(query.text().trim()));
        }
        if (query.gameVersion() != null && !query.gameVersion().isBlank()) {
            uri.append("&gameVersion=").append(encode(query.gameVersion().trim()));
        }
        if (query.type() == ModContentType.MOD && query.loader() != null) {
            uri.append("&modLoaderType=").append(query.loader().curseForgeType());
        }

        JsonObject root = http.get(URI.create(uri.toString()), headers()).getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        List<ModProjectSummary> items = new ArrayList<>();
        if (data != null) {
            for (JsonElement element : data) {
                if (element.isJsonObject()) {
                    items.add(parseSummary(element.getAsJsonObject()));
                }
            }
        }
        JsonObject pagination = root.has("pagination") && root.get("pagination").isJsonObject()
                ? root.getAsJsonObject("pagination") : new JsonObject();
        int total = intValue(pagination, "totalCount");
        return new ModSearchPage(List.copyOf(items), query.pageIndex(), limit, total,
                index + items.size() < total);
    }

    public ModProjectDetail project(String id) throws IOException, InterruptedException {
        requireApiKey();
        JsonObject root = http.get(base.resolve("mods/" + encodePath(id)), headers())
                .getAsJsonObject();
        JsonObject project = root.getAsJsonObject("data");
        if (project == null) {
            throw new BackendException(BackendError.of("mod.error.projectNotFound", id));
        }
        ModProjectSummary summary = parseSummary(project);
        List<JsonObject> fileObjects = fetchAllFiles(id);
        Set<Long> dependencyIds = new LinkedHashSet<>();
        for (JsonObject file : fileObjects) {
            JsonArray dependencies = file.getAsJsonArray("dependencies");
            if (dependencies == null) {
                continue;
            }
            for (JsonElement element : dependencies) {
                if (element.isJsonObject()) {
                    long modId = longValue(element.getAsJsonObject(), "modId");
                    if (modId > 0) {
                        dependencyIds.add(modId);
                    }
                }
            }
        }
        Map<Long, String> dependencyNames = fetchModNames(dependencyIds);
        List<ModFile> files = new ArrayList<>();
        for (JsonObject file : fileObjects) {
            files.add(parseFile(file, dependencyNames));
        }
        return new ModProjectDetail(summary, string(project, "summary"), List.copyOf(files));
    }

    /** 批量查询 CurseForge 指纹对应的工程 ID。 */
    public Map<Long, String> fingerprintModIds(List<Long> fingerprints)
            throws IOException, InterruptedException {
        requireApiKey();
        if (fingerprints == null || fingerprints.isEmpty()) {
            return Map.of();
        }
        JsonObject body = new JsonObject();
        JsonArray values = new JsonArray();
        fingerprints.stream().filter(value -> value != null && value > 0).distinct().forEach(values::add);
        if (values.isEmpty()) {
            return Map.of();
        }
        body.add("fingerprints", values);
        JsonObject root = http.post(base.resolve("fingerprints/432"), body.toString(), headers())
                .getAsJsonObject();
        JsonObject data = root.has("data") && root.get("data").isJsonObject()
                ? root.getAsJsonObject("data") : new JsonObject();
        JsonArray matches = data.getAsJsonArray("exactMatches");
        Map<Long, String> result = new java.util.LinkedHashMap<>();
        if (matches != null) {
            for (JsonElement element : matches) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject project = element.getAsJsonObject();
                long modId = longValue(project, "id");
                JsonObject file = project.has("file") && project.get("file").isJsonObject()
                        ? project.getAsJsonObject("file") : new JsonObject();
                long fingerprint = longValue(file, "fileFingerprint");
                if (modId > 0 && fingerprint > 0) {
                    result.put(fingerprint, Long.toString(modId));
                }
            }
        }
        return Map.copyOf(result);
    }

    /** 按指纹对应的工程查询当前 MC 版本/加载器的最新文件。 */
    public Map<Long, ModFile> latestVersions(
            Map<Long, String> fingerprintToModId, String gameVersion, ModLoader loader)
            throws IOException, InterruptedException {
        requireApiKey();
        if (fingerprintToModId == null || fingerprintToModId.isEmpty()) {
            return Map.of();
        }

        JsonArray modIds = new JsonArray();
        fingerprintToModId.values().stream().distinct().forEach(modIds::add);
        JsonObject modsBody = new JsonObject();
        modsBody.add("modIds", modIds);
        JsonObject modsRoot = http.post(base.resolve("mods"), modsBody.toString(), headers())
                .getAsJsonObject();
        JsonArray projects = modsRoot.getAsJsonArray("data");
        Map<String, Long> fileByMod = new java.util.LinkedHashMap<>();
        if (projects != null) {
            for (JsonElement element : projects) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject project = element.getAsJsonObject();
                String modId = string(project, "id");
                JsonArray indexes = project.getAsJsonArray("latestFilesIndexes");
                if (modId == null || indexes == null) {
                    continue;
                }
                long bestFileId = 0;
                for (JsonElement indexElement : indexes) {
                    if (!indexElement.isJsonObject()) {
                        continue;
                    }
                    JsonObject index = indexElement.getAsJsonObject();
                    if (!gameVersion.equals(string(index, "gameVersion"))) {
                        continue;
                    }
                    int modLoader = intValue(index, "modLoader");
                    if (loader != null && modLoader != 0
                            && modLoader != loader.curseForgeType()) {
                        continue;
                    }
                    bestFileId = Math.max(bestFileId, longValue(index, "fileId"));
                }
                if (bestFileId > 0) {
                    fileByMod.put(modId, bestFileId);
                }
            }
        }
        if (fileByMod.isEmpty()) {
            return Map.of();
        }

        JsonArray fileIds = new JsonArray();
        fileByMod.values().forEach(fileIds::add);
        JsonObject filesBody = new JsonObject();
        filesBody.add("fileIds", fileIds);
        JsonObject filesRoot = http.post(base.resolve("mods/files"), filesBody.toString(), headers())
                .getAsJsonObject();
        JsonArray files = filesRoot.getAsJsonArray("data");
        Map<Long, ModFile> fileById = new java.util.LinkedHashMap<>();
        if (files != null) {
            for (JsonElement element : files) {
                if (element.isJsonObject()) {
                    JsonObject file = element.getAsJsonObject();
                    fileById.put(longValue(file, "id"), parseFile(file, Map.of(), false));
                }
            }
        }

        Map<Long, ModFile> result = new java.util.LinkedHashMap<>();
        for (Map.Entry<Long, String> entry : fingerprintToModId.entrySet()) {
            Long fileId = fileByMod.get(entry.getValue());
            ModFile file = fileId == null ? null : fileById.get(fileId);
            if (file != null) {
                result.put(entry.getKey(), file);
            }
        }
        return Map.copyOf(result);
    }

    private List<JsonObject> fetchAllFiles(String projectId)
            throws IOException, InterruptedException {
        List<JsonObject> result = new ArrayList<>();
        Set<Long> seenFileIds = new LinkedHashSet<>();
        int index = 0;
        int total;
        do {
            URI uri = URI.create(base.resolve("mods/" + encodePath(projectId) + "/files")
                    + "?index=" + index + "&pageSize=" + FILE_PAGE_SIZE);
            JsonObject root = http.get(uri, headers()).getAsJsonObject();
            JsonArray data = root.getAsJsonArray("data");
            if (data != null) {
                for (JsonElement element : data) {
                    if (element.isJsonObject()) {
                        JsonObject file = element.getAsJsonObject();
                        long fileId = longValue(file, "id");
                        if (fileId <= 0 || seenFileIds.add(fileId)) {
                            result.add(file);
                        }
                    }
                }
            }
            JsonObject pagination = root.has("pagination") && root.get("pagination").isJsonObject()
                    ? root.getAsJsonObject("pagination") : new JsonObject();
            total = intValue(pagination, "totalCount");
            int returned = data == null ? 0 : data.size();
            if (returned == 0) {
                break;
            }
            index += returned;
        } while (index < total);
        return result;
    }

    private Map<Long, String> fetchModNames(Set<Long> modIds)
            throws IOException, InterruptedException {
        if (modIds.isEmpty()) {
            return Map.of();
        }
        JsonObject body = new JsonObject();
        JsonArray ids = new JsonArray();
        modIds.forEach(ids::add);
        body.add("modIds", ids);
        JsonObject root = http.post(base.resolve("mods"), body.toString(), headers())
                .getAsJsonObject();
        JsonArray data = root.getAsJsonArray("data");
        Map<Long, String> result = new java.util.LinkedHashMap<>();
        if (data != null) {
            for (JsonElement element : data) {
                if (element.isJsonObject()) {
                    JsonObject mod = element.getAsJsonObject();
                    result.put(mod.get("id").getAsLong(), string(mod, "name"));
                }
            }
        }
        return result;
    }

    private ModProjectSummary parseSummary(JsonObject object) {
        String slug = string(object, "slug");
        JsonObject links = object.has("links") && object.get("links").isJsonObject()
                ? object.getAsJsonObject("links") : new JsonObject();
        String pageUrl = string(links, "websiteUrl");
        if (pageUrl == null || pageUrl.isBlank()) {
            pageUrl = "https://www.curseforge.com/minecraft/mc-mods/" + slug;
        }
        return new ModProjectSummary(
                ModSource.CURSEFORGE,
                string(object, "id"),
                slug,
                string(object, "name"),
                string(object, "summary"),
                firstAuthor(object),
                logoUrl(object),
                longValue(object, "downloadCount"),
                string(object, "dateModified"),
                categories(object),
                latestGameVersions(object),
                latestLoaders(object),
                pageUrl
        );
    }

    private ModFile parseFile(JsonObject object, Map<Long, String> dependencyNames) {
        return parseFile(object, dependencyNames, true);
    }

    private ModFile parseFile(JsonObject object, Map<Long, String> dependencyNames,
                              boolean resolveDependencies) {
        String fileName = string(object, "fileName");
        long fileId = longValue(object, "id");
        List<URI> downloadUris = downloadUris(fileId, fileName, string(object, "downloadUrl"));
        List<String> gameVersions = stringArray(object, "gameVersions");
        List<String> loaders = new ArrayList<>();
        List<String> versions = new ArrayList<>();
        for (String value : gameVersions) {
            ModLoader loader = ModLoader.fromApiName(value);
            if (loader == null) {
                versions.add(value);
            } else {
                loaders.add(loader.apiName());
            }
        }
        long size = longValue(object, "fileLength");
        if (size <= 0) {
            size = -1;
        }
        return new ModFile(
                ModSource.CURSEFORGE,
                string(object, "modId"),
                string(object, "id"),
                string(object, "displayName"),
                string(object, "displayName"),
                fileName,
                downloadUris,
                hash(object, 1),
                hash(object, 2),
                size,
                List.copyOf(versions),
                List.copyOf(loaders),
                resolveDependencies ? dependencies(object, dependencyNames) : List.of(),
                string(object, "fileDate"),
                longValue(object, "downloadCount")
        );
    }

    private static List<URI> downloadUris(long fileId, String fileName, String downloadUrl) {
        if (fileName == null || fileName.isBlank()) {
            return List.of();
        }
        String encodedName = encodePathSegment(fileName);
        Set<String> urls = new LinkedHashSet<>();
        if (downloadUrl != null && !downloadUrl.isBlank()) {
            addDownloadUrlVariants(downloadUrl.replace(fileName, encodedName), urls);
        } else if (fileId > 0) {
            addDownloadUrlVariants("https://edge.forgecdn.net/files/"
                    + (fileId / 1000) + "/" + (fileId % 1000) + "/" + encodedName, urls);
        }

        List<URI> result = new ArrayList<>();
        for (String candidate : urls) {
            try {
                result.add(URI.create(candidate));
            } catch (IllegalArgumentException ignored) {
                // 跳过无法解析的历史地址，继续尝试其它 CDN 变体。
            }
        }
        return List.copyOf(result);
    }

    private static void addDownloadUrlVariants(String url, Set<String> urls) {
        String overwolfNormalized = url.replace("-service.overwolf.wtf", ".forgecdn.net");
        for (String base : List.of(url, overwolfNormalized)) {
            urls.add(base);
            urls.add(base.replace("://edge.", "://mediafilez."));
            urls.add(base.replace("://media.", "://mediafilez."));
            urls.add(base.replace("://media.", "://edge."));
            urls.add(base.replace("://mediafilez.", "://edge."));
        }
    }

    private List<ModDependency> dependencies(JsonObject file, Map<Long, String> names) {
        JsonArray array = file.getAsJsonArray("dependencies");
        if (array == null) {
            return List.of();
        }
        List<ModDependency> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject dependency = element.getAsJsonObject();
            long modId = longValue(dependency, "modId");
            if (modId <= 0) {
                continue;
            }
            result.add(new ModDependency(
                    ModSource.CURSEFORGE,
                    Long.toString(modId),
                    null,
                    names.getOrDefault(modId, Long.toString(modId)),
                    relationType(intValue(dependency, "relationType"))
            ));
        }
        return List.copyOf(result);
    }

    private Map<String, String> headers() {
        return Map.of("x-api-key", apiKey);
    }

    private void requireApiKey() throws IOException {
        if (apiKey.isBlank()) {
            throw new BackendException(BackendError.of("mod.error.curseforgeKeyMissing"));
        }
    }

    private static String configuredApiKey() {
        String property = System.getProperty("caml.curseforge.apiKey");
        if (property != null && !property.isBlank()) {
            return property.trim();
        }
        String environment = System.getenv("CURSEFORGE_API_KEY");
        if (environment != null && !environment.isBlank()) {
            return environment.trim();
        }
        return bundledApiKey();
    }

    /** 读取构建时由 CI 写入 jar 的 Key；不记录、不输出。 */
    private static String bundledApiKey() {
        InputStream input = CurseForgeClient.class.getResourceAsStream(BUNDLED_KEY_RESOURCE);
        if (input == null) {
            return "";
        }
        try (InputStream stream = input;
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Properties properties = new Properties();
            properties.load(reader);
            String encoded = properties.getProperty(ENCODED_KEY_PROPERTY, "").trim();
            if (!encoded.isBlank()) {
                return decodeBundledKey(encoded);
            }
            String plain = properties.getProperty(PLAIN_KEY_PROPERTY, "").trim();
            if (!plain.isBlank()) {
                return plain;
            }
        } catch (IOException e) {
            return "";
        }
        return "";
    }

    /** Base64 → 按固定盐 XOR，还原构建期写入的 Key。 */
    private static String decodeBundledKey(String encoded) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            byte[] plain = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                plain[i] = (byte) (bytes[i] ^ BUNDLED_KEY_SALT[i % BUNDLED_KEY_SALT.length]);
            }
            return new String(plain, StandardCharsets.UTF_8).trim();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static String firstAuthor(JsonObject object) {
        JsonArray authors = object.getAsJsonArray("authors");
        if (authors != null && !authors.isEmpty() && authors.get(0).isJsonObject()) {
            return string(authors.get(0).getAsJsonObject(), "name");
        }
        return "";
    }

    private static String logoUrl(JsonObject object) {
        JsonObject logo = object.has("logo") && object.get("logo").isJsonObject()
                ? object.getAsJsonObject("logo") : new JsonObject();
        return string(logo, "thumbnailUrl", "url");
    }

    private static List<String> categories(JsonObject object) {
        JsonArray array = object.getAsJsonArray("categories");
        if (array == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                String name = string(element.getAsJsonObject(), "name");
                if (name != null) {
                    result.add(name);
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<String> latestGameVersions(JsonObject object) {
        JsonArray array = object.getAsJsonArray("latestFilesIndexes");
        if (array == null) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                String version = string(element.getAsJsonObject(), "gameVersion");
                if (version != null) {
                    result.add(version);
                }
            }
        }
        return List.copyOf(result);
    }

    private static List<String> latestLoaders(JsonObject object) {
        JsonArray array = object.getAsJsonArray("latestFilesIndexes");
        if (array == null) {
            return List.of();
        }
        Set<String> result = new LinkedHashSet<>();
        for (JsonElement element : array) {
            if (element.isJsonObject()) {
                int type = intValue(element.getAsJsonObject(), "modLoader");
                for (ModLoader candidate : ModLoader.values()) {
                    if (candidate.curseForgeType() == type) {
                        result.add(candidate.apiName());
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    private static String hash(JsonObject file, int algorithm) {
        JsonArray hashes = file.getAsJsonArray("hashes");
        if (hashes == null) {
            return null;
        }
        for (JsonElement element : hashes) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject hash = element.getAsJsonObject();
            if (intValue(hash, "algo") == algorithm) {
                return string(hash, "value");
            }
        }
        return null;
    }

    private static ModDependencyType relationType(int value) {
        return switch (value) {
            case 1, 6 -> ModDependencyType.EMBEDDED;
            case 2 -> ModDependencyType.OPTIONAL;
            case 3 -> ModDependencyType.REQUIRED;
            case 5 -> ModDependencyType.INCOMPATIBLE;
            default -> ModDependencyType.UNKNOWN;
        };
    }

    private static List<String> stringArray(JsonObject object, String key) {
        JsonElement value = object.get(key);
        if (value == null || !value.isJsonArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (JsonElement element : value.getAsJsonArray()) {
            if (element.isJsonPrimitive()) {
                result.add(element.getAsString());
            }
        }
        return List.copyOf(result);
    }

    private static String string(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement value = object.get(key);
            if (value != null && value.isJsonPrimitive()) {
                return value.getAsString();
            }
        }
        return null;
    }

    private static long longValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || !value.isJsonPrimitive() ? 0 : value.getAsLong();
    }

    private static int intValue(JsonObject object, String key) {
        JsonElement value = object.get(key);
        return value == null || !value.isJsonPrimitive() ? 0 : value.getAsInt();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodePath(String value) {
        return encode(Objects.requireNonNull(value, "value"));
    }

    private static String encodePathSegment(String value) {
        return encode(value);
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();
    }
}
