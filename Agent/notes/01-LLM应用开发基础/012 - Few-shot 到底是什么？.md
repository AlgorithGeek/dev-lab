# 012 - Few-shot 到底是什么？

> Phase 1：LLM 应用开发基础\
> Node 012：Few-shot\
> Note 012：Few-shot 理论篇
>
> 前置 Node：
>
> ```text
> Node 008：第一次调用模型 API ✅
> Node 009：Message 模型 ✅
> Node 010：System Prompt ✅
> Node 011：Prompt 基础 ✅
> ```
>
> 当前 Node 目标：
>
> ```text
> Zero-shot
> One-shot
> Few-shot
> ```
>
> 核心问题：
>
> > **为什么给模型看几个“输入 → 输出”的示例，会改变它后续的回答方式？**

---

> **学习收尾（2026-11-03）：**用户确认已读完并理解本篇，Node 012 以理论认知完成验收。本篇即为本 Node 的完整学习内容，不单独安排 Java/API 实验，也不新增实验篇或 Codex 实验记录；本文中的分类和 A/B/C 样例均为理论示意，并非真实运行结果。下一步为 Node 013「Structured Output（准备学习）」。

## 一、为什么学完 Prompt 基础以后，下一步就是 Few-shot？

Node 011 我们已经学会了：

```text
Role
Task
Context
Input
Constraints
Output Format
```

也就是说，我们已经知道：

> **如何把任务说清楚。**

但是现实里有很多任务，即使：

```text
任务描述写得很清楚
```

模型仍然可能：

```text
理解方式不一致
输出风格不一致
分类边界不稳定
格式理解有偏差
```

例如你说：

```text
请把下面工单分类成：

BUG
FEATURE
QUESTION
```

然后输入：

```text
登录页面点按钮没反应。
```

模型大概率知道这是：

```text
BUG
```

但如果遇到：

```text
“希望登录后自动记住上次打开的页面。”
```

它可能理解成：

```text
FEATURE
```

也可能觉得：

```text
QUESTION
```

尤其当分类标准比较：

```text
业务化
模糊
依赖约定
```

这时候，仅靠：

```text
规则说明
```

可能还不够。

我们可以进一步告诉模型：

```text
“你看，像这样的输入，我们希望它输出成这样。”
```

也就是：

```text
Example
```

这就是 Few-shot 开始出现的地方。

---

## 二、先给 Few-shot 一个最简单的定义

可以先记住：

> **Few-shot 是在 Prompt 中提供少量示例，让模型根据这些示例理解任务模式、分类标准、输出风格或映射关系，再处理新的输入。**

例如：

```text
任务：
把工单分类为 BUG / FEATURE / QUESTION。

示例 1：
Input: 登录后页面报 500。
Output: BUG

示例 2：
Input: 希望增加夜间模式。
Output: FEATURE

示例 3：
Input: 如何修改绑定邮箱？
Output: QUESTION

现在分类：
Input: 登录页面点按钮没反应。
```

最后让模型输出：

```text
BUG
```

这里：

```text
3 个示例
```

就是 Few-shot Examples。

---

## 三、Zero-shot、One-shot、Few-shot 到底分别是什么？

这是 Node 012 最核心的一组概念。

### Zero-shot

```text
零示例
```

也就是：

```text
只给任务
不给示范
```

例如：

```text
请把下面工单分类为：

BUG
FEATURE
QUESTION

Input:
登录页面点按钮没反应。
```

这里没有任何示例，所以这是：

```text
Zero-shot
```

### One-shot

```text
一个示例
```

例如：

```text
任务：
把工单分类为 BUG / FEATURE / QUESTION。

示例：
Input: 希望增加夜间模式。
Output: FEATURE

现在分类：
Input: 登录页面点按钮没反应。
```

这里只有一个 `Input → Output` 示例，所以是：

```text
One-shot
```

### Few-shot

```text
少量示例
```

例如：

```text
示例 1：
Input: 登录后页面报 500。
Output: BUG

示例 2：
Input: 希望增加夜间模式。
Output: FEATURE

示例 3：
Input: 如何修改绑定邮箱？
Output: QUESTION
```

然后：

```text
Input:
登录页面点按钮没反应。
```

