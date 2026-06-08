---
name: agent-tools-integration-guide
description: "Use when adding Function Calling tools, creating ToolSet, wiring ConfigureAgentCapabilities, extending AgentScaffoldBase, or deciding between Tools and Scaffolds/Agent in this project. 适用于新增 ToolSet、配置 AgentCapabilitySelection、扩展 AgentScaffoldBase、判断 Tools 与 Scaffolds/Agent 落位。关键词：Function Calling、ToolSet、AgentCapabilitySelection、ConfigureAgentCapabilities、AgentScaffoldBase、添加工具、工具接入。"
argument-hint: "描述你要新增哪类工具、落到哪个 ToolSet、由哪个 Agent 或 Demo 挂载，以及是默认显式组合还是自定义选择模式"
user-invocable: true
---

# Agent Tools Integration Guide

## 这个 Skill 解决什么问题

这个 Skill 用于处理当前仓库里 Function Calling 工具的新增、组织和接入，重点覆盖：

1. 新增一个 `ToolSet`。
2. 给 Agent 或 Demo 挂接显式 ToolSet 组合。
3. 使用 `ConfigureAgentCapabilities` 做增量式工具组合。
4. 判断代码应该落在 `Tools/` 还是 `Scaffolds/Agent/`。
5. 扩展 `AgentScaffoldBase` 的工具摘要或历史输出行为。

## 什么时候使用

当用户的问题包含下面这些关键词或意图时，应优先使用这个 Skill：

1. 添加工具
2. 新增 ToolSet
3. Function Calling
4. 显式 ToolSet 组合
5. AgentCapabilitySelection
6. ConfigureAgentCapabilities
7. AgentScaffoldBase
8. 工具预设
9. 工具组合
10. ToolSet 落位

## 模板边界

本 Skill 的 assets 目录只承载 ToolSet 模板，不承载 Agent 入口模板。

允许放在 `./assets/` 的模板类型：

1. `IAgentToolSet` 模板
2. Utility ToolSet 模板
3. Business ToolSet 模板
4. 任何以 `ToolSetKey`、`AIFunctionFactory.Create(...)`、`Description(...)` 为核心关注点的模板

不应放在本目录的模板类型：

1. `AgentBase` 派生模板
2. `AgentScaffoldBase` 派生模板
3. Function Calling Agent 入口模板
4. 任何以 `IDependency`、构造函数注入、`Program.cs` 切换为核心关注点的 Agent 模板

如果你的目标是“新增 Agent 入口类”，不要在本目录继续加模板，直接跳转到 [agent-di-template-guide/SKILL.md](../agent-di-template-guide/SKILL.md)。

## 目录边界

### Tools 目录负责什么

`vol.api.sqlsugar/SimpleEasy.MAF.AI/Tools/` 负责承载工具组织层：

1. `IAgentToolSet`
3. `AgentCapabilitySelection`
4. `AgentToolRegistry`
5. `AgentCapabilitySelector`
6. 具体 `ToolSets/Utility` 与 `ToolSets/Business`

### Scaffolds/Agent 负责什么

`vol.api.sqlsugar/SimpleEasy.MAF.AI/Scaffolds/Agent/` 负责承载建立在 `AgentBase` 之上的高层可继承骨架。

当前典型案例：

1. `AgentScaffoldBase`

这个骨架虽然服务的是工具调用场景，但因为它直接继承 `AgentBase` 并向上提供 Agent 能力，
所以归属 `Scaffolds/Agent`，不归属 `Tools/`。

## 标准处理流程

1. 先判断你要新增的是“工具本身”还是“Agent 侧接入方式”。
2. 如果是工具本身，优先新增 `ToolSet`，不要直接把函数散落到 Demo 中。
3. 如果只是复用已有工具组合，优先在 Agent 中覆写 `ConfigureAgentCapabilities` 并显式列出 ToolSet。
4. 如果需要默认组合 + 增量组合，优先覆写 `ConfigureAgentCapabilities`。
5. 如果需要统一输出工具摘要或历史快照，优先扩展 `AgentScaffoldBase`，而不是在每个 Demo 中复制打印逻辑。
6. 如果只是临时附加少量工具，再考虑在 `ConfigureAgentBuild` 中追加 `agent.Tools`。
7. 修改后优先构建 `SimpleEasy.MAF.AI.csproj` 和 `SimpleEasy.MEAI.AI.Conhost.csproj` 验证。

## 新增 ToolSet 的推荐方式

1. 判断名称前缀属于 `Utility` 还是 `Business`。
2. 放入：
   1. `Tools/ToolSets/Utility`
   2. `Tools/ToolSets/Business`
3. 实现 `IAgentToolSet`。
4. 在 ToolSet 类内声明 `ToolSetKey` 作为稳定注册名。
5. 在 ToolSet 内部决定是否缓存 `AITool` 列表。

最小模板：

```csharp
public sealed class SampleBusinessToolSet : IAgentToolSet
{
    private IReadOnlyList<AITool>? _cachedTools;

    public string Name => "Business.Sample";

    public IList<AITool> CreateTools()
    {
        _cachedTools ??=
        [
            AIFunctionFactory.Create(GetSampleData, name: nameof(GetSampleData))
        ];

        return [.. _cachedTools];
    }
}
```

## Agent 侧接入方式

### 方式 1：默认显式组合模式

适合：

1. 新 Agent 只想复用当前入口定义好的推荐组合。
2. 想把样板代码压到最少。

