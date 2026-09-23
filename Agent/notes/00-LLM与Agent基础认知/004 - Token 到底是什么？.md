# 04 - Token 到底是什么？

> AI Agent 学习笔记 · LLM 基础
>
> 前面已经学完：
>
> ```text
> 01 - LLM 到底是什么？
> 02 - 模型训练与推理到底是什么？
> 03 - LLM 幻觉到底是什么？
> ```
>
> 这一篇开始接触一个以后几乎无处不在的概念：
>
> **Token**
>
> 以后你会不断看到：
>
> ```text
> Input Token
> Output Token
> Token Limit
> Token Usage
> Token Cost
> Context Token
> 1M Tokens
> Max Output Tokens
> ```
>
> 如果 Token 没搞明白，后面的：
>
> ```text
> Context Window
> RAG
> Memory
> Agent Cost
> Prompt Engineering
> Context Engineering
> ```
>
> 都会有点飘。
>
> 所以这一篇的目标就是：
>
> > **彻底搞明白“模型眼里的文字”到底是什么。**

------

# 一、先给 Token 一个最简单的定义

Token 可以暂时理解为：

> **LLM 处理文本时使用的基本单位。**

我们人类看到的是：

```text
Java 是一门编程语言。
```

模型并不是直接像人一样：

```text
“我看到了一句话。”
```

而是会先把文本转换成一串：

```text
Token
```

然后再处理。

因此整个过程可以简单理解为：

```text
人类文字
   ↓
Tokenizer
   ↓
Token
   ↓
LLM
```

模型生成答案时则反过来：

```text
LLM
 ↓
生成 Token
 ↓
Decode
 ↓
我们看到的文字
```

------

# 二、为什么模型不直接处理文字？

因为神经网络最终处理的是：

```text
数字
```

而不是：

```text
Java
苹果
你好
Spring Boot
```

这些文字本身。

计算机模型需要把：

```text
文本
```

转换成：

```text
数字形式
```

才能进一步计算。

所以会经过：

```text
Text
 ↓
Tokenizer
 ↓
Token IDs
 ↓
Model
```

例如为了理解流程，我们暂时假设：

```text
我喜欢Java
```

被切成：

```text
[我]
[喜欢]
[Java]
```

然后每个 Token 对应某个数字：

```text
[我]      → 4382
[喜欢]    → 9281
[Java]    → 17392
```

于是模型真正接收到的可能更像：

```text
4382
9281
17392
```

当然，现实 tokenizer 的切分结果和数字并不一定是这样。

这里只是为了建立直觉。

------

# 三、Token 不是“字”

这是特别重要的一点。

不要理解：

```text
1 个汉字 = 1 Token
```

不一定。

Token 可能是：

```text
一个字
```

也可能：

```text
一个词的一部分
```

也可能：

```text
一个完整单词
```

甚至：

```text
标点
空格
代码符号
```

都可能参与 Token 化。

所以：

> **Token 是模型自己的文本切分单位，并不是自然语言里的“字”或者“单词”。**

------

# 四、Token 也不是“单词”

比如英文：

```text
Java
```

某个 tokenizer 可能把它看成：

```text
[Java]
```

一个 Token。

但一个比较少见的单词：

```text
unbelievability
```

可能被切成：

```text
[un]
[believ]
[ability]
```

也可能采用其他方式。

具体怎么切：

> **由这个模型使用的 tokenizer 决定。**

------

# 五、所以 Token 最准确的理解是什么？

当前阶段可以记成：

> **Token 是 Tokenizer 根据自己的词表和规则，将文本切分后得到的模型基本输入/输出单位。**

整个链：

```text
“我正在学习 Java”
        ↓
     Tokenizer
        ↓
 Token A
 Token B
 Token C
 Token D
        ↓
       LLM
```

------

# 六、什么是 Tokenizer？

```
Tokenizer
```

可以理解为：

> **负责把文本变成 Token，以及把 Token 转换回可读文本的组件。**

它至少涉及两个重要过程：

```text
Encode
```

和：

```text
Decode
```

------

# 七、Encode 是什么？

比如输入：

```text
Java 很好用
```

经过 Tokenizer：

```text
Text
 ↓
Encode
 ↓
Token IDs
```

可能得到：

```text
[15321, 836, 19284, ...]
```

也就是说：

> **文字 → 模型能够处理的 Token。**

------

# 八、Decode 是什么？

模型最终生成的是：

```text
Token ID
Token ID
Token ID
……
```

Tokenizer 再：

```text
Token IDs
 ↓
Decode
 ↓
文字
```

于是你看到：

```text
Java 是一种广泛使用的编程语言。
```

所以完整过程：

```text
用户文字
   ↓
Tokenizer Encode
   ↓
Tokens
   ↓
LLM
   ↓
Tokens
   ↓
Tokenizer Decode
   ↓
模型回答
```

------

# 九、这和 Java 序列化有点像吗？

可以做一个非常粗略的类比。

你 Java 里可能有：

```java
User user = new User();
```

通过 Jackson：

```text
Java Object
↓
JSON
```

或者：

```text
JSON
↓
Java Object
```

Tokenizer 的作用当然和 Jackson 完全不是一回事。

但是从“转换层”的直觉上，可以类比：

