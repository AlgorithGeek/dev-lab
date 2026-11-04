# 011-2 - 实验：对比模糊 Prompt 与结构化 Prompt

> Phase 1：LLM 应用开发基础\
> Node 011：Prompt 基础\
> Note 011-2：Prompt A/B 实验篇
>
> 前置：
>
> ```text
> 011-1 - Prompt 基础到底是什么？ ✅ 已读完
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

---

> **当前记录（2026-11-02）：**第一阶段 A/B 已由用户在 IDEA 真实运行，双方均 HTTP 200、qwen3.8-max、finish_reason=stop；实际 Messages、完整回答和 Token 已记录。B 利用了 Context 并遵循主要格式，但第三项把原始 int 与改写后的对象引用混淆，不能据此判定结构化 Prompt 一定正确。第二阶段 B/C 也已真实运行，完整消息、回答和用量见第三十二节；阅读讲解与两阶段正常实测已收尾，下一步为 Node 012「Few-shot」；新增本地模拟仍未执行，不能将正常响应视作错误路径验证。C 同时调整了不自行假设的约束，因此不将全部变化仅归因于 Context。

# 一、这一篇要验证什么？

011-1 学完以后，我们已经知道一个 Prompt 可以拆成：

```text
Instruction
Context
Input
Output Format
```

并且可以进一步组织成：

```text
Role
Task
Context
Constraints
Output Format
```

但理论上知道这些词还不够。

这一篇要验证的是：

> **当同一个模型处理同一个 Input 时，把一个模糊 Prompt 改成结构清晰的 Prompt，真实 Request、真实回答和 Token Usage 会发生什么变化？**

我们不提前假定：

```text
结构化 Prompt 一定更聪明
```

也不提前假定：

```text
模糊 Prompt 一定回答很差
```

这次实验真正要观察的是：

```text
任务是否更聚焦
Context 是否被正确利用
回答边界是否更清楚
输出结构是否更接近要求
是否减少无关扩展
Prompt Tokens 增加了多少
```

---

# 二、这次继续使用 A/B 对照

Node 009 我们做过：

```text
A：无历史
B：有历史
```

Node 010 做过：

```text
A：无 System
B：有 System
```

Node 011 继续使用同样的方法：

```text
A：模糊 Prompt
B：结构化 Prompt
```

尽量只改变：

```text
Prompt 的组织方式和信息完整度
```

而保持：

```text
Model 相同
Input 相同
reasoning_effort 相同
Endpoint 相同
```

这样才能真正比较 Prompt 设计本身。

---

# 三、这次新增什么 Java 入口？

建议新增：

```text
PromptBasicsDemo.java
```

于是当前学习项目逐步变成：

```text
FirstModelCall.java
    ↓
MessageHistoryDemo.java
    ↓
SystemPromptDemo.java
    ↓
PromptBasicsDemo.java
```

每个类代表一个知识节点。

不要把前面的实验代码重写掉。

也不要现在为了复用而抽象：

```text
ModelClient
PromptService
ProviderFactory
ContextBuilder
```

这些都还不是当前学习重点。

---

# 四、为什么这次不继续用 SystemPromptDemo？

因为 Node 010 已经明确解决：

```text
System Prompt 在哪里
System Message 怎么进 messages
多轮怎样继续携带 System
```

Node 011 的问题不同：

```text
Prompt 本身应该怎样组织
```

所以建议独立做：

```text
PromptBasicsDemo
```

这样以后复习时不会混淆：

```text
Message 结构问题
System role 问题
Prompt 内容组织问题
```

---

# 五、这次实验用什么 Input？

我们需要一个：

```text
同一个 Input
在有 Context 和没有 Context 时
确实可能得出不同判断
```

的 Java 例子。

建议使用：

```java
public class CounterService {
    private int count = 0;

    public int increase() {
        return ++count;
    }
}
```

这段代码本身很简单。

如果只看代码，模型可能会讨论：

```text
线程安全
封装
原子性
设计方式
```

但如果补充 Context：

```text
这是一个 Spring Boot 单例 Service，
同一个实例可能被多个 HTTP 请求线程并发访问。
```

那么：

```text
count++ / ++count 的并发安全问题
```

就从“可能”变成非常重要的分析方向。

这正好可以验证：

```text
Context
```

是不是在实际 Prompt 中产生作用。

---

# 六、A：模糊 Prompt

A 只发送：

```text
帮我看看下面这段代码有什么问题：

public class CounterService {
    private int count = 0;

    public int increase() {
        return ++count;
    }
}
```

这里有：

```text
一个很宽泛的 Instruction
+
Input
```

但没有明确：

```text
Role
Context
Constraints
Output Format
```

---

# 七、B：结构化 Prompt

B 使用完全相同的代码 Input，但把任务组织清楚：

```text
Role:
你是一名 Java 后端代码审查助手。

Task:
检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。

Context:
这段代码位于 Spring Boot 单例 Service 中，同一个 Service 实例可能被多个 HTTP 请求线程并发访问。

Input:
public class CounterService {
    private int count = 0;

    public int increase() {
        return ++count;
    }
}

Constraints:
1. 最多指出 3 个问题。
2. 只基于给出的代码和 Context 分析。
3. 不要虚构不存在的数据库、缓存、接口或调用链。

Output Format:
先给一句总体结论。
然后按“问题 / 原因 / 建议”输出。
```

---

# 八、A 和 B 到底差在哪？

不是：

```text
A 没有 Prompt
B 有 Prompt
```

两边都有 Prompt。

真正区别是：

```text
A：信息少、任务边界模糊

