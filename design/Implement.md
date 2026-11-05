# CAML 当前实现情况

更新日期：2026-11-05

## 1. 总体状态

当前项目已经具备**原版安装链路**、**后端端到端启动能力**和**可操作的 JavaFX 界面**。前端已有游戏位置、版本列表、启动按钮和日志窗口；仍缺少启动错误对话框、前端停止按钮、启动器可见性策略、崩溃分析和真实 Minecraft 客户端端到端验证。

已实现：

```text
远程版本清单
→ 版本 JSON
→ inheritsFrom 合并
→ 客户端/库/natives/资源下载
→ 版本 JSON 落盘
→ 离线账号
→ Java 检测
→ LaunchPlan
→ classpath
→ JVM/游戏参数
→ natives/Log4j
→ 版本隔离判定
→ 命令构建
→ 进程启动/日志
```

尚未完成：微软登录、加载器、启动错误对话框、停止按钮、崩溃分析、启动器可见性策略和真实客户端验证。

## 2. 项目结构

```text
CAML
├─ CAMLBackend   纯 Java 核心
├─ CAMLUI       纯 JavaFX + CSS 的 CAML 组件库
└─ CAMLFront    JavaFX 界面
```

构建：

- Java 25 toolchain。
- Gradle 9.8.2。
- Gson `2.15.0`。
- Log4j `2.26.1`。
- JavaFX `25.0.3`。
- JUnit `6.1.3`。

默认目录：

- 启动器数据：`~/.caml`。
- 外部主题：`~/.caml/themes/*.css`。
- 当前主题：`~/.caml/theme.txt`。
- 游戏数据：
  - Linux/macOS：`~/.minecraft`。
  - Windows：启动器所在目录下的 `.minecraft`。

## 3. 已实现功能

### 3.1 版本元数据

- 解析 `version_manifest_v2.json`。
- 解析单版本 JSON。
- 解析字符串参数和带规则参数。
- 解析 libraries、asset index、downloads、javaVersion 和 logging。
- 支持 OS、架构、版本正则和 features 规则。
- 支持 `inheritsFrom` 父版本合并。
- 检测缺失父版本和循环继承。

### 3.2 远程元数据

- 拉取官方版本清单。
- 版本清单本地缓存。
- 网络失败时读取缓存。
- 拉取指定版本的原始 JSON。

### 3.3 下载

- 生成客户端 jar、libraries、natives、asset index 下载计划。
- 生成 assets objects 下载计划。
- SHA-1 校验。
- `.part` 临时文件。
- 校验通过后原子移动。
- 下载重试。
- 已有有效文件时跳过。

### 3.4 版本安装

- 按版本 ID 安装原版。
- **本地优先**：`versions/<id>/<id>.json` 已存在时直接复用本地版本，不访问官方清单。
- 版本 ID 即 `versions/` 下的文件夹名，支持第三方启动器/整合包的自定义名称（如 `1.21.4-DeepSeek`）。
- 解析后把 manifest 的 `id` 统一为文件夹名，`inheritsFrom` 按文件夹名查找父版本。
- 支持本地已安装父版本。
- 下载完成后才写入版本 JSON；复用本地版本时保持原文件不变。
- 补齐本地版本缺失的库、资源和客户端 jar。
- 返回 `InstalledVersion`。

### 3.5 离线账号

- 离线账号登录。
- 稳定离线 UUID。
- 生成 `AuthSession`。

### 3.6 Java 检测

- 扫描本机 Java。
- 读取版本、供应商、架构和位数。
- 根据版本 JSON 生成 Java 要求。
- 选择满足要求的 Java。
- Windows 优先使用 `javaw.exe`。

### 3.7 启动计划

- 生成不可变 `LaunchPlan`。
- 合并 `InstalledVersion`、`AuthSession`、`JavaRuntime`、目录和 `LaunchOptions`。
- 支持内存、分辨率、JVM 参数、游戏参数和环境变量。
- 创建计划时校验 Java 是否满足版本要求。
- **版本隔离**：`GameDataLayout` 检查 `versions/<id>` 下的 `saves`、`options.txt`、
  `logs`、`mods`、`config`、`resourcepacks`、`shaderpacks`、`usercache.json`、
  `command_history.txt`；命中任意一项时以该版本目录作为 `--gameDir`。
- `LaunchPlanner.create(..., gameDataDirectory)` 支持显式指定游戏数据目录，
  `GameLaunchService` 负责解析隔离布局。
- `.minecraft/libraries` 与 `.minecraft/assets` 始终来自根目录。

