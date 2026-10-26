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
当前节点：Node 010 - System Prompt（学习中）
当前笔记：010-1 理论篇已读完，下一步 010-2 System Prompt 行为对照实验（实验笔记尚未创建）

主项目：projects/agent-learning-lab
项目状态：Java 单轮、A/B 对照和三轮自动历史均已真实跑通

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
- 🟡 Node 010：System Prompt（010-1 已读完，实验待开始）
- ⬜ Node 011：Prompt 基础
- ⬜ Node 012：Few-shot
- ⬜ Node 013：Structured Output
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

---

## 下一步

> **010-2：实验：使用 System Prompt 控制模型行为**（待补充实验笔记）

重点理解：

```text
同一模型、同一 User 输入和参数，对比无 System / 有 System
以 Java Code Review Assistant 为例，观察关注点、回答结构和 Token Usage
再验证多轮请求如何继续携带 System Message
```

---

## 最近进度

### 2026-10-26

- 用户确认 009-2 已全部看完，要求进入下一章节
- Node 009 阅读与代码学习收尾，当前进入 Node 010「System Prompt」准备阶段
- 用户补齐 --history 实际输出，三轮均为 HTTP 200 / stop；第三轮携带并复述第二轮真实回答，已补齐笔记、交接报告和实验检查项
- 用户新增并确认读完 010-1「System Prompt 到底是什么？」理论篇
- 下一步按 010-1 预告进入 010-2 行为对照实验；实验笔记尚未创建，未修改 Java 代码或开始模型调用

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
