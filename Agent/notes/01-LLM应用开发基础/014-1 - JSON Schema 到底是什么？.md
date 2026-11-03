# 014-1 - JSON Schema 到底是什么？

> Node 014：JSON Schema\
> Phase 1：LLM 应用开发基础

> **阅读与收尾记录（2026-11-03）：**用户已读完本篇，提交 User Schema 手写结果；已讲解并修正 required 的层级及缺失闭合括号，用户确认本节结束，不再追加练习。Node 014 按此次学习范围收尾，下一步 Node 015「Java DTO 与 Structured Output（准备学习）」。其他三项建议练习未做，未运行 Schema 校验器、Java API 实验或逐题口述测验，不将其记为已完成。

---

# 一、先说结论

Node 013 我们已经知道：

```text
Structured Output
```

解决的是：

```text
怎么让模型输出
从“给人看的自由文本”
逐渐变成
“给程序处理的数据”
```

但是只让模型输出：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

还不够。

因为程序真正关心的是：

```text
必须有哪些字段？
字段分别是什么类型？
哪些字段必须存在？
哪些字段只能取固定值？
数组里面装什么？
对象里面还能不能嵌套对象？
能不能出现我没定义过的字段？
```

这些问题就需要：

```text
JSON Schema
```

来描述。

一句话：

> **JSON Schema 是用来描述 JSON 数据“应该长什么样”的规则。**

---

# 二、JSON 和 JSON Schema 不是一回事

先看一个普通 JSON：

```json
{
  "name": "张三",
  "age": 18
}
```

这只是：

```text
一份具体数据
```

它告诉我们：

```text
name = 张三
age = 18
```

但是它没有正式说明：

```text
name 必须是字符串吗？
age 必须是整数吗？
name 一定要有吗？
age 可以不传吗？
能不能多一个 city？
```

这些都没有定义。

JSON Schema 做的事情就是：

```text
定义规则
```

例如：

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "age": {
      "type": "integer"
    }
  },
  "required": ["name", "age"]
}
```

这不是某个具体用户。

这是在描述：

```text
“一个合法的 User JSON 应该是什么结构”
```

---

# 三、可以把 JSON 和 JSON Schema 理解成什么关系？

可以类比 Java。

具体对象：

```java
User user = new User("张三", 18);
```

类似：

```json
{
  "name": "张三",
  "age": 18
}
```

而：

```java
class User {

    private String name;

    private Integer age;
}
```

更接近：

```text
Schema / Type Definition
```

当然：

```text
Java class
≠
JSON Schema
```

它们不是同一套技术。

但是思想很像：

```text
具体数据
和
数据结构定义
```

之间的关系。

---

# 四、为什么 LLM Structured Output 需要 JSON Schema？

因为模型本身是生成式系统。

如果你只告诉它：

```text
输出 JSON
```

它可能输出：

```json
{
  "risk": "HIGH"
}
```

下一次：

```json
{
  "riskLevel": "HIGH"
}
```

再下一次：

```json
{
  "riskLevel": 3
}
```

或者：

```json
{
  "riskLevel": "高风险"
}
```

这些都可能是合法 JSON。

但程序需要的是：

```text
固定字段
固定类型
固定取值范围
```

所以需要 Schema 告诉模型：

```text
riskLevel
必须存在

riskLevel
必须是字符串

riskLevel
只能是 HIGH / MEDIUM / LOW
```

这样：

```text
模型自由度
↓
被限制到程序可以管理的范围
```

---

# 五、先看一个完整但简单的 Schema

假设我们要定义一个广告风险分析结果：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高",
  "suggestion": "降低预算"
}
```

对应的 Schema 可以写成：

```json
{
  "type": "object",
  "properties": {
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
    "riskLevel",
    "reason",
    "suggestion"
  ]
}
```

先不要怕。

这一大段其实只做了几件事：

```text
最外层必须是 object

里面有三个字段：
riskLevel
reason
suggestion

三个字段都是 string

riskLevel 只能是：
HIGH
MEDIUM
LOW

三个字段都必须存在
```

这就是 JSON Schema 的核心思想。

---

# 六、JSON Schema 本质上在回答什么问题？

每个 Schema 都在回答：

```text
这个 JSON：

1. 整体是什么类型？
2. 有哪些字段？
3. 字段是什么类型？
4. 哪些字段必须有？
5. 字段允许哪些值？
6. 数组里装什么？
7. 对象能不能继续嵌套？
8. 是否允许额外字段？
```

所以它本质上是：

```text
数据结构规则
```

---

# 七、第一个核心关键词：type

最重要的关键词之一：

```json
"type"
```

它定义：

```text
数据是什么类型
```

常见类型包括：

```text
object
array
string
number
integer
boolean
null
```

---

# 八、string

例如：

```json
{
  "type": "string"
}
```

表示：

```text
这个值必须是字符串
```

合法：

```json
"Java"
```

合法：

```json
"HIGH"
```

不合法：

```json
123
```

不合法：

```json
true
```

---

# 九、integer

```json
{
  "type": "integer"
}
```

表示：

```text
整数
```

例如：

```json
18
```

合法。

```json
1001
```

合法。

而：

```json
18.5
```

通常不属于 integer。

---

# 十、number

```json
{
  "type": "number"
}
```

表示：

```text
数字
```

可以理解成范围比 integer 更宽。

例如：

```json
18
```

可以。

```json
18.5
```

也可以。

所以：

```text
integer
=
整数

number
=
数值
```

---

# 十一、boolean

```json
{
  "type": "boolean"
}
```

合法值：

```json
true
```

或者：

```json
false
```

非常适合：

```text
是否通过
是否启用
是否风险
```

这种字段。

---

# 十二、object

```json
{
  "type": "object"
}
```

表示：

```text
JSON 对象
```

例如：

```json
{
  "name": "张三",
  "age": 18
}
```

在 Structured Output 里：

```text
最外层通常经常是 object
```

因为我们往往需要：

```text
多个字段组成一个结果
```

---

# 十三、array

```json
{
  "type": "array"
}
```

表示：

```text
JSON 数组
```

例如：

```json
[
  "Java",
  "MySQL",
  "Redis"
]
```

或者：

```json
[
  {
    "name": "张三"
  },
  {
    "name": "李四"
  }
]
```

---

# 十四、null

```json
{
  "type": "null"
}
```

表示：

```text
值只能是 null
```

但是实际项目里更常见的问题是：

```text
某字段能不能是 null？
```

这个以后会和：

```text
nullable
required
type 组合
```

等概念联系起来。

现在先记住：

```text
null
也是 JSON Schema 能描述的一种类型
```

---

# 十五、type 是 Schema 的基础

可以把它理解成：

```text
JSON Schema
最先问的就是：

“你是什么类型？”
```

比如：

```json
{
  "type": "object"
}
```

意味着：

