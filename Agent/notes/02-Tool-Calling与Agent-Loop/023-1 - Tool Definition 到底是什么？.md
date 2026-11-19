# Node 023：Tool Definition 到底是什么？

> 所属阶段：Phase 2 / Tool Calling 与 Agent Loop  
> 笔记类型：理论篇  
> 学习定位：理解一个 Tool Definition 由哪些部分组成、它为什么是模型选择工具的依据，以及为什么 Tool Description 会直接影响模型的工具选择。  
> 本篇边界：暂不深入讲参数 Schema 的 `required`、`enum`、范围限制等细节，这些留到 Node 024。

---

## 一、本篇要解决什么问题？

在 Node 022 中，我们已经知道：

> **LLM 不会直接执行 Java 方法。**

模型真正做的是：

```text
理解用户需求
    ↓
判断是否需要工具
    ↓
选择一个工具
    ↓
生成 Tool Call 请求
    ↓
Application 执行真正的工具
```

但是这里马上会出现一个新的问题：

> **模型怎么知道有哪些工具可以用？**

例如，我们的 Java 应用里可能真的有这些方法：

```java
getWeather(String city)

queryCampaign(Long campaignId)

queryBudget(Long campaignId)

pauseCampaign(Long campaignId)
```

但模型并不会自动扫描我们的 Java 源码，也不会天然知道：

```text
getWeather 是干什么的？
queryCampaign 和 queryBudget 有什么区别？
pauseCampaign 会不会修改数据？
每个工具需要哪些参数？
参数应该是什么类型？
```

所以，应用程序必须把“可用工具”用一种模型能够理解的形式描述出来。

这个描述，就是：

# Tool Definition

可以先记住：

> **Tool Definition 是应用程序提供给模型的一份工具说明书。**

模型通过这份说明书判断：

```text
这个工具叫什么？
它能干什么？
什么时候应该使用？
调用时需要提供什么参数？
参数应该是什么结构？
```

---

## 二、Tool Definition 到底是什么？

Tool Definition 可以理解成：

> **应用程序对某项工具能力的结构化描述。**

它不是工具真正的执行代码。

例如，我们真正的 Java 方法可能是：

```java
public Campaign queryCampaign(Long campaignId) {
    return campaignRepository.findById(campaignId);
}
```

而模型看到的并不是这段 Java 代码。

模型看到的更像是：

```text
工具名称：
queryCampaign

工具说明：
根据 Campaign ID 查询广告计划的基础信息。

参数：
campaignId

参数结构：
campaignId 是整数。
```

所以：

```text
Java Method
```

和：

```text
Tool Definition
```

是两个不同层面的东西。

### 2.1 Java Method 是执行实现

例如：

```java
public Campaign queryCampaign(Long campaignId) {
    return campaignRepository.findById(campaignId);
}
```

它负责真正执行：

```text
查数据库
调用 API
计算数据
修改业务状态
```

### 2.2 Tool Definition 是给模型看的说明

它负责告诉模型：

```text
这个能力存在
它叫什么
它解决什么问题
它需要哪些输入
```

可以画成：

```text
                    模型看到的
                        │
                        ▼
                Tool Definition
                        │
                        │ 描述了
                        ▼
                  某项工具能力
                        │
                        │ Application 映射
                        ▼
                   Java Method
                        │
                        ▼
                  真正执行业务
```

---

## 三、为什么不能直接把 Java 方法名丢给模型？

假设我们的应用只告诉模型：

```text
可用工具：

getData
getInfo
query
execute
```

从模型角度看，这几乎没有足够的信息。

用户说：

```text
帮我看看 Campaign 123 的预算。
```

模型可能会思考：

```text
getData？
getInfo？
query？
到底哪个才是预算查询？
```

这就像程序员第一次看到：

```java
a()
b()
c()
doSomething()
```

也很难知道它们真正做什么。

所以 Tool Calling 并不是：

```text
给模型一个函数名
```

这么简单。

模型需要一份足够清晰的工具说明。

---

## 四、一个 Tool Definition 通常包含什么？

按照我们的学习路线，一个 Tool 先拆成四个核心部分：

```text
Name
Description
Parameters
Schema
```

这四个词现在必须建立整体认识。

---

## 五、Name：工具名称

Name 就是工具的名字，例如：

```text
getWeather
queryCampaign
queryCampaignMetrics
queryBudget
pauseCampaign
```

