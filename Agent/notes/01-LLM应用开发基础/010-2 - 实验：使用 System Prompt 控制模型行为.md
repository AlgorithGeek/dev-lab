# 010-2 - 实验：使用 System Prompt 控制模型行为

> Phase 1：LLM 应用开发基础\
> Node 010：System Prompt\
> Note 010-2：System Prompt Java 实验篇
>
> 前置：
>
> ```text
> 010-1 - System Prompt 到底是什么？ ✅ 已读完
> ```
>
> 当前项目：
>
> ```text
> Agent/projects/agent-learning-lab
> ```
>
> 当前环境：
>
> ```text
> Java：21
> HTTP：JDK HttpClient
> JSON：FastJSON2
> Provider：阿里云百炼 Model Studio
> Model：qwen3.8-max
> API：OpenAI-compatible Chat Completions
> ```
>
> 本篇原则：
>
> ```text
> 先验证 System Message 真正进入 Request
> ↓
> 再观察模型行为差异
> ↓
> 再把 System 放进多轮对话
> ↓
> 最后验收 Node 010
> ```

---

> **当前记录（2026-11-02）：**两个阶段均已有用户提供的 IDEA 真实正常调用结果：A/B 为 [user] 对 [system,user]，多轮为 [system,user] 对 [system,user,assistant,user]，原始回答和 Token 已记录。多轮最终历史为四条 User/Assistant，System 每轮重新加入而未存入历史。新增本地模拟检查（包括错误路径）仍未运行，不能以正常调用成功代替；用户确认继续学习并要求维护进度，Node 010 阅读、代码学习及两阶段正常实测已收尾，下一步为 Node 011「Prompt 基础」。

# 一、这一篇到底要验证什么？

010-1 已经理解了：

```text
System Prompt
=
应用提供给模型的高层行为和任务指令
```

但理论知道 `role = system` 还不够。

我们这次需要亲眼验证三件事：

```text
第一件：
同一个 User 输入
有没有 System Message
Request 结构确实不同
```

```text
第二件：
System Prompt 进入 Context 后
模型的关注点、回答方式和约束遵守
可能发生变化
```

```text
第三件：
多轮 Chat Completions 中
System Message 不会因为第一轮发过就自动永久存在
应用需要在后续请求里继续携带
```

所以本篇不是研究怎样写“完美 Prompt”，也不是背几十种 Prompt Engineering 技巧，而是把 010-1 的理论变成真实 Java 实验。

---

# 二、这次不新建项目

继续使用：

```text
Agent/projects/agent-learning-lab
```

现有项目已经有：

```text
FirstModelCall.java
MessageHistoryDemo.java
```

分别承担：

```text
Node 008：单次模型调用
Node 009：Message / Chat History
```

这次建议新增：

```text
SystemPromptDemo.java
```

形成：

```text
agent-learning-lab/
└── src/main/java/com/avexvertex/agent/
    ├── FirstModelCall.java
    ├── MessageHistoryDemo.java
    └── SystemPromptDemo.java
```

不要把前两个实验入口重写掉。

> 学习代码也应该保留演化轨迹。

---

# 三、为什么仍然不用 Spring Boot？

因为 Node 010 的问题只有：

```text
System Message
```

我们真正要观察的是：

```text
messages
```

如何变化。

如果现在加入 Spring Boot、Controller、Service、Prompt Template、Spring AI，实验重点就会被淹没。

所以仍然坚持：

```text
JDK 21
+
HttpClient
+
FastJSON2
```

让 Request 本身保持透明。

---

# 四、为什么不直接改 MessageHistoryDemo？

技术上当然可以，但学习上不推荐。

`MessageHistoryDemo` 已经代表 Node 009 的最终状态：

```text
应用维护 User / Assistant History
```

如果继续往里面塞 System A/B、System 多轮、各种实验模式，最终会变成一个类承担所有学习节点。

所以本篇新增：

```text
SystemPromptDemo
```

但也不要为了“代码复用”提前设计：

```text
ModelClient
ProviderFactory
BaseModelDemo
PromptService
```

这些抽象以后真正出现重复需求时再做。

---

# 五、为什么继续做 A/B？

如果我们只运行：

```text
System + User
↓
模型回答
```

然后看到模型正常工作，我们最多只能证明代码没报错，却很难知道 System Prompt 到底产生了什么影响。

更好的实验是：

```text
同一个 Model
同一个 User
同一个 reasoning_effort
同一个 Endpoint
        │
        ├── A：User
        │
        └── B：System + User
```

尽可能只改变：

```text
System Message
```

这和 Node 009 的“无历史 / 有历史”实验方法完全一致。

---

# 六、A/B 不能只看“回答像不像”

LLM 是概率模型。

即使 A 没有 System，模型也可能碰巧回答得很专业、结构也很好；反过来，B 有 System 也不意味着每一条要求都 100% 严格执行。

所以实验成功不能定义为：

```text
A 一定很差
B 一定完美
```

真正成功标准分两层。

## 第一层：确定性证据

Java 真正发出的 Request 必须不同：

```text
A：
[user]

B：
[system, user]
```

System 的真实文本必须出现在 B 的 Request 中。

## 第二层：模型行为观察

记录：

```text
回答关注点
回答结构
是否遵守主要约束
Prompt Tokens
```

然后分析 System 是否对行为产生了可观察影响。

---

# 七、第一阶段使用什么 System Prompt？

继续沿用 010-1 的 Java Code Review Assistant，但不要写得太复杂。

建议：

```text
你是一名 Java Code Review Assistant。

你的任务是审查用户提供的 Java 代码。

规则：
1. 先给一句总体结论。
2. 最多指出 3 个最重要的问题。
3. 优先关注正确性、空指针风险、并发安全和资源管理。
4. 只分析用户实际提供的代码；如果信息不足，明确说明，不要虚构不存在的上下文。
5. 使用中文回答。
```

这不是为了证明它是“最佳 Prompt”，只是因为它有几个容易观察的要求：

```text
身份
任务
关注点
最多 3 项
先给结论
不虚构
中文
```

---

# 八、User Prompt 用什么？

A / B 必须使用完全相同的 User Message：

```text
请帮我 Review 下面这段代码：

public String getUserName(User user) {
    return user.getProfile().getName();
}
```

为什么选它？

代码很短，不会把实验变成 Java 面试题；同时存在明显可讨论的空指针风险，但没有数据库、多线程、资源等额外上下文。

这样正好可以观察 System 中“只分析实际提供代码”是否减少无依据扩展。

---

# 九、Checkpoint 0：确认仓库状态

# 🛑 停下来操作

在 `dev-lab` 根目录执行：

```bash
git status
git pull origin main
```

确认本地已包含：

```text
Agent/notes/01-LLM应用开发基础/
├── 010-1 - System Prompt 到底是什么？.md
└── 010-2 - 实验：使用 System Prompt 控制模型行为.md
```

同时 `Agent/projects/agent-learning-lab/` 继续包含：

