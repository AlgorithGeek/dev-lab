# 015-1 - Java DTO 与 Structured Output 到底是什么？

> Node 015：Java DTO 与 Structured Output\
> Phase 1：LLM 应用开发基础

> **阅读与收尾记录（2026-11-03）：**用户已读完本篇，并在 015-2 完成 Schema 请求、真实模型 JSON、FastJSON2 → CampaignAnalysis 及 getter 的正常链路；另执行了字段缺失与非法风险等级两个本地样本。Node 015 按用户确认收尾，下一步 Node 016「Streaming（准备学习）」。未另行逐题口述，不将未执行的错误样本或完整 HTTPS 脚本记为通过。

---

# 一、先说结论

前面我们已经完成：

```text
Node 013：Structured Output
↓
Node 014：JSON Schema
```

现在 Node 015 要继续解决：

> **模型按照 Schema 输出结构化 JSON 以后，Java 程序到底怎么真正使用它？**

对于 Java 后端来说，Structured Output 的终点不是：

```text
拿到一段 JSON 字符串
```

而是：

```text
把这段结构化结果
转换成明确的 Java 类型
```

例如模型返回：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "预算较高且转化率偏低",
  "suggestion": "降低预算并继续观察"
}
```

我们真正希望 Java 中得到：

```java
CampaignAnalysis analysis;
```

然后：

```java
analysis.getCampaignId();
analysis.getRiskLevel();
analysis.getReason();
analysis.getSuggestion();
```

所以这一 Node 的主线是：

```text
JSON Schema
↓
Model Output
↓
Structured JSON
↓
Validation
↓
Deserialization
↓
Java DTO
↓
Business Logic
```

---

# 二、为什么“拿到 JSON”还不算结束？

假设模型已经非常配合地返回：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

Java 完全可以这样读取：

```java
JSONObject json = JSON.parseObject(content);

String riskLevel = json.getString("riskLevel");
String reason = json.getString("reason");
```

这当然能工作。

但如果整个业务系统一直这样写：

```text
"riskLevel"
"reason"
"suggestion"
```

这些字符串字段名会散落在代码里。

业务代码还要自己记：

```text
哪个字段是什么类型
哪些字段必须存在
哪些值是合法的
```

Java 更擅长的方式是：

```text
把结构转换成一个明确类型
```

比如：

```java
CampaignAnalysis analysis;
```

这样业务代码处理的是：

```text
Java 对象
```

而不是：

```text
一堆动态 JSON 字段
```

---

# 三、什么是 DTO？

DTO 全称：

```text
Data Transfer Object
```

中文通常叫：

```text
数据传输对象
```

它主要用于：

```text
承载一组需要在系统不同边界之间传递的数据
```

例如：

```java
public class CampaignAnalysis {

    private Long campaignId;

    private String riskLevel;

    private String reason;

    private String suggestion;
}
```

这个类最主要的职责就是：

```text
装数据
```

它不需要负责：

```text
查数据库
调用模型
发送 HTTP
执行复杂业务逻辑
```

---

# 四、DTO 和 Entity 是一回事吗？

不是。

Entity 通常偏向：

```text
数据库实体
```

DTO 偏向：

```text
传输数据
```

例如数据库中的 Campaign 可能有：

```text
id
name
status
createdTime
updatedTime
deleted
tenantId
```

但模型分析结果可能只需要：

```text
campaignId
riskLevel
reason
suggestion
```

所以：

```text
Campaign Entity
≠
CampaignAnalysis DTO
```

不要为了省一个类，就强行让模型输出直接绑定数据库 Entity。

---

# 五、JSON Schema 和 DTO 是什么关系？

这是 Node 015 最核心的关系。

Schema：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer"
    },
    "riskLevel": {
      "type": "string",
      "enum": ["HIGH", "MEDIUM", "LOW"]
    },
    "reason": {
      "type": "string"
    },
    "suggestion": {
      "type": "string"
    }
  },
  "required": [
    "campaignId",
    "riskLevel",
    "reason",
    "suggestion"
  ],
  "additionalProperties": false
}
```

DTO：

```java
public class CampaignAnalysis {

    private Long campaignId;

    private String riskLevel;

    private String reason;

    private String suggestion;
}
```

它们实际上都在描述：

```text
同一份业务数据
```

但位置不同：

```text
JSON Schema
=
JSON / 模型这一侧的数据契约

Java DTO
=
Java 程序这一侧的数据类型
```

---

# 六、可以把它们理解成桥的两端

```text
       模型侧                         Java 侧

   JSON Schema
       │
       ▼
 Structured JSON
       │
       │  Deserialization
       ▼
 CampaignAnalysis DTO
```

Schema 规定：

```text
模型应该输出什么结构
```

DTO 规定：

```text
Java 希望用什么类型接住它
```

中间最关键的动作就是：

```text
反序列化
```

---

# 七、什么是序列化？

假设 Java 中已经存在：

```java
CampaignAnalysis analysis;
```

