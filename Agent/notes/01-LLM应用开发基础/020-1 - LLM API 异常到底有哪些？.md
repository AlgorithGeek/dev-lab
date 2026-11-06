# 020-1 - LLM API 异常到底有哪些？

> Node 020：LLM API 异常\
> Phase 1：LLM 应用开发基础\
> 当前环境：Java 21 + HttpClient + FastJSON2 + 阿里云百炼 Model Studio + OpenAI-compatible API

---

# 一、先说结论

到 Node 019 为止，我们研究的大多数问题都是：

```text
请求成功以后怎么办？
```

例如：

```text
怎么构造 Message
怎么写 Prompt
怎么做 Structured Output
怎么转 DTO
怎么 Streaming
怎么管理 Context
```

Node 020 开始研究另一个真正的后端工程问题：

> **请求失败以后怎么办？**

真实的 LLM API 调用不可能永远都是：

```text
HTTP 200
+
正常 JSON
+
正常模型回答
```

它可能出现：

```text
请求参数错误
API Key 错误
没有模型权限
模型不存在
限流
额度不足
Provider 过载
服务端内部错误
连接失败
请求超时
Context Too Long
响应 JSON 异常
返回 200 但业务结构不符合预期
Streaming 中途断开
```

真正的工程目标不是：

```text
“遇到异常就 Retry”
```

而是：

```text
发生了什么？
↓
这是哪一类错误？
↓
它是否值得重试？
↓
如果不该重试，应该修什么？
↓
应该记录哪些排障信息？
↓
应该怎样把失败交给上层？
```

---

# 二、Node 020 最重要的一句话

> **不是所有失败都应该重试。**

例如：

```text
429 Rate Limit
→ 等一会再试，可能成功

503 Provider Overloaded
→ 稍后重试，可能成功

网络瞬时失败
→ 重试可能成功
```

但是：

```text
401 API Key 错误
→ 重试 100 次还是错误

404 模型名写错
→ 重试 100 次还是不存在

400 参数格式错误
→ 请求不改，重试没有意义

Context Too Long
→ Context 不缩短，重试也不会突然变短
```

所以 Node 020 的核心不是 Retry，而是：

```text
Error Classification
错误分类
```

---

# 三、一次 LLM API 调用到底有哪些失败点？

一次调用可以粗略看成：

```text
Java 代码
↓
构造请求
↓
建立连接
↓
发送 HTTP 请求
↓
Provider 网关
↓
鉴权 / 限流 / 参数校验
↓
模型服务
↓
生成结果
↓
Provider 封装 HTTP Response
↓
网络返回
↓
Java 读取 Response
↓
JSON 解析
↓
业务结构校验
```

任何一层都可能失败。

所以：

```text
LLM API 异常
```

绝不等于：

```text
HTTP 500
```

---

# 四、最实用的异常分类

先把异常分成七类：

```text
1. 本地配置 / 请求构造错误
2. 网络与 Transport 异常
3. HTTP 状态码错误
4. Provider 业务错误
5. Context / 模型能力约束错误
6. HTTP 200 之后的响应与业务错误
7. Streaming 特有异常
```

后面所有判断，都可以从这个框架展开。

---

# 五、第一类：本地配置或请求构造错误

这类错误甚至可能：

```text
请求还没真正发出去
```

例如：

```text
API Key 环境变量不存在
Base URL 为空
URI 格式非法
JSON 构造失败
模型名为空
必填字段缺失
程序参数错误
```

前面的 Demo 已经有类似代码：

```java
String apiKey = System.getenv("DASHSCOPE_API_KEY");

if (apiKey == null || apiKey.isBlank()) {
    throw new IllegalStateException(...);
}
```

这就是：

```text
Local Validation
本地校验
```

---

# 六、为什么这类错误不应该 Retry？

假设：

```text
DASHSCOPE_API_KEY = null
```

你执行：

```text
Retry 1
Retry 2
Retry 3
```

结果不会改变。

因为问题不是：

```text
Provider 暂时失败
```

而是：

```text
程序配置就是错的
```

正确动作应该是：

```text
Fail Fast
快速失败
```

---

# 七、Fail Fast 是什么？

Fail Fast：

> **如果已经能确定请求不可能成功，就尽早失败，不要浪费时间和请求次数。**

例如：

```text
API Key 缺失
模型名为空
Base URL 非法
请求对象缺少必要字段
```

最好在：

```text
真正发 HTTP 请求之前
```

就发现。

---

# 八、第二类：网络 / Transport 异常

这类错误的特点是：

```text
你可能根本没有拿到完整 HTTP Response
```

例如：

```text
DNS 解析失败
TCP 连接失败
TLS 失败
连接超时
请求超时
Connection Reset
网络中断
```

Java 中可能表现为：

```text
IOException
HttpTimeoutException
ConnectException
InterruptedException
```

等。

---

# 九、一个非常重要的 Java 区别

使用 Java `HttpClient` 时：

```text
HTTP 400
HTTP 429
HTTP 500
```

通常不会因为状态码本身自动变成：

```text
Java Exception
```

你仍然可能正常拿到：

```java
HttpResponse<String> response
```

然后：

```java
response.statusCode()
```

得到：

```text
400
429
500
```

---

# 十、那什么更可能抛 Java Exception？

例如：

```text
根本连不上
连接超时
请求整体超时
网络中断
线程被中断
```

这类问题更接近：

```text
Transport / Client Exception
```

所以：

> **HTTP Error 和 Java Exception 不是同一回事。**

---

# 十一、用人话理解这两类失败

第一种：

```text
请求成功到达服务器
↓
服务器明确告诉你：
“你的请求有问题”
↓
HTTP 400
```

此时：

```text
HTTP 通信本身其实完成了
```

第二种：

```text
请求过程中网络断了
↓
完整 HTTP Response 都没拿到
```

