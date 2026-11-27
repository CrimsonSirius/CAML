# CAML 总体架构

> 本文按当前代码重写，最后核对时间：2026-11-27；基线提交：`5d56e08`。
> 涉及“已实现/未实现”的表述以仓库代码和测试为准，不以历史计划为准。

## 1. 目标与硬边界

CAML 是 Java 25 + JavaFX 的 Minecraft 启动器，架构目标是把协议、下载、安装、认证、Java 运行时和进程启动集中在纯 Java 后端，把界面、状态绑定和用户交互留在前端。

硬边界如下：

- `CAMLBackend` 是纯 Java，不 import JavaFX，不依赖 `CAMLFront`。
- `CAMLFront` 只负责 JavaFX 页面、交互和状态绑定，不直接读写文件、不直接发 HTTP、不直接启动进程。
- `CAMLUI` 只包含 JavaFX 控件、布局和结构 CSS，不依赖后端业务模块。
- `CAMLLanguage` 只包含语言契约、生成器工具和语言 JSON，不在运行时读取文件、检测语言、执行回退或格式化文本。
- 前后端通过 `CAMLBackend` 及其领域 facade、DTO、`CompletableFuture`、下载/进程监听器和语言事件交互。
- 游戏安装与启动不得依赖第三方 Minecraft 启动库，元数据和启动逻辑按公开协议自行实现。

## 2. 模块与依赖

```text
CAMLFront ──> CAMLBackend ──> CAMLLanguage
    │              │
    └──────────────┴──────> CAMLUI
```

| 模块 | 职责 | 主要依赖 |
|---|---|---|
| `CAMLLanguage` | 语言标识、语言文本契约、生成期翻译工具、11 套内置 JSON | Gson |
| `CAMLBackend` | 组合服务、生命周期、状态；按领域暴露 facade | CAMLLanguage、Gson、Log4j API、TwelveMonkeys WebP |
| `CAMLUI` | JavaFX 控件库、页面宿主、缩放工具、结构样式 | JavaFX Controls |
| `CAMLFront` | JavaFX 应用入口、页面、控制器、异步任务、进程日志展示 | CAMLBackend、CAMLUI、CAMLLanguage、JavaFX |

根工程统一使用 Java toolchain 25、UTF-8 编译、JUnit Platform；构建版本为 `1.0-SNAPSHOT`。

## 3. 后端包结构

| 包 | 职责 |
|---|---|
| `api` | `CAMLBackend` 组合根、六个领域 facade、`BackendStatus` |
| `auth` | 离线、微软、Yggdrasil、authlib-injector、账号存储和实例账号解析 |
| `config` | `BackendConfig`、默认数据/游戏目录和启动目录创建 |
| `download` | 下载模型、监听器、重试入口、单流/分片下载、续传状态、并发执行和下载源 |
| `download.plan` | 版本文件与资源对象下载计划 |
| `error` | 稳定错误码 `BackendError` 和结构化异常 |
| `instance` | 实例名规则、版本安装、继承解析、FULL/NONE 隔离、实例检查、图标/元数据 |
| `io` | 路径越界防护 `SafePaths` |
| `json` | Gson 文件读写 |
| `language` | 语言初始化、发现、目录加载、回退、格式化与事件发布 |
| `launch` | 启动计划、classpath、参数、natives、进程、兼容修复与预检 |
| `loader` | Fabric/Quilt/Forge/NeoForge/OptiFine 版本查询和安装 |
| `log` | UI 日志 `latest.log`、失败归档 |
| `metadata` | 版本清单、版本 JSON、规则、继承合并 |
| `mod` | Modrinth/CurseForge 搜索、详情、依赖展示、下载和图标 |
| `runtime` | 游戏 Java 检测、要求匹配、托管仓库和 Foojay 下载 |
| `settings` | `settings.properties` 持久化 |
| `system` | 系统/设备/内存/依赖信息和目录打开 |
| `theme` | 外部 CSS 主题扫描、默认主题复制和选择持久化 |

## 4. 后端生命周期

`CAMLApplication.init()` 创建 `CAMLBackend(BackendConfig.defaults())` 并调用 `start()`。

`CAMLBackend.start()` 的固定顺序：

