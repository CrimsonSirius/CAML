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
| Mod 下载 | 下载页新增 Mod 子页；通过 Modrinth/CurseForge 官方 API 搜索、分页、查看详情、按加载器和游戏版本分组、显示依赖并下载到当前实例 `mods` 目录；不使用镜像源；CurseForge 文件地址为空时按 fileId 回退 CDN，兼容旧 CDN 域名变体并透传 `x-api-key`，SHA-1 缺失时回退 MD5，大小 0 视为未知；Modrinth 的 WebP 图标在后端转换为 PNG 后显示 |
| 光影包下载 | 下载页在 Mod 子页下方新增光影包子页；复用搜索、分页、详情和图标链路，Modrinth 使用 `project_type:shader`，CurseForge 使用 `classId=6552`，按游戏版本筛选并下载到当前实例 `shaderpacks` 目录 |
| 资源包下载 | 下载页新增资源包子页；Modrinth 使用 `project_type:resourcepack`，CurseForge 使用 `classId=12`，按游戏版本筛选并下载到当前实例 `resourcepacks` 目录 |
| 地图下载 | 下载页新增地图子页；仅使用 CurseForge `classId=17`，按游戏版本筛选；下载 ZIP 后解压到当前实例 `saves/<世界名>`，重名自动追加序号，失败时清理临时文件 |
| 实例管理 | “管理实例”提供 Mod 与资源包两个入口；Mod 显示本地版本、支持启停和更新检查；资源包扫描实例 `resourcepacks` 中的 zip/目录，读取 `pack.mcmeta` 的 `pack_format` 显示版本，并用 `.disabled` 后缀切换启用状态 |
| 版本安装 | 本地已安装版本直接复用（支持第三方启动器/整合包的自定义版本 ID），下载时可用自定义实例名；官方版本 ID 负责元数据，实例 ID 作为 `versions/<id>` 目录名；支持 Fabric Loader/Fabric API、Quilt Loader/Quilted Fabric API、Forge、NeoForge、OptiFine（仅 Forge）安装；Forge/NeoForge 版本列表优先从 BMCLAPI JSON 接口获取，官方源使用 Maven 元数据和 NeoForge 版本号映射匹配 Minecraft 版本；支持旧式 Forge library、Maven 路径推导和子版本库优先去重；缺失文件自动补齐；版本隔离仅使用 FULL/NONE；基础文件完成后才写入版本 JSON |
| 整合包下载 | 下载页“游戏实例”分组新增整合包；首期支持 Modrinth `.mrpack` 搜索、详情和安装为新实例；解析 `minecraft`、`fabric-loader`、`quilt-loader`、`forge`、`neoforge` 依赖，下载清单文件、解压 `overrides`，写入整合包标记并强制完全隔离 |
| 账号 | 离线登录、稳定离线 UUID；微软设备码登录，OAuth → Xbox Live → XSTS → Minecraft Services，启动时按过期时间自动 refresh；第三方 Yggdrasil（LittleSkin 等）账密登录，启动时 `refresh` 自动续期并自愈未绑定角色的 token；支持多角色切换；authlib-injector 运行时按需下载一次、每次启动 SHA-256 校验后以 `-javaagent` 注入；metadata 三级缓存（网络/内存/磁盘）保证皮肤站不可用时仍能启动；设置页维护全局账号与默认账号，主页二级页为每个实例单独指定账号，未指定时回退默认；accessToken/refresh token 使用 AES-GCM 加密落盘（未勾选“记住登录”时只留在内存），密码不保存 |
| Java | 扫描 `java.home`、`JAVA_HOME`、`PATH` 及常见 JDK 目录；读取版本、供应商、架构、位数；按版本要求选择运行时；Windows 优先 `javaw.exe` |
| 启动 | 不可变 `LaunchPlan`、classpath 构建、JVM/游戏参数与 `${...}` 占位符、natives 子目录解压、Log4j 配置、旧版本 virtual assets 重建；NeoForge 26.2 + Vulkan 自动关闭 early window；根据设置选择，`--gameDir` 指向 `versions/<id>` 或 `.minecraft` 根目录 |
| 进程 | `ProcessBuilder` 启动、工作目录与环境变量、stdout/stderr 转发、Minecraft Log4j XML 转原版日志风格、退出码回调、停止能力；命令行过长时自动回退 `@argfile` / manifest classpath jar；`LaunchException` 分类错误并提供中文提示 |
| 多语言 | `CAMLLanguage` 只包含契约、生成器和语言 JSON；全部运行时逻辑在后端 `LanguageService`：内置文件同步、语言检测、加载、逐键回退、占位符格式化、诊断事件和日志。前端 `UiText` 只转发后端 `LanguageTextProvider` 并显示结果，不读取语言文件、不做回退；翻译文件由 `LanguageTranslationGenerator` 子类生成，`:CAMLLanguage:generateTranslation` 刷新 |

