# CAML 版本下载与启动链路

更新日期：2026-11-07

## 1. 文档范围

本文只说明：

1. `Version` 如何从远程元数据解析、规划并下载到本地。
2. 当前启动链路已经实现到哪一步，以及后续启动流程还缺什么。

总体架构、模块职责和其他模块的能力见 [architecture.md](architecture.md)。

当前状态：

- 版本下载链路已经可用。
- 启动命令构建和基础进程启动已经实现。
- JavaFX 界面已经可以选目录、选版本、启动和查看日志。
- 启动错误对话框、停止按钮、异常退出处理和真实客户端端到端验证还没有完成。

## 2. Version 下载链路

### 2.1 调用入口

```java
InstalledVersion installed = backend.installVersion("1.21.5", DownloadListener.NONE);
```

调用链：

```text
CAMLBackend.installVersion(...)
→ VersionInstaller.install(...)
→ VersionManifestService.fetchManifestOrCache(...)
→ VersionInstaller.resolve(...)
→ DownloadPlanner.plan(...)
→ FileDownloader.download(...)
→ MetadataService.readAssetIndex(...)
→ AssetDownloadPlanner.plan(...)
→ FileDownloader.download(...)
→ 原子写入 versions/<id>/<id>.json
→ 返回 InstalledVersion
```

### 2.2 本地已有版本优先

`VersionInstaller` 启动前先检查：

```text
<游戏目录>/versions/<id>/<id>.json
```

只要该文件存在，就直接使用**本地版本 JSON**，不会访问官方版本清单。
因此版本 ID 就是 `versions/` 下的文件夹名，可以是第三方启动器或整合包
自定义的名称（例如 `1.21.4-DeepSeek`），不要求出现在官方版本清单中。

本地版本分支的调用链：

```text
VersionInstaller.install(...)
→ 读取 versions/<id>/<id>.json
→ 合并 inheritsFrom 父版本
→ DownloadPlanner.plan(...)
→ 只补齐缺失文件
→ 返回 InstalledVersion
```

规则：

- 本地已安装时不会重写版本 JSON，保持与外部启动器一致。

下载原版时还支持单独实例名：官方版本 ID 用于查询清单和拉取版本 JSON，
实例 ID 用于 `versions/<id>` 目录、客户端 jar 和当前实例显示。实例名默认官方版本号。
- 合并 `inheritsFrom` 时按**文件夹名**查找父版本，兼容 JSON 内部 `id` 与文件夹名不一致的情况。
- 解析完成后把 manifest 的 `id` 统一为文件夹名，保证 `versions/<id>/<id>.jar` 等路径一致。
- 本地版本缺库、缺资源或缺客户端 jar 时仍会自动补齐。

只有本地不存在该版本时，才进入下面的官方清单流程。

### 2.3 获取版本清单

`VersionManifestService.fetchManifestOrCache(...)` 默认访问：

```text
https://piston-meta.mojang.com/mc/game/version_manifest_v2.json
```

默认缓存位置：

```text
~/.caml/cache/version_manifest_v2.json
```

行为：

- 优先从网络获取。
- 获取成功后原子写入缓存。
- 网络失败时，如果缓存存在，则读取缓存。

### 2.4 选择版本并拉取版本 JSON

`VersionInstaller` 在清单的 `versions` 列表中按 `id` 查找目标版本。

找到后调用：

```java
manifestService.fetchVersionJson(remoteVersion)
```

拿到原始版本 JSON，再用：

```java
metadata.parseVersionInfo(versionJson)
```

转换为 `VersionInfo`。

原始 JSON 会被保留，安装完成后原样写入：

```text
versions/<id>/<id>.json
```

这样版本目录保持标准 Minecraft 布局，方便后续启动和外部工具读取。

### 2.5 处理 inheritsFrom

`VersionInstaller.resolve(...)` 检查 `VersionInfo.inheritsFrom()`。

当前支持：

- 原版版本：没有 `inheritsFrom`，直接使用。
- 已安装的父版本：从本地读取：

```text
versions/<parentId>/<parentId>.json
```

- 父版本不存在：抛出 `IOException`。
- 循环继承：抛出 `IllegalArgumentException`。

合并由 `ManifestResolver` 负责：

