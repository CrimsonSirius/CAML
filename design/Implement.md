# CAML 当前实现情况

更新日期：2026-11-03

## 1. 总体状态

当前项目已经具备一条可用的**原版安装链路**，但还没有真正启动 Minecraft。

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
```

未实现：

```text
classpath
→ JVM/游戏参数
→ 占位符替换
→ natives 解压
→ 进程启动
→ 日志监控
```

## 2. 项目结构

```text
CAML
├─ CAMLBackend   纯 Java 核心
└─ CAMLFront    JavaFX 界面
```

构建：

- Java 25 toolchain。
- Gradle 9.8.2。
- Gson `2.15.0`。
- Log4j `2.26.1`。
- JavaFX `25.0.3`。
- JUnit `6.1.3`。

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
- 支持本地已安装父版本。
- 下载完成后才写入版本 JSON。
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

### 3.10 JavaFX 界面

- 启动时初始化后端。
- 显示后端状态、版本、数据目录和游戏目录。
- 样式文件：`CAMLFront/src/main/resources/styles/app.css`。

### 3.11 日志

- Log4j 控制台输出。
- 文件日志：`~/.caml/logs/caml.log`。

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
- 34 个后端测试全部通过。
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

## 7. 当前未实现

- natives 解压。
- 游戏 Log4j 配置写入。
- 游戏进程启动。
- stdout/stderr 和退出码监控。
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
