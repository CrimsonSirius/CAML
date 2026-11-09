package org.chs.camlauncher.backend.loader.optifine;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSourceFactory;
import org.chs.camlauncher.backend.download.source.DownloadSourceMode;
import org.chs.camlauncher.backend.download.source.SourceAwareHttpClient;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 获取 OptiFine 版本和 installer 地址，并按设置选择官方或镜像源。 */
public final class OptiFineService {
    private static final Logger LOGGER = LogManager.getLogger(OptiFineService.class);
    private static final URI DEFAULT_BASE = URI.create("https://bmclapi2.bangbang93.com/optifine/");
    private static final URI OFFICIAL_BASE = URI.create("https://optifine.net/");
    private static final URI OFFICIAL_DOWNLOADS = URI.create("https://optifine.net/downloads");
    private static final String USER_AGENT = "CAML/1.0";

    private final SourceAwareHttpClient httpClient;
    private final Gson gson;
    private final URI officialDownloadsUri;
    private volatile URI mirrorBase;
    private volatile DownloadSourceMode sourceMode = DownloadSourceMode.MIRROR;
    private volatile String customApi = "";

    public OptiFineService() {
        this(defaultClient(), new Gson(), DEFAULT_BASE);
    }

    public OptiFineService(HttpClient httpClient, Gson gson, URI base) {
        this(httpClient, gson, base, OFFICIAL_DOWNLOADS);
    }

    OptiFineService(HttpClient httpClient, Gson gson, URI base, URI officialDownloadsUri) {
        this.httpClient = new SourceAwareHttpClient(Objects.requireNonNull(httpClient, "httpClient"));
        this.gson = Objects.requireNonNull(gson, "gson");
        String value = Objects.requireNonNull(base, "base").toString();
        this.mirrorBase = URI.create(value.endsWith("/") ? value : value + "/");
        this.officialDownloadsUri = Objects.requireNonNull(officialDownloadsUri, "officialDownloadsUri");
    }

    public void setSources(List<DownloadSource> sources) {
        httpClient.setSources(sources);
    }

    public void setSourceMode(DownloadSourceMode mode, String customApi) {
        sourceMode = mode == null ? DownloadSourceMode.MIRROR : mode;
        this.customApi = customApi == null ? "" : customApi.trim();
        if (sourceMode == DownloadSourceMode.CUSTOM
                && DownloadSourceFactory.isValidCustomApi(this.customApi)) {
            String value = this.customApi.endsWith("/")
                    ? this.customApi.substring(0, this.customApi.length() - 1)
                    : this.customApi;
            mirrorBase = URI.create(value + "/optifine/");
        } else {
            mirrorBase = DEFAULT_BASE;
        }
    }

    public List<OptiFineVersion> fetchVersions(String gameVersion)
            throws IOException, InterruptedException {
        return switch (effectiveMode()) {
            case OFFICIAL -> fetchOfficialVersions(gameVersion);
            case MIRROR, CUSTOM -> fetchMirrorVersionsWithFallback(gameVersion);
        };
    }

    public URI resolveDownloadUri(OptiFineVersion version)
            throws IOException, InterruptedException {
        Objects.requireNonNull(version, "version");
        URI downloadUri = version.downloadUri();
        if (effectiveMode() == DownloadSourceMode.OFFICIAL) {
            return resolveOfficialDownload(version);
        }
        if (!"optifine.net".equalsIgnoreCase(downloadUri.getHost())) {
            return downloadUri;
        }

        return resolveOfficialDownload(version);
    }

    private URI resolveOfficialDownload(OptiFineVersion version)
            throws IOException, InterruptedException {
        URI downloadUri = OFFICIAL_BASE.resolve(
                "adloadx?f=" + encodeQuery(version.filename()));

        String page = httpClient.getText(downloadUri, Duration.ofSeconds(30), "text/html");
        Matcher matcher = Pattern.compile(
                "downloadx\\?f=[^\"'&]+(?:&amp;|&)x=[0-9a-fA-F]+"
        ).matcher(page);
        if (!matcher.find()) {
            throw new IOException("无法从 OptiFine 下载页解析安装器地址: " + downloadUri);
        }
        return OFFICIAL_BASE.resolve(matcher.group().replace("&amp;", "&"));
    }

