package org.chs.camlauncher.backend.system;

/** 系统物理内存快照，单位 MB。 */
public record SystemMemoryInfo(long totalMb, long availableMb) {
    public SystemMemoryInfo {
        if (totalMb < 0 || availableMb < 0) {
            throw new IllegalArgumentException("内存大小不能为负数");
        }
    }
}
