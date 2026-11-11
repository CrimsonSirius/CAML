package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.error.BackendError;

import java.util.Objects;

/** 实例名可用性检查结果。 */
public record InstanceNameCheck(boolean available, BackendError error) {
    public InstanceNameCheck {
        if (available && error != null) {
            throw new IllegalArgumentException("available check must not carry an error");
        }
        if (!available && error == null) {
            throw new IllegalArgumentException("unavailable check must carry an error");
        }
    }

    public static InstanceNameCheck valid() {
        return new InstanceNameCheck(true, null);
    }

    public static InstanceNameCheck unavailable(BackendError error) {
        return new InstanceNameCheck(false, Objects.requireNonNull(error, "error"));
    }

    /** 兼容旧调用方的诊断文本；界面应使用 {@link #error()} 做本地化。 */
    public String message() {
        return error == null ? "" : error.code();
    }
}
