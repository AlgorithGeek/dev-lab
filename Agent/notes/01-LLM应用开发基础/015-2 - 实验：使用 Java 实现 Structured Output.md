# 015-2 - 实验：使用 Java 实现 Structured Output

> Node 015：Java DTO 与 Structured Output\
> Phase 1：LLM 应用开发基础\
> 实验项目：`projects/agent-learning-lab`

---

> **当前记录（2026-11-03）：**真实正常链路已跑通并记录；按用户授权新增并实际执行 --errors 的两个本地样本（缺少 suggestion、riskLevel=CRITICAL），两者都成功反序列化，再被与真实流程共用的字段检查拒绝。未读取密钥或发送 HTTP，未执行其他坏数据或完整 HTTPS 检查脚本；用户确认 Node 015 收尾，下一步 Node 016「Streaming（准备学习）」；当前不是完整 Schema/业务事实校验。

# 一、这次实验到底要做什么？

015-1 已经把理论链路讲清楚：

```text
JSON Schema
↓
Model Output
↓
Structured JSON
↓
Deserialization
↓
Java DTO
↓
Business Logic
```

015-2 不再停留在概念。

这次要真正做到：

```text
Java
↓
发送 Prompt + JSON Schema
↓
qwen3.8-max
↓
模型严格按照 Schema 返回 JSON
↓
Java 获取 message.content
↓
FastJSON2 反序列化
↓
CampaignAnalysis DTO
↓
Java 直接读取字段
```

完成后，我们的学习项目会从：

```text
“Java 调模型，拿 String”
```

第一次真正走到：

```text
“Java 调模型，拿业务对象”
```

---

# 二、为什么这个实验有必要？

我们的学习原则一直是：

> **必要时才做实验。**

不是每个知识点都机械地：

```text
理论
+
实验
```

但 Node 015 的目标本身就是：

```text
Structured Output
真正进入 Java 类型系统
```

如果不写代码、不真实调用、不反序列化：

```text
Schema
JSON
DTO
```

就仍然只是纸面概念。

所以本 Node 的实验是：

```text
必要实验
```

---

# 三、这次实验不要做什么？

为了看清主链，这次先不要引入：

```text
Spring AI
LangChain4j
Lombok
复杂 Validation 框架
Schema 自动生成
Provider 抽象
ModelClient 接口
Factory
Strategy
复杂嵌套 DTO
数据库
Spring Boot Controller
```

这些以后都可能有价值。

但现在第一目标只有：

> **把 Structured Output → Java DTO 这条最小链路亲手跑通。**

---

# 四、当前实验环境

继续使用现有项目：

```text
Agent/projects/agent-learning-lab
```

当前技术：

```text
Java 21
Java HttpClient
FastJSON2
OpenAI-compatible Chat Completions
阿里云百炼 Model Studio
qwen3.8-max
```

环境变量继续复用：

```text
DASHSCOPE_API_KEY
DASHSCOPE_BASE_URL
```

不要：

```text
把 API Key 写进代码
```

---

# 五、百炼 Structured Output 当前调用方式

我们当前仍使用：

```text
Chat Completions
```

普通请求以前主要包含：

```json
{
  "model": "qwen3.8-max",
  "messages": [...]
}
```

这次要新增：

```json
"response_format"
```

并指定：

```json
{
  "type": "json_schema"
}
```

也就是说：

```text
Prompt
负责告诉模型“做什么”

JSON Schema
负责告诉模型“结果必须长什么样”
```

---

# 六、这次要使用真正的 JSON Schema 模式

不要只写：

```text
请输出 JSON
```

也不要只使用：

```json
{
  "type": "json_object"
}
```

本次实验直接使用：

```text
JSON Schema Mode
+
strict = true
```

因为我们的目标正是验证：

```text
Schema
↓
Structured Output
↓
DTO
```

完整链路。

---

# 七、实验业务场景

设计一个简单的：

```text
Campaign Analysis
广告活动分析
```

输入一条虚构广告活动数据：

```text
Campaign ID：10001
日预算：5000 元
近 7 天转化率：0.3%
```

让模型返回：

```text
campaignId
riskLevel
reason
suggestion
```

---

# 八、为什么选这个例子？

它足够简单，但正好覆盖：

```text
integer
string
enum
DTO
业务文本字段
```

同时又不像：

```text
name / age
```

那样过于脱离实际后端业务。

但这只是：

```text
学习用虚构数据
```

不要把模型输出当成真实广告投放建议。

---

# 九、目标 Structured Output

期望模型输出类似：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "日预算较高且近 7 天转化率偏低",
  "suggestion": "适当降低预算，并继续观察转化表现"
}
```

注意：

```text
具体 reason / suggestion
不要求逐字一致
```

但是结构必须符合 Schema。

---

# 十、第一步：创建 CampaignAnalysis DTO

建议新增：

```text
src/main/java/com/avexvertex/agent/CampaignAnalysis.java
```

第一轮使用普通 Java class。

不要为了省代码引入 Lombok。

示意：

```java
package com.avexvertex.agent;

public class CampaignAnalysis {

    private Long campaignId;
    private String riskLevel;
    private String reason;
    private String suggestion;

    public Long getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(Long campaignId) {
        this.campaignId = campaignId;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }

