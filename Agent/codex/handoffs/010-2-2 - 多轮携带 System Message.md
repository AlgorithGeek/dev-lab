# 010-2-2：多轮携带 System Message

2026-11-02，第二阶段代码编译通过，随后用户提供真实两轮成功输出，详见末节；本地模拟仍未运行。

## 文件及运行方式

- SystemPromptDemo.java：main 选择模式，新增 runHistory；复用现有 message、callModel 和错误处理。保留原 A/B 与中文注释，新增两轮 JSON 注释。
- check-config.py：新增七种两轮 System 检查，保留原有单轮、历史和 System A/B 检查。
- 新增本轮 Prompt / Handoff，更新 progress.md 当前状态。未改 FirstModelCall、MessageHistoryDemo 或 pom.xml。

IDEA Main Class 仍为 `com.avexvertex.agent.SystemPromptDemo`，JDK 21，环境变量沿用同一运行配置。

| 程序实参 | 内容 |
|---|---|
| 留空 | 原第一阶段 A/B |
| `--history` | 本次带 System 的两轮对话 |

两种模式不会同时运行，各自正常发送两次可能计费的请求，无自动重试。其他参数明确报错。

## 数据流与观察点

runHistory 中 String systemPrompt 存规则，JSONArray history 只保存成功问答。
每轮先创建 currentUser，再创建 messages，依次加入 System、全部 history、currentUser。
callModel 校验成功并返回实际 content 后，才往 history 追加 currentUser、Assistant(answer)。
System 不存进 history，下一轮重新加入请求首位；失败时主 history 保留上一次成功状态。

| 时刻 | 请求角色 | 请求条数（含 System） | history 发送前 | history 成功后 |
|---|---|---:|---:|---:|
| 第 1 轮 | system、user | 2 | 0 | 2 |
| 第 2 轮 | system、user、assistant、user | 4 | 2 | 4 |

每轮输出 History Count before、带轮次的标签、Messages、Roles、发送前消息条数、HTTP Status、Model、Content、Finish Reason 和三项 Tokens，成功后输出 History Count after。
finally 始终输出最后有效历史 JSON 和条数，第一轮失败为 0，第二轮失败保留 2，正常两轮结束为 4。
不在 Java 中替模型添加 JAVA-REVIEW:；是否遵守前缀、一句话等要求，均根据真实输出观察。

## 验证边界

- `mvn -o compile`：BUILD SUCCESS，三个入口按 Java 21 编译通过。
- `git diff --check`：通过；其他两个 Java 入口和 pom.xml 无改动。
- 检查脚本已补七种场景：两轮正常，以及每轮分别 HTTP 400、非法 JSON、缺失 usage。
- 新增断言覆盖两轮消息组成、完整相同 System、实际首轮响应复用（含引号、换行、反斜杠）、请求条数 2/4、history 0→2→4、失败后保留 0/2 和无后续请求、不修改回复或泄露假密钥。
- **以上新增本地模拟尚未执行。**此前运行测试被自动审批拒绝，理由是缺少明确测试授权，本轮没有重试。原 System A/B 新增模拟场景同样尚未运行。
- 未读取 IDEA 密钥、未由 Codex 访问百炼，没有真实两轮响应或 Token 可记录。010-2 第二阶段实验表保持待填。

## 初次交付时的下一步

先阅读 runHistory 的 systemPrompt / history / messages 三个变量和请求 JSON 注释。
在 IDEA 填 --history 运行，核对两轮 System 均在首位、第二轮包含首轮实际回答、历史最终四条且没有 System，再记录前缀遵守情况和 Token。
尚未做可选的第二轮删除 System 实验；未增加持久化、框架或抽象层，未标记 Node 010 完成，未提交推送。

## 用户补齐第二阶段真实结果（2026-11-02）

用户提供两轮完整响应和最终历史 JSON，均为 qwen3.8-max、HTTP 200、finish_reason=stop。

| 轮次 | Request Roles | 请求条数 | 成功后 History | Prompt Tokens | Completion Tokens | Total Tokens |
|---|---|---:|---:|---:|---:|---:|
| 1 | system、user | 2 | 2 | 70 | 36 | 106 |
| 2 | system、user、assistant、user | 4 | 4 | 134 | 60 | 194 |

两轮相同 System 均处于首位；第二轮 Assistant 消息与第一轮实际 Content 一致。两次回答均以 JAVA-REVIEW: 开头，程序未补前缀。
最终 history 的四条消息只有 user、assistant、user、assistant；System 没有进入普通历史。
用户本段日志未包含首轮 History Count before 行，其值 0 依据代码初始化推得；第二轮 before=2 和两轮 after=2/4 均在日志中明确输出。
010-2 第三十五节已保存原始 Messages、回答、完整四条历史与用量，第四十二和四十九节已同步。此前“等待真实运行”的内容描述此次用户提供输出之前的状态。
新增本地模拟仍未运行，正常调用成功不能替代错误路径验证。第二阶段真实正常调用完成，Node 010 待用户理解收尾确认；本次未改代码、未执行测试、未提交推送。

## 学习收尾与进度维护（2026-11-02）

按用户要求，已核对并补齐 010-2 两阶段真实实验记录、完整回答、输入消息、历史 JSON、Token 表和行为观察；已同步客观检查项。
理解项依据用户确认读完、理解讨论及继续下一节点的要求记录，未另行进行逐题口述。
progress.md 当前节点改为 Node 011「Prompt 基础（准备学习）」，Node 010 标注学习与两阶段正常实测完成。
新增本地模拟仍未执行，尤其错误路径未验证，继续作为独立待办保留；可选第二轮移除 System 未做。本次未改执行代码、未运行测试、未提交推送。
