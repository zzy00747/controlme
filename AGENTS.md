# AGENTS.md — ControlMe

ControlMe 是一个 Android 原生「AI 自律计划助手」:用中文自然语言描述安排,经对话式确认后生成结构化每日计划。离线优先,正式数据全部落本地 Room,仅当次对话文本上云 LLM 解析(隐私桥)。

## 技术栈

| 领域 | 选型 |
| --- | --- |
| 语言 | Kotlin(Android 原生) |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM + Repository,单向数据流 |
| 本地存储 | Room(计划/任务/大目标/备忘录)、DataStore(会话偏好) |
| 网络 | Retrofit + OkHttp(仅用于 LLM 解析) |
| DI | Hilt |
| 后台 | WorkManager(定时/复盘提醒) |
| 异步 | Kotlin Coroutines + Flow |
| LLM | OpenAI 兼容 / tool-calling 中文模型 |

## 目录结构

代码位于 `app/src/main/java/com/controlme/`:

```
ui/         界面层(Compose + ViewModel)
  conversation/   对话界面 + ConversationViewModel
  plan/           今日计划、打勾
  goal/           大目标与进度
  memo/           备忘录
  reminder/       提醒与通知
  clipboard/      剪贴板识别
domain/     领域层(纯逻辑,不碰 Android)
  model/          PlanTask, Goal, Memo, DraftItem, SessionState
  engine/         SessionCoordinator(核心)、DraftStateMachine
  llm/            LlmClient、tool schema、prompt 模板
data/       数据层
  local/          Room 实体 / DAO / Database
  repository/     PlanRepository, GoalRepository, MemoRepository
di/         Hilt 模块
```

## 架构不变量(必须遵守)

- 依赖方向单向:`ui → domain → data`。`domain/` 与 `model/` 不得 import Android SDK、Room 或 Retrofit,保证领域逻辑可独立单元测试。
- **未确认不落库**:只有 `SessionCoordinator` 在收到 `confirm_commit` 后才写 Room。LLM 永不直接写库,只产出工具调用(tool-calling);协调器将工具调用转为对草稿集合的确定性操作。
- LLM 的 tool schema / prompt 模板统一放 `domain/llm`,是唯一事实来源;UI 或 engine 不得各自重写一套。
- 落库后(COMMITTED)才允许打勾、挂进度;DRAFT 状态仅存在于对话上下文。
- Hilt 依赖关系遵循 `ui/domain/data/di` 分层,不跨层直连。

## 构建命令

```sh
./gradlew :app:assembleDebug          # 构建 debug APK
./gradlew :app:testDebugUnitTest      # 单元测试
./gradlew :app:lintDebug              # lint 静态检查
./gradlew :app:connectedDebugAndroidTest  # 仪器测试(需设备/模拟器)
```

- 依赖版本统一集中在 `gradle/libs.versions.toml`;升版只改这一处,不要散落在 `build.gradle.kts`。
- 新增依赖时先确认项目是否已引入,优先复用现有库,避免重复造。

## 提交与分支规范(Conventional Commit + 模块 scope)

- 分支:`<type>(<scope>)/<kebab-case-summary>`,如 `feat(ui)/plan-confirm-flow`。
- 提交 / PR 标题:`<type>(<scope>): <中文摘要>`,如 `feat(ui): 计划确认流程`。
- scope 取值:`app` `ui` `domain` `data` `di` `llm` `test` `repo` `docs`。
- PR 标题、正文、评审语言用中文;代码标识符、命令、日志、外部契约文本保持原文。

## 测试要求

- `domain/engine` 状态机必须覆盖 DRAFT→COMMIT / DELETE / MEMO 转换;不变量「未 COMMIT 不写库」必须有对应测试。
- LLM 协议测试用固定黄金样例,断言工具参数 JSON 结构正确(时间解析命中率)。
- Repository / Room 测试使用独立测试数据库(如 in-memory Room)。
- 测试要能证明行为恢复,而不是只复述实现细节。

## 文档

- `docs/requirements.md`、`docs/development.md` 是需求与开发基线;改动核心设计须同步更新。
- 代码审查规范见 `docs/review-checklist.md`。
- 架构决策记录到 `docs/adr/`。