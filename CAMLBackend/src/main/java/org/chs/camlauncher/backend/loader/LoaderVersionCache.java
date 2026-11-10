package org.chs.camlauncher.backend.loader;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** 加载器版本列表的内存缓存；下载源变化时整体失效。 */
public final class LoaderVersionCache {
    public static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    public enum Kind {
        FABRIC_LOADER,
        FABRIC_API,
        FORGE,
        NEOFORGE,
        OPTIFINE
    }

    private final Duration ttl;
    private final Clock clock;
    private final ConcurrentHashMap<Key, Entry<?>> entries = new ConcurrentHashMap<>();
    private final AtomicLong generation = new AtomicLong();

    public LoaderVersionCache() {
        this(DEFAULT_TTL, Clock.systemUTC());
    }

    LoaderVersionCache(Duration ttl, Clock clock) {
        this.ttl = Objects.requireNonNull(ttl, "ttl");
        this.clock = Objects.requireNonNull(clock, "clock");
        if (ttl.isNegative()) {
            throw new IllegalArgumentException("ttl must not be negative");
        }
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> get(Kind kind, String gameVersion, LoaderSupplier<T> supplier)
            throws IOException, InterruptedException {
        Key key = new Key(Objects.requireNonNull(kind, "kind"),
                Objects.requireNonNull(gameVersion, "gameVersion"));
        long currentGeneration = generation.get();
        Instant now = clock.instant();
        Entry<?> cached = entries.get(key);
        if (cached != null && now.isBefore(cached.expiresAt())) {
            return (List<T>) cached.versions();
        }

        List<T> versions = List.copyOf(Objects.requireNonNull(supplier.get(), "versions"));
        if (generation.get() == currentGeneration) {
            entries.put(key, new Entry<>(versions, clock.instant().plus(ttl)));
        }
        return versions;
    }

    public void invalidateAll() {
        generation.incrementAndGet();
        entries.clear();
    }

    @FunctionalInterface
    public interface LoaderSupplier<T> {
        List<T> get() throws IOException, InterruptedException;
    }

    private record Key(Kind kind, String gameVersion) {
    }

    private record Entry<T>(List<T> versions, Instant expiresAt) {
    }
}
