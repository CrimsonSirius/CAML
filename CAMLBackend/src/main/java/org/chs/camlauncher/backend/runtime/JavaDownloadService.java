package org.chs.camlauncher.backend.runtime;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.download.model.DownloadItem;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** 基于 foojay Disco API 的 Java 下载、校验和安装。 */
public final class JavaDownloadService {
    private static final String API_ROOT = "https://api.foojay.io/disco/v3.0";
    private static final Gson GSON = new Gson();
    private static final Type PACKAGE_LIST_TYPE = new TypeToken<DiscoResponse<DiscoPackage>>() {
    }.getType();
    private static final Type FILE_LIST_TYPE = new TypeToken<DiscoResponse<DiscoFileInfo>>() {
    }.getType();
    private static final List<JavaDistributionOption> DISTRIBUTIONS = List.of(
            new JavaDistributionOption("temurin", "Eclipse Temurin"),
            new JavaDistributionOption("zulu", "Azul Zulu"),
            new JavaDistributionOption("liberica", "BellSoft Liberica"),
            new JavaDistributionOption("graalvm", "Oracle GraalVM"),
            new JavaDistributionOption("corretto", "Amazon Corretto"),
            new JavaDistributionOption("semeru", "IBM Semeru")
    );

    private final HttpClient httpClient;
    private final FileDownloader downloader;
    private final ManagedJavaRepository repository;
    private final JavaPlatform platform = JavaPlatform.current();

