# 开发文档:AI 自律计划助手(暂定名)

> 状态:草案(待评审)
> 版本:v0.1
> 关联:《需求文档》`docs/requirements.md》

---

## 1. 技术选型

| 领域 | 选型 | 说明 |
| --- | --- | --- |
| 语言 | Kotlin | Android 原生 |
| UI | Jetpack Compose + Material 3 | 声明式 UI |
| 架构 | MVVM + Repository | 单向数据流 |
| 本地存储 | Room(计划/任务/大目标/备忘录)、DataStore(会话偏好) | 本地离线可用 |
| 网络 | Retrofit + OkHttp | 仅用于 LLM 解析请求 |
| 依赖注入 | Hilt | |
| 后台任务 | WorkManager(定时提醒、每晚复盘提醒) | |
| 异步 | Kotlin Coroutines + Flow | |
| LLM | 支持 function/tool-calling 的中文模型,OpenAI 兼容接口 | 解析 + 对话澄清 |

> **隐私桥**:正式计划/备忘录/勾选记录全部落本地 Room,云 LLM 只收到「当前这一次对话输入的文本＋对话上下文」。

## 2. 整体架构(分层)

```
┌────────────────────────────────────────────┐
│           UI 层 (Compose + ViewModel)        │
│   对话界面 · 计划列表 · 大目标 · 备忘录 · 提醒    │
└───────────────┬────────────────────────────┘
                │
┌───────────────▼────────────────────────────┐
│         会话协调层 SessionCoordinator         │
│   (草稿状态机 · 多轮对话上下文 · 操作执行引擎)     │
└───────┬───────────────────────────┬────────┘
        │                           │
┌───────▼──────┐          ┌─────────▼──────────┐
│ LLM 客户端      │          │  本地 Repository      │
│ (Retrofit+     │          │ (Room/DataStore)    │
│  tool-calling) │          └────────────────────┘
└───────────────┘
```

- **SessionCoordinator** 是核心:持有「对话上下文 + 草稿集合」,把 LLM 返回的工具调用转换为对草稿集合的确定性操作,把用户意图与渲染状态绑在一起。
- **LLM 不直接写库**:LLM 只产出"操作意图"(调工具),由协调器在收到确认信号后才把草稿落库。保证「未确认不落库」。

## 3. 目录结构(建议)

```
app/src/main/java/com/controlme/
  ui/
    conversation/   # 对话界面 + ConversationViewModel
    plan/           # 今日计划列表、打勾
    goal/           # 大目标与进度
    memo/           # 备忘录
    reminder/       # 提醒与通知
    clipboard/      # 剪贴板识别
  domain/
    model/          # PlanTask, Goal, Memo, DraftItem, SessionState
    engine/         # SessionCoordinator, DraftStateMachine
    llm/            # LlmClient, tool schema, prompt 模板
  data/
    local/          # Room 实体/DAO/Database
    repository/     # PlanRepository, GoalRepository, MemoRepository
  di/               # Hilt 模块
```

## 4. 核心设计:对话式「草稿 → 确认 → 落地」

### 4.1 会话状态(SessionState)

一次会话保有以下上下文,存于内存(可选持久化为未完成会话):

```
SessionState {
  drafts: DraftItem[]          // 本轮尚未确认的计划草稿
  committed: PlanTask[]        // 已确认落地的计划
  memos: Memo[]                // 已确认写入的备忘录
  messages: ChatMessage[]      // 与 LLM 的对话上下文(含工具调用记录)
  pendingConfirmCount: Int     // 待确认条数(用于 UI 角标/提示)
}

DraftItem {
  id, title, date, startTime, durationMin,
  repeat: RepeatPolicy?, parentGoalId?,
  needsClarification: List<ReviseQuestion>  // LLM 想追问的字段
}
```

### 4.2 草稿状态机

```
                    ┌→ 用户确认 ─→ COMMITTED(写 Room)→ 可打勾
  NEW ─ ADD → DRAFT ─┤
                    └→ 存为备忘录 ─→ MEMO(写 Room)
                    ├→ 用户删除 ─→ (丢弃)
