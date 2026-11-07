package org.chs.camlauncher.backend.loader.forge;

import org.chs.camlauncher.backend.loader.MavenMetadataClient;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 从 Forge Maven 获取与 Minecraft 版本匹配的 Forge 版本。 */
public final class ForgeService {
    private static final URI METADATA_URI = URI.create(
            "https://maven.minecraftforge.net/net/minecraftforge/forge/maven-metadata.xml"
    );
    private static final String MAVEN_BASE = "https://maven.minecraftforge.net/net/minecraftforge/forge/";

    private final MavenMetadataClient metadataClient;
    private final URI metadataUri;
    private final String mavenBase;

    public ForgeService() {
        this(new MavenMetadataClient(), METADATA_URI, MAVEN_BASE);
    }

    public ForgeService(MavenMetadataClient metadataClient, URI metadataUri, String mavenBase) {
        this.metadataClient = metadataClient;
        this.metadataUri = metadataUri;
        this.mavenBase = mavenBase.endsWith("/") ? mavenBase : mavenBase + "/";
    }

    public List<ForgeVersion> fetchVersions(String gameVersion)
            throws IOException, InterruptedException {
        List<String> metadataVersions = new ArrayList<>(metadataClient.fetchVersions(metadataUri));
        Collections.reverse(metadataVersions);

        String prefix = gameVersion + "-";
        List<ForgeVersion> result = new ArrayList<>();
        for (String fullVersion : metadataVersions) {
            if (!fullVersion.startsWith(prefix) || fullVersion.length() == prefix.length()) {
                continue;
            }
            String version = fullVersion.substring(prefix.length());
            if (version.contains("-installer")) {
                continue;
            }
            result.add(new ForgeVersion(
                    gameVersion,
                    version,
                    URI.create(mavenBase + fullVersion + "/forge-" + fullVersion + "-installer.jar")
            ));
        }
        return List.copyOf(result);
    }
}