### 尚未实现

- 微软浏览器回调登录、皮肤/披风上传和系统凭据库
- CAMLFront 启动错误对话框、停止按钮、启动器隐藏 / 保留 / 关闭策略
- 崩溃分析、异常退出分类和真实 Minecraft 客户端端到端验证
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
│  │  ├─ loader/      Fabric/Quilt/Forge/NeoForge/OptiFine 安装编排
│  │  ├─ theme/       主题目录、内置主题复制和当前主题配置
│  │  ├─ settings/    启动器设置持久化
│  │  ├─ system/      物理内存读取（Linux MemAvailable）
│  │  └─ json/        通用 JSON 文件读写
│  ├─ src/main/resources/themes/      打包的内置主题
│  └─ src/test/java/                  后端单元测试与集成测试
├─ CAMLUI/                            纯 JavaFX + CSS 的 CAML 组件库
│  ├─ src/main/java/org/chs/camlauncher/ui/
│  │  ├─ theme/       CamlTheme（安装结构样式、加载外部主题）
│  │  └─ control/     CamlButton、CamlCard、CamlTextField、CamlSwitch 等
│  └─ src/main/resources/org/chs/camlauncher/ui/caml.css（只含结构，不含配色）
├─ CAMLLanguage/                      纯 Java 语言契约、生成器与语言资源
│  ├─ src/main/java/org/chs/camlauncher/language/
│  │  ├─ contract/                    事件、错误码、任务类型、DTO、选择枚举
│  │  └─ generator/                   生成器抽象类、TranslationTable、Runner 和十一种语言
│  ├─ src/main/resources/lang/        内置语言 JSON（zh_cn、zh_tw、en_us、fn_cn、ja_jp、ko_kr、fr_fr、de_de、ru_ru、la_la、bo_cn）
│  └─ src/test/java/                  LanguageTest
├─ CAMLFront/                         JavaFX 界面（不直接读写文件/发 HTTP/启动进程）
│  ├─ src/main/java/org/chs/camlauncher/front/
│  │  ├─ CAMLApplication.java         应用入口
│  │  ├─ UiScale.java                  界面缩放规则（设计基准 1440×900、根字号 16px）
│  │  ├─ WindowSizePlanner.java       窗口尺寸与居中计算（上下限由 UiScale 推导）
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
│  │     ├─ DownloadVersionSetupView.java 版本信息、Forge/OptiFine/Fabric/Quilt/NeoForge 选择与下载
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
- `CAMLLanguage` 只定义语言契约、生成期工具和 JSON 资源，不执行请求线程上的语言运行时逻辑，也不依赖 JavaFX、`CAMLBackend` 或 `CAMLFront`。
- 后端 `LanguageService` 是唯一运行时语言入口，负责所有语言文件 IO、检测、回退、格式化和日志。
- `CAMLFront` 只通过 `CAMLUI` 搭建界面、通过 `UiText` 显示后端已经解析好的文案；`UiText` 不读取文件、不做回退、不格式化。
- `CAMLFront` 不直接读写文件、不直接发 HTTP、不直接启动进程。
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
| TwelveMonkeys ImageIO WebP | 3.15.3 |
| JUnit | 6.1.3 |

---

## 构建与运行

需要本机安装 JDK 25。

Linux 上运行使用 LWJGL 2 的旧版本（如 1.12.2）还需要 `xrandr` 命令，对应 Arch 包
`xorg-xrandr`。CAML 启动游戏时会把 `~/.local/bin` 前插到子进程 `PATH`，因此用户级安装的
`xrandr` 也能被 LWJGL 2 找到。若检测到 LWJGL 2 实例缺少 `xrandr`，启动时会显示应用内提示
和常见发行版安装命令，但不会阻止游戏继续启动。

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

### 发布打包

两种平台都用 jpackage 生成自带裁剪运行时的 `app-image`，再封装成系统安装包。
脚本会读取构建 JDK 的 `release` 文件校验 `IMPLEMENTOR` 与 `JAVA_VERSION`，Linux 和 Windows 都使用 Azul Zulu：

