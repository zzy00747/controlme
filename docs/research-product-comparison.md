# 「AI 自律计划助手」市面产品与开源可复用调研

> 调研日期：2026-08-24　面向产品：Android「AI 自律计划助手」（对话式确认 + 自然语言转每日计划 + 软约束专注闭环）
> 调研原则：优先一手来源（官方文档 / 官网 / GitHub 仓库与 README / 官方 API 说明）。每条关键结论标注来源 URL。
> 数据获取方式：官方网页抓取 + GitHub REST API（star 数为 2026-08-24 抓取值，未认证接口可能受限速影响）；多个搜索聚合源（Bing/Baidu）对本机代理不联通，故「产品叙事」以官方文档为主，个别产品用官方站 URL 佐证并注明"未能下载正文、仅据官方站与常识"。

---

## 一、市面产品对比

### A1. 自然语言 → 生成任务 / 计划（中文支持到什么程度）

| 产品 | 结论 | 一手来源 / 证据 |
|---|---|---|
| **滴答清单（TickTick / Dida365）** | ✅ 官方明确支持。官方功能页写明 *"NLP — Utilize NLP for task time setting. Just type in your information, and it will be identified instantly."* 即输入自然语言任务、自动识别时间并生成任务/提醒；另有**智能语音输入**、**AI 功能**板块（帮助中心单独列了「AI 功能」）。 | 官方功能页 https://dida365.com/about/features ；帮助中心（含「AI 功能」「番茄专注」模块）https://help.dida365.com/ |
| **Todoist** | ✅ 服务端自然语言日期解析是公开 API 能力。REST `create task` 支持 `due_string`（如 `today` / `tomorrow at 5pm` / `next Monday`，官方文档原文列出这三种示例），即把自然语言直接喂给服务端解析成结构化截止时间；App 端「Quick Add」（快捷添加）同一语义，支持中文日期表达。另有 AI 助手（Todoist Assistant）可理解需求并生成任务清单。 | Todoist REST API 文档 https://developer.todoist.com/rest/v2/ ；官网 https://todoist.com/ |
| **Notion AI** | △ 通用型 AI：定位是 "The AI workspace"—Capture / Find / Automate / agents，可让 AI 在数据库中生成文本、任务条目、结构化内容，也可由 Agent/自动化批量落数据；但**不是**「输入一句中文→出每日计划→逐条确认」的闭环产品，需要用户自己搭数据库/模板。 | 官网 https://www.notion.com/ |
| **Microsoft To Do** | ✖ 无自然语言解析能力。属于基础待办：列表 + 任务 + 到期日 + My Day + 提醒，需手动选日期；NL 解析不在官方功能描述中。功能定位可在官方产品页核对（curl 被微软反爬拦截，正文未下载成功，据此定性；请以产品实测为准）。 | 官方产品页 https://www.microsoft.com/en-us/microsoft-365/microsoft-to-do-list-app |
| **Any.do** | △ 有自然语言输入（英文较好），中文表达支持有限；未取到可验证的中文一手文档，需实测。 | 官网 https://www.any.do/ |
| **手机系统助手（Siri / 小米小爱 / 华为小艺）** | ✅ 语言→**单条提醒/备忘/日程**已成熟（"提醒我明天下午3点…"这类单任务闭环）。但都停留在**单条规则式提醒**，无「多任务拆解成每日计划」「目标拆解」「逐条对话确认后再落库」的能力。 | Apple Siri + 提醒 官方指南 https://support.apple.com/zh-cn/guide/iphone/iph83aad8922/ios |
| **国内 AI 日程/办公类（钉钉 AI、通义/Moonshot 入口、企业微信智能助手类）** | △ 钉钉官网以 "AI 时代的工作方式" 卖 AI 助理（AI 日程/会议纪要/排期），属企业协同向；个人向「把一句话变成每日任务表并确认"的轻量产品尚未见明确的一手文档成主流。 | 钉钉官网 https://www.dingtalk.com/ |

> **A1 小结**：中文「一句话 → 结构化任务/日期」的**单条解析**已被滴答清单、Todoist 等做成熟（且都在服务端做 NLP/LLM）。但把「自然语言描述 → 拆解成**一组**每日任务 → 逐条对话式确认 → 落库为可执行计划」的**循环流程**，没有产品做成核心工作流——它们更接近"输入框即时解析"，无确认、无目标关联、无后续专注闭环。

### A2. 任务与目标分层（大目标 → 每日任务两层结构 + 进度）

