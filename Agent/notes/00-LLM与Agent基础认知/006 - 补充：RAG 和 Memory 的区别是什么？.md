# 006 - 补充：RAG 和 Memory 的区别是什么？

在学习 Agent 时，经常会同时看到两个非常重要的概念：

```
RAG
Memory
```

而且它们看起来特别像。

因为两者最终经常都会做类似这样的事情：

```
从某个地方找信息
↓
把信息放进 Context
↓
交给 LLM
↓
让模型基于这些信息回答
```

甚至在具体实现上，两者都有可能使用：

```
Embedding
Vector Database
Similarity Search
Retrieval
```

所以很容易产生一个疑问：

> **既然最后都是“检索信息 → 放进 Context”，那么 RAG 和 Memory 到底有什么区别？**

这一篇就专门把这两个概念拆开。

# 一、先记住最简单的一句话

可以先这样理解：

```
RAG
=
查资料
```

而：

```
Memory
=
记经历
```

或者再准确一点：

> **RAG 主要解决“当前问题需要哪些外部知识”。**

> **Memory 主要解决“过去发生过什么、哪些信息需要被继续记住”。**

这是两者最核心的区别。

# 二、先看一个 RAG 的例子

假设我们开发一个：

```
广告投放 Agent
```

用户问：

```
Meta 广告为什么会因为素材问题审核失败？
```

模型自己的训练知识可能：

```
不够新
不够完整
不够可靠
```

于是我们的系统拥有一套知识库：

```
Meta 官方广告政策
广告审核规则
公司内部投放文档
平台异常说明
历史问题 FAQ
```

Agent 根据用户问题搜索：

```
Meta
素材
审核失败
```

找到几段最相关文档：

```
文档 A
文档 C
文档 F
```

然后：

```
文档 A
+
文档 C
+
文档 F
↓
加入 Context
↓
LLM
↓
回答
```

这就是典型的：

```
RAG
```

# 三、RAG 到底是什么意思？

RAG：

```
Retrieval-Augmented Generation
```

中文通常翻译为：

```
检索增强生成
```

拆开：

```
Retrieval
=
检索

Augmented
=
增强

Generation
=
生成
```

也就是说：

> **先从外部数据源中找到和当前问题相关的信息，再利用这些信息增强模型生成结果。**

基本流程：

```
User Question
↓
Retrieval
↓
Relevant Documents
↓
Context
↓
LLM
↓
Answer
```

# 四、RAG 解决的核心问题是什么？

比如模型训练时并不知道：

```
你们公司最新的接口规范
```

或者：

```
2026 年最新平台政策
```

或者：

```
公司昨天刚写的技术文档
```

这些信息没有必要：

```
重新训练模型
```

而是可以：

```
存入知识库
↓
需要时搜索
↓
给模型
```

所以 RAG 的核心思想是：

> **模型不知道的外部知识，需要的时候再去查。**

# 五、再看 Memory

假设你和 Agent 第一次聊天：

```
User：

我们公司的 Java 项目生产环境必须使用 JDK 8，
以后帮我写代码的时候注意一下。
```

系统认为这是一个长期有价值的信息，于是保存：

```
用户项目约束：
生产环境使用 JDK 8
```

几天以后用户又问：

```
帮我写一个字符串处理工具。
```

系统从 Memory 里取出：

```
生产环境使用 JDK 8
```

再构造 Context：

```
System:
你是一名 Java 开发助手。

Memory:
用户当前项目生产环境使用 JDK 8。

User:
帮我写一个字符串处理工具。
```

于是模型不会给你写：

```
String.formatted(...)
```

之类高版本 API。

这里的：

```
生产环境使用 JDK 8
```

就是：

```
Memory
```

# 六、Memory 到底解决什么问题？

Memory 解决的是：

> **模型本身没有永久保存每次交互，所以系统如何把过去有价值的信息保存下来，在未来需要时重新提供给模型。**

