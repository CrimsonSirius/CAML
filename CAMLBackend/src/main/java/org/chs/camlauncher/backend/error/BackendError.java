package org.chs.camlauncher.backend.error;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/** 后端返回给界面的稳定错误码和格式化参数。 */
public record BackendError(String code, List<String> args) {
    public BackendError {
        code = Objects.requireNonNull(code, "code");
        if (code.isBlank()) {
            throw new IllegalArgumentException("error code must not be blank");
        }
        args = args == null ? List.of() : List.copyOf(args);
    }

    public static BackendError of(String code, Object... args) {
        return new BackendError(code, args == null
                ? List.of()
                : Arrays.stream(args).map(String::valueOf).toList());
    }
}
