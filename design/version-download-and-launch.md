# CAML 版本下载、安装与启动链路

> 本文按当前下载/实例/启动代码重写，最后核对时间：2026-11-27；基线提交：`5d56e08`。

## 1. 范围

覆盖：

- 官方版本清单和版本 JSON；
- 原版文件、libraries、natives、asset index、assets objects；
- 本地版本复用、实例名和 `inheritsFrom`；
- Fabric、Quilt、Forge、NeoForge、OptiFine 安装；
- classpath、JVM/游戏参数、natives、virtual assets、Log4j；
- Java 选择、版本隔离、子进程启动、日志和退出结果。

不覆盖 Mod/光影/资源包搜索本身；相关说明见架构文档和 UI 文档。

## 2. 上游地址与下载源

### 2.1 官方地址

| 内容 | 官方地址 |
|---|---|
| 版本清单 | `https://piston-meta.mojang.com/mc/game/version_manifest_v2.json` |
| 版本 JSON/client/libraries | 由版本清单和版本 JSON 给出 |
| assets | `https://resources.download.minecraft.net/` |
| Forge | `https://maven.minecraftforge.net/net/minecraftforge/forge/` |
| NeoForge | `https://maven.neoforged.net/releases/net/neoforged/neoforge/` |
| Fabric Meta | `https://meta.fabricmc.net/` |
| Quilt Meta | `https://meta.quiltmc.org/` |
| Fabric API | Modrinth `fabric-api` |
| Quilted Fabric API/QSL | Modrinth `qsl` |
| OptiFine | `https://optifine.net/` |

### 2.2 镜像与候选顺序

`DownloadSourceMode`：

- `MIRROR`：默认 BMCLAPI 根 `https://bmclapi2.bangbang93.com`；
- `OFFICIAL`：原地址；
- `CUSTOM`：用户提供的 HTTP/HTTPS 镜像根。

镜像源把 Mojang、Forge、NeoForge、Fabric、Quilt、assets 等地址重写到镜像路径。`DownloadSources.candidates()` 始终在候选末尾再追加原始 URL，所以镜像没有命中或请求失败时仍可回退官方。

`SourceAwareHttpClient` 对每个候选执行请求；文本/二进制解析接口可以把“响应有效但解析为空”也视为失败并继续下一个候选。

## 3. 版本安装入口

`CAMLBackend.downloads()` 暴露下载 facade，内部调用 `VersionInstaller.install(...)`。
`DownloadsController` 按加载器分派：

- 原版：`downloadVersion`
- Fabric：`downloadFabricVersion`
- Quilt：`downloadQuiltVersion`
- Forge：`downloadForgeVersion`
- NeoForge：`downloadNeoForgeVersion`
- Forge + OptiFine：`downloadForgeOptiFineVersion`

前端创建不可变 `DownloadRequest`，包含官方版本、实例 ID、游戏目录和所选加载器版本。

## 4. 本地已有版本优先

`VersionInstaller.install(versionId, instanceId, ...)`：

1. 先检查 `versions/<instanceId>/<instanceId>.json`。
2. 存在则读取本地 JSON，合并父版本并补齐缺失文件，**不重写**本地 JSON。
3. 不存在时才访问版本清单，按官方 `versionId` 拉取版本 JSON。

因此 PCL/HMCL/整合包留下的自定义实例 ID 只要目录里有同名 JSON 就能直接启动；JSON 内部 `id` 与文件夹名不一致时，启动使用的 ID 统一改成文件夹名。

远程安装失败时，如果目标版本目录是本次新建的，整个目录删除，避免半成品残留。

## 5. 版本清单与版本 JSON

### 5.1 清单缓存

`VersionManifestService.fetchManifestOrCache()`：

- 依次尝试全部下载源；
- 解析并校验非空后写入 `~/.caml/cache/version_manifest_v2.json`；
- 全部失败时读取磁盘缓存；
- 没有缓存才抛出网络错误。

清单请求有 10 秒总截止时间，最多跟随 10 次重定向。

### 5.2 版本 JSON 校验

清单中的版本 JSON SHA-1 会在解析前校验；不匹配时换下一个下载源。JSON 必须能解析出 `VersionInfo`，空响应或无效响应不会覆盖缓存。

## 6. `inheritsFrom` 与元数据合并

`ManifestResolver`：

- 从最底层父版本向上解析，检测循环继承；
- 标量字段由子版本优先；
- `arguments.game` / `arguments.jvm` 合并；
- `libraries` 先子版本、后父版本；
- `downloads` / `logging` 合并；
- `jar` 缺失时继承父版本，再缺省为父版本 ID；
- 最终清除 `inheritsFrom`，并把 `id` 改为实例文件夹名。

父版本 JSON 必须已存在于本地 `versions/<parentId>/<parentId>.json`。

