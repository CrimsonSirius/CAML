# CAML 打包与发布

更新日期：2026-11-18

## 1. 文档范围

本文说明 CAML 从可运行模块到 Linux/Windows 发布包的完整流程，包括：

- 根工程、`CAMLFront` 与打包脚本的职责边界；
- Linux deb/rpm 与 Windows EXE/便携 zip 的任务图；
- 打包 JDK 的选择、校验和供应商限制；
- 包内目录、许可证和运行时来源；
- 本地命令、GitHub Actions、tag 发布和产物命名；
- 产物校验与常见故障排查。

本文不描述游戏版本安装、Java 运行时选择或启动命令构建，这些内容见
[version-download-and-launch.md](version-download-and-launch.md) 和 [runtime.md](runtime.md)。

## 2. 设计结论

- 打包是项目级发布职责，不是 `CAMLFront` 的业务职责。
- `build.gradle.kts` 只负责应用 `gradle/packaging.gradle.kts`；具体打包实现集中在后者。
- `CAMLFront` 只保留 `java`、`application`、JavaFX、依赖和 `mainClass`。
- 根工程通过 `:CAMLFront:installDist` 获取完整运行时 classpath，再交给 jpackage。
- 两类系统包都使用 **Azul Zulu JDK 25** 构建，并把裁剪后的 Zulu 运行时打进安装包。
- Linux 和 Windows 都只能在对应宿主平台上构建，不支持交叉打包。
- 正式包不包含 Minecraft 客户端 jar、assets、game libraries 或任何游戏资源。

## 3. 构建与打包的边界

### 3.1 `:CAMLFront:assemble`

`:CAMLFront:assemble` 是标准 Gradle 模块生命周期任务，由 Java/Application 插件产生：

```text
:CAMLFront:assemble
├─ :CAMLFront:jar
├─ :CAMLFront:distZip
└─ :CAMLFront:distTar
```

它生成的是普通 Gradle 分发包：

- 启动脚本；
- `CAMLFront` 与其他模块 jar；
- 第三方依赖 jar；
- **不包含裁剪后的 JDK**；
- 不是 deb/rpm/exe 安装包。

该分发包由 `Build` workflow 上传为 `CAML-Debug-Distribution`，用于开发和调试。

### 3.2 `:CAMLFront:installDist`

根工程的 Linux/Windows 打包任务依赖 `:CAMLFront:installDist`，读取：

```text
CAMLFront/build/install/CAMLFront/lib/*.jar
```

这些 jar 组成 jpackage 的 `--input`，其中 `CAMLFront-<version>.jar` 是
`--main-jar`，主类从 `CAMLFront` 的 `application.mainClass` 读取。

### 3.3 根工程打包脚本

根 `build.gradle.kts` 末尾应用：

```kotlin
apply(from = "gradle/packaging.gradle.kts")
```

`gradle/packaging.gradle.kts` 负责：

- 读取 `CAMLFront` 的版本、主类和工具链；
- 注册全部 `package*`、`stage*`、`clean*` 和 `collectPackages` 任务；
- 解析并校验打包 JDK；
- 调用 jpackage、dpkg-deb、rpmbuild、PowerShell/Java AWT 图标生成逻辑；
- 生成 SHA-256 校验和。

## 4. 版本与参数

打包版本和发行号来自 Gradle 项目属性：

| 属性 | 默认值 | 用途 |
|---|---|---|
| `camlVersion` | `1.0.0` | app-image、安装包和文件名版本 |
| `camlRelease` | `1` | deb/rpm 发行号、Windows 文件名中的 release |
| `camlJdkHome` | 未设置 | 显式指定 Zulu JDK 25 |

规则：

- `camlVersion` 只能由数字和点组成。
- Windows 的 `jpackage` ProductVersion 只接受 `major.minor.build`，每段必须在 `0..255`。
- Linux deb/rpm 文件名使用 `camlVersion-camlRelease`：
  - `caml-launcher_<version>-<release>_amd64.deb`
  - `caml-launcher-<version>-<release>.x86_64.rpm`
