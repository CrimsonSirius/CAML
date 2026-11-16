package org.chs.camlauncher.backend.mod.source;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** CurseForge 使用的 MurmurHash2 文件指纹（忽略空白字节）。 */
public final class CurseForgeFingerprint {
    private static final int M = 0x5bd1e995;

    private CurseForgeFingerprint() {
    }

    public static long of(Path file) throws IOException {
        byte[] input = Files.readAllBytes(file);
        int length = 0;
        for (byte value : input) {
            if (!isWhitespace(value)) {
                length++;
            }
        }
        if (length == 0) {
            return 0;
        }
        byte[] data = new byte[length];
        int cursor = 0;
        for (byte value : input) {
            if (!isWhitespace(value)) {
                data[cursor++] = value;
            }
        }

        int h = 1 ^ length;
        int index = 0;
        int remaining = length;
        while (remaining >= 4) {
            int k = nextFour(data, index);
            index += 4;
            remaining -= 4;
            k *= M;
            k ^= k >>> 24;
            k *= M;
            h *= M;
            h ^= k;
        }

        if (remaining == 3) {
            h ^= (data[index] & 0xff) | ((data[index + 1] & 0xff) << 8)
                    | ((data[index + 2] & 0xff) << 16);
            h *= M;
        } else if (remaining == 2) {
            h ^= (data[index] & 0xff) | ((data[index + 1] & 0xff) << 8);
            h *= M;
        } else if (remaining == 1) {
            h ^= data[index] & 0xff;
            h *= M;
        }

        h ^= h >>> 13;
        h *= M;
        h ^= h >>> 15;
        return Integer.toUnsignedLong(h);
    }

    private static int nextFour(byte[] input, int index) {
        return (input[index] & 0xff)
                | ((input[index + 1] & 0xff) << 8)
                | ((input[index + 2] & 0xff) << 16)
                | ((input[index + 3] & 0xff) << 24);
    }

    private static boolean isWhitespace(byte value) {
        return value == 9 || value == 10 || value == 13 || value == 32;
    }
}
