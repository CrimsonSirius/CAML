# CAML 界面设计

> 本文按当前 JavaFX 页面、CSS 和缩放代码重写，最后核对时间：2026-11-23；基线提交：`c86eb05`。

## 1. 设计意图

CAML 使用 Material 风格的层级、圆角、状态色和焦点反馈，但不照搬 Material 组件：顶栏负责全局导航，下载页和设置页在页面内使用侧栏；主要操作用 filled 按钮，次要操作用 outlined 按钮。

界面结构保持三类稳定锚点：

- 顶部主导航永远在窗口顶部。
- 下载/设置子导航始终属于当前页面，不替换全局导航。
- 关键主操作和返回路径固定，内容再多也只让内容区域滚动。

`CAMLUI` 只定义结构、字号层级、圆角、间距和状态，不定义固定配色。颜色令牌来自 `~/.caml/themes/*.css`。

## 2. 窗口与整体缩放

### 2.1 设计基准

- 逻辑设计窗口：`1440×900`；
- 基准根字号：`16px`；
- `1em` 在设计基准下等于 `16px`。

### 2.2 缩放因子

`UiScale.factor(width, height)`：

```text
ratio = min(windowWidth / 1440, windowHeight / 900)
factor = clamp(ratio, 0.6875, 1.375)
```

因子量化到 `1/16`，使根字号为整数像素：

- 最小根字号：`11px`；
- 最大根字号：`22px`。

CSS 中除 `1px` 描边外，尺寸优先使用 `em`；需要 Java 计算的几何（模组页、工作区、侧栏宽度）也乘同一因子，避免 CSS 与代码各算一套比例。

### 2.3 初始窗口

`WindowSizePlanner`：

- 初始宽：可用宽度 `68%`；
- 初始高：可用高度 `74%`；
- 不超过屏幕可用区域的 `92%` 宽、`90%` 高；
- 最大窗口：约 `1980×1237`（设计基准放大到最大因子）；
- 最小窗口：约 `990×618`，小屏再按屏幕 `90%` 收缩；
- 初始位置居中。

窗口位置、尺寸和最大化状态保存在 `settings.properties`，下次启动恢复到仍可见的屏幕。

## 3. 应用壳层

`MainView` 是 `StackPane`：

- 底层 `BorderPane`：顶栏、页面宿主、底栏；
- 顶层全局遮罩：scrim + 应用内卡片。

顶栏：

- 左侧名称 `CAML` 和副标题；
- 中部主导航：主页 / 下载 / 设置；
- 同一时间只有一个选中项。

底栏显示：

- CAML 版本；
- 后端是否就绪；
- 数据目录。

`CamlPageHost` 负责页面切换动画。页面内容淡出/淡入并向上下小幅位移；如果页面带侧栏，侧栏也从左侧淡入。动画只表达层级变化，不做装饰性运动。

## 4. 全局遮罩与提示

`MainView` 的遮罩层用于不新建系统窗口的弹层：

- Java 下载卡片；
- Linux 缺少 `xrandr` 的启动提示。

遮罩点击关闭；卡片有明确的关闭按钮。Java 下载卡片包含发行版、JDK/JRE、版本和下载进度。xrandr 提示包含说明、常见发行版命令和“复制命令”按钮；命令展示使用 `CamlSelectableText`，可鼠标拖动选择、双击选词、三击全选、`Ctrl/Cmd+A`、`Ctrl/Cmd+C`，但不包含 ScrollPane，原生不可滚动。

## 5. 主页

`HomeView` 由启动表单和运行日志组成，通过 `SplitPane` 分区。

- 宽度 `>= 900` 设计像素（按因子缩放）时左右分栏，表单在左、日志在右；
- 更窄时上下堆叠，表单在上、日志在下；
- 表单字段过多时只滚动字段区，启动按钮固定在底部；
- 日志可折叠，折叠后表单占满工作区。

启动表单显示：

