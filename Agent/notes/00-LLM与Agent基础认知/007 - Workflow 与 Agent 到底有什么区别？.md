# 007 - Workflow 与 Agent 到底有什么区别？

前面我们已经完成了：

```
Node 001：LLM 到底是什么
Node 002：Training / Inference
Node 003：LLM 幻觉
Node 004：Token
Node 005：Context Window
Node 006：什么是 Agent
```

到了 Node 006，我们第一次真正建立了 Agent 的核心心智模型：

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

并且知道：

```
User
 ↓
LLM
 ↓
Decision
 ↓
Tool
 ↓
Observation
 ↓
LLM
 ↓
Decision
 ↓
...
 ↓
Answer
```

但是理解 Agent 以后，一个非常自然的问题马上就会出现：

> **既然 Agent 可以自己决定下一步该做什么，那为什么以后所有 AI 应用都不直接做成 Agent？**

比如：

```
注册用户
处理订单
创建广告
修改预算
审批流程
查询数据
生成报表
```

是不是全部都可以交给 Agent？

答案当然不是。

甚至在真实生产环境中，很多场景：

> **越不 Agent，反而越可靠。**

这就是这一篇最核心的问题：

```
Workflow
vs
Agent
```

# 一、先给 Workflow 一个最简单的定义

Workflow 可以先理解为：

> **一条主要由程序员提前定义好的任务执行流程。**

例如：

```
用户提交订单
↓
校验参数
↓
检查库存
↓
创建订单
↓
扣减库存
↓
发送消息
↓
完成
```

流程已经提前写好了。

也就是：

```
A
↓
B
↓
C
↓
D
```

程序执行时，不需要临时问：

```
下一步该干什么？
```

因为：

```
下一步
```

早就已经由程序员定义好了。

# 二、最简单的 Java Workflow

比如：

```
public OrderResult createOrder(CreateOrderRequest request) {

    validateRequest(request);

    Product product =
            productService.getProduct(request.getProductId());

    checkStock(product, request.getCount());

    Order order =
            orderService.create(request);

    stockService.deduct(
            request.getProductId(),
            request.getCount()
    );

    messageService.sendOrderCreated(order);

    return OrderResult.success(order);
}
```

这里整个执行路径基本已经确定：

```
validate
↓
query product
↓
check stock
↓
create order
↓
deduct stock
↓
send message
```

这就是一种非常典型的：

```
Workflow
```

# 三、Workflow 最重要的特点：路径由程序控制

例如：

```
if (campaign.isActive()) {
    queryMetrics();
} else {
    return "Campaign 已暂停";
}
```

虽然这里也有：

```
分支
```

但分支依然是：

```
程序员提前定义
```

的。

即使有：

```
if
else
switch
for
while
```

它仍然可以是 Workflow。

# 四、有分支不代表就是 Agent

这一点非常重要。

例如：

```
        A
        │
        ▼
      判断
     /    \
    /      \
   B        C
    \      /
     \    /
       D
```

虽然路径不是简单：

```
A → B → C
```

但如果：

```
什么时候走 B
什么时候走 C
```

已经被程序员写死：

```
if (score >= 80) {
    doB();
} else {
    doC();
}
```

那么这仍然属于：

```
Workflow
```

# 五、那 Agent 最大的区别是什么？

Agent 的一个核心特征是：

> **某些执行路径不是程序员提前完全写死，而是模型根据当前 Context 动态决定。**

例如用户说：

```
帮我分析 Campaign 123 为什么今天没有消耗。
```

系统拥有：

```
getCampaign
getMetrics
getAccount
getCreative
getBudget
```

程序并没有写死：

```
必须按：

getCampaign
↓
getMetrics
↓
getAccount
↓
getCreative
↓
getBudget
```

执行。

而是：

```
LLM
↓
根据当前情况选择下一步
```

# 六、Agent 的路径可以动态变化

情况一：

```
getCampaign
↓
PAUSED
```

模型可能直接结束：

```
Campaign 已暂停，所以没有消耗。
```

路径：

```
getCampaign
↓
Finish
```

情况二：

```
getCampaign
↓
ACTIVE
```

模型继续：

```
getMetrics
↓
0 Impression
```

再继续：

```
getAccount
↓
SUSPENDED
```

最后结束。

路径：

```
getCampaign
↓
getMetrics
↓
getAccount
↓
Finish
```

情况三：

Account 正常。

模型可能继续：

```
getCreative
↓
REJECTED
↓
Finish
```

所以不同任务实例：

```
可能走不同路径
```

# 七、Workflow 和 Agent 最核心的区别

可以先记成：

```
Workflow
=
路径主要由程序定义
```

而：

```
Agent
=
路径可以由模型动态决定
```

这句话非常重要。

# 八、再说得更准确一点

Workflow 的核心问题是：

> **程序应该执行哪一步？**

答案通常已经写在：

```
代码 / 流程定义
```

里。

Agent 的核心问题是：

> **现在下一步最合理的行动是什么？**

答案可能需要：

```
LLM
```

动态判断。

# 九、一个特别简单的对比

## Workflow

```
User
↓
Step A
↓
Step B
↓
Step C
↓
Answer
```

## Agent

```
               ┌→ Tool A
               │
User → LLM ────┼→ Tool B
               │
               ├→ Tool C
               │
               └→ Finish
```

# 十、但是千万不要形成“Workflow 没有 LLM”的误解

Workflow 完全可以包含：

```
LLM
```

例如：

```
用户提交文本
↓
LLM 分类
↓
程序读取分类结果
↓
规则判断
↓
LLM 总结
↓
完成
```

这里有两个 LLM 调用。

但整个执行路径仍然：

```
提前确定
```

所以它依然可以是：

```
Workflow
```

# 十一、LLM Application 不等于 Agent

例如：

```
用户输入文章
↓
LLM 总结
↓
结束
```

这是：

```
LLM Application
```

不是 Agent。

再例如：

```
用户输入投诉
↓
LLM 判断投诉类型
↓
Java switch
↓
调用对应业务接口
↓
LLM 生成回复
```

这仍然可能主要是：

```
Workflow
```

因为：

```
整体路径由程序控制
```

# 十二、这意味着 Agent 的关键不是“用了 LLM”

而是：

> **LLM 是否参与了执行路径的动态决策。**

这是这一篇非常重要的判断标准。

# 十三、Workflow 与 Agent 不是简单二选一

现在开始进入这一篇最重要的思想。

不要把系统想成：

```
Workflow
或
Agent
```

两个完全互斥的箱子。

真实系统更像：

```
一条连续光谱
```

# 十四、可以画成一条光谱

```
完全确定性
│
│ 普通 Java 代码
│
▼
固定 Workflow
│
▼
Workflow + LLM
│
▼
Workflow + Agent 节点
│
▼
Agentic Workflow
│
▼
高度 Agentic Agent
│
▼
大量路径由模型动态决定
```

系统可以位于：

```
任何一个位置
```

# 十五、第一级：完全确定性程序

例如：

```
用户注册
```

流程：

```
校验手机号
↓
校验验证码
↓
检查用户是否存在
↓
写数据库
↓
返回结果
```

完全不需要 LLM。

这种系统：

```
Traditional Application
```

# 十六、第二级：固定 Workflow + LLM

例如：

```
智能工单分类
```

流程：

```
用户提交工单
↓
LLM 分类
↓
程序根据分类路由
↓
数据库保存
↓
LLM 生成回复
↓
完成
```

虽然使用了 LLM。

但：

```
第一步干什么
第二步干什么
第三步干什么
```

大体由程序决定。

# 十七、第三级：Workflow 中加入 Agent 节点

例如：

```
创建广告
```

整体流程：