    @Override
    public String toString() {
        return "CampaignAnalysis{" +
                "campaignId=" + campaignId +
                ", riskLevel='" + riskLevel + '\'' +
                ", reason='" + reason + '\'' +
                ", suggestion='" + suggestion + '\'' +
                '}';
    }
}
```

---

# 十一、为什么第一轮 riskLevel 先使用 String？

Schema 会定义：

```text
HIGH
MEDIUM
LOW
```

更工程化的 Java 写法当然可以：

```java
enum RiskLevel
```

但是第一轮实验先使用：

```java
String
```

目的是：

```text
减少变量
```

先把：

```text
LLM → JSON → DTO
```

跑通。

后面如果需要，再把：

```text
String
↓
Java Enum
```

作为小扩展。

---

# 十二、第二步：创建 StructuredOutputDemo

建议新增：

```text
src/main/java/com/avexvertex/agent/StructuredOutputDemo.java
```

这个类负责：

```text
读取环境变量
↓
构造 endpoint
↓
构造 Request Body
↓
发送 HTTP
↓
检查 Status
↓
解析 Provider Response
↓
取得 message.content
↓
反序列化 CampaignAnalysis
↓
打印 DTO
```

---

# 十三、Schema 应该长什么样？

这次使用：

```json
{
  "type": "object",
  "properties": {
    "campaignId": {
      "type": "integer",
      "description": "广告活动 ID"
    },
    "riskLevel": {
      "type": "string",
      "enum": [
        "HIGH",
        "MEDIUM",
        "LOW"
      ],
      "description": "广告活动当前风险等级"
    },
    "reason": {
      "type": "string",
      "description": "风险判断的主要原因"
    },
    "suggestion": {
      "type": "string",
      "description": "给出的简短优化建议"
    }
  },
  "required": [
    "campaignId",
    "riskLevel",
    "reason",
    "suggestion"
  ],
  "additionalProperties": false
}
```

---

# 十四、response_format 应该怎样理解？

完整概念结构：

```json
{
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "campaign_analysis",
      "strict": true,
      "schema": {
        ...
      }
    }
  }
}
```

这里不要把几个 `type` 搞混。

第一层：

```json
"type": "json_schema"
```

是在说：

```text
这次 Response Format 使用 JSON Schema 模式
```

而内部 Schema：

```json
"schema": {
  "type": "object"
}
```

这个：

```text
type = object
```

是在说：

```text
模型最终输出的数据最外层是 object
```

它们虽然都叫：

```text
type
```

但所在层级完全不同。

---

# 十五、json_schema.name 是什么？

例如：

```json
"name": "campaign_analysis"
```

这是：

```text
这个 Schema 的名称
```

它不是模型最终必须返回的字段。

不要把：

```text
Schema metadata
```

和：

```text
业务输出字段
```

混淆。

---

# 十六、strict = true 是什么？

```json
"strict": true
```

表示：

```text
要求模型严格遵循给定 Schema
```

本次实验不是：

```text
“请尽量照这个格式”
```

而是使用 Provider 提供的：

```text
严格 Structured Output 能力
```

---

# 十七、请求体完整结构

概念上应该类似：

```json
{
  "model": "qwen3.8-max",
  "messages": [
    {
      "role": "user",
      "content": "请分析下面广告活动的风险。Campaign ID：10001；日预算：5000 元；近 7 天转化率：0.3%。"
    }
  ],
  "response_format": {
    "type": "json_schema",
    "json_schema": {
      "name": "campaign_analysis",
      "strict": true,
      "schema": {
        "type": "object",
        "properties": {
          "campaignId": {
            "type": "integer",
            "description": "广告活动 ID"
          },
          "riskLevel": {
            "type": "string",
            "enum": ["HIGH", "MEDIUM", "LOW"],
            "description": "广告活动当前风险等级"
          },
          "reason": {
            "type": "string",
            "description": "风险判断的主要原因"
          },
          "suggestion": {
            "type": "string",
            "description": "给出的简短优化建议"
          }
        },
        "required": [
          "campaignId",
          "riskLevel",
          "reason",
          "suggestion"
        ],
        "additionalProperties": false
      }
    }
  },
  "reasoning_effort": "none"
}
```

---

# 十八、Prompt 还需要写“请输出 JSON”吗？

本次使用：

```text
json_schema
```

模式。

所以不需要依靠：

```text
“请以 JSON 格式输出”
```

才能让 API 工作。

这正好体现：

```text
Prompt
和
Structured Output
```

不是一回事。

Prompt：

```text
分析什么
```

Schema：

```text
结果长什么样
```

---

# 十九、为什么保留 reasoning_effort = none？

我们之前的基础实验已经采用：

```json
"reasoning_effort": "none"
```

这里继续保持：

```text
尽量减少实验变量
```

当前重点不是：

```text
比较推理强度
```

而是：

```text
验证 Structured Output
```

---

# 二十、StructuredOutputDemo 主流程

程序可以按下面的结构组织：

```text
main
│
├── 读取 DASHSCOPE_API_KEY
├── 读取 DASHSCOPE_BASE_URL
├── 拼接 /chat/completions
├── 构造 requestBody
├── 创建 HttpRequest
├── HttpClient.send()
├── 打印 HTTP Status
├── 非 2xx → 退出
├── 解析 Response Body
├── 读取 choices[0].message.content
├── 打印原始 Structured Content
├── JSON.parseObject(content, CampaignAnalysis.class)
└── 打印 DTO 字段
```

---

# 二十一、为什么必须打印原始 content？

不要只打印：

```java
CampaignAnalysis
```

我们还应该看到：

```text
模型真实返回了什么 JSON
```

例如：

```text
Structured Content:
{"campaignId":10001,"riskLevel":"HIGH","reason":"...","suggestion":"..."}
```

这样才能把：

```text
模型输出
```

和：

```text
DTO
```

对应起来。

---

# 二十二、为什么还要分别打印 DTO 字段？

例如：

```text
Campaign ID: 10001
Risk Level: HIGH
Reason: ...
Suggestion: ...
```

这一步证明：

```text
模型结果
已经不只是 String
```

而是真的进入：

```text
CampaignAnalysis
```

业务代码已经能够：

```java
analysis.getRiskLevel()
```

直接使用。

---

# 二十三、第一阶段成功标准

至少满足：

```text
HTTP Status = 2xx