把 Java 对象转换成 JSON：

```text
Java Object
↓
JSON
```

这个过程通常叫：

```text
Serialization
序列化
```

例如：

```java
User user;
```

变成：

```json
{
  "name": "张三",
  "age": 18
}
```

---

# 八、什么是反序列化？

反过来：

```text
JSON
↓
Java Object
```

就叫：

```text
Deserialization
反序列化
```

例如模型返回：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "预算过高",
  "suggestion": "降低预算"
}
```

Java 将它转换成：

```java
CampaignAnalysis analysis;
```

这就是 Node 015 要真正跑通的动作。

---

# 九、FastJSON2 在这里做什么？

我们的 `agent-learning-lab` 已经使用 FastJSON2。

未来实验里会出现类似：

```java
CampaignAnalysis analysis =
        JSON.parseObject(content, CampaignAnalysis.class);
```

可以拆成：

```text
content
=
模型返回的 JSON 字符串

CampaignAnalysis.class
=
目标 Java 类型

parseObject(...)
=
把 JSON 反序列化成这个 Java 类型
```

最终：

```text
String
↓
CampaignAnalysis
```

---

# 十、为什么 DTO 比 JSONObject 更适合业务代码？

使用 JSONObject：

```java
JSONObject object = JSON.parseObject(content);

Long campaignId = object.getLong("campaignId");
String riskLevel = object.getString("riskLevel");
```

使用 DTO：

```java
CampaignAnalysis analysis = ...;

Long campaignId = analysis.getCampaignId();
String riskLevel = analysis.getRiskLevel();
```

DTO 的优势包括：

```text
类型明确
IDE 能自动提示
更方便重构
字段集中
业务代码更清晰
更容易测试
```

这就是 Java 强类型系统的优势。

---

# 十一、Structured Output 的终点不是 JSON

这是本 Node 最重要的一句话：

> **Structured Output 的终点不是“得到 JSON”，而是让模型输出进入程序的类型系统。**

对于 Java 来说：

```text
String
Integer
Long
Double
Boolean
List<T>
Enum
DTO
```

这些才是业务代码真正喜欢处理的东西。

所以我们真正希望实现：

```text
LLM Output
↓
Java Type
```

---

# 十二、JSON Schema 类型和 Java 类型怎么对应？

先建立最基础的映射。

## string

Schema：

```json
{
  "type": "string"
}
```

Java 通常：

```java
String
```

例如：

```text
reason
suggestion
name
```

---

## integer

Schema：

```json
{
  "type": "integer"
}
```

Java 常见：

```java
Integer
Long
```

例如：

```text
age
→ Integer

campaignId
→ Long
```

具体选哪个取决于业务和数值范围。

---

## number

Schema：

```json
{
  "type": "number"
}
```

Java 可能：

```java
Double
BigDecimal
```

例如置信度：

```java
Double confidence;
```

如果是金额等对精度敏感的数据：

```java
BigDecimal
```

通常更合适。

---

## boolean

Schema：

```json
{
  "type": "boolean"
}
```

Java：

```java
Boolean
```

例如：

```text
enabled
passed
needReview
```

---

## array

Schema：

```json
{
  "type": "array",
  "items": {
    "type": "string"
  }
}
```

Java 常见：

```java
List<String>
```

例如：

```json
{
  "keywords": [
    "Java",
    "并发",
    "线程池"
  ]
}
```

DTO：

```java
private List<String> keywords;
```

---

## object

Schema：

```json
{
  "type": "object"
}
```

Java 通常对应：

```text
一个 DTO / Java 类
```

例如：

```json
{
  "user": {
    "name": "张三",
    "age": 18
  }
}
```

可以映射：

```java
class User {
    private String name;
    private Integer age;
}
```

---

# 十三、嵌套 object 怎么映射？

JSON：

```json
{
  "user": {
    "name": "张三",
    "age": 18
  }
}
```

Java：

```java
public class Result {

    private User user;
}
```

以及：

```java
public class User {

    private String name;

    private Integer age;
}
```

所以：

```text
JSON object
↓
Java Object
```

可以一层层嵌套。

---

# 十四、array of object 怎么映射？

JSON：

```json
{
  "issues": [
    {
      "type": "BUG",
      "description": "登录按钮无响应"
    },
    {
      "type": "FEATURE",
      "description": "希望增加夜间模式"
    }
  ]
}
```

Java：

```java
private List<Issue> issues;
```

其中：

```java
public class Issue {

    private String type;

    private String description;
}
```

也就是：

```text
array
↓
List<T>

