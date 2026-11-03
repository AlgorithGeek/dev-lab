# 016-1 - Streaming 到底是什么？

> Node 016：Streaming\
> Phase 1：LLM 应用开发基础

> **阅读记录（2026-11-03）：**用户确认本篇已读完，随后在 016-2 完成默认/Debug 模式真实运行与分步讲解，实际结果保存在实验篇。Node 016 当前学习范围收尾，下一步 Node 017「模型参数（准备学习）」；未另行逐题口述，新增本地模拟仍未执行。

---

# 一、先说结论

我们前面调用模型时，Java 一直是这种模式：

```text
发送请求
↓
等待模型完整生成
↓
一次性拿到完整 Response
↓
程序开始处理
```

这叫：

```text
非流式响应
Non-streaming
```

Streaming 则是：

```text
发送请求
↓
模型生成一部分
↓
服务端立刻发送一部分
↓
Java 收到一部分
↓
模型继续生成
↓
Java 继续接收
↓
直到结束
```

也就是说：

> **Streaming 的核心不是“模型生成得更快”，而是“结果还没生成完，程序就已经可以开始接收”。**

---

# 二、为什么需要 Streaming？

假设模型最终要输出：

```text
1000 个 Token
```

非流式模式：

```text
模型生成 1
模型生成 2
模型生成 3
...
模型生成 1000
↓
全部结束
↓
客户端一次性拿到完整结果
```

用户在前面这段时间看到的是：

```text
什么都没有
```

然后突然：

```text
整段答案出现
```

Streaming：

```text
模型生成 1
↓
客户端收到

模型生成 2
↓
客户端收到

模型生成 3
↓
客户端收到

...
```

所以用户会看到：

```text
答案一点一点出现
```

这就是聊天产品里常见的实时输出体验。

---

# 三、Streaming 改变的是“传输方式”

Streaming 主要改变：

```text
服务端如何把结果传给客户端
```

而不是改变：

```text
模型是否更聪明
模型是否更准确
```

普通模式：

```text
模型生成答案
↓
服务端等待全部完成
↓
一次性返回
```

Streaming：

```text
模型生成答案
↓
生成一点就传一点
```

所以：

> **Streaming 首先是响应传输方式问题，不是新的模型能力。**

---

# 四、普通 HTTP 请求是什么样？

我们之前的 Java：

```java
HttpResponse<String> response = client.send(
        request,
        HttpResponse.BodyHandlers.ofString()
);
```

这种写法的思路是：

```text
等待整个 Response Body
↓
全部接收完成
↓
得到 String
```

所以：

```text
Request
↓
Server
↓
完整 Response
↓
Java
```

---

# 五、Streaming 最大的区别

Streaming 不再要求：

```text
整个 body 全部结束以后再处理
```

而是：

```text
响应体还在持续到达
↓
客户端边收边处理
```

可以类比：

```text
普通响应
=
文件全部下载完再打开

Streaming
=
文件还在下载，
程序就先处理已经收到的部分
```

---

# 六、Streaming 不一定让总时间更短

假设完整回答需要：

```text
8 秒
```

非流式：

```text
0 ~ 8 秒
什么都看不到

第 8 秒
完整答案出现
```

Streaming：

```text
第 1 秒
开始看到文字

第 2 秒
看到更多

...

第 8 秒
全部结束
```

总时间可能仍然：

```text
8 秒
```

但是用户体验明显不同。

所以：

> **Streaming 主要优化的是首个结果出现时间和交互体验，不保证整个任务更快结束。**

---

# 七、什么是 TTFT？

Streaming 场景里经常会看到：

```text
TTFT
```

全称：

```text
Time To First Token
```

中文可以理解：

```text
首 Token 延迟
```

意思是：

```text
用户发出请求
↓
到收到第一个模型 Token
```

花了多久。

例如：

```text
Request：12:00:00.000
First Token：12:00:00.850
```

那么：

```text
TTFT ≈ 850 ms
```

---

# 八、为什么 TTFT 很重要？

用户最敏感的往往不是：

```text
答案什么时候彻底结束
```

而是：

