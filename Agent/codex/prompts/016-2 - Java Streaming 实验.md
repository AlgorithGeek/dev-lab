# 016-2：Java Streaming 实验

2026-11-03，用户新增 016-1 / 016-2，确认 016-1 已读完，要求实现实验。按既有分步学习方式，新增独立 StreamingDemo，保留原 Demo 和依赖。

- Java 21、JDK HttpClient、FastJSON2、qwen3.8-max、现有环境变量，reasoning_effort=none。
- Prompt 使用文档原文，stream=true、stream_options.include_usage=true，不带 Schema 或 DTO。
- ofInputStream 取得响应流，检查 HTTP 状态和 Content-Type，UTF-8 逐行读取 SSE。
- 按事件空行边界合并 data 行，先识别 [DONE]，再解析 JSON，分别处理角色/正文/结束/Usage。
- 默认实时 print + flush 并 append，--debug 额外展示实际 payload；统计首个非空正文延迟与收到 DONE 的总耗时。
- 校验字段类型，缺少结束标记或必要汇总字段时明确失败，不把截断结果报作完整成功。
- 写中文注释、请求 JSON 与 SSE 示例，编译，补本地检查脚本及记录。
- 本轮没有发起真实模型调用。既有完整 HTTPS 检查脚本的测试授权未补充，本轮只扩展场景，未执行脚本。
- 不引入框架、多线程、自动重试、重连、Streaming Structured Output 或 Tool Calling；不标记 Node 016 完成，不提交推送。
