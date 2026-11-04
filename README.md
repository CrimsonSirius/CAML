# CAML

CAML 是一个基于 **Java 25 + JavaFX** 的 Minecraft 启动器，采用前后端分离的多模块结构。

- **CAMLBackend** —— 纯 Java 核心，负责元数据、下载、账号、Java 检测与游戏启动。
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
| 版本安装 | 本地已安装版本直接复用（支持第三方启动器/整合包的自定义版本 ID），缺失文件自动补齐；基础文件完成后才写入版本 JSON |
| 账号 | 离线登录，稳定离线 UUID，生成 `AuthSession` |
| Java | 扫描 `java.home`、`JAVA_HOME`、`PATH` 及常见 JDK 目录；读取版本、供应商、架构、位数；按版本要求选择运行时；Windows 优先 `javaw.exe` |
| 启动 | 不可变 `LaunchPlan`、classpath 构建、JVM/游戏参数与 `${...}` 占位符、natives 解压、Log4j 配置、旧版本 virtual assets 重建 |
| 进程 | `ProcessBuilder` 启动、工作目录与环境变量、stdout/stderr 转发、退出码回调、停止能力；命令行过长时自动回退 `@argfile` / manifest classpath jar；`LaunchException` 分类错误并提供中文提示 |
| 界面 | 后端状态、游戏位置选择、版本、离线用户名、最大内存、状态提示、日志窗口 |

### 尚未实现

- 微软账号登录、token 刷新与安全存储
- Fabric / Quilt / Forge / NeoForge 等加载器安装
- 启动器隐藏 / 保留 / 关闭策略、崩溃分析与异常退出分类
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
│  │  ├─ instance/    实例模型与版本安装
│  │  ├─ download/    下载源、下载计划、SHA-1、原子落盘
│  │  ├─ auth/        账号与离线登录
│  │  ├─ runtime/     Java 运行时检测与匹配
│  │  ├─ launch/      LaunchPlan、classpath、参数、进程启动
│  │  └─ json/        通用 JSON 文件读写
│  └─ src/test/java/                  后端单元测试与集成测试
├─ CAMLFront/                         JavaFX 界面（不直接读写文件/发 HTTP/启动进程）
│  ├─ src/main/java/org/chs/camlauncher/front/
│  │  ├─ CAMLApplication.java         应用入口
│  │  ├─ controller/                  MainController
│  │  └─ view/                        MainView
│  └─ src/main/resources/styles/app.css
├─ design/                            设计文档
├─ gradle/                            版本目录与 Gradle wrapper
├─ build.gradle.kts
└─ settings.gradle.kts
```

### 架构约束

- `CAMLBackend` 是纯 Java，不 import JavaFX，也不依赖 `CAMLFront`。
- `CAMLFront` 不直接读写文件、不直接发 HTTP、不直接启动进程。
- 前后端通过 facade、DTO、事件和 `CompletableFuture` 交互。
- 网络、磁盘、进程和认证操作不得在 JavaFX Application Thread 执行。

---

## 技术栈

| 项目 | 版本 |
|---|---|
| Java toolchain | 25 |
| Gradle | 9.8.2 |
| JavaFX | 25.0.3 |
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

运行后端测试：

```bash
.\gradlew.bat :CAMLBackend:test
```

---

## 默认目录

| 用途 | 位置 |
|---|---|
| 启动器数据 | `~/.caml` |
| 版本清单缓存 | `~/.caml/cache/version_manifest_v2.json` |
| 启动器日志 | `~/.caml/logs/caml.log` |
| 游戏数据（Linux / macOS） | `~/.minecraft` |
| 游戏数据（Windows） | 启动器所在目录下的 `.minecraft` |

游戏目录可以在界面中切换：默认使用上述路径，也可以通过 **游戏位置 → 浏览...** 选择本地已有目录。
选择后会扫描 `versions/<id>/<id>.json` 列出已安装版本，可直接启动。

---

## 使用流程

1. 启动 CAML，主界面显示后端状态与数据目录。
2. 在 **游戏位置** 选择默认路径或手动添加本地游戏目录。
3. 选择或输入 **游戏版本**、填写 **离线用户名**、设置 **最大内存**。
4. 点击 **启动游戏**。
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
```

---

## 文档

- [总体架构](design/architecture.md)
- [版本下载与启动链路](design/version-download-and-launch.md)
- [runtime 包](design/runtime.md)
- [当前实现情况](design/Implement.md)

---

## 已知问题

- 部分测试用例使用 POSIX 路径（如 `/tmp/a.jar`）作为断言，在 Windows 上会因路径分隔符不同而失败：
  - `LaunchCommandBuilderTest#placesMainClassBetweenJvmAndGameArguments`
  - `LaunchArgumentsBuilderTest#buildsJvmArgumentsWithMemoryDefaultsAndClasspath`
  - `GameLaunchServiceTest#installsAndLaunchesFakeClientEndToEnd`

  这与功能实现无关，属于测试本身的跨平台问题。

---

## 分支

| 分支 | 说明 |
|---|---|
| `main` | 主分支 |
| `crimson` | 开发分支 |
| `namelessgod2008` | 个人开发分支 |

---

## 许可证

本项目基于 [GPL-3.0-only](LICENSE) 发布。