items: object
↓
T 对应一个 Java DTO
```

---

# 十五、Schema enum 最终可以对应 Java Enum

Schema：

```json
{
  "type": "string",
  "enum": [
    "HIGH",
    "MEDIUM",
    "LOW"
  ]
}
```

Java 初期可以：

```java
private String riskLevel;
```

但是更强类型的设计是：

```java
public enum RiskLevel {
    HIGH,
    MEDIUM,
    LOW
}
```

DTO：

```java
private RiskLevel riskLevel;
```

这样：

```text
Schema enum
↓
Java Enum
```

可以把约束继续带入 Java 类型系统。

---

# 十六、为什么 Java Enum 比 String 更强？

如果使用：

```java
String riskLevel;
```

Java 侧理论上可以出现：

```text
ABC
HELLO
严重
```

如果使用：

```java
RiskLevel
```

Java 侧的合法状态就只有：

```text
HIGH
MEDIUM
LOW
```

这就是：

```text
把动态字符串
进一步变成确定类型
```

不过第一轮实验为了减少变量，可以先使用：

```java
String
```

真正跑通主链后再优化。

---

# 十七、为什么 Schema 字段名最好和 DTO 字段名一致？

JSON：

```json
{
  "riskLevel": "HIGH"
}
```

DTO：

```java
private String riskLevel;
```

这种最简单。

因为：

```text
JSON 字段名
=
Java 属性名
```

JSON 框架通常可以直接映射。

如果 JSON 写：

```text
risk_level
```

而 Java 写：

```text
riskLevel
```

往往还要引入：

```text
注解
命名策略
额外映射
```

学习阶段没必要增加这种变量。

---

# 十八、普通 class、Lombok、record 怎么选？

DTO 可以有很多写法。

普通 class：

```java
public class CampaignAnalysis {

    private Long campaignId;

    private String riskLevel;

    // getter / setter
}
```

也可以用 Lombok。

Java 21 还可以：

```java
public record CampaignAnalysis(
        Long campaignId,
        String riskLevel,
        String reason,
        String suggestion
) {
}
```

这些都可以成为数据载体。

但是 Node 015 的重点不是：

```text
哪种 DTO 写法最优雅
```

而是：

```text
模型 JSON 如何真正进入 Java 类型
```

所以第一轮实验应该优先选择：

```text
最容易看懂的方式
```

---

# 十九、Schema Validation 和 Deserialization 不是一回事

这是非常重要的一点。

假设 Schema 要求：

```text
riskLevel
只能：
HIGH
MEDIUM
LOW
```

模型却返回：

```json
{
  "riskLevel": "CRITICAL"
}
```

DTO：

```java
private String riskLevel;
```

那么：

```text
"CRITICAL"
```

作为字符串完全可以被放进：

```java
String
```

因此：

```text
DTO 反序列化可能成功
```

但是：

```text
Schema 是不合法的
```

所以：

> **反序列化成功，不代表满足 Schema。**

---

# 二十、Deserialization 主要负责什么？

反序列化主要回答：

```text
这份 JSON
能不能映射成目标 Java 类型？
```

例如：

JSON：

```json
{
  "campaignId": 10001
}
```

DTO：

```java
private Long campaignId;
```

很好映射。

但如果：

```json
{
  "campaignId": "hello"
}
```

DTO 仍然要求：

```java
Long
```

就可能发生：

```text
类型转换问题
```

这属于：

```text
Deserialization / Type Mapping
```

的问题。

---

# 二十一、Schema Validation 负责什么？

Schema Validation 关注：

```text
实际数据
是否满足我们定义的 JSON Schema
```

例如：

```text
字段是否存在
字段类型是否正确
enum 是否合法
有没有额外字段
数组元素结构是否正确
```

所以：

```text
Schema Validation
```

检查的是：

```text
数据契约
```

---

# 二十二、Business Validation 又负责什么？

即使结构完全合法：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "预算过高",
  "suggestion": "降低预算"
}
```

程序仍可能发现：

```text
campaignId = 10001
在数据库里根本不存在
```

这就不是 Schema 问题。

而是：

```text
Business Validation
业务校验
```

例如：

```text
ID 是否存在
用户是否有权限
当前状态是否允许操作
金额是否超过余额
```

这些都属于业务规则。

---

# 二十三、JSON Parsing 和 Deserialization 也不是一回事

JSON Parsing 关注：

```text
这是不是合法 JSON？
```

例如：

```json
{
  "riskLevel": "HIGH"
}
```

可以正常解析。

而：

```text
{
  riskLevel: HIGH
}
```

可能就不是合法标准 JSON。

Deserialization 则继续问：

```text
合法 JSON
能不能变成指定的 Java 类型？
```

所以：

```text
JSON 合法
≠
一定能转 DTO
```

---

# 二十四、完整检查链应该怎么想？

以后看到模型 Structured Output，可以按照：

```text
Model Output
↓
1. JSON Syntax 是否合法？
↓
2. 是否符合 JSON Schema？
↓
3. 是否能反序列化成 DTO？
↓
4. DTO 是否满足业务规则？
↓
5. Business Logic
```

这几层不要混成一句：

```text
“解析失败了”
```

---

# 二十五、举一个 Parsing 问题

模型输出：

```text
{
  riskLevel: HIGH
}
```

问题首先是：

```text
JSON Syntax
```

如果 JSON 都解析不了：

