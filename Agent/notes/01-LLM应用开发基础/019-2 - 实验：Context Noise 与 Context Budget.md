# 019-2 - 实验：Context Noise 与 Context Budget

> Node 019：Context Engineering\
> Phase 1：LLM 应用开发基础\
> 实验项目：`projects/agent-learning-lab`

---

# 一、实验目标

019-1 已经理解：

```text
Context Engineering
=
决定这一轮模型到底应该看到什么
```

这次只验证三件事：

```text
A：只给必要 Context

B：保留相同关键事实，
   但加入大量无关 Noise

C：保持大致相同的信息量，
   改变关键事实所在位置
```

观察：

```text
回答是否正确
prompt_tokens
completion_tokens
total_tokens
总耗时
关键事实是否被正确使用
```

---

# 二、为什么这个实验值得做？

017「模型参数」和 018「模型选择」可以只靠理论理解。

但 Context Engineering 有一件事非常值得亲眼看到：

> **More Context ≠ Better Context**

真实 Agent 中最常见的错误之一就是：

```text
反正 Context Window 很大
↓
能拿到的信息全部塞进去
```

真正跑一次以后，你会更容易形成直觉：

```text
更多信息
→ 更多 Input Token

更多信息
→ 可能更慢

更多信息
→ 不一定提高回答质量

无关信息
→ 会降低信息密度
```

所以本实验不是为了“把模型搞错”。

而是为了理解：

> **Context 本身就是一种有限资源。**

---

# 三、这次不要做什么？

不要引入：

```text
Spring Boot
RAG
Embedding
向量数据库
Memory 系统
Agent Loop
Tool Calling
Context Manager
自动摘要
复杂 Tokenizer
大规模 Benchmark
```

继续使用：

```text
Java 21
HttpClient
FastJSON2
阿里云百炼
qwen3.8-max
OpenAI-compatible Chat Completions
```

---

# 四、项目中新增什么？

继续使用：

```text
Agent/projects/agent-learning-lab
```

新增：

```text
src/main/java/com/avexvertex/agent/ContextEngineeringDemo.java
```

本次不新增 DTO。

---

# 五、实验核心问题

我们不是在问：

```text
模型有没有能力回答？
```

而是在问：

```text
同一个模型
同一个任务

只改变 Context
以后会发生什么？
```

因此尽量固定：

```text
Model
Prompt 目标
reasoning_effort
其他参数
```

主要变量只有：

```text
Context
```

---

# 六、实验任务

为了判断模型到底有没有使用 Context，

任务必须：

```text
答案明确
```

而且最好包含：

```text
模型训练数据里不可能知道的虚构事实
```

例如：

```text
当前目标项目：
项目代号：ORANGE-47
数据库：PostgreSQL
负责人：Lin
上线窗口：周四 21:30
```

问题：

```text
1. 项目代号是什么？
2. 使用什么数据库？
```

正确答案：

```text
ORANGE-47
PostgreSQL
```

---

# 七、为什么不用普通知识题？

如果问：

```text
Java HashMap 是什么？
```

模型完全可以：

```text
不看 Context
也靠训练知识回答
```

这样无法验证：

```text
它有没有真正使用我们提供的信息
```

所以使用：

```text
虚构项目事实
```

更合适。

---

# 八、A：Minimal Relevant Context

A 只提供：

```text
完成任务需要的必要信息
```

例如：

```text
项目资料：
- 项目代号：ORANGE-47
- 数据库：PostgreSQL

问题：
1. 项目代号是什么？
2. 使用什么数据库？
```

特点：

```text
Context 短
信息密度高
几乎没有 Noise
```

A 是：

```text
Baseline
基线
```

---

# 九、B：Context Noise

B 保留相同关键事实：

```text
ORANGE-47
PostgreSQL
```

但加入大量：

```text
与问题无关的信息
```

例如：

```text
其他项目说明
测试环境
办公室规则
无关技术栈
旧会议记录
其他服务配置
无关日期
历史版本说明
```

---

# 十、Noise 不要放错误答案

这一点很重要。

不要在 Noise 里故意写：

```text
ORANGE-47 使用 MySQL
```

否则测试的就不再只是：

```text
Context Noise
```

而变成：

```text
Conflicting Context
事实冲突
```

这是另一类问题。

本实验只测试：

```text
无关信息
```

---

# 十一、B 组示意