```text
FirstModelCall.java
MessageHistoryDemo.java
```

## Checkpoint 0 成功标准

- [x] 本地 main 已同步（开始本节实验前已同步；本次仅维护文档，未重新拉取远端）；
- [x] 没有不认识的未提交修改；
- [x] 010-1 已存在；
- [x] 010-2 已存在；
- [x] Node 009 的 Java 代码仍存在。

---

# 十、Checkpoint 1：先看懂 A/B Request

## A：没有 System Prompt

```json
{
  "model": "qwen3.8-max",
  "messages": [
    {
      "role": "user",
      "content": "请帮我 Review 下面这段代码：..."
    }
  ],
  "reasoning_effort": "none"
}
```

角色顺序：

```text
[user]
```

## B：加入 System Prompt

```json
{
  "model": "qwen3.8-max",
  "messages": [
    {
      "role": "system",
      "content": "你是一名 Java Code Review Assistant。..."
    },
    {
      "role": "user",
      "content": "请帮我 Review 下面这段代码：..."
    }
  ],
  "reasoning_effort": "none"
}
```

角色顺序：

```text
[system, user]
```

A / B 的 User、Model、Endpoint、reasoning_effort 都保持一致。

唯一主要变量：

```text
是否带 System Message
```

---

# 十一、为什么 System 放在 messages 第一位？

我们当前使用：

```text
阿里云百炼
OpenAI-compatible Chat Completions
```

当前官方文档说明 System Message 用于设置角色、语气、任务目标或约束条件等，一般位于 `messages` 第一位。

当前项目就采用最清晰的：

```text
System
↓
User
```

不要在这个 Node 实验多个 System、中途插入 System 或奇怪 Role 顺序。

---

# 十二、第一轮 Codex 协作目标

这一轮只负责：

> 新增 `SystemPromptDemo`，完成无 System / 有 System 的可比 Request。

不要一口气继续做多轮。

---

# 十三、Codex Prompt 010-2-1

# 🛑 停下来操作

把下面 Prompt 发给 Codex：

```text
你现在在我的 dev-lab 学习仓库中工作。

当前学习位置：
Phase 1 / Node 010 - System Prompt

010-1 理论篇已经读完，现在开始：
010-2 - 实验：使用 System Prompt 控制模型行为

请先阅读：
- Agent/README.md
- Agent/progress.md
- Agent/roadmap/Agent Engineer 循序渐进学习路线.md
- Agent/notes/01-LLM应用开发基础/010-1 - System Prompt 到底是什么？.md
- Agent/notes/01-LLM应用开发基础/010-2 - 实验：使用 System Prompt 控制模型行为.md
- Agent/projects/agent-learning-lab/pom.xml
- Agent/projects/agent-learning-lab/src/main/java/com/avexvertex/agent/FirstModelCall.java
- Agent/projects/agent-learning-lab/src/main/java/com/avexvertex/agent/MessageHistoryDemo.java
- Agent/codex/handoffs/009-2-2 - 自动维护 Chat History.md

这是学习实验，请严格控制范围。

一、本轮目标
只完成 010-2 第一阶段：
“同一模型、同一 User Message、同一参数下，对比无 System Message 与有 System Message 的真实 Request 和 Response。”
不要继续做多轮 System 实验。

二、保留现有代码
不要破坏 FirstModelCall.java 和 MessageHistoryDemo.java。
新增：
Agent/projects/agent-learning-lab/src/main/java/com/avexvertex/agent/SystemPromptDemo.java
不要重新创建 Maven 项目，不要重新 git init。

三、继续使用当前技术
- Java 21
- JDK HttpClient
- FastJSON2
- DASHSCOPE_API_KEY
- DASHSCOPE_BASE_URL
- qwen3.8-max
- OpenAI-compatible Chat Completions
- reasoning_effort = none
不要引入新框架或 SDK。

四、System Prompt
使用：
你是一名 Java Code Review Assistant。

你的任务是审查用户提供的 Java 代码。

规则：
1. 先给一句总体结论。
2. 最多指出 3 个最重要的问题。
3. 优先关注正确性、空指针风险、并发安全和资源管理。
4. 只分析用户实际提供的代码；如果信息不足，明确说明，不要虚构不存在的上下文。
5. 使用中文回答。

不要自行扩写成复杂 Prompt Engineering 模板。

五、User Message
A 和 B 必须使用完全相同的 User Message：

请帮我 Review 下面这段代码：

public String getUserName(User user) {
    return user.getProfile().getName();
}

六、A/B 请求
A：messages 只有 user。
B：messages 严格为 system, user。

A / B 必须保持：
- model 相同
- User Message 相同
- reasoning_effort 相同
- Endpoint 相同

不要把 A 的回答放进 B。A / B 是两个独立请求，不是多轮历史。

七、输出
每次请求至少输出：
- 实验标签
- Messages JSON
- Roles
- 消息条数
- HTTP Status
- Model
- Content
- Finish Reason
- Prompt Tokens
- Completion Tokens
- Total Tokens

绝对不要输出 API Key、Authorization Header 或真实 Secret。

八、错误处理
沿用当前项目已经验证过的最小错误处理：
- 非 2xx 不解析成功字段
- JSON 无效则明确失败
- model / choices / content / finish_reason / usage 缺失则明确失败
- 不自动重试
- 不伪造模型回答

九、本地验证
扩展 check-config.py 或使用同等清晰方式验证：
1. Maven Java 21 编译通过；
2. FirstModelCall 原入口不受影响；
3. MessageHistoryDemo 原入口不受影响；
4. A roles 为 [user]；
5. B roles 为 [system,user]；
6. A / B User content 完全一致；
7. B 第一条 System content 与实验 Prompt 一致；
8. model / reasoning_effort 一致；
9. 非 2xx 不解析成功字段；
10. 输出不泄露模拟 Secret。

本地模拟结果不能冒充真实百炼结果。

十、Codex 学习记录
保存：
Agent/codex/prompts/010-2-1 - System Prompt 行为对照.md

生成：
Agent/codex/handoffs/010-2-1 - System Prompt 行为对照.md

Handoff 至少记录：
1. 修改了哪些文件；
2. SystemPromptDemo 的入口；
3. A / B 的 Request 结构；
4. System Prompt 在 Java 中怎样构造；
5. 为什么 A / B 不是多轮对话；
6. 输出了哪些观察字段；
7. Maven 编译结果；
8. 本地模拟验证结果；
9. 是否真实调用百炼；
10. 当前还没有实现哪些后续能力；
11. 建议我回来重点看哪些代码。

十一、严格范围控制
本轮不要实现：
- System 多轮对话
- Prompt Template
- Spring Boot
- Spring AI
- LangChain4j
- OpenAI Java SDK
- Structured Output
- JSON Schema
- Tool Calling
- Agent Loop
- RAG
- Memory
- ProviderFactory
- ModelRouter

不要修改 progress.md 为 Node 010 完成。
不要进入 Node 011。
不要自行 commit / push。

完成 A/B 第一阶段后停止，等我在 IDEA 中真实运行并阅读代码。
```

