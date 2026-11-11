package org.chs.camlauncher.backend.error;

/** 携带结构化用户错误的后端异常。 */
public interface BackendErrorCarrier {
    BackendError error();
}
