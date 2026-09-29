# 008-3 - 实验：使用 Java 第一次调用模型 API

> Phase 1：LLM 应用开发基础
> Node 008：第一次调用模型 API
> Note 008-3：第一次 Java 模型 API 实验
>
> 前置 Note：
>
> ```
> 008-1 - 第一次调用模型 API
> 008-2 - 实验：使用 Apifox 第一次调用模型 API
> ```
>
> 当前状态：
>
> ```
> 008-1 ✅ 理解模型 API 的基本调用链
> 
> 008-2 ✅ 使用 Apifox 成功调用 qwen3.8-max
> 
> 008-3 🟡 使用 Java 重新完成同一件事情
> ```
>
> 这一篇完成以后：
>
> ```
> Node 008：第一次调用模型 API
> ✅ 完成
> ```
>
> 然后正式进入：
>
> ```
> Node 009：Message 模型
> ```

# 一、这一篇和 008-2 有什么不同？

`008-2` 中我们已经亲手完成：

```
Apifox
↓
POST
↓
URL
↓
Headers
↓
JSON Body
↓
阿里云百炼
↓
qwen3.8-max
↓
HTTP Response
```

所以这一篇并不是：

> 再学习一种新的模型能力。

模型还是：

```
qwen3.8-max
```

API 还是：

```
OpenAI-compatible Chat Completions
```

Prompt 甚至都可以还是：

```
请用一句话解释 Java 是什么。
```

唯一真正发生变化的是：

```
008-2

你用 Apifox GUI
手动构造 HTTP Request

↓

008-3

你使用 Java 代码
构造完全等价的 HTTP Request
```

所以本篇真正要回答：

> **Apifox 里那些配置，变成 Java 代码以后分别是什么？**

# 二、先看这次实验的核心映射

上一节我们看到：

| Apifox        | Java                 |
| ------------- | -------------------- |
| POST          | HTTP Method          |
| URL           | URI / Endpoint       |
| Authorization | Request Header       |
| Content-Type  | Request Header       |
| JSON Body     | Request Body         |
| 点击 Send     | HTTP Client 发送请求 |
| HTTP Status   | Response Status      |
| Response Body | Java Response Body   |

本篇全部内容其实都围绕这张表展开。

最终我们希望做到：

```
你看到任意一行 Java HTTP 代码
↓
都知道它对应 Apifox 哪个位置
```

而不是：

```
代码运行成功了
↓
但是不知道为什么成功
```

# 三、为什么我们不直接使用 OpenAI Java SDK？

百炼的 OpenAI-compatible API 可以使用兼容的 Java SDK。

但 Node 008 我们暂时不用。

原因很简单。

如果直接写：

```
client.chat().completions()...
```

很多底层细节会立刻被 SDK 隐藏：

```
POST 去哪了？

Authorization Header 去哪了？

Content-Type 去哪了？

JSON Body 怎么生成的？

HTTP Response 到底长什么样？
```

而 Node 008 当前真正要掌握的恰恰就是：

```
HTTP Request
↓
Provider
↓
Model
↓
HTTP Response
```

因此第一版 Java 调用：

> **优先直接使用 Java HTTP Client。**

等以后真正掌握底层以后，再使用：

```
OpenAI Java SDK
Spring AI
LangChain4j
```

你才能知道：

> 它们究竟替我们封装了什么。

# 四、为什么现在也不使用 Spring AI？

同样的原因。

Spring AI 可以让模型调用变得非常简单。

例如未来代码可能接近：

```
chatClient
    .prompt()
    .user("你好")
    .call();
```

但是现在如果直接这样写：

```
HTTP
Message
Serialization
Provider Config
Response Parsing
```

大量东西都会消失在 Framework 里面。

所以我们的顺序坚持：

```
原始 HTTP
↓
Java HTTP
↓
SDK
↓
Spring AI / LangChain4j
```

而不是反过来。

# 五、那为什么项目以后又要以 Spring Boot 为主？

因为：

```
不用 Spring Boot 完成第一次实验
```

不等于：

```
以后不用 Spring Boot
```

这是两个问题。

我们的长期方向仍然是：

```
Java
+
Spring Boot
+
LLM / Agent
```

但是长期项目会一点一点成长。

第一天：

```
Java
↓
HTTP
↓
Qwen
```

以后才逐渐变成：

```
Spring Boot
↓
Model Client
↓
Structured Output
↓
Tool Calling
↓
Agent Runtime
↓
RAG / Memory / MCP
```

所以：

> **项目不是一开始就设计成最终形态，而是随着知识增长逐渐演化。**

# 六、这一篇还会发生另一件重要的事

这是我们第一次正式创建：

```
Agent/projects/agent-learning-lab
```

以前：

```
projects/
```

一直只存在于规划中。

从这一篇开始：

> **长期主项目正式出生。**

# 七、agent-learning-lab 是什么？

它不是：

```
008-demo
```

也不是：

```
qwen-test
```

更不是：

```
用完就删除的小 Demo
```

它是：

> **整个 Agent 学习路线长期演化的主项目。**

以后：

```
Node 008
第一次模型调用

↓

Node 009
Message

↓

Structured Output

↓

Streaming

↓

Tool Calling

↓

Agent Loop

↓

RAG

↓

Memory

↓

Workflow

↓

MCP

↓

Spring AI / LangChain4j

↓

生产级 Agent
```

都会逐渐在这个项目中留下代码。

# 八、所以第一版项目必须克制

第一版绝对不要出现：

```
ModelRouter
ProviderFactory
AgentRuntime
ToolRegistry
MemoryManager
RagService
WorkflowEngine
```

因为这些东西：

```
我们现在还没真正学到
```

如果现在就生成：

```
看起来很专业
```

实际上只是：

```
AI 帮我们提前幻想出来的架构
```

这违背整个 dev-lab 的原则：

> **先理解，再运行，再工程化。**

# 九、第一版项目到底需要什么？

非常少。

理想情况下：

```
agent-learning-lab/
├── pom.xml
└── src/
    └── main/
        └── java/
            └── ...
                └── FirstModelCall.java
```

第一版程序只需要：

```
读取配置
↓
构造 HTTP Request
↓
发送
↓
读取 HTTP Status
↓
打印原始 Response Body
```

