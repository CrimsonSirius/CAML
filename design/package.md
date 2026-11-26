# CAML 打包与发布

> 本文按当前 `gradle/packaging.gradle.kts`、`packaging/` 和 GitHub Actions 重写，最后核对时间：2026-11-26；基线提交：`cf7ef33`。

## 1. 边界

打包分为三层：

1. `:CAMLFront:assemble` / `:CAMLFront:installDist`：生成普通 Java 应用及运行时 jar。
2. jpackage：用 Zulu 25 生成自带裁剪运行时的 app-image。
3. 平台外壳：Deb/RPM、Windows EXE/ZIP、许可证、图标和校验文件。

发布包不包含 Minecraft 客户端 jar、assets、libraries 或任何游戏资源。

## 2. 版本参数

Gradle 属性：

- `camlVersion`：默认 `1.0.0`；
- `camlRelease`：默认 `1`；
- `camlJdkHome`：打包用 Zulu 25 JDK。

Linux/Deb/RPM 版本允许数字与点；Windows EXE 的 ProductVersion 额外要求 1–3 段且每段 0–255。

当前项目自身构建版本仍是 `1.0-SNAPSHOT`，打包版本由 `camlVersion` 独立提供。

## 3. 打包 JDK

只接受 **Azul Zulu 25**：

- `release` 中 `IMPLEMENTOR` 含 `Azul`；
- `JAVA_VERSION` 以 `25` 开头；
- 存在 `bin/jpackage` / `bin/jpackage.exe`。

显式 `-PcamlJdkHome=<path>` 优先；未显式配置时按以下来源查找：

- `ZULU_HOME`；
- `~/.jdks`；
- `~/.sdkman/candidates/java`；
- `~/.gradle/jdks`；
- Windows 的 Program Files/LOCALAPPDATA 常见目录；
- `PATH` 中的 `jpackage` 反推 JDK；
- Gradle Java 25 toolchain。

使用其他供应商 JDK 会直接失败，避免把不可再分发的 JDK 打进安装包，也避免许可证文本与实际运行时不一致。

排查：

```bash
./gradlew printPackagingJdk
```

## 4. 发布输入

运行输入：

- `:CAMLFront:installDist` 生成的 `lib/*.jar`；
- 项目 `LICENSE`；
- `packaging/licenses/*`；
- Zulu `DISCLAIMER`、`readme.txt`；
- JDK runtime 的 `legal/*`；
- `packaging/icons/*`；
- Linux desktop 文件、启动脚本、copyright、postinst/postrm；
- Windows 由 `caml-512.png` 生成的多尺寸 `.ico`。

生成的 `caml.properties` 会按构建环境写入：

- CurseForge Key 的 XOR+Base64 混淆值（不是安全边界）；
- Microsoft OAuth Client ID。

相关环境变量：

- `CURSEFORGE_API_KEY`
- `CAML_MICROSOFT_CLIENT_ID`

## 5. 通用 JavaFX/JDK 模块

Linux 和 Windows app-image 都使用：

```text
--module-path $APPDIR
--add-modules javafx.controls,javafx.fxml
--enable-native-access=ALL-UNNAMED,javafx.graphics
```

jlink 保留模块：

```text
java.base,java.desktop,java.logging,java.management,java.naming,
java.net.http,java.prefs,java.scripting,java.sql,java.xml,
jdk.management,jdk.unsupported,jdk.jfr
```

jlink 选项：

```text
--strip-native-commands --strip-debug --no-man-pages --no-header-files
```

## 6. Linux 任务图

```text
stageLinuxInput
  → packageLinuxAppImage
  → stageLinuxZuluNotices
  → stageLinuxRoot
      ├─ stageDebRoot → packageDeb
      └─ packageRpm
  → packageLinux
```

辅助任务：

