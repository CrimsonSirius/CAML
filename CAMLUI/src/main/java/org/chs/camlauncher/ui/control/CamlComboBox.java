package org.chs.camlauncher.ui.control;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

/** CAML 下拉框。 */
public final class CamlComboBox<T> extends ComboBox<T> {
    public CamlComboBox() {
        getStyleClass().add("caml-combo-box");
    }

    public CamlComboBox(ObservableList<T> items) {
        this();
        setItems(items);
    }
}
