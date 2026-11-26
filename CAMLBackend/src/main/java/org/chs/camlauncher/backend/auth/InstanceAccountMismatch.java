package org.chs.camlauncher.backend.auth;

import java.nio.file.Path;
import java.util.List;

/**
 * “实例账号绑定指向另一个游戏位置”的诊断结果。
 *
 * <p>实例账号绑定以“游戏目录 + 实例名”为键。用户修改“游戏位置”后，旧目录下的绑定
 * 不会自动生效（与禁用 Mod 状态的既有语义一致）。这里把仍然存在于新目录、但绑定留在
 * 旧目录的实例列出来，界面据此提示并允许一键重新套用。</p>
 *
 * @param previousGameDirectory 绑定所属的旧游戏目录
 * @param instances             当前游戏目录里存在、且旧目录下有绑定的实例名
 */
public record InstanceAccountMismatch(Path previousGameDirectory, List<String> instances) {
    public InstanceAccountMismatch {
        instances = instances == null ? List.of() : List.copyOf(instances);
    }

    public boolean isEmpty() {
        return instances.isEmpty();
    }

    public int count() {
        return instances.size();
    }
}