```text
背景资料：

项目 Atlas：
- Redis 7
- Java 21
- 负责人 Chen

测试环境：
- 日志保留 14 天
- CI 每晚 02:00 执行

项目 Nimbus：
- MySQL 8
- Node.js 22
- 发布窗口周二

内部会议：
- 下季度考虑升级监控系统
- 暂不迁移对象存储

......

[CURRENT TARGET PROJECT]
- 项目代号：ORANGE-47
- 数据库：PostgreSQL

......

更多无关资料：
- 办公网络维护时间
- 其他服务端口
- 旧项目命名规则
- 无关业务说明

问题：
1. 项目代号是什么？
2. 使用什么数据库？
```

---

# 十二、B 组需要多长？

不用：

```text
10 万 Token
```

也不要故意把模型拖到极限。

只要：

```text
明显比 A 长很多
```

即可。

例如：

```text
A：
几十到几百 Token

B：
几千 Token
```

已经足够观察：

```text
prompt_tokens
```

明显变化。

---

# 十三、C：改变关键信息位置

C 的目标不是证明：

```text
Lost in the Middle 一定发生
```

而是建立：

```text
长 Context 中
信息位置也是一个变量
```

准备：

```text
C1：关键事实放开头
C2：关键事实放中间
C3：关键事实放结尾
```

其他 Noise 尽量一致。

---

# 十四、为什么 C 要控制变量？

如果：

```text
C1、C2、C3
内容完全不同
```

结果有差异时，

你无法判断：

```text
来自位置
还是来自内容
```

所以继续遵循：

> **一次只改变一个主要变量。**

---

# 十五、位置实验怎么构造？

准备固定：

```text
Noise A
Noise B
Noise C
```

目标块：

```text
[CURRENT TARGET PROJECT]
项目代号：ORANGE-47
数据库：PostgreSQL
```

然后：

## C1 - Start

```text
TARGET
+
Noise A
+
Noise B
+
Noise C
```

## C2 - Middle

```text
Noise A
+
TARGET
+
Noise B
+
Noise C
```

## C3 - End

```text
Noise A
+
Noise B
+
Noise C
+
TARGET
```

---

# 十六、C 组不是为了抓模型出错

即使：

```text
C1
C2
C3
```

全部答对，

实验依然有效。

因为你仍然可以观察：

```text
prompt_tokens
耗时
答案稳定性
```

并且理解：

```text
Lost in the Middle
不是“放中间就一定失败”
```

---

# 十七、请求参数保持固定

建议：

```json
{
  "model": "qwen3.8-max",
  "reasoning_effort": "none"
}
```

因为任务很简单。

不要让：

```text
reasoning_effort
```

成为新的变量。

---

# 十八、temperature 怎么处理？

最简单：

```text
不主动设置
```

使用 Provider 默认值。

如果项目后面决定固定一个值也可以。

关键是：

```text
A / B / C
保持一致
```

---

# 十九、为什么不用 Structured Output？

这次研究的是：

```text
Context
```

不是：

```text
Schema
```

所以让模型返回简单文本即可。

例如：

```text
ProjectCode: ORANGE-47
Database: PostgreSQL
```

这样最容易比较。

---

# 二十、推荐 Prompt

```text
请严格根据下面提供的资料回答。

不要使用自己的猜测。
如果资料中没有答案，请回答 UNKNOWN。

请只输出两行：

ProjectCode: <项目代号>
Database: <数据库>

资料：
{{CONTEXT}}

问题：
1. 当前目标项目的项目代号是什么？
2. 当前目标项目使用什么数据库？
```

---

# 二十一、为什么要求 UNKNOWN？

这强化一个很重要的 Context Engineering 思想：

```text
没有事实
→ 不要猜
```

以后：

```text
RAG
Tool Calling
Agent
```

里会经常遇到这种要求。

---

# 二十二、Java 类建议

新增：

```text
ContextEngineeringDemo.java
```

结构保持简单：

```java
public class ContextEngineeringDemo {

    public static void main(String[] args) {
        // 解析模式

        // 构造 Context

        // 构造 Request

        // 调模型

        // 解析回答

        // 打印 Token / Time / Correct
    }
}
```

---

# 二十三、建议支持五种模式

```text
--minimal
--noise
--position-start
--position-middle
--position-end
```

分别对应：

