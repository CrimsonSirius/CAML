package org.chs.camlauncher.backend.metadata;

import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Map;

public record Library(
        @SerializedName("name") String name,
        @SerializedName("url") String url,
        @SerializedName("downloads") Downloads downloads,
        @SerializedName("natives") Map<String, String> natives,
        @SerializedName("extract") Extract extract,
        @SerializedName("rules") List<Rule> rules
) {
    public Library {
        natives = natives == null ? Map.of() : Map.copyOf(natives);
        rules = rules == null ? List.of() : List.copyOf(rules);
    }

    public record Downloads(
            @SerializedName("artifact") DownloadInfo artifact,
            @SerializedName("classifiers") Map<String, DownloadInfo> classifiers
    ) {
        public Downloads {
            classifiers = classifiers == null ? Map.of() : Map.copyOf(classifiers);
        }
    }

    public record Extract(@SerializedName("exclude") List<String> exclude) {
        public Extract {
            exclude = exclude == null ? List.of() : List.copyOf(exclude);
        }
    }
}
