# Node 024：Tool 参数 Schema 到底是什么？

> 所属阶段：Phase 2 / Tool Calling 与 Agent Loop  
> 笔记类型：理论篇  
> 学习定位：在已经掌握 JSON Schema 基础的前提下，进一步理解 Tool Calling 中参数 Schema 的作用，以及 `required`、`enum` 和常见 constraints 为什么会直接影响 Tool Call 的可靠性。  
> 本篇边界：不重新系统讲一遍 JSON Schema 标准；重点只放在“模型怎样按照 Tool 参数契约生成 arguments，以及 Application 为什么仍然需要继续校验”。  

---

## 一、本篇为什么存在？

在 Node 023 中，我们已经知道，一个 Tool Definition 通常会包含：

```text
Name
Description
Parameters
Schema
```

其中：

```text
Name
```

主要帮助模型知道：

> 这个工具叫什么？

```text
Description
```

主要帮助模型知道：

> 这个工具做什么、什么时候应该使用？

而到了：

```text
Parameters / Schema
```

我们要解决的问题变成：

> **如果模型决定调用这个工具，它应该生成什么参数？**

这就是 Node 024 的核心。

---

## 二、先回顾 queryCampaign

我们沿用路线里的例子：

```text
queryCampaign
```

假设 Java 里真正存在：

```java
public Campaign queryCampaign(Long campaignId) {
    return campaignService.getById(campaignId);
}
```

模型如果想请求这个工具，不能只返回：

```text
我要调用 queryCampaign
```

它还必须告诉 Application：

```text
我要查哪个 Campaign？
```

例如：

```json
{
  "campaignId": 123
}
```

于是 Tool Call 的核心就变成：

```text
Tool Name
+
Arguments
```

而参数 Schema 负责约束：

```text
Arguments 应该长什么样
```

---

# 三、Tool 参数 Schema 到底是什么？

可以先记住一句话：

> **Tool 参数 Schema 是 Application 向模型声明：如果你要调用这个 Tool，arguments 必须按照什么结构生成。**

例如：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer"
    }
  },
  "required": [
    "campaignId"
  ]
}
```

它表达的是：

```text
这个 Tool 的参数整体是 object

里面允许有：
campaignId

campaignId 的类型是：
integer

campaignId：
必须提供
```

因此：

```json
{
  "campaignId": 123
}
```

符合我们想要的参数结构。

而：

```json
{}
```

就缺少必要参数。

---

# 四、为什么 Node 014 学过 JSON Schema，现在还要再学一次？

因为 Node 014 学的是：

```text
JSON Schema 作为通用数据契约
```

当时重点是理解：

```text
type
properties
required
enum
array
items
object
additionalProperties
```

而 Node 024 的视角不同。

现在我们不是在问：

> JSON Schema 是什么？

而是在问：

> **JSON Schema 放进 Tool Definition 以后，到底怎样影响模型生成 Tool Call？**

这是两个层次。

可以理解为：

```text
Node 014
=
学会 Schema 语言

Node 024
=
把 Schema 用到 Tool Calling 参数契约中
```

---

# 五、Tool 参数 Schema 约束的不是最终回答

这一点必须分清。

假设用户说：

```text
帮我查一下 Campaign 123。
```

模型可能生成：

```json
{
  "name": "queryCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

这里：

```text
campaignId = 123
```

才是 Tool 参数 Schema 要约束的东西。

Schema 不是在约束最终用户看到的自然语言回答。

所以：

```text
Tool Parameters Schema
```

约束的是：

```text
Tool Call arguments
```

而不是：

```text
最终 Assistant Answer
```

---

# 六、完整数据流再看一次

```text
Application
    │
    │ 提供 Tool Definition
    │
    │ 其中包含参数 Schema
    ▼
   LLM
    │
    │ 根据 User Message
    │ 根据 Tool Description
    │ 根据参数 Schema
    ▼
生成 Tool Call
    │
    │ name
    │ arguments
    ▼
Application
```

例如：

```text
用户：
查询 Campaign 123
```

模型看到：

```text
Tool:
queryCampaign

Description:
根据 Campaign ID 查询 Campaign 基础信息

Parameters Schema:
campaignId 是必填 integer
```

于是模型更容易生成：

```json
{
  "campaignId": 123
}
```

---

# 七、没有 Schema 会发生什么？

假设 Application 只告诉模型：

```text
Tool:
queryCampaign

Description:
查询 Campaign 信息
```

却不描述参数结构。

模型可能猜：

```json
{
  "id": 123
}
```

也可能猜：

```json
{
  "campaign": 123
}
```

甚至可能返回：

```json
{
  "campaignId": "123"
}
```

或者：

```json
{
  "campaign_id": 123
}
```

从自然语言角度看：

```text
这些都“差不多能懂”
```

但对程序来说：

```text
差一个字段名
差一个类型
都可能直接解析失败
```

所以：

> **Schema 的价值，就是把模型可自由发挥的参数空间压缩成 Application 能稳定处理的结构。**

---

# 八、Schema 的第一层作用：字段名固定

Application 希望：

```json
{
  "campaignId": 123
}
```

那 Schema 中就应该明确：

```json
"campaignId"
```

而不是让模型自由决定：

```text
id
campaign
campaign_id
campaignNo
```

因为 Java 代码真正处理的可能就是：

```java
arguments.getLong("campaignId");
```

如果模型返回：

```json
{
  "id": 123
}
```

程序就拿不到：

```text
campaignId
```

---

# 九、Schema 的第二层作用：类型固定

例如：

```text
campaignId
```

我们希望是：

```text
integer
```

而不是：

```text
string
```

所以写：

```json
{
  "campaignId": {
    "type": "integer"
  }
}
```

模型就知道：

```json
{
  "campaignId": 123
}
```

比：

```json
{
  "campaignId": "123"
}
```

更符合契约。

---

# 十、为什么类型很重要？

Java 里可能会这样处理：

```java
Long campaignId = arguments.getLong("campaignId");
```

如果参数始终按照约定生成：

```json
{
  "campaignId": 123
}
```

处理很自然。

如果模型有时生成：

```json
{
  "campaignId": "123"
}
```

有时又生成：

```json
{
  "campaignId": 123
}
```

Application 就必须承担更多：

```text
类型兼容
转换
异常处理
```

所以参数 Schema 的一个核心作用就是：

> **减少 Tool Call 参数的形态漂移。**

---

# 十一、required：哪些参数必须存在？

假设：

```text
queryCampaign
```

没有 `campaignId` 就完全无法查询。

那么：

```text
campaignId
```

就是必填参数。

可以写：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer"
    }
  },
  "required": [
    "campaignId"
  ]
}
```

---

# 十二、properties 不等于 required

这个在 Node 014 学过，但在 Tool Calling 里尤其重要。

如果只写：

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
如果有 campaignId
它应该是 integer
```

