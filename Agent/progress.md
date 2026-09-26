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
当前节点：Node 008 - 第一次调用模型 API

主项目：projects/agent-learning-lab
项目状态：⬜ 尚未正式创建

主要语言：Java
主要后端框架：Spring Boot
当前 Provider：阿里云百炼 Model Studio
当前主模型：qwen3.8-max
API 风格：OpenAI-compatible
```

---

## 当前阶段进度

### Phase 1：LLM 应用开发基础

- 🟡 Node 008：第一次调用模型 API
- ⬜ Node 009：Message 模型
- ⬜ Node 010：System Prompt
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

- ⬜ Token 切分 / Token Usage 对比实验
  - 暂不单独折腾 Tokenizer
  - 留到首次接入 Qwen3.8-Max API 时，结合真实 API Usage 一起观察
- ⬜ Context Budget / Lost in the Middle 对比实验
  - 留到首次接入 Qwen3.8-Max API 时，比较不同长度、位置和噪声下的回答质量、Token 消耗与延迟
- ⬜ 创建 `projects/agent-learning-lab`
  - 等进入首次模型 API 实践时正式创建
- ⬜ 第一次 Codex 学习协作
  - 等进入真实代码阶段后开始使用 `codex/prompts` 与 `codex/handoffs`

---

## 下一步

> **Node 008：第一次调用模型 API**

重点理解：

```text
使用 Apifox 或 cURL 发出第一条模型请求
理解 HTTP Request → Provider → Model → HTTP Response
看懂 URL、Header、Request Body 和 HTTP Status
读取模型返回内容、finish_reason 和 Usage
确认 API Key 只通过环境变量提供
```

完成后：

```text
Node 008：✅
当前节点：Node 009 - Message 模型
```

---

## 最近进度

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
