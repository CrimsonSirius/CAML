package org.chs.camlauncher.backend.system;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceInfoServiceTest {

    @Test
    void readsX86ProcessorName() {
        String cpu = DeviceInfoService.parseCpuInfo(List.of(
                "processor\t: 0",
                "model name\t: AMD Ryzen 9 7940HS",
                "cpu MHz\t\t: 3200.000"
        ));

        assertEquals("AMD Ryzen 9 7940HS", cpu);
    }

    @Test
    void readsArmProcessorName() {
        String cpu = DeviceInfoService.parseCpuInfo(List.of(
                "processor\t: 0",
                "Hardware\t: Apple M2 Pro"
        ));

        assertEquals("Apple M2 Pro", cpu);
    }

    @Test
    void readsIntegratedAndDiscreteGpus() {
        List<String> gpus = DeviceInfoService.parseLspciGpus(
                "00:02.0 VGA compatible controller: Intel Corporation Raptor Lake-P [Iris Xe Graphics] (rev 04)\n"
                        + "01:00.0 3D controller: NVIDIA Corporation AD107M [GeForce RTX 4060 Max-Q] (rev a1)\n"
                        + "00:1f.0 ISA bridge: Intel Corporation Device 519d"
        );

        assertEquals(List.of(
                "Intel Corporation Raptor Lake-P [Iris Xe Graphics]",
                "NVIDIA Corporation AD107M [GeForce RTX 4060 Max-Q]"
        ), gpus);
    }

    @Test
    void readsNpuFromPciClass() {
        List<String> npus = DeviceInfoService.parseLspciNpus(
                "00:0b.0 Processing accelerators: Intel Corporation Meteor Lake NPU"
        );

        assertEquals(List.of("Intel Corporation Meteor Lake NPU"), npus);
    }

    @Test
    void parsesWindowsPowerShellOutput() {
        DeviceInfo info = DeviceInfoService.parseWindowsOutput(
                "CPU=AMD Ryzen 9 7940HS\n"
                        + "GPU=AMD Radeon 780M;NVIDIA GeForce RTX 4070 Laptop GPU\n"
                        + "NPU=Intel AI Boost NPU\n",
                32768
        );

        assertEquals("AMD Ryzen 9 7940HS", info.cpu());
        assertEquals(List.of("AMD Radeon 780M", "NVIDIA GeForce RTX 4070 Laptop GPU"), info.gpus());
        assertEquals(List.of("Intel AI Boost NPU"), info.npus());
        assertEquals(32768, info.totalMemoryMb());
        assertTrue(info.gpus().size() > 1, "核显与独显都应保留");
    }
}
