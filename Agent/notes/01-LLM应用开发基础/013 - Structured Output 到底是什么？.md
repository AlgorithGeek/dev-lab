# 013 - Structured Output 到底是什么？

> Node 013：Structured Output\
> Phase 1：LLM 应用开发基础

> **阅读记录（2026-11-03）：**用户确认本篇已读完，Node 013 理论阅读收尾。按本篇安排不单独创建 Java Demo、Codex 实验记录或真实 API 实验；未另外进行逐题口述测验。下一步为 Node 014「JSON Schema」，其理论篇也已读完，基础手写练习结果待确认。

---

## 一、先说结论

Structured Output，中文通常可以理解为：

```text
结构化输出
```

它解决的核心问题不是：

```text
“怎么让模型说得更好听？”
```

而是：

```text
“怎么让模型输出的数据，更适合程序直接读取、校验和处理？”
```

以前我们调用模型，拿到的通常是：

```text
String
```

例如：

```text
这条广告存在较高风险，主要原因是预算过高，建议降低出价。
```

人看起来没有问题。

但如果 Java 程序接下来需要分别拿到：

```text
riskLevel
reason
suggestion
```

那就麻烦了。

因为这段文本没有明确的数据结构。

Structured Output 要解决的，就是：

```text
LLM
↓
不再只输出“给人看的文本”
↓
而是输出“给程序消费的数据”
```

---

# 二、先从普通自然语言输出开始

假设我们让模型分析一条广告：

```text
请分析这条广告的风险。
```

模型可能回答：

```text
这条广告风险较高。

主要原因是预算设置过大，并且当前转化率偏低。

建议降低预算，并继续观察后续转化表现。
```

对于人类来说：

```text
很好理解
```

但是对于程序来说，它只是：

```java
String content
```

程序看到的本质仍然是：

```text
一大段字符
```

它不知道：

```text
哪一部分是风险等级？

哪一部分是原因？

哪一部分是建议？
```

所以如果程序必须继续处理这些信息，就可能需要自己：

```text
切字符串
正则表达式
关键词匹配
自己猜格式
```

这些方案都非常脆弱。

---

# 三、为什么自然语言不适合程序直接消费？

因为自然语言本身具有很强的自由度。

同一个意思，模型可能输出：

```text
风险较高。
```

也可能：

```text
该广告属于高风险。
```

还可能：

```text
综合判断：HIGH。
```

甚至：

```text
我认为目前风险等级应判定为较高，建议谨慎处理。
```

人都能看懂。

但是程序如果写：

```java
if (content.contains("风险较高")) {
    ...
}
```

那么模型换一种表达：

```text
高风险
```

代码可能就失效了。

所以：

> **自然语言非常适合人与模型交流，但不适合作为程序之间稳定的数据协议。**

---

# 四、程序更喜欢什么？

程序更喜欢：

```text
固定结构
固定字段
固定类型
明确约束
```

例如：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高，当前转化率偏低",
  "suggestion": "降低预算并继续观察"
}
```

这样 Java 程序就可以明确知道：

```text
riskLevel
=
风险等级

reason
=
原因

suggestion
=
建议
```

这就是结构化数据的基本思想。

---

# 五、什么叫“结构化”？

最简单地理解：

```text
信息按照预先约定的结构组织
```

例如：

```json
{
  "name": "张三",
  "age": 18
}
```

这里已经不是一段自由文本。

它有明确字段：

```text
name
age
```

还有明确值：

```text
张三
18
```

因此程序就可以执行：

```java
user.getName();
user.getAge();
```

而不需要从：

```text
张三今年18岁。
```

这种句子里自己解析。

---

# 六、自然语言和结构化输出的区别

自然语言：

```text
张三今年18岁。
```

结构化数据：

```json
{
  "name": "张三",
  "age": 18
}
```

它们表达的业务信息非常接近。

区别在于：

```text
自然语言
主要给人理解