但不一定表达：

```text
campaignId 必须存在
```

所以：

```text
properties
```

回答：

> 有哪些字段、字段是什么类型？

而：

```text
required
```

回答：

> 哪些字段不能缺？

---

# 十三、为什么 Tool 参数里 required 很重要？

因为 Tool Call 并不是模型最终回答。

Application 后面真的要拿参数执行代码。

例如：

```java
queryCampaign(campaignId);
```

如果模型返回：

```json
{}
```

Application 根本不知道要查哪个 Campaign。

这时候如果 Schema 明确：

```text
campaignId required
```

就能减少模型生成缺失参数的概率。

---

# 十四、required 不是“模型永远不会漏参数”的保证

这一点很重要。

即使 Schema 写了：

```json
"required": ["campaignId"]
```

也不能从工程角度直接推导：

```text
模型返回的 campaignId 永远存在
```

因为现实中还可能有：

```text
Provider 差异
模型能力差异
API 模式差异
兼容模式问题
解析错误
不支持严格 Schema
异常响应
```

所以：

> **Schema 是契约和约束，不等于 Application 可以放弃校验。**

后面 Application 仍然应该检查：

```text
campaignId 是否存在
类型是否正确
业务上是否合法
```

---

# 十五、什么时候一个参数应该 required？

判断标准很简单：

> **如果缺少这个参数，工具是否还能正确执行？**

例如：

```text
queryCampaign(campaignId)
```

没有 `campaignId`：

```text
无法执行
```

所以应该：

```text
required
```

---

# 十六、可选参数是什么？

假设我们有：

```text
queryCampaignMetrics
```

需要：

```text
campaignId
```

并且可以选择：

```text
days
```

例如：

```json
{
  "campaignId": 123,
  "days": 7
}
```

如果 `days` 不传，Application 默认：

```text
7 天
```

那么可以设计：

```text
campaignId
=
required

days
=
optional
```

例如：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer"
    },
    "days": {
      "type": "integer"
    }
  },
  "required": [
    "campaignId"
  ]
}
```

---

# 十七、可选参数不是“无关参数”

可选参数只是：

```text
允许缺省
```

不是：

```text
想传啥都行
```

例如：

```text
days
```

可选，并不意味着：

```json
{
  "foo": "bar"
}
```

也应该被接受。

这是两件事。

---

# 十八、enum：把自由字符串限制成有限选择

假设我们设计一个工具：

```text
queryCampaignMetrics
```

支持：

```text
TODAY
YESTERDAY
LAST_7_DAYS
LAST_30_DAYS
```

如果参数：

```text
dateRange
```

只是普通 string：

```json
{
  "dateRange": "最近几天"
}
```

从模型角度很自然。

但程序可能并不认识：

```text
最近几天
```

所以可以用：

```json
{
  "type": "string",
  "enum": [
    "TODAY",
    "YESTERDAY",
    "LAST_7_DAYS",
    "LAST_30_DAYS"
  ]
}
```

这样模型就知道：

> 如果要填这个字段，应该从这些固定值里选。

---

# 十九、为什么 enum 对 Tool Calling 特别有价值？

因为 Tool Call 参数最终要进入确定性程序。

例如 Application 可能直接：

```java
switch (dateRange) {
    case "TODAY" -> ...
    case "YESTERDAY" -> ...
    case "LAST_7_DAYS" -> ...
    case "LAST_30_DAYS" -> ...
}
```

如果模型自由生成：

```text
今天
今日
TODAY
today
当天
```

程序就很难稳定处理。

而 enum 可以把模型的输出空间限制为：

```text
TODAY
YESTERDAY
LAST_7_DAYS
LAST_30_DAYS
```

---

# 二十、enum 的核心意义

一句话：

> **当程序只接受有限几个离散值时，就应该考虑用 enum 把模型约束到这几个值里。**

典型字段包括：

```text
status
action
sortOrder
dateRange
riskLevel
channel
operationType
```

---

# 二十一、什么情况不适合 enum？

例如：

```text
campaignName
```

可能是任意业务名称：

```text
双十一促销
新品上线
Black Friday
品牌拉新
```

这显然不适合写成固定 enum。

再比如：

```text
reason
```

通常需要自然语言表达。

也不应该为了“结构稳定”强行限制成几个固定句子。

所以：

> **enum 适合有限集合，不适合开放文本。**

---

# 二十二、constraints 是什么？

路线里写了：

```text
constraints
```

这里可以理解成：

> **在 type 之外，对参数合法范围再加限制。**

例如：

```text
integer
```

只能说明：

```text
这是整数
```

但不能说明：

```text
必须大于 0
```

如果 Campaign ID 业务上必须为正整数，可以进一步约束：

```json
{
  "type": "integer",
  "minimum": 1
}
```

---

# 二十三、数值约束：minimum / maximum

例如：

```text
days
```

我们希望只能：

```text
1 到 30
```

可以写：

```json
{
  "type": "integer",
  "minimum": 1,
  "maximum": 30
}
```

于是：

```json
{
  "days": 7
}
```

合理。

而：

```json
{
  "days": 0
}
```

不符合契约。

```json
{
  "days": 3650
}
```

也不符合契约。

---

# 二十四、为什么数值范围很重要？

如果只写：

```text
days = integer
```

模型可能生成：

```json
{
  "days": 100000
}
```

从 JSON 类型上看：

```text
完全正确
```

但业务上可能毫无意义。

因此：

```text
类型正确
```

不等于：

```text
值合理
```

constraints 可以进一步表达：

```text
什么范围才合理
```

---

# 二十五、字符串约束：minLength / maxLength

假设 Tool：

```text
searchCampaignByName
```

参数：

```text
keyword
```

可以约束：

```json
{
  "type": "string",
  "minLength": 1,
  "maxLength": 100
}
```

这意味着：

```text
不能为空字符串
不能无限长
```

---

# 二十六、为什么长度约束也属于工程可靠性？

如果完全不限制：

```text
keyword
```

模型理论上可能生成非常长的文本。

然后 Application 可能把这段内容直接：

```text
传给数据库查询
传给第三方 API
写日志
```

就会增加：

```text
性能风险
接口失败风险
日志污染
业务异常
```

所以合理的 Schema constraints 可以减少不必要输入。

---

# 二十七、array 参数也可以加 constraints

例如：

```text
campaignIds
```

是数组：

```json
{
  "type": "array",
  "items": {
    "type": "integer"
  }
}
```

如果业务只允许一次最多查 20 个：

```json
{
  "type": "array",
  "items": {
    "type": "integer"
  },
  "minItems": 1,
  "maxItems": 20
}
```

这就进一步表达：

```text
至少 1 个
最多 20 个
```

---

# 二十八、additionalProperties：是否允许模型多传字段？

假设我们只允许：

```json
{
  "campaignId": 123
}
```

但模型返回：

```json
{
  "campaignId": 123,
  "accountId": 456,
  "debug": true
}
```

这些额外字段是否允许？

可以通过：

```json
"additionalProperties": false
```

表达：

> 除了 properties 中声明的字段，不接受其他字段。

---

# 二十九、为什么 Tool 参数里 additionalProperties 很有价值？

因为模型有时会“好心”多生成字段。

例如你只定义：

```text
campaignId
```

模型可能根据上下文额外补：

```text
campaignName
accountId
reason
```

对自然语言来说：

```text
信息越多好像越好
```

但对 Tool Call 来说：

```text
多余字段可能破坏契约
```

所以很多 Tool 参数 Schema 会倾向于：

```json
"additionalProperties": false
```

来减少结构漂移。

---

# 三十、但不要机械地认为所有 Provider 都完全一样

Tool Calling 的 Schema 支持程度可能受：

```text
Provider
Model
API 版本
兼容协议
strict 模式
```

影响。

所以这里要建立一个工程意识：

> **“JSON Schema 标准支持什么”与“某个模型 API 实际支持什么”不是永远完全相同。**

学习阶段先掌握核心思想。

以后真正接 Provider 时，再以：

```text
Provider 官方文档
实际测试
```

为准。

---

# 三十一、一个更完整的 queryCampaign 示例

可以把：

```text
queryCampaign
```

定义成：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer",
      "description": "要查询的 Campaign ID",
      "minimum": 1
    }
  },
  "required": [
    "campaignId"
  ],
  "additionalProperties": false
}
```

