package org.chs.camlauncher.backend.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class JsonService {
    private final Gson gson;

    public JsonService() {
        this(new GsonBuilder().setPrettyPrinting().create());
    }

    public JsonService(Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson");
    }

    public <T> T read(Path file, Class<T> type) throws IOException {
        return read(file, (Type) type);
    }

    public <T> T read(Path file, Type type) throws IOException {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        }
    }

    public void write(Path file, Object value) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            gson.toJson(value, writer);
        }
    }
}