结构化数据
主要给程序理解
```

---

# 七、Structured Output 到底是什么？

现在可以正式定义：

> **Structured Output 是让模型按照预定义的数据结构输出结果，使程序能够稳定地解析、校验和继续处理模型返回的数据。**

例如原来：

```text
User
↓
LLM
↓
自然语言 String
```

后来：

```text
User
↓
LLM
↓
结构化数据
↓
程序解析
↓
业务逻辑
```

例如：

```text
广告数据
↓
LLM
↓
{
  riskLevel,
  reason,
  suggestion
}
↓
Java
↓
业务判断
```

---

# 八、Structured Output 为什么在 LLM 应用里这么重要？

因为普通聊天应用可以接受：

```text
模型自由回答
```

但企业应用通常需要：

```text
模型结果继续进入程序流程
```

例如：

```text
文本分类
风险分析
工单分类
内容审核
信息抽取
实体识别
广告分析
订单分析
智能路由
Agent Tool 参数生成
```

这些场景都不是：

```text
模型说完就结束
```

而是：

```text
模型输出
↓
程序继续使用
```

所以 Structured Output 是 LLM 从：

```text
聊天工具
```

走向：

```text
软件系统组件
```

非常关键的一步。

---

# 九、举一个 Java 后端更真实的例子

假设我们以后有一个接口：

```text
POST /ai/analyze
```

输入：

```json
{
  "text": "用户反馈登录按钮点击后没有任何反应"
}
```

模型需要判断：

```text
问题类型
严重程度
原因
建议
```

如果模型返回：

```text
这看起来像一个 BUG，而且可能影响用户登录，
严重程度建议设为 HIGH。
建议优先检查前端事件绑定和后端登录接口。
```

人能理解。

但是后端想直接：

```java
if (result.getSeverity() == HIGH) {
    alert();
}
```

就做不到。

如果 Structured Output 返回：

```json
{
  "category": "BUG",
  "severity": "HIGH",
  "reason": "登录按钮点击后没有响应",
  "suggestion": "检查前端事件绑定和登录接口"
}
```

Java 就能继续处理：

```text
category
severity
reason
suggestion
```

---

# 十、JSON 是 Structured Output 吗？

这个问题需要特别注意。

答案是：

```text
JSON 经常被用来承载 Structured Output，
但“输出 JSON”不一定等于“真正可靠的 Structured Output”。
```

这是 Node 013 最重要的认知之一。

例如我们在 Prompt 里写：

```text
请只输出 JSON。
```

模型可能真的输出：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

看起来很好。

但这还不代表：

```text
程序可以完全相信它
```

---

# 十一、为什么“请输出 JSON”还不够？

假设我们要求：

```text
请输出：

{
  "riskLevel": "...",
  "reason": "..."
}
```

模型第一次可能输出：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

第二次可能输出：

```json
{
  "level": "HIGH",
  "reason": "预算过高"
}
```

字段名变了：

```text
riskLevel
↓
level
```

第三次可能：

```json
{
  "riskLevel": "高",
  "reason": "预算过高"
}
```

原来程序希望：

```text
HIGH
MEDIUM
LOW
```

模型却输出：

```text
高
```

第四次：

```json
{
  "riskLevel": "HIGH"
}
```

`reason` 消失了。

第五次甚至：

```text
下面是 JSON：

