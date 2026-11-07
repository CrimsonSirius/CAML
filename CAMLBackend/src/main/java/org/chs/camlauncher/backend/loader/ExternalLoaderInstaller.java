package org.chs.camlauncher.backend.loader;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.chs.camlauncher.backend.download.DownloadItem;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.instance.InstalledVersion;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.chs.camlauncher.backend.runtime.JavaRequirement;
import org.chs.camlauncher.backend.runtime.JavaRuntime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

/** 调用 Forge/NeoForge 官方 installer 的 headless client 安装。 */
public final class ExternalLoaderInstaller {
    private static final long INSTALL_TIMEOUT_MINUTES = 20;
    private static final int OUTPUT_LIMIT = 64 * 1024;

    private final VersionInstaller versionInstaller;
    private final JavaLocator javaLocator;
    private final FileDownloader fileDownloader;
    private final MetadataService metadata;
    private final URI manifestUri;
    private final Path manifestCacheFile;

    public ExternalLoaderInstaller(
            VersionInstaller versionInstaller,
            JavaLocator javaLocator,
            FileDownloader fileDownloader,
            MetadataService metadata,
            URI manifestUri,
            Path manifestCacheFile
    ) {
        this.versionInstaller = Objects.requireNonNull(versionInstaller, "versionInstaller");
        this.javaLocator = Objects.requireNonNull(javaLocator, "javaLocator");
        this.fileDownloader = Objects.requireNonNull(fileDownloader, "fileDownloader");
        this.metadata = Objects.requireNonNull(metadata, "metadata");
        this.manifestUri = Objects.requireNonNull(manifestUri, "manifestUri");
        this.manifestCacheFile = Objects.requireNonNull(manifestCacheFile, "manifestCacheFile");
    }

    public InstalledVersion install(
            String loaderName,
            String gameVersion,
            String instanceId,
            URI installerUri,
            Path gameDirectory,
            DownloadListener listener
    ) throws IOException, InterruptedException {
        Objects.requireNonNull(loaderName, "loaderName");
        Objects.requireNonNull(gameVersion, "gameVersion");
        Objects.requireNonNull(installerUri, "installerUri");
        Objects.requireNonNull(gameDirectory, "gameDirectory");
        String normalizedInstanceId = validateInstanceId(instanceId);
        if (normalizedInstanceId.equals(gameVersion)) {
            throw new IllegalArgumentException(
                    "安装 " + loaderName + " 时实例名不能与游戏版本号相同");
        }

        Path targetVersionDirectory = gameDirectory
                .resolve("versions")
                .resolve(normalizedInstanceId)
                .normalize();
        if (Files.exists(targetVersionDirectory)) {
            throw new IOException("实例目录已存在: " + targetVersionDirectory);
        }

        InstalledVersion base = versionInstaller.install(
                gameVersion,
                gameVersion,
                gameDirectory,
                manifestUri,
                manifestCacheFile,
                listener
        );
        JavaRuntime java = javaLocator.findSuitable(JavaRequirement.from(base.manifest()))
                .orElseThrow(() -> new IOException(
                        "找不到可用于运行 " + loaderName + " 安装器的 Java"));

        Path staging = Files.createTempDirectory(
                gameDirectory,
                ".caml-" + loaderName.toLowerCase() + "-install-"
        );
        try {
            prepareStaging(staging, gameVersion, base);
            Path installerJar = staging.resolve("installer.jar");
            fileDownloader.download(new DownloadItem(
                    loaderName + " installer",
                    List.of(installerUri),
                    installerJar,
                    null,
                    -1
            ), listener);

            String installedVersionId = readInstallerVersionId(installerJar);
            runInstaller(loaderName, java, installerJar, staging);

            Path installedVersionDirectory = staging.resolve("versions").resolve(installedVersionId);
            if (!Files.isDirectory(installedVersionDirectory)) {
                throw new IOException(
                        loaderName + " 安装器没有生成版本目录: " + installedVersionDirectory);
            }

            mergeLibraries(staging.resolve("libraries"), gameDirectory.resolve("libraries"));
            moveVersionDirectory(
                    installedVersionDirectory,
                    targetVersionDirectory,
                    installedVersionId,
                    normalizedInstanceId
            );
        } finally {
            deleteRecursively(staging);
        }

        return versionInstaller.install(
                normalizedInstanceId,
                gameDirectory,
                manifestUri,
                manifestCacheFile,
                listener
        );
    }