choices 非空

message.content 是 String

content 是目标结构 JSON

FastJSON2 成功反序列化

CampaignAnalysis 不为 null

campaignId 不为 null

riskLevel 不为空

reason 不为空

suggestion 不为空
```

并且你能解释：

```text
每一步是谁负责的
```

---

# 二十四、响应解析仍然有两层

一定要看清：

```text
第一层：
HTTP Response Body
```

它属于：

```text
Provider API Response
```

Java 第一次解析以后，取得：

```text
message.content
```

然后第二次：

```text
Structured JSON
↓
CampaignAnalysis
```

也就是说：

```text
Provider Response JSON
和
业务 Structured JSON
```

不是同一个东西。

---

# 二十五、核心反序列化代码

核心动作类似：

```java
CampaignAnalysis analysis =
        JSON.parseObject(content, CampaignAnalysis.class);
```

然后：

```java
System.out.println("Campaign ID: " + analysis.getCampaignId());
System.out.println("Risk Level: " + analysis.getRiskLevel());
System.out.println("Reason: " + analysis.getReason());
System.out.println("Suggestion: " + analysis.getSuggestion());
```

这一步完成：

```text
Structured JSON String
↓
CampaignAnalysis
```

也就是：

```text
Deserialization
```

---

# 二十六、第一阶段需要记录什么？

真实运行后，把下面内容记录进本篇：

```text
运行日期
HTTP Status
Model
Raw Structured Content
DTO 字段
finish_reason
prompt_tokens
completion_tokens
total_tokens
```

不要现在提前填写。

只有真实运行后再补。

---

# 二十七、第一阶段实测记录

2026-11-03，用户在 IDEA 运行并提供完整输出：HTTP 200，model=qwen3.8-max，finish_reason=stop，prompt/completion/total_tokens=52/98/150。
请求包含 json_schema、strict=true、四字段 required 和 additionalProperties=false，接口接受此次请求。
原始响应中可见 campaignId=10001、riskLevel=HIGH、reason 与 suggestion；完整原文见第五十八节。

第一次调用时只完成 HTTP 发送与原始响应打印；第二次验证了 content 提取，第三次进一步验证了 DTO 与 getter。三次详细记录见第五十八节，不混用不同阶段的结果。

---

# 二十八、第一阶段运行后应该回答的问题

1. `response_format` 放在 Request Body 哪一层？
2. `json_schema` 放在哪里？
3. 真正的业务 Schema 放在哪里？
4. 模型最终返回 JSON 在 Response 的哪里？
5. 为什么需要两次 JSON 处理？
6. 哪一步开始从 JSON 进入 Java 类型？
7. `CampaignAnalysis.class` 起什么作用？

这些问题比：

```text
“代码跑通了”
```

更重要。

---

# 二十九、第二阶段：错误路径实验

正常链路跑通以后，再故意制造问题。

但第二阶段不一定要：

```text
每个错误都真实调用模型
```

因为：

```text
strict = true
```

本来就应该帮助模型避免部分非法输出。

为了观察：

```text
Java 收到坏数据时会怎样
```

可以直接使用：

```text
本地模拟 JSON
```

这样更稳定，也不会为了制造错误反复消耗真实 API。

---

# 三十、错误实验 A：非法 JSON

模拟：

```text
{
  "campaignId": 10001,
  "riskLevel": "HIGH"
```

故意缺少：

```text
}
```

观察：

```text
FastJSON2 是否抛出 JSON 解析异常
```

重点：

```text
这是 JSON Syntax 问题
```

不是：

```text
Business Validation
```

---

# 三十一、错误实验 B：错误类型

模拟：

```json
{
  "campaignId": "abc",
  "riskLevel": "HIGH",
  "reason": "预算过高",
  "suggestion": "降低预算"
}
```

DTO：

```java
Long campaignId;
```

观察：

```text
反序列化会怎样？
```

重点：

```text
这是 Type Mapping / Deserialization 问题
```

---

# 三十二、错误实验 C：字段缺失

模拟：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "预算过高"
}
```

缺少：

```text
suggestion
```

观察：

```text
FastJSON2 是否仍能创建 DTO？

analysis.getSuggestion()
是什么？
```

重点：

```text
DTO 反序列化成功
不代表 required 一定满足
```

---

# 三十三、错误实验 D：非法 enum

模拟：

```json
{
  "campaignId": 10001,
  "riskLevel": "CRITICAL",
  "reason": "预算过高",
  "suggestion": "降低预算"
}
```

如果 DTO 当前仍是：

```java
String riskLevel;
```

那么可能：

```text
反序列化仍然成功
```

这正是我们要观察的。

因为它证明：

```text
Deserialization
≠
Schema Validation
```

---

# 三十四、可选扩展：Java Enum

只有主实验跑通以后，才考虑：

```java
public enum RiskLevel {
    HIGH,
    MEDIUM,
    LOW
}
```

然后 DTO：

