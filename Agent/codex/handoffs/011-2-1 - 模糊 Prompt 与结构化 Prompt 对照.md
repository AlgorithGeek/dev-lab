# 011-2-1：模糊 Prompt 与结构化 Prompt 对照

2026-11-02，四个入口编译通过，用户随后提供真实 A/B 成功输出，详见末节；新增本地模拟仍未执行。

## 改动与运行

- 新增 `projects/agent-learning-lab/src/main/java/com/avexvertex/agent/PromptBasicsDemo.java`。
- 扩展 `projects/agent-learning-lab/check-config.py`，新增 Prompt A/B 的十一种模拟场景。
- 新增本轮 Prompt / Handoff，更新 progress.md。
- 未修改 FirstModelCall、MessageHistoryDemo、SystemPromptDemo 或 pom.xml，没有新依赖。

IDEA Main Class：`com.avexvertex.agent.PromptBasicsDemo`，JDK 21，程序实参留空。
复制旧运行配置时，改主类，并删除 --history 等参数；本轮还没有 --context。
在此运行配置里设置 DASHSCOPE_API_KEY 与 DASHSCOPE_BASE_URL，真实值留在本地，不读取或提交 IDEA 私密配置。
正常运行会发送两次可能计费的请求，无自动重试。

## 建议读代码的顺序

先读 runCompare，不必重读已经学过的 HTTP 部分：

1. input：只定义一次 CounterService 代码，两个 Prompt 复用它；这是字符串中的审查材料，不在本程序中作为服务运行。
2. vaguePrompt：宽泛的任务说明加 input。
3. structuredPrompt：Role / Task / Context / Input / Constraints / Output Format 分段；formatted(input) 将共用代码替换到 %s 位置。
4. 两组 JSONArray：各加一条 user，再分别调用 callModel。A 回答不放进 B。

| 条件 | A | B |
|---|---|---|
| Roles / 条数 | user / 1 | user / 1 |
| Java Input | 同一份 CounterService | 同一份 CounterService |
| Model / reasoning_effort | qwen3.8-max / none | 相同 |
| 背景与任务要求 | 宽泛要求，没有明确背景 | 单例 Service + 多 HTTP 线程背景，明确任务、限制和输出要求 |

Role 等标题只是 User content 内的文字，不是 JSON role 或新字段；本次两组都不加 System。
这是组织方式和信息完整度的综合对照，不能把回答差异全归因于排版。
结构化 Prompt 不等于 Structured Output，本轮不要求 JSON 回答、不传示例答案。

## 输出和错误处理

输出实验标签、实际 Messages JSON、Roles、发送前消息条数、User Prompt 字符数、HTTP Status、Model、Content、Finish Reason、三项 Tokens。
JSON 注释展示请求形状，实际 JSON 由 FastJSON2 序列化，包含换行与转义。
沿用现有地址/配置校验、非 2xx 不解析成功字段、非法 JSON 或缺失/无效必需字段即失败，无自动重试、无伪造回答。
异常交给 main 统一提示，A 失败不继续 B，不打印 Key、Authorization 或完整私密 URL。

## 验证边界

- `mvn -o -f Agent/projects/agent-learning-lab/pom.xml compile`：BUILD SUCCESS，四个源码入口按 release 21 编译。
- `git diff --check`：通过；原三个 Java 入口及 pom.xml 没有改动。
- 本地脚本新增十一种场景：正常 A/B，A/B 各自 HTTP 400，非法 JSON，model/choices/content/finish_reason/usage 无效，以及两项 Token 类型/取值无效。
- 断言核对路径、请求头、两组均单条 user、模型参数、完整 Prompt、逐字相同的代码 Input、B 各段内容、长度和真实响应输出，以及失败停止、非 2xx 不走成功解析、假密钥不泄漏。
- **这些新增场景尚未运行。**此前自动审批拒绝了未获明确授权的测试执行，本轮没有重试；不将“有检查代码”当成“检查通过”。
- 未由 Codex 访问百炼，无真实回答或 Token 用量可填写，011-2 实验记录仍待真实运行后补齐。

## 初次交付时的下一步与停止点

用户先在 IDEA 运行 PromptBasicsDemo，核对两边 Roles 都为 [user]，Input 相同，B 含明确 Context。
再观察模型是否根据单例并发背景讨论 ++count 的非原子性、是否遵守最多三项与结论/问题/原因/建议结构、有无无依据扩展，以及 Token 差异。
不预设 A 一定差或 B 一定好，保留完整原始回答。
本轮未做 Context 消融 C、Few-shot、多轮、Prompt Template 或新框架，Node 011 仍在学习中，未提交或推送。

## 用户反馈：第一阶段真实 A/B 成功（2026-11-02）

用户提供 PromptBasicsDemo 完整实际输出，两边均为 qwen3.8-max、HTTP 200、finish_reason=stop、Roles=[user]、消息数 1；共用相同 CounterService 代码，无 System 或历史。

| 调用 | User Prompt 字符数 | Prompt Tokens | Completion Tokens | Total Tokens |
|---|---:|---:|---:|---:|
| A | 135 | 52 | 730 | 782 |
| B | 396 | 177 | 497 | 674 |

A 讨论竞态与可见性，展开三种修复方案及比较；B 利用单例 Service / HTTP 请求线程背景，先给结论，列三项并按问题/原因/建议组织。
B 的第三项把原始 int count 与改用 AtomicInteger 后的引用混淆，不应视作原代码确定缺陷；A 的“性能最好”也没有基准支撑。两边原文均保留，不为实验效果修改模型回复。
011-2 第十九节已保存完整 Messages 和 Content，第二十四、二十五节补齐数字和观察；进度同步为第一阶段完成、Context 消融待开始。
先前“真实结果未验证”的描述是此次用户运行前的状态。本地模拟仍未执行，正常真实响应不能替代错误路径验证。本次未改代码、未测试或提交推送，也未提前实现第二阶段。
