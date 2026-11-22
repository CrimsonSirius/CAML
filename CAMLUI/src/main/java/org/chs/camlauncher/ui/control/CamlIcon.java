package org.chs.camlauncher.ui.control;

import javafx.scene.Group;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

/** CAML 内置矢量图标；使用 JavaFX 几何图元，不依赖第三方图标库。 */
public final class CamlIcon extends StackPane {
    public enum Glyph {
        HOME,
        DOWNLOAD,
        SETTINGS,
        ACCOUNT,
        PLAY,
        FOLDER,
        GRID,
        APPEARANCE,
        CHECK,
        WARNING,
        MORE,
        SEARCH,
        COPY,
        CHEVRON_DOWN,
        CHEVRON_UP
    }

    private static final double DESIGN_SIZE = 24;

    public CamlIcon(Glyph glyph) {
        this(glyph, 18);
    }

    public CamlIcon(Glyph glyph, double size) {
        if (glyph == null) {
            throw new IllegalArgumentException("glyph must not be null");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive");
        }

        getStyleClass().add("caml-icon");
        setMouseTransparent(true);
        setPickOnBounds(false);
        setFocusTraversable(false);
        setMinSize(size, size);
        setPrefSize(size, size);
        setMaxSize(size, size);

        Group group = new Group(createShapes(glyph));
        double scale = size / DESIGN_SIZE;
        group.setScaleX(scale);
        group.setScaleY(scale);
        getChildren().add(group);
    }

    private static Shape[] createShapes(Glyph glyph) {
        return switch (glyph) {
            case HOME -> new Shape[]{
                    filled(new Polygon(3, 10.5, 12, 3, 21, 10.5, 21, 21, 14.5, 21,
                            14.5, 14, 9.5, 14, 9.5, 21, 3, 21))
            };
            case DOWNLOAD -> new Shape[]{
                    filled(new Rectangle(10, 3, 4, 9)),
                    filled(new Polygon(7, 11, 17, 11, 12, 18)),
                    filled(new Rectangle(5, 20, 14, 2))
            };
            case SETTINGS -> new Shape[]{
                    filled(new Rectangle(3, 5, 18, 2)),
                    filled(new Rectangle(3, 11, 18, 2)),
                    filled(new Rectangle(3, 17, 18, 2)),
                    filled(new Circle(8, 6, 2.2)),
                    filled(new Circle(16, 12, 2.2)),
                    filled(new Circle(8, 18, 2.2))
            };
            case ACCOUNT -> new Shape[]{
                    filled(new Circle(12, 7, 4)),
                    filled(accountBody())
            };
            case PLAY -> new Shape[]{
                    filled(new Polygon(8, 5, 20, 12, 8, 19))
            };
            case FOLDER -> new Shape[]{
                    filled(new Polygon(3, 5, 10, 5, 12, 7, 21, 7, 21, 20, 3, 20))
            };
            case GRID -> new Shape[]{
                    filled(new Rectangle(3, 3, 7, 7)),
                    filled(new Rectangle(14, 3, 7, 7)),
                    filled(new Rectangle(3, 14, 7, 7)),
                    filled(new Rectangle(14, 14, 7, 7))
            };
            case APPEARANCE -> new Shape[]{
                    filled(new Polygon(12, 2, 14.2, 9.8, 22, 12, 14.2, 14.2,
                            12, 22, 9.8, 14.2, 2, 12, 9.8, 9.8))
            };
            case CHECK -> new Shape[]{
                    stroked(new Polyline(4, 12, 9, 17, 20, 6))
            };
            case WARNING -> new Shape[]{
                    stroked(new Polygon(12, 3, 21, 20, 3, 20)),
                    stroked(new Line(12, 8, 12, 14)),
                    filled(new Circle(12, 17.5, 1.3))
            };
            case MORE -> new Shape[]{
                    filled(new Circle(5, 12, 2)),
                    filled(new Circle(12, 12, 2)),
                    filled(new Circle(19, 12, 2))
            };
            case SEARCH -> new Shape[]{
                    stroked(new Circle(10, 10, 6)),
                    stroked(new Line(15, 15, 21, 21))
            };
            case COPY -> new Shape[]{
                    stroked(new Rectangle(8, 3, 13, 13)),
                    stroked(new Rectangle(3, 8, 13, 13))
            };
            case CHEVRON_DOWN -> new Shape[]{
                    stroked(new Polyline(7, 9, 12, 14, 17, 9))
            };
            case CHEVRON_UP -> new Shape[]{
                    stroked(new Polyline(7, 15, 12, 10, 17, 15))
            };
        };
    }

    private static Shape filled(Shape shape) {
        shape.getStyleClass().add("caml-icon-fill");
        return shape;
    }

    private static Shape stroked(Shape shape) {
        shape.getStyleClass().add("caml-icon-stroke");
        return shape;
    }

    private static Arc accountBody() {
        Arc arc = new Arc(12, 22, 8, 7, 180, 180);
        arc.setType(ArcType.CHORD);
        return arc;
    }
}