```text
人类能理解的文本
↓
Tokenizer
↓
模型能处理的表示
```

Tokenizer 就是：

> **人类文字和模型输入之间的重要转换层。**

------

# 十、Token 为什么不直接一个字一个字切？

一个最简单的方案确实可以：

```text
我 / 喜 / 欢 / J / a / v / a
```

但是这样会带来问题。

例如英文：

```text
programming
```

如果按字符：

```text
p
r
o
g
r
a
m
m
i
n
g
```

太碎了。

而如果整个单词全部作为一个单位：

```text
[programming]
```

又会碰到：

```text
世界上的词太多
新词太多
名字太多
代码标识符太多
```

不可能把世界上所有字符串都提前放进一个有限词表。

所以现实 Tokenizer 往往会选择一种折中：

> **常见内容尽量组合成较大的 Token，罕见内容则拆成更小的部分。**

------

# 十一、一个直觉例子

假设 tokenizer 的词表里已经非常熟悉：

```text
Java
```

于是：

```text
Java
```

可能是：

```text
1 Token
```

但是：

```text
SuperAmazingJavaFrameworkXYZ
```

这种奇怪字符串可能被拆成：

```text
Super
Amazing
Java
Framework
X
YZ
```

于是 Token 数明显更多。

再次强调：

> 实际怎么切取决于具体 tokenizer。

我们目前重点是理解原理，不是背某个模型的具体切分。

------

# 十二、为什么不同模型的 Token 数可能不同？

因为：

> **不同模型可能使用不同 Tokenizer。**

例如同一句：

```text
中华人民共和国
```

在：

```text
模型 A
```

可能被分成：

```text
3 Tokens
```

在：

```text
模型 B
```

可能：

```text
5 Tokens
```

甚至其他数量。

所以以后不要说：

> “这段文字一定是 1234 Token。”

除非：

```text
模型
+
Tokenizer
```

已经确定。

------

# 十三、Token 和模型是什么关系？

可以理解成：

```text
Tokenizer
决定怎么切

Model
学习这些 Token 之间的关系
```

训练时：

```text
文本
↓
Tokenizer
↓
Tokens
↓
Model Training
```

推理时：

```text
用户输入
↓
同类 Tokenizer
↓
Tokens
↓
Model Inference
```

因此 Tokenizer 是整个模型体系里非常基础的一部分。

------

# 十四、回到第一篇：模型预测的到底是什么？

我们以前说：

> LLM 本质上不断预测下一个 Token。

现在这个概念终于可以更准确理解了。

比如：

```text
Java 是一种
```

Tokenizer 变成：

```text
[Java] [是] [一种]
```

模型根据已有 Tokens：

```text
[Java]
[是]
[一种]
```

预测：

```text
下一个 Token 是什么？
```

可能概率类似：

```text
编程       52%
面向       10%
广泛        7%
高级        5%
……
```

假设选中：

```text
编程
```

Context 变成：

```text
Java 是一种编程
```

模型再预测。

如此不断继续。

------

# 十五、模型是不是一个字一个字“吐”答案？

我们视觉上经常觉得：

```text
模型一个字一个字往外蹦。
```

真正更准确的说法是：

> **模型一个 Token 一个 Token 地生成。**

一个 Token 可能包含：

```text
一个汉字
几个字符
一个单词
单词的一部分
标点
其他内容
```

所以：

```text
一个 Token
≠
一个字
```

------

# 十六、这也解释了 Streaming

以后学习 Streaming 时，你会看到：

```text
模型正在逐渐返回内容。
```

底层大概：

```text
生成 Token
↓
返回部分内容
↓
生成 Token
↓
继续返回
↓
……
```

所以所谓：

```text
流式输出
```

和 Token 生成机制联系非常紧密。

------

# 十七、什么叫 Input Token？

现在进入 API 工程概念。

```
Input Token
```

就是：

> **这次模型调用中，作为输入提交给模型处理的 Token。**

比如：

```text
请解释一下 Java 中的 synchronized。
```

这句话经过 Tokenizer 后可能形成若干 Tokens。

这些属于：

```text
Input Tokens
```

------

# 十八、只有 User Message 算 Input Token 吗？

不一定。

这是 Agent 工程里特别重要的一点。

一次模型调用的 Input 很可能包含：

```text
System Prompt
+
User Message
+
Conversation History
+
RAG Documents
+
Tool Results
+
Tool Definitions
+
其他 Context
```

它们统统可能占：

```text
Input Tokens
```

所以以后：

```text
用户只输入一句话
```

并不代表：

```text
Input Token 很少。
```

------

# 十九、举个 Agent 例子

用户只说：

```text
分析 campaign 123。
```

看起来只有几个字。

但 Agent 真正发给模型的内容可能是：

```text
System Prompt
2000 Tokens

聊天历史
3000 Tokens

Tool Definitions
4000 Tokens

RAG
6000 Tokens

用户问题
10 Tokens
```

所以总输入：

```text
约 15010 Tokens
```

用户只输入了十来个 Token。

但你的系统实际花掉：

```text
上万 Input Tokens。
```

这就是为什么 Agent 成本优化不能只看：

> 用户输入有多长。

------

# 二十、什么是 Output Token？

```
Output Token
```

就是：

> **模型生成出来的 Token。**

