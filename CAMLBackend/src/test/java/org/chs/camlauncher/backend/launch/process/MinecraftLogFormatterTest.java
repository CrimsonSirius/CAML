package org.chs.camlauncher.backend.launch.process;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MinecraftLogFormatterTest {
    @Test
    void formatsMultiLineLog4jEvent() {
        MinecraftLogFormatter formatter = new MinecraftLogFormatter(ZoneId.of("UTC"));

        assertTrue(formatter.accept(
                "<log4j:Event logger=\"net.minecraft.SystemReport\" timestamp=\"0\" " +
                        "level=\"WARN\" thread=\"main\">").isEmpty());
        assertTrue(formatter.accept(
                "  <log4j:Message><![CDATA[Failed to get system info]]></log4j:Message>").isEmpty());
        assertTrue(formatter.accept(
                "  <log4j:Throwable><![CDATA[java.lang.IllegalStateException: boom\n" +
                        "\tat example.Main.main(Main.java:1)]]></log4j:Throwable>").isEmpty());

        assertEquals(
                List.of(
                        "[00:00:00] [main/WARN]: Failed to get system info",
                        "java.lang.IllegalStateException: boom",
                        "\tat example.Main.main(Main.java:1)"
                ),
                formatter.accept("</log4j:Event>")
        );
    }

    @Test
    void passesThroughNonXmlOutput() {
        MinecraftLogFormatter formatter = new MinecraftLogFormatter(ZoneId.of("UTC"));

        assertEquals(List.of("plain output"), formatter.accept("plain output"));
    }

    @Test
    void formatsSingleLineEvent() {
        MinecraftLogFormatter formatter = new MinecraftLogFormatter(ZoneId.of("UTC"));

        assertEquals(
                List.of("[00:00:00] [Render thread/INFO]: hello"),
                formatter.accept(
                        "<log4j:Event logger=\"test\" timestamp=\"0\" level=\"INFO\" " +
                                "thread=\"Render thread\"><log4j:Message>" +
                                "<![CDATA[hello]]></log4j:Message></log4j:Event>")
        );
    }
}