模型会通过 Name 对工具形成第一层语义判断。

例如：

```text
queryCampaignMetrics
```

相比：

```text
getData
```

明显更容易理解。

因为前者已经透露：

```text
query
    ↓
查询

Campaign
    ↓
广告计划

Metrics
    ↓
表现指标
```

而：

```text
getData
```

几乎没有告诉模型具体业务含义。

### 5.1 Name 应该表达明确能力

比较：

```text
tool1
```

和：

```text
queryCampaignBudget
```

后者更好。

比较：

```text
execute
```

和：

```text
pauseCampaign
```

后者更好。

一个好的工具名称通常会体现：

```text
动作 + 对象
```

例如：

```text
queryCampaign
queryCampaignMetrics
queryAccount
queryBudget
pauseCampaign
updateCampaignBudget
```

### 5.2 Name 不是越长越好

例如：

```text
queryCampaignCurrentDailyBudgetForSpecificCampaignByCampaignId
```

信息很多，但过于啰嗦。

更合理的可能是：

```text
queryCampaignBudget
```

更具体的含义交给 Description。

所以：

```text
Name
```

主要负责：

> **简洁、稳定地标识工具。**

---

## 六、Description：工具描述

这是本 Node 最重要的部分。

路线里专门提出了一个问题：

> **为什么 Tool Description 极其重要？**

因为模型并不是只靠工具名称做选择。

它还会阅读 Description。

Description 会告诉模型：

```text
这个工具究竟做什么
适用于什么场景
返回什么类型的信息
什么时候应该使用
什么时候不应该使用
与其他相似工具有什么区别
```

可以把 Tool Description 理解成：

> **模型选择工具时的重要语义依据。**

### 6.1 一个模糊 Description

假设我们有：

```text
Name:
queryCampaign

Description:
查询数据。
```

这个描述问题很大。

因为模型并不知道：

```text
查询什么数据？
Campaign 基础信息？
表现数据？
预算？
状态？
账户信息？
```

### 6.2 一个更清楚的 Description

例如：

```text
Name:
queryCampaign

Description:
根据 Campaign ID 查询广告计划的基础信息，
包括名称、状态、所属账户等，
不返回表现指标和预算数据。
```

这时候模型就更容易判断：

```text
用户问 Campaign 名称、状态
        ↓
queryCampaign

用户问 Campaign 点击量、消耗、转化
        ↓
不应该只用 queryCampaign
```

这就是 Description 的价值。

---

## 七、为什么 Description 会影响模型选择？

模型收到的上下文可能类似：

```text
用户：
帮我看看 Campaign 123 最近表现怎么样。

可用工具：

1. queryCampaign
   查询 Campaign 基础信息。

2. queryCampaignMetrics
   查询 Campaign 的消耗、展示、点击、转化等表现指标。

3. queryBudget
   查询 Campaign 当前预算。
```

模型要做的是：

```text
理解用户问题
      ↓
理解每个 Tool 的能力
      ↓
比较哪个 Tool 最符合当前任务
      ↓
选择 Tool
```

所以模型选择工具，本质上也是一次：

```text
语义匹配 + 推理
```

如果 Description 写得差：

```text
queryCampaignMetrics：
查询数据。
```

那么模型获取不到足够语义。

如果写得清楚：

```text
queryCampaignMetrics：
根据 Campaign ID 查询广告计划的表现指标，
包括 impressions、clicks、cost、conversions 等。
```

模型就更容易知道：

```text
“表现怎么样”
```

应该对应：

```text
queryCampaignMetrics
```

因此：

> **Description 不是文档装饰，而是模型决策输入的一部分。**

---

## 八、Tool Definition 本质上是模型的“能力菜单”

可以把 Tool Definition 想象成餐厅菜单。

如果菜单只有：

```text
A
B
C
```

你不知道该选什么。

如果菜单写：

```text
A：牛肉面
B：炸酱面
C：番茄鸡蛋面
```

就清楚多了。

Tool Definition 类似：

```text
queryCampaign：
查询 Campaign 基础信息。

queryCampaignMetrics：
查询 Campaign 表现指标。

queryBudget：
查询 Campaign 预算。

queryAccount：
查询广告账户信息。
```

模型就能根据用户任务在“能力菜单”中选择。

因此：

> **Tool Definition 决定了模型眼中的工具世界是什么样子。**