这就是：

```text
Few-shot
```

---

## 四、“Few” 到底是多少个？

没有一个绝对数字。

例如：

```text
2 个
3 个
5 个
8 个
```

都可能叫 Few-shot。

关键不是：

```text
数量达到几条
```

而是：

```text
不是零
不是只有单个示例
并且示例数量仍然相对有限
```

所以 Few-shot 是一个概念，不是严格数学定义。

---

## 五、为什么示例会影响模型？

最直观的理解是：

> **示例让模型看到“这个任务应该怎么做”。**

光说规则：

```text
请分类。
```

模型需要自己解释：

```text
什么算 BUG？
什么算 FEATURE？
什么算 QUESTION？
```

如果你给：

```text
Input: 登录后报 500
Output: BUG
```

模型就获得一个额外信号：

```text
“这类现象在这个任务中，被归类为 BUG。”
```

如果再给：

```text
Input: 希望增加夜间模式
Output: FEATURE
```

模型进一步理解：

```text
“新增能力类需求 → FEATURE”
```

所以示例的作用可以理解为：

```text
补充任务规则
+
补充边界
+
展示输出形式
```

---

## 六、Few-shot 是不是“教会模型新知识”？

要非常谨慎。

Few-shot 可以影响当前推理，但不是：

```text
真正训练模型参数
```

例如你告诉模型：

```text
示例：
Input: 苹果
Output: 蓝色
```

它可能在当前任务里按照你的规则：

```text
苹果 → 蓝色
```

但这不意味着模型永久学会了“苹果是蓝色”。

下一次完全独立请求里，它不会因为上次示例就永久修改参数。

所以：

```text
Few-shot
≠
Training
```

---

## 七、Few-shot 和 Fine-tuning 的区别

这是 Node 012 最需要讲透的一点。

### Few-shot

发生在：

```text
Inference
```

做法：

```text
把示例放进当前 Prompt / Context
```

模型参数：

```text
不改变
```

生命周期：

```text
当前请求 / 当前 Context
```

### Fine-tuning

发生在：

```text
Training
```

做法：

```text
使用训练数据更新模型参数
```

模型参数：

```text
会改变
```

生命周期：

```text
训练后的模型版本
```

可以记成：

```text
Few-shot
=
给模型“看例子”

Fine-tuning
=
真正修改模型
```

---

## 八、Few-shot 和 Memory / Chat History 的区别

Few-shot Example：

```text
是当前 Prompt 的一部分
```

Memory：

```text
是应用为了跨时间保存信息的机制
```

Chat History：

```text
是过去真实发生的 User / Assistant 对话
```

而 Few-shot Example：

```text
是应用人为放进去的示范
```

它们形式可能很像，但语义不同。

---

## 九、为什么 Few-shot Example 看起来很像多轮对话？

因为常见实现会写成：

```text
User:
输入 1

Assistant:
输出 1

User:
输入 2

Assistant:
输出 2

User:
新输入
```

这在 Chat Completions 里可以自然表达成：

```text
user
assistant
user
assistant
user
```

所以形式上和 Chat History 很像。

但用途不同：

```text
Chat History
=
真实历史

Few-shot Example
=
示范
```

---

## 十、Few-shot 可以帮助什么？

常见作用包括：

```text
分类
格式
风格
边界判断
任务映射
术语解释
标签体系
```

尤其是分类任务，非常适合 Few-shot。

因为分类任务通常有有限标签：

```text
BUG
FEATURE
QUESTION
```

模型可以从示例中理解：

```text
输入特征
↓
标签
```

---

## 十一、Few-shot 也可以影响输出风格

例如：

```text
示例：

Input:
线程池为什么会 OOM？

Output:
结论：线程池队列过大或任务堆积可能导致内存持续增长。
```

如果你给多个类似例子，

模型可能更倾向：

```text
都先写“结论：”
```

这说明示例不仅告诉模型：

```text
答什么
```

也告诉它：

```text
怎么答
```

---

## 十二、Few-shot 可以传递“隐含规则”

有时候你不需要把所有规则都写出来。

例如：

```text
示例 1：
Input: 登录报 500
Output: BUG

示例 2：
Input: 希望加夜间模式
Output: FEATURE

示例 3：
Input: 如何修改邮箱
Output: QUESTION
```

