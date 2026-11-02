package org.chs.camlauncher.backend.metadata;

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