这份 Schema 表达：

```text
参数整体必须是 object

必须有 campaignId

campaignId 必须是 integer

campaignId 必须 >= 1

不能出现未定义字段
```

---

# 三十二、把它放回 Tool Definition

概念上：

```json
{
  "name": "queryCampaign",
  "description": "根据 Campaign ID 查询广告计划的基础信息。",
  "parameters": {
    "type": "object",
    "properties": {
      "campaignId": {
        "type": "integer",
        "description": "要查询的 Campaign ID",
        "minimum": 1
      }
    },
    "required": [
      "campaignId"
    ],
    "additionalProperties": false
  }
}
```

重点不是背 JSON。

重点是理解：

```text
Description
决定：
模型为什么选这个工具

Parameters Schema
决定：
选中之后 arguments 应该怎么生成
```

---

# 三十三、Description 和参数 Schema 是两层不同约束

假设：

```text
Description
```

写得很好：

```text
查询 Campaign 基础信息
```

模型正确选中了：

```text
queryCampaign
```

但 Schema 写得很差：

```json
{
  "type": "object"
}
```

那模型仍然可能生成：

```json
{
  "id": "123"
}
```

所以：

```text
工具选择正确
```

不代表：

```text
参数一定正确
```

---

# 三十四、反过来也一样

如果 Schema 很严格：

```text
campaignId required integer
```

但 Tool Description 很模糊：

```text
查询数据
```

模型可能：

```text
选错 Tool
```

然后即使参数完全符合 Schema，也没有意义。

所以：

```text
Tool Calling 可靠性
=
Tool 选择质量
+
参数生成质量
```

而：

```text
Description
```

主要影响前者。

```text
Parameters Schema
```

主要影响后者。

---

# 三十五、Schema 合法 ≠ 业务合法

这是 Node 024 最重要的工程结论之一。

假设 Schema：

```json
{
  "campaignId": {
    "type": "integer",
    "minimum": 1
  }
}
```

模型返回：

```json
{
  "campaignId": 999999999
}
```

它可能：

```text
完全符合 Schema
```

但数据库里根本没有这个 Campaign。

所以：

```text
Schema Validation
```

只能说明：

```text
结构和基础约束合法
```

不能说明：

```text
业务对象真实存在
```

---

# 三十六、Schema Validation 和 Business Validation

这两个必须分开。

## Schema Validation

关注：

```text
字段是否存在
类型是否正确
enum 是否合法
数值范围是否符合
字符串长度是否符合
结构是否正确
```

## Business Validation

关注：

```text
Campaign 是否存在
Campaign 是否属于当前用户
当前用户是否有权限
Campaign 状态是否允许操作
预算是否符合公司规则
```

---

# 三十七、一个具体例子

Tool：

```text
pauseCampaign
```

参数：

```json
{
  "campaignId": 123
}
```

Schema 可以验证：

```text
campaignId 存在
campaignId 是 integer
campaignId >= 1
```

但 Schema 无法验证：

```text
Campaign 123 是否真的存在

当前登录用户是否拥有它

它是否已经暂停

当前用户是否有暂停权限
```

这些必须由 Application 负责。

---

# 三十八、所以绝对不能这样想

错误：

```text
有 JSON Schema 了
所以模型参数可以直接执行
```

正确：

```text
Tool Call
   ↓
Schema / 基础结构校验
   ↓
业务校验
   ↓
权限校验
   ↓
必要时用户确认
   ↓
真正执行
```

后面的权限、Human Approval 等内容会在后续 Node 专门学习。

---

# 三十九、模型参数永远应该被当作“不可信输入”

这是非常重要的安全和工程意识。

模型生成：

```json
{
  "campaignId": 123
}
```

不要因为它来自：

```text
LLM
```

就自动认为可信。

从 Application 角度，它和很多外部输入一样：

```text
需要校验
```

可以先记住：

> **LLM-generated arguments 也是外部输入。**

---

# 四十、Tool 参数 Schema 不是安全边界的全部

Schema 能限制：

```text
结构
类型
枚举
范围
```

但它不能独立承担：

```text
身份认证
权限控制
租户隔离
敏感操作确认
数据授权
业务规则
```

所以：

```text
Schema
=
第一层参数契约
```

不是：

```text
完整安全系统
```

---

# 四十一、为什么 Schema 写得太松不好？

例如：

```json
{
  "type": "object"
}
```

它几乎没有告诉模型：

```text
需要哪些参数
字段叫什么
字段是什么类型
```

模型自由度太高。

Application 解析成本也会变高。

---

# 四十二、一个“太松”的例子

Tool：

```text
queryCampaignMetrics
```

Schema：

```json
{
  "type": "object"
}
```

模型可能输出：

```json
{
  "id": "123",
  "range": "最近一阵子"
}
```

也可能：

```json
{
  "campaign": 123,
  "days": "seven"
}
```

也可能：

```json
{
  "campaignId": 123,
  "from": "some time ago"
}
```

程序很难稳定处理。

---

# 四十三、为什么 Schema 写得太死也不好？

反过来，如果业务本身需要灵活输入，却被过度限制，也会出问题。

例如：