| 产品 | 两层结构 | 进度/统计 | 来源 |
|---|---|---|---|
| 滴答清单 | 清单(List) → 任务 → 子任务/清单项；另有「习惯打卡」「番茄专注」统计 | 打卡/专注/月视图"每月总结复盘" | https://dida365.com/about/features ；https://help.dida365.com/ |
| Todoist | 项目(Project) → 区块(Section) → 任务 → 子任务 | 完成率/Karma 生产力分 | https://developer.todoist.com/rest/v2/ |
| Notion | 页面/数据库任意嵌套（目标库→每日任务库手动关联） | 视建库方式而定，非内置 | https://www.notion.com/ |
| Microsoft To Do | 列表 → 任务（无真正嵌套） | 完成勾选，无目标级进度 | https://www.microsoft.com/en-us/microsoft-365/microsoft-to-do-list-app |
| Loop Habit（开源参考） | 目标/习惯 → 每日打卡记录 → 长期统计/回顾 | 连续天数、统计图最强 | https://github.com/iSoron/uhabits |

> **A2 小结**：分层与进度各家都有"形式"，但多为**手动**（建子任务、选目标），没有"输入大目标 → 自动拆解成每日任务并跟踪总进度"的智能层。这是「目标→日计划+AI 拆解」可切入的空白。

### A3. 专注 / 自律（软提醒 vs 系统级强锁）

| 产品 | 机制 | 强锁 or 软约束 | 来源 |
|---|---|---|---|
| **Forest** | 种虚拟树，"若离开 App / 使用被屏蔽 App，树会死亡"（strict session）；组队专注时"任一成员放弃则全队树死"；无系统级拦截，靠心理/游戏化惩罚 | 软约束（应用内遮罩+惩罚性积分） | 官网 https://www.forestapp.cc/ （原文："If you leave the app or use blocked apps during strict sessions, the tree dies"） |
| **iOS 屏幕使用时间（Screen Time）** | 系统级 "App Limits/App 限额"：设定每日限额，到达后阻止继续使用，需输入(家长)密码/申请延长；可规划停用时段、阻止内容 | **系统级**软锁 | Apple 官方 https://support.apple.com/en-us/HT208982 （"schedule daily app limits, block content"） |
| **番茄ToDo / 各番茄钟 App（含国内）** | 番茄计时 + 专注锁机（学习时锁其他 App）、自习室、数据统计。锁机普遍是**应用内弹层/无障碍提醒**，非系统级强锁 | 多为软约束/应用内锁 | 官网抓取被反爬拦截，未取到可引用一手正文；请以 App 实测为准（官方站搜索入口：番茄ToDo 应用）
| **Freedom** | 跨平台阻断（按会话/计划屏蔽网站与 App），可在系统层(含 iOS/Android 的 Accessibility/VPN 类机制)强制进入"锁定会话" | 更强（近系统级跨设备） | 官网 https://freedom.to/ （正文未下载成功，据此定性，请实测） |

> **A3 小结**：市面分两派——**系统级强锁**（iOS 屏幕使用时间、Freedom 类）与**软约束/游戏化惩罚**（Forest 类）。我们产品定位"软约束专注闭环"，介于两者：用**专注计时 + 选择是否跟随系统锁 + 结束时相关通知/复盘**的组合，不与 iOS/系统强锁硬碰，也不仅是"树死"式惩罚游戏——这块的**结合点（专注→复盘→明日计划回写）目前无产品做成闭环**。

### A4. 认领 / 复盘闭环（专注结束强制复盘、晚间回顾）

- **Forest**：专注历史/森林沉淀（"your tree joins your forest forever"），但没有"结束必填复盘"，无晚间对话式回顾。 https://www.forestapp.cc/
- **滴答清单**：月视图"每月总结复盘"、番茄专注数据、习惯统计——是**被动统计**复盘，非"强制输入 + 对话式回顾"。 https://dida365.com/about/features
- **iOS 屏幕使用时间**：每周使用报告（被动报表）。 https://support.apple.com/en-us/HT208982
- **番茄ToDo**：专注记录/自习室/统计（被动记录为主）。

> **A4 小结**：现有产品做的是**统计报表/被动沉淀**，几乎没有「专注结束**强制**录入复盘 + 晚间 AI 对话式回顾 + 把复盘回写进明日计划」的**主动闭环**。这是最明显的差异化空白。

### A 部分汇总对照表

