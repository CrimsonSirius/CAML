package org.chs.camlauncher.ui.theme;

/** CAML 动画开关；默认读取系统属性 {@code caml.reduceMotion}。 */
public final class CamlMotion {
    private static volatile boolean reduceMotion = Boolean.getBoolean("caml.reduceMotion");

    private CamlMotion() {
    }

    public static boolean reduceMotion() {
        return reduceMotion;
    }

    public static void setReduceMotion(boolean reduceMotion) {
        CamlMotion.reduceMotion = reduceMotion;
    }
}