```text
“它有没有开始回应我？”
```

例如：

```text
TTFT = 0.8 秒
总生成 = 10 秒
```

用户通常感觉：

```text
反应挺快
```

而：

```text
TTFT = 6 秒
总生成 = 8 秒
```

虽然总时间更短，但前 6 秒会感觉：

```text
卡住了
```

---

# 九、Streaming 的三个重要时间点

```text
Request Start
↓
First Token
↓
Last Token
```

所以：

```text
TTFT
=
First Token - Request Start
```

而：

```text
Total Latency
=
Last Token - Request Start
```

两个指标不要混。

---

# 十、Token Streaming 是什么？

模型通常会：

```text
根据前面内容
不断预测下一个 Token
```

大致：

```text
Token 1
↓
Token 2
↓
Token 3
↓
Token 4
...
```

流式 API 可以把生成中的结果不断传回来。

这就是：

```text
Token Streaming
```

---

# 十一、客户端每次收到的一定是一个 Token 吗？

不一定。

服务端可能一次发送：

```text
一个 Token
```

也可能：

```text
几个 Token
```

或者：

```text
一小段文本
```

所以客户端实际收到的东西，更适合叫：

```text
chunk
```

---

# 十二、什么是 chunk？

chunk 可以理解为：

```text
一次收到的一小块数据
```

例如最终答案：

```text
Java 中的 HashMap 不是线程安全的。
```

客户端可能分几次收到：

```text
"Java 中"
```

然后：

```text
"的 HashMap"
```

然后：

```text
" 不是线程安全的"
```

然后：

```text
"。"
```

每次收到的一小块，就是：

```text
chunk
```

---

# 十三、chunk 和完整回答的关系

```text
chunk1
+
chunk2
+
chunk3
+
...
=
最终完整回答
```

所以客户端经常需要：

```java
StringBuilder
```

不断：

```java
append(...)
```

最后得到完整文本。

---

# 十四、什么是 SSE？

LLM Streaming 经常使用：

```text
SSE
```

全称：

```text
Server-Sent Events
```

中文：

```text
服务器发送事件
```

它是一种：

```text
服务器持续向客户端发送事件
```

的 Web 技术。

---

# 十五、SSE 的方向

SSE 主要是：

```text
Server
↓
Client
```

也就是：

```text
客户端先发一个请求
↓
连接保持打开
↓
服务器不断往这个连接发送数据
```

非常适合：

```text
模型持续生成文本
```

这种场景。

---

# 十六、SSE 和 WebSocket 一样吗？

不一样。

WebSocket：

```text
Client ↔ Server
```

是：

```text
双向通信
```

SSE：

```text
Server → Client
```

主要偏：

```text
服务器持续向客户端推数据
```

LLM 文本生成场景里：

```text
客户端发一个请求
然后服务端不断返回
```

和 SSE 很匹配。

---

# 十七、SSE 大概长什么样？

非常简化地看：

```text
data: {...}

data: {...}

data: {...}

data: [DONE]
```

每个：

```text
data:
```

后面是一条事件的数据。

---

# 十八、Streaming 返回的不是一个完整 JSON

普通 Chat Completions：

```text
HTTP Response
↓
一个完整 JSON
```

Streaming 则通常是：

```text
一条事件
一条事件
一条事件
...
```

每条里面只有：

```text
部分增量数据
```

因此不能把它当成：

```text
一个完整 response.body()
```

直接一次解析。

---

# 十九、什么是 delta？

Streaming API 经常会出现：

```text
delta
```

可以理解：

```text
增量
```

也就是：

```text
“这次新增了什么？”
```

例如：

```json
{
  "delta": {
    "content": "Java"
  }
}
```

表示：

```text
这次新生成：
Java
```

下一条：

```json
{
  "delta": {
    "content": " 是"
  }
}
```

最终拼起来：

```text
Java 是
```

---

# 二十、message 和 delta 的区别

普通模式已经结束。

所以服务端可以返回：

```text
完整 message
```

Streaming 过程中答案还没结束，只能持续返回：

```text
delta
```

因此：

