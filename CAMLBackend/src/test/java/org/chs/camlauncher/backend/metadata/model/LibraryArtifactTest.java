package org.chs.camlauncher.backend.metadata.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryArtifactTest {
    @Test
    void fallsBackToForgeMavenForForgeLibrariesWithoutUrl() {
        Library library = library(
                "net.minecraftforge:forge:1.16.5-36.2.42:universal",
                "net/minecraftforge/forge/1.16.5-36.2.42/forge-1.16.5-36.2.42-universal.jar",
                "");

        assertEquals(
                "https://maven.minecraftforge.net/net/minecraftforge/forge/1.16.5-36.2.42/"
                        + "forge-1.16.5-36.2.42-universal.jar",
                LibraryArtifact.downloadUrl(library));
    }

    @Test
    void fallsBackToNeoForgeMavenForNeoForgeLibrariesWithoutUrl() {
        Library library = library(
                "net.neoforged:neoforge:21.1.1",
                "net/neoforged/neoforge/21.1.1/neoforge-21.1.1.jar",
                "");

        assertEquals(
                "https://maven.neoforged.net/releases/net/neoforged/neoforge/21.1.1/"
                        + "neoforge-21.1.1.jar",
                LibraryArtifact.downloadUrl(library));
    }

    @Test
    void keepsMojangFallbackForVanillaLibrariesWithoutUrl() {
        Library library = library(
                "com.google.guava:guava:20.0",
                "com/google/guava/guava/20.0/guava-20.0.jar",
                "");

        assertEquals(
                "https://libraries.minecraft.net/com/google/guava/guava/20.0/guava-20.0.jar",
                LibraryArtifact.downloadUrl(library));
    }

    private static Library library(String name, String path, String url) {
        return new Library(
                name,
                null,
                new Library.Downloads(new DownloadInfo(path, null, null, url), Map.of()),
                Map.of(),
                null,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null
        );
    }
}
