package org.chs.camlauncher.backend.system;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/** 读取 CPU、GPU、NPU 和运行内存总量。 */
public final class DeviceInfoService {
    private static final long COMMAND_TIMEOUT_SECONDS = 3;

    private final SystemMemoryService memoryService;

    public DeviceInfoService() {
        this(new SystemMemoryService());
    }

    DeviceInfoService(SystemMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    public DeviceInfo read() {
        long totalMemoryMb = memoryService.read().totalMb();
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("windows")) {
            return readWindows(totalMemoryMb);
        }
        if (osName.contains("linux")) {
            return readLinux(totalMemoryMb);
        }
        if (osName.contains("mac")) {
            return readMac(totalMemoryMb);
        }
        return new DeviceInfo(cpuFallback(), List.of(), List.of(), totalMemoryMb);
    }

    private static DeviceInfo readLinux(long totalMemoryMb) {
        String cpu = parseCpuInfo(readLines(Path.of("/proc/cpuinfo")));
        String lspci = run(List.of("lspci"));
        return new DeviceInfo(
                cpu.isBlank() ? cpuFallback() : cpu,
                parseLspciGpus(lspci),
                parseLspciNpus(lspci),
                totalMemoryMb
        );
    }

    private static DeviceInfo readWindows(long totalMemoryMb) {
        String output = run(List.of(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-Command",
                "$OutputEncoding=[Console]::OutputEncoding=[Text.Encoding]::UTF8; "
                        + "'CPU=' + ((Get-CimInstance Win32_Processor | Select-Object -First 1 "
                        + "-ExpandProperty Name) -join ';') + [Environment]::NewLine + "
                        + "'GPU=' + ((Get-CimInstance Win32_VideoController "
                        + "| Select-Object -ExpandProperty Name) -join ';') + [Environment]::NewLine + "
                        + "'NPU=' + ((Get-CimInstance Win32_PnPEntity | Where-Object { "
                        + "$_.PNPClass -eq 'ComputeAccelerator' -or $_.Name -match 'NPU|Neural' "
                        + "} | Select-Object -ExpandProperty Name) -join ';')"
        ));
        return parseWindowsOutput(output, totalMemoryMb);
    }

    private static DeviceInfo readMac(long totalMemoryMb) {
        String cpu = run(List.of("sysctl", "-n", "machdep.cpu.brand_string"));
        String displays = run(List.of("system_profiler", "SPDisplaysDataType"));
        return new DeviceInfo(
                cpu.isBlank() ? cpuFallback() : cpu,
                parseMacDisplays(displays),
                List.of(),
                totalMemoryMb
        );
    }

    static String parseCpuInfo(List<String> lines) {
        String modelName = "";
        String cpuModel = "";
        String hardware = "";
        String processor = "";
        for (String line : lines) {
            int separator = line == null ? -1 : line.indexOf(':');
            if (separator <= 0) {
                continue;
            }
            String key = line.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separator + 1).trim();
            if (value.isBlank()) {
                continue;
            }
            switch (key) {
                case "model name" -> modelName = value;
                case "cpu model" -> cpuModel = value;
                case "hardware" -> hardware = value;
                case "processor" -> {
                    if (!value.chars().allMatch(Character::isDigit)) {
                        processor = value;
                    }
                }
                default -> {
                }
            }
        }
        if (!modelName.isBlank()) {
            return modelName;
        }
        if (!cpuModel.isBlank()) {
            return cpuModel;
        }
        if (!hardware.isBlank()) {
            return hardware;
        }
        return processor;
    }

    static List<String> parseLspciGpus(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String line : output.lines().toList()) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("vga compatible controller")
                    || lower.contains("3d controller")
                    || lower.contains("display controller")) {
                addLspciName(names, line);
            }
        }
        return List.copyOf(names);
    }

    static List<String> parseLspciNpus(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String line : output.lines().toList()) {
            String lower = line.toLowerCase(Locale.ROOT);
            if (lower.contains("processing accelerator")
                    || lower.contains("npu")
                    || lower.contains("neural")) {
                addLspciName(names, line);
            }
        }
        return List.copyOf(names);
    }

    static DeviceInfo parseWindowsOutput(String output, long totalMemoryMb) {
        String cpu = "";
        String gpu = "";
        String npu = "";
        if (output != null) {
            for (String rawLine : output.lines().toList()) {
                String line = rawLine.startsWith("\uFEFF") ? rawLine.substring(1) : rawLine;
                if (line.startsWith("CPU=")) {
                    cpu = line.substring(4).trim();
                } else if (line.startsWith("GPU=")) {
                    gpu = line.substring(4).trim();
                } else if (line.startsWith("NPU=")) {
                    npu = line.substring(4).trim();
                }
            }
        }
        return new DeviceInfo(
                cpu.isBlank() ? cpuFallback() : cpu,
                splitValues(gpu),
                splitValues(npu),
                totalMemoryMb
        );
    }

    static List<String> parseMacDisplays(String output) {
        if (output == null || output.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> names = new LinkedHashSet<>();
        for (String line : output.lines().toList()) {
            String trimmed = line.trim();
            if (trimmed.startsWith("Chipset Model:")) {
                String name = trimmed.substring("Chipset Model:".length()).trim();
                if (!name.isBlank()) {
                    names.add(name);
                }
            }
        }
        return List.copyOf(names);
    }

    private static void addLspciName(LinkedHashSet<String> names, String line) {
        int separator = line.indexOf(": ");
        if (separator < 0) {
            return;
        }
        String name = line.substring(separator + 2).trim()
                .replaceFirst("\\s+\\(rev [^)]+\\)$", "");
        if (!name.isBlank()) {
            names.add(name);
        }
    }

    private static List<String> splitValues(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(";"))
                .map(String::trim)
                .filter(part -> !part.isBlank())
                .distinct()
                .toList();
    }

    private static List<String> readLines(Path path) {
        try {
            return Files.readAllLines(path);
        } catch (IOException | RuntimeException ignored) {
            return List.of();
        }
    }

    private static String cpuFallback() {
        String processor = System.getenv("PROCESSOR_IDENTIFIER");
        return processor == null || processor.isBlank()
                ? System.getProperty("os.arch", "")
                : processor;
    }

    private static String run(List<String> command) {
        Process process = null;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.redirectErrorStream(true);
            builder.environment().putIfAbsent("LANG", "C");
            process = builder.start();
            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return "";
            }
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            return "";
        } catch (IOException | RuntimeException ignored) {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            return "";
        }
    }
}
