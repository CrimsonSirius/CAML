package org.chs.camlauncher.backend.loader.quilt;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.annotations.SerializedName;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.SourceAwareHttpClient;
import org.chs.camlauncher.backend.error.BackendErrorCarrier;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Quilt Meta 与 Modrinth Quilted Fabric API 元数据客户端。 */
public final class QuiltService {
    private static final URI DEFAULT_META_BASE = URI.create("https://meta.quiltmc.org/");
    private static final URI DEFAULT_MODRINTH_BASE = URI.create("https://api.modrinth.com/");
    /** Modrinth 上 Quilted Fabric API 与 QSL 共用同一项目，slug 为 `qsl`。 */
    private static final String API_PROJECT = "qsl";

    private final SourceAwareHttpClient httpClient;
    private final Gson gson;
    private final URI metaBase;
    private final URI modrinthBase;

    public QuiltService() {
        this(defaultClient(), new Gson(), DEFAULT_META_BASE, DEFAULT_MODRINTH_BASE);
    }

    public QuiltService(HttpClient httpClient, Gson gson, URI metaBase, URI modrinthBase) {
        this.httpClient = new SourceAwareHttpClient(Objects.requireNonNull(httpClient, "httpClient"));
        this.gson = Objects.requireNonNull(gson, "gson");
        this.metaBase = ensureTrailingSlash(metaBase);
        this.modrinthBase = ensureTrailingSlash(modrinthBase);
    }

    public void setSources(List<DownloadSource> sources) {
        httpClient.setSources(sources);
    }

    public List<QuiltLoaderVersion> fetchLoaderVersions(String gameVersion)
            throws IOException, InterruptedException {
        String game = encodePath(gameVersion);
        try {
            return httpClient.getParsedText(
                    metaBase.resolve("v3/versions/loader/" + game),
                    Duration.ofSeconds(30),
                    "application/json",
                    this::parseLoaderVersions
            ).orElse(List.of());
        } catch (BackendException e) {
            if (isNotFound(e)) {
                // Quilt Meta 对尚未收录的游戏版本返回 404（例如刚发布的正式版），按“无可用版本”处理。
                return List.of();
            }
            throw e;
        }
    }