## 7. 基础下载计划

`DownloadPlanner.plan(resolved, gameDirectory, clientJarId)` 生成 `DownloadPlan`：

- client jar：`versions/<clientJarId>/<clientJarId>.jar`；
- libraries：按规则筛选、`clientreq=false` 跳过；
- 旧式 Forge library：根据 `name`、`url`、`checksums`、`size` 推导 Maven 路径和 URL；
- natives：按当前 OS 和位数选择 classifier，替换 `${arch}`；
- asset index：`assets/indexes/<id>.json`；
- Log4j 配置：`assets/log_configs/<id>`。

相同目标路径按 `putIfAbsent` 去重，避免父子版本重复下载。

## 8. 下载执行

### 8.1 文件下载

`FileDownloader` 只负责候选地址轮换和重试；单次尝试由 `DownloadAttempt`、
`SingleStreamDownload`、`SegmentedDownload`、`SegmentResumeState` 和 `DownloadIo` 分工完成：

- 已有且大小/SHA-1 校验通过的文件直接跳过；
- 最多 6 次尝试，逐个候选地址循环；轮次之间有递增退避；
- 已知大小 ≥ 4 MiB 时先发 `Range: bytes=0-0` 探测；
- 支持 Range 时最多 4 段并行，全局分段信号量上限 8；
- `.part` 保存未完成内容，`.part.json` 保存各段进度、总大小、SHA-1、ETag、Last-Modified；
- 每 1 MiB 或约 1 秒持久化检查点；
- `If-Range` 和响应校验避免服务器内容变化时错误续传；
- 服务器忽略/不支持 Range 时清理分片并回退单流；
- 完整大小、SHA-1、自定义 validator 全部通过后才原子移动到目标；
- 取消/失败清理 `.part` 和检查点，保留原始异常。

### 8.2 批量下载

`DownloadExecutor.download()`：

- 使用有界线程池，并发数 1–256（默认 8）；
- 对总字节数、文件完成数发送 `DownloadSummary`；
- 每文件进度发送 `DownloadProgress`；
- 安装器库缺少 `size` 时，从实际响应动态增加总字节数；
- 单文件下载也走统一执行器，保证进度页汇总覆盖 Forge/NeoForge installer、OptiFine installer、Fabric/Quilt API。

`DownloadCancellationToken` 贯穿下载、安装器进程、OptiFine/Forge 子进程和任务中心；取消后不继续向 UI 提交成功回调。

## 9. 资源对象与索引

`AssetDownloadPlanner`：

- 每个 asset hash 必须是 40 位十六进制；
- 目标路径为 `assets/objects/<前2位>/<hash>`；
- URL 为 `https://resources.download.minecraft.net/<前2位>/<hash>`；
- SHA-1 使用对象 hash，大小使用索引值。

`VirtualAssetInstaller` 在旧版 asset index 标记 `virtual=true` 时，把对象复制到 `assets/virtual/<assetId>/<资源名>`，并把 `LaunchPlan.assetsDirectory` 指向该目录；`map_to_resources=true` 时同步复制到游戏数据目录的 `resources/`。

## 10. 实例命名与目录

- 实例 ID 不能为空、不能包含路径分隔符/Windows 非法字符，拒绝已存在实例目录。
- 原版和加载器安装统一使用 `versions/<instanceId>`。
- 安装 Forge/NeoForge 时实例名不能等于游戏版本号，避免与基础版本目录冲突。
- 加载器 installer 生成自己的版本 ID 时，移动到 `versions/<instanceId>` 并把 JSON/jar 重命名为实例 ID。

## 11. 加载器安装

### 11.1 Fabric / Quilt

流程：

1. 安装基础原版。
2. 从 Meta 获取 loader profile JSON。
3. 在实例锁内把 profile 写入 staging，loader 库登记到共享库事务；校验后原子移动版本目录。
4. 若选择了 API，从 Modrinth 获取 API 文件，写入实例 `mods`，同时复制到根目录 `mods`。
5. 失败回滚 loader 库、删除目标版本目录和 staging。

Fabric 版本列表来自 `v2/versions/loader/<game>`；Quilt 来自 `v3/versions/loader/<game>`。Quilt 版本按语义化版本排序，不以 `build` 号代表新旧。

### 11.2 Forge / NeoForge

`ExternalLoaderInstaller` 流程：

