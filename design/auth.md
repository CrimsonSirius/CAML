# CAML 第三方（Yggdrasil）账号登录

更新日期：2026-11-18

## 1. 方案结论

第三方认证服务器（LittleSkin 等）采用 **标准 Yggdrasil 登录 + authlib-injector 运行时注入**：

```text
用户名 + 密码
→ POST {apiRoot}/authserver/authenticate
→ accessToken + selectedProfile(id, name)
→ （服务器未绑定角色时）POST {apiRoot}/authserver/refresh + selectedProfile 补绑定
→ AuthSession(username, uuid, accessToken, "mojang", "{}")
→ -javaagent:authlib-injector.jar={apiRoot}
```

- 认证走 HTTP，游戏端由 authlib-injector 把 `authserver`/`sessionserver` 指向皮肤站。
- **只改 JVM 参数**，不修改版本 JSON、`libraries` 或 classpath，因此与原版、Fabric、Quilt、Forge、NeoForge、OptiFine 和版本隔离完全解耦。
- 不内置改写过的 authlib，也不打包客户端 jar。

被否决的方案：

| 方案 | 原因 |
|---|---|
| 自带皮肤 Mod（CustomSkinLoader） | 只换皮肤，不能通过 `online-mode` 服务器校验 |
| 修改或替换 authlib | 版本矩阵爆炸，且违背“元数据以官方为准”的原则 |
| 登录失败静默降级离线 | 玩家会以 `legacy` 身份被 `online-mode` 服务器踢出，错误信息完全误导 |

## 2. 代码位置

```text
CAMLBackend/.../backend/auth/
  AccountType.java             offline / yggdrasil
  AccountService.java          账号编排：列表、登录、默认账号、实例解析
  yggdrasil/
    AuthServerInfo.java         metadata + prefetched Base64
    AuthServerService.java      metadata 拉取与内存缓存（失败回退上次成功值）
    YggdrasilClient.java        authenticate / refresh / validate / metadata
    YggdrasilSession.java       accessToken + selectedProfile
  store/
    AccountStore.java           ~/.caml/accounts/accounts.json（原子写）
    SecretCipher.java           AES-GCM，密钥 ~/.caml/accounts/secret.key
    StoredAccount.java          内部模型（toString 不含 token）
  injector/
    AuthlibInjector.java        jar + apiRoot + prefetched
    AuthlibInjectorService.java 版本固定、下载、SHA-256 校验、缓存、自定义覆盖
```

## 3. authlib-injector：运行时下载

**只下载一次，之后每次启动只校验。**

1. 落地 `~/.caml/authlib-injector/<版本>/authlib-injector-<版本>.jar`，版本号目录互不覆盖。
2. 每次启动判断“文件存在 + `*.sha256` 存在 + SHA-256 一致”，命中即复用，**全程零网络**。
3. 只有三种情况重新下载：首次使用、启动器升级提升固定版本号、本地文件被改动导致校验失败。
4. 校验值优先取上游随包发布的 `.sha256` 并固化到本地；取不到时退化为下载后自行计算并记录。
5. 设置页可指定自定义 jar，完全跳过下载与校验（内网/离线场景）。
6. `FileDownloader` 通过既有 `DownloadValidator` 做 SHA-256 校验，不需要改动 `DownloadItem`。

## 4. 账号归属

| 层级 | 位置 | 说明 |
|---|---|---|
| 全局账号列表 + 默认账号 | 设置 → 账号 | 用户可在任意账号上点“默认” |
| 实例账号 | 主页 → 实例账号 | 每个实例一行下拉框，可选“跟随默认账号” |
| 启动时解析 | `AccountService.resolve` | 实例账号 → 全局默认账号 → 无账号则报 `auth.error.noAccount` |

主页启动表单另有“离线用户名”选项，保留原有的离线启动路径。

## 5. 安全

- 密码只在本次登录使用，不落盘、不进日志、不进异常文案。
- `accessToken` 使用 AES-GCM 加密后写入 `accounts.json`，密钥放在 `~/.caml/accounts/secret.key`（POSIX 下收紧到 0600）。
- **这不是安全边界**：密钥与密文在同一台机器上，能读用户目录的程序仍可解密。它的目的只是避免凭据以明文形式躺在磁盘上。
- `clientToken` 首次生成后持久化，用于 `refresh` 校验。
- 日志只记录服务器地址、用户名和 HTTP 状态码。

## 6. 错误语义

| 场景 | 行为 |
|---|---|
| 用户名/密码错误 | `auth.error.invalidCredentials`，不降级离线 |
| 服务器返回未绑定角色的 token | 登录时自动用 `refresh` + `selectedProfile` 补绑定 |
| accessToken 失效但可刷新 | 自动 `refresh` 并回写新 token |
| accessToken 失效且无法刷新 | `auth.error.loginExpired`，提示重新登录 |
| `refresh` 返回 4xx | 一律按“需要重新登录”处理，不把服务器原文抛到界面 |
| 认证服务器不可达（启动时） | 记录警告，继续使用现有 token |
| 未保存凭据（未勾选“记住登录”） | `auth.error.loginRequired` |

## 7. 角色绑定与启动时刷新

部分服务器（LittleSkin 等）在账号拥有**多个角色**时，`authenticate` 只返回 `availableProfiles`，
不设置 `selectedProfile`。此时 accessToken 处于“未绑定角色”状态，服务器会对
`validate`/`refresh` 和进入服务器返回 400：

```text
The access token has no profile assigned, please select a profile.
```

处理方式：

1. 登录：`authenticate` 之后若响应没有 `selectedProfile`，立即调用一次
   `refresh` 并带上所选角色的 `selectedProfile`，把 token 绑定到该角色。
2. 启动：先把原有 `validate` 改成**直接 `refresh`**（同样带 `selectedProfile`）。
   这样历史上已保存的、未绑定角色的 token 会在下次启动时自愈；只有 `refresh`
   被拒且 `validate` 也失败时才报 `auth.error.loginExpired`。
3. 角色选择：首版固定使用账号的第一个可用角色，`StoredAccount.uuid` 即该角色 id。
   多角色切换留给后续的角色选择界面。

代价：每次启动都会轮换一次 accessToken（服务器会吊销旧 token），因此同一账号
不宜同时在多台机器上用本启动器登录。

## 8. 已知限制

- 一次启动只能注入一个认证服务器（`-javaagent` 单实例）。
- Yggdrasil UUID 与离线 UUID 不同，切换账号后服务器端玩家数据/白名单不会自动迁移。
- 首版只支持账号密码登录，2FA/OAuth 需要后续扩展。

## 9. 待办

- LittleSkin 等站点的 2FA / OAuth 授权码流程。
- 使用系统凭据库（DPAPI / libsecret）替换自管理密钥。
- 账号皮肤头像展示。