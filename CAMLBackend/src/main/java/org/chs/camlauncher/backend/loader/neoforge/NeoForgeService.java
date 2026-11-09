package org.chs.camlauncher.backend.loader.neoforge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSourceFactory;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.download.source.MirrorDownloadSource;
import org.chs.camlauncher.backend.loader.MavenMetadataClient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** 从 NeoForge Maven/API 获取与 Minecraft 版本匹配的 NeoForge 版本。 */
public final class NeoForgeService {
    private static final Logger LOGGER = LogManager.getLogger(NeoForgeService.class);
    private static final URI METADATA_URI = URI.create(
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
    );
    private static final String MAVEN_BASE =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/";

    private final MavenMetadataClient metadataClient;
    private final URI metadataUri;
    private final String mavenBase;
    private final HttpClient httpClient;
    private volatile String apiRoot;

    public NeoForgeService() {
        this(new MavenMetadataClient(), METADATA_URI, MAVEN_BASE);
    }

    public NeoForgeService(MavenMetadataClient metadataClient, URI metadataUri, String mavenBase) {
        this.metadataClient = Objects.requireNonNull(metadataClient, "metadataClient");
        this.metadataUri = Objects.requireNonNull(metadataUri, "metadataUri");
        this.mavenBase = mavenBase.endsWith("/") ? mavenBase : mavenBase + "/";
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public void setSources(List<DownloadSource> sources) {
        metadataClient.setSources(sources);
    }

    public void setSourceMode(DownloadSourceMode mode, String customApi) {
        apiRoot = switch (Objects.requireNonNull(mode, "mode")) {
            case MIRROR -> MirrorDownloadSource.BMCLAPI_ROOT;
            case CUSTOM -> DownloadSourceFactory.isValidCustomApi(customApi)
                    ? trimTrailingSlash(customApi)
                    : null;
            case OFFICIAL -> null;
        };
    }

    public List<NeoForgeVersion> fetchVersions(String gameVersion)
            throws IOException, InterruptedException {
        if (apiRoot != null) {
            try {
                return fetchApiVersions(gameVersion);
            } catch (IOException | RuntimeException e) {
                LOGGER.warn("从 {} 获取 NeoForge 版本失败，回退 Maven 元数据", apiRoot, e);
            }
        }
        return fetchMetadataVersions(gameVersion);
    }

    private List<NeoForgeVersion> fetchApiVersions(String gameVersion)
            throws IOException, InterruptedException {
        URI uri = URI.create(apiRoot + "/neoforge/list/" + encodePath(gameVersion));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", "CAML")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " for " + uri);
        }

        JsonArray entries = JsonParser.parseString(response.body()).getAsJsonArray();
        List<NeoForgeVersion> result = new ArrayList<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            if (!object.has("version") || !object.has("mcversion")) {
                continue;
            }
            String version = object.get("version").getAsString();
            String minecraft = object.get("mcversion").getAsString();
            if (!gameVersion.equals(minecraft) || version.contains("-installer")) {
                continue;
            }
            result.add(new NeoForgeVersion(
                    gameVersion,
                    version,
                    URI.create(mavenBase + version + "/neoforge-" + version + "-installer.jar")
            ));
        }
        Collections.reverse(result);
        return List.copyOf(result);
    }

    private List<NeoForgeVersion> fetchMetadataVersions(String gameVersion)
            throws IOException, InterruptedException {
        List<String> metadataVersions = new ArrayList<>(metadataClient.fetchVersions(metadataUri));
        Collections.reverse(metadataVersions);
        List<NeoForgeVersion> result = new ArrayList<>();
        for (String version : metadataVersions) {
            if (version.contains("-installer")) {
                continue;
            }
            String minecraft = minecraftVersionOf(version);
            if (!gameVersion.equals(minecraft)) {
                continue;
            }
            result.add(new NeoForgeVersion(
                    gameVersion,
                    version,
                    URI.create(mavenBase + version + "/neoforge-" + version + "-installer.jar")
            ));
        }
        return List.copyOf(result);
    }

    private static String minecraftVersionOf(String version) {
        try {
            int si1 = version.indexOf('.');
            int si2 = version.indexOf('.', si1 + 1);
            if (si1 < 0 || si2 < 0) {
                return null;
            }

            int major = Integer.parseInt(version.substring(0, si1));
            if (major == 0) {
                return version.substring(si1 + 1, si2);
            }
            if (major >= 26) {
                int si3 = version.indexOf('.', si2 + 1);
                if (si3 < 0) {
                    return null;
                }
                String minecraft = Integer.parseInt(version.substring(si2 + 1, si3)) == 0
                        ? version.substring(0, si2)
                        : version.substring(0, si3);
                int snapshot = version.indexOf('+');
                return snapshot < 0
                        ? minecraft
                        : minecraft + "-" + version.substring(snapshot + 1);
            }

            String minecraft = Integer.parseInt(version.substring(si1 + 1, si2)) == 0
                    ? version.substring(0, si1)
                    : version.substring(0, si2);
            return "1." + minecraft;
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        return result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
    }
}
