package org.chs.camlauncher.language.contract;

/** 后端语言任务的种类。 */
public enum LanguageTaskType {
    INSTALL_BUILTINS,
    SCAN_LOCALES,
    LOAD_LOCALE,
    SWITCH_LOCALE,
    VALIDATE_CATALOG,
    REPAIR_FILE
}
