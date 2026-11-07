package org.chs.camlauncher.backend.download.plan;

import org.chs.camlauncher.backend.metadata.model.AssetIndexFile;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.model.DownloadPlan;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssetDownloadPlannerTest {
    @TempDir
    Path assetsDirectory;

    @Test
    void plansAssetObjects() {
        AssetIndexFile index = new MetadataService().parseAssetIndex("""
                {
                  "objects": {
                    "minecraft/sounds/click.ogg": {
                      "hash": "0123456789abcdef0123456789abcdef01234567",
                      "size": 42
                    }
                  }
                }
                """);

        DownloadPlan plan = new AssetDownloadPlanner(
                List.of(OfficialDownloadSource.INSTANCE),
                "https://resources.download.minecraft.net/"
        ).plan(index, assetsDirectory);

        assertEquals(1, plan.items().size());
        DownloadItem item = plan.items().getFirst();
        assertTrue(item.destination().endsWith(Path.of(
                "objects", "01", "0123456789abcdef0123456789abcdef01234567")));
        assertEquals(
                "https://resources.download.minecraft.net/01/0123456789abcdef0123456789abcdef01234567",
                item.candidates().getFirst().toString()
        );
    }
}
