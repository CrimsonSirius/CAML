package org.chs.camlauncher.backend.mod.source;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
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
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Modrinth v2 官方 API。 */
public final class ModrinthClient {
    private static final URI DEFAULT_BASE = URI.create("https://api.modrinth.com/v2/");
    private final URI base;
    private final ModApiHttpClient http;

    public ModrinthClient() {
        this(defaultClient(), DEFAULT_BASE);
    }

    public ModrinthClient(HttpClient httpClient, URI base) {
        this.base = base.toString().endsWith("/") ? base : URI.create(base + "/");
        this.http = new ModApiHttpClient(httpClient);
    }

    public ModSearchPage search(ModSearchQuery query) throws IOException, InterruptedException {
        int limit = Math.max(1, Math.min(50, query.pageSize()));
        int offset = Math.max(0, query.pageIndex()) * limit;
        StringBuilder uri = new StringBuilder(base.resolve("search").toString())
                .append("?query=").append(encode(query.text() == null ? "" : query.text()))
                .append("&limit=").append(limit)
                .append("&offset=").append(offset)
                .append("&index=relevance")
                .append("&facets=").append(encode(facets(query)));
        JsonObject root = http.get(URI.create(uri.toString()), Map.of()).getAsJsonObject();
        JsonArray hits = root.getAsJsonArray("hits");
        List<ModProjectSummary> items = new ArrayList<>();
        if (hits != null) {
            for (JsonElement element : hits) {
                if (element.isJsonObject()) {
                    items.add(parseSummary(element.getAsJsonObject()));
                }
            }
        }
        int total = root.has("total_hits") ? root.get("total_hits").getAsInt() : items.size();
        return new ModSearchPage(List.copyOf(items), query.pageIndex(), limit, total,
                offset + items.size() < total);
    }

    public ModProjectDetail project(String idOrSlug) throws IOException, InterruptedException {
        JsonObject root = http.get(base.resolve("project/" + encodePath(idOrSlug)), Map.of())
                .getAsJsonObject();
        ModProjectSummary summary = parseSummary(root);
        JsonArray versions = http.get(
                base.resolve("project/" + encodePath(idOrSlug) + "/version"), Map.of())
                .getAsJsonArray();
        Map<String, String> names = new LinkedHashMap<>();
        List<ModFile> files = new ArrayList<>();
        for (JsonElement element : versions) {
            if (element.isJsonObject()) {
                files.add(parseFile(element.getAsJsonObject(), names));
            }
        }
        String body = string(root, "body");
        return new ModProjectDetail(summary, body, List.copyOf(files));
    }

