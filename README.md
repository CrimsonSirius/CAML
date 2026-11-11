# CAML

CAML 是一个基于 **Java 25 + JavaFX** 的 Minecraft 启动器，采用前后端分离的多模块结构。

- **CAMLBackend** —— 纯 Java 核心，负责元数据、下载、账号、Java 检测与游戏启动。
- **CAMLUI** —— 纯 JavaFX + CSS 的 CAML 组件库，不依赖后端。
- **CAMLFront** —— JavaFX 界面，只负责展示和交互。

许可证：[GPL-3.0-only](LICENSE)

---

## 功能概览

### 已实现

| 模块 | 能力 |
|---|---|
| 元数据 | 解析 `version_manifest_v2.json`、单版本 JSON、字符串/带规则参数、libraries、asset index、downloads、javaVersion、logging、`jar`；处理 OS / 架构 / 版本正则 / features 规则；合并 `inheritsFrom` 父版本，子库优先 |
| 远程元数据 | 拉取版本清单与单版本 JSON，本地缓存，网络失败时回退缓存；请求地址遵循下载源设置 |
| 下载 | 客户端 jar、libraries、natives、asset index、assets objects 下载计划；镜像源 / 官方源 / 自定义镜像根地址三种下载源，覆盖版本清单、版本 JSON、Fabric/Forge/NeoForge/OptiFine 元数据、加载器 installer/API 和 Fabric Maven 依赖；SHA-1 校验；`.part` 临时文件 + 原子移动；已知大小的大文件使用 HTTP Range 分片下载，`.part.json` 检查点支持断点续传，Range 不可用时回退单流；响应体有 30 秒空闲读超时，元数据请求有总截止时间；显式 `DownloadCancellationToken`，失败或取消时取消剩余任务并等待 worker 收敛；默认总计最多 6 次下载尝试；并发下载数可设 1–256；已有有效文件跳过 |
| 版本安装 | 本地已安装版本直接复用（支持第三方启动器/整合包的自定义版本 ID），下载时可用自定义实例名；官方版本 ID 负责元数据，实例 ID 作为 `versions/<id>` 目录名；支持 Fabric Loader/Fabric API、Forge、NeoForge、OptiFine（仅 Forge）安装；Forge/NeoForge 版本列表优先从 BMCLAPI JSON 接口获取，官方源使用 Maven 元数据和 NeoForge 版本号映射匹配 Minecraft 版本；支持旧式 Forge library、Maven 路径推导和子版本库优先去重；缺失文件自动补齐；兼容 PCL/HMCL 版本隔离布局；基础文件完成后才写入版本 JSON |
| 账号 | 离线登录，稳定离线 UUID，生成 `AuthSession` |
| Java | 扫描 `java.home`、`JAVA_HOME`、`PATH` 及常见 JDK 目录；读取版本、供应商、架构、位数；按版本要求选择运行时；Windows 优先 `javaw.exe` |
| 启动 | 不可变 `LaunchPlan`、classpath 构建、JVM/游戏参数与 `${...}` 占位符、natives 子目录解压、Log4j 配置、旧版本 virtual assets 重建；NeoForge 26.2 + Vulkan 自动关闭 early window；根据设置选择，`--gameDir` 指向 `versions/<id>` 或 `.minecraft` 根目录 |
| 进程 | `ProcessBuilder` 启动、工作目录与环境变量、stdout/stderr 转发、Minecraft Log4j XML 转原版日志风格、退出码回调、停止能力；命令行过长时自动回退 `@argfile` / manifest classpath jar；`LaunchException` 分类错误并提供中文提示 |
| 多语言 | 独立 `CAMLLanguage` 模块；内置 `zh_cn`、`en_us`、`fn_cn`，启动时与 `~/.caml/lang` 中同名文件比对，不一致就按内置资源重写（用户新增的语言文件不受影响），之后从该目录读取；视图与控制器只引用翻译键，`Language.tr` 支持占位符和缺失键回退；语言下拉框中的语言名硬编码在 `Language.NATIVE_NAMES`，使用该语言自己的写法且不随界面语言变化；翻译文件由 `LanguageTranslationGenerator` 子类生成，`:CAMLLanguage:generateTranslation` 刷新 |