```text
后面的 DTO 映射根本还没开始
```

---

# 二十六、举一个 Deserialization 问题

模型输出：

```json
{
  "campaignId": "abc",
  "riskLevel": "HIGH"
}
```

DTO：

```java
private Long campaignId;
```

这里：

```text
JSON 本身是合法的
```

但：

```text
"abc"
```

不能自然表示：

```text
Long
```

所以这是：

```text
Java 类型映射问题
```

---

# 二十七、举一个 Schema 问题

模型输出：

```json
{
  "campaignId": 10001,
  "riskLevel": "CRITICAL"
}
```

DTO：

```java
Long campaignId;
String riskLevel;
```

Java 可能完全能接住。

但是 Schema：

```text
riskLevel
只允许 HIGH / MEDIUM / LOW
```

因此：

```text
Schema Validation 失败
```

---

# 二十八、举一个 Business Validation 问题

模型输出：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH"
}
```

假设：

```text
JSON 合法
Schema 合法
DTO 反序列化成功
```

但是数据库查不到：

```text
campaignId = 10001
```

那么：

```text
业务校验失败
```

所以：

```text
结构正确
≠
事实正确
```

这和 Node 013 的结论完全一致。

---

# 二十九、DTO 成功创建后能不能直接信任？

不能。

例如：

```java
CampaignAnalysis analysis;
```

成功创建，只能说明：

```text
至少某种程度上数据已经进入 Java 类型
```

它不能自动证明：

```text
模型判断正确
事实真实
业务状态真实
用户有权限
```

所以：

```text
DTO
=
结构化载体

不是
=
可信事实
```

---

# 三十、为什么应该把 LLM Output 当作外部输入？

这是非常重要的工程意识。

LLM 虽然是：

```text
我们主动调用的服务
```

但它产生的内容：

```text
不是我们自己的确定性代码直接算出来的
```

它可能：

```text
错误
遗漏字段
误判
被上下文影响
被 Prompt Injection 影响
```

所以模型输出应该像：

```text
用户输入
第三方 API Response
MQ 消息
```

一样：

```text
验证以后再使用
```

---

# 三十一、LLM 不是传统的“可信内部函数”

普通 Java 方法：

```java
int add(int a, int b)
```

编译器已经明确：

```text
返回 int
```

LLM 原始调用更接近：

```text
String
↓
模型
↓
String
```

非常自由。

Structured Output + Schema + DTO 做的事情，就是逐渐把它变成：

```text
像一个有输入输出契约的组件
```

例如概念上：

```java
CampaignAnalysis analyze(CampaignInput input);
```

---

# 三十二、但模型仍然不是普通 Java 方法

即使最后返回：

```java
CampaignAnalysis
```

模型仍然是：

```text
概率性的
```

所以可能发生：

```text
类型正确
结构正确
但判断错了
```

因此：

> **类型可靠，不等于语义可靠。**

---

# 三十三、可以把可靠性拆成几层

以后做 LLM 工程，可以逐渐形成：

```text
1. Transport
   HTTP 是否成功

2. Syntax
   JSON 是否合法

3. Schema
   是否符合结构契约

4. Type
   是否能进入 Java DTO

5. Semantic
   模型判断是否合理

6. Business
   是否符合真实业务规则
```

这会让排错非常清晰。

---

# 三十四、为什么 Node 015 必须实践？

前面的原则一直是：

```text
必要时才做实验
```

Node 012 Few-shot：

```text
理论理解即可
```

Node 013 Structured Output：

```text
先建立概念即可
```

Node 014 JSON Schema：

```text
手写 Schema 就能完成主要学习目标
```

但 Node 015 不一样。

它的核心目标本身就是：

```text
让 Structured Output
真正进入 Java 类型系统
```

如果不运行：

```text
Java 请求
模型响应
JSON
FastJSON2
DTO
```

那么这个 Node 的关键能力根本没有真正发生。

所以：

```text
Node 015
=
有必要做实验
```

---

# 三十五、015-2 实验真正要证明什么？

不是证明：

```text
FastJSON2 会 parseObject
```

这只是普通 Java JSON 知识。

真正需要跑通：

```text
Java
↓
发送 Structured Output 请求
↓
模型按照 JSON Schema 返回
↓
取得模型生成的 JSON
↓
反序列化
↓
CampaignAnalysis
↓
业务代码直接读取字段
```

这才是完整实验目标。

---

# 三十六、为什么继续使用 HttpClient，而不是直接上 Spring AI？

因为我们现在仍然是在：

```text
学习底层调用链
```

如果直接用框架：

```text
Prompt
↓
DTO
```

可能只需要几行代码。

但：

```text
Schema 放在哪
请求长什么样
模型真正返回了什么
content 怎么取
DTO 怎么转换
```

都会被框架隐藏。

所以当前继续使用：

```text
HttpClient
OpenAI-compatible API
FastJSON2
```

更适合学习。

---

# 三十七、以后 Spring AI 会帮我们做什么？

未来框架很可能帮助我们：

```text
Java Type
↓
生成 Schema
↓
调用模型
↓
解析 Structured Output
↓
Java Object
```

到时候使用框架会非常舒服。

但现在先理解：

```text
框架背后发生了什么
```

以后才不会把：

```text
entity(...)
converter(...)
```

当作魔法。

---

# 三十八、为什么 Structured Output 特别适合 Java？

Java 是：

```text
强类型语言
```

它喜欢：

```text
明确字段
明确类型
Enum
DTO
Validation
```

而 LLM 是：

```text
生成式
概率式
自由度高
```

所以 Structured Output + DTO 的价值可以理解成：

```text
在 LLM 的不确定世界
和
Java 的确定类型世界
之间搭一座桥
```

---

# 三十九、这座桥长什么样？

```text
          不确定                     确定

           LLM
            │
            ▼
       JSON Schema
            │
            ▼
    Structured JSON
            │
            ▼
       Validation
            │
            ▼
    Deserialization
            │
            ▼
        Java DTO
            │
            ▼
     Business Logic