{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

如果程序期待整个字符串就是 JSON：

```text
JSON.parse(...)
```

前面的：

```text
下面是 JSON：
```

就可能导致解析失败。

---

# 十二、所以有三个层次必须区分

这是后面 013～015 整组知识的主线。

```text
自然语言输出
↓
JSON Object
↓
JSON Schema
↓
严格结构约束
↓
Java DTO
```

千万不要把这些全部认为是：

```text
“模型输出 JSON”
```

它们其实不是一个层级。

---

# 十三、第一个层级：自然语言输出

例如：

```text
风险等级为 HIGH，原因是预算过高。
```

特点：

```text
自由
灵活
人容易阅读
程序难稳定解析
```

---

# 十四、第二个层级：JSON 输出

例如：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

它已经比自然语言更适合程序处理。

因为：

```text
字段明确
结构明确
JSON 库可以解析
```

但是仍然存在问题：

```text
字段是否一定存在？
字段名称是否一定正确？
字段类型是否一定正确？
值是否符合约束？
会不会多字段？
会不会少字段？
```

所以：

```text
JSON
≠
完整的数据契约
```

---

# 十五、第三个层级：JSON Schema

JSON Schema 可以进一步描述：

```text
这个 JSON 应该长什么样
```

例如概念上可以要求：

```text
riskLevel
必须存在

riskLevel
只能是
HIGH / MEDIUM / LOW

reason
必须是字符串
```

这样程序和模型之间开始拥有：

```text
明确的数据契约
```

JSON Schema 会放到：

```text
Node 014
```

详细学习。

Node 013 现在只需要先知道：

> **JSON Schema 不只是一个 JSON 示例，而是对 JSON 结构本身的定义和约束。**

---

# 十六、第四个层级：严格结构约束

即使我们拥有 Schema，还要继续考虑：

```text
模型是否真的必须遵守？
```

不同模型、Provider、API：

```text
支持方式可能不同
```

可能存在：

```text
JSON Mode
JSON Schema
Structured Output
strict
```

等不同能力和参数。

这些名称不要现在死记。

真正要理解的是：

```text
约束程度可能不同
```

例如：

```text
只保证输出是 JSON
```

和：

```text
保证输出满足指定 Schema
```

完全不是同一个保证。

---

# 十七、JSON Mode 和 Schema 约束不要混淆

概念上可以这样理解。

JSON Mode：

```text
重点保证
“输出是合法 JSON”
```

但可能不会严格保证：

```text
字段一定叫什么
字段一定存在
enum 一定正确
```

Schema Structured Output：

```text
除了要求 JSON
还要求符合预先定义的结构
```

例如：

```text
必须存在 riskLevel
必须存在 reason
riskLevel 只能是 HIGH / MEDIUM / LOW
```

因此：

```text
合法 JSON
≠
符合业务 Schema
```

---

# 十八、举个最简单的例子

下面两个 JSON 都是合法 JSON。

第一个：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

第二个：

```json
{
  "hello": 123
}
```

从 JSON 语法来说：

```text
两个都合法
```

但是如果业务需要：

```text
riskLevel
reason
```

第二个显然完全不能使用。

所以我们真正关心的不只是：

```text
是不是 JSON
```

还要关心：

```text
是不是我需要的那个 JSON
```

---

# 十九、这就是“数据契约”

Structured Output 最重要的工程概念之一就是：

```text
Data Contract
数据契约
```

程序提前定义：

```text
我需要什么字段
字段是什么类型
哪些字段必须有
哪些值是允许的
```

模型应该按照这个契约返回。

例如：

```text
Java 程序
          契约
             ↓
       riskLevel
       reason
       suggestion
             ↓
            LLM
             ↓
       Structured Output
```

这就比：

```text
“你自由发挥一下”
```

稳定得多。

---

# 二十、为什么企业程序特别需要数据契约？

传统后端开发本身就在大量依赖：

```text
接口契约
数据库 Schema
Java 类型
DTO
枚举
字段校验
```

例如一个正常 API：

```json
{
  "userId": 1001,
  "status": "ACTIVE"
}
```

后端不会希望有时候返回：

```json
{
  "id": "一千零一",
  "userState": "用户是活跃的"
}
```

因为：

```text
程序之间需要稳定契约
```

LLM 接入软件系统以后，同样要遵守这个思想。

---

# 二十一、为什么 LLM 让这个问题变得更明显？

因为传统代码：

```text
确定性很强
```

你写：

```java
return new UserDTO(id, name);
```

正常情况下返回结构就是固定的。

但是 LLM 本质上是：

```text
生成式系统
```

模型会生成：

```text
它认为合理的内容
```

它天然拥有自由度。

所以 LLM 工程的一个重要任务就是：

```text
把模型的不确定性
↓
限制在程序可以管理的范围内
```

Structured Output 就是其中一个核心手段。

---

# 二十二、Structured Output 的本质不是为了“看起来整齐”

这是一个常见误区。

有人会觉得：

```text
JSON 看起来更整齐
```

所以才使用 Structured Output。

不对。

真正原因是：

```text
Machine-readable
机器可读
```

以及：

```text
Machine-processable
机器可继续处理
```

最终目标不是：

```text
让人看 JSON
```

而是：

```text
让程序不用猜
```

---

# 二十三、Structured Output 和 Prompt 是什么关系？

Prompt 仍然很重要。

例如：

```text
请分析下面广告的风险。
```

决定：

```text
模型要做什么任务
```

Structured Output 则决定：

```text
模型结果以什么结构返回
```

可以简单理解：

```text
Prompt
=
任务语义

Structured Output
=
结果结构
```

两者不是互相替代。

---

# 二十四、Structured Output 和 Output Format 是什么关系？

Node 011 我们已经学过 Prompt 的：

```text
Output Format
```

例如：

```text
请按照以下格式回答：

风险等级：
原因：
建议：
```

这属于：

```text
Prompt 层面的格式要求
```

它可以引导模型。

但是：

```text
Prompt 说“请这样输出”
```

和：

```text
API / Schema 层面真正约束输出结构
```

不是完全相同的可靠性。

所以：

```text
Prompt Output Format
=
行为指导

Structured Output
=
程序级结构约束
```

两者经常一起使用。

---

# 二十五、Structured Output 和 Few-shot 是什么关系？

Few-shot 可以给模型示例：

```text
Input:
...

Output:
{
  "riskLevel": "HIGH"
}
```

这会让模型更容易模仿这种格式。

但是：

```text
Few-shot
=
示范
```

Structured Output：

```text
=
结构约束
```

所以：

> **Few-shot 可以帮助模型更像 JSON，但不能替代真正的 Structured Output。**

---

# 二十六、Structured Output 和 Fine-tuning 没什么直接关系

Structured Output 通常发生在：

```text
Inference
推理阶段
```

也就是说：

```text
模型参数没有因为这一次请求而变化
```

我们只是告诉模型：

```text
本次请求的输出应该满足某种结构
```

这仍然属于：

```text
LLM Application
```

不是训练模型。

---

# 二十七、Structured Output 和 RAG 的区别

Structured Output：

```text
解决“怎么输出”
```

RAG：

```text
解决“模型回答时应该看到什么知识”
```

例如：

```text
RAG
↓
找到广告平台规则
↓
提供给模型
↓
模型分析
↓
Structured Output
↓
返回风险等级、原因、建议
```

两个可以同时使用。

---

# 二十八、Structured Output 和 Tool Calling 的关系

以后学习 Tool Calling 时，会再次遇到：

```text
Schema
```

例如模型决定调用：

```text
getWeather
```

并生成参数：

```json
{
  "city": "北京"
}
```

Tool 参数必须符合：

```text
工具参数定义
```

这本质上也依赖：

```text
结构化数据
+
Schema
```

所以 Structured Output 是 Tool Calling 的重要前置基础。

---

# 二十九、Structured Output 和 Agent 有什么关系？

Agent 不只是：

```text
模型聊天
```

Agent 需要：

```text
模型
↓
做判断
↓
产生结构化决策
↓
程序执行
↓
继续下一轮
```

例如：

```json
{
  "action": "SEARCH",
  "query": "Java virtual thread"
}
```

程序看到：

```text
action = SEARCH
```

然后执行搜索。

如果模型只是说：

```text
我觉得现在可以搜索一下 Java virtual thread。
```

程序还要猜：

```text
它是不是想调用搜索？
```

所以 Agent 越工程化：

```text
越需要结构化输出
```

---

# 三十、Structured Output 常见应用场景

## 1. 分类

```json
{
  "category": "BUG"
}
```

---

## 2. 情感分析

```json
{
  "sentiment": "NEGATIVE",
  "score": 0.91
}
```

---

## 3. 信息抽取

输入：

```text
张三今年18岁，在北京工作。
```

输出：

```json
{
  "name": "张三",
  "age": 18,
  "city": "北京"
}
```

---

## 4. 风险分析

```json
{
  "riskLevel": "HIGH",
  "reason": "预算异常"
}
```

---

## 5. 工单分类

```json
{
  "type": "BUG",
  "priority": "P1"
}
```

---

## 6. 路由

```json
{
  "route": "TECH_SUPPORT"
}
```

---

## 7. Agent 决策

```json
{
  "action": "CALL_TOOL",
  "tool": "searchOrder"
}
```

---

# 三十一、为什么信息抽取特别适合 Structured Output？

假设输入：

```text
订单号 A20261103001，
用户张三，
退款金额 199 元，
原因是商品破损。
```

如果要求模型：

```text
帮我总结
```

它可能输出：

```text
张三申请对订单 A20261103001 退款 199 元，
原因是商品破损。
```

但是业务程序真正需要的是：

```json
{
  "orderId": "A20261103001",
  "userName": "张三",
  "refundAmount": 199,
  "reason": "商品破损"
}
```

这样程序才能：

```text
写数据库
调用退款接口
做风控
记录日志
```

---

# 三十二、Structured Output 并不能让模型变成数据库

这一点也要注意。

Structured Output 只解决：

```text
输出结构
```

不能保证：

```text
内容一定真实
```

例如模型可能输出一个结构完美的 JSON：

```json
{
  "orderId": "A123",
  "refundAmount": 199
}
```

但：

```text
199 可能是模型猜的
```

所以：

```text
结构正确
≠
事实正确
```

这是非常重要的区别。

---

# 三十三、Structured Output 也不能消灭幻觉

我们已经学过幻觉。

Structured Output 可以做到：

```text
让幻觉“有结构”
```

但不能自动做到：

```text
消灭幻觉
```

比如：

```json
{
  "userId": 10086,
  "balance": 999999
}
```

JSON 完全合法。

Schema 也可能完全符合。

但如果模型没有真实数据库信息：

```text
balance
可能完全是编的
```

因此：

```text
结构可靠
和
事实可靠
```

是两个问题。

---

# 三十四、程序为什么仍然要 Validation？

即使使用 Structured Output，后端仍然不能完全放弃：

```text
Validation
```

因为程序需要检查：

```text
业务规则
```

例如 Schema 允许：

```text
age
=
integer
```

模型返回：

```json
{
  "age": -300
}
```

JSON 合法。

类型也是 integer。

但是业务上：

```text
明显不合理
```

所以后端仍然需要：

```text
结构校验
+
业务校验
```

---

# 三十五、可以把校验分成几层

以后可以形成这样的链：

```text
LLM Output
↓
JSON Syntax
↓
Schema Validation
↓
Java Deserialization
↓
Business Validation
↓
Business Logic
```

每一层解决的问题不同。

---

# 三十六、第一层：JSON Syntax

检查：

```text
它是不是合法 JSON
```

例如：

```json
{
  "name": "张三"
}
```

合法。

下面：

```text
{
  name: 张三
}
```

可能就不是标准 JSON。

---

# 三十七、第二层：Schema Validation

检查：

```text
结构是不是我们需要的
```

例如：

```text
name 必须存在
age 必须是 integer
```

---

# 三十八、第三层：Java Deserialization

例如：

```json
{
  "name": "张三",
  "age": 18
}
```

转成：

```java
class UserInfo {

    private String name;

    private Integer age;
}
```

这部分放到：

```text
Node 015
```

详细学习。

---

# 三十九、第四层：Business Validation

例如：

```text
age >= 0
age <= 150
```

或者：

```text
riskLevel = HIGH
时
reason 不能为空
```

这已经不只是 JSON 结构问题。

而是：

```text
业务规则
```

---

# 四十、第五层：Business Logic

验证完成后：

```java
if ("HIGH".equals(result.getRiskLevel())) {
    ...
}
```

模型结果才真正进入业务系统。

---

# 四十一、最终我们要形成的链路

这是 Node 013～015 最重要的一张图。

```text
Prompt / Input
      │
      ▼
     LLM
      │
      ▼
Structured Output
      │
      ▼
JSON
      │
      ▼
Schema
      │
      ▼
Validation
      │
      ▼
Java DTO
      │
      ▼
Business Logic
```

---

# 四十二、为什么 DTO 是最终非常关键的一步？

因为 Java 是：

```text
强类型语言
```

我们真正想要的不是：

```java
String content;
```

甚至也不只是：

```java
JSONObject json;
```

更理想的是：

```java
CampaignAnalysis analysis;
```

然后：

```java
analysis.getRiskLevel();
analysis.getReason();
analysis.getSuggestion();
```

这时候：

```text
LLM 的不确定文本
```

开始进入：

```text
Java 的类型系统
```

这是 Structured Output 在 Java 工程里的真正价值。

---

# 四十三、但 Node 013 暂时不要急着写 DTO

我们这组 Node 已经故意拆开。

```text
Node 013
Structured Output

Node 014
JSON Schema

Node 015
Java DTO + Structured Output
```

Node 013 只需要先把：

```text
为什么需要结构化输出
```

真正理解。

否则直接写代码，很容易变成：

```text
记一个 API 参数
复制一个 JSON
然后不知道为什么
```

这不是我们的学习目标。

---

# 四十四、为什么不能现在直接上 Spring AI？

因为目前我们的路线是：

```text
先理解底层
↓
再使用框架
```

Spring AI 以后可能直接帮我们做：

```text
LLM
↓
POJO
```

非常方便。

但是如果现在直接用框架：

```text
你可能只看到一个方法调用
```

却不知道底层经历了：

```text
Schema
Model Output
Validation
Parsing
DTO
```

所以当前继续使用：

```text
HttpClient
OpenAI-compatible API
FastJSON2
```

更有学习价值。

---

# 四十五、Structured Output 会不会增加 Token？

通常会增加一定输入内容。

因为请求里可能需要携带：

```text
Schema
字段描述
约束
```

这些都属于：

```text
Context
```

因此可能增加：

```text
prompt_tokens
```

但是换来的通常是：

```text
程序稳定性
```

在很多企业场景里，这是值得的。

---

# 四十六、Schema 是不是越复杂越好？

不是。

如果 Schema 非常复杂：

```text
字段很多
嵌套很深
约束很多
```

模型处理难度和维护成本都可能增加。

所以 Schema 设计也应该：

```text
够用
清晰
稳定
```

而不是：

```text
为了显得专业无限复杂
```

---

# 四十七、Structured Output 设计时应该优先考虑什么？

几个基本原则：

```text
字段少而明确
命名稳定
类型清楚
枚举有限
避免歧义
避免重复字段
```

例如：

```json
{
  "riskLevel": "HIGH"
}
```

通常比：

```json
{
  "risk": "这个广告我认为风险比较高"
}
```

更适合程序。

---

# 四十八、为什么 enum 很重要？

例如风险等级如果允许模型自由输出：

```text
高
很高
高风险
HIGH
High
严重
危险
```

程序会非常难处理。

如果定义：

```text
HIGH
MEDIUM
LOW
```

程序就只需要处理：

```text
三个确定值
```

所以：

```text
减少模型自由度
=
提高程序确定性
```

这就是 Structured Output 的核心思想之一。

---

# 四十九、为什么字段名称也应该稳定？

如果业务里同一个概念：

一会儿：

```text
risk
```

一会儿：

```text
riskLevel
```

一会儿：

```text
level
```

那么：

```text
DTO
数据库
前端
日志
测试
```

都会更加混乱。

所以结构化输出不是只约束模型。

它也会迫使我们：

```text
先把业务数据模型想清楚
```

---

# 五十、Structured Output 其实也在逼程序员定义问题

这是一个很有价值的点。

如果你只问：

```text
帮我分析一下
```

你可以一直模糊。

但是一旦要 Structured Output：

```text
你必须决定
```

到底要：

```text
riskLevel?
reason?
suggestion?
confidence?
```

所以 Structured Output 也会促使开发者思考：

```text
我的程序真正需要什么信息？
```

---

# 五十一、Structured Output 是否一定要 JSON？

不一定。

结构化数据理论上还可以是：

```text
XML
CSV
Protobuf
其他格式
```

但在 LLM API 生态里：

```text
JSON
```

非常常见。

原因包括：

```text
通用
可读
容易生成
容易解析
大量 API 使用
Schema 生态成熟
```

所以当前路线重点学习：

```text
JSON + JSON Schema
```

---

# 五十二、Structured Output 和数据库 Schema 类似吗？

思想上有一点类似。

数据库 Schema 定义：

```text
表有哪些字段
字段是什么类型
字段是否允许为空
```

JSON Schema 也在描述：

```text
数据有哪些字段
字段是什么类型
哪些字段必须存在
```

但它们不是同一个技术。

只能理解为：

```text
都有“数据结构契约”的思想
```

---

# 五十三、Structured Output 和 Java class 类似吗？

也有对应关系。

例如：

JSON：

```json
{
  "name": "张三",
  "age": 18
}
```

Java：

```java
class User {

    private String name;

    private Integer age;
}
```

可以看到：

```text
JSON field
↔
Java field
```

以及：

```text
JSON string
↔
Java String

JSON integer
↔
Java Integer / Long
```

Node 015 会把这条线真正接起来。

---

# 五十四、一个完整的未来案例

以后我们可能写：

```java
class CampaignAnalysis {

    private Long campaignId;

    private String riskLevel;

    private String reason;

    private String suggestion;
}
```

然后模型输入：

```text
Campaign ID: 10001
Budget: 5000
Conversion Rate: 0.3%
```

模型结构化输出：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "预算较高且转化率偏低",
  "suggestion": "降低预算并观察转化表现"
}
```

程序反序列化：

```java
CampaignAnalysis result = ...
```

之后：

```java
if ("HIGH".equals(result.getRiskLevel())) {
    ...
}
```

这就是我们的最终目标。

---

# 五十五、为什么这是 Agent 学习路线里的关键节点？

因为到目前为止，我们已经学过：

```text
LLM
API
Message
System Prompt
Prompt
Few-shot
```

这些主要解决：

```text
怎么和模型交流
```

Structured Output 开始解决：

```text
怎么让模型进入程序系统
```

因此这是路线里的一个明显转折点。

---

# 五十六、把目前知识串起来

## Node 008：API

学会：

```text
怎么调用模型
```

---

## Node 009：Message

学会：

```text
怎么组织对话消息
```

---

## Node 010：System Prompt

学会：

```text
怎么定义模型长期行为规则
```

---

## Node 011：Prompt

学会：

```text
怎么清晰描述任务
```

---

## Node 012：Few-shot

学会：

```text
怎么用示例展示任务模式
```

---

## Node 013：Structured Output

开始学习：

```text
怎么让模型结果进入程序
```

这就是现在的位置。

---

# 五十七、Structured Output 最大的价值可以怎样理解？

可以记住一句话：

> **把模型输出从“人能看懂”升级成“程序能可靠消费”。**

---

# 五十八、但“可靠”不是绝对可靠

这里也不要走到另一个极端。

即使使用 Structured Output，也仍然需要考虑：

```text
API 错误
模型拒答
Schema 不支持
输出截断
业务内容错误
模型幻觉
网络异常
Provider 差异
```

所以 Structured Output 是：

```text
提高确定性
```

而不是：

```text
让 LLM 变成 100% 确定性程序
```

---

# 五十九、为什么以后还要学异常处理？

因为真实调用链：

```text
Java
↓
HTTP
↓
Provider
↓
Model
↓
Structured Output
↓
Java
```

任何一层都可能失败。

所以 Node 020 还会专门学习：

```text
LLM API 异常
Retry
Timeout
Error Handling
```

Structured Output 只负责其中：

```text
输出结构
```

这一部分。

---

# 六十、常见误区

## 误区 1

```text
只要模型输出 JSON
就是 Structured Output
```

不准确。

更准确应该问：

```text
有没有明确结构？
有没有 Schema？
有没有约束？
程序能不能稳定解析？
```

---

## 误区 2

```text
JSON 合法
就一定符合业务要求
```

错误。

合法 JSON：

```json
{
  "hello": 123
}
```

也可能完全不符合业务 Schema。

---

## 误区 3

```text
Structured Output 可以消灭幻觉
```

错误。

它主要解决：

```text
结构
```

不是：

```text
事实真实性
```

---

## 误区 4

```text
有 Schema 就不需要后端校验
```

错误。

仍然需要：

```text
Business Validation
```

---

## 误区 5

```text
Structured Output 就是 Prompt 写一句“请输出 JSON”
```

错误。

这只是：

```text
Prompt 引导
```

不等于：

```text
强结构约束
```

---

## 误区 6

```text
Structured Output 只是为了格式好看
```

错误。

真正目的：

```text
Machine-readable
Programmatic
```

---

## 误区 7

```text
用了 Structured Output
就必须完全不要自然语言
```

不一定。

可以让某个字段本身就是：

```text
自然语言解释
```

例如：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算明显高于当前转化水平"
}
```