此时更可能：

```text
抛 IOException / Timeout
```

---

# 十二、第三类：HTTP 状态码错误

大体可以先分：

```text
2xx
→ HTTP 层成功

4xx
→ 客户端请求、认证、权限、额度或限流等问题

5xx
→ 服务端 / Provider 问题
```

但注意：

> **不能只看“4xx / 5xx”两个大类就决定是否 Retry。**

因为：

```text
429
```

虽然属于 4xx，

却经常：

```text
值得等待以后再试
```

---

# 十三、400 - Bad Request / Invalid Request

400 通常意味着：

```text
请求本身不符合接口要求
```

例如：

```text
JSON 格式错误
字段缺失
参数类型错误
参数范围非法
模型不支持某参数
Message 结构错误
response_format 不合法
输入长度超过限制
```

---

# 十四、400 应该 Retry 吗？

通常：

```text
不应该原样 Retry
```

因为如果：

```text
什么都不修改
```

再次发送同一个请求：

```text
大概率还是 400
```

正确动作：

```text
读取错误详情
↓
修请求
↓
再发送
```

---

# 十五、为什么 400 一定要看 Error Body？

只记录：

```text
HTTP Status = 400
```

信息太少。

真正原因往往在：

```text
error.code
error.type
error.message
param
request_id
```

不同 Provider 字段会不同。

可能类似：

```json
{
  "error": {
    "message": "Invalid parameter ...",
    "type": "invalid_request_error",
    "code": "invalid_parameter"
  },
  "request_id": "..."
}
```

---

# 十六、401 - Unauthorized

401 一般表示：

```text
认证失败
```

例如：

```text
API Key 没带
API Key 错误
API Key 已失效
Authorization Header 格式错误
Base URL 与 Key 类型不匹配
```

---

# 十七、401 应该 Retry 吗？

通常：

```text
不应该
```

错误 Key 不会因为：

```text
等 2 秒
```

突然变成正确 Key。

正确动作：

```text
检查配置
检查 Key
检查 Header
检查地域 / 套餐 / Base URL
```

---

# 十八、401 其实说明了什么？

它通常说明：

```text
Provider 已经收到请求
并成功判断认证失败
```

所以问题不是：

```text
“网络完全不通”
```

而是：

```text
身份认证
```

---

# 十九、403 - Forbidden

403 通常表示：

```text
身份可能已经识别
但没有权限执行这个操作
```

例如：

```text
没有模型权限
服务未开通
当前 Workspace 无权限
账号权限不足
区域或产品限制
```

通常也不是：

```text
等两秒再试
```

能解决的。

---

# 二十、404 - Not Found

LLM API 中常见：

```text
模型不存在
模型名写错
资源 ID 不存在
接口路径错误
```

我们之前 Node 008 已经真实见过：

```text
model_not_found
```

这就是典型 404 类问题。

---

# 二十一、404 应该 Retry 吗？

如果原因是：

```text
模型名写错
```

就不应该原样重试。

正确动作：

```text
修模型 ID
```

---

# 二十二、429 - Too Many Requests

429 非常重要。

它可能意味着：

```text
请求频率过高
并发过高
Token Throughput 超限
配额耗尽
账户额度 / 余额问题
```

所以：

> **429 不能只看到状态码就无脑 Retry。**

---

# 二十三、两种完全不同的 429

第一种：

```text
Rate Limit
```

例如：

```text
一分钟请求太多
瞬时并发太高
```

这种：

```text
等一会
降低速率
再试
```

可能成功。

第二种：

```text
Quota Exhausted
额度耗尽
```

这种：

```text
1 秒
2 秒
4 秒
```

机械退避可能没有意义。

可能需要：

```text
充值
等待额度重置
调整套餐
```

---

# 二十四、所以 429 不能只判断 statusCode

更合理的是结合：

```text
HTTP 429
+
Provider error.code
+
error.message
+
Retry-After（如果有）
```

共同决定：

```text
是否重试
多久以后重试
```

---

# 二十五、500 - Internal Server Error

500 一般表示：

```text
Provider 服务内部处理失败
```

这和：

```text
你参数写错
```

不是同类问题。

因为问题更可能发生在：

```text
服务端
```

---

# 二十六、500 应该 Retry 吗？

通常：

```text
可以有限重试
```

因为可能只是：

```text
瞬时内部错误
```

但不能：

```text
无限重试
```

Provider 持续异常时，无限重试只会：

```text
制造更多流量
增加成本
拖慢调用链
```

---

# 二十七、502 / 503 / 504

可以先建立直觉：

```text
502 Bad Gateway
→ 网关 / 上游异常

503 Service Unavailable
→ 服务暂时不可用 / 过载

504 Gateway Timeout
→ 网关等待上游超时
```

这些通常比：

```text
400 / 401 / 404
```

更有重试价值。

---

# 二十八、Transient Error 和 Deterministic Error

这是 Node 020 必须建立的概念。

## Transient Error

暂时性错误：

```text
稍后再试可能恢复
```

例如：

```text
临时网络错误
部分 429
500
502
503
504
```

## Deterministic Error

确定性错误：

```text
请求不修改就不会成功
```

例如：

```text
400 参数错误
401 Key 错误
403 无权限
404 模型不存在
Context Too Long
```

---

# 二十九、真正的 Retry 判断

不是：

```text
失败
→ Retry
```

而是：

```text
失败
↓
Transient？
├── 是
│   ↓
│  有限制地 Retry
│
└── 否
    ↓
   不重试
   修请求 / 修配置 / 返回错误
```

---

# 三十、第四类：Provider 业务错误

HTTP Status 只是：

```text
协议层
```

Provider 往往还会返回：

```text
error.code
error.type
error.message
request_id
```

例如两个请求都是：

```text
HTTP 429
```

但业务错误可能分别是：

```text
RATE_LIMITED
```

和：

