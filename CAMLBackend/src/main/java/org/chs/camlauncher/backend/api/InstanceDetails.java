package org.chs.camlauncher.backend.api;

import java.nio.file.Path;
import java.util.List;

/**
 * 管理实例页面需要的实例目录、Mod 和资源包文件信息。
 *
 * @param instanceId        实例 ID
 * @param versionDirectory  versions/&lt;id&gt; 目录
 * @param gameDataDirectory 实际游戏数据目录（版本隔离时是版本目录）
 * @param modsDirectory     mods 目录；即使目录不存在也会返回目标路径
 * @param mods              mods 目录中的已识别文件
 * @param resourcePacksDirectory resourcepacks 目录；即使目录不存在也会返回目标路径
 * @param resourcePacks     resourcepacks 目录中的已识别资源包
 */
public record InstanceDetails(
        String instanceId,
        Path versionDirectory,
        Path gameDataDirectory,
        Path modsDirectory,
        List<InstanceMod> mods,
        Path resourcePacksDirectory,
        List<InstanceResourcePack> resourcePacks
) {
    public InstanceDetails {
        mods = List.copyOf(mods);
        resourcePacks = List.copyOf(resourcePacks);
    }

    public int modCount() {
        return mods.size();
    }

    public int resourcePackCount() {
        return resourcePacks.size();
    }
}
