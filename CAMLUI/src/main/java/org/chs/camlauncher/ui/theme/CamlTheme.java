package org.chs.camlauncher.ui.theme;

import javafx.scene.Parent;
import javafx.scene.Scene;

import java.net.URL;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

/**
 * CAML 主题引擎。
 *
 * <p>组件结构与排版由模块内置的 {@code caml.css} 提供，所有颜色令牌
 * 都来自外部主题文件。模块本身不定义任何固定配色。</p>
 */
public final class CamlTheme {
    private static final String BASE_STYLESHEET = "/org/chs/camlauncher/ui/caml.css";
    private static final String ROOT_CLASS = "caml-root";
    private static final Map<Scene, String> APPLIED_THEMES = new WeakHashMap<>();

    private CamlTheme() {
    }

    /** @return 组件结构样式表的外部 URL */
    public static String baseStylesheet() {
        URL resource = CamlTheme.class.getResource(BASE_STYLESHEET);
        if (resource == null) {
            throw new IllegalStateException("缺少 CAML 结构样式表: " + BASE_STYLESHEET);
        }
        return resource.toExternalForm();
    }

    /** 安装结构样式并标记主题根节点。 */
    public static void install(Scene scene) {
        Objects.requireNonNull(scene, "scene");
        String base = baseStylesheet();
        if (!scene.getStylesheets().contains(base)) {
            scene.getStylesheets().add(base);
        }
        markRoot(scene.getRoot());
    }

    /** 给即将挂到场景上的主题根节点添加 {@code caml-root} 标记。 */
    public static void markRoot(Parent root) {
        if (root != null && !root.getStyleClass().contains(ROOT_CLASS)) {
            root.getStyleClass().add(ROOT_CLASS);
        }
    }

    /**
     * 从外部 CSS 文件加载主题，替换当前主题但保留结构样式。
     *
     * @param scene      JavaFX 场景
     * @param themeFile  {@code <data>/themes/*.css}
     */
    public static void applyTheme(Scene scene, Path themeFile) {
        Objects.requireNonNull(scene, "scene");
        Objects.requireNonNull(themeFile, "themeFile");

        install(scene);
        String stylesheet = themeFile.toAbsolutePath().normalize().toUri().toString();
        String previous = APPLIED_THEMES.put(scene, stylesheet);
        if (previous != null && !previous.equals(stylesheet)) {
            scene.getStylesheets().remove(previous);
        }
        if (!scene.getStylesheets().contains(stylesheet)) {
            scene.getStylesheets().add(stylesheet);
        }
    }
}
