# CAML 总体架构

更新日期：2026-11-07

## 1. 架构目标

CAML 采用前后端分离的多模块结构：

```text
CAMLUI      JavaFX + CSS 的 CAML 组件库
CAMLFront   JavaFX 界面，只负责展示和交互
CAMLBackend Java 核心，负责元数据、下载、账号、Java 和启动
```

核心约束：

- `CAMLBackend` 是纯 Java，不 import JavaFX。
- `CAMLBackend` 不依赖 `CAMLFront`。
- `CAMLUI` 只依赖 JavaFX 和自身 CSS，不依赖 `CAMLBackend` 或 `CAMLFront`。
- `CAMLUI` 不定义固定配色；组件颜色全部来自外部主题文件。
- `CAMLFront` 不直接读写文件、不直接发 HTTP、不直接启动进程。
- 前后端通过 facade、DTO、事件和 `CompletableFuture` 交互。

## 2. 后端模块职责

| 包 | 职责 | 当前状态 |
|---|---|---|
| `api` | 后端 facade 和状态 DTO | 已有 |
| `config` | 配置、目录约定和路径初始化 | 已有 |
| `metadata` | 版本清单和 JSON；子包 `model`、`rule`、`parse`、`resolve` | 已有 |
| `instance` | 实例模型、本地版本复用、版本安装和隔离判定 | 已有 |
| `download` | 下载入口；子包 `model`、`plan`、`source`、`io`、`hash` | 已有 |
| `auth` | 账号、AuthSession、离线登录 | 已有离线能力 |
| `runtime` | Java 运行时检测、版本/架构/位数匹配 | 已有 |
| `launch` | 启动编排；子包 `command`、`classpath`、`prepare`、`process`、`patch` | 已有 |
| `theme` | 主题目录、默认主题复制和当前主题配置 | 已有 |
| `settings` | 启动器设置持久化（游戏位置、最大内存、版本隔离、下载源、下载并发数） | 已有 |
| `system` | 物理内存与可用内存读取 | 已有 |
| `loader` | Fabric Loader/Fabric API、Forge、NeoForge、OptiFine（仅 Forge）安装；Quilt 待实现 | 已有 |
| `json` | 通用 JSON 文件读写 | 已有 |

## 3. 当前已实现能力

### 3.1 元数据

- 解析 `version_manifest_v2.json`。
- 解析单版本 JSON。
- 解析字符串参数和带规则参数。
- 解析 libraries、asset index、downloads、javaVersion 和 logging。
- 处理 OS、架构、版本正则和 features 规则。
- 合并 `inheritsFrom` 父版本。
- 子版本标量字段覆盖父版本；libraries 子版本优先；支持 `jar` 字段。
- 拉取远程版本清单和单版本 JSON。
- 版本清单缓存和网络失败回退。

### 3.2 下载

- 生成客户端 jar、libraries、natives、asset index 下载计划。
- 旧式 Forge library 根据 `name/url/checksums/clientreq` 推导 Maven 下载。
- 生成 assets objects 下载计划。
- 使用 SHA-1 校验。
- 使用 `.part` 临时文件和原子移动。
- 下载使用 HTTP/1.1；失败重试并短暂退避，最终错误包含下载项和地址。
- 使用有界线程池并发执行下载，并发数由设置决定（1–256，默认 8）。
- 下载源支持“尽可能使用镜像源”“尽可能使用官方源”和“自定义镜像根地址”三种模式；
  默认镜像源，镜像失败时回退原始官方地址；覆盖版本文件、库、资源、加载器 installer/API。
- 已有有效文件时跳过。

### 3.3 实例安装