---

# 十四、Codex 第一轮完成后先看什么？

不要立刻只点 Run。

先看：

```text
SystemPromptDemo.java
```

重点找：

```text
systemPrompt
userPrompt
```

然后找 A 对应的消息数组，应该只看到：

```text
user
```

再找 B 对应的消息数组，应该看到：

```text
system
user
```

---

# 十五、现在应该能解释一个关键问题

为什么 System Prompt 不是 Java 里的某种特殊 HTTP Header？

因为在当前 Chat Completions 中：

```text
System Prompt
↓
System Message
↓
messages 数组
↓
JSON Request Body
```

它仍然属于：

```text
模型输入 Context
```

而不是：

```text
Authorization Header
Content-Type
HTTP Method
```

---

# 十六、IDEA 真实运行 A/B

# 🛑 停下来操作

根据 Codex Handoff 运行：

```text
SystemPromptDemo
```

继续使用你本地已有的：

```text
DASHSCOPE_API_KEY
DASHSCOPE_BASE_URL
```

不要把真实值发给 Codex 或写进 Git。

正常情况下应该发出：

```text
2 次真实模型请求
```

可能产生实际 API 用量。

---

# 十七、第一阶段实际运行记录

2026-11-02，用户提供 IDEA 运行 SystemPromptDemo 的完整控制台输出。以下均为真实百炼结果，模型为 qwen3.8-max。

## A（无 System）

HTTP Status：200；Roles：`["user"]`；消息数：1；Finish Reason：stop。

实际发送的 Messages：

```json
[
  {
    "role": "user",
    "content": "请帮我 Review 下面这段代码：\n\npublic String getUserName(User user) {\n    return user.getProfile().getName();\n}"
  }
]
```

模型实际 Content（保留原文，不将模型建议直接视为代码事实）：

````text
# 代码 Review：`getUserName` 方法

这段代码虽然只有一行，但在生产环境中存在几个值得关注的隐患。下面从 **缺陷、设计、改进** 三个维度进行 Review。

---

## 1. 核心问题：空指针异常（NPE）风险

这是最严重的问题。这行代码形成了一个**调用链（Law of Demeter / 迪米特法则违背）**，链条上任何一个节点为 `null` 都会抛出 `NullPointerException`：

```java
user.getProfile().getName();
// ^^^^ 可能为 null
//      ^^^^^^^^^^^ 可能返回 null
```

**风险点：**
* `user` 本身为 `null`
* `user.getProfile()` 返回 `null`（例如用户尚未完善资料）

---

## 2. 改进方案

根据你使用的 Java 版本和项目规范，有以下几种修复方式：

### 方案一：传统判空（推荐用于 Java 8 以下或简单逻辑）
```java
public String getUserName(User user) {
    if (user == null || user.getProfile() == null) {
        return null; // 或者返回默认值 "" / "Unknown"
    }
    return user.getProfile().getName();
}
```

### 方案二：使用 `Optional`（Java 8+ 推荐）
利用 `Optional` 优雅地处理深层调用链的判空：
```java
import java.util.Optional;

public String getUserName(User user) {
    return Optional.ofNullable(user)
            .map(User::getProfile)
            .map(Profile::getName)
            .orElse(null); // 也可以 orElse("DefaultName") 或 orElseThrow(...)
}
```
**优点**：代码紧凑，语义清晰，避免了嵌套的 `if-else`。

### 方案三：防御性编程 + 异常处理（适用于业务要求严格的场景）
如果 `user` 或 `profile` 为 `null` 属于**非法的业务状态**，应该尽早抛出明确的异常，而不是默默返回 `null`：
```java
public String getUserName(User user) {
    if (user == null) {
        throw new IllegalArgumentException("User cannot be null");
    }
    Profile profile = user.getProfile();
    if (profile == null) {
        throw new IllegalStateException("User profile is not initialized for user: " + user.getId());
    }
    return profile.getName();
}
```

---

## 3. 架构与设计层面的建议

### ① 违反迪米特法则（最少知识原则）
当前代码中，外层类需要知道 `User` 内部有 `Profile`，且 `Profile` 里有 `Name`。这导致了强耦合。
**更好的做法**是在 `User` 类中直接提供获取名字的方法，将逻辑封装在内部：

```java
// 在 User 类中添加委托方法
public class User {
    private Profile profile;

    public String getName() {
        return profile != null ? profile.getName() : null;
    }
}

// 外部调用变得极其简单且安全
public String getUserName(User user) {
    return user != null ? user.getName() : null;
}
```

### ② 返回值契约不明确
方法的签名是 `String`，但调用者不知道当没有名字时，该方法会返回 `null`、空字符串 `""` 还是抛出异常。
**建议**：在 JavaDoc 中明确说明，或者使用 `Optional<String>` 作为返回值：
```java
/**
 * 获取用户名。
 * @param user 目标用户，不可为 null
 * @return 用户名；如果未设置 profile 则返回 empty
 */
public Optional<String> getUserName(User user) { ... }
```

---

## 总结 Checklist

| 检查项 | 状态 | 说明 |
| :--- | :---: | :--- |
| **Null Safety** | ❌ | 存在两处潜在的 NPE 风险 |
| **迪米特法则** | ⚠️ | 链式调用过长，耦合度高 |
| **异常处理** | ❌ | 失败时没有明确的降级策略或异常提示 |
| **API 文档** | ❌ | 缺少 JavaDoc 说明边界条件 |

**最终建议**：优先采用 **方案二（Optional）** 解决眼前的 NPE 问题；长远来看，建议采用 **架构建议①**，将 `getName()` 的逻辑下沉到 `User` 实体内部，保持外部调用的简洁。
````

Prompt Tokens：40；Completion Tokens：1001；Total Tokens：1041。

## B（有 System）

HTTP Status：200；Roles：`["system","user"]`；消息数：2；Finish Reason：stop。

实际发送的 Messages：

```json
[
  {
    "role": "system",
    "content": "你是一名 Java Code Review Assistant。\n\n你的任务是审查用户提供的 Java 代码。\n\n规则：\n1. 先给一句总体结论。\n2. 最多指出 3 个最重要的问题。\n3. 优先关注正确性、空指针风险、并发安全和资源管理。\n4. 只分析用户实际提供的代码；如果信息不足，明确说明，不要虚构不存在的上下文。\n5. 使用中文回答。"
  },
  {
    "role": "user",
    "content": "请帮我 Review 下面这段代码：\n\npublic String getUserName(User user) {\n    return user.getProfile().getName();\n}"
  }
]
```

模型实际 Content（保留原文，不将模型建议直接视为代码事实）：

````text
## 总体结论

这段代码逻辑直观，但存在严重的**空指针异常（NullPointerException）风险**，在生产环境中极不安全。

---

## 最重要的问题

