# 016-2 - 实验：使用 Java 实现 Streaming

> Node 016：Streaming\
> Phase 1：LLM 应用开发基础\
> 实验项目：`projects/agent-learning-lab`

---

> **当前记录（2026-11-03）：**用户已提供默认模式的真实 HTTP 200 输出，成功拼接正文，读到 Model、finish_reason=stop、Usage 及 DONE；首段正文延迟和总耗时已记录。随后用户提供 --debug 原始事件，代表性的角色、正文、finish、Usage 和 DONE 已记录，增量拼接与完整回答一致。Node 016 阅读讲解及默认/Debug 正常实测已收尾，下一步 Node 017「模型参数（准备学习）」；新增本地模拟仍未执行，不冒充错误路径验证。

# 一、实验目标

016-1 已经理解：

```text
普通响应
=
等待完整答案
↓
一次性返回

Streaming
=
模型边生成
↓
服务端边发送
↓
Java 边读取
```

016-2 要真正跑通：

```text
Java Request
↓
stream = true
↓
qwen3.8-max
↓
SSE
↓
data: {...}
↓
choices[0].delta.content
↓
实时打印
↓
StringBuilder 拼接
↓
finish_reason
↓
Usage chunk
↓
[DONE]
```

这次实验完成后，不只是“知道 Streaming 是什么”，而是要亲眼看到模型响应一块一块返回。

---

# 二、为什么这个实验有必要？

我们的原则一直是：

> **必要时才做实验。**

Streaming 属于运行机制类知识。只看理论很容易停留在：

```text
“就是一点一点返回”
```

真正运行以后才能看见：

```text
SSE 原始数据
chunk
delta.content
finish_reason
Usage chunk
[DONE]
```

所以这次值得做一个小实验，但不需要把它做成复杂框架。

---

# 三、这次不要做什么？

暂时不要引入：

```text
Spring Boot
Spring WebFlux
Flux / Mono
SseEmitter
WebSocket
多线程
线程池
Reactive Streams
断线重连
Backpressure
前端页面
Streaming Structured Output
Streaming Tool Calling
```

继续使用：

```text
Java 21
HttpClient
FastJSON2
qwen3.8-max
```

---

# 四、项目中新增什么？

继续使用：

```text
Agent/projects/agent-learning-lab
```

新增：

```text
src/main/java/com/avexvertex/agent/StreamingDemo.java
```

本次不需要新增 DTO。

---

# 五、请求体怎么开启 Streaming？

普通请求大致：

```json
{
  "model": "qwen3.8-max",
  "messages": [...]
}
```

这次新增：

```json
"stream": true
```

为了最后看到 Token Usage，再加：

```json
"stream_options": {
  "include_usage": true
}
```

完整示意：

```json
{
  "model": "qwen3.8-max",
  "messages": [
    {
      "role": "user",
      "content": "请用 3 到 5 句话解释 Java HashMap 为什么不是线程安全的。"
    }
  ],
  "stream": true,
  "stream_options": {
    "include_usage": true
  },
  "reasoning_effort": "none"
}
```

---

# 六、为什么 Prompt 不要太长？

这次要观察的是：

```text
Streaming 行为
```

不是模型知识能力。

Prompt 太短：

```text
流很快结束，不好观察
```

Prompt 太长：

```text
没有必要
```

所以 3～5 句话的回答最合适。

---

# 七、流式返回和非流式返回的核心区别

非流式：

```text
choices[0].message.content
```

流式：

```text
choices[0].delta.content
```

这是本实验最重要的结构变化。

---

# 八、非流式 Response 示例

```json
{
  "choices": [
    {
      "message": {
        "role": "assistant",
        "content": "HashMap 不是线程安全的，因为……"
      },
      "finish_reason": "stop"
    }
  ]
}
```

这是一个完整 JSON。

---

# 九、Streaming 返回不是一个完整 JSON

开启 Streaming 后会持续收到 SSE：

```text
data: {...}

data: {...}

data: {...}

data: [DONE]
```

每个 `data:` 后面是一块增量数据。

---

# 十、第一块可能是什么样？

例如：

```text
data: {"choices":[{"delta":{"role":"assistant","content":""},"finish_reason":null,"index":0}],"object":"chat.completion.chunk"}
```

注意：

