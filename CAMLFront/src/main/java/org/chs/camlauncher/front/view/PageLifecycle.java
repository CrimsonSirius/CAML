package org.chs.camlauncher.front.view;

/**
 * 页面生命周期。
 *
 * <p>页面容器负责在切换时调用这些方法；页面在自己的生命周期内管理任务句柄、
 * 订阅和定时器，避免后台回调继续修改已经离开的界面。</p>
 */
interface PageLifecycle {
    default void onShow() {
    }

    default void onHide() {
    }

    default void dispose() {
    }
}