    private static boolean isNotFound(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof BackendErrorCarrier carrier
                    && "network.httpError".equals(carrier.error().code())
                    && carrier.error().args().contains("404")) {
                return true;
            }
        }
        return false;
    }

    private Optional<List<QuiltLoaderVersion>> parseLoaderVersions(String text) {
        // 官方结构为 [{"loader":{"maven":..,"version":..,"build":..},"hashed":{..},"intermediary":{..}}]
        LoaderEntry[] entries = gson.fromJson(text, LoaderEntry[].class);
        if (entries == null) {
            return Optional.empty();
        }

        List<QuiltLoaderVersion> result = new ArrayList<>();
        for (LoaderEntry entry : entries) {
            if (entry == null || entry.loader() == null) {
                continue;
            }
            String version = entry.loader().version();
            if (version == null || version.isBlank()) {
                continue;
            }
            Integer build = entry.loader().build();
            result.add(new QuiltLoaderVersion(version, build == null ? 0 : Math.max(0, build)));
        }
        // Quilt Meta 的 build 是各版本自身的构建号（0.24.0 为 0、0.20.0-beta.9 为 9），
        // 不能用来比较版本新旧，界面统一按语义化版本从新到旧排列。
        result.sort((left, right) -> {
            int byVersion = compareVersions(right.version(), left.version());
            return byVersion != 0 ? byVersion : Integer.compare(right.build(), left.build());
        });
        return result.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(result));
    }

    /** 比较 Quilt Loader 版本号：核心数字按数值比较，正式版高于同号预发布版。 */
    static int compareVersions(String left, String right) {
        ParsedVersion leftVersion = parseVersion(left);
        ParsedVersion rightVersion = parseVersion(right);
        int length = Math.max(leftVersion.core().length, rightVersion.core().length);
        for (int index = 0; index < length; index++) {
            int result = Integer.compare(
                    numericPart(leftVersion.core(), index),
                    numericPart(rightVersion.core(), index));
            if (result != 0) {
                return result;
            }
        }
        boolean leftRelease = leftVersion.prerelease().isEmpty();
        boolean rightRelease = rightVersion.prerelease().isEmpty();
        if (leftRelease || rightRelease) {
            return Boolean.compare(leftRelease, rightRelease);
        }
        return comparePrerelease(leftVersion.prerelease(), rightVersion.prerelease());
    }

    private static ParsedVersion parseVersion(String version) {
        String[] split = version.trim().split("-", 2);
        return new ParsedVersion(split[0].split("\\."), split.length > 1 ? split[1] : "");
    }

    private static int numericPart(String[] parts, int index) {
        if (index >= parts.length) {
            return 0;
        }
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static int comparePrerelease(String left, String right) {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int index = 0; index < length; index++) {
            if (index >= leftParts.length) {
                return -1;
            }
            if (index >= rightParts.length) {
                return 1;
            }
            Integer leftNumber = toNumber(leftParts[index]);
            Integer rightNumber = toNumber(rightParts[index]);
            int result;
            if (leftNumber != null && rightNumber != null) {
                result = Integer.compare(leftNumber, rightNumber);
            } else if (leftNumber != null) {
                result = -1;
            } else if (rightNumber != null) {
                result = 1;
            } else {
                result = leftParts[index].compareTo(rightParts[index]);
            }
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    private static Integer toNumber(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    public String fetchLoaderProfile(String gameVersion, String loaderVersion)
            throws IOException, InterruptedException {
        String game = encodePath(gameVersion);
        String loader = encodePath(loaderVersion);
        URI uri = metaBase.resolve(
                "v3/versions/loader/" + game + "/" + loader + "/profile/json");
        return httpClient.getParsedText(uri, Duration.ofSeconds(30), "application/json",
                        this::parseLoaderProfile)
                .orElseThrow(() -> new IOException("Quilt Meta 没有返回有效的 Loader profile: " + uri));
    }

    private Optional<String> parseLoaderProfile(String text) {
        try {
            JsonObject profile = JsonParser.parseString(text).getAsJsonObject();
            if (profile.has("id") && !profile.get("id").isJsonNull()
                    && !profile.get("id").getAsString().isBlank()) {
                return Optional.of(text);
            }
        } catch (RuntimeException ignored) {
            // 无效 JSON 由 SourceAwareHttpClient 视为当前候选源不可用。
        }
        return Optional.empty();
    }

    public List<QuiltApiVersion> fetchApiVersions(String gameVersion)
            throws IOException, InterruptedException {
        String gameFilter = URLEncoder.encode("[\"" + gameVersion + "\"]", StandardCharsets.UTF_8);
        String loaderFilter = URLEncoder.encode("[\"quilt\"]", StandardCharsets.UTF_8);
        URI uri = modrinthBase.resolve(
                "v2/project/" + API_PROJECT + "/version?game_versions=" + gameFilter
                        + "&loaders=" + loaderFilter
        );

        return httpClient.getParsedText(uri, Duration.ofSeconds(30), "application/json",
                        this::parseApiVersions)
                .orElse(List.of());
    }

    private Optional<List<QuiltApiVersion>> parseApiVersions(String text) {
        ModrinthVersion[] versions = gson.fromJson(text, ModrinthVersion[].class);
        if (versions == null) {
            return Optional.empty();
        }

        List<QuiltApiVersion> result = new ArrayList<>();
        for (ModrinthVersion version : versions) {
            if (version == null || version.files() == null || version.files().isEmpty()) {
                continue;
            }
            if (version.status() != null && !"listed".equals(version.status())) {
                continue;
            }
            if (version.versionNumber() == null || version.versionNumber().isBlank()) {
                continue;
            }
            ModrinthFile file = version.files().stream()
                    .filter(candidate -> candidate != null && Boolean.TRUE.equals(candidate.primary()))
                    .findFirst()
                    .orElseGet(() -> version.files().stream()
                            .filter(Objects::nonNull)
                            .findFirst()
                            .orElse(null));
            if (file == null || file.url() == null || file.filename() == null
                    || file.hashes() == null || file.hashes().get("sha1") == null
                    || file.size() == null) {
                continue;
            }
            result.add(new QuiltApiVersion(
                    version.versionNumber(),
                    version.name() == null ? version.versionNumber() : version.name(),
                    file.filename(),
                    URI.create(file.url()),
                    file.hashes().get("sha1"),
                    file.size(),
                    version.datePublished()
            ));
        }

        result.sort(Comparator.comparing(
                QuiltApiVersion::releaseTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return result.isEmpty() ? Optional.empty() : Optional.of(List.copyOf(result));
    }

    private static URI ensureTrailingSlash(URI uri) {
        String value = Objects.requireNonNull(uri, "uri").toString();
        return URI.create(value.endsWith("/") ? value : value + "/");
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value, "value"), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private record ParsedVersion(String[] core, String prerelease) {
    }

    private record LoaderEntry(@SerializedName("loader") Loader loader) {
    }

    private record Loader(
            @SerializedName("maven") String maven,
            @SerializedName("version") String version,
            @SerializedName("build") Integer build
    ) {
    }

    private record ModrinthVersion(
            @SerializedName("name") String name,
            @SerializedName("version_number") String versionNumber,
            @SerializedName("version_type") String versionType,
            @SerializedName("date_published") String datePublished,
            @SerializedName("status") String status,
            @SerializedName("files") List<ModrinthFile> files
    ) {
    }

    private record ModrinthFile(
            @SerializedName("url") String url,
            @SerializedName("filename") String filename,
            @SerializedName("hashes") java.util.Map<String, String> hashes,
            @SerializedName("size") Long size,
            @SerializedName("primary") Boolean primary
    ) {
    }
}
