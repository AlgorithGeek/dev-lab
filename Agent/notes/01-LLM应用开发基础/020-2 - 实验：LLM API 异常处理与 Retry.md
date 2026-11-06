# 020-2 - 实验：LLM API 异常处理与 Retry

> Node 020：LLM API 异常\
> Phase 1：LLM 应用开发基础\
> 实验项目：`projects/agent-learning-lab`

---

# 一、实验目标

020-1 已经建立：

```text
失败
↓
发生在哪一层？
↓
是什么错误？
↓
Retryable？
├── YES
│   ↓
│  Backoff + Limited Retry
│
└── NO
    ↓
   Fail Fast
   或修请求 / 配置 / Context
```

这次实验不是为了：

```text
收集一堆错误码
```

而是为了：

> **把“错误分类”和“Retry 决策”真正写成 Java 控制流。**

最终验证：

```text
A：错误模型 / 404
→ DO_NOT_RETRY

B：429 Rate Limit
→ RETRY

C：503 Service Unavailable
→ RETRY

D：Timeout
→ CONDITIONAL

E：Context Too Long
→ DO_NOT_RETRY
→ REDUCE_CONTEXT
```

---

# 二、为什么这一节必须做实验？

如果只看理论，很容易停留在：

```text
429 要重试
400 不重试
5xx 可以重试
```

但代码真正要解决的是：

```text
拿到 HttpResponse 后怎么判断？
↓
Error Body 怎么解析？
↓
Timeout 为什么没有 statusCode？
↓
哪些错误应该进入 Retry？
↓
哪些错误应该直接结束？
```

这些属于：

```text
程序行为
```

而不是只靠背概念就能真正掌握。

---

# 三、本实验只做五种场景

```text
A：Invalid Model / 404
B：429 Rate Limit
C：503 Service Unavailable
D：Timeout
E：Context Too Long
```

其中：

```text
A：真实 Provider
B / C / D / E：本地模拟
```

---

# 四、为什么只有 A 用真实 API？

因为：

```text
错误模型名
```

安全、明确、成本低。

而且你以前已经真实遇到过：

```text
model_not_found
```

非常适合正式纳入错误分类实验。

其他错误不需要硬造：

```text
429
→ 不要疯狂请求真实 Provider

503
→ 无法控制 Provider 什么时候过载

Timeout
→ 真实网络不容易稳定复现

Context Too Long
→ 没必要浪费大量 Token 故意撑爆
```

所以：

> **能稳定本地模拟的错误，不要为了“真实”而浪费资源。**

---

# 五、这次不要做什么？

不要引入：

```text
Spring Boot
Resilience4j
Spring Retry
Circuit Breaker
Fallback
多 Provider
统一异常框架
复杂 ErrorCode 体系
生产级 Jitter
Metrics
Tracing
```

继续保持：

```text
Java 21
HttpClient
FastJSON2
JDK 自带 HttpServer
```

---

# 六、项目中新增什么？

继续使用：

```text
Agent/projects/agent-learning-lab
```

新增：

```text
src/main/java/com/avexvertex/agent/LlmApiErrorDemo.java
```

---

# 七、建议支持的运行模式

```text
--invalid-model
--rate-limit
--server-error
--timeout
--context-too-long
```

无参数默认：

```text
--invalid-model
```

---

# 八、为什么不要一次自动跑五个？

学习目标是：

```text
看清每一种错误发生了什么
```

而不是：

```text
一键 Benchmark
```

所以建议：

```text
一组一组运行
```

每组都观察：

```text
HTTP Status
Provider Error Body
Java Exception
Retry Policy
Recommended Action
```

---

# 九、统一输出格式

建议每组都尽量输出：

```text
Scenario:
Layer:
HTTP Status:
Provider Code:
Provider Message:
Request ID:
Java Exception:
Retry Policy:
Recommended Action:
```

例如：

```text
Scenario: INVALID_MODEL
Layer: HTTP / PROVIDER
HTTP Status: 404
Provider Code: model_not_found
Java Exception: NONE
Retry Policy: DO_NOT_RETRY
Recommended Action: FIX_MODEL
```

---

# 十、为什么统一输出很重要？

因为实验真正想建立的是：