### 1. `user` 参数可能为 null（正确性 / 空指针风险）
如果调用方传入的 `user` 为 `null`，执行 `user.getProfile()` 时会直接抛出 `NullPointerException`。

### 2. `getProfile()` 返回值可能为 null（正确性 / 空指针风险）
即使 `user` 不为 null，`user.getProfile()` 也可能返回 `null`（例如用户尚未创建个人资料），此时继续调用 `.getName()` 同样会抛出 `NullPointerException`。

**建议修复方式**：使用 `java.util.Optional` 进行安全链式调用，或者显式判空：
```java
// 方式一：使用 Optional（推荐）
public String getUserName(User user) {
    return Optional.ofNullable(user)
            .map(User::getProfile)
            .map(Profile::getName)
            .orElse(null); // 或 orElse("默认用户名")
}

// 方式二：显式判空
public String getUserName(User user) {
    if (user == null || user.getProfile() == null) {
        return null; // 或抛出自定义异常、返回默认值
    }
    return user.getProfile().getName();
}
```

### 3. 信息不足：缺少上下文约定
仅凭当前代码片段，无法判断 `User` 和 `Profile` 类的契约（Contract）。如果业务上严格要求 `user` 和 `profile` 绝对不可能为 null，建议在方法开头添加 `Objects.requireNonNull(user, "user cannot be null")` 来快速失败（Fail-fast），而不是让 NPE 在后续调用链中难以定位。
````

Prompt Tokens：138；Completion Tokens：398；Total Tokens：536。

---

# 十八、不要只问“谁回答得更好”

我们要观察：

### 1. Request 结构

```text
A：[user]
B：[system,user]
```

这是最确定的证据。

### 2. 角色定位

B 是否更明显以 Java Code Review Assistant 的视角回答？

### 3. 关注点

B 是否更聚焦正确性和空指针，而不是无根据扩展数据库、缓存、微服务、线程池等？

### 4. 回答结构

B 是否先给结论，并最多列 3 个主要问题？

### 5. 信息边界

B 是否减少虚构不存在上下文？

### 6. Token

B 因为多了一段 System Prompt，`prompt_tokens` 是否高于 A？

---

# 十九、为什么 B 的 Prompt Tokens 通常更高？

因为 B 的 Request 是：

```text
System
+
User
```

而 A 是：

```text
User
```

System Prompt 也是模型输入，因此会占用 Input Token。

注意：

```text
Prompt Tokens 更高
```

只能说明输入更多，不能单独证明回答质量更好。

---

# 二十、如果 A 也完全符合 System 规则怎么办？

完全可能。

没有 System 的 A 也可能碰巧先给结论、只列两个问题、只讲空指针。

这并不说明 System Prompt 没有意义，因为模型默认行为、User Prompt、训练形成的能力和随机性都可能让 A 表现相似。

本实验真正验证的是：

```text
B 的 Request 确实包含高层行为指令
```

以及：

```text
真实样本中观察到什么差异
```

真正判断 Prompt 是否稳定有效，需要多个 Case、Evaluation 和 Regression，这不是 Node 010 要一次解决的问题。

---

# 二十一、如果 B 没完全遵守规则怎么办？

同样不代表实验失败。

例如 System 要求“最多 3 个问题”，模型却列了 4 个。

这反而是一个重要观察：

```text
System Prompt
≠
程序 if
≠
强制 Schema
```

把实际结果记录下来。

不要为了让结果“漂亮”而重复调用直到符合预期，也不要修改模型原始回答。

---

# 二十二、第一阶段实验记录

2026-11-02，依据用户提供的真实输出填写；完整原始回答见第十七节。

## Request 对照

| 项目 | A：无 System | B：有 System |
|---|---|---|
| Model | qwen3.8-max | qwen3.8-max |
| reasoning_effort | none | none |
| User Message | 相同 | 相同 |
| Roles | `["user"]` | `["system","user"]` |
| Message Count | 1 | 2 |
| System Message | 无 | 有，第一条为完整的五项规则 |

## Response 对照

| 项目 | A：无 System | B：有 System |
|---|---|---|
| HTTP Status | 200 | 200 |
| Finish Reason | stop | stop |
| Prompt Tokens | 40 | 138 |
| Completion Tokens | 1001 | 398 |
| Total Tokens | 1041 | 536 |

## 行为观察

以下是基于本次输出的观察，未另行进行口述测验。

- A：讨论空指针，展开三种修复方式、迪米特法则、返回值契约及总结表，回答较长。
- B：先写“总体结论”，列出三个主要问题：user 可能为 null、getProfile() 可能返回 null、缺少上下文契约；给出 Optional 与显式判空示例。
- B 本次体现了审查角色、中文、先给结论及最多三项的要求，内容比 A 更集中，但仍附带修改代码。
- B 没有无依据扩展到数据库、缓存或线程池；它也明确指出缺少类的契约。
- B 仍有需要审慎看待的表述：“在生产环境中极不安全”缺少实际非空契约支撑。示例中的 Profile 类型及“尚未创建个人资料”是建议或假设，不是已确认的项目事实。
- A 将链式调用直接判为违反迪米特法则、把某种返回策略作为推荐，也不能只凭这个代码片段就视为必然结论。
- 确定性证据是 B 的请求多了一条 System，两个请求 User 逐字相同，且没有把 A 回答传入 B。
- 本样本中，B 输入 Token 比 A 多 98（138 - 40），输出少 603（1001 - 398）。这不代表所有任务加 System 都能减少输出或保证规则稳定生效，也不是账单计算。

---

# 二十三、Checkpoint 1 成功标准

- [x] 新增 `SystemPromptDemo.java`；
- [x] `FirstModelCall` 未被破坏；
- [x] `MessageHistoryDemo` 未被破坏；
- [x] A 实际发送 `[user]`；
- [x] B 实际发送 `[system,user]`；
- [x] A / B User 完全一致；
- [x] A / B Model 和参数一致；
- [x] B System 文本真实进入 Request；
- [x] IDEA 真实调用 A / B 都成功；
- [x] 两边真实回答已保存；
- [x] Token Usage 已保存；
- [x] 不用单次回答“是否完美”冒充确定性结论；
- [x] 第一份 Codex Prompt / Handoff 已生成。

满足后：

```text
010-2 第一阶段
✅
```

但 Node 010 还没结束。

---

# 二十四、第二阶段：System Prompt + 多轮对话

现在把：

```text
Node 009
Message / Chat History
```

和：

```text
Node 010
System Prompt
```

真正连接起来。

目标结构：

```text
systemPrompt
+
chatHistory
+
currentUser
↓
messages
↓
Model
↓
Assistant
↓
保存进 chatHistory
```

---

# 二十五、最重要的数据结构边界

建议 Java 内部不要把 System Prompt 和 Chat History 混成一个概念。

可以形成：

```text
String systemPrompt
```

负责高层行为规则。

另外：

```text
JSONArray history
```

只保存：

```text
User
Assistant
User
Assistant
```

每轮请求临时构造：

