package org.chs.camlauncher.backend.launch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

final class ArgumentFile {
    private ArgumentFile() {
    }

    static Path write(List<String> arguments) throws IOException {
        Path file = Files.createTempFile("caml-jvm-", ".txt");
        String content = arguments.stream()
                .map(ArgumentFile::quote)
                .collect(Collectors.joining(System.lineSeparator()));
        Files.writeString(file, content + System.lineSeparator(), StandardCharsets.UTF_8);
        return file;
    }

    static String quote(String value) {
        boolean needsQuotes = value.isEmpty()
                || value.startsWith("#")
                || value.chars().anyMatch(Character::isWhitespace)
                || value.indexOf('"') >= 0
                || value.indexOf('\\') >= 0;
        if (!needsQuotes) {
            return value;
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
