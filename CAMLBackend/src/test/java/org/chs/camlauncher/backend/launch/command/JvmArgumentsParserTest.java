package org.chs.camlauncher.backend.launch.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JvmArgumentsParserTest {
    @Test
    void returnsEmptyListForBlankText() {
        assertTrue(JvmArgumentsParser.parse(null).isEmpty());
        assertTrue(JvmArgumentsParser.parse("   ").isEmpty());
    }

    @Test
    void splitsArgumentsAndPreservesQuotedSpaces() {
        assertEquals(
                List.of("-XX:+UseG1GC", "-Dname=hello world"),
                JvmArgumentsParser.parse("-XX:+UseG1GC -Dname=\"hello world\"")
        );
    }

    @Test
    void preservesWindowsPathBackslashes() {
        assertEquals(
                List.of("-Dpath=C:\\Program Files\\Java"),
                JvmArgumentsParser.parse("\"-Dpath=C:\\Program Files\\Java\"")
        );
    }

    @Test
    void supportsSingleQuotesAndMultipleSpaces() {
        assertEquals(
                List.of("-Dname=hello world", "-Dempty="),
                JvmArgumentsParser.parse("  '-Dname=hello world'   -Dempty=  ")
        );
    }

    @Test
    void keepsEmptyQuotedArgument() {
        assertEquals(List.of(""), JvmArgumentsParser.parse("\"\""));
    }

    @Test
    void treatsUnclosedQuoteAsRestOfInput() {
        assertEquals(
                List.of("-Dname=hello world"),
                JvmArgumentsParser.parse("-Dname=\"hello world")
        );
    }
}
