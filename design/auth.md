# CAML 第三方（Yggdrasil）账号登录

更新日期：2026-11-18

## 1. 方案结论

第三方认证服务器（LittleSkin 等）采用 **标准 Yggdrasil 协议登录 + authlib-injector 运行时注入**：

```text
用户名 + 密码
→ POST {apiRoot}/authserver/authenticate
→ accessToken + selectedProfile(id, name) + availableProfiles[]
→ （服务器未绑定角色时）POST {apiRoot}/authserver/refresh + selectedProfile 补绑定
→ AuthSession(username, uuid, accessToken, "mojang", "{}")
→ -javaagent:authlib-injector.jar={apiRoot}
```

- 认证走 HTTP，游戏端由 authlib-injector 把 `authserver`/`sessionserver` 指向皮肤站。
- **只改 JVM 参数**，不修改版本 JSON、`libraries` 或 classpath，因此与原版、Fabric、Quilt、
  Forge、NeoForge、OptiFine 和版本隔离完全解耦。
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
  AccountService.java          编排：列表、登录、重登标记、角色切换、实例/全局解析
  yggdrasil/
    AuthServerInfo.java         metadata + prefetched Base64（占位 metadata 不生成 prefetched）
    AuthServerService.java      metadata 拉取，回退顺序：网络 → 内存 → 磁盘
    YggdrasilClient.java        authenticate / refresh / validate / metadata
    YggdrasilSession.java       accessToken + 选中角色 + 全部可用角色
    YggdrasilProfile.java       角色档案（id + name）
    YggdrasilUnavailableException.java  服务器暂时不可用（5xx / 429 / 网络）
  store/
    AccountStore.java           ~/.caml/accounts/accounts.json（原子写，含角色列表）
    AuthServerStore.java        ~/.caml/accounts/servers.json（metadata 冷启动缓存）
    SecretCipher.java           AES-GCM，密钥 ~/.caml/accounts/secret.key
    StoredAccount.java          内部模型（toString 不含 token）
  injector/
    AuthlibInjector.java        jar + apiRoot + prefetched
    AuthlibInjectorService.java 版本固定、下载、SHA-256 校验、缓存、自定义覆盖
```

前端：

```text
CAMLFront/.../front/
  controller/AccountController.java   账号页控制器（全部走 AsyncTaskHandle + 后台线程）
  view/AccountSettingsView.java       设置 → 账号
  view/InstanceAccountView.java       主页二级页 → 实例账号
  view/LaunchFormView.java            主页启动表单的账号选择器
  text/AccountChoice.java             下拉框展示文本