```text
最外层必须是对象
```

如果模型返回：

```json
[
  "HIGH",
  "预算过高"
]
```

那就不符合这个 Schema。

---

# 十六、第二个核心关键词：properties

如果：

```text
type = object
```

接下来最自然的问题就是：

```text
这个 object 里面有哪些字段？
```

这就由：

```json
"properties"
```

定义。

例如：

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "age": {
      "type": "integer"
    }
  }
}
```

意思是：

```text
这个对象定义了两个属性：

name
age
```

---

# 十七、properties 里面是什么？

看这里：

```json
"properties": {
  "name": {
    "type": "string"
  },
  "age": {
    "type": "integer"
  }
}
```

实际上：

```text
name
```

本身又有自己的 Schema：

```json
{
  "type": "string"
}
```

而：

```text
age
```

也有自己的 Schema：

```json
{
  "type": "integer"
}
```

所以你会发现：

> **Schema 是可以嵌套 Schema 的。**

---

# 十八、为什么 JSON Schema 很适合描述复杂结构？

因为 JSON 本身就能嵌套：

```text
Object
里面可以有 Object

Object
里面可以有 Array

Array
里面可以有 Object
```

JSON Schema 也采用相同思想描述这些结构。

例如：

```text
用户
├── name
├── age
└── address
    ├── city
    └── street
```

Schema 也可以一层层写下去。

---

# 十九、第三个核心关键词：required

一个非常容易误解的点：

```text
properties 里定义了字段
```

不代表：

```text
这个字段一定必须存在
```

例如：

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "age": {
      "type": "integer"
    }
  }
}
```

这里只是定义：

```text
如果 name 存在
它应该是 string

如果 age 存在
它应该是 integer
```

但：

```text
name / age 是否必须出现
```

要看：

```json
"required"
```

---

# 二十、required 怎么写？

例如：

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "age": {
      "type": "integer"
    }
  },
  "required": [
    "name",
    "age"
  ]
}
```

意思：

```text
name 必须存在
age 必须存在
```

如果返回：

```json
{
  "name": "张三"
}
```

就不符合这个 Schema。

因为：

```text
age 缺失
```

---

# 二十一、properties 和 required 必须区分

这是 JSON Schema 最容易踩的坑之一。

```text
properties
=
定义字段规则

required
=
定义哪些字段必须存在
```

它们不是一回事。

---

# 二十二、一个字段不在 required 里意味着什么？

例如：

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "nickname": {
      "type": "string"
    }
  },
  "required": [
    "name"
  ]
}
```

那么：

```text
name
必须存在

nickname
可以不存在
```

下面合法：

```json
{
  "name": "张三"
}
```

下面也合法：

```json
{
  "name": "张三",
  "nickname": "老张"
}
```

---

# 二十三、required 不代表“值不能是 null”

这是一个非常重要的区别。

假设：

```text
字段是 required
```

它表达的是：

```text
字段必须存在
```

它并不天然等于：

```text
字段值绝对不能是 null
```

是否允许 null：

```text
取决于这个字段允许的类型
```

所以：

```text
Required
和
Nullable
```

是两个概念。

这和 Java / 数据库里的思维也很像：

```text
“字段有没有”
和
“字段值是不是 null”
```

不是完全同一件事。

---

# 二十四、第四个核心关键词：enum

如果一个字段只能取有限几个值：

```text
不要让模型自由发挥
```

应该考虑：

```json
"enum"
```

例如：

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

表示：

```text
这个值必须是字符串

而且只能是：
HIGH
MEDIUM
LOW
```

---

# 二十五、为什么 enum 对 LLM 特别重要？

如果没有 enum：

```json
{
  "riskLevel": "风险偏高"
}
```

模型可能这样输出。

下一次：

```json
{
  "riskLevel": "严重"
}
```

再下一次：

```json
{
  "riskLevel": "High"
}
```

程序很难统一处理。

如果约束：

```text
HIGH
MEDIUM
LOW
```

程序就可以：

```java
switch (riskLevel) {
    case "HIGH" -> ...
    case "MEDIUM" -> ...
    case "LOW" -> ...
}
```

所以：

> **enum 是把模型自由表达压缩成有限状态的重要工具。**

---

# 二十六、什么时候适合 enum？

例如：

```text
风险等级
状态
分类标签
优先级
意图类型
路由结果
工具动作
```

这些通常都适合有限集合。

例如：

```json
{
  "type": "string",
  "enum": [
    "BUG",
    "FEATURE",
    "QUESTION"
  ]
}
```

---

# 二十七、什么时候不适合 enum？

例如：

```text
reason
summary
suggestion
description
```

这些字段通常需要自然语言自由表达。

如果把：

```text
reason
```

也做成 enum：

```text
A
B
C
```

可能会失去需要的信息。

所以 Structured Output 的原则还是：

```text
该限制的限制

该自由的自由
```

---

# 二十八、第五个核心关键词：array

数组是非常常见的结构。

例如我们希望模型返回：

```json
{
  "keywords": [
    "Java",
    "并发",
    "线程池"
  ]
}
```

那 Schema 里需要：

```text
keywords 是 array
```

例如：

```json
{
  "type": "object",
  "properties": {
    "keywords": {
      "type": "array"
    }
  }
}
```

但这样还不够。

因为程序还会问：

```text
数组里面装什么？
```

这就需要：

```json
"items"
```

---

# 二十九、array + items

例如：

```json
{
  "type": "array",
  "items": {
    "type": "string"
  }
}
```

意思：

```text
这是一个数组

数组中的每一个元素
都必须是 string
```

合法：

```json
[
  "Java",
  "Redis",
  "MySQL"
]
```

不符合：

```json
[
  "Java",
  123,
  true
]
```

---

# 三十、对象中的数组

例如：

```json
{
  "summary": "Java 并发基础",
  "keywords": [
    "Thread",
    "synchronized",
    "volatile"
  ]
}
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "summary": {
      "type": "string"
    },
    "keywords": {
      "type": "array",
      "items": {
        "type": "string"
      }
    }
  },
  "required": [
    "summary",
    "keywords"
  ]
}
```

---

# 三十一、array 里面也可以放 object

例如：

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

那么：

```text
issues
=
array

items
=
object
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "issues": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "type": {
            "type": "string",
            "enum": [
              "BUG",
              "FEATURE",
              "QUESTION"
            ]
          },
          "description": {
            "type": "string"
          }
        },
        "required": [
          "type",
          "description"
        ]
      }
    }
  },
  "required": [
    "issues"
  ]
}
```

这就是：

```text
Object
↓
Array
↓
Object
```

的嵌套。

---

# 三十二、第六个核心能力：嵌套 object

假设返回：

```json
{
  "user": {
    "name": "张三",
    "age": 18
  }
}
```

这里：

