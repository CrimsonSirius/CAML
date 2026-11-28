package org.chs.camlauncher.backend.mod.service;

import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.error.BackendException;
import org.chs.camlauncher.backend.io.ArchiveExtractionGuard;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldInstallerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void installsNestedWorldFolder() throws Exception {
        Path archive = archive(Map.of(
                "MyWorld/level.dat", bytes("level"),
                "MyWorld/region/r.0.0.mca", bytes("region")
        ));
        Path saves = temporaryDirectory.resolve("saves");

        Path installed = new WorldInstaller().install(
                archive, saves, "fallback.zip", new DownloadCancellationToken());

        assertEquals(saves.resolve("MyWorld"), installed);
        assertEquals("level", Files.readString(installed.resolve("level.dat")));
        assertTrue(Files.isRegularFile(installed.resolve("region/r.0.0.mca")));
    }

    @Test
    void usesFallbackNameWhenLevelDatIsAtArchiveRoot() throws Exception {
        Path archive = archive(Map.of(
                "level.dat", bytes("level"),
                "region/r.0.0.mca", bytes("region")
        ));
        Path saves = temporaryDirectory.resolve("saves");

        Path installed = new WorldInstaller().install(
                archive, saves, "My Map.zip", new DownloadCancellationToken());

        assertEquals(saves.resolve("My Map"), installed);
        assertTrue(Files.isRegularFile(installed.resolve("level.dat")));
    }

    @Test
    void avoidsOverwritingExistingWorld() throws Exception {
        Path saves = temporaryDirectory.resolve("saves");
        Files.createDirectories(saves.resolve("MyWorld"));
        Files.writeString(saves.resolve("MyWorld/level.dat"), "existing");
        Path archive = archive(Map.of("MyWorld/level.dat", bytes("new")));

        Path installed = new WorldInstaller().install(
                archive, saves, "fallback.zip", new DownloadCancellationToken());

        assertEquals(saves.resolve("MyWorld (2)"), installed);
        assertEquals("existing", Files.readString(saves.resolve("MyWorld/level.dat")));
        assertEquals("new", Files.readString(installed.resolve("level.dat")));
    }

    @Test
    void rejectsArchiveWithoutLevelDat() throws Exception {
        Path archive = archive(Map.of("readme.txt", bytes("not a world")));
        Path saves = temporaryDirectory.resolve("saves");

        BackendException error = assertThrows(BackendException.class,
                () -> new WorldInstaller().install(
                        archive, saves, "invalid.zip", new DownloadCancellationToken()));

        assertEquals("mod.error.worldArchiveInvalid", error.error().code());
        assertFalse(hasTemporaryWorldDirectory(saves));
    }

    @Test
    void rejectsPathTraversalEntry() throws Exception {
        Path archive = archive(Map.of("../evil.txt", bytes("escape")));
        Path saves = temporaryDirectory.resolve("saves");

        BackendException error = assertThrows(BackendException.class,
                () -> new WorldInstaller().install(
                        archive, saves, "traversal.zip", new DownloadCancellationToken()));

        assertEquals("mod.error.worldArchiveInvalid", error.error().code());
        assertFalse(Files.exists(temporaryDirectory.resolve("evil.txt")));
        assertFalse(hasTemporaryWorldDirectory(saves));
    }

    @Test
    void rejectsArchiveWithTooManyEntries() throws Exception {
        Path archive = archive(ordered(
                "MyWorld/level.dat", bytes("level"),
                "MyWorld/extra.txt", bytes("extra")
        ));
        Path saves = temporaryDirectory.resolve("saves");

        assertThrows(BackendException.class, () -> new WorldInstaller(
                new ArchiveExtractionGuard(1, 1024, 1024)).install(
                        archive, saves, "many.zip", new DownloadCancellationToken()));
        assertFalse(hasTemporaryWorldDirectory(saves));
    }

    @Test
    void rejectsSingleEntryOverLimit() throws Exception {
        Path archive = archive(Map.of("MyWorld/level.dat", bytes("toolarge")));
        Path saves = temporaryDirectory.resolve("saves");

        assertThrows(BackendException.class, () -> new WorldInstaller(
                new ArchiveExtractionGuard(10, 4, 1024)).install(
                        archive, saves, "large.zip", new DownloadCancellationToken()));
        assertFalse(hasTemporaryWorldDirectory(saves));
    }

    @Test
    void rejectsTotalExtractedSizeOverLimit() throws Exception {
        Path archive = archive(ordered(
                "MyWorld/level.dat", bytes("12345"),
                "MyWorld/extra.txt", bytes("67")
        ));
        Path saves = temporaryDirectory.resolve("saves");

        assertThrows(BackendException.class, () -> new WorldInstaller(
                new ArchiveExtractionGuard(10, 16, 6)).install(
                        archive, saves, "total.zip", new DownloadCancellationToken()));
        assertFalse(hasTemporaryWorldDirectory(saves));
    }

    @Test
    void cancellationPreventsWorldPublicationAndCleansTemporaryDirectory() throws Exception {
        Path archive = archive(Map.of("MyWorld/level.dat", bytes("level")));
        Path saves = temporaryDirectory.resolve("saves");
        DownloadCancellationToken token = new DownloadCancellationToken();
        token.cancel();

        assertThrows(InterruptedException.class, () -> new WorldInstaller().install(
                archive, saves, "cancelled.zip", token));

        assertFalse(Files.exists(saves.resolve("MyWorld")));
        assertFalse(hasTemporaryWorldDirectory(saves));
    }

    private Path archive(Map<String, byte[]> entries) throws Exception {
        Path archive = temporaryDirectory.resolve("world-" + System.nanoTime() + ".zip");
        try (ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(archive))) {
            for (Map.Entry<String, byte[]> entry : new LinkedHashMap<>(entries).entrySet()) {
                output.putNextEntry(new ZipEntry(entry.getKey()));
                output.write(entry.getValue());
                output.closeEntry();
            }
        }
        return archive;
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static Map<String, byte[]> ordered(Object... values) {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            entries.put((String) values[index], (byte[]) values[index + 1]);
        }
        return entries;
    }

    private static boolean hasTemporaryWorldDirectory(Path saves) throws Exception {
        if (!Files.isDirectory(saves)) {
            return false;
        }
        try (var files = Files.list(saves)) {
            return files.anyMatch(path -> path.getFileName().toString().startsWith(".caml-world-"));
        }
    }
}
