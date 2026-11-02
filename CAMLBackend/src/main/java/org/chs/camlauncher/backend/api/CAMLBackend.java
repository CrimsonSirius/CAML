package org.chs.camlauncher.backend.api;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.config.BackendConfig;
import org.chs.camlauncher.backend.config.BackendPaths;
import org.chs.camlauncher.backend.json.JsonService;
import org.chs.camlauncher.backend.metadata.MetadataService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;

public final class CAMLBackend implements AutoCloseable {
    public static final String VERSION = "1.0-SNAPSHOT";

    private static final Logger LOGGER = LogManager.getLogger(CAMLBackend.class);

    private final BackendConfig config;
    private final JsonService json;
    private final MetadataService metadata;
    private boolean started;

    public CAMLBackend(BackendConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.json = new JsonService();
        this.metadata = new MetadataService();
    }

    public void start() {
        if (started) {
            return;
        }

        try {
            BackendPaths.ensure(config);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to prepare CAML directories", e);
        }

        started = true;
        LOGGER.info("CAML backend {} started", VERSION);
        LOGGER.info("Data directory: {}", config.dataDirectory());
        LOGGER.info("Game directory: {}", config.gameDirectory());
    }

    public BackendStatus status() {
        return new BackendStatus(started, VERSION, config.dataDirectory(), config.gameDirectory());
    }

    public JsonService json() {
        return json;
    }

    public MetadataService metadata() {
        return metadata;
    }

    @Override
    public void close() {
        if (started) {
            started = false;
            LOGGER.info("CAML backend stopped");
        }
    }
}
