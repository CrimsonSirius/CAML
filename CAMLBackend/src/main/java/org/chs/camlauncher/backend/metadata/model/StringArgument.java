package org.chs.camlauncher.backend.metadata.model;

import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;

import java.util.List;
import java.util.Objects;

public record StringArgument(String value) implements Argument {
    public StringArgument {
        Objects.requireNonNull(value, "value");
    }

    @Override
    public List<String> resolve(RuleEvaluator evaluator) {
        return List.of(value);
    }
}
