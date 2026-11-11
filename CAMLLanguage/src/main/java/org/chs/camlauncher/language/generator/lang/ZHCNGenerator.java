package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/** 简体中文翻译。 */
public final class ZHCNGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "zh_cn";
    }

    @Override
    protected void populate(TranslationTable t) {
        // 通用
        t.put("common.back", "返回");
        t.put("common.cancel", "取消");
        t.put("common.browse", "浏览");
        t.put("common.save", "保存");

        // 顶部导航与底栏
        t.put("nav.home", "主页");
        t.put("nav.downloads", "下载");
        t.put("nav.settings", "设置");
        t.put("app.subtitle", "Minecraft 启动器");
        t.put("footer.status", "CAML %s · 后端%s · 数据目录: %s");
        t.put("footer.backend.ready", "已就绪");
        t.put("footer.backend.notReady", "未就绪");

        // 主页启动表单
        t.put("home.username", "离线用户名");
        t.put("home.launch", "启动游戏");
        t.put("home.launching", "启动中...");
        t.put("home.browseInstances", "浏览游戏实例");
        t.put("home.manageInstances", "管理实例");
        t.put("home.instance.current", "当前实例：%s");
        t.put("home.instance.current.none", "当前实例：未选择");
        t.put("home.gameDirectory.none", "游戏位置：未设置");
        t.put("home.gameDirectory.missing", "游戏位置：目录不存在 · %s");
        t.put("home.gameDirectory.value", "游戏位置：%s");
        t.put("home.saveLocation.none", "存档位置：未设置");
        t.put("home.saveLocation.fixDirectory", "存档位置：请先修复游戏位置");
        t.put("home.saveLocation.selectInstance", "存档位置：请先选择游戏实例");
        t.put("home.saveLocation.value", "存档位置：%s");
        t.put("home.saveLocation.isolated", "（完全版本隔离）");
        t.put("home.error.inspectDirectory", "检查游戏目录失败：%s");
        t.put("home.error.noGameDirectory", "请先在设置中保存游戏位置");
        t.put("home.error.missingGameDirectory", "游戏位置不存在，请到设置中保存有效目录");
        t.put("home.error.noInstance", "请先浏览并选择游戏实例");
        t.put("home.error.noUsername", "请填写离线用户名");

        // 日志面板
        t.put("log.title", "运行日志");
        t.put("log.subtitle", "下载进度与游戏进程输出");
        t.put("log.accessible", "运行日志");
        t.put("log.download.installed.vanilla", "[下载] 已安装实例 %s（原版 %s）");
        t.put("log.download.installed.fabric", "[下载] 已安装 Fabric 实例 %s（Loader %s）");
        t.put("log.download.installed.loader", "[下载] 已安装 %s 实例 %s");
        t.put("log.download.progress", "[下载] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[下载] %s (%d)");
        t.put("log.gameProcess.exit", "游戏进程已退出，代码: %d");
        t.put("log.gameProcess.exitError", "游戏进程异常退出，代码: %d");
        t.put("log.error.operationFailed", "[%s] 失败: %s");

        // 启动状态
        t.put("status.starting", "正在启动 %s...");
        t.put("status.started", "启动指令已发送");
        t.put("status.launchFailed", "启动失败！");
        t.put("status.launchFailedDetail", "启动失败：%s");
        t.put("error.unknown", "发生未知错误，请查看日志。");
        t.put("download.error.failed", "无法下载 %s（已尝试 %s 次，最后地址: %s）");
        t.put("network.requestFailed", "无法请求 %s（最后地址: %s）");
        t.put("network.timeout", "请求超时（%s ms）：%s");
        t.put("network.httpError", "HTTP %s：%s");
        t.put("network.responseInvalid", "无法解析响应：%s");
        t.put("network.redirectLocationMissing", "重定向缺少 Location：%s");
        t.put("network.tooManyRedirects", "重定向次数过多：%s");
        t.put("network.noSource", "没有可用的下载源：%s");
        t.put("metadata.manifestEmpty", "版本清单为空");
        t.put("metadata.versionJsonEmpty", "版本 JSON 为空");
        t.put("metadata.versionJsonHashMismatch", "版本 JSON SHA-1 校验失败：%s");
        t.put("launch.permissionDenied", "没有权限执行 Java 或访问游戏目录，请检查文件权限。");
        t.put("launch.executableNotFound", "找不到 Java 可执行文件，请重新选择 Java。");
        t.put("launch.commandTooLong", "启动命令过长，请缩短游戏路径或减少类路径。");
        t.put("launch.commandTooLongAfterFallback", "启动命令过长，已尝试参数文件和 manifest 回退，但仍无法启动。请缩短游戏路径或减少类路径。");
        t.put("launch.processStartFailed", "启动游戏进程失败，请查看日志了解详情。");
        t.put("launch.manualJavaNotConfigured", "已启用手动 Java 选择，但没有选择 Java 路径。");
        t.put("launch.manualJavaUnavailable", "手动选择的 Java 不可用：%s");
        t.put("launch.javaRequirementNotFound", "找不到满足版本要求的 Java %s，请安装或选择 Java。");

        // 下载版本类别与来源
        t.put("download.category.release", "正式版");
        t.put("download.category.snapshot", "快照版");
        t.put("download.category.old", "远古版");
        t.put("download.category.unknown", "未知版本");
        t.put("download.source.mirror", "镜像源");
        t.put("download.source.official", "官方源");
        t.put("download.source.custom", "自定义源");

        // 版本列表
        t.put("download.versions.loading", "正在从%1$s获取%2$s列表...");
        t.put("download.versions.empty", "下载源没有返回可下载的%s");
        t.put("download.versions.count", "共 %d 个%s，按发布时间从新到旧排列");
        t.put("download.versions.error", "获取%s列表失败：%s");
        t.put("download.versions.accessible", "%s，%s");

        // 下载内容描述
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // 下载设置页
        t.put("download.setup.status.idle", "未选择加载器时下载原版游戏。");
        t.put("download.setup.instanceName", "实例名字");
        t.put("download.setup.download", "下载实例");
        t.put("download.setup.option.versionInfo", "版本信息");
        t.put("download.setup.option.forge", "选择 Forge 版本");
        t.put("download.setup.option.optifine", "选择 OptiFine 版本");
        t.put("download.setup.option.fabric", "选择 Fabric 版本");
        t.put("download.setup.option.fabricApi", "选择 Fabric API 版本");
        t.put("download.setup.option.neoforge", "选择 NeoForge 版本");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge 与 OptiFine 不兼容，已自动切换 Forge %s");
        t.put("download.setup.optifineIncompatibleForge", "当前 Forge 与 OptiFine 不兼容");
        t.put("download.setup.noCompatibleOptiFine", "没有兼容当前 Forge 的 OptiFine 版本");
        t.put("download.setup.optifineForgeConflict", "Forge %s 与 OptiFine %s 不兼容，请选择 Forge %s 或更早版本。");
        t.put("download.setup.nameEmpty", "实例名字不能为空");
        t.put("download.setup.nameDotSegments", "实例名字不能是 . 或 ..");
        t.put("download.setup.nameTrailing", "实例名字不能以点或空格结尾");
        t.put("download.setup.nameIllegalCharacter", "实例名字包含非法字符: %s");
        t.put("download.setup.nameExists", "实例名已存在：%s，请换一个名字。");
        t.put("download.setup.noGameDirectory", "尚未设置游戏位置");
        t.put("download.setup.checkingName", "正在检查实例名...");
        t.put("download.setup.nameSameAsLoader", "安装加载器时实例名不能与游戏版本号相同");
        t.put("download.setup.nameAvailable", "实例名可用");
        t.put("download.setup.checkNameFailed", "检查实例名失败：%s");
        t.put("download.setup.nameNotChecked", "实例名尚未通过检查，请稍候再试。");
        t.put("download.setup.noGameDirectoryLong", "尚未设置游戏位置，请先到设置中保存游戏位置。");
        t.put("download.setup.optifineRequiresForge", "OptiFine 只能与 Forge 一起安装。");

        // 加载器下拉框提示
        t.put("download.prompt.loading.fabric", "正在获取 Fabric 版本...");
        t.put("download.prompt.loading.forge", "正在获取 Forge 版本...");
        t.put("download.prompt.loading.neoforge", "正在获取 NeoForge 版本...");
        t.put("download.prompt.fabricFirst", "请先选择 Fabric 版本");
        t.put("download.prompt.forgeFirst", "请先选择 Forge 版本");
        t.put("download.prompt.fabricApiFirst", "请先选择 Fabric 版本");
        t.put("download.prompt.select.fabric", "请选择 Fabric 版本");
        t.put("download.prompt.select.forge", "请选择 Forge 版本");
        t.put("download.prompt.select.neoforge", "请选择 NeoForge 版本");
        t.put("download.prompt.select.optifine", "请选择 OptiFine 版本");
        t.put("download.prompt.select.fabricApi", "请选择 Fabric API 版本");
        t.put("download.prompt.empty.fabric", "没有可用的 Fabric 版本");
        t.put("download.prompt.empty.fabricApi", "没有可用的 Fabric API 版本");
        t.put("download.prompt.empty.forge", "没有可用的 Forge 版本");
        t.put("download.prompt.empty.neoforge", "没有可用的 NeoForge 版本");
        t.put("download.prompt.empty.optifine", "没有可用的 OptiFine 版本");
        t.put("download.prompt.error.fabric", "获取 Fabric 版本失败");
        t.put("download.prompt.error.fabricApi", "获取 Fabric API 版本失败");
        t.put("download.prompt.error.forge", "获取 Forge 版本失败");
        t.put("download.prompt.error.neoforge", "获取 NeoForge 版本失败");
        t.put("download.prompt.error.optifine", "获取 OptiFine 版本失败");

        // 下载进度页
        t.put("download.progress.title", "正在下载 %s");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "返回下载列表");
        t.put("download.progress.speed", "网络速度");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "下载进度");
        t.put("download.progress.preparing", "准备下载");
        t.put("download.progress.waiting", "等待下载任务...");
        t.put("download.progress.completed", "下载完成");
        t.put("download.progress.completedTotal", "下载已完成");
        t.put("download.progress.total", "累计 %s");
        t.put("download.progress.totalWithMax", "累计 %s / %s");
        t.put("download.progress.failed", "下载失败：%s");
        t.put("download.progress.currentFile", "当前文件：%s");
        t.put("download.progress.currentFileWithBytes", "当前文件：%s · %s / %s");
        t.put("download.progress.finishedHint", "实例已安装完成，可以返回列表。");
        t.put("download.progress.failedHint", "下载已停止，请返回列表后重试。");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · 文件 %d / %d");
        t.put("download.progress.filesDetail", "文件 %d / %d");
        t.put("download.progress.planning", "正在准备下载计划...");

        // 下载阶段
        t.put("download.stage.preparing", "准备下载");
        t.put("download.stage.completed", "完成");
        t.put("download.stage.failed", "下载失败");
        t.put("download.stage.unknownError", "未知错误");
        t.put("download.stage.version.fetch", "获取版本信息");
        t.put("download.stage.version.checkLocal", "检查本地版本");
        t.put("download.stage.version.downloadFiles", "下载游戏文件与依赖库");
        t.put("download.stage.version.downloadAssets", "下载资源文件");
        t.put("download.stage.version.writeJson", "写入版本信息");
        t.put("download.stage.loader.installer.prepare", "准备 %s 安装器");
        t.put("download.stage.loader.installer.download", "下载 %s 安装器");
        t.put("download.stage.loader.installer.run", "运行 %s 安装器");
        t.put("download.stage.loader.libraries.download", "下载 %s 依赖库");
        t.put("download.stage.loader.files.merge", "合并 %s 文件");
        t.put("download.stage.loader.verify", "校验 %s 安装结果");
        t.put("download.stage.fabric.config.fetch", "获取 Fabric 配置");
        t.put("download.stage.fabric.loader.install", "安装 Fabric Loader");
        t.put("download.stage.fabric.api.download", "下载 Fabric API");
        t.put("download.stage.fabric.verify", "校验 Fabric 安装结果");
        t.put("download.stage.optifine.installer.download", "下载 OptiFine 安装器");
        t.put("download.stage.optifine.installer.run", "运行 OptiFine 安装器");
        t.put("download.stage.optifine.files.merge", "合并 OptiFine 文件");

        // 下载设置
        t.put("download.settings.source", "下载源");
        t.put("download.settings.source.description", "选择版本、库和资源使用的优先下载地址。");
        t.put("download.settings.source.mirror", "尽可能使用镜像源");
        t.put("download.settings.source.official", "尽可能使用官方源");
        t.put("download.settings.source.custom", "导入自定义 API");
        t.put("download.settings.customApi.placeholder", "例如 https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "设定下载并发数");
        t.put("download.settings.concurrency.description", "同时下载的文件数量，范围 1–256。");
        t.put("download.settings.concurrency.label", "下载并发数");
        t.put("download.settings.concurrency.value", "当前并发数：%d");
        t.put("download.settings.concurrency.accessible", "下载并发数");

        // 设置页导航
        t.put("settings.section.game", "游戏设置");
        t.put("settings.section.download", "下载设置");
        t.put("settings.section.appearance", "外观");

        // 游戏位置
        t.put("settings.game.location.title", "游戏位置");
        t.put("settings.game.location.description", "设置 Minecraft 游戏数据目录。手动输入后必须点击保存才会生效。");
        t.put("settings.game.location.browse", "浏览");
        t.put("settings.game.location.save", "保存");
        t.put("settings.game.location.chooserTitle", "选择游戏位置");
        t.put("settings.game.location.notSaved", "尚未保存");
        t.put("settings.game.location.empty", "游戏位置不能为空");
        t.put("settings.game.location.invalid", "游戏位置格式无效");
        t.put("settings.game.location.checking", "正在检查目录...");
        t.put("settings.game.location.missing", "目录不存在：%s");
        t.put("settings.game.location.saved", "游戏位置已保存");
        t.put("settings.game.location.checkFailed", "检查目录失败：%s");

        // 内存
        t.put("settings.game.memory.title", "内存");
        t.put("settings.game.memory.accessible", "最大内存");
        t.put("settings.game.memory.description", "调整游戏启动时使用的内存。");
        t.put("settings.game.memory.label", "内存分配");
        t.put("settings.game.memory.hint", "拖动小球调整游戏分配；灰色条为已使用 + 游戏分配，右侧淡灰为未使用内存。");
        t.put("settings.game.memory.summary", "已使用 %d MB / 游戏 %d MB / 可用 %d MB / 总内存 %d MB");

        // 版本隔离
        t.put("settings.game.isolation.title", "版本隔离");
        t.put("settings.game.isolation.full", "完全版本隔离");
        t.put("settings.game.isolation.none", "不进行版本隔离");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "选择游戏启动时使用的 Java 运行时。自动选择会按游戏版本要求挑选可用 Java。");
        t.put("settings.game.java.auto", "自动选择");
        t.put("settings.game.java.manual", "手动选择");
        t.put("settings.game.java.rescan", "重新扫描");
        t.put("settings.game.java.add", "添加 Java");
        t.put("settings.game.java.scanning", "正在扫描 Java...");
        t.put("settings.game.java.scanningAll", "正在扫描整机 Java...");
        t.put("settings.game.java.scanFailedPrompt", "扫描 Java 失败");
        t.put("settings.game.java.scanFailed", "扫描 Java 失败：%s");
        t.put("settings.game.java.noneFound", "没有发现 Java");
        t.put("settings.game.java.select", "请选择 Java");
        t.put("settings.game.java.autoSelected", "自动选择");
        t.put("settings.game.java.chooserTitle", "选择 Java 可执行文件");
        t.put("settings.game.java.checking", "正在检查 Java...");
        t.put("settings.game.java.addFailed", "添加 Java 失败：%s");
        t.put("settings.game.java.autoHint", "自动选择会从扫描结果中挑选满足当前游戏版本要求的 Java。");
        t.put("settings.game.java.selectOrAdd", "请选择一个 Java，或点击“添加 Java”。");
        t.put("settings.game.java.unavailable", "该 Java 当前不可用：%s");
        t.put("settings.game.java.detail", "版本 %s · %s · %s · %d 位%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // 外观
        t.put("settings.appearance.title", "外观");
        t.put("settings.appearance.description", "调整启动器自身的外观。");
        t.put("settings.appearance.themeHint", "主题来自 ~/.caml/themes，切换后立即生效");
        t.put("settings.appearance.theme", "主题");
        t.put("settings.appearance.theme.accessible", "主题");
        t.put("settings.appearance.language.title", "语言");
        t.put("settings.appearance.language.description", "选择启动器界面语言的显示方式。");
        t.put("settings.appearance.language.auto", "自动");
        t.put("settings.appearance.language.manual", "手动");
        t.put("settings.appearance.language.label", "界面语言");
        t.put("settings.appearance.language.hint", "自动模式跟随系统语言；手动模式使用下方选择的语言。");
        t.put("settings.appearance.language.accessible", "界面语言");

        // 实例选择
        t.put("instances.picker.scanning", "正在扫描游戏实例...");
        t.put("instances.picker.title", "选择游戏实例");
        t.put("instances.picker.noGameDirectory", "尚未设置游戏位置");
        t.put("instances.picker.gameDirectory", "游戏位置：%s");
        t.put("instances.picker.noGameDirectoryHint", "尚未设置游戏位置，请先到设置中保存游戏位置");
        t.put("instances.picker.missing", "游戏位置不存在：%s");
        t.put("instances.picker.empty", "当前游戏位置没有已安装实例");
        t.put("instances.picker.count", "共 %d 个游戏实例");
        t.put("instances.picker.scanFailed", "扫描游戏实例失败：%s");
        t.put("instances.manage.title", "管理实例");

        // 后台操作名
        t.put("operation.inspectGameDirectory", "检查游戏目录");
        t.put("operation.checkInstanceName", "检查实例名");
        t.put("operation.fetchVersionList", "获取官方%s列表");
        t.put("operation.installVanilla", "安装原版实例 %s");
        t.put("operation.fetchFabricLoader", "获取 Fabric Loader 版本");
        t.put("operation.fetchFabricApi", "获取 Fabric API 版本");
        t.put("operation.installFabric", "安装 Fabric 实例 %s");
        t.put("operation.fetchForge", "获取 Forge 版本");
        t.put("operation.fetchNeoForge", "获取 NeoForge 版本");
        t.put("operation.fetchOptiFine", "获取 OptiFine 版本");
        t.put("operation.installLoader", "安装 %s 实例 %s");
        t.put("operation.scanJava", "扫描 Java");
        t.put("operation.addManualJava", "添加手动 Java");
        t.put("operation.saveJavaSelection", "保存 Java 选择");
        t.put("operation.saveMaxMemory", "保存最大内存设置");
        t.put("operation.saveGameDirectory", "保存游戏位置");
        t.put("operation.saveVersionIsolation", "保存版本隔离设置");
        t.put("operation.saveDownloadConcurrency", "保存下载并发数");
        t.put("operation.saveDownloadSource", "保存下载源设置");
        t.put("operation.saveTheme", "保存主题选择");
        t.put("operation.saveLanguage", "保存语言设置");
        t.put("operation.saveLastInstance", "保存最后启动实例");
        t.put("operation.launchGame", "启动游戏 %s");
    }
}
