package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record RuledArgument(
        @SerializedName("rules") List<Rule> rules,
        @SerializedName("value") List<String> values
) implements Argument {
    public RuledArgument {
        rules = rules == null ? List.of() : List.copyOf(rules);
        values = values == null ? List.of() : List.copyOf(values);
    }

    @Override
    public List<String> resolve(RuleEvaluator evaluator) {
        return evaluator.isAllowed(rules) ? values : List.of();
    }
}
