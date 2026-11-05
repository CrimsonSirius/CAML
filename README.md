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
| 元数据 | 解析 `version_manifest_v2.json`、单版本 JSON、字符串/带规则参数、libraries、asset index、downloads、javaVersion、logging；处理 OS / 架构 / 版本正则 / features 规则；合并 `inheritsFrom` 父版本 |
| 远程元数据 | 拉取官方版本清单与单版本 JSON，本地缓存，网络失败时回退缓存 |
| 下载 | 客户端 jar、libraries、natives、asset index、assets objects 下载计划；SHA-1 校验；`.part` 临时文件 + 原子移动；失败重试；已有有效文件跳过 |
| 版本安装 | 本地已安装版本直接复用（支持第三方启动器/整合包的自定义版本 ID），缺失文件自动补齐；兼容 PCL/HMCL 版本隔离布局；基础文件完成后才写入版本 JSON |
| 账号 | 离线登录，稳定离线 UUID，生成 `AuthSession` |
| Java | 扫描 `java.home`、`JAVA_HOME`、`PATH` 及常见 JDK 目录；读取版本、供应商、架构、位数；按版本要求选择运行时；Windows 优先 `javaw.exe` |
| 启动 | 不可变 `LaunchPlan`、classpath 构建、JVM/游戏参数与 `${...}` 占位符、natives 解压、Log4j 配置、旧版本 virtual assets 重建；版本隔离时 `--gameDir` 自动指向 `versions/<id>` |
| 进程 | `ProcessBuilder` 启动、工作目录与环境变量、stdout/stderr 转发、退出码回调、停止能力；命令行过长时自动回退 `@argfile` / manifest classpath jar；`LaunchException` 分类错误并提供中文提示 |
| 界面 | CAML 组件库；外部 CSS 主题（`~/.caml/themes`）；顶部“主页 / 下载 / 设置”导航；窗口尺寸按主屏可用区域自适应并居中（小屏收缩、大屏限幅）；下载页空白侧边栏；设置页“游戏设置 / 启动器设置”侧边栏；内存滑块显示游戏/可用/总内存；宽屏左右分栏、窄屏上下堆叠；后端状态、游戏位置选择、已安装版本列表、存档位置提示、离线用户名、启动按钮、状态提示、日志面板 |

### 尚未实现

- 微软账号登录、token 刷新与安全存储
- Fabric / Quilt / Forge / NeoForge 等加载器安装
- CAMLFront 启动错误对话框、停止按钮、启动器隐藏 / 保留 / 关闭策略
- 崩溃分析、异常退出分类和真实 Minecraft 客户端端到端验证
- 下载页的实际功能（当前为空白导航页）
- 镜像源、并发下载、取消与内容寻址缓存
- quick play、服务器直连、整合包与模组管理

---

## 项目结构

```text
CAML/
├─ CAMLBackend/                       纯 Java 核心（不依赖 JavaFX）
│  ├─ src/main/java/org/chs/camlauncher/backend/
│  │  ├─ api/         后端 facade 与状态 DTO
│  │  ├─ config/      配置与目录约定
│  │  ├─ metadata/    版本清单、版本 JSON、规则、继承合并
│  │  ├─ instance/    实例模型、版本安装与版本隔离判定
│  │  ├─ download/    下载源、下载计划、SHA-1、原子落盘
│  │  ├─ auth/        账号与离线登录
│  │  ├─ runtime/     Java 运行时检测与匹配
│  │  ├─ launch/      LaunchPlan、classpath、参数、进程启动
│  │  ├─ theme/       主题目录、默认主题复制和当前主题配置
│  │  └─ json/        通用 JSON 文件读写
│  ├─ src/main/resources/themes/      打包的默认主题
│  └─ src/test/java/                  后端单元测试与集成测试
├─ CAMLUI/                            纯 JavaFX + CSS 的 CAML 组件库
│  ├─ src/main/java/org/chs/camlauncher/ui/
│  │  ├─ theme/       CamlTheme（安装结构样式、加载外部主题）
│  │  └─ control/     CamlButton、CamlCard、CamlTextField、CamlSwitch 等
│  └─ src/main/resources/org/chs/camlauncher/ui/caml.css（只含结构，不含配色）
├─ CAMLFront/                         JavaFX 界面（不直接读写文件/发 HTTP/启动进程）
│  ├─ src/main/java/org/chs/camlauncher/front/
│  │  ├─ CAMLApplication.java         应用入口
│  │  ├─ WindowSizePlanner.java       窗口尺寸与居中计算
│  │  ├─ controller/                  MainController
│  │  └─ view/
│  │     ├─ MainView.java             主界面与页面导航
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
- `CAMLFront` 只通过 CAMLUI 组件搭建界面，不直接读写文件、不直接发 HTTP、不直接启动进程。
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
CamlTheme.applyTheme(scene, Path.of(System.getProperty("user.home"), ".caml/themes/default.css"));

CamlCard card = new CamlCard(
        new Label("启动游戏"),
        CamlButton.filled("开始")
);
```