```text
同一套错误模型
```

而不是：

```text
每个异常单独写一坨 if
```

这样更容易看到：

```text
404
429
503
Timeout
Context Too Long
```

最终都可以被归一成：

```text
错误分类结果
```

---

# 十一、建议定义 Scenario enum

```java
enum Scenario {
    INVALID_MODEL,
    RATE_LIMIT,
    SERVER_ERROR,
    TIMEOUT,
    CONTEXT_TOO_LONG
}
```

只是为了：

```text
运行模式清楚
```

不要扩展成复杂框架。

---

# 十二、建议定义 RetryPolicy 三态

```java
enum RetryPolicy {
    RETRY,
    DO_NOT_RETRY,
    CONDITIONAL
}
```

这个三态有真实意义。

因为：

```text
Timeout
```

确实很难只用：

```text
true / false
```

表达。

---

# 十三、五种场景对应的 RetryPolicy

```text
404
→ DO_NOT_RETRY

429
→ RETRY

503
→ RETRY

Timeout
→ CONDITIONAL

Context Too Long
→ DO_NOT_RETRY
```

---

# 十四、可以定义一个简单 ErrorDecision

例如：

```java
record ErrorDecision(
        String layer,
        Integer httpStatus,
        String providerCode,
        String providerMessage,
        String requestId,
        String javaException,
        RetryPolicy retryPolicy,
        String action
) {}
```

这里使用 record：

```text
只是为了让实验结果结构清楚
```

不是为了搭框架。

---

# 十五、action 可以有哪些？

保持简单：

```text
FIX_MODEL
BACKOFF_RETRY
TIMEOUT_RETRY_OPTIONAL
REDUCE_CONTEXT
FAIL
```

当前实验真正会用到这些就够了。

---

# 十六、实验 A：Invalid Model / 404

这是唯一一个真实调用场景。

请求中故意把模型改成不存在的名字：

```json
{
  "model": "qwen3.8-max-invalid-demo",
  "messages": [
    {
      "role": "user",
      "content": "hello"
    }
  ]
}
```

---

# 十七、A 组预期

大概率得到：

```text
HTTP 404
```

Provider Error Body 里可能出现：

```text
model_not_found
```

或同类错误信息。

---

# 十八、A 组真正要观察什么？

重点不是只看：

```text
404
```

而是观察：

```text
Java HttpClient 有没有因为 404 自动抛异常？
```

正确认知应该是：

```text
通常不会
```

而是仍然正常拿到：

```java
HttpResponse<String> response
```

然后：

```java
response.statusCode()
```

得到：

```text
404
```

---

# 十九、A 组要验证的理论

```text
HTTP Error
≠
Java Exception
```

404 说明：

```text
HTTP Response 已经返回
```

只是：

```text
请求业务失败
```

---

# 二十、A 组分类

建议：

```text
Layer:
HTTP / PROVIDER

Retry Policy:
DO_NOT_RETRY

Recommended Action:
FIX_MODEL
```

因为：

```text
同一个错误模型名
重试 100 次
还是不存在
```

---

# 二十一、A 组禁止 Retry

不要写：

```java
for (int i = 0; i < 3; i++) {
    callWrongModel();
}
```

这没有学习价值。

应该：

```text
404 model_not_found
↓
Fail Fast
```

---

# 二十二、实验 B：429 Rate Limit

B 不真实触发限流。

使用：

```text
本地 HttpServer
```

第一次固定返回：

```text
HTTP 429
```

模拟 Error Body：

```json
{
  "error": {
    "code": "rate_limit_exceeded",
    "message": "Too many requests"
  },
  "request_id": "local-429-demo"
}
```

---

# 二十三、为什么 429 要模拟？

因为真正想验证的是：

```text
代码收到 429 后怎么决策
```

而不是：

```text
怎么把 Provider 打到限流
```

---

# 二十四、B 组分类

```text
Layer:
HTTP / PROVIDER

HTTP Status:
429

Retry Policy:
RETRY

Recommended Action:
BACKOFF_RETRY
```

---

# 二十五、B 组建议真正 Retry 一次

这是 Node 020 的核心。

模拟：

```text
Attempt 1
→ 429

Backoff

Attempt 2
→ 200
```