    private List<OptiFineVersion> fetchMirrorVersionsWithFallback(String gameVersion)
            throws IOException, InterruptedException {
        try {
            List<OptiFineVersion> versions = fetchMirrorVersions(gameVersion);
            if (!versions.isEmpty()) {
                return versions;
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("OptiFine 镜像源查询失败，尝试官方下载页: {}", gameVersion, e);
        }
        return fetchOfficialVersions(gameVersion);
    }

    private List<OptiFineVersion> fetchMirrorVersions(String gameVersion)
            throws IOException, InterruptedException {
        String lookupVersion = toLookupVersion(gameVersion);
        String game = encodePath(lookupVersion);
        Entry[] entries = gson.fromJson(
                httpClient.getText(mirrorBase.resolve(game), Duration.ofSeconds(30), "application/json"),
                Entry[].class
        );
        if (entries == null) {
            return List.of();
        }

        List<OptiFineVersion> result = new ArrayList<>();
        for (Entry entry : entries) {
            if (entry == null
                    || blank(entry.mcversion())
                    || blank(entry.type())
                    || blank(entry.patch())
                    || !lookupVersion.equals(entry.mcversion())) {
                continue;
            }
            String filename = blank(entry.filename())
                    ? "OptiFine_" + gameVersion + "_" + entry.type() + "_" + entry.patch() + ".jar"
                    : entry.filename();
            URI download = mirrorBase.resolve(
                    game + "/" + encodePath(entry.type()) + "/" + encodePath(entry.patch()));
            result.add(new OptiFineVersion(
                    gameVersion,
                    entry.type(),
                    entry.patch(),
                    filename,
                    entry.forge(),
                    download
            ));
        }
        result.sort(Comparator
                .comparing(OptiFineVersion::preview)
                .thenComparing(OptiFineVersion::version, Comparator.reverseOrder()));
        return List.copyOf(result);
    }

    private List<OptiFineVersion> fetchOfficialVersions(String gameVersion)
            throws IOException, InterruptedException {
        String html = httpClient.getText(officialDownloadsUri, Duration.ofSeconds(30), "text/html");
        String lookupVersion = toLookupVersion(gameVersion);
        Matcher matcher = Pattern.compile(
                "(?is)<h2>\\s*Minecraft\\s+([^<]+?)\\s*</h2>|"
                        + "(<tr\\s+class=['\"]downloadLine[^'\"]*['\"][^>]*>.*?</tr>)"
        ).matcher(html);

        String currentVersion = null;
        List<OptiFineVersion> result = new ArrayList<>();
        while (matcher.find()) {
            if (matcher.group(1) != null) {
                currentVersion = matcher.group(1).trim();
                continue;
            }
            if (!lookupVersion.equals(currentVersion)) {
                continue;
            }
            String row = matcher.group(2);
            Matcher fileMatcher = Pattern.compile("adloadx\\?f=([^&\"']+)").matcher(row);
            if (!fileMatcher.find()) {
                continue;
            }
            String filename = fileMatcher.group(1).replace("&amp;", "&");
            OptiFineVersion version = parseOfficialVersion(
                    gameVersion, lookupVersion, filename, forgeOf(row));
            if (version != null) {
                result.add(version);
            }
        }
        result.sort(Comparator
                .comparing(OptiFineVersion::preview)
                .thenComparing(OptiFineVersion::version, Comparator.reverseOrder()));
        return List.copyOf(result);
    }

    private static OptiFineVersion parseOfficialVersion(
            String gameVersion, String lookupVersion, String filename, String forgeVersion) {
        if (filename == null || !filename.endsWith(".jar")) {
            return null;
        }
        String value = filename.substring(0, filename.length() - ".jar".length());
        if (value.startsWith("preview_")) {
            value = value.substring("preview_".length());
        }
        if (value.startsWith("OptiFine_")) {
            value = value.substring("OptiFine_".length());
        }
        String prefix = lookupVersion + "_";
        if (!value.startsWith(prefix)) {
            return null;
        }
        String[] parts = value.substring(prefix.length()).split("_");
        if (parts.length < 2) {
            return null;
        }
        String patch = parts[parts.length - 1];
        String type = String.join("_", Arrays.copyOf(parts, parts.length - 1));
        if (blank(type) || blank(patch)) {
            return null;
        }
        URI download = OFFICIAL_BASE.resolve(
                "adloadx?f=" + encodeQuery(filename));
        return new OptiFineVersion(
                gameVersion,
                type,
                patch,
                filename,
                blank(forgeVersion) ? null : forgeVersion,
                download
        );
    }

    private static String forgeOf(String row) {
        Matcher matcher = Pattern.compile(
                "(?is)<td\\s+class=['\"]colForge['\"][^>]*>(.*?)</td>"
        ).matcher(row);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1).replaceAll("<[^>]+>", "").trim();
    }

    private DownloadSourceMode effectiveMode() {
        if (sourceMode == DownloadSourceMode.CUSTOM
                && !DownloadSourceFactory.isValidCustomApi(customApi)) {
            return DownloadSourceMode.OFFICIAL;
        }
        return sourceMode;
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value, "value"), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static String encodeQuery(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value, "value"), StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private static String toLookupVersion(String version) {
        return switch (version) {
            case "1.8" -> "1.8.0";
            case "1.9" -> "1.9.0";
            default -> version;
        };
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static HttpClient defaultClient() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    private record Entry(
            @SerializedName("mcversion") String mcversion,
            @SerializedName("type") String type,
            @SerializedName("patch") String patch,
            @SerializedName("filename") String filename,
            @SerializedName("forge") String forge
    ) {
    }
}
