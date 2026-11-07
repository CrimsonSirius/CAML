package org.chs.camlauncher.ui.control;

import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.util.StringConverter;

/** CAML 下拉框。 */
public final class CamlComboBox<T> extends ComboBox<T> {
    public CamlComboBox() {
        getStyleClass().add("caml-combo-box");
    }

    public CamlComboBox(ObservableList<T> items) {
        this();
        setItems(items);
    }

    /**
     * 强制重建按钮单元格。
     *
     * <p>JavaFX 在选中项被清空、但 promptText 仍为原值时，按钮单元格可能保留旧文本。
     * 重新设置 buttonCell 会触发 ComboBox skin 重新渲染当前值或提示文字。</p>
     */
    public void refreshDisplay() {
        setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                if (item == null) {
                    setText(CamlComboBox.this.getPromptText());
                    return;
                }
                StringConverter<T> converter = CamlComboBox.this.getConverter();
                setText(converter == null ? String.valueOf(item) : converter.toString(item));
            }
        });
    }
}