这样你能真正看到：

```text
Retry 成功
```

---

# 二十六、本地 Server 第二次为什么改成 200？

因为我们模拟的是：

```text
短期限流
↓
稍后恢复
```

而不是：

```text
永久额度不足
```

---

# 二十七、Backoff 不要搞复杂

当前只需要：

```text
等待 200～500 ms
```

例如：

```java
Thread.sleep(300);
```

重点是：

```text
Retry 前有等待
```

而不是具体等多久。

---

# 二十八、B 组建议输出

```text
Attempt: 1
HTTP Status: 429
Retry Policy: RETRY
Action: BACKOFF_RETRY

Backoff: 300 ms

Attempt: 2
HTTP Status: 200
Result: SUCCESS
```

---

# 二十九、实验 C：503 Service Unavailable

同样使用：

```text
本地 HttpServer
```

模拟：

```text
Attempt 1
→ 503

Backoff

Attempt 2
→ 200
```

Error Body 例如：

```json
{
  "error": {
    "code": "service_unavailable",
    "message": "Model service is temporarily unavailable"
  },
  "request_id": "local-503-demo"
}
```

---

# 三十、C 组分类

```text
Layer:
HTTP / PROVIDER

HTTP Status:
503

Retry Policy:
RETRY

Recommended Action:
BACKOFF_RETRY
```

---

# 三十一、503 和 429 的共同点

它们都可能属于：

```text
Transient Error
```

也就是：

```text
现在失败
≠
稍后一定还失败
```

所以都有：

```text
有限 Retry 的价值
```

---

# 三十二、503 和 429 的区别

429 更偏：

```text
限流 / 配额
```

503 更偏：

```text
Provider 暂时不可用 / 过载
```

原因不同，

但本实验中：

```text
Retry 决策可以相同
```

---

# 三十三、为什么 C 也只 Retry 一次？

因为这是学习实验。

只需要观察：

```text
503
↓
RETRY
↓
Backoff
↓
第二次成功
```

不需要：

```text
3 次、5 次、10 次
```

---

# 三十四、实验 D：Timeout

D 要验证：

> **Timeout 和 HTTP 429 / 503 不是同一层失败。**

因为 Timeout 很可能：

```text
根本没有拿到 HTTP Response
```

---

# 三十五、怎么稳定模拟 Timeout？

本地 Server：

```text
收到请求
↓
故意 sleep
↓
超过客户端 timeout
```

例如：

```text
Server 延迟：1500 ms
Client Request Timeout：300 ms
```

---

# 三十六、D 组预期

Java 应该出现类似：

```text
HttpTimeoutException
```

而不是：

```text
HTTP Status = 408
```

---

# 三十七、为什么不是 408？

408 是：

```text
服务器返回的 HTTP Status
```

我们这里模拟的是：

```text
客户端自己等不下去
```

所以客户端可能：

```text
根本拿不到完整 HTTP Response
```

直接抛：

```text
HttpTimeoutException
```

---

# 三十八、D 组分类

建议：

```text
Layer:
TRANSPORT

HTTP Status:
NONE

Java Exception:
HttpTimeoutException

Retry Policy:
CONDITIONAL

Recommended Action:
TIMEOUT_RETRY_OPTIONAL
```

---

# 三十九、为什么 Timeout 是 CONDITIONAL？

因为是否重试还取决于：

```text
请求是否幂等
Provider 是否可能已经执行
剩余 Deadline
额外成本
是否存在副作用
```

所以不能简单写：

```text
Timeout = RETRY
```

---

# 四十、Timeout 本实验不自动 Retry

我建议：

```text
只做分类
```

不要自动发第二次。

否则很容易形成错误直觉：

```text
Timeout
→ 必须重试
```

---

# 四十一、实验 E：Context Too Long

不要真的构造几十万 Token。

本地 Server 固定返回：

```text
HTTP 400
```

模拟：

```json
{
  "error": {
    "code": "context_length_exceeded",
    "message": "The request exceeds the model context window"
  },
  "request_id": "local-context-too-long"
}
```

---

# 四十二、为什么 E 是 400？

因为从 Provider 角度：

```text
请求不符合模型接受范围
```

