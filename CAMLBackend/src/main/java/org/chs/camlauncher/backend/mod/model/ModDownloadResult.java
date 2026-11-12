package org.chs.camlauncher.backend.mod.model;

import java.nio.file.Path;

/** Mod 文件下载结果。 */
public record ModDownloadResult(Path path, ModFile file) {
}