例如模型回答：

```text
synchronized 是 Java 提供的一种同步机制……
```

整个回答经过 Token 计数：

```text
生成了 N Tokens
```

这些就是：

```text
Output Tokens
```

------

# 二十一、所以一次调用可以简化为

```text
Input Tokens
     ↓
    LLM
     ↓
Output Tokens
```

例如：

```text
Input：1000 Tokens

Output：500 Tokens
```

这次调用的 Token 使用情况：

```text
输入 1000
输出 500
```

------

# 二十二、为什么 API 要区分输入和输出？

因为：

```text
输入模型
```

和：

```text
模型真正一步一步生成新 Token
```

计算成本并不完全相同。

因此很多模型 API 都会分别统计：

```text
Input Token
Output Token
```

而价格也可能不同。

通常：

```text
Output Token
```

的单价可能比 Input Token 更贵。

但具体价格必须看模型提供商当前定价。

------

# 二十三、Token Cost 是什么？

很多 LLM API：

```text
不是按一次请求固定收钱
```

而是主要根据：

```text
处理了多少 Token
```

计费。

可以粗略写成：

```text
Cost
=
Input Token Cost
+
Output Token Cost
```

再展开：

```text
Input Tokens
×
Input Price

+

Output Tokens
×
Output Price
```

------

# 二十四、举一个纯数学例子

假设某模型：

```text
输入：
100万 Token = 2 元

输出：
100万 Token = 8 元
```

这只是示例数字，不代表任何真实模型当前价格。

一次调用：

```text
Input = 10,000 Tokens

Output = 2,000 Tokens
```

那么：

```text
Input Cost
=
10,000 / 1,000,000 × 2
=
0.02 元
```

Output：

```text
2,000 / 1,000,000 × 8
=
0.016 元
```

总：

```text
0.036 元
```

这就是 Token 计费直觉。

------

# 二十五、为什么写一句 Prompt 也花钱？

例如你写：

```text
你是一名资深 Java Engineer……
```

这段 Prompt：

```text
也是 Input。
```

所以：

```text
System Prompt 越长
↓
Input Token 越多
↓
通常成本越高
```

------

# 二十六、那是不是 Prompt 越短越好？

也不是。

如果为了省：

```text
100 Tokens
```

把关键规则删掉：

```text
Agent 行为质量下降
```

那就得不偿失。

正确目标不是：

```text
Token 越少越牛
```

而是：

> **在保证任务效果的情况下，减少无意义的 Token。**

这以后就是：

```text
Context Engineering
```

的一部分。

------

# 二十七、RAG 为什么特别容易产生大量 Token？

比如 RAG 检索：

```text
Top 10 Documents
```

每篇：

```text
1000 Tokens
```

那光 RAG：

```text
10000 Tokens
```

如果其实真正有用的只有：

```text
2 个 Chunk
```

其余：

```text
8000 Tokens
```

不仅：

```text
花钱
```

还可能：

```text
干扰模型注意力。
```

所以后面 RAG 会学习：

```text
Top K
Chunk Size
Rerank
Retrieval Quality
```

这些都和 Token 有关系。

------

# 二十八、Memory 也会消耗 Token

假设 Agent 一直把完整聊天记录带上：

```text
第1轮
1000 Tokens

第2轮
又加1000

第3轮
再加1000

……
```

聊天越来越长。

那么每一轮：

```text
需要重新给模型大量历史 Context
```

Input Token 会越来越高。

所以以后 Memory 不可能永远简单：

```text
把所有历史消息全部塞进去。
```

这就是为什么会有：

```text
Message Window
Token Window
Summary Memory
Memory Retrieval
```

------

# 二十九、Tool Definition 也吃 Token

以后 Tool Calling 时，你可能给模型：

```text
Tool A
Tool B
Tool C
……
```

每个 Tool 都包含：

```text
name
description
parameters
schema
```

这些信息必须让模型看到。

所以它们也属于：

```text
Context
```

进而占用：

```text
Input Tokens。
```

------

# 三十、如果有 300 个 Tool 会发生什么？

假设：

```text
一个 Tool Schema
约 300 Tokens
```

300 个：

```text
90,000 Tokens
```

这还没算：

```text
System Prompt
用户问题
历史
RAG
```

所以以后你会学：

```text
Tool Search
Tool Discovery
Dynamic Tool Loading
```

目的之一就是：

> **不要每一次把世界上所有 Tool 都塞给模型。**

------

# 三十一、Agent 的 Token 消耗为什么特别容易高？

普通聊天：

```text
User
↓
LLM
↓
Answer
```

可能调用一次模型。

Agent：

```text
User
↓
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
Answer
```

一次任务：

```text
可能调用 3 次
5 次
10 次
```

而且每次都可能携带：

```text
越来越长的 Context
```

所以 Agent Token 消耗可能快速增长。

------

# 三十二、举个例子

第一次：

```text
Input：3000
Output：300
```

模型决定调用 Tool。

第二次：

```text
原 Context
+
Tool Result
```

可能：

```text
Input：4000
Output：200
```

又调用 Tool。

第三次：

```text
更长 Context
```

：

```text
Input：5500
Output：500
```

总 Token 消耗不是：

```text
5500 + 500
```