```java
private RiskLevel riskLevel;
```

再测试：

```text
CRITICAL
```

观察 Java Enum 是否把：

```text
Schema enum
```

的约束进一步带进 Java 类型系统。

这是：

```text
可选扩展
```

不是第一轮必做。

---

# 三十五、Validation 这次做到什么程度？

这次先建立三层意识：

```text
Schema Constraint
↓
Deserialization
↓
简单 Java Validation
```

不需要马上引入：

```text
Jakarta Bean Validation
```

可以简单判断：

```java
if (analysis.getCampaignId() == null) {
    ...
}
```

或者：

```java
if (analysis.getRiskLevel() == null
        || analysis.getRiskLevel().isBlank()) {
    ...
}
```

目的不是写生产级 Validator。

而是理解：

```text
模型输出不能无条件信任
```

---

# 三十六、为什么不做数据库 Business Validation？

不要为了：

```text
Business Validation
```

又引入数据库。

我们只需要知道：

```text
未来 campaignId 是否真实存在
应该由业务层去验证
```

这次做：

```text
最简单字段检查
```

就够了。

---

# 三十七、错误模拟和真实调用要分开

建议：

```text
正常模式
→ 真实模型调用

错误模式
→ 本地 JSON 字符串
```

不要为了制造错误：

```text
破坏正常 Schema
```

然后再调用真实模型。

---

# 三十八、这次应该观察哪些异常？

至少理解：

```text
JSON 解析异常
类型映射异常
字段缺失导致 null
非法 enum 在 String DTO 下可能被接受
HTTP 非 2xx
Provider Error Body
```

不需要现在建立完整异常体系。

Node 020：

```text
LLM API 异常
```

还会专门系统学习。

---

# 三十九、为什么必须打印原始 Response Body？

学习阶段应该保留：

```text
HTTP Status
Response Body
```

因为如果：

```text
response_format 层级写错
json_schema 缺字段
Provider 不接受参数
```

错误信息就在：

```text
原始 Response Body
```

里面。

但不要打印：

```text
API Key
Authorization Header
```

---

# 四十、继续记录 Token Usage

和以前一样，继续记录：

```text
prompt_tokens
completion_tokens
total_tokens
```

这次可以顺便观察：

```text
Schema 也属于请求输入的一部分
```

但本 Node：

```text
不做 Token A/B
```

因为当前实验目标不是：

```text
研究 Schema 成本
```

而是：

```text
跑通 Structured Output → DTO
```

---

# 四十一、为什么不比较 json_object 和 json_schema？

我们已经在理论上理解：

```text
json_object
=
保证 JSON

json_schema
=
保证指定结构
```

当前目标就是：

```text
Schema → DTO
```

所以直接使用：

```text
json_schema
```

即可。

不为了形式多做一次实验。

---

# 四十二、为什么不比较不同模型？

模型选择属于：

```text
Node 018
```

本 Node 固定：

```text
qwen3.8-max
```

减少变量。

---

# 四十三、为什么不引入 Spring Boot？

Structured Output 本质上和：

```text
Controller
Tomcat
Spring MVC
```

没有关系。

一个：

```text
main()
```

已经足够验证核心机制。

Spring Boot 留给后面的第一阶段项目。

---

# 四十四、建议新增的代码文件

本实验完成后：

```text
src/main/java/com/avexvertex/agent/
├── FirstModelCall.java
├── MessageHistoryDemo.java
├── SystemPromptDemo.java
├── PromptBasicsDemo.java
├── StructuredOutputDemo.java
└── CampaignAnalysis.java
```

现在不要为了两个类再拆：

```text
dto/
service/
client/
schema/
validator/
```

等真实项目长大以后再重构。

---

# 四十五、建议的实现顺序

不要一次全部生成完再看。

建议：

```text
第一步
创建 CampaignAnalysis
↓
确认 DTO 字段和类型

第二步
创建 StructuredOutputDemo 请求体
↓
重点检查 response_format / json_schema

第三步
复用现有 HttpClient 调用逻辑
↓
编译

第四步
真实调用
↓
先观察 Structured Content

第五步
parseObject → CampaignAnalysis
↓
打印 DTO 字段

第六步
再做必要错误模拟
```

---

# 四十六、Codex 在这次实验里负责什么？

Codex 可以：

```text
读取现有 agent-learning-lab
复用现有代码风格
新增 DTO
新增 Demo
编译
本地错误模拟
修复编译问题
生成交接报告
```

但不能：

```text
伪造真实 API 运行结果
```

如果环境拿不到：

```text
DASHSCOPE_API_KEY
```

真实模型调用由用户在 IDEA 执行。

---

# 四十七、Codex 不应该做什么？

不要让它：

```text
重构全部旧 Demo
引入 Spring AI
引入 Lombok
替换 FastJSON2
修改 Provider
大规模拆 package
加入复杂架构
```

这次任务应该非常聚焦。

---

# 四十八、建议的 Codex 任务目标

可以要求：

```text
基于现有 agent-learning-lab 新增 Structured Output 学习实验。

保持 Java 21 + HttpClient + FastJSON2。

使用当前 qwen3.8-max 与 OpenAI-compatible Chat Completions。

通过 response_format.json_schema + strict=true
要求模型输出 CampaignAnalysis 结构。

将 choices[0].message.content
反序列化成 CampaignAnalysis DTO。

正常真实调用逻辑和本地错误 JSON 模拟分开。

不要引入 Spring AI、Lombok 或新的架构抽象。
```

---

