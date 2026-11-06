package org.chs.camlauncher.ui.control;

import javafx.scene.control.RadioButton;

/** CAML 单选框。 */
public final class CamlRadioButton extends RadioButton {
    public CamlRadioButton() {
        getStyleClass().add("caml-radio-button");
    }

    public CamlRadioButton(String text) {
        super(text);
        getStyleClass().add("caml-radio-button");
    }
}
