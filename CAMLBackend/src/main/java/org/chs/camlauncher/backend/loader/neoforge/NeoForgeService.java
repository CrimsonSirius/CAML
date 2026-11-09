package org.chs.camlauncher.backend.loader.neoforge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;
import org.chs.camlauncher.backend.loader.RemoteZipEntryReader;
import org.chs.camlauncher.backend.loader.MavenMetadataClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** 从 NeoForge Maven 获取与 Minecraft 版本匹配的 NeoForge 版本。 */
public final class NeoForgeService {
    private static final Logger LOGGER = LogManager.getLogger(NeoForgeService.class);
    private static final URI METADATA_URI = URI.create(
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
    );
    private static final String MAVEN_BASE =
            "https://maven.neoforged.net/releases/net/neoforged/neoforge/";

    private final MavenMetadataClient metadataClient;
    private final URI metadataUri;
    private final String mavenBase;
    private final Map<String, String> inheritedVersions = new ConcurrentHashMap<>();
    private volatile List<DownloadSource> sources = List.of(OfficialDownloadSource.INSTANCE);

    public NeoForgeService() {
        this(new MavenMetadataClient(), METADATA_URI, MAVEN_BASE);
    }

    public NeoForgeService(MavenMetadataClient metadataClient, URI metadataUri, String mavenBase) {
        this.metadataClient = metadataClient;
        this.metadataUri = metadataUri;
        this.mavenBase = mavenBase.endsWith("/") ? mavenBase : mavenBase + "/";
    }

    public void setSources(List<DownloadSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
        metadataClient.setSources(sources);
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
            URI installer = URI.create(mavenBase + version + "/neoforge-" + version + "-installer.jar");
            String inherited;
            try {
                inherited = inheritedVersion(version, installer);
            } catch (IOException e) {
                LOGGER.warn("无法读取 NeoForge {} 的实际 Minecraft 版本，跳过", version, e);
                continue;
            }
            if (!gameVersion.equals(inherited)) {
                continue;
            }
            result.add(new NeoForgeVersion(
                    gameVersion,
                    version,
                    installer
            ));
        }
        return List.copyOf(result);
    }

    private String inheritedVersion(String version, URI installer)
            throws IOException, InterruptedException {
        String cached = inheritedVersions.get(version);
        if (cached != null) {
            return cached;
        }
        String jsonText = RemoteZipEntryReader.readText(
                DownloadSources.candidates(sources, installer.toString()),
                "version.json",
                Duration.ofSeconds(30)
        );
        JsonObject json = JsonParser.parseString(jsonText).getAsJsonObject();
        if (!json.has("inheritsFrom") || json.get("inheritsFrom").isJsonNull()) {
            throw new IOException("NeoForge installer 没有 inheritsFrom: " + version);
        }
        String inherited = json.get("inheritsFrom").getAsString();
        inheritedVersions.put(version, inherited);
        return inherited;
    }
}
