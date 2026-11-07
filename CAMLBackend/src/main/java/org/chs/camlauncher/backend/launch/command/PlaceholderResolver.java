package org.chs.camlauncher.backend.launch.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PlaceholderResolver {
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([^}]+)}");

    private PlaceholderResolver() {
    }

    public static String resolve(String value, Map<String, String> placeholders) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = placeholders.get(matcher.group(1));
            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(replacement != null ? replacement : matcher.group())
            );
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public static List<String> resolve(List<String> values, Map<String, String> placeholders) {
        List<String> result = new ArrayList<>(values.size());
        for (String value : values) {
            result.add(resolve(value, placeholders));
        }
        return List.copyOf(result);
    }
}