```
参数校验
↓
权限校验
↓
素材分析 Agent
↓
人工审批
↓
调用平台 API
↓
保存数据库
```

只有：

```
素材分析 Agent
```

内部可能自己：

```
搜规则
查素材
查历史
调用 Tool
反复推理
```

但整个外部业务流程依然固定。

这种结构非常常见。

# 十八、第四级：Agentic Workflow

Agentic Workflow 可以先理解为：

> **整体仍然有一定流程结构，但其中多个节点允许模型做动态决策。**

比如：

```
用户目标
↓
任务分类
↓
LLM 判断下一步
↓
子任务 Workflow
↓
LLM 根据结果重新决策
↓
可能调用其他 Workflow
↓
结束
```

既有：

```
Workflow
```

又有：

```
Agentic Decision
```

# 十九、第五级：高度 Agentic

例如给 Agent 一个目标：

```
帮我分析最近一个月广告账户表现变差的原因，并输出报告。
```

系统可能只提供：

```
Tools
Data Access
Browser
RAG
Memory
```

然后让模型：

```
自己判断查什么
自己决定顺序
自己判断什么时候结束
```

这就是更高程度的：

```
Agentic
```

# 二十、“Agentic”到底是什么意思？

Agentic 可以先简单理解成：

> **系统拥有一定程度由模型动态决定行动的能力。**

它不是一个严格的：

```
0 / 1
```

开关。

更像：

```
程度
```

# 二十一、所以“这个系统是不是 Agent”有时没有绝对答案

现实项目中：

```
Agent
Workflow
LLM Workflow
Agentic Workflow
```

这些词的边界并没有全行业统一到数学定义。

有人可能把：

```
一个会 Tool Calling 的 LLM
```

叫 Agent。

有人可能认为：

```
必须有多步 Loop
```

才算 Agent。

还有人会要求：

```
Planning
Memory
Autonomy
```

才能算。

所以工程中：

> **不要过分纠结标签。**

更重要的是问：

```
谁决定下一步？

哪些步骤是固定的？

哪些步骤由模型决定？

模型拥有多大权限？

系统怎么结束？

失败怎么处理？
```

# 二十二、为什么很多场景应该优先 Workflow？

因为 Workflow 最大优势是：

```
确定性
```

例如：

```
支付
```

你不希望模型突然决定：

```
这次先不校验余额了。
```

# 二十三、确定性意味着什么？

相同输入和状态下：

```
执行路径大体可预测
```

例如：

```
请求
↓
校验
↓
查询
↓
更新
↓
返回
```

工程师可以明确知道：

```
系统会发生什么
```

# 二十四、Workflow 更容易测试

例如：

```
A
↓
B
↓
C
```

你可以分别写：

```
Test A
Test B
Test C
```

也可以验证：

```
A 成功后一定调用 B
B 失败后一定终止
C 失败后执行 Retry
```

# 二十五、Agent 测试为什么更难？

Agent 可能：

```
这次走 A → B
```

下次：

```
A → C
```

再下次：

```
B → D → A
```

即使最终都正确：

```
路径也可能不同
```

所以传统：

```
assertEquals
```

测试方式不一定完全够用。

以后需要：

```
Eval
Trace
Trajectory Evaluation
```

# 二十六、Workflow 更容易 Debug

如果流程：

```
A
↓
B
↓
C
```

错误发生在：

```
B
```

你可以直接：

```
查 B 日志
```

Agent 可能是：

```
模型为什么选了 B？
↓
为什么没选 C？
↓
Context 当时是什么？
↓
Tool Description 是什么？
↓
Observation 是否正确？
```

Debug 难度明显更高。

# 二十七、Workflow 更容易做权限控制

例如：

```
审批通过
↓
才能调用 updateBudget
```

这是程序写死：

```
if (!approved) {
    throw new PermissionDeniedException();
}
```

非常明确。

Agent 如果自由度很高：

```
LLM
↓
决定调用 updateBudget
```

就必须额外确保：

```
Runtime
```

不会因为模型请求：

```
就直接执行
```

# 二十八、Workflow 更容易控制成本

假设 Workflow 明确：

```
调用 LLM 两次
```

成本比较可预测。

Agent：

```
LLM
↓
Tool
↓
LLM
↓
Tool
↓
LLM
↓
Tool
↓
...
```

有时：

```
3 次模型调用
```

有时：

```
20 次
```

所以成本更不稳定。

# 二十九、Workflow 更容易控制延迟

固定 Workflow：

```
A 100ms
B 200ms
C 300ms
```

大概：

```
600ms
```

Agent 可能：

```
模型推理
↓
Tool
↓
模型推理
↓
Tool
↓
网络
↓
Retry
↓
再推理
```

延迟更难预测。

# 三十、Workflow 更容易保证业务规则

例如：

```
订单金额 > 10000
必须人工审批
```

应该：

```
if (amount > 10000) {
    requireApproval();
}
```

而不是：

```
Prompt：
金额比较大时，请尽量考虑人工审批。
```

这是两种完全不同的可靠性。

# 三十一、一个非常重要的原则

> **必须永远满足的规则，不应该只依赖 LLM 自觉遵守。**

例如：

```
权限
金额上限
数据范围
安全限制
法律约束
审批流程
```

这些更适合：

```
程序
Workflow
Policy Engine
```

控制。

# 三十二、那 Agent 的优势是什么？

既然 Workflow 这么好，为什么还需要 Agent？

因为 Workflow 有一个天然问题：

> **程序员必须提前知道可能有哪些路径。**

# 三十三、简单问题可以穷举

例如：

```
status == ACTIVE
status == PAUSED
status == DELETED
```

三种状态。

程序员很好写：

```
switch (status) {
    ...
}
```

# 三十四、复杂开放问题很难穷举

比如：

```
为什么这个广告账户最近表现变差？
```

可能原因：

```
预算
账户限制
素材
受众
出价
平台异常
时间
地区
竞争
审核
Tracking
转化事件
```

而且：

```
原因之间还会组合
```

# 三十五、如果全部写 Workflow 会怎样？

你可能需要：

```
if A
  do X

if B
  do Y

if C && D
  do Z

if A && C && !E
  ...
```

最后变成：

```
巨大的规则树
```

# 三十六、Agent 的优势就在这里

Agent 可以：

```
理解目标
↓
查看当前信息
↓
决定缺少什么
↓
选择 Tool
↓
观察结果
↓
继续调整
```

这对：

```
开放性任务
```

特别有价值。

# 三十七、Workflow 擅长确定性问题

比如：

```
用户注册
订单支付
库存扣减
审批流程
数据同步
定时任务
ETL
```

通常：

```
路径可预测
规则明确
```

# 三十八、Agent 擅长不确定性问题

例如：

```
分析异常原因

调研一个主题

诊断复杂问题

根据现状选择不同信息源

制定多步解决方案

动态查资料

复杂客服
```

路径可能：

```
提前无法完全确定
```

# 三十九、可以建立一个重要判断标准

问：

> **任务路径能不能可靠地提前写出来？**

如果：

```
能
```

优先考虑：

```
Workflow
```

如果：

```
很难
```

才考虑：

```
Agent
```

# 四十、再问第二个问题

> **模型动态决策到底带来什么价值？**

如果没有明显价值：

```
就不要加 Agent
```

# 四十一、例如用户注册

你可以问：

```
模型动态决定下一步
能带来什么好处？
```

几乎没有。

反而增加：

```
成本
延迟
不确定性
风险
```

所以不用 Agent。

# 四十二、再看广告异常诊断

用户：

```
为什么这个 Campaign 昨天开始突然没消耗？
```

动态决策很有价值。

因为：

```
第一次查什么
```

取决于：

```
当前状态
```

后续查什么又取决于：

```
前一步结果
```

所以 Agent 很合适。

