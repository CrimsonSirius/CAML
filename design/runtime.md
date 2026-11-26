# CAML 游戏 Java 运行时

> 本文按当前 `runtime` 包与相关启动逻辑重写，最后核对时间：2026-11-23；基线提交：`c86eb05`。

## 1. 范围

这里管理的是**游戏 Java**，不是启动器自身运行的 Java 25。启动器自带裁剪 JDK，游戏 Java 由扫描、手动指定或 CAML 托管仓库提供。

主要流程：

```text
VersionInfo.javaVersion
        │
        ▼
JavaRequirement ──> JavaLocator.findSuitable
        │                   │
        │                   ├─ 系统/JAVA_HOME/PATH
        │                   ├─ 常见 JDK 目录
        │                   └─ ~/.caml/java 嵌套仓库
        │
        ├─ 自动模式：选择满足要求的最近 Java
        └─ 手动模式：使用用户指定路径
```

## 2. 数据结构

`JavaRuntime` 是已识别的运行时：

- `executable`：`java`/`java.exe` 路径；
- `version`、`majorVersion`；
- `vendor`；
- `architecture`（规范化为 `x86_64`、`x86`、`arm64` 等）；
- `bits`；
- `jdk`：同目录存在 `javac` 时为 JDK，否则视为 JRE。

`JavaCandidate` 是设置页展示项，额外包含 `manual` 和 `available`。不可用的手动路径也会保留为候选，便于用户发现和修复。

## 3. Java 要求

`JavaRequirement.from(VersionInfo)`：

- 读取版本 JSON 的 `javaVersion.majorVersion`；
- 缺失时回退 Java 8；
- 架构取当前系统架构；
- 位数取当前 JVM 数据模型，无法读取时 x86 视为 32 位，其余视为 64 位。

`isSatisfiedBy()` 要求：

- `runtime.majorVersion >= requirement.majorVersion`；
- 架构完全相同；
- 要求位数大于 0 时位数相同。

选择时 `JavaLocator.findSuitable()` 优先精确匹配主版本，再选择主版本差最小的运行时；同分时优先 JDK，再按路径稳定排序。因此旧版本明确要求 Java 8 时，存在 Java 8 会优先于 Java 25。

## 4. 扫描来源

`JavaLocator.discover()` 会检查：

- 当前 `java.home`；
- `JAVA_HOME`；
- `PATH` 中所有 `java`/`java.exe`；
- `~/.jdks`；
- `~/.gradle/jdks`；
- `~/.sdkman/candidates/java`；
- `~/.caml/java`，最多向下扫描 6 层，识别 `<platform>/<name>/bin/java` 和 macOS 的 `Contents/Home/bin/java`；
- Linux：`/usr/lib/jvm`、`/usr/java`、`/opt`；
- Windows：`ProgramFiles` 下 Java/Adoptium/Zulu/Microsoft 等目录、`LOCALAPPDATA/Programs`。

托管仓库必须递归扫描，因为 CAML 实际布局是：

```text
~/.caml/java/linux-x86_64/zulu-8-jdk/bin/java
```

而不是 `~/.caml/java/<name>/bin/java`。这一规则保证 Java 管理页安装的旧版 Java 能被自动启动流程看到。

## 5. 识别 Java

对每个候选执行：

```text
java -XshowSettings:properties -version
```

合并 stdout/stderr，读取：

- `java.version`；
- `java.vendor`；
- `java.home`；
- `os.arch`；
- `sun.arch.data.model`。

识别有 20 秒超时（CI 上冷启动 JVM 明显慢于本机）。子进程输出由独立线程先读干净再等待退出：
`-XshowSettings:properties` 的输出包含 `java.class.path`，在 Windows 上很容易超过管道缓冲区，
先 `waitFor` 再读会直接死等到超时。

进程探测失败（无法执行、超时、退出码非 0、输出无法解析、被安全软件拦截）时，
回退读取 `<jdk>/release` 里的 `JAVA_VERSION`、`OS_ARCH`、`IMPLEMENTOR`，
只有两者都失败才忽略该候选。这样即使运行环境禁止起子进程，也仍能识别出可用的 Java。
`JavaRuntimeParser` 负责版本主号、架构和位数规范化。

Windows 启动时，如果同目录存在 `javaw.exe`，优先使用 `javaw.exe` 避免控制台窗口。

## 6. 扫描结果合并与排序

