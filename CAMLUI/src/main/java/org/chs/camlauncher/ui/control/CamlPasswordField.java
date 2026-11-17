package org.chs.camlauncher.ui.control;

import javafx.scene.control.PasswordField;

/**
 * CAML 密码输入框。
 *
 * <p>复用 {@code caml-text-field} 的结构样式，因此颜色仍由外部主题文件提供，
 * 组件库不新增任何固定配色。</p>
 */
public final class CamlPasswordField extends PasswordField {
    public CamlPasswordField() {
        getStyleClass().addAll("caml-text-field", "caml-password-field");
    }

    public CamlPasswordField(String prompt) {
        this();
        setPromptText(prompt);
    }
}

