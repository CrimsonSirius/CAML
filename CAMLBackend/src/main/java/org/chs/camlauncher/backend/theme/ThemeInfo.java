package org.chs.camlauncher.backend.theme;

import java.nio.file.Path;
import java.util.Objects;

/**
 * 一个可加载的外部主题。
 *
 * @param id   文件名去掉 {@code .css} 后的标识
 * @param file 主题 CSS 文件
 */
public record ThemeInfo(String id, Path file) {
    public ThemeInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(file, "file");
        if (id.isBlank()) {
            throw new IllegalArgumentException("theme id must not be blank");
        }
    }
}