而是三次调用全部加起来：

```text
3000 + 300
+
4000 + 200
+
5500 + 500
```

=

```text
13500 Input
+
1000 Output
```

所以：

> **Agent Turn 数量和 Token 成本关系很大。**

------

# 三十三、Token 和“上下文长度”有什么关系？

下一篇就是 Context Window。

现在先建立概念：

模型一次调用：

```text
能处理的 Token 数量
```

不是无限的。

假设某模型：

```text
Context Window = 128K Tokens
```

那么：

```text
System Prompt
+
History
+
RAG
+
User Input
+
Tool Information
+
模型生成需要的空间
```

都受到上下文限制。

所以：

> **Context Window 本质上也是用 Token 衡量的。**

------

# 三十四、128K Token 是不是 128K 个字？

不是。

这句话现在应该可以直接判断：

```text
Token
≠
字
```

所以：

```text
128K Tokens
```

不能简单说：

```text
128K 汉字
```

具体能放多少文字：

> 与语言、文本类型和 tokenizer 有关。

------

# 三十五、为什么中文和英文 Token 数不一样？

因为：

```text
语言结构不同
+
Tokenizer 词表不同
+
训练数据分布不同
```

英文里常见：

```text
programming
Java
the
function
```

可能作为比较高效的 Token 单位出现。

中文：

```text
中华人民共和国
```

可能被拆成：

```text
中华
人民
共和国
```

也可能是其他方式。

具体仍然取决于 tokenizer。

------

# 三十六、不能简单说“中文一个字一个 Token”

这是非常常见的说法，但过于粗糙。

实际上：

```text
一个汉字
```

可能是一个 Token。

几个汉字的常见词：

```text
也可能组合成 Token。
```

特殊字、冷门字：

```text
又可能拆得不同。
```

所以不要死记：

```text
中文：1字=1Token
英文：1词=1Token
```

都不准确。

------

# 三十七、代码的 Token 又怎么样？

代码是 Agent / Coding Agent 中非常重要的场景。

例如：

```java
public String getUserName(Long userId) {
    return userService.getName(userId);
}
```

Tokenizer 不会理解为：

```text
一行代码 = 一个 Token
```

它可能切分：

```text
public
String
get
User
Name
(
Long
user
Id
)
……
```

实际结果视 tokenizer 而定。

------

# 三十八、为什么代码容易占很多 Token？

因为代码中有大量：

```text
变量名
类名
括号
符号
字符串
缩进
关键字
```

特别是超长标识符：

```text
CampaignMaterialBatchCreateRequestDTO
```

可能被拆成多个 Token。

大型仓库：

```text
几十万行代码
```

当然不可能随便全部塞给模型。

这就是 Coding Agent 为什么要做：

```text
Repository Search
Code Retrieval
文件选择
Context Management
```

------

# 三十九、JSON 也会占大量 Token

以后 Tool Calling：

```json
{
  "campaignId": 123,
  "campaignName": "test",
  "status": "ACTIVE"
}
```

你看着很短。

但 JSON 有大量：

```text
{
"
:
,
字段名
值
}
```

同样都会形成 Token。

如果 Tool 返回：

```text
500KB JSON
```

直接整个塞回模型：

```text
成本高
Context 占用大
模型理解压力大
```

所以后面会专门学习：

> **Tool Result 应该只返回模型真正需要的数据。**

------

# 四十、这就是为什么不能把整个数据库查询结果扔给模型

比如：

```sql
SELECT *
FROM campaign_metrics
LIMIT 100000;
```

然后：

```text
100000 行
↓
转 JSON
↓
全部塞 LLM
```

非常糟糕。

应该：

```text
业务层
↓
先过滤 / 聚合
↓
只给 Agent 需要的数据
```

例如：

```json
{
  "todayCpa": 65.3,
  "sevenDayAvgCpa": 38.2,
  "todayCvr": 0.031,
  "sevenDayAvgCvr": 0.052
}
```

让 LLM：

```text
做分析
```

而不是：

```text
让 LLM 当数据仓库。
```

------

# 四十一、Token 为什么影响速度？

因为模型生成答案时：

```text
一个 Token
↓
一个 Token
↓
一个 Token
```

逐步生成。

如果输出：

```text
50 Tokens
```

和：

```text
5000 Tokens
```

显然：

```text
5000 Tokens
```

通常需要更多计算和时间。

------

# 四十二、什么叫 Tokens Per Second？

以后看本地模型和推理性能时，经常看到：

```text
tokens/s
```

例如：

```text
50 tokens/s
```

意思：

> 模型平均每秒可以生成大约 50 个 Token。

这个指标通常用来描述：

```text
生成速度。
```

------

# 四十三、但 API 延迟不只有 Token 生成速度

完整延迟还包括：

```text
网络
排队
输入处理
模型 Prefill
Reasoning
Token Generation
Tool Call
其他系统开销
```

所以：

```text
tokens/s
```

不是：

```text
整个请求速度的唯一指标。
```

------

# 四十四、什么是 First Token Latency？

后面 Streaming 会详细学习。

现在先理解：

用户发请求：

```text
Request
↓
等待……
↓
第一个字出现
```

这个等待时间非常影响用户体验。

可以叫：

```text
Time to First Token
```

或者类似概念。