当前提供：`CamlTheme`、`CamlButton`、`CamlCard`、`CamlTextField`、`CamlTextArea`、
`CamlComboBox`、`CamlSpinner`、`CamlSwitch`、`CamlCheckBox`、`CamlSlider`、
`CamlProgressBar`、`CamlSideNav`、`CamlSideNavItem`、`CamlTopNav`、
`CamlTopNavItem`、`CamlPageHost` 和 `CamlTopBar`。

界面布局、组件映射、无障碍和自审计见 [design/ui.md](design/ui.md)。

需要关闭页面切换和按钮缩放动画时，启动参数加 `-Dcaml.reduceMotion=true`。

### 主题系统

主题是普通的 JavaFX CSS 文件，放在 `~/.caml/themes/*.css`。启动器本体不定义
任何固定主题；打包时只携带一个白底默认主题：

```text
CAMLBackend/src/main/resources/themes/default.css
```

首次启动时，后端把默认主题复制到 `~/.caml/themes/default.css`。之后用户可以直接
修改它，或新增其他 `.css` 文件。设置 → 启动器设置中的“主题”下拉框会列出主题目录中的文件，
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

项目根目录的 `.run/` 提供两个共享运行配置：

| 配置 | 作用 |
|---|---|
| `runLauncher` | 执行 `:CAMLFront:run`，可直接使用 IDEA 调试 JavaFX 前端 |
| `build` | 执行 Gradle `build`，构建后端、前端和测试 |

`build` 当前只生成 Gradle 构建产物，还不是单文件启动器。单文件打包需要先确定 Windows/Linux 目标，再接入 `jpackage`。

---

## 默认目录

| 用途 | 位置 |
|---|---|
| 启动器数据 | `~/.caml` |
| 版本清单缓存 | `~/.caml/cache/version_manifest_v2.json` |
| 启动器日志 | `~/.caml/logs/caml.log` |
| 外部主题 | `~/.caml/themes/*.css` |
| 当前主题 | `~/.caml/theme.txt` |
| 游戏数据（Linux / macOS） | `~/.minecraft` |
| 游戏数据（Windows） | 启动器所在目录下的 `.minecraft` |

游戏目录可以在界面中切换：默认使用上述路径，也可以通过 **游戏位置 → 浏览...** 选择本地已有目录。
选择后会扫描 `versions/<id>/<id>.json` 列出已安装版本，可直接启动。

### 版本隔离

CAML 兼容 PCL/HMCL 等启动器的版本隔离布局。后端会检查 `versions/<id>` 中是否存在：

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

命中任意一项时：

- `--gameDir` 指向 `versions/<id>`，以便读取原有存档和设置。
- `libraries`、`assets` 仍然来自 `.minecraft` 根目录，不会重复下载。
- 界面在版本框下方显示“版本隔离：存档在 `versions/<id>/saves`”。
- Windows 的 `APPDATA` 仍按 `.minecraft` 根目录计算，不会误指向版本目录。

---

## 使用流程

1. 启动 CAML，主界面显示后端状态与数据目录。
2. 在 **游戏位置** 选择默认路径或手动添加本地游戏目录。
3. 选择或输入 **游戏版本**、填写 **离线用户名**。
4. 如需调整主题或内存，进入 **设置 → 启动器设置 / 游戏设置**。
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

- 部分测试用例使用 POSIX 路径（如 `/tmp/a.jar`）作为断言，在 Windows 上会因路径分隔符不同而失败：
  - `LaunchCommandBuilderTest#placesMainClassBetweenJvmAndGameArguments`
  - `LaunchArgumentsBuilderTest#buildsJvmArgumentsWithMemoryDefaultsAndClasspath`

  这与功能实现无关，属于测试本身的跨平台问题。

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

本项目基于 [GPL-3.0-only](LICENSE) 发布。
