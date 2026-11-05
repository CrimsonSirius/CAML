package org.chs.camlauncher.ui.control;

import javafx.scene.control.CheckBox;

/** CAML 复选框。 */
public final class CamlCheckBox extends CheckBox {
    public CamlCheckBox() {
        getStyleClass().add("caml-check-box");
    }

    public CamlCheckBox(String text) {
        super(text);
        getStyleClass().add("caml-check-box");
    }
}