然后后续：

```text
Token
Token
Token
```

持续生成。

------

# 四十五、为什么 Streaming 看起来“更快”？

比如完整请求：

```text
10 秒后
↓
一次返回整个答案
```

用户：

```text
干等 10 秒。
```

Streaming：

```text
2 秒
↓
开始看到第一个 Token

2.1 秒
↓
更多

2.2 秒
↓
更多
```

即使：

```text
总生成时间仍是10秒左右
```

用户体感：

```text
明显更快。
```

------

# 四十六、Token 会不会影响模型推理能力？

间接会。

例如你给模型：

```text
100000 Tokens
```

的大量无关内容。

真正问题：

```text
只有一句。
```

虽然模型理论上可能支持这么长的 Context，

但：

```text
有能力装下
≠
所有内容都同样容易被正确利用。
```

无关 Token 可能：

```text
增加成本
增加延迟
增加噪声
影响重要信息利用
```

所以：

> **Context 越多不一定越好。**

这就是以后 Context Engineering 的核心意识之一。

------

# 四十七、“1M Token”到底有多少？

不能给一个适用于所有语言和所有模型的固定答案。

因为：

```text
Tokenizer 不同
语言不同
内容不同
```

所以：

```text
1M Tokens
```

最准确的理解就是：

> **一百万个模型 Token 单位。**

而不是：

```text
固定等于多少汉字
```

或者：

```text
固定等于多少英文单词。
```

------

# 四十八、网上为什么经常有人换算 Token 和文字数量？

因为为了方便理解，会做：

```text
经验估算。
```

例如对某种语言、某个 tokenizer：

```text
平均多少字符 ≈ 1 Token
```

可以作为：

```text
容量规划的粗估
```

但不能当成：

```text
绝对公式。
```

真正准确：

> **直接用目标模型对应的 tokenizer 计算。**

------

# 四十九、以后怎么知道一段文字有多少 Token？

最靠谱的方法：

```text
使用模型厂商提供或兼容的 Tokenizer
```

输入：

```text
你的 Prompt
```

然后：

```text
真正 Encode
```

得到 Token 数。

如果 API Response 本身返回：

```text
usage
```

通常也可以查看实际：

```text
input tokens
output tokens
total tokens
```

具体字段由 API 决定。

------

# 五十、以后我们的 DeepSeek 项目为什么要打印 Usage？

第一次 Java 调 DeepSeek 时，除了看：

```text
回答内容
```

我会建议我们也观察：

```text
Token Usage
```

例如：

```text
Input Tokens: xxx
Output Tokens: xxx
```

这样你会亲眼看到：

```text
一句 Prompt
↓
到底消费多少 Token。
```

这比纯看概念印象深得多。

------

# 五十一、一个 Java Agent 的 Token 账本

以后甚至可以设计：

```java
class TokenUsage {

    long inputTokens;

    long outputTokens;

    long totalTokens;
}
```

每次 Agent 调模型：

```text
记录
↓
数据库 / Metrics
```

最终统计：

```text
这个 Agent 今天用了多少 Token？

哪个用户最贵？

哪一步最耗 Token？

RAG 加入后成本增加多少？
```

这就是 Production Agent 后面的 Cost Observability。

------

# 五十二、为什么 Agent Engineer 必须有 Token 意识？

因为你可能写：

```text
一个功能
```

功能完全正确。

但是：

```text
一次请求 20 万 Tokens
```

一天：

```text
10 万次请求
```

成本可能完全不可接受。

于是：

> **功能能跑 ≠ 系统能上线。**

------

# 五十三、一个极端 Agent 例子

System Prompt：

```text
10000 Tokens
```

聊天历史：

```text
30000 Tokens
```

RAG：

```text
50000 Tokens
```

200 个 Tools：

```text
50000 Tokens
```

User：

```text
10 Tokens
```

结果：

```text
用户只问：
“查下今天CPA”
```

但：

```text
Input
≈ 140000 Tokens
```

这就是一个明显需要优化的 Agent。

------

# 五十四、怎么优化？

后面分别学习。

### Prompt

```text
删掉重复规则
```

### Memory

```text
Summary / Window / Retrieval
```

### RAG

```text
更精准检索
Rerank
减少无关 Chunk
```

### Tools

```text
只加载当前任务需要的 Tools
```

### Tool Result

```text
只返回必要字段
```

这全部：

> **同时改善 Token、成本、速度以及 Context 质量。**

------

# 五十五、Token 和 Prompt Engineering 的关系

假设 Prompt：

```text
你必须非常非常非常认真地……
你一定一定一定不要……
请千万千万注意……
```

大量重复内容：

```text
增加 Token
```

但不一定：

```text
增加效果。
```

所以好的 Prompt 不等于：

```text
越长越好。
```

而是：

```text
信息明确
结构清晰
规则必要
没有无意义重复
```

------

# 五十六、Token 和 Context Engineering 的关系更大

以后 Agent 真正的问题通常不是：

> “Prompt 这句话应该怎么润色？”

而是：

```text
到底应该给模型看到哪些 Token？
```

例如：

```text
System Prompt
哪些？

History
保留哪些？

RAG
取哪些？

Tools
提供哪些？

Memory
召回哪些？

Tool Result
留下哪些？
```