```text
message
=
完整消息

delta
=
本次增量
```

---

# 二十一、Streaming 流程图

普通：

```text
Java
↓
HTTP Request
↓
Model
↓
完整生成
↓
完整 Response
↓
Java parse
```

Streaming：

```text
Java
↓
HTTP Request
↓
Model
↓
生成一点
↓
chunk 1
↓
Java process

模型继续生成
↓
chunk 2
↓
Java process

...
↓
DONE
```

---

# 二十二、为什么连接要保持打开？

Streaming 时：

```text
模型还在生成
```

所以连接需要保持：

```text
open
```

直到：

```text
流结束
```

如果客户端中途关闭：

```text
后面的数据就收不到
```

---

# 二十三、Streaming 是永久长连接吗？

通常不是。

更准确：

```text
一次请求
对应一次持续一段时间的流式响应
```

模型生成结束：

```text
这次流结束
```

下一轮对话：

```text
再发新的 HTTP 请求
```

所以不要简单理解成：

```text
像 WebSocket 一样永久保持
```

---

# 二十四、Streaming 会改变 Context 吗？

不会。

是否 Streaming 和：

```text
messages
system
history
context window
```

不是一回事。

同一个 Prompt：

```text
stream = false
```

和：

```text
stream = true
```

模型看到的 Context 可以完全一样。

区别主要是：

```text
结果怎么被返回
```

---

# 二十五、Streaming 会让模型更聪明吗？

不会。

Streaming：

```text
不是 Prompt 技巧
不是推理增强
不是 RAG
不是 Few-shot
```

它不会直接提高：

```text
准确率
知识
推理能力
```

它主要改变：

```text
传输和交互体验
```

---

# 二十六、Streaming 会减少 Token 吗？

通常不会。

如果输入和最终输出相同：

```text
Token 消耗大体还是那一批
```

Streaming 只是：

```text
分批把输出传回来
```

---

# 二十七、Streaming 对后端有什么价值？

它不只是：

```text
前端打字机效果
```

后端也可以：

```text
边收到
边转发
```

例如：

```text
LLM Provider
↓ stream
Java Backend
↓ stream
Browser
```

这样才是真正：

```text
端到端 Streaming
```

---

# 二十八、如果后端收完再发前端，会怎样？

即使：

```text
Provider → Backend
```

是流式，

但如果后端：

```text
把所有 chunk 收完
↓
拼完整
↓
最后一次性返回前端
```

那么用户仍然感受不到真正实时输出。

所以：

```text
Provider 流式
≠
整个系统一定流式
```

---

# 二十九、Streaming 对 Agent 为什么重要？

Agent 后面可能：

```text
思考
调用工具
等待结果
生成回答
```

长任务中，如果用户一直：

```text
什么都看不到
```

体验会很差。

Streaming 可以帮助逐步展示：

```text
文本
状态
事件
部分结果
```

所以它是很多 Agent 产品的重要基础能力。

---

# 三十、Agent Streaming 不只是文字 Token

以后更复杂时，流里可能出现：

```text
text delta
tool call delta
tool result event
status event
error event
done event
```

所以 Streaming 最终会发展成：

```text
事件流
```

而不只是：

```text
一个字一个字输出
```

---

# 三十一、为什么现在先只学文本 Streaming？

因为现在如果同时加入：

```text
Tool Calling
Agent Loop
Streaming Tool Arguments
多种事件类型
```

复杂度会突然变高。

当前只要先搞懂：

```text
SSE
chunk
delta
DONE
拼接
```

就够了。

---

# 三十二、Java 端最大的变化

我们以前：

```java
HttpResponse.BodyHandlers.ofString()
```

希望：

```text
完整 Body → String
```

Streaming 要换一种思路：

```text
不要等待整个 Body
↓
边读边处理
```

概念上：

```text
HTTP Response Body
↓
InputStream
↓
逐行读取
↓
处理 data:
↓
解析 JSON
↓
取 delta.content
↓
打印 / append
```

---

# 三十三、为什么逐行读取很自然？

SSE 本身通常就是：

```text
文本事件流
```

类似：

