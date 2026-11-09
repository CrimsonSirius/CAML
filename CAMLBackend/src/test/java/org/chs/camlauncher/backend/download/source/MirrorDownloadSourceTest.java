package org.chs.camlauncher.backend.download.source;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MirrorDownloadSourceTest {
    @Test
    void rewritesMojangAndLoaderUrls() {
        MirrorDownloadSource source = new MirrorDownloadSource("https://mirror.example/root/");

        assertEquals(
                "https://mirror.example/root/mc/game/version_manifest_v2.json",
                source.candidates(URI.create(
                        "https://piston-meta.mojang.com/mc/game/version_manifest_v2.json")).get(0).toString()
        );
        assertEquals(
                "https://mirror.example/root/libraries/com/example/a.jar",
                source.candidates(URI.create(
                        "https://libraries.minecraft.net/com/example/a.jar")).get(0).toString()
        );
        assertEquals(
                "https://mirror.example/root/assets/aa/abcdef",
                source.candidates(URI.create(
                        "https://resources.download.minecraft.net/aa/abcdef")).get(0).toString()
        );
        assertTrue(source.candidates(URI.create("https://other.example/a.jar")).isEmpty());
    }

    @Test
    void createsOfficialFallbackAndCustomValidation() {
        assertTrue(DownloadSourceFactory.create(DownloadSourceMode.OFFICIAL, "")
                .get(0) instanceof OfficialDownloadSource);
        assertTrue(DownloadSourceFactory.create(DownloadSourceMode.CUSTOM, "not a url")
                .get(0) instanceof OfficialDownloadSource);
        assertTrue(DownloadSourceFactory.isValidCustomApi("https://mirror.example/api"));
    }
}