结构化的是：

```text
外层数据结构
```

字段值仍然可以是自然语言。

---

# 六十一、Structured Output 不代表所有字段都必须特别死

例如：

```text
riskLevel
```

适合 enum：

```text
HIGH
MEDIUM
LOW
```

但是：

```text
reason
```

通常可以是：

```text
自由文本
```

所以结构化输出其实是：

```text
该固定的地方固定
该自由的地方保留自由
```

---

# 六十二、这和 Agent 的设计思想很像

一个好的 Agent 系统不是：

```text
所有东西都让模型自由决定
```

也不是：

```text
所有东西都写死
```

而是：

```text
确定的部分
交给程序

需要理解和判断的部分
交给模型
```

Structured Output 就是两者之间的接口之一。

---

# 六十三、Structured Output 可以怎样降低系统复杂度？

没有结构化输出时：

```text
LLM
↓
String
↓
正则
↓
字符串切割
↓
猜测字段
↓
各种 if
```

有结构化输出后：

```text
LLM
↓
Schema
↓
DTO
↓
Business Logic
```

代码通常会更容易维护。

---

# 六十四、为什么不能完全依赖正则？

假设模型输出：

```text
风险等级：HIGH
原因：预算过高
```

我们写：

```text
风险等级：(.*)
```

后来模型改成：