- `cleanLinuxPackages`：删除 Linux 打包中间/产物目录；
- `stageLinuxInput`：收集 CAMLFront 运行时 jar；
- `packageLinuxAppImage`：生成 `build/packages/linux/app-image/CAML`；
- `stageLinuxZuluNotices`：复制 Zulu 通知；
- `stageLinuxRoot`：生成 deb/rpm 共用根目录；
- `stageDebRoot`：Debian 根目录；
- `packageDeb` / `packageRpm`：调用 `dpkg-deb` / `rpmbuild`；
- `packageLinux`：生成 deb、rpm 和 SHA256SUMS。

Linux 任务只在 Linux 主机执行；当前只支持 x86_64/amd64。

## 7. Linux 安装内容

安装根：

```text
/opt/caml-launcher/          app-image + 自带裁剪 JDK/JavaFX
/usr/bin/caml                启动脚本，exec /opt/caml-launcher/bin/CAML
/usr/share/applications/caml.desktop
/usr/share/icons/hicolor/256x256/apps/caml.png
/usr/share/icons/hicolor/512x512/apps/caml.png
/usr/share/doc/caml-launcher/
/usr/share/licenses/caml-launcher/
```

许可证目录包含：

- `CAML-GPL-3.0-only.txt`；
- `packaging/licenses/*`；
- Zulu `DISCLAIMER` 和 `readme.txt`；
- JDK `legal/*`。

Debian `control` 当前依赖：

```text
libc6, libgtk-3-0, libx11-6, libxext6, libxrender1, libxtst6, libxi6,
libxxf86vm1, libgl1, fontconfig, libfreetype6, libasound2
```

RPM 当前依赖：

```text
glibc, gtk3, libX11, libXext, libXrender, libXtst, libXi, libXxf86vm,
mesa-libGL, fontconfig, freetype, alsa-lib
```

注意：两套依赖都**没有**声明 `xrandr`/`xorg-xrandr`。LWJGL 2 的 `xrandr` 缺失由应用内启动预检提示处理，用户可自行安装；CAML 也会检查 `~/.local/bin`。

postinst/postrm 只尝试更新 desktop database 和 icon cache，失败不影响安装/卸载。

## 8. Linux 产物

目录：`build/packages/linux/`

```text
caml-launcher_<version>-<release>_amd64.deb
caml-launcher-<version>-<release>.x86_64.rpm
SHA256SUMS.txt
```

CI 在 Ubuntu 22.04（glibc 2.35）构建，以提高跨发行版兼容性。

## 9. Windows 任务图

```text
stageWindowsInput
generateWindowsIcon
stageWindowsZuluNotices → stageWindowsLicenses
  ├─ packageWindowsAppImage → packageWindowsPortable
  └─ packageWindowsExe（独立再跑一次 jpackage）
packageWindows = packageWindowsExe + packageWindowsPortable
```

`packageWindowsExe` 与 `packageWindowsAppImage` 都直接依赖三个 staging 任务，各自调用一次
jpackage（`--type exe` 与 `--type app-image`），因此 EXE 不依赖 app-image 的中间产物。

辅助任务：

- `cleanWindowsPackages`；
- `generateWindowsIcon`：PNG → 多尺寸 ICO；
- `stageWindowsZuluNotices` / `stageWindowsLicenses`；
- `stageWindowsInput`；
- `packageWindowsAppImage`；
- `packageWindowsPortable`：不需要 WiX；
- `packageWindowsExe`：需要 WiX；
- `packageWindows`：EXE + ZIP + SHA256SUMS；
- `smokeTestPackagedApp`：读取 app-image 的 `*.cfg` 启动后端，并校验裁剪运行时。

Windows 任务只在 Windows 主机执行；当前只支持 x64/amd64。

## 10. Windows 安装包

EXE 安装包参数：

- per-machine 安装；
- 固定升级 UUID `6f1c2b4a-8f2d-4e57-9d3a-1b7c5e9a2f60`，发布后不可改；
- 创建开始菜单项；
- 安装时提示是否创建快捷方式，默认勾选；
- 允许选择安装目录；
- 附带许可证文件、图标和帮助 URL；
- 使用 WiX Toolset 3.14 或兼容版本。