这就是 Context Engineering。

所以：

> **Context Engineering 某种程度上就是在管理模型有限且有成本的 Token 预算。**

------

# 五十七、Token Budget 是什么？

可以理解：

> **这一次任务允许使用多少 Token 资源。**

例如：

```text
最多 32K Context
```

你可能规划：

```text
System Prompt    2K
User + History   5K
RAG             10K
Tools            5K
预留输出          5K
安全余量          5K
```

这就是一种：

```text
Token Budget
```

意识。

以后复杂 Agent 很重要。

------

# 五十八、为什么一定要给 Output 留空间？

假设模型 Context 最大：

```text
128K
```

你把输入硬塞：

```text
128K
```

那模型还需要：

```text
生成答案。
```

所以实际系统通常需要考虑：

```text
Input
+
Output
```

共同占据模型允许的范围或受到各自限制。

具体规则取决于模型 API。

因此不能：

```text
Context Window = 128K
→ 我就一定可以塞128K输入再生成无限输出
```

------

# 五十九、这就是下一篇 Context Window 的入口

Token 解决：

> **模型用什么单位处理文本？**

Context Window 解决：

> **模型一次最多能够处理多少这些 Token？**

因此：

```text
Token
↓
Context Window
```

是天然连续的两个知识点。

------

# 六十、现在做路线要求里的几个 Token 实验

路线里安排了几个文本：

```text
你好
Hello
Spring Boot
中华人民共和国
```

真正实验的时候，我们应该：

```text
同一个模型 / 同一个 Tokenizer
```

分别 Encode。

观察：

```text
文本长度
vs
Token 数
```

然后再增加代码：

```java
public String getUserName(Long userId) {
    return userService.getName(userId);
}
```

比较：

```text
中文
英文
代码
```

Token 化结果。

------

# 六十一、实验重点不是记数字

假设你发现：

```text
你好 → X Tokens
Hello → Y Tokens
```

不要背：

```text
你好永远就是 X Token。
```

因为换模型：

```text
可能变化。
```

真正应该观察：

> **文字长度和 Token 数不是一一对应的，而且不同语言/代码经过 tokenizer 后表现不同。**

------

# 六十二、一个常见误区：字符数 = Token 数

错误：

```text
String.length()
```

返回：

```text
1000
```

于是说：

```text
1000 Tokens。
```

完全不可靠。

Java：

```java
text.length()
```

算的并不是模型 Token。

想知道 Token：

> **要使用对应 Tokenizer。**

------

# 六十三、另一个误区：文件大小 = Token 数

例如：

```text
PDF = 10MB
```

不能直接说：

```text
10MB = XX Tokens
```

因为 PDF 中可能有：

```text
文本
图片
字体
压缩数据
元数据
```

RAG 通常需要：

```text
PDF
↓
Parse
↓
抽取文本
↓
Tokenizer
↓
Token
```

才知道真正文本 Token。

------

# 六十四、Token 和图片是什么关系？

现在多模态模型还会处理：

```text
图片
音频
视频
```

这些模型通常也会把不同模态转换成模型内部能够处理的表示，并可能采用相应的计费和上下文单位。

不过当前我们的主线：

```text
Agent + Java + 文本 LLM
```

先把：

```text
Text Token
```

搞透。

后面如果专门学习多模态 Agent，再展开。

------

# 六十五、Token 和 Embedding 是一个东西吗？

不是。

后面 RAG 会详细讲 Embedding。

现在先区分：

```text
Token
=
文本切分后的基本单位
```

而：

```text
Embedding
=
把某个 Token / 文本等转换成向量表示
```

粗略流程：

```text
Text
↓
Tokenizer
↓
Tokens
↓
Embedding Representation
↓
Model Layers
```

所以：

```text
Token ≠ Embedding。
```

------

# 六十六、Token 和 Parameter 也不是一个东西

前面已经学：

```text
Parameter
=
模型训练后内部学习到的数值
```

Token：

```text
当前输入/输出的语言单位
```

例如：

```text
“Java”
```

是输入里的 Token 内容。

而模型内部：

```text
数十亿参数
```

负责处理这些 Token。

所以：

```text
Token
≠
Parameter
```

------

# 六十七、Token 和 Context 也不是一个东西

Token：

```text
基本单位
```

Context：

```text
当前这次推理里模型能够看到的信息集合
```

Context 是由大量 Token 组成的。

类似：

```text
字符
→ 文章
```

虽然这个类比不严格，但直觉上：

```text
Token
→ Context
```

就像：

```text
一个个小单位
→ 构成完整上下文。
```

------

# 六十八、四个概念一次分清

```text
Parameter
=
模型内部已经训练出的能力

Token
=
模型处理语言的基本单位

Context
=
当前推理时模型看到的 Token 集合

Context Window
=
一次能够处理的 Context Token 数量上限
```

这四个以后一定不要混。

------

# 六十九、Java 类比再来一次

假设：

```java
public Result handle(Request request)
```

可以做一个非常粗略类比：

```text
Parameters
≈
handle() 内已经写好的复杂逻辑能力
Tokens
≈
Request 中被模型处理的基本数据单位
Context
≈
这一轮传进来的完整 Request 信息
Inference
≈
执行 handle(request)
```