```text
风险：HIGH
```

正则就失效。

Structured Output 的意义就是尽量避免：

```text
依赖模型自然语言措辞
```

---

# 六十五、为什么这对 Java 程序员尤其自然？

因为 Java 后端本来就在使用：

```text
DTO
VO
Entity
Enum
Validation
Jackson / FastJSON
接口契约
```

Structured Output 并不是完全陌生的新思想。

它其实是在做：

```text
LLM
↓
接入我们熟悉的后端数据模型
```

所以对于 Java 后端来说，这是一个非常重要、也非常自然的切入点。

---

# 六十六、以后框架会替我们做很多事

例如未来 Spring AI 可能让代码变成：

```text
Prompt
↓
LLM
↓
Java POJO
```

看起来非常简单。

但是现在我们要知道：

```text
中间不是魔法
```

底层仍然离不开：

```text
Schema
Structured Output
Parsing
Validation
Mapping
```

理解这些以后再用框架，才知道框架到底帮我们做了什么。

---

# 六十七、Node 013 目前不需要单独做 Java 实验

这个 Node 的核心是：

```text
建立 Structured Output 的正确认知
```

暂时没有必要为了形式完整再新建一个：

```text
StructuredOutputDemo.java
```

因为真正有价值的代码实验应该等到：

```text
Node 014
JSON Schema

+
Node 015
Java DTO
```