模型可以从示例中推断：

```text
现有功能异常 → BUG
新能力需求 → FEATURE
使用咨询 → QUESTION
```

也就是说：

> **示例本身就是任务规范的一部分。**

---

## 十三、但 Few-shot 不能替代 Instruction

如果任务规则很重要，最好：

```text
规则说明
+
示例
```

一起使用。

例如：

```text
分类定义：

BUG：
现有功能与预期不一致。

FEATURE：
希望增加新能力。

QUESTION：
询问现有功能如何使用。

示例：
...
```

这通常比只给示例让模型自己猜更稳定。

所以 Few-shot 更像是：

```text
补充 Instruction
```

而不是替代它。

---

## 十四、Few-shot 和 Prompt 基础是什么关系？

Node 011：

```text
Role
Task
Context
Input
Constraints
Output Format
```

Node 012：

```text
在这些基础上
再加入 Examples
```

所以：

```text
Few-shot
=
Prompt 结构中的“示例层”
```

---

## 十五、一个完整 Few-shot Prompt 可以长什么样？

例如：

```text
Task:
把用户工单分类为 BUG、FEATURE、QUESTION。

Definitions:
BUG = 现有功能异常。
FEATURE = 新功能或改进需求。
QUESTION = 使用咨询。

Examples:

Input:
登录后页面报 500。
Output:
BUG

Input:
希望增加夜间模式。
Output:
FEATURE

Input:
怎么修改绑定邮箱？
Output:
QUESTION

Current Input:
登录页面点按钮没反应。

Output:
```

这里包含：

```text
Task
Definitions
Examples
Current Input
```

---

## 十六、示例质量为什么非常重要？

因为模型会模仿示例。

如果示例本身：

```text
错误
模糊
矛盾
```

模型就可能学到错误模式。

例如：

```text
Input:
登录报 500

Output:
FEATURE
```

如果这是错误标签，

模型可能把错误规则当成任务标准。

所以：

> **Few-shot 的质量高度依赖 Example 质量。**

---

## 十七、示例要不要覆盖不同类别？

通常应该。

如果你有：

```text
BUG
FEATURE
QUESTION
```

但 Few-shot 只给：

```text
3 个 BUG
```

模型会获得一种偏斜信号。

更合理的是：

```text
每个重要类别至少有代表性示例
```

---

## 十八、示例是否应该覆盖边界情况？

应该。

只给特别简单的例子：

```text
500 → BUG
新功能 → FEATURE
怎么用 → QUESTION
```

可能还不够。

真实任务经常难在：

```text
边界样本
```

例如：

```text
“希望登录后不要每次都重新输入验证码。”
```

到底是 BUG 还是 FEATURE，要看业务定义。

所以好的 Few-shot Example 应该帮助模型理解边界，而不是只展示最简单情况。

---

## 十九、Few-shot 为什么可能带来偏差？

因为示例会影响模型判断。

如果示例选择有偏，模型也可能有偏。

例如：

```text
所有 BUG 示例都特别严重
```

模型可能把轻微功能异常更容易错分成 QUESTION 或 FEATURE。

这可以理解为：

```text
Example Bias
```

---

## 二十、示例顺序会不会影响结果？

可能会。

因为模型看到的是：

```text
有顺序的 Context
```

不同顺序可能影响最近示例的显著性。

尤其当：

```text
示例很多
示例冲突
类别分布不均
```

时更明显。

所以真实实验里：

```text
顺序
```

也是可能需要控制的变量。

---

## 二十一、为什么不能把很多示例无脑全部塞进去？

因为示例会消耗：

```text
Input Token
```

如果：

```text
100 个示例
```

Prompt 会变得很长。

可能导致：

```text
Token 成本上升
延迟上升
Context Window 被占用
噪声增加
```

所以：

```text
示例更多
≠
一定更好
```

---

## 二十二、Few-shot 和 Token 是什么关系？

Few-shot Example 属于模型输入，所以会增加：

```text
prompt_tokens
```

Zero-shot：

```text
Task + Input
```

One-shot：

```text
Task + 1 Example + Input
```

Few-shot：

```text
Task + N Examples + Input
```

