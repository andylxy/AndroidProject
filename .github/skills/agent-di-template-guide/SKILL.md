---
name: agent-di-template-guide
description: "Use when creating, explaining, or refactoring Agent DI and Agent creation templates in this project. 适用于 AgentBase、AgentScaffoldBase、AgentSessionContextFactory、ManagedAgentFactory、IDependency 自动注册、Agent Demo 快速创建、ConfigureAgentBuild 参数配置、ConfigureAgentCapabilities、显式 CreateAgent 模板。关键词：Agent DI、Agent 模板、AgentScaffoldBase、ConfigureAgentCapabilities、ManagedAgentFactory、CreateAgent 模板、Agent Demo。"
argument-hint: "描述你的 Agent DI 问题，或直接说要新建哪种 Agent Demo / 使用 AgentBase 还是 AgentScaffoldBase / 使用默认预设还是自定义选择模板"
user-invocable: true
---

# Agent DI And Template Guide

## 这个 Skill 解决什么问题

这个 Skill 用于处理当前仓库中 Agent 相关的依赖注入与创建模板，重点覆盖以下场景：

1. 解释 `AgentBase`、`AgentSessionContextFactory`、`IDependency` 的关系。
2. 按项目现有 Autofac 风格新增一个 Agent Demo。
3. 提供统一的 Agent 主模板，并通过模板内注释引导显式创建、业务脚本和自定义能力扩展。
4. 把旧的 Agent 写法改造成 `IDependency + 构造函数注入` 模式。
5. 排查 `Program`、Autofac、`AgentBase` 之间的解析链路。
6. 在 `AgentBase` 与 `AgentScaffoldBase` 之间做模板选型。
7. 说明显式 ToolSet 组合、`ConfigureAgentCapabilities` 与 `ConfigureAgentBuild` 的职责边界。

## 什么时候使用

当用户的问题包含下面这些关键词或意图时，应优先使用这个 Skill：

1. Agent DI
2. Autofac 注册 Agent
3. AgentBase 注入
4. AgentSessionContextFactory
5. 新建 Agent Demo
6. ConfigureAgentBuild
7. CreateAgent 模板
8. 显式 Agent 模板
9. ChatClientAgentOptions
10. IDependency 自动注册
11. AgentScaffoldBase
13. ConfigureAgentCapabilities

## 模板边界

本 Skill 的 assets 目录只承载 Agent 类模板，不承载 ToolSet 模板。

允许放在 `./assets/` 的模板类型：

1. `AgentBase` 派生模板
2. `AgentScaffoldBase` 派生模板
3. Function Calling Agent 入口模板
4. 任何以 `IDependency`、构造函数注入、`Program.cs` 切换为核心关注点的 Agent 模板

不应放在本目录的模板类型：

1. `IAgentToolSet` 模板
2. 任何围绕 ToolSet 稳定键使用的 Agent 模板
3. 任何以工具方法签名、`AIFunctionFactory.Create(...)`、`Description(...)` 为核心关注点的 ToolSet 模板

如果你的目标是“新增 ToolSet”，不要在本目录继续加模板，直接跳转到 [agent-tools-integration-guide/SKILL.md](../agent-tools-integration-guide/SKILL.md)。

## 标准处理流程

1. 先确认目标类型是否应该由 Autofac 托管。
2. 如果是 Agent Demo，确认它应继承 `AgentBase` 还是 `AgentScaffoldBase`。
3. 确认该类型是否实现 `IDependency`，以便被 `AddModule` 自动扫描。
4. 使用构造函数接收 `AgentSessionContextFactory` 和 `ManagedAgentFactory`，并通过 `: base(sessionContextFactory, managedAgentFactory)` 转交给父类。
5. 如果是 Function Calling 场景且需要统一工具摘要/历史输出，优先继承 `AgentScaffoldBase`。
6. 工具声明优先通过 `ConfigureAgentCapabilities`，再由 `ConfigureAgentBuild` 处理非工具参数。
7. 如果需要显式接管最终 `AsAIAgent` 调用，再覆写 `CreateAgent(AgentBuildOptions options)`。
7. 优先复用基类 helper，而不是自己复制整套参数映射代码。
8. 在 `Program` 中通过容器解析 Demo，而不是直接 `new`。
9. 变更后优先构建 Conhost 项目进行验证。