| 平台 | 构建 JDK | 产物内运行时 |
|---|---|---|
| Linux | Azul Zulu JDK 25 | Zulu 裁剪运行时 |
| Windows | Azul Zulu JDK 25 | Zulu 裁剪运行时 |

通过 `JAVA_HOME` 或 `-PcamlJdkHome` 指定 JDK；用错供应商或版本会直接报错，
不会产出内容与许可文本不一致的包。

没有显式指定时会按下面的顺序自动挑第一个合格的 JDK，所以在 IDE 里直接点任务也能用：

1. `-PcamlJdkHome`（命令行显式指定，**指向非 Zulu 25 会直接失败**）；
2. `PATH` 上的 `jpackage` 所在的 JDK；
3. `JAVA_HOME`、`ZULU_HOME`；
4. Gradle 工具链解析到的 JDK 25；
5. 常见安装位置：`%ProgramFiles%\Zulu`、`%LOCALAPPDATA%\Programs\Zulu`、`~/.jdks`、
   `~/.gradle/jdks`、`/usr/lib/jvm`、`/opt`。

第 2 步之后都是"候选"，不满足 Zulu 25 就跳过，不会因为它而失败。排查时运行：

```bash
./gradlew printPackagingJdk          # 打印候选清单和最终选中的 JDK
```

IDE 里点任务但希望固定某个 JDK 时，在用户级 `~/.gradle/gradle.properties` 写入
`camlJdkHome=<Zulu 25 路径>`，不必改仓库文件。

打包任务都带 `onlyIf` 平台判断，**不能在 Windows 上交叉生成 Linux 包**（反之亦然）。
要在 Windows 上拿到 Linux 产物，用 WSL2、Docker 或 CI 的 Linux runner 各跑一次
`packageLinux`。

#### 产物位置

打包任务由根工程提供，实现集中在 `gradle/packaging.gradle.kts`；`CAMLFront`
只负责生成自己的 `installDist` 运行时。所有发布包都写到根工程的 `build` 目录。

| 位置 | 内容 | 清理方式 |
|---|---|---|
| `build/packages/linux/` | deb、rpm、`SHA256SUMS.txt` | `./gradlew cleanLinuxPackages` |
| `build/packages/windows/` | exe、免安装 zip、`SHA256SUMS.txt` | `./gradlew cleanWindowsPackages` |

安装包会保留 CAML 的 GPL 文本、`THIRD-PARTY-NOTICES.md`、JDK legal，
以及从实际构建 JDK 复制的 Zulu `DISCLAIMER` 和 `readme.txt`。

取包统一用根工程任务，它在 **`build/packages/`** 生成一份覆盖当前平台全部产物的
`SHA256SUMS.txt`：

```bash
./gradlew collectPackages          # 或 .\gradlew.bat collectPackages
```

`collectPackages` 默认依赖 `packageLinux`（Linux 主机）与 `packageWindowsPortable`
（Windows 主机）；需要 Windows EXE 时先单独跑一次 `packageWindows`（需 WiX），
再运行 `collectPackages`，exe 会被一起汇总。
根目录 `./gradlew clean` 会删除 `build/`，因此汇总结果不会长期堆积。

`build/packages/SHA256SUMS.txt` 的条目带平台子目录前缀，可以直接在该目录校验：

```bash
cd build/packages && sha256sum -c SHA256SUMS.txt
```

```powershell
# Windows 没有自带 sha256sum，用 PowerShell 逐项比对
cd build\packages
Get-Content SHA256SUMS.txt | ForEach-Object {
    $hash, $path = $_ -split '\s+', 2
    $actual = (Get-FileHash $path -Algorithm SHA256).Hash.ToLower()
    "{0}  {1}" -f $(if ($actual -eq $hash) { 'OK' } else { 'FAILED' }), $path
}
```

`build/packages/linux/SHA256SUMS.txt` 和 `build/packages/windows/SHA256SUMS.txt`
只收录各自目录内的文件，因此条目不带前缀。

发布包**不做代码签名**：Windows 首次运行会触发 SmartScreen 提示，Linux 也没有 GPG 签名。
请让用户用上面的哈希值自行校验下载完整性。

#### Linux

Arch Linux 本地构建需要：

```bash
sudo pacman -S dpkg rpm-tools

./gradlew packageLinux \
  -PcamlJdkHome=/path/to/zulu-25 \
  -PcamlVersion=1.0.0 \
  -PcamlRelease=1
```

在 Windows 上构建 Linux 包（WSL2，把 Zulu 解压到 `~/jdk/zulu-25`）：

