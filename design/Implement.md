# CAML 当前实现情况

> 本文按当前代码与构建产物重写，最后核对时间：2026-11-27；基线提交：`5d56e08`。

## 1. 总体状态

CAML 已经覆盖一条可运行的完整链路：

```text
启动器初始化
  → 版本清单/版本 JSON
  → 原版与加载器安装
  → Java 选择/检测/下载
  → 账号登录与实例账号解析
  → 资源与 natives 准备
  → classpath/参数/子进程启动
  → 进程日志与退出结果
```

同时具备：

- Forge、NeoForge、Fabric、Quilt、OptiFine 组合安装；
- 多来源 Mod/光影/资源包/地图搜索与下载；
- Modrinth `.mrpack` 整合包搜索与安装为新实例；
- Microsoft/Yggdrasil/离线账号；
- Java 扫描、手动添加、Foojay 托管下载；
- 外部 CSS 主题和 11 种内置语言；
- Linux deb/rpm、Windows EXE/便携 ZIP 打包。

## 2. 工程结构

```text
CAML/
├─ CAMLBackend/    纯 Java 核心
├─ CAMLUI/         JavaFX 控件库与结构 CSS
├─ CAMLLanguage/   语言契约、生成器、JSON
├─ CAMLFront/      JavaFX 应用、控制器、页面
├─ packaging/      图标、许可证、Linux/Windows 打包资源
├─ gradle/         packaging.gradle.kts、版本目录
└─ design/         设计/实现文档
```

## 3. 后端实现

### 3.1 Facade 和生命周期

`CAMLBackend` 是组合根，负责构造服务、启动/关闭和维护状态；领域调用通过六个 facade 暴露：
`settings()`、`java()`、`accounts()`、`downloads()`、`instances()`、`launch()`。

当前对外能力包括：

- 状态、主题、设置、系统/设备信息；
- 版本清单、下载源、下载器和安装器；
- Java 扫描、手动添加、下载安装和选择；
- 实例检查、文件夹打开、Mod/资源包开关和更新；
- 账号登录、默认账号、实例账号和 authlib-injector；
- 启动计划、classpath、参数和进程启动；
- 语言快照和语言选择；
- xrandr 启动预检。

### 3.2 元数据与规则

- `MetadataService` 用 Gson 解析版本清单、版本 JSON、asset index 和规则参数。
- `ManifestResolver` 完成 `inheritsFrom` 合并。
- `RuleEvaluator` 支持 OS 名称/架构/版本正则和 features 规则；最后一个匹配规则生效。
- 版本 JSON SHA-1 校验在下载阶段完成。

### 3.3 下载与安装

- 三种下载源；镜像失败自动回退原始地址。
- 下载器已拆分为 `FileDownloader` 重试入口、`DownloadAttempt`、`SingleStreamDownload`、`SegmentedDownload`、`SegmentResumeState` 和 `DownloadIo`。
- 支持分段 Range、`.part.json` 续传、SHA-1/自定义校验、原子移动、取消清理。
- `DownloadExecutor` 聚合批量进度；未知大小从实际响应动态补齐。
- 原版本安装支持自定义实例 ID、本地版本优先、父版本继承。
- 加载器安装支持 Fabric/Quilt Meta profile、Forge/NeoForge installer、OptiFine Forge 组合。
- Fabric/Quilt/OptiFine/Forge/NeoForge 共用实例锁、共享库事务和版本目录 staging；失败会回滚库并清理版本目录。
- Forge/NeoForge 优先提取 installer 内嵌 Maven 库；缺失 URL 的库按 Forge/NeoForge 坐标回退正确仓库。
- 安装器库首轮 6 次下载失败后，每 5 秒按候选源重试一轮，直到成功或取消；进度统计不会重复累计整批字节。
- Modrinth 与地图解压使用条目数、单条目大小和总解压量限制。

### 3.4 启动

- `LaunchPlanner` 生成不可变 `LaunchPlan`。
- `ClasspathBuilder` 完成规则过滤、去重、client jar 顺序和 OptiFine `anb.class` 兼容处理。
- `JvmArgumentsBuilder` / `GameArgumentsBuilder` 处理新旧参数格式、规则和占位符。
- `NativeExtractor` 清理并解压当前平台 natives。
- `VirtualAssetInstaller` 处理旧版 virtual assets/map_to_resources。
- `ProcessLauncher` 启动子进程，Java 9+ 支持 argfile，命令行过长时再回退 manifest classpath jar。
- `GameProcess` 异步读日志、回调退出码、在退出后清理临时文件。
- Java 主版本不匹配时阻塞确认，可选择停止或强制启动；强制启动仍使用最接近的可用运行时。

### 3.5 账号

- 离线账号稳定 UUID。
- Microsoft OAuth 设备码 + Xbox/XSTS/Minecraft Services。
- Yggdrasil authenticate/refresh/validate、多角色切换和 `selectedProfile` 修复；每次启动 refresh 后把最新角色名与角色列表写回账号，角色 id 统一规范化为 32 位小写十六进制。
- accessToken/refreshToken AES-GCM 加密落盘；密码不保存。
- 实例账号优先于全局默认账号；游戏位置变化后可检测并一键套用旧位置的实例绑定。
- authlib-injector 1.2.5 自动缓存、SHA-256 校验、可覆盖。

### 3.6 Java

- 扫描 `java.home`、`JAVA_HOME`、PATH、常见 JDK 目录、`~/.caml/java` 嵌套托管仓库。
- Java 要求来自版本 JSON，缺省 8。
- 精确主版本优先。
- 托管仓库安装到 `~/.caml/java/<platform>/<name>`。
- Foojay Disco 下载六种发行版，支持 JDK/JRE，SHA-1/256/512 校验，zip/tar.gz 解压。