### 尚未实现

- 微软账号登录、token 刷新与安全存储
- CAMLFront 启动错误对话框、停止按钮、启动器隐藏 / 保留 / 关闭策略
- 崩溃分析、异常退出分类和真实 Minecraft 客户端端到端验证
- Quilt 加载器安装
- 前端下载取消入口与内容寻址缓存
- quick play、服务器直连、整合包与模组管理

---

## 项目结构

```text
CAML/
├─ CAMLBackend/                       纯 Java 核心（不依赖 JavaFX）
│  ├─ src/main/java/org/chs/camlauncher/backend/
│  │  ├─ api/         后端 facade 与状态 DTO
│  │  ├─ config/      配置与目录约定
│  │  ├─ metadata/    版本清单和 JSON；model/rule/parse/resolve 子包
│  │  ├─ instance/    实例模型、版本安装与版本隔离判定
│  │  ├─ download/    下载入口；model/plan/source/io/hash 子包
│  │  ├─ auth/        账号与离线登录
│  │  ├─ runtime/     Java 运行时检测与匹配
│  │  ├─ launch/      启动编排；command/classpath/prepare/process/patch 子包
│  │  ├─ loader/      Fabric/Forge/NeoForge/OptiFine 安装编排
│  │  ├─ theme/       主题目录、默认主题复制和当前主题配置
│  │  ├─ settings/    启动器设置持久化
│  │  ├─ system/      物理内存读取（Linux MemAvailable）
│  │  └─ json/        通用 JSON 文件读写
│  ├─ src/main/resources/themes/      打包的默认主题
│  └─ src/test/java/                  后端单元测试与集成测试
├─ CAMLUI/                            纯 JavaFX + CSS 的 CAML 组件库
│  ├─ src/main/java/org/chs/camlauncher/ui/
│  │  ├─ theme/       CamlTheme（安装结构样式、加载外部主题）
│  │  └─ control/     CamlButton、CamlCard、CamlTextField、CamlSwitch 等
│  └─ src/main/resources/org/chs/camlauncher/ui/caml.css（只含结构，不含配色）
├─ CAMLLanguage/                      纯 Java 多语言运行时、生成器与语言资源
│  ├─ src/main/java/org/chs/camlauncher/language/
│  │  ├─ Language.java                从 ~/.caml/lang 读取翻译
│  │  ├─ LanguageResources.java       同步内置 lang/*.json（不一致即重写）
│  │  ├─ LanguageTranslationGenerator.java 翻译生成器抽象类
│  │  ├─ TranslationTable.java        翻译键值表（实例 put，拒绝重复键）
│  │  ├─ TranslationGeneratorRunner.java 生成任务入口
│  │  └─ generator/                   ZHCNGenerator、ENUSGenerator、FNCNGenerator
│  ├─ src/main/resources/lang/        zh_cn.json、en_us.json、fn_cn.json
│  └─ src/test/java/                  LanguageTest
├─ CAMLFront/                         JavaFX 界面（不直接读写文件/发 HTTP/启动进程）
│  ├─ src/main/java/org/chs/camlauncher/front/
│  │  ├─ CAMLApplication.java         应用入口
│  │  ├─ WindowSizePlanner.java       窗口尺寸与居中计算
│  │  ├─ controller/
│  │  │  ├─ MainController.java       页面控制器组合入口
│  │  │  ├─ HomeController.java       启动、日志回调和实例扫描
│  │  │  ├─ DownloadsController.java  版本列表和实例安装
│  │  │  ├─ SettingsController.java   设置持久化和内存监视
│  │  │  └─ ControllerContext.java    后端访问、下载/设置线程和 UI 回调
│  │  └─ view/
│  │     ├─ MainView.java             应用壳层、顶部导航和页面宿主
│  │     ├─ HomeView.java             主页分栏与自适应
│  │     ├─ LaunchFormView.java       启动表单与启动交互
│  │     ├─ LogPaneView.java          运行日志面板
│  │     ├─ DownloadsView.java        下载页导航与内部页面切换
│  │     ├─ DownloadVersionsView.java 正式版/快照版/远古版列表
│  │     ├─ DownloadVersionSetupView.java 版本信息、Forge/OptiFine/Fabric/NeoForge 选择与下载
│  │     ├─ DownloadProgressView.java 下载进度、网速与阶段列表
│  │     ├─ DownloadTelemetry.java    下载事件聚合与网速计算
│  │     ├─ DownloadRequest.java      下载设置提交快照
│  │     ├─ SettingsView.java         设置页侧边栏
│  │     ├─ GameSettingsView.java     游戏设置分组
│  │     ├─ DownloadSettingsView.java 下载源与下载并发数设置
│  │     ├─ GameLocationSettingsView.java 游戏位置输入、浏览和保存
│  │     ├─ VersionIsolationSettingsView.java 版本隔离单选组
│  │     ├─ InstancePickerView.java   游戏实例二级列表
│  │     ├─ ManageInstancesView.java  管理实例空二级页
│  │     ├─ LauncherSettingsView.java 外观设置与主题
│  │     ├─ LaunchSettings.java       主页/设置页共享参数
│  │     ├─ ViewSupport.java          通用布局工具
│  │     └─ WorkspaceLayout.java      900px 断点布局规则
│  ├─ src/main/resources/styles/app.css
│  └─ src/test/java/                  前端布局与窗口尺寸测试
├─ design/                            设计文档
├─ gradle/                            版本目录与 Gradle wrapper
├─ build.gradle.kts
└─ settings.gradle.kts
```