```bash
# 建议 clone 到 WSL 自己的文件系统，避免 /mnt/f 的权限与性能问题
cd ~ && git clone <repo> caml && cd caml
sudo apt install -y dpkg rpm
./gradlew packageLinux -PcamlJdkHome=~/jdk/zulu-25
```

也可以用 Docker，宿主机不需要安装 dpkg/rpmbuild：

```bash
docker run --rm -v "$PWD":/w -w /w azul/zulu-openjdk-debian:25 \
  bash -lc "apt-get update && apt-get install -y rpm && ./gradlew packageLinux"
```

产物位于 `build/packages/linux/`：

```text
caml-launcher_1.0.0-1_amd64.deb
caml-launcher-1.0.0-1.x86_64.rpm
SHA256SUMS.txt
```

安装后布局：

```text
/opt/caml-launcher/          应用与内置 JDK/JavaFX
/usr/bin/caml                命令入口
/usr/share/applications/caml.desktop
/usr/share/icons/hicolor/    应用图标
/usr/share/doc/caml-launcher/copyright
/usr/share/licenses/caml-launcher/
/usr/share/licenses/caml-launcher/Zulu/       Zulu DISCLAIMER 与 readme
/usr/share/licenses/caml-launcher/JDK/        JDK 模块许可证
```

Debian/Ubuntu：`sudo apt install ./caml-launcher_1.0.0-1_amd64.deb`；
Fedora/RHEL：`sudo dnf install ./caml-launcher-1.0.0-1.x86_64.rpm`。

#### Windows

Linux 和 Windows 包都用 Zulu 25 生成裁剪运行时。

```powershell
.\gradlew.bat packageWindows `
  "-PcamlJdkHome=C:\path\to\zulu-25" `
  "-PcamlVersion=1.0.0" `
  "-PcamlRelease=1"
```

`packageWindows` 会同时产出 EXE 安装包和免安装便携包。EXE 依赖 [WiX Toolset](https://wixtoolset.org/docs/download/)（3.14 或 4.0.4+，5.x 不受支持），
需要把它的 `bin` 目录加入 `PATH`；jpackage 通过 `candle`/`light`（或 `wix`）构建安装器。
**没装 WiX 时可以只出免安装包**：

```powershell
.\gradlew.bat packageWindowsPortable -PcamlJdkHome="C:\path\to\zulu-25"
```

产物位于 `build/packages/windows/`：

```text
caml-launcher-1.0.0-1-windows-x64.exe            安装包（需 WiX）
caml-launcher-1.0.0-1-windows-x64-portable.zip   免安装包（不需要 WiX）
SHA256SUMS.txt                                  packageWindows 生成的校验和
```

免安装包解压后是单层 `CAML/` 目录，直接双击 `CAML.exe` 运行；
应用数据仍写在 `%USERPROFILE%\.caml`，游戏目录仍是 `%USERPROFILE%\.minecraft`。

安装包特点：

- 按机器安装（per-machine），默认目录 `C:\Program Files\CAML`，需要管理员权限；
- 开始菜单固定创建 `CAML` 分组快捷方式；**桌面快捷方式在安装向导中询问，默认勾选**，
  由 `--win-shortcut-prompt` 提供；
- 安装向导包含 GPL-3.0 许可页（`--license-file`），并允许自定义安装目录；
- 升级依赖固定的 `--win-upgrade-uuid`，改动它会让新旧版本并存而不是覆盖安装；
- 卸载只删除安装目录，保留 `%USERPROFILE%\.caml` 与游戏数据。

安装后布局：

```text
C:\Program Files\CAML\           应用与内置 JDK/JavaFX
C:\Program Files\CAML\app\       应用 jar 与 licenses/（含 Zulu/ 许可文本）
C:\Program Files\CAML\runtime\   裁剪后的 Java 运行时
```

应用图标由 `packaging/icons/caml-512.png` 在构建期缩放生成多尺寸
`caml.ico`（16/24/32/48/64/128/256），因此仓库里不需要保存二进制图标。
打包完成后可以复跑冒烟校验：

```bash
./gradlew smokeTestPackagedApp      # 或 .\gradlew.bat smokeTestPackagedApp
```

它读取 app-image 自己的 `*.cfg`，用其中的 `--module-path` 启动后端，并检查裁剪运行时的
`release` 是否覆盖 `packagedRuntimeModules`、以及 `--strip-native-commands` 是否生效。

GitHub Actions 的 `Build` workflow 会在所有分支 push、所有 PR 和手动触发时跑测试，但只在 PR、
`main`/`crimson`/`namelessgod2008` 分支和 tag 上打包：普通功能分支 push 只跑测试，
不再每次都占用 Linux 45 分钟 + Windows 60 分钟的 runner。两个打包 workflow 的细节：

- 测试：Ubuntu 上 `xvfb-run ./gradlew test :CAMLFront:distZip`，Windows 上 `.\gradlew.bat test`；失败时上传测试报告。
- 打包：`Package Linux` 与 `Package Windows` 并行执行；**Windows 打包要求 Windows 用例先通过**，测试红了不会产出安装包。
- 两个打包 job 在上传 artifact 前都会跑 `smokeTestPackagedApp`，以与安装包相同的模块布局启动后端，并校验裁剪运行时的模块集。
- release 号优先使用 PR 编号，artifact 名分别为 `caml-linux-packages-pr-<PR 编号>` 和 `caml-windows-packages-pr-<PR 编号>`；无法从合并提交提取 PR 编号时回退到 run number。Windows artifact 本身是 GitHub 提供的 zip，内含安装式 EXE、便携 zip 和 `SHA256SUMS.txt`。
- `v*` tag 同时触发两个 `Package` workflow，各自上传 GitHub Release。
- 手动触发（`workflow_dispatch`）的 `version` 输入经环境变量传入，并在打包前校验为
  “数字和点组成”；带后缀的 tag（例如 `v1.0.0-beta`）会在解析阶段直接报错，
  不会拖到 Gradle 才失败。

### CAMLUI 组件库

`CAMLUI` 不引入第三方主题库。`caml.css` 只包含布局、字体层级、圆角和
控件结构，不包含任何固定颜色。颜色令牌由外部主题文件提供。

```java
CamlTheme.install(scene);
CamlTheme.applyTheme(scene, Path.of(System.getProperty("user.home"), ".caml/themes/coffee.css"));