```text
最外层
是 object

user
也是 object
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "user": {
      "type": "object",
      "properties": {
        "name": {
          "type": "string"
        },
        "age": {
          "type": "integer"
        }
      },
      "required": [
        "name",
        "age"
      ]
    }
  },
  "required": [
    "user"
  ]
}
```

---

# 三十三、JSON Schema 的递归思维

学 JSON Schema 不应该死记整段。

应该建立：

```text
递归描述结构
```

的思维。

看到：

```json
{
  "user": {
    "name": "张三"
  }
}
```

你就想：

```text
最外层 object
↓
properties
↓
user
↓
type object
↓
properties
↓
name
↓
type string
```

只要会逐层拆，就不会乱。

---

# 三十四、先学会“读 Schema”

比自己写更重要的一步是：

```text
看懂别人写的 Schema
```

例如：

```json
{
  "type": "object",
  "properties": {
    "category": {
      "type": "string",
      "enum": [
        "BUG",
        "FEATURE",
        "QUESTION"
      ]
    },
    "confidence": {
      "type": "number"
    }
  },
  "required": [
    "category",
    "confidence"
  ]
}
```

你应该能口头解释：

```text
最外层是对象

有两个字段：
category
confidence

category 是字符串
只能是 BUG / FEATURE / QUESTION

confidence 是 number

两个字段都必须存在
```

能做到这个，才算真正开始理解 Schema。

---

# 三十五、再学会“从 JSON 反推 Schema”

例如给你：

```json
{
  "name": "张三",
  "age": 18,
  "enabled": true
}
```

你应该能先写：

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "age": {
      "type": "integer"
    },
    "enabled": {
      "type": "boolean"
    }
  },
  "required": [
    "name",
    "age",
    "enabled"
  ]
}
```

这个练习非常重要。

---

# 三十六、Schema 不是根据“示例值”死推业务规则

例如：

```json
{
  "age": 18
}
```

你可以推：

```text
age 是 integer
```

但不能仅凭一次示例就断定：

```text
age 只能是 18
```

同样：

```json
{
  "status": "ACTIVE"
}
```

不能仅凭一个样本就断定 enum 是：

```text
["ACTIVE"]
```

真正的 Schema：

```text
来自业务契约
```

而不是：

```text
简单照抄某一个样本
```

---

# 三十七、Schema 是“规则”，不是“示例”

这一点非常重要。

Example：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

表达：

```text
一次具体结果
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "riskLevel": {
      "type": "string",
      "enum": [
        "HIGH",
        "MEDIUM",
        "LOW"
      ]
    },
    "reason": {
      "type": "string"
    }
  },
  "required": [
    "riskLevel",
    "reason"
  ]
}
```

表达：

```text
所有合法结果应该遵守的结构
```

---

# 三十八、Schema 和 Few-shot Example 的区别

Few-shot Example：

```text
“你看，任务应该这样做”
```

JSON Schema：

```text
“你必须满足这样的数据结构”
```

可以理解成：

```text
Example
=
示范

Schema
=
契约
```

它们可以同时存在。

但是不能互相替代。

---

# 三十九、additionalProperties 是什么？

这是一个很实用的关键词。

假设：

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    }
  }
}
```

那么一个问题来了：

```text
除了 name
还能不能出现别的字段？
```

例如：

```json
{
  "name": "张三",
  "age": 18
}
```

`age` 没有定义在 properties 里。

是否允许？

这就和：

```json
"additionalProperties"
```

有关。

---

# 四十、additionalProperties: false

例如：

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    }
  },
  "required": [
    "name"
  ],
  "additionalProperties": false
}
```

意思：

```text
只允许 Schema 已经定义的字段

不允许额外字段
```

所以：

```json
{
  "name": "张三"
}
```

符合。

而：

```json
{
  "name": "张三",
  "age": 18
}
```

不符合。

---

# 四十一、为什么 additionalProperties 对 Structured Output 很有价值？

如果不限制额外字段，模型可能“热心”地多返回：

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高",
  "suggestion": "降低预算",
  "extraAnalysis": "我还帮你分析了一下……",
  "confidenceText": "我比较确定"
}
```

程序本来只需要：

```text
riskLevel
reason
suggestion
```

多余字段可能：

```text
增加不稳定性
增加维护成本
制造误解
```

因此严格结构中经常会希望：

```text
只返回我们定义过的字段
```

---

# 四十二、但 additionalProperties 也不是永远必须 false

有些场景：

```text
业务本来就允许动态字段
```

那就不能一刀切。

所以原则仍然是：

```text
Schema 要表达真实业务契约
```

而不是：

```text
为了严格而严格
```

---

# 四十三、description 有什么作用？

Schema 里通常还可以给字段增加：

```json
"description"
```

例如：

```json
{
  "type": "string",
  "description": "风险等级，只能表示广告当前风险程度"
}
```

这对人类阅读很有帮助。

在 LLM Structured Output 场景里：

```text
字段描述
```

也经常能帮助模型理解：

```text
这个字段到底是什么意思
```

---

# 四十四、字段名本身不能承载全部语义

例如：

```text
score
```

到底表示：

```text
质量分？
风险分？
置信度？
满意度？
```

很模糊。

所以更好的做法可能是：

```text
confidence
```

再加 description：

```text
模型对分类结果的置信程度
```

Schema 设计其实也要求：

```text
命名清晰
语义明确
```

---

# 四十五、常见字符串约束

JSON Schema 不止能描述：

```text
string
```

还可以进一步加规则。

例如：

```json
{
  "type": "string",
  "minLength": 1
}
```

表示：

```text
至少一个字符
```

还可以有：

```text
maxLength
pattern
format
```

等。

Node 014 不要求你把所有关键字背完。

我们现在最重要的是建立：

```text
Schema
=
可以继续约束类型内部的细节
```

这个认知。

---

# 四十六、常见数字约束

数字也可以限制。

例如：

```json
{
  "type": "number",
  "minimum": 0,
  "maximum": 1
}
```

可以用于：

```text
confidence
```

表示：

```text
0 到 1
```

例如：

```json
{
  "confidence": 0.93
}
```

---

# 四十七、为什么 confidence 常常不能只写 number？

如果只写：

```json
{
  "type": "number"
}
```

那：

```json
{
  "confidence": 10000
}
```

从类型上仍然是：

```text
number
```

但业务上明显不合理。

所以：

```text
type
只能解决类型问题

更细的业务范围
还需要约束
```

---

# 四十八、常见数组约束

数组也可以限制：

```text
最少几个元素
最多几个元素
```

例如概念上：

```json
{
  "type": "array",
  "items": {
    "type": "string"
  },
  "minItems": 1,
  "maxItems": 5
}
```

表示：

```text
必须是 string 数组

至少 1 个元素

最多 5 个元素
```

---

# 四十九、为什么数组长度约束对 LLM 有帮助？

例如你要求：

