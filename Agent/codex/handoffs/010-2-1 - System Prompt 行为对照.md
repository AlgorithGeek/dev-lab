# 010-2-1：System Prompt 行为对照

2026-10-26 实现并编译；2026-11-02 用户提供 IDEA 真实 A/B 成功结果，见末节。新增本地模拟测试仍未运行。

## 文件与入口

- 新增 `projects/agent-learning-lab/src/main/java/com/avexvertex/agent/SystemPromptDemo.java`。
- 扩展 `projects/agent-learning-lab/check-config.py` 的 System A/B 场景，保留旧检查。
- 新增本轮 Prompt / Handoff，更新 progress.md 为第一阶段待运行。
- 没有修改 FirstModelCall、MessageHistoryDemo 或 pom.xml；没有新依赖。

IDEA Main Class：`com.avexvertex.agent.SystemPromptDemo`，JDK 21，程序实参留空。
复制已有运行配置时，注意改主类并删除旧的 --history。配置 DASHSCOPE_API_KEY 与 DASHSCOPE_BASE_URL，真实值留在本地，不读取或提交 IDEA 私密配置。
正常运行发送两次可能计费的模型请求，无自动重试。

## 先读哪些代码

先看 runCompare 中的 systemPrompt 和 userPrompt，再看两个 JSONArray。
systemPrompt 使用笔记中的五条规则，userPrompt 使用 getUserName 的原始示例，均为 Java 文本块；stripTrailing 去掉文本块末尾换行。

| 调用 | messages | 条数 |
|---|---|---:|
| A（无 System） | user | 1 |
| B（有 System） | system、user | 2 |

两组用同一个 userPrompt 变量；模型、reasoning_effort 和 endpoint 相同。B 没有任何 A 的回答，这不是连续对话。
请求构造旁附有完整 JSON 注释，其中字符串内的反斜杠 n 表示换行。实际请求由 FastJSON2 序列化，控制台打印实际 Messages。

message 构造 role/content 对象；callModel 构造顶层 model/messages/reasoning_effort、发送 POST 并校验响应。
输出标签、Messages JSON、Roles、发送前条数、HTTP Status、Model、Content、Finish Reason、Prompt / Completion / Total Tokens。
请求出错时停止，非 2xx 只输出状态与错误响应；JSON 或必需字段无效时明确失败，不虚构回答、不重试、不打印认证头或私密 URL。

## 验证边界

- `mvn -o -f Agent/projects/agent-learning-lab/pom.xml compile`：BUILD SUCCESS，三个入口均按 release 21 编译。
- `git diff --check`：通过。
- 新增十一种本地模拟场景：正常两次请求、A/B 各自 HTTP 400、非法 JSON、model 无效、choices 为空、content 无效、finish_reason 无效、usage 无效，以及两项 Token 类型/取值错误。
- 检查脚本将核对路径、请求头、完整 System 文本、相同 User、角色与消息数、模型参数、成功输出、失败停止和假密钥不泄漏。
- **这些新增模拟场景尚未运行。**运行 `python3 check-config.py` 被自动审批拒绝，理由是用户未明确授权测试。需获得授权后再执行；不能把“已编写检查”记为“检查通过”。
- 未由 Codex 访问百炼，没有真实模型回复或 Token 数据。010-2 笔记的实验表保持空白。

## 用户运行后的观察

先核对 A 为 [user]，B 为 [system,user]，两组 User 完全相同，B 第一条是完整 System。
再比较真实回答：是否先给结论、最多三个问题、聚焦实际代码、避免虚构上下文，以及 Prompt Tokens 有何不同。
A 也可能符合这些要求，B 也可能未完全遵守；如实记录，不用程序修改回答来制造差异。

当前尚未实现第二阶段 System + History，也未引入框架、Prompt Template 或抽象层。
未标记 Node 010 完成，未提交或推送。下一步先获准执行本地模拟，再由用户在 IDEA 真实运行 A/B。

## 用户反馈：第一阶段真实 A/B 成功（2026-11-02）

用户提供完整控制台输出，A Roles=[user]、1 条消息；B Roles=[system,user]、2 条消息，User 完全相同，B 第一条 System 为笔记原文。
两组模型均 qwen3.8-max，HTTP 200，finish_reason=stop。

| 调用 | Prompt Tokens | Completion Tokens | Total Tokens |
|---|---:|---:|---:|
| A | 40 | 1001 | 1041 |
| B | 138 | 398 | 536 |

A 输出空指针风险、多种修复方案、设计建议及总结表；B 先给结论，再列三项主要问题，更集中，输出较短。
B 仍把条件性风险描述为“在生产环境中极不安全”，措辞超过片段可确定的信息；不能把本样本当作稳定遵守全部规则的证明。
010-2 第十七节已保存完整 Messages 与模型原始回答，第二十二和四十九节已填写实际对照及观察。进度更新为第一阶段完成、第二阶段待开始。
此前“真实调用未执行 / 实验表空白”是此次用户运行前的历史状态；本地模拟仍未运行，没有用正常真实响应替代错误路径验证。
本次仅补记录，未实现第二阶段、未运行测试、未提交推送。
