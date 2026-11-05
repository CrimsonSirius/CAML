package org.chs.camlauncher.ui.control;

import javafx.scene.control.Slider;

/** CAML 滑块。 */
public final class CamlSlider extends Slider {
    public CamlSlider() {
        getStyleClass().add("caml-slider");
    }

    public CamlSlider(double min, double max, double value) {
        super(min, max, value);
        getStyleClass().add("caml-slider");
    }
}
