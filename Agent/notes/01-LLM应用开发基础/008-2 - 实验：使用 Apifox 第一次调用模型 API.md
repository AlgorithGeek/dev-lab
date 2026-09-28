# 008-2 - 实验：使用 Apifox 第一次调用模型 API

> Phase 1：LLM 应用开发基础
> Node 008：第一次调用模型 API
> Note 008-2：第一次真实 API 实验
>
> 前置 Note：
>
> ```
> 008-1 - 第一次调用模型 API
> ```
>
> 本篇和 `008-1` 的学习方式完全不同。
>
> `008-1`：
>
> ```
> 阅读
> ↓
> 理解
> ```
>
> 本篇：
>
> ```
> 阅读一小段
> ↓
> 停下来
> ↓
> 亲手操作
> ↓
> 检查结果
> ↓
> 确认成功
> ↓
> 再继续往下
> ```
>
> **不要先把整篇看完再操作。**
>
> 看到：
>
> # 🛑 停下来操作
>
> 就真的先停下来。
>
> 做成功以后再继续往下。

# 一、这次实验到底要完成什么？

这次实验最终只做一件真正重要的事：

> **亲手从自己的电脑发出第一条 LLM API Request，并真正收到 qwen3.8-max 的 Response。**

最终链路：

```
你
↓
Apifox
↓
HTTP Request
↓
阿里云百炼
↓
qwen3.8-max
↓
Inference
↓
HTTP Response
↓
Apifox
↓
你
```

这次实验成功以后，你必须亲眼看到：

```
HTTP Status

模型回答

choices

message

content

finish_reason

usage

prompt_tokens

completion_tokens

total_tokens
```

# 二、本篇暂时不做什么？

这次不要：

```
Java
Spring Boot
Codex
Spring AI
LangChain4j
Tool Calling
Agent
RAG
Memory
Streaming
Structured Output
```

今天只干：

```
Apifox
+
HTTP
+
模型 API
```

把这一层彻底看懂。

# 三、先认识你现在所在的阿里云百炼

你刚刚充值了一些余额。

这里先搞清楚一件事：

```
阿里云账户有余额
```

不等于：

```
API Key 自动创建好了
```

也不等于：

```
你的程序已经可以直接调用模型
```

真正通过 API 调用模型之前，仍然要完成：

```
百炼服务
↓
选择地域
↓
业务空间
↓
API Key
↓
Workspace ID
↓
Endpoint
↓
模型 API
```

官方当前的首次调用流程也要求先完成百炼服务准备、API Key 获取，以及相应地域的 Workspace ID 获取。

# 四、什么是“地域”？

百炼不是所有模型请求都走同一个全球地址。

目前不同地域拥有独立的：

```
API Key

接入域名

模型列表

业务空间
```

官方明确说明：

> 不同地域的 API Key、接入域名和模型列表不能跨地域混用。

例如：

```
华北2（北京）
```

和：

```
新加坡
```

是两个不同地域。

# 五、为什么地域这一步不能乱？

假设你创建的是：

```
北京地域 API Key
```

却向：

```
新加坡 Endpoint
```

发送请求。

就可能出现：

```
鉴权失败
权限异常
模型不可用
```

等问题。

所以从现在开始形成一个习惯：

> **Key、Workspace、Endpoint、模型可用地域要放在同一个地域里理解。**

# 六、我们第一次学习需要创建复杂业务空间吗？

不需要。

百炼有：

```
默认业务空间
```

也可以自己创建：

```
子业务空间
```

子业务空间以后适合：

```
不同项目隔离

权限隔离

成本核算

团队协作

模型调用权限控制
```

但我们现在只是：

```
个人学习
+
第一次调用
```

所以没有必要一开始增加复杂度。

官方当前的 API Key 创建说明中，也把：

```
默认业务空间
```

作为最直接的默认选择；默认业务空间的 Key 可以调用标准模型，而子业务空间会涉及额外模型授权管理。

因此本实验：

> **优先使用默认业务空间。**

# 七、先确定我们使用哪个地域

我们暂时不替自己凭感觉决定。

真正判断方式：

打开：

```
阿里云百炼控制台
```

然后查看：

```
右上角 / 页面上的地域选择
```

你需要确认：

```
你准备在哪个地域创建 API Key
```

目前 `qwen3.8-max` 官方模型信息显示至少在华北2（北京）和新加坡等地域提供模型服务。

对于第一次学习：

```
选择一个地域
↓
之后全部保持一致
```

即可。

不要今天：

```
北京
```

明天又：

```
新加坡
```

然后把两边的 Key 和 Endpoint 混起来。

# 八、实验记录：我的地域

从这里开始，这篇 Note 会逐渐变成你自己的真实实验记录。

### 我最终使用的地域

```
【完成下面操作以后填写】【最终：华北2（北京）】
```

可能类似：