```text
INSUFFICIENT_QUOTA
```

处理动作完全不同。

---

# 三十一、为什么要解析 Provider Error Body？

只看：

```text
statusCode
```

很多时候不够。

例如：

```text
429
```

到底是：

```text
短期限流
还是
额度耗尽
```

不读 body：

```text
无法正确决策
```

---

# 三十二、错误信息最好分开记录

以后更合理的是分别保留：

```text
HTTP Status
Provider Error Code
Provider Error Message
Request ID
Java Exception Type
```

而不是最后只剩一句：

```text
“AI 调用失败”
```

---

# 三十三、Request ID 为什么重要？

很多云 Provider 会给一次请求：

```text
request_id
```

它可以理解为：

```text
Provider 端这一次请求的追踪 ID
```

遇到服务端问题时，

它通常是：

```text
排障和联系 Provider 支持的重要线索
```

---

# 三十四、日志里至少应该考虑什么？

例如：

```text
HTTP Status
Provider Error Code
Provider Error Message
Request ID
Model
耗时
是否 Retry
第几次 Retry
```

但是：

> **绝对不要把 API Key 打进普通日志。**

---

# 三十五、Prompt 要不要完整打进生产日志？

学习 Demo 中：

```text
为了调试可以打印
```

生产环境要谨慎。

因为 Prompt 可能包含：

```text
用户信息
业务数据
RAG 文档
Tool Result
代码
内部资料
```

未来需要考虑：

```text
脱敏
截断
采样
访问权限
```

当前只建立意识。

---

# 三十六、第五类：Context Too Long

这是 LLM API 特别典型的一类错误。

例如模型支持：

```text
100K Context
```

但本次请求实际需要：

```text
110K
```

Provider 可能返回：

```text
400
```

并在错误信息中说明：

```text
input too long
context length exceeded
range of input length ...
```

---

# 三十七、Context Too Long 是 400，但业务意义更具体

从 HTTP 角度：

```text
400
```

从 LLM 应用角度：

```text
Context Too Long
```

所以真实工程不能只分类成：

```text
4xx
```

还要继续识别：

```text
具体业务原因
```

---

# 三十八、Context Too Long 应该 Retry 吗？

如果请求：

```text
完全不变
```

那么：

```text
不应该
```

因为：

```text
110K Token
```

重试以后还是：

```text
110K Token
```

正确动作是：

```text
缩短 History
减少 RAG 文档
压缩 Context
删除 Noise
换更大 Context Window 的模型
拆分任务
```

---

# 三十九、它和 Node 019 直接连接

Node 019 学：

```text
Context Engineering
```

Node 020 这里开始看到：

```text
Context 管理失败
```

的直接工程后果：

```text
Context Too Long
```

所以 Context Engineering 不只影响：

```text
质量
成本
延迟
```

还可能影响：

```text
请求能不能成功
```

---

# 四十、输出预算也可能影响边界

别忘了：

```text
Context Window
```

和：

```text
Max Output
```

都有边界。

输入已经很长时，又要求很大的输出预算，

Provider 可能：

```text
拒绝请求
```

或模型：

```text
提前结束
```

具体行为取决于模型和 Provider。

---

# 四十一、第六类：HTTP 200 之后仍然可能失败

这是一个非常重要的概念：

> **HTTP 200 不等于业务成功。**

例如：

```text
HTTP 200
```

但：

```text
Response JSON 无法解析
choices 为空
message 不存在
content 为空
Structured Output 不是合法 JSON
DTO 字段缺失
枚举值非法
模型事实判断错误
```

这些都属于：

```text
应用层失败
```

---

# 四十二、一次调用其实有多个“成功层级”

可以这样理解：

```text
Level 1：
网络成功

Level 2：
HTTP 成功

Level 3：
Provider 响应结构成功

Level 4：
模型输出结构成功

Level 5：
业务校验成功

Level 6：
业务事实正确
```

只有：

```text
HTTP 200
```

最多说明：

```text
HTTP 层成功
```

不能代表后面全部成功。

---

# 四十三、Node 015 已经见过这个问题

Structured Output 实验中：

```text
HTTP 200
```

并且：

```text
JSON 结构正确
DTO 也成功
```

但模型仍可能：

```text
补充没有依据的行业平均水平
```

所以：

```text
Protocol Success
≠
Business Correctness
```

---

# 四十四、Response Parse Error

例如 Provider 返回结构和你的代码假设不一致。

你的代码认为：

```text
choices[0].message.content
```

一定存在，

但实际：

```text
choices = []
```

可能导致：

```text
JSONException
IllegalStateException
IndexOutOfBounds
```

---

# 四十五、Parse Error 应该 Retry 吗？

答案：

```text
要看原因
```

如果是：

```text
Provider 偶发异常响应
```

重试可能有价值。

但如果是：

```text
你的解析代码和真实协议一直不匹配
```

重试没有意义。

所以：

> **“解析失败”还不是最终错误分类。**

---

# 四十六、Structured Output 校验失败呢？

例如模型返回：

```json
{
  "riskLevel": "CRITICAL"
}
```

而业务只允许：

```text
LOW
MEDIUM
HIGH
```

此时：

```text
HTTP 200
JSON 合法
DTO 可转换
```

但：

```text
业务校验失败
```

这属于：

```text
Business Validation Failure
```

---

# 四十七、这种业务失败和 HTTP Retry 不是一回事

有时重新调用模型：

```text
可能生成一个合法值
```

但如果无限重试：

```text
成本会不断增加
```

更根本的方向可能是：

```text
Schema
Prompt
Validation
有限纠错
```

当前只要知道：

```text
模型输出纠错
```

和：

```text
HTTP Retry
```

不是完全同一个问题。

---

# 四十八、第七类：Streaming 特有异常

Streaming 比普通请求多一个难点：

```text
响应已经开始
然后中途失败
```