```

Node 015 就是在搭这座桥。

---

# 四十、为什么不用自然语言字符串继续做业务判断？

例如：

```java
if (content.contains("高风险")) {
    ...
}
```

问题是模型下一次可能写：

```text
风险较高
```

或者：

```text
HIGH
```

程序逻辑就可能变化。

DTO：

```java
analysis.getRiskLevel()
```

依赖的是：

```text
明确字段
```

而不是：

```text
模型自然语言措辞
```

稳定性差别很大。

---

# 四十一、DTO 也更适合测试

自然语言：

```java
assertTrue(content.contains("高风险"));
```

Structured Output：

```java
assertEquals("HIGH", analysis.getRiskLevel());
```

后者更适合：

```text
自动测试
回归测试
Evaluation
```

这会在后面越来越重要。

---

# 四十二、业务层为什么不应该直接操作 Provider Response？

OpenAI-compatible Response 里有：

```text
choices
message
content
finish_reason
usage
model
```

这些属于：

```text
模型 API 协议
```

而业务层真正想要：

```text
CampaignAnalysis
```

所以更合理的边界是：

```text
Provider Response
↓
模型调用层
↓
CampaignAnalysis DTO
↓
业务层
```

业务层不需要知道：

```text
choices[0].message.content
```

到底怎么取。

---

# 四十三、我们实际上会经历两层 JSON

这一点非常关键。

HTTP Response 本身已经是：

```json
{
  "choices": [
    {
      "message": {
        "content": "..."
      }
    }
  ]
}
```

这是：

```text
Provider Response JSON
```

而 `content` 里面又可能是一份：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH"
}
```

这是：

```text
模型生成的业务 JSON
```

所以程序逻辑实际上是：

```text
HTTP Response Body
↓
第一次解析
↓
Provider Response
↓
取 message.content
↓
第二次解析 / 反序列化
↓
CampaignAnalysis DTO
```

---

# 四十四、为什么这个区别重要？

因为以后出错时：

```text
外层 Response 解析失败
```

和：

```text
内层 Structured Output 解析失败
```

完全不是一个问题。

例如：

```text
Provider API 格式变了
```

属于外层。

模型生成：

```text
非法 Structured JSON
```

属于内层。

排查时必须区分。

---

# 四十五、字段缺失时会发生什么？

DTO：

```java
private String suggestion;
```

JSON：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

没有：

```text
suggestion
```

很多 JSON 框架可能仍然成功创建 DTO，只是：

```java
suggestion == null
```

所以再次证明：

```text
反序列化成功
≠
required 一定满足
```

---

# 四十六、为什么 DTO 中常用包装类型？

例如：

```java
private int age;
```

如果字段完全没有出现：

```text
age
```

Java 基本类型可能表现为默认值：

```text
0
```

你就很难判断：

```text
模型真的输出了 0
```

还是：

```text
字段根本缺失
```

如果是：

```java
private Integer age;
```

缺失时可以表现为：

```java
null
```

所以对于外部输入 DTO：

```text
Integer
Long
Boolean
Double
```

往往比基本类型更容易表达：

```text
缺失状态
```

---

# 四十七、为什么模型输出尤其需要关注 null 和缺失？

因为模型可能：

```text
少输出字段
```

如果 Java 自动把：

```text
int → 0
boolean → false
```

你可能误把：

```text
缺失
```

当成：

```text
真实业务值
```

所以：

```text
null
missing
default value
```

要有明确意识。

---

# 四十八、Bean Validation 和 JSON Schema 是一回事吗？

不是。

以后 Java 里可能出现：

```java
@NotNull
@NotBlank
@Min
@Max
```

这些属于：

```text
Java Bean Validation
```

而 JSON Schema：

```text
约束 JSON 数据结构
```

两者可能表达类似规则。

例如：

```text
JSON Schema required
```

和：

```java
@NotNull
```