```text
提取关键词
```

如果不限制：

```text
模型可能给 2 个
也可能给 30 个
```

而业务只希望：

```text
3～5 个关键词
```

Schema 可以帮助描述这种要求。

---

# 五十、对象嵌套时 required 是分层的

这是一个很容易犯错的地方。

例如：

```json
{
  "user": {
    "name": "张三",
    "address": {
      "city": "北京"
    }
  }
}
```

如果你想要求：

```text
user 必须存在

user.name 必须存在

user.address 必须存在

address.city 必须存在
```

需要在不同层级分别写：

```text
required
```

不能只在最外层写一次。

---

# 五十一、看一个分层 required 的例子

```json
{
  "type": "object",
  "properties": {
    "user": {
      "type": "object",
      "properties": {
        "name": {
          "type": "string"
        },
        "address": {
          "type": "object",
          "properties": {
            "city": {
              "type": "string"
            }
          },
          "required": [
            "city"
          ]
        }
      },
      "required": [
        "name",
        "address"
      ]
    }
  },
  "required": [
    "user"
  ]
}
```

你可以看到：

```text
最外层 required
控制 user

user 内部 required
控制 name / address

address 内部 required
控制 city
```

---

# 五十二、JSON Schema 是一棵树

如果把复杂 Schema 画出来：

```text
object
│
├── user : object
│   │
│   ├── name : string
│   │
│   └── address : object
│       │
│       └── city : string
│
└── tags : array
    │
    └── item : string
```

所以学 Schema 的方法应该是：

```text
从外到内
逐层拆
```

而不是：

```text
一次背完整 JSON
```

---

# 五十三、Nullable 怎么理解？

真实业务中经常出现：

```text
某字段必须有
但值可能为空
```

或者：

```text
字段本身可以不出现
```

这两个情况要区分。

例如：

```json
{
  "nickname": null
}
```

和：

```json
{}
```

不是同一回事。

第一个：

```text
nickname 字段存在
值为 null
```

第二个：

```text
nickname 字段不存在
```

---

# 五十四、“可选字段”和“可为 null”不是一回事

可选字段：

```text
不在 required 中
```

表示：

```text
可以不出现
```

可为 null：

```text
表示这个字段即使出现
值也允许是 null
```

这两个概念在 Java DTO、数据库设计里也经常容易混。

Node 015 时会再次遇到。

---

# 五十五、为什么要尽量少用模糊 null？

例如：

```json
{
  "suggestion": null
}
```

程序会问：

```text
为什么是 null？

因为没有建议？
因为模型不知道？
因为字段不适用？
因为模型失败？
```

所以有时候更好的 Schema 设计是：

```text
显式状态
```

例如：

```json
{
  "hasSuggestion": false,
  "suggestion": ""
}
```

或者：

```json
{
  "status": "NO_SUGGESTION"
}
```

当然具体怎么设计要看业务。

核心认知：

```text
Schema 设计
不只是语法问题
也是业务建模问题
```

---

# 五十六、Schema 设计本质上也是 API 设计

如果你给前端设计接口：

```json
{
  "status": "SUCCESS",
  "data": {
    ...
  }
}
```

你会考虑：

```text
字段叫什么
类型是什么
是否为空
枚举有哪些
嵌套层级是否合理
```

JSON Schema 同样如此。

所以对于 Java 后端程序员来说：

> **JSON Schema 不是陌生世界，它本质上仍然是在做数据契约设计。**

---

# 五十七、为什么 Structured Output 里 Schema 不应该太复杂？

理论上 Schema 可以写得非常复杂。

但是对于 LLM：

```text
Schema 越复杂
模型需要遵守的规则越多
```

同时：

```text
Token 增加
维护成本增加
测试难度增加
Provider 支持差异可能增加
```

因此一个非常重要的工程原则：

> **Schema 应该尽量简单，只表达业务真正需要的结构。**

---

# 五十八、不要为了“一次拿全”设计超级 Schema

例如一个简单文本分类任务，本来只需要：

```json
{
  "category": "BUG"
}
```

却设计成：

```json
{
  "analysis": {
    "metadata": {
      "modelReasoning": "...",
      "categoryInfo": {
        ...
      }
    }
  }
}
```

这可能：

```text
没有必要
```

Schema 不是越复杂越高级。

---

# 五十九、字段越少，通常越容易稳定

例如任务只需要：

```text
category
```

那就返回：

```json
{
  "category": "BUG"
}
```

不要因为模型“能说很多”就要求：

```text
category
reason
confidence
alternative
fullAnalysis
background
summary
details
```

除非业务真的需要。

这也是：

```text
减少 LLM 输出空间
```

的一种方式。

---

# 六十、什么叫“输出空间”？

假设：

```text
category
```

只能取：

```text
BUG
FEATURE
QUESTION
```

模型实际上只有：

```text
3 个合法答案
```

如果完全自由文本：

```text
答案空间几乎无限
```

Structured Output + enum：

```text
把无限输出空间
缩小成有限合法空间
```

这会显著提高程序可控性。

---

# 六十一、为什么 schema 不能代替 Prompt？

Schema 只告诉模型：

```text
输出应该是什么结构
```

它不一定完整告诉模型：

```text
如何完成任务
```

例如：

```json
{
  "category": "BUG"
}
```

Schema 可以规定：

```text
category 只能是 BUG / FEATURE / QUESTION
```

但模型还需要知道：

```text
什么时候算 BUG？
什么时候算 FEATURE？
什么时候算 QUESTION？
```

这仍然需要：

```text
Prompt
业务定义
Context
Few-shot
```

所以：

```text
Prompt
=
任务语义

Schema
=
输出契约
```

---

# 六十二、一个完整请求里可能同时存在什么？

未来真实调用可能同时有：

```text
System Prompt
Task
Context
Input
Few-shot Examples
JSON Schema
```

它们分别承担不同职责。

可以理解成：

```text
System
=
长期行为规则

Prompt
=
这次要做什么

Context
=
这次应该知道什么

Few-shot
=
任务示范

Schema
=
结果必须长什么样
```

---

# 六十三、Schema 不会让模型自动知道业务规则

例如 Schema：

```json
{
  "type": "string",
  "enum": [
    "BUG",
    "FEATURE",
    "QUESTION"
  ]
}
```

它只能告诉模型：

```text
最终只能输出这三个之一
```

但：

```text
“登录按钮没反应”
```

应该归哪个类别？

仍然需要模型理解任务。

所以：

```text
Schema
限制答案形式

Prompt / Context
帮助模型判断答案内容
```

---

# 六十四、Schema 和 Validation 的关系

Schema 本身：

```text
描述规则
```

Validation：

```text
检查实际数据是否符合规则
```

例如 Schema：

```json
{
  "type": "object",
  "properties": {
    "age": {
      "type": "integer"
    }
  },
  "required": [
    "age"
  ]
}
```