例如：

```text
已经输出 40%
↓
网络断开
```

这时不能简单理解成：

```text
“请求完全没发生”
```

因为用户可能已经看见：

```text
前 40%
```

---

# 四十九、Streaming 中途失败为什么更复杂？

普通非流式：

```text
失败
→ 没有完整答案
```

Streaming：

```text
可能已经拿到部分答案
```

如果直接 Retry：

```text
模型通常会从头重新生成
```

可能导致：

```text
重复文本
结果不同
重复计费
```

---

# 五十、Streaming Retry 通常不是断点续传

除非 Provider 特别支持，

否则：

```text
Retry
```

一般意味着：

```text
发起一个全新的模型请求
```

而不是：

```text
从第 357 个 Token 继续
```

---

# 五十一、Streaming 还有 Idle Timeout

普通 Request Timeout 常理解为：

```text
整个请求最多多久
```

Streaming 还会关心：

```text
多久没有任何新数据
```

例如：

```text
连接还开着
但 60 秒没有新 chunk
```

这就是：

```text
Read / Idle Timeout
```

概念。

Node 016 没有单独实现它，

这里先理解，不回头补。

---

# 五十二、Timeout 不是一个单一概念

至少可以区分：

```text
Connect Timeout
Request / Overall Timeout
Read / Idle Timeout
Business Deadline
```

---

# 五十三、Connect Timeout

Connect Timeout：

```text
建立连接最多等多久
```

例如：

```java
HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(15))
```

这里限制的是：

```text
建立连接阶段
```

---

# 五十四、Request Timeout

之前 Demo 中还有：

```java
HttpRequest.newBuilder(endpoint)
    .timeout(Duration.ofSeconds(120))
```

这是：

```text
请求级 Timeout
```

如果超过限制，

可能出现：

```text
HttpTimeoutException
```

---

# 五十五、Business Deadline

真实后端还可能规定：

```text
用户最多只能等 5 秒
```

即使 Provider 技术上允许：

```text
120 秒
```

你的业务仍然可能规定：

```text
超过 5 秒就算失败
```

这就是：

```text
业务 Deadline
```

它和 HTTP Timeout 不一定相同。

---

# 五十六、Timeout 应该 Retry 吗？

通常属于：

```text
Conditional Retry
```

因为：

```text
客户端超时
```

不一定意味着：

```text
Provider 一定没有执行
```

可能只是：

```text
结果没有及时回来
```

如果重试：

```text
可能又产生一次模型调用
```

导致：

```text
重复成本
```

---

# 五十七、纯生成和 Tool Agent 的 Retry 风险不同

如果只是：

```text
总结一段文本
```

重试主要风险：

```text
额外费用
结果不同
```

以后 Agent 如果会：

```text
发送邮件
创建订单
修改数据库
付款
创建工单
```

Retry 就可能导致：

```text
重复副作用
```

这会引出：

```text
Idempotency
幂等
```

---

# 五十八、什么是幂等？

简单理解：

> **同一个操作执行多次，是否会产生额外副作用？**

例如：

```text
查询天气
```

执行多次通常问题不大。

但：

```text
创建订单
```

执行两次可能：

```text
创建两个订单
```

所以 Retry 不只是：

```text
“技术上能不能再发”
```

还要问：

```text
“业务上能不能再执行”
```

---

# 五十九、LLM Retry 本身也不是免费的

即使只是模型生成：

```text
Retry
```

也可能再次：

```text
消耗 Input Token
消耗 Output Token
计费
占用并发
```

所以：

> **Retry 不是免费的。**

---

# 六十、Retry 的基本原则

可以先记：

> **只有当错误可能是暂时的，并且再次请求有合理成功概率时，才考虑 Retry。**

---

# 六十一、哪些通常不重试？

初学阶段可以先记：

```text
本地配置错误
400 普通参数错误
401 鉴权失败
403 权限错误
404 模型 / 资源不存在
Context Too Long
```

这些更应该：

```text
修问题
```

而不是：

```text
再试
```

---

# 六十二、哪些通常可以考虑重试？

例如：

```text
临时网络异常
429 短期限流
500
502
503
504
部分 Timeout
```

但都应该：

```text
有限次数
+
等待
+
有总上限
```

---

# 六十三、为什么不能立即疯狂 Retry？

假设 Provider：

```text
已经过载
```

你收到：

```text
503
```

然后立即：

```text
Retry
Retry
Retry
Retry
```

你其实是在：

```text
继续给已经过载的服务增加压力
```

所以需要：

```text
Backoff
```

---

# 六十四、什么是 Backoff？

Backoff：

```text
重试前等待一段时间
```

例如：

```text
第 1 次失败
→ 等 1 秒

第 2 次失败
→ 等 2 秒

第 3 次失败
→ 等 4 秒
```

这叫：

```text
Exponential Backoff
指数退避
```

---

# 六十五、为什么指数退避有效？

临时故障通常需要：

```text
恢复时间
```

不断立即重试：

```text
很可能继续撞失败
```

退避可以：

```text
降低请求压力
给服务恢复窗口
```

---

# 六十六、当前百炼文档中的实际思路

截至当前百炼官方文档，常见处理建议也明确区分：

```text
普通 4xx
→ 通常不重试

429 限流
→ 退避重试

5xx
→ 有限退避重试
```

OpenAI-compatible 千问接口当前文档列出的常见错误包括：

```text
400 Invalid Request
401 Invalid API Key
429 Rate Limit / Quota
500 Server Error
503 Engine Overloaded
```

这里最重要的不是死背阿里云字段，

而是理解真实 Provider 确实会：

```text
区分不同失败原因
```

---

# 六十七、Retry-After

某些 429 / 503 响应可能包含：

```text
Retry-After
```

它表示：

```text
服务端建议多久以后再试
```

如果 Provider 提供这个信息，