B：
Role 明确
Task 明确
Context 明确
Input 明确
Constraints 明确
Output Format 明确
```

这才是 Node 011 真正要理解的东西。

---

# 九、为什么 B 不使用 Few-shot？

因为 Node 012 才学习：

```text
Zero-shot
One-shot
Few-shot
```

如果 B 现在就加入：

```text
示例输入
示例输出
```

那我们就同时改变了两个变量：

```text
Prompt 结构
+
Few-shot 示例
```

实验不干净。

所以本篇 A / B 都保持：

```text
Zero-shot
```

没有示例。

---

# 十、为什么 B 不要求 JSON？

因为 Node 013～015 会专门学习：

```text
Structured Output
JSON Schema
Java DTO
```

本篇只使用自然语言 Output Format：

```text
总体结论
问题
原因
建议
```

不要把 Node 011 提前变成 Structured Output 实验。

---

# 十一、这次 System Message 怎么处理？

为了让实验变量更干净，建议：

```text
A 和 B 都不额外使用 System Message
```

只使用：

```text
role = user
```

把两种 Prompt 都放在 User Message 中。

为什么？

因为 Node 010 已经验证过 System Prompt 的影响。

如果这次：

```text
A 没 System
B 有 System
```

我们就无法判断差异到底来自：

```text
Prompt 结构
```

还是：

```text
System Role
```

所以本篇故意把 role 固定。

---

# 十二、这次 Request 应该是什么结构？

A：

```json
{
  "model": "qwen3.8-max",
  "messages": [
    {
      "role": "user",
      "content": "<模糊 Prompt>"
    }
  ],
  "reasoning_effort": "none"
}
```

B：

```json
{
  "model": "qwen3.8-max",
  "messages": [
    {
      "role": "user",
      "content": "<结构化 Prompt>"
    }
  ],
  "reasoning_effort": "none"
}
```

注意：

```text
两边 Roles 都是 [user]
```

区别只在：

```text
content
```

---

# 十三、Checkpoint 0：同步仓库

## 🛑 停下来操作

在 `dev-lab` 根目录：

```bash
git status
git pull origin main
```

确认：

```text
011-1 - Prompt 基础到底是什么？.md
```

已经存在。

同时当前 Java 入口应有：

```text
FirstModelCall.java
MessageHistoryDemo.java
SystemPromptDemo.java
```

---

## Checkpoint 0 成功标准

- [x] 当前分支与远端 `main` 同步（开始本节时的基线检查；本次收尾未重新拉取远端）；
- [x] `011-1` 已存在；
- [x] 没有不认识的未提交修改；
- [x] 前三个实验入口仍在。

---

# 十四、第一轮 Codex 任务：只实现 A/B

这一轮不要一次把 Node 011 做完。

目标只有：

> **新增 PromptBasicsDemo，真实构造 A / B 两个 User Prompt，并保持其他调用参数一致。**

---

# 十五、Codex Prompt：011-2-1

## 🛑 停下来操作

把下面内容发给 Codex：

```text
你现在在我的 dev-lab 学习仓库中工作。

当前学习位置：

Phase 1 / Node 011 - Prompt 基础

011-1 理论篇已经读完，现在开始：

011-2 - 实验：对比模糊 Prompt 与结构化 Prompt

请先阅读：

- Agent/README.md
- Agent/progress.md
- Agent/Agent Engineer 循序渐进学习路线.md
- Agent/notes/01-LLM应用开发基础/011-1 - Prompt 基础到底是什么？.md
- Agent/notes/01-LLM应用开发基础/011-2 - 实验：对比模糊 Prompt 与结构化 Prompt.md
- Agent/projects/agent-learning-lab/pom.xml
- Agent/projects/agent-learning-lab/src/main/java/com/avexvertex/agent/FirstModelCall.java
- Agent/projects/agent-learning-lab/src/main/java/com/avexvertex/agent/MessageHistoryDemo.java
- Agent/projects/agent-learning-lab/src/main/java/com/avexvertex/agent/SystemPromptDemo.java
- Agent/codex/handoffs/010-2-2 - 多轮携带 System Message.md

这是学习实验，请严格控制范围。

==================================================
一、本轮目标
==================================================

只完成 Node 011 第一阶段：

“同一模型、同一代码 Input、同一参数下，对比模糊 Prompt 与结构化 Prompt。”

不要进入 Few-shot。
不要做多轮 Prompt 实验。

==================================================
二、新增入口
==================================================

新增：

Agent/projects/agent-learning-lab/src/main/java/com/avexvertex/agent/PromptBasicsDemo.java

保留并不要破坏：

- FirstModelCall.java
- MessageHistoryDemo.java
- SystemPromptDemo.java

不要重新创建 Maven 项目。
不要重新 git init。

==================================================
三、技术保持不变
==================================================

继续使用：

- Java 21
- JDK HttpClient
- FastJSON2
- DASHSCOPE_API_KEY
- DASHSCOPE_BASE_URL
- qwen3.8-max
- OpenAI-compatible Chat Completions
- reasoning_effort = none

不要引入新 SDK 或框架。

==================================================
四、固定 Input
==================================================

A / B 使用完全相同的 Java 代码：

public class CounterService {
    private int count = 0;

    public int increase() {
        return ++count;
    }
}

==================================================
五、A：模糊 Prompt
==================================================

A 的 User Message：

帮我看看下面这段代码有什么问题：

public class CounterService {
    private int count = 0;

    public int increase() {
        return ++count;
    }
}

A 只发送一条 user message。

==================================================
六、B：结构化 Prompt
==================================================

B 的 User Message：

Role:
你是一名 Java 后端代码审查助手。

Task:
检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。

Context:
这段代码位于 Spring Boot 单例 Service 中，同一个 Service 实例可能被多个 HTTP 请求线程并发访问。

Input:
public class CounterService {
    private int count = 0;

    public int increase() {
        return ++count;
    }
}

Constraints:
1. 最多指出 3 个问题。
2. 只基于给出的代码和 Context 分析。
3. 不要虚构不存在的数据库、缓存、接口或调用链。

Output Format:
先给一句总体结论。
然后按“问题 / 原因 / 建议”输出。

B 也只发送一条 user message。

==================================================
七、严格控制变量
==================================================

A / B 必须保持：

- model 相同
- endpoint 相同
- reasoning_effort 相同
- 都只有一条 user message
- Java Input 完全相同

不要给 B 增加 system role。
不要给任何一边增加 Few-shot 示例。

本轮唯一主要变量应是：

Prompt 的组织方式和提供的信息完整度。