通常：

```text
示例越多
↓
prompt_tokens 越高
```

---

## 二十三、Few-shot 的收益必须和成本权衡

如果：

```text
Zero-shot 已经很好
```

那可能不值得塞很多示例。

反过来：

```text
Zero-shot 边界很不稳定
```

Few-shot 可能非常值得。

真实工程需要权衡：

```text
效果
Token
延迟
维护成本
```

---

## 二十四、One-shot 有什么意义？

One-shot 很适合展示：

```text
格式
```

或者某一种典型映射。

例如：

```text
Input:
用户不存在

Output:
ERROR_USER_NOT_FOUND
```

然后新输入：

```text
密码错误
```

模型可能更容易输出：

```text
ERROR_PASSWORD_INVALID
```

---

## 二十五、Few-shot 比 One-shot 强在哪里？

Few-shot 可以提供：

```text
多个模式
多个类别
多个边界
```

如果只给一个 FEATURE 示例，

模型对 BUG / QUESTION 仍然主要靠原有理解。

给多个示例后，任务空间会更清楚。

---

## 二十六、Few-shot 示例应该和真实输入同格式吗？

通常建议尽量一致。

例如真实输入是：

```text
用户工单文本
```

示例也用：

```text
用户工单文本
```

输出格式也最好统一。

如果有的示例输出：

```text
BUG
```

有的输出：

```text
这是一个 FEATURE
```

模型可能不知道最终应该输出哪种形式。

---

## 二十七、Few-shot 可以帮助 Output Format 吗？

可以。

例如要求：

```text
只输出标签
```

再给：

```text
Input: 登录报 500
Output: BUG
```

模型会更容易理解：

```text
不要解释
只输出标签
```

这就是示例强化格式。

---

## 二十八、但 Few-shot 仍然不能替代 Structured Output

非常重要。

就算你给：

```text
10 个 JSON 示例
```

模型也只是：

```text
更倾向输出 JSON
```

不代表：

```text
严格 Schema 保证
```

真正的：

```text
JSON Schema
strict
DTO
```

留给 Node 013～015。

---

## 二十九、如果示例彼此矛盾会怎样？

例如：

```text
Input:
登录报 500

Example 1 Output:
BUG

Example 2 Output:
QUESTION
```

模型就会遇到冲突。

它可能：

```text
随机偏向某一个
尝试解释
忽略部分示例
```

所以：

> **Few-shot 最怕示例标准不一致。**

---

## 三十、Few-shot 可以传递“业务定义”

例如你的公司定义：

```text
“现有功能优化”
也算 FEATURE
```

这可能和一般人的理解不同。

你可以通过：

```text
Instruction
+
Few-shot Examples
```

一起让模型理解内部标签规则。

这就是 Few-shot 很实用的地方。

---

## 三十一、Few-shot 是不是让模型“记住”示例？

在当前 Context 内：

```text
模型能利用这些示例
```

但不是长期永久记忆。

下一次请求如果不再传这些 Example，

不能假设模型自动记得。

这和 System Prompt、Chat History 的逻辑一致。

---

## 三十二、Few-shot 和 Context 是什么关系？

Few-shot Examples：

```text
属于 Context 的一部分
```

模型本轮可能看到：

```text
System
Task
Definitions
Examples
Current Input
```

这些共同组成 Context。

所以 Few-shot 其实也是：

```text
Context Engineering 的一个输入来源
```

---

## 三十三、什么时候适合 Few-shot？

常见场景：

```text
分类标准不够直观
输出风格需要稳定
业务术语有内部定义
边界案例复杂
希望模型模仿某种格式
```

---

## 三十四、什么时候 Zero-shot 就够了？

例如：

```text
翻译一句简单英文
解释一个常见概念
总结一段文本
```

模型本身已经非常擅长。

这时候额外示例可能只是增加成本。

所以：

```text
不是所有任务都需要 Few-shot
```

---

## 三十五、什么时候 Few-shot 反而可能有害？

例如：

```text
示例错误
示例过时
示例偏差严重
示例和真实任务不一致
示例数量过多
示例彼此矛盾
```

都会误导模型。

---

## 三十六、未来业务需要时，怎样用 A/B/C 评估 Few-shot？

