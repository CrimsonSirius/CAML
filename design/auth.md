# CAML 账号与认证

> 本文按当前认证代码重写，最后核对时间：2026-11-26。

## 1. 支持范围

当前支持四类身份来源：

| 类型 | 网络 | 凭据持久化 | 启动注入 |
|---|---|---|---|
| 离线账号 | 无 | 仅用户名和离线 UUID | 无 |
| Microsoft | OAuth 2.0 设备码 + Xbox/Minecraft Services | accessToken、refreshToken 加密保存 | 无 |
| Yggdrasil | 第三方认证服务器 | 可选保存 accessToken；密码永不保存 | authlib-injector `-javaagent` |
| 主页临时离线用户名 | 无 | 不进入账号列表 | 无 |

密码不会落盘，也不会写入日志。前端只接触 `AccountSummary` 和登录会话结果，不接触 token 明文。

## 2. 账号模型与入口

主要类：

- `AccountService`：账号列表、登录、删除、角色切换、实例绑定和启动会话解析。
- `AccountStore`：`~/.caml/accounts/accounts.json` 的原子读写和 token 加解密。
- `SecretCipher`：AES-GCM 加密，密钥保存在 `~/.caml/accounts/secret.key`。
- `MicrosoftAuthService`：设备码、refresh、Xbox Live、XSTS、Minecraft Services 流程。
- `YggdrasilClient`：metadata、authenticate、refresh、validate。
- `AuthServerService` / `AuthServerStore`：Yggdrasil metadata 的内存和磁盘缓存。
- `AuthlibInjectorService`：authlib-injector 下载、缓存、校验和覆盖路径。

`CAMLBackend` 对前端暴露账号摘要、默认账号、实例账号和第三方服务器预设；登录和刷新都在后端线程执行。

## 3. 离线账号

`OfflineAccount` 根据用户名生成稳定的离线 UUID，规则固定，不依赖网络或启动器重启。离线账号不包含 accessToken。

主页在没有可用账号或选择离线模式时，可以使用输入框中的离线用户名直接启动；该用户名不是账号列表里的持久账号。

## 4. Microsoft 登录

### 4.1 配置

- Client ID 属性：`caml.microsoft.clientId`。
- 构建环境变量：`CAML_MICROSOFT_CLIENT_ID`。
- 构建任务会把 Client ID 写入生成的 `caml.properties`；未配置时微软登录返回 `auth.microsoft.clientIdMissing`。
- scope：`XboxLive.signin offline_access`。

### 4.2 设备码流程

1. POST `https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode` 获取 `device_code`、`user_code`、验证地址、有效期和轮询间隔。
2. 前端显示设备码和验证页面，并提供复制按钮；用户自行在浏览器完成登录。
3. 后端轮询 `https://login.microsoftonline.com/consumers/oauth2/v2.0/token`，最多等待 15 分钟。
4. 使用 Microsoft refresh token 换取 Xbox Live token。
5. 调用 Xbox XSTS `https://xsts.auth.xboxlive.com/xsts/authorize`，RelyingParty 为 `rp://api.minecraftservices.com/`。
6. 用 XSTS 换取 Minecraft access token：`https://api.minecraftservices.com/authentication/login_with_xbox`。
7. 检查 `https://api.minecraftservices.com/entitlements/mcstore`，无 Java 版权益时返回明确错误。
8. 读取 `https://api.minecraftservices.com/minecraft/profile`，得到角色名、UUID 和 access token。

微软登录成功后，accessToken 和 refreshToken 都由 `AccountStore` 加密保存。启动前如果距过期不足 1 小时，会自动使用 refresh token 刷新；刷新得到的 profile UUID 与账号不一致时按 `auth.microsoft.profileChanged` 处理，要求重新登录。

常见 Xbox/XSTS 错误分别映射为家庭限制、地区限制、未注册 Xbox、账号封禁、账号暂停等稳定错误码，不把服务端原文直接展示给用户。

## 5. Yggdrasil 登录

### 5.1 协议调用

`YggdrasilClient` 只实现启动器需要的四个调用：

- `GET <apiRoot>`：读取 metadata（服务器名、skinDomains、原始 metadata）。
- `POST /authserver/authenticate`：用户名 + 密码 + clientToken + agent。
- `POST /authserver/refresh`：刷新 accessToken，可携带 `selectedProfile`。
- `POST /authserver/validate`：验证 token 是否仍有效。

登录时 `requestUser=false`，不会请求用户属性。若认证响应没有 `selectedProfile`（部分服务器多角色账号如此），客户端会立刻用 `refresh + selectedProfile` 补绑定；启动时仍会再次 refresh，保证进入服务器时 token 已绑定角色。

### 5.2 错误分类

