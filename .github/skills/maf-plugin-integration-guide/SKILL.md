---
name: maf-plugin-integration-guide
description: "Use when creating, updating, reviewing, or debugging plugin-based tool integration in vol.api.sqlsugar/SimpleEasy.MAF.AI. 适用于 IAgentPlugin、AgentPluginBase、AgentPluginRegistry、AgentCapabilitySelection 插件选择、插件到 ToolSet 映射、PluginWorkAssistantDemo、Plugins/README 运行流程与时序图。关键词：MAF 插件、IAgentPlugin、AgentPluginBase、AgentPluginRegistry、AgentCapabilitySelection、IncludePlugin、UsePlugin、插件技能、插件接入、按插件类型选择。"
argument-hint: "描述你要新增或修改的插件、插件选择主链路、插件到 ToolSet 的映射关系、演示入口或 README 流程文档"
user-invocable: true
---

# MAF Plugin Integration Guide

## 这个 Skill 解决什么问题

这个 Skill 用于约束 `vol.api.sqlsugar/SimpleEasy.MAF.AI` 中插件能力的设计、落位和接入，重点覆盖：

1. 新增 `IAgentPlugin` 插件契约或插件实现类。
2. 修改 `AgentPluginBase`、`AgentPluginRegistry` 这类插件基础设施。
3. 把“按插件选择”接入 `AgentCapabilitySelection`、`AgentBuildOptions` 和 `AgentCapabilitySelector` 主链路。
4. 判断代码应该落在 `Plugins/`、`Tools/`、`Runtime/Agent/` 还是 `Conhost/Demos/`。
5. 更新插件运行流程、时序图和示例入口文档。

## 项目现有风格摘要

1. `Tools/` 负责真实工具集合的定义、命名、分类、选择和产出。
2. `Plugins/` 负责插件契约、插件发现、批量插件诊断信息和插件到 ToolSet 的映射。
3. 插件不是第二套独立 Agent 构建链路，也不是第二种工具执行单元。
4. 插件进入的是 `AgentCapabilitySelection` 的声明层，最终仍由 `AgentCapabilitySelector` 展开为 ToolSet。
5. `ManagedAgentFactory` 继续只消费最终工具列表，不直接依赖插件内部实现。
6. 演示入口类应优先通过 `ConfigureAgentCapabilities(...)` 或 `AgentBuildOptions.UsePlugin<TPlugin>()` 声明插件来源，而不是手工拼接 `agent.Tools`。
7. 注释风格偏向解释“为什么插件只保留组合职责”，而不是只描述代码表面行为。
8. 插件诊断统一通过 `AgentPluginRegistry.GetPluginInfos()` 输出，不额外为单个插件暴露平行查询接口。
9. 插件稳定键直接收口在插件类自身，不再维护中心常量表。
10. 代码侧插件选择统一按插件类型进行，不保留字符串兼容主链路。

## 关键边界

### Plugin 是什么

Plugin 是能力包描述符，只负责：

1. `PluginName`
2. `PluginDescription`
3. `GetToolSetNames()`

Plugin 不负责：

1. `Category`
2. `CreateTools()`
3. 独立运行时工具生成

### ToolSet 是什么

ToolSet 是唯一执行单元，负责：

1. `Name`
2. `Category`
3. `CreateTools()`

### Selector 怎么处理插件

`AgentCapabilitySelector` 负责把：

1. `Preset`
2. `Category`
3. `Plugin`
4. `ToolSet`

统一解析成最终 ToolSet 名称，再交给 `AgentToolRegistry` 产出工具。

## 标准处理流程

1. 先判断修改目标属于插件契约层、插件解析层、Agent 主链路，还是宿主演示层。
2. 如果是“定义一个能力模块”，优先新增插件，不要直接把业务语义塞进 CapabilitySelection 或 Demo。
3. 如果是“新增真实工具方法”，优先落到 `Tools/ToolSets`，不要直接写进插件类。
4. 如果是“让 Agent 使用插件能力”，优先通过 `ConfigureAgentCapabilities(...)` 或 `UsePlugin<TPlugin>()` 声明插件来源。
5. 如果是“解析插件”，优先修改 `AgentCapabilitySelector`，不要让 `ManagedAgentFactory` 直接依赖插件注册表。
6. 如果是“输出插件诊断信息”，优先放在插件注册表或 Demo，并通过 ToolSet 元数据反推分类与工具统计。
7. 如果改动了插件运行链路，同步更新 `Plugins/README.md` 中的流程说明和时序图。

## 具体约束

1. 不要把插件类写成真实工具方法容器；真实工具仍应落在 ToolSet 中。
2. 不要在插件层重新声明 `Category`。
3. 不要让 `ManagedAgentFactory` 直接依赖 `AgentPluginRegistry`。
4. 不要新增 `PluginPreset` 一类平行体系，优先复用 `AgentCapabilitySelection`。
5. 不要把插件重新放回 `Tools/` 或 `Scaffolds/` 目录。
6. 不要把插件诊断信息职责塞进 `AgentCapabilitySelector` 或 `ManagedAgentFactory`。
7. 不要在 README 中继续把插件描述成可直接创建工具的运行时实体。
8. 不要为单个插件额外增加 `GetPluginInfo(...)` 一类冗余接口，除非用户明确要求新的查询语义。
9. 不要新增 `AgentPluginNames` 一类中心命名表。
10. 不要保留字符串形式的 `IncludePlugin(string)` 或 `UsePlugin(string)` 兼容入口。

## 新增插件的最短步骤

1. 在 `Plugins/Definitions/Utility` 或 `Plugins/Definitions/Business` 下新增插件类。
2. 继承 `AgentPluginBase`。
3. 在插件类自身声明稳定键，并覆写 `PluginName`、`PluginDescription`、`GetToolSetNames()`。
4. 通过 `NormalizeToolSetNames(...)` 返回规范化后的 ToolSet 名称。
5. 保持插件只声明映射到哪些 ToolSet，不直接实现工具方法，也不重新声明 `Category`。
6. 通过 DI 自动发现后，在 Demo 或 Agent 中用 `IncludePlugin<TPlugin>()` 或 `UsePlugin<TPlugin>()` 验证。
7. 如果需要展示诊断信息，优先复用 `GetPluginInfos()` 的批量输出，而不是给插件类或注册表增加单插件专用视图方法。

## 让 Agent 使用插件的推荐方式

推荐写法：

```csharp
protected override void ConfigureAgentCapabilities(AgentCapabilitySelection selection)
{
    selection.IncludePlugin<ClockAgentPlugin>()
        .IncludePlugin<TravelPlanningAgentPlugin>();
}
```

如需在低样板场景下直接从构建参数接入，也可以使用：

```csharp
options.UsePlugin<ClockAgentPlugin>();
```

## 回答或实施时的约束

1. 先判断用户要改的是插件、ToolSet、Agent 入口还是文档。
2. 如果只是新增工具函数，不要误导到插件层。
3. 如果只是新增一个能力模块，但内部仍复用现有 ToolSet，优先新增插件而不是重复造 ToolSet。
4. 如果链路变更会影响 Conhost 演示入口，记得检查 `Program.cs` 当前启用项是否被用户手动切换过。
5. 如果用户要求审查，优先检查插件是否真的只承担了组合声明职责，而不是重新长回执行职责。
6. 如果用户要求更新技能或 README，优先同步“批量诊断入口只有 `GetPluginInfos()`”这一约束，避免文档再次漂移。