一起完成。

这样我们可以真正跑通：

```text
Schema
↓
Model
↓
JSON
↓
Java DTO
```

而不是只为了证明：

```text
模型会输出一个 JSON 字符串
```

---

# 六十八、Node 014 下一步会学什么？

Node 014 会正式进入：

```text
JSON Schema
```

重点包括：

```text
type
properties
required
enum
array
object
```

并理解：

```text
JSON Schema
=
程序和模型之间的数据契约
```

---

# 六十九、Node 015 会学什么？

Node 015 会把前面的知识真正接入 Java。

大致链路：

```text
Java
↓
定义 Schema
↓
调用模型
↓
得到 Structured Output
↓
解析 JSON
↓
反序列化 DTO
↓
校验
↓
业务代码
```

那时候建议安排真正的 Codex 实验。

---

# 七十、真正需要记住的 15 个结论

1. Structured Output 的核心目标是让模型输出更适合程序消费。
2. 自然语言适合人类，但不适合作为稳定程序协议。
3. JSON 是常见结构化数据格式，但 JSON 不等于完整的数据契约。
4. “请输出 JSON”只是 Prompt 引导，不等于严格结构约束。
5. 合法 JSON 不代表符合业务需要。
6. JSON Schema 用于描述 JSON 应该具有怎样的结构。
7. JSON Mode 和严格 Schema 约束不能混为一谈。
8. Structured Output 的重要价值是减少程序对自然语言格式的猜测。
9. Structured Output 不会自动消灭模型幻觉。
10. 结构正确不代表事实正确。
11. 即使使用 Structured Output，程序仍然需要 Validation。
12. Structured Output 是 Tool Calling 和 Agent 工程的重要基础。
13. Java DTO 是 Structured Output 在 Java 程序中的重要落点。
14. Structured Output 的本质是把模型的不确定输出限制到程序可管理的结构中。
15. 最终目标不是“得到 JSON”，而是让模型结果安全进入业务逻辑。