- 按版本 ID 安装原版。
- 远程安装区分官方版本 ID 和实例 ID：官方 ID 用于拉取元数据，实例 ID 用作 `versions/<id>` 目录名和当前实例名。
- 实例名默认官方版本号，拒绝空名、路径分隔符和 Windows 非法字符。
- 本地已安装的版本（包括第三方启动器/整合包的自定义版本 ID）直接复用，不访问官方清单。
- 复用本地版本时保持版本 JSON 不变，只补齐缺失文件。
- 本地版本以 `versions/<id>` 文件夹名作为版本 ID；`inheritsFrom` 也按文件夹名查找父版本。
- 安装完成后把 manifest 的 `id` 统一为文件夹名；客户端 jar 按 `jar` 字段回退到实例 ID。
- 基础文件下载完成后才写入版本 JSON。
- 返回 `InstalledVersion`，包含后续启动需要的路径。
- Fabric 安装先确保原版父版本存在，再写入 Fabric Meta profile JSON，并把可选 Fabric API jar 放入实际游戏目录的 `mods`。
- Forge/NeoForge 通过官方 installer 的 `--installClient` 在 staging 目录执行 processor，再合并 libraries 和版本目录。
- OptiFine 从 BMCLAPI 获取版本和 installer；先安装 Forge，再调用 OptiFine 官方 installer 的
  `doInstall` 生成补丁库，最后把 OptiFine 库和启动参数合并进 Forge 版本 JSON。
- OptiFine 只允许与 Forge 组合；选择 Fabric/NeoForge 时 OptiFine 选项会被清空并禁用。
- `CAMLBackend.inspectGameDirectory` 扫描 `versions/<id>/<id>.json`，返回已安装版本和隔离版本列表。

### 3.4 账号

- 离线账号登录。
- 生成稳定的离线 UUID。
- 生成 `AuthSession`。

### 3.5 Java

- 扫描 `java.home`、`JAVA_HOME`、`PATH` 和常见 JDK 目录。
- 读取 Java 版本、供应商、架构和位数。
- 根据版本 JSON 的 `javaVersion` 生成 `JavaRequirement`。
- 选择满足要求的 JavaRuntime。
- Windows 优先使用 `javaw.exe`。
- 详细说明见 [runtime.md](runtime.md)。

### 3.6 启动计划

- 生成不可变 `LaunchPlan`。
- 合并 `InstalledVersion`、`AuthSession`、`JavaRuntime`、目录和 `LaunchOptions`。
- 支持内存、分辨率、JVM 参数、游戏参数和环境变量。
- 启动计划创建时校验 Java 是否满足版本要求。
- **版本隔离**：界面持久化 `VersionIsolationMode.FULL` 或 `NONE`。`FULL` 始终把
  `--gameDir` 指向 `versions/<id>`，`NONE` 始终指向 `.minecraft` 根目录；
  `libraries` 与 `assets` 始终来自根目录。旧 API 仍可使用 `AUTO` 按
  `saves`、`options.txt` 等标记自动检测。`GameLaunchService` 接收显式模式并调用
  `GameDataLayout.resolve`。

### 3.7 classpath

- 按规则筛选 libraries。
- 排除 native-only library。
- 跳过 `clientreq=false`。
- 按 `group:artifact:classifier` 与目标路径去重，子版本优先。
- 子版本库排在父版本库前，客户端 jar 按 `jar` 字段追加在最后。
- 使用平台路径分隔符拼接。

### 3.8 启动参数

- 构建 JVM 参数和游戏参数。
- 支持内存、分辨率、用户自定义参数和环境变量。
- 替换认证、版本、目录、classpath 等 `${...}` 占位符。
- 处理 `has_custom_resolution` 等 features 规则。
- 支持旧版 `minecraftArguments`。
- 自动补充 `-Djava.library.path` 和 `-cp`。

### 3.9 natives 与日志配置

- 清理旧 natives 目录。
- 解压当前系统的 native jar。
- 根据版本 JSON 的 `-Djava.library.path=${natives_directory}/<子目录>` 决定实际解压目录。
- 跳过 `META-INF/` 和 `extract.exclude` 指定内容。
- 下载游戏 Log4j 配置文件。
- 在 JVM 参数中注入 `-Dlog4j.configurationFile`。
- 旧版本 asset index 标记为 virtual 时重建 `assets/virtual/<id>`。
- `map_to_resources` 时同步写入 `resources`。

### 3.10 进程启动

- 构建最终 `java + JVM 参数 + mainClass + 游戏参数` 命令。
- 设置工作目录和环境变量。
- 仅在 Windows 设置 `APPDATA`。
- 使用 `ProcessBuilder` 启动。
- 后台线程读取 stdout/stderr；Minecraft Log4j `LegacyXMLLayout` 事件转换为
  `[HH:mm:ss] [线程/级别]: 消息` 后再转发到界面。
