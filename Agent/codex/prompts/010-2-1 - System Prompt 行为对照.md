# 010-2-1：System Prompt 行为对照

2026-10-26，用户放入 010-2 实验笔记，要求先读取并说明阶段安排，随后授权“你来吧”。本轮仅实现该笔记第一阶段。

- 新增同包 SystemPromptDemo，保留 FirstModelCall 与 MessageHistoryDemo。
- 沿用 Java 21、JDK HttpClient、FastJSON2、环境变量和 qwen3.8-max / reasoning_effort=none，不增加依赖。
- 采用笔记第七节 System Prompt 与第八节 Java 空指针示例，A/B 的 User 字符串、模型、参数和地址完全相同。
- A 只包含 user，B 为 system、user；A 的回复不放进 B。
- 在请求构造旁保留完整 JSON 注释，输出标签、消息、角色、条数、状态、Model、Content、Finish Reason 和三项 Tokens。
- 沿用配置校验、非 2xx 不解析成功字段、JSON 和必要字段校验，无自动重试。
- 编译并扩展本地模拟脚本，保存本轮交接；真实调用由用户在 IDEA 运行，禁止把模拟结果写成真实结果。
- 完成第一阶段后停止，不实现多轮 System，不进入 Node 011，不标记 Node 010 完成，不提交推送。

执行限制：自动审批拒绝运行本地模拟检查，理由是当前未获得用户明确的测试授权。因此本轮仅记录编译通过，新增模拟场景尚未运行，等待用户批准后执行。