虽然底层完全不同，但对 Java 后端理解 Agent 很好用。

------

# 七十、为什么用户输入越来越长会越来越贵？

因为：

```text
前面的文字
↓
也需要模型处理。
```

例如：

```text
第一轮输入：
100 Tokens
```

第二轮如果你把上一轮历史再次提交：

```text
历史 300
+
新问题 100
=
400
```

第三轮：

```text
更多历史
+
新问题
=
700
```

所以：

```text
对话越长
```

在简单“全部历史重传”方案下：

```text
每轮 Input Token 越多。
```

------

# 七十一、这就是为什么“聊天记录”不是免费的

用户可能看到：

```text
我只新发了：
“继续”
```

两个字。

但后台可能：

```text
重新把前面数万 Token Context
```

一起发给模型。

因此这一轮真正成本：

```text
远远不是“继续”两个字。
```

------

# 七十二、模型能不能只记住历史而不用重新给？

这正好连接之前学过的 Memory。

普通 API 场景下：

```text
模型本身不会凭空拥有完整历史。
```

应用需要通过：

```text
历史消息
Session
Memory
Summary
State
```

让模型获得相关 Context。

不管具体 API 是否帮助你管理会话，最终模型推理：

> **仍然需要某种方式获得当前需要的信息。**

------

# 七十三、Agent 成本为什么不仅是钱的问题？

Token 多还会影响：

```text
Latency
Context Noise
信息检索难度
模型注意力分配
Context Limit
```

所以优化 Token：

> **不仅仅是省 API 钱。**

还是：

```text
质量优化
+
性能优化
+
架构优化。
```

------

# 七十四、以后 Debug Agent 要看 Token Usage

例如 Agent 最近突然：

```text
变慢了
```

或者：

```text
贵了两倍。
```

不要第一反应：

```text
模型涨价了吗？
```

应该观察：

```text
Input Token 是否变多？

RAG 是否多塞了大量 Chunk？

History 是否一直无限增长？

Tools 是否增加了很多？

Tool Result 是否返回巨量 JSON？

Agent Loop 是否多跑了几轮？
```

------

# 七十五、一个 Tool Result 的坏例子

```json
{
  "campaign": {
    "id": 123,
    "name": "...",
    "...": "另外几百个字段"
  },
  "rawData": [
    "...几万条数据..."
  ]
}
```

LLM 实际只需要：

```text
CPA
CTR
CVR
Spend
7日平均
```

那么正确工程方向：

```text
Tool
↓
Java先整理
↓
返回精简 DTO
```

例如：

```json
{
  "todayCpa": 65.3,
  "avg7dCpa": 38.2,
  "todayCtr": 0.019,
  "todayCvr": 0.031
}
```

这是：

> **Token 优化，也是数据边界设计。**

------

# 七十六、LLM 应该负责什么，Java 应该负责什么？

Token 这个知识点又一次强化：

Java：

```text
过滤
聚合
计算
校验
查询
压缩数据
```

LLM：

```text
理解
分析
推理
总结
解释
```

如果能在 Java 精确完成：

```text
10万行数据 → 5个关键指标
```

就不要：

```text
10万行全部扔给模型。
```

------

# 七十七、一个非常好的 Agent 设计意识

永远问：

> **模型真的需要看到这些 Token 吗？**

例如：

```text
100个数据库字段
```

模型需要吗？

如果只需要：

```text
5个
```

那就：

```text
只提供5个。
```

------

# 七十八、Token 不是越少越好，也不是越多越好

太少：

```text
信息不足
↓
模型判断错误
```

太多：

```text
成本增加
速度下降
噪声增加
Context紧张
```

理想状态：

```text
给模型完成任务所需的最小充分 Context。
```

这句话非常重要。

------

# 七十九、什么叫“最小充分 Context”？

例如分析：

```text
Campaign CPA为什么上涨？
```

模型可能需要：

```text
今日 CPA
7日平均 CPA
CTR
CVR
CPM
Spend
Conversions
```

而不需要：

```text
Campaign创建人手机号
数据库更新时间
无关素材历史
20年前的日志
```

所以：

> **不是信息越多模型越聪明，而是相关信息越准确越好。**

------

# 八十、Token 与 RAG 的本质关系

RAG 后面非常重要的一件事：

```text
几百万 Token 企业知识库
```

不可能：

```text
全部塞给模型。
```

所以才需要：

```text
Retrieval
```

找到：

```text
本次真正相关的几千 Tokens。
```

这就是 RAG 的重要价值之一：

> **从巨大知识库中选择少量相关 Context。**

------

# 八十一、Token 与 Memory 的本质关系

同样：

```text
用户过去10000条聊天
```

不可能：

```text
每次全部塞进去。
```

所以 Memory 后面会解决：

```text
哪些历史值得保留？

哪些要总结？

哪些需要检索？

哪些可以丢掉？
```

归根到底也是：

> **管理 Context Token。**

------

# 八十二、Token 与 Tool Search 的本质关系

Agent 有：

```text
1000个 Tools
```

不能全塞。

所以：

```text
当前问题
↓
先找相关 Tools
↓
只提供 5～10 个
↓
LLM 选择
```

又是在：

> **节约并优化 Context Token。**

------

# 八十三、所以 Token 是 Agent 工程的隐藏主线

