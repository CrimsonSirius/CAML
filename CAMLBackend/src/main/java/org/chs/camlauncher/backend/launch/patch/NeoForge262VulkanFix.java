package org.chs.camlauncher.backend.launch.patch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;
import org.chs.camlauncher.backend.metadata.model.VersionInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NeoForge 26.2 在 Vulkan 下的兼容修复。
 *
 * <p>NeoForge 26.2 会把用 OpenGL 创建的早期加载窗口交给 Minecraft 主窗口使用。
 * Minecraft 随后尝试在这个窗口上创建 Vulkan surface，GLFW 会报 65540。
 * 关闭 {@code earlyWindowControl} 后，Minecraft 会自行创建带
 * {@code GLFW_NO_API} 的窗口。</p>
 */
public final class NeoForge262VulkanFix {
    private static final Logger LOGGER = LogManager.getLogger(NeoForge262VulkanFix.class);
    private static final String NEOFORGE_MAIN_CLASS = "net.neoforged.fml.startup.Client";
    private static final String EARLY_WINDOW_CONTROL = "earlyWindowControl";
    private static final Pattern EARLY_WINDOW_CONTROL_LINE = Pattern.compile(
            "(?m)^([ \\t]*earlyWindowControl[ \\t]*=[ \\t]*)(true|false)([ \\t]*(?:#.*)?)$"
    );

    private NeoForge262VulkanFix() {
    }

    /**
     * 当实例是 NeoForge 26.2 且选择 Vulkan 时，关闭早期加载窗口。
     *
     * @param manifest         合并后的版本信息
     * @param gameDataDirectory 当前实例实际使用的游戏数据目录
     */
    public static void apply(VersionInfo manifest, Path gameDataDirectory) {
        Objects.requireNonNull(manifest, "manifest");
        Objects.requireNonNull(gameDataDirectory, "gameDataDirectory");
        if (!isNeoForge262(manifest)) {
            return;
        }

        try {
            if (!prefersVulkan(gameDataDirectory.resolve("options.txt"))) {
                return;
            }
            Path config = gameDataDirectory.resolve("config").resolve("fml.toml");
            if (disableEarlyWindow(config)) {
                LOGGER.info("已为 NeoForge 26.2 的 Vulkan 实例关闭早期加载窗口: {}", config);
            }
        } catch (IOException e) {
            LOGGER.warn("无法应用 NeoForge 26.2 Vulkan 兼容修复，游戏可能启动失败", e);
        }
    }

    static boolean isNeoForge262(VersionInfo manifest) {
        if (!NEOFORGE_MAIN_CLASS.equals(manifest.mainClass())) {
            return false;
        }
        List<String> args = manifest.arguments().game().stream()
                .flatMap(argument -> argument.resolve(new RuleEvaluator()).stream())
                .toList();
        String neoForgeVersion = valueAfter(args, "--fml.neoForgeVersion");
        return neoForgeVersion != null && neoForgeVersion.startsWith("26.2.");
    }

    static boolean prefersVulkan(Path optionsFile) throws IOException {
        if (!Files.isRegularFile(optionsFile)) {
            return false;
        }
        for (String line : Files.readAllLines(optionsFile, StandardCharsets.UTF_8)) {
            int separator = line.indexOf(':');
            if (separator <= 0
                    || !line.substring(0, separator).trim().equals("preferredGraphicsBackend")) {
                continue;
            }
            String value = line.substring(separator + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            return value.equalsIgnoreCase("vulkan");
        }
        return false;
    }

    static boolean disableEarlyWindow(Path configFile) throws IOException {
        String content = Files.isRegularFile(configFile)
                ? Files.readString(configFile, StandardCharsets.UTF_8)
                : "";
        Matcher matcher = EARLY_WINDOW_CONTROL_LINE.matcher(content);
        if (matcher.find()) {
            if ("false".equals(matcher.group(2))) {
                return false;
            }
            content = matcher.replaceFirst("$1false$3");
        } else {
            if (!content.isEmpty() && !content.endsWith("\n") && !content.endsWith("\r")) {
                content += System.lineSeparator();
            }
            content += EARLY_WINDOW_CONTROL + " = false" + System.lineSeparator();
        }
        Files.createDirectories(configFile.getParent());
        Files.writeString(configFile, content, StandardCharsets.UTF_8);
        return true;
    }

    private static String valueAfter(List<String> args, String flag) {
        for (int index = 0; index + 1 < args.size(); index++) {
            if (flag.equals(args.get(index))) {
                return args.get(index + 1);
            }
        }
        return null;
    }
}