```

- 每个 DraftItem 只能由「LLM 工具调用 / 用户显式修正」驱动流转;
- 只有 `CONFIRM` 动作会触发落库;落库后才允许打勾、挂进度。

### 4.3 与 LLM 的多轮协作(核心机制)

用一个 `System Prompt + 一组工具(tool-calling)` 约束 LLM 行为,LLM 每轮返回:**一段回复文本**(给用户看)+ **零到多个工具调用**。确保 LLM 无法跳过协调器直接改数据。

**工具清单(Schema 摘要):**

- `add_draft {title, date, start_time?, duration_min?, repeat?, parent_goal?}`
  新增一条草稿。时间表达由 LLM 结合"今天=YYYY-MM-DD 系统日期"解析为绝对 ISO 字段。
- `update_draft {id, ...可改字段}`
  修改草稿。
- `remove_draft {id}`
  删除一条草稿。
- `ask_clarification {draft_id?, questions[]}`
  发现缺时长/缺日期/冲突时,向用户提问,不擅自落库。
- `warn_conflict {draft_id, conflict_target_ids[]}`
  主动提示时间冲突并给建议(对话能力,不硬校验)。
- `suggest_memo {text}` / 由 `add_draft` 的 `is_memo` 分支承担
  判断不适合排期的内容引导进备忘录。
- `confirm_commit {draft_ids[]}`
  在用户明确确认后,标记这些草稿为可落地(协调器随后写 Room)。

**协调流程(伪代码):**

```
onUserMessage(text):
  session.messages.add(user(text))
  reply = llm.chat(session.messages)          // 文本 + tool_calls
  session.messages.add(assistant(reply))
  for tool in reply.tool_calls:
      session.apply(tool)                      // 仅操作 drafts/memos,不写库
  if reply 含 confirm_commit:
      repository.commit(session.committedDrafts)
      session.committed 清空,plans 刷新
  return render(reply.text, session.state)
```

### 4.4 时间解析策略

- LLM 在 `add_draft` 里直接把「明天/周X/X号/晚上8点/下午」解析成**绝对 ISO 字段**(系统把当前日期注入 prompt 作为参照);
- 重复规则输出 `RepeatPolicy{type: DAILY|WEEKLY|NONE, weekdays?}`;WEEKLY 转成每日生成或按星期展示;
- 解析采用工具强类型认证字段,降低自由文本格式漂移。

## 5. 数据模型(Room)

```
PlanTask  plan_id PK
  title, date(ISO), start_time?, duration_min?, is_daily_repeat,
  repeat_weekdays?, goal_id?, done(bool), done_at?, created_at, source(SESSION注入)

Goal      goal_id PK, title, target_note?, progress(派生), created_at
  # 进度 = 其下已 done 任务 / 总任务(由查询聚合,不冗余存储)

Memo      memo_id PK, text, created_at, done(bool)

