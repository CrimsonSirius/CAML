package org.chs.camlauncher.backend.launch;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

final class ClasspathManifest {
    private ClasspathManifest() {
    }

    static Path create(List<Path> classpath) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(
                Attributes.Name.CLASS_PATH,
                classpath.stream()
                        .map(path -> path.toAbsolutePath().normalize().toUri().toASCIIString())
                        .collect(Collectors.joining(" "))
        );

        Path jar = Files.createTempFile("caml-classpath-", ".jar");
        try (OutputStream output = Files.newOutputStream(jar);
             JarOutputStream ignored = new JarOutputStream(output, manifest)) {
            // The manifest itself is the complete content of this jar.
        }
        return jar;
    }
}
