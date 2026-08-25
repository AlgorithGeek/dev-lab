​	

# AI Agent 学习进度

> 这里记录当前实际学习状态。
> Roadmap 决定“以后学什么”，这里负责记录“现在学到哪里”。

------

# 当前状态

```text
状态：🟡 准备开始
阶段：Phase 0 / Agent 基础认知
当前节点：Day 1 - LLM 到底是什么
主项目：projects/agent-learning-lab
主要语言：Java
主要后端框架：Spring Boot
前期模型：DeepSeek
```

目前已经完成学习环境和长期路线的基本规划。

下一步正式开始：

> **Day 1：LLM 到底是什么**

------

# 状态说明

```text
⬜ 未开始

🟡 学习中

✅ 已完成

🔁 需要复习

⚠️ 理解仍有问题
```

一个知识点只有在基本做到：

```text
概念理解
+
代码实践（适用时）
+
运行验证
+
能够自己解释
```

之后，才标记为：

```text
✅
```

------

# Phase 0：建立 Agent 世界观

-  Day 001：LLM 到底是什么
-  Day 002：训练与推理
-  Day 003：LLM 幻觉
-  Day 004：Token
-  Day 005：Context Window
-  Day 006：什么是 Agent
-  Day 007：Workflow 与 Agent

------

# Phase 1：LLM 应用基础

-  第一次调用 DeepSeek API
-  Java 调用 DeepSeek API
-  Message 模型
-  System Prompt
-  Prompt 基础
-  Few-shot
-  Structured Output
-  JSON Schema
-  Java DTO 与 Structured Output
-  Streaming
-  模型参数
-  模型选择
-  Context Engineering
-  LLM API 异常处理
-  第一阶段小项目

详细节点以：

```
roadmap/Agent Engineer 循序渐进学习路线.md
```

为准。

------

# 后续核心阶段

```text
Phase 2
Python Agent 必要基础

Phase 3
Tool Calling / Agent Loop

Phase 4
RAG

Phase 5
Memory / Context / State

Phase 6
Workflow / Agent Orchestration

Phase 7
MCP

Phase 8
Java AI 工程化

Phase 9
Python Agent Framework

Phase 10
生产级 Agent 工程

Phase 11
综合 Agent 项目
```

------

# 项目进度

## agent-learning-lab

当前状态：

```text
⬜ 尚未正式创建
```

计划演化：

```text
v0.1
第一次调用 LLM

↓

v0.2
Spring Boot Chat API

↓

v0.3
Structured Output

↓

v0.4
Streaming

↓

v0.5
Tool Calling

↓

v0.6
Multiple Tools

↓

v0.7
Agent Loop

↓

v0.8
真实业务 Tool

↓

v0.9
Memory / State

↓

v0.10
RAG

↓

v0.11
Workflow / HITL

↓

v0.12
MCP

↓

v0.13
Evaluation / Tracing

↓

v1.0
Production Agent
```

------

# 学习节点验收

每个重要节点尽量检查以下内容：

```text
概念              ⬜
调用链            ⬜
代码              ⬜
运行              ⬜
排错              ⬜
独立解释          ⬜
```

例如学习 Tool Calling 时：

```text
Tool Calling 概念      ✅
知道模型返回什么        ✅
知道 Java 才是真正执行者 ✅
可以写第一个 Tool       ✅
可以跑通 Tool Call      ✅
可以解释异常流程        ⬜
```

那么这个知识点仍然不能算完全结束。

------

# Codex 协作状态

协作方式：

```text
ChatGPT
↓
生成当前学习任务

codex/prompts/
↓
保存 Prompt

Codex
↓
读取真实本地项目
协助编码 / 运行 / 测试 / 排错

codex/handoffs/
↓
保存交接报告

ChatGPT
↓
检查结果和理解程度
↓
继续下一节点
```

Codex 的定位：

> **结对编程老师，而不是项目外包。**

------

# 当前待办

```text
✅ 建立 Agent 学习目录

✅ 保存长期学习路线

✅ 建立 notes

✅ 建立 projects

✅ 建立 codex/prompts

✅ 建立 codex/handoffs

✅ 建立 resources

✅ 创建 README

✅ 创建 progress

⬜ 正式开始 Day 001

⬜ 创建第一份学习笔记

⬜ 创建第一个 Codex 学习 Prompt

⬜ 创建 agent-learning-lab

⬜ 完成第一次 Agent 学习 Git Commit
```

------

# 最近一次进度

## 2026-08-25

完成 Agent 学习区域基础结构规划。

当前目录：

```text
Agent/
├── README.md
├── progress.md
├── roadmap/
├── notes/
├── projects/
├── codex/
│   ├── prompts/
│   └── handoffs/
└── resources/
```

已经确定长期学习方式：

```text
理解知识
↓
小型实践
↓
Codex 协作
↓
真实运行
↓
交接报告
↓
ChatGPT 验收
↓
Git Commit
↓
下一知识点
```

------

# 下一步

> **Day 001：LLM 到底是什么**

完成后更新：

```text
Day 001：✅

当前节点：
Day 002
```

并开始留下第一份真正的 Agent 学习记录。