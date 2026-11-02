# 010-2-2：多轮携带 System Message

2026-11-02，第一阶段真实 A/B 已跑通并记录。用户了解第二阶段的用途后询问下一步，继续按 010-2 第二阶段实现最小两轮演示。

- 保留 SystemPromptDemo 无参数 A/B，增加 --history 模式，不改变其他实验入口或依赖。
- 使用笔记原文的 System Prompt（要求 JAVA-REVIEW: 前缀）和 ThreadLocal / 线程池两轮问题。
- systemPrompt 单独保存规则，history 只存成功的 User / Assistant。
- 每次请求临时组装 System + history + 当前 User；成功后才追加本轮 User 和实际 Assistant。
- 请求角色依次为 [system,user]、[system,user,assistant,user]；成功历史为 2、4 条，不包含 System。
- 保留响应校验和失败停止，不自动加前缀或修改模型回答来伪造规则遵守。
- 附简短中文注释及两次完整请求 JSON 注释；动态回答注明占位性质。
- 编译，补齐本地检查、Prompt / Handoff，停止在本阶段，等待用户 IDEA 真实结果。
- 本地测试此前被自动审批以“缺少明确测试授权”为由拒绝，本轮只扩展检查脚本，没有重试执行测试。
- 不进入 Node 011、不标记 Node 010 完成、不提交推送。
