package org.chs.camlauncher.backend.instance;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** 实例名格式和目标目录冲突的统一规则。 */
public final class InstanceNameRules {
    private InstanceNameRules() {
    }

    public static String validate(String instanceId) {
        String value = Objects.requireNonNull(instanceId, "instanceId").trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("实例名字不能为空");
        }
        if (value.equals(".") || value.equals("..")) {
            throw new IllegalArgumentException("实例名字不能是 . 或 ..");
        }
        if (value.endsWith(".") || value.endsWith(" ")) {
            throw new IllegalArgumentException("实例名字不能以点或空格结尾");
        }

        String invalidChars = "<>:\"/\\|?*";
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character) || invalidChars.indexOf(character) >= 0) {
                throw new IllegalArgumentException("实例名字包含非法字符: " + character);
            }
        }
        return value;
    }

    public static InstanceNameCheck check(Path gameDirectory, String instanceId) {
        String value;
        try {
            value = validate(instanceId);
        } catch (IllegalArgumentException e) {
            return new InstanceNameCheck(false, e.getMessage());
        }
        if (gameDirectory == null) {
            return new InstanceNameCheck(false, "尚未设置游戏位置");
        }

        Path target = gameDirectory
                .resolve("versions")
                .resolve(value)
                .toAbsolutePath()
                .normalize();
        if (Files.exists(target)) {
            return new InstanceNameCheck(false, "实例名已存在：" + value + "，请换一个名字。");
        }
        return new InstanceNameCheck(true, "");
    }

    public static void ensureNewInstance(Path gameDirectory, String instanceId) throws IOException {
        InstanceNameCheck check = check(gameDirectory, instanceId);
        if (!check.available()) {
            throw new IOException(check.message());
        }
    }
}