就够了。

# 十、为什么第一版甚至先不解析 Response？

因为我们想严格按照学习顺序成长。

第一版：

```
HTTP Request
↓
HTTP Response 原始 JSON
```

先证明：

> **Java 已经能够复现 Apifox 的行为。**

然后再进行第二轮修改：

```
原始 JSON
↓
JSON Parsing
↓
content
finish_reason
usage
```

这样：

```
第一版
解决通信

第二版
解决结构化读取
```

问题分层非常清楚。

# 十一、为什么这是更好的 Debug 方式？

假如一次性做：

```
HTTP
+
JSON Serialization
+
JSON Parsing
+
DTO
+
Config
```

然后程序失败：

你可能不知道：

```
到底 HTTP 错？

Body 错？

JSON 解析错？

DTO 映射错？
```

而分两步：

```
第一步
HTTP 先通

第二步
再解析 JSON
```

错误范围会小很多。

# 十二、我们第一次正式引入 Codex

从这一篇开始：

```
ChatGPT
↓
你
↓
Codex
↓
真实仓库
↓
运行 / 测试
↓
Handoff
↓
ChatGPT
```

这条长期协作链正式启动。

# 十三、为什么不是让我直接把 Java 代码全部写出来？

因为我们的学习目标不是：

```
ChatGPT 给代码
↓
你复制
↓
运行
```

我们已经有 Codex。

Codex 真正有价值的地方在于：

```
它可以看到你的真实仓库
看到真实 JDK
看到 Maven
创建文件
运行命令
看到编译错误
修改
再次运行
```

而 ChatGPT 更适合：

```
控制学习路线
解释为什么
设计任务边界
检查你是否理解
```

所以各自做自己最擅长的事情。

# 十四、第一次 Codex 协作的原则

Codex 不能：

```
自由发挥
```

它这一次的任务范围非常小：

```
只完成 Node 008-3 第一阶段
```

禁止提前实现：

```
Spring AI
OpenAI Java SDK
LangChain4j
Streaming
Structured Output
Tool Calling
Agent Loop
RAG
Memory
MCP
复杂 Provider 抽象
```

# 十五、为什么 Codex 也不能一次全部写完？

因为：

```
Codex 写得越多
```

不代表：

```
你学得越多
```

如果第一次让它：

```
创建完整 Spring Boot 项目
↓
封装 ModelClient
↓
建 DTO
↓
异常体系
↓
配置体系
↓
单元测试
```

你回来以后看到几十个文件：

```
反而不知道应该先看哪里
```

所以：

> **第一次 Codex 任务必须尽可能小。**

# 十六、Checkpoint 0：先确认你本地仓库是最新的

# 🛑 停下来操作

进入本地：

```
dev-lab
```

先执行：

```
git status
```

确保没有你不认识的未提交修改。

然后：

```
git pull origin main
```

## 成功标准

本地应该已经拥有：

```
Agent/
├── README.md
├── progress.md
├── roadmap/
└── notes/
    ├── 00-LLM与Agent基础认知/
    └── 01-LLM应用开发基础/
        ├── 008-1 - 第一次调用模型 API.md
        ├── 008-2 - 实验：使用 Apifox 第一次调用模型 API.md
        └── 008-3 - 实验：使用 Java 第一次调用模型 API.md
```

现在：

```
projects/
codex/
```

可能仍然不存在。

这是正常的。

马上就会创建。

# 十七、Checkpoint 1：先让 Codex 检查开发环境

我们不应该凭空假设：

```
你的 Java 是 8
11
17
还是 21
```

也不应该假设：

```
Maven 一定配置好了
```

所以第一次 Codex 任务的第一件事：

> **先检查环境。**

# 十八、为什么推荐 JDK 21？

对于这个长期学习项目，我们正式把 Java 基线定为：

```
JDK 21
```

原因不是：

```
LLM / Agent 只能用 JDK 21
```

而是因为对一个现在新建、并且会长期演化的 Agent 学习项目来说，JDK 21 在“现代能力、生态成熟度、长期稳定性”之间非常合适。

它具备：

```
成熟的 LTS 基线
现代 Java 语言与标准库能力
稳定可用的 java.net.http.HttpClient
Java 21 正式提供的 Virtual Threads
后续 Spring Boot / Spring AI / LangChain4j 生态兼容性良好
```

其中：

```
Virtual Threads
```

以后在 Agent 工程里会很有意思。

因为很多 Agent 工作负载都不是纯 CPU 计算，而是：

```
调用 LLM
↓
等待网络

调用 Tool
↓
等待网络

调用 MCP Server
↓
等待网络

调用数据库 / RAG
↓
等待 I/O
```

这类大量 I/O 等待的场景，正是以后学习现代 Java 并发时值得关注的方向。

不过：

> **Node 008 完全不需要使用 Virtual Threads。**

现在提到它，只是说明为什么 JDK 21 比 JDK 17 更适合作为这个长期新项目的基线。

JDK 17 依然是非常稳定、兼容范围很广的现代 Java 基线，但既然这是一个从零开始的新 Agent 学习项目，我们没有必要主动停在“更保守的最低基线”。

更新的 LTS 版本也可以运行很多现代框架，但我们的目标是学习 Agent，而不是追逐最新 JDK 本身。

因此当前项目统一采用：

```
JDK 21
```

这样既足够现代，又能尽量减少因为追新版本而产生的额外变量。

同时你的公司生产环境即使仍然存在：

```
JDK 8
```

也不代表个人 AI 学习项目必须永远受 JDK 8 限制。

可以长期保持：

```
公司存量项目
→ 按公司要求使用 JDK 8

agent-learning-lab
→ 使用 JDK 21
```

两者并不冲突。

# 十九、但是不要让 Codex偷偷改系统环境

如果 Codex 发现：

```
java -version
```

不是我们期望的项目基线：

```
JDK 21
```

不要让它直接：

```
brew install ...
修改 JAVA_HOME
删除旧 JDK
把项目基线偷偷降成 JDK 17
```

第一轮应该：

```
检查
↓
记录
↓
明确告诉我们当前实际 JDK
↓
判断当前 JDK 是否能够按 Java 21 编译和运行
```

项目本身的目标基线保持：

```
Java 21
```

如果当前机器只有低于 21 的 JDK：

