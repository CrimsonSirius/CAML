package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendErrorCarrier;

import java.util.Objects;

/** 实例名格式错误，携带可本地化的错误码。 */
public final class InstanceNameException extends IllegalArgumentException
        implements BackendErrorCarrier {
    private final BackendError error;

    public InstanceNameException(BackendError error) {
        super(Objects.requireNonNull(error, "error").code());
        this.error = error;
    }

    @Override
    public BackendError error() {
        return error;
    }
}
