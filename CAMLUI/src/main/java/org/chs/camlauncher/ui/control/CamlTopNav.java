package org.chs.camlauncher.ui.control;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** CAML 顶部主导航容器，始终保留一个选中项。 */
public final class CamlTopNav extends HBox {
    private final List<CamlTopNavItem> items = new ArrayList<>();
    private final ReadOnlyObjectWrapper<CamlTopNavItem> selectedItem =
            new ReadOnlyObjectWrapper<>(this, "selectedItem");

    public CamlTopNav() {
        getStyleClass().add("caml-top-nav");
        setSpacing(2);
        setAlignment(Pos.CENTER_LEFT);
    }

    public CamlTopNavItem addItem(String text) {
        CamlTopNavItem item = new CamlTopNavItem(text);
        items.add(item);
        getChildren().add(item);
        item.addEventHandler(ActionEvent.ACTION, event -> {
            if (!item.isSelected()) {
                item.setSelected(true);
            }
            select(item);
        });
        if (selectedItem.get() == null) {
            select(item);
        }
        return item;
    }

    public void select(CamlTopNavItem item) {
        if (!items.contains(item)) {
            throw new IllegalArgumentException("导航项不属于当前导航栏");
        }
        selectedItem.set(item);
        for (CamlTopNavItem candidate : items) {
            candidate.setSelected(candidate == item);
        }
    }

    public ReadOnlyObjectProperty<CamlTopNavItem> selectedItemProperty() {
        return selectedItem.getReadOnlyProperty();
    }
}
