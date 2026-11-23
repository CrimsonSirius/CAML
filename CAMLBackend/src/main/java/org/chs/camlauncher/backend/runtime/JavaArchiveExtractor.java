package org.chs.camlauncher.backend.runtime;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** 解压 Java 发行版归档；避免引入额外的压缩库。 */
final class JavaArchiveExtractor {
    private JavaArchiveExtractor() {
    }

    static void extract(Path archive, Path output, String fileName) throws IOException {
        Files.createDirectories(output);
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".zip")) {
            extractZip(archive, output);
        } else if (lower.endsWith(".tar.gz") || lower.endsWith(".tgz")) {
            try (InputStream input = new GZIPInputStream(Files.newInputStream(archive))) {
                extractTar(input, output);
            }
        } else {
            throw new IOException("不支持的 Java 归档格式: " + fileName);
        }
    }

    private static void extractZip(Path archive, Path output) throws IOException {
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                Path target = safeTarget(output, entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Files.createDirectories(target.getParent());
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
                input.closeEntry();
            }
        }
    }

    private static void extractTar(InputStream input, Path output) throws IOException {
        byte[] header = new byte[512];
        String paxPath = null;
        while (true) {
            int read = readBlock(input, header);
            if (read == 0 || isZeroBlock(header)) {
                return;
            }
            if (read < 512) {
                throw new EOFException("tar 头不完整");
            }

            String name = tarString(header, 0, 100);
            String prefix = tarString(header, 345, 155);
            if (!prefix.isBlank()) {
                name = prefix + "/" + name;
            }
            if (paxPath != null) {
                name = paxPath;
                paxPath = null;
            }
            long size = tarOctal(header, 124, 12);
            char type = (char) (header[156] & 0xff);

            if (type == 'x' || type == 'g' || type == 'L') {
                byte[] metadata = readEntryBytes(input, size);
                if (type == 'L') {
                    paxPath = new String(metadata, StandardCharsets.UTF_8).trim();
                } else if (type == 'x') {
                    paxPath = parsePaxPath(metadata);
                }
                continue;
            }

            Path target = safeTarget(output, name);
            if (type == '5') {
                Files.createDirectories(target);
                skipPadding(input, size);
            } else if (type == '2') {
                Files.createDirectories(target.getParent());
                createSymlink(output, target, tarString(header, 157, 100));
                skipPadding(input, size);
            } else if (type == '0' || type == 0 || type == '7') {
                Files.createDirectories(target.getParent());
                try (var outputStream = Files.newOutputStream(target)) {
                    copyExactly(input, outputStream, size);
                }
                skipPadding(input, size);
            } else {
                skipExactly(input, size);
                skipPadding(input, size);
            }
        }
    }

    private static String parsePaxPath(byte[] metadata) {
        String text = new String(metadata, StandardCharsets.UTF_8);
        for (String line : text.split("\n")) {
            int separator = line.indexOf(' ');
            if (separator < 0) {
                continue;
            }
            String record = line.substring(separator + 1);
            if (record.startsWith("path=")) {
                return record.substring("path=".length());
            }
        }
        return null;
    }

    private static void createSymlink(Path root, Path target, String linkName) throws IOException {
        if (linkName == null || linkName.isBlank()) {
            return;
        }
        Path linkTarget = Path.of(linkName);
        if (linkTarget.isAbsolute()) {
            return;
        }
        Path resolved = target.getParent().resolve(linkTarget).normalize();
        if (!resolved.startsWith(root)) {
            return;
        }
        try {
            Files.deleteIfExists(target);
            Files.createSymbolicLink(target, linkTarget);
        } catch (UnsupportedOperationException | IOException ignored) {
            // Some filesystems do not support symlinks; the Java executable remains usable.
        }
    }

    private static Path safeTarget(Path root, String name) throws IOException {
        String normalizedName = name.replace('\\', '/');
        while (normalizedName.startsWith("./")) {
            normalizedName = normalizedName.substring(2);
        }
        Path target = root.resolve(normalizedName).normalize();
        if (!target.startsWith(root) || target.equals(root)) {
            throw new IOException("归档路径越界: " + name);
        }
        return target;
    }

    private static int readBlock(InputStream input, byte[] block) throws IOException {
        int total = 0;
        while (total < block.length) {
            int read = input.read(block, total, block.length - total);
            if (read < 0) {
                return total;
            }
            total += read;
        }
        return total;
    }

    private static boolean isZeroBlock(byte[] block) {
        for (byte value : block) {
            if (value != 0) {
                return false;
            }
        }
        return true;
    }

    private static String tarString(byte[] block, int offset, int length) {
        int end = offset;
        int limit = Math.min(block.length, offset + length);
        while (end < limit && block[end] != 0) {
            end++;
        }
        return new String(block, offset, end - offset, StandardCharsets.UTF_8).trim();
    }

    private static long tarOctal(byte[] block, int offset, int length) {
        String value = tarString(block, offset, length).trim();
        if (value.isEmpty()) {
            return 0;
        }
        try {
            return Long.parseLong(value, 8);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static byte[] readEntryBytes(InputStream input, long size) throws IOException {
        if (size < 0 || size > Integer.MAX_VALUE) {
            throw new IOException("tar 元数据过大");
        }
        byte[] bytes = new byte[(int) size];
        copyExactly(input, bytes);
        skipPadding(input, size);
        return bytes;
    }

    private static void copyExactly(InputStream input, byte[] target) throws IOException {
        int offset = 0;
        while (offset < target.length) {
            int read = input.read(target, offset, target.length - offset);
            if (read < 0) {
                throw new EOFException("tar 文件内容不完整");
            }
            offset += read;
        }
    }

    private static void copyExactly(InputStream input, java.io.OutputStream output, long size)
            throws IOException {
        byte[] buffer = new byte[8192];
        long remaining = size;
        while (remaining > 0) {
            int read = input.read(buffer, 0, (int) Math.min(buffer.length, remaining));
            if (read < 0) {
                throw new EOFException("tar 文件内容不完整");
            }
            output.write(buffer, 0, read);
            remaining -= read;
        }
    }

    private static void skipExactly(InputStream input, long size) throws IOException {
        long remaining = size;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
            } else if (input.read() < 0) {
                throw new EOFException("tar 文件内容不完整");
            } else {
                remaining--;
            }
        }
    }

    private static void skipPadding(InputStream input, long size) throws IOException {
        long padding = (512 - (size % 512)) % 512;
        skipExactly(input, padding);
    }
}
