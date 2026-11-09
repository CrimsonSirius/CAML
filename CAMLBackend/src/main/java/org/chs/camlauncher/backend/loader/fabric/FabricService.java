package org.chs.camlauncher.backend.loader.fabric;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.SourceAwareHttpClient;

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

/** Fabric Meta 与 Modrinth Fabric API 元数据客户端。 */
public final class FabricService {
    private static final URI DEFAULT_META_BASE = URI.create("https://meta.fabricmc.net/");
    private static final URI DEFAULT_MODRINTH_BASE = URI.create("https://api.modrinth.com/");

    private final SourceAwareHttpClient httpClient;
    private final Gson gson;
    private final URI metaBase;
    private final URI modrinthBase;

    public FabricService() {
        this(defaultClient(), new Gson(), DEFAULT_META_BASE, DEFAULT_MODRINTH_BASE);
    }

    public FabricService(HttpClient httpClient, Gson gson, URI metaBase, URI modrinthBase) {
        this.httpClient = new SourceAwareHttpClient(Objects.requireNonNull(httpClient, "httpClient"));
        this.gson = Objects.requireNonNull(gson, "gson");
        this.metaBase = ensureTrailingSlash(metaBase);
        this.modrinthBase = ensureTrailingSlash(modrinthBase);
    }

    public void setSources(List<DownloadSource> sources) {
        httpClient.setSources(sources);
    }

    public List<FabricLoaderVersion> fetchLoaderVersions(String gameVersion)
            throws IOException, InterruptedException {
        String game = encodePath(gameVersion);
        LoaderEntry[] entries = gson.fromJson(
                fetchText(metaBase.resolve("v2/versions/loader/" + game)),
                LoaderEntry[].class
        );
        if (entries == null) {
            return List.of();
        }

        List<FabricLoaderVersion> result = new ArrayList<>();
        for (LoaderEntry entry : entries) {
            if (entry != null && entry.loader() != null
                    && entry.loader().version() != null
                    && !entry.loader().version().isBlank()) {
                result.add(new FabricLoaderVersion(
                        entry.loader().version(),
                        Boolean.TRUE.equals(entry.loader().stable())
                ));
            }
        }
        return List.copyOf(result);
    }

    public String fetchLoaderProfile(String gameVersion, String loaderVersion)
            throws IOException, InterruptedException {
        String game = encodePath(gameVersion);
        String loader = encodePath(loaderVersion);
        return fetchText(metaBase.resolve(
                "v2/versions/loader/" + game + "/" + loader + "/profile/json"
        ));
    }

    public List<FabricApiVersion> fetchApiVersions(String gameVersion)
            throws IOException, InterruptedException {
        String gameFilter = URLEncoder.encode("[\"" + gameVersion + "\"]", StandardCharsets.UTF_8);
        String loaderFilter = URLEncoder.encode("[\"fabric\"]", StandardCharsets.UTF_8);
        URI uri = modrinthBase.resolve(
                "v2/project/fabric-api/version?game_versions=" + gameFilter + "&loaders=" + loaderFilter
        );

        ModrinthVersion[] versions = gson.fromJson(fetchText(uri), ModrinthVersion[].class);
        if (versions == null) {
            return List.of();
        }

        List<FabricApiVersion> result = new ArrayList<>();
        for (ModrinthVersion version : versions) {
            if (version == null || version.files() == null || version.files().isEmpty()) {
                continue;
            }
            if (version.status() != null && !"listed".equals(version.status())) {
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
            result.add(new FabricApiVersion(
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
                FabricApiVersion::releaseTime,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        return List.copyOf(result);
    }

    private String fetchText(URI uri) throws IOException, InterruptedException {
        return httpClient.getText(uri, Duration.ofSeconds(30), "application/json");
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

    private record LoaderEntry(@SerializedName("loader") Loader loader) {
    }

    private record Loader(
            @SerializedName("version") String version,
            @SerializedName("stable") Boolean stable
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