Node 005 已经讲过：

```
模型当前能看到的
=
Context
```

而不是：

```
模型永久记住所有过去的信息
```

因此：

```
过去发生的重要事情
```

如果希望未来仍然能使用，就需要：

```
保存
↓
召回
↓
重新进入 Context
```

这就是 Memory 的核心。

# 七、Memory 的典型流程

```
User Interaction
↓
提取有价值的信息
↓
Memory Store
↓
保存
```

以后：

```
New User Request
↓
Memory Retrieval
↓
Relevant Memory
↓
Current Context
↓
LLM
```

所以：

```
Memory
```

不是模型突然拥有：

```
永久大脑
```

而是一个应用层机制。

# 八、最核心的区别：Knowledge 和 History

可以先这样区分：

```
RAG
更偏 Knowledge
```

而：

```
Memory
更偏 History / Experience
```

例如：

```
Spring Boot 中 @Transactional 如何工作？
```

这是：

```
Knowledge
```

适合放：

```
技术文档知识库
↓
RAG
```

而：

```
我们这个项目数据库事务不允许跨数据源。
```

如果这是用户之前专门告诉 Agent 的项目约束：

更接近：

```
Memory
```

# 九、RAG 的信息一般从哪里来？

常见来源：

```
PDF
Word
Markdown
Wiki
Confluence
Notion
网页
产品文档
公司内部知识库
代码文档
FAQ
数据库中的知识数据
```

这些信息通常：

> **在用户当前这次聊天之前，就已经存在于某个外部知识源中了。**

# 十、Memory 的信息一般从哪里来？

常见来源：

```
过去聊天
用户偏好
用户身份相关设置
历史任务
Agent 之前采取过的行动
之前得出的结论
项目长期约束
历史决策
```

这些信息很多是：

> **用户与系统不断交互的过程中逐渐产生的。**

# 十一、举一个特别明显的对比

用户问：

```
Spring Boot 的 Bean 生命周期是什么？
```

系统搜索：

```
Spring 官方文档
公司 Java Wiki
```

这是：

```
RAG
```

用户又问：

```
我们之前不是说过，
这个项目不能使用循环依赖吗？
```

系统搜索：

```
过去项目讨论
历史聊天
保存的项目约束
```

这是：

```
Memory
```

# 十二、再用你的广告业务举例

## RAG

用户：

```
Google Ads 的某个出价策略有什么限制？
```

查询：

```
Google Ads 官方文档
内部业务文档
```

这是：

```
RAG
```

## Memory

用户：

```
之前我跟你说过，
我们这个项目的广告创建接口有哪些特殊限制？
```

查询：

```
过去聊天
项目长期记录
用户曾经确认过的设计决定
```

这是：

```
Memory
```

# 十三、RAG 和 Memory 最终为什么看起来很像？

因为最终可能都是：

```
Retrieve
↓
Context
↓
LLM
```

例如 RAG：

```
Vector DB
↓
检索文档
↓
Context
```

Memory：

```
Memory Store
↓
检索历史
↓
Context
```

最后：

```
LLM
```

看到的都只是：

```
一段文本 / Structured Data
```

所以在模型看来，它们可能没有那么大的形式差别。

真正的区别存在于：

> **这些数据为什么被存储，以及为什么被检索。**

# 十四、可以这样画

```
                 Current User Question
                          │
                          ▼
                    Agent Runtime
                          │
             ┌────────────┴────────────┐
             │                         │
             ▼                         ▼
            RAG                      Memory
             │                         │
             ▼                         ▼
      Knowledge Base              Memory Store
             │                         │
             ▼                         ▼
      Relevant Knowledge         Relevant History
             │                         │
             └────────────┬────────────┘
                          ▼
                       Context
                          │
                          ▼
                         LLM
```

# 十五、RAG 更像“查书”

想象你正在解决 Java 问题。

你不会记得 Spring 所有 API。

于是：