```
华北2（北京）
```

或者：

```
新加坡
```

# 九、第一次进入百炼控制台

# 🛑 停下来操作

现在先不要继续往下读。

打开：

```
阿里云百炼 Model Studio 控制台
```

如果页面提示：

```
开通百炼
```

或者：

```
开通模型服务
```

按照控制台引导完成开通。

如果系统提示需要：

```
实名认证
```

先完成实名认证。

阿里云当前首次 API 调用指南明确说明，如果开通服务时提示未实名认证，需要先完成实名认证。

## 这一小步的成功标准

你应该能够正常进入：

```
百炼 Model Studio
```

而不是还停留在：

```
产品介绍
立即开通
实名认证
```

等前置页面。

完成后：

```
✅ 百炼控制台可以正常进入
```

然后再继续往下。

# 十、你刚充值的钱现在要干嘛？

暂时什么都不用干。

充值的意义主要是：

```
账户拥有可用于按量调用的余额
```

但：

```
充值
```

和：

```
API Key
```

是两件事。

我们现在不要继续研究：

```
套餐
预留吞吐
复杂计费模式
企业配额
```

第一次实验只需要确保：

```
账户能够正常使用模型 API
```

即可。

具体模型价格以后：

```
以百炼控制台当前模型价格
```

为准。

不要把某个时间点的价格数字硬写死进长期学习笔记。

# 十一、接下来创建人生第一个百炼 API Key

API Key 是：

```
程序调用百炼 API 时使用的凭证
```

现在真正开始创建。

# 十二、找到 API Key 页面

# 🛑 停下来操作

在百炼控制台中找到：

```
API Key
```

或：

```
密钥管理
```

页面。

官方当前操作流程是：

```
进入百炼 API Key 页面
↓
右上角选择地域
↓
创建 API Key
```

每个地域拥有自己独立的 Key。

## 现在先检查地域

非常重要：

在 API Key 页面上：

```
确认当前地域
```

把它记下来。

### 我的地域

```
华北2（北京）
```

## 成功标准

当前页面应该至少能看到类似：

```
API Key 列表
创建 API Key
```

完成以后继续。

# 十三、点击“创建 API Key”

# 🛑 停下来操作

点击：

```
创建 API Key
```

当前官方文档中，创建时主要会涉及：

```
归属账号
归属业务空间
权限
```

等设置。

# 十四、归属账号怎么选？

你现在是自己学习。

如果使用的是自己的阿里云主账号：

优先保持：

```
阿里云账号 / 主账号
```

即可。

以后公司多人协作时：

```
RAM 用户
```

会非常重要。

但现在先不展开。

# 十五、归属业务空间怎么选？

第一次学习：

```
默认业务空间
```

即可。

不要现在创建：

```
agent-dev
agent-test
agent-prod
```

这些。

以后真正生产项目时：

```
业务空间隔离
```

很有价值。

但 Node 008 没必要。

# 十六、权限怎么选？

如果控制台让你选择：

```
全部
```

或者：

```
自定义
```

当前学习阶段优先选择：

```
全部
```

这样避免第一次实验因为：

```
模型权限
IP 白名单
```

等附加设置导致调用失败。

官方当前 API Key 文档也建议普通初始场景选择全部权限；自定义权限则可以进一步控制模型和 IP。

注意：

> **这是学习环境为了降低复杂度。**

以后生产环境不代表：

```
永远应该所有 Key 全权限
```

生产环境反而通常应该：

```
最小权限
```

# 十七、确认创建

# 🛑 停下来操作

完成：

```
归属账号
默认业务空间
权限
```

以后：

```
确定 / 创建
```

## 成功标准

API Key 列表中出现：

```
一条新 Key
```

# 十八、复制 API Key

现在点击：

```
复制
```

把 API Key 暂时保存到：

```
安全位置
```

但是：

# ⚠️ 千万不要把真实 Key 粘进这篇 Markdown

因为这篇笔记以后会：

```
git commit
↓
push GitHub
```

所以绝对不要写：

```
我的 Key：
sk-xxxxxxxxxxxxxxxx
```

# 十九、Git 仓库里应该记录什么？

可以记录：

```
环境变量名称：

DASHSCOPE_API_KEY
```

不能记录：

```
环境变量真实值
```

所以：

```
DASHSCOPE_API_KEY
```

可以放心写。

真实 Key：

```
绝对不能提交
```

# 二十、API Key 创建成功检查

现在检查：

```
✅ 已创建 API Key
✅ Key 所在地域已经确认
✅ Key 属于默认业务空间
✅ 没有把真实 Key 写进 Markdown
```

全部完成再继续。

# 二十一、实验记录

### API Key 状态

```
✅ 已创建
```

### 所属地域

```
________________
```

### 所属业务空间

```
默认业务空间
```

### Key 本身

```
不记录
```

# 二十二、接下来需要找到 Workspace ID

