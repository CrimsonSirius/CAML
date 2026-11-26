package org.chs.camlauncher.front.view;

/** 启动按钮的三种稳定状态。 */
enum LaunchUiState {
    IDLE("home.launch"),
    PREPARING("home.launching"),
    RUNNING("home.running");

    private final String textKey;

    LaunchUiState(String textKey) {
        this.textKey = textKey;
    }

    String textKey() {
        return textKey;
    }

    boolean disablesLaunch() {
        return this != IDLE;
    }
}