# 四十三、第三个问题：错误成本高不高？

如果错误成本极高：

```
支付
删数据
生产配置
资金
医疗
```

应该尽量：

```
减少自由 Agent
```

或者增加：

```
Workflow
Human Approval
Validation
```

# 四十四、Agent 自主性不是越高越好

这点非常重要。

很多 Agent 宣传喜欢说：

```
Fully Autonomous
```

但生产系统真正追求：

```
可靠性
安全性
可控性
```

而不是：

```
自主程度最大
```

# 四十五、一个更合理的原则

> **只把真正需要不确定性判断的部分交给 LLM。**

其他部分：

```
尽量确定性
```

# 四十六、比如广告创建系统

可以这样设计：

```
用户请求
↓
参数格式校验
↓
权限校验
↓
业务规则校验
↓
LLM 分析素材
↓
必要时 Agent 查政策 / 查历史
↓
人工确认
↓
程序调用 Meta API
↓
程序校验结果
↓
保存数据库
```

这里只有：

```
分析
调查
```

部分需要 Agent。

# 四十七、而不是这样

```
用户：
帮我创建广告。

↓
Agent：

我自己看着办。
```

然后 Agent 自己决定：

```
预算
地区
素材
平台
账户
是否创建
```

这在很多生产场景风险极高。

# 四十八、Workflow 可以给 Agent 设置边界

这是一个很重要的混合思想。

例如：

```
Workflow
↓
Step 1：校验权限
↓
Step 2：Agent 分析
↓
Step 3：人工审批
↓
Step 4：固定执行操作
```

Agent 只能出现在：

```
Step 2
```

这样：

```
既保留灵活性
又保留安全边界
```

# 四十九、Agent 也可以调用 Workflow

例如 Agent 判断：

```
需要创建 Campaign
```

但它不是自己一步步自由操作。

而是调用：

```
createCampaignWorkflow()
```

这个 Workflow 内部：

```
校验
↓
权限
↓
创建
↓
写库
↓
日志
```

全部确定性执行。

这也是非常好的设计。

# 五十、Tool 不一定是一个简单 API

我们在 Node 006 里经常把 Tool 想成：

```
getCampaign
```

但 Tool 也可以封装：

```
完整 Workflow
```

例如：

```
createCampaign
```

Tool 内部可能执行：

```
validate
↓
create campaign
↓
create ad group
↓
create creative
↓
save DB
```

模型不用知道所有内部细节。

# 五十一、这就是抽象边界

Agent 看到：

```
Tool:
createCampaign
```

程序内部：

```
100 行甚至 1000 行 Workflow
```

这样可以避免：

```
Agent 直接控制过多底层步骤
```

# 五十二、这很像微服务思想

用户不会控制：

```
订单服务内部每个 SQL
```

用户只是调用：

```
createOrder
```

同样：

Agent 不一定需要控制：

```
Campaign 创建每一个数据库操作
```

它只需要调用：

```
createCampaignWorkflow
```

# 五十三、所以 Tool 设计粒度非常重要

如果 Tool 太细：

```
insertRow
updateField
deleteRow
```

Agent 自由度太高。

风险：

```
大
```

如果 Tool 更高层：

```
pauseCampaign
createCampaign
approveCampaign
```

业务语义清晰。

程序内部还可以：

```
权限校验
事务
审计
```

通常更可靠。

# 五十四、Agent 适合决定“做什么”

程序更适合决定：

```
“具体怎么安全地做”
```

例如：

```
LLM：

这个 Campaign 需要暂停。
```

Program：

```
检查权限
↓
检查 Campaign 状态
↓
检查操作人
↓
执行 pause
↓
记录 audit log
```

# 五十五、这个职责划分非常重要

可以记：

```
Agent
偏 WHAT

Workflow
偏 HOW
```

并不是绝对。

但作为工程直觉非常有用。

# 五十六、Agent 负责策略，Workflow 负责执行

例如：

```
Agent：
我判断下一步需要查询 Account。

Workflow：
如何安全、稳定地查询 Account。
```

或者：

```
Agent：
我认为应该暂停 Campaign。

Workflow：
暂停 Campaign 需要哪些校验、审批和事务。
```

# 五十七、为什么生产系统通常是 Hybrid？

Hybrid：

```
混合
```

因为真实系统同时存在：

```
确定性部分
+
不确定性部分
```

# 五十八、例如 AI 客服

用户：

```
我的订单为什么没有到？
```

Agent：

```
理解问题
↓
决定查订单
```

Tool：

```
queryOrderWorkflow
```

返回：

```
已发货
物流异常
```

Agent：

```
分析结果
```

如果需要退款：

```
进入 Refund Workflow
```

退款流程：

```
资格校验
↓
金额校验
↓
审批
↓
退款
↓
记录
```

这些通常不应该让 Agent 自由发挥。

# 五十九、所以完整系统可能是

```
                   User
                    │
                    ▼
                 Agent
                    │
          ┌─────────┼─────────┐
          ▼         ▼         ▼
     Query Tool   RAG      Workflow Tool
                              │
                              ▼
                      Deterministic Logic
                              │
                              ▼
                         Real System
```

# 六十、Workflow 的执行路径一定完全固定吗？

也不是。

Workflow 可以有：

```
条件
分支
并行
循环
重试
```

例如：

```
          A
          │
          ▼
        判断
       /    \
      B      C
      │      │
      └──┬───┘
         ▼
         D
```

依然是 Workflow。

关键是：

```
规则是谁定义的？
```

# 六十一、程序定义分支 vs 模型定义分支

程序定义：

```
if (status == ACTIVE) {
    queryMetrics();
}
```

这是：

```
Workflow
```

模型定义：

```
LLM 看到当前信息
↓
自己判断是否查询 Metrics
```

这更：

```
Agentic
```

# 六十二、两者甚至可以一起存在

例如：

```
if (!user.hasPermission()) {
    return;
}

AgentDecision decision =
        agent.analyze(context);
```

这里：

```
权限检查
=
Workflow / Program
```

之后：

```
动态分析
=
Agent
```

# 六十三、为什么确定性校验要放 Agent 外面？

因为：

```
必须 100% 执行
```

例如权限。

不能依赖：

```
模型是否记得调用 permissionTool
```

# 六十四、特别重要的安全原则

错误设计：

```
LLM：
在修改数据前，请记得先检查权限。
```

正确得多：

```
Runtime：

任何 write tool 执行前
强制 permission check
```

区别非常大。

# 六十五、Prompt 是软约束

例如：

```
请不要删除生产数据。
```

模型通常会遵守。

但：

```
不是绝对保证
```

# 六十六、程序规则是硬约束

```
if (environment == PROD) {
    deleteTool.disable();
}
```

这是真正：

```
不能执行
```

所以高风险规则尽量做：

```
Hard Constraint
```

# 六十七、Agent 适合 Soft Decision

例如：

```
这个问题更像账户问题还是素材问题？
```

这是：

```
语义判断
```

适合 LLM。

# 六十八、Workflow 适合 Hard Rule

例如：

```
如果账户状态 = SUSPENDED
禁止创建广告
```

直接：

```
if (account.isSuspended()) {
    throw ...
}
```

# 六十九、一个判断方法

问：

> **这件事情如果模型偶尔判断错一次，能接受吗？**

如果：

```
完全不能
```

优先：

```
确定性程序
```

# 七十、再问

> **这个规则能不能明确写成代码？**

如果：

```
能
```

通常：

```
写代码
```

比让模型判断可靠。

# 七十一、例如年龄限制

规则：

```
age >= 18
```

不要：

```
LLM 判断这个用户是否成年
```

直接：

```
age >= 18
```

# 七十二、LLM 擅长什么？

比如：