实际数据：

```json
{
  "age": "十八"
}
```

Validation 会发现：

```text
age 应该是 integer
但实际是 string
```

---

# 六十五、Structured Output 场景里为什么仍然要理解 Validation？

因为将来流程会是：

```text
Schema
↓
Model Output
↓
Validation
↓
Deserialization
↓
Java DTO
```

即使 Provider 能帮我们做很多结构约束：

```text
后端仍然要理解
“这个结果凭什么可以信任”
```

Node 015 会把这条链继续接起来。

---

# 六十六、Schema 不是业务校验的全部

例如：

```json
{
  "type": "object",
  "properties": {
    "age": {
      "type": "integer"
    }
  }
}
```

模型返回：

```json
{
  "age": -999
}
```

它满足：

```text
integer
```

但可能不满足业务。

所以仍然存在：

```text
Schema Validation
↓
Business Validation
```

两层。

---

# 六十七、结构校验和业务校验的区别

结构校验：

```text
字段有没有？
类型对不对？
值是否属于 enum？
数组元素类型对不对？
```

业务校验：

```text
年龄是否合理？
预算是否超过账户余额？
campaignId 是否真的存在？
用户是否有权限？
```

不要混淆。

---

# 六十八、JSON Schema 不能验证外部世界事实

例如：

```json
{
  "campaignId": 123
}
```

Schema 能验证：

```text
campaignId 是 integer
```

但不能仅靠 Schema 知道：

```text
数据库里是不是真的有 campaignId = 123
```

这需要：

```text
业务系统
数据库
Tool
API
```

所以：

```text
Schema
=
数据结构规则

不是
=
事实数据库
```

---

# 六十九、description 也不能当硬约束

例如：

```json
{
  "type": "integer",
  "description": "必须大于 0"
}
```

这只是描述。

如果真正要结构层面表达：

```text
大于 0
```

应该使用对应约束，例如概念上：

```text
minimum
```

所以：

```text
description
=
帮助理解

真正规则
=
Schema constraint
```

---

# 七十、一个适合我们的广告分析 Schema

先设计一个不复杂的版本：

```json
{
  "type": "object",
  "properties": {
    "riskLevel": {
      "type": "string",
      "enum": [
        "HIGH",
        "MEDIUM",
        "LOW"
      ]
    },
    "reason": {
      "type": "string"
    },
    "suggestion": {
      "type": "string"
    }
  },
  "required": [
    "riskLevel",
    "reason",
    "suggestion"
  ],
  "additionalProperties": false
}
```

这个 Schema 很适合学习。

因为它已经覆盖：

```text
object
properties
string
enum
required
additionalProperties
```

---

# 七十一、逐行读这个 Schema

最外层：

```json
"type": "object"
```

表示：

```text
返回结果必须是对象
```

---

```json
"properties"
```

表示：

```text
对象有哪些定义好的字段
```

---

```json
"riskLevel"
```

表示字段名：

```text
riskLevel
```

它的类型：

```json
"type": "string"
```

然后：

```json
"enum": [
  "HIGH",
  "MEDIUM",
  "LOW"
]
```

表示：

```text
只能三选一
```

---

`reason`：

```json
{
  "type": "string"
}
```

表示：

```text
原因是自由文本字符串
```

---

`suggestion`：

```json
{
  "type": "string"
}
```

表示：

```text
建议也是自由文本字符串
```

---

```json
"required": [
  "riskLevel",
  "reason",
  "suggestion"
]
```

表示：

```text
三个字段一个都不能缺
```

---

```json
"additionalProperties": false
```

表示：

```text
不能自己多加字段
```

这就是一个很完整的简单 Schema。

---

# 七十二、如果模型返回这个呢？

```json
{
  "riskLevel": "CRITICAL",
  "reason": "预算过高",
  "suggestion": "停止投放"
}
```

问题：

```text
CRITICAL
不在 enum
```

所以不符合 Schema。

---

# 七十三、如果返回这个呢？

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

问题：

```text
suggestion
是 required
但缺失
```

不符合 Schema。

---

# 七十四、如果返回这个呢？

```json
{
  "riskLevel": "HIGH",
  "reason": "预算过高",
  "suggestion": "降低预算",
  "confidence": 0.9
}
```

如果：

```json
"additionalProperties": false
```

那么：

```text
confidence
没有被定义
```

所以不符合。

---

# 七十五、如果返回这个呢？

```json
{
  "riskLevel": 3,
  "reason": "预算过高",
  "suggestion": "降低预算"
}
```

问题：

```text
riskLevel 应该是 string
实际是 number / integer
```

不符合。

---

# 七十六、如果返回这个呢？

```json
{
  "riskLevel": "HIGH",
  "reason": 123,
  "suggestion": "降低预算"
}
```

问题：

```text
reason 应该是 string
```

不符合。

---

# 七十七、你现在应该开始形成“Schema 检查视角”

以后看到一个模型输出，不只是看：

```text
内容对不对
```

还要先看：

```text
结构对不对？
```

例如：

```text
字段名
字段类型
必填字段
枚举
额外字段
嵌套结构
```

这就是工程化 LLM 和纯聊天最大的区别之一。

---

# 七十八、为什么 JSON Schema 是“程序和模型之间的契约”？

程序提前说：

```text
我只接受这样的结构
```

Schema：

```text
就是契约文本
```

模型：

```text
按照这个契约生成
```

程序：

```text
按照同一个契约解析
```

所以双方共享：

```text
同一份结构定义
```

这就是数据契约。

---

# 七十九、Contract 思维为什么重要？

没有契约：

```text
模型输出什么
程序就临时适配什么
```

代码会越来越乱。

有契约：

```text
程序先定义接受什么
模型必须向契约靠拢
```

这和普通后端 API 开发是同一个思想。

---

# 八十、JSON Schema 和 OpenAPI 有什么关系？

以后你可能在：

```text
Swagger
OpenAPI
```

里面看到类似：

```text
type
properties
required
enum
```

因为 API 文档也需要描述：

```text
请求 / 响应的数据结构
```

它们和 JSON Schema 在思想和很多语法上有很强联系。

现在不用展开标准差异。

你只要意识到：

> **Schema 思维早就在传统 API 工程里存在，LLM Structured Output 只是把它带到了模型输出这一侧。**

---

# 八十一、JSON Schema 和数据库 Schema 有什么关系？

数据库 Schema：

```text
定义表结构
```

JSON Schema：

```text
定义 JSON 结构
```

两者不是同一个技术。

但是都属于：

```text
结构契约
```

这种工程思想。

---

# 八十二、JSON Schema 和 Java DTO 有什么关系？

例如 Schema：

```json
{
  "type": "object",
  "properties": {
    "name": {
      "type": "string"
    },
    "age": {
      "type": "integer"
    }
  },
  "required": [
    "name",
    "age"
  ]
}
```