1. 安装基础原版。
2. 按基础版本 Java 要求选择 installer 运行 Java。
3. 在游戏目录创建 staging，准备父版本 JSON/jar。
4. 下载 installer；优先读取 `.sha1`，没有时至少校验 ZIP 内存在 `version.json` 或 `install_profile.json`。
5. 从 installer 的 `version.json` / `install_profile.json` 收集 libraries；优先提取 installer JAR 内嵌 `maven/` 文件，其余预下载到 staging。
6. 缺失 URL 的 Forge/NeoForge 库按坐标回退到对应 Maven，而不是 Mojang 库站。
7. 安装器库首轮最多 6 次下载，失败后每 5 秒按候选源重试一轮，直到成功或取消。
8. 运行 installer headless，最多重试 2 次；每次使用全新 staging，20 分钟超时。
9. 校验生成的版本 JSON、合并 staging libraries、移动版本目录。
10. 再次用 `VersionInstaller` 补齐/验证库文件。

Forge 镜像源优先读取 BMCLAPI 的 per-game JSON；官方源读取 Maven metadata。NeoForge 支持新坐标 `net.neoforged:neoforge` 与旧坐标 `net.neoforged:forge`，并从版本号推导 Minecraft 版本。

### 11.3 OptiFine（仅 Forge）

流程：

1. 检查 Forge/OptiFine 已知兼容性。
2. 安装基础版本和 Forge；二者与 OptiFine 共用实例锁和共享库事务。
3. 下载 OptiFine installer（官方下载页或 BMCLAPI）。
4. 用独立 Java 子进程运行 `optifine.Installer.doInstall(staging)`。
5. 查找生成的 OptiFine 版本 JSON，合并它的 libraries；先写 staging，完成库校验后再原子替换 Forge JSON。
6. 对 LaunchWrapper Forge 注入 `--tweakClass optifine.OptiFineForgeTweaker`。
7. 删除 OptiFine 库中的 `META-INF/mods.toml`，避免被现代 Forge 当普通 Mod 加载。
8. 最后重新走 `VersionInstaller` 补齐文件。

已写死规则：OptiFine 1.20.1 HD U I6 与 Forge 47.4.27+ 不兼容，UI 会限制到 47.4.26 及以下。

## 12. 游戏数据目录与版本隔离

`VersionIsolationMode`：

- `FULL`：游戏数据目录始终为 `versions/<id>`；
- `NONE`：始终为 `.minecraft` 根目录。

不提供自动布局检测。无论哪种模式，`libraries` 和 `assets` 都仍来自 `.minecraft` 根目录。

## 13. Java 选择

自动模式：

- `JavaRequirement.from(installed.manifest())` 读取版本要求；
- `JavaLocator.findSuitable()` 在系统、PATH、常见目录和 `~/.caml/java` 中查找；
- 只选择主版本严格匹配的 Java；没有精确版本时前端弹出阻塞确认。
- 用户选择“强制启动”时使用主版本最接近的可用运行时。

手动模式：

- 使用 `selectedJavaPath`；
- 路径不存在或无法识别时抛出 `LaunchException`；
- 不回退自动选择。

旧版 Forge 的 `net.minecraft.launchwrapper.Launch` 需要 Java 8；CAML 的托管仓库递归扫描保证 Java 管理页安装的 Zulu 8 能被自动模式选中。

## 14. 启动前准备

1. `VersionInstaller` 保证版本完整。
2. 解析游戏数据目录。
3. 自动游戏语言：只有 `options.txt` 尚未包含 `lang:` 时才写入；拉丁文/藏文映射到默认语言，地道中国话映射简体中文。
4. NeoForge 26.2 + Vulkan：若 `options.txt` 使用 Vulkan，修改 `config/fml.toml` 的 `earlyWindowControl=false`。
5. 创建 `LaunchPlan`，可选注入 authlib-injector。
6. virtual assets 准备。
7. natives 解压到 `versions/<id>/natives`。
8. 构建 classpath 和命令。

## 15. `LaunchPlan` 与 `LaunchOptions`

`LaunchPlan` 持有：

- `InstalledVersion`；
- 合并后的 `VersionInfo`；
- `AuthSession`；
- `JavaRuntime`；
- `LaunchOptions`；
- 实际游戏数据目录；
- assets 目录（可能被 virtual assets 改写）；
- natives 目录；
- 可选 `AuthlibInjector`。

`LaunchOptions` 支持最小/最大内存、宽高、全屏、额外 JVM 参数、额外游戏参数和环境变量。默认页面只设置最大内存、额外 JVM 参数，宽高/全屏/额外游戏参数可由 API 使用。

## 16. classpath

`ClasspathBuilder`：

- 按规则筛选 libraries；
- 跳过 native-only 和 `clientreq=false`；
- 按 `group:artifact:classifier` 与目标路径去重，子版本在前；
- 普通 LaunchWrapper 版本把 client jar 放在 libraries 之后；
- BootstrapLauncher（现代 Forge/NeoForge）不追加 vanilla client jar；
- **LaunchWrapper + OptiFine 特殊顺序**：OptiFine 补丁 jar 延后到 client jar 之后。