如果以后真实业务遇到分类边界不稳定，可以用以下三组评估是否值得加入示例：

```text
Zero-shot
One-shot
Few-shot
```

可以形成对照组。这是评估方法的认知，不是本 Node 的必做实验。

例如：

```text
A：Zero-shot
B：One-shot
C：Few-shot
```

保持：

```text
同一个 Model
同一个 Current Input
同一组参数
```

只改变：

```text
示例数量
```

这样非常适合观察：

```text
模型行为
格式稳定性
Token 成本
```

---

## 三十七、未来业务评估的理论示例

下面仅用工单分类说明如何比较不同示例数量；不要求当前实现 Java Demo，也没有实际 API 调用结果。假设任务是：

```text
工单分类
```

分类：

```text
BUG
FEATURE
QUESTION
```

当前 Input：

```text
登录页面点击提交按钮没有任何反应。
```

### A：Zero-shot

```text
请分类为 BUG / FEATURE / QUESTION。

Input:
登录页面点击提交按钮没有任何反应。
```

### B：One-shot

```text
请分类为 BUG / FEATURE / QUESTION。

Example:
Input:
希望增加夜间模式。

Output:
FEATURE

Current Input:
登录页面点击提交按钮没有任何反应。
```

### C：Few-shot

```text
请分类为 BUG / FEATURE / QUESTION。

Example 1:
Input:
登录后页面报 500。

Output:
BUG

Example 2:
Input:
希望增加夜间模式。

Output:
FEATURE

Example 3:
Input:
如何修改绑定邮箱？

Output:
QUESTION

Current Input:
登录页面点击提交按钮没有任何反应。
```

---

## 三十八、为什么这个实验不能只看“是否答 BUG”？

因为这个例子本身太容易。

Zero-shot 也可能正确。

如果以后在业务中评估，应考虑再加入：

```text
边界样本
```

例如：

```text
希望登录成功后自动跳回上次访问页面。
```

这更可能体现示例和定义对分类边界的影响。

---

## 三十九、以后评估 Few-shot 时应该观察什么？

不仅看最终标签，还看：

```text
格式
解释
边界
一致性
Token
```

例如：

```text
是否只输出标签
是否出现额外解释
不同样本是否稳定
```

---

## 四十、为什么不能用一次实验决定“Few-shot 一定更好”？

因为：

```text
一个样本
```

不代表：

```text
整个任务分布
```

真正判断 Few-shot 是否值得，需要看：

```text
多个测试样本
准确率
格式稳定性
成本
```

Node 012 只建立这个评估意识；是否值得加入示例，留到真实业务需要时再结合样本集和 Evaluation 判断。

---

## 四十一、Few-shot Example 应该怎样选？

可以记住几个原则：

```text
正确
代表性
一致
覆盖边界
贴近真实输入
```

### 正确

示例标签必须可靠。

### 代表性

不要只选特别简单案例。

### 一致

示例之间规则一致、格式一致。

### 覆盖边界

最好覆盖容易混淆的情况。

### 贴近真实输入

示例形式尽量与真实请求相似。

---

## 四十二、Few-shot 和 RAG / Tool Calling 的区别

Few-shot：

```text
给模型任务示范
```

RAG：

```text
给模型检索到的知识
```

Tool Calling：

```text
让模型产生工具调用
程序执行真实动作
```

所以：

```text
Examples
≠
Knowledge Retrieval
≠
Tools
```

---

## 四十三、Few-shot 示例里能不能放真实敏感数据？

不应该随便放。

只要示例进入模型 Context，

就要假设模型看到了它。

因此：

```text
API Key
密码
用户隐私
真实 Secret
```

不应该拿来当 Example。

学习阶段最好使用虚构示例。

---

## 四十四、Few-shot 和模型能力是什么关系？

示例可以引导模型已有能力，

但不能凭空创造模型完全不具备的能力。

所以：

```text
Few-shot
=
Behavior Guidance
```

不是：

```text
Capability Creation
```

---

## 四十五、Few-shot 的本质可以怎样理解？

最简单：

> **用例子告诉模型“这个任务应该长什么样”。**

再进一步：

```text
Instruction
=
用语言描述规则

Few-shot
=
用示例展示规则
```