### 架构约束

- `CAMLBackend` 是纯 Java，不 import JavaFX，也不依赖 `CAMLFront`。
- `CAMLUI` 只依赖 JavaFX 和自身 CSS，不依赖 `CAMLBackend` 或 `CAMLFront`。
- `CAMLUI` 不内置任何固定配色；所有颜色令牌来自外部主题文件。
- `CAMLLanguage` 是纯 Java 模块，不依赖 JavaFX、`CAMLBackend` 或 `CAMLFront`。
- `CAMLFront` 只通过 `CAMLUI` 搭建界面、通过 `CAMLLanguage` 读取文案，不直接读写文件、不直接发 HTTP、不直接启动进程。
- `CAMLFront` 不在视图里硬编码界面文案，统一通过 `Language.tr(key)` 读取 `~/.caml/lang/*.json`。
- `MainView` 只负责壳层和页面切换；各页面、表单、日志和设置内容独立成类。
- `MainController` 只组合 `HomeController`、`DownloadsController` 和 `SettingsController`，具体交互按页面拆分。
- 前后端通过 facade、DTO、事件和 `CompletableFuture` 交互。
- 网络、磁盘、进程和认证操作不得在 JavaFX Application Thread 执行。

---

## 技术栈

| 项目 | 版本 |
|---|---|
| Java toolchain | 25 |
| Gradle | 9.8.2 |
| JavaFX | 25.0.3 |
| CAMLUI | 项目内模块，纯 JavaFX + CSS |
| Gson | 2.15.0 |
| Log4j | 2.26.1 |
| JUnit | 6.1.3 |

---

## 构建与运行

需要本机安装 JDK 25。

```bash
# Windows
.\gradlew.bat build
.\gradlew.bat :CAMLFront:run

# Linux / macOS
./gradlew build
./gradlew :CAMLFront:run
```

运行测试：

```bash
# Windows
.\gradlew.bat :CAMLBackend:test
.\gradlew.bat :CAMLFront:test

# Linux / macOS
./gradlew :CAMLBackend:test
./gradlew :CAMLFront:test
```

### CAMLUI 组件库

`CAMLUI` 不引入第三方主题库。`caml.css` 只包含布局、字体层级、圆角和
控件结构，不包含任何固定颜色。颜色令牌由外部主题文件提供。

```java
CamlTheme.install(scene);
CamlTheme.applyTheme(scene, Path.of(System.getProperty("user.home"), ".caml/themes/mango.css"));

CamlCard card = new CamlCard(
        new Label("启动游戏"),
        CamlButton.filled("开始")
);
```

