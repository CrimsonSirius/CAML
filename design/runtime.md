# CAML runtime 包

更新日期：2026-11-03

## 1. 职责

`org.chs.camlauncher.backend.runtime` 负责游戏 Java 运行时的发现、识别和选择。

它只处理**游戏 Java**，不负责启动器自身运行的 JDK。

## 2. 类型

| 类型 | 职责 |
|---|---|
| `JavaRuntime` | 描述一个已发现的 Java 运行时 |
| `JavaRuntimeParser` | 解析 Java 版本、架构、位数和属性输出 |
| `JavaRequirement` | 描述某个游戏版本对 Java 的要求 |
| `JavaLocator` | 扫描本机 Java，并选择满足要求的运行时 |

## 3. JavaRuntime

`JavaRuntime` 是 record，包含：

| 字段 | 含义 |
|---|---|
| `executable` | java 可执行文件路径 |
| `version` | 完整版本字符串，例如 `25.0.4.1` |
| `majorVersion` | 主版本，例如 8、17、21、25 |
| `vendor` | Java 供应商 |
| `architecture` | 规范化架构，例如 `x86_64`、`arm64` |
| `bits` | 32 或 64 |
| `jdk` | 同目录是否存在 `javac` |

## 4. JavaRuntimeParser

负责纯文本解析，不执行进程：

- `parseProperties(output)`：解析 `key = value` 格式的属性输出。
- `parseMajorVersion(version)`：兼容 `1.8.0_402` 和 `25.0.4.1` 两类版本格式。
- `normalizeArchitecture(arch)`：把 `amd64`、`x64` 等统一为 `x86_64`。
- `parseBits(dataModel, architecture)`：解析 32/64 位，缺失时按架构推断。

## 5. JavaRequirement

`JavaRequirement` 包含：

```text
majorVersion
architecture
bits
```

从版本 JSON 创建：

```java
JavaRequirement requirement = JavaRequirement.from(versionInfo);
```

规则：

- 优先读取版本 JSON 的 `javaVersion.majorVersion`。
- 没有 `javaVersion` 时默认要求 Java 8。
- 架构使用当前系统架构。
- 位数使用当前系统位数。

`isSatisfiedBy(runtime)` 要求：

- `runtime.majorVersion() >= majorVersion`。
- 架构完全相同。
- `bits` 大于 0 时，位数相同。

## 6. JavaLocator

### 6.1 扫描来源

`JavaLocator.discover()` 会检查：

- 当前 `java.home`。
- `JAVA_HOME`。
- `PATH` 中的 `java` 或 `java.exe`。
- `~/.jdks`。
- `~/.gradle/jdks`。
- `~/.sdkman/candidates/java`。
- `~/.caml/java`。
- Linux/macOS 的 `/usr/lib/jvm`、`/usr/java`、`/opt`。
- Windows 的 `ProgramFiles` 下常见 JDK 目录和 `LOCALAPPDATA/Programs`。

### 6.2 识别 Java

对每个候选可执行文件运行：

```text
java -XshowSettings:properties -version
```

读取：

- `java.version`
- `java.vendor`
- `java.home`
- `os.arch`
- `sun.arch.data.model`

进程最多等待 5 秒。失败、超时或输出不完整的候选会被忽略。

### 6.3 去重和排序

- 按可执行文件路径去重。
- 按主版本降序排序。
- 通过 `java.home/bin/javac` 判断是否为 JDK。

### 6.4 选择 Java

`findSuitable(requirement)` 的优先级：

1. 主版本精确匹配。
2. 主版本差距更小。
3. 优先 JDK。
4. 路径排序保证结果稳定。

### 6.5 Windows javaw

`launchExecutable(runtime)` 在 Windows 下如果同目录存在 `javaw.exe`，优先返回它，避免启动游戏时弹出控制台窗口。

## 7. 对外入口

```java
JavaRuntime java = backend.javaLocator()
        .findSuitable(JavaRequirement.from(installed.manifest()))
        .orElseThrow();
```

## 8. 当前限制

- 不自动下载 Java。
- 架构要求完全一致，暂不支持 Rosetta 或 Windows ARM 转译。
- macOS 尚未扫描 `/Library/Java/JavaVirtualMachines`。
- Windows 尚未扫描注册表。
- 只扫描常见目录的一级子目录。

## 9. 测试

- `JavaRuntimeParserTest`：版本、架构、位数和属性解析。
- `JavaRequirementTest`：版本要求匹配。
- `JavaLocatorTest`：实际识别当前 Java 运行时。