我们在 `008-1` 已经讲过：

很多百炼当前 Endpoint 会长成：

```
https://{WorkspaceId}.xxxxx.maas.aliyuncs.com/...
```

这里：

```
{WorkspaceId}
```

不是让你真的把：

```
{WorkspaceId}
```

这几个字符粘进去。

而是：

```
替换成自己的业务空间 ID
```

# 二十三、去哪里找 Workspace ID？

官方当前说明：

```
业务空间管理页面
```

可以查看 Workspace ID；

在北京、新加坡、日本、德国、中国香港等地域使用相应模型 Endpoint 时，Workspace ID 会成为 Base URL 的组成部分。

# 二十四、找到 Workspace ID

# 🛑 停下来操作

回到百炼控制台。

找到：

```
业务空间管理
```

然后找到：

```
默认业务空间
```

进入：

```
业务空间详情
```

寻找：

```
业务空间 ID
```

或者：

```
Workspace ID
```

# 二十五、现在检查一个非常重要的东西

业务空间的：

```
地域
```

必须和刚才 API Key：

```
地域一致
```

也就是说：

```
API Key：
北京

Workspace：
北京
```

或者：

```
API Key：
新加坡

Workspace：
新加坡
```

而不是混搭。

# 二十六、记录 Workspace ID 吗？

Workspace ID 本身不像 API Key 那样属于高敏感 Secret。

但为了避免把个人环境参数到处复制，本学习笔记也不必记录完整真实值。

可以只写：

### Workspace ID

```
✅ 已获取
```

以后我们在 Apifox 中把它保存成：

```
WORKSPACE_ID
```

变量即可。

# 二十七、Workspace 准备检查

现在应该满足：

```
✅ API Key 有了
✅ Workspace ID 有了
✅ 两者地域一致
```

如果完成：

继续。

# 二十八、接下来确认模型

我们当前学习路线已经确定：

```
qwen3.8-max
```

官方当前模型调用 ID：

```
qwen3.8-max
```

并且目前支持：

```
OpenAI-compatible Chat Completions
```

调用。

# 二十九、需要给 qwen3.8-max 单独创建 Key 吗？

不需要。

API Key 创建时：

```
不是给某一个具体模型生成一把专属 Key
```

调用模型时通过 Request Body：

```
{
  "model": "qwen3.8-max"
}
```

指定模型。

官方当前首次调用说明也明确说明：

> 创建 API Key 时不需要选择某一个模型，真正调用时通过 `model` 参数指定模型。

# 三十、需要自己部署 qwen3.8-max 吗？

不需要。

我们使用的是：

```
百炼提供的标准模型 API
```

Provider 已经帮我们完成：

```
模型部署
Serving
GPU
负载
```

我们只负责：

```
HTTP Request
```

# 三十一、第一次要不要开联网搜索？

不要。

`qwen3.8-max` 本身当前支持联网搜索等更多能力，但这不是 Node 008 第一次实验的目标。

第一条请求：

```
不开搜索
不开 Tool
不开 Structured Output
不开 Streaming
```

越简单越好。

# 三十二、第一次要不要开强 Reasoning？

也不要。

`qwen3.8-max` 当前 Chat Completions 默认推理强度较高；官方目前支持通过：

```
reasoning_effort
```

控制，并且：

```
none
```

会映射为关闭 Thinking。

所以第一条请求我们会使用：

```
"reasoning_effort": "none"
```

目的：

```
减少额外变量
↓
先看懂最基础调用链
```

# 三十三、不要同时设置 thinking_budget

当前 `qwen3.8-max` 不支持：

```
reasoning_effort
```

和：

```
thinking_budget
```

同时配置。

同时设置会导致请求错误。

所以我们本次：

```
只设置 reasoning_effort
```

# 三十四、平台准备阶段完成

现在应该已经有：

```
百炼服务
✅

地域
✅

API Key
✅

默认业务空间
✅

Workspace ID
✅

模型
qwen3.8-max
```

到这里：

> **阿里平台部分准备完成。**

接下来才正式进入：

```
Apifox
```

# 三十五、准备 Apifox 环境变量

这里我们继续坚持：

```
Secret 不写死
```

建议在 Apifox 中准备两个变量：

```
DASHSCOPE_API_KEY
WORKSPACE_ID
```

其中：

```
DASHSCOPE_API_KEY
```

是真正敏感信息。

```
WORKSPACE_ID
```

是业务空间 ID。

# 三十六、在 Apifox 中创建环境

# 🛑 停下来操作

打开 Apifox。

进入你准备用来测试的项目。

找到：

```
环境
```

或：

```
环境管理 / 环境变量
```

不同版本 UI 可能略有差异。

创建一个用于本学习项目的环境，例如：

```
Agent Learning
```

# 三十七、添加 API Key 变量

添加：