```

## 3. authlib-injector：运行时下载

**只下载一次，之后每次启动只校验。**

1. 落地 `~/.caml/authlib-injector/<版本>/authlib-injector-<版本>.jar`，版本号目录互不覆盖。
2. 每次启动判断“文件存在 + `*.sha256` 存在 + SHA-256 一致”，命中即复用，**全程零网络**。
3. 只有三种情况重新下载：首次使用、启动器升级提升固定版本号、本地文件被改动导致校验失败。
4. 校验值优先取上游随包发布的 `.sha256` 并固化到本地；取不到时退化为下载后自行计算并记录。
5. 设置页可指定自定义 jar，完全跳过下载与校验（内网/离线场景）。
6. `FileDownloader` 通过既有 `DownloadValidator` 做 SHA-256 校验，不需要改动 `DownloadItem`。
7. 请求统一带 `User-Agent: CAML/1.0`，与项目其它 HTTP 客户端一致（部分站点的 WAF 会拒绝无 UA 请求）。

## 4. 账号归属

| 层级 | 位置 | 说明 |
|---|---|---|
| 全局账号列表 + 默认账号 | 设置 → 账号 | 用户可在任意账号上点“默认” |
| 实例账号 | 主页 → 实例账号 | 每个实例一行下拉框，可选“跟随默认账号” |
| 启动时解析 | `AccountService.resolve` | 实例账号 → 全局默认账号 → 无账号则抛 `auth.error.noAccount` |

主页启动表单另有“离线用户名”选项，保留原有的离线启动路径。

**隐藏策略（用户名输入框）**：只有选中“离线用户名”时才显示用户名输入框；选择账号
（含“跟随默认账号”）时该行隐藏，原位显示一行说明（`home.username.managed`）指出实际使用的
账号。这样界面与真实行为一致——账号模式下用户名输入框会被后端忽略。

## 5. 安全

- 密码只在本次登录使用，**不落盘、不进日志、不进异常文案**；登录后立即从控件清除。
  JavaFX 的 `PasswordField.getText()` 只能返回 `String`，无法做到真正的 `char[]` 清零，
  因此这里的安全目标是“不持久化、不外泄”，而不是“内存中不可恢复”。
- `accessToken` 使用 AES-GCM 加密后写入 `accounts.json`，密钥放在
  `~/.caml/accounts/secret.key`（POSIX 下收紧到 0600）。
- **这不是安全边界**：密钥与密文在同一台机器上，能读用户目录的程序仍可解密。
  它的目的只是避免凭据以明文形式躺在磁盘上。
- `clientToken` 首次生成后持久化，用于 `refresh` 校验。
- 日志只记录服务器地址、用户名和 HTTP 状态码；服务器返回的错误原文只写 DEBUG 日志，
  不进入界面文案。
- **已知暴露面**：accessToken 会出现在游戏进程的命令行中（Yggdrasil 协议要求），
  同一台机器上的其他本地用户可通过进程查询看到。每次启动轮换 token 可以压缩这个窗口，
  但不能消除它。

### 5.1 “不记住登录”的真实语义

勾选“记住登录”时才把 accessToken 加密落盘；**不勾选时 token 只保存在后端进程内存里**：

| 场景 | 行为 |
|---|---|
| 登录后立即启动 | 正常启动（走内存中的会话） |
| 重启启动器后再启动 | 要求重新登录，账号卡片显示“登录已失效” |

这条区分很重要：否则“不记住登录”会变成“登录等于没登录”，用户必须立刻再登一次才能启动。

## 6. 错误语义

| 场景 | 行为 |
|---|---|
| 用户名/密码错误 | `auth.error.invalidCredentials`，不降级离线 |
| 认证服务器返回未绑定角色的 token | 登录时自动用 `refresh` + `selectedProfile` 补绑定 |
| accessToken 失效但可刷新 | 自动 `refresh` 并回写新 token |
| accessToken 失效且无法刷新 | `auth.error.loginExpired`，提示重新登录 |
| `refresh` 返回 4xx | 一律按“需要重新登录”处理，不把服务器原文抛到界面 |
| 服务器 5xx / 网络失败 / 超时 | **启动时继续使用现有 token**；登录时以 `auth.error.requestFailed` 报错 |
| 请求被限流（429） | `auth.error.tooManyRequests`，提示稍后重试 |
| 认证服务器不可达（登录时） | `auth.error.requestFailed`（元数据缺失不再阻塞登录） |
| 未保存凭据（未勾选“记住登录”且已重启） | `auth.error.loginRequired` |
| 指定的角色不在账号里 | `auth.error.profileMissing` |

“服务器暂时不可用”统一用 `YggdrasilUnavailableException` 表示，与“凭据被拒绝”区分开：
启动流程遇到它可以继续使用已有 token（单人生存不依赖认证服务器，多人服务器的校验由游戏自己完成），
登录流程则把同一个对象原样展示给用户。

## 7. 角色绑定、多角色与启动时刷新

部分服务器（LittleSkin 等）在账号拥有**多个角色**时，`authenticate` 只返回 `availableProfiles`、
不设置 `selectedProfile`。此时 accessToken 处于“未绑定角色”状态，服务器会对
`validate`/`refresh` 和进入服务器返回 400：

```text
The access token has no profile assigned, please select a profile.
```

处理方式：

1. 登录：`authenticate` 之后若响应没有 `selectedProfile`，立即调用一次 `refresh` 并带上所选角色的
   `selectedProfile`，把 token 绑定到该角色（失败不影响登录，启动时会再试）。
2. 启动：直接 `refresh`（同样带 `selectedProfile`），而不是先 `validate`。这样历史上已保存的、
   未绑定角色的 token 会在下次启动时自愈；只有 `refresh` 被拒且 `validate` 也失败时才报
   `auth.error.loginExpired`。
3. **多角色切换**：登录响应里的全部角色写入 `accounts.json`，设置页账号行提供角色下拉框。
   切换走 `POST /authserver/refresh` + 目标角色的 `selectedProfile`，**账号 id 保持不变**
   （`yggdrasil:<uuid>`），只更新显示名、UUID 与角色列表，避免同一账号在列表里变成多条。

代价：每次启动都会轮换一次 accessToken（服务器会吊销旧 token），因此同一账号不宜同时
在多台机器上用本启动器登录。

## 8. metadata 与 prefetched

- `GET {apiRoot}` 是 Yggdrasil 的**可选**扩展：登录与启动都不依赖它成功。
- 登录时按“尽力而为”拉取（`fetchOrRemember`），失败就以主机名作显示名继续登录。
- 启动时把 metadata 原文 Base64 塞进 `-Dauthlibinjector.yggdrasil.prefetched`，
  游戏启动时无需再联网拉 metadata。
- 缓存分三级：网络 → 内存（进程内）→ 磁盘（`~/.caml/accounts/servers.json`）。
  磁盘缓存保证冷启动或皮肤站维护期间仍能注入 prefetched。
- 占位 metadata（`{}`）**不会**生成 prefetched：注入空 metadata 会让皮肤域名与签名公钥
  全部缺失，而且很难从日志里看出根因。
- 不注入 `-Dauthlibinjector.disableHttpd`：内置 HTTP 服务是皮肤域名���理的一部分，
  关掉它属于无明确收益的行为变更，保持与上游默认一致。

## 9. 已知限制

- 一次启动只能注入一个认证服务器（`-javaagent` 单实例），账号与服务器一一绑定。
- Yggdrasil UUID 与离线 UUID 不同，切换账号后服务器端玩家数据/白名单不会自动迁移。
- 首版只支持账号密码登录，2FA/OAuth 需要后续扩展。
- 实例账号绑定以“游戏目录 + 实例名”为键：修改“游戏位置”后旧绑定不会自动生效
  （与禁用 Mod 状态相同的既有行为）。绑定本身不会丢，主页“实例账号”页检测到这种情况时
  会列出受影响的实例，并提供“应用到当前游戏位置”一键重新套用；旧目录下的绑定保留，
  切回原目录仍然有效。

## 10. 待办

- LittleSkin 等站点的 2FA / OAuth 授权码流程。
- 使用系统凭据库（DPAPI / libsecret）替换自管理密钥。
- 账号的皮肤头像展示。
- 账号导入导出（换机器不必重新登录）。

