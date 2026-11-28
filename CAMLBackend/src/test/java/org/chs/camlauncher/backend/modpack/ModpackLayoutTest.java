package org.chs.camlauncher.backend.modpack;

import org.chs.camlauncher.backend.mod.model.ModFile;
import org.chs.camlauncher.backend.mod.model.ModSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ModpackLayoutTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void markerIdentifiesModpackInstance() throws Exception {
        ModFile file = new ModFile(
                ModSource.MODRINTH, "project", "version", "Test Pack", "1.0",
                "test.mrpack", List.of(), null, null, 0,
                List.of(), List.of(), List.of(), "", 0
        );
        ModrinthModpackIndex index = new ModrinthModpackIndex(
                "Test Pack", "1.0", "summary", Map.of("minecraft", "1.20.1"), List.of());

        ModpackLayout.writeMarker(temporaryDirectory, index, file);

        assertTrue(ModpackLayout.isModpack(temporaryDirectory));
        assertTrue(Files.readString(temporaryDirectory.resolve(ModpackLayout.MARKER_FILE))
                .contains("\"source\":\"modrinth\""));
    }

    @Test
    void curseForgeMarkerRecordsSource() throws Exception {
        ModFile file = new ModFile(
                ModSource.CURSEFORGE, "project", "version", "Curse Pack", "1.0",
                "curse-pack.zip", List.of(), null, null, 0,
                List.of(), List.of(), List.of(), "", 0
        );
        CurseForgeModpackManifest manifest = CurseForgeModpackManifest.parse("""
                {
                  "name": "Curse Pack",
                  "version": "1.0",
                  "minecraft": {"version": "1.20.1"}
                }
                """);

        ModpackLayout.writeMarker(temporaryDirectory, manifest, file);

        assertTrue(ModpackLayout.isModpack(temporaryDirectory));
        assertTrue(Files.readString(temporaryDirectory.resolve(ModpackLayout.MARKER_FILE))
                .contains("\"source\":\"curseforge\""));
    }
}