1. `BackendPaths.ensure()` 创建数据目录、`logs`、`cache`、`themes`、`java`、游戏目录和 `versions`。
2. `SingleInstanceLock.tryAcquire()` 获取启动器单实例锁。
3. `BackendSettings.ensureThemes()` 复制缺失的内置主题；旧版 `mango.css` 自动迁移为 `coffee.css`，不覆盖已有文件。
4. `LatestLogService.start()` 截断并打开 `logs/latest.log`。
5. `BackendSettings.initializeLanguage()` 同步内置语言、读取语言设置并建立语言快照。
6. 读取自动游戏语言。
7. `BackendDownloads.initialize()` 读取下载并发、解压限制和下载源，并应用到安装器与 HTTP 客户端。
8. `BackendJava.initialize()` 应用 Java 选择；`BackendAccounts.initialize()` 应用 authlib-injector 覆盖路径。
9. 标记 backend ready。

`CAMLBackend` 只保留组合和生命周期，领域调用通过 `settings()`、`java()`、
`accounts()`、`downloads()`、`instances()`、`launch()` 进入对应 facade。

`close()` 关闭日志写入器；前端 `CAMLApplication.stop()` 还会先提交窗口/设置、释放页面、关闭控制器线程池。

## 5. 数据目录

默认 `BackendConfig`：

- 数据目录：`~/.caml`
- 游戏目录：`~/.minecraft`
- 两者在 Windows/Linux 都以用户主目录为基准，避免安装目录只读问题。

| 路径 | 内容 |
|---|---|
| `~/.caml/settings.properties` | 游戏位置、隔离、内存、JVM 参数、下载、Java、语言、窗口、账号绑定、禁用 Mod 等；损坏文件会改名为 `settings.properties.corrupt-*` 后从默认值恢复 |
| `~/.caml/theme.txt` | 当前主题 ID |
| `~/.caml/themes/*.css` | 外部主题；内置 `coffee.css` / `night.css` 首次启动复制 |
| `~/.caml/lang/*.json` | 运行时语言文件；内置语言随启动器版本同步 |
| `~/.caml/logs/latest.log` | 本次 UI 日志；有失败时退出额外生成 `.log.gz` 归档 |
| `~/.caml/cache/version_manifest_v2.json` | 版本清单缓存 |
| `~/.caml/cache/mod-icons/` | Mod/光影/资源包图标缓存（WebP 转 PNG） |
| `~/.caml/java/<platform>/<name>/` | CAML 托管 Java |
| `~/.caml/accounts/accounts.json` | 账号索引，token 字段加密 |
| `~/.caml/accounts/secret.key` | 本机账号加密密钥 |
| `~/.caml/accounts/servers.json` | Yggdrasil metadata 磁盘缓存 |
| `~/.caml/authlib-injector/1.2.5/` | authlib-injector jar 与 SHA-256 记录 |
| `~/.minecraft/versions/<id>/` | 版本 JSON、客户端 jar、natives、实例数据 |
| `~/.minecraft/libraries/` | 全局库 |
| `~/.minecraft/assets/` | 资源索引、objects、log_configs、虚拟资源 |
| `~/.minecraft/mods|resourcepacks|shaderpacks/` | 非隔离实例内容目录 |

版本隔离只改变“游戏数据目录”，`libraries` 与 `assets` 始终位于 `.minecraft` 根目录。

## 6. 核心链路

### 6.1 元数据

- 版本清单默认地址：`https://piston-meta.mojang.com/mc/game/version_manifest_v2.json`。
- 版本 JSON 下载后按清单中的 SHA-1 校验，再解析为 `VersionInfo`。
- 清单网络失败时回退 `version_manifest_v2.json` 磁盘缓存。
- `ManifestResolver` 负责 `inheritsFrom`：标量由子版本覆盖，game/JVM 参数合并，libraries 子版本优先，`jar` 默认继承。
- 规则由 `RuleEvaluator` 按顺序匹配，最后一个匹配规则决定 allow/deny；无规则默认允许。

### 6.2 下载

- 下载源模式：镜像、官方、自定义。镜像默认 BMCLAPI，最终仍追加原始官方 URL 作为兜底。
- `FileDownloader` 只保留候选源轮换和重试；`DownloadAttempt` 选择单流/分片，`SingleStreamDownload`、`SegmentedDownload` 和 `SegmentResumeState` 分别处理普通下载、分片与续传检查点。
- 已知大小且不少于 4 MiB 的文件先探测 HTTP Range；支持时最多 4 段并行，全局最多 8 个分段槽位。
- 分片下载使用 `.part` 和 `.part.json` 检查点，校验 ETag/Last-Modified，支持断点续传；Range 失效时清理分片并回退单流。
- 所有路径使用临时文件，大小与哈希/自定义校验通过后原子移动到目标；失败或取消清理临时文件。
- `DownloadExecutor` 使用 1–256 的并发数聚合多项进度；安装器依赖缺少声明大小时，从实际响应动态补齐总字节数。