# 四十九、真实运行谁来做？

推荐：

```text
Codex
负责代码修改、编译、本地模拟

用户
在 IDEA 使用已有环境变量真实调用模型
```

这样：

```text
API Key
```

继续留在用户本地。

真实结果也由用户亲眼确认。

---

# 五十、真实运行后发回什么？

把控制台完整输出发给 ChatGPT。

至少包括：

```text
HTTP Status
Response Body
Structured Content
DTO 字段
Token Usage
```

不要包含：

```text
API Key
Authorization Header
```

---

# 五十一、拿到真实输出后做什么？

下一步再：

```text
逐层解释输出
检查 Structured Output 是否真的生效
检查 DTO 是否正确
分析 Token Usage
分析 finish_reason
判断错误模拟是否还需要全部执行
```

不是默认把所有可选实验都做完。

---

# 五十二、错误实验也遵循“必要时才做”

本文列出了：

```text
非法 JSON
错误类型
字段缺失
非法 enum
```

目的不是：

```text
凑四个实验
```

而是分别帮助理解：

```text
Syntax
Type Mapping
required
Schema enum
```

如果某个问题已经完全理解：

```text
可以不继续增加实验
```

---

# 五十三、第一阶段验收问题

真实调用成功后，应该能回答：

1. Schema 是不是随请求发给 Provider？
2. `response_format` 和业务 `schema` 分别在哪一层？
3. `strict=true` 是干什么的？
4. 模型结构化结果在 Response 的哪里？
5. 为什么要做两次 JSON 处理？
6. DTO 是在哪一步创建出来的？
7. FastJSON2 在这条链里负责什么？
8. 为什么 HTTP 200 不等于实验完成？

---

# 五十四、第二阶段验收问题

完成必要错误验证后，应该能回答：

1. JSON Syntax Error 是谁发现的？
2. `"abc" → Long` 属于什么问题？
3. 字段缺失时为什么 DTO 可能仍然创建成功？
4. `CRITICAL` 为什么在 String DTO 下可能不会报错？
5. 为什么 Deserialization 不等于 Schema Validation？
6. 为什么 DTO 创建成功不等于业务正确？
7. Schema、Java 类型和 Business Validation 各管什么？

---

# 五十五、Node 015 的最终理解

完成以后应该能完整解释：

```text
Prompt
告诉模型分析什么

JSON Schema
告诉模型输出什么结构

Provider
执行 Structured Output 约束

Model
生成符合结构的数据

API Response
包裹模型结果

message.content
承载业务 JSON

FastJSON2
把业务 JSON 反序列化

CampaignAnalysis
在 Java 中承载结果

Validation
继续检查程序能否安全使用

Business Logic
真正消费模型结果
```

---

# 五十六、实验完成标准

Node 015 按用户确认收尾。理论项依据阅读与分步讲解，不另行逐题口述；实测项依据原始输出。错误模拟只执行字段缺失与非法风险等级两项，非法 JSON 和类型错误不补造实测记录。

## 理论

- [x] 已完成 015-1 理论篇；
- [x] 能解释 Schema 和 DTO 的关系；
- [x] 能解释反序列化；
- [x] 能区分 Parsing / Deserialization / Validation。

## 代码

- [x] 新增 `CampaignAnalysis.java`；
- [x] 新增 `StructuredOutputDemo.java`（真实正常链路已跑通，本地 --errors 两项已执行）；
- [x] 使用 Java 21 编译通过（含 DTO、HTTP、反序列化、共用字段检查与本地两样本）；
- [x] 没有新增不必要依赖；
- [x] 没有泄露 API Key。

## Structured Output

- [x] Request 使用 `response_format.type = json_schema`；
- [x] 提供 `json_schema.name`；
- [x] 使用 `strict = true`；
- [x] 提供完整 `schema`；
- [x] 使用 `required`；
- [x] 使用 `additionalProperties = false`。

## 真实调用

- [x] IDEA 真实调用成功；
- [x] HTTP Status 为 2xx；
- [x] Java 单独取得 Structured Content（第二次 IDEA 实际输出已包含该段）；
- [x] 原始响应中 Content 呈目标结构 JSON（本样本的人工观察，未运行独立 Schema 校验器）；
- [x] 成功反序列化 `CampaignAnalysis`（第三次真实输出）；
- [x] Java 能正常读取所有字段（第三次真实 getter 输出）；
- [x] 记录 Token Usage。

## 必要错误理解

- [x] 能解释非法 JSON（理论说明，未单独执行该样本）；
- [x] 能解释字段类型不匹配（理论说明，未单独执行该样本）；
- [x] 能解释字段缺失（本地实际得到 null，再被字段检查拒绝）；
- [x] 能解释非法 enum（本地 String 实际接受 CRITICAL，再被字段检查拒绝）；
- [x] 不把 Deserialization 当成完整 Schema Validation。

---

# 五十七、本篇当前状态

收尾状态（2026-11-03；用户确认结束，真实正常链路与本地 C/D 样本分别记录）：

```text
Node 015：✅ 完成当前学习范围，下一步 Node 016 - Streaming（准备学习）

实验设计：✅ 完成

CampaignAnalysis.java：
✅ 已创建并编译，正常真实响应已反序列化；本地缺失/非法等级也已观察

StructuredOutputDemo.java：
✅ 已真实执行 content 读取、CampaignAnalysis 转换及四个 getter 打印

真实 API：
✅ 用户已在 IDEA 真实调用，HTTP 200 / stop

真实 Structured Output：
✅ 已保存三次实际响应，第三次业务 JSON 已转为 DTO

DTO 反序列化：
✅ 第三次实际输出包含 CampaignAnalysis 及四个 getter 的结果

错误路径：
✅ 本地缺少 suggestion、非法风险等级两个样本已执行
⬜ 非法 JSON、错误字段类型及完整 HTTPS 检查脚本未执行
```