| 维度 | 滴答清单 | Todoist | Notion AI | 微软To Do | Forest | iOS 屏幕使用时间 | 国内AI日程/系统助手 |
|---|---|---|---|---|---|---|---|
| 中文 NL→任务/日期 | ✅ 官方 NLP | ✅ due_string | △ 通用AI | ✖ | — | — | ✅ 单条提醒 |
| 一句话→**一组**每日计划+确认 | ✖ | ✖ | ✖ | ✖ | ✖ | ✖ | ✖ |
| 目标→日任务 智能拆解+总进度 | △ 手动 | △ 手动 | △ 需自建 | ✖ | ✖ | ✖ | ✖ |
| 专注软约束 | △ 番茄 | ✖ | ✖ | ✖ | ✅ 树死惩罚 | ✅ 系统级限额 | — |
| 专注结束**强制**复盘+晚间回顾闭环 | ✖ | ✖ | ✖ | ✖ | △ 被动 | △ 被动 | ✖ |

---

### A 部分结论：差异化是否成立？

**市面有「自然语言→单条任务解析」的成熟产品（滴答清单、Todoist）和「系统级锁机」（iOS 屏幕使用时间、Freedom）以及「专注游戏化」（Forest）作为单点能力，但没有产品把三者串成一条完整闭环：**

```
一句话/大目标 → LLM 拆成多任务 → 对话式逐条确认 → 落库为「目标-每日任务」两层
→ 当日按计划番茄专注（软约束/可选系统锁） → 专注结束强制复盘录入
→ 晚间 AI 回顾 → 复盘结果回写明日计划
```

**结论：直接竞品不存在，差异化成立。** 成立的前提是把"**确认**"（对话式逐条确认防幻觉）和"**闭环**"（专注→复盘→回写）做成核心而非可选项，这是滴答/Todoist/Forest 都不会做、也因产品形态（清单应用/游戏）很难快速跟进的地方。主要风险不是"有竞品抄"，而是**单点能力要打平滴答的 NLP 和 iOS 的系统锁需要投入** —— 建议首版聚焦"足够好的解析 + 闭环体验"，系统级强锁可借道 Android 无障碍/`UsageStats` 做成可选增强。

---

## 二、开源可复用（GitHub 优先）

> star 数为 2026-08-24 GitHub REST API 抓取值；"ERR/未核"表示该时点接口限速未取到，已注明。

### B1. 中文自然语言 → 时间/日期解析库

| 仓库/包 | 语言 | star | 说明与 Android 可嵌入性 |
|---|---|---|---|
| `scrapinghub/dateparser` | Python | 2852 | 多语言人类可读日期解析，支持中文（`# zh`）。规则最全、维护最久。**Python 库，不可直接嵌入 Android**（除非上 Chaquopy/独立服务），仅适合作**服务端解析参考**。 https://github.com/scrapinghub/dateparser |
| `taccisum/cntime-nlp` | Python/JVM? | 1 | 同名即「中文自然语言时间表达式解析」，但 star 极少、不成熟。 https://github.com/taccisum/cntime-nlp |
| `shouya/natural-time-zh` | 不明 | 2 | "parsing time in natural chinese speech"，实验级。 https://github.com/shouya/natural-time-zh |
| `lilianyu/SmartTimeParser` | Java(Android) | 0 | 明确标注 "Chinese time parser in android **based on regex**"，是极少数 Android 本地方案，但基于正则、覆盖有限。 https://github.com/lilianyu/SmartTimeParser |
| `BZHDZNJJ/zh-time-parser` | Python | 0 | 中文时间语义解析（"上个月/明天下午3点/截至月底"），零依赖，实验级。 https://github.com/BZHDZNJJ/zh-time-parser |

**推荐：无"开箱即用、可直接嵌入 Android 的成熟 JVM 中文时间解析库"**（JVM/Kotlin 侧检索无匹配，见下）。
- 若坚持**纯本地规则**：可自研「正则 + 有限状态机」，参考 ①Android 的 `lilianyu/SmartTimeParser`（正则骨架）②Python `scrapinghub/dateparser` 的中文词表与规则（抄思路，代码不搬）。
- **更优解**：把时间解析交给 LLM（见 B2/B3），在**一个 JSON 里同时得到 task 时间与结构**，避免维护两套解析；本地只需一层轻量正则兜底"明天/今晚/每周三"这类高频表达。
- 检索佐证（JVM/Kotlin 侧无强匹配）：https://github.com/search?q=kotlin+time+parser （结果为 TOML/通用解析器，非中文时间语义库）

### B2. 自然语言任务 → 结构化 task 的现成库 / Prompt 模板 / JSON schema 方案

