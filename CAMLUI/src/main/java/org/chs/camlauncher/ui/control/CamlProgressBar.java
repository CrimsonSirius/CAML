package org.chs.camlauncher.ui.control;

import javafx.scene.control.ProgressBar;

/** CAML 进度条。 */
public final class CamlProgressBar extends ProgressBar {
    public CamlProgressBar() {
        getStyleClass().add("caml-progress-bar");
    }

    public CamlProgressBar(double progress) {
        super(progress);
        getStyleClass().add("caml-progress-bar");
    }
}
