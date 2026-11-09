package org.chs.camlauncher.backend.io;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Objects;

/** 将元数据中的相对路径限制在指定目录内。 */
public final class SafePaths {
    private SafePaths() {
    }

    /** 解析允许嵌套的相对路径，并拒绝越界路径。 */
    public static Path resolveUnder(Path root, String relative, String label) {
        Objects.requireNonNull(root, "root");
        if (relative == null || relative.isBlank()) {
            throw new IllegalArgumentException(label + " 不能为空");
        }
        Path value;
        try {
            value = Path.of(relative);
        } catch (InvalidPathException e) {
            throw new IllegalArgumentException(label + " 是非法路径: " + relative, e);
        }
        if (value.isAbsolute()) {
            throw new IllegalArgumentException(label + " 不能是绝对路径: " + relative);
        }

        Path base = root.toAbsolutePath().normalize();
        Path target = base.resolve(value).normalize();
        if (target.equals(base) || !target.startsWith(base)) {
            throw new IllegalArgumentException(label + " 越出目录边界: " + relative);
        }
        return target;
    }

    /** 解析单个文件名/ID，不允许目录分隔符。 */
    public static Path resolveChild(Path root, String name, String label) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(label + " 不能为空");
        }
        if (name.equals(".") || name.equals("..")
                || name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || name.indexOf('\0') >= 0) {
            throw new IllegalArgumentException(label + " 不能包含目录分隔符: " + name);
        }
        return resolveUnder(root, name, label);
    }
}
