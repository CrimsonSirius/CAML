package org.chs.camlauncher.backend.metadata.parse;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import org.chs.camlauncher.backend.metadata.model.Argument;
import org.chs.camlauncher.backend.metadata.model.RuledArgument;
import org.chs.camlauncher.backend.metadata.model.StringArgument;
import org.chs.camlauncher.backend.metadata.rule.Rule;

import java.lang.reflect.Type;
import java.util.List;

public final class ArgumentAdapter implements JsonDeserializer<Argument> {
    private static final Type RULE_LIST_TYPE = new TypeToken<List<Rule>>() {
    }.getType();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    @Override
    public Argument deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isString()) {
            return new StringArgument(json.getAsString());
        }

        if (!json.isJsonObject()) {
            throw new JsonParseException("Argument must be a string or an object");
        }

        JsonObject object = json.getAsJsonObject();
        List<Rule> rules = object.has("rules")
                ? context.deserialize(object.get("rules"), RULE_LIST_TYPE)
                : List.of();

        JsonElement valueElement = object.has("value") ? object.get("value") : object.get("values");
        if (valueElement == null) {
            throw new JsonParseException("Argument object has no value");
        }

        List<String> values;
        if (valueElement.isJsonArray()) {
            values = context.deserialize(valueElement, STRING_LIST_TYPE);
        } else if (valueElement.isJsonPrimitive() && valueElement.getAsJsonPrimitive().isString()) {
            values = List.of(valueElement.getAsString());
        } else {
            throw new JsonParseException("Argument value must be a string or a string array");
        }

        return new RuledArgument(rules, values);
    }
}
