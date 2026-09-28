# 006 - 什么是 Agent？

前面五个 Node，我们其实一直都在研究一个核心问题：

```text
LLM 到底是什么？
```

我们依次搞清楚了：

```text
Node 001：LLM
↓
模型本质上根据 Context 预测 Token

Node 002：Training / Inference
↓
普通模型调用属于推理，不会自动修改模型参数

Node 003：Hallucination
↓
模型生成语言合理的内容，不等于事实一定正确

Node 004：Token
↓
模型处理文本的基本单位

Node 005：Context Window
↓
模型一次能够看到的信息有限
```

但是到目前为止，我们研究的依然主要是：

```text
LLM
```

也就是：

> **一个能够接收 Context，然后生成结果的模型。**

从这一篇开始，我们正式跨过一条非常重要的分界线：

```text
LLM
↓
Agent
```

也就是说，我们第一次开始研究：

> **如何让 LLM 不只是“回答问题”，而是能够根据目标决定下一步做什么、调用外部能力、观察结果、继续行动，直到完成任务。**

这就是：

```text
AI Agent
```

------

# 一、先看普通 LLM 应用是什么样

最简单的 Chat 应用：

```text
User
 ↓
LLM
 ↓
Answer
```

比如：

```text
User：

Java 中 HashMap 和 ConcurrentHashMap 有什么区别？
```

模型收到问题以后：

```text
理解问题
↓
根据已有知识和当前 Context
↓
生成答案
```

然后任务结束。

整个过程可以简化成：

```text
Input
↓
Inference
↓
Output
```

这仍然只是：

> **LLM 调用。**

------

# 二、普通 Chat 最大的特点是什么？

它通常是：

```text
用户问
↓
模型答
↓
结束
```

模型没有真正做外部动作。

例如你问：

```text
帮我看看 Campaign 123 当前是不是 ACTIVE。
```

如果只是普通 Chat：

模型只能：

```text
根据你给的 Context 猜
```

或者：

```text
告诉你它不知道
```

因为模型本身并不能神奇地访问你的：

```text
MySQL
Redis
广告平台 API
公司内部接口
GitHub
文件系统
```

它只有模型本身。

------

# 三、LLM 本身并不会自动“做事情”

这是理解 Agent 的第一步。

比如你问：

```text
帮我把 Campaign 123 暂停。
```

LLM 即使回答：

```text
好的，Campaign 123 已暂停。
```

这也不代表：

```text
数据库真的更新了
```

更不代表：

```text
Facebook / Google Ads API 真被调用了
```

它可能只是生成了一句话。

这与我们前面学的幻觉直接相关：

```text
模型说：
“已经暂停”

≠

真实系统：
真的已经暂停
```

------

# 四、如果想让模型真的做事怎么办？

必须给它：

```text
外部能力
```

例如程序定义一个 Java 方法：

```java
public Campaign getCampaign(Long campaignId) {
    return campaignService.getById(campaignId);
}
```

再定义：

```java
public void pauseCampaign(Long campaignId) {
    campaignService.pause(campaignId);
}
```

模型本身不能直接运行这些 Java 方法。

但是我们的应用可以告诉模型：

```text
你现在有两个能力：

1. getCampaign
2. pauseCampaign
```

模型可以根据用户问题决定：

```text
我要调用 getCampaign
```

或者：

```text
我要调用 pauseCampaign
```

然后真正执行方法的仍然是：

```text
Java Application
```

------

# 五、这就是 Agent 开始出现的地方

普通 Chat：

```text
User
 ↓
LLM
 ↓
Answer
```

Agent：

```text
User
 ↓
LLM
 ↓
Decision
 ↓
Action / Tool
 ↓
Observation
 ↓
LLM
 ↓
Decision
 ↓
...
 ↓
Final Answer
```

这已经不再只是：

```text
问一次
答一次
```

而变成了：

```text
观察
↓
决策
↓
行动
↓
得到新信息
↓
再次决策
```

------

# 六、先给 Agent 一个最简单的定义

在我们当前学习阶段，可以先记：

> **Agent 是一个以模型作为决策核心，能够根据目标和当前状态决定下一步行动、调用外部能力，并根据行动结果继续推进任务的系统。**

注意这里有几个关键词：

```text
Goal
Decision
Action
Observation
State
Loop
```

这些以后会反复出现。

------

# 七、第一版核心公式

当前阶段，我们先使用这条最重要的公式：

```text
Agent
=
LLM
+
Tools
+
State
+
Loop
```

注意：

这不是一个严格数学公式。

而是一张：

```text
Agent 心智模型
```

意思是：

一个真正能够持续执行任务的 Agent，通常至少需要考虑：

```text
LLM
Tools
State
Loop
```

------

# 八、先分别解释这四个东西

```text
LLM
=
负责理解、推理和决策

Tools
=
让 Agent 可以和外部世界交互

State
=
让 Agent 知道任务现在进行到哪里

Loop
=
让 Agent 可以多轮执行，而不是执行一步就结束
```

可以先记成：

```text
LLM = 脑子

Tools = 手脚

State = 当前任务记录

Loop = 不断“想 → 做 → 看结果 → 再想”的机制
```

只是类比。

后面我们一个个拆。

------

# 九、LLM 在 Agent 中到底负责什么？

LLM 在 Agent 中最核心的作用通常不是：

```text
执行代码
```

而是：

```text
理解当前任务
↓
分析当前 Context
↓
决定下一步应该做什么
```

例如用户：

```text
为什么 Campaign 123 今天没有消耗？
```

Agent 当前已经知道：

```text
Campaign ID = 123
```

模型可能判断：

```text
我需要先查询 Campaign 当前状态。
```

于是产生：

```text
getCampaign(123)
```

然后程序执行。

------

# 十、所以 LLM 更像“决策器”

可以把 Agent 中的 LLM 理解成：

```text
Decision Engine
```

输入：

```text
Goal
Context
Current State
Available Tools
Previous Observations
```

输出可能是：

```text
调用哪个 Tool
传什么参数
是否继续
是否已经可以给最终答案
```

------

# 十一、LLM 并不等于整个 Agent

这是一个非常重要的认知。

错误理解：

```text
Agent = GPT / Qwen / Claude
```

并不准确。

模型只是：

```text
Agent Runtime 中的核心组件之一
```

一个真实 Agent 系统可能是：

```text
                  ┌────────────┐
                  │    LLM     │
                  └─────┬──────┘
                        │
                        ▼
                  Agent Runtime
                        │
       ┌────────────────┼────────────────┐
       ▼                ▼                ▼
     Tools            State           Memory
       │                │                │
       ▼                ▼                ▼
   Business API      Redis/DB        Vector DB
```

所以：

> **模型很重要，但 Agent 不只是模型。**

------

# 十二、什么是 Tool？

Tool 可以先理解为：

> **Agent 可以使用的外部能力。**

例如：

```text
查询数据库
调用 HTTP API
查询天气
发送邮件
搜索网页
读取文件
执行 Java 方法
创建工单
修改广告状态
```

都可以被包装成：

```text
Tool
```

------

# 十三、一个 Tool 最核心的组成

通常至少包括：

```text
Name
Description
Parameters
Execution Logic
```

例如：

```text
Name:
getCampaign

Description:
根据 Campaign ID 查询广告 Campaign 基础信息。

Parameters:
campaignId: Long
```

真正执行：

```java
campaignService.getById(campaignId);
```

------

# 十四、模型到底会不会直接执行 Tool？

不会。

这一点极其重要。

假设模型输出：