- 标量字段由子版本覆盖父版本；`jar` 未指定时继承父版本。
- `arguments.game` 和 `arguments.jvm` 合并。
- `libraries` 合并为“子版本库在前、父版本库在后”，classpath 按路径去重时子库优先。
- `downloads` 和 `logging` 合并。
- 清除 `inheritsFrom`，得到可下载的解析后 manifest。

加载器版本的 `inheritsFrom` 依赖本地已经存在的父版本。Fabric 安装会先确保原版父版本存在。

Fabric 安装流程：

- 从 `https://meta.fabricmc.net/v2/versions/loader/<game>` 获取 Loader 版本。
- 获取 `<game>/<loader>/profile/json`，写入 `versions/<instance>/<instance>.json`。
- profile 没有 `jar` 时复用父版本客户端 jar。
- Fabric API 从 Modrinth `fabric-api` 项目获取，并写入实际游戏目录的 `mods`。

Forge/NeoForge 安装流程：

- 从 Forge/NeoForge Maven `maven-metadata.xml` 筛选当前 Minecraft 版本的 installer。
- 先确保原版父版本存在，再创建 staging 目录并复制父版本 JSON 和客户端 jar。
- 运行官方 installer 的 headless `--installClient <staging>`，由官方 processor 生成运行时库和版本文件。
- 把 staging libraries 合并回共享 `libraries`，再按用户实例名移动版本目录。
- 最后用 `VersionInstaller` 读取安装结果，解析 `inheritsFrom` 并校验启动所需文件。

### 2.6 生成基础下载计划

`DownloadPlanner.plan(resolved, gameDirectory)` 生成基础文件计划，包含：

- 客户端 jar：`versions/<id>/<id>.jar`。
- libraries：`libraries/<Maven 路径>`。
- 旧式 Forge library 没有 `downloads` 时，从 `name` 推导 Maven 路径，从 `url` 补下载地址，并使用 `checksums` 第一个 SHA-1。
- 当前系统的 native classifier。
- asset index：`assets/indexes/<assetIndexId>.json`。

规则处理：

- 使用 `RuleEvaluator` 过滤 library 的 `rules`。
- `clientreq=false` 的 library 在客户端下载和 classpath 中跳过。
- 最后一个匹配规则的 action 生效。
- native classifier 中的 `${arch}` 替换为当前位数。
- 相同目标路径会去重。

### 2.7 执行基础下载

`VersionInstaller.downloadPlan(...)` 遍历：

```java
plan.missing()
```

只下载不存在或 SHA-1 不匹配的文件。

每个文件由 `FileDownloader` 处理：

```text
检查已有文件
→ 创建目标父目录
→ 尝试候选 URL
→ 下载到目标目录下的 .part 临时文件
→ 计算 SHA-1
→ 校验通过后原子移动
→ 失败重试
```

默认最多重试 3 轮。当前候选 URL 只有官方源。

### 2.8 下载资源对象

asset index 下载完成后，`VersionInstaller` 读取：

```text
assets/indexes/<assetIndexId>.json
```

解析为 `AssetIndexFile`，再交给 `AssetDownloadPlanner`。

资源对象路径：

```text
assets/objects/<hash 前两位>/<hash>
```

默认资源对象地址：

```text
https://resources.download.minecraft.net/<hash 前两位>/<hash>
```

资源对象同样经过 `FileDownloader`，即临时文件、SHA-1 和原子移动。

### 2.9 写入版本 JSON

所有基础文件和资源对象完成后，`VersionInstaller` 才把原始版本 JSON 原子写入：

```text
versions/<id>/<id>.json
```

这样可以避免下载失败时留下一个看起来已经安装完成的版本目录。

### 2.10 返回结果

`InstalledVersion` 包含：

```text
id
manifest
versionDirectory
versionJson
clientJar
librariesDirectory
assetsDirectory
assetIndexFile
```

后续启动流程应直接使用这个结果，而不是重新猜测路径。

### 2.11 下载目录布局

默认配置：

```text
~/.caml/                             启动器数据目录
└─ cache/
   └─ version_manifest_v2.json       版本清单缓存

游戏目录：
  Linux/macOS: ~/.minecraft
  Windows:     <启动器所在目录>/.minecraft

<游戏目录>/
├─ versions/
│  └─ <id>/
│     ├─ <id>.json                   原始版本 JSON
│     └─ <id>.jar                    客户端 jar
├─ libraries/                        共享库
└─ assets/
   ├─ indexes/
   │  └─ <assetIndexId>.json         资源索引
   └─ objects/
      └─ <hash 前两位>/<hash>         资源对象
```