所以它通常属于：

```text
Invalid Request
```

这一大类。

---

# 四十三、为什么不能只分类成普通 400？

因为：

```text
普通参数错误
```

和：

```text
Context Too Long
```

下一步动作不一样。

普通参数错误：

```text
FIX_REQUEST
```

Context Too Long：

```text
REDUCE_CONTEXT
```

---

# 四十四、E 组分类

```text
Layer:
HTTP / PROVIDER

HTTP Status:
400

Provider Code:
context_length_exceeded

Retry Policy:
DO_NOT_RETRY

Recommended Action:
REDUCE_CONTEXT
```

---

# 四十五、为什么原样 Retry 没意义？

因为：

```text
Context 太长
```

不是暂时性状态。

原样请求：

```text
第一次 120K
第二次还是 120K
第三次还是 120K
```

不会突然成功。

---

# 四十六、REDUCE_CONTEXT 代表什么？

这里只打印建议：

```text
缩短 History
减少 RAG
压缩 Context
删除 Noise
换更大 Context Window 模型
拆分任务
```

当前实验不真正实现压缩。

---

# 四十七、本地 HTTP Server 怎么实现？

直接使用 JDK 自带：

```java
com.sun.net.httpserver.HttpServer
```

就够。

不要新增：

```text
WireMock
MockServer
Spring Boot
OkHttp MockWebServer
```

---

# 四十八、为什么用 JDK 自带 HttpServer？

因为当前只需要：

```text
返回 429
返回 503
返回 400
故意延迟响应
```

JDK 已经足够。

这样可以：

```text
少依赖
少配置
更容易看懂
```

---

# 四十九、本地 Server 建议只绑定 127.0.0.1

例如：

```java
HttpServer.create(
        new InetSocketAddress("127.0.0.1", 0),
        0
);
```

不要监听：

```text
0.0.0.0
```

因为这只是本地实验。

---

# 五十、为什么端口写 0？

端口：

```text
0
```

表示：

```text
让操作系统自动选择一个可用端口
```

然后：

```java
server.getAddress().getPort()
```

读取实际端口。

这样避免：

```text
8080 被占用
```

之类的问题。

---

# 五十一、429 / 503 怎么记录请求次数？

可以用：

```java
AtomicInteger
```

例如：

```java
AtomicInteger count = new AtomicInteger();
```

第一次：

```text
429 / 503
```

第二次：

```text
200
```

---

# 五十二、建议统一 classifyHttpError

这是本实验真正的核心方法。

例如：

```java
private static ErrorDecision classifyHttpError(
        int statusCode,
        String body
)
```

职责：

```text
解析 Provider Error Body
↓
识别 status / code
↓
判断 RetryPolicy
↓
返回 Recommended Action
```

---

# 五十三、建议分类规则

可以保持很简单：

```text
如果 providerCode == context_length_exceeded
→ DO_NOT_RETRY
→ REDUCE_CONTEXT

否则如果 status == 429
→ RETRY
→ BACKOFF_RETRY

否则如果 status == 500 / 502 / 503 / 504
→ RETRY
→ BACKOFF_RETRY

否则如果 status == 400 / 401 / 403 / 404
→ DO_NOT_RETRY

否则
→ DO_NOT_RETRY
```

---

# 五十四、为什么 Context Too Long 要优先判断？

因为它虽然也是：

```text
400
```

但我们已经知道：

```text
更准确的动作是 REDUCE_CONTEXT
```

如果一上来只写：

```java
if (status == 400) {
    return FIX_REQUEST;
}
```

就丢掉了更有价值的信息。

---

# 五十五、Provider Code 可能不存在

真实 Provider 和本地模拟：

```text
字段结构可能不同
```

所以：

```text
error.code
```

要允许：

```text
null
```

不要因为 Provider 没返回 code：

```text
错误处理自己又崩掉
```

---

# 五十六、Provider Message 也要保留

即使：

```text
code
```

存在，

`message` 仍然有排障价值。

当前不需要：

```text
复杂解析 message
```

但应该保留。

---

# 五十七、真实 404 怎么请求？

复用：

```text
DASHSCOPE_API_KEY
DASHSCOPE_BASE_URL
```

