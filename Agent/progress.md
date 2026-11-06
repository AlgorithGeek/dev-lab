# AI Agent 学习进度

> 这里记录当前实际学习状态。
>
> Node 表示知识节点，不对应自然日。
>
> - `README.md`：说明这个 Agent 学习区怎么使用
> - `Agent Engineer 循序渐进学习路线.md`：记录完整学习路线
> - `progress.md`：只记录当前学到哪里
>
> 完整路线以 `Agent Engineer 循序渐进学习路线.md` 为准。

---

## 当前状态

```text
状态：🟡 学习中
阶段：Phase 1 / LLM 应用开发基础
当前节点：Node 021 - 第一阶段项目（准备学习）
当前笔记：020-1 已读完，020-2 一组真实调用及四组本地模拟已完成；下一步 Node 021

主项目：projects/agent-learning-lab
项目状态：已跑通单轮、历史、Prompt 对照、Structured Output → DTO 及文本 Streaming 的真实正常链路

主要语言：Java
主要后端框架：Spring Boot
当前 Provider：阿里云百炼 Model Studio
当前主模型：qwen3.8-max
API 风格：OpenAI-compatible
```

---

## 当前阶段进度

### Phase 1：LLM 应用开发基础

- ✅ Node 008：第一次调用模型 API
- ✅ Node 009：Message 模型（阅读、代码学习与必做实验完成）
- ✅ Node 010：System Prompt（学习与两阶段正常实测完成；新增本地模拟待执行）
- ✅ Node 011：Prompt 基础（阅读讲解与两阶段正常实测完成；新增本地模拟待执行）
- ✅ Node 012：Few-shot（理论理解完成，不单独安排 Java 实验）
- ✅ Node 013：Structured Output（理论阅读完成，不单独安排 Java 实验）
- ✅ Node 014：JSON Schema（理论阅读、User Schema 练习与修正，按用户确认收尾）
- ✅ Node 015：Java DTO 与 Structured Output（理论、真实正常链路与两个本地坏数据样本完成）
- ✅ Node 016：Streaming（默认/Debug 正常实测与讲解完成；新增本地模拟待执行）
- ✅ Node 017：模型参数（理论阅读完成，不单独安排参数实验）
- ✅ Node 018：模型选择（理论阅读完成，不单独安排跨模型实验）
- ✅ Node 019：Context Engineering（理论阅读与五组实验完成）
- ✅ Node 020：LLM API 异常（理论阅读与五组实验完成）
- 🟡 Node 021：第一阶段项目（准备学习）

> 这里只展开当前 Phase。后续完整节点请查看 roadmap。

---

## 当前实践

- ✅ 第一轮 Token Usage 对比实验
  - 已通过 Apifox 对比短 / 长 Prompt 与短 / 长回答
  - 已观察 `prompt_tokens`、`completion_tokens` 和 `total_tokens`
- ⬜ Token 切分 / Tokenizer 实验
  - 暂不单独折腾 Tokenizer，后续结合真实需要再做
- ✅ Context Budget / Lost in the Middle 对比实验（Node 019）
  - A 组输入 102 Token，B 组加入无关资料后为 331 Token，两组答案相同。
  - C1 / C2 / C3 分别把目标资料放在开头、中间和结尾，三组均答对，输入均为 331 Token。本次未观察到位置影响答案。
- ✅ 创建 `projects/agent-learning-lab`
  - 最小 Maven Java 21 项目；HttpClient 调用、FastJSON2 解析与错误路径已验证
- ✅ 第一次 Codex 学习协作
  - 已保存两轮 `codex/prompts` 与 `codex/handoffs`
- ✅ 有历史 / 无历史对照实验
  - IDEA 真实运行：A 回答“无法确定”，B 回答“青柠47”；输入 Token 为 32 / 63
- ✅ 自动维护 Chat History
  - 代码、三轮请求 JSON 注释、编译与本地模拟检查已完成
  - IDEA --history 三轮真实成功：发送 1/3/5 条，保存 2/4/6 条；输入 Token 为 28/51/90，完整历史已记录