```text
searchCampaign
```

本来支持用户输入任意关键词。

如果你把：

```text
keyword
```

写成：

```json
{
  "type": "string",
  "enum": [
    "Java",
    "Redis",
    "MySQL"
  ]
}
```

那显然错误。

因为业务能力并不是只有这三个关键词。

---

# 四十四、Schema 设计本质上是“真实业务契约”的表达

最好的思路不是：

```text
越严格越好
```

也不是：

```text
越宽松越好
```

而是：

> **Schema 应该尽可能准确地表达真实工具能力。**

真实业务只接受：

```text
PAUSED / ENABLED
```

那就用 enum。

真实业务允许任意搜索关键词：

```text
那就不要乱用 enum
```

---

# 四十五、约束越强，模型输出空间越小

例如：

```text
string
```

允许的值非常多。

而：

```text
enum
```

只允许有限几个值。

所以从模型生成角度：

```text
约束越明确
↓
可选输出空间越小
↓
参数更容易稳定
```

但前提是：

```text
约束必须符合真实业务
```

---

# 四十六、参数 Description 也很重要

Schema 里的字段本身也可以有 Description。

例如：

```json
{
  "campaignId": {
    "type": "integer",
    "description": "要查询的广告 Campaign ID"
  }
}
```

为什么有价值？

因为：

```text
campaignId
```

虽然已经比较清楚，但模型仍然可以通过 Description 获得更具体语义。

---

# 四十七、字段名和字段 Description 的配合

不好：

```json
{
  "id": {
    "type": "integer",
    "description": "ID"
  }
}
```

更好：

```json
{
  "campaignId": {
    "type": "integer",
    "description": "要查询的 Campaign ID"
  }
}
```

为什么？

因为：

```text
字段名
+
字段描述
```

共同帮助模型理解参数。

---

# 四十八、不要让字段 Description 和字段名冲突

例如：

```json
{
  "campaignId": {
    "type": "integer",
    "description": "广告账户 ID"
  }
}
```

这会产生矛盾。

模型会看到：

```text
字段名说 Campaign ID

Description 说 Account ID
```

这种设计很差。

---

# 四十九、参数名应该尽量业务语义明确

比较：

```text
id
```

和：

```text
campaignId
```

比较：

```text
type
```

和：

```text
campaignStatus
```

比较：

```text
value
```

和：

```text
dailyBudget
```

通常后者更有语义。

Tool 参数不是只给 Java 读。

它还要给：

```text
模型理解
```

---

# 五十、一个包含 enum 的例子

Tool：

```text
queryCampaignByStatus
```

参数：

```json
{
  "status": "PAUSED"
}
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "status": {
      "type": "string",
      "enum": [
        "ENABLED",
        "PAUSED"
      ],
      "description": "要查询的 Campaign 状态"
    }
  },
  "required": [
    "status"
  ],
  "additionalProperties": false
}
```

模型就知道：

```text
status 只能从 ENABLED / PAUSED 里选
```

---

# 五十一、一个包含数值 constraints 的例子

Tool：

```text
queryTopCampaigns
```

参数：

```json
{
  "limit": 10
}
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "limit": {
      "type": "integer",
      "minimum": 1,
      "maximum": 100,
      "description": "最多返回多少个 Campaign"
    }
  },
  "required": [
    "limit"
  ],
  "additionalProperties": false
}
```

它表达：

```text
limit 必须是整数
最少 1
最多 100
```

---

# 五十二、为什么不能只靠 Prompt 说“limit 不要超过 100”？

你当然可以在 Prompt 里写：

```text
limit 不要超过 100
```

但 Schema 更适合表达：

```text
机器可读的结构规则
```

例如：

```json
"maximum": 100
```

这比自然语言：

```text
请尽量不要超过 100
```

更像正式接口契约。

所以：

```text
Prompt
```

和：

```text
Schema
```

不是互相替代。

---

# 五十三、Prompt 和 Schema 分别负责什么？

可以简单记：

```text
Prompt / Tool Description
=
解释任务和语义

Schema
=
定义参数结构和约束
```

例如：

```text
Description：
查询最近一段时间 Campaign 的表现数据。

Schema：
campaignId required integer
days integer 1~30
```

两者共同工作。

---

# 五十四、Schema 不能理解用户意图

例如用户说：

```text
查一下最近的数据
```

Schema 本身不会决定：

```text
最近 = 7 天
```

这属于：

```text
语义理解
业务默认值
Prompt
Tool Description
Application 规则
```

的范畴。

Schema 只负责：

```text
days 如果出现
应该是什么结构和范围
```

---

# 五十五、默认值应该由谁负责？

这是一个容易混乱的问题。

假设：

```text
days
```

没传时默认：

```text
7
```

你可以把这个事实：

```text
写进 Tool / 参数 Description
```

例如：

```text
days：
查询最近多少天，不传时默认 7 天。
```

真正执行默认逻辑：

```java
if (days == null) {
    days = 7;
}
```

仍然是 Application 的责任。

不要把：

```text
Schema 描述默认行为
```

误解成：

```text
模型一定会主动补默认值
```

---

# 五十六、可选参数和默认值经常一起出现

例如：

```text
campaignId
```

必须有。

```text
days
```

可以省略。

那么：

```json
"required": [
  "campaignId"
]
```

就够了。

Application：

```java
Integer days = arguments.getInteger("days");

if (days == null) {
    days = 7;
}
```

这样职责比较清楚。

---

# 五十七、什么时候不应该让模型决定默认值？

如果默认值属于：

```text
稳定业务规则
```

通常更适合 Application 决定。

例如：

```text
没传 days
默认 7
```

这属于确定性逻辑。

没有必要让 LLM 每次自己猜：

```text
7？
14？
30？
```

所以：

> **能由确定性代码稳定解决的规则，不要无意义地交给模型。**

---

# 五十八、Tool Schema 也会影响 Token 和 Context

Tool Definition 是模型请求的一部分。

如果你注册很多工具，每个工具的 Schema 又特别庞大：

```text
Context
```

就会变大。

因此：

```text
Tool 数量
+
Tool Description
+
参数 Schema
```

都会占用上下文。

---

# 五十九、为什么不能给所有 Tool 写巨大 Schema？

因为会增加：

```text
Token
成本
延迟
模型理解负担
```

如果 Tool 有几十个，每个 Schema 又嵌套几十层：

```text
模型每轮都需要阅读大量工具定义
```

这会成为 Agent 工程中的实际问题。

后面我们还会学习：

```text
Tool Search
Tool Context
```

本质上都和这个问题有关。

---

# 六十、Schema 应该“够用而清晰”

不是：

```text
能塞多少约束就塞多少
```

而是：

```text
真实业务需要什么
就准确表达什么
```

例如 `campaignId`：