```text
role = assistant
```

可能先出现。

而：

```text
content
```

可能还是空字符串。

因此：

```text
第一个 chunk
≠
一定已经有正文
```

---

# 十一、中间 chunk

例如：

```text
data: {"choices":[{"delta":{"content":"HashMap"},"finish_reason":null,"index":0}],"object":"chat.completion.chunk"}
```

下一块：

```text
data: {"choices":[{"delta":{"content":" 本身"},"finish_reason":null,"index":0}],"object":"chat.completion.chunk"}
```

下一块：

```text
data: {"choices":[{"delta":{"content":"没有同步机制"},"finish_reason":null,"index":0}],"object":"chat.completion.chunk"}
```

客户端要把它们拼起来。

---

# 十二、结束 chunk

接近结束时可能出现：

```json
{
  "choices": [
    {
      "delta": {
        "content": ""
      },
      "finish_reason": "stop",
      "index": 0
    }
  ]
}
```

这里：

```text
finish_reason = stop
```

说明模型内容生成结束。

---

# 十三、Usage chunk

如果设置：

```json
"stream_options": {
  "include_usage": true
}
```

最后还会有一个包含 Usage 的 chunk，例如：

```json
{
  "choices": [],
  "usage": {
    "prompt_tokens": 30,
    "completion_tokens": 70,
    "total_tokens": 100
  }
}
```

非常重要：

```text
choices = []
```

所以代码不能每次都直接取：

```java
choices.getJSONObject(0)
```

---

# 十四、最后还有 [DONE]

SSE 最后：

```text
data: [DONE]
```

它是结束标记，不是 JSON。

正确顺序：

```text
拿到 data 后面的 payload
↓
先判断是不是 [DONE]
↓
不是才 JSON.parseObject(...)
```

---

# 十五、完整事件序列

可以这样理解：

```text
HTTP 200
↓
role chunk
content 可能为空
↓
content chunk
↓
content chunk
↓
content chunk
↓
finish_reason = stop
↓
usage chunk
choices=[]
↓
[DONE]
```

---

# 十六、Java 最大的变化

以前：

```java
HttpResponse<String> response = client.send(
        request,
        HttpResponse.BodyHandlers.ofString()
);
```

这会等完整 Body。

Streaming 更适合：

```java
HttpResponse<InputStream> response = client.send(
        request,
        HttpResponse.BodyHandlers.ofInputStream()
);
```

因为：

```text
Response Body 还在持续到达
```

Java 要边到达边读取。

---

# 十七、为什么使用 BufferedReader？

SSE 本质是文本事件流。

可以：

```java
BufferedReader reader = new BufferedReader(
        new InputStreamReader(
                response.body(),
                StandardCharsets.UTF_8
        )
);
```

然后：

```java
reader.readLine()
```

逐行读取。

---

# 十八、SSE 主循环

概念代码：

```java
String line;

while ((line = reader.readLine()) != null) {

    if (line.isBlank()) {
        continue;
    }

    if (!line.startsWith("data:")) {
        continue;
    }

    String payload = line.substring(5).trim();

    if ("[DONE]".equals(payload)) {
        break;
    }

    JSONObject chunk = JSON.parseObject(payload);

    // 处理 delta / usage
}
```

这是整个实验的核心之一。

---

# 十九、为什么先检查 data:？

因为：

```text
data:
```

属于 SSE 协议。

它不是 JSON 的一部分。

例如：

```text
data: {"choices":[]}
```

真正需要解析的 JSON 是：

```json
{"choices":[]}
```

所以必须先去掉 `data:`。

---

# 二十、为什么先判断 [DONE]？

因为：

```text
[DONE]
```

不是 JSON。

如果先：

```java
JSON.parseObject("[DONE]")
```

就会失败。

所以：

```text
SSE 协议判断
```

要发生在：

```text
JSON 解析
```

之前。

---

# 二十一、如何处理 Usage chunk？

拿到 chunk：

```java
JSONArray choices = chunk.getJSONArray("choices");
```

如果：

```text
choices == null
或
choices.isEmpty()
```

再检查：

```java
JSONObject usage = chunk.getJSONObject("usage");
```

如果 usage 存在：

```text
保存 prompt_tokens
保存 completion_tokens
保存 total_tokens
```

