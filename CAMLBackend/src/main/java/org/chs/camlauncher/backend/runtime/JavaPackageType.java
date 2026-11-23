package org.chs.camlauncher.backend.runtime;

/** Java 下载包类型。 */
public enum JavaPackageType {
    JDK("jdk", "JDK"),
    JRE("jre", "JRE");

    private final String apiValue;
    private final String displayName;

    JavaPackageType(String apiValue, String displayName) {
        this.apiValue = apiValue;
        this.displayName = displayName;
    }

    public String apiValue() {
        return apiValue;
    }

    public String displayName() {
        return displayName;
    }

    public static JavaPackageType fromApi(String value) {
        return "jre".equalsIgnoreCase(value) ? JRE : JDK;
    }
}