==================================================
八、输出要求
==================================================

A / B 每次至少输出：

- Label
- Messages JSON
- Roles
- Message Count
- User Prompt 字符数（可选但推荐）
- HTTP Status
- Model
- Content
- Finish Reason
- Prompt Tokens
- Completion Tokens
- Total Tokens

不要打印：

- API Key
- Authorization Header
- Secret

==================================================
九、错误处理
==================================================

沿用已有实验代码的最小错误处理：

- 非 2xx 不解析成功字段
- JSON 无效明确失败
- model / choices / content / finish_reason / usage 缺失明确失败
- 不自动重试
- 不伪造回答

不要设计新的异常框架。

==================================================
十、本地模拟验证
==================================================

扩展 check-config.py 或用当前项目等价方式验证。

至少检查：

1. Maven Java 21 编译通过；
2. 前三个 Java 入口仍然能编译；
3. A roles = [user]；
4. B roles = [user]；
5. A / B model 相同；
6. A / B reasoning_effort 相同；
7. A / B 中的 Java Input 完全一致；
8. B 包含 Role / Task / Context / Constraints / Output Format；
9. A 不包含这些结构化段落；
10. 非 2xx 不解析成功字段；
11. 输出不泄露假 Secret。

本地模拟不能冒充真实百炼结果。

==================================================
十一、学习记录
==================================================

保存本轮任务：

Agent/codex/prompts/011-2-1 - 模糊 Prompt 与结构化 Prompt 对照.md

生成 Handoff：

Agent/codex/handoffs/011-2-1 - 模糊 Prompt 与结构化 Prompt 对照.md

Handoff 至少记录：

1. 修改了哪些文件；
2. PromptBasicsDemo 怎么运行；
3. A / B Request 分别是什么；
4. 两边保持了哪些变量一致；
5. B 中 Role / Task / Context / Input / Constraints / Output Format 分别在哪里；
6. 为什么两边都只使用 user role；
7. Maven 编译结果；
8. 本地模拟结果；
9. 是否真实调用百炼；
10. 目前还没实现哪些能力；
11. 我回来后应该重点阅读哪些代码。

==================================================
十二、范围控制
==================================================

本轮不要实现：

- Few-shot
- One-shot 示例
- Prompt Template
- Prompt Registry
- System Prompt 新实验
- 多轮 Prompt
- Structured Output
- JSON Schema
- Spring Boot
- Spring AI
- LangChain4j
- Tool Calling
- Agent Loop
- RAG
- Memory
- ProviderFactory

不要把 progress.md 标记为 Node 011 完成。
不要进入 Node 012。
不要自行 Git commit / push。

完成第一阶段以后停止，等我在 IDEA 中真实运行并阅读结果。
```

---

# 十六、Codex 完成后先读什么？

先打开：

```text
PromptBasicsDemo.java
```

不要先看 HTTP 部分。

优先看：

```text
vaguePrompt
structuredPrompt
```

或者 Codex 实际采用的等价变量。

然后确认：

```text
A：只有一条 user
B：也只有一条 user
```

最后对比：

```text
A content
B content
```

这才是本 Node 的核心。

---

# 十七、你应该能在代码中指出四个核心部分

在 B 里找到：

```text
Instruction / Task
```

```text
Context
```

```text
Input
```

```text
Output Format
```

另外找到：

```text
Role
Constraints
```

如果看着 B 仍然分不清这些部分，先回去看 011-1。

---

# 十八、IDEA 真实运行

## 🛑 停下来操作

按 Codex Handoff 运行：

```text
PromptBasicsDemo
```

继续使用本地已有：

```text
DASHSCOPE_API_KEY
DASHSCOPE_BASE_URL
```

正常情况会发送：

```text
2 次真实模型请求
```

可能产生实际模型用量。

---

# 十九、第一阶段真实记录

2026-11-02，以下来自用户提供的 IDEA 实际输出，不是本地模拟。两组均为 HTTP 200、qwen3.8-max、finish_reason=stop，消息数各 1，Roles 均为 [user]。模型原文保留，不将其中建议直接视为已确认事实。

## A：模糊 Prompt

User Prompt 字符数：135；Prompt Tokens：52；Completion Tokens：730；Total Tokens：782。

实际 Messages：

```json
[
  {
    "role": "user",
    "content": "帮我看看下面这段代码有什么问题：\n\npublic class CounterService {\n    private int count = 0;\n\n    public int increase() {\n        return ++count;\n    }\n}"
  }
]
```

实际 Content：

````text
# 代码问题分析

这段代码最核心的问题是 **线程不安全**，在多线程环境下会出现严重问题。下面详细分析：

---

## 1. 核心问题：线程安全（竞态条件）

`++count` 看起来是一行代码，但在底层它**不是原子操作**，而是分为三步：

```
1. 读取 count 的值（read）
2. 将值加 1（increment）
3. 将新值写回 count（write）
```

### 并发场景下的问题演示：

```
假设 count = 0，线程A和线程B同时调用 increase()

时间线：
线程A: 读取 count = 0
线程B: 读取 count = 0      ← 还没等A写回，B也读了
线程A: 计算 0 + 1 = 1
线程B: 计算 0 + 1 = 1      ← B用的也是旧值
线程A: 写回 count = 1
线程B: 写回 count = 1      ← 期望是2，实际只变成了1！
```

> **结果**：两个线程各调了一次 `increase()`，期望结果是 `2`，但实际结果是 `1`，**丢失了一次更新**。

---

## 2. 次要问题：可见性

即使不考虑原子性，一个线程修改了 `count` 的值，其他线程可能因为 **CPU 缓存**的原因看不到最新值（没有 `volatile` 或同步机制保证内存可见性）。

---

## 修复方案

### 方案一：使用 `AtomicInteger`（✅ 推荐，性能最好）

```java
import java.util.concurrent.atomic.AtomicInteger;

public class CounterService {
    private AtomicInteger count = new AtomicInteger(0);

    public int increase() {
        return count.incrementAndGet(); // CAS 无锁操作，线程安全
    }
}
```

### 方案二：使用 `synchronized` 关键字

```java
public class CounterService {
    private int count = 0;

