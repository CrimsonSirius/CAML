package org.chs.camlauncher.backend.loader.optifine;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.chs.camlauncher.backend.download.DownloadCancellationToken;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.model.DownloadItem;
import org.chs.camlauncher.backend.download.source.DownloadSource;
import org.chs.camlauncher.backend.download.source.DownloadSources;
import org.chs.camlauncher.backend.download.source.OfficialDownloadSource;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.InstanceNameRules;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.io.SafePaths;
import org.chs.camlauncher.backend.loader.ExternalLoaderInstaller;
import org.chs.camlauncher.backend.loader.forge.ForgeVersion;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.runtime.JavaRequirement;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipFile;

/**
 * 在 Forge 实例上安装 OptiFine。
 *
 * <p>先由 Forge 官方 installer 生成 Forge 版本，再调用 OptiFine 官方 installer
 * 生成补丁库，最后把 OptiFine 库和启动参数合并进 Forge 版本 JSON。</p>
 */
public final class OptiFineInstaller {
    private static final String LAUNCH_WRAPPER_MAIN = "net.minecraft.launchwrapper.Launch";
    private static final String OPTIFINE_FORGE_TWEAKER = "optifine.OptiFineForgeTweaker";
    private static final long INSTALL_TIMEOUT_MINUTES = 20;

    private final VersionInstaller versionInstaller;
    private final ExternalLoaderInstaller externalLoaderInstaller;
    private final JavaLocator javaLocator;
    private final FileDownloader fileDownloader;
    private final URI manifestUri;
    private final Path manifestCacheFile;
    private final OptiFineService optiFineService;
    private volatile List<DownloadSource> sources = List.of(OfficialDownloadSource.INSTANCE);

    public OptiFineInstaller(
            VersionInstaller versionInstaller,
            ExternalLoaderInstaller externalLoaderInstaller,
            JavaLocator javaLocator,
            FileDownloader fileDownloader,
            URI manifestUri,
            Path manifestCacheFile
    ) {
        this(versionInstaller, externalLoaderInstaller, javaLocator, fileDownloader,
                manifestUri, manifestCacheFile, new OptiFineService());
    }

    public OptiFineInstaller(
            VersionInstaller versionInstaller,
            ExternalLoaderInstaller externalLoaderInstaller,
            JavaLocator javaLocator,
            FileDownloader fileDownloader,
            URI manifestUri,
            Path manifestCacheFile,
            OptiFineService optiFineService
    ) {
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.externalLoaderInstaller = Objects.requireNonNull(
                externalLoaderInstaller, "externalLoaderInstaller");
        this.javaLocator = Objects.requireNonNull(javaLocator, "javaLocator");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
        this.manifestUri = Objects.requireNonNull(manifestUri, "manifestUri");
        this.manifestCacheFile = Objects.requireNonNull(manifestCacheFile, "manifestCacheFile");
        this.optiFineService = Objects.requireNonNull(optiFineService, "optiFineService");
    }

    public void setSources(List<DownloadSource> sources) {
        this.sources = List.copyOf(Objects.requireNonNull(sources, "sources"));
    }

    public InstalledVersion install(
            String gameVersion,
            String instanceId,
            ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        return install(gameVersion, instanceId, forgeVersion, optiFineVersion, gameDirectory,
                listener, new DownloadCancellationToken());
    }

    public InstalledVersion install(
            String gameVersion,
            String instanceId,
            ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(token, "token");
        token.throwIfCancelled();
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        InstanceNameRules.ensureNewInstance(gameDirectory, instanceId);
        Path targetVersionDirectory = SafePaths.resolveChild(
                gameDirectory.resolve("versions"), instanceId, "version id");
        try {
            return installInternal(
                    gameVersion, instanceId, forgeVersion, optiFineVersion, gameDirectory,
                    listener, token);
        } catch (IOException | RuntimeException | InterruptedException e) {
            deleteRecursively(targetVersionDirectory);
            throw e;
        }
    }