Session   session_id PK, payload(JSON: 未完成草稿/上下文快照), updated_at
```

## 6. LLM 集成

- Retrofit POST 到 OpenAI 兼容端点;`tool_calls` 走 function-calling 协议;
- 请求带 `max_tokens` 上限、`temperature` 低温(≥0 保证结构化稳定);
- **超时与取消**:单轮对话请求 15s 超时;用户可中断,中断时草稿集合停留在上一稳定态;
- **隐私**:请求体仅含当前会话上下文;不携带本地库中未参与对话的数据。
- 解析质量兜底:输出若非合法工具调用,协调器按"仅文本"处理并显示,不写库。

## 7. UI 流程要点(Compose)

- **对话界面**:气泡流展示 LLM 文本 + 每条草稿以"可编辑卡片"内联呈现(改时间/改标题直接改卡,映射为 `update_draft`);底部是「确认 N 条 / 全部转备忘录 / 删除」操作条。
- **计划界面**:按日期分组,任务右侧复选框打勾;未完成提示。
- **大目标**:顶部进度条 + 关联任务数。
- **剪贴板**:App 可见时读取剪贴板文本,若可能含待解析内容,弹提示「识别到文本,解析为计划?」。
- **提醒**:WorkManager 做专注结束、晚间复盘(约 23:00,本地时间)的本地通知。

## 8. 测试策略

- **LLM 协议测试**:用固定输入的黄金样例,断言工具参数 JSON 结构正确(时间解析命中率)。
- **状态机测试**:单元测试 DRAFT→COMMIT/DELETE/MEMO 转换,invariant「未 COMMIT 不写库」。
- **Repository / 迁移测试**:Room 增删改查、ObjectBox→Room 类迁移不涉及。
- **冒烟路径**:剪贴板→解析→对话确认→落地→打勾,端到端用例。

## 9. 里程碑与任务拆分

- **M0(脚手架)**:工程初始化、Hilt、Room、Compose 骨架、CI。
- **M1(核心对话闭环)**:LLM 工具协议 + SessionCoordinator + 草稿状态机 + 对话 UI → 完成「输入→确认→落地→打勾」。
- **M2(周边)**:剪贴板识别、大目标进度、备忘录、每日重复。
- **M3(提醒与打磨)**:专注计时提醒、晚间复盘提醒、通知、空态/加载/错误态。
- **M4(验证)**:灰度使用、解析准确率与修正率埋点、访谈,决定是否进入 P1(软约束专注闭环)。

## 10. 市面对比与开源复用

> 完整论证见调研文档 `docs/research-product-comparison.md`(2026-08-24)。本节为结论与落地决策。star 数据均取自该调研的 GitHub REST API 快照;标注「未实测」的竞品需发布前真机复核。

### 10.1 差异化结论(直接竞品不存在,成立)

- **单点能力已被做成熟**:滴答清单、Todoist 官方都支持「中文自然语言 → 单条任务 + 自动时间解析」;iOS 屏幕使用时间 / Freedom 做系统级锁;iOS 系统助手做单条提醒。均为官方案例(调研文档 A1/A3)。
- **空白在于「闭环」**:没有产品把以下流程做成核心工作流——
  `一句话/大目标 → LLM 拆成一组日任务 → 对话式逐条确认 → 落库为「目标-每日任务」两层 → 番茄专注(软约束) → 专注结束强制复盘 → 晚间 AI 回顾 → 结果回写明日计划`。
- **成立前提**:把「确认(防幻觉)」和「闭环(专注→复盘→回写)」当作核心而非可选项;首版聚焦「够好的 LLM 解析 + 闭环体验」,系统级强锁只做可选增强。
- **主要风险**:单点 NLP 解析要打平滴答、系统锁要打平 iOS 需持续投入;用「确认 + 闭环」护城河对冲,不与单点在功能数量上硬刚。

### 10.2 开源复用清单(落地决策)

| 复用项 | 用途 | 决策 |
| --- | --- | --- |
| `vishal2376/snaptick`(750★) | Jetpack Compose 任务 + 番茄钟工程骨架,最贴近本项目 | **M0 脚手架参考** |
| `iSoron/uhabits`(10160★) | 「目标→每日记录→统计」数据模型 | **Room 表结构借鉴**(本开发文档 §5 由此强化统计字段) |
| `nsh07/Tomato`(1416★) | 番茄专注计时状态机(UI/状态) | **P1 专注计时参考** |
| `kasnder/redd-focus-android`(96★) / `code-with-the-italians/bundel`(295★) | Android 无障碍服务 + UsageStats 拦截分心应用 | **P2 可选系统锁参考** |
| `abhiz123/todoist-mcp-server`(392★) / `LearnPrompt/afu-llm-todo`(82★) | 任务结构化 JSON schema / Prompt 模板 | **LLM 输出 schema 与 prompt 蓝本** |
| `partial-json-parser-kmp`(7★) | KMP 断裂 JSON 修复 | **LLM 输出容错兜底(可选)** |
| `mlc-ai/mlc-llm`(23086★) / `google-ai-edge/mediapipe`(36701★) | 端侧推理 | **仅作离线/隐私兜底,不做主解析**(推迟到 P2) |

> **中文时间解析**:JVM/Android **无成熟可嵌入库**(如 `scrapinghub/dateparser` 2852★ 是 Python,仅服务端)。**决策:不引入难嵌入的解析库**,时间和任务统一交给 LLM 在一个 JSON 里产出,本地只留一层轻量正则兜底高频表达(「明天/晚上/每周三」)。

### 10.3 对 §4.3 的补充:两种解析模式的取舍

- **对话主路径**:继续用 **function-calling 逐条 `add_draft`**(多轮确认、逐条可改)——与「对话式确认」需求一致。
- **批量快速导入**:对「直接粘一段完整安排、不想逐条谈」的场景,加一条 **`response_format=json_object` 一次性输出整份 `{goal?, tasks[]}`** 的快速通道,一次性导入对话草稿,再统一走确认。
- 二者共用同一草稿集合与确认流程,不引入第二套落库逻辑。

### 10.4 需自研的难点(无可复用仓库)

调研明确:以下四块无现成方案,是自研重点——
1. 对话式逐条确认的**对话状态机**(多轮合并 / 撤销 / 改期),即 §4。
2. 目标 → 日计划的**拆解策略与进度回算**。
3. 专注结束**强制复盘**、**晚间回顾**的 Prompt 与数据回写。
4. LLM **幻觉与时间歧义的兜底**、用户确认机制。