# 011-2-1：模糊 Prompt 与结构化 Prompt 对照

2026-11-02，用户读入 011-2，要求详细讲清第一步。解释同一代码 Input、A/B 均为单条 User、B 增加任务背景和约束后，用户授权“ok，开始吧”。

- 仅实现第一阶段，新增同包 PromptBasicsDemo，保留现有三个独立入口和依赖。
- 沿用 Java 21、JDK HttpClient、FastJSON2、现有环境变量及 qwen3.8-max / reasoning_effort=none。
- 固定 CounterService 代码为共用 input，A 为模糊任务 + input，B 按笔记原文的 Role/Task/Context/Input/Constraints/Output Format 组织。
- 两次请求均只有一条 user，无 System、历史、Few-shot；相同模型、参数、endpoint，A 回复不进入 B。
- 加中文注释、完整请求 JSON 注释，打印消息、角色、条数、Prompt 长度、状态、回答、结束原因和 Token。
- 保留配置及响应边界校验，无自动重试；编译并补本地检查脚本、协作记录。
- 既有本地测试运行曾被自动审批以缺少明确测试授权为由拒绝，本轮不重试测试。真实模型调用由用户在 IDEA 完成，不填造实验数据。
- 不实现第二阶段 C / --context、不提前进入 Node 012，不标记 Node 011 完成，不提交推送。