应用程序里即使有 100 个 Java 方法，如果没有把它们作为 Tool Definition 提供给模型，那么从当前这一轮模型调用的角度看，它们就不属于可用工具。

---

## 九、Parameters：工具需要什么输入？

Tool Definition 不仅要告诉模型：

```text
这个工具是干什么的
```

还要告诉模型：

```text
调用这个工具需要什么参数
```

例如：

```text
queryCampaign
```

可能需要：

```text
campaignId
```

模型调用时可能生成：

```json
{
  "campaignId": 123
}
```

再比如：

```text
getWeather
```

可能需要：

```text
city
```

模型可能生成：

```json
{
  "city": "Beijing"
}
```

所以 Parameters 可以理解成：

> **工具调用时需要模型提供的输入信息。**

### 9.1 参数名称也需要有语义

比较：

```text
id
```

和：

```text
campaignId
```

在复杂业务里：

```text
campaignId
```

往往更明确。

因为同时可能存在：

```text
accountId
campaignId
adGroupId
```

一个模糊的：

```text
id
```

可能让模型难以判断到底需要什么 ID。

### 9.2 Parameters 不等于 Java 方法签名本身

Java 方法可能是：

```java
public Campaign queryCampaign(Long campaignId)
```

但模型并不直接读取这个 Java 签名。

Application 会把参数信息转换成模型 API 所要求的 Tool Definition 格式。

因此：

```text
Java 参数
       ↓
Application 描述
       ↓
Tool Parameters / Schema
       ↓
LLM
```

模型根据这份定义生成参数。

---

## 十、Schema：参数结构的正式约定

仅仅告诉模型：

```text
这个工具需要 campaignId
```

还不够完整。

模型还需要知道：

```text
campaignId 是字符串还是整数？
参数整体是什么结构？
有哪些字段？
```

所以 Tool Definition 通常会使用 Schema 描述参数结构。

一个简化示意：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer"
    }
  }
}
```

它表达：

```text
整个参数是一个 object

里面有：
campaignId

campaignId 的类型是：
integer
```

这和我们之前学过的 JSON Schema 有联系。

### 10.1 这里先不要进入 Node 024

本 Node 只需要理解：

> **Schema 用来描述 Tool 参数应该长什么样。**

现在不要提前深入：

```text
required
enum
minimum
maximum
array
嵌套 object
复杂约束
```

这些会在 Node 024 专门学习。

---

## 十一、一个完整的 Tool Definition 示例

假设我们的业务中有一个工具：

```text
queryCampaign
```

概念上可以描述成：

```text
Name:
queryCampaign

Description:
根据 Campaign ID 查询广告计划的基础信息，
包括名称、状态和所属账户。
不返回广告表现指标和预算。

Parameters:
campaignId

Schema:
campaignId 是 integer。
```

如果用接近模型 API 的结构表示，可能类似：

```json
{
  "name": "queryCampaign",
  "description": "根据 Campaign ID 查询广告计划的基础信息，包括名称、状态和所属账户，不返回广告表现指标和预算。",
  "parameters": {
    "type": "object",
    "properties": {
      "campaignId": {
        "type": "integer",
        "description": "广告计划 ID"
      }
    }
  }
}
```

这里先不要死记 JSON 字段。

重点看逻辑：

```text
name
      ↓
这个 Tool 叫什么

description
      ↓
这个 Tool 能做什么

parameters
      ↓
调用它需要什么输入

schema
      ↓