```
打开官方文档
↓
搜索 @Transactional
↓
找到对应章节
↓
阅读
↓
解决问题
```

这就很像：

```
RAG
```

所以可以记：

> **RAG = 查书。**

# 十六、Memory 更像“翻自己的笔记”

假设你之前写过：

```
这个项目只能用 JDK 8。

用户表不能物理删除。

Campaign 修改预算必须走审批。
```

今天遇到问题：

```
这个项目预算修改怎么处理？
```

你翻自己的项目笔记：

```
之前已经定过：
预算修改必须审批。
```

这就很像：

```
Memory
```

所以可以记：

> **Memory = 翻自己的笔记。**

# 十七、Tool 又是什么？

这里顺便把三者区分。

假设用户问：

```
Campaign 123 现在是什么状态？
```

你不是：

```
查知识库
```

也不是：

```
查过去聊天
```

而是：

```
调用实时业务接口
```

例如：

```
getCampaign(123)
```

得到：

```
ACTIVE
```

这是：

```
Tool
```

所以可以形成非常好记的一组：

```
RAG
=
查书

Memory
=
翻笔记

Tool
=
现场去查 / 去做
```

# 十八、三者一起对比

## RAG

```
Meta 广告审核规则是什么？
```

去：

```
知识库
```

## Memory

```
我之前跟你说过哪些广告创建约束？
```

去：

```
历史记忆
```

## Tool

```
Campaign 123 当前状态是什么？
```

去：

```
真实业务系统
```

# 十九、这三种信息的“新鲜程度”也不同

RAG 中的文档可能：

```
今天更新
上个月更新
去年更新
```

取决于知识库。

Memory：

```
来自过去交互
```

可能是几分钟前，也可能几个月前。

Tool：

通常更适合获取：

```
当前真实状态
```

例如：

```
当前 Campaign Status
当前账户余额
当前库存
当前订单状态
```

所以不同问题应该选择不同信息来源。

# 二十、一个非常重要的原则：Source of Truth

假设 Memory 里记着：

```
Campaign 123 是 ACTIVE。
```

这是昨天保存的。

但今天用户问：

```
Campaign 123 当前什么状态？
```

这个时候：

```
Memory
```

不应该成为真正的：

```
Source of Truth
```

应该调用：

```
getCampaign(123)
```

获得实时数据。

因为 Campaign 状态可能已经变化。

# 二十一、所以 Memory 不是数据库真相

Memory 里的很多信息：

```
可能过时
```

比如：

```
用户以前住北京
```

今天可能已经搬走。

或者：

```
昨天 Campaign ACTIVE
```

今天可能已经 PAUSED。

因此 Memory 应该区分：

```
长期稳定信息
```

和：

```
会变化的信息
```

# 二十二、RAG 也不是绝对 Truth

RAG 查出来：

```
一篇旧文档
```

可能已经失效。

知识库可能：

```
没更新
有错误
有冲突
```

所以：

```
RAG
≠
100% 正确
```

它只是提供：

```
外部 Grounding
```

# 二十三、三者应该怎么选？

可以问自己：

```
这个问题需要的东西是什么？
```

如果是：

```
通用 / 企业知识
```

优先想：

```
RAG
```

如果是：

```
过去这个用户 / 这个任务发生过什么
```

优先想：

```
Memory
```

如果是：

```
真实系统现在是什么状态
或者需要执行动作
```

优先想：

```
Tool
```

# 二十四、技术实现为什么会重叠？

这里是最容易混淆的地方。

比如 RAG：

```
Document
↓
Embedding
↓
Vector DB
↓
Similarity Search
↓
Relevant Documents
```

Memory 也可能：

```
Past Conversation
↓
Embedding
↓
Vector DB
↓
Similarity Search
↓
Relevant Memories
```

看起来一模一样。

所以很多初学者会认为：

```
RAG = Memory
```

但这是错误的。

# 二十五、技术手段相同，不代表概念相同