```
这段用户投诉主要在表达什么问题？
```

难以用：

```
if else
```

完整覆盖。

适合：

```
LLM
```

# 七十三、所以系统设计可以这样想

```
能写死的
↓
尽量写死

不适合写死的
↓
考虑 LLM

需要动态多步行动的
↓
考虑 Agent
```

这是非常实用的一条原则。

# 七十四、不要“为了 Agent 而 Agent”

很多项目看到：

```
Agent 很火
```

于是：

```
任何功能
↓
都加 Agent
```

这是典型误区。

# 七十五、Agent 会带来真实成本

包括：

```
Token 成本
模型 API 成本
延迟
Tool 调用成本
复杂度
Debug 成本
Eval 成本
运维成本
安全风险
```

所以必须：

```
有价值
```

才值得用。

# 七十六、Agent 比 Workflow 更灵活

这是优势。

因为：

```
模型可以适应更多输入情况
```

不需要：

```
程序员预先穷举全部路径
```

# 七十七、但是灵活性和确定性通常存在取舍

可以画：

```
更确定
◄──────────────────────────►
更灵活

Workflow                 Agent
```

通常：

```
越靠 Workflow
越稳定、可预测
越靠 Agent
越灵活、动态
```

# 七十八、但这不是绝对的零和关系

好的工程设计可以：

```
把 Agent 限制在合适范围
```

从而同时获得：

```
一定灵活性
+
较高可靠性
```

这就是：

```
Hybrid Architecture
```

的价值。

# 七十九、Workflow 的典型优点

```
可预测
可测试
容易 Debug
成本稳定
延迟稳定
规则明确
权限容易控制
容易审计
```

# 八十、Workflow 的典型缺点

```
灵活性低
需要提前设计路径
复杂开放任务难以穷举
规则树可能越来越复杂
对自然语言理解能力有限
```

# 八十一、Agent 的典型优点

```
灵活
能处理开放任务
可以动态选择 Tool
可以根据 Observation 调整
可以理解自然语言目标
减少巨大 if / else 决策树
```

# 八十二、Agent 的典型缺点

```
不确定
更难测试
更难 Debug
成本更高
延迟更高
可能循环
可能选错 Tool
可能误解 Observation
安全风险更高
```

# 八十三、可以做一张对比表

| 维度         | Workflow   | Agent          |
| ------------ | ---------- | -------------- |
| 路径         | 程序预定义 | 模型可动态决定 |
| 确定性       | 高         | 较低           |
| 灵活性       | 较低       | 高             |
| 调试难度     | 较低       | 较高           |
| 测试难度     | 较低       | 较高           |
| 成本         | 更可预测   | 更不稳定       |
| 延迟         | 更可预测   | 更不稳定       |
| 风险         | 较低       | 较高           |
| 复杂开放任务 | 较弱       | 较强           |
| 规则型业务   | 很合适     | 通常没必要     |

# 八十四、什么时候应该优先 Workflow？

典型情况：

```
流程明确
规则稳定
路径可提前定义
错误成本高
需要严格审计
需要事务
需要高确定性
```

例如：

```
支付
审批
订单
库存
数据同步
账户注册
广告正式创建
```

# 八十五、什么时候适合 Agent？

典型情况：

```
任务开放
路径不确定
需要动态查资料
需要根据中间结果调整
用户输入高度自然语言化
可能调用不同 Tool
很难提前穷举全部情况
```

例如：

```
问题诊断
研究
智能客服分析
广告异常分析
代码调查
复杂数据分析
```

# 八十六、什么时候适合 Workflow + Agent？

这可能是生产环境最常见的。

比如：

```
用户提交
↓
程序校验
↓
Agent 分析
↓
程序审批
↓
Agent 补充调查
↓
Workflow 执行
↓
结果校验
```

既有：

```
硬规则
```

又有：

```
柔性判断
```

# 八十七、Agentic Workflow 是什么？

现在正式给一个当前阶段够用的定义：

> **Agentic Workflow 是一个整体流程仍然存在明确结构，但其中部分步骤由 LLM 动态判断或执行的 Workflow。**

# 八十八、例如

```
Step 1
固定：
读取用户请求

Step 2
LLM：
任务分类

Step 3
程序：
根据类别进入对应分支

Step 4
Agent：
在分支内动态调用 Tool

Step 5
固定：
生成审计记录

Step 6
固定：
返回结果
```

它既不是：

```
完全 Workflow
```

也不是：

```
完全自由 Agent
```

而是：

```
Agentic Workflow
```

# 八十九、Agentic Workflow 为什么很重要？

因为它非常符合：

```
真实企业系统
```

企业往往希望：

```
该确定的确定
该灵活的灵活
```

而不是：

```
全部自动发挥
```

# 九十、可以把自由度设计成“局部开放”

比如：

```
整个 Workflow 有 10 步
```

只有第：

```
4
5
6
```

步允许 Agent 动态执行。

其他：

```
1
2
3
7
8
9
10
```

全部固定。

这样系统更容易控制。

# 九十一、Agent 也可以作为 Workflow 中的普通 Node

比如：

```
Workflow

Start
↓
Load Data
↓
Agent Analysis
↓
Human Review
↓
Save Result
↓
End
```

这里 Agent 就只是：

```
一个节点
```

而不是整个系统。

# 九十二、反过来，Workflow 也可以成为 Agent 的 Tool

Agent：

```
我要创建一个 Campaign。
```

调用：

```
createCampaignWorkflow
```

Workflow：

```
Validate
↓
Permission
↓
Create
↓
Persist
↓
Audit
```

完成后返回：

```
CampaignCreated
```

这也是非常强大的组合。

# 九十三、所以 Workflow 和 Agent 可以互相嵌套

```
Workflow
    │
    ▼
   Agent
    │
    ▼
 Workflow Tool
    │
    ▼
   Agent
```

真实系统完全可能这么复杂。

# 九十四、这再次说明 Agent 概念不是互斥标签

我们刚才学 RAG / Memory 时已经悟到：

```
Tool
RAG
Memory
Context
```

可以同时成立。

这里也一样：

```
Workflow
Agent
Tool
```

不是永远互斥。

# 九十五、一个 Agent 可以运行在 Workflow 中

同时：

```
Agent 内部又调用 Workflow Tool
```

所以不要：

```
看到 Agent
就认为没有 Workflow
```

# 九十六、接下来讲 Orchestrator

以后你会经常看到：

```
Orchestrator
```

可以理解为：

```
协调多个组件 / 子任务
```

的控制层。

# 九十七、Workflow Orchestrator

比如：

```
A
↓
B
↓
C
```

由：

```
Workflow Engine
```

协调。

# 九十八、Agent Orchestrator

可能由 LLM：

```
动态决定
```

哪个子 Agent / Tool：

```
下一步执行
```

# 九十九、例如 Multi-Agent

以后可能出现：

```
Coordinator Agent
│
├── Research Agent
├── Code Agent
├── Data Agent
└── Review Agent
```

Coordinator：

```
决定把任务给谁
```

这属于：

```
Agentic Orchestration
```

当前知道概念即可。

# 一百、不要现在就追 Multi-Agent

Multi-Agent 很容易让初学者觉得：

```
更高级
```

但：

```
一个 Agent 能解决
```

就没必要：

```
5 个 Agent
```

因为每多一个：

```
Context
通信
状态
成本
错误
```

都会增加。

# 一百零一、Workflow 的 State 和 Agent 的 State

Workflow 也有：

```
State
```

比如：

```
{
  "orderId": 123,
  "step": "PAYMENT",
  "status": "WAITING"
}
```

所以：

```
State
```

不是 Agent 独占概念。

# 一百零二、这一点也很重要

很多 Agent 概念其实来自：

```
传统软件工程
```

比如：