> **先报告环境问题，不要为了“让代码先跑起来”而擅自把项目改成 Java 17。**

如果后续确实需要安装新的 JDK，优先考虑成熟的 OpenJDK 发行版，例如：

```
Eclipse Temurin 21
```

但安装和系统环境调整应该由我们明确决定，而不是 Codex 静默完成。

# 二十、第一次 Codex Prompt

# 🛑 停下来操作

现在打开 Codex。

确保当前工作目录是：

```
dev-lab
```

然后把下面整段 Prompt 发给 Codex。

## Codex Prompt 008-3-1

```
你现在在我的 dev-lab 学习仓库中工作。

这是一个长期 AI Agent / LLM 应用工程学习项目。

请先阅读：

- Agent/README.md
- Agent/progress.md
- Agent/roadmap/Agent Engineer 循序渐进学习路线.md
- Agent/notes/01-LLM应用开发基础/008-1 - 第一次调用模型 API.md
- Agent/notes/01-LLM应用开发基础/008-2 - 实验：使用 Apifox 第一次调用模型 API.md
- Agent/notes/01-LLM应用开发基础/008-3 - 实验：使用 Java 第一次调用模型 API.md

当前只完成 Node 008-3 的第一阶段：

“使用 Java 原始 HTTP 调用，复现已经在 Apifox 中成功的 qwen3.8-max Chat Completions 请求。”

这是学习任务，不是让你一次性生成完整 AI 项目。

请严格控制范围。

--------------------------------
一、先检查环境
--------------------------------

先检查并记录：

- 当前仓库状态
- java -version
- javac -version
- mvn -version

本项目正式 Java 基线为：

JDK 21

请确认当前环境是否能够按 Java 21 编译和运行。

不要静默安装软件。
不要静默修改系统 JDK。
不要为了适配本机环境而擅自把项目降级为 JDK 17 或更低版本。

如果当前 JDK 低于 21，优先明确说明环境问题和建议，不要伪装成环境已经满足要求。
如果当前 JDK 高于 21，也仍然把项目的编译目标保持为 Java 21。

--------------------------------
二、创建长期项目
--------------------------------

创建：

Agent/projects/agent-learning-lab

这是整个 Agent 学习路线长期使用的主项目，不是一次性 Demo。

不要在里面重新 git init。

第一版只创建最小 Maven Java 项目。

项目 Java 编译基线固定为 21。

在 pom.xml 中使用清晰、常规的 Maven 配置把 Java release/source target 设为 21，
不要依赖当前机器碰巧使用的 JDK 默认值。

暂时不要引入：

- Spring Boot
- Spring AI
- LangChain4j
- OpenAI Java SDK
- DashScope SDK
- Tool Calling
- Streaming
- Structured Output
- RAG
- Memory
- Agent Loop
- ProviderFactory
- ModelRouter

--------------------------------
三、第一版调用目标
--------------------------------

第一版只做：

Java
→ 原始 HTTP Request
→ 阿里云百炼
→ qwen3.8-max
→ 原始 HTTP Response

本项目基线是 JDK 21，第一版直接使用 JDK 自带的 java.net.http.HttpClient。

代码必须能够清楚对应我之前在 Apifox 里做过的：

- POST
- URL
- Authorization Header
- Content-Type
- JSON Body
- Send
- HTTP Status
- Response Body

--------------------------------
四、配置
--------------------------------

绝对不要把真实 API Key 或 Workspace ID 写进源码、配置文件、Markdown 或 Git。

API Key 从环境变量读取：

DASHSCOPE_API_KEY

Base URL 从环境变量读取：

DASHSCOPE_BASE_URL

我的实际地域是华北2（北京）。

DASHSCOPE_BASE_URL 应由我本地设置为类似：

https://<我的WorkspaceId>.cn-beijing.maas.aliyuncs.com/compatible-mode/v1

代码在此基础上调用：

/chat/completions

如果缺少环境变量：

程序要直接给出清晰错误信息，不要发送一个错误请求。

--------------------------------
五、Request
--------------------------------

第一版继续复现 Apifox 中已经成功的请求。

Model：

qwen3.8-max

User Message：

请用一句话解释 Java 是什么。

reasoning_effort：

none

请求的核心 JSON 应与我在 Apifox 中已经验证成功的请求保持一致。

--------------------------------
六、第一版不要解析业务字段
--------------------------------

第一版成功以后只需要打印：

- HTTP Status
- 原始 Response Body

暂时不要把：

content
finish_reason
usage

解析成 Java DTO。

也暂时不要设计 ChatCompletionResponse 等完整对象模型。

我们要先确认：

“Java 已经成功复现 Apifox 的 HTTP 请求。”

--------------------------------
七、安全
--------------------------------

检查：

- 真实 API Key 没进入 Git
- 真实 Workspace ID 没进入 Git
- 不生成带 Secret 的 .env 并提交
- 不在日志里打印 API Key
- 不把 Authorization Header 完整打印出来

--------------------------------
八、Codex 学习记录
--------------------------------

如果 Agent/codex 不存在，则创建：

Agent/codex/prompts
Agent/codex/handoffs

把本次任务说明整理保存为：

Agent/codex/prompts/008-3-1 - 创建最小 Java 模型调用.md

任务执行完成后，生成：

Agent/codex/handoffs/008-3-1 - 创建最小 Java 模型调用.md

handoff 至少记录：

1. 检测到的 Java / Maven 环境
2. 新建了哪些文件
3. 每个文件负责什么
4. 最终项目结构
5. 使用什么 HTTP Client
6. 如何设置需要的环境变量
7. 如何编译
8. 如何运行
9. 实际运行结果
10. HTTP Status
11. 是否成功获得 qwen3.8-max Response
12. 遇到的错误以及如何修复
13. 当前没有实现哪些后续能力
14. 建议我回来重点理解哪些代码

--------------------------------
九、运行验证
--------------------------------

不要只写代码。

实际执行编译和运行。

如果当前环境因为缺少我本地 Secret 导致无法真正请求：

不要伪造成功结果。

明确告诉我：

代码编译是否成功；
运行卡在哪个环境变量；
我应该在本地执行什么命令。

如果环境变量已经存在，则实际发送请求并记录真实结果。

--------------------------------
十、完成后停止
--------------------------------

完成：

“原始 Java HTTP 请求成功”

以后停止。

不要继续实现 Response 解析。
不要进入 Node 009。
不要修改 notes 和 progress.md 的学习状态。

最后给我一份清晰的 handoff。
```

