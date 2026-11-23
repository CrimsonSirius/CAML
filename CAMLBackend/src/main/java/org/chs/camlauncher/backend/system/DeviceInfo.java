package org.chs.camlauncher.backend.system;

import java.util.List;

/** 设备信息；列表保留多个条目，例如独显与核显。 */
public record DeviceInfo(
        String cpu,
        List<String> gpus,
        List<String> npus,
        long totalMemoryMb
) {
    public DeviceInfo {
        cpu = cpu == null ? "" : cpu;
        gpus = gpus == null ? List.of() : List.copyOf(gpus);
        npus = npus == null ? List.of() : List.copyOf(npus);
    }
}