- Windows 文件名使用：
  - `caml-launcher-<version>-<release>-windows-x64.exe`
  - `caml-launcher-<version>-<release>-windows-x64-portable.zip`

## 5. 打包 JDK

### 5.1 硬性要求

Linux 和 Windows 发布包都必须使用 **Azul Zulu JDK 25**：

- `IMPLEMENTOR` 必须包含 `Azul`；
- `JAVA_VERSION` 必须以 `25` 开头；
- JDK 必须自带对应平台的 `jpackage`；
- Windows 使用 `jpackage.exe`，Linux 使用 `jpackage`。

显式 `-PcamlJdkHome` 不合格时会直接失败，避免产出运行时和许可证文本不一致的包。

### 5.2 隐式候选顺序

没有显式设置 `camlJdkHome` 时，脚本按以下顺序寻找第一个合格 JDK：

1. `PATH` 中 `jpackage` 所在的 JDK；
2. `JAVA_HOME`；
3. Gradle 工具链解析到的 JDK 25；
4. `ZULU_HOME`；
5. 常见安装位置：
   - Windows：`%ProgramFiles%\Zulu`、`%ProgramFiles(x86)%\...`、`%LOCALAPPDATA%\Programs\Zulu`；
   - 用户目录：`~/.jdks`、`~/.sdkman/candidates/java`、`~/.gradle/jdks`；
   - Linux：`/usr/lib/jvm`、`/usr/lib64/jvm`、`/opt`。

隐式候选不满足 Zulu 25 时会跳过，不会因为某个无关 JDK 而失败。

### 5.3 排查命令

```bash
./gradlew printPackagingJdk
```

该任务打印平台、显式配置、候选清单和最终选中的 JDK。IDE 中直接点击打包任务时，
建议在用户级 `~/.gradle/gradle.properties` 写入：

```properties
camlJdkHome=/absolute/path/to/zulu-25
```

## 6. 任务图

### 6.1 Linux

```text
:CAMLFront:installDist
└─ stageLinuxInput
   └─ packageLinuxAppImage

stageLinuxZuluNotices ─┐
packageLinuxAppImage ──┴─ stageLinuxRoot
                            ├─ stageDebRoot ─ packageDeb
                            └─ packageRpm

packageDeb + packageRpm ─ packageLinux
```

### 6.2 Windows

```text
:CAMLFront:installDist ─ stageWindowsInput

generateWindowsIcon

stageWindowsZuluNotices ─ stageWindowsLicenses

stageWindowsInput + generateWindowsIcon + stageWindowsLicenses
├─ packageWindowsAppImage ─ packageWindowsPortable
└─ packageWindowsExe

packageWindowsExe + packageWindowsPortable ─ packageWindows
```

### 6.3 汇总与清理

| 任务 | 作用 |
|---|---|
| `collectPackages` | 生成本机平台包，并在 `build/packages/SHA256SUMS.txt` 汇总 |
| `cleanLinuxPackages` | 删除 `build/packages/linux` |
| `cleanWindowsPackages` | 删除 `build/packages/windows` |
| `printPackagingJdk` | 打印打包 JDK 候选和选择结果 |

`collectPackages` 依赖 `packageLinux` 和 `packageWindowsPortable`；非本机任务会因
`onlyIf` 被跳过。统一校验和条目带 `linux/` 或 `windows/` 前缀。

## 7. 输入资源

