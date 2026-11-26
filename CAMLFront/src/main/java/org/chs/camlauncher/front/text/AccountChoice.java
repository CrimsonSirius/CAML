package org.chs.camlauncher.front.text;

import org.chs.camlauncher.backend.auth.AccountSummary;

/**
 * 账号在界面上的展示文本。
 *
 * <p>账号下拉框同时包含“跟随默认账号”和“离线用户名”两个特殊项，用固定前缀区分，
 * 避免让 JavaFX 下拉框承载 {@code null} 值。</p>
 */
public final class AccountChoice {
    /** 使用实例账号 → 全局账号的解析结果。 */
    public static final String DEFAULT = "@default";
    /** 使用主页输入的离线用户名。 */
    public static final String OFFLINE = "@offline";

    private AccountChoice() {
    }

    public static boolean isAccountId(String value) {
        return value != null && !value.isBlank()
                && !value.equals(DEFAULT) && !value.equals(OFFLINE);
    }

    /** 账号在列表中的显示文本：用户名 · 服务器名（离线账号只显示用户名）。 */
    public static String label(AccountSummary account) {
        if (account == null) {
            return "";
        }
        if (!account.yggdrasil() || account.serverName().isBlank()) {
            return account.displayName();
        }
        return account.displayName() + " · " + account.serverName();
    }

    public static String label(String choice, Iterable<AccountSummary> accounts) {
        if (choice == null || choice.equals(DEFAULT)) {
            return UiText.tr("account.choice.default");
        }
        if (choice.equals(OFFLINE)) {
            return UiText.tr("account.choice.offline");
        }
        for (AccountSummary account : accounts) {
            if (account.id().equals(choice)) {
                return label(account);
            }
        }
        return choice;
    }
}