- `400/401/403`：凭据或 token 无效，返回固定的登录失效错误或需要重新登录。
- `429`、5xx、网络失败、超时：`YggdrasilUnavailableException`，语义是“暂时无法确认凭据状态”；启动流程可以继续使用已有 token。
- 服务器返回的原文只写 debug 日志，不直接进入界面。

### 5.3 多角色

`StoredAccount.profiles` 保存全部可用角色，`uuid` 是当前角色；账号 ID 使用 profile/account 映射生成并在切换角色时保持不变。切换角色会 refresh token 到新 profile，并把新的 accessToken 写回账号存储或内存会话。

每次启动的 refresh 也会把服务器返回的最新角色名与角色列表写回账号：角色在皮肤站改名后，启动参数与账号列表都不会继续使用旧名字。角色 id 在解析响应时统一规范化为 32 位小写十六进制，格式非法的角色会被忽略，不会把无法启动的 UUID 存进账号文件。

## 6. 凭据存储与“记住登录”

账号文件格式版本为 1，条目字段包括账号 ID、类型、显示名、UUID、apiRoot、服务器名、加密后的 accessToken/refreshToken、clientToken、过期时间和角色列表。

加密方式：

- AES/GCM/NoPadding；
- 随机 12 字节 IV 放在密文前；
- 128 位 GCM tag；
- 密钥文件长度为 16 字节，POSIX 下收紧为 `rw-------`；
- 读取失败时只丢弃 token，不删除账号条目。

“记住登录”的真实语义：

- Yggdrasil 勾选：accessToken 加密写入账号文件，重启后可直接使用/刷新。
- Yggdrasil 不勾选：账号条目仍保存 clientToken、角色列表和服务器信息，accessToken 只放在进程内 `transientSessions`；本次运行可用，重启后要求重新登录。
- Microsoft：设备码登录后 accessToken 和 refreshToken 都加密保存，没有“不记住”分支。

密码只在登录请求生命周期内存在，不进入 `StoredAccount`。

## 7. 默认账号与实例账号

解析顺序：

1. 实例账号：`settings.properties` 中 `instanceAccounts` 以“游戏目录 + 实例 ID”为键。
2. 全局默认账号：`defaultAccountId`。
3. 若仍无账号，主页可回退到输入的离线用户名。

设置文件保存的是账号 ID，不保存 token。删除账号时会清理全局默认和所有实例绑定；游戏位置变化时，旧目录绑定不会静默套用到新目录，界面可以检测并迁移仍然存在的实例绑定。

## 8. authlib-injector

- 固定版本：`1.2.5`。
- 缓存：`~/.caml/authlib-injector/1.2.5/authlib-injector-1.2.5.jar` 和 `.sha256`。
- 来源：authlib-injector 官方 artifact，GitHub release 作为备用候选。
- 每次使用前用 SHA-256 校验；校验失败删除并重新下载。
- 支持设置 `authlibInjectorPath` 覆盖内置版本，跳过自动下载和校验。
- 启动时只追加 JVM 参数：`-javaagent:<jar>=<apiRoot>`；有 metadata 时再追加 `-Dauthlibinjector.yggdrasil.prefetched=<base64>`。
- 不修改版本 JSON、libraries、classpath 或游戏参数。

## 9. metadata 缓存

`AuthServerService` 的回退顺序是“网络 → 内存 → 磁盘”。

- 网络：读取 `<apiRoot>` metadata；
- 内存：`ConcurrentHashMap`；
- 磁盘：`~/.caml/accounts/servers.json`。

metadata 失败不能阻塞 Yggdrasil 密码登录或已有 token 启动；只是无法提供 `prefetched`，authlib-injector 会自行联网获取。

## 10. 安全边界与限制

- Client ID 是公共客户端 ID，不是 secret。
- CurseForge API Key 与账号认证无关，见 Mod 下载文档。
- Windows 没有系统级安全存储集成；账号 token 仅依赖本机 key 文件加密，能访问用户目录的攻击者仍可能同时取得 key 和密文。
- 暂不支持第三方服务器的两步验证 / OAuth 辅助登录，只做用户名 + 密码；这类账号需要先在皮肤站关闭二次验证或改用应用密码。
- 账号列表不显示皮肤头像（B4），也不提供账号导入/导出（B7）。
- 没有 Mojang 旧版 Yggdrasil 官方登录；微软账号使用官方 OAuth 设备码流程。
- 没有 token 吊销/登出服务端会话；删除账号只清理本地凭据。

## 11. 相关测试

- `AccountModelTest`
- `OfflineAccountTest`
- `AccountServiceTest`
- `MicrosoftAuthServiceTest`
- `YggdrasilClientTest`
- `AuthServerStoreTest`
- `InstanceAccountBindingTest`