只把：

```text
model
```

故意改成不存在的值。

不要：

```text
修改真实 API Key
```

---

# 五十八、为什么不建议故意改 API Key？

因为容易：

```text
污染 IDEA Run Configuration
```

而且我们已经能用：

```text
错误模型
```

安全验证 Non-retryable HTTP Error。

---

# 五十九、A 组要打印 Raw Error Body

建议：

```text
HTTP Status: 404
Raw Error Body:
...
```

然后再输出：

```text
Classification
```

这样能看到：

```text
真实 Provider Error
↓
Java 分类结果
```

---

# 六十、本地模拟必须明确标记

输出：

```text
Mode: LOCAL SIMULATION
```

这样以后不会误以为：

```text
本地构造的 error.code
```

就是百炼官方真实格式。

---

# 六十一、429 建议输出

```text
=== RATE_LIMIT ===
Mode: LOCAL SIMULATION

Attempt: 1
HTTP Status: 429
Provider Code: rate_limit_exceeded
Retry Policy: RETRY
Recommended Action: BACKOFF_RETRY

Backoff: 300 ms

Attempt: 2
HTTP Status: 200
Result: SUCCESS
```

---

# 六十二、503 建议输出

```text
=== SERVER_ERROR ===
Mode: LOCAL SIMULATION

Attempt: 1
HTTP Status: 503
Provider Code: service_unavailable
Retry Policy: RETRY
Recommended Action: BACKOFF_RETRY

Backoff: 300 ms

Attempt: 2
HTTP Status: 200
Result: SUCCESS
```

---

# 六十三、Timeout 建议输出

```text
=== TIMEOUT ===
Mode: LOCAL SIMULATION

HTTP Status: NONE
Java Exception: HttpTimeoutException
Retry Policy: CONDITIONAL
Recommended Action: TIMEOUT_RETRY_OPTIONAL
```

---

# 六十四、Context Too Long 建议输出

```text
=== CONTEXT_TOO_LONG ===
Mode: LOCAL SIMULATION

HTTP Status: 400
Provider Code: context_length_exceeded
Retry Policy: DO_NOT_RETRY
Recommended Action: REDUCE_CONTEXT
```

---

# 六十五、Retry 方法怎么写？

不要复杂。

例如：

```java
private static void executeWithOneRetry(...)
```

流程：

```text
Attempt 1
↓
成功？
├── 是 → 结束
└── 否
    ↓
   classify
    ↓
   RetryPolicy == RETRY？
   ├── 是
   │   ↓
   │  Backoff
   │   ↓
   │  Attempt 2
   │
   └── 否
       ↓
      结束
```

---

# 六十六、为什么最多 Retry 一次？

因为实验只想看懂：

```text
分类
Backoff
Retry
```

不是建立生产级策略。

一次已经足够。

---

# 六十七、Retry 时要打印 Attempt

例如：

```text
Attempt: 1
Attempt: 2
```

这样你能真正确认：

```text
有没有发生 Retry
```

---

# 六十八、Non-retryable 必须证明没有第二次请求

例如 404：

```text
Attempt 1
→ 404
→ DO_NOT_RETRY
→ END
```

不能：

```text
文档说不重试
代码却偷偷又发一次
```

---

# 六十九、Context Too Long 也只能一次

```text
Attempt 1
→ 400
→ context_length_exceeded
→ DO_NOT_RETRY
→ REDUCE_CONTEXT
```

结束。

---

# 七十、错误日志应该打印什么？

可以打印：

```text
Scenario
Attempt
HTTP Status
Provider Code
Provider Message
Request ID
Retry Policy
Recommended Action
Java Exception
```

不要打印：

```text
API Key
Authorization Header
```

---

# 七十一、真实 Provider Error Body 和预期不同怎么办？

很正常。

真实 Provider 可能：

```text
字段不同
code 不同
message 不同
request_id 位置不同
```

实验记录应该：

```text
以真实响应为准
```

不要为了和笔记一样：

```text
硬改结果
```

---

# 七十二、本地模拟格式为什么可以简单？

因为目标是：

```text
验证 Java Error Handling
```

不是：

```text
复刻百炼完整错误协议
```

