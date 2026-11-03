# CAML 总体架构

更新日期：2026-11-03

## 1. 架构目标

CAML 采用前后端分离的多模块结构：

```text
CAMLFront   JavaFX 界面，只负责展示和交互
CAMLBackend Java 核心，负责元数据、下载、账号、Java 和启动
```

核心约束：

- `CAMLBackend` 是纯 Java，不 import JavaFX。
- `CAMLBackend` 不依赖 `CAMLFront`。
- `CAMLFront` 不直接读写文件、不直接发 HTTP、不直接启动进程。
- 前后端通过 facade、DTO、事件和 `CompletableFuture` 交互。

## 2. 后端模块职责

| 包 | 职责 | 当前状态 |
|---|---|---|
| `api` | 后端 facade 和状态 DTO | 已有 |
| `config` | 配置、目录约定和路径初始化 | 已有 |
| `metadata` | 版本清单、版本 JSON、参数、规则、库、继承合并 | 已有 |
| `instance` | 实例模型和版本安装编排 | 已有基础能力 |
| `download` | 下载源、下载计划、SHA-1、临时文件和原子落盘 | 已有基础能力 |
| `auth` | 账号、AuthSession、离线登录 | 已有离线能力 |
| `runtime` | Java 运行时检测、版本/架构/位数匹配 | 已有 |
| `launch` | LaunchPlan、classpath、参数、进程启动 | 仅有占位 |
| `loader` | Fabric、Quilt、Forge、NeoForge 等加载器 | 未实现 |
| `json` | 通用 JSON 文件读写 | 已有 |

## 3. 当前已实现能力

### 3.1 元数据

- 解析 `version_manifest_v2.json`。
- 解析单版本 JSON。
- 解析字符串参数和带规则参数。
- 解析 libraries、asset index、downloads、javaVersion 和 logging。
- 处理 OS、架构、版本正则和 features 规则。
- 合并 `inheritsFrom` 父版本。
- 拉取远程版本清单和单版本 JSON。
- 版本清单缓存和网络失败回退。

### 3.2 下载

- 生成客户端 jar、libraries、natives、asset index 下载计划。
- 生成 assets objects 下载计划。
- 使用 SHA-1 校验。
- 使用 `.part` 临时文件和原子移动。
- 下载重试。
- 已有有效文件时跳过。

### 3.3 实例安装

- 按版本 ID 安装原版。
- 基础文件下载完成后才写入版本 JSON。
- 返回 `InstalledVersion`，包含后续启动需要的路径。

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

### 3.7 classpath

- 按规则筛选 libraries。
- 排除 native-only library。
- 按目标路径去重并保持顺序。
- 客户端 jar 追加在最后。
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
- 跳过 `META-INF/` 和 `extract.exclude` 指定内容。
- 下载游戏 Log4j 配置文件。
- 在 JVM 参数中注入 `-Dlog4j.configurationFile`。
- 旧版本 asset index 标记为 virtual 时重建 `assets/virtual/<id>`。
- `map_to_resources` 时同步写入 `resources`。

## 4. 尚未实现的能力

- 游戏进程启动和日志监控。
- 微软账号登录。
- token 刷新和安全存储。
- Fabric、Quilt、Forge、NeoForge 等加载器安装。
- 镜像源、并发下载、取消和内容寻址缓存。
- 整合包和模组管理。

## 5. 数据与安全原则

- 版本元数据以官方 `version_manifest_v2.json` 和单版本 JSON 为准。
- 下载必须校验 SHA-1，先写临时文件，校验通过后再原子移动。
- 缓存和实例目录必须有明确边界。
- 启动器数据默认放在 `~/.caml`。
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
```

## 7. 相关文档

- 版本下载和启动链路详见 [version-download-and-launch.md](version-download-and-launch.md)。
- runtime 包详见 [runtime.md](runtime.md)。
- 当前实现情况详见 [Implement.md](Implement.md)。
- 开发任务和阶段计划由本地 `TODO.md` 维护。