都在一定程度上表达：

```text
“这个数据不能缺”
```

但它们：

```text
不是同一个技术
也不一定完全等价
```

---

# 四十九、未来可能出现多层 Validation

真实系统可能是：

```text
JSON Schema
↓
约束模型输出结构

DTO Validation
↓
约束 Java 对象

Business Validation
↓
检查真实业务状态
```

这很正常。

因为：

```text
没有哪一层应该单独承担全部可信性
```

---

# 五十、为什么不能让 DTO 承担所有业务逻辑？

DTO 的主要职责是：

```text
承载数据
```

例如：

```text
campaignId
riskLevel
reason
suggestion
```

如果 DTO 又负责：

```text
查数据库
发 HTTP
修改投放
调用 MQ
```

职责会混乱。

更合理：

```text
DTO
=
数据边界

Service
=
业务逻辑
```

---

# 五十一、DTO 设计应该围绕真正需要的数据

如果业务只需要：

```text
riskLevel
reason
suggestion
```

就没必要让模型返回：

```text
20 个额外分析字段
```

因为字段越多：

```text
Schema 越复杂
Token 越多
模型输出越长
失败面越大
DTO 越难维护
```

所以 Structured Output 设计也是：

```text
API / DTO 设计
```

---

# 五十二、Structured Output 不等于消灭自然语言

例如：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算明显高于当前转化水平",
  "suggestion": "降低预算并观察未来三天的转化表现"
}
```

这里：

```text
riskLevel
```

是高度结构化的。

但是：

```text
reason
suggestion
```

仍然是自然语言。

所以好的 Structured Output 往往是：

```text
确定结构
+
必要的自由文本
```

---

# 五十三、该固定的地方固定，该自由的地方自由

例如：

```text
riskLevel
```

适合：

```text
enum
```

因为程序需要明确状态。

而：

```text
reason
```

适合：

```text
string
```

因为模型需要解释。

这就是 Structured Output 很重要的设计思想：

> **不是把模型完全限制死，而是只在程序需要确定性的地方限制它。**

---

# 五十四、Schema 和 DTO 谁先设计？

真实工程里两种都可能。

可能：

```text
先有 Java DTO
↓
生成 Schema
```

也可能：

```text
先定义 Schema
↓
编写对应 DTO
```

现在不需要纠结谁必须先。

真正重要的是：

```text
两边表达同一个数据契约
```

---

# 五十五、Schema 和 DTO 不一致会怎样？

Schema：

```text
campaignId = integer
```

DTO：

```java
String campaignId;
```

也许某些框架能帮你转换。

但是：

```text
契约已经开始混乱
```

更明显：

Schema：

```text
keywords = array
```

DTO：

```java
String keywords;
```

就很容易导致：

```text
映射失败
```

所以原则是：

> **Schema 与 DTO 的字段、类型和层级应该尽量一致。**

---

# 五十六、为什么现在不要过度工程化？

当前不要先造：

```text
ModelClient
ProviderFactory
SchemaRegistry
StructuredOutputService
ValidatorChain
```

因为我们现在只有：

```text
一个 Provider
一个主要模型
一个 Structured Output 场景
```

如果提前做大量抽象：

```text
会把真正要学习的调用链藏起来
```

当前目标：

```text
先看懂
↓
先跑通
↓
真实出现差异
↓
再抽象
```

---

# 五十七、为什么实验适合用 CampaignAnalysis？

例如：

```java
class CampaignAnalysis {

    private Long campaignId;

    private String riskLevel;

    private String reason;

    private String suggestion;
}
```

它足够简单，又能同时练：

```text
integer → Long
string → String
enum 概念
Structured Output
DTO
```

很适合做第一个完整闭环。

---

# 五十八、为什么第一轮不做复杂嵌套？

如果同时加入：

```text
List
嵌套 DTO
Map
Nullable
复杂 Enum
多层 Object
```

实验失败时：

```text
变量太多
```

不容易判断问题在哪。

学习实验最好：

```text
一次只增加必要复杂度
```

所以第一轮：

```text
扁平 DTO
```

最合适。

---

# 五十九、第一阶段实验真正的成功标准

不是：

```text
HTTP Status = 200
```

也不是：

```text
控制台打印出 JSON
```

真正成功是：

```text
模型真实返回 Structured Output
↓
Java 取得模型结果
↓
成功反序列化成 CampaignAnalysis
↓
通过 Java 字段 / getter 获取具体值
```

这才算：

```text
LLM → Java DTO
```

真正跑通。

---

# 六十、第二阶段为什么要故意制造错误？

如果只跑成功路径：

```text
你会觉得所有东西都自动完成
```

故意制造：

```text
非法 JSON
字段缺失
错误类型
非法 enum
```

才会真正知道：

```text
JSON Parser 做什么
Schema 做什么
Deserializer 做什么
Validation 做什么
```

这也是为什么这个 Node 的实验很有价值。

---

# 六十一、Node 015 和 Tool Calling 有什么关系？

后面的 Tool Calling 也会发生：

```text
模型生成结构化参数
↓
Java 解析参数
↓
调用真实工具
```

例如：

```json
{
  "campaignId": 10001
}
```

程序最终希望得到：

```java
CampaignQuery query;
```

所以：

```text
Schema → JSON → Java Type
```

这套能力以后会直接复用。

---

# 六十二、Node 015 和 Agent Loop 有什么关系？

Agent Loop 里模型可能返回：

```text
下一步动作
参数
状态
```

程序必须：

```text
稳定读取
```

例如：

```json
{
  "action": "SEARCH",
  "query": "Java virtual threads"
}
```

如果最终能转换成：

```java
AgentDecision decision;
```

程序就能：

```java
switch (decision.getAction()) {
    ...
}
```

所以 Structured Output + DTO 是 Agent Runtime 的基础能力之一。

---

# 六十三、Node 015 和 Evaluation 有什么关系？

结构化结果非常适合自动评估。

例如：

```text
Expected:
category = BUG

