# CAML 版本下载与启动链路

更新日期：2026-11-03

## 1. 文档范围

本文只说明：

1. `Version` 如何从远程元数据解析、规划并下载到本地。
2. 当前启动链路已经实现到哪一步，以及后续启动流程还缺什么。

总体架构、模块职责和其他模块的能力见 [architecture.md](architecture.md)。

当前状态：

- 版本下载链路已经可用。
- 启动命令构建和游戏进程启动还没有实现。
- `launch` 包目前只有占位说明。

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

### 2.2 获取版本清单

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

### 2.3 选择版本并拉取版本 JSON

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

### 2.4 处理 inheritsFrom

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

- 父版本在前，子版本覆盖标量字段。
- `arguments.game` 和 `arguments.jvm` 合并。
- `libraries` 合并。
- `downloads` 和 `logging` 合并。
- 清除 `inheritsFrom`，得到可下载的解析后 manifest。

当前没有实现从远程自动安装加载器版本，所以加载器版本的 `inheritsFrom` 依赖本地已经存在的父版本。

### 2.5 生成基础下载计划

`DownloadPlanner.plan(resolved, gameDirectory)` 生成基础文件计划，包含：

- 客户端 jar：`versions/<id>/<id>.jar`。
- libraries：`libraries/<Maven 路径>`。
- 当前系统的 native classifier。
- asset index：`assets/indexes/<assetIndexId>.json`。

规则处理：

- 使用 `RuleEvaluator` 过滤 library 的 `rules`。
- 最后一个匹配规则的 action 生效。
- native classifier 中的 `${arch}` 替换为当前位数。
- 相同目标路径会去重。

### 2.6 执行基础下载

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

### 2.7 下载资源对象

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

### 2.8 写入版本 JSON

所有基础文件和资源对象完成后，`VersionInstaller` 才把原始版本 JSON 原子写入：

```text
versions/<id>/<id>.json
```

这样可以避免下载失败时留下一个看起来已经安装完成的版本目录。

### 2.9 返回结果

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

### 2.10 下载目录布局

默认配置：

```text
~/.caml/                             启动器数据目录
├─ cache/
│  └─ version_manifest_v2.json       版本清单缓存
└─ minecraft/                        游戏目录
   ├─ versions/
   │  └─ <id>/
   │     ├─ <id>.json                原始版本 JSON
   │     └─ <id>.jar                 客户端 jar
   ├─ libraries/                     共享库
   └─ assets/
      ├─ indexes/
      │  └─ <assetIndexId>.json      资源索引
      └─ objects/
         └─ <hash 前两位>/<hash>      资源对象
```

### 2.11 下载链路关键类

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

### 2.12 当前下载限制

- 只支持原版安装；加载器安装尚未实现。
- `inheritsFrom` 只读取本地父版本，不会自动安装父版本。
- 只有官方下载源，镜像源接口尚未接入配置。
- 下载是顺序执行，没有并发和取消。
- 没有内容寻址缓存，只有目标文件存在且 SHA-1 匹配时跳过。

## 3. 启动链路

### 3.1 当前状态

当前还不能真正启动 Minecraft。

`launch` 包目前已经包含 `LaunchPlan`、`LaunchOptions`、`LaunchPlanner`、`ClasspathBuilder`、`JvmArgumentsBuilder` 和 `GameArgumentsBuilder`。以下内容尚未实现：

- natives 解压。
- Log4j 配置写入。
- `ProcessBuilder` 启动。
- stdout/stderr 监控。
- 退出码和异常退出处理。

### 3.2 已有启动前置数据

启动流程需要三类数据：

```text
InstalledVersion  版本目录、manifest、客户端 jar、libraries、assets
AuthSession       用户名、UUID、accessToken、userType、userProperties
JavaRuntime       实际 Java 可执行文件、版本、架构、位数
```

这些类型已经存在，并由 `LaunchPlanner` 组合成不可变的 `LaunchPlan`。

### 3.3 计划中的启动流程

```text
读取 InstalledVersion
→ 读取 AuthSession
→ 选择 JavaRuntime
→ 创建 LaunchPlan（已实现）
→ 构建 classpath（已实现）
→ 构建 JVM 参数（已实现）
→ 构建游戏参数（已实现）
→ 替换占位符（已实现）
→ 准备 natives 和日志配置
→ ProcessBuilder 启动
→ 监控 stdout/stderr 和退出码
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
| `gameDirectory` | `Path` | 游戏目录 |
| `assetsDirectory` | `Path` | assets 目录 |
| `nativesDirectory` | `Path` | natives 解压目录 |

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
→ 读取 downloads.artifact
→ 解析 libraries/<path> 路径
→ 去重并保持顺序
→ 最后追加客户端 jar
```

规则：

- `downloads.artifact` 存在时才加入 classpath。
- classifier 为 `natives-*` 的 native-only 库不加入 classpath。
- 有 native classifier 的普通库仍保留主 artifact。
- 同一个目标路径只加入一次。
- 客户端 jar 固定放在最后。
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

- 玩家名、UUID、accessToken。
- 版本名、版本类型、游戏目录。
- assets 目录、asset index、natives 目录。
- classpath、libraries 目录。
- 启动器名称和版本。
- 分辨率。

对外入口：

```java
List<String> jvmArgs = backend.jvmArgumentsBuilder().build(plan, classpath);
List<String> gameArgs = backend.gameArgumentsBuilder().build(plan, classpath);
```

## 4. 相关源码

- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/instance/VersionInstaller.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/metadata/VersionManifestService.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/download/DownloadPlanner.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/download/AssetDownloadPlanner.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/download/FileDownloader.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/ClasspathBuilder.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/PlaceholderResolver.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/LaunchPlaceholders.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/JvmArgumentsBuilder.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/GameArgumentsBuilder.java`
- `CAMLBackend/src/main/java/org/chs/camlauncher/backend/launch/`