```text
A
B
C1
C2
C3
```

无参数可以默认：

```text
--minimal
```

---

# 二十四、Context 构造建议拆方法

例如：

```java
private static String buildMinimalContext()

private static String buildNoiseContext()

private static String buildPositionContext(Position position)
```

这样你能更清楚地看到：

```text
Context 是程序动态构造的
```

而不只是：

```text
Prompt 里的一大段字符串
```

---

# 二十五、这就是 Context Engineering 的代码形态

以后真实系统可能类似：

```java
context.add(systemPrompt);
context.add(relevantMemory);
context.add(history);
context.add(ragDocuments);
context.add(toolResults);
context.add(currentUserMessage);
```

现在虽然只是小实验，

但已经开始建立：

```text
Context 是由应用组织出来的
```

这个意识。

---

# 二十六、Noise 怎么准备？

不需要写乱码。

准备一组：

```text
自然但无关的业务信息
```

例如：

```text
其他项目技术栈
其他环境说明
旧会议记录
其他发布窗口
日志保留策略
无关服务端口
```

可以使用：

```java
List<String> noiseBlocks
```

再组合成较长 Context。

---

# 二十七、为什么 Noise 应该“像真的”？

真实项目里的 Noise 通常不是：

```text
AAAAAAAAAAA
```

而是：

```text
看起来似乎有点相关
但实际上不帮助当前任务
```

所以自然业务文本：

```text
更接近真实 Agent Context
```

---

# 二十八、建议记录哪些数据？

每组至少打印：

```text
Mode
HTTP Status
Model
Answer
Correct
Prompt Tokens
Completion Tokens
Total Tokens
Total Time
```

---

# 二十九、Correct 怎么判断？

因为正确答案固定：

```text
ORANGE-47
PostgreSQL
```

可以简单：

```java
boolean correct =
        content.contains("ORANGE-47")
        && content.contains("PostgreSQL");
```

---

# 三十、为什么简单 contains 就够？

因为：

```text
这不是生产级 Evaluation
```

只是学习实验。

当前没必要做：

```text
复杂评分器
LLM-as-a-Judge
```

只需要知道：

```text
明显答对没有
```

---

# 三十一、可以打印字符数，但不要冒充 Token

可以额外打印：

```text
Context Characters
```

但一定要区分：

```text
字符数
≠
Token 数
```

真正 Token 数据使用：

```text
usage.prompt_tokens
```

---

# 三十二、为什么不自己接 Tokenizer？

因为 Provider 已经返回：

```text
prompt_tokens
```

当前没有必要：

```text
额外引入 Tokenizer
```

否则会偏离本实验目标。

---

# 三十三、耗时怎么记录？

使用：

```java
long startNanos = System.nanoTime();
```

请求结束：

```java
long endNanos = System.nanoTime();
```

输出：

```text
Total Time(ms)
```

即可。

---

# 三十四、为什么不用 Streaming？

因为：

```text
Streaming 会增加变量
```

这次只研究：

```text
Context
```

所以使用：

```text
普通非流式请求
```

最干净。

---

# 三十五、Response 继续读取什么？

复用前面知识：

```text
choices[0].message.content
usage.prompt_tokens
usage.completion_tokens
usage.total_tokens
model
finish_reason
```

这次不需要新 API。

---

# 三十六、第一步：跑 A

先运行：

```text
--minimal
```

确认：

```text
HTTP 200
答案正确
Token Usage 正常
```

把结果记录下来。

---

# 三十七、A 的意义

A 建立：

```text
Baseline
```

也就是：

```text
完成这个任务
最精简 Context 大概是什么样
```

---

# 三十八、第二步：跑 B

运行：

```text
--noise
```

这次最值得看：

```text
prompt_tokens
```

它应该：

```text
明显高于 A
```

这是最稳定的观察结果。

---

# 三十九、B 是否一定答错？

不一定。

现代模型完全可能：

```text
加入大量 Noise 后
仍然正确回答
```

这不是实验失败。

---

# 四十、如果 A 和 B 都答对了呢？

例如：

```text
A：
Prompt Tokens = 100
Correct = true

B：
Prompt Tokens = 3000
Correct = true
```

那你已经观察到：

```text
B 花了更多 Context Budget
```

但：

```text
没有带来任务收益
```

这本身就是很重要的结果。

---

# 四十一、不要为了让 B 出错疯狂加 Noise

