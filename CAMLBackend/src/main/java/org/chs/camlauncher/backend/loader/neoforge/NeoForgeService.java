package org.chs.camlauncher.backend.loader.neoforge;

import org.chs.camlauncher.backend.loader.MavenMetadataClient;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 从 NeoForge Maven 获取与 Minecraft 版本匹配的 NeoForge 版本。 */
public final class NeoForgeService {
    private static final URI METADATA_URI = URI.create(
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
    );
    private static final String MAVEN_BASE =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/";

    private final MavenMetadataClient metadataClient;
    private final URI metadataUri;
    private final String mavenBase;

    public NeoForgeService() {
        this(new MavenMetadataClient(), METADATA_URI, MAVEN_BASE);
    }

    public NeoForgeService(MavenMetadataClient metadataClient, URI metadataUri, String mavenBase) {
        this.metadataClient = metadataClient;
        this.metadataUri = metadataUri;
        this.mavenBase = mavenBase.endsWith("/") ? mavenBase : mavenBase + "/";
    }

    public List<NeoForgeVersion> fetchVersions(String gameVersion)
            throws IOException, InterruptedException {
        List<String> metadataVersions = new ArrayList<>(metadataClient.fetchVersions(metadataUri));
        Collections.reverse(metadataVersions);

        String minecraftPrefix = gameVersion.startsWith("1.")
                ? gameVersion.substring(2)
                : gameVersion;
        String prefix = minecraftPrefix + ".";
        List<NeoForgeVersion> result = new ArrayList<>();
        for (String version : metadataVersions) {
            if (!version.startsWith(prefix) || version.contains("-installer")) {
                continue;
            }
            result.add(new NeoForgeVersion(
                    gameVersion,
                    version,
                    URI.create(mavenBase + version + "/neoforge-" + version + "-installer.jar")
            ));
        }
        return List.copyOf(result);
    }
}
