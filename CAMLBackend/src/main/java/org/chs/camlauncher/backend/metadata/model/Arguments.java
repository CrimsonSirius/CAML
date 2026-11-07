package org.chs.camlauncher.backend.metadata.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public record Arguments(
        @SerializedName("game") List<Argument> game,
        @SerializedName("jvm") List<Argument> jvm
) {
    public Arguments {
        game = game == null ? List.of() : List.copyOf(game);
        jvm = jvm == null ? List.of() : List.copyOf(jvm);
    }

    public static Arguments merge(Arguments base, Arguments overlay) {
        if (base == null) {
            return overlay;
        }
        if (overlay == null) {
            return base;
        }
        return new Arguments(concat(base.game(), overlay.game()), concat(base.jvm(), overlay.jvm()));
    }

    private static <T> List<T> concat(List<T> first, List<T> second) {
        List<T> result = new ArrayList<>(first.size() + second.size());
        result.addAll(first);
        result.addAll(second);
        return List.copyOf(result);
    }
}