输入应该是什么结构
```

---

## 十二、Tool Definition 和 Tool Call 的区别

这是非常容易混淆的两个概念。

### 12.1 Tool Definition

这是应用程序发给模型的：

```text
工具说明
```

它表达：

> **“你可以使用这个工具。”**

### 12.2 Tool Call

这是模型返回给应用程序的：

```text
工具调用请求
```

例如：

```json
{
  "name": "queryCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

它表达：

> **“我现在想调用这个工具，并传入这些参数。”**

### 12.3 两者方向不同

```text
Application
    │
    │ Tool Definition
    ▼
   LLM

   LLM
    │
    │ Tool Call
    ▼
Application
```

所以：

```text
Tool Definition：
Application → LLM

Tool Call：
LLM → Application
```

一定不要混淆。

---

## 十三、Tool Definition 和 Structured Output 的 Schema 有什么关系？

前面学习 Structured Output 时，我们已经接触过 JSON Schema。

二者底层思想有相似之处：

```text
都在描述结构
都希望模型按照结构生成数据
```

但是用途不同。

### Structured Output

描述：

```text
模型最终回答的数据结构
```

例如：

```json
{
  "summary": "...",
  "sentiment": "positive"
}
```

意思是：

> 请把业务结果按照这个 Schema 返回。

### Tool Parameters Schema

描述：

```text
调用工具时 arguments 应该是什么结构
```

例如：

```json
{
  "campaignId": 123
}
```

意思是：

> 如果你要请求调用这个工具，请按照这个参数结构生成 arguments。

### 对比

| 对比项 | Structured Output Schema | Tool Parameters Schema |
|---|---|---|
| 描述对象 | 模型业务输出 | Tool Call 参数 |
| 主要目的 | 让最终结果结构化 | 让工具参数结构化 |
| 后续用途 | 反序列化成业务数据 | Application 执行 Tool |
| 是否直接代表工具调用 | 否 | 是 Tool Call 的参数契约 |

可以这样记：

```text
Structured Output Schema
=
“最终答案应该长什么样”

Tool Parameters Schema
=
“调用这个工具的参数应该长什么样”
```

---

## 十四、为什么 Tool Description 比很多人想象中更重要？

Tool Calling 中其实有两个不同问题：

```text
问题 1：
模型应该调用哪个 Tool？

问题 2：
选中 Tool 后应该生成什么参数？
```

其中：

```text
Description
```

主要帮助解决第一个问题。

而：

```text
Parameters / Schema
```

主要帮助解决第二个问题。

所以：

```text
Description 不清楚
```

可能导致：

```text
工具选错
```

而：

```text
Schema 不清楚
```

可能导致：

```text
参数生成错误
```

---

## 十五、多个相似 Tool 时，Description 更重要

假设以后有四个工具：

```text
queryCampaign
queryCampaignMetrics
queryBudget
queryAccount
```

用户说：

```text
帮我看看 Campaign 123 最近花了多少钱。
```

模型需要区分：

```text
queryCampaign
        ↓
基础信息

queryCampaignMetrics
        ↓
实际表现、消耗等指标

queryBudget
        ↓
预算配置

queryAccount
        ↓
账户信息
```

“花了多少钱”更接近：

```text
实际消耗
```

而不是：

```text
预算
```

如果 Description 写得很差：

```text
queryCampaignMetrics：
查询 Campaign 数据。

queryBudget：
查询 Campaign 数据。
```

模型就很难区分。

如果写得清楚：

```text
queryCampaignMetrics：
查询 Campaign 的实际投放表现，
包括 impressions、clicks、cost、conversions 等指标。

queryBudget：
查询 Campaign 当前配置的预算额度，
用于回答预算上限、每日预算等问题，
不表示实际已经消耗的金额。
```

模型就更容易正确选择。

---

## 十六、Description 应该写什么？

先建立几个基础原则。

### 16.1 说明工具真正的能力

不好：

```text
查询数据。
```

更好：

```text
根据 Campaign ID 查询广告计划的基础信息。
```

### 16.2 明确工具处理的对象

不好：

```text
查询状态。
```

更好：

```text
查询指定 Campaign 当前的启用、暂停等业务状态。
```

### 16.3 必要时说明边界

例如：

```text
查询 Campaign 的基础信息，不返回表现指标和预算。
```

“不能做什么”有时候和“能做什么”一样重要。

### 16.4 不要把实现细节全塞进去

没有必要写：

```text
这个工具内部会调用 CampaignService，
CampaignService 再调用 CampaignMapper，
最后使用 MyBatis 查询 MySQL。
```

模型真正需要知道的是：

```text
什么时候应该使用这个 Tool
这个 Tool 能得到什么
```

所以 Tool Definition 面向的是：

```text
模型决策
```

而不是：

```text
Java 内部代码文档
```

---

## 十七、Description 不是 Prompt 的替代品

Tool Description 和 System Prompt、User Message 都会影响模型，但职责不同。

System Prompt 可能描述：

```text
你是一个广告投放助手。
你不能未经确认执行高风险操作。
```

User Message 描述：

```text
帮我看看 Campaign 123 最近表现怎么样。
```

Tool Description 描述：

```text
queryCampaignMetrics：
查询指定 Campaign 的消耗、点击、转化等表现指标。
```

可以理解为：

```text
System Prompt
    ↓
告诉模型整体规则

User Message
    ↓
告诉模型当前需求

Tool Definition
    ↓
告诉模型当前有哪些可用能力
```

---

## 十八、模型会看到 Java 方法源码吗？

正常情况下，不会。

例如：

```java
public Campaign queryCampaign(Long campaignId) {
    return campaignService.getById(campaignId);
}
```

模型不会因为这个方法存在，就自动知道它是一个工具。

所以：

```text
Java Method 存在
≠
模型知道 Tool 存在
```

还需要：

```text
Java Method
      ↓
被 Application 注册 / 描述为 Tool
      ↓
Tool Definition
      ↓
发送给模型
```

之后模型才能在这一轮调用中考虑它。

---

## 十九、Tool Definition 不代表工具一定会被调用

即使我们把 Tool Definition 发给模型：

```text
getWeather
queryCampaign
queryBudget
```

模型也不一定会调用。

例如用户说：

```text
你好。
```

模型完全可能直接回答，而不需要 Tool Call。

所以：

```text
提供 Tool
```

只是告诉模型：

> 你有这些能力可以选择。

不是：

> 你必须调用一个 Tool。

具体是否调用，还取决于：

```text
用户需求
模型判断
Tool Description
上下文
API 配置
```

---

## 二十、Tool Definition 是 Contract

可以进一步把 Tool Definition 理解成：

> **LLM 与 Application 之间的一种接口契约。**

Application 告诉模型：

```text
我提供一个叫 queryCampaign 的能力。

它负责查询 Campaign 基础信息。

如果你想使用它，
请按照约定结构提供 campaignId。
```

模型则按照这个契约生成 Tool Call，Application 再解析。

这和传统后端里的接口契约有点像。

例如 REST API 会规定：

```text
Endpoint
Method
Request Body
Response Body
```

而 Tool Definition 规定：

```text
Tool Name
Description
Parameters
Schema
```

虽然不是同一种协议，但都体现：

> **调用双方必须对能力和数据格式形成约定。**

---

## 二十一、Tool Definition 和 API 文档有什么不同？

Tool Definition 有点像 API 文档，但不能完全等同。

API 文档主要给：

```text
开发人员
```

阅读。

Tool Definition 主要给：

```text
模型
```

作为推理上下文。

所以 API 文档可能写很多：

```text
认证方式
HTTP 状态码
示例请求
数据库结构
版本信息
```

而 Tool Definition 更关注：

```text
模型如何判断什么时候应该调用
调用时应该给什么参数
```

因此 Tool Description 的写法要服务于：

```text
模型工具选择
```

---

## 二十二、Tool Definition 和 Java Interface 也不是一回事

例如：

```java
public interface CampaignService {

    Campaign queryCampaign(Long id);

    Metrics queryMetrics(Long id);

}
```

这是 Java 类型系统中的接口。

Tool Definition 则属于：

```text
LLM Tool Calling 协议中的能力描述
```

二者可能存在映射：

```text
Tool Definition
      ↓
queryCampaign
      ↓
CampaignService.queryCampaign()
```

但不能认为：

```text
Tool Definition = Java Interface
```

---

## 二十三、一个业务能力是否都应该暴露成 Tool？

不一定。

假设后端有：

```text
300 个 Service 方法
```

并不意味着：

```text
300 个方法全部注册成 Tool
```

Tool 应该是：

> **希望模型能够根据任务主动选择和请求使用的能力。**

例如：

```text
private String buildInternalCacheKey(...)
```

显然没必要成为 Tool。

而：

```text
queryCampaign
queryCampaignMetrics
queryBudget
```

更可能适合暴露给 Agent。

所以 Tool Definition 同时也是一种：

```text
能力边界设计
```

应用程序主动决定：

```text
模型能够看到什么能力
```

而不是把整个系统内部方法全部暴露出去。

---

## 二十四、Tool Definition 设计不好会发生什么？

### 24.1 工具名称太模糊

例如：

```text
getData
```

模型不知道它具体查询什么。

### 24.2 Description 太泛

例如：

```text
查询 Campaign。
```

如果还有：

```text
queryCampaignMetrics
queryBudget
```

模型可能无法正确区分。

### 24.3 Description 与真实能力不一致

Description 写：

```text
查询 Campaign 的实时表现指标。
```

但实际工具只能返回：

```text
Campaign 名称
Campaign 状态
```

模型就会基于错误认知做决策。

因此：

> **Tool Definition 必须和真实工具能力保持一致。**

### 24.4 参数命名不明确

例如：

```text
id
```

在复杂业务里可能不知道是：

```text
accountId
campaignId
adId
```

### 24.5 Tool 之间职责严重重叠

例如：

```text
getCampaignData
getCampaignInfo
queryCampaign
fetchCampaign
```

四个 Tool 实际做差不多的事情。

这会给模型增加选择难度。

---

## 二十五、Tool Description 和 Tool Name 应该如何配合？

可以把它们理解成：

```text
Name
=
短标签

Description
=
详细语义
```

例如：

```text
Name:
queryCampaignMetrics

Description:
查询指定 Campaign 的投放表现指标，
包括展示、点击、消耗和转化数据。
```

模型先通过 Name 快速形成概念，再通过 Description 理解具体边界。

不要让：

```text
Name 表达一种含义
Description 表达另一种含义
```

例如：

```text
Name:
queryBudget

Description:
查询 Campaign 的点击量和转化率。
```

这就是明显冲突。

---

## 二十六、一个好 Tool Definition 的基本特征

本 Node 暂时记住五个关键词：

```text
清晰
明确
一致
可区分
不过度
```

### 清晰

模型能够理解 Tool 做什么。

### 明确

知道 Tool 的业务对象和用途。

### 一致

Description 与真实实现一致。

### 可区分

与其他 Tool 有明确边界。

### 不过度

不塞入模型决策不需要的实现细节。

---

## 二十七、从模型视角看 Tool Definition

模型收到：

```text
User:
帮我看看 Campaign 123 最近表现怎么样。

Tools:

queryCampaign
查询 Campaign 的基础信息，包括名称和状态。

queryCampaignMetrics
查询 Campaign 的展示、点击、消耗和转化等表现指标。

queryBudget
查询 Campaign 当前配置的预算。
```

模型需要推理：

```text
用户问“表现怎么样”
        ↓
这不是只问基础信息
        ↓
也不是只问预算配置
        ↓
“表现”对应 metrics
        ↓
选择 queryCampaignMetrics
```

所以 Tool Description 实际参与了模型的：

```text
工具选择推理
```

这就是为什么 Description 不是可有可无的注释。

---

## 二十八、从 Application 视角看 Tool Definition

Application 负责：

```text
1. 定义有哪些 Tool

2. 为 Tool 写清楚：
   Name
   Description
   Parameters
   Schema

3. 把 Tool Definition 随请求发送给 LLM

4. 接收 LLM 返回的 Tool Call

5. 再进入后续执行流程
```

在 Node 023 阶段，我们重点学习的是：

```text
Application
      │
      │ 构造
      ▼
Tool Definition
      │
      │ 发送
      ▼
     LLM
```

还没有进入：

```text
LLM
 ↓
Tool Call
 ↓
Dispatcher
 ↓
Java Method
```

这一部分属于后续 Node。

---

## 二十九、当前阶段不要急着使用框架封装

后面学习 Spring AI、LangChain4j 等框架时，框架可能允许我们通过注解或 API 自动生成 Tool Definition。

这样会很方便。

但现在路线刻意先学底层概念。

因为如果一开始只看到：

```text
加一个注解
然后模型就能调用方法
```

很容易误以为：

```text
模型真的直接调用了 Java 方法
```

实际上中间存在：

```text
Java Tool Metadata
      ↓
Tool Definition
      ↓
Model API
      ↓
Tool Call
      ↓
Application
      ↓
Java Method
```

先理解这条链路，后面再用框架，就更容易看懂框架隐藏了什么。

---

## 三十、本篇最重要的几个区别

### 30.1 Java Method vs Tool Definition

```text
Java Method
=
真正执行能力

Tool Definition
=
给模型看的能力说明
```

### 30.2 Tool Definition vs Tool Call

```text
Tool Definition
=
Application → LLM
告诉模型有哪些工具

Tool Call
=
LLM → Application
模型请求调用某个工具
```

### 30.3 Description vs Parameters

```text
Description
=
这个工具做什么、什么时候用

Parameters
=
调用工具需要提供什么输入
```

### 30.4 Parameters vs Schema

当前先粗略理解：

```text
Parameters
=
工具有哪些输入

Schema
=
这些输入应该是什么结构
```

详细参数约束留到 Node 024。

---

## 三十一、把 Node 022 和 Node 023 串起来

```text
Application 中存在真实 Java 能力
        ↓
Application 为能力构造 Tool Definition
        ↓
Tool Definition 包含
Name
Description
Parameters
Schema
        ↓
Application 把 Tool Definition 发给 LLM
        ↓
LLM 阅读用户问题和 Tool Definition
        ↓
LLM 判断是否需要工具
        ↓
LLM 判断应该选哪个工具
        ↓
LLM 生成 Tool Call
```

这时：

```text
真正执行 Java 方法
```

还没有发生。

后面才会进入：

```text
Application 解析 Tool Call
        ↓
Dispatcher
        ↓
Java Method
```

---

## 三十二、本篇知识总结

### 32.1 一句话定义

**Tool Definition 是应用程序提供给模型的结构化工具说明，用来告诉模型某个工具叫什么、能做什么、需要什么参数，以及参数应遵循什么结构。**

### 32.2 四个核心组成部分

```text
Tool Definition
├── Name
├── Description
├── Parameters
└── Schema
```

### 32.3 各自解决的问题

```text
Name
↓
这个工具叫什么？

Description
↓
这个工具做什么、什么时候应该用？

Parameters
↓
调用这个工具需要哪些输入？

Schema
↓
这些输入应该是什么结构？
```

### 32.4 本篇最关键结论

```text
Java 方法存在
≠
模型知道它存在

必须通过 Tool Definition
把能力描述给模型。
```

以及：

```text
Tool Description
不是普通注释。

它会进入模型上下文，
直接影响模型如何理解和选择工具。
```

---

## 三十三、自测：确认自己是否真正理解

建议先不看正文，尝试自己回答。

### 问题 1

Tool Definition 是什么？为什么模型需要它？

### 问题 2

Java 代码里已经存在 `queryCampaign()` 方法，为什么模型仍然不知道自己可以调用它？

### 问题 3

Tool Definition 中的 Name、Description、Parameters、Schema 分别解决什么问题？

### 问题 4

为什么 `getData` 通常不是一个好的 Tool Name？

### 问题 5

为什么下面这个 Description 很差？

```text
queryCampaignMetrics：
查询数据。
```

### 问题 6

Tool Definition 和 Tool Call 有什么区别？它们的数据流方向分别是什么？

### 问题 7

Tool Description 为什么会直接影响模型选择工具？

### 问题 8

Structured Output 的 Schema 和 Tool Parameters Schema 分别描述什么？

### 问题 9

如果 `queryBudget` 的 Description 写成“查询 Campaign 实际消耗”，可能出现什么问题？

### 问题 10

为什么不应该把后端所有 Java Service 方法都直接暴露成 Tool？

---

## 三十四、完成标准

完成本 Node 的理论学习后，应当能够做到：

- [ ] 不看笔记，能够解释 Tool Definition 是什么。
- [ ] 能够解释为什么 Java 方法存在不等于模型知道这个 Tool。
- [ ] 能够说出 Tool Definition 的四个核心部分：Name、Description、Parameters、Schema。
- [ ] 能够解释 Name 和 Description 的职责区别。
- [ ] 能够解释为什么 Tool Description 会影响模型的工具选择。
- [ ] 能够区分 Tool Definition 和 Tool Call。
- [ ] 能够区分 Tool Parameters Schema 和 Structured Output Schema 的用途。
- [ ] 能够识别过于模糊、冲突或职责重叠的 Tool Definition。
- [ ] 能够理解 Tool Definition 本质上是一种 LLM 与 Application 之间的能力契约。

**达到以上标准，再进入 Node 024。**

---

## 三十五、与后续 Node 的边界

本篇只学习：

```text
如何向模型描述一个 Tool
```

暂时不深入：

```text
参数 required 怎么设计
enum 怎么设计
数字范围怎么限制
嵌套参数怎么写
```

这些属于：

```text
Node 024：参数 Schema
```

也暂时不实现：

```text
switch(toolName)
Tool Dispatcher
真实 Java Method 调用
Tool Result 回传
```

这些属于：

```text
Node 025：Tool Dispatcher
Node 026：第一次完整 Tool Call
```

所以本篇结束以后，我们应该形成的是：

> **“我知道应用程序应该怎样把工具能力描述给模型，以及模型为什么依赖这些描述进行工具选择。”**

而不是：

> “我已经实现了完整 Tool Calling。”

---

## 学习记录

- 理论阅读日期：待填写
- 自测情况：待填写
- 尚未理解的问题：待填写
- 是否达到完成标准：待确认