- 当前实例；
- 游戏位置；
- 存档位置及是否完全隔离；
- 账号选择；
- 离线用户名（离线模式时）；
- 启动按钮和状态文字。

日志面板：

- 预览当前状态；
- 保留 UI 日志；
- 支持清空；
- 折叠/展开；
- 游戏 stdout/stderr 和退出码由后端事件写入。

主页二级页：

- 浏览游戏实例：扫描当前游戏目录的 `versions`，选择后更新“当前实例”。
- 管理实例：打开实例数据目录、查看 Mod/资源包、启用/禁用、打开文件夹、检查 Mod 更新。
- 实例账号：为当前实例绑定独立账号；没有绑定则回退全局默认账号。

## 6. 下载页

`DownloadsView` 使用页面内侧栏：

- “游戏实例”分组：正式版、快照版、远古版、整合包；
- “游戏内容”分组：Mod、光影包、资源包、地图。

两个分组各有一个标题和横向分隔线，分组标题不参与选中状态和焦点遍历。

整合包页首期只接 Modrinth `.mrpack`：搜索/详情复用内容页，安装时创建新实例并强制完全隔离。

下载页侧栏宽度取窗口宽度 `1/6`，限制在 `180–260` 设计像素，再乘 UI 因子。

### 6.1 版本列表

`DownloadVersionsView` 使用虚拟化 `ListView`：

- 只创建可见行；
- 分类按 `releaseTime` 新到旧排序；
- 每行显示版本 ID、类型、发布时间；
- 点击后进入设置页。

### 6.2 下载设置

`DownloadVersionSetupView`：

- 实例名输入；停止约 500ms 后异步检查格式、目录重复、加载器同名校验；
- 版本信息；
- Fabric Loader / Fabric API；
- Quilt Loader / Quilted Fabric API；
- Forge；
- NeoForge；
- OptiFine（仅在 Forge 已选时可用）；
- 取消和整宽“下载实例”。

加载器互斥；Fabric/Quilt/NeoForge 会清空 OptiFine。选项较多时，选项区域独立滚动，底部主按钮始终可见。

### 6.3 下载进度页

`DownloadProgressView`：

- 标题和返回按钮固定；
- 正文放在纵向 `ScrollPane` 中；
- 左侧卡片显示实时速度、累计字节；
- 右侧卡片显示总进度、当前文件、当前阶段和阶段列表；
- 阶段列表可无限增长，但不会挤掉进度条和返回按钮。

下载页顶部有跨页面保留的任务条：

- 显示标题、当前阶段、文件和进度；
- 运行中的任务有可见取消按钮；
- 任务句柄在页面切换后仍保留；
- 完成、失败、取消状态与主进度页同步。

### 6.4 Mod / 光影包 / 资源包 / 地图

四类内容复用 `ModSearchView`、`ModDetailView` 和 `ModDownloadLayout`：

- 搜索表单支持来源、游戏版本、加载器；
- 内容宽度 `>= 660` 设计像素：标签在左的两行两列；
- `520–659`：标签在上的两行两列；
- `< 520`：单列纵向堆叠；
- 结果行图标 `88`、最小行高 `112` 设计像素；
- 结果列表和分页随整页纵向滚动；
- 详情页按游戏版本/加载器分组，显示依赖，下载到 `mods`、`shaderpacks`、`resourcepacks` 或 `saves`；
- 地图只查询 CurseForge（`classId=17`），下载 ZIP 后解压为 `saves/<世界名>`，重名时自动追加序号。

图标由后端下载/缓存，Modrinth WebP 在显示前转 PNG。图标批量加载有超时、并发和延迟重试，避免慢图标长期占住线程。

## 7. 设置页

设置页侧栏包含：

- 游戏设置；
- Java 管理；
- 下载设置；
- 账号；
- 外观；
- 关于。

右侧内容整体可纵向滚动；分组卡之间间距固定，页面切换使用短淡入。

