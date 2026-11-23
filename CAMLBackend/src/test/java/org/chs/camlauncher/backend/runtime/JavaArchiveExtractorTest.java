package org.chs.camlauncher.backend.runtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaArchiveExtractorTest {
    @TempDir
    Path directory;

    @Test
    void extractsZipArchive() throws Exception {
        Path archive = directory.resolve("jdk.zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new ZipEntry("jdk-25/bin/java"));
            output.write("zip-java".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }

        Path target = directory.resolve("zip-out");
        JavaArchiveExtractor.extract(archive, target, "jdk.zip");

        assertEquals("zip-java", Files.readString(target.resolve("jdk-25/bin/java")));
    }

    @Test
    void extractsTarGzArchive() throws Exception {
        Path archive = directory.resolve("jdk.tar.gz");
        byte[] content = "tar-java".getBytes(StandardCharsets.UTF_8);
        try (GZIPOutputStream gzip = new GZIPOutputStream(Files.newOutputStream(archive))) {
            gzip.write(tarEntry("jdk-25/bin/java", content));
            gzip.write(new byte[1024]);
        }

        Path target = directory.resolve("tar-out");
        JavaArchiveExtractor.extract(archive, target, "jdk.tar.gz");

        assertEquals("tar-java", Files.readString(target.resolve("jdk-25/bin/java")));
        assertTrue(Files.isDirectory(target));
    }

    private static byte[] tarEntry(String name, byte[] content) {
        byte[] header = new byte[512];
        write(header, 0, name);
        write(header, 100, "0000644");
        write(header, 108, "0000000");
        write(header, 116, "0000000");
        write(header, 124, String.format("%011o", content.length));
        write(header, 136, "00000000000");
        header[156] = '0';
        write(header, 257, "ustar");
        write(header, 263, "00");

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(header);
        output.writeBytes(content);
        int padding = (512 - (content.length % 512)) % 512;
        output.writeBytes(new byte[padding]);
        return output.toByteArray();
    }

    private static void write(byte[] target, int offset, String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(bytes, 0, target, offset, bytes.length);
    }
}
