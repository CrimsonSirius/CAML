package org.chs.camlauncher.ui.control;

import javafx.scene.control.Button;

/**
 * CAML 按钮。
 *
 * <p>通过样式类区分 filled、tonal、outlined、text 和 elevated 五种变体。</p>
 */
public final class CamlButton extends Button {
    public enum Variant {
        FILLED("filled"),
        TONAL("tonal"),
        OUTLINED("outlined"),
        TEXT("text"),
        ELEVATED("elevated");

        private final String styleName;

        Variant(String styleName) {
            this.styleName = styleName;
        }
    }

    public CamlButton(String text) {
        this(text, Variant.FILLED);
    }

    public CamlButton(String text, Variant variant) {
        super(text);
        getStyleClass().addAll("caml-button", "caml-button-" + variant.styleName);
    }

    public static CamlButton filled(String text) {
        return new CamlButton(text, Variant.FILLED);
    }

    public static CamlButton tonal(String text) {
        return new CamlButton(text, Variant.TONAL);
    }

    public static CamlButton outlined(String text) {
        return new CamlButton(text, Variant.OUTLINED);
    }

    public static CamlButton text(String text) {
        return new CamlButton(text, Variant.TEXT);
    }

    public static CamlButton elevated(String text) {
        return new CamlButton(text, Variant.ELEVATED);
    }
}