- 提供退出码回调和停止能力。
- 命令行过长时，Java 9+ 自动使用 `@argfile`。
- Java 8 或 argfile 失败时使用 manifest classpath jar。
- 两种回退都失败时抛出明确异常。
- `LaunchException` 区分命令行过长、权限不足、找不到 Java 和其他启动失败。
- 每种错误提供用户可读提示文本。
- `GameLaunchService` 串联安装、离线登录、Java 选择、LaunchPlan、classpath、natives 和进程启动。
- 版本隔离时 `workingDirectory` 是 `versions/<id>`，但 Windows `APPDATA` 仍按 `.minecraft` 根目录计算。

### 3.11 前端界面

- `CAMLApplication` 初始化后端并创建 `MainView`。
- `MainView` 只负责壳层：顶栏、主导航、`CamlPageHost` 和底栏。
- `MainController` 只组合 `HomeController`、`DownloadsController` 和 `SettingsController`；
  共享的 `ControllerContext` 负责后端访问、I/O 线程和日志回调。
- `HomeView` 组合 `LaunchFormView` 与 `LogPaneView`，负责主页分栏和 900px 断点。
- `LaunchFormView` 负责当前实例、用户名、状态提示和启动交互；
  “浏览游戏实例”打开二级列表，“管理实例”打开空管理页。
- `GameLocationSettingsView` 负责游戏位置草稿、浏览、显式保存和目录校验。
- `InstancePickerView` 扫描当前游戏位置并列出全部已安装实例。
- `DownloadsView` 负责下载页子导航；`DownloadVersionsView` 加载官方正式版列表，
  `DownloadVersionSetupView` 负责实例命名、版本信息、Forge/OptiFine/Fabric/NeoForge 选择；
  OptiFine 只有在选择 Forge 后才可用，底部按钮通过后端安装原版或加载器游戏。
- `GameSettingsView` 负责叠层内存滑块，`DownloadSettingsView` 负责下载源三选一、
  自定义 API 输入和 1–256 下载并发滑块，
  `LauncherSettingsView` 负责外观设置中的主题选择。
- `SystemMemoryService` 在 Linux 读取 `/proc/meminfo` 的 `MemAvailable`，避免把
  `MemFree` 误当成任务管理器显示的可用内存。
- `SettingsController` 每 500ms 在后台线程读取一次系统内存，并通过 `Platform.runLater`
  立即刷新设置页。
- 游戏位置、版本隔离、下载源、下载并发数、最大内存和最后启动实例保存在
  `~/.caml/settings.properties`，启动时加载；游戏位置只有点击保存才写入，其余选项立即写入。
- `LaunchSettings` 在主页和设置页之间共享游戏位置、版本隔离和最大内存值；`ViewSupport` 提供通用字段与侧边栏布局。
- 当前实例通过共享 `LaunchSettings.selectedVersion` 管理；启动器初始化时恢复 `lastInstanceId`，实例信息下方显示存档位置。
- `ControllerContext` 使用独立 I/O 线程执行目录探测和下载；`HomeController` 负责启动派发，
  不阻塞 JavaFX Application Thread。
- 下载日志按文件切换和每 10% 进度节流，避免刷屏。
- 主工作区使用 `SplitPane`：宽度 `< 900` 时上下堆叠，`>= 900` 时左右分栏。
- 断点切换时放宽表单和日志的最小高度，表单和日志在堆叠时仍然都能看到。
- `WindowSizePlanner` 按主屏可用区域计算窗口初始尺寸、最小尺寸和居中位置；
  `WorkspaceLayout` 负责 900px 断点参数。
- 启动设置是主区域，日志是支持面板；表单字段采用“标签在上、控件在下”的稳定结构。
- 启动按钮固定在表单底部，字段过多时只滚动字段区域，主操作始终可见。
- 顶栏只保留应用身份和“主页 / 下载 / 设置”主导航。
- 主页承载启动设置和日志；下载页带“添加游戏实例”胶囊、正式版列表和原版下载设置页。
- 设置页带“游戏设置 / 下载设置 / 外观”子侧边栏，不显示页面标题或说明。
- 游戏设置使用内存滑块，线上方显示“游戏内存 / 可用内存 / 总内存”。
- 外观设置使用圆角组承载主题选择，切换后立即生效，不额外放保存按钮。
- 侧边栏不使用灰色背景，只用右侧分隔线；设置项选中时为橙黄实心胶囊，未选中时为橙黄描边胶囊。

