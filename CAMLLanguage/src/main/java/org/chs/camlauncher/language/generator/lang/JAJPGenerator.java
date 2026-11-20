package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.LanguageTranslationGenerator;
import org.chs.camlauncher.language.generator.TranslationTable;

/** 日本語訳。 */
public final class JAJPGenerator extends LanguageTranslationGenerator {
    @Override
    protected String locale() {
        return "ja_jp";
    }

    @Override
    protected void populate(TranslationTable t) {
        // 共通
        t.put("common.back", "戻る");
        t.put("common.cancel", "キャンセル");
        t.put("common.browse", "参照");
        t.put("common.save", "保存");

        // ナビゲーションとフッター
        t.put("nav.home", "ホーム");
        t.put("nav.downloads", "ダウンロード");
        t.put("nav.settings", "設定");
        t.put("app.subtitle", "Minecraft ランチャー");
        t.put("footer.status", "CAML %s · バックエンド %s · データディレクトリ: %s");
        t.put("footer.backend.ready", "準備完了");
        t.put("footer.backend.notReady", "未準備");

        // 起動フォーム
        t.put("home.username", "オフラインユーザー名");
        t.put("home.launch", "ゲームを起動");
        t.put("home.launching", "起動中...");
        t.put("home.browseInstances", "インスタンスを参照");
        t.put("home.manageInstances", "インスタンスを管理");
        t.put("home.instance.current", "現在のインスタンス: %s");
        t.put("home.instance.current.none", "現在のインスタンス: 未選択");
        t.put("home.gameDirectory.none", "ゲームディレクトリ: 未設定");
        t.put("home.gameDirectory.missing", "ゲームディレクトリ: 見つかりません · %s");
        t.put("home.gameDirectory.value", "ゲームディレクトリ: %s");
        t.put("home.saveLocation.none", "セーブデータの場所: 未設定");
        t.put("home.saveLocation.fixDirectory", "セーブデータの場所: 先にゲームディレクトリを修正してください");
        t.put("home.saveLocation.selectInstance", "セーブデータの場所: 先にインスタンスを選択してください");
        t.put("home.saveLocation.value", "セーブデータの場所: %s");
        t.put("home.saveLocation.isolated", "（完全バージョン分離）");
        t.put("home.error.inspectDirectory", "ゲームディレクトリの確認に失敗しました: %s");
        t.put("home.error.noGameDirectory", "先に設定でゲームディレクトリを保存してください");
        t.put("home.error.missingGameDirectory", "ゲームディレクトリが存在しません。設定で有効なディレクトリを保存してください");
        t.put("home.error.noInstance", "先にインスタンスを参照して選択してください");
        t.put("home.error.noUsername", "オフラインユーザー名を入力してください");

        // ログ
        t.put("log.title", "実行ログ");
        t.put("log.subtitle", "ダウンロードの進行状況とゲームプロセスの出力");
        t.put("log.accessible", "実行ログ");
        t.put("log.download.installed.vanilla", "[ダウンロード] インスタンス %s をインストールしました（バニラ %s）");
        t.put("log.download.installed.fabric", "[ダウンロード] Fabric インスタンス %s をインストールしました（Loader %s）");
        t.put("log.download.installed.loader", "[ダウンロード] %s インスタンス %s をインストールしました");
        t.put("log.download.progress", "[ダウンロード] %s (%d/%d)");
        t.put("log.download.progressNoTotal", "[ダウンロード] %s (%d)");
        t.put("log.gameProcess.exit", "ゲームプロセスが終了しました。コード: %d");
        t.put("log.gameProcess.exitError", "ゲームプロセスが異常終了しました。コード: %d");
        t.put("log.error.operationFailed", "[%s] 失敗: %s");

        // 起動状態
        t.put("status.starting", "%s を起動しています...");
        t.put("status.started", "起動コマンドを送信しました");
        t.put("status.launchFailed", "起動に失敗しました！");
        t.put("status.launchFailedDetail", "起動に失敗しました: %s");
        t.put("error.unknown", "不明なエラーが発生しました。ログを確認してください。");
        t.put("download.error.failed", "%s のダウンロードに失敗しました（%s 回試行、最後のアドレス: %s）");
        t.put("network.requestFailed", "%s へのリクエストに失敗しました（最後のアドレス: %s）");
        t.put("network.timeout", "リクエストがタイムアウトしました（%s ms）: %s");
        t.put("network.httpError", "HTTP %s: %s");
        t.put("network.responseInvalid", "レスポンスを解析できませんでした: %s");
        t.put("network.redirectLocationMissing", "リダイレクトに Location がありません: %s");
        t.put("network.tooManyRedirects", "リダイレクトが多すぎます: %s");
        t.put("network.noSource", "利用可能なダウンロード元がありません: %s");
        t.put("metadata.manifestEmpty", "バージョンマニフェストが空です");
        t.put("metadata.versionJsonEmpty", "バージョン JSON が空です");
        t.put("metadata.versionJsonHashMismatch", "バージョン JSON の SHA-1 検証に失敗しました: %s");
        t.put("launch.permissionDenied", "Java の実行またはゲームディレクトリへのアクセスが許可されていません。ファイルの権限を確認してください。");
        t.put("launch.executableNotFound", "Java 実行ファイルが見つかりません。Java を選択し直してください。");
        t.put("launch.commandTooLong", "起動コマンドが長すぎます。ゲームのパスまたはクラスパスを短くしてください。");
        t.put("launch.commandTooLongAfterFallback", "argfile とマニフェストへのフォールバック後も起動コマンドが長すぎます。ゲームのパスまたはクラスパスを短くしてください。");
        t.put("launch.processStartFailed", "ゲームプロセスの開始に失敗しました。詳細はログを確認してください。");
        t.put("launch.manualJavaNotConfigured", "手動 Java 選択は有効ですが、Java のパスが選択されていません。");
        t.put("launch.manualJavaUnavailable", "選択した Java は使用できません: %s");
        t.put("launch.javaRequirementNotFound", "バージョン要件 %s を満たす Java が見つかりません。Java をインストールするか選択してください。");

        // バージョン分類と取得元
        t.put("download.category.release", "リリース");
        t.put("download.category.snapshot", "スナップショット");
        t.put("download.category.old", "旧バージョン");
        t.put("download.category.unknown", "不明なバージョン");
        t.put("download.source.mirror", "ミラー");
        t.put("download.source.official", "公式");
        t.put("download.source.custom", "カスタム");

        // バージョン一覧
        t.put("download.versions.loading", "%1$s から %2$s 一覧を取得しています...");
        t.put("download.versions.empty", "ダウンロード元からダウンロード可能な %s が返されませんでした");
        t.put("download.versions.count", "全 %d 件の %s（リリース日時の新しい順）");
        t.put("download.versions.error", "%s 一覧の取得に失敗しました: %s");
        t.put("download.versions.accessible", "%s、%s");

        // 内容の説明
        t.put("download.description.optifine", "Forge %s + OptiFine %s");
        t.put("download.description.forge", "Forge %s");
        t.put("download.description.fabric", "Fabric Loader %s");
        t.put("download.description.fabricWithApi", "Fabric Loader %s + Fabric API %s");
        t.put("download.description.neoforge", "NeoForge %s");
        t.put("download.description.vanilla", "Minecraft %s");

        // ダウンロード設定ページ
        t.put("download.setup.status.idle", "ローダーが選択されていない場合はバニラをダウンロードします。");
        t.put("download.setup.instanceName", "インスタンス名");
        t.put("download.setup.download", "インスタンスをダウンロード");
        t.put("download.setup.option.versionInfo", "バージョン情報");
        t.put("download.setup.option.forge", "Forge バージョンを選択");
        t.put("download.setup.option.optifine", "OptiFine バージョンを選択");
        t.put("download.setup.option.fabric", "Fabric バージョンを選択");
        t.put("download.setup.option.fabricApi", "Fabric API バージョンを選択");
        t.put("download.setup.option.neoforge", "NeoForge バージョンを選択");
        t.put("download.setup.versionInfo", "Minecraft %s · %s");
        t.put("download.setup.versionInfoWithDate", "Minecraft %s · %s · %s");
        t.put("download.setup.optifineSwitched", "Forge と OptiFine は互換性がありません。Forge %s に切り替えました");
        t.put("download.setup.optifineIncompatibleForge", "現在の Forge は OptiFine と互換性がありません");
        t.put("download.setup.noCompatibleOptiFine", "現在の Forge と互換性のある OptiFine バージョンがありません");
        t.put("download.setup.optifineForgeConflict", "Forge %s は OptiFine %s と互換性がありません。Forge %s 以前を選択してください。");
        t.put("download.setup.nameEmpty", "インスタンス名を空にできません");
        t.put("download.setup.nameDotSegments", "インスタンス名に . または .. は使用できません");
        t.put("download.setup.nameTrailing", "インスタンス名をピリオドまたは空白で終えることはできません");
        t.put("download.setup.nameIllegalCharacter", "インスタンス名に使用できない文字が含まれています: %s");
        t.put("download.setup.nameExists", "インスタンス名は既に存在します: %s。別の名前を選んでください。");
        t.put("download.setup.noGameDirectory", "ゲームディレクトリが設定されていません");
        t.put("download.setup.checkingName", "インスタンス名を確認しています...");
        t.put("download.setup.nameSameAsLoader", "ローダーをインストールする場合、インスタンス名をゲームバージョンと同じにできません");
        t.put("download.setup.nameAvailable", "インスタンス名は使用できます");
        t.put("download.setup.checkNameFailed", "インスタンス名の確認に失敗しました: %s");
        t.put("download.setup.nameNotChecked", "インスタンス名はまだ確認されていません。しばらくしてから再度お試しください。");
        t.put("download.setup.noGameDirectoryLong", "ゲームディレクトリが設定されていません。先に設定で保存してください。");
        t.put("download.setup.optifineRequiresForge", "OptiFine は Forge と一緒にしかインストールできません。");

        // ローダー選択のヒント
        t.put("download.prompt.loading.fabric", "Fabric バージョンを取得しています...");
        t.put("download.prompt.loading.forge", "Forge バージョンを取得しています...");
        t.put("download.prompt.loading.neoforge", "NeoForge バージョンを取得しています...");
        t.put("download.prompt.fabricFirst", "先に Fabric のバージョンを選択してください");
        t.put("download.prompt.forgeFirst", "先に Forge バージョンを選択してください");
        t.put("download.prompt.fabricApiFirst", "先に Fabric バージョンを選択してください");
        t.put("download.prompt.select.fabric", "Fabric バージョンを選択してください");
        t.put("download.prompt.select.forge", "Forge バージョンを選択してください");
        t.put("download.prompt.select.neoforge", "NeoForge バージョンを選択してください");
        t.put("download.prompt.select.optifine", "OptiFine バージョンを選択してください");
        t.put("download.prompt.select.fabricApi", "Fabric API バージョンを選択してください");
        t.put("download.prompt.empty.fabric", "利用可能な Fabric バージョンがありません");
        t.put("download.prompt.empty.fabricApi", "利用可能な Fabric API バージョンがありません");
        t.put("download.prompt.empty.forge", "利用可能な Forge バージョンがありません");
        t.put("download.prompt.empty.neoforge", "利用可能な NeoForge バージョンがありません");
        t.put("download.prompt.empty.optifine", "利用可能な OptiFine バージョンがありません");
        t.put("download.prompt.error.fabric", "Fabric バージョンの取得に失敗しました");
        t.put("download.prompt.error.fabricApi", "Fabric API バージョンの取得に失敗しました");
        t.put("download.prompt.error.forge", "Forge バージョンの取得に失敗しました");
        t.put("download.prompt.error.neoforge", "NeoForge バージョンの取得に失敗しました");
        t.put("download.prompt.error.optifine", "OptiFine バージョンの取得に失敗しました");

        // 進行状況ページ
        t.put("download.progress.title", "%s をダウンロードしています");
        t.put("download.progress.subtitle", "%s · Minecraft %s");
        t.put("download.progress.back", "ダウンロード一覧に戻る");
        t.put("download.progress.speed", "ネットワーク速度");
        t.put("download.progress.speedValue", "0 B/s");
        t.put("download.progress.progress", "ダウンロードの進行状況");
        t.put("download.progress.preparing", "ダウンロードを準備しています");
        t.put("download.progress.waiting", "ダウンロードタスクを待機しています...");
        t.put("download.progress.completed", "ダウンロード完了");
        t.put("download.progress.completedTotal", "ダウンロードが完了しました");
        t.put("download.progress.total", "合計 %s");
        t.put("download.progress.totalWithMax", "合計 %s / %s");
        t.put("download.progress.failed", "ダウンロードに失敗しました: %s");
        t.put("download.progress.currentFile", "現在のファイル: %s");
        t.put("download.progress.currentFileWithBytes", "現在のファイル: %s · %s / %s");
        t.put("download.progress.finishedHint", "インスタンスのインストールが完了しました。一覧に戻れます。");
        t.put("download.progress.failedHint", "ダウンロードを停止しました。一覧に戻って再試行してください。");
        t.put("download.progress.bytesDetail", "%.1f%% · %s / %s · ファイル %d / %d");
        t.put("download.progress.filesDetail", "ファイル %d / %d");
        t.put("download.progress.planning", "ダウンロード計画を準備しています...");

        // ダウンロード段階
        t.put("download.stage.preparing", "ダウンロードを準備しています");
        t.put("download.stage.completed", "完了");
        t.put("download.stage.failed", "ダウンロード失敗");
        t.put("download.stage.unknownError", "不明なエラー");
        t.put("download.stage.version.fetch", "バージョン情報を取得しています");
        t.put("download.stage.version.checkLocal", "ローカルバージョンを確認しています");
        t.put("download.stage.version.downloadFiles", "ゲームファイルとライブラリをダウンロードしています");
        t.put("download.stage.version.downloadAssets", "アセットをダウンロードしています");
        t.put("download.stage.version.writeJson", "バージョン情報を書き込んでいます");
        t.put("download.stage.loader.installer.prepare", "%s インストーラーを準備しています");
        t.put("download.stage.loader.installer.download", "%s インストーラーをダウンロードしています");
        t.put("download.stage.loader.installer.run", "%s インストーラーを実行しています");
        t.put("download.stage.loader.libraries.download", "%s ライブラリをダウンロードしています");
        t.put("download.stage.loader.files.merge", "%s ファイルを結合しています");
        t.put("download.stage.loader.verify", "%s のインストールを検証しています");
        t.put("download.stage.fabric.config.fetch", "Fabric 設定を取得しています");
        t.put("download.stage.fabric.loader.install", "Fabric Loader をインストールしています");
        t.put("download.stage.fabric.api.download", "Fabric API をダウンロードしています");
        t.put("download.stage.fabric.verify", "Fabric のインストールを検証しています");
        t.put("download.stage.optifine.installer.download", "OptiFine インストーラーをダウンロードしています");
        t.put("download.stage.optifine.installer.run", "OptiFine インストーラーを実行しています");
        t.put("download.stage.optifine.files.merge", "OptiFine ファイルを結合しています");

        // ダウンロード設定
        t.put("download.settings.source", "ダウンロード元");
        t.put("download.settings.source.description", "バージョン、ライブラリ、アセットに使用する優先ダウンロード先を選択します。");
        t.put("download.settings.source.mirror", "ミラーを優先");
        t.put("download.settings.source.official", "公式を優先");
        t.put("download.settings.source.custom", "カスタム API を読み込む");
        t.put("download.settings.customApi.placeholder", "例: https://bmclapi2.bangbang93.com");
        t.put("download.settings.concurrency", "ダウンロードの並列数");
        t.put("download.settings.concurrency.description", "同時にダウンロードするファイル数（1～256）。");
        t.put("download.settings.concurrency.label", "ダウンロードの並列数");
        t.put("download.settings.concurrency.value", "現在の並列数: %d");
        t.put("download.settings.concurrency.accessible", "ダウンロードの並列数");

        // 設定ナビゲーション
        t.put("settings.section.game", "ゲーム設定");
        t.put("settings.section.download", "ダウンロード設定");
        t.put("settings.section.appearance", "外観");

        // ゲームディレクトリ
        t.put("settings.game.location.title", "ゲームディレクトリ");
        t.put("settings.game.location.description", "Minecraft のデータディレクトリを設定します。手動で入力した場合は保存を押すまで反映されません。");
        t.put("settings.game.location.browse", "参照");
        t.put("settings.game.location.save", "保存");
        t.put("settings.game.location.chooserTitle", "ゲームディレクトリを選択");
        t.put("settings.game.location.notSaved", "未保存");
        t.put("settings.game.location.empty", "ゲームディレクトリを空にできません");
        t.put("settings.game.location.invalid", "ゲームディレクトリの形式が無効です");
        t.put("settings.game.location.checking", "ディレクトリを確認しています...");
        t.put("settings.game.location.missing", "ディレクトリが存在しません: %s");
        t.put("settings.game.location.saved", "ゲームディレクトリを保存しました");
        t.put("settings.game.location.checkFailed", "ディレクトリの確認に失敗しました: %s");

        // メモリ
        t.put("settings.game.memory.title", "メモリ");
        t.put("settings.game.memory.accessible", "最大メモリ");
        t.put("settings.game.memory.description", "ゲーム起動時に使用するメモリを調整します。");
        t.put("settings.game.memory.label", "メモリ割り当て");
        t.put("settings.game.memory.hint", "ハンドルをドラッグしてゲームへの割り当てを調整します。グレーのバーは使用済み＋ゲームのメモリ、右側の薄いグレーは未使用のメモリです。");
        t.put("settings.game.memory.summary", "使用済み %d MB / ゲーム %d MB / 利用可能 %d MB / 合計 %d MB");

        // バージョン分離
        // JVM 引数
        t.put("settings.game.jvm.title", "JVM 引数");
        t.put("settings.game.jvm.description", "ゲーム起動時に追加の JVM 引数を指定します。メモリ割り当ては上のスライダーで管理されます。内容が分からない場合は変更しないでください。");
        t.put("settings.game.jvm.label", "追加引数");
        t.put("settings.game.jvm.hint", "引数は空白で区切ります。空白を含む値は -Dfoo=\"bar baz\" のように二重引用符で囲んでください。${game_directory}、${library_directory} などのプレースホルダーに対応しています。");
        t.put("settings.game.jvm.placeholder", "-XX:+UseG1GC -Dexample=value");
        t.put("settings.game.jvm.save", "保存");
        t.put("settings.game.jvm.saved", "JVM 引数を保存しました");
        t.put("settings.game.jvm.saveFailed", "JVM 引数の保存に失敗しました: %s");

        t.put("settings.game.isolation.title", "バージョン分離");
        t.put("settings.game.isolation.full", "完全バージョン分離");
        t.put("settings.game.isolation.none", "バージョン分離しない");

        // ゲームの言語
        t.put("settings.game.language.title", "ゲームの言語");
        t.put("settings.game.language.description", "新しいインスタンスを初めて起動したとき、ゲームの言語をランチャーの現在の言語に自動設定します。");
        t.put("settings.game.language.enabled", "自動切り替え");
        t.put("settings.game.language.disabled", "オフ");
        t.put("settings.game.language.hint", "インスタンスにまだ言語が設定されていない場合のみ有効です。options.txt に既に設定された言語は上書きされません。「地道中国話」は簡体字中国語にマッピングされます。");

        // Java
        t.put("settings.game.java.title", "Java");
        t.put("settings.game.java.description", "ゲーム起動に使用する Java 実行環境を選択します。自動選択ではゲームバージョンの要件を満たす Java を選びます。");
        t.put("settings.game.java.auto", "自動選択");
        t.put("settings.game.java.manual", "手動選択");
        t.put("settings.game.java.rescan", "再スキャン");
        t.put("settings.game.java.add", "Java を追加");
        t.put("settings.game.java.scanning", "Java をスキャンしています...");
        t.put("settings.game.java.scanningAll", "システム全体の Java をスキャンしています...");
        t.put("settings.game.java.scanFailedPrompt", "Java のスキャンに失敗しました");
        t.put("settings.game.java.scanFailed", "Java のスキャンに失敗しました: %s");
        t.put("settings.game.java.noneFound", "Java が見つかりません");
        t.put("settings.game.java.select", "Java を選択してください");
        t.put("settings.game.java.autoSelected", "自動選択");
        t.put("settings.game.java.chooserTitle", "Java 実行ファイルを選択");
        t.put("settings.game.java.checking", "Java を確認しています...");
        t.put("settings.game.java.addFailed", "Java の追加に失敗しました: %s");
        t.put("settings.game.java.autoHint", "自動選択では、スキャン結果から現在のゲームバージョンの要件を満たす Java を選びます。");
        t.put("settings.game.java.selectOrAdd", "Java を選択するか、「Java を追加」をクリックしてください。");
        t.put("settings.game.java.unavailable", "この Java は現在使用できません: %s");
        t.put("settings.game.java.detail", "バージョン %s · %s · %s · %d ビット%s");
        t.put("settings.game.java.jdk", " · JDK");
        t.put("settings.game.java.jre", " · JRE");

        // 外観
        t.put("settings.appearance.title", "外観");
        t.put("settings.appearance.description", "ランチャー自体の外観を調整します。");
        t.put("settings.appearance.themeHint", "テーマは ~/.caml/themes から読み込まれ、切り替え後すぐに反映されます");
        t.put("settings.appearance.theme", "テーマ");
        t.put("settings.appearance.theme.accessible", "テーマ");
        t.put("settings.appearance.language.title", "言語");
        t.put("settings.appearance.language.description", "ランチャー界面言語の選択方法を設定します。");
        t.put("settings.appearance.language.auto", "自動");
        t.put("settings.appearance.language.manual", "手動");
        t.put("settings.appearance.language.label", "界面言語");
        t.put("settings.appearance.language.hint", "自動モードはシステム言語に従います。手動モードでは下で選択した言語を使用します。");
        t.put("settings.appearance.language.accessible", "界面言語");
        t.put("settings.appearance.languageFallback.title", "言語フォールバック");
        t.put("settings.appearance.languageFallback.description", "翻訳が欠けている場合に補完する言語を設定します。");
        t.put("settings.appearance.languageFallback.system", "システム言語");
        t.put("settings.appearance.languageFallback.default", "既定の言語");
        t.put("settings.appearance.languageFallback.key", "生のキーを表示");
        t.put("settings.appearance.languageFallback.custom", "カスタム言語");
        t.put("settings.appearance.languageFallback.hint", "システム言語は検出された言語のみを使用します。既定の言語は常に英語を使用します。生のキーはフォールバックしません。カスタム言語は下で選択した言語のみを使用します。既定の言語以外では、欠けた内容は生のキーで表示されます。");
        t.put("settings.appearance.languageFallback.accessible", "言語フォールバック");

        // インスタンス選択
        t.put("instances.picker.scanning", "ゲームインスタンスをスキャンしています...");
        t.put("instances.picker.title", "ゲームインスタンスを選択");
        t.put("instances.picker.noGameDirectory", "ゲームディレクトリが設定されていません");
        t.put("instances.picker.gameDirectory", "ゲームディレクトリ: %s");
        t.put("instances.picker.noGameDirectoryHint", "ゲームディレクトリが設定されていません。先に設定で保存してください");
        t.put("instances.picker.missing", "ゲームディレクトリが存在しません: %s");
        t.put("instances.picker.empty", "現在のゲームディレクトリにインストール済みのインスタンスはありません");
        t.put("instances.picker.count", "全 %d 件のゲームインスタンス");
        t.put("instances.picker.scanFailed", "ゲームインスタンスのスキャンに失敗しました: %s");
        t.put("instances.manage.title", "インスタンスを管理");
        t.put("instances.manage.instance", "インスタンス：%s");
        t.put("instances.manage.folder", "インスタンスフォルダー：%s");
        t.put("instances.manage.openFolder", "インスタンスフォルダーを開く");
        t.put("instances.manage.openFolder.opening", "インスタンスフォルダーを開いています...");
        t.put("instances.manage.openFolder.failed", "インスタンスフォルダーを開けませんでした：%s");
        t.put("instances.manage.noGameDirectory", "ゲームディレクトリが未設定です");
        t.put("instances.manage.noInstance", "インスタンスが選択されていません");
        t.put("instances.manage.mods.title", "MOD 一覧");
        t.put("instances.manage.mods.scanning", "MOD 一覧を読み込んでいます...");
        t.put("instances.manage.mods.count", "%d 個の MOD");
        t.put("instances.manage.mods.empty", "このインスタンスに MOD はありません");
        t.put("instances.manage.mods.scanFailed", "MOD 一覧の読み込みに失敗しました：%s");
        t.put("instances.manage.mods.toggleFailed", "MOD の状態を切り替えられませんでした：%s");
        t.put("instances.manage.mods.update", "更新");
        t.put("instances.manage.mods.updating", "%s を更新しています...");
        t.put("instances.manage.mods.updateFailed", "MOD の更新に失敗しました：%s");
        t.put("instances.manage.mods.enabled", "有効");
        t.put("instances.manage.mods.disabled", "無効");
        t.put("instances.manage.mods.pageTitle", "Mod の管理");
        t.put("instances.manage.scanning", "インスタンス情報を読み込み中...");
        t.put("instances.manage.scanFailed", "インスタンス情報の読み込みに失敗しました：%s");
        t.put("instances.manage.summary", "Mod %d 個、リソースパック %d 個");
        t.put("instances.manage.manageMods", "Mod を管理");
        t.put("instances.manage.manageResourcePacks", "リソースパックを管理");
        t.put("instances.manage.resourcePacks.pageTitle", "リソースパックの管理");
        t.put("instances.manage.resourcePacks.scanning", "リソースパック一覧を読み込み中...");
        t.put("instances.manage.resourcePacks.count", "リソースパック %d 個");
        t.put("instances.manage.resourcePacks.empty", "このインスタンスにはリソースパックがありません");
        t.put("instances.manage.resourcePacks.scanFailed", "リソースパック一覧の読み込みに失敗しました：%s");
        t.put("instances.manage.resourcePacks.toggleFailed", "リソースパックの状態変更に失敗しました：%s");
        t.put("instances.manage.resourcePacks.enabled", "有効");
        t.put("instances.manage.resourcePacks.disabled", "無効");
        t.put("instances.manage.resourcePacks.version", "バージョン：%s");

        // MOD ダウンロード
        t.put("mods.nav", "MOD");
        t.put("mods.search.title", "MOD を検索");
        t.put("mods.search.name", "名前");
        t.put("mods.search.nameHint", "MOD 名を入力");
        t.put("mods.search.source", "ソース");
        t.put("mods.search.gameVersion", "ゲームバージョン");
        t.put("mods.search.versionHint", "例: 1.20.1（空欄ですべて）");
        t.put("mods.search.loader", "ローダー");
        t.put("mods.search.allSources", "すべてのソース");
        t.put("mods.search.allLoaders", "すべてのローダー");
        t.put("mods.search.button", "検索");
        t.put("mods.search.reset", "条件をリセット");
        t.put("mods.search.loading", "MOD を検索しています...");
        t.put("mods.search.empty", "一致する MOD が見つかりません");
        t.put("mods.search.count", "全 %d 件の結果");
        t.put("mods.search.failed", "MOD の読み込みに失敗しました: %s");
        t.put("mods.page.previous", "前へ");
        t.put("mods.page.next", "次へ");
        t.put("mods.page.info", "%d ページ");
        t.put("mods.source.modrinth", "Modrinth");
        t.put("mods.source.curseforge", "CurseForge");
        t.put("mods.loader.fabric", "Fabric");
        t.put("mods.loader.forge", "Forge");
        t.put("mods.loader.neoforge", "NeoForge");
        t.put("mods.loader.quilt", "Quilt");
        t.put("mods.value.unknown", "不明");
        t.put("mods.list.meta", "ローダー: %s · バージョン: %s · ダウンロード %s · 更新 %s");
        t.put("mods.detail.loading", "MOD の詳細を読み込んでいます...");
        t.put("mods.detail.count", "全 %d 件のファイルバージョン");
        t.put("mods.detail.meta", "ダウンロード %s · 更新 %s");
        t.put("mods.detail.versionFilter", "ゲームバージョン");
        t.put("mods.detail.loaderFilter", "ローダー");
        t.put("mods.detail.allVersions", "すべてのバージョン");
        t.put("mods.detail.allLoaders", "すべてのローダー");
        t.put("mods.detail.empty", "現在の条件に一致するバージョンがありません");
        t.put("mods.detail.versionMeta", "公開 %s · ダウンロード %s · ファイル %s");
        t.put("mods.detail.download", "ダウンロード");
        t.put("mods.detail.downloading", "%s をダウンロードしています...");
        t.put("mods.detail.downloaded", "ダウンロード先: %s");
        t.put("mods.detail.noGameDirectory", "ゲームディレクトリを設定してください");
        t.put("mods.detail.noInstance", "先にゲームインスタンスを選択してください");
        t.put("mods.detail.failed", "操作に失敗しました: %s");
        t.put("mods.detail.dependencies", "依存関係:");
        t.put("mods.detail.noDependencies", "依存関係なし");
        t.put("mods.dependency.required", "必須");
        t.put("mods.dependency.optional", "任意");
        t.put("mods.dependency.incompatible", "非互換");
        t.put("mods.dependency.embedded", "内蔵");
        t.put("mods.dependency.unknown", "不明");
        t.put("mod.error.curseforgeKeyMissing", "CurseForge API キーが設定されていません。CURSEFORGE_API_KEY または -Dcaml.curseforge.apiKey を設定してください。");
        t.put("mod.error.downloadUrlMissing", "この MOD バージョンには公式のダウンロード URL がありません: %s");
        t.put("mod.error.projectNotFound", "MOD プロジェクトが見つかりません: %s");
        t.put("mod.error.iconUrlInvalid", "MOD アイコンの URL が無効です: %s");
        t.put("mod.error.iconEmpty", "MOD アイコンファイルが空です: %s");

        // バックグラウンド操作名
        t.put("operation.inspectGameDirectory", "ゲームディレクトリを確認");
        t.put("operation.inspectInstance", "インスタンス情報を読み込む");
        t.put("operation.openInstanceFolder", "インスタンスフォルダーを開く");
        t.put("operation.loadInstanceModIcon", "MOD アイコンを読み込む");
        t.put("operation.setModDisabled", "MOD の状態を切り替える");
        t.put("operation.setResourcePackDisabled", "リソースパックの状態を切り替える");
        t.put("operation.checkModUpdates", "MOD の更新を確認");
        t.put("operation.updateMod", "MOD %s を更新");
        t.put("operation.checkInstanceName", "インスタンス名を確認");
        t.put("operation.fetchVersionList", "公式 %s 一覧を取得");
        t.put("operation.installVanilla", "バニラインスタンス %s をインストール");
        t.put("operation.fetchFabricLoader", "Fabric Loader バージョンを取得");
        t.put("operation.fetchFabricApi", "Fabric API バージョンを取得");
        t.put("operation.installFabric", "Fabric インスタンス %s をインストール");
        t.put("operation.fetchForge", "Forge バージョンを取得");
        t.put("operation.fetchNeoForge", "NeoForge バージョンを取得");
        t.put("operation.fetchOptiFine", "OptiFine バージョンを取得");
        t.put("operation.installLoader", "%s インスタンス %s をインストール");
        t.put("operation.scanJava", "Java をスキャン");
        t.put("operation.addManualJava", "手動 Java を追加");
        t.put("operation.saveJavaSelection", "Java の選択を保存");
        t.put("operation.saveMaxMemory", "最大メモリ設定を保存");
        t.put("operation.saveJvmArguments", "JVM 引数を保存");
        t.put("operation.saveGameDirectory", "ゲームディレクトリを保存");
        t.put("operation.saveVersionIsolation", "バージョン分離設定を保存");
        t.put("operation.saveAutoGameLanguage", "ゲーム言語の自動設定を保存");
        t.put("operation.saveDownloadConcurrency", "ダウンロードの並列数を保存");
        t.put("operation.saveDownloadSource", "ダウンロード元の設定を保存");
        t.put("operation.saveTheme", "テーマの選択を保存");
        t.put("operation.saveLanguage", "言語設定を保存");
        t.put("operation.saveLanguageFallback", "言語フォールバック設定を保存");
        t.put("operation.searchMods", "MOD を検索: %s");
        t.put("operation.fetchModProject", "MOD プロジェクトを取得: %s");
        t.put("operation.downloadMod", "MOD をダウンロード: %s");
        t.put("operation.loadModIcon", "MOD アイコンを読み込む");
        t.put("operation.saveLastInstance", "最後に起動したインスタンスを保存");
        t.put("operation.launchGame", "ゲーム %s を起動");

        // Quilt ローダー
        t.put("download.description.quilt", "Quilt Loader %s");
        t.put("download.description.quiltWithApi", "Quilt Loader %s + Quilted Fabric API %s");
        t.put("download.setup.option.quilt", "Quilt バージョンを選択");
        t.put("download.setup.option.quiltApi", "Quilted Fabric API バージョンを選択");
        t.put("download.prompt.loading.quilt", "Quilt バージョンを取得しています...");
        t.put("download.prompt.quiltFirst", "先に Quilt バージョンを選択してください");
        t.put("download.prompt.select.quilt", "Quilt バージョンを選択してください");
        t.put("download.prompt.select.quiltApi", "Quilted Fabric API バージョンを選択してください");
        t.put("download.prompt.empty.quilt", "利用できる Quilt バージョンがありません");
        t.put("download.prompt.empty.quiltApi", "利用できる Quilted Fabric API バージョンがありません");
        t.put("download.prompt.error.quilt", "Quilt バージョンの取得に失敗しました");
        t.put("download.prompt.error.quiltApi", "Quilted Fabric API バージョンの取得に失敗しました");
        t.put("download.stage.quilt.config.fetch", "Quilt 設定を取得しています");
        t.put("download.stage.quilt.loader.install", "Quilt Loader をインストールしています");
        t.put("download.stage.quilt.api.download", "Quilted Fabric API をダウンロードしています");
        t.put("download.stage.quilt.verify", "Quilt のインストールを検証しています");
        t.put("log.download.installed.quilt", "[ダウンロード] Quilt インスタンス %s をインストールしました (Loader %s)");
        t.put("operation.fetchQuiltLoader", "Quilt Loader のバージョンを取得");
        t.put("operation.fetchQuiltApi", "Quilted Fabric API のバージョンを取得");
        t.put("operation.installQuilt", "Quilt インスタンス %s をインストール");
    }
}