    private InstalledVersion installInternal(
            String gameVersion,
            String instanceId,
            ForgeVersion forgeVersion,
            OptiFineVersion optiFineVersion,
            Path gameDirectory,
            DownloadListener listener,
            DownloadCancellationToken token
    ) throws IOException, InterruptedException {
        token.throwIfCancelled();
        Objects.requireNonNull(forgeVersion, "forgeVersion");
        Objects.requireNonNull(optiFineVersion, "optiFineVersion");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        if (!OptiFineCompatibility.isForgeCompatible(forgeVersion, optiFineVersion)) {
            throw new IllegalArgumentException(
                    OptiFineCompatibility.incompatibleMessage(forgeVersion, optiFineVersion));
        }

        InstalledVersion base = versionInstaller.install(
                gameVersion,
                gameVersion,
                gameDirectory,
                manifestUri,
                manifestCacheFile,
                listener,
                token
        );
        InstalledVersion forge = externalLoaderInstaller.install(
                "Forge",
                gameVersion,
                instanceId,
                forgeVersion.installerUri(),
                gameDirectory,
                listener,
                token
        );

        JavaRuntime java = javaLocator.findSuitable(JavaRequirement.from(base.manifest()))
                .orElseThrow(() -> new IOException("找不到可用于运行 OptiFine 安装器的 Java"));

        Path installerDirectory = Files.createTempDirectory("caml-optifine-");
        Path installer = installerDirectory.resolve("installer.jar");
        Path staging = Files.createTempDirectory(gameDirectory, ".caml-optifine-install-");
        try {
            URI installerUri = optiFineService.resolveDownloadUri(optiFineVersion);
            DownloadListener.notifyStage(listener, "下载 OptiFine 安装器");
            token.throwIfCancelled();
            fileDownloader.download(new DownloadItem(
                    "OptiFine installer",
                    DownloadSources.candidates(sources, installerUri.toString()),
                    installer,
                    null,
                    -1
            ), listener, OptiFineInstaller::validateInstallerArchive, token);

            token.throwIfCancelled();
            prepareStaging(staging, gameVersion, base);
            DownloadListener.notifyStage(listener, "运行 OptiFine 安装器");
            runInstaller(java, installer, staging, token);
            OptiFineVersionFiles generated = findGeneratedVersion(staging, gameVersion);
            DownloadListener.notifyStage(listener, "合并 OptiFine 文件");
            token.throwIfCancelled();
            mergeLibraries(staging.resolve("libraries"), gameDirectory.resolve("libraries"));
            removeModsToml(gameDirectory.resolve("libraries").resolve("optifine"));

            JsonObject forgeJson = parseJson(forge.versionJson());
            mergeManifest(forgeJson, generated.json());
            writeJson(forge.versionJson(), forgeJson);
        } finally {
            deleteRecursively(installerDirectory);
            deleteRecursively(staging);
        }

        return versionInstaller.install(
                instanceId,
                gameDirectory,
                manifestUri,
                manifestCacheFile,
                listener,
                token
        );
    }