- ✅ System Prompt 两阶段真实正常调用
  - A/B 输入 Token 为 40/138，输出为 1001/398；完整回答及观察已记录
  - 多轮请求角色为 [system,user] / [system,user,assistant,user]，历史最终四条且不含 System
  - 多轮输入/输出/总 Token 为 70/36/106、134/60/194，两次回答均带 JAVA-REVIEW: 前缀
- ⬜ Node 010 新增本地模拟检查
  - 检查脚本已编写，尚未执行；真实正常调用不代表错误路径已验证
- ✅ Prompt 基础 A/B 与 B/C 真实正常调用
  - 同一代码、固定 user 角色，对比模糊/结构化 Prompt，再观察删除背景与约束调整的效果
  - 两阶段完整消息、原始回答、Token 和模型错误已记录，未将本次 B 覆盖旧 B
- ⬜ Node 011 新增本地模拟检查
  - 代码已编写，尚未执行；不将真实正常调用等同于错误路径验证
- ✅ Few-shot 基础认知
  - 已理解 Zero-shot / One-shot / Few-shot，以及示例属于 Context 而不是训练
  - 已理解示例质量、覆盖、偏差及 Token 成本，Few-shot 不保证优于 Zero-shot
  - 当前不单独做 Few-shot Java/API 实验；真实业务需要时再结合样本集和 Evaluation 验证
- ✅ Structured Output 基础认知
  - 用户已读完 Node 013，理解结构化结果、数据契约以及结构正确不等于事实正确
  - 按笔记安排不单独新增 Java Demo 或 API 实验
- ✅ JSON Schema 基础认知与 User 手写练习
  - 用户已读完 014，并提交 name/string、age/integer 的 User Schema
  - 已指出并修正 required 应与 properties 同级、最外层缺闭合括号的问题；用户确认本节收尾，不再追加其他练习
  - 其余三项建议练习未执行，未运行 Schema 校验器或模型 API，不将这些项目记为已完成
- ✅ Structured Output → Java DTO
  - IDEA 真实 Schema 请求、业务 JSON、FastJSON2 反序列化及四个 getter 输出已完成，三次实际结果分别保留
  - --errors 已在本地执行：缺少 suggestion、非法 riskLevel 均可转 DTO，再由共用字段检查拒绝
  - 非法 JSON / 错误字段类型样本及完整 HTTPS 脚本未执行，未引入完整 Schema 或业务事实校验
- ✅ Streaming 默认 / Debug 真实正常调用
  - 读取 SSE、提取正文增量、实时 print/flush、拼接最终回答、finish / Usage / DONE 均有实际结果与代码路径支撑
  - 默认与 Debug 两次耗时、Token、完整回答分别记录，已保存五类代表事件
- ⬜ Node 016 新增本地模拟检查
  - 九种场景已编写但未运行；读流空闲超时没有单独实现，不记为生产级容错完成

- ✅ 模型参数与模型选择基础认知
  - 用户已读完 017 / 018，按理论学习范围收尾
  - 参数控制生成策略与资源边界；模型选择先筛硬条件，再以业务样本权衡质量、成本、延迟和稳定性
  - 未执行参数对比或跨模型评测，未接入第二个 Provider，也未创建 Provider 抽象

---

## 下一步

> **Node 021：第一阶段项目（准备学习）**

Smart Text Analyzer 将前面学过的模型调用、结构化输出、DTO 和异常处理组合进一个 Spring Boot 接口。

---

## 最近进度

### 2026-11-06

- 已读完 020-1。020-2 的五组实验均得到预期结果，已记录到实验笔记。
- 真实错误模型返回 404 和 model_not_found，程序读出错误正文后结束。
- 本地 429 / 503 等待 300 ms 后各重试一次，第二次返回 200。超时和上下文过长各只发送一次。
- 下一步准备学习 Node 021「Smart Text Analyzer」。

### 2026-11-05

- 已读完 019-1，019-2 的 A、B、C1、C2、C3 五组真实调用均返回 HTTP 200 并答对。
- A / B 输入为 102 / 331 Token，追加无关资料多用 229 Token。C1/C2/C3 输入均为 331 Token，本次未观察到资料位置影响答案。
- 已记录答案、用量和耗时。每组一次调用，无法据此判断耗时规律。下一步准备学习 Node 020。