```json
{
  "name": "getCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

这只是：

```text
Tool Call Request
```

并不是：

```text
Java 方法已经执行
```

真正执行：

```java
getCampaign(123);
```

的是：

```text
Agent Runtime / Application
```

------

# 十五、一个完整 Tool Call 是怎样发生的？

用户：

```text
Campaign 123 现在是什么状态？
```

模型看到：

```text
Available Tools:

getCampaign(campaignId)
```

模型决定：

```text
我要调用 getCampaign
```

于是返回：

```json
{
  "tool": "getCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

程序解析：

```text
tool = getCampaign
campaignId = 123
```

然后执行：

```java
Campaign campaign = getCampaign(123L);
```

结果：

```json
{
  "id": 123,
  "status": "PAUSED"
}
```

程序再把结果交给模型。

模型最终回答：

```text
Campaign 123 当前状态是 PAUSED。
```

------

# 十六、完整流程画出来

```text
User
│
│ Campaign 123 是什么状态？
▼
LLM
│
│ 我要调用 getCampaign
▼
Agent Runtime
│
│ 执行 Java Method
▼
getCampaign(123)
│
│ 返回 Campaign
▼
Tool Result
│
│ {"status":"PAUSED"}
▼
LLM
│
│ 根据结果组织答案
▼
Final Answer
```

------

# 十七、这个过程中谁做了什么？

## LLM

负责：

```text
判断需要查询 Campaign
选择 getCampaign
生成参数 campaignId = 123
理解 Tool Result
生成最终回答
```

## Java Application

负责：

```text
真正执行 getCampaign
访问数据库
做权限校验
处理异常
返回结果
```

一定不能混。

------

# 十八、Tools 为什么让 LLM 变成 Agent 的基础？

没有 Tool 时：

```text
LLM
=
只能基于现有 Context 生成内容
```

拥有 Tool 以后：

```text
LLM
↓
可以决定查询外部世界
↓
拿到新的 Observation
↓
再继续推理
```

也就是说：

> **Tool 让模型从“只会说”开始拥有“行动能力”。**

------

# 十九、但只有 Tool 就一定叫 Agent 吗？

不一定。

比如：

```text
用户点击按钮
↓
程序固定调用 getWeather
↓
把天气结果交给 LLM 总结
```

这里：

```text
调用哪个 Tool
```

根本不是模型决定的。

程序员已经提前写死：

```text
永远调用 getWeather
```

这更接近：

```text
LLM-enhanced application
```

或者：

```text
固定 Workflow
```

而不是一个具有明显 Agent 决策特征的系统。

------

# 二十、Agent 的关键之一：模型参与“下一步选择”

例如拥有三个 Tool：

```text
getCampaign
getCampaignMetrics
getAccount
```

用户问：

```text
Campaign 123 为什么不花钱了？
```

程序没有写死：

```text
必须按 1 → 2 → 3 顺序调用
```

模型可能决定：

```text
第一步：
getCampaign(123)
```

发现：

```text
status = PAUSED
```

那么任务可能已经结束：

```text
因为 Campaign 已暂停。
```

它就根本不需要继续查：

```text
Metrics
Account
```

这就是：

```text
动态决策
```

------

# 二十一、再看另一个情况

如果：

```text
getCampaign(123)
```

返回：

```text
status = ACTIVE
```

模型可能继续判断：

```text
状态正常，那我要查 Metrics。
```

于是：

```text
getCampaignMetrics(123)
```

结果：

```text
spend = 0
impressions = 0
```

模型继续：

```text
可能是账户问题，再查 Account。
```

于是：

```text
getAccount(...)
```

这就是 Agent 的：

```text
多步决策
```

------

# 二十二、什么是 Observation？

Observation 可以简单理解为：

> **Agent 执行动作以后，从外部世界得到的结果。**

例如：

```text
Action:
getCampaign(123)

Observation:
status = ACTIVE
```

或者：

```text
Action:
getCampaignMetrics(123)

Observation:
spend = 0
impressions = 0
```

------

# 二十三、为什么 Observation 特别重要？

因为 Agent 下一步的决策通常依赖：

```text
上一动作产生的结果
```

没有 Observation：

```text
Agent 就不知道刚才做的事情发生了什么
```

例如：

```text
暂停 Campaign
```

Tool Result：

```text
SUCCESS
```

和：

```text
PERMISSION_DENIED
```

会导致完全不同的下一步。

------

# 二十四、Agent 的基本循环

我们可以第一次正式引入：

```text
Agent Loop
```

基本结构：

```text
Goal
 ↓
Observe
 ↓
Think / Decide
 ↓
Act
 ↓
Observe
 ↓
Think / Decide
 ↓
Act
 ↓
...
 ↓
Finish
```

更工程一点：

```text
Context
↓
LLM
↓
Decision
↓
Tool Call
↓
Tool Execution
↓
Tool Result
↓
Update Context / State
↓
LLM
↓
...
```

------

# 二十五、什么是 Loop？

Loop 就是：

> **Agent 不会只让模型决定一次，而是可以根据每一步执行结果不断重新决策。**

普通 Chat：

```text
LLM 调一次
↓
结束
```

Agent：

```text
LLM
↓
Tool
↓
LLM
↓
Tool
↓
LLM
↓
...
```

直到：

```text
任务完成
```

或者：

```text
失败
```

或者：

```text
达到最大步骤数
```

------

# 二十六、为什么 Agent 一定需要 Loop？

考虑任务：

```text
帮我找出 Campaign 123 没有消耗的原因。
```

Agent 开始时并不知道答案。

必须一步步收集信息：

```text
查 Campaign
↓
发现 ACTIVE
↓
查 Metrics
↓
发现 0 Impression
↓
查 Account
↓
发现账户被限制
↓
得出结论
```

如果只允许：

```text
调用一次模型
```

那么模型不可能在后续 Tool Result 出现之后：

```text
继续决定下一步
```

因此必须有 Loop。

------

# 二十七、Agent Loop 本质上是什么？

其实从 Java 程序员角度看，很简单。

大概就是：

```java
while (!finished) {

    Decision decision = llm.decide(context);

    if (decision.isFinalAnswer()) {
        return decision.getAnswer();
    }

    ToolResult result =
            toolExecutor.execute(decision.getToolCall());

    context.add(result);
}
```

当然真实代码复杂很多。

但本质真的很接近：

```text
while
```

------

# 二十八、Agent 没有魔法

很多 Agent Framework 看起来特别神奇：

```text
LangGraph
OpenAI Agents SDK
Spring AI
LangChain
LangChain4j
```

但底层核心流程往往依然离不开：

```text
请求模型
↓
检查模型是否请求 Tool
↓
执行 Tool
↓
把 Tool Result 放回 Context
↓
再次请求模型
```

也就是：

```text
Loop
```

------

# 二十九、Tool Calling 和 Agent Loop 的关系

Tool Calling 解决：

```text
模型如何表达：
“我想调用这个工具”
```

Agent Loop 解决：

```text
工具执行完以后怎么办？
```

完整结构：

```text
LLM
↓
Tool Call
↓
Tool Execution
↓
Tool Result
↓
LLM
↓
继续？
```

------

# 三十、只有 Tool Calling，没有 Loop 会怎样？

例如模型：

```text
我要调用 getCampaign
```

程序执行：

```text
getCampaign
```

得到：

```text
ACTIVE
```

然后程序结束。

那模型无法：

```text
根据 ACTIVE 决定继续查 Metrics
```

所以：

```text
Tool Calling
≠
完整 Agent
```

它只是 Agent 的重要基础能力。

------

# 三十一、现在讲 State

这是第二个特别容易被初学者忽略的概念。

State 可以先理解为：

> **任务当前进行到什么状态。**

例如：

```json
{
  "goal": "分析 Campaign 123 无消耗原因",
  "campaignId": 123,
  "step": 2,
  "campaignStatus": "ACTIVE",
  "metricsLoaded": true,
  "accountLoaded": false
}
```

这就是一种：

```text
Task State
```

------

# 三十二、为什么 Agent 需要 State？

普通 Chat：

```text
问
↓
答
↓
结束
```

状态非常简单。

但 Agent：

```text
第 1 步
第 2 步
第 3 步
第 4 步
```

系统需要知道：

```text
目标是什么？
已经做过什么？
得到了什么？
现在做到哪里？
下一步还能做什么？
任务是否完成？
```

这些都和 State 有关。

------

# 三十三、没有 State 会发生什么？

假设 Agent：

```text
查 Campaign
↓
查 Metrics
↓
查 Account
```

如果它忘记：

```text
Campaign 已经查过
```

可能再次：

```text
getCampaign
```

再下一轮又：

```text
getCampaign
```

陷入：

```text
重复调用
```

甚至死循环。

------

# 三十四、State 不一定全部存在模型 Context 里

这是 Node 005 刚学过的东西。

系统可能在：

```text
Java Object
Redis
MySQL
Workflow Engine
```

保存 State。

例如：

```java
public class AgentState {

    private String taskId;

    private Long campaignId;

    private int step;

    private Set<String> executedTools;

    private Map<String, Object> observations;

}
```

然后只把当前模型真正需要的信息：

```text
注入 Context
```

而不是：

```text
所有 State 永远全部塞进去
```

------

# 三十五、State 和 Memory 有什么区别？

当前阶段先简单理解：

```text
State
=
当前任务的运行状态

Memory
=
跨任务、跨会话保存的长期信息
```

例如：

```text
用户喜欢 Java
```

更像：

```text
Memory
```

而：

```text
当前 Campaign 分析任务已经执行到第 3 步
```

更像：

```text
State
```

------

# 三十六、State 和 Context 又有什么区别？

继续复习 Node 005：

```text
State
=
系统保存的任务状态

Context
=
本轮模型实际看到的信息
```

可能：

```text
State
↓
选择部分字段
↓
Context
↓
LLM
```

------

# 三十七、现在把四个核心组成重新放一起

```text
Agent
=
LLM
+
Tools
+
State
+
Loop
```

具体：

```text
LLM
↓
决定下一步做什么

Tools
↓
执行外部动作

State
↓
记录任务进行到哪里

Loop
↓
不断执行“决定 → 行动 → 观察 → 再决定”
```

------

# 三十八、一个完整 Agent 任务

用户：

```text
为什么 Campaign 123 今天没有消耗？
```

系统初始 State：

```json
{
  "goal": "diagnose_campaign",
  "campaignId": 123,
  "step": 0
}
```

可用 Tools：

```text
getCampaign
getCampaignMetrics
getAccount
```

------

# 三十九、第一轮

Context：

```text
Goal:
分析 Campaign 123 无消耗原因

Available Tools:
getCampaign
getCampaignMetrics
getAccount
```

LLM：

```text
首先查询 Campaign 状态。
```

Tool Call：

```json
{
  "name": "getCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

------

# 四十、程序执行

Java：

```java
Campaign campaign =
        campaignService.getById(123L);
```

结果：

```json
{
  "id": 123,
  "status": "ACTIVE"
}
```

这就是：

```text
Observation 1
```

State 更新：

```json
{
  "campaignStatus": "ACTIVE",
  "step": 1
}
```

------

# 四十一、第二轮

Context 加入：

```text
Campaign 123 status = ACTIVE
```

LLM 再判断：

```text
Campaign 没有暂停，需要查询今日指标。
```

Tool Call：

```json
{
  "name": "getCampaignMetrics",
  "arguments": {
    "campaignId": 123
  }
}
```

------

# 四十二、第二次 Observation

Tool Result：

```json
{
  "spend": 0,
  "impressions": 0,
  "clicks": 0
}
```

模型看到：

```text
0 展示
```

可能判断：

```text
问题发生在流量进入之前。
```

下一步：

```text
查询 Account。
```

------

# 四十三、第三轮

Tool：

```text
getAccount
```

Result：

```json
{
  "status": "SUSPENDED"
}
```

模型终于具备足够信息。

于是返回：

```text
Campaign 本身处于 ACTIVE，
但是所属广告账户已经被 SUSPENDED，
因此今天没有产生展示和消耗。
```

任务结束。

------

# 四十四、这个任务真正发生了什么？

不是：

```text
用户问
↓
模型突然知道答案
```

而是：

```text
Goal
↓
模型决定查 Campaign
↓
Observation
↓
模型决定查 Metrics
↓
Observation
↓
模型决定查 Account
↓
Observation
↓
得出答案
```

这才是：

```text
Agentic Process
```

------

# 四十五、Decision 是 Agent 的核心

为什么很多人会把：

```text
自主决策
```

和 Agent 联系起来？

因为普通程序通常是：

```java
if (...) {
    doA();
} else {
    doB();
}
```

路径由程序员提前定义。

Agent 中部分路径可能由模型动态判断：

```text
当前情况应该：
A？
B？
C？
还是结束？
```

------

# 四十六、但是“自主”千万不要理解成无限自由

Agent 的自主通常是：

> **在程序定义的边界内进行动态选择。**

比如你的系统只暴露：

```text
getCampaign
getMetrics
getAccount
```

模型不能凭空：

```text
转账
删除服务器
重启数据库
```

因为：

```text
它没有这些 Tool
```

------

# 四十七、Agent 的自主性是被约束的

一个好的生产 Agent 应该是：

```text
Bounded Autonomy
```

也就是：

```text
有限自主
```

例如：

```text
可以自由选择查询 Tool

但不能自由执行 DELETE
```

或者：

```text
读取操作自动执行

写操作必须用户确认
```

这会在：

```text
Human-in-the-loop
Security
Production Agent
```

阶段继续深入。

------

# 四十八、Tool 是 Agent 能力边界的一部分

如果提供：

```text
getCampaign
```

Agent 可以：

```text
查 Campaign
```

如果没有：

```text
pauseCampaign
```

那 Agent 就不能真正：

```text
暂停 Campaign
```

这是一件好事。

因为：

> **系统可以通过控制 Tool 集合控制 Agent 能做什么。**

------

# 四十九、Read Tool 和 Write Tool 风险不同

例如：

```text
getCampaign
```

属于：

```text
Read Tool
```

风险通常较低。

而：

```text
pauseCampaign
deleteCampaign
updateBudget
```

属于：

```text
Write Tool
```

风险明显更高。

以后 Agent 不能简单设计成：

```text
LLM 一想调用
↓
马上执行
```

尤其涉及：

```text
删除
支付
发消息
修改生产数据
```

时。

------

# 五十、Agent ≠ “模型拥有所有权限”

恰恰相反。

好的 Agent 应该有：

```text
最小权限
```

例如广告诊断 Agent：

```text
只读数据库
```

而广告运营 Agent：

```text
可以修改部分状态
```

财务 Agent：

```text
可能只能查询，不能付款
```

Agent 的能力应由业务边界决定。

------

# 五十一、为什么 Agent 比普通 Chat 风险更高？

普通 Chat 幻觉：

```text
模型说错一句话
```

可能只是：

```text
信息错误
```

Agent 幻觉：

```text
模型误判
↓
调用错误 Tool
↓
执行真实操作
```

可能变成：

```text
真实系统事故
```

------

# 五十二、举个极端例子

用户：

```text
帮我看看 Campaign 123 为什么表现差。
```

模型误判成：

```text
应该删除 Campaign。
```

如果 Agent 拥有：

```text
deleteCampaign
```

且：

```text
不需要确认
```

那问题就严重了。

因此 Agent 系统必须：

```text
权限
校验
审批
确认
幂等
审计
```

------

# 五十三、模型决定，不代表程序必须执行

这是非常重要的一条原则。

模型：

```text
我想调用 deleteCampaign(123)
```

程序完全可以：

```text
拒绝
```

例如：

```java
if (!permissionService.canDelete(userId, campaignId)) {
    throw new PermissionDeniedException();
}
```

甚至：

```text
DELETE Tool 根本不给模型
```

------

# 五十四、Agent Runtime 才是执行层

Agent Runtime 大概负责：

```text
管理模型调用
管理 Tool
校验 Tool 参数
执行 Tool
维护 State
控制 Loop
控制最大步数
处理异常
记录 Trace
决定什么时候结束
```

它并不是：

```text
LLM 本身
```

------

# 五十五、可以第一次认识 Agent Runtime

未来我们的 Java Agent 项目可能逐渐形成：

```text
AgentService
↓
AgentRuntime
├── ModelClient
├── ToolRegistry
├── ToolExecutor
├── StateManager
├── ContextBuilder
└── LoopController
```

现在不要写。

我们只是开始知道：

> **为什么以后这些层会自然出现。**

------

# 五十六、模型会怎么表示“我要调用 Tool”？

以后 Tool Calling Node 会详细学。

现在只需要知道：

模型可能输出类似：

```json
{
  "name": "getCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

Provider API 也可能以专门字段表达：

```text
tool_calls
```

而不是普通文本。

程序检测到：

```text
模型请求 Tool
```

以后再执行。

------

# 五十七、为什么不用让模型直接输出：

```text
请调用 getCampaign
```

然后字符串解析？

当然也能自己这么做。

但是现代模型 API 通常提供：

```text
Structured Tool Calling
```

让 Tool Name 和 Arguments 有明确结构。

相比：

```text
从自然语言里猜模型想调用什么
```

更稳定。

------

# 五十八、Agent Loop 的结束条件是什么？

一个 Loop 不能：

```text
永远跑
```

通常需要结束条件。

例如：

```text
模型返回 Final Answer
```

或者：

```text
任务完成
```

或者：

```text
达到最大步骤数
```

或者：

```text
发生不可恢复错误
```

或者：

```text
需要 Human Approval
```

------

# 五十九、为什么一定要 Max Steps？

因为模型可能出现：

```text
A Tool
↓
B Tool
↓
A Tool
↓
B Tool
↓
A Tool
↓
……
```

无限循环。

所以：

```text
maxSteps = 10
```

之类的限制非常重要。

否则 Agent 可能：

```text
无限调用 API
无限消耗 Token
无限产生费用
```

------

# 六十、一个最简单的 Java Agent Loop

先看伪代码：

```java
public String run(String userInput) {

    AgentState state = new AgentState(userInput);

    for (int step = 0; step < MAX_STEPS; step++) {

        ModelResponse response = model.call(state);

        if (response.hasFinalAnswer()) {
            return response.getFinalAnswer();
        }

        ToolCall toolCall = response.getToolCall();

        ToolResult result = toolExecutor.execute(toolCall);

        state.addObservation(result);
    }

    throw new AgentMaxStepsException();
}
```

这已经非常接近：

```text
Agent Runtime 核心循环
```

------

# 六十一、以后框架到底帮你做了什么？

例如未来使用：

```text
Spring AI
LangChain4j
OpenAI Agents SDK
LangGraph
```

框架可能帮你处理：

```text
模型请求
Tool Schema
Tool Dispatch
Message 构造
Tool Result 回传
Loop
State
Tracing
```

但如果你不知道底层：

```text
LLM → Tool → Observation → LLM
```

你就会感觉框架像魔法。

------

# 六十二、我们的路线为什么不直接从 Agent Framework 开始？

因为如果一上来：

```text
Spring AI Agent
```

然后：

```java
agent.run();
```

虽然代码跑起来了。

但你可能完全不知道：

```text
Agent 到底为什么能行动？

Tool 到底是谁执行？

Loop 在哪？

State 在哪？

模型什么时候再次调用？

为什么会停？

为什么会死循环？
```

那只是：

```text
会用 API
```

不是：

```text
理解 Agent
```

------

# 六十三、什么叫 Agentic？

你以后经常会看到：

```text
Agent
Agentic
Agentic Workflow
Agentic System
```

Agentic 可以先理解成：

```text
具有一定 Agent 特征
```

例如：

```text
模型动态选择 Tool
模型根据结果决定下一步
存在多步循环
```

都带有：

```text
Agentic Behavior
```

------

# 六十四、Agent 一定要有 Tools 吗？

从严格学术或不同产品定义来看：

```text
Agent
```

这个词没有一个所有人完全统一的边界。

有些定义会把：

```text
能够自主规划、多轮行动
```

也视为 Agent。

但在我们这条：

```text
Agent Engineer 工程路线
```

中，最实用的理解是：

> **如果模型完全无法与外部环境交互，只能生成文本，那么它距离我们真正关心的工程 Agent 还差很远。**

所以 Tools 是我们的核心组成。

------

# 六十五、Agent 一定要有 Memory 吗？

不一定。

一个很简单的 Agent 可以：

```text
只完成当前任务
```

任务结束后：

```text
什么都不长期保存
```

依然可以有：

```text
LLM + Tools + State + Loop
```

所以：

```text
Memory
```

非常重要，但不一定是所有 Agent 的最低组成。

这也是为什么我们的第一版公式没有写：

```text
Memory
```

------

# 六十六、为什么公式里有 State，但没有 Memory？

因为多步 Agent 只要存在：

```text
当前任务
```

通常就需要某种状态。

但长期 Memory 是：

```text
跨任务 / 跨会话
```

的更高级能力。

所以：

```text
State
```

更接近 Agent Loop 的最低需求。

------

# 六十七、Agent 一定要有 RAG 吗？

不一定。

例如天气 Agent：

```text
getWeather Tool
```

就能工作。

完全不需要：

```text
Vector Database
```

所以：

```text
RAG
```

只是 Agent 可以使用的一种外部能力或 Context 来源。

不是 Agent 的定义本身。

------

# 六十八、Agent 一定要有向量数据库吗？

完全不是。

这是非常常见的初学者误区：

```text
Agent
=
LLM
+
Vector DB
+
LangChain
```

错。

最简单 Agent：

```text
LLM
+
1 个 Tool
+
Loop
```

就已经可以具有 Agent 行为。

------

# 六十九、Agent 一定要用 Python 吗？

当然不是。

Agent 是：

```text
系统设计模式 / AI 应用形态
```

不是：

```text
Python 专属技术
```

Java 一样可以实现：

```text
HTTP
LLM API
Tool Calling
State
Loop
```

只是目前 Python Agent 生态：

```text
框架更多
社区更活跃
实验速度更快
```

所以我们以后会补 Python。

但当前主线仍然：

```text
Java + Spring Boot
```

------

# 七十、Agent 和 Chatbot 有什么区别？

一个最简单 Chatbot：

```text
User
↓
LLM
↓
Text
```

Agent 更强调：

```text
Goal
↓
Decision
↓
Action
↓
Observation
↓
New Decision
```

Chatbot 重点：

```text
Conversation
```

Agent 重点：

```text
Task Execution
```

当然现实产品会混合。

一个 Chat 产品完全可以内部使用 Agent。

------

# 七十一、比如 ChatGPT 这种产品就不能简单理解成一个 LLM

现代 AI 产品可能组合：

```text
LLM
Web Search
Files
Code Execution
Memory
Connectors
Tools
Agent Runtime
```

你看到的：

```text
聊天界面
```

只是：

```text
UI
```

背后可能存在非常复杂的 Agent 系统。

所以：

```text
Chat UI
≠
只是单次 LLM 调用
```

------

# 七十二、Agent 和传统自动化有什么区别？

传统自动化：

```text
程序员提前定义路径
```

例如：

```text
if status == ACTIVE
    queryMetrics()

if spend == 0
    queryAccount()
```

逻辑完全写死。

Agent：

```text
模型根据当前 Context
动态判断下一步
```

------

# 七十三、这是不是意味着 Agent 比传统代码更好？

不是。

千万不要形成：

```text
Agent > Traditional Code
```

很多任务根本不应该交给模型动态判断。

例如：

```text
金额计算
权限判断
事务
订单状态流转
支付逻辑
数据库约束
```

这些适合：

```text
确定性代码
```

------

# 七十四、Agent 真正适合什么问题？

通常更适合：

```text
路径不固定
输入自然语言
需要理解语义
需要动态选择工具
需要根据观察结果继续调整
任务步骤无法完全提前穷举
```

例如：

```text
帮我分析这个广告为什么效果突然变差。
```

它可能需要查：

```text
Campaign
Metrics
Account
Creative
Budget
Platform Error
```

到底查哪些，要看中间结果。

------

# 七十五、不适合 Agent 的例子

例如：

```text
用户注册
↓
校验手机号
↓
写数据库
↓
发送验证码
```

流程非常稳定。

没有必要：

```text
每一步让 LLM 决定下一步
```

直接写：

```text
Java Workflow
```

通常更合理。

------

# 七十六、这会直接引出下一 Node

下一篇：

```text
Node 007：Workflow 与 Agent
```

会正式解决：

```text
什么时候应该写固定 Workflow？

什么时候应该给模型决策权？

Workflow 和 Agent 到底是不是二选一？

Agentic Workflow 又是什么？
```

所以这篇暂时只建立概念。

------

# 七十七、Agent 的“目标”也非常重要

Agent 通常不是：

```text
随便运行
```

而是围绕：

```text
Goal
```

例如：

```text
分析 Campaign 123 无消耗原因
```

整个 Loop 都应该围绕这个 Goal。

------

# 七十八、如果 Goal 不清晰会发生什么？

比如用户：

```text
看看这个广告。
```

到底：

```text
看什么？
状态？
性能？
风险？
创意？
预算？
```

目标模糊。

Agent 可能：

```text
乱调用 Tool
```

所以 Agent 也需要：

```text
Task Understanding
```

有时甚至需要：

```text
向用户补充询问
```

------

# 七十九、Agent 会不会自己“规划”？

可能。

例如复杂任务：

```text
帮我分析最近 7 天 Campaign 123 的异常，并提出优化建议。
```

模型可能内部形成：

```text
1. 查 Campaign
2. 查 7 天 Metrics
3. 查 Creative
4. 查 Account
5. 对比趋势
6. 输出分析
```

这可以视为：

```text
Planning
```

但 Planning 并不是所有 Agent 都必须显式拥有。

------

# 八十、不要过早把 Agent 理解成“先生成完整计划”

Agent 也可以：

```text
一步一判断
```

例如：

```text
先查 Campaign
↓
看看结果
↓
再决定下一步
```

这叫：

```text
Reactive
```

风格。

有些 Agent：

```text
Planning + Execution
```

有些：

```text
Reactive Loop
```

以后再慢慢学习。

------

# 八十一、什么是 ReAct？

你以后会经常看到：

```text
ReAct
```

它来源于：

```text
Reasoning + Acting
```

直观理解：

```text
Reason
↓
Act
↓
Observe
↓
Reason
↓
Act
```

它和我们现在学的：

```text
Decision
Action
Observation
Loop
```

高度相关。

当前阶段知道概念即可。

------

# 八十二、Agent 的 Observation 一定来自 Tool 吗？

不一定。

可能来自：

```text
Tool Result
User Reply
Environment State
Workflow State
Error
Human Approval
```

例如：

```text
Tool：
pauseCampaign

Result：
需要管理员审批
```

那么：

```text
需要审批
```

也是新的 Observation。

------

# 八十三、错误也是 Observation

比如：

```text
getCampaignMetrics
```

失败：

```text
HTTP 429
```

或者：

```text
TIMEOUT
```

Agent 下一步可能：

```text
Retry
```

也可能：

```text
换 Tool
```

也可能：

```text
告诉用户暂时无法查询
```

所以 Error 不应该简单丢掉。

它也是：

```text
Agent 需要处理的环境反馈
```

------

# 八十四、Agent 为什么需要 Error Handling？

因为真实世界永远会出现：

```text
Timeout
Rate Limit
Permission Denied
Invalid Parameter
Network Error
Database Error
Tool Not Found
```

如果每次 Tool 出错：

```text
Agent 直接崩
```

就很难称为可靠系统。

------

# 八十五、但 Error Handling 不能全交给 LLM

例如：

```text
HTTP 429
```

程序可以明确：

```text
指数退避 Retry
```

没有必要让模型：

```text
猜怎么重试
```

这仍然体现：

> **确定性问题优先交给确定性程序。**

------

# 八十六、Agent 不应该负责所有控制逻辑

未来比较成熟的结构往往是：

```text
确定性程序
+
LLM 动态决策
```

而不是：

```text
所有东西都让 LLM 决定
```

比如：

```text
权限检查
最大步骤
Timeout
Retry Policy
Schema Validation
```

应该由程序控制。

------

# 八十七、Agent 不是“让 AI 接管程序”

正确理解应该是：

> **程序设计一个受控运行环境，让模型在其中负责某些不确定决策。**

这个区别非常关键。

```text
Application
```

仍然是主人。

```text
LLM
```

是一个决策组件。

------

# 八十八、可以这样理解职责边界

## 程序负责

```text
有哪些 Tool
谁有权限
参数是否合法
什么时候停止
最多执行几步
哪些操作需要确认
怎么 Retry
怎么记录日志
怎么处理事务
```

## 模型负责

```text
理解用户意图
分析当前信息
判断下一步需要什么
选择合适 Tool
理解 Observation
组织最终结果
```

------

# 八十九、Agent 的可靠性从哪里来？

不是：

```text
模型够聪明
```

就够了。

而是：

```text
Model
+
Tool Design
+
State Management
+
Validation
+
Permissions
+
Loop Control
+
Observability
+
Evaluation
```

共同决定。

------

# 九十、这就是为什么 Agent Engineer 不等于 Prompt Engineer

Prompt Engineer 可能主要关心：

```text
怎么写 Prompt
```

Agent Engineer 还要关心：

```text
Tool
State
Loop
API
数据库
权限
异常
Context
Memory
RAG
Workflow
Tracing
Eval
安全
```

所以你的 Java 后端经验其实非常有用。

------

# 九十一、Java 后端能力为什么和 Agent 很搭？

你已经熟悉：

```text
Controller
Service
Database
Redis
MQ
HTTP API
异常处理
状态机
权限
事务
线程池
日志
```

未来 Agent 依然需要：

```text
这些传统工程能力
```

区别是多了一个：

```text
LLM Decision Layer
```

------

# 九十二、未来 Agent 系统可能是什么样？

例如：

```text
                    User
                     │
                     ▼
                Controller
                     │
                     ▼
                AgentService
                     │
                     ▼
                AgentRuntime
                     │
        ┌────────────┼────────────┐
        ▼            ▼            ▼
       LLM          State        Tools
                      │            │
                      ▼            ▼
                  Redis/DB     Business API
                                   │
                                   ▼
                             Facebook / Google
```

你会发现：

```text
绝大多数东西依然是后端工程
```

------

# 九十三、Agent Runtime 的核心伪代码再看一次

```java
while (true) {

    Context context =
            contextBuilder.build(state);

    ModelResponse response =
            modelClient.call(context);

    if (response.isFinal()) {
        return response.getAnswer();
    }

    ToolCall call =
            response.getToolCall();

    validate(call);

    ToolResult result =
            toolExecutor.execute(call);

    state.record(call, result);
}
```

这段伪代码非常重要。

你以后学很多框架：

```text
最终都可以回来对照它
```

------

# 九十四、这和我们 Node 005 的 Context 直接连起来了

每一次 Loop：

```text
State
+
History
+
Tool Result
+
Goal
+
Available Tools
↓
ContextBuilder
↓
LLM
```

所以 Agent：

```text
不是只有 Loop
```

而是：

```text
每一轮都在重新构造 Context
```

------

# 九十五、Agent Loop 其实也是 Context Loop

第一轮：

```text
Context 1
↓
LLM
↓
Tool 1
```

执行以后：

```text
Observation 1
```

第二轮：

```text
Context 2
=
Context 1
+
Observation 1
```

然后：

```text
LLM
↓
Tool 2
```

如此循环。

------

# 九十六、这也解释了为什么长任务需要 Context Management

如果 Agent 执行：

```text
100 步
```

不断加入：

```text
Tool Calls
Tool Results
Reasoning
Messages
```

Context 会越来越长。

所以以后长期 Agent 一定会碰到：

```text
Context Compression
State
Memory
Artifacts
Retrieval
```

------

# 九十七、Agent 和模型的关系可以怎么理解？

模型：

```text
是推理核心
```

Agent：

```text
是围绕模型构建出来的完整执行系统
```

类比：

```text
CPU
≠
Computer
```

CPU 非常重要。

但电脑还有：

```text
内存
硬盘
操作系统
IO
网络
```

同样：

```text
LLM
≠
Agent
```

------

# 九十八、LLM 就像一个“不会直接碰业务系统的大脑”

Agent Runtime 告诉它：

```text
你现在可以使用：
Tool A
Tool B
Tool C
```

模型：

```text
决定我想用哪个
```

Runtime：

```text
验证
↓
执行
↓
返回结果
```

这就是非常好的第一层理解。

------

# 九十九、Agent 到底“聪明”在哪里？

很多时候不是因为：

```text
模型智商突然提高
```

而是：

```text
模型可以获取新信息
可以行动
可以观察结果
可以继续修正
```

例如普通 LLM 不知道：

```text
Campaign 123 当前状态
```

Agent 可以：

```text
查询
```

所以看起来更强。

------

# 一百、Agent 可以纠正自己吗？

一定程度上可以。

例如 Agent 猜测：

```text
Campaign 可能暂停了
```

于是查询：

```text
getCampaign
```

结果：

```text
ACTIVE
```

模型就会更新判断：

```text
之前猜错了
```

然后继续调查。

这是 Tool / Observation 带来的巨大价值。

------

# 一百零一、但是 Agent 仍然会犯错

Agent 可能：

```text
选错 Tool
参数填错
误解 Tool Result
重复调用
提前结束
形成错误结论
```

所以 Agent 不是：

```text
LLM + Tool = 绝对可靠
```

而只是：

```text
获得了更强的执行能力
```

------

# 一百零二、模型越强，Agent 就一定越好吗？

通常更强模型可能带来：

```text
更好的理解
更好的 Tool Selection
更好的复杂推理
```

但 Agent 整体质量依然受：

```text
Tool Design
Context
State
Prompt
Runtime
Data Quality
```

影响。

所以同一个模型：

```text
不同 Agent 架构
```

表现可以差很多。

------

# 一百零三、Tool Description 为什么以后会特别重要？

假设两个 Tool：

```text
getData
getInfo
```

模型根本不知道：

```text
什么时候用哪个
```

如果改成：

```text
getCampaignCurrentStatus

getCampaignPerformanceMetrics
```

模型更容易判断。

所以 Tool 本身也是：

```text
模型的 Interface
```

------

# 一百零四、Tool 可以类比 Java Interface

例如：

```java
public interface CampaignTools {

    Campaign getCampaign(Long campaignId);

    CampaignMetrics getMetrics(Long campaignId);

}
```

模型看到的不是 Java 实现。

而是类似：

```text
方法名
描述
参数 Schema
```

然后根据这些：

```text
选择调用
```

------

# 一百零五、Agent 的 Tool 更像“API Contract”

模型不会看到：

```java
campaignMapper.selectById(...)
```

这种内部实现。

它只需要知道：

```text
Tool Name
Tool Description
Input Schema
```

这和：

```text
REST API Contract
```

非常像。

------

# 一百零六、Tool Result 也应该是契约

比如不要随便返回：

```text
一大段无法理解的字符串
```

更合理是：

```json
{
  "campaignId": 123,
  "status": "ACTIVE",
  "budget": 100
}
```

让模型更稳定理解。

这会和未来：

```text
Structured Output
JSON Schema
Tool Schema
```

串起来。

------

# 一百零七、Agent 也是一个“控制循环”

从软件工程角度看：

```text
Observe
↓
Decide
↓
Act
↓
Observe
```

很像：

```text
Control Loop
```

不断：

```text
读取环境
↓
做决策
↓
影响环境
↓
读取新环境
```

------

# 一百零八、Agent 和机器人为什么概念很像？

实体机器人：

```text
摄像头
↓
观察环境
↓
控制系统
↓
决定动作
↓
机械臂
↓
环境变化
```

软件 Agent：

```text
Context / Tool Result
↓
观察
↓
LLM
↓
决定动作
↓
Tool
↓
系统变化
```

本质结构很相似。

------

# 一百零九、Software Agent 的环境是什么？

可能是：

```text
Web
Database
API
GitHub
Gmail
Calendar
File System
Browser
Business System
```

Tools 就是 Agent 接触这些环境的：

```text
接口
```

------

# 一百一十、Agent 的能力由三件东西共同决定

粗略可以记：

```text
能不能理解
↓
Model

能不能做
↓
Tools

能不能持续做
↓
State + Loop
```

这是一个很好记的结构。

------

# 一百一十一、一个没有 Tools 的强模型

例如：

```text
非常聪明的 LLM
```

但：

```text
没有任何外部 Tool
```

它可能非常会分析。

却不能：

```text
获取实时数据库信息
修改业务状态
读取内部文件
调用 API
```

------

# 一百一十二、一个 Tools 很多但模型很差的 Agent

反过来：

```text
100 个 Tool
```

但模型：

```text
经常选错工具
经常填错参数
```

也很难用。

所以：

```text
Model Capability
```

仍然重要。

------

# 一百一十三、有模型、有 Tool，但没有 State

可能：

```text
执行几步以后忘了目标
重复调用
无法判断完成度
```

因此多步任务很困难。

------

# 一百一十四、有前面三者，但没有 Loop

模型只能：

```text
决定一次
```

无法：

```text
根据 Observation 再做第二次决定
```

于是 Agent 能力依然有限。

------

# 一百一十五、所以公式为什么合理？

```text
Agent
=
LLM
+
Tools
+
State
+
Loop
```

因为它们分别解决：

```text
LLM
→ 决策

Tools
→ 行动

State
→ 连续性

Loop
→ 多步执行
```

------

# 一百一十六、以后公式还会扩展吗？

会。

以后可能加入：

```text
Memory
RAG
Workflow
Human Approval
Planning
Observability
Evaluation
Security
```

但这些是在：

```text
基础 Agent
```

之上逐渐增加的。

当前不要一次塞满。

------

# 一百一十七、我们为什么先学“最小 Agent”？

因为只有看清：

```text
最小骨架
```

以后才知道：

```text
Memory 为什么出现
RAG 为什么出现
Workflow 为什么出现
MCP 为什么出现
LangGraph 为什么出现
```

否则容易变成：

```text
框架名词收藏家
```

------

# 一百一十八、一个最小 Agent 可以只有什么？

例如：

```text
LLM
+
getWeather Tool
+
Loop
```

用户：

```text
北京天气怎么样？
```

模型：

```text
调用 getWeather("Beijing")
```

程序：

```text
执行 API
```

模型：

```text
根据结果回答
```

这已经可以算：

```text
非常简单的 Tool-using Agent
```

------

# 一百一十九、复杂 Agent 只是不断往上叠能力

```text
最小 Tool Agent
↓
Multiple Tools
↓
Agent Loop
↓
State
↓
Memory
↓
RAG
↓
Workflow
↓
MCP
↓
Human Approval
↓
Tracing
↓
Evaluation
↓
Security
↓
Production Agent
```

这基本也是我们的学习路线。

------

# 一百二十、为什么现在还不写 Agent 代码？

因为我们还没真正学：

```text
模型 API
Message
Structured Output
Tool Calling
Tool Schema
Tool Dispatcher
```

现在如果直接让 Codex：

```text
给我写一个 Agent
```

Codex 很容易生成：

```text
几十个类
框架代码
Tool
Loop
配置
```

然后你只能：

```text
运行成功
```

但：

```text
看不懂
```

这违反我们的路线原则。

------

# 一百二十一、当前阶段只要做到“脑子里能跑起来”

当用户说：

```text
帮我查 Campaign 123 为什么没消耗
```

你脑子里应该自动浮现：

```text
User Goal
↓
LLM
↓
选择 Tool
↓
Runtime 执行
↓
Observation
↓
State 更新
↓
再次 LLM
↓
选择下一个 Tool
↓
...
↓
Final Answer
```

如果能这样想，006 就学到核心了。

------

# 一百二十二、把 Agent 和前五节彻底连起来

## LLM

```text
Agent 的决策核心
```

## Training / Inference

```text
Agent 每次调用 LLM
本质仍然属于 Inference
```

## Hallucination

```text
Agent 不能盲信模型
需要 Tool / Grounding / Validation
```

## Token

```text
每次 Agent Loop 都消耗 Token
```

## Context Window

```text
Tool Results
History
State
Tools
都会占 Context
```

## Agent

```text
利用 LLM
+
外部能力
+
状态
+
循环
去持续完成任务
```

现在整个体系终于连起来了。

------

# 一百二十三、一个重要认知：Agent 不是新模型

创建 Agent：

```text
不需要重新训练一个模型
```

你可以拿现成：

```text
Qwen
GPT
Claude
DeepSeek
```

然后在应用层增加：

```text
Tools
State
Loop
```

构成 Agent。

所以：

```text
Agent Engineering
```

主要属于：

```text
Application Layer
```

------

# 一百二十四、Agent 和 Fine-tuning 没有直接绑定

你可以：

```text
完全不 Fine-tune
```

照样开发很强的 Agent。

Agent 能力主要来自：

```text
模型能力
Context
Tools
State
Runtime
```

而不是必须：

```text
训练自己的模型
```

这正符合我们的路线：

```text
不做模型训练
专注 LLM Application / Agent Engineering
```

------

# 一百二十五、Agent 和 API 的关系

你以后实际开发 Agent：

```text
本质仍然是大量 API 调用
```

比如：

```text
Agent Runtime
↓
LLM API

Tool
↓
Business API

Tool
↓
Database

Tool
↓
External SaaS
```

所以 Agent 工程仍然是：

```text
后端系统工程
```

------

# 一百二十六、Agent 不是一个类

不要形成：

```java
new Agent();
```

就代表：

```text
我已经理解 Agent
```

Agent 更像：

```text
一整套运行模式
```

包括：

```text
模型调用
工具
状态
循环
错误处理
权限
上下文
```

------

# 一百二十七、Agent 也不是某个框架

```text
LangChain
≠
Agent

LangGraph
≠
Agent

Spring AI
≠
Agent

OpenAI Agents SDK
≠
Agent
```

这些是：

```text
实现 Agent 的工具 / 框架
```

Agent 本身是更高层的：

```text
系统概念
```

------

# 一百二十八、以后学任何 Agent Framework 都问这几个问题

看到框架以后不要先背 API。

先找：

```text
LLM 在哪里？

Tools 在哪里？

State 在哪里？

Loop 在哪里？

Context 在哪里构造？

Tool 是谁执行？

Tool Result 怎么回模型？

什么时候停止？
```

如果这几个问题能回答：

框架就没有那么神秘。

------

# 一百二十九、一个优秀 Agent 最重要的不是“自主程度最高”

有些宣传喜欢强调：

```text
全自动
自主
无人干预
```

但真实生产系统真正关心：

```text
可靠
可控
可观察
可审计
安全
成本可接受
```

所以：

```text
自主越多
```

并不天然：

```text
越好
```

------

# 一百三十、有时候越少让模型决定越好

例如：

```text
步骤 A 必须发生
↓
步骤 B 必须发生
↓
步骤 C 必须发生
```

那就写：

```text
A → B → C
```

没必要每一步问 LLM：

```text
下一步该干嘛？
```

这就是为什么下一节一定要学习：

```text
Workflow vs Agent
```

------

# 一百三十一、最终最核心的一张图

```text
                         User
                           │
                           ▼
                         Goal
                           │
                           ▼
                  ┌─────────────────┐
                  │  Agent Runtime  │
                  └────────┬────────┘
                           │
                           ▼
                       Context
                           │
                           ▼
                      ┌────────┐
                      │  LLM   │
                      └────┬───┘
                           │
                        Decision
                           │
             ┌─────────────┴─────────────┐
             │                           │
             ▼                           ▼
        Final Answer                 Tool Call
                                         │
                                         ▼
                                  Tool Executor
                                         │
                                         ▼
                                    Environment
                                         │
                                         ▼
                                   Observation
                                         │
                                         ▼
                                  Update State
                                         │
                                         └──────────┐
                                                    │
                                                    ▼
                                                   LLM
```

这个循环一直持续到：

```text
Finish
```

------

# 一百三十二、这一篇最核心的 15 句话

### 1

> **LLM 不等于 Agent。**

### 2

> **Agent 是围绕 LLM 构建出来的可执行系统。**

### 3

> **当前阶段可以用 Agent = LLM + Tools + State + Loop 建立心智模型。**

### 4

> **LLM 主要负责理解、推理和决定下一步。**

### 5

> **Tool 让 Agent 能够和外部世界交互。**

### 6

> **模型请求 Tool，不等于 Tool 已经执行。**

### 7

> **真正执行 Java 方法、API、数据库操作的是 Application / Agent Runtime。**

### 8

> **Observation 是 Action 执行后得到的结果。**

### 9

> **State 记录任务当前进行到哪里。**

### 10

> **Loop 让 Agent 可以根据 Observation 不断重新决策。**

### 11

> **Agent 的自主性应该是受控自主，而不是无限权限。**

### 12

> **模型提出行动，不代表程序必须执行。**

### 13

> **确定性逻辑应该尽量留在传统程序中。**

### 14

> **Agent 更适合路径动态、需要根据环境反馈不断调整的任务。**

### 15

> **Agent Engineer 的工作远不只是写 Prompt。**

------

# 一百三十三、自测

## 基础

### Q1

LLM 和 Agent 有什么区别？

------

### Q2

为什么：

```text
LLM + Prompt
```

通常不能直接理解成完整 Agent？

------

### Q3

目前我们使用的 Agent 第一版公式是什么？

------

### Q4

公式中的：

```text
LLM
Tools
State
Loop
```

分别解决什么问题？

------

# 一百三十四、Tool 自测

### Q5

用户说：

```text
把 Campaign 123 暂停。
```

模型输出：

```json
{
  "name": "pauseCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

Campaign 是否已经被暂停？

为什么？

------

### Q6

真正执行：

```java
pauseCampaign(123L);
```

的是谁？

------

### Q7

为什么说：

```text
模型决定调用 Tool
```

和：

```text
程序真正执行 Tool
```

是两个不同阶段？

------

# 一百三十五、Loop 自测

### Q8

为什么 Tool Calling 本身还不等于完整 Agent Loop？

------

### Q9

Agent 为什么需要：

```text
Observation
```

？

------

### Q10

如果 Agent 查询 Campaign 得到：

```text
ACTIVE
```

为什么它可能还需要再次调用 LLM？

------

### Q11

为什么 Agent 必须设置：

```text
Max Steps
```

之类的限制？

------

# 一百三十六、State 自测

### Q12

State 是什么？

------

### Q13

State 和 Memory 有什么区别？

------

### Q14

State 和 Context 有什么区别？

------

### Q15

为什么不能简单把：

```text
所有 State
```

全部永久放进 Context？

------

# 一百三十七、理解题

### Q16

为什么 Agent 比普通 Chat 风险更高？

------

### Q17

为什么 Agent 的 Tool 权限应该受限制？

------

### Q18

为什么：

```text
模型想执行
```

不等于：

```text
系统一定允许执行
```

？

------

### Q19

为什么：

```text
权限校验
参数校验
Retry
最大步数
```

这些东西通常应该由程序控制，而不是全部交给模型？

------

# 一百三十八、Agent 判断题

判断下面哪些更接近 Agent。

## 场景 A

```text
用户输入一段文章
↓
LLM 总结
↓
结束
```

更接近：

```text
普通 LLM Application
```

------

## 场景 B

```text
用户问天气
↓
代码固定调用天气 API
↓
LLM 帮忙润色结果
```

更接近：

```text
固定 Workflow + LLM
```

因为模型没有决定：

```text
是否调用天气 Tool
```

------

## 场景 C

```text
用户：
帮我分析 Campaign 123 无消耗原因。

LLM：
决定查 Campaign

Tool：
返回 ACTIVE

LLM：
决定查 Metrics

Tool：
返回 0 Impression

LLM：
决定查 Account

Tool：
返回 SUSPENDED

LLM：
给出最终分析
```

这已经具有非常明显的：

```text
Agent
```

特征。

------

# 一百三十九、实际场景题

用户：

```text
帮我检查 Campaign 123 有没有问题，
如果有问题就告诉我原因。
```

系统提供：

```text
getCampaign
getMetrics
getAccount
getCreative
```

一个合理 Agent 可能：

```text
Goal
↓
LLM
↓
getCampaign
↓
ACTIVE
↓
LLM
↓
getMetrics
↓
CTR 异常
↓
LLM
↓
getCreative
↓
素材审核失败
↓
LLM
↓
Final Answer
```

这里分别找出：

```text
LLM
Tool
Action
Observation
State
Loop
```

如果你可以清楚指出每一部分，说明这一 Node 基本已经掌握。

------

# 一百四十、Java 思维自测

如果未来让你不用 Agent Framework，自己设计最简 Agent：

你脑子里至少应该出现：

```text
ModelClient

ToolRegistry

ToolExecutor

AgentState

AgentLoop
```

以及类似：

```java
while (!finished) {

    response = model.call(context);

    if (response.isFinal()) {
        return response;
    }

    toolCall = response.getToolCall();

    result = toolExecutor.execute(toolCall);

    state.add(result);
}
```

不需要现在真写。

但应该：

> **能看懂为什么以后代码一定会慢慢长成这样。**

------

# 一百四十一、Node 006 完成标准

如果现在不用翻笔记，你可以解释：

```text
什么是 Agent？

LLM 和 Agent 有什么区别？

为什么普通 Chat 不等于 Agent？

什么是 Tool？

模型是否真的直接执行 Tool？

什么是 Action？

什么是 Observation？

什么是 State？

什么是 Agent Loop？

为什么 Agent 需要 Loop？

为什么需要 Max Steps？

为什么 Agent 的自主性应该受限？

为什么 Agent 比普通 Chat 风险更高？

为什么不是所有业务都应该做成 Agent？

Agent Runtime 大概负责什么？
```

并且能够完整解释：

```text
User
↓
Goal
↓
LLM
↓
Decision
↓
Tool Call
↓
Application 执行 Tool
↓
Observation
↓
State 更新
↓
LLM 再次决策
↓
...
↓
Final Answer
```

那么：

```text
Node 006：什么是 Agent

✅ PASS
```

------

# 一百四十二、这一节暂时不要做什么

当前不要：

```text
直接安装 LangChain4j

直接安装 Spring AI

直接让 Codex 写完整 Agent

直接做 Tool Calling Demo

直接设计复杂 Agent Runtime
```

原因很简单：

我们现在只完成了：

```text
认知层
```

真正代码路线还没到。

后面会一步一步经历：

```text
模型 API
↓
Message
↓
Prompt
↓
Structured Output
↓
Tool Calling
↓
Tool Dispatcher
↓
第一次完整 Tool Call
↓
Agent Loop
```

到时候每一层都亲手拆开。

------

# 一百四十三、下一篇

下一篇：

> **007 - Workflow 与 Agent 到底有什么区别？**

会重点解决一个特别重要的问题：

```text
既然 Agent 能自己决定下一步，

是不是以后所有业务都做成 Agent 就好了？
```

答案当然不是。

下一篇我们会正式比较：

```text
Workflow

A → B → C
```

和：

```text
Agent

       ┌→ Tool A
LLM ───┼→ Tool B
       ├→ Tool C
       └→ Finish
```

重点理解：

```text
确定性
vs
动态决策

固定路径
vs
模型决定路径

可靠性
vs
灵活性

什么时候该用 Workflow？

什么时候该用 Agent？

为什么生产系统经常是 Workflow + Agent 混合？
```

学完 007 以后：

```text
Phase 0：LLM 与 Agent 基础认知
```

就会正式结束。

到那时候，我们就从：

```text
“AI Agent 是什么？”
```

进入真正的：

```text
“开始开发 LLM 应用。”
```

也就是：

> **Phase 1：LLM 应用开发基础。**