Java DTO：

```java
class UserDTO {

    private String name;

    private Integer age;
}
```

你可以看到：

```text
Schema
描述 JSON 世界中的结构

DTO
描述 Java 世界中的结构
```

Node 015 要做的就是：

```text
把这两个世界接起来
```

---

# 八十三、以后真正链路是什么？

```text
Java DTO
      ↕
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
```

这条链是我们后面真正要掌握的。

---

# 八十四、Schema 和 DTO 是否永远一一对应？

不一定。

实际工程中可能：

```text
Schema
和 DTO
高度对应
```

也可能因为：

```text
Provider 限制
字段转换
业务层 DTO
传输层 DTO
中间结构
```

而存在差异。

现在暂时不用提前设计复杂抽象。

Node 015 先学习最直观的：

```text
Schema
↔
DTO
```

映射。

---

# 八十五、Schema 设计时命名为什么很重要？

字段：

```text
level
```

太模糊。

字段：

```text
riskLevel
```

更清楚。

字段：

```text
type
```

也可能太泛。

字段：

```text
issueType
```

更清楚。

因为 Schema 不只是给程序看。

在 LLM 场景里：

```text
模型也需要理解字段语义
```

---

# 八十六、字段名称应该尽量表达业务概念

例如：

```text
result
data
value
info
```

这些词过于泛化。

更好的可能是：

```text
riskLevel
reason
suggestion
category
confidence
keywords
```

这样：

```text
Schema 自身就更容易理解
```

---

# 八十七、什么时候用 object，什么时候用 array？

如果结果是：

```text
一个有多个属性的实体
```

通常：

```text
object
```

例如：

```json
{
  "name": "张三",
  "age": 18
}
```

如果结果是：

```text
多个同类元素组成集合
```

通常：

```text
array
```

例如：

```json
[
  "Java",
  "Redis",
  "MySQL"
]
```

---

# 八十八、不要把所有内容都塞成 string

这是很常见的坏设计。

例如：

```json
{
  "age": "18",
  "enabled": "true",
  "score": "0.92"
}
```

虽然都能当字符串。

但程序真正需要的是：

```json
{
  "age": 18,
  "enabled": true,
  "score": 0.92
}
```

这样类型更准确。

Schema 的意义之一就是：

```text
让数据拥有真实类型
```

---

# 八十九、为什么真实类型很重要？

因为 Java 程序才能自然处理：

```java
if (enabled) {
    ...
}
```

而不是：

```java
if ("true".equals(enabled)) {
    ...
}
```

以及：

```java
score > 0.8
```

而不是先：

```java
Double.parseDouble(score)
```

正确类型会降低很多额外转换。

---

# 九十、不要为了“兼容”把一切都放宽

例如：

```text
riskLevel
```

明明应该：

```text
HIGH / MEDIUM / LOW
```

却因为怕模型失败，直接写：

```text
string
```

这相当于：

```text
放弃了很多约束价值
```

更好的思路应该是：

```text
Schema 表达真正业务契约

如果模型不稳定
就解决模型 / Prompt / 兼容问题
```

而不是一开始把 Schema 写得毫无约束。

---

# 九十一、但也不能过度约束

反过来：

```text
reason
```

本来应该允许自然语言。

如果你把它限制成：

```text
enum
```

只有几个固定句子：

```text
BUDGET_HIGH
CTR_LOW
UNKNOWN
```

可能无法表达真实原因。

所以：

```text
约束强度
应该和业务需求匹配
```

---

# 九十二、Schema 设计是确定性与表达力的权衡

约束越强：

```text
程序越容易处理
```

但模型可以表达的空间：

```text
越小
```

约束越弱：

```text
模型越自由
```

但程序处理难度：

```text
越高
```

所以工程目标是：

```text
找到合适平衡
```

---

# 九十三、为什么 Agent Action 特别适合 enum？

以后 Agent 可能返回：

```json
{
  "action": "SEARCH"
}
```

如果 action 是自由字符串：

```text
SEARCH
SEARCH_WEB
去搜索
search
LOOKUP
```

程序很难处理。

如果定义：

```json
{
  "type": "string",
  "enum": [
    "SEARCH",
    "CALL_TOOL",
    "FINISH"
  ]
}
```

Agent Loop 就能稳定做：

```text
switch action
```

所以 Schema 会直接影响 Agent Runtime 的可靠性。

---

# 九十四、Tool Calling 里的参数为什么也需要 Schema？

假设工具：

```text
getWeather(city)
```

模型需要生成：

```json
{
  "city": "北京"
}
```

程序必须知道：

```text
city 必须是什么类型
是否必须存在
还能不能传其他字段
```

所以 Tool Definition 本质上也需要：

```text
参数 Schema
```

这也是为什么现在学 JSON Schema 很关键。

---

# 九十五、我们现在不是为了背 JSON Schema 标准

JSON Schema 本身是一个很大的规范体系。

里面还有很多更高级的关键字和组合能力。

但是现在我们的目标不是：

```text
成为 JSON Schema 标准专家
```

而是：

```text
为 LLM Structured Output
Tool Calling
Java DTO
Agent 工程
建立够用的 Schema 能力
```

---

# 九十六、Node 014 当前必须掌握哪些关键词？

第一层必须掌握：

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

第二层理解即可：

```text
description
minimum
maximum
minLength
maxLength
minItems
maxItems
null / optional
```

高级组合现在暂时不强求。

---

# 九十七、哪些高级内容暂时不要深挖？

例如：

```text
oneOf
anyOf
allOf
not
$ref
$defs
if / then / else
复杂 pattern
复杂 format
```

这些都是真实存在的 Schema 能力。

但现在没有必要一次全部学完。

否则很容易：

```text
学标准
超过学 Agent
```

这违反我们的路线目标。

---

# 九十八、什么时候再学高级 Schema？

以后真实项目遇到：

```text
复杂联合类型
可复用结构
多种输出形态
复杂嵌套
Provider 兼容
```

再针对性补。

现在：

```text
先掌握最常用 20%
```

足够支撑：

```text
80% 的 Structured Output / Tool 参数场景
```

---

# 九十九、练习 1：简单对象

目标 JSON：

```json
{
  "title": "Java 并发",
  "difficulty": "MEDIUM"
}
```

要求：

```text
title
string

difficulty
只能是：
EASY
MEDIUM
HARD

两个字段都必须存在
```

你应该能够写出：

```json
{
  "type": "object",
  "properties": {
    "title": {
      "type": "string"
    },
    "difficulty": {
      "type": "string",
      "enum": [
        "EASY",
        "MEDIUM",
        "HARD"
      ]
    }
  },
  "required": [
    "title",
    "difficulty"
  ],
  "additionalProperties": false
}
```

---

# 一百、练习 2：数组

目标：

