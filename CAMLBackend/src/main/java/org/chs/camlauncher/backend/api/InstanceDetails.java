package org.chs.camlauncher.backend.api;

import java.nio.file.Path;
import java.util.List;

/**
 * 管理实例页面需要的实例目录和 Mod 文件信息。
 *
 * @param instanceId        实例 ID
 * @param versionDirectory  versions/&lt;id&gt; 目录
 * @param gameDataDirectory 实际游戏数据目录（版本隔离时是版本目录）
 * @param modsDirectory     mods 目录；即使目录不存在也会返回目标路径
 * @param mods              mods 目录中的已识别文件
 */
public record InstanceDetails(
        String instanceId,
        Path versionDirectory,
        Path gameDataDirectory,
        Path modsDirectory,
        List<InstanceMod> mods
) {
    public InstanceDetails {
        mods = List.copyOf(mods);
    }

    public int modCount() {
        return mods.size();
    }
}