当前提供：`CamlTheme`、`CamlButton`、`CamlCard`、`CamlTextField`、`CamlTextArea`、
`CamlComboBox`、`CamlSpinner`、`CamlSwitch`、`CamlCheckBox`、`CamlSlider`、`CamlRadioButton`、
`CamlMemorySlider`、`CamlProgressBar`、`CamlSideNav`、`CamlSideNavItem`、`CamlTopNav`、
`CamlTopNavItem`、`CamlPageHost` 和 `CamlTopBar`。

界面布局、组件映射、无障碍和自审计见 [design/ui.md](design/ui.md)。

需要关闭页面切换和按钮缩放动画时，启动参数加 `-Dcaml.reduceMotion=true`。

### 多语言

前端不在代码里硬编码界面文案，而是通过独立 `CAMLLanguage` 模块读取翻译：

```text
CAMLLanguage/src/main/resources/lang/zh_cn.json
CAMLLanguage/src/main/resources/lang/en_us.json
CAMLLanguage/src/main/resources/lang/fn_cn.json
~/.caml/lang/zh_cn.json
~/.caml/lang/en_us.json
~/.caml/lang/fn_cn.json
```

每次启动时，`CAMLLanguage` 都会把内置语言文件与 `~/.caml/lang` 中的同名文件按翻译键值比对：
缺失或内容不一致（改过文案、缺键、JSON 损坏）时**按内置文件重写**，保证新版内置翻译和新增键总能生效。
`~/.caml/lang` 中**非内置**的语言文件不参与比对，始终原样保留，仍可自由新增或修改。

视图和控制器只引用翻译键：

```java
import org.chs.camlauncher.language.Language;

CamlButton launch = CamlButton.filled(Language.tr("home.launch"));
statusLabel.setText(Language.tr("home.instance.current", version));
```

- `Language.tr(key)` 找不到键时直接返回键名，方便发现遗漏；支持 `%s` / `%d` 等占位符。
- 当前语言按顺序取 `-Dcaml.language`、系统语言（`en*` → `en_us`，其余 → `Language.DEFAULT_LOCALE`）。
- 每次启动时 `CAMLLanguage` 把内置 `lang/*.json` 与 `~/.caml/lang` 的同名文件按键值比对，
  不一致（含 JSON 损坏）就按内置文件重写；非内置语言文件始终原样保留。
- 设置 → 外观 → 语言提供“自动 / 手动”两种模式；手动模式可从语言下拉框选择
  `zh_cn`、`en_us` 或 `fn_cn`。模式与手动语言分别保存在 `settings.properties` 的
  `languageMode` 和 `language`，切换后立即重建主视图刷新界面文案。
- 语言下拉框中的语言名不参与翻译，始终使用该语言自己的写法：`zh_cn` → `简体中文`、
  `en_us` → `English`、`fn_cn` → `地道中国话`。名称**硬编码**在 `Language.NATIVE_NAMES`，
  不写入也不读取翻译文件，因此切换界面语言、或翻译文件写错名称都不会改变下拉框内容；
  未登记的 locale 回退显示 locale 标识。新增内置语言时在 `Language.NATIVE_NAMES` 中登记自称。
- 语言文件**由代码生成**，不要手工编辑：每种语言实现一个继承
  `LanguageTranslationGenerator` 的生成器（`CAMLLanguage` 下的 `ZHCNGenerator`、
  `ENUSGenerator`、`FNCNGenerator`），在 `populate(TranslationTable)` 中用实例方法登记键和文案：

  ```java
  @Override
  protected void populate(TranslationTable t) {
      t.put("home.launch", "启动游戏");
      t.put("home.instance.current", "当前实例：%s");
  }
  ```

- 新增语言只需在 `generator` 下添加一个生成器、在 `TranslationGeneratorRunner` 中登记，
  并在 `Language.NATIVE_NAMES` 中补充该语言的自称（语言名不写进翻译文件）。

### 主题系统

主题是普通的 JavaFX CSS 文件，放在 `~/.caml/themes/*.css`。启动器本体不定义
任何固定主题；打包时只携带一个白底默认主题：