一般应该优先考虑，

而不是：

```text
完全自己猜等待时间
```

---

# 六十八、什么是 Jitter？

如果 1000 个客户端都严格：

```text
1 秒后 Retry
```

那么 1 秒以后：

```text
1000 个请求又一起冲上来
```

可能再次造成尖峰。

生产级重试常加入：

```text
Jitter
随机抖动
```

例如：

```text
不是严格 2.000 秒
而是在 1.7～2.3 秒附近
```

当前：

```text
理解即可
```

---

# 六十九、Retry 必须有最大次数

错误写法：

```java
while (true) {
    tryAgain();
}
```

Provider 如果长期故障：

```text
线程可能永远重试
```

所以要有：

```text
maxAttempts
```

例如：

```text
最多 3 次
```

达到上限：

```text
最终失败
```

---

# 七十、Retry 还要考虑总时间预算

即使：

```text
最多重试 3 次
```

如果每次都等：

```text
120 秒
```

整个接口仍然可能不可接受。

所以真实工程还会有：

```text
Retry Budget
Deadline
```

概念。

---

# 七十一、一个最基本的 Retry Decision Table

| 错误 | 默认动作 |
|---|---|
| 本地配置错误 | 不重试，修配置 |
| 400 Invalid Request | 不重试，修请求 |
| 401 | 不重试，修认证 |
| 403 | 不重试，修权限 |
| 404 | 不重试，修模型 / 资源 |
| 429 Rate Limit | 退避后有限重试 |
| 429 Quota Exhausted | 通常不做短时间机械重试 |
| Context Too Long | 缩短 Context / 换模型 |
| 500 | 有限重试 |
| 502 | 有限重试 |
| 503 | 有限重试 |
| 504 | 有限重试 |
| 临时网络异常 | 视情况有限重试 |
| Timeout | 根据请求语义、成本、状态决定 |
| Response Parse Error | 先判断协议问题还是临时异常 |
| Business Validation Failure | 不等同于 HTTP Retry |

---

# 七十二、为什么这个表不能当绝对标准？

因为不同 Provider：

```text
错误码
业务 code
Header
限流规则
超时语义
```

可能不同。

同样是：

```text
429
```

可能代表：

```text
RPM
TPM
并发
额度
余额
```

所以最终一定要：

```text
看当前 Provider 文档
+
看真实 Error Body
```

---

# 七十三、OpenAI-compatible 不等于错误行为完全一致

我们当前使用：

```text
阿里云百炼
+
OpenAI-compatible API
```

“兼容”主要表示：

```text
请求 / 响应风格相似
```

不意味着：

```text
所有 error.code
所有限制
所有 Header
所有超时行为
```

都和另一个 Provider 完全一致。

---

# 七十四、什么时候才需要 Provider Error Mapping？

以后如果真的同时接：

```text
Provider A
Provider B
```

可能会发现：

```text
HTTP Status 类似
```

但：

```text
Provider Code
字段结构
Retry 提示
Request ID
```

都不同。

这时才有真实依据考虑：

```text
Provider Error Mapping
```

现在不要提前抽象。

---

# 七十五、Exception Mapping 是什么？

真实后端里，

上层可能不希望直接看到：

```text
HttpTimeoutException
IOException
原始 Provider JSON
```

而会转换成：

```text
LlmAuthenticationException
LlmRateLimitException
LlmTimeoutException
LlmInvalidRequestException
LlmProviderException
```

这叫：

```text
Exception Mapping
```

---

# 七十六、现在要不要马上建十几个异常类？

不建议。

当前只有：

```text
一个学习项目
一个 Provider
```

现在就创建：

```text
ErrorCode Enum
十几个 Exception
ProviderErrorAdapter
RetryPolicy
```

很容易：

```text
过度设计
```

020-2 先用清晰的分类逻辑理解机制。

Node 021 真正做 Spring Boot 项目时，

再决定：

```text
哪些异常类型真的值得存在
```

---

# 七十七、异常处理的三个层次

## 第一层：能看到失败

```text
打印 HTTP Status
打印 Error Body
```

## 第二层：能识别失败

```text
知道是 401 / 429 / 503 / Timeout
```

## 第三层：能采取正确动作

```text
该 Retry 的 Retry
不该 Retry 的快速失败
Context Too Long 去缩 Context
认证失败去修配置
```

Node 020 的目标就是进入：

```text
第三层
```

---

# 七十八、不要吞异常

错误做法：

```java
try {
    callModel();
} catch (Exception e) {
    return "";
}
```

这样会导致：

```text
失败原因完全丢失
```

上层甚至可能以为：

```text
模型正常返回了空字符串
```

---

# 七十九、也不要只会 printStackTrace

另一种初学写法：

```java
catch (Exception e) {
    e.printStackTrace();
}
```

它只解决：

```text
开发时看堆栈
```

没有解决：

```text
错误分类
业务返回
日志语义
Retry
```

---

# 八十、好的错误信息应该回答什么？

至少尽量回答：

```text
发生在哪里？
是什么错误？
Provider 返回什么？
Request ID 是什么？
是否重试过？
第几次重试？
最终准备怎么处理？
```

---

# 八十一、不要泄露 API Key

错误日志绝对不要打印：

```text
Authorization: Bearer ...
```

API Key 应该：

```text
永远避免进入普通日志
```

---

# 八十二、Timeout 后“服务端状态未知”

这是非常重要的分布式系统思想。

客户端 Timeout 只说明：

```text
客户端等不到结果了
```

不等于：

```text
服务端百分之百没有执行
```

可能发生：

```text
服务端其实已经成功完成
↓
Response 在返回过程中延迟 / 丢失
↓
客户端 Timeout
```

---

# 八十三、为什么这会影响 Retry？

如果你看到 Timeout 以后：

```text
立刻 Retry
```

第二次请求可能：