### 2.12 下载链路关键类

| 类 | 职责 |
|---|---|
| `VersionInstaller` | 串起版本选择、解析、下载和版本 JSON 落盘 |
| `VersionManifestService` | 拉取版本清单和单版本 JSON，处理缓存回退 |
| `MetadataService` | 解析版本 JSON、版本清单和 asset index |
| `ManifestResolver` | 合并 `inheritsFrom` 父版本 |
| `DownloadPlanner` | 生成客户端、libraries、natives、asset index 下载计划 |
| `AssetDownloadPlanner` | 生成 assets objects 下载计划 |
| `FileDownloader` | HTTP 下载、重试、SHA-1、临时文件和原子移动 |
| `DownloadItem` | 单个下载任务 |
| `DownloadPlan` | 一组下载任务，并提供缺失文件过滤 |
| `InstalledVersion` | 安装结果和启动所需的基础路径 |

### 2.13 当前下载限制

- 支持原版、Fabric Loader/Fabric API、Forge 和 NeoForge 安装；Quilt 尚未实现。
- `inheritsFrom` 只读取本地父版本，不会自动安装父版本。
- 只有官方下载源，镜像源接口尚未接入配置。
- 下载使用有界线程池并发执行，并发数可配置为 1–256；下载取消尚未实现。
- 没有内容寻址缓存，只有目标文件存在且 SHA-1 匹配时跳过。

### 2.14 metadata / download 包结构

```text
metadata/
├─ MetadataService.java         JSON 解析入口
├─ VersionManifestService.java  版本清单/单版本 JSON 获取与缓存
├─ model/                       版本、库、资源和参数 DTO
├─ rule/                        规则模型和系统匹配
├─ parse/                       自定义 Gson 解析
└─ resolve/                     inheritsFrom 合并

download/
├─ FileDownloader.java          文件下载入口
├─ DownloadListener.java        进度回调
├─ DownloadConcurrency.java     并发数约束
├─ model/                       DownloadItem、DownloadPlan
├─ plan/                        版本和资源下载计划
├─ source/                      下载源和候选地址
├─ io/                          并发任务执行
└─ hash/                        SHA-1 计算与校验
```

## 3. 启动链路

### 3.1 当前状态

底层已经可以启动 Minecraft 进程，前端也已经接入启动按钮和日志窗口。以下内容尚未完成：

- 启动错误对话框。
- CAMLFront 停止游戏按钮。
- 异常退出分类和崩溃分析。
- 启动器隐藏/保留/关闭策略。
- 真实 Minecraft 客户端端到端验证。

### 3.2 已有启动前置数据

启动流程需要三类数据：

```text
InstalledVersion  版本目录、manifest、客户端 jar、libraries、assets
AuthSession       用户名、UUID、accessToken、userType、userProperties
JavaRuntime       实际 Java 可执行文件、版本、架构、位数
```

这些类型已经存在，并由 `LaunchPlanner` 组合成不可变的 `LaunchPlan`。

### 3.3 当前启动流程

```text
读取 InstalledVersion
→ 读取 AuthSession
→ 选择 JavaRuntime
→ 读取用户选择的版本隔离模式
→ 创建 LaunchPlan（已实现）
→ 构建 classpath（已实现）
→ 构建 JVM 参数（已实现）
→ 构建游戏参数（已实现）
→ 替换占位符（已实现）
→ 准备 natives 和日志配置（已实现）
→ ProcessBuilder 启动（已实现）
→ 监控 stdout/stderr 和退出码（已实现）
```

### 3.4 LaunchPlan

`LaunchPlan` 是启动阶段的核心输入对象。它把“安装结果、账号、Java、目录和用户选项”固定在一个不可变对象里，后续 classpath、参数和进程构建都从这里读取。

字段如下：

| 字段 | 类型 | 含义 |
|---|---|---|
| `instance` | `InstalledVersion` | 已安装版本及其文件路径 |
| `manifest` | `VersionInfo` | 解析后的启动 manifest |
| `auth` | `AuthSession` | 用户名、UUID、token、userType、userProperties |
| `java` | `JavaRuntime` | 实际使用的游戏 Java |
| `options` | `LaunchOptions` | 内存、分辨率、自定义参数和环境变量 |
| `gameDirectory` | `Path` | 实际 `--gameDir`：版本隔离时为 `versions/<id>`，否则为 `.minecraft` 根目录 |
| `assetsDirectory` | `Path` | assets 目录 |
| `nativesDirectory` | `Path` | natives 解压目录 |