### 3.12 CAMLUI 组件库与主题

- 独立 Gradle 模块 `CAMLUI`，只依赖 JavaFX controls。
- 不引入 JFoenix、AtlantaFX 等第三方主题库。
- `caml.css` 只定义结构、字体层级、圆角、阴影和控件状态，不包含固定颜色。
- `CamlTheme.install(scene)` 安装结构样式；`CamlTheme.applyTheme(scene, path)` 加载外部 CSS。
- 主题文件位于 `~/.caml/themes/*.css`，设置 → 外观提供主题选择器。
- 当前主题记录在 `~/.caml/theme.txt`，下次启动自动恢复。
- 后端 `ThemeService` 负责扫描主题、首次复制默认主题和保存选择。
- 默认主题 mango 作为 `CAMLBackend/src/main/resources/themes/mango.css` 打包，
  首次启动复制到 `~/.caml/themes/mango.css`。
- 默认主题使用白底、灰色表面、黄色强调色和深色文字。
- 组件 API 包括 `CamlButton`、`CamlCard`、`CamlTextField`、`CamlTextArea`、
  `CamlComboBox`、`CamlSpinner`、`CamlSwitch`、`CamlCheckBox`、`CamlSlider`、
  `CamlMemorySlider`、`CamlRadioButton`、`CamlProgressBar`、`CamlSideNav`、`CamlSideNavItem`、`CamlTopNav`、
  `CamlTopNavItem`、`CamlPageHost` 和 `CamlTopBar`。
- `CAMLFront` 依赖 `CAMLUI`，不再把控件外观写死在业务界面里。

## 4. 尚未实现的能力

- 启动器隐藏/保留/关闭策略。
- 异常退出分类和崩溃分析。
- CAMLFront 启动错误对话框。
- CAMLFront 停止游戏按钮。
- 真实 Minecraft 客户端端到端验证。
- 启动脚本导出。
- 微软账号登录。
- token 刷新和安全存储。
- Quilt 加载器安装。
- 下载取消和内容寻址缓存。
- 整合包和模组管理。

## 5. 数据与安全原则

- 版本元数据以官方 `version_manifest_v2.json` 和单版本 JSON 为准。
- 下载必须校验 SHA-1，先写临时文件，校验通过后再原子移动。
- 缓存和实例目录必须有明确边界。
- 启动器数据默认放在 `~/.caml`。
- 外部主题放在 `~/.caml/themes`，启动器不覆盖用户已有主题。
- 游戏数据默认放在 `~/.minecraft`（Linux/macOS）或启动器目录下的 `.minecraft`（Windows）。
- 微软登录只使用官方 OAuth 流程，不收集用户密码。
- refresh token 不得明文保存，不得写入日志。
- 启动器 JDK 和游戏 Java 是两套运行时，不能混用。
- 启动命令使用 `List<String>` 构建，不拼接 shell 字符串。
- 网络、磁盘、进程和认证操作不得在 JavaFX Application Thread 执行。

## 6. 后端入口

主要入口是：

```text
org.chs.camlauncher.backend.api.CAMLBackend
```

当前主要 API：

```java
backend.start();
backend.installVersion("1.21.5", listener);
backend.loginOffline("Steve");
backend.javaLocator();
backend.fetchVersionManifest();
backend.createLaunchPlan(installed, auth, java, options);
backend.classpathBuilder().build(plan);
backend.jvmArgumentsBuilder().build(plan, classpath);
backend.gameArgumentsBuilder().build(plan, classpath);
backend.nativeExtractor().extract(plan);
backend.virtualAssetInstaller().prepare(plan);
backend.launchCommandBuilder().build(plan, classpath);
backend.processLauncher().launch(plan, classpath, listener);
backend.launchGame(versionId, gameDirectory, username, options, downloadListener, processListener);

GameDirectoryInfo info = backend.inspectGameDirectory(gameDirectory);
info.installedVersions();
info.isolatedVersions();
```

## 7. 相关文档

- 版本下载和启动链路详见 [version-download-and-launch.md](version-download-and-launch.md)。
- 界面布局、组件映射和动效详见 [ui.md](ui.md)。
- runtime 包详见 [runtime.md](runtime.md)。
- 当前实现情况详见 [Implement.md](Implement.md)。
- 开发任务和阶段计划由本地 `TODO.md` 维护。