```json
{
  "keywords": [
    "Java",
    "Thread",
    "Lock"
  ]
}
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "keywords": {
      "type": "array",
      "items": {
        "type": "string"
      }
    }
  },
  "required": [
    "keywords"
  ],
  "additionalProperties": false
}
```

---

# 一百零一、练习 3：嵌套对象

目标：

```json
{
  "user": {
    "name": "张三",
    "age": 18
  }
}
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "user": {
      "type": "object",
      "properties": {
        "name": {
          "type": "string"
        },
        "age": {
          "type": "integer"
        }
      },
      "required": [
        "name",
        "age"
      ],
      "additionalProperties": false
    }
  },
  "required": [
    "user"
  ],
  "additionalProperties": false
}
```

---

# 一百零二、练习 4：数组中放对象

目标：

```json
{
  "items": [
    {
      "name": "Java",
      "score": 0.95
    },
    {
      "name": "Redis",
      "score": 0.86
    }
  ]
}
```

Schema：

```json
{
  "type": "object",
  "properties": {
    "items": {
      "type": "array",
      "items": {
        "type": "object",
        "properties": {
          "name": {
            "type": "string"
          },
          "score": {
            "type": "number"
          }
        },
        "required": [
          "name",
          "score"
        ],
        "additionalProperties": false
      }
    }
  },
  "required": [
    "items"
  ],
  "additionalProperties": false
}
```

---

# 一百零三、真正学会 Schema 的判断标准

不是：

```text
我看过 type / properties
```

而是：

```text
给你一个目标 JSON
你能自己写 Schema

给你一个 Schema
你能自己解释合法 JSON 长什么样

给你一个错误 JSON
你能指出哪里违反 Schema
```

这才叫掌握。

---

# 一百零四、Schema Debug 应该怎么想？

未来 Structured Output 失败时，不要只说：

```text
模型输出错了
```

应该具体定位：

```text
JSON Syntax Error？

Missing Required Field？

Wrong Type？

Invalid Enum？

Unexpected Property？

Nested Object Error？

Array Item Error？
```

这种思维以后会直接用于排错。

---

# 一百零五、模型输出失败时不要第一反应改 Prompt

有时候问题可能不是：

```text
Prompt
```

而是：

```text
Schema 本身设计错误
```

例如：

```text
required
漏字段

enum
写错

嵌套层级不对

type
写错
```

所以 Agent 工程排查以后需要：

```text
Prompt
+
Schema
+
Model Output
+
Parser
```

一起看。

---

# 一百零六、JSON Schema 会不会增加 Prompt Token？

在 LLM Structured Output 场景里：

```text
Schema 通常也是请求的一部分
```

因此 Schema 越大：

```text
输入体积通常越大
```

这意味着可能增加：

```text
Token
延迟
成本
```

所以 Schema 也应该：

```text
简洁
明确
避免无用字段
```

---

# 一百零七、这和 Context Engineering 有什么关系？

以后 Node 019 会学：

```text
Context Engineering
```

Schema 其实也属于模型当前请求里：

```text
需要看到的约束信息
```

所以未来我们会发现：

```text
Prompt
Few-shot
RAG
History
Schema
Tool Definitions
```

都在争夺：

```text
Context Budget
```

---

# 一百零八、Schema 也需要版本意识

真实业务里如果：

```text
Schema 改了
```

例如新增：

```text
confidence
```

或者 enum 新增：

```text
CRITICAL
```

程序：

```text
DTO
业务逻辑
测试
```

可能都要一起调整。

所以 Schema 本质上也是：

```text
接口契约
```

契约变化就可能影响消费者。

---

# 一百零九、不要随便修改 enum

例如原来：

```text
HIGH
MEDIUM
LOW
```

后来改成：

```text
H
M
L
```

虽然看起来只是字符串变化。

但程序里的：

```java
switch
数据库
前端
测试
日志
```

都可能受影响。

所以 enum 也是 API Contract 的一部分。

---

# 一百一十、为什么测试 Case 很重要？

Structured Output 不能只测试一个正常输入。

以后至少应该测试：

```text
普通输入
边界输入
信息缺失
模糊输入
多对象
空数组
极端值
```

看看模型能否仍然符合 Schema。

这会在后续：

```text
Evaluation
第一阶段项目
```

继续建立。

---

# 一百一十一、Schema 能解决什么？

它擅长解决：

```text
结构
类型
字段存在性
有限取值
嵌套规则
数组元素规则
```

---

# 一百一十二、Schema 不能解决什么？

它不能独立保证：

```text
模型事实正确
业务判断正确
数据库真实存在
外部 API 一定成功
Prompt 没有歧义
模型没有幻觉
```

所以 Schema 是：

```text
一个重要层
```

而不是：

```text
整个 AI 系统
```

---

# 一百一十三、把 Node 013 和 Node 014 串起来

Node 013：

```text
Structured Output
```

回答：

```text
为什么程序需要结构化结果？
```

Node 014：

```text
JSON Schema
```

回答：

```text
程序怎样准确描述“我想要什么结构”？
```

下一步 Node 015：

```text
Java DTO
```

回答：

```text
这个结构怎样真正进入 Java 类型系统？
```

---

# 一百一十四、这三层不要混

```text
Structured Output
=
目标 / 机制

JSON Schema
=
结构契约

Java DTO
=
Java 侧类型表示
```

三者相关，但不是一个东西。

---

# 一百一十五、完整链路再看一次