```
变量名：

DASHSCOPE_API_KEY
```

值：

```
刚才复制的真实 API Key
```

如果 Apifox 当前版本支持：

```
Secret / 私密变量
```

优先使用私密形式。

# 三十八、添加 Workspace 变量

再添加：

```
WORKSPACE_ID
```

值：

```
刚才查到的 Workspace ID
```

# 三十九、保存环境

确保当前选中的环境：

```
Agent Learning
```

或者你刚创建的环境。

# 四十、变量准备成功标准

```
✅ {{DASHSCOPE_API_KEY}} 可被 Apifox 解析

✅ {{WORKSPACE_ID}} 可被 Apifox 解析
```

如果 Apifox 有变量预览功能：

可以确认：

```
变量存在
```

但不要截图 / 复制真实 API Key 到仓库。

# 四十一、正式创建第一条模型 API Request

# 🛑 停下来操作

现在在 Apifox 中：

```
新建 HTTP 请求
```

名字可以叫：

```
Qwen - First Chat Completion
```

或者：

```
第一次调用 qwen3.8-max
```

# 四十二、Method

选择：

```
POST
```

# 四十三、URL 怎么填？

根据刚才选择的地域。

目前官方当前推荐的业务空间专属 OpenAI-compatible Chat Completions Endpoint 形式如下：

### 如果使用华北2（北京）

```
https://{{WORKSPACE_ID}}.cn-beijing.maas.aliyuncs.com/compatible-mode/v1/chat/completions
```

### 如果使用新加坡

```
https://{{WORKSPACE_ID}}.ap-southeast-1.maas.aliyuncs.com/compatible-mode/v1/chat/completions
```

其他地域也有对应专属域名。

官方明确要求 `{WorkspaceId}` 替换为真实业务空间 ID，并且不同地域 Base URL 不通用。

# 四十四、只选你自己的地域

不要：

```
北京也试一下
新加坡也试一下
```

现在只使用：

```
刚才创建 Key 的那个地域
```

# 四十五、配置 Authorization Header

# 🛑 停下来操作

进入：

```
Headers
```

添加：

```
Key:

Authorization
```

Value：

```
Bearer {{DASHSCOPE_API_KEY}}
```

注意：

```
Bearer
```

后面有：

```
一个空格
```

完整形式：

```
Bearer + 空格 + API Key
```

# 四十六、配置 Content-Type

添加：

```
Content-Type
```

值：

```
application/json
```

如果 Apifox 在选择 JSON Body 后自动添加：

```
Content-Type
```

也可以检查一下即可。

# 四十七、Header 成功标准

现在至少应该有：

```
Authorization
=
Bearer {{DASHSCOPE_API_KEY}}

Content-Type
=
application/json
```

# 四十八、配置 Body

# 🛑 停下来操作

进入：

```
Body
```

选择：

```
JSON
```

或者：

```
application/json
```

# 四十九、第一次 Request Body

填写：

```
{
  "model": "qwen3.8-max",
  "messages": [
    {
      "role": "user",
      "content": "请用一句话解释 Java 是什么。"
    }
  ],
  "reasoning_effort": "none"
}
```

# 五十、现在不要马上 Send

先自己检查一次。

你现在应该能够指出：

```
model
```

在哪里；

```
messages
```

在哪里；

```
role
```

在哪里；

```
content
```

在哪里；

```
reasoning_effort
```

在哪里。

# 五十一、自己解释一下 Request

在真正 Send 之前，脑子里应该是：

```
POST
↓
发送到百炼 Chat Completions Endpoint

Authorization
↓
证明我有权限

Content-Type
↓
告诉百炼 Body 是 JSON

model
↓
qwen3.8-max

messages
↓
给模型当前看到的消息

reasoning_effort
↓
这次关闭 Thinking
```

如果能解释：

再继续。

# 五十二、第一次 Send

# 🛑 停下来操作

现在：

```
点击 Send / 发送
```

然后：

> **不要急着看模型回答好不好。**

先看：

```
HTTP Status
```

# 五十三、第一目标：HTTP 200

如果看到：

```
200
```

或者：

```
2xx 成功
```

先停一下。

这意味着：

```
网络
URL
Key
请求结构
模型调用
```

这一整条链：

```
至少基本跑通了
```

# 五十四、如果第一次不是 200 怎么办？

不要慌。

第一次调用 API 出错：

```
完全正常
```

这反而是学习的一部分。

# 五十五、如果是 401

优先检查：

```
API Key 是否复制完整

Authorization 是否为：

Bearer {{DASHSCOPE_API_KEY}}

Bearer 后有没有空格

变量是否真正生效

Key 所在地域是否正确
```

# 五十六、如果是 403

检查：

```
业务空间权限

模型权限

地域

账号 / RAM 权限
```

如果你使用：

```
默认业务空间
+
自己的主账号
```

一般会少很多权限问题。

