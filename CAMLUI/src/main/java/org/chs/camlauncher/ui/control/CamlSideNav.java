package org.chs.camlauncher.ui.control;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
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

    /** 在侧栏当前位置插入分组标题和分隔线；标题不参与选中和焦点遍历。 */
    public void addSection(String title) {
        Label label = new Label(title);
        label.getStyleClass().add("caml-side-nav-section-title");
        label.setMaxWidth(Double.MAX_VALUE);

        Region divider = new Region();
        divider.getStyleClass().add("caml-side-nav-section-divider");
        divider.setMaxWidth(Double.MAX_VALUE);

        VBox section = new VBox(label, divider);
        section.getStyleClass().add("caml-side-nav-section");
        section.setFillWidth(true);
        getChildren().add(section);
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
