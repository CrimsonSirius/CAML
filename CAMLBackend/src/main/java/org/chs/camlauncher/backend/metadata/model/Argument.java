package org.chs.camlauncher.backend.metadata.model;

import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;

import java.util.List;

public sealed interface Argument permits StringArgument, RuledArgument {
    List<String> resolve(RuleEvaluator evaluator);
}