# 五十七、如果是 404

检查：

```
URL
```

尤其：

```
WORKSPACE_ID 是否正确

地域域名是否正确

/chat/completions 是否漏了
```

# 五十八、如果是 400

优先查看 Response 中：

```
错误 message
```

然后检查：

```
JSON 是否合法

model 是否正确

messages 是否正确

reasoning_effort 是否正确
```

# 五十九、如果是 429

通常考虑：

```
Rate Limit
Quota
并发
Token 限制
```

但第一次只发一条请求就遇到 429 的概率通常不高。

# 六十、如果完全连接不上

检查：

```
网络

域名

VPN / 代理

DNS

Workspace 域名
```

先判断：

```
是 HTTP API 返回错误
```

还是：

```
连 HTTP Response 都没拿到
```

这是两个不同问题。

# 六十一、记录第一次结果

如果成功：

### 第一次请求日期

```
2026年9月28日 20:53
```

### HTTP Status

```
200
```

### Provider

```
阿里云百炼 Model Studio
```

### Model

```
qwen3.8-max
```

### Region

```
华北2（北京）
```

# 六十二、现在终于看 Response Body

# 🛑 停下来操作

现在打开：

```
Response Body
```

先不要往下面看答案。

自己在 JSON 中寻找：

```
choices
```

# 六十三、找到 choices[0]

里面应该存在类似：

```
message
```

继续展开。

# 六十四、找到 message.content

寻找：

```
content
```

这才是：

```
模型真正生成给你的主要文本
```

# 六十五、记录第一次模型回答

### Prompt

```
请用一句话解释 Java 是什么。
```

### Response Content

```
【把模型实际回答复制到这里】
```

```
{
    "id": "chatcmpl-655360eb-fb67-9474-ac49-c1db76da20b0",
    "object": "chat.completion",
    "created": 1790599746,
    "model": "qwen3.8-max",
    "choices": [
        {
            "index": 0,
            "message": {
                "role": "assistant",
                "content": "Java 是一种广泛使用的、面向对象的编程语言，其核心特点是“一次编写，到处运行”，可跨平台开发各类应用程序。"
            },
            "finish_reason": "stop",
            "logprobs": null
        }
    ],
    "usage": {
        "prompt_tokens": 19,
        "total_tokens": 46,
        "completion_tokens": 27,
        "prompt_tokens_details": {
            "cached_tokens": 0,
            "text_tokens": 19
        }
    }
}
```

注意：

只复制：

```
模型回答
```

不要复制：

```
API Key
```

等私密信息。

# 六十六、找到 finish_reason

继续在 Response 中寻找：

```
finish_reason
```

记录：

```
"finish_reason": "stop",
```

最普通正常完成通常可能看到：

```
stop
```

但这里：

> **以你自己的真实 Response 为准。**

# 六十七、自己解释 finish_reason

如果：

```
stop
```

你应该能理解：

```
模型正常完成这次生成
```

如果不是：

```
先记录下来
```

不要急着自行猜。

# 六十八、找到 usage

接下来寻找：

```
usage
```

这是今天非常重要的部分。

# 六十九、找到 prompt_tokens

记录：

```
prompt_tokens = 19
```

这就是 Node 004 学过的：

```
Input Token
```

在真实 API 中第一次出现。

# 七十、找到 completion_tokens

记录：

```
completion_tokens = ________
```

它对应：

```
Output Token
```

# 七十一、找到 total_tokens

记录：

```
total_tokens = 46
```

# 七十二、第一次 Token Usage 记录

| 指标              | 实际值 |
| ----------------- | ------ |
| Prompt Tokens     | 19     |
| Completion Tokens | 27     |
| Total Tokens      | 46     |

# 七十三、现在回忆 Node 004

你肉眼看到的输入：

```
请用一句话解释 Java 是什么。
```

字符数量：

```
不等于
```

Response 中：

```
prompt_tokens
```

因为：

```
Character
≠
Token
```

这一次你终于不是：

```
听别人说
```

而是：

```
亲眼看到
```

了。

# 七十四、再回忆 Node 005

这次 Request 中：

```
messages
```

就是模型 Context 的核心组成。

当前非常简单：

```
只有一条 User Message
```

以后：

```
System
History
Tools
RAG
Memory
```

加入以后：

```
prompt_tokens
```

会不断上涨。

这就是：

```
Context
↓
Token
↓
API Usage
```

真正连起来。

# 七十五、检查 reasoning_content

由于这次设置：

```
"reasoning_effort": "none"
```

模型应该关闭 Thinking。

如果 Response 中存在相关：

```
reasoning_content
```

字段：

观察它。

如果没有：

也正常。

当前只要知道：

```
我们故意关闭了复杂 Reasoning
```

即可。

不要在这里深入研究 Reasoning Token。

# 七十六、到这里，第一次模型调用正式成功