两者一起：

```text
规则说明
+
任务示范
```

---

## 四十六、把 Zero-shot / One-shot / Few-shot 放在一起

```text
Zero-shot
=
任务说明
+
当前输入

One-shot
=
任务说明
+
1 个示例
+
当前输入

Few-shot
=
任务说明
+
少量示例
+
当前输入
```

---

## 四十七、把 Few-shot 放进我们已经学过的知识链

### Node 009：Message

Example 可以通过 Message 表达。

### Node 010：System Prompt

System 可以定义角色和规则，Few-shot 可以展示规则如何落地。

### Node 011：Prompt 基础

Prompt 有：

```text
Task
Context
Input
Constraints
Output Format
```

Few-shot 又增加：

```text
Examples
```

### Node 013～015

Few-shot 可以帮助模型更像 JSON，但不能替代 Structured Output、Schema、DTO。

### Node 019

Example 也属于 Context Engineering 的一部分。

---

## 四十八、十个最常见误区

1. “Few-shot 就是 Fine-tuning。” —— 错。
2. “示例越多越好。” —— 错。
3. “Few-shot 一定比 Zero-shot 准。” —— 错。
4. “只要给例子，模型就会永久记住。” —— 错。
5. “Few-shot 可以替代 Instruction。” —— 错。
6. “示例随便挑几个就行。” —— 错。
7. “示例标签错一点没关系。” —— 错。
8. “Few-shot 能保证格式。” —— 错。
9. “Few-shot 和 Chat History 是一回事。” —— 错。
10. “Few-shot 就是给模型更多知识。” —— 不准确。

---

## 四十九、把 Few-shot 压缩成一张图

```text
                 Task / Instruction
                        │
                        ▼
                    Examples
              ┌─────────┼─────────┐
              ▼         ▼         ▼
           Example1  Example2  Example3
              │         │         │
              └─────────┼─────────┘
                        │
                        ▼
                  Current Input
                        │
                        ▼
                      Model
                        │
                        ▼
                     Output
```

---

## 五十、真正需要记住的 15 个结论

1. Zero-shot 是不给示例，直接让模型完成任务。
2. One-shot 是提供一个示例。
3. Few-shot 是提供少量示例。
4. Few-shot Example 属于当前 Context。
5. Few-shot 不会修改模型参数。
6. Few-shot 不等于 Fine-tuning。
7. Few-shot 不等于 Memory。
8. Few-shot 不等于 Chat History。
9. Few-shot 可以帮助模型理解分类边界、输出风格和任务模式。
10. 示例本身就是任务规范的一部分。
11. 示例质量、覆盖面和一致性非常重要。
12. 示例越多会消耗更多 Token，不一定越好。
13. Few-shot 仍然不能替代 Structured Output。
14. Few-shot 是否值得需要通过真实实验评估。
15. 一个好的 Few-shot 方案需要在效果、Token、延迟和维护成本之间权衡。

---

## 五十一、理解自测

### 基础

1. 什么是 Zero-shot？
2. 什么是 One-shot？
3. 什么是 Few-shot？
4. Few-shot 为什么会影响模型输出？
5. Few-shot 会不会改变模型参数？

### 概念辨析

6. Few-shot 和 Fine-tuning 有什么区别？
7. Few-shot 和 Chat History 有什么区别？
8. Few-shot 和 Memory 有什么区别？
9. Few-shot 和 RAG 有什么区别？
10. Few-shot 和 Structured Output 有什么区别？

### 示例设计

11. 为什么示例必须正确？
12. 为什么示例需要覆盖不同类别？
13. 为什么示例应该覆盖边界情况？
14. 为什么示例格式最好统一？
15. 为什么示例顺序可能影响结果？
16. 为什么示例不是越多越好？

### 工程理解

17. Few-shot 为什么会增加 prompt_tokens？
18. 为什么不能只跑一个样本就说 Few-shot 一定更好？
19. 什么情况下 Zero-shot 已经足够？
20. 为什么真实 Few-shot 需要 Evaluation？

---

## 五十二、口述验收

如果有人问：

> “Few-shot 不就是给模型几个例子吗？为什么单独学？”

你应该能解释：

