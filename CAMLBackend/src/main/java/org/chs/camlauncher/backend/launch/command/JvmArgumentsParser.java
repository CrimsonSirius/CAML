package org.chs.camlauncher.backend.launch.command;

import java.util.ArrayList;
import java.util.List;

/**
 * 把用户输入的 JVM 参数文本拆成进程参数列表。
 *
 * <p>空白分隔参数；单引号和双引号可以保留路径中的空格。Windows 路径中的反斜杠
 * 默认按普通字符处理，只有双引号内的 {@code \"} 和 {@code \\} 会被转义。</p>
 */
public final class JvmArgumentsParser {
    private JvmArgumentsParser() {
    }

    public static List<String> parse(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> arguments = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean singleQuoted = false;
        boolean doubleQuoted = false;
        boolean hasValue = false;

        for (int index = 0; index < text.length(); index++) {
            char character = text.charAt(index);
            if (singleQuoted) {
                if (character == '\'') {
                    singleQuoted = false;
                } else {
                    current.append(character);
                }
                hasValue = true;
            } else if (doubleQuoted) {
                if (character == '"') {
                    doubleQuoted = false;
                } else if (character == '\\' && index + 1 < text.length()) {
                    char next = text.charAt(index + 1);
                    if (next == '"' || next == '\\') {
                        current.append(next);
                        index++;
                    } else {
                        current.append(character);
                    }
                } else {
                    current.append(character);
                }
                hasValue = true;
            } else if (character == '\'') {
                singleQuoted = true;
                hasValue = true;
            } else if (character == '"') {
                doubleQuoted = true;
                hasValue = true;
            } else if (Character.isWhitespace(character)) {
                if (hasValue) {
                    arguments.add(current.toString());
                    current.setLength(0);
                    hasValue = false;
                }
            } else {
                current.append(character);
                hasValue = true;
            }
        }

        if (hasValue) {
            arguments.add(current.toString());
        }
        return List.copyOf(arguments);
    }
}