如果已经完成：

```
HTTP 200
✅

content
✅

finish_reason
✅

usage
✅
```

那么：

> **你已经完成了人生第一次手动 LLM API 调用。**

# 七十七、但是实验还没有结束

接下来再做：

```
几个非常小的 Token 对比实验
```

目的是把：

```
Node 004
```

真正坐实。

# 七十八、实验 A：更短的 Prompt

# 🛑 停下来操作

把 Body 中：

```
content
```

改成：

```
你好
```

保持其他东西不变。

Send。

# 七十九、记录实验 A

| 指标              | 值   |
| ----------------- | ---- |
| Prompt            | 你好 |
| Prompt Tokens     | 13   |
| Completion Tokens | 14   |
| Total Tokens      | 27   |

# 八十、实验 B：更长的 Prompt

# 🛑 停下来操作

把 Prompt 改成：

```
请从 JVM、Spring Boot、MySQL、Redis 四个方面，简要说明一个 Java 后端工程师日常开发中可能处理哪些问题。
```

Send。

# 八十一、记录实验 B

| 指标              | 值   |
| ----------------- | ---- |
| Prompt Tokens     | 39   |
| Completion Tokens | 1618 |
| Total Tokens      | 1657 |

# 八十二、比较 A 和 B

现在回答：

```
更长的 Prompt
```

是否通常：

```
prompt_tokens 更多？
```

### 我的观察

```
是的
```

# 八十三、实验 C：控制回答长短

# 🛑 停下来操作

Prompt：

```
请只用一句话解释 Java 的主要特点。
```

Send。

记录：

```
completion_tokens = 28
```

# 八十四、实验 D：要求详细回答

# 🛑 停下来操作

Prompt：

```
请详细解释 Java 的主要特点，并分别说明面向对象、跨平台、垃圾回收、并发和生态体系，每一项给出简单例子。
```

Send。

记录：

```
completion_tokens = 2755
```

# 八十五、比较 C 和 D

看看：

```
要求输出更详细
```

是否通常：

```
completion_tokens 更多
```

### 我的观察

```
是的
```

# 八十六、第一次 Token 实验总结

现在应该已经亲眼看到：

```
输入更多
↓
通常 Input Token 更多

输出要求更多
↓
通常 Output Token 更多
```

所以 Node 004 的：

```
Token Cost
```

开始真正变成工程直觉。

# 八十七、暂时不要开始做超长 Context 实验

Node 005 还留着：

```
4K
16K
32K
64K

Lost in the Middle

Context Noise
```

但是今天：

```
不做
```

原因：

```
第一次 API 实验已经有足够多新东西
```

不要贪多。

# 八十八、现在可以做一个可选“错误实验”

这一部分：

```
可选
```

不是必须。

如果第一次调用过程已经让你很累：

```
直接跳过
```

# 八十九、可选实验：故意写错 Model

把：

```
"model": "qwen3.8-max"
```

暂时改成一个明显不存在的名字，例如：

```
"model": "this-model-does-not-exist"
```

然后 Send。

# 九十、观察什么？

不要关心：

```
它是不是某个固定 Status
```

而是观察：

```
HTTP Status

Error Body

Error Code

Error Message
```

你会第一次发现：

> **失败 Response 本身也是结构化信息。**
>
> > {
> >
> > ​    "error": {
> >
> > ​        "message": "Model not exist.",
> >
> > ​        "id": "d0bf6d05-656d-9d0d-bdf5-5aeaa00453ac",
> >
> > ​        "type": "invalid_request_error",
> >
> > ​        "code": "model_not_found"
> >
> > ​    }
> >
> > }

# 九十一、记住之后恢复

做完错误实验：

一定恢复：

```
"model": "qwen3.8-max"
```

然后再次 Send。

确保：

```
重新成功
```

# 九十二、为什么故意制造错误有价值？

因为以后真正开发时：

```
错误一定会发生
```

第一次就培养：

```
看 Status
↓
看 Error Code
↓
看 Error Message
↓
判断错误层级
```

的习惯。

而不是：

```
一报错就把整段错误扔给 AI
```

自己完全不看。

# 九十三、现在把整个实验流程复述一遍

你刚才实际做了：

```
进入百炼
↓
确认地域
↓
创建 API Key
↓
找到默认 Workspace
↓
获取 Workspace ID
↓
进入 Apifox
↓
配置环境变量
↓
创建 POST Request
↓
配置 Endpoint
↓
Authorization
↓
Content-Type
↓
JSON Body
↓
Send
↓
HTTP 200
↓
读取 Response
↓
content
↓
finish_reason
↓
usage
↓
Token 实验
```

这一整套：

就是：

```
第一次真实 LLM API 调用
```

# 九十四、现在你应该能解释为什么“充值”不是调用本身

最开始：

```
充值
```

只解决：

```
付费能力
```

真正调用需要：

