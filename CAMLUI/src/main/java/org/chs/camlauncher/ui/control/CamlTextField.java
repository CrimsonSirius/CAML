package org.chs.camlauncher.ui.control;

import javafx.scene.control.TextField;

/** CAML 单行输入框。 */
public final class CamlTextField extends TextField {
    public CamlTextField() {
        getStyleClass().add("caml-text-field");
    }

    public CamlTextField(String text) {
        this();
        setText(text);
    }

    public CamlTextField(String text, String prompt) {
        this(text);
        setPromptText(prompt);
    }
}
