package org.chs.camlauncher.ui.control;

import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;

/** CAML 侧边导航项目。 */
public final class CamlSideNavItem extends Button {
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
    private boolean selected;

    public CamlSideNavItem(Node icon, String label) {
        super(label, icon);
        getStyleClass().add("caml-side-nav-item");
        setContentDisplay(ContentDisplay.TOP);
        setGraphicTextGap(4);
        setAlignment(Pos.CENTER);
        setMaxWidth(Double.MAX_VALUE);
        setFocusTraversable(true);
    }

    public CamlSideNavItem(String label) {
        super(label);
        getStyleClass().add("caml-side-nav-item");
        setContentDisplay(ContentDisplay.LEFT);
        setAlignment(Pos.CENTER_LEFT);
        setMaxWidth(Double.MAX_VALUE);
        setFocusTraversable(true);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        pseudoClassStateChanged(SELECTED, selected);
    }
}
