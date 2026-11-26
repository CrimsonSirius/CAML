package org.chs.camlauncher.language.generator.lang;

import org.chs.camlauncher.language.generator.TranslationTable;

/** 日本語の共通文言（アカウント・設定・タスクなど）。 */
final class JAJPSharedTexts {
    private JAJPSharedTexts() {
    }

    static void populate(TranslationTable t) {
        // アカウント
        t.put("account.choice.default", "既定のアカウントに従う");
        t.put("account.choice.offline", "オフラインのユーザー名");
        t.put("account.default.saved", "%s を既定のアカウントに設定しました");
        t.put("account.error.passwordRequired", "パスワードを入力してください");
        t.put("account.error.serverRequired", "認証サーバーのアドレスを入力してください");
        t.put("account.error.usernameRequired", "ユーザー名を入力してください");
        t.put("account.injector.builtin", "同梱バージョンを使用（必要に応じてダウンロード）");
        t.put("account.injector.chooserTitle", "authlib-injector の jar を選択");
        t.put("account.injector.custom", "カスタムファイル: %s");
        t.put("account.injector.reset", "同梱バージョンを使用");
        t.put("account.injector.saved", "authlib-injector の設定を保存しました");
        t.put("account.instance.count", "%d 個のインスタンスがあります。起動に使うアカウントを選択してください");
        t.put("account.instance.loading", "アカウントを読み込んでいます...");
        t.put("account.instance.mismatch",
                "一部のインスタンスのアカウントは %s に紐付けられており、このゲームの場所では適用されません（%d 個）");
        t.put("account.instance.mismatchApplied",
                "%d 個のインスタンスのアカウント紐付けをこのゲームの場所に適用しました");
        t.put("account.instance.mismatchApply", "このゲームの場所に適用");
        t.put("account.instance.saved", "インスタンス %s のアカウントを保存しました");
        t.put("account.instance.title", "インスタンスのアカウント");
        t.put("account.list.default", "既定");
        t.put("account.list.empty",
                "アカウントがまだありません。オフラインアカウントを追加するか、Microsoft でサインインするか、"
                        + "サードパーティのサーバーにサインインしてください。");
        t.put("account.list.needsLogin", "サインインの有効期限が切れました");
        t.put("account.list.relogin", "再サインイン");
        t.put("account.list.remove", "削除");
        t.put("account.login.customServer", "カスタムサーバーアドレス");
        t.put("account.login.customServerOption", "カスタムアドレス");
        t.put("account.login.hint", "パスワードは今回のサインインにのみ使用され、保存されません。");
        t.put("account.login.password", "パスワード");
        t.put("account.login.reloginHint",
                "%s のパスワードをもう一度入力してください。ランチャーはパスワードを保存しません。");
        t.put("account.login.remember", "サインイン状態を保持");
        t.put("account.login.running", "サインインしています...");
        t.put("account.login.server", "認証サーバー");
        t.put("account.login.submit", "サインイン");
        t.put("account.login.success", "%s としてサインインしました");
        t.put("account.login.username", "メールアドレスまたはユーザー名");
        t.put("account.microsoft.codeReady",
                "ブラウザーで認証ページを開き、コピーしたコードを入力してください。");
        t.put("account.microsoft.copyCode", "コードをコピー");
        t.put("account.microsoft.copyUrl", "認証ページをコピー");
        t.put("account.microsoft.login", "Microsoft でサインイン");
        t.put("account.microsoft.running", "Microsoft のデバイスコードを取得しています...");
        t.put("account.microsoft.userCode", "デバイスコード: %s");
        t.put("account.microsoft.verificationUri", "認証ページ: %s");
        t.put("account.offline.add", "追加");
        t.put("account.offline.added", "オフラインアカウント %s を追加しました");
        t.put("account.offline.label", "オフラインアカウントを追加");
        t.put("account.offline.placeholder", "オフラインのユーザー名");
        t.put("account.profile.label", "プロフィール");
        t.put("account.profile.switch", "切り替え");
        t.put("account.profile.switched", "プロフィールを %s に切り替えました");
        t.put("account.removed", "%s を削除しました");

        // 認証エラー
        t.put("auth.error.accountMissing", "アカウントが見つかりません: %s");
        t.put("auth.error.accountRequired", "先にアカウントを選択してください");
        t.put("auth.error.injectorChecksum", "authlib-injector の検証に失敗しました: %s");
        t.put("auth.error.injectorNotFound", "authlib-injector が見つかりません: %s");
        t.put("auth.error.invalidCredentials", "%s のユーザー名またはパスワードが正しくありません");
        t.put("auth.error.loginExpired",
                "%s（%s）のサインインが失効しました。もう一度サインインしてください");
        t.put("auth.error.loginRequired",
                "アカウント %s（%s）には保存された資格情報がありません。もう一度サインインしてください");
        t.put("auth.error.metadataUnavailable", "認証サーバーのメタデータを読み取れません: %s（HTTP %s）");
        t.put("auth.error.noAccount",
                "アカウントが設定されていません。設定で追加するか、オフラインのユーザー名を使用してください。");
        t.put("auth.error.noSelectedProfile", "このアカウントには Minecraft プロフィールがありません: %s");
        t.put("auth.error.passwordRequired", "パスワードが必要です");
        t.put("auth.error.profileMissing", "そのプロフィールは利用できません: %s");
        t.put("auth.error.requestFailed", "認証リクエストに失敗しました: %s（HTTP %s）");
        t.put("auth.error.responseInvalid", "認証サーバーが予期しない応答を返しました: %s");
        t.put("auth.error.tooManyRequests",
                "%s へのリクエストが多すぎます。しばらく待ってから再試行してください");
        t.put("auth.error.usernameRequired", "ユーザー名が必要です");

        // Microsoft 認証エラー
        t.put("auth.microsoft.accountSuspended", "この Microsoft アカウントは停止されている可能性があります。");
        t.put("auth.microsoft.clientIdMissing",
                "Microsoft ログインが設定されていません。CAML の Microsoft OAuth クライアント ID を設定してください。");
        t.put("auth.microsoft.forbidden",
                "現在のネットワークは Minecraft Services に拒否されました。別のネットワークで試してください。");
        t.put("auth.microsoft.loginDeclined", "Microsoft のサインインが拒否されました。");
        t.put("auth.microsoft.loginExpired", "Microsoft のサインインが失効しました。もう一度サインインしてください。");
        t.put("auth.microsoft.loginFailed", "Microsoft のサインインに失敗しました: %s");
        t.put("auth.microsoft.loginTimeout", "Microsoft のデバイスコードが失効しました。再試行してください。");
        t.put("auth.microsoft.missingField", "Microsoft 認証の応答に %s がありません。");
        t.put("auth.microsoft.noLicense",
                "このアカウントは Minecraft: Java Edition を所有していないか、Game Pass の特典が失効しています。");
        t.put("auth.microsoft.noProfile",
                "このアカウントには Minecraft プロフィールがありません。作成してから再度サインインしてください。");
        t.put("auth.microsoft.profileChanged",
                "アカウントの更新中に Minecraft プロフィールが変更されました。もう一度サインインしてください。");
        t.put("auth.microsoft.requestFailed", "%s で Microsoft 認証リクエストに失敗しました（HTTP %s）。");
        t.put("auth.microsoft.responseInvalid", "%s から予期しない Microsoft 認証応答が返されました。");
        t.put("auth.microsoft.serviceUnavailable",
                "Minecraft Services は一時的に利用できません。後でもう一度お試しください。");
        t.put("auth.microsoft.tooManyRequests",
                "サインインの試行回数が多すぎます。数分待ってから再試行してください。");
        t.put("auth.microsoft.wrongVerifyMethod",
                "Microsoft のサインインページでは、他の方法ではなくパスワードを使用してください。");
        t.put("auth.microsoft.xboxBanned", "この Microsoft アカウントは Xbox Live で禁止されています。");
        t.put("auth.microsoft.xboxFamily",
                "この子供のアカウントは、先に Microsoft ファミリーに追加する必要があります。");
        t.put("auth.microsoft.xboxNoAccount",
                "この Microsoft アカウントには Xbox アカウントがありません。作成してから再試行してください。");
        t.put("auth.microsoft.xboxRegion", "現在の国または地域では Xbox Live を利用できません。");
        t.put("auth.microsoft.xboxUnknown", "Xbox Live がサインインを拒否しました（XErr %s）。");

        // 共通
        t.put("common.close", "閉じる");
        t.put("common.reset", "リセット");

        // 展開制限
        t.put("download.settings.archive.description", "Mod パックとワールドの展開時に適用される制限です。");
        t.put("download.settings.archive.entries", "エントリ数の上限");
        t.put("download.settings.archive.entrySize", "エントリサイズの上限（MiB）");
        t.put("download.settings.archive.invalid",
                "有効な制限を入力してください。合計サイズはエントリの制限以上である必要があります。");
        t.put("download.settings.archive.saved", "展開制限を保存しました。");
        t.put("download.settings.archive.title", "展開制限");
        t.put("download.settings.archive.totalSize", "展開後の合計サイズの上限（MiB）");

        // ホーム
        t.put("home.account", "アカウント");
        t.put("home.error.loadAccounts", "アカウントの読み込みに失敗しました: %s");
        t.put("home.manageAccounts", "インスタンスのアカウント");
        t.put("home.username.managed", "ユーザー名は選択したアカウントから取得されます: %s");

        // 起動警告
        t.put("launch.warning.java.force", "強制起動");
        t.put("launch.warning.java.message",
                "このゲームには Java %s が必要ですが、Java %s が選択されています。強制起動すると失敗する可能性があります。");
        t.put("launch.warning.java.stop", "中止");
        t.put("launch.warning.java.title", "Java のバージョンが互換でない可能性があります");

        // ログ
        t.put("log.collapse", "折りたたむ");
        t.put("log.empty", "まだ記録がありません");
        t.put("log.expand", "展開");

        // Mod パック
        t.put("modpack.error.invalidArchive", "Mod パックのアーカイブが無効です: %s");
        t.put("modpack.error.invalidFile", "Mod パックのファイルパスが無効です: %s");
        t.put("modpack.error.missingFileUrl", "Mod パックのファイルにダウンロード URL がありません: %s");
        t.put("modpack.error.missingMinecraft", "Mod パックに Minecraft のバージョンが指定されていません");
        t.put("modpack.error.unsupportedLoader", "Mod パックのローダーは未対応です: %s");
        t.put("modpacks.detail.install", "Mod パックをインストール");
        t.put("modpacks.detail.installed", "Mod パックをインストールしました: %s");
        t.put("modpacks.detail.installing", "Mod パック %s をインストールしています...");
        t.put("modpacks.detail.loading", "Mod パックの詳細を読み込んでいます...");
        t.put("modpacks.install.instanceName", "インスタンス名");
        t.put("modpacks.install.instanceNameEmpty", "インスタンス名を空にできません");
        t.put("modpacks.list.meta", "バージョン: %s · ダウンロード %s · 更新 %s");
        t.put("modpacks.nav", "Mod パック");
        t.put("modpacks.search.empty", "一致する Mod パックが見つかりません");
        t.put("modpacks.search.failed", "Mod パックの読み込みに失敗しました: %s");
        t.put("modpacks.search.loading", "Mod パックを検索しています...");
        t.put("modpacks.search.nameHint", "Mod パック名を入力");
        t.put("modpacks.search.title", "Mod パックを検索");

        // 操作名
        t.put("operation.addOfflineAccount", "オフラインアカウントを追加");
        t.put("operation.downloadJava", "Java %s をダウンロードしてインストール");
        t.put("operation.fetchAuthServer", "認証サーバー情報を取得");
        t.put("operation.fetchJavaDownloadList", "Java のダウンロード一覧を取得: %s");
        t.put("operation.fetchModpackProject", "Mod パックのプロジェクトを取得 %s");
        t.put("operation.installModpack", "Mod パック %s をインストール");
        t.put("operation.loadAccounts", "アカウントを読み込む");
        t.put("operation.loadInstanceAccount", "インスタンスのアカウントを読み込む");
        t.put("operation.loginAccount", "アカウントにサインイン");
        t.put("operation.loginMicrosoftAccount", "Microsoft でサインイン");
        t.put("operation.openJavaFolder", "Java のフォルダーを開く");
        t.put("operation.removeAccount", "アカウントを削除");
        t.put("operation.saveArchiveExtractionLimits", "展開制限を保存");
        t.put("operation.saveDefaultAccount", "既定のアカウントを保存");
        t.put("operation.saveInjectorPath", "authlib-injector のパスを保存");
        t.put("operation.saveInstanceAccount", "インスタンスのアカウントを保存");
        t.put("operation.searchModpacks", "Mod パックを検索 %s");
        t.put("operation.switchProfile", "アカウントのプロフィールを切り替え");

        // 設定: このアプリについて
        t.put("settings.about.application.title", "ランチャー");
        t.put("settings.about.architecture", "アーキテクチャ");
        t.put("settings.about.backend", "バックエンド");
        t.put("settings.about.copyright", "著作権");
        t.put("settings.about.dataDirectory", "データディレクトリ");
        t.put("settings.about.dependencies.title", "プロジェクトの依存関係");
        t.put("settings.about.dependency.author", "作者: %s");
        t.put("settings.about.dependency.license", "ライセンス: %s");
        t.put("settings.about.device.title", "デバイス情報");
        t.put("settings.about.directories.title", "ディレクトリ");
        t.put("settings.about.gameDirectory", "ゲームディレクトリ");
        t.put("settings.about.javaVendor", "Java ベンダー");
        t.put("settings.about.javaVersion", "Java バージョン");
        t.put("settings.about.kernelVersion", "カーネルバージョン");
        t.put("settings.about.license", "ライセンス");
        t.put("settings.about.memory", "システムメモリ");
        t.put("settings.about.notDetected", "検出されません");
        t.put("settings.about.os", "オペレーティングシステム");
        t.put("settings.about.osVersion", "システムバージョン");
        t.put("settings.about.project.title", "プロジェクト");
        t.put("settings.about.projectName", "名前");
        t.put("settings.about.runtime.title", "ランチャーの実行環境");
        t.put("settings.about.system.title", "システム");
        t.put("settings.about.unknown", "不明");
        t.put("settings.about.version", "バージョン");

        // 設定: アカウント
        t.put("settings.account.description",
                "ランチャーが保存しているアカウントです。アクセストークンは暗号化してから保存され、"
                        + "パスワードは保存されません。");
        t.put("settings.account.injector.description",
                "authlib-injector は初回のみダウンロードして再利用し、起動のたびに SHA-256 で検証します。");
        t.put("settings.account.login.description",
                "Yggdrasil 認証サーバー（LittleSkin など）のアカウントでサインインします。");
        t.put("settings.account.login.title", "サードパーティのサインイン");
        t.put("settings.account.microsoft.description",
                "デバイスコードを使って Microsoft にサインインします。パスワードをランチャーに入力する必要はありません。");
        t.put("settings.account.microsoft.title", "Microsoft アカウント");
        t.put("settings.account.title", "アカウント");

        // 設定: Java の管理
        t.put("settings.javaManagement.count", "%d 個の Java ランタイム");
        t.put("settings.javaManagement.distribution", "ディストリビューション");
        t.put("settings.javaManagement.download", "Java をダウンロード");
        t.put("settings.javaManagement.downloadAndInstall", "ダウンロードしてインストール");
        t.put("settings.javaManagement.downloading", "Java をダウンロードしています...");
        t.put("settings.javaManagement.downloadingProgress", "Java をダウンロードしています... %d%%");
        t.put("settings.javaManagement.empty", "Java ランタイムが見つかりません");
        t.put("settings.javaManagement.emptyVersions", "ダウンロードできる Java バージョンがありません");
        t.put("settings.javaManagement.installed", "Java をインストールしました: %s");
        t.put("settings.javaManagement.installFailed", "Java のインストールに失敗しました: %s");
        t.put("settings.javaManagement.linuxHint",
                "Linux ではディストリビューションのパッケージマネージャー（apt、dnf、pacman など）で "
                        + "JDK または JRE をインストールしてください。CAML による Java の管理は推奨しません。");
        t.put("settings.javaManagement.loadingVersions", "Java のバージョンを読み込んでいます...");
        t.put("settings.javaManagement.loadVersionsFailed", "Java のバージョンの読み込みに失敗しました: %s");
        t.put("settings.javaManagement.manual", "手動で追加");
        t.put("settings.javaManagement.meta", "%s · %d ビット%s");
        t.put("settings.javaManagement.openFolder", "フォルダーを開く");
        t.put("settings.javaManagement.openFolder.failed", "Java のフォルダーを開けませんでした: %s");
        t.put("settings.javaManagement.openFolder.opening", "フォルダーを開いています...");
        t.put("settings.javaManagement.packageType", "パッケージの種類");
        t.put("settings.javaManagement.unavailable", "利用できません");
        t.put("settings.javaManagement.version", "バージョン");
        t.put("settings.javaManagement.versionCount", "%d 個のバージョン");

        // 設定: セクション
        t.put("settings.section.about", "このアプリについて");
        t.put("settings.section.account", "アカウント");
        t.put("settings.section.java", "Java の管理");

        // タスク
        t.put("tasks.back", "タスク一覧に戻る");
        t.put("tasks.empty", "タスクはまだありません");
        t.put("tasks.open", "タスク %s を開く · %s");
        t.put("tasks.status.cancelled", "キャンセル済み");
        t.put("tasks.status.completed", "完了");
        t.put("tasks.status.failed", "失敗");
        t.put("tasks.status.running", "実行中");
        t.put("tasks.subtitle", "ダウンロードとインストール");
        t.put("tasks.title", "タスク");
    }
}