`gameDirectory` 是实际传给 `--gameDir` 的目录：完全版本隔离时为 `versions/<id>`，
不隔离时为 `.minecraft` 根目录。`assetsDirectory` 和 `libraries` 始终来自根目录。
构造时会校验所有字段非空。

### 3.5 LaunchOptions

`LaunchOptions` 也是不可变 record，包含：

| 字段 | 含义 | 约束 |
|---|---|---|
| `minMemoryMb` | 最小内存 | 可为空；不能为负数 |
| `maxMemoryMb` | 最大内存 | 可为空；不能为负数且不能小于最小内存 |
| `width` / `height` | 游戏窗口分辨率 | 可为空；如果存在必须为正数 |
| `fullscreen` | 是否全屏 | 布尔值 |
| `jvmArguments` | 用户 JVM 参数 | 复制为不可变列表 |
| `gameArguments` | 用户游戏参数 | 复制为不可变列表 |
| `environmentVariables` | 启动环境变量 | 复制为不可变 Map |

`LaunchOptions.defaults()` 返回一个所有可选数值为空、列表和 Map 为空的默认选项。

### 3.6 LaunchPlanner 创建流程

```text
LaunchPlanner.create(instance, auth, java, options)
→ 校验参数非空
→ JavaRequirement.from(instance.manifest())
→ requirement.isSatisfiedBy(java)
→ 不满足则抛出 IllegalArgumentException
→ 构造 LaunchPlan
→ nativesDirectory = versions/<id>/natives
```

`LaunchPlanner` 还提供：

```java
create(instance, auth, java, options, gameDataDirectory)
```

用于显式指定游戏数据目录。`GameLaunchService` 根据 `VersionIsolationMode` 调用
`GameDataLayout.resolve(...)`：`FULL` 选择 `versions/<id>`，`NONE` 选择根目录，
旧调用的 `AUTO` 才根据目录内容自动判断。

当前 `LaunchPlan` 只负责组织启动所需数据，不负责：

- 生成 JVM/游戏参数。
- 替换占位符。
- 解压 natives。
- 启动进程。

classpath 由 `ClasspathBuilder` 单独生成。Java 检测和匹配细节见 [runtime.md](runtime.md)。

### 3.7 ClasspathBuilder

`ClasspathBuilder.build(LaunchPlan)` 生成 `List<Path>`：

```text
遍历 resolved manifest 的 libraries
→ 按 RuleEvaluator 过滤 rules
→ 排除 native-only library
→ 读取 downloads.artifact，缺失时从 name 推导 Maven 路径
→ 解析 libraries/<path> 路径
→ 去重并保持顺序
→ 最后追加客户端 jar
```

规则：

- 新式 library 使用 `downloads.artifact.path`；旧式 Forge library 从 `name` 推导 Maven 路径。
- `clientreq=false` 的库不加入 classpath。
- classifier 为 `natives-*` 的 native-only 库不加入 classpath。
- 有 native classifier 的普通库仍保留主 artifact。
- 先按 `group:artifact:classifier` 去重，保留先出现的子版本库；再按目标路径去重。
- 客户端 jar 固定放在最后。
- 客户端 jar 路径来自 manifest 的 `jar` 字段，未指定时回退到实例 ID。
- `join(List<Path>)` 使用 `File.pathSeparator` 拼接。

对外入口：

```java
List<Path> classpath = backend.classpathBuilder().build(plan);
String joined = backend.classpathBuilder().join(classpath);
```

### 3.8 启动参数构建

`JvmArgumentsBuilder` 负责：

- 添加 `-Xms` / `-Xmx`。
- 解析版本 JSON 的 `arguments.jvm`。
- 应用 `rules` 和 `features`。
- 追加用户 JVM 参数。
- 缺少时补充 `-Djava.library.path`。
- 缺少时补充 `-cp` 和 classpath。
- 过滤空字符串。

`GameArgumentsBuilder` 负责：

- 解析版本 JSON 的 `arguments.game`。
- 支持旧版 `minecraftArguments`。
- 替换占位符。
- 追加用户游戏参数。
- 添加 `--fullscreen`。
- 过滤空字符串。