```text
又执行一次
```

于是：

```text
重复调用
重复 Token 成本
```

以后如果 Agent 带副作用：

```text
甚至重复业务操作
```

---

# 八十四、什么是 Retry Storm？

假设 Provider 大面积故障：

```text
1000 个请求一起失败
```

每个都立即重试 3 次：

```text
1000
↓
3000
```

Provider 反而收到更多压力。

这就是：

```text
Retry Storm
重试风暴
```

所以重试必须：

```text
有限
退避
必要时限流
```

---

# 八十五、Circuit Breaker 是什么？

成熟系统有时会使用：

```text
Circuit Breaker
熔断器
```

如果发现 Provider：

```text
持续大量失败
```

暂时停止继续打请求，

等待一段时间再恢复。

当前：

```text
知道概念即可
```

不实现。

---

# 八十六、Fallback 是什么？

Fallback：

```text
主方案失败后切备用方案
```

例如：

```text
主模型不可用
↓
切备用模型
```

或者：

```text
AI 服务不可用
↓
返回降级结果
```

但不同模型：

```text
能力
价格
格式支持
Tool Calling
```

可能不同。

所以 Fallback 也不是：

```text
随便切一个模型
```

当前不实现。

---

# 八十七、异常处理和 Model Selection 也连接起来了

Node 018 学过：

```text
Model + Provider
```

真实选型不只看：

```text
模型聪不聪明
```

还要看：

```text
稳定性
限流
错误率
可用性
Provider 行为
```

所以异常处理也是：

```text
模型 / Provider 工程质量
```

的一部分。

---

# 八十八、异常处理和 Observability / Evaluation

以后真实服务会统计：

```text
成功率
429 比例
Timeout 比例
5xx 比例
Retry 后成功率
平均 Retry 次数
P95 延迟
```

这些已经进入：

```text
Observability
Production Evaluation
```

领域。

当前只建立基础意识。

---

# 八十九、成功率不能只统计 HTTP 200

如果你定义：

```text
HTTP 200 = 成功
```

会漏掉：

```text
JSON 解析失败
Structured Output 不合法
业务字段错误
模型返回空答案
```

更合理的成功率最终应该根据：

```text
业务是否真正完成
```

定义。

---

# 九十、一个更完整的调用结果模型

以后可以形成：

```text
Request
↓
Transport Result
↓
HTTP Result
↓
Provider Result
↓
Model Output Result
↓
Business Validation Result
```

每一层：

```text
都可能失败
```

---

# 九十一、Node 020 暂时不追求生产级框架

不要一下子引入：

```text
Resilience4j
Circuit Breaker
Retry Template
统一 Provider 网关
多 Provider Fallback
Metrics
Tracing
```

这会太早。

---

# 九十二、Node 020 当前真正要掌握什么？

只需要掌握：

```text
1. Java Exception 和 HTTP Error 的区别

2. 常见 4xx / 5xx 的语义

3. Provider Error Body 的价值

4. Retryable / Non-retryable

5. Timeout

6. Context Too Long

7. HTTP 200 后仍可能业务失败

8. Limited Retry + Backoff

9. Retry 的成本和副作用

10. 排障日志应该保留什么
```

---

# 九十三、020-2 为什么值得实验？

因为如果不跑，

很容易只停留在：

```text
“429 要重试，400 不重试”
```

的状态码背诵。

020-2 应该让你真正看到：

```text
错误模型
→ HTTP Error Response

模拟 429
→ Retryable

模拟 503
→ Retryable

模拟 Timeout
→ Java Exception

模拟 Context Too Long
→ Non-retryable
```

然后把：

```text
错误分类
```

变成：

```text
Java 控制流
```

---

# 九十四、哪些错误适合真实 API 实验？

建议真实触发：

```text
错误模型名
```

因为它：

```text
安全
明确
成本低
```

而且你之前已经真实遇到过：

```text
404 model_not_found
```

020-2 可以把它正式纳入异常分类实验。

---

# 九十五、哪些错误不要为了实验故意真实触发？

例如：

```text
疯狂并发制造 429
故意把 Provider 打到过载
故意塞巨大 Context 浪费 Token
```

没有必要。

这些应该：

```text
本地模拟
```

---

# 九十六、为什么 429 要模拟？

为了触发真实限流而：

```text
疯狂请求
```

可能：

```text
浪费额度
影响账号
制造无意义流量
```

真正要验证的是：

```text
Java 代码收到 429 后怎么决策
```

而不是：

```text
如何把 Provider 打到限流
```

---

# 九十七、为什么 5xx 要模拟？

你无法控制：

```text
Provider 什么时候真的出 500 / 503
```

为了等真实故障：

```text
没有学习价值
```

本地模拟一个：

```text
HTTP 503
```

反而更稳定。

---

# 九十八、为什么 Timeout 也适合模拟？

真实网络 Timeout：

```text
不可预测
```

本地服务可以：

```text
故意延迟响应
```

从而稳定让 Java 触发：

```text
Timeout
```

这样更适合学习。

---

# 九十九、020-2 可以设计成什么？

建议：

```text
A：错误模型 / 404
→ Non-retryable

B：模拟 429 Rate Limit
→ Retryable

C：模拟 503
→ Retryable

D：模拟 Timeout
→ Java Timeout Exception

E：模拟 Context Too Long
→ Non-retryable
```

---

# 一百、但实验重点不是“错误数量”

真正目标不是：

```text
收集五个 Error Code
```

而是：

> **同一套错误处理逻辑能否根据不同失败原因，做出不同动作。**

---

# 一百零一、一个最小错误决策模型

以后代码可能类似：

```text
Error
↓
classify()
↓
Retryable?
├── YES
│   ↓
│  Backoff
│   ↓
│  Retry（有上限）
│
└── NO
    ↓
   Fail Fast
```

Context Too Long：