```text
data: ...

空行

data: ...

空行
```

所以 Java 可以：

```text
读一行
↓
判断是不是 data:
↓
处理
```

这比：

```text
等整个 Body 完成
```

更符合 Streaming。

---

# 三十四、为什么还需要 StringBuilder？

虽然我们可以实时：

```text
打印 chunk
```

但最终很多业务仍然需要：

```text
完整回答
```

例如：

```text
保存数据库
加入 Chat History
后续作为 Context
```

所以常见做法：

```java
StringBuilder fullContent = new StringBuilder();
```

每收到：

```text
delta.content
```

就：

```java
fullContent.append(delta);
```

最后：

```java
String complete = fullContent.toString();
```

---

# 三十五、“边显示”和“最终保存”可以同时做

例如：

```java
System.out.print(delta);
fullContent.append(delta);
```

也就是：

```text
实时给用户看
+
后台同步积累完整结果
```

最后：

```text
显示完成
```

同时：

```text
完整消息也保存好了
```

---

# 三十六、chunk 不等于 Message

例如：

```text
chunk1 = "Java "
chunk2 = "是一门"
chunk3 = "语言"
```

这三个不是：

```text
三条 Assistant Message
```

它们只是：

```text
同一条 Message 的三个传输片段
```

最终：

```text
Java 是一门语言
```

才是一条完整消息。

---

# 三十七、为什么 chunk 不应该直接存进 Chat History？

如果错误地存成：

```text
assistant: Java
assistant: 是一门
assistant: 语言
```

历史结构就错了。

正确做法通常是：

```text
Streaming 期间
→ 累积 chunk

Streaming 结束
→ 得到完整 Assistant Message

然后
→ 加入 History
```

所以：

> **Streaming Fragment ≠ Conversation Message。**

---

# 三十八、什么是 [DONE]？

很多 SSE 风格 API 最后可能发送：

```text
data: [DONE]
```

表示：

```text
这次流结束了
```

客户端看到后：

```text
停止等待更多 chunk
```

然后进行：

```text
收尾
保存完整内容
记录耗时
```

---

# 三十九、[DONE] 是 JSON 吗？

通常：

```text
不是业务 JSON
```

所以代码不能把所有：

```text
data:
```

后面的内容都直接：

```text
JSON.parseObject(...)
```

而应该先判断：

```text
是不是 [DONE]
```

---

# 四十、为什么有些 chunk 没有 content？

有些流式协议可能先返回：

```text
role
metadata
finish_reason
```

而不是每条都有：

```text
delta.content
```

所以代码不能假设：

```text
每个 chunk 必有 content
```

否则容易：

```text
空指针
```

---

# 四十一、finish_reason 在 Streaming 里怎么理解？

普通响应已经结束，所以：

```text
finish_reason
```

直接在完整 Response 里。

Streaming：

```text
前面的 chunk
```

可能还没有：

```text
finish_reason
```

直到接近结束时才出现：

```text
stop
length
...
```

所以不能假设：

```text
每个 chunk 都有结束原因
```

---

# 四十二、Usage 怎么办？

不同 Provider 可能：

```text
最后一个 chunk
```

才提供 Usage，

也可能需要：

```text
额外参数
```

才返回。

所以：

```text
Token Usage
```

通常也不是：

```text
每个 chunk 都有完整值
```

这个属于 Provider-specific 细节。

真正做实验时再核对当前百炼行为。

---

# 四十三、Streaming 比普通 Response 更难解析

普通：

```text
一个完整 JSON
↓
parse once
```

Streaming：

```text
很多事件
↓
每条检查
↓
每条解析
↓
取增量
↓
拼接
↓
判断结束
```

所以代码复杂度自然会上升。

---

# 四十四、Streaming 常见边界情况

至少要知道：

```text
空行
没有 content 的 chunk
只有 role 的 chunk
finish_reason
[DONE]
网络中断
JSON 解析异常
用户主动取消
超时
```

但 Node 016 不需要一次全部做成生产级。

---

# 四十五、Streaming 和线程是什么关系？

Streaming 本身不等于：

```text
多线程
```