# 二十一、发给 Codex 以后不要继续看下面

真的先停。

让 Codex完成：

```
检查环境
↓
创建项目
↓
写第一版代码
↓
编译
↓
运行
↓
生成 Handoff
```

# 二十二、Codex 第一轮成功标准

最终至少应该出现：

```
Agent/
├── projects/
│   └── agent-learning-lab/
│       ├── pom.xml
│       └── src/
│           └── main/
│               └── java/
│                   └── ...
│                       └── FirstModelCall.java
│
└── codex/
    ├── prompts/
    │   └── 008-3-1 - 创建最小 Java 模型调用.md
    │
    └── handoffs/
        └── 008-3-1 - 创建最小 Java 模型调用.md
```

实际包名可能与示意不同。

不要为了和这张图一模一样去强改。

# 二十三、如果 Codex 说没有 DASHSCOPE_API_KEY

这是正常情况。

你可能以前只在：

```
Apifox
```

里面配置了 Key。

Apifox 的环境变量：

```
不会自动变成 macOS Shell 环境变量
```

这是两个不同环境。

# 二十四、macOS 临时设置 API Key

如果需要：

# 🛑 停下来操作

在当前 Terminal 会话中：

```
export DASHSCOPE_API_KEY="你的真实 API Key"
```

注意：

```
真实 Key 不要复制进笔记
不要截图
不要 commit
```

# 二十五、设置 Base URL

我们不希望源码里出现真实 Workspace ID。

所以：

```
export DASHSCOPE_BASE_URL="https://你的WorkspaceId.cn-beijing.maas.aliyuncs.com/compatible-mode/v1"
```

注意：

这里应该是：

```
Base URL
```

不是：

```
/chat/completions
```

代码自己追加：

```
/chat/completions
```

# 二十六、为什么把 Base URL 放环境变量？

我们在 008-1 已经学过：

```
不同 Provider
不同 Region
不同 Workspace
```

都会导致：

```
Base URL
```

变化。

所以这类内容天然属于：

```
Configuration
```

而不是：

```
Business Logic
```

# 二十七、为什么暂时不把 model 也放环境变量？

现在只有：

```
qwen3.8-max
```

一个学习模型。

为了第一次代码足够直观：

```
model
```

可以暂时作为清晰的代码常量。

以后到：

```
Node 018：模型选择
```

再正式设计：

```
Model Config
Provider Config
```

不要现在提前复杂化。

# 二十八、如何检查 API Key 环境变量存在？

不要：

```
echo $DASHSCOPE_API_KEY
```

直接把 Secret 打到屏幕。

可以：

```
echo ${DASHSCOPE_API_KEY:+configured}
```

如果存在：

```
configured
```

# 二十九、Base URL 可以打印吗？

Base URL 不像 API Key 那么敏感。

但是里面存在：

```
Workspace ID
```

为了保持习惯：

也没必要到处截图传播。

只需要确认：

```
echo ${DASHSCOPE_BASE_URL:+configured}
```

即可。

# 三十、重新运行 Codex / Java 程序

环境变量设置完成以后：

根据 Codex Handoff 给出的命令：

```
重新编译 / 运行
```

直到：

```
HTTP Status = 200
```

并看到：

```
完整 Response JSON
```

# 三十一、第一版代码运行成功以后先不要让 Codex继续

这是非常重要的一步。

我们需要先：

> **自己读懂第一版代码。**

# 三十二、第一件事：找到 Endpoint

在代码中寻找：

```
DASHSCOPE_BASE_URL
```

以及：

```
/chat/completions
```

然后回忆昨天 Apifox：

```
URL
```

这两个应该对应。

# 三十三、建立第一个映射

```
Apifox：

URL

↓

Java：

URI / Endpoint
```

不要只是：

```
看懂代码语法
```

要问：

> **它在完整调用链中负责什么？**

答案：

```
告诉 Java
Request 发到哪里
```

# 三十四、找到 POST

在代码里找到：

```
POST
```

对应：

```
Apifox 左上角：

POST
```

第二个映射：

```
Apifox Method
↔
Java Request Method
```

# 三十五、找到 Authorization

代码中应该有类似：

```
Authorization
```

然后：

```
Bearer + apiKey
```

这对应 Apifox：

```
Authorization:
Bearer {{DASHSCOPE_API_KEY}}
```

第三个映射：

```
Apifox Header
↔
Java Header
```

# 三十六、现在自己回答

为什么：

```
API Key
```

虽然没有写进源码，

程序依然能够构造：

```
Authorization Header
```

？

答案应该是：

```
运行时
↓
Java 从环境变量读取 Secret
↓
构造 Header
```

# 三十七、找到 Content-Type

代码中寻找：

```
Content-Type
application/json
```

它对应：

```
Apifox Headers
```

你现在应该很熟悉。

# 三十八、找到 JSON Body

找到类似：

```
model
messages
reasoning_effort
```

的地方。

无论 Codex 最终使用：

```
String
Text Block
```

还是一个极简单构造方式，

它最后必须生成类似：

```
{
  "model": "qwen3.8-max",
  "messages": [
    {
      "role": "user",
      "content": "请用一句话解释 Java 是什么。"
    }
  ],
  "reasoning_effort": "none"
}
```

# 三十九、第四个映射

```
Apifox：

Body → JSON

↓

Java：

Request Body
```

本质没有变化。

# 四十、找到真正发送请求的代码

找到：

```
send
```

或者当前 HTTP Client 对应的执行方法。

它对应：

```
Apifox：

Send 按钮
```

# 四十一、这个映射特别重要

```
Apifox 点击 Send
```

本质上不是：

```
Apifox 的某种魔法
```

而是：

```
真正通过网络发送 HTTP Request
```

Java 中：

```
HTTP Client send(...)
```

做的是同一件事。

# 四十二、找到 HTTP Status

代码应该输出：

```
HTTP Status
```

例如：

```
200
```

回忆 Apifox：

```
200 OK
```

又对应上了。

# 四十三、找到 Response Body