```text
packaging/
├─ icons/
│  ├─ caml.png                  Linux 256x256 图标
│  ├─ caml-512.png              app-image 图标与 ICO 源图
│  └─ caml.svg                  图标源文件
├─ licenses/
│  ├─ Apache-2.0.txt
│  ├─ BSD-3-Clause.txt
│  ├─ Log4j-NOTICE.txt
│  ├─ OpenJFX-GPL-2.0-CE.txt
│  └─ THIRD-PARTY-NOTICES.md
├─ linux/
│  ├─ caml                      /usr/bin 启动脚本
│  ├─ caml.desktop             桌面入口
│  ├─ copyright                Debian copyright
│  ├─ postinst                 deb 安装后脚本
│  └─ postrm                   deb 卸载后脚本
├─ rpm/
│  └─ caml-launcher.spec       RPM spec 模板
└─ smoke/
   └─ BackendSmoke.java        CI 发布包冒烟校验（不进入安装包）
```

另有根目录 `LICENSE`，以 `CAML-GPL-3.0-only.txt` 复制进安装包。

## 8. Linux 打包

### 8.1 输入收集

`stageLinuxInput` 把 `:CAMLFront:installDist` 的 `lib/*.jar` 收集到：

```text
build/packages/linux/input/
```

### 8.2 app-image

`packageLinuxAppImage` 调用 Zulu 的 `jpackage --type app-image`，关键参数：

- `--name CAML`；
- `--main-jar CAMLFront-<project.version>.jar`；
- `--main-class` 来自 `CAMLFront`；
- `--input` 指向收集的运行时 jar；
- `--java-options --module-path=$APPDIR`；
- `--java-options --add-modules=javafx.controls,javafx.fxml`；
- `--add-modules` 指定游戏启动所需 JDK 模块；
- `--jlink-options` 去掉命令、调试、手册和头文件；
- `--icon packaging/icons/caml-512.png`。

输出：

```text
build/packages/linux/app-image/CAML/
```

### 8.3 Zulu 通知

`stageLinuxZuluNotices` 从实际打包 JDK 复制：

- `DISCLAIMER`
- `readme.txt`

缺任意文件都会失败，保证包内 Zulu 通知与实际运行时一致。

### 8.4 共用根文件系统

`stageLinuxRoot` 生成：

```text
/opt/caml-launcher/                       app-image
/usr/bin/caml                             启动命令
/usr/share/applications/caml.desktop      桌面入口
/usr/share/icons/hicolor/256x256/apps/    图标
/usr/share/icons/hicolor/512x512/apps/    图标
/usr/share/doc/caml-launcher/copyright
/usr/share/doc/caml-launcher/licenses/
/usr/share/licenses/caml-launcher/
```

其中：

- `/usr/share/licenses/caml-launcher/JDK/` 来自裁剪运行时的 `legal/`；
- `/usr/share/licenses/caml-launcher/Zulu/` 来自 Zulu `DISCLAIMER` 和 `readme.txt`；
- `THIRD-PARTY-NOTICES.md`、Apache/BSD/Log4j/OpenJFX 文本随包安装。

### 8.5 Debian

`stageDebRoot` 复制共用根目录，`packageDeb` 生成 `DEBIAN/control`、`postinst`、
`postrm` 和 `md5sums`，最后调用：

```text
dpkg-deb --build --root-owner-group <root> <output>
```

control 中声明：

- `Package: caml-launcher`
- `Architecture: amd64`
- `Depends: libc6, libgtk-3-0, libx11-6, ...`
- `Homepage`、`Description`、`Installed-Size`

### 8.6 RPM

`packageRpm`：

1. 创建 `_topdir` 和独立 `_dbpath`；
2. 把 Linux 根文件系统复制到 `SOURCES/root`；
3. 把 `caml-launcher.spec` 中的 `@VERSION@`、`@RELEASE@` 替换为实际值；
4. 调用 `rpmbuild -bb`；
5. 把生成 RPM 复制为最终文件名。

spec 关闭 debug package 和 os_install_post，并声明运行时依赖、桌面数据库和图标缓存
的 post/postun 刷新命令。RPM `License` 使用：

```text
GPL-3.0-only AND GPL-2.0-only WITH Classpath-exception-2.0
```

