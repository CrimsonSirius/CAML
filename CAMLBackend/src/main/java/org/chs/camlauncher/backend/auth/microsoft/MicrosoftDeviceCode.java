package org.chs.camlauncher.backend.auth.microsoft;

/**
 * 微软设备码登录中需要展示给用户的信息。
 *
 * @param userCode        用户在验证页面输入的代码
 * @param verificationUri 微软验证页面地址
 * @param expiresIn       设备码有效期，单位秒
 * @param interval        轮询间隔，单位秒
 */
public record MicrosoftDeviceCode(
        String userCode,
        String verificationUri,
        int expiresIn,
        int interval
) {
    public MicrosoftDeviceCode {
        if (userCode == null || userCode.isBlank()) {
            throw new IllegalArgumentException("userCode must not be blank");
        }
        if (verificationUri == null || verificationUri.isBlank()) {
            throw new IllegalArgumentException("verificationUri must not be blank");
        }
    }
}