只要能稳定提供：

```text
status
code
message
```

就够。

---

# 七十三、执行顺序

建议：

```text
1. --invalid-model
2. --rate-limit
3. --server-error
4. --timeout
5. --context-too-long
```

---

# 七十四、第一步：Invalid Model

运行：

```text
--invalid-model
```

重点记录：

```text
HTTP Status
Raw Error Body
Provider Code
Provider Message
Request ID
Retry Policy
Action
```

重点确认：

```text
404 没有因为状态码自动进入 IOException
```

---

# 七十五、第二步：Rate Limit

运行：

```text
--rate-limit
```

确认：

```text
Attempt 1：429
↓
RETRY
↓
Backoff
↓
Attempt 2：200
```

---

# 七十六、第三步：Server Error

运行：

```text
--server-error
```

确认：

```text
Attempt 1：503
↓
RETRY
↓
Backoff
↓
Attempt 2：200
```

---

# 七十七、第四步：Timeout

运行：

```text
--timeout
```

确认：

```text
没有 HTTP Status
↓
HttpTimeoutException
↓
CONDITIONAL
```

---

# 七十八、第五步：Context Too Long

运行：

```text
--context-too-long
```

确认：

```text
HTTP 400
↓
进一步读取 Provider Code
↓
context_length_exceeded
↓
DO_NOT_RETRY
↓
REDUCE_CONTEXT
```

---

# 七十九、真实结果记录模板

## A - Invalid Model

```text
运行日期：2026-11-06
真实 / 模拟：真实
HTTP Status：404
Provider Code：model_not_found
Provider Message：Model not exist.
Request ID：186667af-a4d5-99d6-93eb-212f2c38fdad
Java Exception：未进入 IOException 分支，正常读取 HTTP 响应
Retry Policy：DO_NOT_RETRY
Recommended Action：FIX_MODEL
是否发生 Retry：否，仅打印 Attempt: 1
```

## B - Rate Limit

```text
运行日期：2026-11-06
真实 / 模拟：本地模拟
Attempt 1 Status：429
Attempt 2 Status：200
Provider Code：rate_limit_exceeded
Retry Policy：RETRY
Backoff：300 ms
最终结果：SUCCESS，本地服务收到 2 次请求
```

## C - Server Error

```text
运行日期：2026-11-06
真实 / 模拟：本地模拟
Attempt 1 Status：503
Attempt 2 Status：200
Provider Code：service_unavailable
Retry Policy：RETRY
Backoff：300 ms
最终结果：SUCCESS，本地服务收到 2 次请求
```

## D - Timeout

```text
运行日期：2026-11-06
真实 / 模拟：本地模拟
HTTP Status：NONE
Java Exception：HttpTimeoutException
Retry Policy：CONDITIONAL
Recommended Action：TIMEOUT_RETRY_OPTIONAL
是否自动 Retry：否，本地服务收到 1 次请求
```

## E - Context Too Long

```text
运行日期：2026-11-06
真实 / 模拟：本地模拟
HTTP Status：400
Provider Code：context_length_exceeded
Provider Message：The request exceeds the model context window
Request ID：local-context-too-long
Retry Policy：DO_NOT_RETRY
Recommended Action：REDUCE_CONTEXT
是否发生 Retry：否，本地服务收到 1 次请求
最终结果：FAILED，符合实验预期
```

---

# 八十、实验后必须能回答的问题

1. 为什么 404 没有进入 `catch(IOException)`？
2. 为什么 429 和 404 都是 HTTP Error，但 Retry 决策不同？
3. 为什么 503 通常有 Retry 价值？
4. 为什么 Timeout 没有 HTTP Status？
5. 为什么 Timeout 是 `CONDITIONAL`，而不是直接 `RETRY`？
6. 为什么 Context Too Long 虽然是 400，却应该进一步映射成 `REDUCE_CONTEXT`？
7. 为什么 Retry 不能写成无限循环？

---

# 八十一、代码注释重点

继续写中文注释。

重点解释：

```text
为什么这么分类
为什么 Retry
为什么不 Retry
```

例如：

```java
// 429 虽然属于 4xx，但短期限流通常是暂时性错误。
// 因此这里允许一次退避重试，不能简单按“4xx 全不重试”处理。
```