### 2026-11-04

- 用户新增并确认读完 017「模型参数到底是什么？」与 018「模型选择到底在选什么？」
- 已统一 017 正文标题、完成标准及总结编号，去掉残留的 017-1；两篇笔记补充阅读与收尾记录
- Node 017 / 018 按理论学习范围完成，未另行逐题口述验收，不单独创建参数实验或跨模型实验
- 未执行真实参数对比、跨模型 Benchmark 或第二个 Provider 接入，未创建 Provider 抽象；未改动 Java 代码和路线
- 当前节点与下一步切到 Node 019「Context Engineering（准备学习）」；既有待执行本地模拟保持原状态

### 2026-11-03

- 用户新增 016-1 / 016-2 并确认读完理论篇，要求实现 Streaming 实验
- 已新增带中文和 JSON/SSE 注释的 StreamingDemo，默认实时正文，--debug 查看 payload；支持事件边界、Usage 空 choices、DONE 与中断识别，七个源码文件编译通过
- 九种 Streaming 本地模拟已编写但尚未执行；Codex 未读取 IDEA 密钥或调用百炼，真实结果待用户运行后填写
- 用户提供 StreamingDemo 默认模式真实 HTTP 200 输出：首段 "Java"，正文延迟 657.241167 ms，总耗时 3899.898959 ms，stop，Token 为 30/138/168，DONE received=true；完整正文与汇总已记录
- 原始 Debug 事件尚未提供，粘贴日志不单独证明现场逐段展示；本地模拟仍未执行，不把本样本当性能基准
- 用户随后提供 --debug 实际输出并观察到每块只含新增 content；已保存五类代表性原始事件，拼接文本与 Full Content 一致
- Debug 本次延迟 786.463 ms、总耗时 4358.231667 ms，Token 为 30/166/196，stop，DONE=true；不覆盖默认模式的原数据
- 已讲解增量拼接、SSE 如何通过一次 HTTP 响应持续传输，以及首段正文延迟与总耗时的区别；Node 016 当前学习范围收尾，准备进入 Node 017
- 未另行逐题口述或测量 GUI 刷新时序，未运行新增本地模拟，不增加流式框架或新功能

- 用户新增 015-1 理论篇及 015-2 实验篇，并确认已读完 015-1
- 当前进入 Node 015 实验准备，按笔记顺序先 DTO、再请求、再真实结果和反序列化，必要错误模拟放在正常链路之后
- 用户要求开始且后续代码都加注释；已创建 CampaignAnalysis 普通 class，Long campaignId 和三个 String 字段，带 getter/setter、toString 和中文说明
- Maven Java 21 编译通过（五个源码文件）；尚未新增 StructuredOutputDemo、调用模型或验证反序列化，Node 015 整体仍在学习中
- 第二步新增 StructuredOutputDemo，使用 FastJSON2 逐层构造 Prompt、四字段 Schema 和 response_format.json_schema，附中文注释与完整 JSON 示例；六个源码文件编译通过
- 已核对百炼官方 response_format / json_schema 及模型支持文档；当前仅有打印代码，未执行 main、未真实调用或验证模型输出
- 第三步已接入环境变量、HTTPS 地址校验、POST、状态码与原始响应打印；六个源码文件编译通过，尚未在 IDEA 真实运行
- 补充本地 HTTP 模拟的三种检查场景，但未执行；不解析 content、不创建 DTO，不将编译成功记作实测成功
- 用户提供首次 Structured Output 实际请求和响应：HTTP 200、qwen3.8-max、stop，Token 为 52/98/150，完整原文已保存
- 原始 content 呈四字段目标结构，但尚未由 Java 提取或转 DTO；模型引用了未提供的行业平均水平，已记录结构与事实可靠性的边界
- 第四步加入外层 JSON 解析、choices/message/content 类型检查及 Structured Content 打印，附中文说明和两层 JSON 示例；六个源码文件编译通过
- 当前仍不解析 content 内部的业务 JSON 或创建 DTO；本地检查更新为九种外层解析场景但未执行，等待用户 IDEA 实际输出
- 用户提供第二次实际输出，Java 已打印 Structured Content；HTTP 200 / stop，Token 为 52/76/128，原始响应与 content 已分别保存，下一步转 DTO
- 第五步已加入 content → CampaignAnalysis、空对象/字段/风险等级检查与四个 getter 打印，附中文注释；六个源码文件编译通过，真实 DTO 输出仍待用户运行
- 本地检查随 DTO 增量更新为十三种场景但尚未执行；不引入新框架或完整 Schema 校验器，不提前标记 Node 015 完成
- 用户提供第三次实际输出：HTTP 200 / stop，JSON 已转成 CampaignAnalysis，四个 getter 成功读取；Token 为 52/192/244，原始结果已补齐
- 记录模型无依据补充行业基准、点击量和 ROI/ROAS 判断的局限；正常类型链路完成，错误路径和完整业务校验未验证
- 用户授权执行两个本地坏数据样本，已新增 --errors 并实际运行：缺少 suggestion 时 DTO 字段为 null，CRITICAL 被 String 接受；均被共用字段检查按预期拒绝，退出码 0
- 本地模式在读取环境变量之前返回，没有 API 调用；仅这两项实际执行，非法 JSON / 错误类型及完整 check-config.py 未跑，不将其一并记成通过
- 用户确认 Node 015 收尾并要求提交推送；理论阅读、真实 DTO 正常链路及本地 C/D 结果已同步，当前准备进入 Node 016「Streaming」
- 本轮不增加新功能或重跑真实 API；其余错误样本和完整 HTTPS 检查仍如实保留未执行状态