`PlaceholderResolver` 和 `LaunchPlaceholders` 负责：

- 玩家名、UUID、accessToken、旧版 `auth_session`。
- 版本名、版本类型、游戏目录。
- assets 目录、asset index、natives 目录。
- classpath、libraries 目录、`jar` 指定的客户端 jar。
- 启动器名称和版本。
- 分辨率。

对外入口：

```java
List<String> jvmArgs = backend.jvmArgumentsBuilder().build(plan, classpath);
List<String> gameArgs = backend.gameArgumentsBuilder().build(plan, classpath);
```

### 3.9 natives 与 Log4j 配置

`NativeExtractor.extract(plan)`：

- 清理 `versions/<id>/natives`。
- 若版本 JSON 指定 `-Djava.library.path=${natives_directory}/java`，解压到
  `versions/<id>/natives/java`；26.3 的原生库必须位于该目录。
- 按当前 OS 和位数选择 native jar。
- 支持旧式 `natives` classifier 和新式 `natives-*` library。
- 跳过目录、`META-INF/` 和 `extract.exclude`。
- 防止 zip slip 路径逃逸。

Log4j 配置由两部分组成：

- `DownloadPlanner` 下载 `logging.client.file` 到 `assets/log_configs/<id>`。
- `JvmArgumentsBuilder` 注入 `logging.client.argument`，并把 `${path}` 替换为实际文件路径。

对外入口：

```java
backend.nativeExtractor().extract(plan);
```

旧版本 virtual assets 由 `VirtualAssetInstaller.prepare(plan)` 处理：

- 读取 asset index 的 `virtual` 和 `map_to_resources`。
- 把 objects 复制到 `assets/virtual/<assetIndexId>/<逻辑路径>`。
- `map_to_resources` 时同时复制到 `gameDirectory/resources`。
- 返回一个 `assetsDirectory` 已替换为 virtual 目录的 `LaunchPlan`。

```java
LaunchPlan prepared = backend.virtualAssetInstaller().prepare(plan);
```

### 3.10 ProcessLauncher

`LaunchCommandBuilder` 组合最终参数：

```text
java 可执行文件
→ JVM 参数
→ mainClass
→ 游戏参数
```

`ProcessLauncher.launch(plan, classpath, listener)`：

- 使用 `ProcessBuilder`。
- 工作目录设置为游戏目录。
- 合并 `LaunchOptions.environmentVariables()`。
- 仅在 Windows 设置 `APPDATA`，并以 `.minecraft` 根目录为基准计算；
  版本隔离时不会误用 `versions/<id>` 的父目录。
- 启动后返回 `GameProcess`。

`GameProcess`：

- 独立线程读取 stdout 和 stderr。
- Minecraft 官方控制台 XML 事件由 `MinecraftLogFormatter` 转换为
  `[HH:mm:ss] [线程/级别]: 消息`，堆栈保持多行。
- 通过 `ProcessListener` 转发格式化后的日志。
- 进程退出后回调退出码。
- 支持 `waitFor`、`exitValue` 和 `stop`。

命令行过长处理：

- 捕获 Windows `CreateProcess error=206` 和 Linux `Argument list too long`。
- Java 9+ 把 JVM 参数写入 `@argfile`，命令行只保留：

```text
java @argfile mainClass gameArgs
```

- Java 8 或 argfile 回退失败时，创建 manifest classpath jar：

```text
java [JVM 参数] -cp classpath-manifest.jar mainClass gameArgs
```

- 两种方式都失败时抛出明确的 `IOException`。
- 临时 argfile 和 manifest jar 在进程退出后删除。

启动错误由 `LaunchException` 分类：

| 类型 | 含义 |
|---|---|
| `COMMAND_TOO_LONG` | 命令行过长，回退仍失败 |
| `PERMISSION_DENIED` | 没有权限执行 Java 或访问目录 |
| `EXECUTABLE_NOT_FOUND` | 找不到 Java 可执行文件 |
| `PROCESS_START_FAILED` | 其他进程启动失败 |

每种错误都提供中文 `userMessage()`，供前端展示。

对外入口：

```java
GameProcess process = backend.processLauncher().launch(plan, classpath, listener);
```

### 3.11 GameLaunchService

`GameLaunchService` 是后端统一启动入口：