例如：

```
MySQL
```

既可以保存：

```
User
```

也可以保存：

```
Order
```

不能因为都在：

```
MySQL
```

里，就认为：

```
User = Order
```

同理：

```
Vector Database
```

只是底层技术之一。

你存：

```
知识文档
```

用于知识检索：

```
RAG
```

你存：

```
历史经历
```

用于未来召回：

```
Memory
```

# 二十六、所以判断是不是 RAG，不要看它用了什么数据库

不要说：

```
用了 Milvus
所以就是 RAG。
```

错误。

也不要说：

```
用了 pgvector
所以就是 Memory。
```

也错误。

应该问：

> **存的是什么信息？**

以及：

> **检索这些信息的目的是什么？**

# 二十七、一个判断公式

可以用：

```
Why Retrieve?
```

来判断。

如果：

```
因为当前问题需要外部知识
```

更像：

```
RAG
```

如果：

```
因为需要回忆之前发生过的事情
```

更像：

```
Memory
```

# 二十八、Memory 其实也分很多种

后面 Phase 4 会专门学习：

```
Memory / Context / State
```

现在只需要有一个初步概念。

Memory 可能包括：

```
Short-term Memory
Long-term Memory
Semantic Memory
Episodic Memory
User Memory
Conversation Memory
```

不同框架的命名不完全统一。

所以现在不用急着背。

# 二十九、最简单的 Conversation Memory

最简单的 Memory：

```
把聊天历史保存下来
```

例如：

```
User:
我叫张三。

Assistant:
你好张三。
```

下一轮重新发送：

```
我叫张三
```

模型就表现得像：

```
记住了
```

但其实这只是：

```
Conversation History
↓
重新进入 Context
```

# 三十、更长期的 Memory

如果聊天已经：

```
1000 轮
```

显然不能：

```
每次全部放入 Context
```

系统可能提取：

```
用户姓名：张三
职业：Java 开发
项目使用 JDK 8
偏好简洁回答
```

保存到：

```
Long-term Memory
```

以后按需要加载。

# 三十一、RAG 通常不会“自动学习用户”

假设知识库中有：

```
Spring 官方文档
```

你和 Agent 聊十天：

RAG 知识库本身通常不会因为：

```
你说了十句话
```

自动改变。

而 Memory 通常正好相反。

它可能随着交互：

```
持续产生
```

# 三十二、这也是两者一个很明显的区别

RAG 数据一般是：

```
Knowledge Ingestion
```

也就是：

```
有人把文档放进去
```

例如：

```
上传 PDF
同步 Wiki
同步 Notion
爬网页
导入数据库
```

Memory 数据则常常来自：

```
Interaction
```

也就是：

```
Agent 在运行过程中产生
```

# 三十三、举一个完整 Agent 场景

用户问：

```
根据我们之前讨论过的投放原则，
结合 Meta 最新审核规则，
看看 Campaign 123 为什么不能投放。
```

这里三个数据源全来了。

## 第一步：Memory

查询：

```
我们之前讨论过的投放原则
```

得到：

```
用户要求：
预算不得超过 500 美元。
敏感素材必须人工审批。
```

## 第二步：RAG

查询：

```
Meta 最新审核规则
```

得到：

```
Policy Document
```

## 第三步：Tool

查询：

```
Campaign 123
```

得到：

```
{
  "status": "REJECTED",
  "budget": 800,
  "creativeType": "SENSITIVE"
}
```

## 第四步：Context

最终：

```
Memory:
预算不能超过 500。
敏感素材必须人工审批。

RAG:
Meta 当前审核规则……

Tool Result:
Campaign 123:
budget = 800
creative = SENSITIVE
status = REJECTED

User:
为什么不能投放？
```

然后模型进行分析。

# 三十四、这就是生产 Agent 真正的样子

一个 Agent 并不是：

```
RAG
```

或者：

```
Memory
```

或者：

```
Tool
```