实验不是：

```text
把模型整崩
```

如果 B 仍然答对：

```text
就接受这个结果
```

不要继续：

```text
加到 100K
加到 500K
```

直到它失败。

---

# 四十二、第三步：跑位置实验

依次运行：

```text
--position-start
--position-middle
--position-end
```

记录：

```text
Answer
Correct
Prompt Tokens
Total Time
```

---

# 四十三、每组跑几次？

当前：

```text
每组 1 次
```

就够。

如果出现特别有意思的结果，例如：

```text
Middle 答错
```

可以：

```text
再重复 1～2 次
```

确认是不是偶然。

不要做：

```text
几十次统计 Benchmark
```

---

# 四十四、为什么一次不能证明规律？

因为模型结果还会受：

```text
采样
Provider 状态
模型版本
请求时机
```

等因素影响。

所以只能说：

```text
本次实验观察到……
```

不能说：

```text
证明这个模型一定……
```

---

# 四十五、如果 C1/C2/C3 全答对呢？

结论应该是：

```text
在当前长度和任务下，
模型能够正确利用三个位置的关键信息
```

而不是：

```text
Lost in the Middle 不存在
```

---

# 四十六、如果 Middle 真答错呢？

可以记录：

```text
本次样本中，
中间位置的关键事实未被正确使用
```

然后少量复测。

仍然不要扩展成：

```text
模型长上下文性能报告
```

---

# 四十七、这次不要造 100K Context

为什么？

因为会带来：

```text
高 Token 成本
长等待
更复杂的实验设计
```

而当前 Phase 1 的学习目标只需要：

```text
建立 Context Budget 和 Noise 直觉
```

不需要做：

```text
长上下文压力测试
```

---

# 四十八、推荐输出格式

例如：

```text
Mode: NOISE
HTTP Status: 200
Model: qwen3.8-max

Answer:
ProjectCode: ORANGE-47
Database: PostgreSQL

Correct: true
Prompt Tokens: 3248
Completion Tokens: 18
Total Tokens: 3266
Total Time(ms): 1320.41
```

---

# 四十九、代码里可以用一个简单 enum

例如：

```java
enum Mode {
    MINIMAL,
    NOISE,
    POSITION_START,
    POSITION_MIDDLE,
    POSITION_END
}
```

只是为了：

```text
代码清楚
```

不要继续扩展成：

```text
策略框架
```

---

# 五十、不要创建 Context 抽象框架

当前不要创建：

```text
ContextManager
ContextBuilder
ContextStrategy
ContextPipeline
ContextSource
```

因为现在只有一个学习 Demo。

这些属于：

```text
过早抽象
```

---

# 五十一、不要顺手重构旧 Demo

不要因为看到重复代码，就开始：

```text
抽 HttpClient 工具类
抽 Provider SDK
抽 Request Builder
抽 Usage DTO
```

Node 019 的目标只有：

```text
Context Engineering
```

保持边界。

---

# 五十二、错误处理做到基础即可

沿用：

```text
环境变量缺失
Base URL 非法
HTTP 非 2xx
Response 结构错误
IOException
InterruptedException
```

就够。

不要提前加入：

```text
Retry
Rate Limit Handling
复杂 Timeout Policy
Fallback
```

这些属于：

```text
Node 020
```

---

# 五十三、不要偷学 Node 020

尤其不要在本 Demo 顺手加：

```text
指数退避
自动重试
Circuit Breaker
Provider Error Mapping
```

否则学习边界会变乱。

---

# 五十四、中文注释写什么？

继续保留中文注释。

但重点解释：

```text
为什么 Context 这样构造
```

例如：

```java
// B 组保留和 A 完全相同的目标事实，只增加无关资料。
// 这样主要变量仍然是 Context Noise，而不是答案发生变化。
```

这种注释比：

```java
// 创建一个 String
```

更有学习价值。

---

# 五十五、Codex 实现边界

如果交给 Codex，可以要求：

```text
1. 先阅读当前 agent-learning-lab 的代码风格
2. 新增 ContextEngineeringDemo.java
3. 使用 Java 21 + HttpClient + FastJSON2
4. 使用 qwen3.8-max
5. reasoning_effort=none
6. 支持五种模式
7. 构造 Minimal / Noise / Position Context
8. 使用非流式请求
9. 打印 Answer / Usage / Total Time / Correct
10. 保持中文注释
11. Maven compile
```