```text
integer
minimum 1
required
```

可能已经够用。

没有必要为了显得复杂再加一堆无关规则。

---

# 六十一、复杂嵌套参数要谨慎

假设一个 Tool 参数长这样：

```json
{
  "campaign": {
    "account": {
      "owner": {
        "region": {
          "code": "..."
        }
      }
    }
  }
}
```

理论上 Schema 可以描述。

但你要问：

> **这个工具真的需要这么复杂的输入吗？**

如果 Tool 参数过于复杂：

```text
模型生成难度上升
校验难度上升
调试难度上升
```

有时候更合理的设计是：

```text
简化 Tool 输入
```

---

# 六十二、Tool 设计和 Schema 设计是一起的

如果一个 Tool 的参数 Schema 很难写清楚，可能不只是 Schema 的问题。

也可能是：

```text
Tool 设计本身职责太复杂
```

例如：

```text
manageCampaignEverything
```

参数可能同时控制：

```text
查询
暂停
恢复
改预算
改名称
改状态
```

Schema 会变得非常复杂。

这时也许应该拆成：

```text
queryCampaign
pauseCampaign
resumeCampaign
updateCampaignBudget
```

---

# 六十三、Schema 复杂可能是在提醒你 Tool 太胖

这是一个很实用的工程判断。

如果你发现：

```text
几十个参数
大量 oneOf
很多互斥字段
很多 action 分支
```

先别急着继续堆 Schema。

先问：

> **这个 Tool 是否承担了太多职责？**

好的 Tool 往往：

```text
能力清晰
参数相对明确
边界明确
```

---

# 六十四、一个反例：万能 Tool

例如：

```text
manageCampaign
```

参数：

```json
{
  "action": "...",
  "campaignId": 123,
  "budget": 100,
  "name": "...",
  "status": "...",
  "dateRange": "...",
  "metric": "..."
}
```

然后不同 action 需要不同字段。

这会导致：

```text
Schema 非常复杂
模型容易传错组合
Application 分支很多
测试困难
```

---

# 六十五、拆分之后

可以变成：

```text
queryCampaign
queryCampaignMetrics
updateCampaignBudget
pauseCampaign
resumeCampaign
```

每个 Tool 的参数 Schema 更简单。

例如：

```text
pauseCampaign
```

只需要：

```json
{
  "campaignId": 123
}
```

这往往更容易让模型正确调用。

---

# 六十六、Schema 设计其实会影响模型行为

虽然 Schema 本质是结构契约，但它也是模型上下文的一部分。

模型会看到：

```text
字段名
字段类型
enum
description
required
```

这些信息会帮助模型推理。

例如用户说：

```text
查最近 7 天
```

模型看到：

```text
days: integer
minimum: 1
maximum: 30
```

就更容易生成：

```json
{
  "days": 7
}
```

---

# 六十七、Schema 是“给模型看”也是“给程序用”

它同时服务两个方向。

对模型：

```text
告诉模型参数应该怎么生成
```

对 Application：

```text
告诉程序预期参数契约是什么
```

因此 Schema 是连接：

```text
概率模型
```

和：

```text
确定性程序
```

的一层桥梁。

---

# 六十八、这和 Java DTO 很像，但不是一回事

例如 Java：

```java
class QueryCampaignArgs {
    private Long campaignId;
}
```

这定义了 Java 侧类型。

Schema：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer"
    }
  },
  "required": [
    "campaignId"
  ]
}
```

定义的是模型 API 侧参数契约。

两者可以映射：

```text
Tool Schema
     ↓
Tool Call arguments
     ↓
JSON Parse
     ↓
Java Args DTO
```

但：

```text
Tool Schema
≠
Java DTO
```

---

# 六十九、未来可以把 arguments 反序列化成 Java DTO

例如：

```json
{
  "campaignId": 123
}
```

可以转成：

```java
class QueryCampaignArgs {

    private Long campaignId;

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }
}
```

之后：

```java
queryCampaign(args.getCampaignId());
```

但这个实现细节不属于本 Node 的重点。

后面进入 Tool Dispatcher / 完整 Tool Call 时再做。

---

# 七十、为什么 Java 类型校验仍然需要存在？

即使模型 API 支持 Schema，Application 仍然应该有自己的防线。

例如：

```java
if (args.getCampaignId() == null) {
    throw new IllegalArgumentException("campaignId 不能为空");
}
```

为什么？

因为系统不能把可靠性完全押在：

```text
模型一定遵守 Schema
```

上面。

---

# 七十一、Tool Schema 是契约，不是信任来源

可以这样理解：

```text
Schema
=
我们希望双方遵守的契约

Validation
=
我们真正检查契约有没有被遵守
```

这两个不是一个东西。

---

# 七十二、Schema 能减少错误，不等于消灭错误

合理的 Schema 可以减少：

```text
字段名错误
类型错误
缺少必填参数
非法 enum
超范围数值
多余字段
```

但不能保证：

```text
用户意图理解正确
Tool 选择正确
业务 ID 正确
权限正确
工具执行成功
```

---

# 七十三、Tool Calling 的错误可以分层看

以后排错时，可以开始形成这种思维：

```text
第一层：
Tool Selection Error
选错工具

第二层：
Argument Generation Error
参数生成错

第三层：
Schema Validation Error
参数违反结构契约

第四层：
Business Validation Error
结构正确但业务不合法

第五层：
Tool Execution Error
真正执行失败
```

Node 024 主要聚焦：

```text
第二层和第三层
```

---

# 七十四、一个错误案例：缺少 required

Schema：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer"
    }
  },
  "required": [
    "campaignId"
  ]
}
```

模型却返回：

```json
{}
```

这属于：

```text
Missing Required Field
```

---

# 七十五、一个错误案例：类型错误

Schema：

```text
campaignId = integer
```

模型返回：

```json
{
  "campaignId": "abc"
}
```

属于：

```text
Wrong Type
```

---

# 七十六、一个错误案例：enum 非法

Schema：

```json
{
  "status": {
    "type": "string",
    "enum": [
      "ENABLED",
      "PAUSED"
    ]
  }
}
```

模型返回：

```json
{
  "status": "RUNNING"
}
```

属于：

```text
Invalid Enum
```

---

# 七十七、一个错误案例：数值越界

Schema：

```text
days
minimum 1
maximum 30
```

模型返回：

```json
{
  "days": 365
}
```

属于：

```text
Constraint Violation
```

---

# 七十八、一个错误案例：多余字段

Schema：

```json
{
  "additionalProperties": false
}
```

只声明：

```text
campaignId
```

模型返回：

```json
{
  "campaignId": 123,
  "accountId": 456
}
```

属于：

```text
Unexpected Property
```

---

# 七十九、为什么这种错误分类很有价值？

因为以后不能只说：

```text
Tool Call 错了
```

