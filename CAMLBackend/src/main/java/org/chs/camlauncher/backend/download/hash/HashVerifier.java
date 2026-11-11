package org.chs.camlauncher.backend.download.hash;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class HashVerifier {
    private HashVerifier() {
    }

    public static String sha1(Path file) throws IOException {
        MessageDigest digest = newSha1Digest();

        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String sha1(byte[] content) {
        return HexFormat.of().formatHex(newSha1Digest().digest(content));
    }

    /** 校验文件 SHA-1；没有期望哈希时返回 {@code false}，调用方必须显式处理无哈希下载项。 */
    public static boolean matches(Path file, String expectedSha1) throws IOException {
        return expectedSha1 != null
                && !expectedSha1.isBlank()
                && sha1(file).equalsIgnoreCase(expectedSha1);
    }

    private static MessageDigest newSha1Digest() {
        try {
            return MessageDigest.getInstance("SHA-1");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-1 is not available", e);
        }
    }
}