```text
NO RETRY
↓
Reduce Context
```

---

# 一百零二、Retryable 不是错误本身唯一的属性

更进一步：

```text
是否重试
```

还可能取决于：

```text
请求是否幂等
已经执行到哪一步
Streaming 是否已经输出
剩余 Deadline
已 Retry 几次
额外成本
业务副作用
```

所以生产级 Retry Policy 会比：

```java
if (status == 500)
```

复杂很多。

当前先掌握基础。

---

# 一百零三、错误处理的黄金顺序

以后遇到失败，可以按这个顺序想：

```text
1. 有没有拿到 HTTP Response？

2. 如果没有：
   是网络、Timeout、Interrupted 还是连接失败？

3. 如果有：
   HTTP Status 是多少？

4. Provider Error Code / Message 是什么？

5. 是暂时错误还是确定性错误？

6. 是否值得 Retry？

7. 如果 Retry：
   等多久？
   最多几次？

8. 如果不 Retry：
   应该修请求、配置、权限还是 Context？

9. 记录 request_id 和必要日志

10. 把明确失败交给上层
```

---

# 一百零四、常见错误的人话翻译

```text
400
→ “你发给我的东西有问题。”

401
→ “你的身份认证不对。”

403
→ “我知道你是谁，但你没有权限。”

404
→ “你要找的模型 / 资源不存在。”

429 Rate Limit
→ “你来得太快了，等一下。”

429 Quota
→ “不是来太快，是额度不够了。”

500
→ “我内部出错了。”

503
→ “我现在忙不过来 / 暂时不可用。”

Timeout
→ “客户端等不下去了，但不代表服务端一定没执行。”

Context Too Long
→ “你给我的上下文超过可处理范围。”
```

---

# 一百零五、不要死背状态码，要记“下一步动作”

真正有工程价值的是：

```text
401
→ 修 Key

429 Rate Limit
→ Backoff

503
→ 有限 Retry

Context Too Long
→ 缩 Context

400 Invalid Parameter
→ 修参数
```

---

# 一百零六、当前百炼环境需要记住什么？

截至当前百炼 OpenAI-compatible 文档，常见错误包括：

```text
400 Invalid Request
401 Invalid API Key
429 Rate Limit
429 Quota / Billing
500 Server Error
503 Engine Overloaded
```

百炼不同 API 的业务错误字段和处理细节可能不同。

所以实际编码时：

> **以当前调用接口的官方文档和真实 Error Body 为准。**

不要把某个 Provider 的：

```text
error.code
```

当成所有 LLM API 的统一标准。

---

# 一百零七、为什么开发阶段要保留 Raw Error Body？

刚接一个 Provider 时：

```text
你还不知道真实错误结构
```

所以开发阶段保存：

```text
HTTP Status
+
Raw Error Body
```

很有价值。

等协议理解清楚以后，

再做：

```text
结构化解析
```

---

# 一百零八、不要用 LLM 去解析确定性的 Provider Error

例如收到：

```json
{
  "error": {
    "code": "...",
    "message": "..."
  }
}
```

不需要：

```text
再调用一个 LLM
判断这是什么错误
```

确定性协议：

```text
Java 代码直接解析
```

更便宜、更稳定、更可控。

---

# 一百零九、为什么这节很“后端”？

因为这里处理的是：

```text
网络
HTTP
状态码
超时
重试
并发
错误分类
日志
可靠性
```

本质上就是：

```text
Backend Engineering
Distributed Systems
Reliability
```

所以对于 Java 后端转 Agent 工程来说，

Node 020 非常重要。

---

# 一百一十、它和普通第三方 API 有什么相同？

很多思想完全通用：

```text
Timeout
Retry
Rate Limit
Authentication
5xx
Backoff
Observability
```

所以这不是：

```text
只对 AI 有用
```

也是通用后端能力。

---

# 一百一十一、LLM API 又有什么特殊之处？

LLM 有一些额外特点：

```text
请求可能很贵
响应可能很慢
输出存在随机性
Context 有长度限制
Streaming 很常见
Retry 后答案可能不同
同请求再次消耗 Token
HTTP 200 也不代表事实正确
Tool Agent 以后可能产生副作用
```

所以它又比普通 CRUD API：

```text
更复杂
```

---

# 一百一十二、Node 020 和前面知识的连接

```text
Node 008
第一次 API
↓
知道怎么请求

Node 016
Streaming
↓
知道响应可以逐块回来

Node 019
Context Engineering
↓
知道请求可能过长

Node 020
异常
↓
知道失败以后怎么办

Node 021
第一阶段项目
↓
第一次把异常处理放进真正 Spring Boot 项目
```

---

# 一百一十三、Node 021 为什么需要 Node 020？

因为一个真正的接口：

```text
POST /ai/analyze
```

不能假设：

```text
LLM 永远成功
```

至少要考虑：

```text
Provider 不可用
请求超时
输入不合法
模型输出不合法
```

否则它只是：

```text
Demo
```

还不算一个像样的后端功能。

---

# 一百一十四、理解自测

## 基础

1. 为什么 LLM API 异常不能只理解成 HTTP 500？
2. Java Exception 和 HTTP Error 有什么区别？
3. 为什么 `HttpClient` 收到 400 时不一定抛异常？
4. 什么是 Fail Fast？

## HTTP

5. 400 一般意味着什么？
6. 401 和 403 有什么区别？
7. 404 为什么通常不应该机械 Retry？
8. 429 为什么不能一概而论？
9. Rate Limit 和 Quota Exhausted 有什么区别？
10. 500 / 503 为什么通常比 400 更适合 Retry？

## Provider

11. 为什么要解析 Provider Error Body？
12. `request_id` 有什么价值？
13. 为什么 OpenAI-compatible 不代表错误协议完全一致？

## Timeout