## Agent 参数配置与 CreateAgent 约定

1. `AgentBuildOptions` 的创建过程由 `AgentBase` 内部统一完成，对子类不可见。
2. 子类应通过 `ConfigureAgentBuild` 配置 Name 之外的参数，例如 Temperature、Tools、HistoryOptions。
3. 如果 Agent 已经继承 `AgentScaffoldBase`，优先通过 `ConfigureAgentCapabilities` 声明工具来源。
4. `ConfigureAgentBuild` 在这类场景中更适合保留给 Description、History、Temperature 等非工具参数。
5. `CreateAgent` 的职责应当只是接管最终 Agent 创建动作，必要时做少量微调。
6. 如果默认 helper 已够用，优先复用基类提供的 `CreateDefaultChatClient` 和 `CreateDefaultChatClientAgentOptions`。

## AgentBase vs AgentScaffoldBase 选型

### 什么时候选 AgentBase

适合：

1. 普通多轮对话 Agent，不需要 Function Calling 专用骨架。
2. 不需要统一的 ToolSet 诊断输出与历史打印。
3. 你只想保留最小继承链。

### 什么时候选 AgentScaffoldBase

适合：

1. Agent 主要围绕 Function Calling 场景构建。
2. 希望复用 `ResolveToolSetNames(...)`、`WriteFunctionCallingToolSummary(...)`、`PrintFunctionCallingHistoryAsync(...)`。
3. 希望把工具摘要输出、函数调用历史打印做成统一骨架能力。

### 最小模板对比

普通 Agent：

```csharp
public sealed class MyAgentDemo : AgentBase, IDependency
{
}
```

Function Calling Agent：

```csharp
public sealed class MyFunctionCallingDemo : AgentScaffoldBase, IDependency
{
    protected override void ConfigureAgentCapabilities(AgentCapabilitySelection selection)
    {
        selection.IncludeToolSet(ClockAgentToolSet.ToolSetKey)
            .IncludeToolSet(CalculatorAgentToolSet.ToolSetKey)
            .IncludeToolSet(TravelPlanningToolSet.ToolSetKey);
    }
}
```

## AgentBuildOptions 校验约定

1. Agent 的校验应当面向最终的 `AgentBuildOptions`，而不是直接面向 `AgentBase` 上的原始属性。
2. 如果需要补充业务约束，子类应覆写 `ValidateAgentBuildOptions(AgentBuildOptions options)`。
3. 调用顺序固定为：基类先校验最终 `AgentBuildOptions` 的通用约束，再执行子类补充校验。
4. 不要把校验逻辑散落到 `ConfigureAgentBuild` 或 `CreateAgent` 中。

## CreateAgent 模板约定

### 简单方式

默认优先使用下面这个参数配置模板：

```csharp
protected override void ConfigureAgentBuild(AgentBuildOptions options)
{
}
```

### 显式创建方式

如果需要把最终创建动作下沉到 Demo 内，优先使用下面这个模板：

```csharp
protected override AIAgent CreateAgent(AgentBuildOptions options)
{
    var chatClient = CreateDefaultChatClient(options);
    var chatClientAgentOptions = CreateDefaultChatClientAgentOptions(options, chatClient);
    return chatClient.AsAIAgent(chatClientAgentOptions);
}
```

### 复杂方式

如果确实需要更细的创建控制，再在显式创建方式里做局部微调，而不是重新复制参数映射：

```csharp
// protected override AIAgent CreateAgent(AgentBuildOptions options)
// {
//     var chatClient = CreateDefaultChatClient(options);
//     var chatClientAgentOptions = CreateDefaultChatClientAgentOptions(options, chatClient);
//     chatClientAgentOptions.Description = $"{chatClientAgentOptions.Description}-Custom";
//     return chatClient.AsAIAgent(chatClientAgentOptions);
// }
```