三选一。

而可能是：

```
                 Agent
                   │
         ┌─────────┼─────────┐
         ▼         ▼         ▼
        RAG      Memory      Tools
         │         │          │
         ▼         ▼          ▼
      Knowledge   History   Real World
```

然后共同构建：

```
Context
```

# 三十五、RAG 和 Memory 与 Context 的关系

Node 005 已经学过：

```
Context
=
模型这一轮实际看到的东西
```

所以：

```
RAG
```

和：

```
Memory
```

都不等于 Context 本身。

它们是：

```
Context 来源
```

# 三十六、可以这样画

```
Knowledge Base
     │
     │ RAG
     ▼
Relevant Documents
     │
     ├───────────┐
                 │
Memory Store     │
     │           │
     │ Recall    │
     ▼           │
Relevant Memory  │
     │           │
     └─────┬─────┘
           ▼
        Context
           │
           ▼
          LLM
```

# 三十七、Context Window 为什么同时限制它们？

假设：

```
RAG 找回 100K Token

Memory 找回 50K Token

聊天历史 20K Token
```

总计：

```
170K Token
```

但你的 Context Budget 只有：

```
32K
```

那就不可能：

```
全放进去
```

因此必须做：

```
筛选
排序
压缩
截断
```

# 三十八、RAG 也需要 Context Engineering

例如：

```
Top 100 Documents
```

全部放进去：

很可能不好。

应该考虑：

```
Top K
Re-ranking
Chunk Selection
Filtering
```

找：

```
最有价值的信息
```

# 三十九、Memory 同样需要 Context Engineering

不能：

```
所有 Memory 全部放进去
```

例如用户有：

```
10000 条 Memory
```

当前问 Java：

没有必要把：

```
旅游偏好
游戏偏好
昨天吃什么
```

全部给模型。

只应该召回：

```
当前问题相关 Memory
```

# 四十、所以 RAG 和 Memory 本质上都有 Retrieval 问题

RAG：

```
从知识库中
找和问题最相关的知识
```

Memory：

```
从历史记忆中
找和当前任务最相关的记忆
```

因此：

```
Retrieval
```

并不是 RAG 独占的技术。

# 四十一、RAG 一定使用 Vector DB 吗？

不一定。

RAG 的 Retrieval 可以使用：

```
关键词搜索
SQL
全文检索
Elasticsearch
Vector Search
Hybrid Search
```

甚至：

```
固定规则
```

都可以。

所以：

```
RAG
≠
Vector Database
```

# 四十二、Memory 一定使用 Vector DB 吗？

也不一定。

例如用户信息：

```
{
  "name": "张三",
  "language": "Java",
  "jdk": 8
}
```

完全可以存在：

```
MySQL
Redis
```

甚至：

```
JSON
```

然后直接读取。

所以：

```
Memory
≠
Vector Database
```

# 四十三、什么时候 Vector Search 更有意义？

比如 Memory 中存了：

```
过去 10000 条长对话
```

当前用户问：

```
我们之前是不是聊过 Campaign 修改失败的问题？
```

你很难：

```
SQL WHERE xxx = ?
```

精确匹配。

于是可以：

```
Embedding
↓
Semantic Search
```

找到语义相关历史。

# 四十四、RAG 的典型目标是什么？

例如：

```
回答公司内部知识问题

分析技术文档

根据法律文档回答

根据产品文档解释 API

根据最新业务规则回答
```

本质：

```
External Knowledge Grounding
```

# 四十五、Memory 的典型目标是什么？

例如：

```
记住用户偏好

记住项目约束

记住过去决策

记住任务进展

记住之前发生过什么
```

本质：

```
Continuity
```

也就是：

> **让 Agent 在时间上具有连续性。**

# 四十六、RAG 解决“知识断层”

模型不知道：

```
公司内部资料
最新资料
私有资料
```

RAG：

```
查出来
↓
给模型
```

# 四十七、Memory 解决“时间断层”

