package org.chs.camlauncher.front.view;

/** 下拉框中的“全部/具体值”选项。 */
record ModChoice<T>(String label, T value) {
    @Override
    public String toString() {
        return label;
    }
}
