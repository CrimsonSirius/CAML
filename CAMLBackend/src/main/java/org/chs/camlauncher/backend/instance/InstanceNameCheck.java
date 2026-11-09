package org.chs.camlauncher.backend.instance;

/** 实例名可用性检查结果。 */
public record InstanceNameCheck(boolean available, String message) {
}