模型这一轮结束以后：

```
当前 Context 不会天然永远存在
```

Memory：

```
重要信息保存
↓
未来重新提供
```

所以可以记：

```
RAG
解决知识空间问题

Memory
解决时间连续性问题
```

这个理解非常好。

# 四十八、再用一句更抽象的话

```
RAG
=
我现在还需要知道什么？
```

Memory：

```
我以前已经知道过什么？
```

# 四十九、一个很容易混淆的边界情况

假设公司把：

```
过去 3 年所有客服聊天记录
```

全部放进知识库。

用户现在问：

```
类似问题以前怎么解决的？
```

系统检索：

```
过去客服聊天
```

这是 RAG 还是 Memory？

答案是：

> **两种说法都可能合理，要看系统如何定义这批数据。**

如果它被当成：

```
企业知识库文档
```

更偏：

```
RAG
```

如果它代表：

```
Agent / 用户过去经历
```

更偏：

```
Memory
```

# 五十、所以两者没有绝对物理边界

现实系统不是：

```
这个数据库只能叫 RAG
那个数据库只能叫 Memory
```

真正区分的是：

```
语义
用途
生命周期
```

因此学习 Agent 时不要陷入：

```
这个到底百分之百属于哪一个？
```

很多时候：

```
有重叠
```

很正常。

# 五十一、我们学习时应该怎么判断？

当前阶段使用这个标准就够了：

```
当前问题需要外部知识？
↓
RAG
当前问题需要过去保存的信息？
↓
Memory
当前问题需要实时状态或真实动作？
↓
Tool
```

# 五十二、把三者用 Java 后端类比

## RAG

类似：

```
knowledgeService.search(question);
```

返回：

```
相关知识
```

## Memory

类似：

```
memoryService.findRelevantMemory(userId, question);
```

返回：

```
用户以前的重要信息
```

## Tool

类似：

```
campaignService.getById(campaignId);
```

或者：

```
campaignService.pause(campaignId);
```

获取 / 修改真实业务状态。

# 五十三、未来 ContextBuilder 可能这样工作

以后我们的 Agent 项目中可能出现：

```
Context build(
        UserRequest request,
        AgentState state
) {

    List<Document> docs =
            ragService.retrieve(request);

    List<Memory> memories =
            memoryService.recall(request);

    List<Tool> tools =
            toolRegistry.select(request);

    return contextBuilder.build(
            request,
            docs,
            memories,
            state,
            tools
    );
}
```

当然现在不要写。

这里只是为了理解：

```
RAG
Memory
State
Tools
```

最终都会服务于：

```
Current Context
```

# 五十四、为什么 RAG 不能代替 Memory？

假设你有一个特别强的 RAG 系统。

里面有：

```
几百万篇公司文档
```

但是用户昨天说：

```
以后叫我小明。
```

这条信息根本没有进入知识库。

今天用户回来：

```
你记得我叫什么吗？
```

RAG 搜公司文档：

```
搜不到
```

所以仍然需要：

```
Memory
```

# 五十五、为什么 Memory 不能代替 RAG？

反过来。

Agent 记得：

```
用户喜欢 Java
项目用 JDK 8
```

但用户问：

```
Spring AI 最新 API 怎么使用？
```

Memory 根本没有这方面资料。

此时需要：

```
官方文档 / Knowledge Base
```

也就是：

```
RAG
```

或者其他检索 / Web Tool。

# 五十六、为什么 Tool 也不能代替两者？

Tool 更偏：

```
实时查询和行动
```

例如：

```
查询 Campaign
```

但你不能让：

```
getCampaign
```

回答：

```
Meta 广告审核政策是什么？
```

也不能让：

```
getCampaign
```

告诉你：

```
用户三个月前说过什么
```

所以：

```
RAG
Memory
Tool
```

各自解决不同问题。

# 五十七、最终可以把 Agent 的“信息来源”这样理解