不要提前把这些：

```text
⬜
```

改成：

```text
✅
```

只有真实完成后再更新。

---

# 五十八、真实实验记录区

## 第一次真实调用（2026-11-03，用户提供 IDEA 输出）

```text
HTTP Status：200
Model：qwen3.8-max
Request Schema：见本文四字段 CampaignAnalysis Schema；实际请求为 json_schema / strict=true
CampaignAnalysis：尚未反序列化，未创建接收结果的 DTO 实例
Campaign ID / Risk Level / Reason / Suggestion 的 getter 输出：尚未实现
Finish Reason：stop
Prompt Tokens：52
Completion Tokens：98
Total Tokens：150
```

Raw Response Body（原文）：

```json
{"id":"chatcmpl-c0d3a1ba-1569-9b89-b1c5-c528058e30a1","object":"chat.completion","created":1793703132,"model":"qwen3.8-max","choices":[{"index":0,"message":{"role":"assistant","content":"{\n  \"campaignId\": 10001,\n  \"riskLevel\": \"HIGH\",\n  \"reason\": \"近7天转化率仅为0.3%，远低于行业平均水平，结合5000元的日预算，存在严重的广告费浪费风险。\"\n  ,\n  \"suggestion\": \"建议立即降低日预算或暂停投放，排查落地页体验、受众定向及创意素材问题，优化后再恢复测试。\"\n}"},"finish_reason":"stop","logprobs":null}],"usage":{"prompt_tokens":52,"total_tokens":150,"completion_tokens":98,"prompt_tokens_details":{"cached_tokens":0,"text_tokens":52}}}
```

Structured Content（从用户提供的原始响应中展开供阅读；不是 Java 新增的提取输出）：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "近7天转化率仅为0.3%，远低于行业平均水平，结合5000元的日预算，存在严重的广告费浪费风险。"
  ,
  "suggestion": "建议立即降低日预算或暂停投放，排查落地页体验、受众定向及创意素材问题，优化后再恢复测试。"
}
```

### 观察

- 此次服务端接受了 response_format；实际 content 有四个 required 字段，无额外业务字段，campaignId 为整数，riskLevel 属于给定枚举，reason / suggestion 为字符串。
- 这是一个实际样本的形状观察，未运行独立 Schema 校验器，也未声称 DTO 已转换成功。
- 模型声称“远低于行业平均水平”，但输入只有预算和转化率，没有行业或基准数据，这属于没有输入依据的推断。结构正确不保证业务结论真实，模型建议不是实际投放操作。
- 原始响应的 content 是承载业务 JSON 的字符串；下一步先解析外层响应并读取它，再学习把这段字符串反序列化成 CampaignAnalysis。
- 原始 usage 如实记录，不从本样本推断 Schema 不计 Token 或直接推算账单。
- 新增本地 HTTP 检查、DTO 反序列化和错误实验尚未执行。

---

## 第二次真实调用：Java 提取 content 成功（2026-11-03）

用户重新运行 StructuredOutputDemo，原始 Response Body 后实际出现 Structured Content。请求仍为相同 Prompt、四字段 Schema、strict=true 与 reasoning_effort=none。

```text
HTTP Status：200
Model：qwen3.8-max
Finish Reason：stop
Prompt Tokens：52
Completion Tokens：76
Total Tokens：128
CampaignAnalysis / getter 输出：仍未实现
```

Raw Response Body（原文）：

```json
{"id":"chatcmpl-e303511d-0f19-9d44-80b9-51d3ee19fe64","object":"chat.completion","created":1793703519,"model":"qwen3.8-max","choices":[{"index":0,"message":{"role":"assistant","content":"{\n  \"campaignId\": 10001,\n  \"riskLevel\": \"HIGH\",\n  \"reason\": \"转化率极低且预算消耗风险高，存在严重的ROI亏损可能。\"\n  ,\n  \"suggestion\": \"建议立即暂停或大幅降低预算，排查落地页、定向人群及广告素材问题。\"\n}"},"finish_reason":"stop","logprobs":null}],"usage":{"prompt_tokens":52,"total_tokens":128,"completion_tokens":76,"prompt_tokens_details":{"cached_tokens":0,"text_tokens":52}}}
```

Java 实际打印的 Structured Content：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "转化率极低且预算消耗风险高，存在严重的ROI亏损可能。"
  ,
  "suggestion": "建议立即暂停或大幅降低预算，排查落地页、定向人群及广告素材问题。"
}
```

该段与本次原始响应的 choices[0].message.content 一致，证明外层 JSONObject → content 字符串的提取已实际执行。内容尚未反序列化成 DTO，不能把 campaignId=10001 等原始 JSON 字段记成 getter 输出。
模型提到 ROI 亏损，但输入未提供收益/成本等完整依据，不将其判断当成已验证业务事实。两次模型文字及输出 Token 不同，记录分别保留。
下一步只做 content → CampaignAnalysis，并通过 getter 打印，再补实际结果；错误模拟与校验器仍未执行。

---

## 第三次真实调用：DTO 与 getter 成功（2026-11-03）