### 8.7 Linux 最终产物

`packageLinux` 校验两个产物都存在，并写入：

```text
build/packages/linux/SHA256SUMS.txt
```

## 9. Windows 打包

### 9.1 图标生成

`generateWindowsIcon` 读取 `packaging/icons/caml-512.png`，在构建期缩放并编码为
32bpp ICO，包含 16/24/32/48/64/128/256 尺寸：

```text
build/packages/windows/caml.ico
```

### 9.2 输入与许可证

`stageWindowsInput` 收集 `installDist` 的 jar。

许可证分两步：

1. `stageWindowsZuluNotices` 复制 Zulu `DISCLAIMER`、`readme.txt`；
2. `stageWindowsLicenses` 复制 `packaging/licenses`，并加入 `Zulu/`。

jpackage 通过 `--app-content` 把许可证放进应用目录，EXE 额外把根 `LICENSE`
作为安装向导许可页。

### 9.3 app-image

`packageWindowsAppImage` 调用 Zulu `jpackage.exe --type app-image`，参数与 Linux
基本一致，另外传入：

- `--app-content windowsLicensesDirectory`；
- `--app-content LICENSE`；
- Zulu 运行时模块列表。

### 9.4 便携包

`packageWindowsPortable` 是 `Zip` 任务：

- 依赖 `packageWindowsAppImage`；
- 把 app-image 放进单层 `CAML/`；
- 加入 `CAML-GPL-3.0-only.txt`；
- 固定时间戳和文件顺序，保证可复现；
- 不需要 WiX。

输出：

```text
build/packages/windows/caml-launcher-<version>-<release>-windows-x64-portable.zip
```

### 9.5 EXE 安装包

`packageWindowsExe` 依赖：

- `stageWindowsInput`；
- `generateWindowsIcon`；
- `stageWindowsLicenses`。

构建前调用 `requireWix()`，接受 WiX 3.14 或 WiX 4.0.4+，并在 Windows 上自动把
`candle`、`light`、`wix` 解析为 `.exe`。

jpackage `--type exe` 关键设置：

- per-machine 安装；
- 固定升级 UUID：`6f1c2b4a-8f2d-4e57-9d3a-1b7c5e9a2f60`；
- 开始菜单 `CAML` 分组；
- 桌面快捷方式安装时询问，默认勾选；
- 允许选择安装目录；
- 显示 GPL-3.0 许可页；
- 帮助链接指向项目仓库。

Windows 上 jpackage 产物常带只读属性，脚本通过 `deleteReadOnly` 先清理旧 EXE，
再移动新产物。

### 9.6 Windows 最终产物

`packageWindows` 校验 EXE 和便携 zip 都存在，并写入：

```text
build/packages/windows/SHA256SUMS.txt
```

## 10. 许可证与分发边界

发布包包含：

- CAML 本体：GPL-3.0-only；
- Azul Zulu 运行时：OpenJDK 部分 GPL-2.0 with Classpath Exception；
- Zulu 特定 `DISCLAIMER`、`readme.txt` 和 JDK 模块 legal；
- OpenJFX：GPL-2.0 with Classpath Exception；
- Gson、Log4j：Apache-2.0；
- TwelveMonkeys ImageIO：BSD-3-Clause；
- Error Prone annotations：Apache-2.0。

发布包**不包含**：

- Minecraft 客户端 jar；
- assets、game libraries 或任何游戏资源；
- 用户账号、access token 或 refresh token；
- 第三方 Minecraft 启动库。

当前发布包没有代码签名和 GPG 签名。Windows 首次运行可能触发 SmartScreen；
Linux 用户应使用 `SHA256SUMS.txt` 校验下载完整性。

## 11. GitHub Actions

### 11.1 `Build`

触发：所有分支 push、pull request、手动触发。

主要步骤：