```text
messages = []

加入 System
↓
加入 history
↓
加入 Current User
↓
发送
```

成功后：

```text
history
+
Current User
+
Assistant Response
```

System 仍单独维护。

---

# 二十六、为什么不直接把 System 存进 history？

技术上可以维护完整 messages，但当前教学更推荐分开。

因为我们刚学过：

```text
System Prompt
≠
Chat History
```

变量也分开：

```text
systemPrompt
chatHistory
```

更容易形成概念边界。

每次发请求时再组合，也是以后 Context Engineering 的雏形。

---

# 二十七、第二阶段用什么多轮问题？

System：

```text
你是一名 Java Code Review Assistant。
回答每个问题时，第一行必须以“JAVA-REVIEW:”开头。
只讨论 Java 代码或 Java 后端工程问题。
使用中文回答。
```

第一轮 User：

```text
ThreadLocal 最常见的风险是什么？请用一句话回答。
```

第二轮 User：

```text
那在线程池里为什么更需要小心？仍然用一句话回答。
```

这里不追求 Prompt 设计质量，只需要一个明显、容易观察的行为标记：

```text
JAVA-REVIEW:
```

以及多轮 Context。

---

# 二十八、第一轮 Request 应是什么？

```text
System
User 1
```

Roles：

```text
[system, user]
```

---

# 二十九、第二轮 Request 应是什么？

第一轮成功后保存：

```text
User 1
Assistant 1
```

第二轮构造：

```text
System
User 1
Assistant 1
User 2
```

Roles：

```text
[system, user, assistant, user]
```

System 仍然在第一位。

---

# 三十、System 为什么不是“只在第一轮发一次”？

因为我们当前使用 Chat Completions，每次 HTTP Request 都要构造本轮 `messages`。

模型不会因为上一轮发过 System，就自动把它永久放进下一轮。

因此第二轮 System 仍然需要由 Java 再放进去。

这和 Node 009 的历史重传是同一个底层逻辑：

> 模型能利用什么，取决于这一轮 Context 里真正有什么。

---

# 三十一、Codex Prompt 010-2-2

# 🛑 停下来操作

只有第一阶段真实 A/B 跑通并理解后，再发下面 Prompt：

```text
继续在当前 dev-lab 仓库中工作。

前置：
010-2 第一阶段已经完成，SystemPromptDemo 能对比 A（无 System）和 B（有 System）。

现在只完成第二阶段：
“验证 System Message 在多轮 Chat Completions 中由应用每轮继续携带。”

请先阅读：
- Agent/notes/01-LLM应用开发基础/010-1 - System Prompt 到底是什么？.md
- Agent/notes/01-LLM应用开发基础/010-2 - 实验：使用 System Prompt 控制模型行为.md
- Agent/projects/agent-learning-lab/src/main/java/com/avexvertex/agent/SystemPromptDemo.java
- Agent/projects/agent-learning-lab/src/main/java/com/avexvertex/agent/MessageHistoryDemo.java
- Agent/codex/handoffs/010-2-1 - System Prompt 行为对照.md

一、目标
在 SystemPromptDemo 现有第一阶段 A/B 功能基础上，增加独立的多轮 System 实验模式。
建议程序参数：--history
不带参数时保留原 A/B 对照。
不要删除第一阶段功能。

二、System Prompt 与 Chat History 分开
代码中保持：

systemPrompt = 高层行为指令
history = 只保存成功的 user / assistant 对话历史

每轮真正发送时：
messages = system + history + current user

不要把 System Message 当成普通历史回答保存进 history。

三、多轮实验 System Prompt
使用：

你是一名 Java Code Review Assistant。
回答每个问题时，第一行必须以“JAVA-REVIEW:”开头。
只讨论 Java 代码或 Java 后端工程问题。
使用中文回答。

不要扩写成复杂 Prompt 模板。

四、两轮 User
第 1 轮：
ThreadLocal 最常见的风险是什么？请用一句话回答。

第 2 轮：
那在线程池里为什么更需要小心？仍然用一句话回答。

五、请求结构
第 1 轮 roles：
[system,user]

第 1 轮成功后，history 只保存：
[user,assistant]

第 2 轮 roles：
[system,user,assistant,user]

第 2 轮成功后，history 只保存：
[user,assistant,user,assistant]

System Prompt 每轮重新加入 Request，但不算入 chatHistory 的 user / assistant 历史条数。

六、每轮输出
至少输出：
- Round
- Messages JSON
- Roles
- Request Message Count（包含 System）
- Chat History Count（不包含 System）
- HTTP Status
- Model
- Content
- Finish Reason
- Prompt Tokens
- Completion Tokens
- Total Tokens

需要能从控制台直接看到：
第 1 轮 [system,user]
第 2 轮 [system,user,assistant,user]

七、失败语义
只有本轮 HTTP 和必要 Response 字段全部成功以后，才把本轮 User + Assistant 写入主 history。

如果第 2 轮失败：
- 保留第一轮已经成功的 history；
- 不伪造第二轮 Assistant；
- 不把失败轮误记成已完成；
- 明确停止。

System Prompt 始终是应用配置，不因失败写入 chatHistory。

八、本地验证
扩展 check-config.py 或等价检查，至少验证：
1. Java 21 Maven 编译通过；
2. 原 A/B 模式仍正常；
3. --history 第 1 轮 roles 为 [system,user]；
4. --history 第 2 轮 roles 为 [system,user,assistant,user]；
5. 两轮第一条 System content 完全一致；
6. 第 2 轮 Assistant 历史来自第 1 轮响应，而不是写死；
7. history 成功后条数为 2 / 4；
8. System 没有被算进 history；
9. 第 1 轮失败时 history=0；
10. 第 2 轮失败时保留 history=2；
11. 非 2xx 不解析成功字段；
12. Secret 不泄露。

本地模拟结果不能冒充真实百炼结果。

九、记录
保存：
Agent/codex/prompts/010-2-2 - 多轮携带 System Message.md

生成：
Agent/codex/handoffs/010-2-2 - 多轮携带 System Message.md

Handoff 至少记录：
1. 修改文件；
2. 两种运行模式；
3. systemPrompt 与 history 分别存在哪里；
4. 每轮 Request 怎样组合；
5. 为什么 System 每轮重新加入；
6. 为什么 System 不计入本实验的 Chat History；
7. 两轮 roles 与消息数；
8. 失败时 history 怎样处理；
9. Maven / 本地模拟结果；
10. 是否真实调用百炼；
11. 当前仍未实现的内容。

十、范围控制
仍然不要引入：
- Prompt Template
- Prompt Version Registry
- Spring Boot
- Spring AI
- LangChain4j
- Structured Output
- JSON Schema
- Tool Calling
- Agent Loop
- RAG
- Memory Framework
- Provider abstraction

不要进入 Node 011。
不要把 progress.md 提前标成 Node 010 完成。
不要自行 Git commit / push。

完成代码、本地验证和 Handoff 后停止，等我在 IDEA 中真实运行并完成理解验收。
```

