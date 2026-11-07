package org.chs.camlauncher.backend.launch.command;

import org.chs.camlauncher.backend.launch.LaunchPlan;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Path;

public final class LaunchPlaceholders {
    public static final String LAUNCHER_NAME = "CAML";
    public static final String LAUNCHER_VERSION = "1.0-SNAPSHOT";

    private LaunchPlaceholders() {
    }

    public static Map<String, String> create(LaunchPlan plan, List<Path> classpath) {
        Map<String, String> values = new LinkedHashMap<>();
        String classpathValue = String.join(
                File.pathSeparator,
                classpath.stream().map(Path::toString).toList()
        );
        String assetsIndex = plan.manifest().assetIndex() != null
                ? plan.manifest().assetIndex().id()
                : plan.manifest().assets();

        values.put("auth_player_name", plan.auth().username());
        values.put("version_name", plan.manifest().id());
        values.put("game_directory", plan.gameDirectory().toString());
        values.put("assets_root", plan.assetsDirectory().toString());
        values.put("assets_index_name", assetsIndex == null ? "" : assetsIndex);
        values.put("auth_uuid", plan.auth().uuid().toString().replace("-", ""));
        values.put("auth_access_token", plan.auth().accessToken());
        values.put("auth_session", plan.auth().accessToken());
        values.put("user_type", plan.auth().userType());
        values.put("user_properties", plan.auth().userProperties());
        values.put("version_type", plan.manifest().type() == null ? "release" : plan.manifest().type());
        values.put("natives_directory", plan.nativesDirectory().toString());
        values.put("classpath", classpathValue);
        values.put("launcher_name", LAUNCHER_NAME);
        values.put("launcher_version", LAUNCHER_VERSION);
        values.put("library_directory", plan.instance().librariesDirectory().toString());
        values.put("classpath_separator", File.pathSeparator);
        values.put("primary_jar", plan.instance().clientJar().toString());
        values.put("primary_jar_name", plan.instance().clientJar().getFileName().toString());
        values.put("game_assets", plan.assetsDirectory().toString());
        values.put("resolution_width", plan.options().width() == null ? "" : plan.options().width().toString());
        values.put("resolution_height", plan.options().height() == null ? "" : plan.options().height().toString());
        return Map.copyOf(values);
    }

    public static Map<String, Boolean> features(LaunchPlan plan) {
        return Map.of(
                "has_custom_resolution",
                plan.options().width() != null && plan.options().height() != null
        );
    }
}