- `:CAMLBackend:test :CAMLFront:assemble`；
- 失败时上传测试报告；
- 上传 `CAML-Debug-Distribution`；
- `build` 完成后并行调用 `linux-packages` 和 `windows-packages` 两个 reusable workflow；
- 两个调用 job 都使用 `secrets: inherit`，并授予 `contents: write` 供 tag release 使用。
- 使用 `concurrency` 按 ref 分组：同一分支连续推送时取消旧运行，避免多个打包任务并行抢占 runner；
  两个打包 workflow 自身使用 `cancel-in-progress: false`，避免中断正在进行的发布构建。
- 超时：`build` 30 分钟、`Package Linux` 45 分钟、`Package Windows` 60 分钟、release job 15 分钟。
- 打包产物只在冒烟校验通过后才上传 artifact（见 11.5）。

PR/分支 artifact 名称：

- `caml-linux-packages-pr-<PR 编号>`；
- `caml-windows-packages-pr-<PR 编号>`；
- 无法提取 PR 编号时分别回退到 `caml-linux-packages-run-<run number>` 和
  `caml-windows-packages-run-<run number>`。

### 11.2 `Package Linux`

触发：

- `workflow_call`（由 `Build` 调用）；
- `workflow_dispatch`；
- `v*` tag。

行为：

- 被 `Build` 调用时，根据 PR/分支生成 `caml-linux-packages-pr-*` 或 `run-*` artifact；
- 手动触发生成 `caml-linux-packages` artifact；
- tag 触发生成 `caml-linux-packages`，并由 release job 上传到 GitHub Release；
- tag 的版本从 `GITHUB_REF_NAME` 去掉前导 `v` 得到。

### 11.3 `Package Windows`

触发：

- `workflow_call`（由 `Build` 调用）；
- `workflow_dispatch`；
- `v*` tag。

行为：

- 被 `Build` 调用时：PR/分支生成 `caml-windows-packages-pr-<PR>` 或
  `caml-windows-packages-run-<run>`；
- 手动触发：`caml-windows-packages`；
- tag：`caml-windows-packages`，release job 上传 EXE、便携 zip 和校验和。

CI 会在 Windows runner 上：

1. 依次查找 `%ProgramFiles(x86)%\WiX Toolset v3.14\bin`、`%ProgramFiles%\WiX Toolset v3.14\bin`、
   `%ProgramFiles(x86)%\WiX Toolset v3.11\bin`；都不存在时才执行
   `choco install wixtoolset --version=3.14.1`，安装后重新查找；
2. 校验 `candle.exe` 和 `light.exe` 都存在，并把 WiX `bin` 传给构建 step；
3. 执行 `packageWindows`；
4. 验证 EXE、便携 zip 和 `SHA256SUMS.txt` 都存在；
5. 用安装包的模块布局启动后端做冒烟校验（见 11.5），失败则不上传 artifact。

PowerShell 中 `-PcamlVersion=1.0.0` 必须整体加引号：

```powershell
"-PcamlVersion=1.0.0"
```

否则 PowerShell 可能把它拆成 `-PcamlVersion=1` 和 `.0.0`，Gradle 会把 `.0.0`
当成任务名。

### 11.4 tag 发布

推荐流程：

```bash
git tag -a v1.0.0 -m "CAML 1.0.0"
git push origin v1.0.0
```

`Package Linux` 和 `Package Windows` 都会触发，并上传到同一个 `v1.0.0` Release。
两个 release job 都使用 `gh release view || create || view` 处理并发创建同一个
Release 的情况。

Release 资产为四个安装文件：

- `caml-launcher_<version>-<release>_amd64.deb`
- `caml-launcher-<version>-<release>.x86_64.rpm`
- `caml-launcher-<version>-<release>-windows-x64.exe`
- `caml-launcher-<version>-<release>-windows-x64-portable.zip`

以及两个平台专用的校验和文件：

- `SHA256SUMS-linux.txt`
- `SHA256SUMS-windows.txt`