14. Connect Timeout 是什么？
15. Request Timeout 是什么？
16. 为什么 Timeout 后不能断言“服务端一定没执行”？
17. 为什么 Timeout Retry 要考虑成本和副作用？

## Context

18. Context Too Long 为什么通常属于 Non-retryable？
19. 正确动作是什么？
20. 它和 Node 019 有什么关系？

## Retry

21. 什么是 Transient Error？
22. 什么是 Deterministic Error？
23. 什么是 Exponential Backoff？
24. 为什么 Retry 需要最大次数？
25. 什么是 Retry Storm？
26. 什么是幂等？
27. 为什么 LLM Retry 不是免费的？

## 应用层

28. 为什么 HTTP 200 仍可能业务失败？
29. Structured Output 合法是否代表事实正确？
30. Response Parse Error 是否一定值得 Retry？

## Streaming

31. Streaming 中途断开为什么比普通请求复杂？
32. 为什么 Retry 通常不是从断点继续生成？
33. 什么是 Idle Timeout？

---

# 一百一十五、口述验收

如果有人问：

> LLM API 调用失败以后应该怎么办？

你应该能回答：

> 先区分有没有拿到 HTTP Response。如果没有，可能是连接失败、网络异常或 Timeout 等 Transport 问题；如果拿到了 Response，则先看 HTTP Status，再解析 Provider 返回的 error code、message 和 request_id。
>
> 错误不能统一 Retry。400、401、403、404、Context Too Long 这类确定性问题通常应该修请求、配置、权限或 Context，而不是原样重试；429 的短期限流、部分网络异常以及 500、502、503、504 等暂时性错误通常可以考虑有限重试，并配合 Backoff。
>
> 同时 HTTP 200 也不代表业务一定成功，因为后面还可能出现响应解析失败、Structured Output 不合法、DTO 或业务校验失败、事实错误等问题。LLM Retry 还会产生额外 Token、成本和潜在副作用，因此真正的重点是先分类错误，再决定动作。

如果能自然讲清：

```text
Transport Error
HTTP Error
Provider Error
Application Error
Retryable
Non-retryable
Backoff
Timeout
Context Too Long
```

说明核心已经建立。

---

# 一百一十六、Node 020-1 完成标准

读完后应该能够：

- [ ] 区分本地配置错误、网络异常、HTTP Error、Provider Error；
- [ ] 理解 Java Exception 不等于 HTTP Error；
- [ ] 理解 `HttpClient` 收到 4xx / 5xx 时仍可能返回 `HttpResponse`；
- [ ] 理解 400 / 401 / 403 / 404 / 429 / 500 / 503 的基本含义；
- [ ] 理解 429 Rate Limit 和 Quota Exhausted 的区别；
- [ ] 理解 Provider Error Body 的作用；
- [ ] 理解 `request_id` 的排障价值；
- [ ] 理解 Context Too Long；
- [ ] 理解为什么 Context Too Long 原样 Retry 没意义；
- [ ] 理解 HTTP 200 不等于业务成功；
- [ ] 理解 Timeout 的基本层次；
- [ ] 理解 Timeout 后服务端状态可能未知；
- [ ] 理解 Transient Error 和 Deterministic Error；
- [ ] 理解 Retryable 和 Non-retryable；
- [ ] 理解 Exponential Backoff；
- [ ] 理解 Retry 需要次数上限；
- [ ] 理解 Retry 会增加 Token 和费用；
- [ ] 初步理解幂等；
- [ ] 理解 Streaming 中途失败的特殊性；
- [ ] 理解日志不能泄露 API Key；
- [ ] 知道当前还不需要搭生产级容错框架。

---

# 一百一十七、本篇停止点

本篇只建立：

```text
LLM API Failure Model
+
Error Classification
+
Retry Decision
```

暂时不实现：

```text
完整 Retry Framework
Resilience4j
Circuit Breaker
多 Provider Fallback
统一异常继承树
Metrics Dashboard
Distributed Tracing
生产级 Jitter
复杂 Idempotency 系统
```

---

# 一百一十八、下一步：020-2

下一篇建议进入：

```text
020-2 - 实验：LLM API 异常处理与 Retry
```

实验保持小而明确：

```text
A：错误模型 / 404
→ Non-retryable

B：模拟 429 Rate Limit
→ Retryable

C：模拟 503
→ Retryable

D：模拟 Timeout
→ Java Timeout Exception

E：模拟 Context Too Long
→ Non-retryable
```

核心不是：

```text
制造五个错误
```

而是：

```text
同一个 Java Error Handler
↓
识别不同错误
↓
做出不同 Retry 决策
```

---

# 本篇一句话总结

> **LLM API 异常处理的核心不是“失败就重试”，而是先判断失败发生在哪一层、它是暂时性错误还是确定性错误，再决定 Retry、Backoff、修请求、修配置、缩短 Context 或直接失败。**

最终心智模型：

```text
LLM Request
↓
有没有拿到 HTTP Response？
│
├── 没有
│   ↓
│  Network / Timeout / Interrupted
│   ↓
│  判断是否值得 Retry
│
└── 有
    ↓
   HTTP Status
    ↓
   Provider Error Body
    ↓
   Error Classification
    ↓
   ┌────────────────────┐
   │ Transient          │
   │ 429 / 5xx / ...    │
   └─────────┬──────────┘
             ↓
      Backoff + Limited Retry

   ┌────────────────────┐
   │ Deterministic      │
   │ 400 / 401 / ...    │
   └─────────┬──────────┘
             ↓
      Fix Request / Config
      / Permission / Context

HTTP 200
↓
Parse
↓
Validate
↓
Business Success
```

真正要记住：

> **Retry is a decision, not a reflex.**

也就是：

> **重试应该是经过错误分类后的决策，而不是看到失败后的条件反射。**

**Node 020-1：从“会调用 LLM API”，开始进入“会可靠地调用 LLM API”。**