```
Provider Account
+
Region
+
Workspace
+
API Key
+
Endpoint
+
Model
+
HTTP Request
```

这是完全不同的一层。

# 九十五、Region、Key、Workspace、Endpoint 再梳理一次

```
Region
↓
决定在哪个地域使用百炼

Workspace
↓
这个地域里的业务空间

Workspace ID
↓
业务空间的唯一标识

API Key
↓
调用凭证

Endpoint
↓
真正发送 HTTP 请求的地址

Model
↓
真正执行 Inference 的模型
```

它们不是一个东西。

# 九十六、最容易出现的错误：地域混用

例如：

```
北京 Key
+
新加坡 Workspace
+
北京 Endpoint
```

这种组合：

```
不要出现
```

形成原则：

```
一个实验
=
固定一个 Region
```

然后：

```
Key
Workspace
Endpoint
```

全跟着它走。

# 九十七、默认业务空间为什么适合我们现在？

因为当前目标只是：

```
第一次调用模型
```

默认业务空间：

```
减少权限配置
减少模型授权问题
减少额外概念
```

等以后做：

```
公司级项目
测试 / 生产隔离
不同团队
成本拆分
```

再学习：

```
子业务空间
```

# 九十八、API Key 为什么不能出现在截图里？

以后你可能：

```
截图问 ChatGPT
截图发群
截图写博客
```

如果页面上显示：

```
真实 API Key
```

就可能泄露。

所以：

```
截图前检查 Secret
```

也是工程习惯。

# 九十九、如果 Key 不小心泄露怎么办？

不要只是：

```
把 Git 文件删掉
```

最安全的第一反应应该是：

```
立即让泄露 Key 失效 / 删除
↓
创建新 Key
```

因为：

```
泄露的 Key
```

已经不能再被认为安全。

然后再处理：

```
Git History
```

等问题。

# 一百、这次实验不需要记模型价格数字

原因：

```
模型价格可能调整
```

我们长期笔记真正应该记录：

```
在哪里看价格
```

而不是：

```
2026 年某一天的价格
```

所以以后使用模型前：

```
模型详情 / 模型列表 / 计费页面
```

查看当前价格即可。

# 一百零一、qwen3.8-max 当前模型信息只需要知道什么？

当前官方模型信息确认：

```
调用模型 ID：

qwen3.8-max
```

并提供 OpenAI-compatible 等模型服务能力。

现在不需要记：

```
参数量
所有地域
所有能力
全部限流
价格
```

# 一百零二、第一次实验为什么不用快照模型？

当前我们只是：

```
验证 API 链路
```

所以直接使用：

```
qwen3.8-max
```

最直观。

以后到了：

```
长期 Java 实验
回归测试
Eval
```

为了实验复现，

再讨论：

```
是否固定快照模型
```

更合适。

# 一百零三、实验结果最终应该保存什么？

建议保留：

```
地域
模型
Prompt
HTTP Status
模型 Response Content
finish_reason
Token Usage
遇到的错误
怎么修复
```

不要保存：

```
API Key
```

# 一百零四、我的第一次完整实验记录

## 日期

```
2026年9月28日21:10
```

## Region

```
华北2（北京）
```

## Workspace

```
默认业务空间
```

## API Key

```
✅ 已创建，不记录真实值
```

## Workspace ID

```
✅ 已获取，不在本笔记记录真实值
```

## Model

```
qwen3.8-max
```

## API

```
OpenAI-compatible Chat Completions
```

## reasoning_effort

```
none
```

## Prompt

```
请用一句话解释 Java 是什么。
```

## HTTP Status

```
200
```

## finish_reason

```
stop
```

## prompt_tokens

```
19
```

## completion_tokens

```
23
```

## total_tokens

```
42
```

## 模型回答

```json
{
    "id": "chatcmpl-46e08e91-ed91-931b-a102-6c1c69afe36e",
    "object": "chat.completion",
    "created": 1790601072,
    "model": "qwen3.8-max",
    "choices": [
        {
            "index": 0,
            "message": {
                "role": "assistant",
                "content": "Java 是一种跨平台、面向对象的通用编程语言，其核心理念是“一次编写，到处运行”。"
            },
            "finish_reason": "stop",
            "logprobs": null
        }
    ],
    "usage": {
        "prompt_tokens": 19,
        "total_tokens": 42,
        "completion_tokens": 23,
        "prompt_tokens_details": {
            "cached_tokens": 0,
            "text_tokens": 19
        }
    }
}
```

# 一百零五、这次遇到的第一个问题

如果没有：

```
无
```

如果有：

### 现象

```
________________________________________________
```

### HTTP Status / Error Code

```
________________________________________________
```

### 原因

```
________________________________________________
```

### 解决方法

```
________________________________________________
```

### 我学到了什么

```
________________________________________________
```

# 一百零六、Token 对比实验记录

