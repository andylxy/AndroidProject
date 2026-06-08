# Agent DI 参考说明

这份参考资料服务于 `.github/skills/agent-di-template-guide/SKILL.md`，用于补充当前仓库中 Agent 的 Autofac 注册、模板与创建链路。

## 1. Autofac 自动扫描入口

项目统一通过 `AddModule` 自动扫描实现 `IDependency` 的类型。

关键逻辑位于：

- [vol.api.sqlsugar/SimpleEasy.Core/AutofacManager/AutofacContainerModuleExtension.cs](../../../vol.api.sqlsugar/SimpleEasy.Core/AutofacManager/AutofacContainerModuleExtension.cs)

核心规则：

```csharp
builder.RegisterAssemblyTypes(assemblies.ToArray())
    .Where(type => baseType.IsAssignableFrom(type)
        && !type.IsAbstract
        && !type.Name.EndsWith("ApiClient"))
    .AsSelf()
    .AsImplementedInterfaces()
    .InstancePerLifetimeScope();
```

所以只要某个类型实现了 `IDependency`，通常就不需要再手写 `RegisterType<T>()`。

## 2. Agent 相关的当前分工

### AgentBase

位置：

- [vol.api.sqlsugar/SimpleEasy.MAF.AI/Runtime/Agent/AgentBase.cs](../../../vol.api.sqlsugar/SimpleEasy.MAF.AI/Runtime/Agent/AgentBase.cs)

职责：

1. 持有并惰性创建 `AIAgent`
2. 管理 Session 相关公共能力
3. 对子类暴露 `SendAsync`、`GetSessionAsync` 等统一入口
4. 通过构造函数接收 `AgentSessionContextFactory` 和 `ManagedAgentFactory`
5. 在基类内部统一创建 `AgentBuildOptions`，子类只通过 `ConfigureAgentBuild` 配置参数

### AgentSessionContextFactory

位置：

- [vol.api.sqlsugar/SimpleEasy.MAF.AI/Runtime/Agent/AgentSessionContextFactory.cs](../../../vol.api.sqlsugar/SimpleEasy.MAF.AI/Runtime/Agent/AgentSessionContextFactory.cs)

职责：

1. 自身由 Autofac 托管
2. 为 `AgentBase` 提供 `AgentSessionContext` 的创建能力
3. 避免在 `AgentBase` 内部直接 new 具体依赖

## 3. CreateAgent 模板选择与职责边界

当前约定里，子类看不到 `CreateAgentBuildOptions`，因为 `AgentBuildOptions` 的创建过程由基类统一收口。

职责边界如下：

1. `ConfigureAgentBuild`：唯一参数配置入口
2. `CreateAgent`：只负责接管最终 Agent 创建动作，必要时做局部微调
3. 如需默认创建逻辑，优先复用 `CreateDefaultChatClient` 和 `CreateDefaultChatClientAgentOptions`
4. `ValidateAgentBuildOptions`：面向最终 `AgentBuildOptions` 的业务补充校验入口

### 简单模板

默认优先使用简单模板：

```csharp
protected override void ConfigureAgentBuild(AgentBuildOptions options)
{
}
```

### 显式创建模板

```csharp
protected override AIAgent CreateAgent(AgentBuildOptions options)
{
    var chatClient = CreateDefaultChatClient(options);
    var chatClientAgentOptions = CreateDefaultChatClientAgentOptions(options, chatClient);
    return chatClient.AsAIAgent(chatClientAgentOptions);
}
```

### 复杂模板

如果确实需要更细的创建控制，再在显式创建模板基础上做少量微调：

```csharp
// protected override AIAgent CreateAgent(AgentBuildOptions options)
// {
//     var chatClient = CreateDefaultChatClient(options);
//     var chatClientAgentOptions = CreateDefaultChatClientAgentOptions(options, chatClient);
//     chatClientAgentOptions.Description = $"{chatClientAgentOptions.Description}-Custom";
//     return chatClient.AsAIAgent(chatClientAgentOptions);
// }
```

## 4. AgentBuildOptions 校验扩展点

当前约定里，校验不再直接针对 `AgentBase` 上的原始属性，而是针对已经完成默认值填充和 `ConfigureAgentBuild` 处理后的最终 `AgentBuildOptions`。

推荐模板：

```csharp
protected override void ValidateAgentBuildOptions(AgentBuildOptions options)
{
    if (!string.Equals(options.Provider, "AzureOpenAI", StringComparison.OrdinalIgnoreCase))
    {
        throw new InvalidOperationException("当前 Demo 只允许使用 AzureOpenAI Provider。");
    }
}
```

职责边界：

1. 基类负责校验 `options.Provider`、`options.AgentOptions.Name`、`options.AgentOptions.Instructions` 等通用约束。
2. 子类只补充业务特有约束，例如 Provider 白名单、HistoryOptions 组合限制、Tools 依赖关系。
3. 不要在 `ConfigureAgentBuild` 或 `CreateAgent` 里夹带校验逻辑。

## 5. Program 中如何使用

位置：

- [vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Program.cs](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Program.cs)

重点：

1. 通过 `AddModule` 扫描 `IDependency`
2. 通过容器解析 Demo
3. 不直接 `new` Demo

## 6. 模板资产

1. [统一 Agent 主模板](../assets/function-calling-agent-template.cs.txt)
2. [TravelPlannerAgentDemo 实例](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/TravelPlannerAgentDemo.cs)
3. [StudyPlannerAgentDemo 实例](../../../vol.api.sqlsugar/SimpleEasy.MEAI.AI.Conhost/Demos/StudyPlannerAgentDemo.cs)