最后一条解决 FML binpatch 的 `anb.class` 冲突：OptiFine jar 自带一个 `anb.class`，如果它排在 client jar 前面，Forge 计算 Adler-32 时会读到 OptiFine 版本并报 `binary discrepancy`。

## 17. 启动参数

### 17.1 JVM 参数

顺序：

1. `-Xmx` / `-Xms`；
2. 可选 authlib-injector `-javaagent` 与 prefetched；
3. 版本 JSON `arguments.jvm`（按规则）；
4. 客户端 Log4j `-Dlog4j.configurationFile=...`；
5. 用户额外 JVM 参数；
6. 缺少时补 `-Djava.library.path=<natives>`；
7. 缺少时补 `-cp <classpath>`。

占位符包括 `auth_*`、`version_name`、`game_directory`、`assets_root`、`assets_index_name`、`natives_directory`、`classpath`、`library_directory`、`primary_jar`、`launcher_name/version` 等。`has_custom_resolution` 由宽高是否同时存在决定。

### 17.2 游戏参数

- 新格式：`arguments.game` 按规则解析；
- 旧格式：`minecraftArguments` 按空白拆分为 token，再替换占位符；
- 追加额外游戏参数；
- 全屏时追加 `--fullscreen`。

## 18. 进程启动

`ProcessLauncher`：

- `ProcessBuilder(List<String>)`，不拼接 shell 字符串；
- 工作目录为实际游戏数据目录；
- 合并 `LaunchOptions.environmentVariables`；
- Linux 把 `~/.local/bin` 去重前插到 `PATH`；
- Windows 使用可用的 `javaw.exe` 避免控制台窗口。

命令行过长时：

1. Java 9+ 先写 `@argfile`；
2. 仍失败则生成 manifest classpath jar，把 `-cp` 指向该 jar；
3. 两种回退都失败时抛出 `launch.commandTooLongAfterFallback`。

`GameProcess` 用独立线程读取 stdout/stderr，并等待退出；与游戏无关的临时 argfile/manifest jar 在退出后清理。

## 19. 日志与退出

`MinecraftLogFormatter` 将 Log4j XML 事件转换成：

```text
[HH:mm:ss] [线程/级别]: 消息
```

异常堆栈保留后续行。前端把 stdout 原样、stderr 加上 `[ERR]` 前缀写入日志面板；非零退出码标记失败并写入 `log.gameProcess.exitError`。

## 20. Linux + LWJGL 2 的 `xrandr` 预检

LWJGL 2 的 `LinuxDisplay` 会执行外部 `xrandr -q`。系统只有 `libXrandr` 而没有 `xrandr` 命令时，显示模式列表为空并抛出 `ArrayIndexOutOfBoundsException`。

CAML 处理：

- `LaunchCompatibilityService` 沿实例继承链查找 LWJGL 2；
- 检查 `PATH` 和 `~/.local/bin` 中是否存在可执行的 `xrandr`；
- 缺失时前端显示应用内提示和发行版安装命令；
- 提示不等待、不取消启动；启动任务已先提交；
- 游戏子进程仍收到包含 `~/.local/bin` 的 `PATH`。

## 21. 已知限制

- 没有 CurseForge/HMCL/MultiMC 整合包导入、导出、更新和修复；Modrinth `.mrpack` 安装已可用。
- 下载任务可取消，但大型文件没有暂停/继续 UI；断点续传由文件层自动使用 `.part.json`。
- 游戏启动后没有前端停止按钮；后端 `GameProcess.stop()` 已存在。
- OptiFine 仅支持 Forge 组合。
- 不支持 Mojang 官方旧版登录端点；微软账号使用 OAuth 设备码。
- 没有 macOS 正式安装包和代码签名。

## 22. 相关测试

后端关键测试包括：

- `VersionManifestServiceTest`
- `ManifestResolverTest`
- `DownloadPlannerTest`
- `AssetDownloadPlannerTest`
- `FileDownloaderTest`
- `DownloadExecutorTest`
- `SharedLibraryTransactionTest` / `InstanceInstallLockTest`
- `LocalVersionInstallerTest`
- `VersionInstallerTest`
- `ExternalLoaderInstallerTest`
- `FabricInstallerTest` / `QuiltInstallerTest` / `OptiFineInstallerTest`
- `LaunchPlannerTest` / `ClasspathBuilderTest`
- `LaunchArgumentsBuilderTest` / `LaunchCommandBuilderTest`
- `ProcessLauncherTest`
- `NativeExtractorTest` / `VirtualAssetInstallerTest`
- `LaunchCompatibilityServiceTest`

前端布局/进度测试包括：

- `DownloadViewsLayoutTest`
- `DownloadTelemetryTest`
- `ProcessLauncher` 相关进度页、任务中心和焦点回归测试
