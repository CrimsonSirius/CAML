package org.chs.camlauncher.backend.loader.optifine;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** 从 BMCLAPI 获取 OptiFine 版本和 installer 地址。 */
public final class OptiFineService {
    private static final URI DEFAULT_BASE = URI.create("https://bmclapi2.bangbang93.com/optifine/");

    private final HttpClient httpClient;
    private final Gson gson;
    private final URI base;

    public OptiFineService() {
        this(defaultClient(), new Gson(), DEFAULT_BASE);
    }

    public OptiFineService(HttpClient httpClient, Gson gson, URI base) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.gson = Objects.requireNonNull(gson, "gson");
        String value = Objects.requireNonNull(base, "base").toString();
        this.base = URI.create(value.endsWith("/") ? value : value + "/");
    }

    public List<OptiFineVersion> fetchVersions(String gameVersion)
            throws IOException, InterruptedException {
        String lookupVersion = toLookupVersion(gameVersion);
        String game = encodePath(lookupVersion);
        Entry[] entries = gson.fromJson(fetchText(base.resolve(game)), Entry[].class);
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
            URI download = base.resolve(
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

    private String fetchText(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("User-Agent", "CAML/1.0")
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("HTTP " + response.statusCode() + " for " + uri);
        }
        return response.body();
    }

    private static String encodePath(String value) {
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
