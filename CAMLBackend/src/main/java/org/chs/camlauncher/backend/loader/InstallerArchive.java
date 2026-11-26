package org.chs.camlauncher.backend.loader;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.model.Library;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.zip.ZipFile;

/** Forge/NeoForge installer JAR 的归档读取与内嵌库提取。 */
final class InstallerArchive implements AutoCloseable {
    private static final Gson GSON = new Gson();

    private final Path path;
    private final MetadataService metadata;
    private final ZipFile zip;

    InstallerArchive(Path path, MetadataService metadata) throws IOException {
        this.path = Objects.requireNonNull(path, "path");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.zip = new ZipFile(path.toFile());
    }

    static void validate(Path installer) throws IOException {
        try (ZipFile zip = new ZipFile(installer.toFile())) {
            if (zip.getEntry("version.json") == null
                    && zip.getEntry("install_profile.json") == null) {
                throw new IOException("installer 不是有效的 Forge/NeoForge 归档: " + installer);
            }
        }
    }

    String versionId() throws IOException {
        try {
            String versionJson = readEntry(zip, "version.json");
            if (versionJson != null) {
                return metadata.parseVersionInfo(versionJson).id();
            }

            String profileJson = readEntry(zip, "install_profile.json");
            if (profileJson != null) {
                JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                if (profile.has("version") && !profile.get("version").isJsonNull()) {
                    return profile.get("version").getAsString();
                }
            }
        } catch (RuntimeException e) {
            throw new IOException("无法读取安装器版本 ID: " + path, e);
        }
        throw new IOException("安装器缺少 version.json 或 install_profile.version: " + path);
    }

    List<Library> libraries() throws IOException {
        List<Library> libraries = new ArrayList<>();
        try {
            String versionJson = readEntry(zip, "version.json");
            if (versionJson != null) {
                libraries.addAll(metadata.parseVersionInfo(versionJson).libraries());
            }

            String profileJson = readEntry(zip, "install_profile.json");
            if (profileJson != null) {
                JsonObject profile = JsonParser.parseString(profileJson).getAsJsonObject();
                if (profile.has("libraries") && profile.get("libraries").isJsonArray()) {
                    List<Library> profileLibraries = GSON.fromJson(
                            profile.get("libraries"),
                            new TypeToken<List<Library>>() { }.getType()
                    );
                    if (profileLibraries != null) {
                        libraries.addAll(profileLibraries);
                    }
                }
            }
        } catch (RuntimeException e) {
            throw new IOException("无法读取安装器依赖库列表: " + path, e);
        }
        return libraries;
    }

    boolean extractBundledLibrary(
            String relativePath,
            Path destination,
            String sha1,
            Long size
    ) throws IOException {
        var entry = zip.getEntry("maven/" + relativePath);
        if (entry == null || entry.isDirectory()) {
            return false;
        }

        Path parent = destination.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".caml-bundled-library-", ".part");
        try {
            try (InputStream input = zip.getInputStream(entry)) {
                Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
            }
            if (size != null && size >= 0 && Files.size(temporary) != size) {
                throw new IOException("安装器内嵌库大小不匹配: " + relativePath);
            }
            if (sha1 != null && !sha1.isBlank()
                    && !HashVerifier.matches(temporary, sha1)) {
                throw new IOException("安装器内嵌库 SHA-1 不匹配: " + relativePath);
            }
            LoaderInstallFiles.moveFile(temporary, destination);
            return true;
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void close() throws IOException {
        zip.close();
    }

    private static String readEntry(ZipFile zip, String name) throws IOException {
        var entry = zip.getEntry(name);
        if (entry == null) {
            return null;
        }
        try (InputStream input = zip.getInputStream(entry)) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