然后：

```text
continue
```

---

# 二十二、如何处理普通内容 chunk？

如果 choices 非空：

```java
JSONObject choice = choices.getJSONObject(0);
JSONObject delta = choice.getJSONObject("delta");
String content = delta.getString("content");
```

但：

```text
content
```

可能是：

```text
null
或空字符串
```

所以应该判断：

```java
if (content != null && !content.isEmpty()) {
    ...
}
```

---

# 二十三、实时输出

收到非空正文：

```java
System.out.print(content);
```

这里使用：

```text
print
```

而不是：

```text
println
```

因为 chunk 本身不是自然换行。

---

# 二十四、为什么还需要 StringBuilder？

实时显示之外，业务最终仍然需要：

```text
完整 Assistant Message
```

所以：

```java
StringBuilder fullContent = new StringBuilder();
```

每次：

```java
fullContent.append(content);
```

最后：

```java
String finalMessage = fullContent.toString();
```

---

# 二十五、chunk 不是 Message

例如：

```text
chunk1 = "Hash"
chunk2 = "Map"
chunk3 = "不是线程安全的"
```

这不是三条 Assistant Message。

而是：

```text
同一条 Assistant Message
的三个传输片段
```

最终：

```text
HashMap不是线程安全的
```

才是一条完整消息。

---

# 二十六、为什么不能把每个 chunk 存进 Chat History？

否则历史会变成：

```text
assistant: Hash
assistant: Map
assistant: 不是线程安全的
```

这显然不是真实对话结构。

正确流程：

```text
Streaming 时
→ append chunk

结束后
→ 得到完整 Assistant Message

然后
→ 放进 Chat History
```

---

# 二十七、finish_reason 怎么处理？

可以：

```java
String finishReason = choice.getString("finish_reason");
```

中间通常是：

```text
null
```

结束附近可能：

```text
stop
```

可以记录下来。

但不要看到 `stop` 就立即认为整个 SSE 流已经结束，因为后面可能还有：

```text
Usage chunk
[DONE]
```

---

# 二十八、finish_reason 和 [DONE] 的区别

```text
finish_reason
=
模型内容为什么停止生成

[DONE]
=
这次 SSE 数据流结束
```

这是两个不同层次。

---

# 二十九、这次顺便记录 TTFT

实验很适合观察：

```text
TTFT
```

不用做 Benchmark，只记录一次即可。

请求发送前：

```java
long startNanos = System.nanoTime();
```

收到第一个非空 `delta.content` 时：

```java
long firstContentNanos = System.nanoTime();
```

计算：

```java
double ttftMs =
        (firstContentNanos - startNanos) / 1_000_000.0;
```

---

# 三十、为什么记录“首个非空 content”？

因为第一块可能只有：

```text
role
```

或者：

```text
content = ""
```

如果第一条 SSE 就记 TTFT，会把：

```text
协议元数据到达时间
```

误当成：

```text
真正正文开始时间
```

因此这次实验记录：

```text
首个非空 delta.content 延迟
```

更有意义。

---

# 三十一、完整耗时也记录一次

遇到：

```text
[DONE]
```

以后记录：

```java
long endNanos = System.nanoTime();
```

于是得到：

```text
Total Time
```

这次就能直观看：

```text
TTFT
vs
完整生成时间
```

---

# 三十二、为什么不做性能压测？

因为现在目标是：

```text
理解 Streaming
```

不是：

```text
Benchmark qwen3.8-max
```

单次 TTFT 只是帮助理解。

不要：

```text
跑几十次
求平均
比较模型
```

这些不是当前节点的必要内容。

---

# 三十三、建议的 StreamingDemo 结构

```text
main
│
├── 读取 DASHSCOPE_API_KEY
├── 读取 DASHSCOPE_BASE_URL
├── 构造 endpoint
├── 构造 messages
├── stream=true
├── include_usage=true
├── 记录 startNanos
├── POST
├── 检查 HTTP Status
├── BufferedReader 读取 InputStream
├── while(readLine)
│   ├── 空行 → skip
│   ├── 非 data: → skip
│   ├── [DONE] → break
│   ├── parse chunk JSON
│   ├── choices 为空 → usage
│   ├── choices 非空 → delta
│   ├── content 非空 → print + append
│   └── finish_reason → 记录
├── 记录 endNanos
├── 打印完整回答
├── 打印 TTFT
├── 打印 Total Time
└── 打印 Token Usage
```

