package org.chs.camlauncher.ui.control;

import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Path;
import javafx.scene.text.HitInfo;
import javafx.scene.text.Text;

/** 原生不可滚动的多行可选文本块，支持鼠标选择与 Ctrl/Cmd+C 复制。 */
public final class CamlSelectableText extends StackPane {
    private final Text textNode = new Text();
    private final Text selectedTextNode = new Text();
    private final Path selectionBackground = new Path();
    private final Path selectionClip = new Path();
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private int dragAnchor = -1;
    private boolean dragging;

    public CamlSelectableText() {
        getStyleClass().add("caml-selectable-text");
        setAccessibleRole(AccessibleRole.TEXT);
        setFocusTraversable(true);

        textNode.getStyleClass().add("caml-selectable-text-text");
        textNode.setCursor(Cursor.TEXT);
        textNode.setPickOnBounds(true);
        textNode.setSelectionFill(Color.TRANSPARENT);
        textNode.textProperty().addListener((observable, oldValue, value) -> {
            setAccessibleText(value);
            clearSelection();
        });
        textNode.layoutBoundsProperty().addListener(
                (observable, oldValue, value) -> updateSelectionClip());

        selectedTextNode.getStyleClass().addAll(
                "caml-selectable-text-text", "caml-selectable-text-selected-text");
        selectedTextNode.textProperty().bind(textNode.textProperty());
        selectedTextNode.setManaged(false);
        selectedTextNode.setMouseTransparent(true);
        selectionBackground.getStyleClass().add("caml-selectable-text-selection");
        selectionBackground.setManaged(false);
        selectionBackground.setMouseTransparent(true);
        selectionClip.setManaged(false);
        selectionClip.setFill(Color.BLACK);
        selectedTextNode.setClip(selectionClip);

        Group content = new Group(selectionBackground, textNode, selectedTextNode);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        getChildren().add(content);

        textNode.setOnMousePressed(this::handleMousePressed);
        textNode.setOnMouseDragged(this::handleMouseDragged);
        textNode.setOnMouseReleased(event -> dragging = false);
        textNode.setOnMouseClicked(this::handleMouseClicked);
        addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    }

    public CamlSelectableText(String text) {
        this();
        setText(text);
    }

    public void setText(String text) {
        textNode.setText(text == null ? "" : text);
    }

    public String getText() {
        return textNode.getText();
    }

    public void selectAll() {
        int length = getText().length();
        if (length > 0) {
            setSelection(0, length);
        }
    }

    public void selectRange(int start, int end) {
        setSelection(start, end);
    }

    public void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
        textNode.setSelectionStart(-1);
        textNode.setSelectionEnd(-1);
        updateSelectionClip();
    }

    public String getSelectedText() {
        if (selectionStart < 0 || selectionEnd <= selectionStart) {
            return "";
        }
        return getText().substring(selectionStart, selectionEnd);
    }

    public void copySelectedText() {
        String selected = getSelectedText();
        if (selected.isEmpty()) {
            return;
        }
        ClipboardContent content = new ClipboardContent();
        content.putString(selected);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        requestFocus();
        int index = indexAt(event);
        if (index < 0) {
            return;
        }
        dragAnchor = index;
        dragging = true;
        setSelection(index, index);
        event.consume();
    }

    private void handleMouseDragged(MouseEvent event) {
        if (!dragging) {
            return;
        }
        int index = indexAt(event);
        if (index >= 0 && dragAnchor >= 0) {
            setSelection(dragAnchor, index);
        }
        event.consume();
    }

    private void handleMouseClicked(MouseEvent event) {
        if (event.getButton() != MouseButton.PRIMARY) {
            return;
        }
        if (event.getClickCount() >= 3) {
            selectAll();
            event.consume();
            return;
        }
        if (event.getClickCount() == 2) {
            selectWordAt(indexAt(event));
            event.consume();
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        if (!event.isShortcutDown()) {
            return;
        }
        if (event.getCode() == KeyCode.C) {
            copySelectedText();
            event.consume();
        } else if (event.getCode() == KeyCode.A) {
            selectAll();
            event.consume();
        }
    }

    private int indexAt(MouseEvent event) {
        Point2D local = textNode.sceneToLocal(event.getSceneX(), event.getSceneY());
        HitInfo hit = textNode.hitTest(local);
        return hit == null ? -1 : hit.getInsertionIndex();
    }

    private void selectWordAt(int index) {
        String text = getText();
        if (index < 0 || text.isEmpty()) {
            return;
        }
        int position = Math.min(index, text.length() - 1);
        if (!isWordCharacter(text.charAt(position))) {
            setSelection(index, Math.min(index + 1, text.length()));
            dragAnchor = index;
            return;
        }
        int start = position;
        while (start > 0 && isWordCharacter(text.charAt(start - 1))) {
            start--;
        }
        int end = position + 1;
        while (end < text.length() && isWordCharacter(text.charAt(end))) {
            end++;
        }
        dragAnchor = start;
        setSelection(start, end);
    }

    private void setSelection(int first, int second) {
        int length = getText().length();
        int start = Math.max(0, Math.min(first, second));
        int end = Math.min(length, Math.max(first, second));
        if (start == end) {
            clearSelection();
            return;
        }
        selectionStart = start;
        selectionEnd = end;
        textNode.setSelectionStart(start);
        textNode.setSelectionEnd(end);
        updateSelectionClip();
    }

    private void updateSelectionClip() {
        if (selectionStart < 0 || selectionEnd <= selectionStart) {
            selectionBackground.getElements().clear();
            selectionClip.getElements().clear();
            return;
        }
        var shape = textNode.rangeShape(selectionStart, selectionEnd);
        selectionBackground.getElements().setAll(shape);
        selectionClip.getElements().setAll(shape);
    }

    private static boolean isWordCharacter(char value) {
        return Character.isLetterOrDigit(value)
                || value == '_'
                || value == '-'
                || value == '.'
                || value == '/'
                || value == ':';
    }
}