### 7.1 游戏设置

- 游戏位置：手动输入后必须点击“保存”，通过目录校验才生效。
- 版本隔离：完全版本隔离 / 不进行版本隔离。
- 游戏语言：首次启动实例时自动写入游戏语言；仅当 `options.txt` 尚无 `lang:` 时生效。
- Java：自动/手动选择；手动时显示运行时详情；扫描/添加/下载按钮在独立 Java 管理页。
- 内存：滑块最小 512 MB，上限为物理内存，步进 256 MB；显示已使用、游戏分配、可用和总内存；停止约 300ms 后保存。
- JVM 参数：额外参数文本区，保存后生效；支持引号和占位符。

### 7.2 Java 管理

工具栏：

- 重新扫描；
- 添加 Java；
- 下载 Java。

每个 Java 候选显示为独立分组卡：

- 版本、供应商、架构、位数、JDK/JRE；
- 可执行文件路径；
- 打开所在文件夹按钮（方形圆角焦糖色描边，按下略缩小）；
- 不可用状态；
- 手动添加标记。

下载 Java 使用应用内遮罩卡片，不打开新 Stage：发行版、JDK/JRE、版本、进度、关闭和下载安装。Linux 显示“建议使用包管理器”的提示，Windows 不显示。

### 7.3 下载设置

- 下载源：镜像 / 官方 / 自定义；
- 自定义 HTTP/HTTPS 根地址；
- 下载并发数 1–256；
- 滑块/文本变化防抖后保存。

### 7.4 账号

- 账号列表、全局默认账号、删除；
- 添加离线账号；
- 微软账号：设备码、验证页面、复制设备码/网址；
- 第三方登录：LittleSkin 预设或自定义 API Root、用户名、密码、记住登录；
- 多角色账号可切换 Profile；
- authlib-injector 自定义 jar 路径；
- 所有 token 由后端保存和刷新，界面不显示 token。

### 7.5 外观

- 语言：自动 / 手动，手动时选择内置语言；
- 语言回退：系统语言 / 默认语言 / 显示裸键 / 自定义语言；
- 主题：从 `~/.caml/themes/*.css` 选择，切换时短暂淡出/淡入；`caml.reduceMotion` 开启时立即切换。

语言或回退切换会重建 `MainView`，重建后恢复页面和子项，并把焦点交回语言设置，不落到顶栏“主页”胶囊。

### 7.6 关于

七组信息：

- 启动器：版本、后端状态、许可证；
- 启动器运行环境：Java 版本、供应商；
- 系统：操作系统/发行版、内核/系统版本、架构；
- 设备信息：CPU、GPU（可多行）、NPU（可多行）、运行内存总容量；
- 目录：数据目录、游戏目录；
- 项目：名称、版权；
- 项目依赖：名称/版本、作者、许可证。

Linux 系统组显示发行版名称和内核版本；Windows/macOS 显示系统名称和系统版本。

## 8. CAMLUI 组件

| 用途 | 组件/样式 |
|---|---|
| 应用根 | `app-root` / `caml-root` |
| 顶栏与主导航 | `CamlTopBar`、`CamlTopNav`、`CamlTopNavItem` |
| 页面宿主 | `CamlPageHost` |
| 侧栏 | `CamlSideNav`、`CamlSideNavItem` |
| 按钮 | `CamlButton`（filled/outlined） |
| 文本输入 | `CamlTextField`、`CamlPasswordField`、`CamlTextArea` |
| 可选不可滚动文本 | `CamlSelectableText` |
| 选择 | `CamlComboBox`、`CamlRadioButton`、`CamlCheckBox`、`CamlSwitch` |
| 数值 | `CamlSlider`、`CamlSpinner`、`CamlMemorySlider` |
| 进度 | `CamlProgressBar` |
| 图标 | `CamlIcon` |
| 卡片/分组 | `CamlCard` + `app-settings-group` |
| 主题 | `CamlTheme`、`CamlMetrics`、`CamlMotion` |

