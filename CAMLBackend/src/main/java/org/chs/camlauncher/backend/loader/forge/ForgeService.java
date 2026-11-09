package org.chs.camlauncher.backend.loader.forge;

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

/** 从 Forge Maven 获取与 Minecraft 版本匹配的 Forge 版本。 */
public final class ForgeService {
    private static final Logger LOGGER = LogManager.getLogger(ForgeService.class);
    private static final URI METADATA_URI = URI.create(
            "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"
    );
    private static final String MAVEN_BASE = "https://maven.minecraftforge.net/net/minecraftforge/forge/";

    private final MavenMetadataClient metadataClient;
    private final URI metadataUri;
    private final String mavenBase;
    private final HttpClient httpClient;
    private volatile String apiRoot;

    public ForgeService() {
        this(new MavenMetadataClient(), METADATA_URI, MAVEN_BASE);
    }

    public ForgeService(MavenMetadataClient metadataClient, URI metadataUri, String mavenBase) {
        this.metadataClient = metadataClient;
        this.metadataUri = metadataUri;
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

    public List<ForgeVersion> fetchVersions(String gameVersion)
            throws IOException, InterruptedException {
        if (apiRoot != null) {
            try {
                return fetchApiVersions(gameVersion);
            } catch (IOException e) {
                LOGGER.warn("从 {} 获取 Forge 版本失败，回退 Maven 元数据", apiRoot, e);
            }
        }
        return fetchMetadataVersions(gameVersion);
    }

    private List<ForgeVersion> fetchMetadataVersions(String gameVersion)
            throws IOException, InterruptedException {
        List<String> metadataVersions = new ArrayList<>(metadataClient.fetchVersions(metadataUri));
        Collections.reverse(metadataVersions);

        String prefix = gameVersion + "-";
        List<ForgeVersion> result = new ArrayList<>();
        for (String fullVersion : metadataVersions) {
            if (!fullVersion.startsWith(prefix) || fullVersion.length() == prefix.length()) {
                continue;
            }
            String version = fullVersion.substring(prefix.length());
            if (version.contains("-installer")) {
                continue;
            }
            result.add(new ForgeVersion(
                    gameVersion,
                    version,
                    URI.create(mavenBase + fullVersion + "/forge-" + fullVersion + "-installer.jar")
            ));
        }
        return List.copyOf(result);
    }

    private List<ForgeVersion> fetchApiVersions(String gameVersion)
            throws IOException, InterruptedException {
        URI uri = URI.create(apiRoot + "/forge/minecraft/" + encodePath(gameVersion));
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
        List<ForgeVersion> result = new ArrayList<>();
        for (JsonElement element : entries) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject object = element.getAsJsonObject();
            if (!object.has("version") || !hasInstaller(object.get("files"))) {
                continue;
            }
            String version = object.get("version").getAsString();
            String fullVersion = gameVersion + "-" + version;
            result.add(new ForgeVersion(
                    gameVersion,
                    version,
                    URI.create(mavenBase + fullVersion + "/forge-" + fullVersion + "-installer.jar")
            ));
        }
        Collections.reverse(result);
        return List.copyOf(result);
    }

    private static boolean hasInstaller(JsonElement files) {
        if (files == null || !files.isJsonArray()) {
            return false;
        }
        for (JsonElement file : files.getAsJsonArray()) {
            if (!file.isJsonObject()) {
                continue;
            }
            JsonObject object = file.getAsJsonObject();
            if (object.has("category") && object.has("format")
                    && "installer".equals(object.get("category").getAsString())
                    && "jar".equals(object.get("format").getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String trimTrailingSlash(String value) {
        String result = value.trim();
        return result.endsWith("/") ? result.substring(0, result.length() - 1) : result;
    }
}