---

# 三十二、第二阶段 Codex 完成后先读数据流

这一次你最应该找的不是 HTTP Client，因为那部分已经学过了。

要找的是：

```text
systemPrompt
history
currentUser
```

以及它们怎样组成：

```text
messages
```

---

# 三十三、你应该能画出这条数据流

```text
               systemPrompt
                    │
                    ▼
             ┌────────────┐
             │  System    │
             └────────────┘
                    │
                    │
history ────────────┼──────────── currentUser
                    │
                    ▼
                 messages
                    │
                    ▼
                 Request
                    │
                    ▼
                  Model
                    │
                    ▼
                Assistant
                    │
                    ▼
       history += User + Assistant
```

下一轮重新：

```text
System
+
更新后的 History
+
新 User
```

---

# 三十四、IDEA 真实运行第二阶段

# 🛑 停下来操作

根据 Codex 实际 Handoff，使用：

```text
SystemPromptDemo --history
```

或 Codex 最终定义的等价参数运行。

正常情况：

```text
真实调用百炼 2 次
```

---

# 三十五、第二阶段真实记录

2026-11-02，用户提供 SystemPromptDemo --history 的实际输出。两轮均为 qwen3.8-max、HTTP 200、finish_reason=stop，以下不是本地模拟数据。模型回答按原文保存，本节主要记录请求结构和行为观察。

## 第 1 轮

```text
Roles：["system", "user"]
Request Message Count：2
History Count before：0（依据代码的空数组初始化；用户本段日志未展示首轮 before 行）
HTTP Status：200
Model：qwen3.8-max
Finish Reason：stop
Prompt Tokens：70
Completion Tokens：36
Total Tokens：106
History Count after：2
```

实际 Messages：

```json
[
  {
    "role": "system",
    "content": "你是一名 Java Code Review Assistant。\n回答每个问题时，第一行必须以“JAVA-REVIEW:”开头。\n只讨论 Java 代码或 Java 后端工程问题。\n使用中文回答。"
  },
  {
    "role": "user",
    "content": "ThreadLocal 最常见的风险是什么？请用一句话回答。"
  }
]
```

实际 Content：

````text
JAVA-REVIEW: ThreadLocal 最常见的风险是在线程池环境下使用后未及时调用 `remove()` 清理，从而导致内存泄漏以及线程复用时产生的数据串扰问题。
````

## 第 2 轮

```text
Roles：["system", "user", "assistant", "user"]
Request Message Count：4
History Count before：2（实际日志）
HTTP Status：200
Model：qwen3.8-max
Finish Reason：stop
Prompt Tokens：134
Completion Tokens：60
Total Tokens：194
History Count after：4
```

实际 Messages：

```json
[
  {
    "role": "system",
    "content": "你是一名 Java Code Review Assistant。\n回答每个问题时，第一行必须以“JAVA-REVIEW:”开头。\n只讨论 Java 代码或 Java 后端工程问题。\n使用中文回答。"
  },
  {
    "role": "user",
    "content": "ThreadLocal 最常见的风险是什么？请用一句话回答。"
  },
  {
    "role": "assistant",
    "content": "JAVA-REVIEW: ThreadLocal 最常见的风险是在线程池环境下使用后未及时调用 `remove()` 清理，从而导致内存泄漏以及线程复用时产生的数据串扰问题。"
  },
  {
    "role": "user",
    "content": "那在线程池里为什么更需要小心？仍然用一句话回答。"
  }
]
```

实际 Content：

````text
JAVA-REVIEW: 因为线程池中的线程会被反复复用而不会销毁，若使用后未调用 `remove()` 清理，ThreadLocalMap 中的 Entry 会随线程一直存活，不仅造成内存泄漏，还会导致后续任务读取到前一个任务残留的脏数据。
````

## 最终已保存历史

实际为 4 条，仅包含 User / Assistant：

```json
[
  {
    "role": "user",
    "content": "ThreadLocal 最常见的风险是什么？请用一句话回答。"
  },
  {
    "role": "assistant",
    "content": "JAVA-REVIEW: ThreadLocal 最常见的风险是在线程池环境下使用后未及时调用 `remove()` 清理，从而导致内存泄漏以及线程复用时产生的数据串扰问题。"
  },
  {
    "role": "user",
    "content": "那在线程池里为什么更需要小心？仍然用一句话回答。"
  },
  {
    "role": "assistant",
    "content": "JAVA-REVIEW: 因为线程池中的线程会被反复复用而不会销毁，若使用后未调用 `remove()` 清理，ThreadLocalMap 中的 Entry 会随线程一直存活，不仅造成内存泄漏，还会导致后续任务读取到前一个任务残留的脏数据。"
  }
]
```

## 本次观察

- 第二轮请求的 Assistant 与第一轮实际 Content 一致；两轮 System 文本一致，均在第一位。
- 请求条数为 2 / 4，成功后的 Chat History 为 2 / 4；后者不包含 System。
- 本次两轮均以 JAVA-REVIEW: 开头，并用一句话接续 ThreadLocal / 线程池话题；Java 未自动添加前缀。
- 输入 Token 从 70 增到 134，第二轮输入增加了真实问答历史和追问；这是输入变化的记录，不是账单计算或稳定遵守规则的保证。
- 本次只验证正常实际调用；异常时 history 是否保留的新增本地模拟检查仍未执行。

---

# 三十六、理论上预期看到什么？

角色结构应该是确定的：

```text
第 1 轮：
[system,user]
```

```text
第 2 轮：
[system,user,assistant,user]
```

History：

```text
第 1 轮成功后：
[user,assistant]
```

```text
第 2 轮成功后：
[user,assistant,user,assistant]
```

System：

```text
每轮都在 Request 第一位
```

但：

```text
不属于本实验定义的 user / assistant Chat History
```

---

# 三十七、回答是否必须以 `JAVA-REVIEW:` 开头？

这是行为观察，不是程序正确性的唯一判断。

如果两轮都遵守：

```text
记录：本次模型遵守
```

如果某轮没遵守：

```text
也记录真实结果
```

然后得到一个真实结论：

```text
System Prompt 是行为约束
但不是 if / schema / 权限系统
```

不要为了“通过实验”在 Java 中自动给模型回答加 `JAVA-REVIEW:`。

那会把模型行为伪造成程序后处理结果，实验就失去意义。

---

# 三十八、为什么第二轮 Token 通常继续增加？

第 1 轮输入：

```text
System
+
User 1
```

第 2 轮输入：

```text
System
+
User 1
+
Assistant 1
+
User 2
```

所以：

```text
历史变多
+
System 仍然存在
```

输入 Token 通常继续增加。

这同时连接：

```text
Node 004：Token
Node 005：Context Window
Node 009：Chat History
Node 010：System Prompt
```

---

# 三十九、这是不是说明 System Prompt 每轮都“重复收费”？

在我们当前这种：

```text
Chat Completions
每轮重新传完整 messages
```

