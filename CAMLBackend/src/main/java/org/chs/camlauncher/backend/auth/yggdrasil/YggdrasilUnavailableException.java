package org.chs.camlauncher.backend.auth.yggdrasil;

import org.chs.camlauncher.backend.error.BackendError;
import org.chs.camlauncher.backend.error.BackendException;

/**
 * 认证服务器<b>暂时</b>不可用：网络失败、超时、5xx 或限流。
 *
 * <p>与其它 {@link BackendException} 的区别在于语义：这不是“凭据被拒绝”，而是
 * “本次无法确认凭据状态”。启动流程遇到它可以继续使用已有的 accessToken
 * （单人生存不依赖认证服务器，多人服务器的校验由游戏自己完成），
 * 登录流程则把同一个对象原样展示给用户。</p>
 */
public class YggdrasilUnavailableException extends BackendException {
    public YggdrasilUnavailableException(BackendError error) {
        super(error);
    }

    public YggdrasilUnavailableException(BackendError error, Throwable cause) {
        super(error, cause);
    }
}