```text
CAMLBackend/src/main/resources/themes/mango.css
```

首次启动时，后端把默认主题 mango 复制到 `~/.caml/themes/mango.css`。之后用户可以直接
修改它，或新增其他 `.css` 文件。设置 → 外观中的“主题”下拉框会列出主题目录中的文件，
当前选择记录在 `~/.caml/theme.txt`。

主题文件只需要覆盖 `.caml-root` 下的颜色令牌，例如：

```css
.caml-root {
    -caml-accent: #f99e1a;
    -caml-background: #ffffff;
    -caml-surface-container: #f0f0f0;
    -caml-on-surface: #1a1a1a;
}
```

### IDEA 运行配置

项目根目录的 `.run/` 提供三个共享运行配置：

| 配置 | 作用 |
|---|---|
| `runLauncher` | 执行 `:CAMLFront:run`，可直接使用 IDEA 调试 JavaFX 前端 |
| `build` | 执行 Gradle `build`，构建后端、前端和测试 |
| `generateTranslation` | 执行 `:CAMLLanguage:generateTranslation`，重新生成 `lang/*.json` |

`build` 当前只生成 Gradle 构建产物，还不是单文件启动器。单文件打包需要先确定 Windows/Linux 目标，再接入 `jpackage`。

---

## 默认目录

| 用途 | 位置 |
|---|---|
| 启动器数据 | `~/.caml` |
| 版本清单缓存 | `~/.caml/cache/version_manifest_v2.json` |
| 启动器 UI 日志 | `~/.caml/logs/latest.log`（每次关闭刷新保留；启动器错误、游戏 `[ERR]` 或异常退出时额外生成 `.log.gz`） |
| 外部主题 | `~/.caml/themes/*.css` |
| 当前主题 | `~/.caml/theme.txt` |
| 外部语言文件 | `~/.caml/lang/*.json` |
| 启动器设置 | `~/.caml/settings.properties` |
| 游戏数据（Linux / macOS） | `~/.minecraft` |
| 游戏数据（Windows） | 启动器所在目录下的 `.minecraft` |

游戏目录在 **设置 → 游戏设置 → 游戏位置** 中修改；输入或浏览后必须点击保存才会生效。
版本隔离在 **设置 → 游戏设置 → 版本隔离** 中选择，修改后立即生效并写入设置文件。
主页的 **浏览游戏实例** 会扫描当前游戏位置并列出 `versions/<id>/<id>.json`。

### 版本隔离

设置页提供两个选项：

- **完全版本隔离**：`--gameDir` 始终指向 `versions/<id>`，这是默认值。
- **不进行版本隔离**：`--gameDir` 始终指向 `.minecraft` 根目录。

选择会影响下一次启动。切换模式不会自动搬运已有存档；从“不进行版本隔离”切换到
“完全版本隔离”后，旧存档仍留在 `.minecraft/saves`，新存档写入
`versions/<id>/saves`。需要沿用旧存档时应手动复制。

后端仍保留对 PCL/HMCL 隔离布局的自动检测重载，用于旧 API 和目录扫描；界面启动
使用用户选择的显式模式，不再根据版本目录中是否已有 `saves` 来猜测。

自动检测会检查 `versions/<id>` 中是否存在：

```text
saves
options.txt
logs
mods
config
resourcepacks
shaderpacks
usercache.json
command_history.txt
```

无论选择哪种模式，`libraries`、`assets` 都来自 `.minecraft` 根目录；Windows 的
`APPDATA` 也始终按 `.minecraft` 根目录计算。主页会根据当前选择显示实际存档位置。

---

## 使用流程

1. 启动 CAML，主界面显示后端状态与数据目录。
2. 首次使用时进入 **设置 → 游戏设置 → 游戏位置**，输入或浏览目录并点击保存。
3. 回到主页，点击 **浏览游戏实例**，从当前游戏位置扫描出的列表中选择实例。
4. 填写 **离线用户名**；如需调整主题或内存，进入 **设置**。
5. 点击 **启动游戏**。
   - 本地已有该版本时直接复用，不访问官方清单。
   - 本地没有该版本时，从官方清单安装并补齐文件。