第一版暂时不解析。

所以程序应该把：

```
原始 Response JSON
```

直接打印出来。

它应该与你之前 Apifox 中看到的结构非常相似：

```
id
object
model
choices
usage
```

# 四十四、到这里完成最核心的一次认知连接

```
昨天：

Apifox

今天：

Java

其实没有出现另一套模型调用机制。
```

只有：

```
GUI
↓
变成
↓
Code
```

# 四十五、现在自己画一次对应关系

不要看前面。

自己尝试填写：

| Apifox        | Java   |
| ------------- | ------ |
| POST          | ______ |
| URL           | ______ |
| Authorization | ______ |
| Content-Type  | ______ |
| JSON Body     | ______ |
| Send          | ______ |
| 200           | ______ |
| Response JSON | ______ |

如果能全部填出来：

第一轮代码已经基本看懂。

# 四十六、为什么第一版打印原始 JSON 很重要？

因为你已经在 008-2 亲眼见过原始 JSON。

现在 Java 再打印：

```
同样的结构
```

你就能明确：

> **这个 Response 并不是 Java SDK 编造的对象。**

真正从网络回来的是：

```
HTTP Response Body
```

其中是一段：

```
JSON
```

# 四十七、那以后 Java DTO 是怎么来的？

以后可能：

```
JSON
↓
Jackson / SDK
↓
Java Object
```

例如：

```
ChatCompletionResponse
```

但本质：

```
网络上回来的是 JSON
```

Java Object 是：

```
客户端为了方便使用
在本地进行的结构化映射
```

这个区别非常重要。

# 四十八、第一轮实验记录

### JDK

```
【根据 Codex Handoff 填写】
```

### Maven

```
【填写】
```

### HTTP Client

```
【填写】
```

### 编译是否成功

```
【填写】
```

### Java 第一次真实调用是否成功

```
【填写】
```

### HTTP Status

```
【填写】
```

### 是否看到原始 Response

```
【填写】
```

# 四十九、第一轮遇到的问题

### 问题现象

```
【填写】
```

### 原因

```
【填写】
```

### 修复

```
【填写】
```

### 学到什么

```
【填写】
```

# 五十、第一阶段 PASS 条件

必须满足：

```
✅ agent-learning-lab 已创建

✅ Java 能编译

✅ Java 成功发送 HTTP Request

✅ HTTP Status = 200

✅ 能看到原始 Response JSON

✅ API Key 没进 Git

✅ Workspace ID 没进源码

✅ 自己能把 Java 与 Apifox 对应起来
```

完成以后：

```
008-3 第一阶段
✅ PASS
```

但还没结束。

# 五十一、第二阶段要解决什么？

现在我们的代码已经：

```
拿到了 JSON
```

例如：

```
{
  "choices": [...],
  "usage": {...}
}
```

但 Java 目前只是：

```
打印整个字符串
```

实际业务不可能永远：

```
System.out.println(response.body())
```

我们最终需要读取：

```
content

finish_reason

prompt_tokens

completion_tokens

total_tokens
```

所以进入：

> **第二阶段：解析 Response。**

# 五十二、为什么现在可以引入 JSON 库？

因为：

```
HTTP 调用已经理解
```

现在需要解决的是：

```
JSON → Java 可读取结构
```

这已经是一个新的、独立的问题。

# 五十三、为什么现在仍然不创建一整套 DTO？

后面的：

```
Node 015
```

会专门学习：

```
Java DTO 与 Structured Output
```

如果 Node 008 现在就创建：

```
ChatCompletionResponse
Choice
Message
Usage
PromptTokensDetails
...
```

又会把重点带偏。

所以这一次建议：

> **使用 Jackson 的树模型 / JsonNode 做最小解析。**

目的只是：

```
找到几个字段
```

不是：

```
设计完整 API Object Model
```

# 五十四、第二次 Codex Prompt

# 🛑 停下来操作

现在才把第二个 Prompt 发给 Codex。

## Codex Prompt 008-3-2

```
继续在当前 dev-lab 仓库中工作。

前置任务：

008-3-1 已经完成了最小 Java HTTP 请求。

现在进入 Node 008-3 第二阶段：

“解析 qwen3.8-max Chat Completions Response。”

请先阅读：

- Agent/README.md
- Agent/progress.md
- Agent/notes/01-LLM应用开发基础/008-3 - 实验：使用 Java 第一次调用模型 API.md
- Agent/codex/handoffs/008-3-1 - 创建最小 Java 模型调用.md
- Agent/projects/agent-learning-lab 当前代码

然后在现有项目基础上做最小增量修改。

--------------------------------
一、目标
--------------------------------

保持当前 HTTP 请求逻辑不变。

在成功拿到 HTTP 200 和原始 JSON Response 后：

使用轻量 JSON 解析方式读取并打印：

- model
- choices[0].message.content
- choices[0].finish_reason
- usage.prompt_tokens
- usage.completion_tokens
- usage.total_tokens

--------------------------------
二、JSON 技术
--------------------------------

可以引入 Jackson。

当前优先使用 JsonNode / Tree Model。

不要为了这次实验建立一整套：

ChatCompletionResponse
Choice
Message
Usage

等完整 DTO。

Java DTO 的系统学习留给后续 Node。

--------------------------------
三、输出
--------------------------------

程序最终输出应该尽量清楚，例如：

HTTP Status: 200
Model: ...
Content: ...
Finish Reason: ...
Prompt Tokens: ...
Completion Tokens: ...
Total Tokens: ...

是否额外打印原始 JSON可以自行判断，
但解析后的字段必须明确展示。

--------------------------------
四、最小错误处理
--------------------------------

如果 HTTP Status 不是 2xx：

不要继续按照成功 Response 结构解析。

至少：

- 打印 HTTP Status
- 打印错误 Response Body
- 明确结束本次调用

不要在这里设计完整异常体系、Retry、Backoff。

这些属于后续 Node。

--------------------------------
五、限制
--------------------------------

仍然不要引入：

- Spring Boot
- Spring AI
- LangChain4j
- OpenAI Java SDK
- DashScope SDK
- Streaming
- Structured Output
- Tool Calling
- Agent Loop
- ProviderFactory
- ModelRouter

--------------------------------
六、安全
--------------------------------

继续确保：

- API Key 只来自 DASHSCOPE_API_KEY
- Base URL 只来自 DASHSCOPE_BASE_URL
- 不把真实 Secret 写进 Git
- 日志不打印真实 API Key

--------------------------------
七、记录
--------------------------------

保存本次任务说明：

Agent/codex/prompts/008-3-2 - 解析模型响应.md

生成 handoff：

Agent/codex/handoffs/008-3-2 - 解析模型响应.md

handoff 记录：

1. 修改了哪些文件
2. 新增了什么依赖
3. 为什么选择这种 JSON 解析方式
4. 每个解析字段来自 Response 的哪个 JSON 路径
5. 编译结果
6. 真实运行结果
7. HTTP Status
8. content
9. finish_reason
10. Token Usage
11. 是否出现错误
12. 当前仍然刻意没有实现哪些能力

--------------------------------
八、实际验证
--------------------------------

实际编译并运行。

不要伪造 Response。

完成上述目标后停止。

不要进入 Node 009。
不要修改 progress.md 的 Node 完成状态。
```