而应该具体判断：

```text
选错 Tool？
参数缺失？
参数类型错？
enum 错？
范围错？
业务不存在？
执行失败？
```

这种分层思维会直接影响：

```text
日志
错误处理
Retry
Evaluation
调试
```

---

# 八十、参数 Schema 设计的一个基本顺序

以后拿到一个 Tool，可以按这个顺序想：

```text
1. Tool 真正需要哪些输入？

2. 每个参数叫什么？

3. 每个参数是什么类型？

4. 哪些参数必填？

5. 哪些参数只能取有限值？

6. 数值 / 字符串 / 数组有没有合理范围？

7. 是否允许额外字段？

8. Schema 是否和真实业务实现一致？
```

---

# 八十一、先设计业务，再写 Schema

不要反过来：

```text
我先研究 Schema 能写什么
然后硬塞进 Tool
```

正确流程更像：

```text
先明确 Tool 能力
      ↓
明确真实参数需求
      ↓
再用 Schema 表达
```

Schema 是：

```text
业务契约的表示方式
```

不是业务本身。

---

# 八十二、练习：queryCampaign 第一版

目标 Tool：

```text
queryCampaign
```

要求：

```text
campaignId
integer
必须存在
必须大于等于 1
不允许额外字段
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer",
      "minimum": 1,
      "description": "要查询的 Campaign ID"
    }
  },
  "required": [
    "campaignId"
  ],
  "additionalProperties": false
}
```

---

# 八十三、练习：queryCampaignMetrics

需求：

```text
campaignId
必填 integer >= 1

dateRange
必填
只能：
TODAY
YESTERDAY
LAST_7_DAYS
LAST_30_DAYS
```

可以写：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer",
      "minimum": 1,
      "description": "要查询的 Campaign ID"
    },
    "dateRange": {
      "type": "string",
      "enum": [
        "TODAY",
        "YESTERDAY",
        "LAST_7_DAYS",
        "LAST_30_DAYS"
      ],
      "description": "查询表现数据的时间范围"
    }
  },
  "required": [
    "campaignId",
    "dateRange"
  ],
  "additionalProperties": false
}
```

---

# 八十四、练习：queryTopCampaigns

需求：

```text
limit
integer
1~100
必填

status
可选
ENABLED / PAUSED
```

可以写：

```json
{
  "type": "object",
  "properties": {
    "limit": {
      "type": "integer",
      "minimum": 1,
      "maximum": 100,
      "description": "最多返回多少个 Campaign"
    },
    "status": {
      "type": "string",
      "enum": [
        "ENABLED",
        "PAUSED"
      ],
      "description": "只返回指定状态的 Campaign；不传则不过滤状态"
    }
  },
  "required": [
    "limit"
  ],
  "additionalProperties": false
}
```

---

# 八十五、注意：enum 值应该和程序保持一致

如果 Java 里定义：

```java
enum CampaignStatus {
    ENABLED,
    PAUSED
}
```

Schema 最好也使用：

```text
ENABLED
PAUSED
```

而不是：

```text
ACTIVE
STOPPED
```

否则 Application 还要额外做：

```text
映射转换
```

这增加不必要复杂度。

---

# 八十六、Schema 和 Java Enum 可以形成明确映射

例如：

```json
{
  "status": "PAUSED"
}
```

可以直接映射：

```java
CampaignStatus.valueOf("PAUSED");
```

这种一致性能够降低：

```text
转换逻辑
错误概率
维护成本
```

---

# 八十七、但不要因为 Java Enum 存在就自动暴露全部值

假设 Java Enum：

```java
enum CampaignStatus {
    ENABLED,
    PAUSED,
    DELETED,
    ARCHIVED,
    INTERNAL_ERROR
}
```

Tool 业务可能只允许模型查询：

```text
ENABLED
PAUSED
```

那 Tool Schema 就不应该机械暴露所有内部值。

因为：

> **Tool Schema 表达的是 Tool 对模型开放的契约，不一定等于内部系统所有状态。**

---

# 八十八、Tool Schema 同时也是能力暴露边界

这个概念很重要。

例如内部系统支持：

```text
forceDelete = true
```

但你不希望模型能够控制这个参数。

那么就不要把：

```text
forceDelete
```

放进 Tool Schema。

模型不知道这个参数存在，就不会正常生成它作为正式 Tool 参数。

所以：

> **Tool Definition 和 Schema 也参与定义 Agent 能看到什么能力。**

---

# 八十九、不要把内部实现参数全部暴露给模型

例如 Java 方法内部可能有：

```java
queryCampaign(
    Long campaignId,
    boolean useCache,
    int retryCount,
    String traceId
)
```

这不代表 Tool Schema 也应该包含：

```text
useCache
retryCount
traceId
```

这些可能属于：

```text
Application 内部控制参数
```

而不是用户语义参数。

Tool Schema 应该面向：

```text
模型需要决定的业务输入
```

---

# 九十、这是一个非常重要的分层

Java 方法的所有参数：

```text
不一定全部来自 LLM
```

有些可能来自：

```text
当前登录用户
Application 配置
Context
服务器生成的 traceId
权限系统
默认值
```

例如：

```java
queryCampaign(Long campaignId, Long currentUserId)
```

Tool Schema 可能只暴露：

```text
campaignId
```

而：

```text
currentUserId
```

由 Application 从当前用户会话注入。

---

# 九十一、为什么不能让模型自己传 currentUserId？

因为这属于：

```text
安全上下文
```

如果让模型决定：

```json
{
  "currentUserId": 999
}
```

那就是错误的能力边界。

Application 应该从可信上下文获取：

```text
当前用户身份
```

而不是信任模型生成。

这个思想以后学：

```text
Tool Context
权限
Human Approval
```

时还会继续出现。

---

# 九十二、所以 Tool 参数要区分两类输入

可以先建立：

```text
LLM Arguments
```

和：

```text
Application Context
```

两种概念。

## LLM Arguments

模型可以根据用户意图生成：

```text
campaignId
dateRange
limit
```

## Application Context

服务器自己掌握：

```text
currentUserId
tenantId
permissions
traceId
requestId
```

不要把两者混在一起。

---

# 九十三、Schema 不是越详细越专业

一个 Schema 是否专业，看的是：

```text
是否准确表达真实契约
```

不是：

```text
有多少关键字
有多少层嵌套
有多复杂
```

最简单的 Tool：

```text
getCurrentTime
```

甚至可能完全不需要业务参数。

---

# 九十四、无参数 Tool 怎么办？

例如：

```text
getCurrentTime
```

不需要模型传任何参数。

参数 Schema 可以概念上表达成：

```json
{
  "type": "object",
  "properties": {},
  "additionalProperties": false
}
```

意思是：

```text
参数仍然是 object
但没有业务字段
```

---

# 九十五、不要为了“必须有参数”硬造字段

例如不要设计：

```json
{
  "confirm": true
}
```

只是为了让 Tool 看起来有参数。

如果工具本来不需要输入：

```text
就让它没有业务参数
```

真实契约比形式更重要。

---

# 九十六、Schema 能不能防 Prompt Injection？

不能直接这么理解。

Schema 可以限制：

```text
Tool arguments 的结构
```

但 Prompt Injection 涉及：

```text
模型如何理解恶意输入
工具调用策略
权限边界
上下文污染
```

这是更大的安全问题。

所以不要把：

```text
Schema 约束
```

误解成：

```text
Agent 安全已经解决
```

---

# 九十七、Schema 能不能防止危险操作？

只能提供很有限的帮助。

比如：

```text
amount <= 1000
```

可以限制数值范围。

但是否允许：

```text
转账
删数据
暂停 Campaign
```

仍然需要：

```text
权限
策略
Human Approval
业务逻辑
```

来控制。

---

# 九十八、Schema 和权限是不同层

```text
Schema
=
这个参数格式是否允许