```
State
Workflow
Retry
Timeout
Queue
Event
```

不是 AI 发明的。

Agent 只是：

```
把 LLM 动态决策
```

加入了这些传统系统。

# 一百零三、这也是 Java 后端的优势

你已经知道：

```
状态
事务
异常
流程
数据库
权限
接口
```

以后学 Agent 时：

```
这些都不会消失
```

反而更加重要。

# 一百零四、一个 Java Workflow Engine 的思想

以后可能有：

```
interface WorkflowStep {

    StepResult execute(WorkflowContext context);

}
```

然后：

```
Step A
↓
Step B
↓
Step C
```

# 一百零五、Agent 则可能出现

```
while (!finished) {

    Decision decision =
            llm.decide(context);

    ActionResult result =
            actionExecutor.execute(decision);

    context.update(result);
}
```

两种控制模式明显不同。

# 一百零六、Workflow 是 Program-Driven

可以记：

```
Program-Driven
```

程序决定：

```
下一步
```

# 一百零七、Agent 是 Model-Driven

至少部分步骤：

```
Model-Driven
```

模型决定：

```
下一步
```

# 一百零八、Hybrid 是 Program + Model

```
Program
控制边界

LLM
控制局部动态决策
```

这是非常重要的生产设计思想。

# 一百零九、谁应该掌握最终控制权？

答案通常应该是：

```
Application / Runtime
```

而不是：

```
LLM
```

LLM 可以：

```
提出决策
```

但 Runtime：

```
决定是否允许执行
```

# 一百一十、这和 Node 006 完全一致

模型：

```
我要调用 pauseCampaign。
```

Runtime：

```
检查：

Permission
Approval
State
Policy
```

然后：

```
允许 / 拒绝
```

# 一百一十一、所以 Workflow 可以成为 Agent 的安全护栏

例如：

```
Agent Proposal
↓
Validation Workflow
↓
Approval Workflow
↓
Execution Workflow
```

模型负责：

```
提出
```

系统负责：

```
验证和执行
```

# 一百一十二、什么时候应该 Human-in-the-loop？

比如：

```
高风险修改
```

例如：

```
删除
退款
转账
发布
生产环境修改
批量操作
```

Agent 可以：

```
建议
```

但：

```
Human Approve
```

之后才能执行。

# 一百一十三、Human Approval 本质也是 Workflow Node

比如：

```
Agent
↓
Propose Action
↓
Waiting Approval
↓
Human Approve
↓
Execute
```

这就是：

```
Workflow + Agent + Human
```

# 一百一十四、真实 Agent 系统越来越像传统分布式系统

以后你会发现：

```
LLM
```

只占整个系统的一部分。

旁边还有：

```
Database
Redis
MQ
Workflow
API
Permission
Observability
Eval
```

这就是为什么：

```
后端工程能力
```

非常重要。

# 一百一十五、Workflow 也可以并行

例如：

```
              ┌→ 查 Account
              │
Start ────────┼→ 查 Metrics
              │
              └→ 查 Creative
                    │
                    ▼
                   Merge
```

如果这些查询互不依赖：

```
并行
```

比 Agent 一个一个查：

```
更快
```

# 一百一十六、这是一个很重要的优化思想

如果程序已经知道：

```
三个数据肯定都需要
```

那就：

```
并行调用
```

没必要：

```
LLM：
先查 A

等待

LLM：
再查 B

等待

LLM：
再查 C
```

这种 Agent Loop 反而更慢。

# 一百一十七、所以 Agent 不一定更高效

Agent 的优势：

```
动态
```

但动态本身：

```
有成本
```

# 一百一十八、什么时候应该固定并行？

如果：

```
A、B、C 永远都要查
```

直接 Workflow：

```
parallel(A, B, C)
```

更合理。

# 一百一十九、什么时候 Agent 动态选择更合理？

如果：

```
A、B、C 只有部分场景需要
```

并且：

```
提前很难判断
```

模型动态选：

```
可能更节省
```

# 一百二十、这其实是一个 Cost Trade-off

Agent：

```
多一次模型判断
```

但可能减少：

```
无用 Tool 调用
```

Workflow：

```
不需要模型判断
```

但可能执行：

```
一些无用步骤
```

真正项目需要：

```
实验和测量
```

# 一百二十一、不要凭感觉决定

以后做生产 Agent：

```
Eval
Metrics
Tracing
```

会告诉你：

```
哪个设计更好
```

而不是：

```
Agent 听起来更先进
```

# 一百二十二、Workflow 适合“Known Path”

Known Path：

```
已知路径
```

例如：

```
订单创建
```

# 一百二十三、Agent 适合“Unknown Path”

Unknown Path：

```
未知路径
```

例如：

```
帮我调查为什么这个服务最近响应变慢。
```

可能需要：

```
日志
Metrics
数据库
Deploy
网络
```

到底先查什么：

```
动态决定
```

# 一百二十四、一个很好的判断公式

```
路径确定性越高
↓
越偏 Workflow

路径不确定性越高
↓
越适合 Agent
```

# 一百二十五、再加一个维度：风险

```
风险越高
↓
越偏 Workflow / Human Approval
```

# 一百二十六、再加一个维度：开放性

```
任务越开放
↓
越适合 Agent
```

# 一百二十七、所以可以画成二维理解

```
高风险
  ▲
  │      Workflow
  │
  │
  │
  │
  │
  └──────────────────► 开放性
                  Agent
```

当然不是严格数学图。

只是帮助理解。

# 一百二十八、生产系统一个非常实用的设计原则

> **用 Workflow 包住 Agent。**

意思是：

```
外层
=
确定性流程

内层
=
动态智能
```

# 一百二十九、例如

```
Request
↓
Auth
↓
Validation
↓
Agent
↓
Validation
↓
Approval
↓
Execution
↓
Audit
```

Agent 被夹在：

```
硬规则
```

中间。

这种设计通常比：

```
完全放飞 Agent
```

可靠得多。

# 一百三十、另一个原则：让 Agent 调高层 Tool

不要：

```
Agent
↓
直接操作数据库字段
```

而是：

```
Agent
↓
Business Tool
↓
Service
↓
Domain Logic
↓
Database
```

# 一百三十一、例如错误设计

Tool：

```
updateDatabaseField(
  table,
  column,
  value
)
```

Agent 几乎可以：

```
随便改数据库
```

风险巨大。

# 一百三十二、更好的 Tool

```
pauseCampaign(campaignId)
```

内部：

```
permission
validation
transaction
audit
```

Agent 只能：

```
执行允许的业务动作
```

# 一百三十三、Workflow 还能做幂等控制

例如：

```
pauseCampaign
```

被 Agent 连续调用两次。

Workflow / Service 可以保证：

```
第二次调用
不会产生错误副作用
```

这就是：

```
Idempotency
```

# 一百三十四、Agent 非常需要幂等

因为模型可能：

```
重复 Tool Call
```

所以 write Tool 最好：

```
尽量具备幂等能力
```

或者使用：

```
idempotency key
```

以后生产级阶段再详细学。

# 一百三十五、Agent 也需要超时

例如：

```
maxSteps = 10
```

之外，还需要：

```
taskTimeout = 60s
```

否则：

```
Tool 慢
LLM 慢
Retry
```

可能导致任务一直运行。

# 一百三十六、这些都说明

Agent 不是：

```
LLM + 一个 Prompt
```

而是：

```
复杂的软件系统
```

# 一百三十七、再说一个常见词：Deterministic

Deterministic：

```
确定性
```

传统 Workflow 往往：

```
更 Deterministic
```

# 一百三十八、Agent 更偏 Non-deterministic

Non-deterministic：

```
非确定性
```

模型可能：

```
不同调用产生不同选择
```

即使：

