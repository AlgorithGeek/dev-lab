# Agent

这里是 `dev-lab` 中专门用于学习、实验和记录 **AI Agent / LLM 应用工程** 的区域。

它不是一个单独追求完整性的知识库，也不是一个一开始就设计好的大型项目。

更准确地说，这里会记录我从 Java 后端出发，一点一点学习 AI Agent 的整个过程：

- 学过的知识
- 写过的 Demo
- 踩过的坑
- Codex 协作记录
- 一些实验性的想法
- 从简单到复杂逐渐演化的 Agent 项目

有些内容会整理得比较完整，有些可能只是当时留下的一点记录。

核心目标只有一个：

> **真正理解并掌握 AI Agent 工程，而不是只认识一堆名词和框架。**

------

## 学习方向

主要学习方向：

```text
LLM API
↓
Prompt / Context
↓
Structured Output
↓
Streaming
↓
Tool Calling
↓
Agent Loop
↓
Memory / State
↓
RAG
↓
Workflow
↓
Human-in-the-loop
↓
MCP
↓
Spring AI / LangChain4j
↓
Python Agent 生态
↓
Evaluation / Tracing / Observability
↓
Security / Reliability
↓
Production Agent
```

当前以 **Java + Spring Boot** 为主线。

Python 作为 Agent 生态的补充技术栈，主要学习：

```text
Python 基础
FastAPI
Pydantic
async / await
OpenAI Agents SDK
LangGraph
```

目标方向偏：

> **AI Agent Engineer / LLM Application Engineer**

而不是大模型训练、算法研究或模型工程。

------

## 学习方式

整个学习过程采用：

```text
ChatGPT
负责知识路线、原理讲解、任务设计和验收
        ↓
我
理解知识、亲手参与编码
        ↓
Codex
读取真实项目、协助编码、运行、测试和排错
        ↓
交接报告
        ↓
ChatGPT
检查理解、补充问题、继续下一节点
```

原则：

> **先理解，再运行，再工程化。**

不会为了快速完成项目，让 AI 一次性生成大量自己看不懂的代码。

每个重要知识点尽量做到：

```text
理解概念
↓
看懂调用链
↓
亲手写代码
↓
运行验证
↓
故意制造问题
↓
分析和修复
↓
能够自己解释
```

------

## 目录结构

```text
Agent/
│
├── README.md
├── progress.md
│
├── roadmap/
│   └── Agent Engineer 循序渐进学习路线.md
│
├── notes/
│   └── ...
│
├── projects/
│   └── ...
│
├── codex/
│   ├── prompts/
│   └── handoffs/
│
└── resources/
```

### `roadmap`

存放长期学习路线和专项学习路线。

### `notes`

按知识领域整理学习笔记。

例如：

```text
01-LLM
02-Prompt
03-Structured-Output
04-Tool-Calling
05-Agent-Loop
06-RAG
07-Memory-State
...
```

这里保存的是：

> **我真正理解后的知识。**

而不是简单复制教程。

### `projects`

存放可以真正运行的实验和项目。

长期主项目：

```text
agent-learning-lab
```

它不会一开始就拥有完整架构，而是会随着学习一点一点成长。

例如：

```text
第一次调用 LLM
↓
加入 Structured Output
↓
加入 Tool Calling
↓
加入 Agent Loop
↓
加入 Memory
↓
加入 RAG
↓
加入 Workflow
↓
加入 MCP
↓
逐渐走向生产级 Agent
```

### `codex/prompts`

保存学习过程中发送给 Codex 的任务提示词。

### `codex/handoffs`

保存 Codex 完成学习任务后输出的交接报告。

通过这些记录，可以回顾：

```text
当时学习了什么
↓
给 Codex 什么任务
↓
项目发生了什么变化
↓
遇到了什么问题
↓
最终是怎么解决的
```

### `resources`

存放值得长期保留的：

```text
官方文档
资料链接
术语
参考项目
其他辅助材料
```

------

## 学习项目

主学习项目：

```text
projects/agent-learning-lab
```

这个项目会作为整个 Agent 学习过程的长期实验场。

前期可能只是：

```text
Java
↓
DeepSeek API
↓
Response
```

后期则可能逐渐演化为：

```text
                     User
                      │
                      ▼
                 Spring Boot
                      │
                      ▼
                 Agent Runtime
                      │
        ┌─────────────┼─────────────┐
        ▼             ▼             ▼
      Tools          RAG          Memory
        │             │             │
        ▼             ▼             ▼
   Business API   Vector Store   Redis/MySQL
        │
        ▼
       MCP
```

重点不是最后代码有多少。

而是能够理解：

> **为什么每一层会出现。**

------

## 模型

学习前期优先使用：

```text
DeepSeek API
```

主要考虑：

- 国内调用方便
- 成本适合长期实验
- 能学习标准 LLM API 调用方式
- 支持 Structured Output、Tool Calling 等 Agent 基础能力

后续会逐渐接触：

```text
OpenAI
Claude
Qwen
其他模型
```

目标不是绑定某一家模型，而是建立：

> **模型是可替换基础设施**

的工程意识。

------

## Git

整个 `Agent` 目录由外层：

```text
dev-lab
```

仓库统一进行 Git 管理。

不会在 `Agent` 或 `agent-learning-lab` 内再次创建 Git 仓库。

学习过程中的关键变化都会尽量留下 Commit，例如：

```text
learn(agent): call DeepSeek API for the first time

learn(agent): add structured output

learn(agent): implement first tool call

learn(agent): implement agent loop

docs(agent): add RAG notes
```

希望未来回头看 Git History 时，可以完整看到：

> **自己是怎么从第一次调用 LLM，一点一点走到真正的 Agent 工程。**

------

## 最终目标

不是：

> 我学过 Spring AI。

不是：

> 我知道 RAG、MCP、LangGraph。

也不是：

> 我能让 AI 帮我生成一个 Agent Demo。

最终希望达到：

> **能够独立理解、设计、实现和维护一个真实可运行的 AI Agent 系统。**

能够回答：

```text
为什么需要这个组件？

它解决什么问题？

它位于调用链的哪里？

代码到底是谁调用谁？

出现异常怎么办？

什么时候应该使用？

什么时候不应该使用？

如何验证它真的有效？

如何让它安全、稳定地运行？
```

当这些问题逐渐能够自己回答时，这个实验室的目标也就正在实现。