Permission
=
这个用户是否允许做这个动作
```

例如：

```json
{
  "campaignId": 123
}
```

完全符合 Schema。

但用户可能没有权限暂停 Campaign 123。

所以：

```text
Schema 正确
≠
授权正确
```

---

# 九十九、Schema 和幂等性也不是一回事

以后会学：

```text
Idempotency
```

例如模型重复调用：

```text
pauseCampaign(123)
```

Schema 可以保证：

```text
campaignId 格式正确
```

但不能解决：

```text
重复执行会发生什么
```

这是工具执行层的问题。

---

# 一百、Node 024 真正要建立的是哪种思维？

不是：

```text
背 minimum / maximum
```

而是：

> **模型生成的 arguments 不能是一坨自由 JSON，它应该遵守明确的工具参数契约。**

这个契约要尽可能准确表达：

```text
字段
类型
必填性
有限取值
合理范围
额外字段策略
```

---

# 一百零一、Tool 参数 Schema 的核心目标

可以总结成四点：

```text
减少歧义
减少自由度
提高可解析性
提高可验证性
```

---

# 一百零二、减少歧义

例如：

```text
id
```

变成：

```text
campaignId
```

---

# 一百零三、减少自由度

例如：

```text
status: string
```

变成：

```text
status:
ENABLED / PAUSED
```

---

# 一百零四、提高可解析性

例如：

```text
campaignId:
integer
```

让 Application 不用处理：

```text
"123"
123
"Campaign-123"
```

各种随机形态。

---

# 一百零五、提高可验证性

例如：

```text
limit:
1 ~ 100
```

Application 能很明确判断：

```text
101
```

不符合契约。

---

# 一百零六、Schema 不能替代错误处理

即使设计再好，Tool Calling 仍然可能失败。

所以后续还要学习：

```text
Argument Validation
Tool Error
Tool Result
```

Node 024 只是建立：

```text
参数契约
```

不是一次解决所有失败问题。

---

# 一百零七、和后续 Node 的关系

Node 024：

```text
参数应该长什么样？
```

Node 025：

```text
Application 收到 name 后，
怎么找到真正 Java Tool？
```

Node 026：

```text
怎么把模型请求
→ Java 执行
→ Tool Result
→ 再次模型请求
完整串起来？
```

Node 029：

```text
arguments 到底怎么做正式校验？
```

所以现在不要把：

```text
Schema Definition
```

和：

```text
Runtime Validation
```

全部提前揉在一起。

---

# 一百零八、Schema Definition 和 Runtime Validation 的区别

## Schema Definition

告诉模型：

```text
你应该生成什么
```

## Runtime Validation

Application 真正检查：

```text
你生成的东西是否合法
```

二者缺一不可。

---

# 一百零九、一个完整参数生命周期

```text
Application 定义 Schema
        ↓
发送给 LLM
        ↓
LLM 生成 arguments
        ↓
Application 解析 arguments
        ↓
Schema / 基础参数校验
        ↓
业务校验
        ↓
权限校验
        ↓
执行 Tool
```

Node 024 主要是第一步。

---

# 一百一十、为什么 Node 024 不需要立刻写复杂 Java？

因为这一个 Node 的核心仍然是：

```text
参数契约设计
```

如果现在立刻把注意力放到：

```text
反射
Dispatcher
注解
Method.invoke
Tool Registry
```

就会提前进入 Node 025。

我们的路线是逐层建立：

```text
先知道工具是什么
↓
再知道工具怎么描述
↓
再知道参数怎么约束
↓
再知道 Application 怎么分发
```

这个顺序是有意义的。

---

# 一百一十一、最小手写练习 1

请自己尝试设计：

```text
queryCampaign
```

要求：

```text
campaignId
integer
必填
>= 1
禁止额外字段
```

目标：

```json
{
  "campaignId": 123
}
```

---

# 一百一十二、参考答案 1

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer",
      "minimum": 1,
      "description": "要查询的 Campaign ID"
    }
  },
  "required": [
    "campaignId"
  ],
  "additionalProperties": false
}
```

---

# 一百一十三、最小手写练习 2

Tool：

```text
queryCampaignMetrics
```

要求：

```text
campaignId
integer
必填
>= 1

dateRange
string
必填
只能：
TODAY
YESTERDAY
LAST_7_DAYS
LAST_30_DAYS
```

你应该能够独立写出 Schema。

---

# 一百一十四、最小手写练习 3

Tool：

```text
searchCampaign
```

要求：

```text
keyword
string
必填
长度 1~100

limit
integer
可选
1~50

禁止额外字段
```

重点检查：

```text
keyword 是否 required

limit 是否没有被错误地放进 required

minLength / maxLength

minimum / maximum
```

---

# 一百一十五、错误判断练习 1

Schema：

```text
campaignId:
integer
required
minimum 1
```

参数：

```json
{
  "campaignId": "123"
}
```

问题：

```text
违反 type
```

---

# 一百一十六、错误判断练习 2

Schema：

```text
status:
enum ENABLED / PAUSED
```

参数：

```json
{
  "status": "RUNNING"
}
```

问题：

```text
违反 enum
```

---

# 一百一十七、错误判断练习 3

Schema：

```text
limit:
integer
1~100
```

参数：

```json
{
  "limit": 500
}
```

问题：

```text
违反 maximum
```

---

# 一百一十八、错误判断练习 4

Schema：

```text
campaignId required
```

参数：

```json
{}
```

问题：

```text
缺少 required 字段
```

---

# 一百一十九、错误判断练习 5

Schema：

```text
只允许 campaignId
additionalProperties = false
```

参数：

```json
{
  "campaignId": 123,
  "debug": true
}
```

问题：

