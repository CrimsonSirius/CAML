package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.mod.model.ModLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InstanceEnvironmentResolverTest {
    @TempDir
    Path game;

    @Test
    void resolvesFabricBaseVersionAndLoader() throws Exception {
        Path vanilla = game.resolve("versions").resolve("1.21.4");
        Files.createDirectories(vanilla);
        Files.writeString(vanilla.resolve("1.21.4.json"), """
                {"id":"1.21.4","mainClass":"net.minecraft.client.main.Main","libraries":[]}
                """);
        Path fabric = game.resolve("versions").resolve("cuteJKpww");
        Files.createDirectories(fabric);
        Files.writeString(fabric.resolve("cuteJKpww.json"), """
                {"id":"fabric-loader-0.16.0-1.21.4","inheritsFrom":"1.21.4",
                 "mainClass":"net.fabricmc.loader.impl.launch.knot.KnotClient",
                 "libraries":[{"name":"net.fabricmc:fabric-loader:0.16.0"}]}
                """);

        InstanceEnvironment environment = InstanceEnvironmentResolver.resolve(
                game, "cuteJKpww", new MetadataService());

        assertEquals("1.21.4", environment.gameVersion());
        assertEquals(ModLoader.FABRIC, environment.loader());
    }
}
