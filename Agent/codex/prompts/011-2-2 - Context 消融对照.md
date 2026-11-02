# 011-2-2：Context 消融对照

2026-11-02，第一阶段真实 A/B 已完成并记录。说明 B 保留背景、C 去掉背景并限制自行假设后，用户要求“你做吧”。

- 只在 PromptBasicsDemo 增加 --context，不新增类，默认保留原 A/B。
- --context 只跑 B（有 Context）和 C（无 Context），各一次，不重复运行 A。
- B 复用第一阶段完整 structuredPrompt，C 复用相同 Input、Role、Task 和 Output Format，按手册原文去除 Context 并调整两条约束。
- 两组均为单条 user，模型、参数和 endpoint 相同，无 System、历史或 Few-shot。
- 明确这是背景与不自行假设约束的联合观察，不把差异全归因于单独删除 Context。
- 沿用已建立的请求、响应校验和输出，补中文说明和完整 JSON 注释。
- 编译并扩展检查脚本，保存本轮记录，等待用户 IDEA 真实运行，不填写假数据。
- 本地测试此前被自动审批以缺少明确授权拒绝，本轮没有重试执行；新增检查仅编写、未运行。
- 不进入 Node 012，不标记 Node 011 完成，不提交推送。