### 6.3 实例与加载器

- 本地已有 `versions/<id>/<id>.json` 时优先使用本地版本，不重写 JSON。
- 远程安装使用官方版本 ID 拉元数据，可用独立实例 ID 命名目录；新实例先写版本 staging，client jar 一并暂存，成功后才原子发布。
- 基础版本安装补齐 client、libraries、natives、asset index、assets objects 和 Logging 配置。
- Fabric/Quilt 通过 Meta profile 安装：loader 库登记到共享库事务，版本目录先写 staging，校验成功后原子移动。
- Forge/NeoForge 下载官方 installer，优先提取 installer 内嵌 `maven/` 库，其余依赖使用共享库事务；installer 子进程失败时使用全新 staging 重试。
- Forge/NeoForge 安装器库首轮沿用 6 次下载，之后每 5 秒重试一轮，直到成功或取消；重试轮不会重复累计进度总字节。
- OptiFine 仅在 Forge 组合中启用：先装 Forge，再运行 OptiFine 官方 installer，合并库、tweak 参数并移除会误加载的 `META-INF/mods.toml`。
- 已知兼容冲突：OptiFine 1.20.1 HD U I6 不允许 Forge 47.4.27 及以上。

### 6.4 启动

`GameLaunchService.launch()` 的顺序：

1. `VersionInstaller.install()` 确保版本文件完整。
2. 按自动或手动模式选择 Java；自动模式读取版本 `javaVersion.majorVersion`，缺省为 8。
3. 按 `VersionIsolationMode` 解析游戏数据目录。
4. 可选写入首次游戏语言；应用 NeoForge 26.2 + Vulkan 修复。
5. 创建不可变 `LaunchPlan`，可选注入 authlib-injector。
6. 为旧资源索引准备 virtual assets，解压 natives。
7. 构建 classpath、JVM 参数和游戏参数，启动子进程。
8. 后台线程读取 stdout/stderr，转换 Log4j XML 为 `[HH:mm:ss] [线程/级别]: 消息`，退出时回调退出码。

Linux + LWJGL 2 启动前会异步检查 `xrandr`；缺失时前端显示非阻塞提示，但启动任务已经提交，不改变启动结果。

### 6.5 账号

- 离线账号：稳定离线 UUID，仅本地。
- 微软账号：OAuth 2.0 设备码 → Xbox Live → XSTS → Minecraft Services → entitlements/profile；accessToken 与 refreshToken 加密保存，启动前按过期时间刷新。
- Yggdrasil：metadata、authenticate、refresh、validate；支持多角色切换，启动刷新时把服务器最新角色名与角色列表写回账号。网络/5xx/限流按“暂不可用”分类，可继续使用已有 token。
- 实例账号优先，其次全局默认账号；未配置账号时主页可退回输入的离线用户名；游戏位置变化后可检测并套用旧位置的实例绑定。
- 第三方账号通过 authlib-injector `-javaagent` 注入，不修改版本 JSON、libraries 或 classpath。
- 尚未提供第三方服务器的两步验证/OAuth 辅助登录、账号头像和账号导入/导出。

### 6.6 内容下载

- `ModService` 聚合 Modrinth 与 CurseForge，支持 MOD、SHADER、RESOURCE_PACK、WORLD 四种内容类型；地图仅使用 CurseForge。
- Modrinth `.mrpack` 整合包由独立安装器处理：先在隐藏 staging 实例安装依赖加载器、下载清单文件、解压 overrides 并写入整合包标记，改写版本 JSON/jar 后原子发布最终目录。
- Modrinth 使用官方 v2 API；CurseForge 使用官方 v1 API，要求 API Key。
- 搜索结果按 50 条上限分页；双来源模式按下载量合并。
- 文件下载支持 SHA-1，CURSEFORGE 文件缺少 SHA-1 时可回退 MD5。
- 图标由后端下载并缓存；Modrinth WebP 在后端转 PNG 后交给 JavaFX。
- 地图下载 ZIP 后由后端安全解压为实例数据目录下的 `saves/<世界名>`，已有同名世界不覆盖。
- 管理实例页可检查本地 Mod 更新：Modrinth 使用哈希匹配，CurseForge 使用 MurmurHash2 指纹匹配；更新在实例锁内 staging，备份旧文件后原子替换，设置失败时恢复。