`discoverCandidates(manualPaths, selectedPath)`：

1. 扫描系统 Java；
2. 合并手动添加路径，同一真实路径只保留一项并标记 `manual=true`；
3. 当前选中的路径即使无效也保留，标记 `available=false`；
4. 排序：可用优先、主版本降序、路径稳定排序。

设置页的“重新扫描”只刷新扫描结果，不丢弃手动项；Java 管理页提供“重新扫描 / 添加 Java / 下载 Java”。

## 7. 托管 Java 仓库

路径：

```text
~/.caml/java/<platform>/<name>/
~/.caml/java/<platform>/<name>.json
```

`<platform>` 形如 `linux-x86_64`、`windows-x86_64`、`macos-arm64`。

`ManagedJavaRepository.install()`：

- 生成唯一安装名；
- 解压到临时目录；
- 若归档只有一个顶层目录，则将其移动为安装目录，否则把子项移入安装目录；
- 定位 `bin/java`（macOS 支持 `Contents/Home/bin/java`）；
- 设置可执行权限；
- 用 `JavaLocator.inspect()` 确认运行时可用；
- 写 `<name>.json` 清单；
- 任一步失败删除目标目录。

清单记录名称、发行版 ID/名称、JDK/JRE、主版本、完整 Java 版本、归档类型、文件名、校验值和安装时间。

## 8. Java 下载

`JavaDownloadService` 使用 Foojay Disco API v3：

```text
https://api.foojay.io/disco/v3.0
```

支持发行版：

- Eclipse Temurin；
- Azul Zulu；
- BellSoft Liberica；
- Oracle GraalVM；
- Amazon Corretto；
- IBM Semeru。

请求参数根据当前平台设置：

- OS：`windows`、`linux`、`macos`；
- 架构：`x64`、`x86`、`aarch64`；
- 归档：Windows `zip`，其他 `tar.gz`；
- 包类型：`jdk` 或 `jre`；
- `release_status=ga`；
- `directly_downloadable=true`；
- Linux 额外要求 `lib_c_type=glibc`。

每个主版本保留最新发行版版本，按主版本降序展示。下载时先请求包详情中的直接下载 URI 和 checksum；支持 SHA-1、SHA-256、SHA-512。校验通过后才解压安装。

Linux UI 会提示优先使用 `apt`、`dnf`、`pacman` 等包管理器；Windows 不显示该提示。

## 9. 归档解压

`JavaArchiveExtractor` 不引入额外压缩库：

- ZIP：`ZipInputStream`；
- TAR/GZIP：自行解析 tar 头、PAX/LongName、目录、普通文件、相对符号链接；
- 绝对路径、越界路径、指向根目录外的 symlink 被拒绝或跳过；
- 未知类型跳过。

这样无需 `tar` 命令即可处理 Adoptium/Zulu 等常见归档。

## 10. 自动选择与手动选择

设置值：

- `javaSelectionMode=auto|manual`；
- `selectedJavaPath`；
- `manualJavaPaths`。

自动模式在每次启动时重新按版本要求选择 Java，不固定某个旧路径。手动模式只使用用户选定路径；路径缺失或无法识别时抛出结构化启动错误，不静默回退。

## 11. Linux 启动环境提示

`LaunchCompatibilityService` 检查：

- 系统是 Linux；
- 实例继承链中存在 LWJGL 2；
- `PATH` 与 `~/.local/bin` 都找不到可执行的 `xrandr`。

满足时前端显示应用内提示，列出 Arch/Debian/Fedora 等安装命令。提示不阻止启动，且启动任务在预检之前已经提交。

`ProcessLauncher` 会为游戏子进程把 `~/.local/bin` 前插到 `PATH`，让用户级安装的 `xrandr` 也能被 LWJGL 2 找到。

## 12. 测试与限制

测试覆盖：

- `JavaRequirementTest`
- `JavaRuntimeParserTest`
- `JavaLocatorTest`
- `JavaArchiveExtractorTest`
- `LaunchCompatibilityServiceTest`

限制：

- 不自动下载缺失的 Java；需要用户进入 Java 管理页下载或手动添加。
- 托管仓库只使用 Foojay 提供的 glibc Linux 包；musl/Alpine 不在当前筛选范围内。
- Java 下载未提供暂停/续传 UI；文件下载层仍支持断点续传。
- Windows 没有注册表级 JDK 全量枚举，只扫描常见目录与 PATH。