    public synchronized int increase() {  // 加锁保证互斥
        return ++count;
    }
}
```

### 方案三：使用 `ReentrantLock`

```java
import java.util.concurrent.locks.ReentrantLock;

public class CounterService {
    private int count = 0;
    private final ReentrantLock lock = new ReentrantLock();

    public int increase() {
        lock.lock();
        try {
            return ++count;
        } finally {
            lock.unlock(); // 确保异常时也能释放锁
        }
    }
}
```

---

## 总结对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| `AtomicInteger` | 无锁、高性能、非阻塞 | 仅适合简单计数场景 |
| `synchronized` | 写法简单 | 阻塞式，高并发下性能较差 |
| `ReentrantLock` | 灵活（可尝试获取锁、超时等） | 需要手动释放锁，代码稍复杂 |

> 💡 **建议**：对于简单的计数器场景，优先使用 **`AtomicInteger`**，它是利用 CPU 的 CAS（Compare-And-Swap）指令实现的，既保证了线程安全又避免了加锁的开销。
````

## B：结构化 Prompt

User Prompt 字符数：396；Prompt Tokens：177；Completion Tokens：497；Total Tokens：674。

实际 Messages：

```json
[
  {
    "role": "user",
    "content": "Role:\n你是一名 Java 后端代码审查助手。\n\nTask:\n检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。\n\nContext:\n这段代码位于 Spring Boot 单例 Service 中，同一个 Service 实例可能被多个 HTTP 请求线程并发访问。\n\nInput:\npublic class CounterService {\n    private int count = 0;\n\n    public int increase() {\n        return ++count;\n    }\n}\n\nConstraints:\n1. 最多指出 3 个问题。\n2. 只基于给出的代码和 Context 分析。\n3. 不要虚构不存在的数据库、缓存、接口或调用链。\n\nOutput Format:\n先给一句总体结论。\n然后按“问题 / 原因 / 建议”输出。"
  }
]
```

实际 Content：

````text
## 总体结论
该代码存在严重的并发安全问题，在多线程环境下 `count` 的值将不可预测且会丢失更新，必须优先修复。

---

### 1. 非原子性的竞态条件（Race Condition）
- **问题**：`++count` 操作不是原子操作。
- **原因**：`++count` 在底层分为三步执行：读取当前值、加一、写回新值。当多个 HTTP 请求线程同时调用 `increase()` 时，可能出现两个线程同时读取到相同的旧值（例如都读到 0），各自加一后写回（都写回 1），导致本应增加两次的计数只增加了一次，产生数据丢失。
- **建议**：将 `int count` 替换为 `java.util.concurrent.atomic.AtomicInteger`，并使用其提供的原子方法。
  ```java
  private final AtomicInteger count = new AtomicInteger(0);

  public int increase() {
      return count.incrementAndGet();
  }
  ```

### 2. 缺乏内存可见性保障
- **问题**：普通变量 `count` 在多线程间没有可见性保证。
- **原因**：在 Spring Boot 单例 Service 中，不同线程可能运行在不同的 CPU 核心上。由于 Java 内存模型（JMM）的特性，一个线程对 `count` 的修改可能只停留在该线程的工作内存（CPU 缓存）中，未及时刷新到主内存，导致其他线程读取到的始终是过期数据。
- **建议**：上述使用 `AtomicInteger` 的建议已经天然解决了此问题，因为 `AtomicInteger` 内部基于 `volatile` 和 CAS 机制保证了内存可见性。

### 3. 字段未声明为不可变引用
- **问题**：成员变量 `count` 没有被声明为 `final`。
- **原因**：作为单例 Service 中的共享状态，如果计数器对象本身的引用被意外重新赋值（例如误写了 `this.count = xxx`），不仅会破坏计数逻辑，还会引发更难以排查的并发异常。
- **建议**：在采用 `AtomicInteger` 等线程安全对象后，加上 `final` 修饰符防止引用被篡改。
  ```java
  private final AtomicInteger count = new AtomicInteger(0);
  ```
````

---

# 二十、这次到底应该观察什么？

不要只问：

```text
B 是不是更长？
```

应该从下面几个维度看。

## 1. Task 聚焦

A 是否可能：

```text
泛泛点评
```

而 B 是否更明确聚焦：

```text
并发安全
```

---

## 2. Context 使用

B 是否真正利用：

```text
Spring Boot 单例 Service
多个 HTTP 请求线程并发访问
```

来判断：

```text
count 共享
++count 非原子
```

如果 B 完全忽略 Context，说明：

```text
Prompt 虽然写了 Context
但本次模型没有充分利用
```

也要如实记录。

---

## 3. Constraints

B 是否：

```text
最多 3 项
```

是否避免虚构：

```text
Redis
数据库
Controller
MQ
```

等不存在的信息。

---

## 4. Output Format

B 是否：

```text
先给总体结论
```

然后：

```text
问题 / 原因 / 建议
```

---

## 5. Token Usage

B 明显更长，通常：

```text
Prompt Tokens
```

也会更高。

这个代价必须一起观察。

Prompt 更清楚：

```text
往往不是免费的
```

---

# 二十一、为什么 B 的 Token 更多不代表 B 一定更值？

因为 Prompt 设计本身也是成本权衡。

如果：

```text
A 已经足够完成简单任务
```

那 B 加很多 Context 和 Constraints 可能只是增加：

```text
Token
延迟
维护成本
```

所以真正工程问题是：

> **为了获得更稳定、更聚焦的输出，值得增加多少 Context 和约束？**

这已经开始接近 Context Engineering 思维。

---

# 二十二、如果 A 也准确指出线程安全问题怎么办？

完全正常。

模型本身懂 Java。

这不能说明：

```text
Context 没用
```

因为 A 可能仅从：

```text
成员变量 int count
```

推测存在并发风险。

真正应该比较的是：

```text
A 的判断是否带假设
B 是否基于明确 Context 给出更确定的判断
```

例如：

```text
A：如果该对象被多线程共享，则可能有线程安全问题。
```

```text
B：由于你已经说明它是 Spring 单例 Service 且被多个请求线程共享，因此 count 是共享可变状态，++count 存在竞态。
```

两者的“正确程度”可能都不错，

但 B 的：

```text
依据
```

更加明确。

---

# 二十三、如果 B 仍然虚构信息怎么办？

也正常记录。

不要为了实验漂亮而删掉。

这正好说明：

```text
Constraints
≠
硬程序规则
```

Prompt 只能引导。

如果未来业务要求绝对禁止某些输出，仍然需要：

```text
程序校验
权限控制
Schema
安全层
```

---

# 二十四、第一阶段对照表

| 项目 | A：模糊 Prompt | B：结构化 Prompt |
|---|---|---|
| Role 数组 | [user] | [user] |
| Message Count | 1 | 1 |
| Input 代码 | 相同 CounterService | 相同 CounterService |
| Context | 无明确 Context | 单例 Service、多 HTTP 请求线程 |
| Constraints | 无明确约束 | 最多 3 项、基于已给信息、不虚构上下文 |
| Output Format | 无明确格式 | 总体结论 → 问题 / 原因 / 建议 |
| User Prompt 字符数 | 135 | 396 |
| HTTP Status | 200 | 200 |
| Finish Reason | stop | stop |
| Prompt Tokens | 52 | 177 |
| Completion Tokens | 730 | 497 |
| Total Tokens | 782 | 674 |

---

# 二十五、行为观察记录

依据本次实际输出填写，未另行进行口述测验。

- A 识别了 ++count 非原子与多线程竞态，给出时序例子、可见性说明及 AtomicInteger / synchronized / ReentrantLock 三种方案；回答较展开。
- A 提到多线程条件，但没有明确的 Spring 单例或 HTTP 请求线程背景；不能把它识别并发风险当作“模型知道真实生命周期”。
- B 明确使用 Spring Boot 单例 Service 和多个 HTTP 请求线程的 Context，更直接地说明共享 count 的并发风险。
- B 先给总体结论，列三项，基本按问题 / 原因 / 建议输出；没有虚构数据库、缓存或接口调用链。
- **B 第三项存在概念混淆：原始 count 是 int，不是可被替换的计数器对象引用。**将 AtomicInteger 引用声明为 final 可以是改写后的建议，但不应把它当成原始 int 字段的“引用不可变”缺陷；原始计数 int 也不能简单加 final 后继续 ++。
- A 的“AtomicInteger 性能最好”缺少具体负载与基准证据，不能视为本实验已经验证的性能结论。
- 本次 B 输入 Token 比 A 多 125（177 - 52），输出少 233（730 - 497），总 Token 少 108（782 - 674）；这只是样本记录，不直接等于费用差异。
- 本次最明显的差异是 B 更明确使用背景并遵循格式，A 展开更多方案；但 B 仍有错误，不能把格式整齐等同于内容可靠。
- 两组同时改变信息完整度、任务重点、限制与表达结构，不是仅修改排版的单变量对照。第二阶段将进一步观察 Context 的作用。

---

# 二十六、Checkpoint 1 成功标准

- [x] 新增 `PromptBasicsDemo.java`；
- [x] 前三个实验入口未被破坏；
- [x] A 只有 `[user]`；
- [x] B 也只有 `[user]`；
- [x] A / B 使用相同 Java Input；
- [x] A / B Model 与参数一致；
- [x] B 清楚包含 Role / Task / Context / Constraints / Output Format；
- [x] 没有 Few-shot；
- [x] IDEA 真实 A/B 调用成功；
- [x] 两边 Response 已记录；
- [x] Token Usage 已记录；
- [x] 第一份 Codex Prompt / Handoff 已保存。

完成后：

```text
011-2 第一阶段 ✅
```

---

# 二十七、Node 011 需不需要第二阶段？

这一次和 Node 009、010 不一样。

Node 011 的核心目标：

```text
理解 Prompt 的组成
```

其实通过一次干净的 A/B 就能完成主要验证。

因此本篇不强制再写第二个复杂 Java 功能。

但为了真正理解：

```text
Context
```

我们建议加一个**非常小的第二阶段**：

> **只删除 B 的 Context，看看结论怎么变化。**

这不是为了做第三套大功能，

而是做一个最小消融实验（ablation）。

---

# 二十八、什么叫“消融实验”？

这里不需要背学术定义。

可以简单理解为：

```text
原方案有 A+B+C
↓
去掉其中一项 B
↓
看结果发生什么变化
```

我们的结构化 Prompt 有：

```text
Role
Task
Context
Input
Constraints
Output Format
```

如果只去掉：

```text
Context
```

其它都保持不变，

就能更直接观察：

```text
Context 本身的作用
```

---

# 二十九、C：结构化 Prompt，但不提供 Context

C 可以是：

```text
Role:
你是一名 Java 后端代码审查助手。

