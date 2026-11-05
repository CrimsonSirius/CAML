package org.chs.camlauncher.backend.system;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * 读取系统物理内存。
 *
 * <p>Linux 使用 {@code /proc/meminfo} 的 {@code MemAvailable}，这是任务管理器/系统监视器
 * 所说的“可用内存”，包含可回收缓存；{@code MemFree} 只是完全空闲内存，会明显偏小。
 * 其他平台回退到 JDK 的 OperatingSystemMXBean。</p>
 */
public final class SystemMemoryService {
    private static final long KB_PER_MB = 1024;

    public SystemMemoryInfo read() {
        if (isLinux()) {
            try {
                SystemMemoryInfo linux = parseLinuxMeminfo(Files.readAllLines(Path.of("/proc/meminfo")));
                if (linux != null) {
                    return linux;
                }
            } catch (IOException | RuntimeException ignored) {
            }
        }

        try {
            java.lang.management.OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();
            if (bean instanceof com.sun.management.OperatingSystemMXBean os) {
                long total = os.getTotalMemorySize() / (1024 * 1024);
                long available = os.getFreeMemorySize() / (1024 * 1024);
                if (total > 0) {
                    return new SystemMemoryInfo(total, Math.min(total, Math.max(0, available)));
                }
            }
        } catch (RuntimeException ignored) {
        }

        long total = Math.max(1024, Runtime.getRuntime().maxMemory() / (1024 * 1024));
        long available = Math.max(0, Runtime.getRuntime().freeMemory() / (1024 * 1024));
        return new SystemMemoryInfo(total, Math.min(total, available));
    }

    static SystemMemoryInfo parseLinuxMeminfo(List<String> lines) {
        Long totalKb = null;
        Long availableKb = null;
        Long freeKb = null;
        for (String line : lines) {
            String[] parts = line.trim().split("\\s+");
            if (parts.length < 2) {
                continue;
            }
            String key = parts[0];
            long value;
            try {
                value = Long.parseLong(parts[1]);
            } catch (NumberFormatException ignored) {
                continue;
            }
            if ("MemTotal:".equals(key)) {
                totalKb = value;
            } else if ("MemAvailable:".equals(key)) {
                availableKb = value;
            } else if ("MemFree:".equals(key)) {
                freeKb = value;
            }
        }
        if (totalKb == null || totalKb <= 0) {
            return null;
        }
        long available = availableKb != null ? availableKb : (freeKb != null ? freeKb : 0);
        long totalMb = totalKb / KB_PER_MB;
        long availableMb = Math.min(totalMb, Math.max(0, available / KB_PER_MB));
        return new SystemMemoryInfo(totalMb, availableMb);
    }

    private static boolean isLinux() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("linux");
    }
}
