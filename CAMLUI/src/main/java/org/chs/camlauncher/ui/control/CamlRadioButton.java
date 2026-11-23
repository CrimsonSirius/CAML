package org.chs.camlauncher.ui.control;

import javafx.scene.control.RadioButton;

/** CAML 单选框。 */
public final class CamlRadioButton extends RadioButton {
    public CamlRadioButton() {
        configure();
    }

    public CamlRadioButton(String text) {
        super(text);
        configure();
    }

    private void configure() {
        getStyleClass().add("caml-radio-button");
        setFocusTraversable(false);
    }
}