- 用户新增并确认读完 013「Structured Output 到底是什么？」与 014「JSON Schema 到底是什么？」
- Node 013 按纯理论节点收尾，不新增 Java Demo / API 实验；笔记正文标题与文件名统一为 013
- 用户提供 User Schema 手写结果，已讲解并修正 required 的层级及缺失闭合括号；用户确认本节结束
- Node 014 按用户确认收尾，当前准备进入 Node 015；其余建议练习未做、未运行校验器，不伪造完成记录

- 用户读完 012「Few-shot 到底是什么？」并确认理论理解，Node 012 以 Zero-shot / One-shot / Few-shot 基础认知收尾
- 按用户决定，不单独创建 Few-shot Java Demo、额外实验笔记或 Codex 实验记录，不进行本 Node 的真实 API / Token 实验
- 012 已改写强制实验预告、完成标准和停止点，保留未来业务通过样本集和 Evaluation 评估示例收益的认知
- 当前准备进入 Node 013「Structured Output」；未创建其笔记，未开始 Node 014 / 015，也未改动 roadmap 或已有 Java 代码

### 2026-11-02

- 用户新增并确认读完 011-1「Prompt 基础到底是什么？」理论篇，已同步笔记阅读记录及进度
- 用户新增 011-2，已实现 PromptBasicsDemo 第一阶段：同一代码 Input、两组均为单条 User，Prompt 清晰度和背景信息不同；附中文及 JSON 注释
- 四个入口按 Java 21 编译通过；用户提供 PromptBasicsDemo 真实 A/B 输出，两组均 HTTP 200 / stop、单条 user
- A 字符数 135，Token 为 52/730/782；B 字符数 396，Token 为 177/497/674；完整消息、回答和观察已保存
- B 使用了单例并发背景并遵循三项格式，但第三项混淆原始 int 与改写后的 AtomicInteger 引用，已如实记录；新增本地模拟仍未执行
- 已增加 PromptBasicsDemo --context 模式，复用原 B 与相同代码，C 删除 Context 并调整不自行假设约束；四个入口编译通过
- 用户提供本次 B/C 实测，两边均 HTTP 200 / stop；B Token 为 177/634/811，C 为 147/396/543，完整消息、回答和观察已保存
- C 明确承认无法确认单例/共享场景；B 仍扩展到未提供的全局 ID 需求，相关推断问题已记录；没有覆盖第一阶段旧 B 的结果
- 已结合实际回复讲解 Prompt 各部分、Context 对条件性判断的作用及模型回复中的错误，Node 011 阅读讲解与两阶段正常实测收尾
- 当前准备学习 Node 012「Few-shot」，对应笔记尚未创建；未另行逐题口述，新增加的本地模拟检查仍是独立待办

