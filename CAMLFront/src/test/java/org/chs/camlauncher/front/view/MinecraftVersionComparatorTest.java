package org.chs.camlauncher.front.view;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftVersionComparatorTest {
    @Test
    void comparesNumericSegmentsSemantically() {
        assertTrue(MinecraftVersionComparator.compare("1.10", "1.9") > 0);
        assertTrue(MinecraftVersionComparator.compare("1.21", "1.20.6") > 0);
        assertTrue(MinecraftVersionComparator.compare("1.20.10", "1.20.9") > 0);
    }

    @Test
    void treatsPrereleasesAsOlderThanRelease() {
        assertTrue(MinecraftVersionComparator.compare("1.20.5", "1.20.5-rc1") > 0);
        assertTrue(MinecraftVersionComparator.compare("1.20.5-rc1", "1.20.5-pre2") > 0);
        assertTrue(MinecraftVersionComparator.compare("1.20.5-pre2", "1.20.5-pre1") > 0);
    }

    @Test
    void keepsCommonVersionGroupsOrdered() {
        assertTrue(MinecraftVersionComparator.compare("24w14b", "24w14a") > 0);
        assertTrue(MinecraftVersionComparator.compareGroupKey(
                "1.20.1 Fabric", "1.20.1 Forge") < 0);
        assertTrue(MinecraftVersionComparator.compareGroupKey(
                "1.20.10 Fabric", "1.20.9 Forge") > 0);
    }
}
