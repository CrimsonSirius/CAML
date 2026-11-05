package org.chs.camlauncher.ui.control;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** CAML 侧边导航栏。 */
public final class CamlSideNav extends VBox {
    private final List<CamlSideNavItem> items = new ArrayList<>();
    private final ReadOnlyObjectWrapper<CamlSideNavItem> selectedItem =
            new ReadOnlyObjectWrapper<>(this, "selectedItem");

    public CamlSideNav() {
        getStyleClass().add("caml-side-nav");
        setSpacing(4);
        setPadding(new Insets(12, 8, 12, 8));
        setFillWidth(true);
    }

    public void addItem(CamlSideNavItem item) {
        Objects.requireNonNull(item, "item");
        items.add(item);
        getChildren().add(item);
        item.addEventHandler(ActionEvent.ACTION, event -> select(item));
        if (selectedItem.get() == null) {
            select(item);
        }
    }

    public void select(CamlSideNavItem item) {
        if (!items.contains(item)) {
            throw new IllegalArgumentException("导航项不属于当前导航栏");
        }
        selectedItem.set(item);
        for (CamlSideNavItem candidate : items) {
            candidate.setSelected(candidate == item);
        }
    }

    public ReadOnlyObjectProperty<CamlSideNavItem> selectedItemProperty() {
        return selectedItem.getReadOnlyProperty();
    }
}
