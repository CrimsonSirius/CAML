package org.chs.camlauncher.backend.loader.optifine;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptiFineInstallerTest {
    @Test
    void mergesLibrariesAndForgeTweakerForLaunchWrapper() {
        JsonObject forge = JsonParser.parseString("""
                {
                  "mainClass": "net.minecraft.launchwrapper.Launch",
                  "libraries": [{"name":"net.minecraftforge:forge:1.12.2"}],
                  "arguments": {"game":["--username","Steve"]}
                }
                """).getAsJsonObject();
        JsonObject optiFine = JsonParser.parseString("""
                {
                  "libraries": [
                    {"name":"optifine:OptiFine:1.12.2_HD_U_F5",
                     "downloads":{"artifact":{"path":"optifine/OptiFine/1.12.2_HD_U_F5/OptiFine-1.12.2_HD_U_F5.jar","sha1":"abc","size":123}}},
                    {"name":"optifine:launchwrapper-of:2.1"}
                  ]
                }
                """).getAsJsonObject();

        OptiFineInstaller.mergeManifest(forge, optiFine);

        JsonArray libraries = forge.getAsJsonArray("libraries");
        assertEquals(3, libraries.size());
        assertEquals("optifine:OptiFine:1.12.2_HD_U_F5",
                libraries.get(1).getAsJsonObject().get("name").getAsString());
        assertFalse(libraries.get(1).getAsJsonObject()
                .getAsJsonObject("downloads").getAsJsonObject("artifact").has("sha1"));
        JsonArray gameArguments = forge.getAsJsonObject("arguments").getAsJsonArray("game");
        assertTrue(gameArguments.toString().contains("--tweakClass"));
        assertTrue(gameArguments.toString().contains("optifine.OptiFineForgeTweaker"));
    }

    @Test
    void keepsModernForgeMainClassWithoutTweakClass() {
        JsonObject forge = JsonParser.parseString("""
                {
                  "mainClass": "cpw.mods.bootstraplauncher.BootstrapLauncher",
                  "libraries": []
                }
                """).getAsJsonObject();
        JsonObject optiFine = JsonParser.parseString("""
                {"libraries":[{"name":"optifine:OptiFine:1.20.1_HD_U_I6"}]}
                """).getAsJsonObject();

        OptiFineInstaller.mergeManifest(forge, optiFine);

        assertFalse(forge.has("arguments"));
        assertEquals("cpw.mods.bootstraplauncher.BootstrapLauncher",
                forge.get("mainClass").getAsString());
        assertEquals(1, forge.getAsJsonArray("libraries").size());
    }
}