便携包直接压缩 app-image，不需要 WiX，适合免安装试用。

产物目录：`build/packages/windows/`

```text
caml-launcher-<version>-<release>-windows-x64.exe
caml-launcher-<version>-<release>-windows-x64-portable.zip
SHA256SUMS.txt
```

## 11. 汇总任务

```bash
./gradlew collectPackages
```

只依赖当前平台可执行的包任务，并把当前已有产物汇总到：

```text
build/packages/SHA256SUMS.txt
```

条目带 `linux/` 或 `windows/` 前缀，可直接在 `build/packages` 下运行 `sha256sum -c SHA256SUMS.txt`。

## 12. GitHub Actions

### 12.1 `Build`

- 触发：所有分支 push、PR、手动；
- Ubuntu + Zulu 25；
- 执行 `xvfb-run ./gradlew test :CAMLFront:distZip`
  （`distZip` 必须一起跑，否则上传 debug distribution 的步骤永远拿不到文件）；
- Windows job 执行 `.\gradlew.bat test`；两个平台都跑全量用例，失败时上传测试报告；
- 注入 `CURSEFORGE_API_KEY`、`CAML_MICROSOFT_CLIENT_ID` secret；
- 打包 job 只在 PR、`main`/`crimson`/`namelessgod2008` 分支和 tag 上运行：普通功能分支
  push 只跑测试，避免每次 push 都占用 Linux 45 分钟 + Windows 60 分钟的 runner；
- `windows-packages` 依赖统一的 `build` 矩阵；任一平台测试失败都不会产出安装包。

### 12.2 `Package Linux`

- 可被 Build 调用、手动、tag 触发；
- Ubuntu 22.04，Zulu 25；
- 安装 `rpm`（`dpkg-deb` 由 runner 自带）；
- 版本来源：tag 去掉 `v`、手动输入、PR 号或 run number；手动输入经环境变量传入并以
  `^[0-9]+(\.[0-9]+)*$` 预先校验，带后缀的 tag（`v1.0.0-beta`）在解析阶段即失败；
- 打包并检查 deb/rpm；
- 调用根工程的 `smokeTestPackagedApp` 执行发布冒烟测试；
- tag 时上传 GitHub Release。

### 12.3 `Package Windows`

- Windows runner，Zulu 25；
- 自动安装/定位 WiX 3.14，并校验 `light.exe` 存在；
- 打包 EXE 和 portable ZIP；
- 检查 EXE/ZIP；
- 调用根工程的 `smokeTestPackagedApp` 执行发布冒烟测试；
- tag 时上传 GitHub Release。

两个 `release` job 都不 checkout，因此显式设置 `GH_REPO: ${{ github.repository }}`，
否则 `gh release` 会以 “no git remotes found” 失败。

### 12.4 发布冒烟测试

`./gradlew smokeTestPackagedApp` 读取 app-image 自己的 `*.cfg`，把其中的 `--module-path`
展开为实际路径后启动 `BackendSmoke`，因此校验用的 JVM 参数与安装包完全一致，
改 `--java-options` 时校验不会失效。

`packaging/smoke/BackendSmoke.java` 会断言 `CAMLBackend` 以命名模块加载，启动后端，检查 11 个内置语言和 `lang/zh_cn.json`。它专门捕获 jpackage 模块路径下跨 jar 资源不可见的问题，普通 classpath 单元测试无法覆盖。

任务在启动成功后还会校验裁剪运行时：

- `runtime/release` 的 `MODULES` 必须覆盖 `packagedRuntimeModules`（Linux 与 Windows 共用
  同一份模块列表）；少列模块只会在用户机器上抛 `ClassNotFoundException`；
- `runtime/bin/java(.exe)` 必须不存在，否则说明 `--strip-native-commands` 没生效。

### 12.5 失败诊断