## 本项目中的关键定位点

1. Autofac 自动扫描入口见 [Autofac 扫描参考](./references/agent-di-reference.md)。
2. 统一 Agent 主模板见 [Function Calling Agent 模板](./assets/function-calling-agent-template.cs.txt)。
3. 面向人的详细文档见 [Conhost Agent DI 文档](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/README-Agent-DI.md)。
4. 简单显式创建案例见 [StudyPlannerAgentDemo](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/StudyPlannerAgentDemo.cs)。
5. 正式业务模板案例见 [TravelPlannerAgentDemo](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/TravelPlannerAgentDemo.cs)。
6. Function Calling Agent 骨架见 [AgentScaffoldBase](../../../vol.api.sqlsugar/SimpleEasy.MAF.AI/Scaffolds/Agent/AgentScaffoldBase.cs)。
7. Function Calling 预设模式案例见 [FunctionCallingPresetAgentDemo](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/FunctionCallingPresetAgentDemo.cs)。
8. Function Calling 自定义模式案例见 [FunctionCallingCustomSelectionAgentDemo](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/FunctionCallingCustomSelectionAgentDemo.cs)。

## 回答或实施时的约束

1. 不要建议在 Agent Demo 或 `AgentBase` 中直接 `new AgentSessionContextFactory`。
2. 不要把 `AgentBase` 设计成全局静态单例访问对象。
3. 优先复用项目现有 `AddModule + IDependency` 扫描机制。
4. 如果只是为了简化调用入口，可以增加薄封装，但不要把 Session 状态做成全局共享。
5. 如果简单模板已经满足需求，不要默认切到复杂模板。
6. 不要让子类自己创建 `AgentBuildOptions`；参数配置统一放到 `ConfigureAgentBuild`。
7. 不要在 `CreateAgent` 里复制整套参数映射，优先复用基类 helper。
8. 如果需要业务约束校验，统一放到 `ValidateAgentBuildOptions`，不要直接校验 `AgentBase` 原始属性。
9. 如果只是 Function Calling 工具接入，不要跳过 `ConfigureAgentCapabilities` 直接把工具散落到入口类。
10. 如果需要 Function Calling 统一输出骨架，不要再继承裸 `AgentBase` 重复实现相同 helper，优先使用 `AgentScaffoldBase`。

## 新建 Agent Demo 的最短步骤

1. 复制 [Function Calling Agent 模板](./assets/function-calling-agent-template.cs.txt)。
2. 改类名、`AgentName`、描述、模式说明和消息脚本。
3. 如果只需要最小链路，保留模板默认的显式 ToolSet 组合与最小 messages。
4. 如果需要业务能力组合，按模板中的注释启用 `ConfigureAgentCapabilities(...)`、摘要输出或历史输出扩展。
5. 如果需要接管最终创建，再按模板注释启用 `CreateAgent(AgentBuildOptions options)` 并复用基类 helper。
6. 如果需要业务约束校验，再按模板注释启用 `ValidateAgentBuildOptions(AgentBuildOptions options)`。
7. 在 `Program` 中通过 `serviceProvider.GetRequiredService<T>()` 运行。
8. 构建 `SimpleEasy.MEAI.AI.Conhost.csproj` 验证。

## 新建 Function Calling Agent 的最短步骤

1. 继承 `AgentScaffoldBase`，并实现 `IDependency`。
2. 构造函数接收 `AgentSessionContextFactory`、`ManagedAgentFactory`、`AgentToolRegistry`、`AgentCapabilitySelector`，并通过 `base(...)` 传递。
3. 覆写 `AgentName`。
4. 优先覆写 `ConfigureAgentCapabilities` 声明工具来源。
5. 通过 `ConfigureFunctionCallingBuild` 补充非工具参数。
6. 如需自定义工具摘要或历史输出，再覆写 `WriteFunctionCallingToolSummary` / `PrintFunctionCallingHistoryAsync`。
7. 在 `Program` 中通过容器解析运行。
8. 构建 `SimpleEasy.MEAI.AI.Conhost.csproj` 验证。

