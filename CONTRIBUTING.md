# CAML 贡献指南

本文说明 CAML 的协作方式、提交信息建议和 Pull Request 审查约定。

> 提交信息格式是建议，不是硬性规范；仓库不配置 commit hook 或自动提交检查，PR 由维护者人工审查。

## 分支与协作

项目当前为双人协作，分支约定见 [README.md](README.md) 的“协作约定”一节：

- `main`：主分支，保持可构建状态；
- `crimson`：CrimsonSirius 的开发分支；
- `namelessgod2008`：namelessgod2008 的开发分支。

建议在自己的开发分支上提交，通过 Pull Request 合入 `main`。提交前确认仓库级 Git 身份正确；`.mailmap` 只用于统一展示和统计，不改写历史提交。

## 提交信息建议

推荐参考 [Conventional Commits](https://www.conventionalcommits.org/) 组织提交信息，便于阅读和后续生成变更记录，但不要求每个提交严格符合格式。

推荐格式：

```text
<type>(<scope>): <summary>
```

`scope` 可以省略。常用 type：

| type | 用途 |
|---|---|
| `feat` | 新功能 |
| `fix` | 修复问题 |
| `docs` | 文档 |
| `refactor` | 不改变行为的重构 |
| `perf` | 性能优化 |
| `test` | 测试 |
| `build` | 构建、依赖、打包 |
| `ci` | GitHub Actions 等 CI 配置 |
| `chore` | 杂项维护 |
| `revert` | 回滚 |

常用 scope 尽量对应模块或领域：

```text
backend front ui language
auth download instance launch loader metadata runtime mod
theme settings packaging build docs ci
```

示例：

```text
feat(theme): add night theme
fix(front): remove navigation capsule focus border
fix(theme): darken input and mod result surfaces
docs(design): document theme surface hierarchy
build(packaging): pin Zulu 25 runtime
refactor(launch): extract classpath ordering
```

破坏性变更可以在 type 后加 `!`，并在正文或 footer 中说明：

```text
feat(auth)!: replace legacy token storage

BREAKING CHANGE: stored accounts must be re-encrypted.
```

如果提交信息不符合上述建议，只要标题和正文能清楚说明“改了什么、为什么改”，仍然可以提交；维护者会在 PR 审查时结合改动内容判断。merge commit 不需要套用该格式。

## Pull Request

PR 标题建议使用与提交信息相同的风格，例如：

```text
feat(theme): add night theme
fix(front): keep focus ring off navigation capsules
```

PR 描述建议包含：

- 改动目的和用户可见行为；
- 涉及的模块和主要文件；
- 验证方式，例如执行的测试、手动验证步骤或截图；
- 已知限制、兼容性影响或破坏性变更。

PR 由维护者人工审查后合并，不依赖自动提交规范检查。审查重点包括：

- 是否符合模块边界和现有设计；
- 是否引入不必要的复杂度或无关改动；
- 是否有对应的测试或可复现的验证步骤；
- UI 改动是否处理了深色/浅色主题、缩放和键盘焦点；
- 是否同步更新了相关文档。

审查意见优先于提交格式建议；格式问题不应阻塞有明确价值的改动。

## 提交前检查

根据改动范围选择执行：

```bash
# 全量测试
./gradlew test

# 后端
./gradlew :CAMLBackend:test

# 前端
./gradlew :CAMLFront:test

# 单个测试类
./gradlew :CAMLBackend:test --tests 'org.chs.camlauncher.backend.theme.ThemeServiceTest'

# 启动前端
./gradlew :CAMLFront:run
```

涉及打包、Java 运行时或发布流程时，按 [design/package.md](design/package.md) 的说明执行对应平台的打包任务。

## 代码与文档约定

项目架构和模块边界见 [AGENTS.md](AGENTS.md) 与 `design/` 目录。提交前尤其注意：

- `CAMLBackend` 保持纯 Java，不依赖 JavaFX 或 `CAMLFront`；
- `CAMLFront` 不直接读写文件、发 HTTP 或启动进程；
- `CAMLUI` 不依赖后端，结构 CSS 不写固定配色；
- `CAMLLanguage` 的语言 JSON 由生成器生成，不直接手改后遗漏生成任务；
- 不引入第三方 Minecraft 启动库，不提交 Minecraft 客户端、assets、libraries 或游戏资源；
- 改动相关行为时同步更新测试和文档，避免文档描述与当前代码不一致。

提交前请避免把构建产物、临时文件、个人配置或与本 PR 无关的修改混入提交。