一个线程也可以：

```text
阻塞读取流
↓
收到一条处理一条
```

这已经是 Streaming。

所以：

```text
流式
≠
必须线程池
```

---

# 四十六、同步 Streaming 和异步 Streaming

可以有：

```text
同步阻塞读取
```

也可以：

```text
异步回调 / Reactive
```

后面的 Spring WebFlux / Reactor 会更偏：

```text
异步流
```

但当前 Java 基础实验：

```text
同步读取流
```

已经足够理解机制。

---

# 四十七、为什么现在不要直接学 Reactor？

因为那会把两个问题混在一起：

```text
LLM Streaming
+
Reactive Programming
```

如果一上来就学：

```text
Flux
Mono
Subscriber
Backpressure
```

很容易忘了：

```text
我们原本只是想搞懂模型结果怎么流回来
```

所以：

```text
先普通 Java
再框架
```

更合理。

---

# 四十八、什么是 Backpressure？

简单知道即可。

如果：

```text
生产者太快
消费者太慢
```

怎么办？

这类问题叫：

```text
Backpressure
背压
```

Streaming 系统以后可能遇到。

但当前 LLM 文本流：

```text
不需要深入
```

---

# 四十九、Streaming 能不能中途取消？

可以。

例如用户点击：

```text
停止生成
```

本质上就是：

```text
客户端不想继续接收
```

可以：

```text
取消请求
关闭流
关闭连接
```

---

# 五十、Streaming 会改变错误处理方式

普通：

```text
请求失败
→ 没结果
```

Streaming 可能：

```text
前面已经收到一部分
↓
中途网络断了
```

这时出现：

```text
Partial Response
部分响应
```

所以以后错误处理要考虑：

```text
“已经输出了一半怎么办？”
```

---

# 五十一、Streaming + Structured Output 为什么更复杂？

假设最终目标是：

```json
{
  "riskLevel": "HIGH",
  "reason": "..."
}
```

流式过程中可能只收到：

```text
{"riskLe
```

这时候：

```text
还不是完整 JSON
```

无法立即：

```text
反序列化成 DTO
```

所以：

```text
Streaming Structured Output
```

要比普通文本 Streaming 更复杂。

当前先不学。

---

# 五十二、Streaming 和 Structured Output 可以同时存在吗？

可以。

概念上：

```text
Structured Output
负责最终结构

Streaming
负责分段传输
```

它们不冲突。

但是当前：

```text
先把 Streaming 自己搞懂
```

不要同时增加变量。

---

# 五十三、Streaming 和 Tool Calling 可以同时存在吗？

也可以。

以后 Tool Call 参数可能：

```text
逐块生成
```

客户端需要不断累积：

```text
tool arguments
```

直到完整。

所以 Streaming 以后会直接影响：

```text
Tool Calling
Agent Runtime
```

---

# 五十四、Streaming 和 Chat History 的关系

Streaming 时：

```text
边输出
边拼接
```

结束后：

```text
把完整 Assistant 内容
加入 History
```

所以之前学的：

```text
Message History
```

仍然会复用。

---

# 五十五、Streaming 对日志意味着什么？

如果每个 chunk 都：

```text
INFO 日志打印
```

会非常吵。

真实工程里更有价值的可能是：

```text
完整响应
TTFT
总耗时
finish_reason
Token Usage
错误
```

Debug 时再观察：

```text
原始 chunk
```

---

# 五十六、什么是 Tokens Per Second？

可以粗略理解：

```text
模型开始输出以后
每秒生成多少 Token
```

例如：

```text
100 Token
用了 2 秒
```

大约：

```text
50 tokens/s
```

它偏向：

```text
生成速度
```

而 TTFT 偏向：

```text
开始响应速度
```

---

# 五十七、TTFT 和 Tokens/s 不一样

模型 A：

```text
TTFT = 0.5s
20 tokens/s
```

模型 B：

```text
TTFT = 2s
60 tokens/s
```

A：

```text
开始得快
后面慢
```

B：

```text
开始得慢
后面快
```

用户体验可能完全不同。

---

# 五十八、为什么现在不做性能 Benchmark？

