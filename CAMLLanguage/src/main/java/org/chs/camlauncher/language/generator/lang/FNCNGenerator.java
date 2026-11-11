package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/*地道中国话翻译*/
public final class FNCNGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "fn_cn";
    }

    @Override
    protected void populate(TranslationTable t) {
        // 通用
        t.put("common.back", "后面");
        t.put("common.cancel", "作废");
        t.put("common.browse", "吃草");
        t.put("common.save", "拯救");

        // 顶部导航与底栏
        t.put("nav.home", "家");
        t.put("nav.downloads", "向下装载们");
        t.put("nav.settings", "安放们");
        t.put("app.subtitle", "Minecraft 发射器");
        t.put("footer.status", "CAML %s · 后部末端 %s · 数据簿：%s");
        t.put("footer.backend.ready", "后部末端已开窍");
        t.put("footer.backend.notReady", "后部末端未开窍");

        // 主页启动表单
        t.put("home.username", "家·用户名字");
        t.put("home.launch", "发射比赛");
        t.put("home.launching", "正在发射中...");
        t.put("home.browseInstances", "吃草事例们");
        t.put("home.manageInstances", "操纵事例们");
        t.put("home.instance.current", "当前事例：%s");
        t.put("home.instance.current.none", "当前事例：未被揪出");
        t.put("home.gameDirectory.none", "比赛名录：未被安放");
        t.put("home.gameDirectory.missing", "比赛名录：走失了 · %s");
        t.put("home.gameDirectory.value", "比赛名录：%s");
        t.put("home.saveLocation.none", "拯救位置：未被安放");
        t.put("home.saveLocation.fixDirectory", "拯救位置：请先修理比赛名录");
        t.put("home.saveLocation.selectInstance", "拯救位置：请先揪出比赛事例");
        t.put("home.saveLocation.value", "拯救位置：%s");
        t.put("home.saveLocation.isolated", "（完整译本已被孤立法关起来）");
        t.put("home.error.inspectDirectory", "检查比赛名录失败：%s");
        t.put("home.error.noGameDirectory", "请先在安放们里拯救比赛名录");
        t.put("home.error.missingGameDirectory", "比赛名录走丢了，请去安放们里拯救一个有效的名录");
        t.put("home.error.noInstance", "请先吃草并揪出一个比赛事例");
        t.put("home.error.noUsername", "请填写离线用户名字");

        // 日志面板
        t.put("log.title", "原木标题");
        t.put("log.subtitle", "向下装载进度与比赛过程吐出");
        t.put("log.accessible", "跑路原木");
        t.put("log.download.installed.vanilla", "[向下装载] 已经打进事例 %s（香草 %s）");
        t.put("log.download.installed.fabric", "[向下装载] 已经打进布料事例 %s（布料装载器 %s）");
        t.put("log.download.installed.loader", "[向下装载] 已经打进 %s 事例 %s");
        t.put("log.download.progress", "[向下装载] %s（%d/%d）");
        t.put("log.download.progressNoTotal", "[向下装载] %s（%d）");
        t.put("log.gameProcess.exit", "比赛过程已经走掉，法典：%d");
        t.put("log.gameProcess.exitError", "比赛过程异常走掉，法典：%d");
        t.put("log.error.operationFailed", "[%s] 失败：%s");

        // 启动状态与错乱
        t.put("status.starting", "正在起始 %s...");
        t.put("status.started", "发射命令已经送出");
        t.put("status.launchFailed", "发射失败！");
        t.put("status.launchFailedDetail", "发射仆街：%s");
        t.put("error.unknown", "撞见无名错乱，请去翻阅原木。");
        t.put("download.error.failed", "没法向下装载 %s（已经试了 %s 次，最后住址: %s）");
        t.put("network.requestFailed", "没法讨要 %s（最后住址: %s）");
        t.put("network.timeout", "讨要超时（%s 毫秒）：%s");
        t.put("network.httpError", "HTTP %s：%s");
        t.put("network.responseInvalid", "没法拆解回音：%s");
        t.put("network.redirectLocationMissing", "掉头缺少 住址：%s");
        t.put("network.tooManyRedirects", "掉头次数太多：%s");
        t.put("network.noSource", "没有能用的向下装载源头：%s");
        t.put("metadata.manifestEmpty", "译本清单是空气");
        t.put("metadata.versionJsonEmpty", "译本 JSON 是空气");
        t.put("metadata.versionJsonHashMismatch", "译本 JSON 的 SHA-1 穿错鞋：%s");
        t.put("launch.permissionDenied", "没有通行证处决爪哇或拜访比赛名录，请检查公文通行证。");
        t.put("launch.executableNotFound", "撞不见爪哇能处决的公文，请重新揪出爪哇。");
        t.put("launch.commandTooLong", "发射命令太长，请截短比赛小径或减少类别小径。");
        t.put("launch.commandTooLongAfterFallback", "发射命令太长，已经试过参数公文和 manifest 退堂鼓，但还是没法发射。请截短比赛小径或减少类别小径。");
        t.put("launch.processStartFailed", "发射比赛过程仆街，请去翻阅原木了解内幕。");
        t.put("launch.manualJavaNotConfigured", "已经开窍肉手爪哇揪出，但没有揪出爪哇小径。");
        t.put("launch.manualJavaUnavailable", "肉手揪出的爪哇不能用：%s");
        t.put("launch.javaRequirementNotFound", "撞不见满足译本要求的爪哇 %s，请打进或揪出爪哇。");

        // 下载版本类别与来源
        t.put("download.category.release", "释放版");
        t.put("download.category.snapshot", "快照版");
        t.put("download.category.old", "老版");
        t.put("download.category.unknown", "无名版");
        t.put("download.source.mirror", "镜子来源");
        t.put("download.source.official", "朝廷源头");
        t.put("download.source.custom", "海关来源");

        // 版本列表
        t.put("download.versions.loading", "正在从 %1$s 获取 %2$s 列表...");
        t.put("download.versions.empty", "向下装载来源没有送回能装载的 %s");
        t.put("download.versions.count", "总共 %d 个 %s，依据释放时钟由崭新向老旧规整");
        t.put("download.versions.error", "获取 %s 列表失败：%s");
        t.put("download.versions.accessible", "%s，%s");

        // 下载内容描述
        t.put("download.description.optifine", "铁砧 %s + 光罚款 %s");
        t.put("download.description.forge", "铁砧 %s");
        t.put("download.description.fabric", "布料装载器 %s");
        t.put("download.description.fabricWithApi", "布料装载器 %s + 布料应用编程接口 %s");
        t.put("download.description.neoforge", "新铁砧 %s");
        t.put("download.description.vanilla", "香草 Minecraft %s");

        // 下载设置页
        t.put("download.setup.status.idle", "没有揪出装载器时，会向下装载香草比赛。");
        t.put("download.setup.instanceName", "事例名字");
        t.put("download.setup.download", "向下装载事例");
        t.put("download.setup.option.versionInfo", "译本情报");
        t.put("download.setup.option.forge", "揪出铁砧版本");
        t.put("download.setup.option.optifine", "揪出光罚款版本");
        t.put("download.setup.option.fabric", "揪出布料版本");
        t.put("download.setup.option.fabricApi", "揪出布料应用编程接口版本");
        t.put("download.setup.option.neoforge", "揪出新铁砧版本");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "铁砧和光罚款不相容，已自动拨动到铁砧 %s");
        t.put("download.setup.optifineIncompatibleForge", "当前铁砧与光罚款不相容");
        t.put("download.setup.noCompatibleOptiFine", "没有跟当前铁砧相容的光罚款版本");
        t.put("download.setup.optifineForgeConflict", "铁砧 %s 和 光罚款 %s 犯冲，请揪出铁砧 %s 或更早的译本。");
        t.put("download.setup.nameEmpty", "事例名字不能是空气");
        t.put("download.setup.nameDotSegments", "事例名字不能是 . 或 ..");
        t.put("download.setup.nameTrailing", "事例名字不能以点或空白收尾");
        t.put("download.setup.nameIllegalCharacter", "事例名字夹带违法字眼: %s");
        t.put("download.setup.nameExists", "事例名字已经被抢注：%s，请换一个名字。");
        t.put("download.setup.noGameDirectory", "比赛名录还没被安放");
        t.put("download.setup.checkingName", "正在检查事例名字...");
        t.put("download.setup.nameSameAsLoader", "打进装载器时，事例名字不能跟比赛版本穿一条裤子");
        t.put("download.setup.nameAvailable", "事例名字能上岗");
        t.put("download.setup.checkNameFailed", "检查事例名字失败：%s");
        t.put("download.setup.nameNotChecked", "事例名字还没通过检查，请稍后再试。");
        t.put("download.setup.noGameDirectoryLong", "比赛名录还没被安放，请先到安放们里拯救比赛名录。");
        t.put("download.setup.optifineRequiresForge", "光罚款只能跟铁砧一起打进。");

        // 加载器下拉框提示
        t.put("download.prompt.loading.fabric", "正在获取布料版本...");
        t.put("download.prompt.loading.forge", "正在获取铁砧版本...");
        t.put("download.prompt.loading.neoforge", "正在获取新铁砧版本...");
        t.put("download.prompt.fabricFirst", "请先揪出布料版本");
        t.put("download.prompt.forgeFirst", "请先揪出铁砧版本");
        t.put("download.prompt.fabricApiFirst", "请先揪出布料版本");
        t.put("download.prompt.select.fabric", "请揪出布料版本");
        t.put("download.prompt.select.forge", "请揪出铁砧版本");
        t.put("download.prompt.select.neoforge", "请揪出新铁砧版本");
        t.put("download.prompt.select.optifine", "请揪出光罚款版本");
        t.put("download.prompt.select.fabricApi", "请揪出布料应用编程接口版本");
        t.put("download.prompt.empty.fabric", "没有能用的布料版本");
        t.put("download.prompt.empty.fabricApi", "没有能用的布料应用编程接口版本");
        t.put("download.prompt.empty.forge", "没有能用的铁砧版本");
        t.put("download.prompt.empty.neoforge", "没有能用的新铁砧版本");
        t.put("download.prompt.empty.optifine", "没有能用的光罚款版本");
        t.put("download.prompt.error.fabric", "获取布料版本失败");
        t.put("download.prompt.error.fabricApi", "获取布料应用编程接口版本失败");
        t.put("download.prompt.error.forge", "获取铁砧版本失败");
        t.put("download.prompt.error.neoforge", "获取新铁砧版本失败");
        t.put("download.prompt.error.optifine", "获取光罚款版本失败");

        // 下载进度页
        t.put("download.progress.title", "正在向下装载 %s");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "退回向下装载清单");
        t.put("download.progress.speed", "网兜速度");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "向下装载进度");
        t.put("download.progress.preparing", "备马向下装载");
        t.put("download.progress.waiting", "正在坐等向下装载活干...");
        t.put("download.progress.completed", "向下装载干掉");
        t.put("download.progress.completedTotal", "向下装载已经全部干掉");
        t.put("download.progress.total", "堆积 %s");
        t.put("download.progress.totalWithMax", "堆积 %s / %s");
        t.put("download.progress.failed", "向下装载仆街：%s");
        t.put("download.progress.currentFile", "当前公文：%s");
        t.put("download.progress.currentFileWithBytes", "当前公文：%s · %s / %s");
        t.put("download.progress.finishedHint", "事例已经打进，可以退回清单。");
        t.put("download.progress.failedHint", "向下装载已经停下，请退回清单后再试。");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · 公文 %d / %d");
        t.put("download.progress.filesDetail", "公文 %d / %d");
        t.put("download.progress.planning", "正在备马向下装载计谋...");

        // 下载阶段
        t.put("download.stage.preparing", "备马向下装载");
        t.put("download.stage.completed", "干掉");
        t.put("download.stage.failed", "向下装载仆街");
        t.put("download.stage.unknownError", "无名错乱");
        t.put("download.stage.version.fetch", "获取译本情报");
        t.put("download.stage.version.checkLocal", "检查本地译本");
        t.put("download.stage.version.downloadFiles", "向下装载比赛公文与依赖功能书架");
        t.put("download.stage.version.downloadAssets", "向下装载肥水公文");
        t.put("download.stage.version.writeJson", "刻入译本情报");
        t.put("download.stage.loader.installer.prepare", "备马 %s 打进器");
        t.put("download.stage.loader.installer.download", "向下装载 %s 打进器");
        t.put("download.stage.loader.installer.run", "跑路 %s 打进器");
        t.put("download.stage.loader.libraries.download", "向下装载 %s 依赖功能书架");
        t.put("download.stage.loader.files.merge", "揉在一起 %s 公文");
        t.put("download.stage.loader.verify", "查验 %s 打进后果");
        t.put("download.stage.fabric.config.fetch", "获取 布料 配置");
        t.put("download.stage.fabric.loader.install", "打进 布料装载器");
        t.put("download.stage.fabric.api.download", "向下装载 布料应用编程接口");
        t.put("download.stage.fabric.verify", "查验 布料 打进后果");
        t.put("download.stage.optifine.installer.download", "向下装载 光罚款 打进器");
        t.put("download.stage.optifine.installer.run", "跑路 光罚款 打进器");
        t.put("download.stage.optifine.files.merge", "揉在一起 光罚款 公文");

        // 下载设置
        t.put("download.settings.source", "向下装载来源");
        t.put("download.settings.source.description", "揪出版本、功能书架和肥水使用的优先向下装载住址。");
        t.put("download.settings.source.mirror", "拼了老命使用镜子来源");
        t.put("download.settings.source.official", "拼了老命使用朝廷源头");
        t.put("download.settings.source.custom", "吸进海关接缝");
        t.put("download.settings.customApi.placeholder", "打个比方 https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "摆弄向下装载并发数");
        t.put("download.settings.concurrency.description", "同时向下装载的公文数量，地盘 1–256。");
        t.put("download.settings.concurrency.label", "向下装载并发数");
        t.put("download.settings.concurrency.value", "当前并发数：%d");
        t.put("download.settings.concurrency.accessible", "向下装载并发数");

        // 设置页导航
        t.put("settings.section.game", "比赛安放们");
        t.put("settings.section.download", "向下装载安放们");
        t.put("settings.section.appearance", "外表");

        // 游戏位置
        t.put("settings.game.location.title", "比赛位置");
        t.put("settings.game.location.description", "摆弄 Minecraft 比赛情报名录。肉手敲完必须戳一下拯救才会发芽。");
        t.put("settings.game.location.browse", "吃草");
        t.put("settings.game.location.save", "拯救");
        t.put("settings.game.location.chooserTitle", "揪出比赛位置");
        t.put("settings.game.location.notSaved", "尚未被拯救");
        t.put("settings.game.location.empty", "比赛位置不能是空气");
        t.put("settings.game.location.invalid", "比赛位置排版不合乎王法");
        t.put("settings.game.location.checking", "正在检查名录...");
        t.put("settings.game.location.missing", "名录走失：%s");
        t.put("settings.game.location.saved", "比赛位置已经被拯救");
        t.put("settings.game.location.checkFailed", "检查名录失败：%s");

        // 内存
        t.put("settings.game.memory.title", "记忆");
        t.put("settings.game.memory.accessible", "最大记忆");
        t.put("settings.game.memory.description", "拨弄发射比赛时使用的记忆。");
        t.put("settings.game.memory.label", "记忆分配");
        t.put("settings.game.memory.hint", "拖拽把手拨弄比赛记忆分配；灰色棍子是已榨干 + 比赛记忆，右边浅灰色是没用过的记忆。");
        t.put("settings.game.memory.summary", "已榨干 %d MB / 比赛 %d MB / 能榨 %d MB / 底朝天 %d MB");

        // 版本隔离
        t.put("settings.game.isolation.title", "译本孤立法");
        t.put("settings.game.isolation.full", "完整译本孤立法");
        t.put("settings.game.isolation.none", "不施行译本孤立法");

        // Java
        t.put("settings.game.java.title", "爪哇");
        t.put("settings.game.java.description", "揪出发射比赛时使用的爪哇跑路包围圈。自动的会按照比赛版本要求揪出一个能用的爪哇。");
        t.put("settings.game.java.auto", "自动的");
        t.put("settings.game.java.manual", "肉手的");
        t.put("settings.game.java.rescan", "重新用光扫");
        t.put("settings.game.java.add", "硬塞爪哇");
        t.put("settings.game.java.scanning", "正在用光扫爪哇...");
        t.put("settings.game.java.scanningAll", "正在用光扫体制里的爪哇...");
        t.put("settings.game.java.scanFailedPrompt", "用光扫爪哇失败");
        t.put("settings.game.java.scanFailed", "用光扫爪哇失败：%s");
        t.put("settings.game.java.noneFound", "没有撞见爪哇");
        t.put("settings.game.java.select", "请揪出一个爪哇");
        t.put("settings.game.java.autoSelected", "自动的");
        t.put("settings.game.java.chooserTitle", "揪出爪哇能处决的公文");
        t.put("settings.game.java.checking", "正在检查爪哇...");
        t.put("settings.game.java.addFailed", "硬塞爪哇失败：%s");
        t.put("settings.game.java.autoHint", "自动的会从用光扫结果里揪出符合当前比赛版本的爪哇。");
        t.put("settings.game.java.selectOrAdd", "请揪出一个爪哇，或者戳「硬塞爪哇」。");
        t.put("settings.game.java.unavailable", "这个爪哇现在不能被用：%s");
        t.put("settings.game.java.detail", "版本 %s · %s · %s · %d 位%s");
        t.put("settings.game.java.jdk", " · 爪哇劈开干活的袋子");
        t.put("settings.game.java.jre", " · 爪哇跑路包围圈");

        // 外表（外观）与舌头（语言）
        t.put("settings.appearance.title", "外表");
        t.put("settings.appearance.description", "拨弄发射器自己的外表。");
        t.put("settings.appearance.themeHint", "主旋律来自 ~/.caml/themes，拨动之后立刻发芽");
        t.put("settings.appearance.theme", "命题作文");
        t.put("settings.appearance.theme.accessible", "主旋律");

        t.put("settings.appearance.language.title", "舌头");
        t.put("settings.appearance.language.description", "揪出发射器外表吐出的舌头方言。");
        t.put("settings.appearance.language.auto", "自动的");
        t.put("settings.appearance.language.manual", "肉手的");
        t.put("settings.appearance.language.label", "外表舌头");
        t.put("settings.appearance.language.hint", "自动模式跟着体制舌头走；肉手模式使用底下的舌头。");
        t.put("settings.appearance.language.accessible", "外表舌头");

        // 事例选择
        t.put("instances.picker.scanning", "正在用光扫比赛事例们...");
        t.put("instances.picker.title", "揪出比赛事例");
        t.put("instances.picker.noGameDirectory", "比赛名录还没被安放");
        t.put("instances.picker.gameDirectory", "比赛名录：%s");
        t.put("instances.picker.noGameDirectoryHint", "比赛名录还没被安放，请先去安放们里拯救");
        t.put("instances.picker.missing", "比赛名录走丢：%s");
        t.put("instances.picker.empty", "当前比赛名录里没有打进的例子");
        t.put("instances.picker.count", "总共 %d 个比赛事例");
        t.put("instances.picker.scanFailed", "用光扫比赛事例们失败：%s");
        t.put("instances.manage.title", "操纵事例们");

        // 后台操作名
        t.put("operation.inspectGameDirectory", "检查比赛名录");
        t.put("operation.checkInstanceName", "检查事例名字");
        t.put("operation.fetchVersionList", "获取朝廷 %s 列表");
        t.put("operation.installVanilla", "打进香草事例 %s");
        t.put("operation.fetchFabricLoader", "获取布料装载器版本");
        t.put("operation.fetchFabricApi", "获取布料应用编程接口版本");
        t.put("operation.installFabric", "打进布料事例 %s");
        t.put("operation.fetchForge", "获取铁砧版本");
        t.put("operation.fetchNeoForge", "获取新铁砧版本");
        t.put("operation.fetchOptiFine", "获取光罚款版本");
        t.put("operation.installLoader", "打进 %s 事例 %s");
        t.put("operation.scanJava", "用光扫爪哇");
        t.put("operation.addManualJava", "肉手硬塞爪哇");
        t.put("operation.saveJavaSelection", "拯救爪哇挑三拣四");
        t.put("operation.saveMaxMemory", "拯救最大记忆摆弄");
        t.put("operation.saveGameDirectory", "拯救比赛名录");
        t.put("operation.saveVersionIsolation", "拯救译本孤立法摆弄");
        t.put("operation.saveDownloadConcurrency", "拯救向下装载并发数");
        t.put("operation.saveDownloadSource", "拯救向下装载来源摆弄");
        t.put("operation.saveTheme", "拯救命题作文挑三拣四");
        t.put("operation.saveLanguage", "拯救舌头摆弄");
        t.put("operation.saveLastInstance", "拯救最后发射的事例");
        t.put("operation.launchGame", "发射比赛 %s");
    }
}