现在看起来只是：

```text
文本切分单位
```

但未来：

```text
成本
Context
Memory
RAG
Tool
Latency
模型选择
Observability
```

都会再次碰到 Token。

所以这不是：

```text
一个无聊的小概念。
```

它是：

> **理解整个 LLM 工程资源模型的基础。**

------

# 八十四、这一篇最核心的 15 句话

如果以后整篇忘了，至少记住这些。

## 1

> **Token 是 LLM 处理文本时使用的基本单位。**

## 2

> **Token 不等于汉字，也不等于英文单词。**

## 3

> **Tokenizer 负责把文本转换成 Token，也负责把 Token 解码回文本。**

## 4

> **不同模型使用的 Tokenizer 可能不同，因此同一段文本的 Token 数也可能不同。**

## 5

> **LLM 实际是在一个 Token 一个 Token 地生成输出。**

## 6

> **Input Token 是模型本次推理看到的输入 Token。**

## 7

> **Output Token 是模型本次生成的 Token。**

## 8

> **System Prompt、History、RAG、Tool Definition 和 Tool Result 都可能属于 Input Token。**

## 9

> **API 成本通常和 Input / Output Token 数量直接相关。**

## 10

> **用户输入很短，不代表一次 Agent 请求的总 Input Token 很少。**

## 11

> **Agent 多轮 Tool Calling 会导致多次模型调用，因此 Token 消耗会累加。**

## 12

> **Context Window 的大小也是以 Token 为核心单位衡量的。**

## 13

> **RAG、Memory、Tool Search 很大程度上都在解决“应该给模型哪些 Token”。**

## 14

> **Context 不是越长越好，真正目标是提供任务所需的最小充分信息。**

## 15

> **Token 优化不仅是成本优化，同时也是性能和 Agent 质量优化。**

------

# 八十五、自测

不要先看上面。

尝试自己回答。

------

## Q1

Token 是什么？

------

## Q2

为什么不能简单理解：

```text
一个汉字 = 一个 Token
```

？

------

## Q3

Tokenizer 负责什么？

------

## Q4

为什么：

```text
Hello
```

在不同模型中可能得到不同 Token 数？

------

## Q5

什么是 Input Token？

------

## Q6

什么是 Output Token？

------

## Q7

下面哪些东西可能占 Input Token？

```text
System Prompt
用户问题
聊天历史
RAG文档
Tool定义
Tool结果
```

------

## Q8

为什么用户只输入：

```text
“继续”
```

一次请求仍然可能非常贵？

------

## Q9

为什么一个 Agent 有 300 个 Tools 时，不应该每次无脑把所有 Tool Schema 都发给模型？

------

## Q10

为什么 500KB Tool Result 不应该直接全部塞给模型？

------

## Q11

Token 和 Parameter 是一个东西吗？

分别是什么？

------

## Q12

Token 和 Context 是什么关系？

------

## Q13

为什么：

```java
text.length()
```

不能直接用来计算模型 Token 数？

------

## Q14

为什么：

```text
Context 越多
```

不一定：

```text
效果越好？
```

------

## Q15

RAG 为什么和 Token 管理关系很大？

------

# 八十六、学习完成标准

如果你能够不看笔记解释：

```text
文本
↓
Tokenizer
↓
Token
↓
LLM
↓
Output Token
↓
最终文字
```

并且理解：

```text
Token是什么
↓
Tokenizer是什么
↓
Input / Output Token是什么
↓
为什么Token影响价格
↓
为什么Agent特别烧Token
↓
为什么RAG / Memory / Tool都要管理Token
```

那么：

```text
Node 004：Token 到底是什么？

✅ PASS
```

------

# 八十七、建议做一个非常小的观察实验

这一篇目前依然不需要正式建 Agent 项目。

等第一次接 DeepSeek API 时，要专门观察：

```text
usage
```

里面实际的：

```text
Input Token
Output Token
Total Token
```

这些数字统计的是整次请求的用量，可能还包含消息结构等开销；`usage` 不会展示文本具体被切成哪些 Token。

要比较切分结果，请用目标模型匹配的 Tokenizer，分别对下面的文本做 Encode：

```text
你好

Hello

Spring Boot

中华人民共和国
```

再测试：

```java
public String getUserName(Long userId) {
    return userService.getName(userId);
}
```

亲眼看看目标模型 Tokenizer 如何处理：

```text
中文
英文
代码
```

不要提前猜。

切分以实际 Tokenizer 结果为准；完整请求的用量以 API 返回的 `usage` 为准。

------

# 八十八、下一篇

下一篇：

> **05 - Context Window 到底是什么？**

会正式解决：

```text
模型“一次能看到多少东西”是什么意思？

128K Context 到底是什么？

System Prompt、历史消息、RAG、Tool Result
是不是都在抢同一个 Context 空间？

Context 满了会发生什么？

为什么模型不会拥有无限记忆？

Lost in the Middle 是什么？

为什么聊天越长不能无限追加？

为什么 Memory ≠ 把所有聊天记录全塞进去？

Agent 为什么必须做 Context Engineering？
```

学完下一篇以后：

```text
LLM
Training / Inference
Hallucination
Token
Context Window
```

这五块基础就会正式连起来。