因为 Node 016 的目标是：

```text
理解 Streaming 机制
```

不是：

```text
给模型做性能压测
```

实验时可以记录：

```text
一次 TTFT
```

帮助理解即可。

不需要：

```text
跑几十次求平均
```

---

# 五十九、几个常见误解

```text
误解 1：
Streaming = 一个字一个字返回

更准确：
按增量 chunk 持续返回
```

```text
误解 2：
Streaming 让模型生成更快

更准确：
主要让客户端更早看到部分结果
```

```text
误解 3：
Streaming 必须 WebSocket

更准确：
很多 LLM API 使用 HTTP + SSE
```

```text
误解 4：
Streaming 是前端功能

更准确：
Provider、后端、前端整条链都可能参与
```

```text
误解 5：
每个 chunk 都是一条 Message

更准确：
chunk 是同一条 Message 的传输片段
```

---

# 六十、这次理论真正需要记住的核心链

```text
Request
↓
Connection stays open
↓
Server produces incremental data
↓
SSE / stream
↓
Client reads chunks
↓
Extract delta
↓
Display immediately
↓
Append to full content
↓
DONE
↓
Complete Message
```

---

# 六十一、Java 实验大概会长什么样？

以后实验很可能从：

```java
BodyHandlers.ofString()
```

转向：

```text
能够流式读取 Response Body
```

然后：

```text
逐行读取
↓
识别 data:
↓
跳过空行
↓
识别 [DONE]
↓
解析每条 JSON
↓
取 delta.content
↓
实时打印
↓
StringBuilder 拼接完整回答
```

---

# 六十二、这个实验为什么大概率有必要？

Streaming 属于：

```text
运行机制类知识
```

只看图容易觉得：

```text
“就是一点一点返回”
```

但真正跑一次以后才能看到：

```text
SSE 原始数据长什么样
data: 是什么
chunk 怎么解析
delta.content 在哪里
[DONE] 怎么处理
```

所以 Node 016 大概率适合做一个：

```text
小而完整的实验
```

---

# 六十三、但实验不需要做太大

按照：

```text
必要时才做实验
```

的原则，

这次不需要：

```text
Spring WebFlux
前端页面
WebSocket
多线程
Reactive Streams
生产级断线重连
Backpressure 框架
```

一个最小 Java Demo：

```text
发请求
↓
流式读取
↓
实时打印
↓
拼完整回答
```

就足够。

---

# 六十四、实验最值得观察什么？

重点观察：

```text
非流式：
多久后第一次看到输出？

流式：
多久后第一次看到输出？

每次收到什么？

最终内容怎么拼出来？

结束信号是什么？
```

核心不是：

```text
代码复杂
```

而是：

```text
亲眼看到传输方式变化
```

---

# 六十五、以后和 Spring Boot 怎么结合？

以后 Web 项目中可能是：

```text
浏览器
↓
Spring Boot
↓
LLM Provider
```

后端把 Provider 的流：

```text
继续以 SSE
```

转发给浏览器。

以后会看到：

```text
SseEmitter
Flux
ServerSentEvent
```

但现在：

```text
先不学
```

---

# 六十六、为什么先学底层再学框架？

以后看到：

```java
Flux<String>
```

如果已经理解：

```text
Provider 在不断发 chunk
```

那么 Flux 只是：

```text
更方便表达和处理流
```

不会觉得：

```text
这是 AI 魔法
```

---

# 六十七、Streaming 和传统后端知识的连接

它会连接到：

```text
HTTP
InputStream
SSE
连接管理
超时
取消
异常
日志
监控
```

所以 Streaming 并不是：

```text
AI 专属知识
```

AI 只是让它：

```text
非常常见
```

---

# 六十八、为什么这个知识对 Java 后端重要？

Java 后端通常会负责：

```text
调用模型
认证
业务校验
保存会话
工具调用
把流转发给前端
```

如果后端不会 Streaming：

```text
很多 AI 产品体验会受限
```

---

# 六十九、理解自测