CamlCard card = new CamlCard(
        new Label("启动游戏"),
        CamlButton.filled("开始")
);
```

当前提供：`CamlTheme`、`CamlButton`、`CamlCard`、`CamlTextField`、`CamlTextArea`、
`CamlSelectableText`、
`CamlComboBox`、`CamlSpinner`、`CamlSwitch`、`CamlCheckBox`、`CamlSlider`、`CamlRadioButton`、
`CamlMemorySlider`、`CamlProgressBar`、`CamlSideNav`、`CamlSideNavItem`、`CamlTopNav`、
`CamlTopNavItem`、`CamlPageHost` 和 `CamlTopBar`。

界面布局、组件映射、无障碍和自审计见 [design/ui.md](design/ui.md)。

需要关闭页面切换和按钮缩放动画时，启动参数加 `-Dcaml.reduceMotion=true`。

### 多语言

前端不在代码里硬编码界面文案。语言 JSON 位于 `CAMLLanguage`，运行时由后端加载并解析：

```text
CAMLLanguage/src/main/resources/lang/zh_cn.json
CAMLLanguage/src/main/resources/lang/zh_tw.json
CAMLLanguage/src/main/resources/lang/en_us.json
CAMLLanguage/src/main/resources/lang/fn_cn.json
CAMLLanguage/src/main/resources/lang/ja_jp.json
CAMLLanguage/src/main/resources/lang/ko_kr.json
CAMLLanguage/src/main/resources/lang/fr_fr.json
CAMLLanguage/src/main/resources/lang/de_de.json
CAMLLanguage/src/main/resources/lang/ru_ru.json
CAMLLanguage/src/main/resources/lang/la_la.json
CAMLLanguage/src/main/resources/lang/bo_cn.json
~/.caml/lang/<locale>.json
```

每次启动时，后端 `LanguageService` 都会把内置语言文件与 `~/.caml/lang` 中的同名文件按翻译键值比对：
缺失或内容不一致（改过文案、缺键、JSON 损坏）时**按内置文件重写**，保证新版内置翻译和新增键总能生效。
`~/.caml/lang` 中**非内置**的语言文件不参与比对，始终原样保留，仍可自由新增或修改。

视图和控制器只引用翻译键，`UiText` 只负责把 query 转给后端并显示最终文本：

```java
import org.chs.camlauncher.front.text.UiText;

