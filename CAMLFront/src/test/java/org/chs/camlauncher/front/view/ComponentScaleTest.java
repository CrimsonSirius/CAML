package org.chs.camlauncher.front.view;

import org.chs.camlauncher.ui.control.CamlIcon;
import org.chs.camlauncher.ui.control.CamlSlider;
import org.chs.camlauncher.ui.control.CamlSwitch;
import org.chs.camlauncher.ui.theme.CamlMetrics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ComponentScaleTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void componentsScaleWithMetrics() throws Exception {
        double[] sizes = onFxThread(() -> {
            double previous = CamlMetrics.scale();
            try {
                CamlMetrics.setScale(1);
                CamlIcon icon = new CamlIcon(CamlIcon.Glyph.HOME, 24);
                CamlSlider slider = new CamlSlider(1, 10, 5);
                CamlSwitch toggle = new CamlSwitch();

                CamlMetrics.setScale(1.5);
                return new double[]{
                        icon.prefWidth(-1),
                        slider.prefHeight(-1),
                        toggle.prefWidth(-1)
                };
            } finally {
                CamlMetrics.setScale(previous);
            }
        });

        assertEquals(36, sizes[0], 0.5);
        assertEquals(42, sizes[1], 0.5);
        assertEquals(78, sizes[2], 0.5);
    }
}