的理解中，System Prompt 每轮都属于本轮输入 Context，因此会参与本轮输入 Token 计算。

具体账单还可能受到模型价格、缓存机制、Provider 规则等影响。

所以不要简单把：

```text
System 字符数 × 请求次数
```

直接当账单公式。

本 Node 只建立：

```text
System 是输入的一部分
```

这个正确概念。

---

# 四十、为什么不把“System 只放第一轮”设成必做实验？

当然可以做，但不作为必做。

如果第二轮不带 System，却带了 Assistant 1，上一轮 Assistant 的表达方式本身也进入了 Context。

模型可能继续保持类似风格，这会让结果解释变得模糊。

我们真正需要证明的是：

```text
应用应该显式构造本轮需要的 System Context
```

而不是依赖：

```text
模型可能从历史风格中自己猜
```

---

# 四十一、可选实验：第二轮移除 System

**可选，不属于 Node 010 必做。**

可以临时对比：

```text
C：
System + User1 + Assistant1 + User2

D：
User1 + Assistant1 + User2
```

观察 D 是否仍延续 `JAVA-REVIEW:` 风格。

但无论结果如何，都不能推出：

```text
“不需要 System”
```

因为历史 Assistant 本身也在影响模型。

这个实验主要帮助理解：

> Context 中的不同部分都会影响生成行为。

做完必须恢复主实验版本。

---

# 四十二、第二阶段成功标准

- [x] System Prompt 与 Chat History 在代码概念上分开；
- [x] 第 1 轮 Request 是 `[system,user]`；
- [x] 第 2 轮 Request 是 `[system,user,assistant,user]`；
- [x] 两轮 System 文本一致；
- [x] 第二轮 Assistant 历史来自第一轮真实 Response；
- [x] System 每轮由应用重新加入；
- [x] System 没被错误当作 Assistant History；
- [x] 真实两轮百炼调用成功；
- [x] 真实 Token Usage 已记录；
- [x] Prompt 遵守情况如实记录；
- [x] 第二份 Codex Prompt / Handoff 已保存。

---

# 四十三、错误路径应该怎么理解？

假设：

```text
第 1 轮成功
第 2 轮 HTTP 失败
```

正确状态：

```text
systemPrompt
仍然存在于应用配置
```

```text
history
只保留：
User 1
Assistant 1
```

不能把 User 2 / Assistant 2 保存为“成功历史”，更不能编造一个 Assistant 2 塞进去。

这延续 Node 009 已经建立的原则：

> 历史只能记录真实成功发生过的交互。

---

# 四十四、System Prompt 和 History 的生命周期不同

### System Prompt

可能来自：

```text
应用配置
Prompt 文件
模板
代码常量
运行时配置
```

生命周期可能跨很多个对话。

### Chat History

通常属于：

```text
某一个用户
某一个会话
某一段任务
```

生命周期不同。

所以：

```text
System Prompt
≠
Chat History
```

不仅是理论区别，也会导致真实代码里不同的存储、更新逻辑和生命周期。

---

# 四十五、为什么这已经开始接近 Context Engineering？

因为每轮我们都在做：

```text
这一轮到底给模型看到什么？
```

目前只有：

```text
System
History
Current User
```

以后还会加入：

```text
RAG
Memory
Tool Result
State
Business Context
```

最终 `messages / input` 就是应用组织出来的 Context。

所以 Node 010 虽然只学 System Prompt，其实已经在为 Node 019 Context Engineering 铺路。

---

# 四十六、但现在不要设计 ContextBuilder

我们已经知道未来可能会有 System、History、RAG、Memory、Tool，但现在仍然不要提前写：

```text
ContextBuilder
PromptAssembler
ContextManager
ConversationEngine
```

原因很简单：

```text
我们还没真正学过那些输入来源
```

先让代码简单到你能一眼看出每条 Message 从哪里来，比提前抽象更重要。

---

# 四十七、Checkpoint 3：检查 Git 和 Secret

# 🛑 停下来操作

完成两阶段实验后，在仓库根目录执行：

```bash
git status
git diff --check
git diff --stat
```

检查：

```text
SystemPromptDemo.java
check-config.py
codex/prompts
codex/handoffs
010-2 笔记中的真实记录
```

---

# 四十八、Secret 检查

确保没有提交：

```text
DASHSCOPE_API_KEY 的真实值
Authorization: Bearer <真实值>
IDEA 私有运行配置
```

System Prompt 本身不是 Secret 存储区。

不要因为它叫 `System` 就误以为适合塞敏感数据。

---

# 四十九、010-2 最终实验记录

## 第一阶段：A/B

```text
实际运行日期：2026-11-02
A Roles：[user]；B Roles：[system,user]
A Prompt Tokens：40；B Prompt Tokens：138
A Completion Tokens：1001；B Completion Tokens：398
A Total Tokens：1041；B Total Tokens：536
两组均为 HTTP 200、finish_reason=stop。
A 回答摘要：空指针风险、多种修复方案、设计建议及总结表，篇幅较长。
B 回答摘要：先给总体结论，最多三项主要问题，更集中于空指针和缺失契约。
本样本观察：B 更集中，输入增加、输出减少；但措辞仍有过度判断，不能认为 System 是绝对规则。
完整回答与 Messages 见第十七节，分析见第二十二节。
```

## 第二阶段：多轮 System

```text
真实运行日期：2026-11-02
第 1 轮 Roles：[system,user]；第 2 轮 Roles：[system,user,assistant,user]
第 1 轮 History 成功后条数：2；第 2 轮：4
第 1 轮 Prompt/Completion/Total Tokens：70/36/106
第 2 轮 Prompt/Completion/Total Tokens：134/60/194
两轮均 HTTP 200、finish_reason=stop。
两轮都携带相同 System，均处于 messages 第一位。
最终 history 只有四条 User/Assistant，没有 System。
第二轮 Assistant 历史来自第一轮实际回复。
本次两轮都遵守 JAVA-REVIEW: 开头，未通过 Java 补前缀。
正常调用验证成功；新增本地模拟测试仍未运行，错误路径未实测。
完整原始消息、回答、历史见第三十五节。
```

---

# 五十、Node 010 最终理解自测

你应该能够解释：

1. System Prompt 和 System Message 有什么区别？
2. 为什么 System Prompt 在当前 Chat Completions 中属于 Request Body，而不是 HTTP Header？
3. 为什么 A / B 实验必须保证 User Message 相同？
4. 为什么不能只凭一次回答判断 System Prompt 的稳定性？
5. 为什么 System Prompt 会增加 `prompt_tokens`？
6. 为什么第二轮还要重新携带 System Message？
7. `systemPrompt` 和 `chatHistory` 为什么适合分开维护？
8. 为什么 System 不应该被当成普通 Assistant 历史？
9. 为什么 System Prompt 不能代替 Java 权限控制？
10. 为什么“最多 3 项”即使写进 System，也不是绝对程序约束？
11. 为什么 System Prompt 不能存 API Key？
12. 为什么 System Prompt 不是 Memory？
13. 为什么 System Prompt 不会改变模型参数？
14. System + History + Current User 和 Context 是什么关系？
15. 未来 Agent 为什么仍然需要 Java Runtime，而不能把所有规则都写进 System？