CamlButton launch = CamlButton.filled(UiText.tr("home.launch"));
statusLabel.setText(UiText.tr("home.instance.current", version));
```

- 运行时文本查询由后端 `LanguageService.text(key, args...)` 完成，支持逐键回退、`%s` / `%d` 等占位符，并记录缺失键与格式化诊断。
- 当前语言按顺序取 `-Dcaml.language`、系统语言。默认语言是 `en_us`，因此不检测英语系统，
  只检测有内置翻译且允许自动切换的系统语言：`zh_CN` / `zh_SG`（含脚本 `Hans`）等简体中文系统使用
  `zh_cn`，`zh_TW` / `zh_HK` / `zh_MO`（含脚本 `Hant`）等繁体中文系统使用 `zh_tw`，
  日语系统使用 `ja_jp`，韩语系统使用 `ko_kr`，法语系统使用 `fr_fr`，德语系统使用 `de_de`，
  俄语系统使用 `ru_ru`；其余系统（含英语）一律使用 `en_us`。拉丁文（`la_la`）与藏文（`bo_cn`）
  不参与系统检测，只能在手动模式中选择。
- 每次启动时后端把内置 `lang/*.json` 与 `~/.caml/lang` 的同名文件按键值比对，
  不一致（含 JSON 损坏）就按内置文件重写；非内置语言文件始终原样保留。
- 设置 → 外观 → 语言提供“自动 / 手动”两种模式；手动模式可从语言下拉框选择
  `zh_cn`、`zh_tw`、`en_us`、`fn_cn`、`ja_jp`、`ko_kr`、`fr_fr`、`de_de`、`ru_ru`、`la_la`
  或 `bo_cn`。模式与手动语言分别保存在 `settings.properties` 的
  `languageMode` 和 `language`，切换后立即重建主视图刷新界面文案。
- 外观页同时提供语言回退策略：`系统语言`、`默认语言`（固定英语）、
  `裸键`（不回退）和`自定义语言`（仅下拉选择的 locale）。除默认语言外，缺失内容显示裸键。模式与自定义 locale
  持久化为 `languageFallbackMode` 和 `languageFallbackLocale`。
- 语言下拉框中的语言名不参与翻译，始终使用该语言自己的写法：`zh_cn` → `简体中文`、
  `zh_tw` → `繁體中文`、`en_us` → `English`、`fn_cn` → `地道中国话`、`ja_jp` → `日本語`、
  `ko_kr` → `한국어`、`fr_fr` → `Français`、`de_de` → `Deutsch`、`ru_ru` → `Русский`、
  `la_la` → `Latina`、`bo_cn` → `བོད་ཡིག`。名称**硬编码**在后端
  `LanguageLocales` 的名称表，不写入也不读取翻译文件，因此切换界面语言、或翻译文件写错名称都不会改变下拉框内容；
  未登记的 locale 回退显示 locale 标识。新增内置语言时在后端语言名称表中登记自称。
- 语言文件**由代码生成**，不要手工编辑：每种语言实现一个继承
  `LanguageTranslationGenerator` 的生成器（`CAMLLanguage` 的 `generator.lang` 包下：
  `ZHCNGenerator`、`ZHTWGenerator`、`ENUSGenerator`、`FNCNGenerator`、`JAJPGenerator`、
  `KOKRGenerator`、`FRFRGenerator`、`DEDEGenerator`、`RURUGenerator`、`LALAGenerator`、
  `BOCNGenerator`），
  在 `populate(TranslationTable)` 中用实例方法登记键和文案：

  ```java
  @Override
  protected void populate(TranslationTable t) {
      t.put("home.launch", "启动游戏");
      t.put("home.instance.current", "当前实例：%s");
  }
  ```

- 新增语言需要四处登记：在 `generator.lang` 下添加生成器（`locale()` 返回新标识）、在
  `TranslationGeneratorRunner` 的生成器列表中登记、把 locale 加入
  `LanguageConstants.BUNDLED_LOCALES`（决定安装、同步与下拉框顺序）、并在后端
  `LanguageLocales` 的名称表补充自称（语言名不写进翻译文件）。若该语言需要参与自动检测，
  还要在 `LanguageLocales.DETECTED_LANGUAGE_LOCALES` 中登记语言代码；拉丁文与藏文不参与检测。
  `LanguageTest` 会校验 `BUNDLED_LOCALES`、生成器与已提交 JSON 三者一一对应。

### Mod 下载

光影包与 Mod 共用同一套下载页和详情页，但来源筛选不同：Modrinth 使用
`project_type:shader`，CurseForge 使用 `classId=6552`；文件写入实例实际数据目录下的
`shaderpacks`，版本隔离规则与 Mod 相同。

资源包也复用同一套下载页和详情页：Modrinth 使用 `project_type:resourcepack`，
CurseForge 使用 `classId=12`；文件写入实例实际数据目录下的 `resourcepacks`。

地图复用同一套下载页和详情页，但 Modrinth 没有 world 项目类型，因此只查询
CurseForge `classId=17`。下载的 ZIP 会解压为实例数据目录下的 `saves/<世界名>`；
同名世界不会覆盖，自动追加 `(2)` 等序号。

整合包位于“游戏实例”分组，首期只支持 Modrinth `.mrpack`。安装会创建新实例，
安装对应加载器、下载 `modrinth.index.json` 中的文件并解压 overrides；
实例写入 `.caml-modpack.json` 标记，启动时强制使用该实例目录作为游戏数据目录。

- Modrinth 使用 `https://api.modrinth.com/v2` 官方 API，无需额外配置。
- CurseForge 使用 `https://api.curseforge.com/v1` 官方 API，必须配置 API Key。
  运行时读取优先级：JVM 参数 `-Dcaml.curseforge.apiKey=...` → 环境变量
  `CURSEFORGE_API_KEY` → 构建时写入 jar 的编码 Key。
- 本地开发可以直接设置环境变量：
  ```bash
  CURSEFORGE_API_KEY='你的Key' ./gradlew :CAMLFront:run
  ```
- CI 发布时不要把 Key 写进仓库。`CAMLBackend` 的 `generateBuildProperties` 会在构建期读取
  `CURSEFORGE_API_KEY`，按固定盐做 XOR 后再 Base64，写成
  `caml.curseforge.apikey.enc` 并打进 `CAMLBackend` 的 `caml.properties`；发布包即可开箱使用。
  未配置时选择 CurseForge 会显示明确错误；“全部来源”仍会返回 Modrinth 结果。
  编码只是让发布包里不出现能被 `grep` 直接命中的明文，**不构成安全边界**：
  拿到 jar 的人仍可还原出 Key，因此务必使用专用 Key 并定期轮换。
- GitHub Actions 示例（在仓库 Settings → Secrets and variables → Actions 新建
  `CURSEFORGE_API_KEY`）：

  ```yaml
  name: Build
  on: [push, pull_request, workflow_dispatch]
  jobs:
    build:
      runs-on: ubuntu-latest
      steps:
        - uses: actions/checkout@v5
        - uses: actions/setup-java@v5
          with:
            distribution: zulu
            java-version: '25'
        - name: Build
          env:
            CURSEFORGE_API_KEY: ${{ secrets.CURSEFORGE_API_KEY }}
            CAML_MICROSOFT_CLIENT_ID: ${{ secrets.CAML_MICROSOFT_CLIENT_ID }}
          # distZip 必须和 test 一起跑：只跑 test 不会产出 distributions/*.zip。
          run: ./gradlew test :CAMLFront:distZip --no-daemon
        - uses: actions/upload-artifact@v4
          with:
            name: CAML-Debug-Distribution
            path: CAMLFront/build/distributions/*.zip
            if-no-files-found: error
  ```

  Key 会嵌入客户端 jar，和 HMCL/PCL 的做法相同；请使用专用 Key，泄露后及时在
  CurseForge 控制台重新生成。
- TODO（未实现）：允许用户在 `~/.caml/caml.properties` 或设置页填写自己的
  CurseForge Key，以覆盖内置 Key；实现前普通用户无法自行更换 Key。

### 微软登录

- 登录采用设备码流程：启动器显示用户码和验证页面，用户在浏览器完成 Microsoft 登录，
  启动器轮询 OAuth token，再依次换取 Xbox Live、XSTS、Minecraft access token、许可证和 profile。
- Client ID 读取优先级：JVM 参数 `-Dcaml.microsoft.clientId=...` → 环境变量
  `CAML_MICROSOFT_CLIENT_ID` → 构建时写入 jar 的 `caml.microsoft.clientId`。
- 本地开发：
  ```bash
  CAML_MICROSOFT_CLIENT_ID='你的公开客户端ID' ./gradlew :CAMLFront:run
  ```
- CI 发布时通过 `CAML_MICROSOFT_CLIENT_ID` 注入公开客户端 ID。这是桌面公共客户端的
  Application ID，不是客户端密钥；Azure 应用需要允许设备码公共客户端流程。
- 未配置 Client ID 时，设置页的微软登录会显示结构化错误，不影响离线与 Yggdrasil 账号。
- Microsoft refresh token 和 Minecraft access token 与原有账号 token 一样使用 AES-GCM
  加密保存；这不是系统凭据库，能读取 `~/.caml/accounts` 的程序仍可解密。

### 界面缩放

字号、控件尺寸和窗口尺寸共用同一条缩放规则，大屏与小屏看到的比例都与设计稿一致：

- 设计基准是 `1440×900` 的逻辑窗口与 `16px` 根字号。
- `UiScale.factor(宽, 高)` 取 `min(宽/1440, 高/900)`，量化到 `1/16`（根字号取整数像素），
  钳制在 `0.6875–1.375`（根字号 `11–22px`）。
- `MainView` 把因子换算成根字号写入根节点；`caml.css` 与 `app.css` 中除 `1px` 描边外的所有长度
  都用 `em`，因此字号、控件尺寸、圆角与留白随窗口等比缩放。
- 无法用 CSS 表达的几何（模组页、工作区分栏、侧边栏宽度）由 `ModDownloadLayout` /
  `WorkspaceLayout` / `DownloadsView` 读取同一个因子换算。
- `WindowSizePlanner` 的窗口上下限同样由 `UiScale` 推导：初始尺寸取屏幕可用区域的 `68%`/`74%`，
  并限制在 `990×618`（因子下限）与 `1980×1237`（因子上限）之间。

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
| 账号索引 | `~/.caml/accounts/accounts.json`（accessToken 加密存储） |
| authlib-injector | `~/.caml/authlib-injector/<版本>/`（只在首次使用时下载） |
| 认证服务器 metadata | `~/.caml/accounts/servers.json`（冷启动回退，保证 prefetched 可用） |
| 游戏数据（Linux / macOS / Windows） | `~/.minecraft`（Windows 为 `%USERPROFILE%\.minecraft`） |

游戏目录在 **设置 → 游戏设置 → 游戏位置** 中修改；输入或浏览后必须点击保存才会生效。
版本隔离在 **设置 → 游戏设置 → 版本隔离** 中选择，修改后立即生效并写入设置文件。
额外 JVM 参数在 **设置 → 游戏设置 → JVM 参数** 中编辑，保存到
`settings.properties` 的 `jvmArguments`，下一次启动时追加到版本自带参数之后。
参数支持双引号和 `${game_directory}`、`${library_directory}` 等占位符。
主页的 **浏览游戏实例** 会扫描当前游戏位置并列出 `versions/<id>/<id>.json`。

### 版本隔离

设置页提供两个选项：

- **完全版本隔离**：`--gameDir` 始终指向 `versions/<id>`，这是默认值。
- **不进行版本隔离**：`--gameDir` 始终指向 `.minecraft` 根目录。

选择会影响下一次启动。切换模式不会自动搬运已有存档；从“不进行版本隔离”切换到
“完全版本隔离”后，旧存档仍留在 `.minecraft/saves`，新存档写入
`versions/<id>/saves`。需要沿用旧存档时应手动复制。

CAML 只支持显式的 FULL/NONE，不再自动检测 PCL/HMCL 混合布局，也不会根据
`versions/<id>` 中已有的 `saves`、`options.txt` 等文件猜测模式。导入第三方实例时，
需要自行选择与目录实际布局匹配的模式。

无论选择哪种模式，`libraries`、`assets` 都来自 `.minecraft` 根目录。游戏进程继承系统环境
（不再改写 `APPDATA`）。主页会根据当前选择显示实际存档位置。

---

## 使用流程

1. 启动 CAML，主界面显示后端状态与数据目录。
2. 首次使用时进入 **设置 → 游戏设置 → 游戏位置**，输入或浏览目录并点击保存。
3. 回到主页，点击 **浏览游戏实例**，从当前游戏位置扫描出的列表中选择实例。
4. 填写 **离线用户名**；如需调整主题、内存或 JVM 参数，进入 **设置**。
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
```

---

## 文档

- [总体架构](design/architecture.md)
- [界面设计](design/ui.md)
- [版本下载与启动链路](design/version-download-and-launch.md)
- [runtime 包](design/runtime.md)
- [第三方账号登录](design/auth.md)
- [当前实现情况](design/Implement.md)

---

## 已知问题

`./gradlew test` 目前在 Windows 与 Linux 上都是 489 个用例全部通过，CI 的 `build` 与
`windows-test` 两个 job 会分别在两个平台各跑一遍。

历史上的平台相关失败（POSIX 路径断言、`fml.toml` 换行符、`java` / `java.exe` 文件名、
JavaFX 文本节点里的路径分隔符）都已修掉。新增用例如果出现平台相关失败，请用
`Path.toString()`、`File.pathSeparator` 或平台可执行文件名拼期望值来修复，
不要跳过测试，也不要只在 Linux 上保留用例。

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