### 3.8 classpath

- 按规则筛选 libraries。
- 排除 native-only library。
- 去重并保持顺序。
- 客户端 jar 最后追加。
- 使用平台路径分隔符拼接。

### 3.9 启动参数

- JVM 参数：内存、版本 JSON 参数、用户参数、`-Djava.library.path`、`-cp`。
- 游戏参数：版本 JSON 参数、旧版 `minecraftArguments`、用户参数、全屏。
- 占位符：认证信息、版本、目录、assets、natives、classpath、启动器信息、分辨率。
- features：`has_custom_resolution`。
- 过滤空参数。

### 3.10 natives 与 Log4j

- 清理旧 natives 目录。
- 解压当前系统 native jar。
- 跳过 `META-INF/` 和 `extract.exclude`。
- 下载 Log4j 配置到 `assets/log_configs`。
- 注入 `-Dlog4j.configurationFile`。
- 旧版本 virtual assets 重建。
- `map_to_resources` 写入 `resources`。

### 3.11 JavaFX 界面

- 启动时初始化后端。
- 游戏位置下拉框 + 浏览按钮，可切换默认目录或本地已有 `.minecraft`。
- 版本下拉框可编辑，后端探测到已安装版本时自动填充。
- 支持直接输入第三方启动器/整合包的自定义版本 ID。
- 显示存档位置：隔离版本提示 `versions/<id>/saves`，否则提示 `.minecraft/saves`。
- 离线用户名、最大内存、启动按钮、状态提示和日志窗口。
- `MainController` 用独立 I/O 线程执行目录探测和启动，下载日志每 10% 节流。
- 主工作区宽屏左右分栏，窄屏上下堆叠；断点为 900px。
- 断点规则集中在纯计算的 `WorkspaceLayout`：跨过断点时切换 `SplitPane` 方向、放宽最小高度
  （表单 320→240、日志面板 220→140、日志文本区 200→120，首选高度 460→260）并重置分隔条
  （宽屏 0.56、堆叠 0.62）。
- `WorkspaceLayout.Plan` 只依赖断点两侧，缩放过程中方案不变，因此不会重复改写布局，
  用户手动拖动的分隔条也不会被重置；不改变控件尺寸、间距和字号。
- `app.css` 不再声明日志文本区的 `-fx-min-height` / `-fx-pref-height`：CSS 声明会覆盖代码设置，
  会让断点放宽失效。
- 窗口尺寸不再固定：`WindowSizePlanner` 按 `Screen.getPrimary().getVisualBounds()` 计算初始
  宽高（可用区域的 68% / 74%）、最小尺寸与居中位置，并限制在 `720x700` 到 `1440x960` 之间；
  小屏幕上最小尺寸按可用区域的 90% 收缩，保证窗口不会大于桌面。
- `WindowSizePlanner` 为纯计算类，不依赖 JavaFX，由 `WindowSizePlannerTest` 覆盖小屏、全高清、
  超宽屏、居中与非法输入；`WorkspaceLayoutTest` 覆盖 900px 断点两侧、最小高度放宽和分隔条位置。
- 启动按钮固定在表单底部，字段区域独立滚动。
- 表单标签位于控件上方，主题选择器位于顶栏并即时生效。
- 顶栏主导航提供“主页 / 下载 / 设置”，主页为当前启动界面。
- 下载和设置页面当前为占位内容，导航切换会保留主页已填写的状态。
- 界面控件来自 `CAMLUI`：`CamlButton`、`CamlCard`、`CamlTextField`、`CamlComboBox`、
  `CamlSpinner`、`CamlTextArea` 和 `CamlTopBar`。
- 主题入口：`CamlTheme.install(scene)` 和 `CamlTheme.applyTheme(scene, path)`。
- 结构样式：`CAMLUI/src/main/resources/org/chs/camlauncher/ui/caml.css`，不含固定配色。
- 默认主题：`CAMLBackend/src/main/resources/themes/default.css`，白底黄灰白配色。
- 顶栏主题下拉框列出 `~/.caml/themes` 中的 CSS 文件并保存选择。
- 布局样式：`CAMLFront/src/main/resources/styles/app.css`。

### 3.12 外部主题

- 启动器本体不定义固定主题。
- 首次启动由 `ThemeService` 把默认主题复制到 `~/.caml/themes/default.css`。
- 主题文件是普通 JavaFX CSS，覆盖 `.caml-root` 下的 `-caml-*` 颜色令牌。
- 主题选择写入 `~/.caml/theme.txt`，无效选择回退到默认主题。
- 用户修改或新增主题文件后，重启即可在顶栏主题下拉框中看到。