    public JavaDownloadService(ManagedJavaRepository repository, FileDownloader downloader) {
        this.repository = repository;
        this.downloader = downloader;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public List<JavaDistributionOption> distributions() {
        return DISTRIBUTIONS;
    }

    public List<JavaDownloadOption> listOptions(String distributionId,
                                                 JavaPackageType packageType)
            throws IOException, InterruptedException {
        JavaDistributionOption distribution = DISTRIBUTIONS.stream()
                .filter(option -> option.id().equals(distributionId))
                .findFirst()
                .orElseThrow(() -> new IOException("未知的 Java 发行版: " + distributionId));
        String url = API_ROOT + "/packages?" + query(
                "distribution", distribution.id(),
                "operating_system", platform.operatingSystem(),
                "architecture", platform.foojayArchitecture(),
                "archive_type", platform.archiveType(),
                "package_type", packageType.apiValue(),
                "release_status", "ga",
                "directly_downloadable", "true",
                "lib_c_type", "linux".equals(platform.operatingSystem()) ? "glibc" : ""
        );
        DiscoResponse<DiscoPackage> response = get(url, PACKAGE_LIST_TYPE);
        Map<Integer, DiscoPackage> latestByMajor = new LinkedHashMap<>();
        for (DiscoPackage item : response.result()) {
            if (!distribution.id().equals(item.distribution())
                    || !packageType.apiValue().equals(item.packageType())
                    || !item.directlyDownloadable()
                    || item.javafxBundled()
                    || !platform.archiveType().equals(item.archiveType())
                    || item.links() == null
                    || item.links().pkgInfoUri() == null) {
                continue;
            }
            int major = item.jdkVersion() > 0 ? item.jdkVersion() : item.majorVersion();
            DiscoPackage current = latestByMajor.get(major);
            if (current == null || compareVersions(
                    item.distributionVersion(), current.distributionVersion()) > 0) {
                latestByMajor.put(major, item);
            }
        }
        List<JavaDownloadOption> options = new ArrayList<>();
        for (Map.Entry<Integer, DiscoPackage> entry : latestByMajor.entrySet()) {
            DiscoPackage item = entry.getValue();
            options.add(new JavaDownloadOption(
                    item.id(),
                    distribution.id(),
                    distribution.displayName(),
                    packageType,
                    entry.getKey(),
                    item.javaVersion(),
                    item.distributionVersion(),
                    item.archiveType(),
                    item.fileName(),
                    item.links().pkgInfoUri(),
                    item.size()
            ));
        }
        options.sort(Comparator.comparingInt(JavaDownloadOption::majorVersion).reversed());
        return List.copyOf(options);
    }

    public JavaCandidate downloadAndInstall(JavaDownloadOption option, String name,
                                            DownloadListener listener,
                                            DownloadCancellationToken token)
            throws IOException, InterruptedException {
        if (option.packageInfoUri().isBlank()) {
            throw new IOException("Java 包缺少下载信息");
        }
        DiscoResponse<DiscoFileInfo> response = get(option.packageInfoUri(), FILE_LIST_TYPE);
        if (response.result().isEmpty()) {
            throw new IOException("Java 下载源没有返回文件信息");
        }
        DiscoFileInfo file = response.result().getFirst();
        if (file.directDownloadUri() == null || file.directDownloadUri().isBlank()
                || file.checksum() == null || file.checksum().isBlank()) {
            throw new IOException("Java 下载源缺少下载地址或校验值");
        }

        Path archive = repository.tempArchivePath(name, option.fileName());
        String checksumType = file.checksumType() == null
                ? "sha256"
                : file.checksumType().toLowerCase(Locale.ROOT);
        String sha1 = "sha1".equals(checksumType) ? file.checksum() : null;
        DownloadItem item = new DownloadItem(
                "Java " + option.javaVersion(),
                List.of(URI.create(file.directDownloadUri())),
                archive,
                sha1,
                option.size()
        );
        try {
            downloader.download(item, listener, downloaded -> validateChecksum(
                    downloaded, checksumType, file.checksum()), token);
            return repository.install(option, archive, name, file.checksum());
        } finally {
            Files.deleteIfExists(archive);
        }
    }

    private void validateChecksum(Path file, String type, String expected) throws IOException {
        String actual = switch (type) {
            case "sha1" -> HashVerifier.sha1(file);
            case "sha256" -> HashVerifier.sha256(file);
            case "sha512" -> HashVerifier.sha512(file);
            default -> throw new IOException("不支持的 Java 校验算法: " + type);
        };
        if (!actual.equalsIgnoreCase(expected)) {
            throw new IOException("Java 归档校验失败");
        }
    }

    private <T> DiscoResponse<T> get(String url, Type type) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(45))
                .header("Accept", "application/json")
                .header("User-Agent", "CAML Launcher")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(
                request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Java 下载源返回 HTTP " + response.statusCode());
        }
        DiscoResponse<T> parsed = GSON.fromJson(response.body(), type);
        if (parsed == null || parsed.result() == null) {
            throw new IOException("Java 下载源响应无效");
        }
        return parsed;
    }

    private static String query(String... keyValues) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            String value = keyValues[i + 1];
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append('&');
            }
            builder.append(URLEncoder.encode(keyValues[i], StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
        }
        return builder.toString();
    }

    private static int compareVersions(String left, String right) {
        if (left == null) {
            return right == null ? 0 : -1;
        }
        if (right == null) {
            return 1;
        }
        String[] a = left.split("[^0-9]+");
        String[] b = right.split("[^0-9]+");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            long x = i < a.length && !a[i].isBlank() ? Long.parseLong(a[i]) : 0;
            long y = i < b.length && !b[i].isBlank() ? Long.parseLong(b[i]) : 0;
            if (x != y) {
                return Long.compare(x, y);
            }
        }
        return left.compareTo(right);
    }

    private record DiscoResponse<T>(List<T> result) {
    }

    private record DiscoPackage(
            String id,
            @SerializedName("archive_type") String archiveType,
            String distribution,
            @SerializedName("major_version") int majorVersion,
            @SerializedName("java_version") String javaVersion,
            @SerializedName("distribution_version") String distributionVersion,
            @SerializedName("jdk_version") int jdkVersion,
            @SerializedName("directly_downloadable") boolean directlyDownloadable,
            @SerializedName("package_type") String packageType,
            @SerializedName("javafx_bundled") boolean javafxBundled,
            @SerializedName("filename") String fileName,
            Links links,
            long size
    ) {
    }

    private record Links(@SerializedName("pkg_info_uri") String pkgInfoUri) {
    }

    private record DiscoFileInfo(
            @SerializedName("direct_download_uri") String directDownloadUri,
            String checksum,
            @SerializedName("checksum_type") String checksumType
    ) {
    }
}