```
输入很接近
```

# 一百三十九、但不要理解成“Agent 完全随机”

不是。

Agent 仍然受到：

```
Prompt
Tools
Context
Model
Temperature
Runtime Rules
```

影响。

只是：

```
不是传统程序那种严格确定路径
```

# 一百四十、一个重要词：Guardrail

Guardrail：

```
护栏
```

意思是：

```
限制 Agent 行为
```

例如：

```
Tool Permission
Output Validation
Policy Check
Human Approval
Max Steps
Budget Limit
```

# 一百四十一、Workflow 本身就是一种 Guardrail

如果你规定：

```
Agent 必须经过 Approval
```

那 Workflow 就把：

```
Agent 自由度
```

限制在安全范围。

# 一百四十二、Workflow 和 Agent 的关系可以这样理解

```
Workflow
=
轨道
```

Agent：

```
=
司机
```

在 Workflow 中：

```
轨道已经铺好
```

Agent 只能在：

```
允许范围内做决定
```

# 一百四十三、完全 Agentic 更像越野

没有固定轨道。

Agent：

```
自己选路线
```

优点：

```
灵活
```

缺点：

```
更容易走错
```

# 一百四十四、企业通常更喜欢“有轨道的智能”

这可以理解成：

```
Bounded Autonomy
```

也就是：

```
有限自主
```

Node 006 已经提过。

# 一百四十五、Workflow + Agent 就是 Bounded Autonomy 的重要实现

系统规定：

```
什么可以自由决定
```

和：

```
什么不能自由决定
```

# 一百四十六、一个 Ads Agent 例子

用户：

```
帮我优化 Campaign 123。
```

Agent 可以自由：

```
分析
查数据
查规则
提出建议
```

但不能自由：

```
修改预算
暂停 Campaign
创建 Campaign
```

只有用户确认：

```
执行建议
```

才进入：

```
Workflow
```

执行修改。

# 一百四十七、这就是非常典型的 Read vs Write 分离

Read Agent：

```
自由度更高
```

因为：

```
查数据风险较低
```

Write Agent：

```
自由度更低
```

因为：

```
会改变真实世界
```

# 一百四十八、一个现实的权限模型

```
Read Tools
↓
自动执行

Write Tools
↓
需要 Validation

High-risk Tools
↓
需要 Human Approval
```

# 一百四十九、Workflow 非常适合把这些规则固定下来

例如：

```
if tool.type == READ
    execute

if tool.type == WRITE
    validate

if tool.risk == HIGH
    waitHumanApproval
```

而不是：

```
Prompt 里提醒模型自己注意
```

# 一百五十、Agent 可以动态选择 Workflow

例如用户：

```
帮我处理这个客户投诉。
```

Agent 判断：

```
这是退款问题
```

于是启动：

```
refundWorkflow
```

另一个投诉：

```
物流问题
```

启动：

```
logisticsWorkflow
```

这时候：

```
Agent
负责路由

Workflow
负责执行
```

非常合理。

# 一百五十一、这种模式非常重要

可以叫：

```
LLM Router
```

或者：

```
Agent Router
```

模型负责：

```
选择哪条确定性流程
```

而不是：

```
自己完成全部细节
```

# 一百五十二、这可能是很多企业 Agent 最实用的形态

因为企业已经有：

```
大量业务 API
大量 Workflow
```

Agent 不需要：

```
重写它们
```

只需要：

```
理解用户自然语言
↓
选择正确业务能力
```

# 一百五十三、Java 后端视角非常容易理解

原来：

```
Controller
↓
Service
↓
Business Logic
```

现在可能变成：

```
Natural Language
↓
Agent
↓
Service / Workflow
↓
Business Logic
```

# 一百五十四、Agent 不是替换 Service

它更像：

```
新的智能入口和决策层
```

# 一百五十五、业务逻辑仍然应该在业务层

例如：

```
CampaignService
```

仍然负责：

```
真正业务规则
```

而不是把：

```
全部业务规则写到 Prompt
```

# 一百五十六、Prompt 不应该成为新的 Business Service

这是很重要的工程意识。

错误：

```
System Prompt：

如果预算超过……
如果状态是……
如果账户是……
如果……
```

Prompt 最后变成：

```
10000 行业务规则
```

非常难维护。

# 一百五十七、业务规则应该尽量留在代码

模型：

```
调用 Tool
```

Tool 内部：

```
Business Service
```

负责规则。

# 一百五十八、为什么？

因为代码可以：

```
测试
版本控制
类型检查
事务
IDE 重构
静态分析
```

Prompt：

```
没有这些强保证
```

# 一百五十九、Workflow 也更适合审计

例如：

```
审批流
```

可以记录：

```
谁提交
谁审批
何时执行
执行结果
```

Agent 也必须支持：

```
Trace
```

但更复杂。

# 一百六十、什么是 Trace？

以后会正式学习。

现在只需要理解：

```
Agent 每一步做了什么
```

都应该记录。

例如：

```
Step 1
LLM chose getCampaign

Step 2
Tool returned ACTIVE

Step 3
LLM chose getMetrics
```

这叫：

```
Execution Trace
```

# 一百六十一、Workflow Trace 比较简单

```
A
↓
B
↓
C
```

Agent Trace：

```
动态
```

所以生产系统更依赖：

```
Tracing
Observability
```

# 一百六十二、为什么 Agent Eval 很重要？

Workflow 测试可以问：

```
是不是调用 B？
```

Agent 测试可能要问：

```
最终任务是否完成？

Tool 选择是否合理？

有没有无用步骤？

有没有危险操作？

成本是否过高？
```

评价方式更复杂。

# 一百六十三、现在再看一个完整实例

用户：

```
分析 Campaign 123 为什么昨天转化下降，
并给出建议。
```

方案 A：纯 Workflow

程序固定：

```
查 Campaign
↓
查 Metrics
↓
查 Creative
↓
查 Account
↓
查 Budget
↓
全部给 LLM
↓
生成答案
```

优点：

```
简单
稳定
```

缺点：

```
可能查很多无用数据
```

# 一百六十四、方案 B：纯 Agent

```
LLM
↓
自己决定查什么
↓
不断 Tool Call
↓
最终回答
```

优点：

```
灵活
```

缺点：

```
路径不可预测
成本更高
可能漏查
```

# 一百六十五、方案 C：Hybrid

程序先固定查：

```
Campaign 基础信息
Metrics 基础趋势
```

因为：

```
无论如何都需要
```

然后：

```
LLM 分析
```

如果发现：

```
素材异常迹象
```

再由 Agent：

```
查 Creative
```

如果发现：

```
账户异常迹象
```

再：

```
查 Account
```

# 一百六十六、Hybrid 为什么可能最好？

因为：

```
共同必需部分
↓
Workflow

动态调查部分
↓
Agent
```

同时兼顾：

```
效率
稳定
灵活
```

# 一百六十七、这就是“固定骨架 + 动态节点”

非常值得记。

```
固定骨架
+
动态节点
```

是很多生产 Agent 的好架构。

# 一百六十八、什么时候应该让模型做 Router？

比如用户请求可能属于：

```
Campaign
Creative
Account
Billing
```

自然语言非常复杂。

LLM：

```
判断类别
```

很适合。

# 一百六十九、但是 Router 后面可以接固定 Workflow

```
LLM Router
│
├── Campaign Workflow
├── Creative Workflow
├── Account Workflow
└── Billing Workflow
```

这是非常典型的：

```
LLM + Workflow
```

# 一百七十、什么时候不用 Agent Loop？

如果模型只需要：

```
一次分类
```

那么：

```
一次 LLM Call
```

就够。

不要为了“Agent”硬写：

```
while loop
```

# 一百七十一、所以 Agent Loop 也不是越长越好