### 3.7 内容服务

- Modrinth/CurseForge 搜索、详情、版本和文件。
- MOD/SHADER/RESOURCE_PACK/WORLD 四类内容；地图下载后解压到 `saves/<世界名>`。
- SHA-1；CurseForge 可回退 MD5。
- Mod API 图标下载和 WebP→PNG 缓存。
- 本地 Mod 更新检查：Modrinth hash、CurseForge MurmurHash2 fingerprint。

### 3.8 设置、语言、主题、日志

- 设置写入串行化、临时文件 + 原子替换；损坏 properties 会隔离后恢复默认值。
- 内置语言由生成器生成并随启动器同步；支持四类回退。
- 内置主题从 classpath 复制到数据目录，不覆盖用户主题；`theme.txt` 和首次主题复制使用原子写入。
- UI 日志异步写盘；失败时退出生成压缩归档。

## 4. 前端实现

### 4.1 页面

- 主页：启动表单、日志、实例选择、实例管理、实例账号。
- 任务页：任务列表、状态、进度和详情；点击任务复用下载进度页。
- 下载页：正式/快照/远古/Mod/光影/资源包/地图；版本设置、进度、内容搜索/详情。
- 设置页：游戏、Java 管理、下载、账号、外观、关于。

### 4.2 线程模型

`ControllerContext` 提供独立线程池：

- 下载 1；
- 设置 1；
- 检查 1；
- 元数据 6；
- 图标 8；
- 图标重试 4；
- Java 1；
- 启动 1。

所有阻塞操作离开 JavaFX 线程；回调经 `Platform.runLater` 回到 UI。

### 4.3 任务

- `AsyncTaskHandle<T>`：可取消后台任务。
- `DownloadTask`：跨页面保留标题、主题、遥测、token 和取消动作。
- `DownloadTelemetry`：汇总阶段、速度、字节、文件数，处理未知大小与跨下载计划累计。
- 顶部主导航为：主页 / 下载 / 任务 / 设置。
- 任务中心列表每 250ms 刷新；详情页复用下载进度界面，支持取消和返回任务列表。

### 4.4 缩放与布局

- 设计基准 `1440×900`、根字号 16px。
- 因子 0.6875–1.375，量化为整数根字号 11–22px。
- 工作区 900px 断点；模组表单 660/520px 断点；下载侧栏窗口宽度 1/6、180–260 设计像素。
- 下载进度页固定标题栏，正文独立滚动。

### 4.5 无障碍和焦点

- 按钮/输入有可见焦点反馈。
- 单选项 `CamlRadioButton` 不进入焦点遍历，避免额外焦点环。
- 页面切换会处理焦点丢失。
- 命令块 `CamlSelectableText` 原生不可滚动，支持选择、复制和反色选中文本。

## 5. 多语言实现

- 内置 locale：`zh_cn`、`zh_tw`、`en_us`、`fn_cn`、`ja_jp`、`ko_kr`、`fr_fr`、`de_de`、`ru_ru`、`la_la`、`bo_cn`。
- 自动检测：中文区分简繁，日/韩/法/德/俄映射对应 locale，其他语言默认英语；拉丁文/藏文不参与自动检测。
- 运行时语言文件：`~/.caml/lang/*.json`。
- 生成器：`CAMLLanguage/src/main/java/.../generator/lang/*Generator.java`。
- 生成命令：`./gradlew :CAMLLanguage:generateTranslation`。
- `LanguageService` 负责发现、回退、格式化和事件；前端只通过 `UiText` 获取最终文本。

## 6. 测试与验证

当前构建产物中共有 **125 个测试类、502 个测试方法**，最近一次全量测试无失败/错误/跳过。

测试覆盖重点：

- 规则、继承合并、版本清单缓存；
- 下载计划、文件下载、Range/续传、并发、哈希；
- 实例安装、本地版本、Forge/NeoForge/Fabric/Quilt/OptiFine；
- classpath、参数、进程、natives、virtual assets、兼容修复；
- Java 定位/要求/归档；
- Microsoft/Yggdrasil/账号存储；
- 设置、主题、语言一致性；
- UI 缩放、断点、焦点、对比度、下载布局、可选文本、Mod 布局。

## 7. 当前明确未实现

- 整合包导入/导出、更新和修复；Modrinth `.mrpack` 安装已可用。
- Mod 依赖递归下载。
- 游戏进程停止按钮（后端已有停止能力）。
- 启动器自更新和代码签名。
- macOS 正式安装包。
- 下载任务的暂停/继续 UI；自动断点续传已存在。
- 完整 CurseForge Key 获取渠道：需要构建环境或系统属性提供。
- Microsoft Client ID 未注入时无法登录微软账号。
- 第三方服务器的两步验证/OAuth 辅助登录、账号头像和账号导入/导出。

## 8. 构建与运行

```bash
# 全量测试
./gradlew test

# 启动前端
./gradlew :CAMLFront:run

# 只跑后端测试
./gradlew :CAMLBackend:test

# 只跑前端测试
./gradlew :CAMLFront:test

# 重新生成语言文件
./gradlew :CAMLLanguage:generateTranslation

# 打包当前平台
./gradlew packageLinux
./gradlew packageWindows
./gradlew collectPackages
```

打包细节见 [package.md](package.md)。

## 9. 文档索引

- [architecture.md](architecture.md)：模块、数据流、并发与边界；
- [version-download-and-launch.md](version-download-and-launch.md)：下载/安装/启动；
- [auth.md](auth.md)：账号与认证；
- [runtime.md](runtime.md)：游戏 Java；
- [ui.md](ui.md)：界面、布局、主题和测试；
- [package.md](package.md)：打包与发布；
