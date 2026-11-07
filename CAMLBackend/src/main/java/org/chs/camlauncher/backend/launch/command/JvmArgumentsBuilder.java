package org.chs.camlauncher.backend.launch.command;

import org.chs.camlauncher.backend.metadata.model.Argument;
import org.chs.camlauncher.backend.metadata.model.LoggingInfo;
import org.chs.camlauncher.backend.metadata.rule.RuleEvaluator;
import org.chs.camlauncher.backend.launch.LaunchPlan;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JvmArgumentsBuilder {
    public List<String> build(LaunchPlan plan, List<Path> classpath) {
        Map<String, String> placeholders = LaunchPlaceholders.create(plan, classpath);
        RuleEvaluator ruleEvaluator = new RuleEvaluator(LaunchPlaceholders.features(plan));
        List<String> result = new ArrayList<>();

        if (plan.options().maxMemoryMb() != null) {
            result.add("-Xmx" + plan.options().maxMemoryMb() + "m");
        }
        if (plan.options().minMemoryMb() != null) {
            result.add("-Xms" + plan.options().minMemoryMb() + "m");
        }

        result.addAll(resolve(plan.manifest().arguments().jvm(), ruleEvaluator, placeholders));
        LoggingInfo logging = plan.manifest().logging().get("client");
        if (logging != null && logging.argument() != null && logging.file() != null && logging.file().id() != null) {
            String logConfig = plan.instance().assetsDirectory()
                    .resolve("log_configs")
                    .resolve(logging.file().id())
                    .toString();
            result.add(PlaceholderResolver.resolve(
                    logging.argument(),
                    Map.of("path", logConfig)
            ));
        }
        result.addAll(plan.options().jvmArguments());

        if (result.stream().noneMatch(argument -> argument.startsWith("-Djava.library.path="))) {
            result.add("-Djava.library.path=" + plan.nativesDirectory());
        }
        if (result.stream().noneMatch(argument -> argument.equals("-cp") || argument.equals("-classpath"))) {
            result.add("-cp");
            result.add(placeholders.get("classpath"));
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