每增加一步：

```
成本
延迟
错误概率
```

都会增加。

# 一百七十二、如果两步能解决

不要：

```
跑十步
```

# 一百七十三、Agent 设计其实也是“控制自由度”

你在决定：

```
哪里固定
哪里开放
```

这可能是 Agent 系统设计最重要的问题之一。

# 一百七十四、自由度可以存在多个维度

例如：

```
Tool 选择自由度
参数自由度
执行顺序自由度
终止自由度
写操作自由度
```

不是只有：

```
Agent / 非 Agent
```

# 一百七十五、例如可以允许模型自由选 Tool

但：

```
Tool 参数
```

必须满足 Schema。

# 一百七十六、可以允许模型自由决定查询顺序

但：

```
最大查询次数 = 5
```

# 一百七十七、可以允许模型提出修改建议

但：

```
不允许直接修改
```

# 一百七十八、这就是 Guardrails

Agent：

```
自由
```

Runtime：

```
边界
```

# 一百七十九、可以记一个非常重要的公式

```
Production Agent
=
Autonomy
+
Constraints
```

只有：

```
Autonomy
```

没有：

```
Constraints
```

往往很危险。

# 一百八十、为什么很多 Demo Agent 看起来很强？

Demo：

```
失败了重新跑就行
```

没有：

```
真实钱
真实用户
真实数据库
```

生产：

```
一次错操作
```

可能造成：

```
真实事故
```

所以生产系统会：

```
更 Workflow
更 Guarded
```

# 一百八十一、Agent 工程不是追求最酷架构

而是追求：

```
任务成功率
成本
延迟
安全
稳定
维护性
```

# 一百八十二、什么时候不用 AI 最好？

这是一个非常成熟的问题。

比如：

```
1 + 1
```

不需要 LLM。

或者：

```
CampaignStatus == ACTIVE
```

不要让模型判断。

# 一百八十三、可以用 SQL 的不要强行 LLM

比如：

```
过去 7 天 spend 总和
```

SQL：

```
SUM(spend)
```

最可靠。

# 一百八十四、可以用规则的不要强行 Agent

例如：

```
余额 < 0
```

直接代码。

# 一百八十五、需要语义判断再用 LLM

例如：

```
这些用户反馈主要在抱怨什么？
```

这才是 LLM 强项。

# 一百八十六、需要动态多步语义决策再用 Agent

例如：

```
帮我调查为什么系统最近变慢。
```

# 一百八十七、可以形成一个四层选择

```
能用普通代码？
↓
普通代码

需要固定多步？
↓
Workflow

需要 LLM 语义能力但路径固定？
↓
LLM Workflow

需要动态多步决策？
↓
Agent
```

这张非常值得记。

# 一百八十八、再画一次

```
问题
 │
 ▼
能确定性解决？
 │
 ├─ 是 → 普通代码
 │
 └─ 否
      │
      ▼
路径能提前确定？
      │
      ├─ 是 → Workflow / LLM Workflow
      │
      └─ 否
           │
           ▼
需要动态 Tool / 多步决策？
           │
           ├─ 是 → Agent
           │
           └─ 否 → 单次 LLM
```

# 一百八十九、这不是绝对规则

但作为：

```
第一版系统设计判断框架
```

非常好用。

# 一百九十、Node 006 和 007 串起来

Node 006：

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

Node 007：

```
但是
不是所有任务都应该进入 Loop
```

只有：

```
路径需要动态决定
```

时才值得。

# 一百九十一、Node 005 也串起来

Agent Loop 每一步：

```
都会增加 Context
```

Workflow 如果步骤固定：

```
可以更精确控制 Context
```

# 一百九十二、Node 004 也串起来

Agent 多一步：

```
就可能多一轮 Token
```

所以 Agent：

```
成本通常更高
```

# 一百九十三、Node 003 也串起来

Agent 幻觉比普通 Chat 更危险。

因为：

```
错误判断
↓
可能变成真实 Action
```

所以需要：

```
Workflow
Validation
Guardrail
Human Approval
```

# 一百九十四、现在整个 Phase 0 已经开始闭环

```
LLM
↓
Inference
↓
Hallucination
↓
Token
↓
Context
↓
Agent
↓
Workflow vs Agent
```

你现在已经不只是知道：

```
Agent 是什么
```

还知道：

```
什么时候不应该用 Agent
```

这其实更加重要。

# 一百九十五、最常见误区 1

```
用了 LLM
=
Agent
```

错误。

# 一百九十六、误区 2

```
有 Tool
=
一定是 Agent
```

也不一定。

如果 Tool 调用路径固定：

```
可能只是 Workflow
```

# 一百九十七、误区 3

```
Agent 比 Workflow 高级
```

错误。

它们解决：

```
不同问题
```

# 一百九十八、误区 4

```
Agent 自由度越大越强
```

错误。

生产系统通常需要：

```
Bounded Autonomy
```

# 一百九十九、误区 5

```
Workflow 不能使用 LLM
```

错误。

LLM 完全可以是：

```
Workflow Node
```

# 二百、误区 6

```
用了 Agent 就不需要传统业务代码
```

完全错误。

实际上：

```
Agent 越复杂
```

通常越需要：

```
成熟后端工程
```

# 二百零一、误区 7

```
Agent 可以直接管理数据库
```

通常不应该。

最好：

```
Agent
↓
Business Tool
↓
Service
↓
DB
```

# 二百零二、误区 8

```
所有判断都交给 LLM
```

错误。

确定性判断：

```
应该尽量代码化
```

# 二百零三、误区 9

```
Workflow 和 Agent 互斥
```

错误。

可以：

```
Workflow 包 Agent
Agent 调 Workflow
```

# 二百零四、误区 10

```
Agentic Workflow 就是纯 Agent
```

不一定。

它往往是：

```
结构化 Workflow
+
模型动态决策
```

# 二百零五、这一篇最核心的 15 句话

### 1

> **Workflow 的执行路径主要由程序定义。**

### 2

> **Agent 可以让模型动态决定部分执行路径。**

### 3

> **使用了 LLM，不代表系统就是 Agent。**

### 4

> **Workflow 完全可以包含 LLM。**

### 5

> **Agent 和 Workflow 不是简单的二选一。**

### 6

> **真实生产系统经常使用 Workflow + Agent 的混合结构。**

### 7

> **能确定性写成代码的逻辑，通常优先使用确定性代码。**

### 8

> **必须满足的业务规则不应该只写在 Prompt 中。**

### 9

> **Agent 更适合路径无法提前完全确定的开放任务。**

### 10

> **Workflow 更适合规则明确、错误成本高、路径稳定的任务。**

### 11

> **Agent 自主性不是越高越好。**

### 12

> **Production Agent 更重要的是 Bounded Autonomy。**

### 13

> **Agent 可以决定做什么，Workflow 可以负责安全地怎么做。**

### 14

> **Workflow 可以包住 Agent，Agent 也可以调用 Workflow。**

### 15

> **Agent 工程真正重要的问题不是“能不能 Agent”，而是“哪里值得 Agent”。**

# 二百零六、一个最终判断框架

以后面对一个 AI 功能，先问：

```
1. 能不能直接用普通程序完成？

2. 流程是否可以提前确定？

3. 是否真的需要 LLM 理解自然语言？

4. 是否真的需要模型动态选择下一步？

5. 错误成本有多高？

6. 是否涉及真实写操作？

7. 哪些步骤必须 100% 确定？

8. 哪些步骤允许模型有自由度？
```

# 二百零七、实际场景判断

## 场景 1

```
用户注册
```

建议：

```
Workflow
```

## 场景 2

```
文本情感分类
```

建议：

```
单次 LLM / LLM Workflow
```

通常不需要 Agent。