---

# 三十四、建议保留一个 --debug 模式

Node 016 的核心之一就是：

```text
亲眼看真实 SSE
```

所以可以给：

```text
StreamingDemo
```

提供：

```text
--debug
```

默认模式：

```text
只实时显示正文
+
最终指标
```

Debug 模式：

```text
额外打印每个真实 SSE payload
```

---

# 三十五、为什么不默认打印全部 chunk JSON？

如果每块都打印：

```text
JSON
正文
JSON
正文
JSON
正文
```

会非常乱。

所以：

```text
默认模式
→ 看用户体验

--debug
→ 看协议结构
```

比较合适。

---

# 三十六、Debug 模式要观察哪些代表性 chunk？

不需要把几十个 chunk 全塞进学习笔记。

只保留：

```text
第一块
一个普通正文块
finish_reason 块
Usage 块
[DONE]
```

就足够。

---

# 三十七、错误路径做到什么程度？

这次只需要基本处理：

```text
缺少 API Key
Base URL 错误
HTTP 非 2xx
SSE JSON 解析失败
IOException
InterruptedException
```

不要做：

```text
自动重试
断线重连
生产级容错
```

Node 020 会系统学习 LLM API 异常。

---

# 三十八、为什么 HTTP Status 要先检查？

如果返回：

```text
400
401
500
```

服务端可能返回的是：

```text
普通错误 JSON
```

而不是正常 SSE。

因此：

```text
先判断 Status
```

非 2xx：

```text
读取错误 Body
打印
停止
```

不要继续按 `data:` 处理。

---

# 三十九、API Key 继续放环境变量

保持：

```text
DASHSCOPE_API_KEY
DASHSCOPE_BASE_URL
```

不要：

```text
硬编码 API Key
打印 Authorization Header
```

---

# 四十、Request Body 可以打印吗？

学习环境中可以打印：

```text
model
messages
stream=true
stream_options
reasoning_effort
```

因为 API Key 不在 Body 中。

但如果以后 Prompt 带真实敏感业务数据：

```text
日志也要谨慎
```

---

# 四十一、这次最值得理解的两段代码

第一段：

```java
while ((line = reader.readLine()) != null) {

    if (!line.startsWith("data:")) {
        continue;
    }

    String payload = line.substring(5).trim();

    if ("[DONE]".equals(payload)) {
        break;
    }

    JSONObject chunk = JSON.parseObject(payload);
}
```

它负责：

```text
SSE → JSON chunk
```

第二段：

```java
String content = delta.getString("content");

if (content != null && !content.isEmpty()) {
    System.out.print(content);
    fullContent.append(content);
}
```

它负责：

```text
增量内容
→ 实时显示
→ 拼完整 Message
```

---

# 四十二、为什么这两段就是 Streaming 核心？

因为 Streaming 最本质的事情只有：

```text
不断收到
↓
不断处理
```

并不是：

```text
某个神奇 AI 类
```

SSE 和 JSON 都只是协议细节。

---

# 四十三、第一次真实运行时观察什么？

重点不是回答内容，而是：

```text
第一块有没有 role？
第一块 content 是不是空？
object 是否是 chat.completion.chunk？
普通 chunk 的 delta.content 多长？
finish_reason 前面是否一直 null？
stop 在哪一块出现？
Usage chunk 的 choices 是否为空？
[DONE] 是否单独出现？
```

---

# 四十四、不要假设所有 chunk 一样

可能有：

```text
角色 chunk
正文 chunk
结束 chunk
Usage chunk
```

所以代码要：

```text
先检查字段
再读取
```

而不是假设每块都有：

```text
choices[0].delta.content
```

---

# 四十五、这和 Node 015 最大区别是什么？

Node 015：

```text
拿到完整业务 JSON
↓
一次性反序列化 DTO
```

Node 016：

```text
不断收到 Provider 事件 JSON
↓
每次取增量
↓
拼完整 Message
```

所以：

```text
Node 015
偏数据结构

Node 016
偏传输机制
```

---

# 四十六、为什么现在不做 Streaming Structured Output？

因为那会变成：