```text
出现未声明字段
```

---

# 一百二十、Schema 合法但业务非法的练习

Schema：

```text
campaignId:
integer
minimum 1
```

参数：

```json
{
  "campaignId": 999999
}
```

如果数据库里没有 Campaign 999999：

```text
Schema Validation：
通过

Business Validation：
失败
```

这是必须会区分的。

---

# 一百二十一、本篇必须记住的 15 个结论

1. Tool 参数 Schema 约束的是 Tool Call 的 `arguments`。  
2. Tool Description 主要帮助模型选择工具，参数 Schema 主要帮助模型生成正确参数。  
3. `properties` 定义参数字段和字段结构。  
4. `required` 定义调用工具时哪些字段不能缺。  
5. `enum` 适合有限离散值，不适合开放文本。  
6. `minimum / maximum` 可以限制数值范围。  
7. `minLength / maxLength` 可以限制字符串长度。  
8. `minItems / maxItems` 可以限制数组大小。  
9. `additionalProperties: false` 可以减少模型多生成未声明字段。  
10. Schema 越严格不一定越好，必须匹配真实业务能力。  
11. Schema 写得太松会增加参数漂移和解析成本。  
12. Schema 合法不代表业务合法。  
13. LLM 生成的 arguments 仍然属于不可信外部输入，Application 必须校验。  
14. Tool Schema 不应该暴露服务器内部、安全上下文或模型无需决定的参数。  
15. Schema Definition 与 Runtime Validation 是两层不同责任。  

---

# 一百二十二、自测

建议先不看正文，自己回答。

### 问题 1

Tool 参数 Schema 约束的到底是什么？

### 问题 2

为什么有了 `properties` 还需要 `required`？

### 问题 3

什么类型的参数适合使用 `enum`？

### 问题 4

为什么 `status: string` 往往不如明确 enum 稳定？

### 问题 5

`minimum / maximum` 解决了什么问题？

### 问题 6

为什么 Schema 写得太松会导致 Tool Call 参数漂移？

### 问题 7

为什么 Schema 写得太严格也可能是坏设计？

### 问题 8

`additionalProperties: false` 有什么价值？

### 问题 9

为什么 Schema 合法不代表业务合法？

### 问题 10

为什么 `currentUserId`、`tenantId` 之类的上下文通常不应该让 LLM 自己生成？

### 问题 11

Tool Description 和 Tool Parameters Schema 分别主要影响什么？

### 问题 12

Schema Definition 和 Runtime Validation 有什么区别？

### 问题 13

为什么 LLM 生成的 arguments 仍然必须被 Application 校验？

### 问题 14

如果一个 Tool 的 Schema 极其复杂，除了 Schema 本身，还应该怀疑什么？

### 问题 15

为什么不能把 Java 方法所有内部参数都直接暴露到 Tool Schema？

---

# 一百二十三、口述验收

如果有人问：

> Tool 参数 Schema 不就是告诉模型参数是 JSON 吗？

你应该能够解释：

> 不是。Tool 参数 Schema 的作用不是简单告诉模型“返回 JSON”，而是精确描述如果模型决定调用某个 Tool，它生成的 arguments 应该有哪些字段、字段是什么类型、哪些必须存在、哪些值只能从有限集合中选择，以及数值、字符串、数组等是否存在范围限制。
>
> 比如 queryCampaign 需要 campaignId，那么 Schema 可以规定 campaignId 必须是大于等于 1 的 integer，并且是 required。这样模型生成的参数更容易被 Java 程序稳定解析。
>
> 但 Schema 只负责结构和基础约束，不能保证 Campaign 一定存在，也不能保证当前用户有权限，所以 Application 仍然必须继续做业务校验和权限校验。

如果能够自然讲清这一段：

```text
Node 024 的核心认知
基本建立
```

---

# 一百二十四、Node 024 完成标准

读完本篇以后，应该能够：

- [ ] 解释 Tool 参数 Schema 约束的是 Tool Call arguments。
- [ ] 解释为什么 Tool 参数不能只是自由 JSON。
- [ ] 区分 `properties` 和 `required`。
- [ ] 知道什么时候应该使用 `enum`。
- [ ] 理解 `minimum / maximum` 的作用。
- [ ] 理解 `minLength / maxLength` 的作用。
- [ ] 理解 `minItems / maxItems` 的作用。
- [ ] 理解 `additionalProperties: false` 的作用。
- [ ] 能为 `queryCampaign` 写出基础参数 Schema。
- [ ] 能判断参数是类型错误、缺少必填、非法 enum 还是范围越界。
- [ ] 理解 Schema 合法不等于业务合法。
- [ ] 理解模型 arguments 仍然需要 Runtime Validation。
- [ ] 理解 Tool Schema 不应该暴露服务器内部上下文参数。
- [ ] 理解复杂 Schema 有时意味着 Tool 职责设计过重。
- [ ] 能把 Tool Description 与 Tool Parameters Schema 的职责区分开。

**达到以上标准，再进入 Node 025。**

---

# 一百二十五、本 Node 的停止点

现在不需要：

```text
研究完整 JSON Schema 标准

研究 oneOf / anyOf / allOf

研究复杂 $ref

研究所有 Provider 的 Tool Schema 差异

写 Tool Dispatcher

写反射调用

实现 Agent Loop

实现权限系统
```

当前只需要真正理解：

```text
模型决定调用 Tool 后
arguments 不能随便生成

Application 必须通过 Schema
明确告诉模型：
参数应该长什么样
```

---

# 一百二十六、下一步：Node 025

下一步会进入：

```text
Node 025：Tool Dispatcher
```

到那时问题会从：

```text
参数应该长什么样？
```

转变为：

```text
模型返回：
name = queryCampaign

Application 怎么根据这个 name
找到并执行真正的 Java 方法？
```

也就是开始从：

```text
Tool Contract
```

真正走向：

```text
Tool Execution
```

---

# 本篇一句话总结

> **Tool 参数 Schema 的本质，是把模型要生成的 Tool Call arguments 从“自由 JSON”收缩成一个明确、机器可读、可验证的参数契约。**

最重要的一条链：

```text
Tool Definition
    ↓
Parameters Schema
    ↓
LLM 生成 arguments
    ↓
Application 校验
    ↓
Business Validation
    ↓
真正执行 Tool
```

其中：

```text
required
enum
constraints
```

不是为了让 Schema 看起来更复杂。

而是为了：

```text
减少参数歧义
减少输出漂移
提高程序可解析性
提高 Tool Calling 可靠性
```

**Node 024：Tool 参数 Schema 理论学习。**

---

## 学习记录

- 理论阅读日期：待填写
- 手写 Schema 练习：待填写
- 自测情况：待填写
- 尚未理解的问题：待填写
- 是否达到完成标准：待确认