Actual:
category = FEATURE
```

程序很容易比较。

而自然语言：

```text
“这个问题看起来像一个功能异常……”
```

要自动判断就困难很多。

所以：

```text
Structured Output
```

也会让后续：

```text
Evaluation
Regression Test
```

更容易自动化。

---

# 六十四、Node 015 和第一阶段项目有什么关系？

后面的 Node 021：

```text
Smart Text Analyzer
```

会需要：

```text
LLM
Structured Output
DTO
Exception Handling
测试 Case
```

所以 Node 015 是一个非常直接的前置。

等这一节完成：

```text
我们已经不再只是“调用模型聊天”
```

而是开始：

```text
让模型成为后端程序中的结构化组件
```

---

# 六十五、015-2 实验大致会做什么？

这里只看步骤，不提前写完整实现。

```text
1. 创建 CampaignAnalysis DTO

2. 编写对应 JSON Schema

3. Java 构造 Structured Output 请求

4. 调用 qwen3.8-max

5. 获取 HTTP Response

6. 解析 Provider Response

7. 获取 message.content

8. 把 content 反序列化成 CampaignAnalysis

9. 打印并使用 DTO 字段

10. 再测试错误路径
```

---

# 六十六、为什么 015-1 不直接把实验代码写完？

因为我们仍然采用：

```text
先理解
↓
再运行
```

理论篇只解决：

```text
为什么这么做
各层分别负责什么
```

实验篇再解决：

```text
具体怎么写
怎么运行
哪里会失败
```

这样看到代码时，每一行都有概念对应。

---

# 六十七、进入实验前应该能回答哪些问题？

至少应该知道：

1. DTO 是什么？
2. 为什么模型 JSON 还要转 DTO？
3. 什么是反序列化？
4. Schema 和 DTO 有什么区别？
5. JSON Parsing 和 Deserialization 有什么区别？
6. Schema Validation 和 Business Validation 有什么区别？
7. 为什么 DTO 创建成功也不能完全相信模型？
8. 为什么 LLM Output 应该被当成外部输入？

如果这些基本明白：

```text
就可以进入 015-2
```

---

# 六十八、把几个核心概念并排

```text
JSON Schema
=
模型输出的数据契约

Structured JSON
=
模型实际生成的数据

Deserialization
=
JSON → Java Object

DTO
=
Java 中承载模型结果的类型

Schema Validation
=
检查结构契约

Business Validation
=
检查真实业务规则

Business Logic
=
真正使用结果做事情
```

---

# 六十九、最容易混淆的五组概念

## JSON vs DTO

```text
JSON
=
文本数据格式

DTO
=
Java 对象
```

---

## Parsing vs Deserialization

```text
Parsing
=
读懂 JSON 结构

Deserialization
=
映射成指定 Java 类型
```

---

## Schema vs DTO

```text
Schema
=
JSON 数据契约

DTO
=
Java 数据类型
```

---

## Schema Validation vs Business Validation

```text
Schema Validation
=
结构是否合法