### 6.7 设置、主题、语言和日志

- 设置写入由写锁串行化，使用临时文件 + 原子替换；损坏的 properties 会隔离后按默认值恢复。
- `theme.txt` 和首次主题文件复制使用临时文件 + 原子移动。
- 主题来自 `<data>/themes/*.css`，后端只持久化 ID，前端负责加载 CSS。
- 内置语言为 `zh_cn`、`zh_tw`、`en_us`、`fn_cn`、`ja_jp`、`ko_kr`、`fr_fr`、`de_de`、`ru_ru`、`la_la`、`bo_cn`；语言文件由生成器生成，前后端不手改 JSON。
- `LatestLogService` 异步写入 UI 日志；检测到异常/失败关键字或显式标记失败时，退出后额外压缩归档。

## 7. 前端架构

`CAMLApplication` 负责后端初始化、主题/语言加载、窗口几何、场景根切换和关闭清理。`MainView` 是应用壳层：

- 顶栏主导航：主页 / 下载 / 任务 / 设置。
- 页面内容交给 `CamlPageHost`，支持页面与侧栏的淡入/位移过渡。
- 全局 `StackPane` 遮罩层承载 Java 下载、xrandr 提示等应用内弹窗。
- 底栏显示版本、后端就绪状态和数据目录。

页面控制器通过 `ControllerContext` 访问后端。控制器只处理参数、任务派发和 UI 回调，不直接触碰磁盘/网络/进程。

`CAMLApplication.rebuildView()` 用于语言切换后重建整个场景；重建前保存页面状态，重建后恢复设置页子项和下载分类，并处理程序化焦点落点。

## 8. 并发与线程模型

后端所有阻塞操作由前端线程池移出 JavaFX Application Thread：

| 线程池 | 线程/并发 | 用途 |
|---|---:|---|
| `caml-download-worker` | 1 | 普通下载、目录检查等长任务 |
| `caml-settings-worker` | 1 | 设置写入 |
| `caml-check-worker` | 1 | 实例名、目录、启动环境检查 |
| `caml-metadata-worker` | 6 | 版本/加载器/Mod 元数据 |
| `caml-mod-icon-worker` | 8 | 图标下载 |
| `caml-mod-icon-retry` | 4 | 图标延迟重试 |
| `caml-java-worker` | 1 | Java 扫描、添加、下载 |
| `caml-launch-worker` | 1 | 游戏安装/启动 |

UI 更新统一切回 `Platform.runLater`。下载详情页每 200ms 轮询遥测，任务中心列表每 250ms 刷新，设置页内存监视每 500ms 读取一次。

## 9. 错误与安全

- 后端公开错误使用 `BackendError(code, args)`；前端通过 `LanguageTextProvider` 本地化，不显示服务器原文或 token。
- 路径写入统一经过 `SafePaths`，拒绝越界；下载/解压也检查路径。
- accessToken/refreshToken 使用 AES-GCM 加密后落盘；key 位于 `~/.caml/accounts/secret.key`，POSIX 下尽量收紧权限。
- 密码不落盘、不写日志；登录后由表单清除。
- 不打包 Minecraft 客户端 jar、assets、libraries 或游戏资源。

## 10. 测试与当前限制

当前测试共 125 个测试类、508 个测试方法，覆盖后端、前端和语言一致性；最近一次全量结果为 0 failure / 0 error / 0 skipped。

当前明确未提供：

- 整合包导入/导出、更新/修复；Modrinth `.mrpack` 安装已经可用。
- Mod 依赖的递归下载；详情页只展示依赖信息。
- 游戏进程停止按钮；后端 `GameProcess.stop()` 已有，前端未接入。
- 启动器自更新、代码签名和 macOS 正式包。
- CurseForge 无内置可用 Key 时的自动获取；需要构建环境或系统属性提供 Key。
- Microsoft OAuth Client ID 未注入时无法登录微软账号。

## 11. 相关文档

- [version-download-and-launch.md](version-download-and-launch.md)
- [auth.md](auth.md)
- [runtime.md](runtime.md)
- [ui.md](ui.md)
- [Implement.md](Implement.md)
- [package.md](package.md)