| 方案 | 一句话 | source |
|---|---|---|
| `abhiz123/todoist-mcp-server`（392⭐） | MCP server，把 Todoist 任务增删查暴露给 LLM，内含**结构化任务操作 schema**，可作为 task JSON-schema 参考 | https://github.com/abhiz123/todoist-mcp-server |
| `flesler/mcp-tasks`（47⭐） | MCP 任务管理，Markdown/JSON/YAML 多格式 | https://github.com/flesler/mcp-tasks |
| `bgheneti/Amazing-Marvin-MCP`（41⭐） | 任务/项目/分类结构化 schema 参考 | https://github.com/bgheneti/Amazing-Marvin-MCP |
| `LearnPrompt/afu-llm-todo`（82⭐） | LLM 把"收件箱🗄备注"转成"本周 todo 卡片"的**提示词与流程**范例 | https://github.com/LearnPrompt/afu-llm-todo |
| Python 侧结构化输出 | `instructor` / `outlines`（开箱结构化/JSON-schema 约束生成），印证"LLM 结构化输出"路线成熟 | （Python，服务端参考） |

**推荐：无现成"中文 NL→task JSON schema"一键库，** 应**自研 Prompt 模板 + JSON Schema + 服务端校验**：
1. 用 LLM **JSON-mode / response_format** 一次性输出 `{goal?, tasks:[{title,date,time,priority,recurrence,notes}]}`；
2. 本地用 JSON Schema（或 Kotlin 数据类 + `kotlinx.serialization`）**校验 + 修复**（可选接 `FunnySaltyFish/partial-json-parser-kmp`，7⭐，KMP 的断裂 JSON 修复库）；
3. 参考上表 MCP/LLM-todo 仓库的任务字段模型作为 schema 设计蓝本。

### B3. Android 端 LLM 调用与 JSON-mode 解析

| 方案 | star | 说明 |
|---|---|---|
| **云 API + Retrofit/OkHttp** | — | 主推。调用 OpenAI/DeepSeek/通义等 `chat/completions` 的 `response_format={type:"json_object"}`（或 function-calling 强制 JSON），中文→结构化最稳；Retrofit + kotlinx-serialization 反序列化是 Android 标准做法。 |
| `mlc-ai/mlc-llm` | 23086 | 端侧 LLM 部署引擎（TVM 编译），支持 Android；可跑 Qwen 等小模型做离线兜底/隐私场景。 https://github.com/mlc-ai/mlc-llm |
| `google-ai-edge/mediapipe` | 36701 | Google 官方，MediaPipe LLM Inference API 支持端侧跑 Gemma/Qwen；另有 ML Kit（实体抽取、推荐等离线 NLP）可做"无 LLM 时的规则/实体兜底"。 https://github.com/google-ai-edge/mediapipe |
| `ggerganov/llama.cpp` | 未核(大) | 端侧 GGUF 推理，Android 可链接；star 具体值该时点接口限速未取到，仅作定性引用 https://github.com/ggerganov/llama.cpp |

**推荐：主架构 = 云 LLM（Retrofit + response_format JSON）**；端侧推理（mlc-llm/mediapipe）作为**离线/隐私增强**，不承担主解析（中文质量与 prompt 可控性云侧更稳）。原因：①JSON-mode 现成、Android 生态成熟；②中文任务打标/to-do schema 无需超大模型；③云侧迭代 prompt 快、无端侧体积与机型兼容成本。

### B4. Android 专注 / 自律开源 App（可整体复用或参考）

| 仓库 | star | 说明 / 可复用点 |
|---|---|---|
| `iSoron/uhabits`（Loop 习惯打卡） | 10160 | **数据模型最佳参考**：目标/习惯 → 每日打卡记录 → 长期统计与回顾，正是「目标→每日记录→复盘」的离线数据层范式，Kotlin。 https://github.com/iSoron/uhabits |
| `super-productivity` | 21544 | 任务 + 番茄 + 时间追踪 + 本地优先；**是 Electron/桌面Web 应用，非 Android 原生**，架构价值>代码复用价值。 https://github.com/super-productivity/super-productivity |
| `vishal2376/snaptick` | 750 | Jetpack Compose 全栈范文：任务管理 + 内置番茄钟 + MVVM，**最贴近"日计划+专注"的 Android 工程**。 https://github.com/vishal2376/snaptick |
| `nsh07/Tomato` | 1416 | Material3 Compose 纯番茄钟（数据导向），专注计时 UI/状态机参考。 https://github.com/nsh07/Tomato |
| `kasnder/redd-focus-android` | 96 | **无障碍(accessibility)服务**监控/屏蔽分心应用——"软约束 + 无障碍拦截"的可参考实现。 https://github.com/kasnder/redd-focus-android |
| `code-with-the-italians/bundel` | 295 | 数字健康 Android 应用，应用限时/自我约束，软约束 + 无障碍思路参考。 https://github.com/code-with-the-italians/bundel |
| `Maxr1998/MaxLock` | 234 | Android App 锁（root & 无 root），应用级锁定实现参考（与"强制专注"可结合）。 https://github.com/Maxr1998/MaxLock |

