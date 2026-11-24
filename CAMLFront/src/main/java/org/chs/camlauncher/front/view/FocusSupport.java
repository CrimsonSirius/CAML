package org.chs.camlauncher.front.view;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import org.chs.camlauncher.ui.control.CamlSideNavItem;
import org.chs.camlauncher.ui.control.CamlTopNavItem;

/**
 * 焦点交接工具。
 *
 * <p>移除当前焦点节点后，JavaFX 会把焦点交给焦点遍历中的第一个控件。在 CAML 中
 * 这通常是顶栏或侧栏的导航胶囊，而导航胶囊的 {@code :selected} 和 {@code :focused}
 * 会叠加成很重的描边和阴影。所有会替换页面、关闭遮罩或移除内容的路径都应通过
 * 本类显式交出焦点，避免依赖 JavaFX 的隐式回退。</p>
 */
final class FocusSupport {
    private FocusSupport() {
    }

    /** @return {@code scene} 的当前焦点是否位于 {@code container} 内 */
    static boolean containsFocus(Node container, Scene scene) {
        if (container == null || scene == null) {
            return false;
        }
        for (Node node = scene.getFocusOwner(); node != null; node = node.getParent()) {
            if (node == container) {
                return true;
            }
        }
        return false;
    }

    /**
     * 恢复焦点到安全位置。
     *
     * <p>导航项、已脱离场景的旧节点和不可用节点都不能成为程序化焦点目标；
     * 这些情况下使用 {@code fallback}（通常是页面容器）。</p>
     */
    static void restoreFocus(Node preferred, Node fallback) {
        Node target = resolveRestoreTarget(preferred, fallback);
        if (isUsable(target)) {
            target.requestFocus();
        }
    }

    /** 聚焦容器内第一个可用控件；找不到时退回 {@code fallback}。 */
    static void focusFirst(Node container, Node fallback) {
        Node target = resolveFirstTarget(container, fallback);
        if (isUsable(target)) {
            target.requestFocus();
        }
    }

    /** 计算恢复焦点时的目标节点；导航项和不可用节点会被替换为 {@code fallback}。 */
    static Node resolveRestoreTarget(Node preferred, Node fallback) {
        if (isUsable(preferred) && !isNavigationItem(preferred)) {
            return preferred;
        }
        return isUsable(fallback) ? fallback : null;
    }

    /** 计算内容区第一个焦点目标；没有可用控件时退回 {@code fallback}。 */
    static Node resolveFirstTarget(Node container, Node fallback) {
        Node first = firstFocusable(container);
        if (isUsable(first)) {
            return first;
        }
        return isUsable(fallback) ? fallback : null;
    }

    private static boolean isUsable(Node node) {
        return node != null
                && node.getScene() != null
                && node.isVisible()
                && !node.isDisabled();
    }

    private static boolean isNavigationItem(Node node) {
        return node instanceof CamlTopNavItem || node instanceof CamlSideNavItem;
    }

    private static Node firstFocusable(Node node) {
        if (!isUsable(node)) {
            return null;
        }
        if (node instanceof ScrollPane scrollPane) {
            return firstFocusable(scrollPane.getContent());
        }
        if (node instanceof SplitPane splitPane) {
            for (Node item : splitPane.getItems()) {
                Node focusable = firstFocusable(item);
                if (focusable != null) {
                    return focusable;
                }
            }
            return null;
        }
        if (node.isFocusTraversable()) {
            return node;
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node focusable = firstFocusable(child);
                if (focusable != null) {
                    return focusable;
                }
            }
        }
        return null;
    }
}