    private static void prepareStaging(Path staging, String gameVersion, InstalledVersion base)
            throws IOException {
        Path versionDirectory = staging.resolve("versions").resolve(gameVersion);
        Files.createDirectories(versionDirectory);
        Files.copy(base.versionJson(), versionDirectory.resolve(gameVersion + ".json"),
                StandardCopyOption.REPLACE_EXISTING);
        if (Files.isRegularFile(base.clientJar())) {
            Files.copy(base.clientJar(), versionDirectory.resolve(gameVersion + ".jar"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Files.writeString(staging.resolve("launcher_profiles.json"),
                "{\"profiles\":{},\"settings\":{}}\n", StandardCharsets.UTF_8);
    }

    private static void validateInstallerArchive(Path installer) throws IOException {
        try (ZipFile zip = new ZipFile(installer.toFile())) {
            if (zip.getEntry("optifine/Installer.class") == null) {
                throw new IOException("OptiFine installer 缺少 optifine/Installer.class: " + installer);
            }
        }
    }

    private static void runInstaller(JavaRuntime java, Path installer, Path staging,
                                     DownloadCancellationToken token)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(java.executable().toString());
        command.add("-Djava.awt.headless=true");
        command.add("-cp");
        command.add(bridgeClasspath());
        command.add(OptiFineInstallerBridge.class.getName());
        command.add(installer.toString());
        command.add(staging.toString());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(staging.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> captureOutput(process, output), "caml-optifine-installer");
        reader.setDaemon(true);
        reader.start();

        boolean finished = false;
        long deadline = System.nanoTime() + TimeUnit.MINUTES.toNanos(INSTALL_TIMEOUT_MINUTES);
        try {
            while (!(finished = process.waitFor(200, TimeUnit.MILLISECONDS))) {
                token.throwIfCancelled();
                if (System.nanoTime() >= deadline) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("OptiFine 安装器超时");
        }
        reader.join(5000);
        if (process.exitValue() != 0) {
            throw new IOException("OptiFine 安装器失败，退出码 " + process.exitValue()
                    + "\n" + output);
        }
    }

    private static String bridgeClasspath() throws IOException {
        String classpath = System.getProperty("java.class.path", "");
        List<String> entries = new ArrayList<>();
        for (String entry : classpath.split(File.pathSeparator)) {
            if (!entry.isBlank()) {
                entries.add(Path.of(entry).toAbsolutePath().normalize().toString());
            }
        }
        if (entries.isEmpty()) {
            try {
                entries.add(Path.of(OptiFineInstallerBridge.class.getProtectionDomain()
                        .getCodeSource().getLocation().toURI()).toString());
            } catch (Exception e) {
                throw new IOException("无法定位 OptiFine 安装桥的 classpath", e);
            }
        }
        return String.join(File.pathSeparator, entries);
    }

    private static OptiFineVersionFiles findGeneratedVersion(Path staging, String gameVersion)
            throws IOException {
        Path versions = staging.resolve("versions");
        if (Files.isDirectory(versions)) {
            try (Stream<Path> directories = Files.list(versions)) {
                for (Path directory : directories.filter(Files::isDirectory).toList()) {
                    Path jsonFile = directory.resolve(directory.getFileName() + ".json");
                    if (!Files.isRegularFile(jsonFile)) {
                        continue;
                    }
                    JsonObject json = parseJson(jsonFile);
                    String id = json.has("id") ? json.get("id").getAsString() : "";
                    if (!id.equals(gameVersion)
                            && id.toLowerCase(Locale.ROOT).contains("optifine")) {
                        return new OptiFineVersionFiles(jsonFile, json);
                    }
                }
            }
        }
        throw new IOException("OptiFine 安装器没有生成版本 JSON");
    }

    static void mergeManifest(JsonObject forge, JsonObject optiFine) {
        JsonArray forgeLibraries = array(forge, "libraries");
        Set<String> names = new HashSet<>();
        for (JsonElement element : forgeLibraries) {
            if (element.isJsonObject() && element.getAsJsonObject().has("name")) {
                names.add(element.getAsJsonObject().get("name").getAsString());
            }
        }
        for (JsonElement element : array(optiFine, "libraries")) {
            if (element.isJsonObject()) {
                JsonObject library = element.getAsJsonObject();
                String name = library.has("name") ? library.get("name").getAsString() : "";
                if (!name.isBlank() && names.add(name)) {
                    if (name.startsWith("optifine:")) {
                        stripChecksum(library);
                    }
                    forgeLibraries.add(library);
                }
            }
        }

        String mainClass = forge.has("mainClass") ? forge.get("mainClass").getAsString() : "";
        if (LAUNCH_WRAPPER_MAIN.equals(mainClass)) {
            appendForgeTweaker(forge);
        }
    }

    private static void appendForgeTweaker(JsonObject forge) {
        JsonObject arguments = forge.has("arguments") && forge.get("arguments").isJsonObject()
                ? forge.getAsJsonObject("arguments") : null;
        if (arguments != null) {
            JsonArray game = arguments.has("game") && arguments.get("game").isJsonArray()
                    ? arguments.getAsJsonArray("game") : new JsonArray();
            if (!contains(game, OPTIFINE_FORGE_TWEAKER)) {
                game.add("--tweakClass");
                game.add(OPTIFINE_FORGE_TWEAKER);
            }
            arguments.add("game", game);
            return;
        }

        String value = forge.has("minecraftArguments")
                ? forge.get("minecraftArguments").getAsString() : "";
        if (!value.contains(OPTIFINE_FORGE_TWEAKER)) {
            forge.addProperty("minecraftArguments",
                    value + (value.isBlank() ? "" : " ")
                            + "--tweakClass " + OPTIFINE_FORGE_TWEAKER);
        }
    }

    private static boolean contains(JsonArray values, String value) {
        for (JsonElement element : values) {
            if (element.isJsonPrimitive() && value.equals(element.getAsString())) {
                return true;
            }
        }
        return false;
    }

    private static void stripChecksum(JsonObject library) {
        library.remove("checksums");
        if (!library.has("downloads") || !library.get("downloads").isJsonObject()) {
            return;
        }
        JsonObject downloads = library.getAsJsonObject("downloads");
        if (downloads.has("artifact") && downloads.get("artifact").isJsonObject()) {
            JsonObject artifact = downloads.getAsJsonObject("artifact");
            artifact.remove("sha1");
            artifact.remove("size");
        }
    }

    private static JsonArray array(JsonObject object, String key) {
        if (object.has(key) && object.get(key).isJsonArray()) {
            return object.getAsJsonArray(key);
        }
        JsonArray array = new JsonArray();
        object.add(key, array);
        return array;
    }

    private static JsonObject parseJson(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8))
                .getAsJsonObject();
    }

    private static void writeJson(Path file, JsonObject json) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = Files.createTempFile(parent, "caml-optifine-", ".json");
        try {
            Files.writeString(temporary, new Gson().toJson(json), StandardCharsets.UTF_8);
            move(temporary, file);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private static void mergeLibraries(Path source, Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        Path normalizedTarget = target.toAbsolutePath().normalize();
        try (Stream<Path> files = Files.walk(source)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) {
                Path destination = normalizedTarget.resolve(source.relativize(file)).normalize();
                if (!destination.startsWith(normalizedTarget)) {
                    throw new IOException("OptiFine 库路径越界: " + file);
                }
                Files.createDirectories(destination.getParent());
                if (Files.isRegularFile(destination)
                        && Files.size(destination) == Files.size(file)) {
                    continue;
                }
                Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void removeModsToml(Path optifineLibraries) throws IOException {
        if (!Files.isDirectory(optifineLibraries)) {
            return;
        }
        try (Stream<Path> files = Files.walk(optifineLibraries)) {
            for (Path jar : files.filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .toList()) {
                removeZipEntry(jar, "META-INF/mods.toml");
            }
        }
    }

    private static void removeZipEntry(Path jar, String entryName) throws IOException {
        Path temporary = Files.createTempFile(jar.getParent(), "caml-optifine-lib-", ".jar");
        try (ZipInputStream input = new ZipInputStream(Files.newInputStream(jar));
             ZipOutputStream output = new ZipOutputStream(Files.newOutputStream(temporary))) {
            ZipEntry entry;
            while ((entry = input.getNextEntry()) != null) {
                if (entryName.equals(entry.getName())) {
                    continue;
                }
                output.putNextEntry(new ZipEntry(entry.getName()));
                input.transferTo(output);
                output.closeEntry();
            }
        }
        move(temporary, jar);
    }

    private static void move(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void captureOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (output) {
                    if (output.length() > 64 * 1024) {
                        output.delete(0, output.length() - 64 * 1024);
                    }
                    output.append(line).append('\n');
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void deleteRecursively(Path root) {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        } catch (IOException ignored) {
        }
    }

    private record OptiFineVersionFiles(Path file, JsonObject json) {
    }
}