```text
流式读取
+
JSON 尚未完整
+
Schema
+
DTO
```

变量太多。

当前只做：

```text
普通文本 Streaming
```

先把机制搞懂。

---

# 四十七、为什么现在不做 WebFlux？

因为那又会加入：

```text
Reactive Programming
```

你会同时学习：

```text
Flux
Mono
Subscriber
Backpressure
```

这会稀释当前目标。

所以继续：

```text
普通 Java HttpClient
```

最合适。

---

# 四十八、Codex 这次应该负责什么？

可以让 Codex：

```text
读取现有 agent-learning-lab

新增 StreamingDemo.java

保持 Java 21 + HttpClient + FastJSON2

使用 qwen3.8-max

请求中设置：
stream=true
stream_options.include_usage=true

使用 BodyHandlers.ofInputStream()

BufferedReader 逐行读取 SSE

识别 data:

识别 [DONE]

解析 choices[0].delta.content

实时 print

StringBuilder 拼完整回答

记录首个非空 content 延迟

记录完整耗时

读取 finish_reason

读取 Usage chunk

增加 --debug 查看原始 SSE payload

添加中文注释

编译通过
```

---

# 四十九、Codex 不应该做什么？

不要：

```text
新增 Spring
新增 Reactor
新增 WebSocket
新增 Controller
重构旧 Demo
创建通用 LLM SDK
创建 Provider Factory
修改之前实验
伪造真实 API 结果
```

---

# 五十、真实调用怎么做？

如果 Codex 环境没有你的本地环境变量：

```text
Codex
→ 写代码
→ 编译

用户 IDEA
→ 真实调用
```

和 Node 015 一样。

---

# 五十一、真实运行后需要记录什么？

把控制台结果记录回来：

```text
运行日期
HTTP Status
Model
stream=true
include_usage=true

第一个非空 delta.content
TTFT
Total Time

Finish Reason

Prompt Tokens
Completion Tokens
Total Tokens

最终完整回答
```

---

# 五十二、真实实验记录

2026-11-03，用户提供 StreamingDemo 默认模式的真实控制台输出，不是本地模拟。

```text
HTTP Status：200
Model：qwen3.8-max
Stream：true
Include Usage：true
第一个非空 Content："Java"
TTFT（首个非空正文）：657.241167 ms
Total Time（收到 DONE）：3899.898959 ms
Finish Reason：stop
Prompt Tokens：30
Completion Tokens：138
Total Tokens：168
[DONE] received：true
```

实际 Request Body：

```json
{"model":"qwen3.8-max","messages":[{"role":"user","content":"请用 3 到 5 句话解释 Java HashMap 为什么不是线程安全的。"}],"stream":true,"stream_options":{"include_usage":true},"reasoning_effort":"none"}
```

实际“实时正文”与“Full Content（已拼接正文）”两部分文本一致，完整内容保留如下：

````text
Java HashMap 不是线程安全的，根本原因在于它的所有读写操作都没有加锁或任何同步机制。在多线程并发执行 `put` 操作时，如果多个线程同时触发扩容（resize），可能会导致链表成环（JDK 7）或数据覆盖丢失（JDK 8）。此外，一个线程正在遍历 HashMap 时，另一个线程修改了它的结构，会引发 `ConcurrentModificationException` 异常（即快速失败机制）。由于缺乏内存可见性保障，一个线程写入的数据也可能无法被其他线程及时看到。因此，在并发环境下应使用 `ConcurrentHashMap` 或 `Collections.synchronizedMap()` 来替代 HashMap。
````

本次约 0.657 秒检测到第一段非空正文，约 3.900 秒收到 DONE。指标包含客户端观察到的网络/处理过程，只是一个真实样本，不能用来证明总生成速度优于非流式，也不是精确到模型内部 Token 的时间。
用户粘贴的是最终文本日志，无法由此单独验证屏幕上的逐段出现过程；程序的 print/flush 与 append 已实现，现场体验仍以用户实际观察为准。
模型对 HashMap 的解释按原文保存，本次目标是验证传输、解析和拼接，不将回复内容当作已独立核实的 Java 技术结论。

---

# 五十三、Debug SSE 真实记录

