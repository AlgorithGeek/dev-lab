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
阶段：Phase 0 / LLM 与 Agent 基础认知
当前节点：Node 005 - Context Window

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

### Phase 0：LLM 与 Agent 基础认知

- ✅ Node 001：LLM 到底是什么
- ✅ Node 002：训练与推理
- ✅ Node 003：LLM 幻觉
- ✅ Node 004：Token
- 🟡 Node 005：Context Window
- ⬜ Node 006：什么是 Agent
- ⬜ Node 007：Workflow 与 Agent

> 这里只展开当前 Phase。后续完整节点请查看 roadmap。

---

## 当前实践

- ⬜ Token 切分 / Token Usage 对比实验
  - 暂不单独折腾 Tokenizer
  - 留到首次接入 Qwen3.8-Max API 时，结合真实 API Usage 一起观察
- ⬜ 创建 `projects/agent-learning-lab`
  - 等进入首次模型 API 实践时正式创建
- ⬜ 第一次 Codex 学习协作
  - 等进入真实代码阶段后开始使用 `codex/prompts` 与 `codex/handoffs`

---

## 下一步

> **Node 005：Context Window**

重点理解：

```text
Context
Context Window
上下文长度
为什么模型不会天然拥有无限记忆
Lost in the Middle
Context Overflow
Memory ≠ 模型永久记住
```

完成后：

```text
Node 005：✅
当前节点：Node 006 - 什么是 Agent
```

---

## 最近进度

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