Business Validation
=
业务是否合理、真实
```

---

## 类型正确 vs 事实正确

```text
类型正确
≠
模型判断正确
```

---

# 七十、真正需要记住的 15 个结论

1. Node 015 的目标是让模型结构化结果进入 Java 类型系统。
2. Structured Output 的终点不是 JSON，而是程序可使用的类型。
3. DTO 是用来承载数据的 Java 对象。
4. JSON Schema 描述 JSON 世界的数据契约。
5. DTO 描述 Java 世界的数据结构。
6. 反序列化是 JSON → Java Object。
7. FastJSON2 可以把 JSON 字符串映射成 DTO。
8. Schema 与 DTO 的字段、类型和层级应尽量对应。
9. Schema `string` 通常对应 Java `String`。
10. Schema `integer` 常对应 Java `Integer / Long`。
11. Schema `array` 常对应 Java `List<T>`。
12. Schema Validation、Deserialization、Business Validation 是不同层。
13. DTO 创建成功不代表模型内容一定正确。
14. LLM Output 应当视为外部输入，校验后再使用。
15. Structured Output + DTO 是 Tool Calling、Agent Loop 和 Evaluation 的基础能力。

---

# 七十一、理解自测

## 基础

1. DTO 是什么？
2. 为什么模型输出 JSON 后还要转 DTO？
3. 什么叫序列化？
4. 什么叫反序列化？
5. JSON Schema 和 Java DTO 有什么区别？

## 类型映射

6. Schema string 常对应什么 Java 类型？
7. Schema integer 常对应什么 Java 类型？
8. Schema number 可能对应什么 Java 类型？
9. Schema boolean 常对应什么 Java 类型？
10. Schema array 怎么映射 Java？
11. 嵌套 object 怎么映射 Java？

## 工程理解

12. JSON Parsing 和 Deserialization 有什么区别？
13. Schema Validation 和 Deserialization 有什么区别？
14. Business Validation 又负责什么？
15. 为什么 DTO 创建成功也不能直接信任模型？
16. 为什么 LLM Output 应该视为外部输入？
17. 为什么 Structured Output 对 Java 特别有价值？
18. 为什么业务层不应该直接操作 Provider 原始 Response？

---

# 七十二、口述验收

如果有人问：

> 模型已经输出 JSON 了，为什么还要 Java DTO？

你应该能够解释：

> JSON 只是模型返回的数据格式，而 Java 业务代码更适合操作明确类型。通过反序列化，可以把模型返回的结构化 JSON 转成 CampaignAnalysis 这样的 DTO，然后通过字段或 getter 直接读取 campaignId、riskLevel、reason 等数据，而不需要在业务代码里不断通过字符串字段名操作 JSONObject。
>
> JSON Schema 和 DTO 分别位于模型侧和 Java 侧。Schema 定义模型应该输出什么结构，DTO 定义 Java 程序希望接收什么类型，中间通过 Structured JSON 和反序列化连接起来。
>
> 但是 DTO 创建成功并不代表结果一定正确。还要区分 JSON 语法、Schema Validation、Java Deserialization 和 Business Validation。Structured Output 主要让模型结果可靠地进入程序类型系统，并不能保证模型的业务判断或事实一定正确。

如果能自然讲清：

```text
Schema
↓
JSON
↓
Deserialization
↓
DTO
↓
Validation
↓
Business Logic
```

就可以进入实践篇。

---

# 七十三、Node 015-1 完成标准

读完本篇后，应该能够：

- [ ] 理解 DTO 是什么；
- [ ] 理解为什么 Structured Output 最终要进入 Java DTO；
- [ ] 理解序列化与反序列化；
- [ ] 理解 Schema 和 DTO 的关系；
- [ ] 理解常见 Schema 类型和 Java 类型映射；
- [ ] 理解 array → List<T>；
- [ ] 理解 object → DTO；
- [ ] 理解 JSON Parsing 与 Deserialization 的区别；
- [ ] 理解 Schema Validation 与 Deserialization 的区别；
- [ ] 理解 Business Validation；
- [ ] 理解 DTO 创建成功不等于事实正确；
- [ ] 理解为什么 LLM Output 应视为外部输入；
- [ ] 理解为什么业务层更适合使用 DTO；
- [ ] 理解 Node 015 为什么需要实验。

---

# 七十四、015-1 的停止点

本篇只负责：

```text
Java DTO
+
Structured Output
```

之间的理论连接。

现在不要求：

```text
完整 Structured Output Java 代码
真实 API 调用
错误实验
Spring AI
Bean Validation
Schema 自动生成
Provider 抽象
复杂 DTO 嵌套
```

其中真正值得实践的：

```text
真实 API
Structured Output
DTO 反序列化
错误路径
```

统一放到：

```text
015-2
```

---

# 七十五、下一步：015-2 Java 实验

理论篇完成后，下一步：

```text
015-2 - 实验：使用 Java 实现 Structured Output
```

目标链路：

```text
CampaignAnalysis DTO
↓
JSON Schema
↓
Java 调用模型
↓
Structured JSON
↓
FastJSON2
↓
CampaignAnalysis
↓
Business Logic
```

然后再通过错误样本观察：

```text
JSON Parsing
Schema
Deserialization
Validation
```

分别负责什么。

这一次属于：

```text
有必要的实验
```

因为 Node 015 的核心能力本身就是：

```text
让模型的结构化输出真正进入 Java
```

---

# 本篇一句话总结

> **Java DTO 与 Structured Output 的核心，就是把模型生成的结构化 JSON，通过明确的数据契约、校验和反序列化，转换成 Java 可以直接处理的明确类型。**

最终链路：

```text
JSON Schema
↓
LLM
↓
Structured JSON
↓
Validation
↓
Deserialization
↓
Java DTO
↓
Business Logic
```

**015-1 理论篇完成后，下一步进入 015-2 Java Structured Output 实验。**