```text
Task / Prompt
      │
      ▼
   JSON Schema
      │
      ▼
     Model
      │
      ▼
Structured Output
      │
      ▼
   JSON Result
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

现在我们已经走到：

```text
JSON Schema
```

这一层。

---

# 一百一十六、真正需要记住的 20 个结论

1. JSON 是具体数据，JSON Schema 是数据结构规则。
2. JSON Schema 用来描述 JSON 应该长什么样。
3. `type` 定义数据类型。
4. 常见类型包括 object、array、string、number、integer、boolean、null。
5. `properties` 定义 object 中有哪些字段以及字段 Schema。
6. `properties` 中定义字段，不代表字段一定必须存在。
7. `required` 定义必须存在的字段。
8. required 和“允许 null”不是同一个概念。
9. `enum` 用来限制字段只能取有限几个值。
10. enum 对分类、状态、路由、Agent action 非常重要。
11. `array` 用来描述数组。
12. `items` 用来描述数组中元素的结构。
13. object 可以嵌套 object。
14. array 中也可以放 object。
15. `additionalProperties: false` 可以禁止未定义额外字段。
16. Schema 是数据契约，不是 Few-shot 示例。
17. Schema 限制输出结构，但不能替代 Prompt 的任务定义。
18. Schema 正确不代表模型事实一定正确。
19. Schema Validation 和 Business Validation 是两层不同校验。
20. JSON Schema 是 Node 015 Java DTO Structured Output 的直接前置知识。

---

# 一百一十七、理解自测

## 基础

1. JSON 和 JSON Schema 有什么区别？
2. `type` 是做什么的？
3. object 和 array 有什么区别？
4. `properties` 是做什么的？
5. `required` 是做什么的？
6. 为什么 properties 和 required 不能混淆？
7. `enum` 为什么对 LLM 很重要？
8. `items` 是做什么的？
9. `additionalProperties: false` 有什么意义？

---

## 类型

10. integer 和 number 有什么区别？
11. boolean 适合什么字段？
12. string 是否适合所有字段？
13. 为什么年龄不应该默认写成 string？
14. 为什么 confidence 可能需要 number？

---

## 嵌套

15. object 里面能不能有 object？
16. object 里面能不能有 array？
17. array 里面能不能有 object？
18. 嵌套对象中的 required 应该写在哪一层？

---

## 工程理解

19. 为什么 Schema 是程序与模型之间的数据契约？
20. 为什么 Schema 不能替代 Prompt？
21. 为什么 Schema 不能消灭幻觉？
22. 为什么还需要 Business Validation？
23. 为什么 Schema 不应该无限复杂？
24. 为什么 enum 修改可能影响业务代码？
25. 为什么 Tool Calling 也需要 Schema？

---

# 一百一十八、口述验收

如果有人问：

> JSON Schema 不就是一堆 JSON 配置吗？

你应该能够解释：

> JSON Schema 不是某一份业务数据，而是对 JSON 数据结构的描述。它定义对象里有哪些字段、字段是什么类型、哪些字段必须存在、哪些字段只能取有限值、数组里允许放什么，以及是否允许额外字段。
>
> 在 LLM Structured Output 里，JSON Schema 的价值在于把模型原本非常自由的输出空间限制到程序能处理的结构中。比如风险等级不能让模型随便回答“很高”“严重”“High”，而是通过 enum 只允许 HIGH、MEDIUM、LOW。
>
> 但 Schema 只解决结构契约，不负责保证业务事实一定正确，也不能替代 Prompt、RAG 或业务校验。它只是把模型输出可靠地接入程序的一层基础设施。

如果你能够自然讲清：

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

并且能够从一个目标 JSON 自己写出基础 Schema：

```text
Node 014 核心认知
基本建立
```

---

# 一百一十九、Node 014 完成标准

读完本篇以后，应该能够：

- [ ] 理解 JSON 和 JSON Schema 的区别；
- [ ] 理解 Schema 为什么是数据契约；
- [ ] 掌握 `type`；
- [ ] 掌握 object；
- [ ] 掌握 `properties`；
- [ ] 掌握 `required`；
- [ ] 掌握 `enum`；
- [ ] 掌握 array；
- [ ] 掌握 `items`；
- [ ] 理解 object / array 嵌套；
- [ ] 理解 `additionalProperties`；
- [ ] 理解 required 与 null / optional 的区别；
- [ ] 能读懂简单 Schema；
- [ ] 能根据目标 JSON 写简单 Schema；
- [ ] 能指出常见 Schema 违规；
- [ ] 理解 Schema Validation 与 Business Validation 的区别；
- [ ] 理解 Schema 不能替代 Prompt；
- [ ] 理解 Schema 和 Tool Calling 的关系；
- [ ] 理解 Schema 和 Java DTO 的关系。

本 Node 当前：

```text
不单独要求 Java API 实验
```

但是和 Node 013 不同的是：

```text
建议自己真正手写几个 Schema
```

因为 JSON Schema 本身就是：

```text
需要动手才能真正熟悉的结构语言
```

---

# 一百二十、建议做的最小练习

不用启动模型。

只需要自己手写下面 4 个 Schema：

```text
1. User
   name: string
   age: integer

2. Ticket
   type: BUG / FEATURE / QUESTION
   priority: P0 / P1 / P2 / P3

3. ArticleAnalysis
   summary: string
   keywords: string[]

4. CampaignAnalysis
   riskLevel: HIGH / MEDIUM / LOW
   reason: string
   suggestion: string
```

然后检查：

```text
type 对不对
properties 对不对
required 对不对
enum 对不对
array 有没有 items
是否需要 additionalProperties: false
```

这就是 Node 014 最值得做的实践。

---

# 一百二十一、为什么这里不需要 Codex 大实验？

因为这个 Node 主要学习：

```text
Schema 语言本身
```

如果现在立刻让 Codex 把：

```text
Java
HTTP
Provider
Structured Output
DTO
Validation
```

全部写出来，

注意力会从：

```text
JSON Schema
```

转移到：

```text
代码调用细节
```

真正完整的 Java 实验放到：

```text
Node 015
```

更合适。

---

# 一百二十二、Node 014 的停止点

当你能够：

```text
看懂 Schema
↓
自己写简单 Schema
↓
指出数据哪里违反 Schema
```

就可以结束 Node 014。

不需要：

```text
背完整 JSON Schema 标准

研究所有高级关键字

研究所有 Draft 差异

写复杂 Schema 框架

做 Provider 抽象
```

这些都不属于当前停止点。

---

# 一百二十三、下一步：Node 015

Node 015：

```text
Java DTO 与 Structured Output
```

会真正把：

```text
JSON Schema
```

接进我们现有：

```text
agent-learning-lab
```

目标链路：

```text
Java
↓
构造 Structured Output 请求
↓
JSON Schema
↓
Qwen
↓
Structured JSON
↓
FastJSON2
↓
Java DTO
↓
Validation
↓
Business Logic
```

到那里：

```text
Structured Output
才算真正从理论走进代码
```

---

# 本篇一句话总结

> **JSON Schema 的本质，是用机器可读的方式明确规定：一个 JSON 结果允许有哪些字段、字段是什么类型、哪些必须存在、哪些值合法，以及整个结构应该怎样组织。**

```text
JSON
=
具体数据

JSON Schema
=
数据契约
```

而在 LLM 工程里：

```text
Prompt
负责告诉模型“做什么”

JSON Schema
负责告诉模型“结果必须长什么样”
```

**Node 014：JSON Schema 理论与基础结构学习。**

## 本次 User Schema 手写记录（2026-11-03）

用户写出了 object、name/string、age/integer，并列出必填字段 name、age。
原稿将 required 放在 properties 里面，最外层还缺少闭合括号；已说明 required 应与 properties 同级，并提供下面的修正版：

```json
{
  "type": "object",
  "properties": {
    "name": {"type": "string"},
    "age": {"type": "integer"}
  },
  "required": ["name", "age"]
}
```

本次仅在对话中检查写法，没有运行 Schema 校验器。此 Schema 未禁止额外字段，未添加或声称 additionalProperties: false。
用户确认本节到此结束，其余建议练习不追加；前面的理解清单供回顾，不代表本次逐题验收记录。
