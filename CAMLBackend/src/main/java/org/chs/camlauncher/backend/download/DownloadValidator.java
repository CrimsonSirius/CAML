package org.chs.camlauncher.backend.download;

import java.io.IOException;
import java.nio.file.Path;

/** 下载完成、写入目标文件前的额外内容校验。 */
@FunctionalInterface
public interface DownloadValidator {
    void validate(Path file) throws IOException;
}