---

# 七十一、理解自测

## 基础

1. Structured Output 是什么？
2. 为什么自然语言不适合程序稳定解析？
3. JSON 为什么比自然语言更适合程序？
4. 为什么“模型输出 JSON”还不能完全解决问题？
5. JSON Schema 大概解决什么问题？

---

## 概念辨析

6. JSON 和 JSON Schema 有什么区别？
7. “请只输出 JSON”和 Structured Output 有什么区别？
8. JSON Mode 和严格 Schema 约束有什么区别？
9. Structured Output 和 Few-shot 有什么区别？
10. Structured Output 和 RAG 有什么区别？
11. Structured Output 和 Tool Calling 有什么关系？

---

## 工程理解

12. 为什么后端仍然需要 Validation？
13. 为什么结构正确不代表事实正确？
14. 为什么 Structured Output 不能消灭幻觉？
15. 为什么 Java DTO 是 Structured Output 的重要落点？
16. 为什么 Agent 系统特别需要结构化数据？
17. 为什么正则解析模型自然语言不是理想方案？

---

# 七十二、口述验收

如果有人问：

> Structured Output 不就是让模型输出 JSON 吗？为什么还要专门学？

你应该能够自然解释：

> 不完全是。让模型输出一个看起来像 JSON 的字符串，只是第一步。程序真正需要的是稳定的数据契约，例如字段名称固定、字段类型明确、必填字段存在、某些字段只能取规定值。Structured Output 的目标，是把模型原本自由的自然语言结果变成程序能够稳定解析、校验并继续处理的数据。
>
> JSON 是常见的承载形式，但合法 JSON 不代表一定符合业务结构，所以后面还需要 JSON Schema。再往后，在 Java 中还要把模型输出映射成 DTO，经过校验以后才能进入业务逻辑。
>
> Structured Output 解决的是“输出结构”的问题，它不能保证模型说的事实一定是真的，也不能消灭幻觉。因此结构校验和业务校验仍然需要存在。