    private static void prepareStaging(Path staging, String gameVersion, InstalledVersion base)
            throws IOException {
        Path stagedVersionDirectory = staging.resolve("versions").resolve(gameVersion);
        Files.createDirectories(stagedVersionDirectory);
        Files.copy(base.versionJson(),
                stagedVersionDirectory.resolve(gameVersion + ".json"),
                StandardCopyOption.REPLACE_EXISTING);
        if (Files.isRegularFile(base.clientJar())) {
            Files.copy(base.clientJar(),
                    stagedVersionDirectory.resolve(gameVersion + ".jar"),
                    StandardCopyOption.REPLACE_EXISTING);
        }
        Files.writeString(
                staging.resolve("launcher_profiles.json"),
                "{\"profiles\":{},\"settings\":{}}\n",
                StandardCharsets.UTF_8
        );
    }

    private static void runInstaller(
            String loaderName,
            JavaRuntime java,
            Path installerJar,
            Path staging
    ) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(java.executable().toString());
        command.add("-Djava.awt.headless=true");
        command.add("-jar");
        command.add(installerJar.toString());
        command.add("--installClient");
        command.add(staging.toString());

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(staging.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();

        StringBuilder output = new StringBuilder();
        Thread reader = new Thread(() -> captureOutput(process, output),
                "caml-" + loaderName.toLowerCase() + "-installer-output");
        reader.setDaemon(true);
        reader.start();

        boolean finished;
        try {
            finished = process.waitFor(INSTALL_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            process.destroyForcibly();
            throw e;
        }
        if (!finished) {
            process.destroyForcibly();
            throw new IOException(loaderName + " 安装器超时");
        }
        reader.join(5000);

        if (process.exitValue() != 0) {
            throw new IOException(loaderName + " 安装器失败，退出码 "
                    + process.exitValue() + "\n" + tail(output));
        }
    }

    private static void captureOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                synchronized (output) {
                    output.append(line).append('\n');
                    if (output.length() > OUTPUT_LIMIT) {
                        output.delete(0, output.length() - OUTPUT_LIMIT);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static String tail(StringBuilder output) {
        synchronized (output) {
            return output.toString();
        }
    }

    private static String readInstallerVersionId(Path installerJar) throws IOException {
        try (ZipFile zip = new ZipFile(installerJar.toFile())) {
            var versionEntry = zip.getEntry("version.json");
            if (versionEntry != null) {
                try (var input = zip.getInputStream(versionEntry)) {
                    String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    return new MetadataService().parseVersionInfo(text).id();
                }
            }

            var profileEntry = zip.getEntry("install_profile.json");
            if (profileEntry != null) {
                try (var input = zip.getInputStream(profileEntry)) {
                    JsonObject profile = JsonParser.parseString(
                            new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    ).getAsJsonObject();
                    if (profile.has("version") && !profile.get("version").isJsonNull()) {
                        return profile.get("version").getAsString();
                    }
                }
            }
        }
        throw new IOException("安装器缺少 version.json 或 install_profile.version: " + installerJar);
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
                    throw new IOException("安装器库路径越界: " + file);
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

    private static void moveVersionDirectory(
            Path source,
            Path target,
            String installedVersionId,
            String instanceId
    ) throws IOException {
        Files.createDirectories(target.getParent());
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }

        if (!installedVersionId.equals(instanceId)) {
            Path json = target.resolve(installedVersionId + ".json");
            if (!Files.isRegularFile(json)) {
                throw new IOException("安装器生成的版本 JSON 不存在: " + json);
            }
            Files.move(json, target.resolve(instanceId + ".json"),
                    StandardCopyOption.REPLACE_EXISTING);

            Path jar = target.resolve(installedVersionId + ".jar");
            if (Files.isRegularFile(jar)) {
                Files.move(jar, target.resolve(instanceId + ".jar"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
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

    private static String validateInstanceId(String instanceId) {
        String value = Objects.requireNonNull(instanceId, "instanceId").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("实例名字不能为空");
        }
        if (value.equals(".") || value.equals("..")) {
            throw new IllegalArgumentException("实例名字不能是 . 或 ..");
        }
        if (value.endsWith(".") || value.endsWith(" ")) {
            throw new IllegalArgumentException("实例名字不能以点或空格结尾");
        }
        String invalidChars = "<>:\"/\\|?*";
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character) || invalidChars.indexOf(character) >= 0) {
                throw new IllegalArgumentException("实例名字包含非法字符: " + character);
            }
        }
        return value;
    }
}