用户提供 StructuredOutputDemo 实际输出，HTTP 200、qwen3.8-max、finish_reason=stop；本次输入/输出/总 Token 为 52/192/244。
请求仍使用原四字段 Schema、strict=true 和同一虚构广告活动，不改 Prompt 来制造结果。

原始 Response Body（按用户原始字段保存）：

```json
{"id":"chatcmpl-10cd896a-619d-9f16-a762-a38eecddced5","object":"chat.completion","created":1793703785,"model":"qwen3.8-max","choices":[{"index":0,"message":{"role":"assistant","content":"{\n  \"campaignId\": 10001,\n  \"riskLevel\": \"HIGH\",\n  \"reason\": \"近7天转化率仅0.3%，远低于行业基准（通常1%-3%），且日预算高达5000元，意味着每天约1666次点击中仅产生约5次转化。按此趋势，单日广告支出回报率（ROAS）极可能为负，存在严重的预算浪费风险。建议立即暂停或大幅缩减预算，并排查落地页体验、受众定向精准度、创意素材吸引力及转化路径是否存在障碍。\"\n  ,\n  \"suggestion\": \"1. 暂停投放，避免持续亏损；2. 检查落地页加载速度与移动端适配；3. 缩小受众范围，聚焦高意向人群；4. A/B测试新创意素材；5. 确认转化追踪代码是否正常工作。\"\n}"},"finish_reason":"stop","logprobs":null}],"usage":{"prompt_tokens":52,"total_tokens":244,"completion_tokens":192,"prompt_tokens_details":{"cached_tokens":0,"text_tokens":52}}}
```