release job 在下载 artifact 后先重命名校验和，避免 Linux 与 Windows 的
`SHA256SUMS.txt` 同名覆盖。

手动 `workflow_dispatch` 只生成 artifact，不发布 Release；Release job 只在 tag 上运行。

### 11.5 发布包冒烟校验

两个打包 workflow 在构建完成后都会运行 `packaging/smoke/BackendSmoke.java`（该文件不进入安装包）：

```
javac -encoding UTF-8 -d classes -cp "<app>/*" packaging/smoke/BackendSmoke.java
java --module-path "<app>" --add-modules=javafx.controls,javafx.fxml \
     -cp "classes:<app>/*" BackendSmoke <dataDir> <gameDir>
```

`<app>` 是 app-image 内 jar 所在目录（Linux `lib/app`，Windows `app`）。校验三件事：

1. `CAMLBackend` 必须以**命名模块**加载，否则说明本次运行没有复现安装包布局，校验没有意义；
2. `CAMLBackend.start()` 必须成功；
3. 内置语言数量为 11，且 `lang/zh_cn.json` 已同步到数据目录。

这一步不初始化 JavaFX 工具包，不需要图形环境，可以在两个 runner 上稳定执行；也不会访问网络，
只会在 `RUNNER_TEMP` 下创建一个临时数据目录。

为什么必须有它：1.0.0 安装包因 CAMLBackend 读取 CAMLLanguage 的 `lang/*.json` 失败而启动即退出，
而 `:CAMLBackend:test` 运行在类路径模式下，jar 处于无名模块，跨模块资源可以互相读取，
这类问题在单元测试中不会出现。

## 12. 本地命令

### 12.1 Linux

```bash
./gradlew packageLinux \
  -PcamlJdkHome=/path/to/zulu-25 \
  -PcamlVersion=1.0.0 \
  -PcamlRelease=1
```

依赖：

```bash
sudo pacman -S dpkg rpm-tools          # Arch
sudo apt install -y dpkg rpm            # Debian/Ubuntu
```

### 12.2 Windows

```powershell
.\gradlew.bat packageWindows `
  "-PcamlJdkHome=C:\path\to\zulu-25" `
  "-PcamlVersion=1.0.0" `
  "-PcamlRelease=1"
```

仅生成便携包：

```powershell
.\gradlew.bat packageWindowsPortable `
  "-PcamlJdkHome=C:\path\to\zulu-25"
```

### 12.3 通用任务

```bash
./gradlew collectPackages
./gradlew cleanLinuxPackages
./gradlew cleanWindowsPackages
./gradlew printPackagingJdk
```

## 13. 产物校验

### 13.1 哈希

```bash
cd build/packages
sha256sum -c SHA256SUMS.txt
```

Windows PowerShell 没有自带 `sha256sum`，可逐项计算：

```powershell
cd build\packages
Get-Content SHA256SUMS.txt | ForEach-Object {
    $hash, $path = $_ -split '\s+', 2
    $actual = (Get-FileHash $path -Algorithm SHA256).Hash.ToLower()
    "{0}  {1}" -f $(if ($actual -eq $hash) { 'OK' } else { 'FAILED' }), $path
}
```

### 13.2 Linux 包

```bash
dpkg-deb -I build/packages/linux/*.deb
dpkg-deb -c build/packages/linux/*.deb | less
rpm --dbpath /tmp/caml-rpmdb -qpi build/packages/linux/*.rpm
rpm --dbpath /tmp/caml-rpmdb -qpl build/packages/linux/*.rpm | less
```

至少确认：

- `/opt/caml-launcher/lib/runtime` 存在；
- `/usr/bin/caml` 可执行；
- `licenses/Zulu/DISCLAIMER`、`licenses/Zulu/readme.txt` 存在；
- `licenses/JDK/` 存在；
- `copyright`、`THIRD-PARTY-NOTICES.md` 存在。