| 实验                | Prompt Tokens | Completion Tokens | Total Tokens |
| ------------------- | ------------- | ----------------- | ------------ |
| A：你好             |               |                   |              |
| B：较长 Java Prompt |               |                   |              |
| C：一句话回答       |               |                   |              |
| D：详细回答         |               |                   |              |

# 一百零七、我的 Token 实验结论

```
________________________________________________

________________________________________________

________________________________________________
```

# 一百零八、现在做自测

不要看前面的答案。

## Q1

为什么充值以后还不能直接说：

```
“我已经有 API 了”
```

？

## Q2

Region 是什么？

## Q3

为什么不同地域的 Key 和 Endpoint 不能乱混？

## Q4

什么是默认业务空间？

## Q5

为什么这次不创建子业务空间？

## Q6

API Key 是什么？

## Q7

为什么不能把 API Key 写进这篇 Markdown？

## Q8

Workspace ID 是什么？

## Q9

Workspace ID 和 API Key 是不是同一个东西？

## Q10

Endpoint 是什么？

## Q11

为什么：

```
{WorkspaceId}
```

需要替换？

## Q12

Authorization Header 中为什么要写：

```
Bearer {{DASHSCOPE_API_KEY}}
```

？

## Q13

Content-Type 为什么是：

```
application/json
```

？

## Q14

Body 中：

```
"model": "qwen3.8-max"
```

有什么作用？

## Q15

为什么：

```
messages
```

和 Context 有关系？

## Q16

为什么第一次使用：

```
reasoning_effort = none
```

？

## Q17

HTTP 200 代表什么？

## Q18

HTTP 200 能不能证明模型回答正确？

## Q19

模型真正的回答主要在哪个字段？

## Q20

finish_reason 有什么意义？

## Q21

prompt_tokens 是什么？

## Q22

completion_tokens 是什么？

## Q23

为什么长 Prompt 通常会让 Input Token 增加？

## Q24

为什么要求更详细回答通常会让 Output Token 增加？

# 一百零九、口述验收

你现在应该可以说清楚：

> 我先在百炼选择一个地域，在该地域的默认业务空间创建 API Key，并获得对应 Workspace ID。
>
> 然后在 Apifox 里构造一个 POST 请求，请求发送到该地域、该 Workspace 对应的 OpenAI-compatible Chat Completions Endpoint。
>
> Authorization Header 使用 API Key 完成鉴权，Body 中通过 model 指定 qwen3.8-max，通过 messages 给模型传入当前消息。
>
> 百炼收到 Request 后调用 qwen3.8-max 进行 Inference，然后返回 HTTP Response。
>
> Response 中除了模型回答 content，还能看到 finish_reason 和 usage。
>
> usage 中的 prompt_tokens 和 completion_tokens，就是之前 Token 章节里的 Input Token 和 Output Token 在真实 API 中的体现。

如果能不用看笔记讲清：

```
008-2 的核心理解
已经基本完成。
```

# 一百一十、008-2 完成标准

必须真正完成：

- 成功进入百炼控制台
- 确认自己的 Region
- 创建第一个 API Key
- 使用默认业务空间
- 获取 Workspace ID
- Key / Workspace / Endpoint 地域一致
- 在 Apifox 创建学习环境
- API Key 没有进入 Git
- 创建 POST Chat Completions Request
- Authorization Header 配置正确
- Content-Type 正确
- model = `qwen3.8-max`
- 第一次 Request 使用简单 User Message
- 第一轮关闭复杂 Thinking
- 真正点击 Send
- 成功获得模型 Response
- 找到 content
- 找到 finish_reason
- 找到 usage
- 记录 prompt_tokens
- 记录 completion_tokens
- 记录 total_tokens
- 完成短 / 长 Prompt Token 对比
- 完成短 / 长 Output Token 对比
- 记录真实踩坑

全部完成：

```
008-2
✅ PASS
```

但是：

```
Node 008
```

此时仍然：

```
🟡 学习中
```

因为我们还没有：

```
用 Java 完成同样的事情
```

# 一百一十一、下一步

完成这篇以后：

> **不要直接进入 Node 009。**

下一篇：

```
008-3 - 实验：使用 Java 第一次调用模型 API
```

到那时候才正式创建：

```
Agent/
└── projects/
    └── agent-learning-lab/
```

并第一次让：

```
Codex
```

进入我们的长期学习流程。

下一步的目标非常明确：

```
刚才 Apifox 手动完成的 Request
↓
用 Java 再做一遍
```

然后真正建立：

```
Apifox URL
↔
Java URI

Apifox Header
↔
Java Header

Apifox Body
↔
Java Request Body

Apifox Response
↔
Java Response
```

到那时：

```
“Java 调大模型”
```

就不再神秘。

它只是：

> **把你刚刚手动完成的 HTTP Request，用代码自动完成一次。**