```
模型参数
↓
模型本来学会的通用知识

RAG
↓
外部知识

Memory
↓
过去历史

Tool
↓
当前现实世界

State
↓
当前任务状态
```

然后：

```
这些信息中的必要部分
↓
Context
↓
LLM
```

# 五十八、一张完整脑图

```
                        Agent
                          │
                          ▼
                    Context Builder
                          │
      ┌────────────┬──────┼──────┬────────────┐
      │            │      │      │            │
      ▼            ▼      ▼      ▼            ▼
Model Knowledge   RAG   Memory  State        Tools
                   │      │                  │
                   ▼      ▼                  ▼
               Knowledge History        Real World
                   │      │                  │
                   └──────┴─────────┬────────┘
                                    ▼
                              Current Context
                                    │
                                    ▼
                                   LLM
```

# 五十九、最容易出现的错误认知

## 错误 1

```
RAG = Vector Database
```

错误。

Vector DB 只是可能的实现技术。

## 错误 2

```
Memory = 聊天记录
```

不完整。

聊天历史只是 Memory 的一种形式。

## 错误 3

```
RAG 和 Memory 都使用检索，所以是同一个东西。
```

错误。

技术流程可能相似，但业务目的不同。

## 错误 4

```
用了 Memory 后，模型就真的永久记住了。
```

错误。

仍然通常是：

```
外部保存
↓
重新召回
↓
加入 Context
```

## 错误 5

```
RAG 查出来的资料一定正确。
```

错误。

知识库也可能：

```
错误
过时
冲突
```

## 错误 6

```
Memory 中的信息永远可信。
```

错误。

Memory 也可能：

```
过时
错误
不完整
```

# 六十、这一篇最核心的 10 句话

### 1

> **RAG 主要解决外部知识检索问题。**

### 2

> **Memory 主要解决过去信息保存和召回问题。**

### 3

> **RAG 可以简单理解成“查资料”。**

### 4

> **Memory 可以简单理解成“记经历”。**

### 5

> **RAG 更偏 Knowledge，Memory 更偏 History / Experience。**

### 6

> **RAG 和 Memory 最终都可能把信息放进 Context。**

### 7

> **RAG 和 Memory 都可能使用 Vector Database，但 Vector Database 不等于任何一个概念本身。**

### 8

> **RAG、Memory 和 Tool 是三个不同的信息来源。**

### 9

> **RAG 查书，Memory 翻笔记，Tool 去现场查询或执行。**

### 10

> **最终 Agent 的关键不是拥有多少信息，而是当前任务应该把哪些信息放进 Context。**

# 六十一、最值得记住的三分法

如果以后又混淆：

直接问三个问题。

```
我要查“资料”吗？
↓
RAG
我要回忆“以前发生过什么”吗？
↓
Memory
我要知道“现在真实系统是什么状态”或“真的执行操作”吗？
↓
Tool
```

这个判断在当前学习阶段已经足够用了。

# 六十二、自测

## Q1

RAG 和 Memory 最核心的区别是什么？

## Q2

为什么说：

```
RAG = 查书
Memory = 翻笔记
```

？

## Q3

RAG 找回来的内容最终通常会放到哪里？

## Q4

Memory 召回的信息最终通常会放到哪里？

## Q5

为什么：

```
Memory ≠ 模型永久记住
```

？

## Q6

RAG 和 Memory 能不能都使用 Vector Database？

## Q7

如果都用了 Vector Database，为什么仍然不是同一个概念？

## Q8

下面哪个更适合 RAG？

```
“Spring Boot 官方文档里这个配置是什么意思？”
```

## Q9

下面哪个更适合 Memory？

```
“我们以前给这个项目定过什么规则？”
```

## Q10

下面哪个更适合 Tool？

```
“Campaign 123 现在是什么状态？”
```

# 六十三、进阶判断

用户问：

