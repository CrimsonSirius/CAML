package org.chs.camlauncher.backend.loader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.zip.InflaterInputStream;
import java.util.zip.Inflater;

/** 通过 HTTP Range 读取远程 ZIP 中的单个文本条目。 */
public final class RemoteZipEntryReader {
    private static final long EOCD_SIGNATURE = 0x06054b50L;
    private static final long CENTRAL_SIGNATURE = 0x02014b50L;
    private static final int EOCD_MIN_SIZE = 22;
    private static final int EOCD_SEARCH_SIZE = 65_557;
    private static final long MAX_ENTRY_SIZE = 4L * 1024 * 1024;

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    private RemoteZipEntryReader() {
    }

    public static String readText(List<URI> candidates, String entryName, Duration timeout)
            throws IOException, InterruptedException {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(entryName, "entryName");
        IOException lastFailure = null;
        for (URI candidate : candidates) {
            try {
                return readText(candidate, entryName, timeout);
            } catch (IOException e) {
                lastFailure = e;
            }
        }
        throw lastFailure == null
                ? new IOException("没有可用的 ZIP 地址: " + entryName)
                : lastFailure;
    }

    private static String readText(URI uri, String entryName, Duration timeout)
            throws IOException, InterruptedException {
        RangeResponse probe = range(uri, 0, 0, timeout);
        if (!probe.ranged()) {
            return readFromFullZip(probe.body(), entryName);
        }

        long size = probe.totalSize();
        long tailStart = Math.max(0, size - EOCD_SEARCH_SIZE);
        byte[] tail = range(uri, tailStart, size - 1, timeout).body();
        int eocd = findEocd(tail);
        long centralOffset = uint32(tail, eocd + 16);
        long centralSize = uint32(tail, eocd + 12);
        if (centralOffset == 0xFFFFFFFFL || centralSize == 0xFFFFFFFFL) {
            throw new IOException("不支持 ZIP64 installer: " + uri);
        }

        byte[] central = range(uri, centralOffset, centralOffset + centralSize - 1, timeout).body();
        int offset = 0;
        while (offset + 46 <= central.length) {
            if (uint32(central, offset) != CENTRAL_SIGNATURE) {
                throw new IOException("ZIP 中央目录格式错误: " + uri);
            }
            int method = uint16(central, offset + 10);
            long compressedSize = uint32(central, offset + 20);
            int nameLength = uint16(central, offset + 28);
            int extraLength = uint16(central, offset + 30);
            int commentLength = uint16(central, offset + 32);
            long localOffset = uint32(central, offset + 42);
            if (compressedSize == 0xFFFFFFFFL || localOffset == 0xFFFFFFFFL) {
                throw new IOException("不支持 ZIP64 条目: " + uri);
            }
            int nameStart = offset + 46;
            int nameEnd = nameStart + nameLength;
            if (nameEnd > central.length) {
                throw new IOException("ZIP 文件名越界: " + uri);
            }
            String name = new String(central, nameStart, nameLength, StandardCharsets.UTF_8);
            if (entryName.equals(name)) {
                return readEntry(uri, localOffset, compressedSize, method, timeout);
            }
            offset = nameEnd + extraLength + commentLength;
        }
        throw new IOException("ZIP 中没有条目 " + entryName + ": " + uri);
    }

    private static String readEntry(URI uri, long localOffset, long compressedSize,
                                    int method, Duration timeout)
            throws IOException, InterruptedException {
        if (compressedSize > MAX_ENTRY_SIZE) {
            throw new IOException("ZIP 条目过大: " + uri);
        }
        byte[] header = range(uri, localOffset, localOffset + 29, timeout).body();
        if (header.length < 30 || uint32(header, 0) != 0x04034b50L) {
            throw new IOException("ZIP 本地文件头无效: " + uri);
        }
        int nameLength = uint16(header, 26);
        int extraLength = uint16(header, 28);
        long dataStart = localOffset + 30 + nameLength + extraLength;
        byte[] compressed = range(uri, dataStart, dataStart + compressedSize - 1, timeout).body();
        byte[] data = switch (method) {
            case 0 -> compressed;
            case 8 -> inflate(compressed);
            default -> throw new IOException("不支持的 ZIP 压缩方式 " + method + ": " + uri);
        };
        return new String(data, StandardCharsets.UTF_8);
    }

    private static String readFromFullZip(byte[] data, String entryName) throws IOException {
        try (var input = new java.util.zip.ZipInputStream(new ByteArrayInputStream(data))) {
            java.util.zip.ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    return new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
            }
        }
        throw new IOException("ZIP 中没有条目 " + entryName);
    }

    private static byte[] inflate(byte[] compressed) throws IOException {
        try (InputStream input = new InflaterInputStream(
                new ByteArrayInputStream(compressed), new Inflater(true));
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            input.transferTo(output);
            return output.toByteArray();
        }
    }

    private static int findEocd(byte[] data) throws IOException {
        int start = Math.max(0, data.length - EOCD_SEARCH_SIZE);
        for (int index = data.length - EOCD_MIN_SIZE; index >= start; index--) {
            if (uint32(data, index) == EOCD_SIGNATURE) {
                return index;
            }
        }
        throw new IOException("ZIP 中找不到 EOCD");
    }

    private static RangeResponse range(URI uri, long start, long end, Duration timeout)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .header("Range", "bytes=" + start + "-" + end)
                .GET()
                .build();
        HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 200) {
            return new RangeResponse(response.body(), response.body().length, false);
        }
        if (response.statusCode() != 206) {
            throw new IOException("HTTP " + response.statusCode() + " for " + uri);
        }
        String contentRange = response.headers().firstValue("Content-Range").orElse("");
        int slash = contentRange.lastIndexOf('/');
        if (slash < 0) {
            throw new IOException("缺少 Content-Range: " + uri);
        }
        return new RangeResponse(response.body(), Long.parseLong(contentRange.substring(slash + 1)), true);
    }

    private static int uint16(byte[] data, int offset) {
        return (data[offset] & 0xff) | ((data[offset + 1] & 0xff) << 8);
    }

    private static long uint32(byte[] data, int offset) {
        return (data[offset] & 0xffL)
                | ((data[offset + 1] & 0xffL) << 8)
                | ((data[offset + 2] & 0xffL) << 16)
                | ((data[offset + 3] & 0xffL) << 24);
    }

    private record RangeResponse(byte[] body, long totalSize, boolean ranged) {
    }
}
