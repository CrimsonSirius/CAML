package org.chs.camlauncher.backend.metadata;

import java.util.List;

public sealed interface Argument permits StringArgument, RuledArgument {
    List<String> resolve(RuleEvaluator evaluator);
}
