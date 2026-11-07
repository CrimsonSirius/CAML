package org.chs.camlauncher.backend.launch.process;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 把 Minecraft Log4j {@code LegacyXMLLayout} 的控制台输出转换成原版日志格式。
 *
 * <p>官方客户端配置把 XML 事件写到 stdout，启动器直接转发会非常难读。
 * 这里按事件累积多行 XML，再输出成
 * {@code [HH:mm:ss] [线程/级别]: 消息}，异常堆栈保持后续行。</p>
 */
final class MinecraftLogFormatter {
    private static final Pattern ATTRIBUTE = Pattern.compile("(\\w+)=\"([^\"]*)\"");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ZoneId zoneId;
    private final StringBuilder event = new StringBuilder();
    private boolean insideEvent;

    MinecraftLogFormatter() {
        this(ZoneId.systemDefault());
    }

    MinecraftLogFormatter(ZoneId zoneId) {
        this.zoneId = Objects.requireNonNull(zoneId, "zoneId");
    }

    List<String> accept(String line) {
        if (!insideEvent) {
            if (line.contains("<log4j:Event")) {
                insideEvent = true;
                event.setLength(0);
                event.append(line).append('\n');
                if (line.contains("</log4j:Event>")) {
                    return finishEvent();
                }
                return List.of();
            }
            return List.of(line);
        }

        event.append(line).append('\n');
        if (line.contains("</log4j:Event>")) {
            return finishEvent();
        }
        return List.of();
    }

    List<String> flush() {
        if (!insideEvent) {
            return List.of();
        }
        String raw = event.toString().stripTrailing();
        insideEvent = false;
        event.setLength(0);
        return List.of(raw);
    }

    private List<String> finishEvent() {
        String xml = event.toString();
        insideEvent = false;
        event.setLength(0);

        String thread = attribute(xml, "thread");
        String level = attribute(xml, "level");
        String timestamp = attribute(xml, "timestamp");
        String message = element(xml, "Message");
        String throwable = element(xml, "Throwable");

        String time = formatTime(timestamp);
        String header = "[" + time + "] ["
                + (thread.isBlank() ? "main" : thread) + "/"
                + (level.isBlank() ? "INFO" : level) + "]:";

        List<String> result = new ArrayList<>();
        List<String> messageLines = lines(message);
        if (messageLines.isEmpty()) {
            result.add(header);
        } else {
            result.add(header + " " + messageLines.get(0));
            for (int index = 1; index < messageLines.size(); index++) {
                result.add(messageLines.get(index));
            }
        }
        result.addAll(lines(throwable));
        return List.copyOf(result);
    }

    private String formatTime(String timestamp) {
        try {
            return Instant.ofEpochMilli(Long.parseLong(timestamp))
                    .atZone(zoneId)
                    .format(TIME_FORMAT);
        } catch (NumberFormatException e) {
            return Instant.now().atZone(zoneId).format(TIME_FORMAT);
        }
    }

    private static String attribute(String xml, String name) {
        Matcher matcher = ATTRIBUTE.matcher(xml);
        while (matcher.find()) {
            if (name.equals(matcher.group(1))) {
                return matcher.group(2);
            }
        }
        return "";
    }

    private static String element(String xml, String name) {
        String open = "<log4j:" + name + ">";
        String close = "</log4j:" + name + ">";
        int start = xml.indexOf(open);
        int end = xml.indexOf(close, start + open.length());
        if (start < 0 || end < 0) {
            return "";
        }

        String content = xml.substring(start + open.length(), end).strip();
        if (content.startsWith("<![CDATA[")) {
            content = content.substring("<![CDATA[".length());
        }
        if (content.endsWith("]]>")) {
            content = content.substring(0, content.length() - "]]>".length());
        }
        return content.strip();
    }

    private static List<String> lines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return value.lines().map(String::stripTrailing).toList();
    }
}