Task:
检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。

Input:
public class CounterService {
    private int count = 0;

    public int increase() {
        return ++count;
    }
}

Constraints:
1. 最多指出 3 个问题。
2. 只基于给出的代码分析。
3. 信息不足时明确说明，不要自行假设对象生命周期或并发场景。

Output Format:
先给一句总体结论。
然后按“问题 / 原因 / 建议”输出。
```

注意：

```text
B：明确告诉模型这是 Singleton + 多线程共享
C：明确不允许自己假设生命周期 / 并发场景
```

这样差异会很清楚。

---

# 三十、B 和 C 预期有什么不同？

B 已知：

```text
共享
并发
```

因此可以更确定地说：

```text
存在竞态条件
```

C 不知道对象是否共享。

更严谨的回答应该类似：

```text
如果该实例被多个线程共享，则 ++count 存在线程安全风险；
仅凭当前代码无法确认运行时是否发生并发访问。
```

这正是：

```text
Context
```

的价值。

---

# 三十一、Codex Prompt：011-2-2（小型 Context 消融）

只有第一阶段真实 A/B 跑完并理解后再做。

```text
继续当前 Node 011 Prompt 基础实验。

前置：

PromptBasicsDemo 已完成：
A = 模糊 Prompt
B = 结构化 Prompt + 明确 Context