# 五十五、第二轮完成以后又不要急着继续

现在回来。

我们重点看：

```
JsonNode
```

到底做了什么。

# 五十六、JSON Parsing 本质是什么？

网络返回：

```
String JSON
```

例如：

```
{
  "choices": [
    {
      "message": {
        "content": "Java 是..."
      }
    }
  ]
}
```

Jackson：

```
JSON String
↓
Parser
↓
JsonNode Tree
```

然后代码就可以：

```
沿着 JSON 路径
```

寻找：

```
choices
↓
0
↓
message
↓
content
```

# 五十七、这不是 LLM 特有技术

JSON Parsing：

```
普通第三方 API
```

同样需要。

所以：

```
LLM API
```

再次表现出：

```
它首先仍然是一个 API
```

# 五十八、自己找到 content 的 JSON 路径

应该可以写成概念路径：

```
choices[0].message.content
```

这和 008-2 中你用眼睛展开 JSON：

```
choices
↓
第 0 个
↓
message
↓
content
```

完全一样。

只是：

```
以前是你手动找

现在是 Java 自动找
```

# 五十九、finish_reason 路径

```
choices[0].finish_reason
```

回忆：

```
stop
```

代表正常停止。

以后如果：

```
length
```

就可能涉及输出被截断。

# 六十、Usage 路径

```
usage.prompt_tokens

usage.completion_tokens

usage.total_tokens
```

这些就是：

```
Node 004
```

真正进入代码。

# 六十一、现在 Token 已经变成 Java 运行时数据

以前：

```
Token
=
概念
```

008-2：

```
Token
=
Apifox Response 里的数字
```

008-3：

```
Token
=
程序可以主动读取、记录、统计的数据
```

这一步非常重要。

以后：

```
成本监控
Tracing
Metrics
Context Budget
```

都建立在这里。

# 六十二、第二阶段实验记录

### model

```
【填写】
```

### content

```
【填写】
```

### finish_reason

```
【填写】
```

### prompt_tokens

```
【填写】
```

### completion_tokens

```
【填写】
```

### total_tokens

```
【填写】
```

# 六十三、现在比较 Java 与 Apifox 的 Response

Apifox：

```
你自己在 JSON 中找字段
```

Java：

```
程序替你找
```

所以：

```
Response
```

没有发生变化。

变化的是：

```
谁负责解析
```

# 六十四、接下来故意制造一次错误

整个 dev-lab 的学习原则里有：

```
故意制造问题
↓
分析
↓
修复
```

所以 Node 008 最后再做一个小错误实验。

不用复杂。

# 六十五、错误实验：故意写错 Model

# 🛑 停下来操作

暂时把：

```
qwen3.8-max
```

改成：

```
this-model-does-not-exist
```

运行程序。

# 六十六、这次不要期待 200

我们希望观察：

```
HTTP Status
```

以及：

```
Error Response Body
```

# 六十七、观察代码有没有做对一件事

如果不是：

```
2xx
```

程序不应该继续尝试：

```
choices[0].message.content
```

因为失败 Response：

```
根本不保证拥有 choices
```

# 六十八、这说明一个非常重要的工程原则

```
先判断 Transport / API Success
↓
再解析 Success Payload
```

不能：

```
任何 Response
↓
都假设是成功 JSON
```

# 六十九、记录错误实验

### 错误 Model

```
this-model-does-not-exist
```

### HTTP Status

```
【填写】
```

### Error Code

```
【填写】
```

### Error Message

```
【填写】
```

### Java 是否错误地尝试解析 choices

```
【填写】
```

# 七十、恢复正确模型

# 🛑 停下来操作

一定改回：

```
qwen3.8-max
```

再次运行。

确保：

```
HTTP 200
```

重新出现。

# 七十一、为什么一定要恢复后再测一次？

因为：

```
“我把错误代码改回去了”
```

不等于：

```
系统真的恢复
```

真正的软件工程习惯：

```
修复
↓
重新验证
```

# 七十二、现在检查 Git

# 🛑 停下来操作

执行：

```
git status
```

仔细看新增文件。

重点确认：

```
没有 API Key
```

进入：

```
tracked files
```

# 七十三、可以搜索 Secret 吗？

可以做一个最简单检查。

例如不要搜索真实 Key 全值并让它显示出来。

可以检查：

```
DASHSCOPE_API_KEY
```

是否只是变量名。

同时自己检查：

```
.java
.md
xml
properties
yaml
```

有没有误贴真实 Secret。

# 七十四、项目内部不要重新 git init

确认：

```
Agent/projects/agent-learning-lab/.git
```

不存在。

整个项目始终由：

```
dev-lab/.git
```

统一管理。

# 七十五、为什么统一 Git 很重要？

因为我们希望未来：

```
git log
```

能够连续看到：

```
第一次 LLM API

↓

Structured Output

↓

Streaming

↓

Tool Calling

↓

Agent Loop
```

而不是：

```
一个 Agent 仓库
里面又套十几个独立仓库
```

# 七十六、008-3 完成后的理想目录

大致：