版本 ID 即 `versions/` 下的文件夹名，因此第三方启动器/整合包的自定义名称
（例如 `1.21.4-DeepSeek`）可以直接选中并启动。

---

## 后端 API

主要入口：`org.chs.camlauncher.backend.api.CAMLBackend`

```java
CAMLBackend backend = new CAMLBackend(BackendConfig.defaults());
backend.start();

InstalledVersion installed = backend.installVersion("1.21.5", DownloadListener.NONE);

AuthSession auth = backend.loginOffline("Steve");

JavaRuntime java = backend.javaLocator()
        .findSuitable(JavaRequirement.from(installed.manifest()))
        .orElseThrow();

LaunchPlan plan = backend.createLaunchPlan(installed, auth, java, LaunchOptions.defaults());

List<Path> classpath = backend.classpathBuilder().build(plan);
backend.nativeExtractor().extract(plan);
plan = backend.virtualAssetInstaller().prepare(plan);

GameProcess process = backend.processLauncher().launch(plan, classpath, listener);

// 一步启动（可选指定游戏目录）
GameLaunch launch = backend.launchGame(
        "1.21.5", gameDirectory, "Steve", LaunchOptions.defaults(), listener, processListener);
```

界面侧的目录探测同样由后端完成：

```java
GameDirectoryInfo info = backend.inspectGameDirectory(Path.of("D:/games/.minecraft"));
info.directoryExists();
info.installedVersions();
info.isolatedVersions();
```

---

## 文档

- [总体架构](design/architecture.md)
- [界面设计](design/ui.md)
- [版本下载与启动链路](design/version-download-and-launch.md)
- [runtime 包](design/runtime.md)
- [当前实现情况](design/Implement.md)

---

## 已知问题

以下后端测试在当前 Windows 开发环境下会失败，均与前端和翻译无关：

- 使用 POSIX 路径作为断言，Windows 路径分隔符不同：
  - `LaunchCommandBuilderTest#placesMainClassBetweenJvmAndGameArguments`
  - `LaunchArgumentsBuilderTest#buildsJvmArgumentsWithMemoryDefaultsAndClasspath`
- 端到端/加载器测试依赖子进程，当前 JDK 25 下子进程启动 `FakeForgeInstallerMain` 失败
  （`LinkageError`），并导致 `GameLaunchServiceTest#installsAndLaunchesFakeClientEndToEnd`
  的退出码断言不符：
  - `ExternalLoaderInstallerTest`（3 个用例）
  - `GameLaunchServiceTest#installsAndLaunchesFakeClientEndToEnd`
- 文本比较未归一化换行符：
  - `NeoForge262VulkanFixTest#createsFmlConfigWhenMissing`

前端测试（含 `LanguageTest` 与 `DownloadTelemetryTest`）在本环境全部通过。

---

## 分支

| 分支 | 说明 |
|---|---|
| `main` | 主分支，保持可构建状态 |
| `crimson` | CrimsonSirius 的开发分支 |
| `namelessgod2008` | namelessgod2008 的开发分支 |

---

## 协作约定

本项目为双人合作。

| 成员 | 提交邮箱 | 分支 |
|---|---|---|
| CrimsonSirius | `227993016@qq.com` | `crimson` |
| namelessgod2008 | `3505488164@qq.com` | `namelessgod2008` |

- 各自在自己的分支开发，通过 Pull Request 合入 `main`。
- 提交前请确认仓库级 Git 身份正确，避免提交错记为他人。
- 仓库根目录的 `.mailmap` 用于统一作者邮箱；
  namelessgod2008 早期的 GitHub noreply 地址已归并到主邮箱，
  使贡献者统计保持为两人。`.mailmap` 只影响展示，不改写历史提交。

---

## 贡献者

- [CrimsonSirius](https://github.com/CrimsonSirius)
- [namelessgod2008](https://github.com/namelessgod2008)

---

## 许可证

本项目基于 [GPL-3.0-only](LICENSE) 发布