### 13.3 Windows 包

在 Windows 上检查：

- EXE 能启动安装向导；
- 便携 zip 解压后存在 `CAML/CAML.exe`；
- `CAML/app/licenses/` 包含 Zulu 与第三方许可文本；
- `CAML/runtime/` 存在；
- 安装后开始菜单、桌面快捷方式、升级和卸载行为正常。

CI 会检查 EXE、便携 zip 和校验和都存在，并对 app-image 执行后端冒烟校验（见 11.5）；
安装器交互仍需人工或 Windows 端到端测试。

## 14. 常见故障

| 现象 | 原因 | 处理 |
|---|---|---|
| `未找到 jpackage` | JDK 不含 jpackage 或路径错误 | 指向 Zulu JDK 25，运行 `printPackagingJdk` |
| JDK 供应商错误 | 使用了非 Zulu JDK | 设置 `-PcamlJdkHome` 或 `ZULU_HOME` |
| Linux/Windows 打包任务被跳过 | 当前宿主平台不匹配 | 在对应平台构建，不能交叉打包 |
| `未找到 dpkg-deb` | 未安装打包工具 | Arch 安装 `dpkg`，Debian/Ubuntu 安装 `dpkg` |
| `未找到 rpmbuild` | 未安装 RPM 工具 | Arch 安装 `rpm-tools`，Debian/Ubuntu 安装 `rpm` |
| `未找到 WiX Toolset` | WiX 未安装或 PATH 不含 bin | 安装 WiX 3.14/4.0.4+，或只构建便携包 |
| Windows CLI 报 `.0.0` 任务不存在 | PowerShell 拆分未加引号的 `-PcamlVersion=1.0.0` | 把整个 `-P...` 参数加双引号 |
| Windows EXE 覆盖失败 | jpackage 产物带只读属性 | 使用脚本内置 `deleteReadOnly`，或先运行 `cleanWindowsPackages` |
| 安装后双击闪退，stderr 报 `缺少内置语言资源: /lang/xx.json` | app 目录下 jar 全部位于 `--module-path`，成为命名（自动）模块；命名模块只能看到自身资源 | 资源必须由所属模块的类去读，见 §15 维护规则 |

## 15. 维护规则

- 修改打包版本命名时，同时更新 `gradle/packaging.gradle.kts`、README、CI 和本文。
- 修改 Windows 升级 UUID 会让新旧版本并存；发布后不要改。
- 修改许可证文件时，同步检查 Debian copyright、RPM License 和
  `THIRD-PARTY-NOTICES.md`。
- 新增第三方运行时组件时，必须更新许可证清单并确认可再分发。
- 新增安装位置或文件时，同步更新 deb/rpm 文件清单、校验说明和卸载行为。
- 不要把 Minecraft 客户端、assets 或游戏 libraries 打进发布包。
- 新增跨模块资源（例如 A 模块读取 B 模块里的 `*.json`/`*.css`）时，必须用**资源所属模块**的类
  作为定位锚点（`SomeClassInOwnerModule.class.getResourceAsStream(...)`），并保留类加载器兜底；
  安装包里 `app/` 下所有 jar 都在 `--module-path` 上，命名模块读不到其他模块的资源，
  单元测试（类路径模式）无法覆盖这种情况。
- 发布前至少执行一次 `./gradlew build`、对应平台的 `package*` 和目标包内容检查。
- 修改资源加载后，至少手动启动一次安装包内的 `CAML.exe`（或在 Linux 上运行 `/opt/caml-launcher/bin/CAML`），
  确认 `~/.caml/logs/latest.log` 出现 `CAML backend ... started`。

## 16. 相关文档

- [总体架构](architecture.md)
- [版本下载与启动链路](version-download-and-launch.md)
- [runtime 包](runtime.md)
- [第三方账号登录](auth.md)
- [当前实现情况](Implement.md)