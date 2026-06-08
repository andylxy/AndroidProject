# Agent Assets Boundary

本目录只放 Agent 类模板，不放 ToolSet 模板。

允许放在这里的模板：

1. `AgentBase` 派生模板
2. `AgentScaffoldBase` 派生模板
3. Function Calling Agent 入口模板
4. 依赖注入、`Program.cs` 切换、会话入口相关模板

不要放在这里的模板：

1. `IAgentToolSet` 模板
2. ToolSet 稳定键引用模板
3. 工具方法签名和 `AIFunctionFactory.Create(...)` 为核心的 ToolSet 模板

如果你需要新增 ToolSet，请跳转到 `../agent-tools-integration-guide/assets/` 对应模板目录。

## 可复制模板

规则：如果你需要的是 ToolSet 模板，而不是 Agent 入口模板，不要在本目录重复找镜像模板，直接跳转到 `../agent-tools-integration-guide/`。

1. 统一 Agent 主模板：`function-calling-agent-template.cs.txt`

## 使用方式

1. 如果你只想先打通最小链路，保留模板默认的显式 ToolSet 组合、最小 messages 和默认摘要输出。
2. 如果你要做业务 Agent，在同一份模板里启用 `ConfigureAgentCapabilities(...)`、摘要输出覆写、历史输出覆写和更长的消息脚本。
3. 如果你不想从模板起步，也可以直接参考真实案例：
4. `vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/TravelPlannerAgentDemo.cs`
5. `vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/FunctionCallingPresetAgentDemo.cs`
6. `vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/FunctionCallingCustomSelectionAgentDemo.cs`