```
Agent/
│
├── README.md
├── progress.md
│
├── roadmap/
│
├── notes/
│   └── 01-LLM应用开发基础/
│       ├── 008-1 - 第一次调用模型 API.md
│       ├── 008-2 - 实验：使用 Apifox 第一次调用模型 API.md
│       └── 008-3 - 实验：使用 Java 第一次调用模型 API.md
│
├── projects/
│   └── agent-learning-lab/
│       ├── pom.xml
│       └── src/
│           └── main/
│               └── java/
│                   └── ...
│
└── codex/
    ├── prompts/
    │   ├── 008-3-1 - 创建最小 Java 模型调用.md
    │   └── 008-3-2 - 解析模型响应.md
    │
    └── handoffs/
        ├── 008-3-1 - 创建最小 Java 模型调用.md
        └── 008-3-2 - 解析模型响应.md
```

# 七十七、为什么 Codex Prompt 也值得留在 Git？

未来你可能忘了：

```
“这个代码为什么当时写成这样？”
```

这时候可以回看：

```
Prompt
```

知道当时任务边界是什么。

例如：

```
明确禁止使用 Spring AI
```

以后看到第一版代码非常原始：

你不会误以为：

```
“当时怎么写得这么低级？”
```

而会知道：

> **这是为了学习原始 HTTP 故意这么做的。**

# 七十八、为什么 Handoff 也值得留？

Handoff 记录：

```
Codex 真正做了什么
```

而不是：

```
ChatGPT 原本希望它做什么
```

Prompt：

```
计划
```

Handoff：

```
实际结果
```

两个东西不能互相代替。

# 七十九、现在回头看完整学习链

Node 008 已经经历：

```
008-1
理论理解

↓

008-2
Apifox 手动 HTTP

↓

008-3
Java 自动 HTTP
```

形成：

```
Concept
↓
Manual Experiment
↓
Code
```

这就是以后很多 Node 的学习模板。

# 八十、以后 Structured Output 也可以这样学

例如：

```
理论
↓
Apifox / 原始 JSON
↓
Java
```

Tool Calling：

```
理论
↓
看原始 Tool Call JSON
↓
Java 执行 Tool
```

Agent Loop：

```
理论
↓
手动画 Loop
↓
Java Runtime
```

所以 Node 008 同时在建立：

> **我们整个 Agent 学习项目的实验方法。**

# 八十一、这一篇我们故意没做什么？

依然没有：

```
Spring Boot
```

没有：

```
Controller
Service
```

没有：

```
OpenAI SDK
```

没有：

```
Spring AI
```

没有：

```
Structured Output
```

没有：

```
Tool Calling
```

没有：

```
Agent
```

# 八十二、代码这么少是不是“项目太简单”？

不是。

当前的复杂度：

```
正好等于当前理解
```

这是最理想的状态。

错误路线是：

```
代码复杂度
>>>>>>>>>
知识理解
```

最终项目虽然很大：

但你驾驭不了。

# 八十三、什么时候开始 Spring Boot？

不是：

```
现在为了显得像项目
```

强行加。

而是在后面：

```
HTTP API
配置
业务接口
生命周期
依赖注入
Agent Service
```

真正需要 Spring Boot 时再进入。

# 八十四、什么时候使用 OpenAI Java SDK？

当你已经知道：

```
SDK 帮你隐藏的 HTTP 长什么样
```

以后再使用。

那时候看到：

```
OpenAIClient
```

不会认为：

```
“这就是 LLM 的底层”
```

而会知道：

```
它只是 Client Abstraction
```

# 八十五、什么时候设计 ModelClient？

当项目真的出现：

```
多处模型调用
多个 Provider
统一接口需求
```

时。

不是：

```
第一天
```

凭想象设计。

# 八十六、Node 008 最终需要形成什么能力？

不是背 Java API。

而是能够解释：

```
Java
↓
读取 Config
↓
构造 JSON
↓
构造 HTTP Request
↓
Authorization
↓
POST
↓
Provider
↓
Model
↓
Inference
↓
HTTP Response
↓
JSON
↓
Java Parsing
```

# 八十七、最终完整调用链

```
DASHSCOPE_API_KEY
DASHSCOPE_BASE_URL
        │
        ▼
    Java Program
        │
        │ build request
        ▼
    HTTP Request
        │
        ├── POST
        ├── URI
        ├── Authorization
        ├── Content-Type
        └── JSON Body
                 │
                 ▼
       Alibaba Cloud Model Studio
                 │
                 ▼
           qwen3.8-max
                 │
                 ▼
             Inference
                 │
                 ▼
          HTTP Response
                 │
        ┌────────┴─────────┐
        ▼                  ▼
     Status              JSON Body
                             │
                             ▼
                          Jackson
                             │
            ┌────────────────┼────────────────┐
            ▼                ▼                ▼
         content       finish_reason         usage
                                               │
                           ┌───────────────────┼──────────────┐
                           ▼                   ▼              ▼
                    prompt_tokens    completion_tokens   total_tokens
```

# 八十八、把 Phase 0 再串一次

## Node 001：LLM

```
qwen3.8-max
```

就是本次真正使用的 LLM。

## Node 002：Inference

Java 发 Request：

```
不是训练模型
```

而是请求：

```
Inference
```

## Node 003：Hallucination

即使：

```
HTTP Status = 200
```

仍然：

```
不能证明 content 正确
```

## Node 004：Token

现在 Java 已经能够读取：

```
prompt_tokens
completion_tokens
total_tokens
```

## Node 005：Context

Request Body 中：

```
messages
```

构成当前 Context 的核心部分。

## Node 006：Agent

以后 Agent：

```
会重复执行现在这次 LLM API Call
```

## Node 007：Workflow

以后：

```
模型调用
```

也可以成为固定 Workflow 中的一个 Node。

# 八十九、自测 1

为什么：

```
Apifox 能调用
```

而：

```
Java 也能调用
```

？

# 九十、自测 2

Apifox 中：

```
POST
```

对应 Java 什么？

# 九十一、自测 3

Apifox 的：

```
Authorization Header
```

对应 Java 哪一部分？

# 九十二、自测 4

为什么 API Key：

```
不能硬编码
```

？

# 九十三、自测 5

为什么：

```
DASHSCOPE_BASE_URL
```

更像配置，

而不是业务逻辑？

# 九十四、自测 6

Java 第一次拿到：

```
Response Body
```

