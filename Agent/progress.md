# AI Agent 学习进度

> 这里记录当前实际学习状态。
>
> Node 表示知识节点，不对应自然日。
>
> - `README.md`：说明这个 Agent 学习区怎么使用
> - `roadmap/`：记录完整学习路线
> - `progress.md`：只记录当前学到哪里
>
> 完整路线以 `roadmap/Agent Engineer 循序渐进学习路线.md` 为准。

---

## 当前状态

```text
状态：🟡 学习中
阶段：Phase 1 / LLM 应用开发基础
当前节点：Node 013 - Structured Output（准备学习）
当前笔记：012 Few-shot 理论篇已读完并由用户确认理解；本 Node 不单独安排 Java 实验，下一步进入 Node 013 Structured Output

主项目：projects/agent-learning-lab
项目状态：已跑通单轮、历史、System 及 Prompt A/B、B/C 的真实正常调用

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
- 🟡 Node 013：Structured Output（准备学习）
- ⬜ Node 014：JSON Schema
- ⬜ Node 015：Java DTO 与 Structured Output
- ⬜ Node 016：Streaming
- ⬜ Node 017：模型参数
- ⬜ Node 018：模型选择
- ⬜ Node 019：Context Engineering
- ⬜ Node 020：LLM API 异常
- ⬜ Node 021：第一阶段项目

> 这里只展开当前 Phase。后续完整节点请查看 roadmap。

---

## 当前实践

- ✅ 第一轮 Token Usage 对比实验
  - 已通过 Apifox 对比短 / 长 Prompt 与短 / 长回答
  - 已观察 `prompt_tokens`、`completion_tokens` 和 `total_tokens`
- ⬜ Token 切分 / Tokenizer 实验
  - 暂不单独折腾 Tokenizer，后续结合真实需要再做
- ⬜ Context Budget / Lost in the Middle 对比实验
  - 留到首次接入 Qwen3.8-Max API 时，比较不同长度、位置和噪声下的回答质量、Token 消耗与延迟
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

---

## 下一步

> **Node 013：Structured Output（准备学习）**

重点理解：

```text
自然语言输出
↓
结构化输出
↓
为什么程序和企业应用更喜欢结构化结果
```

---

## 最近进度

### 2026-11-03

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
