# Node 025：Tool Dispatcher 到底是什么？

> 所属阶段：Phase 2 / Tool Calling 与 Agent Loop  
> 笔记类型：理论 + 最小实现篇  
> 学习定位：理解模型返回 Tool Call 之后，Java Application 如何根据 `toolName` 找到对应的真实业务方法，并完成最基础的参数解析与方法分发。  
> 本篇边界：只解决 **“Tool Call 到 Java Method 之间怎么接起来”**；暂不打通完整的「用户 → 模型 → Tool Call → Tool Result → 模型 → 最终回答」链路，这部分留到 Node 026。  
> 核心目标：亲手写出一个最简单的 Tool Dispatcher，知道后续 Spring AI、OpenAI Agents SDK、LangGraph 等框架帮我们隐藏了什么。

---

# 一、本篇为什么存在？

前面的几个 Node，我们已经依次解决了三个问题：

```text
Node 022
Tool Calling 是什么？
    ↓
模型可以决定“我要调用工具”

Node 023
Tool Definition 是什么？
    ↓
应用程序怎样告诉模型“有哪些工具可以用”

Node 024
Tool 参数 Schema 是什么？
    ↓
如果模型决定调用某个工具，
它应该生成什么样的 arguments
```

现在终于来到一个真正开始进入：

```text
执行
```

的问题。

假设模型已经返回：

```json
{
  "name": "queryCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

此时真正的问题来了：

> **Java 程序接下来到底干什么？**

模型只返回了：

```text
queryCampaign
campaignId = 123
```

但 Java 程序里真正存在的，可能是：

```java
public Campaign queryCampaign(Long campaignId) {
    return campaignService.getById(campaignId);
}
```

那么：

```text
模型返回的 Tool Name
```

怎样变成：

```text
真正的 Java Method 调用
```

中间必须有一层东西负责“接线”。

这就是：

# Tool Dispatcher

---

# 二、先看最核心的一张图

整个关系可以先记成：

```text
LLM
 │
 │ 返回 Tool Call
 ▼
┌─────────────────────────┐
│ name = queryCampaign    │
│ arguments = {...}       │
└─────────────────────────┘
 │
 ▼
Application
 │
 ▼
Tool Dispatcher
 │
 ├── queryCampaign  ─────→ queryCampaign(...)
 │
 ├── queryBudget    ─────→ queryBudget(...)
 │
 └── pauseCampaign  ─────→ pauseCampaign(...)
```

所以 Tool Dispatcher 最核心的职责就是：

> **根据模型返回的 Tool Name，把一次抽象的 Tool Call，分发到真正的程序代码。**

---

# 三、Dispatcher 这个词是什么意思？

`Dispatcher` 可以理解为：

```text
分发器
调度分发器
路由器
```

它做的事情很像：

```text
收到一个名字
    ↓
判断这个名字对应谁
    ↓