---

# 五十六、明确禁止 Codex 做什么

```text
不要加 Spring Boot
不要加 RAG
不要加向量数据库
不要加 Memory
不要加 Tool Calling
不要重构旧 Demo
不要创建 Provider 抽象
不要实现 Retry
不要伪造真实 API 运行结果
不要提前把实验标记为完成
```

---

# 五十七、为什么 Codex 编译通过还不算实验完成？

因为：

```text
代码能编译
```

只能说明：

```text
Java 实现没有明显编译错误
```

不能说明：

```text
真实模型表现
```

真正的实验结果必须来自：

```text
你在 IDEA 中真实运行
```

---

# 五十八、推荐真实执行顺序

```text
1. --minimal
2. --noise
3. --position-start
4. --position-middle
5. --position-end
```

一组一组跑。

不要做：

```text
一键连续调用五次
```

因为学习目标是：

```text
每次都观察 Context 和结果
```

---

# 五十九、每组跑完先看什么？

顺序建议：

```text
1. Answer
2. Correct
3. Prompt Tokens
4. Total Time
```

因为最重要的始终是：

```text
任务有没有完成
```

然后才讨论：

```text
效率和成本
```

---

# 六十、真实结果记录

### A - Minimal

- 运行日期：2026-11-05
- HTTP Status：200
- Model：qwen3.8-max
- Prompt Tokens：102
- Completion Tokens：12
- Total Tokens：114
- Total Time：940.241459 ms
- Correct：true（根据原始响应人工确认两个预期值均出现）

Answer：

```text
ProjectCode: ORANGE-47
Database: PostgreSQL
```

这是 A 组首次真实调用的基线，当时程序打印原始响应，以上 Token 来自响应的 usage。

### B - Noise

- 运行日期：2026-11-05
- HTTP Status：200
- Model：qwen3.8-max
- Prompt Tokens：331
- Completion Tokens：12
- Total Tokens：343
- Total Time：883.478292 ms
- Correct：true（程序检查两个预期值均出现）

```text
ProjectCode: ORANGE-47
Database: PostgreSQL
```

与 A 组相比，输入从 102 增至 331 Token，多了 229 Token，答案与输出 Token 数相同。B 这次耗时更短，但每组只有一次实测，无法据此判断加噪声对延迟的影响。

### C - 目标资料位置对照

2026-11-05，三组均使用 qwen3.8-max，HTTP 200，程序均输出 Correct: true。

| 组别 | 目标资料位置 | 输入 Token | 输出 Token | 总 Token | 总耗时（ms） |
| --- | --- | ---: | ---: | ---: | ---: |
| C1 | 开头 | 331 | 12 | 343 | 901.038416 |
| C2 | 第一段无关资料之后 | 331 | 12 | 343 | 872.332334 |
| C3 | 结尾 | 331 | 12 | 343 | 1041.91075 |

三组实际答案相同：

```text
ProjectCode: ORANGE-47
Database: PostgreSQL
```

C1 和 B 请求体相同，分别记录两次调用。C1/C2/C3 使用同一批资料，只调整目标资料的位置。本题三组都找到答案，Token 用量也一致。每组只运行一次，耗时差异不足以判断哪个位置更快，也无法把结果推广到更长或更复杂的上下文。

---

# 六十一、实验后真正要比较什么？

不要只比较：

```text
有没有答错
```

还要问：

```text
A 和 B：
Prompt Tokens 差多少？

B 多出的 Context：
有没有带来任何业务收益？

C1 / C2 / C3：
位置变化后是否仍然稳定？

更长 Context：
耗时是否发生变化？
```

---

# 六十二、最重要的可能结果

例如：

```text
A：
100 Prompt Tokens
Correct = true

B：
3100 Prompt Tokens
Correct = true
```

最重要的结论不是：

```text
模型真厉害，都答对了
```

而是：

> **B 多花的 3000 Token 对这个任务没有产生额外价值。**

这就是：

```text
Context Efficiency
```

---

# 六十三、如果 B 答错呢？

可以记录：

```text
本次样本中，
大量无关 Context 可能干扰了模型使用关键事实
```

注意措辞：

```text
本次样本
可能
```

不要写成绝对规律。

---

# 六十四、实验结果可能很平淡

因为：

```text
qwen3.8-max 能力较强
```