如果能够自然讲清楚：

```text
自然语言
↓
JSON
↓
Schema
↓
DTO
```

以及：

```text
结构正确
≠
事实正确
```

那么 Node 013 的核心认知基本已经建立。

---

# 七十三、Node 013 完成标准

读完本篇后，应该能够：

- [ ] 理解 Structured Output 是什么；
- [ ] 理解为什么程序不喜欢自由自然语言；
- [ ] 理解 JSON 为什么比自然语言更适合程序消费；
- [ ] 明白 JSON 不等于 JSON Schema；
- [ ] 明白“请输出 JSON”不等于严格 Structured Output；
- [ ] 理解 JSON Mode 与 Schema 约束不是同一层级；
- [ ] 理解数据契约的意义；
- [ ] 理解 Structured Output 和 Prompt / Few-shot 的区别；
- [ ] 理解 Structured Output 和 Tool Calling / Agent 的关系；
- [ ] 明白 Structured Output 不能消灭幻觉；
- [ ] 明白结构正确不等于事实正确；
- [ ] 理解为什么后端仍然需要 Validation；
- [ ] 理解最终为什么要进入 Java DTO。

本 Node：

```text
不要求单独 Java 实验
```

真正的结构化输出代码实践安排在：

```text
Node 014
+
Node 015
```

连续完成。

---

# 七十四、Node 013 和后续节点边界

## Node 013

重点：

```text
为什么需要 Structured Output
```

回答：

```text
模型输出为什么需要从自然语言进入结构化数据？
```

---

## Node 014

重点：

```text
JSON Schema
```

回答：

```text
程序如何明确描述自己需要什么结构？
```

---

## Node 015

重点：

```text
Java DTO
```

回答：

```text
模型结构化结果如何真正进入 Java 类型系统和业务代码？
```

三者组成：

```text
Structured Output
↓
JSON Schema
↓
Java DTO
```

---

# 七十五、停止点与下一步

本篇读完并理解以后：

```text
Node 013
即可结束
```

不需要为了 Node 013 单独创建：

```text
Java Demo
Codex Prompt
Codex Handoff
API A/B 实验
```

下一步进入：

```text
Node 014 - JSON Schema
```

从下面这个问题开始：

```text
如果程序已经知道自己想要 JSON，
那么程序怎样准确描述：

有哪些字段？
字段是什么类型？
哪些字段必须存在？
哪些值是允许的？
```

这就是：

```text
JSON Schema
```

---

# 本篇一句话总结

> **Structured Output 的本质，是把 LLM 原本面向人类的自由文本输出，逐步转换成程序可以解析、校验、反序列化并安全进入业务逻辑的数据结构。**

```text
自然语言
↓
JSON
↓
JSON Schema
↓
Validation
↓
Java DTO
↓
Business Logic
```

**Node 013：Structured Output 理论篇。**
