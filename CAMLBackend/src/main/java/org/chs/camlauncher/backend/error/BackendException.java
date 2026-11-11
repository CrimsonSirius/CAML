package org.chs.camlauncher.backend.error;

import java.io.IOException;
import java.util.Objects;

/** 带稳定错误码和参数的 IOException，供前端本地化展示。 */
public final class BackendException extends IOException implements BackendErrorCarrier {
    private final BackendError error;

    public BackendException(BackendError error) {
        this(error, null);
    }

    public BackendException(BackendError error, Throwable cause) {
        super(Objects.requireNonNull(error, "error").code(), cause);
        this.error = error;
    }

    @Override
    public BackendError error() {
        return error;
    }
}
