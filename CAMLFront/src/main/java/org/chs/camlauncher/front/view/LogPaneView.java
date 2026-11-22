package org.chs.camlauncher.front.view;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.chs.camlauncher.front.text.UiText;
import org.chs.camlauncher.ui.control.CamlButton;
import org.chs.camlauncher.ui.control.CamlIcon;
import org.chs.camlauncher.ui.control.CamlTextArea;

import java.util.ArrayDeque;
import java.util.Deque;

/** 主页右侧的活动面板：折叠时显示最近 5 条，展开时显示完整日志。 */
final class LogPaneView extends VBox {
    private static final int RECENT_LINE_LIMIT = 5;

    private final CamlTextArea logArea;
    private final Label preview;
    private final CamlButton toggle;
    private final Deque<String> recent = new ArrayDeque<>();
    private boolean collapsed = true;
    private Runnable onCollapsedChanged;

    LogPaneView() {
        Label heading = new Label(UiText.tr("log.title"));
        heading.getStyleClass().add("caml-title-large");

        Label subheading = new Label(UiText.tr("log.subtitle"));
        subheading.getStyleClass().addAll("caml-body-small", "caml-on-surface-muted");

        VBox headingBlock = new VBox(heading, subheading);
        headingBlock.getStyleClass().add("app-log-heading");

        toggle = CamlButton.text("");
        toggle.getStyleClass().add("app-log-toggle");
        toggle.setOnAction(event -> setCollapsed(!collapsed));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(headingBlock, spacer, toggle);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("app-log-header");

        preview = new Label();
        preview.getStyleClass().add("app-log-preview");
        preview.setWrapText(false);
        preview.setMaxWidth(Double.MAX_VALUE);
        preview.setAccessibleText(UiText.tr("log.accessible"));

        logArea = new CamlTextArea();
        logArea.setEditable(false);
        logArea.setWrapText(false);
        logArea.setMaxHeight(Double.MAX_VALUE);
        logArea.setAccessibleText(UiText.tr("log.accessible"));
        logArea.getStyleClass().add("app-log-area");
        VBox.setVgrow(logArea, Priority.ALWAYS);

        getStyleClass().add("app-log-pane");
        getChildren().addAll(header, preview, logArea);
        updateContent();
    }

    void append(String line) {
        logArea.appendText(line + "\n");
        while (recent.size() >= RECENT_LINE_LIMIT) {
            recent.removeFirst();
        }
        recent.addLast(line);
        updatePreview();
    }

    void clear() {
        logArea.clear();
        recent.clear();
        updatePreview();
    }

    boolean isCollapsed() {
        return collapsed;
    }

    void setCollapsed(boolean collapsed) {
        if (this.collapsed == collapsed) {
            return;
        }
        this.collapsed = collapsed;
        updateContent();
        if (onCollapsedChanged != null) {
            onCollapsedChanged.run();
        }
    }

    void setOnCollapsedChanged(Runnable listener) {
        onCollapsedChanged = listener;
    }

    CamlTextArea logArea() {
        return logArea;
    }

    private void updateContent() {
        toggle.setText(UiText.tr(collapsed ? "log.expand" : "log.collapse"));
        toggle.setGraphic(new CamlIcon(collapsed
                ? CamlIcon.Glyph.CHEVRON_DOWN
                : CamlIcon.Glyph.CHEVRON_UP, 16));
        preview.setVisible(collapsed);
        preview.setManaged(collapsed);
        logArea.setVisible(!collapsed);
        logArea.setManaged(!collapsed);
        updatePreview();
    }

    private void updatePreview() {
        preview.setText(recent.isEmpty()
                ? UiText.tr("log.empty")
                : String.join("\n", recent));
    }
}