```
结合我们上个月讨论的 Campaign 规则，
参考最新 Meta 文档，
再查询 Campaign 123 当前状态，
告诉我为什么广告不能投放。
```

这个问题需要：

```
上个月讨论的 Campaign 规则
↓
Memory
最新 Meta 文档
↓
RAG
Campaign 123 当前状态
↓
Tool
```

最终：

```
Memory
+
RAG
+
Tool Result
↓
Context
↓
LLM
↓
Answer
```

如果你能一眼拆成这三个来源，那么：

```
RAG
Memory
Tool
Context
```

这四个概念已经基本分清了。

# 六十四、和 Node 005、006 串起来

Node 005 学过：

```
Context
=
模型这一轮真正看到的信息
```

那么：

```
RAG
Memory
Tool Result
State
```

其实都是：

```
Context 的潜在来源
```

Node 006 学 Agent：

```
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

以后随着 Agent 变复杂：

```
RAG
Memory
```

也会逐渐加入系统。

最终可以理解成：

```
            Agent Runtime
                  │
     ┌────────────┼─────────────┐
     ▼            ▼             ▼
   Tools         RAG          Memory
     │            │             │
     ▼            ▼             ▼
Real World     Knowledge       History
     │            │             │
     └────────────┼─────────────┘
                  ▼
               Context
                  │
                  ▼
                 LLM
```

到这里就可以暂时结束。

RAG 和 Memory 后面还会分别在：

```
Phase 3：RAG
```

和：

```
Phase 4：Memory / Context / State
```

中系统学习。

当前 Node 006 阶段最重要的是：

> **先知道它们不是一个东西，同时知道它们最终都在帮助 Agent 构建更好的 Context。**



# 还有一个非常重要的细节

**Tool 和 RAG 也可能同时出现。**

比如：

```
User
↓
LLM
↓
决定调用 webSearch Tool
↓
Web Search
↓
搜索结果
↓
加入 Context
↓
LLM
↓
Answer
```

这里：

```
webSearch
```

是 **Tool**。

而：

```
搜索外部资料 → 拿结果增强回答
```

这个过程又具有 **RAG / Retrieval-Augmented Generation** 的性质。

所以不要把这些概念理解成互斥分类。

可以这么看：

```
Tool
=
“怎么去拿信息”

RAG
=
“拿外部知识来增强生成”这个模式

Memory
=
“怎么保存并召回过去的信息”
```

这一层理解就非常准确了。

> 这个是个点睛之笔：
>
> 我原本还在想，这种涉及实时搜索的场景使用了 Tool、没有预先存储资料，所以既不是 RAG，也不是 Memory，最多只和 Context 有关。后来才意识到：是否使用 Tool，与是否属于广义 RAG，是两个不同维度，它们并不互斥。
>
> 它们不是互斥的。
>
> RAG ≠ Vector DB
> RAG ≠ 必须提前存文档

### 不过这里再补一个“严谨版”的细节

现实工程里，“RAG”这个词有**广义和狭义用法**。

狭义、经典意义上的 RAG，一般大家想到的是：

```
自己的知识库
↓
提前建立索引
↓
检索相关 Chunk
↓
给 LLM
```

例如：

```
Spring Boot 官方文档
↓
抓取并存入公司知识库
↓
Embedding
↓
Vector DB
↓
检索
```

这个所有人基本都会直接叫：

> RAG。

而：

```
LLM
↓
实时调用 Google / Bing / Web Search
↓
拿网页
↓
回答
```

有些工程师会叫：

```
Search-Augmented Generation
Web-augmented generation
Search grounding
```

而不严格称为“经典 RAG”。

但是从**广义架构思想**上：

```
Retrieval
+
Augmented Generation
```

它当然是同一类思想。

所以以后你看到不同资料有人说：

> “Web Search 不是 RAG。”

也不要觉得前面学错了。

他大概率是在用：

```
狭义 RAG
=
基于自己的 indexed knowledge base 的检索增强
```

这个定义。