2026-11-03，用户提供 --debug 完整输出，HTTP 200，Model=qwen3.8-max。只保存代表性事件，不把全部重复元数据复制进笔记。
本次首段正文为 "Java"，正文延迟 786.463 ms，总耗时 4358.231667 ms，finish_reason=stop，输入/输出/总 Token 为 30/166/196，DONE received=true。这是第二次独立请求，保留第五十二节默认模式的原数字。

## 第一块：角色与空正文

```text
data: {"model":"qwen3.8-max","id":"chatcmpl-b99b62a0-69c3-9392-aa76-aeb65fcce064","created":1793718028,"object":"chat.completion.chunk","usage":null,"choices":[{"logprobs":null,"index":0,"delta":{"content":"","role":"assistant"},"finish_reason":null}]}
```

## 普通正文块：只含新增文字

```text
data: {"id":"chatcmpl-b99b62a0-69c3-9392-aa76-aeb65fcce064","object":"chat.completion.chunk","created":1793718028,"model":"qwen3.8-max","choices":[{"index":0,"delta":{"content":"Java"},"logprobs":null,"finish_reason":null}],"usage":null}
```

## finish_reason 块：记录生成停止原因

```text
data: {"id":"chatcmpl-b99b62a0-69c3-9392-aa76-aeb65fcce064","object":"chat.completion.chunk","created":1793718032,"model":"qwen3.8-max","choices":[{"index":0,"delta":{"content":""},"finish_reason":"stop","logprobs":null}],"usage":null}
```

## Usage 块：choices 为空，记录用量

```text
data: {"id":"chatcmpl-b99b62a0-69c3-9392-aa76-aeb65fcce064","object":"chat.completion.chunk","created":1793718032,"model":"qwen3.8-max","choices":[],"usage":{"prompt_tokens":30,"total_tokens":196,"completion_tokens":166,"prompt_tokens_details":{"cached_tokens":0,"text_tokens":30}}}
```

## 结束标记：不是 JSON

```text
data: [DONE]
```

## 增量与拼接

本次开头正文依次为：

```text
"Java"
" HashMap 不是"
"线程安全的"
"，根本"
```

每一块带的是新增内容，不是从头重发的累计正文；程序逐次 print / append，组成一条回答。
本次解析出的正文片段拼接后，与用户输出的 Full Content 一致。chunk 大小不固定，不能把每块当作一个 Token。
Debug 同时显示 payload JSON 与提取的正文，所以同一片段在日志的不同展示位置重复出现，不代表模型重复生成。

本次最终正文（原文）：

````text
Java HashMap 不是线程安全的，根本原因在于它的所有读写操作都没有使用任何同步机制（如锁）来保护共享状态。当多个线程同时执行 `put` 操作并触发扩容（resize）时，并发修改内部数组和链表结构会导致数据丢失或覆盖。在 JDK 1.7 及更早版本中，多线程扩容还会使链表形成环形引用，导致后续读取时陷入死循环；JDK 1.8 虽然改用红黑树缓解了死循环问题，但依然存在数据不一致的风险。此外，一个线程正在遍历 HashMap 时，若另一个线程对其进行了结构性修改，会破坏迭代器的预期状态并抛出 `ConcurrentModificationException`。因此，在多线程环境下应使用 `ConcurrentHashMap` 等专门为并发设计的集合类来替代 HashMap。
````

## 观察边界

真实事件验证了角色块可为空、普通 delta 递增、finish 后仍有独立 Usage、choices=[] 以及 DONE。默认/Debug 两次正文、耗时和用量不同，分别保留，不据此做性能对照。
用户正在观察增量行为；日志可验证收到的事件和拼接结果，不以单次结果声明性能稳定，也不把模型的 HashMap 技术表述当作已独立核实。
新增本地模拟仍未执行；读流空闲超时与错误场景未验证。

---

# 五十四、实验完成后应该能回答什么？

1. 请求里怎样开启 Streaming？
2. `data:` 是 JSON 的一部分吗？
3. 为什么要去掉 `data:` 再解析？
4. 为什么 `[DONE]` 不能直接 JSON.parse？
5. 非流式为什么是 `message.content`？
6. 流式为什么是 `delta.content`？
7. chunk 和 Message 有什么区别？
8. 为什么需要 StringBuilder？
9. 为什么第一块不一定有正文？
10. 为什么 Usage chunk 的 `choices` 可能为空？
11. `finish_reason=stop` 和 `[DONE]` 有什么区别？
12. TTFT 是怎样记录的？