## 可复制模板

如果要直接起步，只复制这一份主模板：

规则：如果你需要的是 ToolSet 模板，而不是 Agent 入口模板，不要在本目录重复找镜像模板，直接跳转到 [agent-tools-integration-guide/SKILL.md](../agent-tools-integration-guide/SKILL.md)。

1. 统一 Agent 主模板：`./assets/function-calling-agent-template.cs.txt`

模板已经包含：

1. `AgentScaffoldBase` 继承关系
2. `ConfigureAgentCapabilities` 默认接入方式
3. `ConfigureFunctionCallingBuild` 模板
4. 可选的 `ConfigureAgentCapabilities` 注释模板
5. 可选的虚函数覆写模板
6. 最小 `RunAsync` 与多轮对话脚本骨架

## 复制后需要替换的占位符清单

1. `YOUR_CLASS_NAME`：替换成入口类名。
2. `YOUR_AGENT_NAME`：替换成 Agent 名称。
3. `YOUR_DESCRIPTION`：替换成 Agent 描述。
4. `YOUR_TOOLSET_CLASS.ToolSetKey`：如果启用自定义选择模板，就替换成目标 ToolSet 类型的稳定键引用。
5. `YOUR_SUMMARY_TEXT`、`YOUR_HISTORY_TITLE`：如果启用覆写模板，就替换成真实输出文案。
6. `YOUR_MODE_DESCRIPTION`、`YOUR_FIRST_MESSAGE`、`YOUR_SECOND_MESSAGE`：替换成实际模式说明与验证脚本。
7. `YOUR_THIRD_MESSAGE`：如果需要业务化多轮脚本，就按模板注释扩展第三轮及后续消息。

## 从模板复制到接入 Program.cs 的完整落地步骤

1. 从本 Skill 的统一 Agent 主模板复制一份到 `vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos`。
2. 按模板内占位符清单替换类名、Agent 名称、描述、模式说明和消息脚本。
3. 如果启用了自定义能力组合，同时确认目标 ToolSet 已存在；如不存在，先按 [agent-tools-integration-guide/SKILL.md](../agent-tools-integration-guide/SKILL.md) 创建 ToolSet。
4. 如果模板里使用了自定义 ToolSet 键引用，确认目标 ToolSet 类已经声明 `ToolSetKey`。
5. 确认新 Agent 继续实现 `IDependency`，不要改掉构造函数注入签名，这样容器扫描才能自动发现。
6. 打开 [Program.cs](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Program.cs)，按现有风格新增一组注释说明，并切换到 `await serviceProvider.GetRequiredService<YOUR_CLASS_NAME>().RunAsync();`。
7. 如果当前 Program 里还有别的默认 Demo 在运行，先把那一行注释掉，避免入口冲突。
8. 构建 `vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/SimpleEasy.MEAI.AI.Conhost.csproj`；构建通过后再 `dotnet run --project vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/SimpleEasy.MEAI.AI.Conhost.csproj` 验证运行结果。

## 正式业务 Agent 的最短步骤

1. 复制 [Function Calling Agent 模板](./assets/function-calling-agent-template.cs.txt) 或直接复制 `TravelPlannerAgentDemo`。
2. 修改类名、`AgentName`、描述和业务脚本。
3. 如果需要业务 ToolSet 组合，启用模板中的 `ConfigureAgentCapabilities(...)` 注释块。
4. 如需显式接管创建，用模板中的 `CreateAgent(AgentBuildOptions options)` 注释块并复用基类 helper。
5. 如需业务约束校验，再启用 `ValidateAgentBuildOptions(AgentBuildOptions options)`。
6. 把 `messages` 扩展成你的业务多轮验证脚本。
7. 在 `Program` 中增加一行 `serviceProvider.GetRequiredService<YourAgentDemo>().RunAsync()` 作为切换入口。
8. 构建 `SimpleEasy.MEAI.AI.Conhost.csproj`，确认没有编译错误。