`CamlSelectableText` 使用 `Text` 节点和自绘选区，不包含 ScrollPane：原生不可滚动，同时支持鼠标选择、复制和反色选中文字。

## 9. 主题角色

主题文件必须提供以下颜色令牌，结构 CSS 只引用它们：

- 背景/表面：`-caml-background`、`-caml-surface`、`-caml-surface-low`、`-caml-surface-high`、`-caml-surface-highest`、`-caml-surface-container`、`-caml-surface-lowest`；
- 文本：`-caml-on-background`、`-caml-on-surface`、`-caml-on-surface-muted`；
- 强调：`-caml-accent`、`-caml-on-accent`、`-caml-accent-text`、`-caml-accent-container`、`-caml-on-accent-container`；
- 错误：`-caml-danger`、`-caml-danger-container`、`-caml-on-danger-container`；
- 边框：`-caml-border`、`-caml-border-subtle`；
- 导航：`-caml-nav-border`、`-caml-nav-focus-border`、`-caml-nav-divider`。

内置主题 `coffee`：白/米色表面、深棕文字、焦糖橙强调色；`night`：参考 IntelliJ IDEA 深色界面的黑灰表面、浅色文字和蓝色强调色。默认主题为 `night`，两者都按令牌成对使用文字色。

深色主题的明度层级固定为：页面背景最暗，分组/卡片居中，输入框/选择框使用 `-caml-surface-lowest` 作为最亮表面；该顺序由后端主题测试锁定。

## 10. 焦点、键盘与无障碍

- 表单控件有持久标签，并通过 `Label.setLabelFor` 关联主要输入。
- 按钮和导航项有可见焦点环；程序化切页时避免焦点错误落到顶栏第一个导航胶囊。
- `CamlRadioButton` 统一不参与焦点遍历，避免小圆形单选项出现额外焦点描边；键盘操作通过其所属组和标签完成。
- `CamlPageHost` 只承载页面，不参与焦点遍历；页面切换后按需选择第一个可聚焦控件。
- 可选命令文本支持鼠标拖动、双击选词、三击全选、`Ctrl/Cmd+A` 和 `Ctrl/Cmd+C`。
- 图标按钮和输入控件提供 accessible text；日志面板提供无障碍文本。

## 11. 动效

`CamlMotion.reduceMotion()` 读取系统属性 `caml.reduceMotion`：

- `true` 时页面切换、按压动画和设置内容淡入直接跳转；
- 具体组件仍自行检查该开关，例如 Java 文件夹按钮的缩放动画。

默认页面切换：

- 旧页淡出/上移约 90ms；
- 新页延迟约 30ms 后淡入/下移约 220ms；
- 侧栏同时从左侧和下方小幅进入；
- 动画完成后删除旧页面并恢复焦点。

## 12. 测试

当前 UI 测试覆盖：

- `UiScaleTest` / `WindowSizePlannerTest`；
- `AppCssScaleTest` / `ComponentScaleTest`；
- `WorkspaceLayoutTest`；
- `ModDownloadLayoutTest` / `ModResultRowLayoutTest`；
- `SidebarWidthRegressionTest` / `CamlPageHostSidebarTest`；
- `DownloadViewsLayoutTest` / `DownloadTelemetryTest`；
- `FocusStyleTest` / `InputContrastTest`；
- `AboutSettingsViewTest` / `SelectableTextControlTest`；
- `ComboBoxTextLayoutTest` / `MinecraftVersionComparatorTest`。

## 13. 当前界面限制

- 没有游戏运行中的停止按钮；
- 没有整合包导入/安装页；
- Mod 依赖只展示，不递归安装；
- 没有 macOS 原生安装包；
- 下载任务支持取消，但不提供暂停/继续按钮；
- 主题只支持外部 CSS，不提供可视化主题编辑器。
