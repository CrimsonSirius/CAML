package org.chs.camlauncher.front.view;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 按数字段比较 Minecraft 游戏版本，避免字符串排序把 1.9 排到 1.10 前面。 */
final class MinecraftVersionComparator {
    static final Comparator<String> NEWEST_FIRST = (left, right) -> compare(right, left);
    static final Comparator<String> GROUP_NEWEST_FIRST =
            (left, right) -> compareGroupKey(right, left);

    private MinecraftVersionComparator() {
    }

    static int compare(String left, String right) {
        List<Token> leftTokens = tokenize(left);
        List<Token> rightTokens = tokenize(right);
        leftTokens.add(Token.END);
        rightTokens.add(Token.END);
        int index = 0;
        while (index < leftTokens.size() && index < rightTokens.size()) {
            int result = leftTokens.get(index).compareTo(rightTokens.get(index));
            if (result != 0) {
                return result;
            }
            index++;
        }
        return 0;
    }

    static int compareGroupKey(String left, String right) {
        int leftSpace = left.indexOf(' ');
        int rightSpace = right.indexOf(' ');
        String leftVersion = leftSpace < 0 ? left : left.substring(0, leftSpace);
        String rightVersion = rightSpace < 0 ? right : right.substring(0, rightSpace);
        int result = compare(leftVersion, rightVersion);
        if (result != 0) {
            return result;
        }
        String leftLoader = leftSpace < 0 ? "" : left.substring(leftSpace + 1);
        String rightLoader = rightSpace < 0 ? "" : right.substring(rightSpace + 1);
        return leftLoader.compareToIgnoreCase(rightLoader);
    }

    private static boolean isPrerelease(String text) {
        return switch (text) {
            case "pre", "rc", "alpha", "beta", "snapshot", "experimental" -> true;
            default -> false;
        };
    }

    private static List<Token> tokenize(String version) {
        List<Token> tokens = new ArrayList<>();
        if (version == null) {
            return tokens;
        }
        StringBuilder current = new StringBuilder();
        Boolean numeric = null;
        for (int index = 0; index < version.length(); index++) {
            char character = version.charAt(index);
            if (character == '.' || character == '-' || character == '_'
                    || character == '+' || Character.isWhitespace(character)) {
                flush(tokens, current, numeric);
                numeric = null;
                continue;
            }
            boolean digit = Character.isDigit(character);
            if (numeric != null && numeric != digit) {
                flush(tokens, current, numeric);
            }
            numeric = digit;
            current.append(Character.toLowerCase(character));
        }
        flush(tokens, current, numeric);
        return tokens;
    }

    private static void flush(List<Token> tokens, StringBuilder current, Boolean numeric) {
        if (current.isEmpty() || numeric == null) {
            current.setLength(0);
            return;
        }
        String text = current.toString();
        Kind kind = numeric
                ? Kind.NUMERIC
                : isPrerelease(text) ? Kind.PRERELEASE : Kind.TEXT;
        tokens.add(new Token(kind, text));
        current.setLength(0);
    }

    /** 固定顺序保证比较器满足全序：预发布 < 结尾哨兵 < 数字 < 普通文本。 */
    private enum Kind {
        PRERELEASE,
        END,
        NUMERIC,
        TEXT
    }

    private record Token(Kind kind, String text) implements Comparable<Token> {
        private static final Token END = new Token(Kind.END, "");

        @Override
        public int compareTo(Token other) {
            if (kind != other.kind) {
                return Integer.compare(kind.ordinal(), other.kind.ordinal());
            }
            if (kind == Kind.END) {
                return 0;
            }
            if (kind == Kind.NUMERIC) {
                String left = stripLeadingZeros(text);
                String right = stripLeadingZeros(other.text);
                if (left.length() != right.length()) {
                    return Integer.compare(left.length(), right.length());
                }
                return left.compareTo(right);
            }
            return text.compareTo(other.text);
        }

        private static String stripLeadingZeros(String value) {
            int index = 0;
            while (index < value.length() - 1 && value.charAt(index) == '0') {
                index++;
            }
            return value.substring(index);
        }
    }
}