这种注释最有学习价值。

---

# 八十二、不要写低价值注释

少写：

```java
// 创建 HttpClient
HttpClient client = ...
```

多写：

```java
// 本场景故意让服务端第一次返回 503、第二次返回 200，
// 用于验证“暂时性 Provider 错误 → Backoff → Limited Retry”。
```

---

# 八十三、Codex 实现边界

如果交给 Codex，实现范围建议明确为：

```text
1. 先阅读当前 agent-learning-lab 代码风格
2. 新增 LlmApiErrorDemo.java
3. Java 21
4. HttpClient + FastJSON2
5. JDK HttpServer 做本地模拟
6. 支持五种运行模式
7. 定义 Scenario
8. 定义 RetryPolicy 三态
9. 有统一 classifyHttpError
10. 429 / 503 最多 Retry 一次
11. Timeout 不自动 Retry
12. 打印结构化错误决策
13. 中文注释
14. Maven compile
```

---

# 八十四、明确禁止 Codex 做什么

```text
不要加 Spring Boot
不要加 Spring Retry
不要加 Resilience4j
不要加 Circuit Breaker
不要加 Fallback
不要加多 Provider
不要创建复杂异常继承树
不要改旧 Demo
不要改真实 API Key
不要为了 429 疯狂请求 Provider
不要伪造真实 404 结果
不要提前把 Node 020 标记完成
```

---

# 八十五、Maven 编译

至少执行：

```bash
mvn -q -DskipTests compile
```

确认：

```text
Java 21 编译通过
```

但：

> **编译通过不等于实验完成。**

---

# 八十六、哪些需要你自己运行？

建议五种模式都自己在 IDEA 运行：

```text
--invalid-model
--rate-limit
--server-error
--timeout
--context-too-long
```

其中：

```text
--invalid-model
```

是真实 Provider。

其余：

```text
本地模拟
```

---

# 八十七、本地模拟为什么也算实验？

因为验证的是：

```text
Java Error Handling Control Flow
```

而不是：

```text
Provider 今天是否恰好真的出 503
```

只要：

```text
真实 HttpClient
真实本地 HTTP Response
真实 Timeout
真实 Retry 控制流
```

就有学习价值。

---

# 八十八、不要把 Thread.sleep 写太久

实验：

```text
200～500 ms
```

足够。

不要为了模拟指数退避真的：

```text
1 秒
2 秒
4 秒
```

当前重点是：

```text
有 Backoff
```

---

# 八十九、不要写通用泛型 Retry Framework

不要一上来做：

```java
<T> T retry(
    Supplier<T> action,
    Predicate<Throwable> retryable,
    BackoffStrategy strategy,
    ...
)
```

这明显过度设计。

当前一个：

```text
最多 Retry 一次
```

的小方法就够。

---

# 九十、当前允许一点重复代码

这是学习实验。

比起：

```text
抽象得特别漂亮
```

更重要的是：

```text
每一步你能看懂
```

真正值得统一的只有：

```text
classifyHttpError()
printDecision()
```

因为这两个正好对应 Node 020 核心。

---

# 九十一、不要提前建立十几个异常类

现在不要创建：

```text
BaseLlmException
RetryableLlmException
NonRetryableLlmException
LlmRateLimitException
LlmContextException
...
```

Node 021 做真正 Spring Boot API 时，

再根据真实需求决定。

---

# 九十二、为什么不使用 Spring Retry / Resilience4j？

因为框架会把：

```text
Retry 控制流
```

隐藏起来。

当前我们就是要亲眼看懂：

```text
为什么重试
什么时候重试
什么时候不重试
```

先手写最小机制更适合学习。

---

# 九十三、Node 020 真正最重要的学习结果

不是：

```text
“我会写 Retry”
```

而是：

> **我知道什么时候根本不应该 Retry。**

这比会写一个重试循环更重要。

---

# 九十四、这和普通 Java 后端有什么关系？

这节几乎就是：

```text
Backend Reliability
```

你会真正碰到：

```text
HTTP Status
Transport Exception
Timeout
Backoff
Retry
Fail Fast
错误分类
日志
```

以后不只 LLM API：

