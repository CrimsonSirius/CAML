package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/** 繁體中文翻譯。 */
public final class ZHTWGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "zh_tw";
    }

    @Override
    protected void populate(TranslationTable t) {
        // 通用
        t.put("common.back", "返回");
        t.put("common.cancel", "取消");
        t.put("common.browse", "瀏覽");
        t.put("common.save", "儲存");

        // 頂端導覽與底欄
        t.put("nav.home", "首頁");
        t.put("nav.downloads", "下載");
        t.put("nav.settings", "設定");
        t.put("app.subtitle", "Minecraft 啟動器");
        t.put("footer.status", "CAML %s · 後端%s · 資料目錄: %s");
        t.put("footer.backend.ready", "已就緒");
        t.put("footer.backend.notReady", "未就緒");

        // 首頁啟動表單
        t.put("home.username", "離線使用者名稱");
        t.put("home.launch", "啟動遊戲");
        t.put("home.launching", "啟動中...");
        t.put("home.browseInstances", "瀏覽遊戲實例");
        t.put("home.manageInstances", "管理實例");
        t.put("home.instance.current", "目前實例：%s");
        t.put("home.instance.current.none", "目前實例：未選擇");
        t.put("home.gameDirectory.none", "遊戲位置：未設定");
        t.put("home.gameDirectory.missing", "遊戲位置：目錄不存在 · %s");
        t.put("home.gameDirectory.value", "遊戲位置：%s");
        t.put("home.saveLocation.none", "存檔位置：未設定");
        t.put("home.saveLocation.fixDirectory", "存檔位置：請先修復遊戲位置");
        t.put("home.saveLocation.selectInstance", "存檔位置：請先選擇遊戲實例");
        t.put("home.saveLocation.value", "存檔位置：%s");
        t.put("home.saveLocation.isolated", "（完全版本隔離）");
        t.put("home.error.inspectDirectory", "檢查遊戲目錄失敗：%s");
        t.put("home.error.noGameDirectory", "請先在設定中儲存遊戲位置");
        t.put("home.error.missingGameDirectory", "遊戲位置不存在，請至設定中儲存有效目錄");
        t.put("home.error.noInstance", "請先瀏覽並選擇遊戲實例");
        t.put("home.error.noUsername", "請填寫離線使用者名稱");

        // 日誌面板
        t.put("log.title", "執行日誌");
        t.put("log.subtitle", "下載進度與遊戲程序輸出");
        t.put("log.accessible", "執行日誌");
        t.put("log.download.installed.vanilla", "[下載] 已安裝實例 %s（原版 %s）");
        t.put("log.download.installed.fabric", "[下載] 已安裝 Fabric 實例 %s（Loader %s）");
        t.put("log.download.installed.loader", "[下載] 已安裝 %s 實例 %s");
        t.put("log.download.progress", "[下載] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[下載] %s (%d)");
        t.put("log.gameProcess.exit", "遊戲程序已結束，代碼: %d");
        t.put("log.gameProcess.exitError", "遊戲程序異常結束，代碼: %d");
        t.put("log.error.operationFailed", "[%s] 失敗: %s");

        // 啟動狀態
        t.put("status.starting", "正在啟動 %s...");
        t.put("status.started", "啟動指令已送出");
        t.put("status.launchFailed", "啟動失敗！");
        t.put("status.launchFailedDetail", "啟動失敗：%s");
        t.put("error.unknown", "發生未知錯誤，請查看日誌。");
        t.put("download.error.failed", "無法下載 %s（已嘗試 %s 次，最後位址: %s）");
        t.put("network.requestFailed", "無法請求 %s（最後位址: %s）");
        t.put("network.timeout", "請求逾時（%s ms）：%s");
        t.put("network.httpError", "HTTP %s：%s");
        t.put("network.responseInvalid", "無法解析回應：%s");
        t.put("network.redirectLocationMissing", "重新導向缺少 Location：%s");
        t.put("network.tooManyRedirects", "重新導向次數過多：%s");
        t.put("network.noSource", "沒有可用的下載來源：%s");
        t.put("metadata.manifestEmpty", "版本清單為空");
        t.put("metadata.versionJsonEmpty", "版本 JSON 為空");
        t.put("metadata.versionJsonHashMismatch", "版本 JSON SHA-1 驗證失敗：%s");
        t.put("launch.permissionDenied", "沒有權限執行 Java 或存取遊戲目錄，請檢查檔案權限。");
        t.put("launch.executableNotFound", "找不到 Java 執行檔，請重新選擇 Java。");
        t.put("launch.commandTooLong", "啟動命令過長，請縮短遊戲路徑或減少類別路徑。");
        t.put("launch.commandTooLongAfterFallback", "啟動命令過長，已嘗試參數檔案與 manifest 備援，但仍無法啟動。請縮短遊戲路徑或減少類別路徑。");
        t.put("launch.processStartFailed", "啟動遊戲程序失敗，請查看日誌了解詳情。");
        t.put("launch.manualJavaNotConfigured", "已啟用手動 Java 選擇，但沒有選擇 Java 路徑。");
        t.put("launch.manualJavaUnavailable", "手動選擇的 Java 無法使用：%s");
        t.put("launch.javaRequirementNotFound", "找不到符合版本需求的 Java %s，請安裝或選擇 Java。");

        // 下載版本類別與來源
        t.put("download.category.release", "正式版");
        t.put("download.category.snapshot", "快照版");
        t.put("download.category.old", "遠古版");
        t.put("download.category.unknown", "未知版本");
        t.put("download.source.mirror", "鏡像來源");
        t.put("download.source.official", "官方來源");
        t.put("download.source.custom", "自訂來源");

        // 版本清單
        t.put("download.versions.loading", "正在從%1$s取得%2$s清單...");
        t.put("download.versions.empty", "下載來源沒有回傳可下載的%s");
        t.put("download.versions.count", "共 %d 個%s，依發布時間由新到舊排列");
        t.put("download.versions.error", "取得%s清單失敗：%s");
        t.put("download.versions.accessible", "%s，%s");

        // 下載內容描述
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // 下載設定頁
        t.put("download.setup.status.idle", "未選擇載入器時下載原版遊戲。");
        t.put("download.setup.instanceName", "實例名稱");
        t.put("download.setup.download", "下載實例");
        t.put("download.setup.option.versionInfo", "版本資訊");
        t.put("download.setup.option.forge", "選擇 Forge 版本");
        t.put("download.setup.option.optifine", "選擇 OptiFine 版本");
        t.put("download.setup.option.fabric", "選擇 Fabric 版本");
        t.put("download.setup.option.fabricApi", "選擇 Fabric API 版本");
        t.put("download.setup.option.neoforge", "選擇 NeoForge 版本");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge 與 OptiFine 不相容，已自動切換 Forge %s");
        t.put("download.setup.optifineIncompatibleForge", "目前 Forge 與 OptiFine 不相容");
        t.put("download.setup.noCompatibleOptiFine", "沒有相容於目前 Forge 的 OptiFine 版本");
        t.put("download.setup.optifineForgeConflict", "Forge %s 與 OptiFine %s 不相容，請選擇 Forge %s 或更早版本。");
        t.put("download.setup.nameEmpty", "實例名稱不能為空");
        t.put("download.setup.nameDotSegments", "實例名稱不能是 . 或 ..");
        t.put("download.setup.nameTrailing", "實例名稱不能以點或空格結尾");
        t.put("download.setup.nameIllegalCharacter", "實例名稱包含非法字元: %s");
        t.put("download.setup.nameExists", "實例名稱已存在：%s，請換一個名稱。");
        t.put("download.setup.noGameDirectory", "尚未設定遊戲位置");
        t.put("download.setup.checkingName", "正在檢查實例名稱...");
        t.put("download.setup.nameSameAsLoader", "安裝載入器時實例名稱不能與遊戲版本號相同");
        t.put("download.setup.nameAvailable", "實例名稱可用");
        t.put("download.setup.checkNameFailed", "檢查實例名稱失敗：%s");
        t.put("download.setup.nameNotChecked", "實例名稱尚未通過檢查，請稍後再試。");
        t.put("download.setup.noGameDirectoryLong", "尚未設定遊戲位置，請先至設定中儲存遊戲位置。");
        t.put("download.setup.optifineRequiresForge", "OptiFine 只能與 Forge 一起安裝。");

        // 載入器下拉選單提示
        t.put("download.prompt.loading.fabric", "正在取得 Fabric 版本...");
        t.put("download.prompt.loading.forge", "正在取得 Forge 版本...");
        t.put("download.prompt.loading.neoforge", "正在取得 NeoForge 版本...");
        t.put("download.prompt.fabricFirst", "請先選擇 Fabric 版本");
        t.put("download.prompt.forgeFirst", "請先選擇 Forge 版本");
        t.put("download.prompt.fabricApiFirst", "請先選擇 Fabric 版本");
        t.put("download.prompt.select.fabric", "請選擇 Fabric 版本");
        t.put("download.prompt.select.forge", "請選擇 Forge 版本");
        t.put("download.prompt.select.neoforge", "請選擇 NeoForge 版本");
        t.put("download.prompt.select.optifine", "請選擇 OptiFine 版本");
        t.put("download.prompt.select.fabricApi", "請選擇 Fabric API 版本");
        t.put("download.prompt.empty.fabric", "沒有可用的 Fabric 版本");
        t.put("download.prompt.empty.fabricApi", "沒有可用的 Fabric API 版本");
        t.put("download.prompt.empty.forge", "沒有可用的 Forge 版本");
        t.put("download.prompt.empty.neoforge", "沒有可用的 NeoForge 版本");
        t.put("download.prompt.empty.optifine", "沒有可用的 OptiFine 版本");
        t.put("download.prompt.error.fabric", "取得 Fabric 版本失敗");
        t.put("download.prompt.error.fabricApi", "取得 Fabric API 版本失敗");
        t.put("download.prompt.error.forge", "取得 Forge 版本失敗");
        t.put("download.prompt.error.neoforge", "取得 NeoForge 版本失敗");
        t.put("download.prompt.error.optifine", "取得 OptiFine 版本失敗");

        // 下載進度頁
        t.put("download.progress.title", "正在下載 %s");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "返回下載清單");
        t.put("download.progress.speed", "網路速度");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "下載進度");
        t.put("download.progress.preparing", "準備下載");
        t.put("download.progress.waiting", "等待下載工作...");
        t.put("download.progress.completed", "下載完成");
        t.put("download.progress.completedTotal", "下載已完成");
        t.put("download.progress.total", "累計 %s");
        t.put("download.progress.totalWithMax", "累計 %s / %s");
        t.put("download.progress.failed", "下載失敗：%s");
        t.put("download.progress.currentFile", "目前檔案：%s");
        t.put("download.progress.currentFileWithBytes", "目前檔案：%s · %s / %s");
        t.put("download.progress.finishedHint", "實例已安裝完成，可以返回清單。");
        t.put("download.progress.failedHint", "下載已停止，請返回清單後重試。");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · 檔案 %d / %d");
        t.put("download.progress.filesDetail", "檔案 %d / %d");
        t.put("download.progress.planning", "正在準備下載計畫...");

        // 下載階段
        t.put("download.stage.preparing", "準備下載");
        t.put("download.stage.completed", "完成");
        t.put("download.stage.failed", "下載失敗");
        t.put("download.stage.unknownError", "未知錯誤");
        t.put("download.stage.version.fetch", "取得版本資訊");
        t.put("download.stage.version.checkLocal", "檢查本機版本");
        t.put("download.stage.version.downloadFiles", "下載遊戲檔案與依賴函式庫");
        t.put("download.stage.version.downloadAssets", "下載資源檔案");
        t.put("download.stage.version.writeJson", "寫入版本資訊");
        t.put("download.stage.loader.installer.prepare", "準備 %s 安裝器");
        t.put("download.stage.loader.installer.download", "下載 %s 安裝器");
        t.put("download.stage.loader.installer.run", "執行 %s 安裝器");
        t.put("download.stage.loader.libraries.download", "下載 %s 依賴函式庫");
        t.put("download.stage.loader.files.merge", "合併 %s 檔案");
        t.put("download.stage.loader.verify", "驗證 %s 安裝結果");
        t.put("download.stage.fabric.config.fetch", "取得 Fabric 設定");
        t.put("download.stage.fabric.loader.install", "安裝 Fabric Loader");
        t.put("download.stage.fabric.api.download", "下載 Fabric API");
        t.put("download.stage.fabric.verify", "驗證 Fabric 安裝結果");
        t.put("download.stage.optifine.installer.download", "下載 OptiFine 安裝器");
        t.put("download.stage.optifine.installer.run", "執行 OptiFine 安裝器");
        t.put("download.stage.optifine.files.merge", "合併 OptiFine 檔案");

        // 下載設定
        t.put("download.settings.source", "下載來源");
        t.put("download.settings.source.description", "選擇版本、函式庫與資源使用的優先下載位址。");
        t.put("download.settings.source.mirror", "盡可能使用鏡像來源");
        t.put("download.settings.source.official", "盡可能使用官方來源");
        t.put("download.settings.source.custom", "匯入自訂 API");
        t.put("download.settings.customApi.placeholder", "例如 https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "設定下載並行數");
        t.put("download.settings.concurrency.description", "同時下載的檔案數量，範圍 1–256。");
        t.put("download.settings.concurrency.label", "下載並行數");
        t.put("download.settings.concurrency.value", "目前並行數：%d");
        t.put("download.settings.concurrency.accessible", "下載並行數");

        // 設定頁導覽
        t.put("settings.section.game", "遊戲設定");
        t.put("settings.section.download", "下載設定");
        t.put("settings.section.appearance", "外觀");

        // 遊戲位置
        t.put("settings.game.location.title", "遊戲位置");
        t.put("settings.game.location.description", "設定 Minecraft 遊戲資料目錄。手動輸入後必須點擊儲存才會生效。");
        t.put("settings.game.location.browse", "瀏覽");
        t.put("settings.game.location.save", "儲存");
        t.put("settings.game.location.chooserTitle", "選擇遊戲位置");
        t.put("settings.game.location.notSaved", "尚未儲存");
        t.put("settings.game.location.empty", "遊戲位置不能為空");
        t.put("settings.game.location.invalid", "遊戲位置格式無效");
        t.put("settings.game.location.checking", "正在檢查目錄...");
        t.put("settings.game.location.missing", "目錄不存在：%s");
        t.put("settings.game.location.saved", "遊戲位置已儲存");
        t.put("settings.game.location.checkFailed", "檢查目錄失敗：%s");

        // 記憶體
        t.put("settings.game.memory.title", "記憶體");
        t.put("settings.game.memory.accessible", "最大記憶體");
        t.put("settings.game.memory.description", "調整遊戲啟動時使用的記憶體。");
        t.put("settings.game.memory.label", "記憶體分配");
        t.put("settings.game.memory.hint", "拖曳小球調整遊戲分配；灰色條為已使用 + 遊戲分配，右側淡灰為未使用記憶體。");
        t.put("settings.game.memory.summary", "已使用 %d MB / 遊戲 %d MB / 可用 %d MB / 總記憶體 %d MB");

        // 版本隔離
        t.put("settings.game.isolation.title", "版本隔離");
        t.put("settings.game.isolation.full", "完全版本隔離");
        t.put("settings.game.isolation.none", "不進行版本隔離");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "選擇遊戲啟動時使用的 Java 執行環境。自動選擇會依遊戲版本需求挑選可用的 Java。");
        t.put("settings.game.java.auto", "自動選擇");
        t.put("settings.game.java.manual", "手動選擇");
        t.put("settings.game.java.rescan", "重新掃描");
        t.put("settings.game.java.add", "新增 Java");
        t.put("settings.game.java.scanning", "正在掃描 Java...");
        t.put("settings.game.java.scanningAll", "正在掃描全系統 Java...");
        t.put("settings.game.java.scanFailedPrompt", "掃描 Java 失敗");
        t.put("settings.game.java.scanFailed", "掃描 Java 失敗：%s");
        t.put("settings.game.java.noneFound", "找不到 Java");
        t.put("settings.game.java.select", "請選擇 Java");
        t.put("settings.game.java.autoSelected", "自動選擇");
        t.put("settings.game.java.chooserTitle", "選擇 Java 執行檔");
        t.put("settings.game.java.checking", "正在檢查 Java...");
        t.put("settings.game.java.addFailed", "新增 Java 失敗：%s");
        t.put("settings.game.java.autoHint", "自動選擇會從掃描結果中挑選符合目前遊戲版本需求的 Java。");
        t.put("settings.game.java.selectOrAdd", "請選擇一個 Java，或點擊「新增 Java」。");
        t.put("settings.game.java.unavailable", "該 Java 目前無法使用：%s");
        t.put("settings.game.java.detail", "版本 %s · %s · %s · %d 位元%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // 外觀
        t.put("settings.appearance.title", "外觀");
        t.put("settings.appearance.description", "調整啟動器本身的外觀。");
        t.put("settings.appearance.themeHint", "主題來自 ~/.caml/themes，切換後立即生效");
        t.put("settings.appearance.theme", "主題");
        t.put("settings.appearance.theme.accessible", "主題");
        t.put("settings.appearance.language.title", "語言");
        t.put("settings.appearance.language.description", "選擇啟動器介面語言的顯示方式。");
        t.put("settings.appearance.language.auto", "自動");
        t.put("settings.appearance.language.manual", "手動");
        t.put("settings.appearance.language.label", "介面語言");
        t.put("settings.appearance.language.hint", "自動模式跟隨系統語言；手動模式使用下方選擇的語言。");
        t.put("settings.appearance.language.accessible", "介面語言");
        t.put("settings.appearance.languageFallback.title", "語言備援");
        t.put("settings.appearance.languageFallback.description", "設定翻譯缺少時使用哪種語言補齊。");
        t.put("settings.appearance.languageFallback.system", "系統語言");
        t.put("settings.appearance.languageFallback.default", "預設語言");
        t.put("settings.appearance.languageFallback.key", "顯示原始鍵值");
        t.put("settings.appearance.languageFallback.custom", "自訂語言");
        t.put("settings.appearance.languageFallback.hint", "系統語言備援僅使用系統偵測語言；預設語言固定使用英語；原始鍵值不備援；自訂語言僅使用下方選擇的語言。除預設語言外，缺少的內容都會顯示原始鍵值。");
        t.put("settings.appearance.languageFallback.accessible", "語言備援");

        // 實例選擇
        t.put("instances.picker.scanning", "正在掃描遊戲實例...");
        t.put("instances.picker.title", "選擇遊戲實例");
        t.put("instances.picker.noGameDirectory", "尚未設定遊戲位置");
        t.put("instances.picker.gameDirectory", "遊戲位置：%s");
        t.put("instances.picker.noGameDirectoryHint", "尚未設定遊戲位置，請先至設定中儲存遊戲位置");
        t.put("instances.picker.missing", "遊戲位置不存在：%s");
        t.put("instances.picker.empty", "目前遊戲位置沒有已安裝實例");
        t.put("instances.picker.count", "共 %d 個遊戲實例");
        t.put("instances.picker.scanFailed", "掃描遊戲實例失敗：%s");
        t.put("instances.manage.title", "管理實例");

        // Mod 下載
        t.put("mods.nav", "模組");
        t.put("mods.search.title", "搜尋 Mod");
        t.put("mods.search.name", "名稱");
        t.put("mods.search.nameHint", "輸入 Mod 名稱");
        t.put("mods.search.source", "來源");
        t.put("mods.search.gameVersion", "遊戲版本");
        t.put("mods.search.versionHint", "例如 1.20.1，留空表示全部");
        t.put("mods.search.loader", "載入器");
        t.put("mods.search.allSources", "全部來源");
        t.put("mods.search.allLoaders", "全部載入器");
        t.put("mods.search.button", "搜尋");
        t.put("mods.search.reset", "重設條件");
        t.put("mods.search.loading", "正在搜尋 Mod...");
        t.put("mods.search.empty", "找不到符合的 Mod");
        t.put("mods.search.count", "共 %d 個結果");
        t.put("mods.search.failed", "載入 Mod 失敗：%s");
        t.put("mods.page.previous", "上一頁");
        t.put("mods.page.next", "下一頁");
        t.put("mods.page.info", "第 %d 頁");
        t.put("mods.source.modrinth", "Modrinth");
        t.put("mods.source.curseforge", "CurseForge");
        t.put("mods.loader.fabric", "Fabric");
        t.put("mods.loader.forge", "Forge");
        t.put("mods.loader.neoforge", "NeoForge");
        t.put("mods.loader.quilt", "Quilt");
        t.put("mods.value.unknown", "未知");
        t.put("mods.list.meta", "載入器：%s · 版本：%s · 下載 %s · 更新 %s");
        t.put("mods.detail.loading", "正在載入 Mod 詳細資料...");
        t.put("mods.detail.count", "共 %d 個檔案版本");
        t.put("mods.detail.meta", "下載 %s · 更新 %s");
        t.put("mods.detail.versionFilter", "遊戲版本");
        t.put("mods.detail.loaderFilter", "載入器");
        t.put("mods.detail.allVersions", "全部版本");
        t.put("mods.detail.allLoaders", "全部載入器");
        t.put("mods.detail.empty", "目前篩選條件下沒有可用版本");
        t.put("mods.detail.versionMeta", "發布 %s · 下載 %s · 檔案 %s");
        t.put("mods.detail.download", "下載");
        t.put("mods.detail.downloading", "正在下載 %s...");
        t.put("mods.detail.downloaded", "已下載至：%s");
        t.put("mods.detail.noGameDirectory", "請先設定遊戲目錄");
        t.put("mods.detail.noInstance", "請先選擇遊戲實例");
        t.put("mods.detail.failed", "操作失敗：%s");
        t.put("mods.detail.dependencies", "依賴：");
        t.put("mods.detail.noDependencies", "無依賴");
        t.put("mods.dependency.required", "必要");
        t.put("mods.dependency.optional", "選用");
        t.put("mods.dependency.incompatible", "不相容");
        t.put("mods.dependency.embedded", "內嵌");
        t.put("mods.dependency.unknown", "未知");
        t.put("mod.error.curseforgeKeyMissing", "未設定 CurseForge API Key，請設定 CURSEFORGE_API_KEY 或 -Dcaml.curseforge.apiKey。");
        t.put("mod.error.downloadUrlMissing", "該 Mod 版本沒有可用的官方下載位址：%s");
        t.put("mod.error.projectNotFound", "找不到 Mod 專案：%s");
        t.put("mod.error.iconUrlInvalid", "Mod 圖示位址無效：%s");
        t.put("mod.error.iconEmpty", "Mod 圖示檔案為空：%s");

        // 背景操作名稱
        t.put("operation.inspectGameDirectory", "檢查遊戲目錄");
        t.put("operation.checkInstanceName", "檢查實例名稱");
        t.put("operation.fetchVersionList", "取得官方%s清單");
        t.put("operation.installVanilla", "安裝原版實例 %s");
        t.put("operation.fetchFabricLoader", "取得 Fabric Loader 版本");
        t.put("operation.fetchFabricApi", "取得 Fabric API 版本");
        t.put("operation.installFabric", "安裝 Fabric 實例 %s");
        t.put("operation.fetchForge", "取得 Forge 版本");
        t.put("operation.fetchNeoForge", "取得 NeoForge 版本");
        t.put("operation.fetchOptiFine", "取得 OptiFine 版本");
        t.put("operation.installLoader", "安裝 %s 實例 %s");
        t.put("operation.scanJava", "掃描 Java");
        t.put("operation.addManualJava", "新增手動 Java");
        t.put("operation.saveJavaSelection", "儲存 Java 選擇");
        t.put("operation.saveMaxMemory", "儲存最大記憶體設定");
        t.put("operation.saveGameDirectory", "儲存遊戲位置");
        t.put("operation.saveVersionIsolation", "儲存版本隔離設定");
        t.put("operation.saveDownloadConcurrency", "儲存下載並行數");
        t.put("operation.saveDownloadSource", "儲存下載來源設定");
        t.put("operation.saveTheme", "儲存主題選擇");
        t.put("operation.saveLanguage", "儲存語言設定");
        t.put("operation.saveLanguageFallback", "儲存語言備援設定");
        t.put("operation.searchMods", "搜尋 Mod：%s");
        t.put("operation.fetchModProject", "取得 Mod 專案：%s");
        t.put("operation.downloadMod", "下載 Mod：%s");
        t.put("operation.loadModIcon", "載入 Mod 圖示");
        t.put("operation.saveLastInstance", "儲存最後啟動實例");
        t.put("operation.launchGame", "啟動遊戲 %s");
    }
}
