package org.chs.camlauncher.ui.control;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** CAML 卡片容器。 */
public final class CamlCard extends VBox {
    public enum Variant {
        ELEVATED("elevated"),
        FILLED("filled"),
        OUTLINED("outlined");

        private final String styleName;

        Variant(String styleName) {
            this.styleName = styleName;
        }
    }

    public CamlCard() {
        this(Variant.ELEVATED);
    }

    public CamlCard(Variant variant) {
        getStyleClass().addAll("caml-card", "caml-card-" + variant.styleName);
        setSpacing(16);
    }

    public CamlCard(Node... children) {
        this();
        getChildren().addAll(children);
    }
}