- 用户提供 SystemPromptDemo A/B 的完整真实输出；两组均 HTTP 200 / stop
- A 输入/输出/总 Token 为 40/1001/1041，B 为 138/398/536；完整消息和回答已补入 010-2
- B 本样本先给结论并列三项主要问题，内容更集中；仍有过强判断，不能据此声明 System 绝对可靠
- 第一阶段正常调用已验证；新增本地模拟的错误路径检查仍未执行，保留待授权状态
- 已增加 SystemPromptDemo --history 两轮模式，System 与 history 分开，附中文及 JSON 注释；三个入口编译通过
- 用户提供第二阶段真实两轮成功输出，Roles 为 [system,user] / [system,user,assistant,user]；最终历史四条、不含 System，两次回答均带 JAVA-REVIEW: 前缀
- 第二阶段实际输入/输出/总 Token 为 70/36/106、134/60/194；已补齐原始消息、回答、历史及交接记录
- 用户要求同步进度和补齐实验文档，Node 010 学习及两阶段正常实测收尾，当前准备进入 Node 011「Prompt 基础」
- 新增本地检查仍未执行，作为独立待办保留；未把可选删 System 实验或逐题口述测试记为已执行

### 2026-10-26

- 用户确认 009-2 已全部看完，要求进入下一章节
- Node 009 阅读与代码学习收尾，当前进入 Node 010「System Prompt」准备阶段
- 用户补齐 --history 实际输出，三轮均为 HTTP 200 / stop；第三轮携带并复述第二轮真实回答，已补齐笔记、交接报告和实验检查项
- 用户新增并确认读完 010-1「System Prompt 到底是什么？」理论篇
- 用户新增 010-2 实验笔记，已实现 SystemPromptDemo 第一阶段 A/B、JSON 注释及协作记录；三个入口编译通过，尚未真实调用模型
- 本地模拟脚本已扩展，执行被自动审批以“未明确授权测试”为由拒绝；新增检查尚未运行，待用户授权后执行

### 2026-10-10

- 已读完 009-1「Message 模型到底是什么？」理论篇
- 按用户要求跳过单独口述确认，下一步进入 009-2 Java 多轮对话实验
- Node 009 整体仍在学习中，完成实验后再更新节点状态

### 2026-10-09

- 完成 008-3「使用 Java 第一次调用模型 API」，用户确认本篇结束
- 在 IDEA 独立配置环境变量，成功调用 qwen3.8-max
- 使用 FastJSON2 读取 model、content、finish_reason 和 Token Usage
- 错误模型返回 404 / model_not_found，程序以退出码 1 结束且不解析成功字段
- 恢复正确模型后重新验证 HTTP 200，Token 用量为 19 + 20 = 39
- 已补齐实验记录，当前进入 Node 009「Message 模型」

### 2026-09-28

- 完成 008-1「第一次调用模型 API」基础认知
- 完成 008-2「使用 Apifox 第一次调用模型 API」实验
- 成功调用 qwen3.8-max，并完成第一轮 Token Usage 对比
- Node 008 继续学习，下一步进入 008-3 Java 实验

### 2026-09-26

- 完成 Node 007「Workflow 与 Agent」，Phase 0 完成
- 当前进入 Phase 1 / Node 008「第一次调用模型 API」
- 完成 Node 006「什么是 Agent」
- 增加 RAG 与 Memory 区别的补充笔记
- 当前进入 Node 007「Workflow 与 Agent」

### 2026-09-24

- 完成 Node 005「Context Window」
- Context Budget、Lost in the Middle 与 Context Noise 实验留待首次模型 API 实践时进行
- 当前进入 Node 006「什么是 Agent」

### 2026-09-23

- 完成 Node 003「LLM 幻觉」
- 完成 Node 004「Token」
- Token 实验留待首次模型 API 实践时进行
- 当前进入 Node 005「Context Window」

### 2026-09-22

- 完成 Node 001「LLM 到底是什么」
- 完成 Node 002「训练与推理」
- 针对 Training / Pre-training / Inference / Reasoning / Deployment 增加补充笔记

---

> Progress 只负责回答一件事：
>
> **我现在学到哪里了，下一步做什么。**