### 3.13 日志

- Log4j 控制台输出。
- 文件日志：`~/.caml/logs/caml.log`。

### 3.14 进程启动

- 构建最终启动命令。
- 设置工作目录。
- 合并环境变量。
- Windows 条件设置 `APPDATA`。
- 启动 `ProcessBuilder`。
- 转发 stdout/stderr。
- 回调退出码。
- 支持等待和停止进程。
- Java 9+ 命令行过长时自动使用 `@argfile`。
- Java 8 或 argfile 失败时使用 manifest classpath jar。
- 两种回退都失败时抛出明确异常。
- 启动失败分类：命令行过长、权限不足、找不到 Java、其他进程错误。
- 提供中文 `userMessage()`。
- `GameLaunchService` 串联安装、离线登录、Java、LaunchPlan、classpath、natives 和进程启动。
- 版本隔离时工作目录为 `versions/<id>`，但 Windows `APPDATA` 仍按 `.minecraft` 根目录计算。

## 4. 对外 API

```java
CAMLBackend backend = new CAMLBackend(BackendConfig.defaults());
backend.start();

InstalledVersion installed = backend.installVersion("1.21.5", DownloadListener.NONE);

AuthSession auth = backend.loginOffline("Steve");

JavaRuntime java = backend.javaLocator()
        .findSuitable(JavaRequirement.from(installed.manifest()))
        .orElseThrow();

LaunchPlan plan = backend.createLaunchPlan(
        installed,
        auth,
        java,
        LaunchOptions.defaults()
);

List<Path> classpath = backend.classpathBuilder().build(plan);

List<String> jvmArgs = backend.jvmArgumentsBuilder().build(plan, classpath);
List<String> gameArgs = backend.gameArgumentsBuilder().build(plan, classpath);

backend.nativeExtractor().extract(plan);

plan = backend.virtualAssetInstaller().prepare(plan);

GameProcess process = backend.processLauncher().launch(plan, classpath, listener);

GameLaunch launch = backend.launchGame(
        "1.21.5", gameDirectory, "Steve", LaunchOptions.defaults(), listener, processListener);

GameDirectoryInfo info = backend.inspectGameDirectory(gameDirectory);
info.installedVersions();
info.isolatedVersions();
```

## 5. 运行方式

```bash
JAVA_HOME=/usr/lib/jvm/zulu-25 \
PATH=/usr/lib/jvm/zulu-25/bin:$PATH \
./gradlew :CAMLFront:run
```

## 6. 已验证内容

- 后端构建通过。
- JavaFX 启动冒烟测试通过。
- 62 个后端测试全部通过。
- 集成测试覆盖：
  - 版本清单获取与缓存。
  - 版本 JSON 获取。
  - 客户端、library、asset index、asset object 下载。
  - SHA-1 校验和原子落盘。
  - 离线账号。
  - Java 检测和匹配。
  - LaunchPlan 创建和 Java 要求校验。
  - classpath 过滤、去重、native 排除和客户端 jar 顺序。
  - JVM/游戏参数、占位符、features 和旧版参数回退。
  - natives 清理、解压和排除规则。
  - virtual assets 重建和 assets 目录替换。
  - 平台默认游戏目录：Linux 用户目录，Windows 启动器目录。
  - 默认主题首次复制、主题列表排序、主题选择持久化和路径穿越校验。
  - 最终命令顺序、进程启动、stdout/stderr 转发和退出码。
  - argfile 转义和 manifest classpath jar。
  - 超长 classpath 的自动回退。
  - 启动失败分类和用户提示文本。
  - 本地 HTTP 安装假客户端并完成端到端启动。
  - 本地自定义版本 ID 复用和版本 JSON 保持。
  - 已安装版本/隔离版本目录探测。
  - PCL/HMCL 版本隔离判定。

## 7. 当前未实现

- 启动器隐藏/保留/关闭策略。
- 异常退出分类和崩溃分析。
- CAMLFront 启动错误对话框。
- CAMLFront 停止游戏按钮。
- 真实 Minecraft 客户端端到端验证。
- 启动脚本导出。
- 微软账号登录。
- token 刷新和安全存储。
- Fabric、Quilt、Forge、NeoForge 等加载器。
- 镜像源、并发下载、取消和内容寻址缓存。
- quick play 和服务器直连。
- 整合包和模组管理。

## 8. 相关文档

- [总体架构](architecture.md)
- [版本下载与启动链路](version-download-and-launch.md)
- [runtime 包](runtime.md)