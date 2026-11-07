package org.chs.camlauncher.backend.launch.command;

import org.chs.camlauncher.backend.metadata.model.Argument;
import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;
import org.chs.camlauncher.backend.launch.LaunchPlan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class GameArgumentsBuilder {
    public List<String> build(LaunchPlan plan, List<Path> classpath) {
        Map<String, String> placeholders = LaunchPlaceholders.create(plan, classpath);
        RuleEvaluator ruleEvaluator = new RuleEvaluator(LaunchPlaceholders.features(plan));
        List<String> result = new ArrayList<>();

        if (!plan.manifest().arguments().game().isEmpty()) {
            result.addAll(resolve(plan.manifest().arguments().game(), ruleEvaluator, placeholders));
        } else if (plan.manifest().minecraftArguments() != null) {
            result.addAll(PlaceholderResolver.resolve(
                    Arrays.asList(plan.manifest().minecraftArguments().split("\\s+")),
                    placeholders
            ));
        }

        result.addAll(plan.options().gameArguments());
        if (plan.options().fullscreen()) {
            result.add("--fullscreen");
        }

        return result.stream().filter(argument -> !argument.isBlank()).toList();
    }

    private static List<String> resolve(
            List<Argument> arguments,
            RuleEvaluator evaluator,
            Map<String, String> placeholders
    ) {
        List<String> result = new ArrayList<>();
        for (Argument argument : arguments) {
            for (String value : argument.resolve(evaluator)) {
                result.add(PlaceholderResolver.resolve(value, placeholders));
            }
        }
        return result;
    }
}