而且本实验：

```text
没有故意把 Context 推到极限
```

所以可能：

```text
五组全部答对
```

完全正常。

本实验仍然验证了：

```text
Context 构造
Token 变化
预算意识
控制变量
```

---

# 六十五、不要追求“模型失败”

实验目标是：

```text
建立工程直觉
```

不是：

```text
找模型弱点
```

所以：

```text
全部答对
≠
实验失败
```

---

# 六十六、实验后建议回答四个问题

```text
1. A 和 B 的 prompt_tokens 差了多少？

2. B 的额外 Context 有没有带来明显收益？

3. C1/C2/C3 是否出现位置差异？

4. 这次实验让我对
   More Context ≠ Better Context
   有什么更具体的理解？
```

---

# 六十七、Node 019 最终应该形成的意识

不是：

```text
Context 越短越好
```

而是：

> **Context 应该足够，但不应该无意义膨胀。**

可以记：

```text
Enough Context
Not Maximum Context
```

---

# 六十八、Context Budget 的真正思想

不是问：

```text
还能塞多少 Token？
```

而是问：

```text
这个信息值得占 Context 吗？
```

真实工程当然不会：

```text
人工逐 Token 判断
```

但应该有：

```text
信息价值 / Token 成本
```

意识。

---

# 六十九、实验完成后 Node 019 怎么处理？

如果：

```text
019-1 理论已读完
+
019-2 五组真实调用完成
+
结果已经理解
```

Node 019 就可以：

```text
正式收尾
```

不要继续扩展：

```text
RAG
Memory
超长上下文 Benchmark
```

---

# 七十、接下来是什么？

Node 019 后面是：

```text
Node 020：LLM API 异常
```

重点：

```text
HTTP Error
Rate Limit
Timeout
Invalid Request
Context Too Long
Provider Error
Retry
```

这是从：

```text
“能调通模型”
```

进入：

```text
“工程上可靠地调用模型”
```

的重要一步。

---

# 七十一、Node 020 再后面

就是：

```text
Node 021：
Phase 1 第一阶段项目
Smart Text Analyzer
```

届时会把：

```text
Spring Boot
LLM
Structured Output
DTO
Exception Handling
Context Engineering
基础 Evaluation
```

组合起来。

---

# 七十二、本实验完成标准

- [x] 新增 `ContextEngineeringDemo.java`
- [x] Java 21 编译通过
- [x] 使用当前 HttpClient / FastJSON2 风格
- [x] 固定 Model 与主要参数
- [x] 完成 Minimal Context 真实调用
- [x] 完成 Noise Context 真实调用
- [x] 完成 Position Start 真实调用
- [x] 完成 Position Middle 真实调用
- [x] 完成 Position End 真实调用
- [x] 每组记录 Answer
- [x] 每组记录 `prompt_tokens`
- [x] 每组记录 `completion_tokens`
- [x] 每组记录 `total_tokens`
- [x] 每组记录总耗时
- [x] 每组判断 Correct
- [ ] 能解释 B 为什么更消耗 Context Budget
- [ ] 能解释 More Context 为什么不一定更有价值
- [x] 不把一次运行结果写成绝对规律
- [x] 不扩展 RAG / Memory / Agent 框架

---

# 七十三、本实验停止点

完成：

```text
同一个任务
↓
不同 Context
↓
真实调用模型
↓
比较 Answer / Token / Time
↓
理解 Context Noise 与 Context Budget
```

就停止。

不要继续：

```text
100K 长上下文
1M Context 压测
RAG Top-K
Embedding
Memory Retrieval
自动摘要
Context Router
Prompt Injection 攻防
```

这些以后有更合适的位置。

---

# 本篇一句话总结

> **019-2 的目的不是证明“长 Context 一定更差”，而是亲眼看到：Context 是有限资源，更多信息会消耗更多 Token，而只有真正相关的信息才值得占据这笔预算。**

最重要的实验结构：

```text
A：必要 Context
↓
建立基线

B：加入 Noise
↓
观察额外 Context 的成本与收益

C：改变关键信息位置
↓
建立“信息位置也是变量”的意识
```

最终应该形成：

> **Context Engineering 不是尽量把模型喂饱，而是让模型看到完成当前任务最需要的信息。**

**Node 019-2：用最小实验建立真正的 Context Budget 直觉。**
