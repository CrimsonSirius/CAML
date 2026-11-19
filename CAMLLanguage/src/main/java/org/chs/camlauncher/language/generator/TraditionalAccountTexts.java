package org.chs.camlauncher.language.generator;

/**
 * 繁體中文的帳號文案覆蓋。
 *
 * <p>與 {@link AccountTexts} 相同的「僅在鍵不存在時寫入」策略，因此在
 * {@code populateSharedDefaults} 裡先呼叫本類、再呼叫英文預設值即可：
 * 已翻譯的鍵保留繁中，其餘仍回退英文。</p>
 */
public final class TraditionalAccountTexts {
    private TraditionalAccountTexts() {
    }

    public static void populate(TranslationTable t) {
        put(t, "account.choice.default", "跟隨預設帳號");
        put(t, "account.choice.offline", "離線使用者名稱");
        put(t, "home.account", "帳號");
        put(t, "home.manageAccounts", "實例帳號");
        put(t, "home.error.loadAccounts", "讀取帳號失敗：%s");
        put(t, "home.username.managed", "使用者名稱由所選帳號決定：%s");
        put(t, "settings.section.account", "帳號");
        put(t, "settings.account.title", "帳號");
        put(t, "settings.account.description",
                "啟動器儲存的帳號。accessToken 加密後才寫入磁碟，密碼不會被儲存。");
        put(t, "account.list.empty", "還沒有帳號。可以新增離線帳號、使用微軟登入，或登入第三方認證伺服器。");
        put(t, "account.list.default", "預設");
        put(t, "account.list.remove", "移除");
        put(t, "account.list.needsLogin", "登入已失效");
        put(t, "account.list.relogin", "重新登入");
        put(t, "account.default.saved", "%s 已設為預設帳號");
        put(t, "account.removed", "已移除 %s");
        put(t, "account.profile.label", "角色");
        put(t, "account.profile.switch", "切換");
        put(t, "account.profile.switched", "已切換角色為 %s");
        put(t, "account.offline.label", "新增離線帳號");
        put(t, "account.offline.placeholder", "離線使用者名稱");
        put(t, "account.offline.add", "新增");
        put(t, "account.offline.added", "已新增離線帳號 %s");
        put(t, "settings.account.microsoft.title", "微軟帳號");
        put(t, "settings.account.microsoft.description", "使用裝置碼登入微軟帳號，啟動器不會要求輸入密碼。");
        put(t, "account.microsoft.login", "使用微軟登入");
        put(t, "account.microsoft.running", "正在申請微軟裝置碼...");
        put(t, "account.microsoft.codeReady", "請在瀏覽器開啟驗證頁面，並輸入已複製的裝置碼。");
        put(t, "account.microsoft.userCode", "裝置碼：%s");
        put(t, "account.microsoft.verificationUri", "驗證頁面：%s");
        put(t, "account.microsoft.copyCode", "複製裝置碼");
        put(t, "account.microsoft.copyUrl", "複製驗證頁面");
        put(t, "settings.account.login.title", "第三方登入");
        put(t, "settings.account.login.description",
                "使用 Yggdrasil 認證伺服器（例如 LittleSkin）的帳號登入。");
        put(t, "account.login.server", "認證伺服器");
        put(t, "account.login.customServer", "自訂伺服器位址");
        put(t, "account.login.customServerPlaceholder", "https://example.com/api/yggdrasil");
        put(t, "account.login.customServerOption", "自訂位址");
        put(t, "account.login.username", "電子郵件或使用者名稱");
        put(t, "account.login.password", "密碼");
        put(t, "account.login.hint", "密碼只用於本次登入，不會被儲存。");
        put(t, "account.login.remember", "記住登入狀態");
        put(t, "account.login.submit", "登入");
        put(t, "account.login.running", "正在登入...");
        put(t, "account.login.reloginHint", "請重新輸入 %s 的密碼，啟動器不會儲存密碼。");
        put(t, "account.login.success", "已登入為 %s");
        put(t, "account.error.serverRequired", "請填寫認證伺服器位址");
        put(t, "account.error.usernameRequired", "請填寫使用者名稱");
        put(t, "account.error.passwordRequired", "請填寫密碼");
        put(t, "settings.account.injector.title", "authlib-injector");
        put(t, "settings.account.injector.description",
                "啟動器只在首次使用時下載 authlib-injector 並長期重複使用，每次啟動都會以 SHA-256 驗證。");
        put(t, "account.injector.builtin", "使用內建版本（視需要下載）");
        put(t, "account.injector.custom", "自訂檔案：%s");
        put(t, "account.injector.reset", "使用內建版本");
        put(t, "account.injector.saved", "authlib-injector 設定已儲存");
        put(t, "account.injector.chooserTitle", "選擇 authlib-injector jar");
        put(t, "account.instance.title", "實例帳號");
        put(t, "account.instance.loading", "正在讀取帳號...");
        put(t, "account.instance.count", "共 %d 個實例，為每個實例選擇啟動時使用的帳號");
        put(t, "account.instance.saved", "已儲存實例 %s 的帳號");
        put(t, "operation.loginMicrosoftAccount", "微軟帳號登入");
        put(t, "account.instance.mismatch", "部分實例帳號原本綁定在 %s，目前不生效（共 %d 個實例）");
        put(t, "account.instance.mismatchApply", "套用到目前遊戲位置");
        put(t, "account.instance.mismatchApplied", "已將 %d 個實例帳號綁定套用到目前遊戲位置");
        put(t, "operation.loadAccounts", "讀取帳號");
        put(t, "operation.loginAccount", "登入帳號");
        put(t, "operation.addOfflineAccount", "新增離線帳號");
        put(t, "operation.fetchAuthServer", "取得認證伺服器資訊");
        put(t, "operation.saveDefaultAccount", "儲存預設帳號");
        put(t, "operation.removeAccount", "移除帳號");
        put(t, "operation.loadInstanceAccount", "讀取實例帳號");
        put(t, "operation.saveInstanceAccount", "儲存實例帳號");
        put(t, "operation.saveInjectorPath", "儲存 authlib-injector 路徑");
        put(t, "operation.switchProfile", "切換帳號角色");
        put(t, "auth.error.usernameRequired", "請填寫使用者名稱");
        put(t, "auth.error.passwordRequired", "請填寫密碼");
        put(t, "auth.error.metadataUnavailable", "無法讀取認證伺服器資訊：%s（HTTP %s）");
        put(t, "auth.error.invalidCredentials", "%s 的使用者名稱或密碼錯誤");
        put(t, "auth.error.requestFailed", "認證請求失敗：%s（HTTP %s）");
        put(t, "auth.error.responseInvalid", "認證伺服器回傳了無法解析的回應：%s");
        put(t, "auth.error.noSelectedProfile", "該帳號沒有 Minecraft 角色：%s");
        put(t, "auth.error.profileMissing", "該角色已不存在：%s");
        put(t, "auth.error.tooManyRequests", "對 %s 的請求過於頻繁，請稍後再試");
        put(t, "auth.error.accountMissing", "找不到帳號：%s");
        put(t, "auth.error.accountRequired", "請先選擇帳號");
        put(t, "auth.error.loginRequired", "帳號 %s（%s）沒有儲存的憑證，請重新登入");
        put(t, "auth.error.loginExpired", "帳號 %s（%s）的登入已失效，請重新登入");
        put(t, "auth.error.noAccount", "尚未設定帳號，請到設定中新增帳號，或使用離線使用者名稱。");
        put(t, "auth.error.injectorNotFound", "找不到 authlib-injector：%s");
        put(t, "auth.error.injectorChecksum", "authlib-injector 驗證失敗：%s");
        put(t, "auth.microsoft.clientIdMissing", "尚未設定微軟登入 Client ID，請設定 CAML 的 Microsoft OAuth Client ID。");
        put(t, "auth.microsoft.requestFailed", "微軟認證請求失敗：%s（HTTP %s）");
        put(t, "auth.microsoft.responseInvalid", "微軟認證伺服器回傳了無法解析的回應：%s");
        put(t, "auth.microsoft.missingField", "微軟認證回應缺少欄位：%s");
        put(t, "auth.microsoft.loginTimeout", "微軟裝置碼已過期，請重試。");
        put(t, "auth.microsoft.loginDeclined", "微軟登入已被拒絕。");
        put(t, "auth.microsoft.loginFailed", "微軟登入失敗：%s");
        put(t, "auth.microsoft.loginExpired", "微軟登入已失效，請重新登入。");
        put(t, "auth.microsoft.wrongVerifyMethod", "請在微軟登入頁面選擇使用密碼登入，不要使用其他登入方式。");
        put(t, "auth.microsoft.xboxBanned", "該微軟帳號已被 Xbox Live 封鎖。");
        put(t, "auth.microsoft.xboxNoAccount", "該微軟帳號尚未註冊 Xbox 帳戶，請註冊後重試。");
        put(t, "auth.microsoft.xboxRegion", "目前國家或地區無法使用 Xbox Live。");
        put(t, "auth.microsoft.xboxFamily", "該兒童帳號需要先加入微軟家庭。");
        put(t, "auth.microsoft.xboxUnknown", "Xbox Live 拒絕了登入（XErr %s）。");
        put(t, "auth.microsoft.tooManyRequests", "登入嘗試過於頻繁，請等待幾分鐘後重試。");
        put(t, "auth.microsoft.forbidden", "目前網路被 Minecraft Services 拒絕，請更換網路後重試。");
        put(t, "auth.microsoft.serviceUnavailable", "Minecraft Services 暫時無法使用，請稍後重試。");
        put(t, "auth.microsoft.accountSuspended", "該微軟帳號可能已被封鎖。");
        put(t, "auth.microsoft.noLicense", "該帳號沒有 Minecraft Java 版，或 Game Pass 權益已過期。");
        put(t, "auth.microsoft.noProfile", "該帳號尚未建立 Minecraft 角色，請建立後重新登入。");
        put(t, "auth.microsoft.profileChanged", "重新整理帳號時 Minecraft 角色發生變化，請重新登入。");
    }

    private static void put(TranslationTable t, String key, String value) {
        if (!t.containsKey(key)) {
            t.put(key, value);
        }
    }
}