---

# 五十五、实验完成标准

## 代码

- [x] 新增 `StreamingDemo.java`
- [x] Java 21 编译通过
- [x] 使用 HttpClient
- [x] 使用 FastJSON2
- [x] 不新增不必要依赖
- [x] 本次代码及笔记未包含真实 API Key（真实配置留在 IDEA）

## 请求

- [x] `stream=true`
- [x] `stream_options.include_usage=true`
- [x] 继续使用 qwen3.8-max
- [x] 继续使用现有环境变量

## Streaming

- [x] HTTP 2xx
- [x] 能逐行读取 SSE
- [x] 能识别 `data:`
- [x] 能识别 `[DONE]`
- [x] 能解析 chunk JSON
- [x] 能读取 `choices[0].delta.content`
- [x] 真实流程执行 print/flush 与增量拼接，用户已观察新 content；未单独测量控制台 GUI 刷新时序
- [x] 能拼出完整回答
- [x] 能读取 finish_reason
- [x] 能读取 Usage chunk

## 性能理解

- [x] 记录一次首个非空正文延迟
- [x] 记录一次完整耗时
- [x] 已结合真实数值讲解首段正文延迟与 Total Time 的区别（未另行口述）
- [x] 默认与 Debug 作为独立样本记录，不把单次结果当性能 Benchmark

---

# 五十六、什么时候 Node 016 可以结束？

只要真正跑通一次：

```text
stream=true
↓
SSE
↓
delta.content
↓
实时输出
↓
StringBuilder
↓
完整 Message
↓
Usage
↓
[DONE]
```

并能解释：

```text
chunk
delta
TTFT
finish_reason
Usage chunk
[DONE]
```

Node 016 就基本完成。

---

# 五十七、实验成功后不要继续无限扩展

暂时不要顺手继续做：

```text
Spring WebFlux
SSE Controller
前端 Chat 页面
Streaming Structured Output
Streaming Tool Calling
断线重连
多 Provider Streaming Adapter
性能压测
```

这些现在都不是必要的。

---

# 五十八、下一步

Node 016 收尾后：

```text
Node 017：模型参数
```

会开始理解：

```text
temperature
top_p
max_tokens
stop
reasoning_effort
```

以及这些参数什么时候值得调整。

---

# 本篇一句话总结

> **016-2 的目标不是做复杂流式框架，而是用最小 Java 代码亲眼看到：模型通过 SSE 不断返回 chunk，Java 从 `delta.content` 读取增量，一边实时显示，一边拼成最终完整 Message。**

核心闭环：

```text
Java Request
↓
stream=true
↓
qwen3.8-max
↓
SSE
↓
data: chunk
↓
choices[0].delta.content
↓
print + append
↓
finish_reason
↓
Usage chunk
↓
[DONE]
↓
Complete Assistant Message
```

**完成这一条真实链路，Node 016 就基本完成。**

---

# 资料核对说明

生成本文时已核对阿里云百炼当前 OpenAI-compatible Streaming 官方说明：

```text
开启：
stream = true

协议：
SSE

非流式正文：
choices[0].message.content

流式正文：
choices[0].delta.content

结束：
data: [DONE]

Token Usage：
stream_options.include_usage = true

最后 Usage chunk：
choices 可能为空
```

Provider-specific 参数以后若发生变化，以最新官方文档为准。

真正需要长期掌握的是：

```text
SSE
chunk
delta
边读边处理
TTFT
完整消息拼接
```

## Node 016 学习收尾记录（2026-11-03）

016-1 已读完，016-2 默认模式和 Debug 模式均有用户提供的真实结果；已讲解请求的 stream/include_usage、HTTP 响应流、SSE 事件边界、delta 增量与拼接、finish_reason/Usage/DONE 以及两类耗时。
本 Node 当前学习范围收尾，进度进入 Node 017「模型参数（准备学习）」，不另行要求逐题口述或增加更多功能。
新增本地模拟未执行；未独立测控制台刷新时序、未实现读流空闲超时、未做性能压测或生产级重连，均不记为已完成。本次只更新记录，未改代码或自行提交推送。
