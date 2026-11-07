package org.chs.camlauncher.backend.metadata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.chs.camlauncher.backend.json.JsonService;
import org.chs.camlauncher.backend.metadata.model.Argument;
import org.chs.camlauncher.backend.metadata.model.AssetIndexFile;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;
import org.chs.camlauncher.backend.metadata.model.VersionManifest;
import org.chs.camlauncher.backend.metadata.parse.ArgumentAdapter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public final class MetadataService {
    private final Gson gson;
    private final JsonService json;

    public MetadataService() {
        this(defaultGson());
    }

    public MetadataService(Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
        this.json = new JsonService(gson);
    }

    public VersionManifest readVersionManifest(Path file) throws IOException {
        return json.read(file, VersionManifest.class);
    }

    public VersionInfo readVersionInfo(Path file) throws IOException {
        return json.read(file, VersionInfo.class);
    }

    public AssetIndexFile readAssetIndex(Path file) throws IOException {
        return json.read(file, AssetIndexFile.class);
    }

    public VersionManifest parseVersionManifest(String text) {
        return gson.fromJson(text, VersionManifest.class);
    }

    public VersionInfo parseVersionInfo(String text) {
        return gson.fromJson(text, VersionInfo.class);
    }

    public AssetIndexFile parseAssetIndex(String text) {
        return gson.fromJson(text, AssetIndexFile.class);
    }

    private static Gson defaultGson() {
        return new GsonBuilder()
                .registerTypeAdapter(Argument.class, new ArgumentAdapter())
                .create();
    }
}
