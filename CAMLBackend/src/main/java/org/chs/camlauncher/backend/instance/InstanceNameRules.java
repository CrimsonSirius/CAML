package org.chs.camlauncher.backend.instance;

import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/** 实例名格式和目标目录冲突的统一规则。 */
public final class InstanceNameRules {
    private InstanceNameRules() {
    }

    public static String validate(String instanceId) {
        String value = Objects.requireNonNull(instanceId, "instanceId").trim();
        if (value.isEmpty()) {
            throw new InstanceNameException(BackendError.of("download.setup.nameEmpty"));
        }
        if (value.equals(".") || value.equals("..")) {
            throw new InstanceNameException(BackendError.of("download.setup.nameDotSegments"));
        }
        if (value.endsWith(".") || value.endsWith(" ")) {
            throw new InstanceNameException(BackendError.of("download.setup.nameTrailing"));
        }

        String invalidChars = "<>:\"/\\|?*";
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isISOControl(character) || invalidChars.indexOf(character) >= 0) {
                throw new InstanceNameException(BackendError.of(
                        "download.setup.nameIllegalCharacter", character));
            }
        }
        return value;
    }

    public static InstanceNameCheck check(Path gameDirectory, String instanceId) {
        String value;
        try {
            value = validate(instanceId);
        } catch (InstanceNameException e) {
            return InstanceNameCheck.unavailable(e.error());
        }
        if (gameDirectory == null) {
            return InstanceNameCheck.unavailable(
                    BackendError.of("download.setup.noGameDirectory"));
        }

        Path target = gameDirectory
                .resolve("versions")
                .resolve(value)
                .toAbsolutePath()
                .normalize();
        if (Files.exists(target)) {
            return InstanceNameCheck.unavailable(
                    BackendError.of("download.setup.nameExists", value));
        }
        return InstanceNameCheck.valid();
    }

    public static void ensureNewInstance(Path gameDirectory, String instanceId) throws IOException {
        InstanceNameCheck check = check(gameDirectory, instanceId);
        if (!check.available()) {
            throw new BackendException(check.error());
        }
    }
}