现在只增加一个很小的 Context 消融实验 C。

不要增加新类。
不要改动 A / B 的现有行为。

==================================================
一、目标
==================================================

验证：

“在 Prompt 结构基本相同的情况下，是否提供关键 Context，会怎样影响模型判断的确定程度。”

==================================================
二、C Prompt
==================================================

使用与 B 相同的 Role / Task / Input / Output Format，
但删除下面 Context：

“这段代码位于 Spring Boot 单例 Service 中，同一个 Service 实例可能被多个 HTTP 请求线程并发访问。”

并将 Constraints 调整为：

1. 最多指出 3 个问题。
2. 只基于给出的代码分析。
3. 信息不足时明确说明，不要自行假设对象生命周期或并发场景。

其它主要条件保持一致。

==================================================
三、运行模式
==================================================

可以在 PromptBasicsDemo 中新增一个清晰参数：

--context

或使用项目中更自然的等价方式。

默认模式仍保留 A/B。

--context 模式只跑：

B（有 Context）
C（无 Context）

不要一次把 A/B/C 全部重复请求，避免不必要的模型用量。

==================================================
四、输出
==================================================

B / C 至少输出：

- Label
- Messages JSON
- HTTP Status
- Content
- Finish Reason
- Prompt Tokens
- Completion Tokens
- Total Tokens

==================================================
五、重点验证
==================================================

1. B / C Input 完全一致；
2. Role / Task / Output Format 基本一致；
3. B 有 Singleton + 多线程 Context；
4. C 不包含该 Context；
5. C 明确要求不要自行假设运行时并发场景；
6. 两边都没有 Few-shot；
7. 模拟结果和真实结果分开记录。

==================================================
六、记录
==================================================

保存：

Agent/codex/prompts/011-2-2 - Context 消融对照.md

生成：

Agent/codex/handoffs/011-2-2 - Context 消融对照.md

Handoff 记录：

- 修改文件
- 运行参数
- B / C 差异
- 本地模拟
- 是否真实百炼调用
- 建议我重点观察什么

==================================================
七、范围控制
==================================================

不要进入：

- Few-shot
- Structured Output
- Prompt Template
- Prompt Registry
- 多轮对话新功能
- Spring AI
- Tool Calling

不要标记 Node 011 完成。
不要进入 Node 012。
不要 commit / push。