实际 Structured Content：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "近7天转化率仅0.3%，远低于行业基准（通常1%-3%），且日预算高达5000元，意味着每天约1666次点击中仅产生约5次转化。按此趋势，单日广告支出回报率（ROAS）极可能为负，存在严重的预算浪费风险。建议立即暂停或大幅缩减预算，并排查落地页体验、受众定向精准度、创意素材吸引力及转化路径是否存在障碍。"
  ,
  "suggestion": "1. 暂停投放，避免持续亏损；2. 检查落地页加载速度与移动端适配；3. 缩小受众范围，聚焦高意向人群；4. A/B测试新创意素材；5. 确认转化追踪代码是否正常工作。"
}
```

实际对象与 getter 输出：

```text
CampaignAnalysis: CampaignAnalysis{campaignId=10001, riskLevel='HIGH', reason='近7天转化率仅0.3%，远低于行业基准（通常1%-3%），且日预算高达5000元，意味着每天约1666次点击中仅产生约5次转化。按此趋势，单日广告支出回报率（ROAS）极可能为负，存在严重的预算浪费风险。建议立即暂停或大幅缩减预算，并排查落地页体验、受众定向精准度、创意素材吸引力及转化路径是否存在障碍。', suggestion='1. 暂停投放，避免持续亏损；2. 检查落地页加载速度与移动端适配；3. 缩小受众范围，聚焦高意向人群；4. A/B测试新创意素材；5. 确认转化追踪代码是否正常工作。'}
Campaign ID: 10001
Risk Level: HIGH
Reason: 近7天转化率仅0.3%，远低于行业基准（通常1%-3%），且日预算高达5000元，意味着每天约1666次点击中仅产生约5次转化。按此趋势，单日广告支出回报率（ROAS）极可能为负，存在严重的预算浪费风险。建议立即暂停或大幅缩减预算，并排查落地页体验、受众定向精准度、创意素材吸引力及转化路径是否存在障碍。
Suggestion: 1. 暂停投放，避免持续亏损；2. 检查落地页加载速度与移动端适配；3. 缩小受众范围，聚焦高意向人群；4. A/B测试新创意素材；5. 确认转化追踪代码是否正常工作。
```

### 本次验证了什么

- 第一次 JSON 处理解析 Provider Response，取得 content 字符串；第二次通过 JSON.parseObject(content, CampaignAnalysis.class) 创建业务对象。
- toString 与四个 getter 的实际输出一致，证明 Java 已能通过类型化字段访问结果，正常链路完成。
- 当前空值、风险等级和文本检查通过，并不等于完整 Schema Validation，也没有查询数据库或核实实际广告事实。
- 模型提到“行业基准通常 1%-3%”“1666 次点击”“约 5 次转化”，输入并未提供行业基准、点击成本、实际点击量或收益，无法据此推算这些数值或 ROAS。原文保留作为“结构/类型正确不代表事实正确”的实例，不能作为真实广告操作依据。
- 本次 usage 直接来自原始响应，未在 Java 中新增独立的 Model / Finish Reason / Token 打印；不将记录动作冒充运行功能。
- 三次结果分别保存，未覆盖前两次内容；本地模拟和第二阶段错误实验仍未执行。

---

# 五十九、错误实验记录区

2026-11-03，用户明确要求执行两个本地坏数据演示。本次只运行 C、D，不读取 API 环境变量或发送 HTTP，不用模拟结果冒充模型结果。

## 运行方式与边界

同一 StructuredOutputDemo：不带参数仍为真实 API 模式；程序实参填写 `--errors` 时，只运行以下两个本地样本并在读取环境变量之前返回。

实际执行：

```bash
env -u DASHSCOPE_API_KEY -u DASHSCOPE_BASE_URL java -cp target/classes:/Users/nox/.m2/repository/com/alibaba/fastjson2/fastjson2/2.0.65/fastjson2-2.0.65.jar com.avexvertex.agent.StructuredOutputDemo --errors
```

Maven Java 21 编译通过，运行退出码 0。此退出码表示两个拒绝行为符合演示预期，不表示坏数据被业务流程接受。
真实 HTTP 流程与两个样本共用 validateCampaignAnalysis；拒绝理由不符或坏数据意外通过时，本地模式会报非预期结果并失败。未调用完整 check-config.py。

## A：非法 JSON

未执行，本次不追加。未填写异常实测值。

## B：错误字段类型

未执行，本次不追加。未填写类型映射实测值。

## C：字段缺失（已执行）

实际使用的本地 JSON：

```json
{
  "campaignId": 10001,
  "riskLevel": "HIGH",
  "reason": "本地模拟原因"
}
```

实际输出：

```text
反序列化结果: CampaignAnalysis{campaignId=10001, riskLevel='HIGH', reason='本地模拟原因', suggestion='null'}
suggestion == null: true
riskLevel: HIGH
字段检查拒绝: DTO 的 reason 和 suggestion 必须有非空白文本。
```

toString 中的 suggestion='null' 是本类拼接显示的效果，实际 getter 返回 Java null，而不是字符串 "null"。
FastJSON2 没有因为缺少 suggestion 就拒绝创建 DTO，随后共用的字段检查抛出 IllegalStateException 并由演示捕获。
这说明 DTO 反序列化不是 required 校验。

## D：非法 enum（已执行）

实际使用的本地 JSON：

```json
{
  "campaignId": 10001,
  "riskLevel": "CRITICAL",
  "reason": "本地模拟原因",
  "suggestion": "本地模拟建议"
}
```

实际输出：

```text
反序列化结果: CampaignAnalysis{campaignId=10001, riskLevel='CRITICAL', reason='本地模拟原因', suggestion='本地模拟建议'}
suggestion == null: false
riskLevel: CRITICAL
字段检查拒绝: DTO 的 riskLevel 必须是 HIGH、MEDIUM 或 LOW。
```

DTO 的 riskLevel 是 String，因此反序列化成功并保留 CRITICAL；限制 HIGH / MEDIUM / LOW 的是后面的 Java 字段检查，不是 String 类型本身。
本地样本没有送到 Provider，也没有让 Provider 执行 strict Schema，不能用它推断真实服务的 Schema 约束失效。

## 两个样本的共同结论

```text
能解析并创建 DTO
≠
required / enum 都满足
≠
业务事实正确
```

最后实际输出：

```text
本地演示完成：两个样本都成功转成 DTO，但均被字段检查拒绝。
```

本次只验证上述两个本地样本，没有运行全部 HTTPS 模拟或加入新的校验框架。

---

# 六十、什么时候 Node 015 可以结束？

当你真正能够从头解释：

```text
Schema
↓
发送给模型服务
↓
Model Structured Output
↓
message.content
↓
FastJSON2
↓
CampaignAnalysis
↓
简单 Validation
↓
Business Logic
```

并且已经：

```text
真实跑通一次
```

Node 015 就基本达到目标。

不要求把 Structured Output 做成：

```text
生产级框架
```

---

# 六十一、不要在这个 Node 无限扩展

下面暂时不要继续追：

```text
自动 Schema Generator
多模型 Structured Output 对比
复杂嵌套 DTO
泛型 Schema
Spring AI Entity Mapping
Jackson 对比 FastJSON2
完整 Bean Validation
JSON Schema Validator 框架
Provider 抽象
统一 LLM Gateway
```

以后真实需要再学。

---

# 六十二、Node 015 结束后的下一步

完成以后进入：

```text
Node 016：Streaming
```

会从：

```text
Request
↓
等待完整 Response
```

继续学习：

```text
Request
↓
模型边生成
↓
程序边接收
```

但进入 Node 016 前：

```text
先把 Node 015 的真实 Structured Output → DTO 跑通。
```

---

# 本篇一句话总结

> **015-2 的目标不是“让模型打印一个 JSON”，而是让 Java 把严格 Structured Output 真正转换成一个可直接参与业务代码的 DTO。**

核心闭环：

```text
Java
↓
Prompt + JSON Schema
↓
qwen3.8-max
↓
Structured JSON
↓
FastJSON2
↓
CampaignAnalysis
↓
Java Business Logic
```

完成这个闭环以后：

```text
我们才真正从
“调用模型”
开始走向
“把模型嵌进后端程序”
```

---

# 资料核对说明

本实验的 Provider-specific 部分以生成本文时的阿里云百炼官方文档为依据：

```text
OpenAI-compatible Chat Completions
response_format.type = json_schema
json_schema.name
json_schema.schema
json_schema.strict = true
additionalProperties = false
qwen3.8-max 支持 Structured Output
```

以后如果百炼 API 发生变化：

```text
以最新官方文档为准
```

而本篇真正需要长期掌握的是：

```text
Schema
↓
Structured Output
↓
Deserialization
↓
DTO
```

这条通用工程链路。

## Node 015 收尾说明（2026-11-03）

用户确认本 Node 可以结束，要求更新笔记、进度并将本轮代码提交推送。
已完成必要学习闭环：Schema + Prompt → 真实模型结果 → content → FastJSON2 → CampaignAnalysis → getter；并通过两个本地样本观察“创建 DTO 不等于 required / enum 已满足”。
未单独做非法 JSON / 错误字段类型演示、Java Enum 扩展、完整 HTTPS 检查脚本、独立 Schema Validator 或数据库 Business Validation，不把这些内容记为已执行。
当前保留无参数真实 API 模式和 --errors 本地模式，未提前实现 Streaming。