时，它本质是什么？

# 九十五、自测 7

Jackson 做了什么？

# 九十六、自测 8

为什么这次不建立：

```
ChatCompletionResponse DTO
```

？

# 九十七、自测 9

为什么：

```
HTTP 非 2xx
```

时不能直接去找：

```
choices[0]
```

？

# 九十八、自测 10

为什么故意写错 Model 有学习价值？

# 九十九、自测 11

为什么第一轮：

```
不使用 Spring AI
```

？

# 一百、自测 12

为什么第一版：

```
没有 ProviderFactory
```

反而是好事情？

# 一百零一、口述验收

最终你应该可以不看资料解释：

> 我之前在 Apifox 中手动构造过一次 Qwen Chat Completions HTTP 请求。
>
> Java 调模型没有出现另一套神秘机制。
>
> 程序只是从环境变量读取 API Key 和 Base URL，然后构造 POST Request。
>
> Request Header 中包含 Authorization 和 Content-Type，Body 中包含 model、messages 和相关参数。
>
> Java HTTP Client 把这个 Request 发送到阿里云百炼，百炼调用 qwen3.8-max 完成一次 Inference，再返回 HTTP Response。
>
> 第一版 Java 代码可以直接打印原始 Response JSON。
>
> 第二版再使用 Jackson 从 JSON 中读取 content、finish_reason 和 usage。
>
> Apifox 和 Java 做的本质上是同一件事情，只不过一个通过 GUI 构造 HTTP Request，一个通过代码构造。
>
> API Key 属于 Secret，不能进入 Git；Base URL 等 Provider 信息属于运行配置。
>
> HTTP 200 只代表 API 调用成功，并不能证明模型回答一定正确。

如果你能真正解释清楚：

```
Node 008
已经基本掌握。
```

# 一百零二、Node 008 最终验收 Checklist

## 理论

- 理解 Provider
- 理解 Model
- 理解 API
- 理解 SDK
- 理解 OpenAI-compatible
- 理解 HTTP Request
- 理解 HTTP Response

## Apifox

- 成功创建百炼 API Key
- 确认 Region
- 确认 Workspace
- 成功调用 qwen3.8-max
- 看到 content
- 看到 finish_reason
- 看到 usage
- 完成 Token Usage 实验

## Java

- 创建 agent-learning-lab
- 创建 Maven Java 项目
- Java 从环境变量读取 API Key
- Java 从环境变量读取 Base URL
- Java 构造 POST
- Java 设置 Header
- Java 构造 JSON Body
- Java 成功发送 Request
- Java 获取 HTTP 200
- Java 获取原始 Response
- Java 解析 content
- Java 解析 finish_reason
- Java 解析 usage
- 故意制造过一次 API 错误
- 能解释错误发生在哪一层
- Secret 没有进入 Git

## Codex

- `codex/prompts` 已建立
- `codex/handoffs` 已建立
- 第一轮 Prompt 已保存
- 第一轮 Handoff 已保存
- 第二轮 Prompt 已保存
- 第二轮 Handoff 已保存
- 自己真正看懂 Codex 改过的代码

# 一百零三、Node 008 完成条件

全部完成以后：

```
Node 008：第一次调用模型 API

✅ PASS
```

这意味着：

```
Phase 1
正式完成了第一个实际编码节点。
```

# 一百零四、完成以后 progress.md 应该发生什么变化？

到那时再更新：

```
Node 008
✅
```

并把：

```
当前节点
```

切换到：

```
Node 009 - Message 模型
```

注意：

> **不要让 Codex 在实验中提前帮你改成完成。**

因为：

```
代码运行成功
```

只是完成标准的一部分。

还需要：

```
你自己理解
+
ChatGPT 验收
```

之后才能真正：

```
Node 008 ✅
```

# 一百零五、为什么这个规则很重要？

如果：

```
Codex 一运行成功
↓
自动把 progress 改成下一 Node
```

学习进度会逐渐变成：

```
AI 完成代码的进度
```

而不是：

```
你真正掌握知识的进度
```

这两者不是一回事。

# 一百零六、下一篇是什么？

完成 008-3 后：

```
Node 009：Message 模型
```

到时候我们会开始真正解释：

```
System Message
User Message
Assistant Message
Tool Message
```

以及非常重要的：

```
为什么聊天历史不是自动存在模型脑子里
```

而是：

```
应用程序
↓
一次又一次构造 messages
↓
重新发送给模型
```

# 一百零七、Node 009 和现在有什么关系？

你现在 Java Body：

```
{
  "messages": [
    {
      "role": "user",
      "content": "请用一句话解释 Java 是什么。"
    }
  ]
}
```

目前我们只是：

```
会使用
```

但还没有系统理解：

```
Message Model
```

Node 009 就会把：

```
messages
```

彻底拆开。

所以学习路线仍然严格是：

```
先真实看到
↓
再深入理解
```

而不是：

```
先把所有理论堆完
再半年以后写代码
```

# 一百零八、这一篇真正想让你记住的一句话

> **Java 调用大模型，第一步并不是学习某个 AI Framework，而是理解：Java 如何构造和发送一个普通 HTTP Request。**

再往后：

```
SDK
Framework
Agent Runtime
```

都是建立在这条最基础链路之上的。

# 一百零九、当前最终状态

在开始本篇之前：

```
Concept
✅

Apifox HTTP
✅

Java HTTP
⬜
```

完成本篇以后：

```
Concept
✅

Apifox HTTP
✅

Java HTTP
✅
```

于是：

```
第一次模型 API 调用
```

这个 Node 才真正形成：

```
理解
+
手动实验
+
代码实现
+
排错
+
解释能力
```

完整闭环。

# 一百一十、停止点

如果你现在只是刚把这篇 Note 放进仓库：

**不要直接往下自己自由发挥写代码。**

从：

```
Checkpoint 0
```

开始。

然后严格按照：

```
git pull
↓
Codex Prompt 008-3-1
↓
Codex Handoff
↓
回来读代码
↓
Codex Prompt 008-3-2
↓
故意制造错误
↓
最终验收
```

推进。

一次只完成一个 Checkpoint。

> **我们的目标不是今天快速得到一个 AI 项目，而是从这一刻开始，真正知道这个项目里的每一层为什么存在。**