package org.chs.camlauncher.front.view;

/** 主页和设置页共享的启动参数。 */
final class LaunchSettings {
    private int maxMemoryMb = 4096;

    int maxMemoryMb() {
        return maxMemoryMb;
    }

    void setMaxMemoryMb(int maxMemoryMb) {
        this.maxMemoryMb = maxMemoryMb;
    }
}
