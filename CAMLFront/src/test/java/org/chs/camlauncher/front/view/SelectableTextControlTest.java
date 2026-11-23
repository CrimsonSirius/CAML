package org.chs.camlauncher.front.view;

import javafx.scene.Scene;
import javafx.scene.input.Clipboard;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import org.chs.camlauncher.ui.control.CamlSelectableText;
import org.chs.camlauncher.ui.theme.CamlTheme;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.chs.camlauncher.front.view.FxTestSupport.addStylesheet;
import static org.chs.camlauncher.front.view.FxTestSupport.onFxThread;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SelectableTextControlTest {
    @BeforeAll
    static void startToolkit() throws Exception {
        FxTestSupport.ensureStarted();
    }

    @Test
    void selectsAndCopiesWithoutScrollPane() throws Exception {
        String copied = onFxThread(() -> {
            CamlSelectableText text = new CamlSelectableText("alpha\nbeta\ngamma");
            VBox root = new VBox(text);
            root.setPrefSize(420, 120);
            Scene scene = new Scene(root, 420, 120);
            CamlTheme.install(scene);
            addStylesheet(scene, "/themes/mango.css");
            addStylesheet(scene, "/styles/app.css");
            root.applyCss();
            root.resize(420, 120);
            root.layout();

            assertNull(text.lookup(".scroll-pane"), "可选文本块不能包含滚动容器");
            assertNotNull(text.lookup(".caml-selectable-text-text"));

            text.selectAll();
            Text textNode = (Text) text.lookup(".caml-selectable-text-text");
            assertNotNull(textNode);
            assertTrue(textNode.getSelectionEnd() > textNode.getSelectionStart(),
                    "全选后必须设置原生文本选区");
            Path highlight = (Path) text.lookup(".caml-selectable-text-selection");
            assertNotNull(highlight);
            assertTrue(highlight.getElements().size() > 0, "选区背景必须绘制");
            assertTrue(highlight.getFill() instanceof Color color
                            && color.getOpacity() == 1,
                    "选区背景必须使用不透明的主题强调色");
            assertTrue(highlight.getStroke() == null
                            || highlight.getStroke() instanceof Color stroke
                            && stroke.getOpacity() == 0,
                    "选区背景不能带原生控件之外的描边");
            Text selectedOverlay =
                    (Text) text.lookup(".caml-selectable-text-selected-text");
            assertNotNull(selectedOverlay);
            assertTrue(selectedOverlay.getClip() instanceof Path clip
                            && !clip.getElements().isEmpty(),
                    "选中文字的反色覆盖层必须裁剪到选区");
            assertEquals("alpha\nbeta\ngamma", text.getSelectedText());

            text.copySelectedText();
            return Clipboard.getSystemClipboard().getString();
        });

        assertEquals("alpha\nbeta\ngamma", copied);
    }
}
