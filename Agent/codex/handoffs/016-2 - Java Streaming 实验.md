# 016-2：Java Streaming 实验

2026-11-03，StreamingDemo 编译通过，用户随后提供默认模式真实成功输出，详见末节；新增本地模拟仍未执行。

## 文件和运行

- 新增 StreamingDemo.java，保留现有六个源码文件和 pom.xml，不增加依赖。
- check-config.py 增加可配置模拟 Content-Type 和九种 Streaming 场景，保留原场景。
- 保存本轮任务说明与交接，更新理论阅读和当前实验进度；没有填写真实响应、耗时或 Token。

IDEA Main Class：`com.avexvertex.agent.StreamingDemo`，JDK 21；将 DASHSCOPE_API_KEY / DASHSCOPE_BASE_URL 配在本运行配置中。
复制原配置时需改主类、清除 --errors / --context 等旧参数，不读取或提交私密配置。

| 程序实参 | 内容 |
|---|---|
| 留空 | 实时显示正文，结束后打印拼接正文及指标 |
| `--debug` | 同样只发一次请求，额外打印每个 SSE payload（含 DONE） |

每次正常运行发送一次可能计费的真实请求，不自动重试。运行不同模式是另一次请求，不复用之前结果。

## 建议读代码的顺序

1. main 的请求体：单条 user，增加 stream=true 和 include_usage=true；请求 JSON 注释完整展示。
2. ofInputStream 与 BufferedReader：不等整个 Body 收齐才开始处理。
3. readStream 的 eventData：收集 data 行，空行完成事件，再处理 payload；支持一个事件多条 data 行，忽略注释及 event/id 字段。
4. 先判断 DONE，普通事件才交给 FastJSON2；Usage 可独立于 choices，空 choices 不取第 0 项。
5. 读取 delta.content，非空字符串立即打印并 flush，同时追加 fullContent；空格正文也保留。
6. finish_reason 只记录，不能立即停读，否则可能漏 Usage / DONE；流 EOF 没有 DONE 会明确报片段/失败。
7. 最后打印收到的 Model、首个非空正文、TTFT（正文延迟代理指标）、Total Time、结束原因、三项 Token 与 DONE 状态。

## 实现边界

这是当前 Provider 文本 SSE 的最小客户端，不是完整浏览器 EventSource 实现。没有自动重连、缓存会话、多线程、前端或框架。
JSON 与结束标记属于不同层；角色块不必有正文，content 缺失/空字符串可跳过，但错误类型会拒绝。正文片段不作为单独 Message 写历史。
收到 DONE 但缺正文、Model、finish_reason 或 Usage 时也不记为实验完成，不造出缺失的指标。
finish_reason 如为 length，显示原值；拼接的是已收到的正文，不据此宣称回答未截断。
`HttpRequest.timeout` 不能当作后续读 InputStream 的总超时。本例未单独实现读流空闲超时，若长时间无数据可在 IDEA 停止；需要生产接入时再补独立超时和取消机制，代码中已注记。

## 验证证据

- `mvn -o compile`：BUILD SUCCESS，七个源码文件按 release 21 编译。
- `git diff --check`：通过。既有 Java 代码、pom.xml 和 roadmap 未改。
- 本地模拟已编写九种场景：默认/Debug 正常流、400 普通错误体、无 DONE 的提前 EOF、缺 Usage、非法 chunk JSON、content 类型错误、负数 Token、错误 Content-Type。
- 场景涵盖空角色正文、空格正文、多 data 行事件、UTF-8 中文、finish 后仍读独立 Usage、最终拼接、模式输出和指标不伪造。
- **以上新增模拟尚未运行。**此前完整 HTTPS 检查曾因缺少明确测试授权被自动审批拒绝，本轮没有重试或把“写了断言”记为“已通过”。
- 未由 Codex 访问百炼，没有真实 SSE、回答、TTFT、总耗时或 Token 可填写。模拟按完整 Body 提供事件，只用于校验解析与拼接，不用于证明网络时序或性能。
- 参数结构已核对百炼官方流式输出文档：https://help.aliyun.com/zh/model-studio/stream 。

## 初次交付时的下一步

先阅读请求 JSON 和 readStream 中“事件 → payload → delta → print/append”的路径，再由用户在 IDEA 运行默认模式观察正文。
必要时再运行 --debug，保留首块、正文块、finish、Usage、DONE 的代表性样本，按实际结果补笔记。
本次停在代码与编译阶段，不提前标记 Node 016 完成，未提交推送。

## 用户提供默认模式真实结果（2026-11-03）

HTTP 200，Model=qwen3.8-max，首个非空正文为 "Java"，首段延迟 657.241167 ms，总耗时 3899.898959 ms，finish_reason=stop，Token 为 30/138/168，DONE received=true。
实时正文部分和最终拼接部分文本一致，已在 016-2 第五十二节保存请求、正文和指标；不重复保存整份启动命令。
用户未提供 Debug 原始事件，不能伪造首块/Usage 块，也不能仅凭最终粘贴日志推断屏幕上的逐段出现过程。第五十三节明确保留未记录状态。
此前“真实调用未执行”的段落属于初次交付状态。本次实际正常流已确认，新本地模拟仍未跑；未改代码、未运行测试或提交推送。

## 用户提供 Debug 原始事件（2026-11-03）

HTTP 200，首个非空正文 "Java"，正文延迟 786.463 ms，总耗时 4358.231667 ms，stop，Token 为 30/166/196，DONE=true。
已保存角色空正文、首个普通正文、finish_reason、choices=[] 的 Usage、DONE 五类代表性事件。delta.content 为新增片段，逐块拼接与用户 Full Content 一致，未重发累计全文。
Debug 中 JSON 与紧随其后的正文是同一片段的两种展示，不是两次模型生成。完整回答保留在笔记第五十三节，默认模式的旧数据未覆盖。
本次仅解析用户提供的日志并维护记录，没有新增模型调用或执行本地模拟，未改代码或提交推送。

## 学习范围收尾（2026-11-03）

已完成理论阅读、独立 StreamingDemo 编译、默认/Debug 两次真实调用记录与 SSE/增量/指标的讲解，当前进度转为 Node 017「模型参数（准备学习）」。
没有追加新流式功能或模型请求，未另行逐题口述或测量控制台 GUI 刷新时序。新增本地模拟、独立空闲超时、生产容错和压测仍不在已验证范围。本次未改执行代码或提交推送。
