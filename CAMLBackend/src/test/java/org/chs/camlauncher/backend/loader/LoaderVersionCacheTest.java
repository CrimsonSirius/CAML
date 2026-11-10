package org.chs.camlauncher.backend.loader;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LoaderVersionCacheTest {
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-11-10T00:00:00Z"), ZoneOffset.UTC);

    @Test
    void reusesCachedVersionsForSameLoaderAndGameVersion() throws Exception {
        LoaderVersionCache cache = new LoaderVersionCache(Duration.ofMinutes(5), CLOCK);
        AtomicInteger calls = new AtomicInteger();

        List<String> first = cache.get(LoaderVersionCache.Kind.FORGE, "1.21.1", () -> {
            calls.incrementAndGet();
            return List.of("47.4.0");
        });
        List<String> second = cache.get(LoaderVersionCache.Kind.FORGE, "1.21.1", () -> {
            calls.incrementAndGet();
            return List.of("47.4.0");
        });

        assertEquals(List.of("47.4.0"), first);
        assertEquals(first, second);
        assertEquals(1, calls.get());
    }

    @Test
    void separatesLoaderKindsAndGameVersions() throws Exception {
        LoaderVersionCache cache = new LoaderVersionCache(Duration.ofMinutes(5), CLOCK);
        AtomicInteger calls = new AtomicInteger();

        cache.get(LoaderVersionCache.Kind.FORGE, "1.21.1", () -> {
            calls.incrementAndGet();
            return List.of("forge");
        });
        cache.get(LoaderVersionCache.Kind.NEOFORGE, "1.21.1", () -> {
            calls.incrementAndGet();
            return List.of("neoforge");
        });
        cache.get(LoaderVersionCache.Kind.FORGE, "1.20.1", () -> {
            calls.incrementAndGet();
            return List.of("old-forge");
        });

        assertEquals(3, calls.get());
    }

    @Test
    void invalidateAllForcesNextFetch() throws Exception {
        LoaderVersionCache cache = new LoaderVersionCache(Duration.ofMinutes(5), CLOCK);
        AtomicInteger calls = new AtomicInteger();

        cache.get(LoaderVersionCache.Kind.OPTIFINE, "1.20.1", () -> {
            calls.incrementAndGet();
            return List.of("HD_U_I6");
        });
        cache.invalidateAll();
        cache.get(LoaderVersionCache.Kind.OPTIFINE, "1.20.1", () -> {
            calls.incrementAndGet();
            return List.of("HD_U_I7");
        });

        assertEquals(2, calls.get());
    }

    @Test
    void expiresEntriesAfterTtl() throws Exception {
        LoaderVersionCache cache = new LoaderVersionCache(Duration.ZERO, CLOCK);
        AtomicInteger calls = new AtomicInteger();

        cache.get(LoaderVersionCache.Kind.FABRIC_LOADER, "1.21.1", () -> {
            calls.incrementAndGet();
            return List.of("0.16.0");
        });
        cache.get(LoaderVersionCache.Kind.FABRIC_LOADER, "1.21.1", () -> {
            calls.incrementAndGet();
            return List.of("0.16.1");
        });

        assertEquals(2, calls.get());
    }
}
