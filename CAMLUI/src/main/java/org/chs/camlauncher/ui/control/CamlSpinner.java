package org.chs.camlauncher.ui.control;

import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;

/** CAML 整数微调框。 */
public final class CamlSpinner extends Spinner<Integer> {
    public CamlSpinner(int min, int max, int initialValue, int step) {
        super(min, max, initialValue, step);
        getStyleClass().add("caml-spinner");
    }

    public CamlSpinner(SpinnerValueFactory<Integer> valueFactory) {
        super(valueFactory);
        getStyleClass().add("caml-spinner");
    }
}