---

# 五十一、口述验收

如果有人问：

> “System Prompt 在 Java Agent 应用里到底是什么？为什么每轮都还要传？”

你应该能解释：

> 在我们当前使用的 OpenAI-compatible Chat Completions 中，System Prompt 会作为 `role=system` 的 System Message 进入本轮 `messages`，属于模型当前可见 Context 的一部分。它用于告诉模型应用希望它以什么角色、遵守什么行为规则以及怎样回答。
>
> 它不是一个永久写入模型的配置，也不会训练模型。当前 Chat Completions 请求本身是无状态的，所以后续请求如果仍希望这些 System 规则生效，应用就要继续把 System Message 放进新的 `messages`。
>
> 在 Java 应用里，可以把 `systemPrompt` 和 user/assistant `chatHistory` 分开保存，每轮构造 Request 时组合成 `System + History + Current User`。模型成功返回后，再把本轮 User 和真实 Assistant 保存进 History。
>
> System Prompt 只是自然语言行为约束，不是权限系统、数据库约束或绝对安全边界；这些硬规则仍然要由 Java 程序真正执行。

能自然解释这套逻辑：

```text
Node 010
基本掌握
```

---

# 五十二、Node 010 最终 Checklist

理解项依据用户已读完、理解讨论及要求进入下一节点记录，未另行执行逐题口述测验。客观实验项依据实际输出填写。新增本地模拟检查仍未运行，不在此冒充已通过；可选的第二轮移除 System 实验未执行。

## 理论

- [x] 理解 System Prompt；
- [x] 理解 System Message；
- [x] 能区分 System / User / Assistant；
- [x] 理解 System 属于 Context；
- [x] 理解 System 会消耗 Token；
- [x] 理解 System 不是永久模型配置；
- [x] 理解 System 不是权限 / Secret / 安全边界。

## 第一阶段 A/B

- [x] A 为 `[user]`；
- [x] B 为 `[system,user]`；
- [x] User 完全一致；
- [x] Model / 参数一致；
- [x] B System 真实进入 Request；
- [x] A / B 真实模型调用完成；
- [x] A / B Response 已记录；
- [x] A / B Token 已记录；
- [x] 能解释观察到的行为差异。

## 第二阶段多轮

- [x] `systemPrompt` 与 `history` 分开；
- [x] 第 1 轮 `[system,user]`；
- [x] 第 2 轮 `[system,user,assistant,user]`；
- [x] System 每轮重新加入；
- [x] Assistant 历史来自真实 Response；
- [x] 成功后 History 为 2 / 4 条；
- [x] 多轮真实调用完成；
- [x] Token 已记录；
- [x] 能解释为什么 System 不会自动永久生效。

## 工程

- [x] `FirstModelCall` 未受破坏；
- [x] `MessageHistoryDemo` 未受破坏；
- [x] 新增 `SystemPromptDemo`；
- [x] 不引入无关框架；
- [x] Secret 未进入 Git；
- [x] 两轮 Codex Prompt / Handoff 已保存；
- [x] 模拟结果和真实百炼结果明确区分。

本轮学习与两阶段真实正常调用已完成；新增本地模拟（含错误路径）作为独立待办保留，未另行逐题口述。下一节点为 Node 011。以下是原验收目标：

```text
010-2：✅ PASS
Node 010：✅ PASS
```

然后进入：

```text
Node 011 - Prompt 基础
```

---

# 五十三、完成 Node 010 后，模型调用链变成什么？

Node 008：

```text
Java
↓
HTTP Request
↓
Model
↓
Response
```

Node 009：

```text
Java
↓
History
↓
messages
↓
Model
↓
Assistant
↓
保存 History
```

Node 010：

```text
Java Application
        │
        ├── System Prompt
        │
        ├── Chat History
        │
        └── Current User
                │
                ▼
             messages
                │
                ▼
              Model
                │
                ▼
            Assistant
                │
                ▼
          更新 Chat History
```

这已经开始非常接近真正 LLM Application 的基本形态。

---

# 五十四、下一步为什么是 Node 011？

到这里我们已经知道：

```text
Prompt 放在哪里
```

也知道：

```text
System 和 User 各自承担什么角色
```

下一步才能自然进入：

```text
Prompt 到底应该怎样组织
```

也就是：

```text
Instruction
Context
Input
Output Format
```

以及：

```text
Role
Task
Constraints
```

所以顺序：

```text
Message
↓
System Prompt
↓
Prompt 基础
```

不是随便排的。

---

# 五十五、停止点

如果你现在只是刚把这篇文档放进仓库，不要直接让 Codex 一次把两个阶段全做完。

严格按：

```text
Checkpoint 0
↓
Codex 010-2-1
↓
IDEA 真实 A/B
↓
自己读懂 Request
↓
记录真实结果
↓
Codex 010-2-2
↓
IDEA 真实多轮
↓
记录真实结果
↓
Git / Secret 检查
↓
Node 010 验收
```

推进。

一次只增加一个新的知识变量。

---

# 参考资料与范围说明

## 当前仓库基线

生成本篇时的历史基线（当前完成状态见文首及 progress.md）：

```text
Node 008 ✅
Node 009 ✅
Node 010 🟡
010-1 已读完
010-2 尚未开始
```

当前已有：

```text
FirstModelCall.java
MessageHistoryDemo.java
```

`MessageHistoryDemo` 已真实跑通：

```text
A/B 有历史对照
三轮自动 Chat History
```

本篇在此基础上新增 System Prompt 实验，不重做 Node 009。

## 阿里云百炼官方说明

### OpenAI兼容 - Chat

https://help.aliyun.com/zh/model-studio/qwen-api-via-openai-chat-completions/

当前文档说明：

- `messages` 是按对话顺序传给模型的上下文；
- System Message 为可选消息；
- 可用于设置模型的角色、语气、任务目标和约束；
- `role` 固定为 `system`；
- System Message 一般位于 `messages` 第一位。

### 通义千问模型的多轮对话

https://help.aliyun.com/zh/model-studio/multi-round-conversation

当前文档说明：

- Qwen Chat Completions API 本身不保存对话历史；
- 多轮对话需要应用维护并在每次 Request 中显式传入历史 `messages`；
- 历史增加会带来更多 Context / Token 消耗。

> 核对时间：2026-10-26。\
> 本篇只针对当前项目使用的 `qwen3.8-max + OpenAI-compatible Chat Completions` 学习主线。不同 Provider、模型和 API 的高层角色设计可能不同，后续切换时应重新核对对应官方文档。

---

# 本篇一句话总结

> **Node 009 让 Java 学会了“把过去带进下一轮”，Node 010 则让 Java 学会了“每一轮都告诉模型：你在这个应用里应该怎样工作”。**