1. 什么是 Streaming？
2. Streaming 和普通响应最大的区别是什么？
3. Streaming 会让模型本身更聪明吗？
4. Streaming 一定能缩短总生成时间吗？
5. 什么是 TTFT？
6. 什么是 SSE？
7. SSE 和 WebSocket 最大区别是什么？
8. 什么是 chunk？
9. chunk 一定等于一个 Token 吗？
10. 什么是 delta？
11. `[DONE]` 是干什么的？
12. 为什么 `BodyHandlers.ofString()` 不适合真正 Streaming？
13. 为什么还要用 StringBuilder 累积完整内容？
14. 为什么 chunk 不应该直接存进 Chat History？
15. 为什么 Streaming 不等于必须多线程？

---

# 七十、口述验收

如果有人问：

> LLM Streaming 到底是什么？

你应该能够解释：

> Streaming 是一种响应传输方式。普通请求会等待模型把完整答案生成并返回后，客户端才拿到整个 Response；Streaming 则保持连接，在模型生成过程中持续把增量结果发送给客户端。客户端可以逐个处理 chunk，从其中读取 delta.content，一边实时展示，一边拼接成最终完整回答。
>
> Streaming 不一定让模型总生成时间更短，也不会让模型更聪明，它主要降低用户感知等待时间。常见 LLM Streaming 会使用 SSE，重点指标包括 TTFT，也就是从发出请求到收到首个 Token 的时间。
>
> 在 Java 中，Streaming 的核心变化是不能再只把整个 Response Body 一次性读取成 String，而要边读取流、边解析事件、边处理增量内容，直到收到结束信号。

如果能自然讲清：

```text
SSE
chunk
delta
TTFT
[DONE]
边读边处理
```

理论基础就够了。

---

# 七十一、Node 016-1 完成标准

读完后应该能够：

- [ ] 理解非流式和 Streaming 的区别；
- [ ] 理解 Streaming 主要改善用户感知等待；
- [ ] 理解 TTFT；
- [ ] 理解 chunk 不一定等于一个 Token；
- [ ] 理解 delta 是增量；
- [ ] 理解 SSE 的基本方向；
- [ ] 理解 SSE 和 WebSocket 不同；
- [ ] 理解 `[DONE]` 的作用；
- [ ] 理解为什么流式响应需要保持连接；
- [ ] 理解为什么不能简单等待完整 Body；
- [ ] 理解为什么客户端仍需拼完整内容；
- [ ] 理解 chunk 不是 Message；
- [ ] 理解 Streaming 和 Chat History 的关系；
- [ ] 理解 Streaming 和 Structured Output 可以组合但暂不一起学；
- [ ] 理解 Node 016 为什么大概率值得做一个小实验。

---

# 七十二、本篇停止点

本篇只负责：

```text
理解 Streaming 是什么
```

暂时不要求：

```text
写 Java Streaming Demo
调用真实模型
性能 Benchmark
Spring WebFlux
Flux / Mono
SseEmitter
WebSocket
前端页面
断线重连
Backpressure
Streaming Structured Output
Streaming Tool Calling
```

这些内容：

```text
必要的
→ 进入实验篇

过早的
→ 留到后面
```

---

# 七十三、下一步

读完本篇以后，再决定：

```text
016-2 是否做实验
```

从知识性质来看：

```text
Streaming 是运行机制类知识
```

因此最有价值的实践通常是：

```text
一个最小 Java Streaming Demo
```

目标只需要：

```text
调用 qwen3.8-max
↓
开启 Streaming
↓
实时读取 SSE
↓
打印 delta.content
↓
统计首个 chunk 时间
↓
拼接最终完整回答
```

不需要复杂框架。

---

# 本篇一句话总结

> **Streaming 的本质，是模型还没生成完整答案时，服务端就把已经生成的增量结果持续发送给客户端，让程序可以边接收、边处理、边展示。**

核心链路：

```text
Request
↓
Model generating
↓
SSE / Stream
↓
Chunk
↓
Delta
↓
Java reads immediately
↓
Display + Append
↓
DONE
↓
Complete Message
```

**Node 016-1：先理解 Streaming，再决定最小必要实验。**