两个测试 job 在失败时会把 JUnit XML 里的失败用例名和首行断言消息写进 Run Summary，
两个打包 job 则在打包前把 `JAVA_HOME`、JDK 的 `IMPLEMENTOR`/`JAVA_VERSION` 以及
`jpackage`/`jlink`/`javac` 是否存在写进 Run Summary 并运行 `printPackagingJdk`。
这两块信息足以区分“用例本身跨平台”与“runner 上的 JDK 不含 jpackage / 供应商不符”，
不需要下载 artifact 才能定位。

PowerShell 步骤的脚本正文保持 ASCII：GitHub 把 `run` 保存为无 BOM 的 UTF-8，
非 ASCII 字符在 PowerShell 5.1 下会被按 ANSI 解码而破坏引号配对。

### 12.6 并发与超时

- `Build` 用 `cancel-in-progress: true`，同一分支连续推送取消旧运行；
- 两个 `Package` workflow 用固定前缀 `caml-linux-packages-<ref>` /
  `caml-windows-packages-<ref>` 且 `cancel-in-progress: false`：
  被 `workflow_call` 调用时 `github.workflow` 是调用方名字，用它会让两个平台互相排队；
- 超时：测试 30 分钟、Linux 打包 45 分钟、Windows 打包 60 分钟、发布 15 分钟。

## 13. 本地命令

Linux：

```bash
./gradlew packageLinux -PcamlJdkHome=/path/to/zulu-25 \
  -PcamlVersion=1.0.0 -PcamlRelease=1
```

Windows：

```powershell
.\gradlew.bat packageWindows "-PcamlJdkHome=$env:JAVA_HOME" `
  "-PcamlVersion=1.0.0" "-PcamlRelease=1"
```

冒烟校验（打包完成后，两个平台命令相同）：

```bash
./gradlew smokeTestPackagedApp      # 或 .\gradlew.bat smokeTestPackagedApp
```

清理：

```bash
./gradlew cleanLinuxPackages
./gradlew cleanWindowsPackages
```

## 14. 产物检查

Linux：

```bash
dpkg-deb -I build/packages/linux/*.deb
dpkg-deb -c build/packages/linux/*.deb >/dev/null
rpm --dbpath /tmp/caml-rpmdb -qpi build/packages/linux/*.rpm
sha256sum -c build/packages/linux/SHA256SUMS.txt
```

Windows：

```powershell
Get-ChildItem build/packages/windows/*.exe, build/packages/windows/*.zip
Get-Content build/packages/windows/SHA256SUMS.txt
```

## 15. 常见故障

- **找不到合适 JDK**：运行 `printPackagingJdk`，确认 Zulu 25 和 `camlJdkHome`。
- **Linux 缺少 `dpkg-deb`**：安装 `dpkg`。
- **Linux 缺少 `rpmbuild`**：安装 `rpm-tools` / `rpm`。
- **Windows 缺少 WiX**：安装 WiX 3.14 或 4.0.4+，或只跑 `packageWindowsPortable`。
- **EXE 版本无效**：版本必须 1–3 段且每段 0–255。
- **安装后启动即找不到语言**：检查是否用模块路径启动、是否执行了 `BackendSmoke`。
- **Linux 旧版 Forge 启动崩溃于显示模式**：安装 `xorg-xrandr`/`xrandr`/`x11-xserver-utils`，或确保 `~/.local/bin/xrandr` 可用。

## 16. 维护规则

- 不得把非 Zulu 25 JDK 打入正式包。
- 新增运行时依赖时同步更新：
  - `packaging/licenses/THIRD-PARTY-NOTICES.md`；
  - `ProjectDependencyService`；
  - About 页显示。
- 不得打包 Minecraft 客户端、assets、libraries 或游戏资源。
- 修改升级 UUID 会让新旧版本并存。
- 修改 `camlVersion`/`camlRelease` 后重新生成校验文件。
- 发布 tag 前先跑 `./gradlew test` 和当前平台打包任务。
- 修改 `--add-modules` 时必须同步 `packagedRuntimeModules`（Linux/Windows 共用），
  并运行 `smokeTestPackagedApp` 确认裁剪运行时的 `MODULES` 覆盖该列表。
