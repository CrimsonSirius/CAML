package org.chs.camlauncher.backend.metadata;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleEvaluatorTest {
    private static final Rule ALLOW_WINDOWS = new Rule(
            Rule.Action.ALLOW,
            new Rule.Os("windows", null, null),
            Map.of()
    );
    private static final Rule DISALLOW_WINDOWS = new Rule(
            Rule.Action.DISALLOW,
            new Rule.Os("windows", null, null),
            Map.of()
    );

    @Test
    void noRulesMeansAllowed() {
        assertTrue(new RuleEvaluator("linux", "x86_64", "6.0", Map.of()).isAllowed(List.of()));
    }

    @Test
    void matchesOperatingSystemAndArchitecture() {
        Rule rule = new Rule(
                Rule.Action.ALLOW,
                new Rule.Os("linux", "x86_64", "6\\..*"),
                Map.of()
        );

        assertTrue(new RuleEvaluator("linux", "x86_64", "6.8.0", Map.of()).isAllowed(List.of(rule)));
        assertFalse(new RuleEvaluator("windows", "x86_64", "10.0", Map.of()).isAllowed(List.of(rule)));
        assertFalse(new RuleEvaluator("linux", "arm64", "6.8.0", Map.of()).isAllowed(List.of(rule)));
    }

    @Test
    void matchesFeatures() {
        Rule rule = new Rule(
                Rule.Action.ALLOW,
                null,
                Map.of("has_custom_resolution", true)
        );

        assertTrue(new RuleEvaluator(
                "linux", "x86_64", "6.0", Map.of("has_custom_resolution", true)
        ).isAllowed(List.of(rule)));
        assertFalse(new RuleEvaluator("linux", "x86_64", "6.0", Map.of()).isAllowed(List.of(rule)));
    }

    @Test
    void lastMatchingRuleWins() {
        RuleEvaluator evaluator = new RuleEvaluator("windows", "x86_64", "10.0", Map.of());

        assertFalse(evaluator.isAllowed(List.of(ALLOW_WINDOWS, DISALLOW_WINDOWS)));
        assertTrue(evaluator.isAllowed(List.of(DISALLOW_WINDOWS, ALLOW_WINDOWS)));
    }
}
