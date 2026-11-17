package org.chs.camlauncher.backend.auth.store;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

/**
 * accessToken 的本地加密存储。
 *
 * <p>使用 AES-GCM 加密，密钥放在 {@code ~/.caml/accounts/secret.key}（POSIX 下尽量收紧到
 * 0600）。<b>这不是安全边界</b>：密钥与密文在同一台机器上，能读到用户目录的程序仍然可以
 * 解密。它的目的只是避免凭据以明文形式躺在磁盘上、被备份或日志工具顺手带走。</p>
 */
final class SecretCipher {
    private static final Logger LOGGER = LogManager.getLogger(SecretCipher.class);
    private static final String PREFIX = "enc:v1:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;

    private final Path keyFile;
    private final SecureRandom random = new SecureRandom();
    private SecretKey key;

    SecretCipher(Path keyFile) {
        this.keyFile = keyFile;
    }

    String encrypt(String plain) throws IOException {
        if (plain == null || plain.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_BYTES];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IOException("无法加密账号凭据", e);
        }
    }

    /**
     * 解密凭据。密文损坏或密钥丢失时返回空串：token 失效只会要求重新登录，
     * 不应该让整个账号列表读取失败。
     */
    String decrypt(String stored) {
        if (stored == null || stored.isBlank()) {
            return "";
        }
        if (!stored.startsWith(PREFIX)) {
            // 兼容早期可能的明文写入，读取后由下一次保存加密。
            return stored;
        }
        try {
            byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX.length()));
            if (combined.length <= IV_BYTES) {
                return "";
            }
            byte[] iv = new byte[IV_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_BYTES);
            byte[] payload = new byte[combined.length - IV_BYTES];
            System.arraycopy(combined, IV_BYTES, payload, 0, payload.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(payload), StandardCharsets.UTF_8);
        } catch (Exception e) {
            LOGGER.warn("账号凭据解密失败，需要重新登录", e);
            return "";
        }
    }

    private synchronized SecretKey key() throws IOException {
        if (key != null) {
            return key;
        }
        byte[] raw;
        if (Files.isRegularFile(keyFile)) {
            raw = Files.readAllBytes(keyFile);
            if (raw.length != KEY_BYTES) {
                throw new IOException("账号密钥长度无效: " + keyFile);
            }
        } else {
            raw = new byte[KEY_BYTES];
            random.nextBytes(raw);
            Path parent = keyFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(keyFile, raw);
            restrictPermissions(keyFile);
        }
        key = new SecretKeySpec(raw, "AES");
        return key;
    }

    private static void restrictPermissions(Path file) {
        try {
            Files.setPosixFilePermissions(file, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows 或非 POSIX 文件系统：由用户目录的 ACL 保护。
        }
    }
}

