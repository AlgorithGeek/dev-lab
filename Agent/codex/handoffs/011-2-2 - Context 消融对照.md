# 011-2-2：Context 消融对照

2026-11-02，代码编译通过，用户随后提供真实 B/C 输出，详见末节；新增本地模拟仍未执行。

## 文件和运行方式

- PromptBasicsDemo.java：识别 --context，复用 input、structuredPrompt，新增 runContextCompare 构造 C、发送 B/C，保留原 A/B 及注释。
- check-config.py：保留默认 A/B 十一种场景，同一组检查再覆盖 --context 的十一种场景。
- 新增本轮 Prompt / Handoff，更新进度和笔记当前状态，不填真实实验表。
- 前三个 Java 入口、pom.xml 未修改，没有新依赖。

IDEA Main Class 仍为 `com.avexvertex.agent.PromptBasicsDemo`，JDK 21，环境变量保留同一运行配置的 DASHSCOPE_API_KEY 与 DASHSCOPE_BASE_URL。

| 程序实参 | 执行内容 |
|---|---|
| 留空 | A：模糊 Prompt → B：结构化 Prompt |
| `--context` | B：有 Context → C：无 Context |

两种模式正常各发两次可能计费的请求，--context 不会请求 A，不带上任何上次运行的回答，无自动重试。其他参数明确失败。

## 读代码与对照边界

先读 runCompare 中共用的 input、structuredPrompt，再看 contextMode 分支，最后看 runContextCompare 的 withoutContextPrompt。
B 直接传入第一阶段的 structuredPrompt，保证内容不变。C 的 Role / Task / Input / Output Format 相同，代码也是同一个 input；删除 Spring Boot 单例、多请求线程的 Context，并按手册调整约束为仅基于代码、信息不足时说明、不得自行假设生命周期或并发场景。
二者各构造独立 JSONArray，只放一条 user；B 的回答不进入 C。
保留 A/B JSON 注释，新增 C 的完整 JSON 注释；Role 等段标题仍是 content 中的文本，不是新的 JSON 字段或 System。

这是手册定义的“背景 + 不自行假设约束”联合对照，并非严格只删除 Context 的单变量实验。
观察 B 是否根据已知并发背景下结论、C 是否主动保留“如果实例被多线程共享”的条件；C 即使没有明确 Context 也可能指出 ++count 非原子，不能只凭提到并发就判失败。
记录真实错误和无依据推断，不预设 B/C 哪组一定正确。

## 验证边界

- `mvn -o -f Agent/projects/agent-learning-lab/pom.xml compile`：BUILD SUCCESS，四个 Java 入口按 release 21 编译通过。
- `git diff --check`：通过，原三个入口及 pom.xml 无改动。
- 本地脚本新增 --context 十一种场景：正常 B/C、两组各自 HTTP 400、非法 JSON、model/choices/content/finish_reason/usage 无效、两项 Token 无效；原默认 A/B 十一种仍保留。
- 断言检查两边只有 [user]、模型参数/路径/认证一致、完整 Prompt 符合预期、Input 逐字一致、B 文本不变、C 缺失 Context 且有不自行假设约束、只发 B/C 而不发 A、失败停止及假密钥不泄漏。
- **新增本地检查尚未运行。**此前自动审批拒绝未获得明确授权的测试执行，本轮未重试。编译通过不等于模拟测试通过。
- 没有读取 IDEA 私密配置或由 Codex 访问百炼，本轮没有真实 B/C 回答或 Token 可填写。

## 初次交付时的下一步

用户在 IDEA 填 --context 运行，提供本次 B/C 的完整输出。第二次运行 B 的回答不保证与第一阶段完全相同，按本次结果比较，不能沿用旧 B 的数字冒充新调用。
记录两组回答、判断中是否保留并发条件、有无假设 Spring 单例、实际 Token。暂不标记 Node 011 完成，不进入 Few-shot 或新增框架。
本轮未提交推送；第二阶段实际记录仍待补齐。

## 用户反馈：真实 B/C 成功（2026-11-02）

用户提供 --context 完整输出，双方均 qwen3.8-max、HTTP 200、finish_reason=stop、Roles=[user]、消息数 1。Input 相同，B 复用原 Prompt，C 没有运行背景且要求不得自行假设。

| 调用 | User Prompt 字符数 | Prompt Tokens | Completion Tokens | Total Tokens |
|---|---:|---:|---:|---:|
| B（本次新调用） | 396 | 177 | 634 | 811 |
| C | 315 | 147 | 396 | 543 |

B 使用单例与 HTTP 并发背景；C 在第三项明确无法确定单例/共享场景，没有把 Spring 单例当作已知事实，但开头仍偏肯定。
B 第三项扩展了未提供的全局 ID / 主键用途，把业务完成顺序与值的唯一性混淆；原文完整保留并另记分析。
011-2 第三十二节已补齐本次两组的 Messages、完整回答、用量及观察，第一阶段旧 B 结果未覆盖。背景与约束均改变，不作单变量因果结论。
此前“等待实际输出”的段落为此次用户运行前状态；新增本地模拟仍未执行，正常调用不等于错误路径验证。Node 011 两阶段正常实测完成，待用户理解收尾确认。本次未改代码、未运行测试或提交推送。

## 阅读讲解收尾（2026-11-02）

已结合本次真实输出讲解三组 Prompt 的信息差别、Context 与 Input、约束和输出格式的作用，以及模型即使格式规整也会有技术错误。
progress.md 已将 Node 011 标注为阅读讲解及两阶段正常实测完成，当前准备进入 Node 012「Few-shot」。笔记的实际结果已填齐，客观检查项已同步，理解自测保留为本人回顾，未另行逐题口述。
新增本地模拟仍未执行，不声明错误路径已验证。本次仅维护记录，未修改执行代码、未运行测试、未提交推送。