```text
安装版本
→ 离线登录
→ 选择游戏 Java
→ GameDataLayout.resolve 应用版本隔离模式
→ NeoForge 26.2 Vulkan 兼容修复
→ 创建 LaunchPlan
→ 重建 virtual assets
→ 解压 natives
→ 构建 classpath
→ 启动进程
→ 返回 GameLaunch
```

对外入口：

```java
GameLaunch launch = backend.launchGame(
        versionId,
        gameDirectory,
        username,
        LaunchOptions.defaults(),
        DownloadListener.NONE,
        processListener
);
```

`GameLaunch` 包含安装结果、AuthSession、JavaRuntime、LaunchPlan、classpath 和 GameProcess。

### 3.12 版本隔离

界面默认选择“完全版本隔离”，并把 `VersionIsolationMode.FULL` 保存到
`~/.caml/settings.properties` 的 `versionIsolation` 项。启动时：

- `FULL`：`--gameDir` 始终指向 `versions/<id>`。
- `NONE`：`--gameDir` 始终指向 `.minecraft` 根目录。

切换模式只影响后续启动，不会自动搬迁已有存档。旧 API 的 `AUTO` 模式仍通过
`GameDataLayout` 检测以下标记：

```text
options.txt
saves
logs
mods
config
resourcepacks
shaderpacks
usercache.json
command_history.txt
```

命中任意一项时使用版本目录作为自动模式的结果。无论显式模式还是自动模式，
`libraries` 和 `assets` 都来自 `.minecraft` 根目录。主页根据当前显式模式显示实际
存档位置；`GameDirectoryInfo.isolatedVersions()` 仍保留给目录扫描和兼容用途。

### 3.13 NeoForge 26.2 + Vulkan 兼容修复

NeoForge 26.2 会把早期加载屏创建器创建的 OpenGL 窗口交给 Minecraft 作为主窗口。
Vulkan 后端随后尝试在该窗口上创建 surface，GLFW 因窗口的 client API 不是
`GLFW_NO_API` 而报 65540。

`GameLaunchService` 在创建 `LaunchPlan` 前调用 `NeoForge262VulkanFix`：

- 仅匹配主类为 `net.neoforged.fml.startup.Client` 且
  `--fml.neoForgeVersion` 为 `26.2.*` 的实例。
- 仅在实际游戏数据目录的 `options.txt` 选择 `vulkan` 时生效。
- 把该目录的 `config/fml.toml` 中 `earlyWindowControl` 改为 `false`；
  文件不存在时创建最小配置。
- 其他 NeoForge 版本、OpenGL 实例和原版实例不受影响。

关闭早期窗口后，Minecraft 会自行创建带 `GLFW_NO_API` 的窗口，Vulkan surface
可以正常初始化。

### 3.14 launch 包结构

```text
launch/
├─ GameLaunchService.java       启动编排入口
├─ GameLaunch.java              启动结果
├─ LaunchPlan.java              启动计划
├─ LaunchPlanner.java           启动计划构建
├─ LaunchOptions.java           启动选项
├─ LaunchException.java         启动错误
├─ LaunchFailure.java           启动错误分类
├─ command/                     参数、占位符和启动命令
├─ classpath/                   classpath 构建
├─ prepare/                     natives 和 virtual assets
├─ process/                     进程、参数文件/manifest 回退和输出监控
└─ patch/                       版本专用兼容修复
```

## 4. 相关源码

- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/instance/VersionInstaller.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/instance/GameDataLayout.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/instance/VersionIsolationMode.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/api/GameDirectoryInfo.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/metadata/VersionManifestService.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/download/plan/DownloadPlanner.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/download/plan/AssetDownloadPlanner.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/download/FileDownloader.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/classpath/ClasspathBuilder.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/command/PlaceholderResolver.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/command/LaunchPlaceholders.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/command/JvmArgumentsBuilder.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/command/GameArgumentsBuilder.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/prepare/NativeExtractor.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/prepare/VirtualAssetInstaller.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/command/LaunchCommand.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/command/LaunchCommandBuilder.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/process/ProcessLauncher.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/process/GameProcess.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/process/MinecraftLogFormatter.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/process/ProcessListener.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/process/ArgumentFile.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/process/ClasspathManifest.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/LaunchFailure.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/LaunchException.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/GameLaunch.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/GameLaunchService.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/patch/NeoForge262VulkanFix.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/`
