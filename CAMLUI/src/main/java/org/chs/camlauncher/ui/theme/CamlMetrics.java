package org.chs.camlauncher.ui.theme;

import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;

/** CAML 组件共用的 UI 缩放因子；1.0 对应设计基准字号 16px。 */
public final class CamlMetrics {
    private static final ReadOnlyDoubleWrapper SCALE = new ReadOnlyDoubleWrapper(1.0);

    private CamlMetrics() {
    }

    public static double scale() {
        return SCALE.get();
    }

    public static ReadOnlyDoubleProperty scaleProperty() {
        return SCALE.getReadOnlyProperty();
    }

    public static void setScale(double scale) {
        if (scale <= 0 || !Double.isFinite(scale)) {
            throw new IllegalArgumentException("UI scale must be positive and finite");
        }
        SCALE.set(scale);
    }

    public static double size(double designPx) {
        return designPx * scale();
    }
}
