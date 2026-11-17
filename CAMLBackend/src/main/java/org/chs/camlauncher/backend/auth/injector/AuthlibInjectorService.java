package org.chs.camlauncher.backend.auth.injector;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * authlib-injector 的获取、校验与缓存。
 *
 * <p>运行时下载<b>只发生一次</b>：jar 落在
 * {@code ~/.caml/authlib-injector/<version>/} 下，之后每次启动只做 SHA-256 校验，
 * 命中即复用，全程零网络。只有三种情况会重新下载：首次使用、启动器升级提升固定版本号、
 * 本地文件被改动导致校验失败。</p>
 *
 * <p>校验值来自上游随包发布的 {@code .sha256} 文件（首次下载后固化到本地），
 * 因此不需要在代码里硬编码一个无法随上游更新的哈希；用户指定自定义 jar 时完全跳过
 * 下载与校验。</p>
 */
public final class AuthlibInjectorService {
    private static final Logger LOGGER = LogManager.getLogger(AuthlibInjectorService.class);

    /** 固定版本：升级启动器时同步提升，旧版本目录保留，互不覆盖。 */
    public static final String VERSION = "1.2.5";

    private static final String ARTIFACT_BASE =
            "https://authlib-injector.yushi.moe/artifact/" + VERSION;
    private static final String FILE_NAME = "authlib-injector-" + VERSION + ".jar";
    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    private final Path cacheDirectory;
    private final FileDownloader downloader;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private volatile Path override;

    public AuthlibInjectorService(Path dataDirectory, FileDownloader downloader) {
        Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.cacheDirectory = dataDirectory.resolve("authlib-injector").resolve(VERSION);
        this.downloader = Objects.requireNonNull(downloader, "downloader");
    }

    /**
     * 指定自定义 jar，跳过下载与校验（内网、离线或用户自行管理版本的场景）。
     *
     * @param jar jar 路径；{@code null} 表示恢复使用内置版本
     */
    public void setOverride(Path jar) {
        this.override = jar == null ? null : jar.toAbsolutePath().normalize();
    }

    public Path override() {
        return override;
    }

    public Path cachedJar() {
        return cacheDirectory.resolve(FILE_NAME);
    }

    /**
     * 确保 authlib-injector 可用，并返回本次启动需要的注入参数。
     *
     * @param apiRoot          认证服务器地址
     * @param prefetchedBase64 预取的 metadata；可为空
     */
    public AuthlibInjector ensure(String apiRoot, String prefetchedBase64)
            throws IOException, InterruptedException {
        Path jar = override;
        if (jar != null) {
            if (!Files.isRegularFile(jar)) {
                throw new BackendException(BackendError.of(
                        "auth.error.injectorNotFound", jar.toString()));
            }
            return new AuthlibInjector(jar, apiRoot, prefetchedBase64);
        }

        ensureCachedJar();
        return new AuthlibInjector(cachedJar(), apiRoot, prefetchedBase64);
    }

    private void ensureCachedJar() throws IOException, InterruptedException {
        Path jar = cachedJar();
        Path checksumFile = cacheDirectory.resolve(FILE_NAME + ".sha256");

        if (Files.isRegularFile(jar) && Files.isRegularFile(checksumFile)) {
            String expected = readChecksum(checksumFile);
            if (!expected.isEmpty() && expected.equalsIgnoreCase(sha256(jar))) {
                return;
            }
            LOGGER.warn("authlib-injector 校验失败，重新下载: {}", jar);
        }

        Files.createDirectories(cacheDirectory);
        String expected = fetchPublishedChecksum();

        DownloadItem item = new DownloadItem(
                "authlib-injector " + VERSION,
                List.of(URI.create(ARTIFACT_BASE + "/" + FILE_NAME),
                        URI.create("https://github.com/yushijinhun/authlib-injector/releases"
                                + "/download/v" + VERSION + "/" + FILE_NAME)),
                jar,
                null,
                0);
        downloader.download(item, DownloadListener.NONE, file -> verify(file, expected),
                new DownloadCancellationToken());

        String actual = sha256(jar);
        if (expected != null && !expected.isBlank() && !expected.equalsIgnoreCase(actual)) {
            Files.deleteIfExists(jar);
            throw new BackendException(BackendError.of(
                    "auth.error.injectorChecksum", jar.toString()));
        }
        Files.writeString(checksumFile, actual, StandardCharsets.UTF_8);
        LOGGER.info("已下载 authlib-injector {}: {}", VERSION, jar);
    }

    private static void verify(Path file, String expected) throws IOException {
        if (expected == null || expected.isBlank()) {
            return;
        }
        if (!expected.equalsIgnoreCase(sha256(file))) {
            throw new IOException("authlib-injector SHA-256 mismatch");
        }
    }

    /** 上游随包发布的校验值；不可用时返回空串，退化为下载后自行计算。 */
    private String fetchPublishedChecksum() {
        try {
            HttpRequest request = HttpRequest.newBuilder(
                            URI.create(ARTIFACT_BASE + "/" + FILE_NAME + ".sha256"))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(
                    request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return normalizeChecksum(response.body());
            }
        } catch (IOException e) {
            LOGGER.debug("无法获取 authlib-injector 校验值，将按下载内容计算", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "";
    }

    private static String readChecksum(Path file) {
        try {
            return normalizeChecksum(Files.readString(file, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return "";
        }
    }

    private static String normalizeChecksum(String raw) {
        if (raw == null) {
            return "";
        }
        String value = raw.trim();
        if (value.isEmpty()) {
            return "";
        }
        // 校验文件可能是 "<hex>" 或 "<hex>  filename" 两种形式。
        String first = value.split("\\s+")[0];
        return first.matches("[0-9a-fA-F]{64}") ? first : "";
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
