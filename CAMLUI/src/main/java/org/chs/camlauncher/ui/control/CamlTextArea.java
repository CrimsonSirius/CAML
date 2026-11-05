package org.chs.camlauncher.ui.control;

import javafx.scene.control.TextArea;

/** CAML 多行文本框。 */
public final class CamlTextArea extends TextArea {
    public CamlTextArea() {
        getStyleClass().add("caml-text-area");
    }

    public CamlTextArea(String text) {
        this();
        setText(text);
    }
}