写法：

```csharp
protected override void ConfigureAgentCapabilities(AgentCapabilitySelection selection)
{
    selection.IncludeToolSet(ClockAgentToolSet.ToolSetKey)
        .IncludeToolSet(CalculatorAgentToolSet.ToolSetKey)
        .IncludeToolSet(TravelPlanningToolSet.ToolSetKey);
}
```

### 方式 2：自定义选择模式

适合：

1. 需要默认组合 + 显式 ToolSet 增量组合。
2. 需要教学、验证或灰度试验。

写法：

```csharp
protected override void ConfigureAgentCapabilities(AgentCapabilitySelection selection)
{
    selection.IncludeToolSet(ClockAgentToolSet.ToolSetKey)
        .IncludeToolSet(CalculatorAgentToolSet.ToolSetKey)
        .IncludeToolSet(TravelPlanningToolSet.ToolSetKey);
}
```

## AgentScaffoldBase 扩展点

如果 Agent 需要统一的 Function Calling 接入骨架，优先继承 `AgentScaffoldBase`。

它当前提供了这些可复用点：

1. `FunctionCallingInstructions`
2. `ConfigureFunctionCallingBuild(...)`
3. `ResolveToolSetNames(...)`
4. `WriteFunctionCallingToolSummary(...)`
5. `PrintFunctionCallingHistoryAsync(...)`

其中后两者已经是虚函数，子类可以覆写：

```csharp
protected override void WriteFunctionCallingToolSummary(
    string entryTitle,
    string modeDescription,
    AgentSession session,
    IReadOnlyList<string> resolvedToolSets)
{
    base.WriteFunctionCallingToolSummary(entryTitle, modeDescription, session, resolvedToolSets);
    Console.WriteLine("自定义说明: 当前入口演示的是增量式工具组合。\n");
}
```

## 本项目中的关键定位点

1. `vol.api.sqlsugar/SimpleEasy.MAF.AI/Tools/README.md`
2. `vol.api.sqlsugar/SimpleEasy.MAF.AI/Scaffolds/Agent/AgentScaffoldBase.cs`
3. `vol.api.sqlsugar/SimpleEasy.MAF.AI/Scaffolds/Agent/README.md`
4. `vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/FunctionCallingPresetAgentDemo.cs`
5. `vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/FunctionCallingCustomSelectionAgentDemo.cs`
6. `./assets/toolset-template.cs.txt`

## 可复制模板

如果要快速起步，只复制这一份 ToolSet 主模板：

规则：如果你需要的是 Agent 入口模板，而不是 ToolSet 模板，不要在本目录重复找镜像模板，直接跳转到 [agent-di-template-guide/SKILL.md](../agent-di-template-guide/SKILL.md)。

1. 统一 ToolSet 主模板：`./assets/toolset-template.cs.txt`

建议做法：

1. 先复制模板。
2. 替换类名、`ToolSetKey`、分类和工具方法。
3. 再决定由哪个 Agent 通过 `ConfigureAgentCapabilities` 挂接。

## 复制后需要替换的占位符清单

1. `YOUR_CLASS_NAME`：替换成 ToolSet 类名。
2. `YOUR_TOOLSET_KEY`：替换成当前 ToolSet 类内的稳定键字符串。
3. `YOUR_METHOD_NAME`、`YOUR_PARAMETER_NAME`：替换成你的真实工具方法与参数。
4. `YOUR_METHOD_DESCRIPTION`、`YOUR_PARAMETER_DESCRIPTION`、`YOUR_RESULT_TEXT`：替换成真实文案。
5. 如果需要多个工具方法，按模板中的注释块继续追加第二个、第三个方法。

## 从模板复制到接入 Program.cs 的完整落地步骤

1. 从本 Skill 的统一 ToolSet 主模板复制一份到 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Tools/ToolSets/Utility` 或 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Tools/ToolSets/Business`。
2. 按模板内占位符清单替换类名、`ToolSetKey`、方法名、参数名和 Description 文案。
3. 在新 ToolSet 类内声明 `public const string ToolSetKey = "能力域.能力名";`。
4. 确认新 ToolSet 实现了 `IAgentToolSet`，并保持构造简单，便于 Registry 自动发现。
5. 选择一个 Agent 接入方式：
6. 在 Agent 中覆写 `ConfigureAgentCapabilities`，并引用 `YourToolSetClass.ToolSetKey`。
7. 如果仓库里还没有对应 Agent，可继续使用 [agent-di-template-guide/SKILL.md](../agent-di-template-guide/SKILL.md) 中的 Function Calling Agent 模板创建入口类。
8. 打开 [Program.cs](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Program.cs)，增加或切换一行 `await serviceProvider.GetRequiredService<YOUR_CLASS_NAME>().RunAsync();` 到你的 Agent 入口类。
9. 构建 `vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/SimpleEasy.MEAI.AI.Conhost.csproj` 验证从 ToolSet 到 Program 入口的整条链路。

## 回答或实施时的约束

1. 不要把 ToolSet 直接写进 Demo 入口类。
2. 不要把 `AgentScaffoldBase` 再放回 `Tools/` 目录。
3. 不要在新增工具时跳过 `ToolSetKey` 这类稳定命名约定。
4. 不要让 Agent 直接依赖 Registry/Selector 完成业务逻辑，优先通过 `ConfigureAgentCapabilities` 声明。
5. 如果只是想复用默认组合，不要默认切到更复杂的自定义模式。