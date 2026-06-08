# ToolSet Assets Boundary

本目录只放 ToolSet 模板，不放 Agent 入口模板。

允许放在这里的模板：

1. `IAgentToolSet` 模板
2. Utility ToolSet 模板
3. Business ToolSet 模板
4. 工具注册名、工具方法、`AIFunctionFactory.Create(...)` 相关模板

不要放在这里的模板：

1. `AgentBase` 派生模板
2. `AgentScaffoldBase` 派生模板
3. Function Calling Agent 入口模板
4. 依赖注入、`Program.cs` 切换、会话入口相关模板

如果你需要新增 Agent，请跳转到 `../agent-di-template-guide/assets/` 对应模板目录。

## 可复制模板

规则：如果你需要的是 Agent 入口模板，而不是 ToolSet 模板，不要在本目录重复找镜像模板，直接跳转到 `../agent-di-template-guide/`。

1. 统一 ToolSet 主模板：`toolset-template.cs.txt`

## 使用方式

1. 默认先保留一个方法，先打通注册、选择和 Function Calling 最小链路。
2. 如果是正式业务 ToolSet，在同一份模板里继续追加第二个、第三个方法块。
3. 能力域不再靠独立分类字段区分，而是直接通过各 ToolSet 类内 `ToolSetKey` 的 `Utility.*` 或 `Business.*` 前缀表达。
