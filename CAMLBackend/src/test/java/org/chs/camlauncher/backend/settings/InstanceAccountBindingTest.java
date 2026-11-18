package org.chs.camlauncher.backend.settings;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 实例账号绑定：键包含游戏目录，改游戏位置后能找回旧绑定并迁移。 */
class InstanceAccountBindingTest {
    @Test
    void decodedBindingsExposeGameDirectory(@TempDir Path dataDirectory) throws Exception {
        SettingsService service = new SettingsService(dataDirectory);
        Path oldDirectory = dataDirectory.resolve("old-game");
        Path newDirectory = dataDirectory.resolve("new-game");
        service.setInstanceAccountId(oldDirectory, "1.21.4-DeepSeek", "yggdrasil:abc");
        service.setInstanceAccountId(oldDirectory, "1.20.1-Vanilla", "offline:def");

        List<SettingsService.InstanceAccountBinding> bindings = service.instanceAccountBindings();

        assertEquals(2, bindings.size());
        assertTrue(bindings.stream().allMatch(binding ->
                binding.gameDirectory().equals(oldDirectory.toAbsolutePath().normalize())));
        assertTrue(bindings.stream().anyMatch(binding ->
                binding.instanceId().equals("1.21.4-DeepSeek")
                        && binding.accountId().equals("yggdrasil:abc")));
        // 新目录此时没有任何绑定。
        assertTrue(service.instanceAccountId(newDirectory, "1.21.4-DeepSeek").isEmpty());
    }

    @Test
    void moveBindingsCopiesOnlyKnownInstances(@TempDir Path dataDirectory) throws Exception {
        SettingsService service = new SettingsService(dataDirectory);
        Path oldDirectory = dataDirectory.resolve("old-game");
        Path newDirectory = dataDirectory.resolve("new-game");
        service.setInstanceAccountId(oldDirectory, "keep-me", "yggdrasil:abc");
        service.setInstanceAccountId(oldDirectory, "drop-me", "yggdrasil:abc");

        int moved = service.moveInstanceAccountBindings(oldDirectory, newDirectory, List.of("keep-me"));

        assertEquals(1, moved);
        assertEquals(Optional.of("yggdrasil:abc"),
                service.instanceAccountId(newDirectory, "keep-me"));
        assertTrue(service.instanceAccountId(newDirectory, "drop-me").isEmpty(),
                "只迁移调用方确认存在的实例");
        // 旧绑定保留：用户可能还会切回旧目录。
        assertEquals(Optional.of("yggdrasil:abc"),
                service.instanceAccountId(oldDirectory, "keep-me"));
    }

    @Test
    void movingToSameDirectoryIsNoop(@TempDir Path dataDirectory) throws Exception {
        SettingsService service = new SettingsService(dataDirectory);
        Path directory = dataDirectory.resolve("game");
        service.setInstanceAccountId(directory, "inst", "yggdrasil:abc");

        assertEquals(0, service.moveInstanceAccountBindings(directory, directory, List.of("inst")));
        assertEquals(Optional.of("yggdrasil:abc"), service.instanceAccountId(directory, "inst"));
    }
}

