package org.chs.camlauncher.backend.launch;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.chs.camlauncher.backend.download.plan.AssetDownloadPlanner;
import org.chs.camlauncher.backend.download.DownloadListener;
import org.chs.camlauncher.backend.download.plan.DownloadPlanner;
import org.chs.camlauncher.backend.download.FileDownloader;
import org.chs.camlauncher.backend.download.hash.HashVerifier;
import org.chs.camlauncher.backend.instance.VersionIsolationMode;
import org.chs.camlauncher.backend.instance.VersionInstaller;
import org.chs.camlauncher.backend.launch.classpath.ClasspathBuilder;
import org.chs.camlauncher.backend.launch.prepare.NativeExtractor;
import org.chs.camlauncher.backend.launch.prepare.VirtualAssetInstaller;
import org.chs.camlauncher.backend.launch.process.ProcessLauncher;
import org.chs.camlauncher.backend.launch.process.ProcessListener;
import org.chs.camlauncher.backend.metadata.MetadataService;
import org.chs.camlauncher.backend.metadata.VersionManifestService;
import org.chs.camlauncher.backend.runtime.JavaLocator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameLaunchServiceTest {
    @TempDir
    Path rootDirectory;

    @Test
    void installsAndLaunchesFakeClientEndToEnd() throws Exception {
        Path clientJar = rootDirectory.resolve("client.jar");
        createClientJar(clientJar);
        String clientSha1 = HashVerifier.sha1(clientJar);
        long clientSize = Files.size(clientJar);

        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        int port = server.getAddress().getPort();
        String baseUrl = "http://127.0.0.1:" + port;
        String manifestJson = """
                {
                  "latest": {"release": "test-version", "snapshot": "test-version"},
                  "versions": [
                    {
                      "id": "test-version",
                      "type": "release",
                      "url": "%s/version.json"
                    }
                  ]
                }
                """.formatted(baseUrl);
        String versionJson = """
                {
                  "id": "test-version",
                  "type": "release",
                  "mainClass": "org.chs.camlauncher.backend.launch.FakeGameMain",
                  "downloads": {
                    "client": {
                      "sha1": "%s",
                      "size": %d,
                      "url": "%s/client.jar"
                    }
                  }
                }
                """.formatted(clientSha1, clientSize, baseUrl);

        server.createContext("/manifest.json",
                exchange -> respond(exchange, manifestJson.getBytes(StandardCharsets.UTF_8)));
        server.createContext("/version.json",
                exchange -> respond(exchange, versionJson.getBytes(StandardCharsets.UTF_8)));
        server.createContext("/client.jar",
                exchange -> respond(exchange, Files.readAllBytes(clientJar)));
        server.start();

        Path gameDirectory = rootDirectory.resolve("game");
        StringBuffer output = new StringBuffer();
        CountDownLatch exited = new CountDownLatch(1);
        ProcessListener listener = new ProcessListener() {
            @Override
            public void onLog(String line, boolean error) {
                output.append(line).append('\n');
            }

            @Override
            public void onExit(int exitCode) {
                exited.countDown();
            }
        };

        try {
            GameLaunchService service = new GameLaunchService(
                    gameDirectory,
                    URI.create(baseUrl + "/manifest.json"),
                    rootDirectory.resolve("cache/version_manifest_v2.json"),
                    new VersionInstaller(
                            new MetadataService(),
                            new VersionManifestService(),
                            new DownloadPlanner(),
                            new AssetDownloadPlanner(),
                            new FileDownloader()
                    ),
                    new JavaLocator(),
                    new LaunchPlanner(),
                    new ClasspathBuilder(),
                    new NativeExtractor(),
                    new VirtualAssetInstaller(),
                    new ProcessLauncher()
            );

            GameLaunch launch = service.launch(
                    "test-version",
                    gameDirectory,
                    "Steve",
                    LaunchOptions.defaults(),
                    VersionIsolationMode.FULL,
                    DownloadListener.NONE,
                    listener
            );

            assertTrue(launch.process().waitFor(15, TimeUnit.SECONDS));
            assertEquals(5, launch.process().exitValue());
            assertEquals(
                    gameDirectory.resolve("versions").resolve("test-version"),
                    launch.plan().gameDirectory()
            );
            assertTrue(output.toString().contains(
                    "fake-game-user-dir=" + gameDirectory.resolve("versions/test-version")));
            assertTrue(exited.await(5, TimeUnit.SECONDS));
            assertTrue(output.toString().contains("fake-game-hello"));
            launch.process().close();
        } finally {
            server.stop(0);
        }
    }

    private static void createClientJar(Path jar) throws IOException {
        String entryName = FakeGameMain.class.getName().replace('.', '/') + ".class";
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(jar))) {
            output.putNextEntry(new JarEntry(entryName));
            try (InputStream input = FakeGameMain.class.getResourceAsStream("FakeGameMain.class")) {
                if (input == null) {
                    throw new IOException("Missing FakeGameMain.class");
                }
                input.transferTo(output);
            }
            output.closeEntry();
        }
    }

    private static void respond(HttpExchange exchange, byte[] content) throws IOException {
        exchange.sendResponseHeaders(200, content.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(content);
        }
    }
}
