# ControlMe — AI 自律计划助手

用中文自然语言描述安排,自动整理成可执行的结构化每日计划;解析结果先停留在对话中,**经用户确认后才正式写入**,并围绕计划提供轻量的专注与复盘闭环。

> 状态:草案 / M0 脚手架阶段。需求与开发基线见 [`docs/requirements.md`](docs/requirements.md)、[`docs/development.md`](docs/development.md)。

## 核心思路

```
输入自然语言 → LLM 在对话流中产出计划草稿(未正式写入)
       ↓ 多轮澄清(改时间/补时长/冲突提醒)
  用户明确确认 → 正式写入结构化计划
       ↓
  打勾执行 · 看进度 · 每日重复
```

关键规则:

- **未确认不落库**:对话中的计划草稿只存在于对话上下文,不进入正式计划表;只有 `SessionCoordinator` 收到 `confirm_commit` 后才写 Room。
- **隐私桥**:正式计划/备忘录/勾选记录全部落本地 Room;云 LLM 只收到当次对话文本 + 对话上下文,不批量上传、不留存规划数据。

## 技术栈

Kotlin · Jetpack Compose + Material 3 · MVVM + Repository · Room · DataStore · Retrofit + OkHttp · Hilt · WorkManager · Coroutines + Flow · OpenAI 兼容 tool-calling LLM。

## 构建

```sh
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

给协作 Agent 的工程规则见 [`AGENTS.md`](AGENTS.md);代码审查规范见 [`docs/review-checklist.md`](docs/review-checklist.md)。