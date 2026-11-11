package org.chs.camlauncher.language.contract;

import java.util.Objects;

/** 语言系统向外传递的结构化异常；消息由后端负责诊断，不在契约层格式化。 */
public final class LanguageException extends RuntimeException {
    private final LanguageErrorCode errorCode;
    private final String locale;
    private final String key;

    public LanguageException(LanguageErrorCode errorCode, String locale, String key, Throwable cause) {
        super(Objects.requireNonNull(errorCode, "errorCode").name(), cause);
        this.errorCode = errorCode;
        this.locale = locale;
        this.key = key;
    }

    public LanguageErrorCode errorCode() {
        return errorCode;
    }

    public String locale() {
        return locale;
    }

    public String key() {
        return key;
    }
}
