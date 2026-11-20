package org.chs.camlauncher.backend.instance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstanceResourcePackMetadataServiceTest {
    @TempDir
    Path root;

    @Test
    void readsPackFormatFromZip() throws Exception {
        Path pack = root.resolve("pack.zip");
        writeZip(pack, "{\"pack\":{\"pack_format\":34,\"description\":\"test\"}}");

        assertEquals("34", InstanceResourcePackMetadataService.version(pack));
    }

    @Test
    void readsPackFormatFromDirectory() throws Exception {
        Path pack = root.resolve("pack");
        Files.createDirectories(pack);
        Files.writeString(pack.resolve("pack.mcmeta"),
                "{\"pack\":{\"pack_format\":15}}", StandardCharsets.UTF_8);

        assertEquals("15", InstanceResourcePackMetadataService.version(pack));
    }

    @Test
    void ignoresMissingOrInvalidMetadata() throws Exception {
        Path invalid = root.resolve("invalid.zip");
        writeZip(invalid, "{\"pack\":{\"description\":\"test\"}}");
        Path notAZip = root.resolve("not-a-zip.zip");
        Files.writeString(notAZip, "plain text", StandardCharsets.UTF_8);

        assertEquals("", InstanceResourcePackMetadataService.version(invalid));
        assertEquals("", InstanceResourcePackMetadataService.version(notAZip));
    }

    private static void writeZip(Path path, String metadata) throws IOException {
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(path))) {
            output.putNextEntry(new ZipEntry("pack.mcmeta"));
            output.write(metadata.getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
        }
    }
}