完成后停止，等我真实运行。
```

---

# 三十二、第二阶段真实观察

2026-11-02，用户提供 PromptBasicsDemo --context 的完整实际 B/C 输出。两组均为 qwen3.8-max、HTTP 200、finish_reason=stop、Roles=[user]、消息数 1。这里的 B 是本次重新调用的结果，不覆盖第一阶段 B 的原始记录。

| 项目 | B：有 Context（本次新调用） | C：无 Context |
|---|---:|---:|
| User Prompt 字符数 | 396 | 315 |
| Prompt Tokens | 177 | 147 |
| Completion Tokens | 634 | 396 |
| Total Tokens | 811 | 543 |

## B：有 Context

实际 Messages：

```json
[
  {
    "role": "user",
    "content": "Role:\n你是一名 Java 后端代码审查助手。\n\nTask:\n检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。\n\nContext:\n这段代码位于 Spring Boot 单例 Service 中，同一个 Service 实例可能被多个 HTTP 请求线程并发访问。\n\nInput:\npublic class CounterService {\n    private int count = 0;\n\n    public int increase() {\n        return ++count;\n    }\n}\n\nConstraints:\n1. 最多指出 3 个问题。\n2. 只基于给出的代码和 Context 分析。\n3. 不要虚构不存在的数据库、缓存、接口或调用链。\n\nOutput Format:\n先给一句总体结论。\n然后按“问题 / 原因 / 建议”输出。"
  }
]
```

实际 Content（原文保留）：

````text
## 总体结论

该代码存在严重的并发安全问题，在 Spring Boot 单例 Service 的多线程环境下会导致数据竞争和计数丢失，必须优先修复。

---

### 问题 1：`++count` 操作非原子性导致计数丢失（最高优先级）

- **问题**：`increase()` 方法中的 `++count` 不是原子操作，在并发访问时会产生竞态条件（Race Condition），导致最终计数值小于实际调用次数。
- **原因**：`++count` 实际上包含三个步骤：读取当前值、加 1、写回新值。当多个 HTTP 请求线程同时执行该方法时，可能出现两个线程同时读到相同的旧值（例如都读到 `0`），各自加 1 后写回相同的值（都写回 `1`），从而导致其中一次递增被覆盖丢失。
- **建议**：将 `int count` 替换为 `java.util.concurrent.atomic.AtomicInteger`，并使用其提供的原子方法：
  ```java
  private final AtomicInteger count = new AtomicInteger(0);

  public int increase() {
      return count.incrementAndGet();
  }
  ```

---

### 问题 2：共享可变状态 `count` 缺乏内存可见性保障

- **问题**：实例变量 `count` 没有任何同步机制或可见性修饰，多线程下可能读到过期（脏）数据。
- **原因**：根据 Java 内存模型（JMM），在没有 `synchronized`、`volatile` 或 `Atomic*` 等同步手段的情况下，一个线程对 `count` 的修改不保证对其他线程立即可见。其他线程可能一直从自己工作内存（如 CPU 缓存）中读取旧值，导致业务逻辑基于错误的计数值运行。
- **建议**：采用“问题 1”中建议使用 `AtomicInteger` 即可一并解决此问题，因为 `AtomicInteger` 内部基于 `volatile` 和 CAS 机制，天然保证了跨线程的内存可见性。

---

### 问题 3：返回值无法代表全局真实序号

- **问题**：即使修复了计数丢失问题，`increase()` 返回的值也不能安全地作为“全局唯一递增序号”供调用方使用。
- **原因**：在并发场景下，线程 A 先调用 `increase()` 得到返回值 `1`，线程 B 后调用得到 `2`，但由于线程调度的不确定性，线程 B 完全可能在业务处理上比线程 A 更早拿到结果并往下执行。如果调用方依赖这个返回值来严格判断事件发生的先后顺序或作为业务主键，将会引发逻辑错误。
- **建议**：明确该计数器的用途。如果仅用于统计总调用量（Metrics），当前修复后的方案已足够；如果调用方需要严格的时序标识或全局唯一 ID，不应依赖此方法的返回值，而应考虑引入数据库自增序列、雪花算法（Snowflake）等专门的全局发号机制。
````

## C：无 Context

实际 Messages：

```json
[
  {
    "role": "user",
    "content": "Role:\n你是一名 Java 后端代码审查助手。\n\nTask:\n检查下面代码中最值得优先修复的问题，重点判断是否存在并发安全风险。\n\nInput:\npublic class CounterService {\n    private int count = 0;\n\n    public int increase() {\n        return ++count;\n    }\n}\n\nConstraints:\n1. 最多指出 3 个问题。\n2. 只基于给出的代码分析。\n3. 信息不足时明确说明，不要自行假设对象生命周期或并发场景。\n\nOutput Format:\n先给一句总体结论。\n然后按“问题 / 原因 / 建议”输出。"
  }
]
```

实际 Content（原文保留）：

````text
## 总体结论

该代码存在明显的并发安全风险，`++count` 不是原子操作，在多线程环境下会导致计数丢失。

---

### 问题 1：`++count` 存在竞态条件（并发安全问题）

- **原因**：`++count` 并非原子操作，它包含“读取当前值 -> 加 1 -> 写回新值”三个步骤。当多个线程同时调用 `increase()` 时，可能出现两个线程读到相同的旧值并各自加 1 写回，导致最终结果少算。
- **建议**：将 `int count` 替换为 `java.util.concurrent.atomic.AtomicInteger`，并使用其 `incrementAndGet()` 方法；或者在 `increase()` 方法上加 `synchronized` 关键字。

### 问题 2：缺少内存可见性保障（并发安全问题）

- **原因**：普通变量 `count` 没有使用 `volatile` 修饰，也没有被锁保护。在多线程场景下，一个线程对 `count` 的修改可能只停留在该线程的工作内存中，其他线程无法及时看到最新值。
- **建议**：如果采用 `AtomicInteger`（如问题 1 的建议），其内部基于 `volatile` 语义，可一并解决此问题；若仅加 `synchronized`，同步块的进入和退出也会保证可见性。

### 问题 3：并发使用场景信息不足（约束声明）

- **原因**：根据约束要求，仅基于给出的代码分析，无法确认 `CounterService` 实例是单例还是每次请求新建，也无法确认是否真的会被多线程共享调用。如果是每个线程独占一个实例，则上述并发问题实际不会触发。
- **建议**：明确该类的对象生命周期与调用上下文。但作为通用服务类，强烈建议按线程安全标准编写以防御未知风险。
````

## 观察与边界

- **B 更确定地使用并发背景。**它明确基于 Spring Boot 单例 Service、多 HTTP 请求线程讨论 count 的竞争与可见性。
- **C 承认条件不足。**第三项明确说无法确认对象生命周期或是否被多线程共享，并说明线程独占实例时这些并发问题不会触发。没有自行认定它是 Spring 单例。
- C 的总体结论和前两项仍偏肯定，直到第三项才充分补充运行条件；不能说它从头到尾都完全谨慎。
- **B 的第三项超出了所给用途。**用户没有提供全局 ID、主键或事件排序需求，模型却扩展到了数据库序列、雪花算法等方案。
- B 用“后续处理可能乱序”推导“不能提供唯一递增序号”也不充分：处理/返回完成顺序、分配值的唯一性和跨实例全局唯一是不同性质。在单个共享 AtomicInteger、无溢出/重置的条件下，原子递增分配的值可以互异，但这不保证业务完成顺序，更不自动成为跨进程全局 ID。模型不能混为一个确定缺陷。
- 本次 B/C 输入 Token 为 177/147，输出为 634/396；B 的 Prompt 与第一阶段相同，但两次 B 的输出和用量不同，说明不能把同样 Prompt 视为必然相同的回答。
- C 还改变了“不自行假设”的约束，因此本次只支持“背景及约束影响判断表达”的观察，不能把所有差异全归因于单独删除 Context。
- 实验说明清晰背景有助于给出有依据的分析，但不能保证模型没有推测或错误；正常真实调用不代替新增本地模拟的错误路径验证。

---

# 三十三、为什么这个小实验比继续加 Prompt 技巧更重要？

因为它验证的是 Prompt 最基础的一个原则：

> **模型不会自动知道你没有提供的运行背景。**

如果：

```text
Context 缺失
```

模型只能：

```text
保留条件
或
做推测
```

如果：

```text
Context 明确
```

模型才能把：

```text
可能
```

变成：

```text
基于已知条件的明确判断
```

---

# 三十四、这和“幻觉”有什么关系？

Node 003 学过：

```text
模型可能一本正经补全不存在的信息
```

Prompt 中明确：

```text
信息不足时说明不足
不要自行假设
```

可以帮助降低这种风险。

但仍然不能保证：

```text
绝不幻觉
```

所以：

```text
Prompt 约束
≠
可靠性终点
```

---

# 三十五、这和 Context Engineering 有什么关系？

Node 011 现在只是在一个很小的例子中做：

```text
要不要把“Singleton + 多线程”放进去？
```

Node 019 会把问题扩大成：

```text
这一轮到底应该给模型看到：
System？
History？
RAG？
Memory？
Tool Result？
State？
哪些用户信息？
```

所以现在这个小实验其实已经是：

```text
Context Engineering 的最小雏形
```

---

# 三十六、什么时候算 Node 011 真正完成？

不是：

```text
代码编译通过
```

也不是：

```text
B 回答比 A 好看
```

而是你能真正解释：

```text
Instruction 是什么
Context 是什么
Input 是什么
Output Format 是什么
Role / Task / Constraints 各负责什么
```

并且在真实 Request 中指出它们。

---

# 三十七、Node 011 最终理解问题

你应该能回答：

1. 为什么 A 和 B 都叫 Prompt？
2. 为什么 B 不需要换成 system role 才算“结构化 Prompt”？
3. Instruction 和 Task 有什么关系？
4. Context 和 Input 为什么不能混为一谈？
5. Constraints 为什么是软约束？
6. Output Format 为什么还不是 Structured Output？
7. 为什么 A / B 要保持 Input 一致？
8. 为什么不能在 B 中加入 Few-shot 示例？
9. 为什么结构化 Prompt 往往更耗 Token？
10. 为什么更多 Token 不代表一定更值得？
11. 为什么 C 的“不知道是否并发”可能反而是更严谨的回答？
12. 为什么 Context 缺失时模型不应该被要求“猜对”？
13. 为什么 Prompt 需要真实样本和 A/B，而不是凭感觉优化？
14. 为什么 Prompt 不能存 Secret？
15. 为什么 Prompt 设计最终会过渡到 Context Engineering？

---

# 三十八、口述验收

如果有人问：

> “结构化 Prompt 到底比一句‘帮我看看’多了什么？”

你应该能解释：

> 结构化 Prompt 的价值不是看起来更专业，而是把任务所需的信息拆清楚。Instruction 或 Task 告诉模型要做什么，Context 提供完成任务需要的背景，Input 是本轮真正要处理的数据，Constraints 限制范围，Output Format 说明结果应该怎么表达，Role 则提供处理任务的视角。
>
> 这些内容不一定每次全部都需要，也不是固定模板。核心原则是把重要信息表达清楚，让模型少猜。结构化 Prompt 通常会增加输入 Token，所以要在稳定性、清晰度和成本之间权衡。
>
> Prompt 里的限制仍然只是自然语言软约束，不能替代权限、Schema 或程序校验。真正工程里还需要通过多个样本、A/B 和回归测试验证 Prompt 是否稳定。

如果能自然解释：

```text
Node 011
基本掌握
```

---

# 三十九、Git 与 Secret 检查

## 🛑 停下来操作

完成真实实验后：

```bash
git status
git diff --check
git diff --stat
```

检查：

```text
PromptBasicsDemo.java
check-config.py
011-2 笔记
codex/prompts
codex/handoffs
```

确认没有：

```text
真实 DASHSCOPE_API_KEY
Authorization Header
IDEA 私有配置
```

---

# 四十、Node 011 最终 Checklist

本节阅读讲解与两阶段正常实测已收尾。以下理解题保留供本人回顾，未单独逐题口述或代填自测答案；这不表示缺少对应代码。客观实验项按真实输出勾选。新增本地模拟仍未执行，作为独立待办，不混同于正常调用验证。

## 理论

- [ ] 理解 Prompt / Message / System Prompt 的区别；
- [ ] 理解 Instruction；
- [ ] 理解 Context；
- [ ] 理解 Input；
- [ ] 理解 Output Format；
- [ ] 理解 Role / Task / Constraints；
- [ ] 明白 Prompt 结构不是固定模板；
- [ ] 明白 Prompt 不是越长越好。

## A/B 实验

- [x] 新增 `PromptBasicsDemo.java`；
- [x] A / B 都只有 user role；
- [x] A / B Input 一致；
- [x] Model / 参数一致；
- [x] A 为模糊 Prompt；
- [x] B 包含完整结构；
- [x] 真实 A/B 调用完成；
- [x] 真实 Response 已记录；
- [x] Token Usage 已记录；
- [ ] 能解释差异，不用单次样本过度下结论。

## Context 消融

- [x] B 有明确 Context；
- [x] C 无 Context；
- [x] Input 相同；
- [x] C 明确禁止自行假设运行环境；
- [x] 真实 B/C 对照完成；
- [ ] 能解释 Context 对判断确定性的影响。

## 工程

- [x] 前三个实验入口未破坏；
- [x] 没有引入无关框架；
- [x] 没有 Few-shot；
- [x] 没有 Structured Output；
- [x] Secret 未进入 Git；
- [x] Codex Prompt / Handoff 已保存；
- [x] 模拟结果和真实模型结果明确区分。

完成并理解后：

```text
011-2：✅ PASS
Node 011：✅ PASS
```

下一步：

```text
Node 012 - Few-shot
```

---

# 四十一、Node 011 完成以后，我们新增了什么能力？

Node 008：

```text
会调用模型
```

Node 009：

```text
会管理 Message 和 Chat History
```

Node 010：

```text
会通过 System Prompt 定义高层行为
```

Node 011：

```text
会把一个任务拆成：
Role
Task
Context
Input
Constraints
Output Format
```

于是应用不再只是：

```text
把一句话扔给模型
```

而开始真正：

```text
设计模型输入
```

---

# 四十二、为什么下一步正好是 Few-shot？

到这里，我们已经能通过自然语言：

```text
把任务描述清楚
```

但有些任务即使解释很多，模型仍然可能不完全理解：

```text
分类标准
风格
边界
特殊输出规律
```

这时一种非常自然的方法就是：

```text
直接给模型看例子
```

例如：

```text
输入 A → 输出 A
输入 B → 输出 B
```

让模型从示例中学习本轮任务模式。

这就是 Node 012：

```text
Zero-shot
One-shot
Few-shot
```

所以顺序非常自然：

```text
先会把任务说清楚
↓
再学习用示例补充说明
```

---

# 四十三、停止点

不要在本篇完成前提前做：

```text
Few-shot
CoT
JSON Schema
Structured Output
Prompt Template 系统
Prompt 版本管理
RAG
Tool Calling
```

先把这一次最基础的实验真正跑明白：

```text
模糊 Prompt
vs
结构化 Prompt
vs
去掉关键 Context
```

这三个对比已经足够支撑 Node 011。

---

# 本篇一句话总结

> **Prompt 基础不是“会写漂亮提示词”，而是知道模型完成任务需要哪些信息，并用最清楚、最少歧义的方式把 Task、Context、Input、Constraints 和 Output Format 组织起来。**