把请求交给对应处理者
```

例如 Web 开发里你其实早就见过类似思想。

HTTP 请求：

```text
GET /campaign/123
```

最终会被 Spring MVC 路由到：

```java
@GetMapping("/campaign/{id}")
public Campaign getCampaign(...) {
    ...
}
```

Tool Dispatcher 也有点类似：

```text
toolName = queryCampaign
```

最终被路由到：

```java
queryCampaign(...)
```

所以：

> **Tool Dispatcher 本质上也是一种路由。**

只是它路由的不是：

```text
HTTP URL
```

而是：

```text
Tool Name
```

---

# 四、最简单的 Dispatcher 长什么样？

路线里已经给出了最朴素的写法：

```java
switch (toolName) {

    case "queryCampaign":
        ...

    case "pauseCampaign":
        ...
}
```

我们把它写完整一点：

```java
public Object dispatch(String toolName, JSONObject arguments) {

    return switch (toolName) {

        case "queryCampaign" -> {
            Long campaignId = arguments.getLong("campaignId");
            yield queryCampaign(campaignId);
        }

        case "queryBudget" -> {
            Long campaignId = arguments.getLong("campaignId");
            yield queryBudget(campaignId);
        }

        default ->
            throw new IllegalArgumentException(
                "Unknown tool: " + toolName
            );
    };
}
```

这段代码已经是一个最小版本的：

```text
Tool Dispatcher
```

了。

---

# 五、这段代码到底做了什么？

逐步拆开。

模型返回：

```json
{
  "name": "queryCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

Application 解析之后得到：

```java
String toolName = "queryCampaign";

JSONObject arguments = ...;
```

然后调用：

```java
dispatch(toolName, arguments);
```

Dispatcher 判断：

```java
case "queryCampaign"
```

于是进入：

```java
Long campaignId = arguments.getLong("campaignId");
```

然后真正执行：

```java
queryCampaign(campaignId);
```

最终得到：

```java
Campaign result
```

所以本质过程是：

```text
String Tool Name
        ↓
匹配
        ↓
解析 Arguments
        ↓
转换成 Java 参数
        ↓
调用真实 Java Method
```

---

# 六、Tool Dispatcher 不是 LLM 的一部分

这是一个非常重要的边界。

很多初学者会模糊地想成：

```text
模型调用了 queryCampaign()
```

其实不是。

真正发生的是：

```text
模型：
我建议调用 queryCampaign，
参数是 campaignId = 123

Application：
好的，我收到这个请求了

Dispatcher：
这个 name 对应 Java 里的 queryCampaign(...)

Java：
真正执行方法
```

所以：

> **LLM 不执行 Java 方法。**

模型只生成：

```text
Tool Call Request
```

真正执行的是：

```text
Application
```

而 Tool Dispatcher 是 Application 内部的一部分。

---

# 七、再区分一次：Tool Definition、Tool Call、Dispatcher

这三个概念现在必须彻底分开。

## Tool Definition

方向：

```text
Application → LLM
```

作用：

```text
告诉模型有哪些工具可以使用
```

例如：

```json
{
  "name": "queryCampaign",
  "description": "根据 campaignId 查询广告计划",
  "parameters": {
    ...
  }
}
```

---

## Tool Call

方向：

```text
LLM → Application
```

作用：

```text
模型表达：
我想调用某个 Tool
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

---

## Tool Dispatcher

位置：

```text
Application 内部
```

作用：

```text
根据 Tool Call
找到真正的 Java 方法
```

例如：

```text
queryCampaign
    ↓
CampaignTool.queryCampaign(...)
```

---

# 八、为什么不能让模型直接返回 Java 方法名然后反射执行？

你可能会自然想到：

> Java 不是有反射吗？  
> 模型返回什么方法名，我直接反射调用不就行了？

例如：

```java
clazz.getMethod(toolName, ...).invoke(...);
```

技术上当然能做。

但是：

> **能做，不代表一开始就应该这样做。**

因为如果你直接把：

```text
模型输出
```

当成：

```text
任意方法调用权限
```

风险会非常大。

比如模型输出：

```text
deleteAllCampaigns
```

或者：

```text
shutdown
```

如果程序真的直接反射：

```java
method.invoke(...)
```

那么：

```text
模型能生成什么名字
≈
模型能尝试执行什么方法
```

这绝对不是一个合理的权限边界。

---

# 九、Dispatcher 本质上也是能力白名单

最简单的 `switch`：

```java
switch (toolName) {

    case "queryCampaign":
        ...

    case "queryBudget":
        ...

    default:
        throw ...
}
```

其实隐含了一层非常重要的东西：

```text
只有这些 Tool Name
才允许被执行
```

也就是说：

```text
Dispatcher
```

不仅负责：

```text
“找到方法”
```

同时也负责：

```text
“限制哪些方法可以作为 Tool 被执行”
```

从安全角度看，这非常重要。

---

# 十、Tool Definition 和 Dispatcher 必须保持一致

假设你给模型的 Tool Definition 里写了：

```text
queryCampaign
```

但是 Dispatcher 里却只有：

```java
case "getCampaign":
```

那么模型返回：

```text
queryCampaign
```

Application 就会发现：

```text
找不到对应 Tool
```

所以：

```text
Tool Definition
        ↕
Tool Dispatcher
```

之间必须保持一致。

最核心的是：

```text
Tool Name
```

必须一致。

---

# 十一、Tool Name 就像一份协议里的“路由键”

例如：

```text
queryCampaign
```

这个名字实际上同时出现在：

```text
Tool Definition
模型 Tool Call
Application Dispatcher
```

它像一个：

```text
Routing Key
```

可以画成：

```text
Tool Definition
name = queryCampaign
        ↓
LLM
        ↓
Tool Call
name = queryCampaign
        ↓
Dispatcher
case queryCampaign
        ↓
Java Method
queryCampaign(...)
```

只要中间有一个名字不一致：

```text
链路就断了
```

---

# 十二、arguments 不能直接当 Java 参数使用

模型返回的是：

```json
{
  "campaignId": 123
}
```

而 Java 方法可能是：

```java
queryCampaign(Long campaignId)
```

这两者并不是同一个东西。

前者是：

```text
JSON 数据
```

后者是：

```text
Java 类型
```

所以 Dispatcher 通常还需要完成一件事：

> **把 Tool arguments 转换成 Java 能够使用的参数。**

例如：

```java
Long campaignId =
    arguments.getLong("campaignId");
```

---

# 十三、Dispatcher 的最小职责

一个最基础的 Dispatcher，可以先认为有四步：

```text
1. 读取 toolName
2. 匹配对应 Tool
3. 解析 arguments
4. 调用真正 Java 方法
```

写成：

```text
Tool Call
   ↓
Read Name
   ↓
Route
   ↓
Parse Arguments
   ↓
Invoke Method
```

---

# 十四、一个完整一点的例子

假设我们有两个 Tool：

```java
public Campaign queryCampaign(Long campaignId) {
    return new Campaign(
        campaignId,
        "Summer Campaign",
        "RUNNING"
    );
}

public Budget queryBudget(Long campaignId) {
    return new Budget(
        campaignId,
        1000L,
        650L
    );
}
```

Dispatcher：

```java
public Object dispatch(
        String toolName,
        JSONObject arguments
) {

    return switch (toolName) {

        case "queryCampaign" -> {

            Long campaignId =
                arguments.getLong("campaignId");

            yield queryCampaign(campaignId);
        }

        case "queryBudget" -> {

            Long campaignId =
                arguments.getLong("campaignId");

            yield queryBudget(campaignId);
        }

        default ->
            throw new IllegalArgumentException(
                "Unknown tool: " + toolName
            );
    };
}
```

---

# 十五、为什么返回 Object？

上面的例子用了：

```java
Object
```

因为不同 Tool 的返回类型可能不同：

```text
queryCampaign → Campaign

queryBudget → Budget

queryMetrics → Metrics
```

为了先让 Dispatcher 能够统一处理：

```java
public Object dispatch(...)
```

是最简单的方式。

这并不代表：

```text
生产代码一定应该全部 Object
```

只是当前 Node 用它可以把重点放在：

```text
分发
```

本身。

---

# 十六、Dispatcher 不应该承担所有职责

这是非常重要的一点。

如果以后 Dispatcher 变成：

```java
public Object dispatch(...) {

    // 校验 JSON
    // 校验权限
    // 查询数据库
    // 写日志
    // 限流
    // 事务
    // 重试
    // 拼业务 SQL
    // 调第三方接口
    // 组装返回值
    // ...
}
```

那么它就会变成一个巨型方法。

正确思路更像：

```text
Dispatcher
    ↓
只负责找到对应 Tool Handler
    ↓
Handler / Service
    ↓
真正处理业务
```

---

# 十七、Dispatcher 和业务方法应该分层

推荐理解成：

```text
Tool Dispatcher
      ↓
Tool Handler
      ↓
Business Service
      ↓
Repository / API
```

例如：

```java
case "queryCampaign" -> {

    Long campaignId =
        arguments.getLong("campaignId");

    yield campaignTool.queryCampaign(campaignId);
}
```

而：

```java
campaignTool.queryCampaign(...)
```

里面再调用：

```java
campaignService.getById(...)
```

这样职责更清楚。

---

# 十八、最小架构可以这样理解

```text
LLM
 ↓
Tool Call
 ↓
Tool Dispatcher
 ↓
CampaignTool
 ↓
CampaignService
 ↓
Database / Business API
```

这里每一层的职责不同。

### LLM

负责：

```text
理解用户意图
决定调用什么 Tool
生成参数
```

### Tool Dispatcher

负责：

```text
根据 Tool Name 分发
```

### Tool

负责：

```text
把 Agent 能力和业务能力连接起来
```

### Service

负责：

```text
真正业务逻辑
```

---

# 十九、Dispatcher 和 Controller 有一点像

如果你熟悉 Spring Boot，可以做一个类比。

Controller：

```text
HTTP Request
    ↓
URL / Method
    ↓
Controller Method
```

Tool Dispatcher：

```text
Tool Call
    ↓
Tool Name
    ↓
Tool Method
```

例如：

```text
HTTP
GET /campaign/123
        ↓
CampaignController.getCampaign(...)
```

类似：

```text
Tool Call
queryCampaign
        ↓
CampaignTool.queryCampaign(...)
```

只是入口协议不同。

---

# 二十、但 Tool Dispatcher 不等于 Controller

不要把二者完全等同。

Controller 面对：

```text
客户端 HTTP 请求
```

Dispatcher 面对：

```text
LLM 生成的 Tool Call
```

而且 LLM 输出具有：

```text
概率性
不完全可信
可能格式错误
可能参数不合理
```

所以 Tool Dispatcher 后面的链路一般会比普通 Controller 更强调：

```text
校验
权限
错误处理
边界控制
```

---

# 二十一、未知 Tool 必须明确失败

假设模型返回：

```json
{
  "name": "deleteEverything",
  "arguments": {}
}
```

Dispatcher 不应该：

```text
猜一下它想干什么
```

也不应该：

```text
找一个名字最接近的方法
```

更不能：

```text
直接反射调用
```

正确行为应该是：

```java
default -> throw new IllegalArgumentException(
    "Unknown tool: " + toolName
);
```

也就是说：

> **未注册 Tool 默认拒绝。**

---

# 二十二、为什么“名字相近自动匹配”很危险？

比如你注册：

```text
pauseCampaign
```

模型却返回：

```text
pauseAllCampaigns
```

如果你做：

```text
模糊匹配
```

就可能出现：

```text
“差不多就是这个意思”
```

然后执行了危险操作。

Tool Execution 里应该更倾向：

```text
精确匹配
```

而不是：

```text
猜测匹配
```

---

# 二十三、Tool arguments 依然是不可信输入

Node 024 已经强调过：

```text
Schema 能约束模型
但不能把模型输出当成可信输入
```

进入 Dispatcher 后，这一点更加重要。

例如模型返回：

```json
{
  "campaignId": -999
}
```

即使 JSON 本身：

```text
格式正确
```

也不代表：

```text
业务上合法
```

所以后续会继续有：

```text
Node 029：Tool 参数校验
```

---

# 二十四、当前 Node 只做最小参数检查即可

Node 025 不需要提前把 Node 029 全学掉。

当前只需要理解：

```text
参数至少得能被解析
```

例如：

```java
Long campaignId =
    arguments.getLong("campaignId");

if (campaignId == null) {
    throw new IllegalArgumentException(
        "campaignId is required"
    );
}
```

先做到：

```text
能取出来
能调用
错误时明确失败
```

就够了。

---

# 二十五、为什么不应该把所有 Tool 都写进一个巨型 switch？

一开始两个 Tool：

```java
switch (toolName) {
    case "queryCampaign":
    case "queryBudget":
}
```

完全没问题。

但以后如果有：

```text
50 个 Tool
100 个 Tool
300 个 Tool
```

一个 `switch` 就会变成：

```text
超大分支
```

例如：

```java
case "queryCampaign":
case "queryBudget":
case "queryAccount":
case "queryMetrics":
case "pauseCampaign":
case "resumeCampaign":
case "createCampaign":
case "updateBudget":
...
```

很难维护。

---

# 二十六、所以生产实现通常会逐渐演化

从：

```text
switch
```

演化成：

```text
Map<String, ToolHandler>
```

例如：

```java
Map<String, ToolHandler> handlers;
```

注册：

```java
handlers.put(
    "queryCampaign",
    new QueryCampaignToolHandler(...)
);
```

执行：

```java
ToolHandler handler =
    handlers.get(toolName);

if (handler == null) {
    throw new IllegalArgumentException(
        "Unknown tool: " + toolName
    );
}

return handler.execute(arguments);
```

---

# 二十七、Map Dispatcher 的基本结构

例如定义：

```java
public interface ToolHandler {

    String name();

    Object execute(JSONObject arguments);
}
```

实现：

```java
public class QueryCampaignToolHandler
        implements ToolHandler {

    @Override
    public String name() {
        return "queryCampaign";
    }

    @Override
    public Object execute(JSONObject arguments) {

        Long campaignId =
            arguments.getLong("campaignId");

        return queryCampaign(campaignId);
    }
}
```

Dispatcher：

```java
public class ToolDispatcher {

    private final Map<String, ToolHandler> handlers;

    public ToolDispatcher(List<ToolHandler> handlers) {

        this.handlers = handlers.stream()
            .collect(Collectors.toMap(
                ToolHandler::name,
                handler -> handler
            ));
    }

    public Object dispatch(
            String toolName,
            JSONObject arguments
    ) {

        ToolHandler handler =
            handlers.get(toolName);

        if (handler == null) {
            throw new IllegalArgumentException(
                "Unknown tool: " + toolName
            );
        }

        return handler.execute(arguments);
    }
}
```

---

# 二十八、为什么现在还要先学 switch？

既然 Map 更优雅，为什么路线还让我们先写：

```java
switch
```

因为学习目标不是：

```text
马上写最抽象的架构
```

而是：

> **先把最底层的分发机制看清楚。**

`switch` 非常透明：

```text
name
 ↓
case
 ↓
method
```

你一眼就能看到：

```text
框架到底在帮你做什么
```

---

# 二十九、框架里的 Tool Calling 本质上也绕不开 Dispatcher

以后使用 Spring AI 时，你可能会写：

```java
@Tool
public Campaign queryCampaign(Long campaignId) {
    ...
}
```

然后框架自动帮你完成：

```text
注册 Tool
生成 Tool Definition
解析 Tool Call
找到方法
反序列化 arguments
调用方法
序列化结果
```

看起来好像：

```text
没有 Dispatcher
```

其实不是。

真正情况是：

> **Dispatcher 被框架藏起来了。**

---

# 三十、框架帮你隐藏了什么？

至少可能隐藏：

```text
Tool Registry

Tool Name → Method 映射

Arguments JSON → Java DTO

Method Invocation

异常捕获

Tool Result 序列化

上下文传递
```

所以当以后看到：

```java
@Tool
```

你应该知道：

```text
注解不是魔法
```

背后依然存在：

```text
注册
映射
解析
调用
```

---

# 三十一、从“注册表”角度理解 Tool Dispatcher

一个很有用的思维是：

```text
Tool Registry
```

例如：

```text
queryCampaign → QueryCampaignHandler

queryBudget → QueryBudgetHandler

queryMetrics → QueryMetricsHandler
```

Dispatcher 做的事情就是：

```text
从 Registry 中查找
```

然后执行。

所以 Tool Calling 的执行端，经常可以拆成：

```text
Tool Registry
+
Tool Dispatcher
```

---

# 三十二、Registry 和 Dispatcher 有什么区别？

简单理解：

### Registry

负责：

```text
保存有哪些 Tool
```

例如：

```java
Map<String, ToolHandler>
```

---

### Dispatcher

负责：

```text
根据 Tool Name 找 Tool
然后执行
```

例如：

```java
handler =
    registry.get(toolName);

handler.execute(arguments);
```

---

# 三十三、小规模项目可以不强行拆 Registry

如果现在只有：

```text
2～4 个 Tool
```

那么：

```java
switch
```

完全合理。

没必要为了“架构高级”提前堆：

```text
Factory
Registry
Adapter
Strategy
Reflection
Annotation Processor
```

当前 Node 的目标不是炫技。

而是：

> **真正理解 Tool Call 怎样落到 Java 方法。**

---

# 三十四、Dispatcher 的输入应该是什么？

最简单可以是：

```java
dispatch(
    String toolName,
    JSONObject arguments
)
```

以后可能演化成：

```java
dispatch(ToolCall toolCall)
```

例如：

```java
public record ToolCall(
    String id,
    String name,
    String arguments
) {
}
```

然后：

```java
dispatch(toolCall);
```

---

# 三十五、为什么 Tool Call 可能还需要 id？

真实 Provider 的 Tool Call 往往不只有：

```text
name
arguments
```

还可能有：

```text
tool_call_id
```

例如：

```json
{
  "id": "call_abc123",
  "type": "function",
  "function": {
    "name": "queryCampaign",
    "arguments": "{\"campaignId\":123}"
  }
}
```

这个 `id` 后续很重要。

因为 Application 返回 Tool Result 时，需要告诉模型：

```text
这个 Tool Result
对应的是哪一次 Tool Call
```

不过：

> 这属于 Node 026 的完整链路。

Node 025 只需要知道：

```text
真实 Tool Call 往往还有 id
```

现在不深入。

---

# 三十六、arguments 有时不是 JSON Object，而是 JSON String

这是一个非常容易踩坑的地方。

Provider 返回的：

```text
arguments
```

有时可能长这样：

```json
"{\"campaignId\":123}"
```

也就是说它本身是：

```text
String
```

而不是已经解析好的：

```text
JSONObject
```

所以真实 Application 可能先做：

```java
JSONObject arguments =
    JSON.parseObject(argumentsJson);
```

然后再：

```java
dispatcher.dispatch(
    toolName,
    arguments
);
```

---

# 三十七、所以完整的执行前半段大概是

```text
LLM Response
    ↓
读取 Tool Call
    ↓
读取 Tool Name
    ↓
读取 arguments JSON String
    ↓
JSON.parseObject(...)
    ↓
Tool Dispatcher
    ↓
Java Method
```

---

# 三十八、JSON 解析失败怎么办？

例如模型返回：

```text
{"campaignId":123
```

缺少括号。

那么：

```java
JSON.parseObject(...)
```

就会失败。

这个错误其实发生在：

```text
Dispatcher 之前
```

或：

```text
Dispatcher 输入准备阶段
```

后面我们会逐渐把 Tool Calling 错误分层。

现在只需要知道：

```text
Tool Call 并不一定天然可执行
```

---

# 三十九、Tool Dispatcher 自己也可能失败

Dispatcher 常见失败可以先分成：

```text
Unknown Tool
Invalid Arguments
Java Method Error
```

例如：

### Unknown Tool

```text
toolName = deleteUniverse
```

---

### Invalid Arguments

```text
campaignId 缺失
```

---

### Java Method Error

```text
queryCampaign(123)
    ↓
Database Timeout
```

这些以后 Node 029 / 030 会继续展开。

---

# 四十、Dispatcher 不应该吞掉所有异常

不要写成：

```java
try {

    ...

} catch (Exception e) {

    return null;
}
```

这种做法会让上层根本不知道：

```text
Tool 到底成功还是失败
```

更合理的是：

```text
明确失败
```

例如：

```java
throw new ToolExecutionException(...);
```

当然当前 Node 不需要设计完整异常体系。

但要建立一个原则：

> **Tool 执行失败应该有明确语义，而不是静默失败。**

---

# 四十一、Tool 返回值接下来去哪？

假设 Dispatcher 执行：

```java
queryCampaign(123)
```

得到：

```json
{
  "campaignId": 123,
  "name": "Summer Campaign",
  "status": "RUNNING"
}
```

接下来这个结果不是直接返回用户。

完整 Tool Calling 里，它通常会：

```text
Tool Result
    ↓
再次发送给 LLM
    ↓
LLM 根据 Tool Result
生成自然语言答案
```

这就是 Node 026。

所以 Node 025 到这里先停：

```text
Tool Call
    ↓
Dispatcher
    ↓
Java Method
    ↓
Result
```

---

# 四十二、Node 025 和 Node 026 的边界

Node 025：

```text
已知 Tool Call
    ↓
如何执行 Tool
```

Node 026：

```text
真实调用模型
    ↓
模型返回 Tool Call
    ↓
执行 Tool
    ↓
把 Tool Result 回给模型
    ↓
模型生成最终回答
```

所以：

> **025 学的是执行中间件。**

> **026 学的是完整闭环。**

---

# 四十三、最小 Demo 可以完全不调用模型

为了学习 Dispatcher，其实根本不需要先真实请求 LLM。

我们可以手写一个 Tool Call：

```java
String toolName = "queryCampaign";

JSONObject arguments =
    new JSONObject();

arguments.put(
    "campaignId",
    123L
);
```

然后：

```java
Object result =
    dispatcher.dispatch(
        toolName,
        arguments
    );
```

这样可以先单独验证：

```text
Dispatcher
```

本身。

---

# 四十四、为什么这种本地 Demo 很有价值？

因为它把问题拆开了。

如果你一开始直接做：

```text
真实 LLM
+
Tool Definition
+
Tool Call
+
Dispatcher
+
Tool Result
+
第二次 LLM 请求
```

一旦出错，你很难知道是哪一层的问题。

而先本地手写：

```text
Tool Call
```

就可以单独验证：

```text
Java Dispatcher 是否正确
```

---

# 四十五、建议的第一个 Demo

例如：

```java
public class ToolDispatcherDemo {

    public static void main(String[] args) {

        String toolName =
            "queryCampaign";

        JSONObject arguments =
            new JSONObject();

        arguments.put(
            "campaignId",
            123L
        );

        Object result =
            dispatch(
                toolName,
                arguments
            );

        System.out.println(result);
    }

    private static Object dispatch(
            String toolName,
            JSONObject arguments
    ) {

        return switch (toolName) {

            case "queryCampaign" -> {

                Long campaignId =
                    arguments.getLong(
                        "campaignId"
                    );

                yield queryCampaign(
                    campaignId
                );
            }

            default ->
                throw new IllegalArgumentException(
                    "Unknown tool: "
                        + toolName
                );
        };
    }

    private static String queryCampaign(
            Long campaignId
    ) {

        return """
            {
              "campaignId": %d,
              "name": "Demo Campaign",
              "status": "RUNNING"
            }
            """.formatted(campaignId);
    }
}
```

---

# 四十六、这个 Demo 练的不是业务代码

不要纠结：

```text
为什么 queryCampaign 只是返回写死字符串？
```

因为这一节练的是：

```text
Tool Name
    ↓
Dispatcher
    ↓
Java Method
```

业务数据真假不是重点。

---

# 四十七、第一个错误实验：Unknown Tool

把：

```java
String toolName =
    "queryCampaign";
```

改成：

```java
String toolName =
    "unknownTool";
```

预期：

```text
抛出 Unknown Tool 错误
```

目的：

```text
验证未注册 Tool 不会被执行
```

---

# 四十八、第二个错误实验：缺少参数

删除：

```java
arguments.put(
    "campaignId",
    123L
);
```

此时：

```java
campaignId == null
```

建议显式判断：

```java
if (campaignId == null) {

    throw new IllegalArgumentException(
        "campaignId is required"
    );
}
```

目的：

```text
理解 Schema 不是 Runtime Validation 的替代品
```

---

# 四十九、第三个实验：增加第二个 Tool

增加：

```text
queryBudget
```

例如：

```java
private static String queryBudget(
        Long campaignId
) {

    return """
        {
          "campaignId": %d,
          "dailyBudget": 1000,
          "spent": 650
        }
        """.formatted(campaignId);
}
```

Dispatcher：

```java
case "queryBudget" -> {

    Long campaignId =
        arguments.getLong("campaignId");

    yield queryBudget(campaignId);
}
```

此时你就能明显感受到：

```text
Tool Name
```

真的就是一个：

```text
路由键
```

---

# 五十、为什么 Node 025 仍然不需要框架？

因为现在最重要的不是：

```text
怎么少写代码
```

而是：

```text
怎么理解代码
```

如果一开始就写：

```java
@Tool
```

你看到的只是：

```text
一个注解
```

但你不知道背后到底发生了：

```text
注册
匹配
参数解析
方法调用
```

---

# 五十一、以后用框架时应该能“脑补”出底层链路

以后看到：

```java
@Tool
public Campaign queryCampaign(
    Long campaignId
) {
    ...
}
```

你脑中应该自动出现：

```text
Framework
    ↓
扫描 Tool
    ↓
生成 Definition
    ↓
注册 name → method
    ↓
LLM 返回 Tool Call
    ↓
查 registry
    ↓
arguments → Java 参数
    ↓
invoke method
    ↓
result → Tool Result
```

这才是真正学会框架。

---

# 五十二、Tool Dispatcher 和 Strategy Pattern 有什么关系？

如果以后把每个 Tool 都做成：

```java
ToolHandler
```

然后：

```java
Map<String, ToolHandler>
```

其实已经有点像：

```text
Strategy Pattern
```

每个 Handler：

```text
负责一种 Tool 执行策略
```

Dispatcher：

```text
根据名字选择策略
```

不过：

> 当前 Node 不需要为了设计模式而设计模式。

理解即可。

---

# 五十三、Tool Dispatcher 和 Command Pattern 也有一点关系

Tool Call 也可以被理解成一种：

```text
Command
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

它表达的是：

```text
执行某个命令
```

然后 Application：

```text
解释命令
执行命令
```

所以 Tool Calling 在软件工程上并不是完全陌生的东西。

它其实借用了很多经典思想：

```text
Routing
Registry
Command
Strategy
Serialization
Validation
```

只不过：

```text
命令是由 LLM 生成的
```

---

# 五十四、LLM 生成命令带来了什么新问题？

传统代码里：

```text
调用哪个方法
```

通常由程序员明确写死。

而 Tool Calling 里：

```text
模型动态决定调用哪个 Tool
```

所以执行链路里必须额外关注：

```text
模型可能选错 Tool

模型可能传错参数

模型可能生成未知 Tool

模型可能重复调用

模型可能调用危险 Tool
```

后续 Phase 2 基本就是围绕这些问题展开。

---

# 五十五、从 Node 025 往后会发生什么？

当前：

```text
025 Dispatcher
```

接下来：

```text
026 第一次完整 Tool Call
```

然后：

```text
027 多个 Tools
```

再然后：

```text
028 Description 设计
029 参数校验
030 Tool Error
031 Tool Result
032 Read / Write Tool
033 权限
034 Human Approval
035 幂等性
036 Agent Loop
...
```

你会发现：

> **Tool Dispatcher 是“模型决定调用工具”真正进入执行世界的第一道门。**

---

# 五十六、Dispatcher 是 Tool Calling 的“执行入口”

之前：

```text
Tool Definition
Schema
```

都还主要属于：

```text
描述
契约
约束
```

而从 Dispatcher 开始：

```text
真正有程序行为发生了
```

所以它是一个很关键的转折点：

```text
描述 Tool
        ↓
执行 Tool
```

---

# 五十七、Tool Name 应该被当成外部输入

虽然它是模型生成的：

```text
toolName
```

但是 Application 应该把它当成：

```text
外部输入
```

而不是：

```text
可信内部变量
```

因为：

```text
LLM 输出不是程序代码的一部分
```

模型输出更像：

```text
一个外部系统传来的请求
```

---

# 五十八、因此 Dispatcher 要遵循“默认拒绝”

一个很重要的安全原则：

```text
Allow List
```

而不是：

```text
Deny List
```

也就是：

```text
只允许明确注册的 Tool
```

而不是：

```text
除了几个危险方法，其他都能调用
```

例如：

```java
handlers.get(toolName)
```

找不到：

```text
直接失败
```

这就是一个典型的白名单模型。

---

# 五十九、不要让模型决定 Application Context

回顾 Node 024：

Tool 参数应该区分：

```text
LLM Arguments
```

和：

```text
Application Context
```

例如：

```text
campaignId
```

可以让模型生成。

但：

```text
currentUserId
tenantId
permissions
```

通常不应该让模型自己填写。

所以 Dispatcher 后面真正调用 Tool 时，可能是：

```java
tool.execute(
    arguments,
    toolContext
);
```

其中：

```text
arguments
```

来自模型。

而：

```text
toolContext
```

来自应用程序自身。

---

# 六十、一个未来版签名可能长这样

```java
public ToolResult dispatch(
    ToolCall toolCall,
    ToolContext context
) {
    ...
}
```

其中：

```java
ToolCall
```

包含：

```text
name
arguments
toolCallId
```

而：

```java
ToolContext
```

包含：

```text
userId
tenantId
requestId
permissions
```

Node 040 会专门展开这个问题。

现在知道这个方向即可。

---

# 六十一、Dispatcher 不是权限系统

即使 Dispatcher 只允许：

```text
pauseCampaign
```

也不代表：

```text
任何用户都能 pauseCampaign
```

Dispatcher 只解决：

```text
这个 Tool 是否存在
```

权限系统解决：

```text
当前用户是否有权执行
```

两者不能混淆。

---

# 六十二、Dispatcher 不是参数 Schema

Schema 负责：

```text
告诉模型：
参数应该长什么样
```

Dispatcher 负责：

```text
收到参数后：
把它交给哪个方法
```

所以：

```text
Schema
≠
Dispatcher
```

---

# 六十三、Dispatcher 也不是 Validation

Validation 负责：

```text
参数是否合法
```

Dispatcher 负责：

```text
参数应该交给谁
```

当然最小 Demo 里我们可能顺手写：

```java
if (campaignId == null)
```

但架构职责上还是应该区分。

---

# 六十四、Dispatcher 也不是 Agent Loop

Agent Loop 是：

```text
模型
 ↓
Tool
 ↓
模型
 ↓
Tool
 ↓
模型
 ↓
Final Answer
```

Dispatcher 只负责其中一个很小的动作：

```text
执行某一次 Tool Call
```

所以：

```text
Dispatcher
```

是 Agent Runtime 的一个组成部分。

不是整个 Agent。

---

# 六十五、可以把未来 Agent Runtime 想成这样

```text
Agent Runtime
│
├── LLM Client
│
├── Message History
│
├── Tool Registry
│
├── Tool Dispatcher
│
├── Tool Validator
│
├── Tool Context
│
├── Loop Controller
│
└── Error Handler
```

现在我们只是在逐个拆开学习。

---

# 六十六、为什么这种拆解很重要？

如果你只用框架，很容易看到：

```text
Agent
```

然后脑中是一团：

```text
模型自己调用工具
```

但真正工程系统里：

```text
每一步都有明确责任主体
```

例如：

```text
LLM 决定
Application 执行
Dispatcher 分发
Validator 校验
Service 处理业务
Loop 决定继续还是结束
```

这就是 Agent 工程的底层骨架。

---

# 六十七、Tool Dispatcher 的最低完成标准

完成 Node 025 后，你至少应该能回答：

```text
1. Tool Dispatcher 是什么？

2. 为什么 LLM 不能直接执行 Java 方法？

3. Tool Name 是怎么映射到 Java Method 的？

4. Tool Definition / Tool Call / Dispatcher
   三者分别是什么？

5. Dispatcher 为什么本质上也是白名单？

6. 为什么不建议直接把模型返回的方法名用于任意反射调用？

7. arguments 为什么需要从 JSON 转成 Java 参数？

8. Unknown Tool 应该怎么办？

9. switch Dispatcher 和 Map Dispatcher 有什么区别？

10. 框架里的 @Tool 帮你隐藏了哪些事情？
```

---

# 六十八、你现在应该能画出这张图

```text
               Application
                    │
                    │ Tool Definitions
                    ▼
                   LLM
                    │
                    │ Tool Call
                    ▼
        ┌─────────────────────┐
        │   Tool Dispatcher   │
        └──────────┬──────────┘
                   │
       ┌───────────┼───────────┐
       │           │           │
       ▼           ▼           ▼
 queryCampaign  queryBudget  queryMetrics
       │           │           │
       ▼           ▼           ▼
   Service      Service      Service
```

---

# 六十九、最重要的一条调用链

本 Node 最重要的是：

```text
LLM
 ↓
Tool Call
 ↓
Application
 ↓
Tool Dispatcher
 ↓
Java Method
```

其中：

```text
LLM
```

负责：

```text
提出调用请求
```

而：

```text
Application
```

负责：

```text
决定如何真正执行
```

---

# 七十、Node 025 的一句话定义

> **Tool Dispatcher 是 Application 内部负责根据 Tool Call 中的 Tool Name 找到对应工具实现、解析参数并触发真实 Java 方法执行的分发层。**

---

# 七十一、最小实现模板

当前阶段你完全可以先记住这一版：

```java
public Object dispatch(
        String toolName,
        JSONObject arguments
) {

    return switch (toolName) {

        case "queryCampaign" -> {

            Long campaignId =
                arguments.getLong(
                    "campaignId"
                );

            if (campaignId == null) {

                throw new IllegalArgumentException(
                    "campaignId is required"
                );
            }

            yield queryCampaign(
                campaignId
            );
        }

        case "queryBudget" -> {

            Long campaignId =
                arguments.getLong(
                    "campaignId"
                );

            if (campaignId == null) {

                throw new IllegalArgumentException(
                    "campaignId is required"
                );
            }

            yield queryBudget(
                campaignId
            );
        }

        default ->
            throw new IllegalArgumentException(
                "Unknown tool: "
                    + toolName
            );
    };
}
```

这就是 Node 025 最核心的代码。

---

# 七十二、不要把这段代码想复杂了

它本质上就是：

```text
模型说：
“调用 queryCampaign”

程序说：
“queryCampaign 对应这个 Java 方法”

程序解析参数

程序执行方法
```

没有魔法。

---

# 七十三、为什么这个 Node 很重要？

因为从这里开始，你会真正意识到：

> **Tool Calling 不是“模型会调用代码”。**

而是：

> **模型输出结构化调用意图，Application 把这个意图解释成真实代码执行。**

这两个说法看起来很像。

但工程含义完全不同。

---

# 七十四、这会直接影响你的安全思维

如果你认为：

```text
模型直接调用代码
```

很容易产生：

```text
模型既然调用了，那就执行吧
```

但如果你理解：

```text
模型只是提交请求
```

你就会自然想到：

```text
校验
授权
白名单
审批
幂等
错误处理
```

而这些正是后续 Node 要学的东西。

---

# 七十五、练习 1：判断谁负责执行

用户：

```text
查询 campaign 123
```

模型返回：

```json
{
  "name": "queryCampaign",
  "arguments": {
    "campaignId": 123
  }
}
```

问题：

```text
真正调用 queryCampaign(123)
的是模型还是 Application？
```

答案：

```text
Application
```

---

# 七十六、练习 2：判断 Dispatcher 做什么

以下哪些属于 Dispatcher 的核心职责？

```text
A. 理解自然语言用户需求

B. 根据 toolName 找到对应 Tool

C. 训练模型

D. 把 arguments 解析成 Java 参数

E. 调用真实 Java 方法
```

答案：

```text
B
D
E
```

---

# 七十七、练习 3：判断未知 Tool

模型返回：

```text
toolName = resetDatabase
```

但 Registry 没有这个 Tool。

应该：

```text
A. 猜最接近的 Tool

B. 反射找同名方法

C. 明确返回 Unknown Tool

D. 让模型自己执行
```

答案：

```text
C
```

---

# 七十八、练习 4：判断 Tool Definition 和 Dispatcher 的关系

Tool Definition：

```text
name = queryCampaign
```

Dispatcher：

```java
case "getCampaign"
```

是否合理？

答案：

```text
不合理
```

因为：

```text
Tool Name 不一致
```

模型返回：

```text
queryCampaign
```

时 Dispatcher 找不到。

---

# 七十九、练习 5：为什么先学 switch？

因为：

```text
A. switch 永远是生产最佳实践

B. switch 最容易看清 Tool Name → Java Method 的底层映射

C. Java 只能用 switch

D. 框架底层一定只用 switch
```

答案：

```text
B
```

---

# 八十、自测问题 1

什么是 Tool Dispatcher？

参考回答：

> Tool Dispatcher 是 Application 内部的一层分发逻辑，它根据 LLM 返回的 Tool Call 中的 Tool Name，找到对应的 Tool Handler 或 Java Method，解析 arguments，并触发真实方法执行。

---

# 八十一、自测问题 2

为什么说 LLM 并没有真正调用 Java 方法？

参考回答：

> 因为 LLM 只能输出 Tool Call 请求，例如 Tool Name 和 arguments。真正读取这个请求、匹配方法并执行 Java 代码的是 Application。

---

# 八十二、自测问题 3

为什么 Tool Dispatcher 也是一种安全边界？

参考回答：

> 因为 Dispatcher 通常只允许执行明确注册的 Tool。模型即使生成其他 Tool Name，也不能自动获得任意 Java 方法的执行能力。

---

# 八十三、自测问题 4

为什么不建议直接使用模型返回的方法名进行任意反射调用？

参考回答：

> 因为模型输出是不可信输入。如果任意方法名都能通过反射执行，就可能让模型越过 Tool 白名单，调用不应该暴露的内部方法。

---

# 八十四、自测问题 5

Tool Schema 和 Tool Dispatcher 分别解决什么问题？

参考回答：

```text
Schema：
参数应该长什么样

Dispatcher：
这个 Tool Call 应该交给哪个 Java 方法执行
```

---

# 八十五、自测问题 6

为什么 arguments 通常还要解析？

因为模型返回的 arguments 通常是：

```text
JSON
```

而 Java 方法需要：

```text
Long
String
Enum
DTO
List
...
```

Application 需要完成：

```text
JSON → Java Type
```

---

# 八十六、自测问题 7

为什么一开始使用 `switch` 是合理的？

因为 Tool 数量很少时：

```text
简单
透明
容易调试
容易理解底层机制
```

学习阶段尤其适合。

---

# 八十七、自测问题 8

什么时候可能从 switch 演化成 Registry + Handler？

当：

```text
Tool 数量越来越多
Tool 执行逻辑越来越复杂
需要统一注册、扩展、测试
```

时。

---

# 八十八、自测问题 9

Dispatcher 执行成功后，结果应该直接作为最终回答返回给用户吗？

通常：

```text
不是
```

完整 Tool Calling 里一般会：

```text
Tool Result
    ↓
再次发送给模型
    ↓
模型生成最终自然语言回答
```

这属于 Node 026。

---

# 八十九、自测问题 10

Node 025 最终希望你真正理解什么？

不是：

```text
会写一个 switch
```

而是：

> **理解 Tool Call 到真实程序执行之间必须存在一个 Application-controlled execution layer。**

---

# 九十、本篇必须记住的 15 个结论

1. **LLM 不会真正执行 Java 方法。**

2. **LLM 只会返回 Tool Call 请求。**

3. **Tool Dispatcher 位于 Application 内部。**

4. **Dispatcher 根据 Tool Name 找到对应执行逻辑。**

5. **Tool Name 是 Definition、Tool Call 和 Dispatcher 之间的重要路由键。**

6. **Tool Definition 与 Dispatcher 中的 Tool Name 必须保持一致。**

7. **arguments 通常需要从 JSON 转换成 Java 参数。**

8. **模型输出应该被视为不可信外部输入。**

9. **未注册 Tool 应默认拒绝。**

10. **不应该让模型返回任意 Java 方法名然后直接反射执行。**

11. **简单场景使用 switch 完全合理。**

12. **Tool 较多时可演化成 Registry + Handler。**

13. **Dispatcher 不等于参数校验、权限系统或 Agent Loop。**

14. **框架的 @Tool 等机制本质上只是把注册、分发、参数转换和方法调用隐藏起来。**

15. **Node 025 是 Tool Calling 从“描述能力”进入“真实执行”的关键节点。**

---

# 九十一、完成标准

完成本 Node 后，你应该能够做到：

- [ ] 能解释 Tool Dispatcher 是什么。
- [ ] 能画出 `Tool Call → Dispatcher → Java Method` 的链路。
- [ ] 能区分 Tool Definition、Tool Call 和 Tool Dispatcher。
- [ ] 能解释为什么 LLM 不能直接执行 Java 方法。
- [ ] 能解释为什么 Tool Name 必须精确匹配。
- [ ] 能解释为什么 Dispatcher 也是 Tool 白名单的一部分。
- [ ] 能解释为什么不应该直接把模型输出作为任意反射调用的方法名。
- [ ] 能手写一个最简单的 `switch` Dispatcher。
- [ ] 能从 arguments 中取出 `campaignId` 并调用 Java 方法。
- [ ] 能处理 Unknown Tool。
- [ ] 能理解 switch 和 Registry + Handler 的演化关系。
- [ ] 能理解框架里的 Tool Invocation 本质上仍然需要类似 Dispatcher 的机制。

**达到以上标准，再进入 Node 026。**

---

# 九十二、本 Node 的停止点

现在不需要：

```text
接入真实 LLM Tool Call

完成第二次模型调用

实现完整 Agent Loop

写复杂反射框架

设计 Annotation Processor

设计通用 Plugin System

实现完整 Tool Validation

实现权限系统

实现 Human Approval

实现幂等性

实现 Tool Context
```

当前只需要真正掌握：

```text
模型返回 Tool Call
        ↓
Application 根据 toolName
找到并执行真正的 Java 方法
```

---

# 九十三、下一步：Node 026

下一步进入：

```text
Node 026：第一次完整 Tool Call
```

到时候我们会真正把：

```text
用户
 ↓
模型
 ↓
Tool Call
 ↓
Dispatcher
 ↓
Java Method
 ↓
Tool Result
 ↓
模型
 ↓
Final Answer
```

整条链路第一次完整跑通。

也就是说，Node 025 解决的是：

```text
“怎么执行”
```

Node 026 解决的是：

```text
“怎么把整个 Tool Calling 闭环跑起来”
```

---

# 本篇一句话总结

> **Tool Dispatcher 的本质，是 Application 对 LLM Tool Call 的执行解释器：它把模型返回的 Tool Name 和 arguments，映射成受控的、真实的 Java 方法调用。**

最重要的一条链：

```text
Tool Definition
      ↓
LLM
      ↓
Tool Call
      ↓
Application
      ↓
Tool Dispatcher
      ↓
Java Method
      ↓
Tool Result
```

其中最关键的思想不是：

```text
switch
```

而是：

```text
LLM 负责提出调用意图

Application 负责决定如何执行
```

**Node 025：Tool Dispatcher。**

---

## 学习记录

- 理论阅读日期：待填写
- 最小 Dispatcher Demo：待完成
- Unknown Tool 实验：待完成
- 缺少参数实验：待完成
- 第二个 Tool 实验：待完成
- 自测情况：待填写
- 尚未理解的问题：待填写
- 是否达到完成标准：待确认