> 另有实验级无障碍拦截：`ABHILESH1412/sankalp`（6⭐）、`Shanners45/FocusLock`（5⭐）可作"拦截短视频"最小实现参考。整体看：**没有"AI 计划 + 专注 + 复盘"一体化的成熟开源 App**，需自行拼装。
> 补充参考（竞品同类的开源习惯/游戏化）：`HabitRPG/habitica`（14085⭐，目标游戏化）属习惯养成向，个别数据/激励设计可借鉴：https://github.com/HabitRPG/habitica

**推荐组合（B4）**：以 `vishal2376/snaptick` 为 Android 工程骨架 → `iSoron/uhabits` 抄"目标-每日记录-统计"数据模型 → `nsh07/Tomato` 做番茄计时状态机 → 无障碍拦截参考 `kasnder/redd-focus-android`。**"对话确认 + LLM 拆解 + 强制复盘回写"这一核心逻辑无可复用仓库，需自研。**

---

## 三、技术选型建议

- **语言/框架**：Kotlin + Jetpack Compose（对照 `snaptick`/`Tomato`/`uhabits` 的成熟栈）；架构 MVVM / 单向数据流，本地离线优先。
- **LLM 接入**：主走**云 API + Retrofit**，用 `response_format=json_object`（或 function-calling）在**一次调用**里产出整份 `{goal, tasks[]}` JSON；配置可切 DeepSeek / 通义 / OpenAI 兼容端点（国内网络选国内模型）。**端侧推理（mlc-llm / MediaPipe Gemma / llama.cpp）作为离线兜底**，不作为主路径。
- **NL→结构化**：不引入难以嵌入的 Python 日期库；用 **LLM 统一解析时间+任务**，本地配一层轻量正则兜底高频表达（"明天/晚上/每周三"）；JSON 落地用 `kotlinx.serialization` + 容错解析（可兼参考 `partial-json-parser-kmp`）。
- **存储**：Room（离线任务/计划/专注记录/复盘）为源；`uhabits` 的"目标-每日记录-统计"表结构可直接借鉴；可选远程同步留接口。
- **专注/软约束**：番茄计时自研（状态机参考 `Tomato`）；"可选系统级锁"用 **Android 无障碍服务 + `UsageStatsManager`** 做应用拦截（参考 `redd-focus-android`/`bundel`），注意无障碍权限的合规与提示；**不要承诺 iOS 式系统强锁**。
- **需自研的难点**：①对话式逐条确认的**对话状态机**（多轮合并/撤销/改期）；②目标→日计划的**拆解策略与进度回算**；③专注结束**强制复盘**与**晚间回顾**的 prompt 与数据回写；④解析结果**兜底与纠错**（LLM 幻觉、时间歧义的用户确认）。

---

## 附录 来源 URL 汇总

- 开源（GitHub）：`scrapinghub/dateparser`(2852)、`iSoron/uhabits`(10160)、`mlc-ai/mlc-llm`(23086)、`google-ai-edge/mediapipe`(36701)、`vishal2376/snaptick`(750)、`nsh07/Tomato`(1416)、`super-productivity`(21544)、`kasnder/redd-focus-android`(96)、`code-with-the-italians/bundel`(295)、`Maxr1998/MaxLock`(234)、`HabitRPG/habitica`(14085)、`abhiz123/todoist-mcp-server`(392)、`LearnPrompt/afu-llm-todo`(82)、`taccisum/cntime-nlp`(1)、`lilianyu/SmartTimeParser`(0)、`FunnySaltyFish/partial-json-parser-kmp`(7) —— 均取自动 GitHub REST API。
- 产品一手：https://dida365.com/about/features ・ https://help.dida365.com/ ・ https://developer.todoist.com/rest/v2/ ・ https://todoist.com/ ・ https://www.notion.com/ ・ https://www.forestapp.cc/ ・ https://support.apple.com/en-us/HT208982 ・ https://support.apple.com/zh-cn/guide/iphone/iph83aad8922/ios ・ https://www.microsoft.com/en-us/microsoft-365/microsoft-to-do-list-app ・ https://www.any.do/ ・ https://www.dingtalk.com/ ・ https://freedom.to/

> 未验证提示：①Microsoft To Do / Any.do / 番茄ToDo / Freedom 的正文抓取被反爬拦截，结论基于官方产品定位与常识，建议发布前对这几项做**真机实测复核**；②未认证 GitHub API 偶发限速，个别 star（如 llama.cpp）标注"未核"，引用时避免写死数字。