## 场景 3

```
订单退款
```

核心执行：

```
Workflow
```

可以让 LLM：

```
理解退款原因
```

但不应该让 Agent：

```
自由决定退款流程
```

## 场景 4

```
广告异常诊断
```

比较适合：

```
Agent
```

因为调查路径动态。

## 场景 5

```
创建 Campaign
```

核心创建：

```
Workflow
```

前置：

```
Agent 可以帮助分析参数 / 素材 / 规则
```

## 场景 6

```
技术调研
```

非常适合：

```
Agent
```

可以：

```
搜索
阅读
比较
继续搜索
总结
```

# 二百零八、自测

## Q1

Workflow 和 Agent 最核心的区别是什么？

## Q2

一个流程中使用了 LLM，是否就一定是 Agent？

为什么？

## Q3

有 if / else 的 Workflow 是否仍然是 Workflow？

为什么？

## Q4

什么叫：

```
Program-Driven
```

？

## Q5

什么叫：

```
Model-Driven
```

？

# 二百零九、理解题

## Q6

为什么用户注册一般不适合 Agent？

## Q7

为什么广告异常诊断更适合 Agent？

## Q8

为什么：

```
必须执行的权限校验
```

不应该只写进 Prompt？

## Q9

为什么 Agent 比 Workflow 更难测试？

## Q10

为什么 Agent 成本和延迟更难预测？

# 二百一十、混合架构题

用户：

```
帮我创建一个广告，
但创建之前先分析素材是否有风险。
```

一个合理系统：

```
参数校验
↓
权限校验
↓
Agent 分析素材风险
↓
用户 / 人工确认
↓
Create Ad Workflow
↓
数据库记录
```

请解释：

```
哪些是 Workflow？

哪些是 Agent？

为什么这么分？
```

# 二百一十一、进阶题

假设 Agent 拥有：

```
pauseCampaign
updateBudget
deleteCampaign
```

模型可以自由调用。

你应该考虑：

```
哪些 Tool 可以自动执行？

哪些要 Validation？

哪些要 Human Approval？

哪些根本不应该暴露给 Agent？
```

如果你已经开始主动考虑：

```
风险边界
```

说明开始具备：

```
Production Agent
```

思维。

# 二百一十二、Java 工程思维自测

如果以后设计：

```
Ads Agent
```

你应该更倾向：

```
Agent
↓
CampaignService.pauseCampaign()
```

而不是：

```
Agent
↓
直接 UPDATE campaign SET status = ...
```

为什么？

答案应该涉及：

```
业务规则
权限
事务
审计
可测试性
幂等
```

# 二百一十三、阶段测试

现在你应该可以解释：

> **为什么不是所有 AI 应用都应该做成 Agent？**

一个比较完整的回答应该接近：

> Agent 适合任务路径不固定、需要模型根据中间结果动态选择下一步的场景。但 Agent 会带来更高的不确定性、成本、延迟、测试和安全复杂度。对于流程明确、规则稳定、错误成本高的业务，更适合使用确定性 Workflow。生产系统往往不会在 Workflow 和 Agent 之间二选一，而是把需要灵活判断的部分交给 Agent，把权限、审批、事务和固定业务逻辑留在 Workflow 和传统代码中。

如果你已经能够不用看笔记说出类似这段话：

```
Node 007
基本就掌握了。
```

# 二百一十四、Phase 0 完成标准

到这里：

```
Phase 0：
LLM 与 Agent 基础认知
```

已经学习：

```
✅ Node 001：LLM 到底是什么
✅ Node 002：Training / Inference
✅ Node 003：Hallucination
✅ Node 004：Token
✅ Node 005：Context Window
✅ Node 006：什么是 Agent
✅ Node 007：Workflow 与 Agent
```

你应该已经具备第一层完整认知：

```
LLM 是什么
↓
模型怎么运行
↓
为什么会幻觉
↓
Token 是什么
↓
Context 为什么有限
↓
Agent 如何通过 Tool + State + Loop 行动
↓
为什么不是所有任务都应该 Agent 化
```

# 二百一十五、Phase 0 最终脑图

```
                         AI Application
                               │
                               ▼
                              LLM
                               │
              ┌────────────────┼────────────────┐
              ▼                ▼                ▼
            Token           Context        Hallucination
                               │
                               ▼
                             Agent
                               │
                ┌──────────────┼──────────────┐
                ▼              ▼              ▼
              Tools           State          Loop
                │                              │
                └──────────────┬───────────────┘
                               ▼
                        Dynamic Decisions
                               │
                  ┌────────────┴────────────┐
                  ▼                         ▼
               Workflow                  Agent
            Program-Driven            Model-Driven
                  │                         │
                  └────────────┬────────────┘
                               ▼
                          Hybrid System
```

# 二百一十六、接下来终于要进入真正代码阶段

下一 Node：

> **008 - 第一次调用模型 API**

从这里开始，我们会正式：

```
写代码
```

而不再只停留在认知。

第一步先理解：

```
你的程序
↓
HTTP Request
↓
Model Provider
↓
Model
↓
HTTP Response
```

# 二百一十七、Node 008 会做什么？

首先不会立刻：

```
Spring AI
LangChain4j
Agent Framework
```

而是从最底层开始：

```
Apifox / cURL
↓
HTTP
↓
OpenAI-compatible Chat Completions
↓
Qwen3.8-Max
```

真正看懂：

```
URL
Header
API Key
Request Body
Messages
Model
Response
Usage
```

# 二百一十八、然后才进入 Java

流程：

```
第一次 HTTP 调模型
↓
Java 调模型
↓
Spring Boot
↓
agent-learning-lab
```

# 二百一十九、Node 004 留下的实验也要回来

我们之前暂存：

```
Token Usage
```

Node 008 开始以后：

```
可以真实观察
```

# 二百二十、Node 005 的实验也会逐渐回来

以后可以测试：

```
不同 Context 长度
不同噪声
不同 Token Usage
```

# 二百二十一、也就是说

Phase 0：

```
建立脑子里的地图
```

Phase 1：

```
开始真正造东西
```

这就是我们的阶段变化。

# 二百二十二、最终总结

如果这一篇只记一件事：

请记：

> **不要问“我要不要用 Agent”，而要问“这个任务的哪些部分真的需要模型动态决定”。**

然后：

```
确定性部分
↓
代码 / Workflow

不确定性部分
↓
LLM / Agent
```

最终：

```
Workflow
+
Agent
+
传统程序
```

组成一个真正可靠的 AI 系统。

# 二百二十三、Node 007 完成标准

如果你现在能够不用翻笔记回答：

```
Workflow 是什么？

Agent 和 Workflow 最大区别是什么？

用了 LLM 是否就是 Agent？

Workflow 是否可以使用 LLM？

为什么有分支不代表 Agent？

什么时候应该用 Workflow？

什么时候应该用 Agent？

为什么 Agent 自主性不是越高越好？

什么是 Bounded Autonomy？

什么是 Agentic Workflow？

为什么生产系统经常 Workflow + Agent？

为什么权限、审批、事务应该留给程序？

为什么 Agent 可以决定 WHAT，而 Workflow 更适合 HOW？

为什么 Agent 可以调用 Workflow？

为什么 Workflow 也可以包含 Agent？

为什么不是所有 AI 应用都应该做成 Agent？
```

并且真正理解：

```
能确定性解决的
不要强行 Agent 化

需要语义但路径固定的
用 LLM Workflow

真正需要动态多步决策的
再用 Agent
```

那么：

```
Node 007：Workflow 与 Agent

✅ PASS
```

同时：

```
Phase 0：LLM 与 Agent 基础认知

✅ COMPLETE
```

下一步正式进入：

> **Phase 1 / Node 008：第一次调用模型 API。**