    /**
     * 批量查询本地文件对应的最新兼容版本。
     *
     * @param hashes     本地 jar 的 SHA-1
     * @param gameVersion 当前实例 MC 版本
     * @param loader      当前实例加载器
     * @return SHA-1 -> 最新兼容版本；不在 Modrinth 的文件不会出现在结果中
     */
    public Map<String, ModFile> latestVersions(
            List<String> hashes, String gameVersion, ModLoader loader)
            throws IOException, InterruptedException {
        if (hashes == null || hashes.isEmpty()) {
            return Map.of();
        }
        JsonObject body = new JsonObject();
        body.addProperty("algorithm", "sha1");
        JsonArray entries = new JsonArray();
        for (String hash : hashes) {
            if (hash == null || hash.isBlank()) {
                continue;
            }
            JsonObject entry = new JsonObject();
            entry.addProperty("hash", hash);
            JsonArray loaders = new JsonArray();
            loaders.add(loader.apiName());
            entry.add("loaders", loaders);
            JsonArray gameVersions = new JsonArray();
            gameVersions.add(gameVersion);
            entry.add("game_versions", gameVersions);
            entries.add(entry);
        }
        if (entries.isEmpty()) {
            return Map.of();
        }
        body.add("hashes", entries);
        JsonObject root = http.post(
                base.resolve("version_files/update_individual"),
                body.toString(),
                Map.of()
        ).getAsJsonObject();
        Map<String, ModFile> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                result.put(entry.getKey(),
                        parseFile(entry.getValue().getAsJsonObject(), Map.of(), false));
            }
        }
        return Map.copyOf(result);
    }

    private ModProjectSummary parseSummary(JsonObject object) {
        String slug = string(object, "slug");
        return new ModProjectSummary(
                ModSource.MODRINTH,
                string(object, "project_id", "id"),
                slug,
                string(object, "title", "name"),
                string(object, "description", "summary"),
                string(object, "author"),
                string(object, "icon_url"),
                longValue(object, "downloads"),
                string(object, "date_modified", "updated"),
                strings(object, "display_categories", "categories"),
                strings(object, "versions", "game_versions"),
                loaders(strings(object, "categories", "loaders")),
                "https://modrinth.com/mod/" + slug
        );
    }

    private ModFile parseFile(JsonObject object, Map<String, String> dependencyNames)
            throws IOException, InterruptedException {
        return parseFile(object, dependencyNames, true);
    }

    private ModFile parseFile(JsonObject object, Map<String, String> dependencyNames,
                              boolean resolveDependencies)
            throws IOException, InterruptedException {
        String projectId = string(object, "project_id");
        List<ModDependency> dependencies = resolveDependencies
                ? parseDependencies(object.getAsJsonArray("dependencies"), dependencyNames)
                : List.of();
        JsonArray fileArray = object.getAsJsonArray("files");
        if (fileArray == null || fileArray.isEmpty()) {
            return new ModFile(ModSource.MODRINTH, projectId, string(object, "id"),
                    string(object, "name"), string(object, "version_number"), "", List.of(),
                    null, null, -1,
                    strings(object, "game_versions"),
                    loaders(strings(object, "loaders")), dependencies,
                    string(object, "date_published"), longValue(object, "downloads"));
        }
        JsonObject file = primaryFile(fileArray);
        JsonObject hashes = file.has("hashes") && file.get("hashes").isJsonObject()
                ? file.getAsJsonObject("hashes") : new JsonObject();
        URI download = URI.create(string(file, "url"));
        return new ModFile(
                ModSource.MODRINTH,
                projectId,
                string(object, "id"),
                string(object, "name"),
                string(object, "version_number"),
                string(file, "filename"),
                List.of(download),
                string(hashes, "sha1"),
                string(hashes, "md5"),
                longValue(file, "size"),
                strings(object, "game_versions"),
                loaders(strings(object, "loaders")),
                dependencies,
                string(object, "date_published"),
                longValue(object, "downloads")
        );
    }

    private List<ModDependency> parseDependencies(JsonArray array, Map<String, String> names)
            throws IOException, InterruptedException {
        if (array == null) {
            return List.of();
        }
        List<ModDependency> result = new ArrayList<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject dependency = element.getAsJsonObject();
            String projectId = string(dependency, "project_id");
            String versionId = string(dependency, "version_id");
            if ((projectId == null || projectId.isBlank()) && versionId != null && !versionId.isBlank()) {
                projectId = dependencyProjectId(versionId);
            }
            String name = dependencyName(projectId, names);
            result.add(new ModDependency(
                    ModSource.MODRINTH, projectId, versionId, name,
                    dependencyType(string(dependency, "dependency_type"))));
        }
        return List.copyOf(result);
    }

    private String dependencyProjectId(String versionId) throws IOException, InterruptedException {
        JsonObject version = http.get(base.resolve("version/" + encodePath(versionId)), Map.of())
                .getAsJsonObject();
        return string(version, "project_id");
    }

    private String dependencyName(String projectId, Map<String, String> names)
            throws IOException, InterruptedException {
        if (projectId == null || projectId.isBlank()) {
            return "unknown";
        }
        String cached = names.get(projectId);
        if (cached != null) {
            return cached;
        }
        try {
            JsonObject project = http.get(base.resolve("project/" + encodePath(projectId)), Map.of())
                    .getAsJsonObject();
            String name = string(project, "title", "slug");
            names.put(projectId, name);
            return name;
        } catch (IOException e) {
            names.put(projectId, projectId);
            return projectId;
        }
    }

    private static String facets(ModSearchQuery query) {
        JsonArray groups = new JsonArray();
        groups.add(array("project_type:" + query.type().modrinthProjectType()));
        if (query.gameVersion() != null && !query.gameVersion().isBlank()) {
            groups.add(array("versions:" + query.gameVersion().trim()));
        }
        if (query.type() == ModContentType.MOD && query.loader() != null) {
            groups.add(array("categories:" + query.loader().apiName()));
        }
        return groups.toString();
    }

    private static JsonArray array(String value) {
        JsonArray array = new JsonArray();
        array.add(value);
        return array;
    }

    private static JsonObject primaryFile(JsonArray files) {
        for (JsonElement element : files) {
            if (element.isJsonObject() && element.getAsJsonObject().has("primary")
                    && element.getAsJsonObject().get("primary").getAsBoolean()) {
                return element.getAsJsonObject();
            }
        }
        return files.get(0).getAsJsonObject();
    }

    private static List<String> loaders(List<String> values) {
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            ModLoader loader = ModLoader.fromApiName(value);
            if (loader != null) {
                result.add(loader.apiName());
            }
        }
        return List.copyOf(result);
    }

    private static ModDependencyType dependencyType(String value) {
        if (value == null) {
            return ModDependencyType.UNKNOWN;
        }
        return switch (value) {
            case "required" -> ModDependencyType.REQUIRED;
            case "optional" -> ModDependencyType.OPTIONAL;
            case "incompatible" -> ModDependencyType.INCOMPATIBLE;
            case "embedded" -> ModDependencyType.EMBEDDED;
            default -> ModDependencyType.UNKNOWN;
        };
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

    private static List<String> strings(JsonObject object, String... keys) {
        for (String key : keys) {
            JsonElement value = object.get(key);
            if (value != null && value.isJsonArray()) {
                List<String> result = new ArrayList<>();
                for (JsonElement element : value.getAsJsonArray()) {
                    if (element.isJsonPrimitive()) {
                        result.add(element.getAsString());
                    }
                }
                return List.copyOf(result);
            }
        }
        return List.of();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String encodePath(String value) {
        return encode(Objects.requireNonNull(value, "value"));
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(java.time.Duration.ofSeconds(15))
                .build();
    }
}