```text
广告 API
支付 API
云服务 API
第三方平台 API
```

都能用到。

---

# 九十五、LLM API 又有什么特殊性？

要额外记住：

```text
Retry 会再次消耗 Token
Retry 后答案可能变化
Streaming 可能已经输出部分内容
Context Too Long 是 LLM 高频问题
Tool Agent 以后可能存在副作用
```

所以：

```text
LLM Retry
```

不能完全照搬普通 GET 请求。

---

# 九十六、实验完成标准

- [x] 新增 `LlmApiErrorDemo.java`
- [x] Java 21 编译通过
- [x] 支持五种运行模式
- [ ] 定义 `Scenario`
- [ ] 定义 `RetryPolicy`：RETRY / DO_NOT_RETRY / CONDITIONAL
- [x] 能解析 Provider Error Body
- [x] 能读取 code / message / request_id
- [ ] 有统一 `classifyHttpError`
- [x] Invalid Model 真实调用完成
- [x] 真实观察 404 返回 `HttpResponse`，而不是因为状态码自动抛异常
- [x] 429 本地模拟完成
- [x] 429 第一次失败、Backoff 后第二次成功
- [x] 503 本地模拟完成
- [x] 503 第一次失败、Backoff 后第二次成功
- [x] Timeout 本地模拟完成
- [x] 观察到 `HttpTimeoutException`
- [x] Timeout 没有伪造 HTTP Status
- [x] Context Too Long 本地模拟完成
- [x] Context Too Long 被识别为 `DO_NOT_RETRY`
- [x] Context Too Long 动作为 `REDUCE_CONTEXT`
- [x] Non-retryable 场景没有第二次请求
- [x] 日志没有打印 API Key
- [x] 没有引入 Spring Boot / Resilience4j / Circuit Breaker

当前 Demo 用程序实参选择场景，在发送分支决定是否重试，并统一打印 HTTP 错误细节。上面三项是笔记建议的代码组织形式，本次实测覆盖的是五种行为。

---

# 九十七、本实验停止点

完成：

```text
Failure
↓
Transport or HTTP?
↓
Status / Error Body / Exception
↓
Classification
↓
RetryPolicy
↓
Action
↓
有限 Retry 或 Fail Fast
```

就停止。

不要继续扩展：

```text
完整指数退避算法
Jitter
Circuit Breaker
Fallback
多 Provider
统一错误网关
生产 Metrics
Tracing
复杂幂等体系
```

---

# 九十八、Node 020 完成后的状态

如果：

```text
020-1 理论已读完
+
020-2 五组实验完成
+
你能解释不同 Retry 决策
```

Node 020 就可以正式收尾。

然后进入：

```text
Node 021：第一阶段项目
Smart Text Analyzer
```

---

# 九十九、Node 021 会发生什么？

到那时第一次真正组合：

```text
Spring Boot
+
LLM API
+
Structured Output
+
DTO
+
Context
+
Exception Handling
+
最小 Evaluation
```

Node 020 的错误分类就会开始服务于：

```text
真正的 HTTP 接口
```

而不是只存在于 Demo 中。

---

# 本篇一句话总结

> **020-2 的目标不是“制造五种错误”，而是让 Java 程序第一次真正学会：有些错误应该退避重试，有些错误应该立即失败，有些错误需要修改 Context，而 Timeout 必须结合业务语义才能决定是否重试。**

最终实验模型：

```text
A：404 Invalid Model
↓
DO_NOT_RETRY
↓
FIX_MODEL

B：429 Rate Limit
↓
RETRY
↓
BACKOFF
↓
SUCCESS

C：503 Service Unavailable
↓
RETRY
↓
BACKOFF
↓
SUCCESS

D：Timeout
↓
TRANSPORT ERROR
↓
CONDITIONAL

E：Context Too Long
↓
DO_NOT_RETRY
↓
REDUCE_CONTEXT
```

真正要形成的工程直觉：

> **Retry is a policy decision after classification, not an automatic response to failure.**

也就是：

> **先判断错误，再决定重试；不是先重试，再想为什么失败。**

**Node 020-2：把 LLM API 失败从“异常情况”变成“可以被程序明确处理的工程状态”。**