> Few-shot 的确是给模型少量示例，但核心不是“多写几条内容”，而是用示例展示这个任务真正的输入输出模式、分类边界和回答风格。相比只用自然语言描述规则，示例可以让模型看到“这个任务应该怎么做”。
>
> Zero-shot 是没有示例，One-shot 是一个示例，Few-shot 是少量示例。它们都发生在推理阶段，示例只是进入当前 Context，并不会修改模型参数，所以 Few-shot 和 Fine-tuning 完全不同。
>
> 示例也会消耗 Token，而且错误、偏斜或互相矛盾的示例会误导模型。因此 Few-shot 不是越多越好，而需要选择正确、代表性强、边界清楚的示例，并通过真实测试判断它是否值得。

如果能自然讲清：

```text
012
基本理解完成
```

---

## 五十三、Node 012 完成标准

- [x] 理解 Zero-shot；
- [x] 理解 One-shot；
- [x] 理解 Few-shot；
- [x] 理解示例为什么能影响模型；
- [x] 明白 Few-shot 不修改模型参数；
- [x] 能区分 Few-shot 与 Fine-tuning；
- [x] 能区分 Few-shot 与 Chat History；
- [x] 能区分 Few-shot 与 Memory；
- [x] 理解 Example 属于 Context；
- [x] 理解示例质量的重要性；
- [x] 理解示例覆盖和边界；
- [x] 理解示例可能引入偏差；
- [x] 理解示例会增加 Token；
- [x] 明白 Few-shot 不一定优于 Zero-shot；
- [x] 明白 Few-shot 不能替代 Structured Output。

以上理解项依据用户已读完并确认理论理解记录，不另行强制逐题口述，也不以 API 实验作为完成条件。

完成状态：

```text
012 理论篇：✅ 完成
Node 012：Few-shot ✅ PASS

本 Node 不单独安排 Java 实验。
下一步：Node 013 - Structured Output（准备学习）
```

---

## 五十四、Node 012 在理论理解后收尾

本 Node 的目标是理解 Zero-shot、One-shot、Few-shot，以及示例为什么能够影响模型行为。本篇已覆盖这些概念，读完并理解后即可结束 Node 012。

当前不单独创建 Few-shot Java 实验、额外实验笔记或 Codex 实验 Prompt / Handoff，也不为了形式统一安排真实 API A/B/C 调用或 Token 实验。

保留下面的评估思路，供以后真实业务需要时使用：

```text
遇到分类边界不稳定或输出风格不一致
↓
先明确任务规则，检查是否需要示例
↓
用代表性样本比较 Zero-shot / One-shot / Few-shot
↓
结合效果、Token、延迟和维护成本评估
```

不能只凭一次正确回答就认定 Few-shot 更好，也不能把本文的示例当作已经完成的实测。只有在业务收益值得时再加入示例。

---

## 五十五、Node 012 和 Node 013 的边界

Node 012：

```text
Examples
```

解决：

```text
模型如何更好理解任务模式
```

Node 013：

```text
Structured Output
```

解决：

```text
模型输出怎样变成更适合程序消费的结构
```

即使 Few-shot 示例里写：

```json
{"label":"BUG"}
```

也仍然不代表已经学会 Structured Output。

---

## 五十六、停止点与下一步

本篇读完并理解后，Node 012 即可结束。

当前不单独创建 Few-shot Java 实验。真正遇到需要 Few-shot 的业务场景时，再通过样本集和 Evaluation 验证收益。

当前状态：

```text
Node 012：Few-shot ✅
```

下一步进入：

```text
Node 013 - Structured Output（准备学习）
```

Node 013 将从下面的问题开始：

```text
自然语言输出
↓
结构化输出
↓
为什么程序和企业应用更喜欢结构化数据
```

本次只完成 Node 012 收尾与进度同步，不提前创建 Node 013 笔记。后续更具体的结构契约及 Java 数据映射，按既有路线继续学习。

---

# 本篇一句话总结

> **Few-shot 的本质不是“让模型多看几句话”，而是用少量高质量示例向模型展示：这个任务到底应该怎么做。**

**Node 012：Few-shot ✅ 已完成理论学习；下一步：Node 013 - Structured Output（准备学习）。**
