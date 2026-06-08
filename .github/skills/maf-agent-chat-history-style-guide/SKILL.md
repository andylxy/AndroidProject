---
name: maf-agent-chat-history-style-guide
description: "Use when creating, updating, or reviewing vol.api.sqlsugar/SimpleEasy.MAF.AI Agent and ChatHistory code. 适用于 AgentBase 子类、AgentSessionContext、ChatHistoryProvider、SqlSugar+Cache 混合消息存储、Provider Factory、会话锁和会话序列化风格。关键词：SimpleEasy.MAF.AI、AgentBase、ChatHistory、HybridChatHistoryProvider、AgentSessionContext、Agent Demo。"
argument-hint: "描述你要新增或修改的 Agent、Session、ChatHistory Provider、Store、Cache 或消息历史流程"
user-invocable: true
---

# MAF Agent And ChatHistory Style Guide

## 这个 Skill 解决什么问题

这个 Skill 用于约束 `vol.api.sqlsugar/SimpleEasy.MAF.AI` 中 Agent 与 ChatHistory 相关实现，重点覆盖：

1. 新增 `AgentBase` 子类或 Demo/场景 Agent。
2. 调整 `AgentSessionContext` 与会话生命周期。
3. 新增或改造 `ChatHistoryProvider`、Store、Cache、SessionLock。
4. 保持生产级消息历史为“数据库 + 缓存”混合结构。
5. 保持 Agent 基础设施、Provider 工厂和 DI 组织方式与现有实现一致。

## 项目现有风格摘要

1. `AgentBase` 是所有 Agent/Demo 的统一继承入口，子类只关注 `AgentName`、`Instructions`、`CreateAgent` 和演示逻辑。
2. Session 相关职责已从 `AgentBase` 下沉到 `AgentSessionContext`，但对外 protected API 保持稳定。
3. `AgentSessionContextFactory` 负责内部协作对象创建，本身由 DI 托管，避免在 Agent 中手工 new SessionContext。
4. ChatHistory 采用分层设计：`IChatHistoryStore` 持久层、`IChatHistoryCache` 缓存层、`IChatHistorySessionLock` 并发锁、`HybridChatHistoryProvider` 负责协调。
5. 生产存储采用 SqlSugar 持久化 + SimpleCache 缓存；锁默认仅是进程内锁，因此要明确其边界。
6. Provider 的创建通过工厂 `HybridChatHistoryProviderFactory` 完成，避免调用方散落构造细节。
7. ChatHistory 的持久化实体应落在 `SimpleEasy.Entity/DomainModels/MAF` 这类通用域目录，而不是留在 `SimpleEasy.MAF.AI` 基础设施项目中。
8. ChatHistory 的数据访问与缓存访问风格应参考仓库 Service 层的 `DbContext.Queryable<T>()` 与 `SimpleCacheService` 注入习惯，但不意味着基础设施层应继承 `ServiceBase`。
9. 注释偏解释设计原因，而不只是解释代码表面行为。

## 什么时候使用

当用户的问题包含这些关键词或意图时，应优先使用这个 Skill：

1. 新增 Agent Demo
2. 新增 AgentBase 子类
3. 调整 SessionContext
4. 新增 ChatHistoryProvider
5. 新增消息历史存储
6. SqlSugar 聊天记录表
7. Agent 多轮记忆
8. ChatHistory 缓存
9. Agent 会话锁
10. HybridChatHistoryProvider

## 标准处理流程

1. 先判断目标是 Agent 行为层，还是 Session/History 基础设施层。
2. 如果是业务 Agent，优先继承 `AgentBase`，并通过构造函数注入 `AgentSessionContextFactory`。
3. 如果是 Session 能力，优先扩展 `AgentSessionContext` 或其工厂，不要把状态逻辑重新塞回 `AgentBase`。
4. 如果是消息历史能力，按 `Store + Cache + SessionLock + Provider + Factory` 分层落位。
5. `ChatHistoryProvider` 必须保持异步存储/读取，不要退回同步实现。
6. 默认持久层优先数据库，缓存作为加速层，不要反转为“只靠缓存”。
7. 数据操作尽量按仓库 Service 层风格组织，优先使用 `DbContext.Queryable<T>()` 等访问习惯。
8. 缓存操作尽量按仓库 Service 层风格组织，优先使用注入的 `ISimpleCacheService`。
9. 如果需要注入注册，优先在扩展注册或 IDependency 扫描层统一处理。

## 本项目中的关键定位点

1. Agent 基类参考 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Runtime/Agent/AgentBase.cs`。
2. Session 状态管理参考 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Runtime/Agent/AgentSessionContext.cs`。
3. Session 工厂参考 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Runtime/Agent/AgentSessionContextFactory.cs`。
4. Provider 实现参考 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Runtime/History/HybridChatHistoryProvider.cs`。
5. Provider 工厂参考 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Runtime/History/HybridChatHistoryProviderFactory.cs`。
6. Store/Cache/Lock 抽象参考 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Abstractions/History/IChatHistoryStore.cs`、`IChatHistoryCache.cs`、`IChatHistorySessionLock.cs`。
7. SqlSugar 存储实现参考 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Infrastructure/History/SqlSugarChatHistoryStore.cs`。
8. 缓存实现参考 `vol.api.sqlsugar/SimpleEasy.MAF.AI/Infrastructure/History/SimpleCacheChatHistoryCache.cs`。

## 具体约束

1. 不要在业务 Agent 或 Demo 中直接 new `AgentSessionContextFactory`。
2. 不要把 Session 状态做成全局共享静态实例。
3. 不要把 ChatHistory 逻辑重新塞回 `AgentBase` 主体，保持基础设施分层。
4. 不要把缓存视为唯一真相源；生产持久化仍应以数据库为准。
5. 不要假设 `AgentSession` 一定暴露固定 Id 属性，涉及会话键解析时要兼容已有反射/回退策略。
6. 不要误导调用方认为 `SemaphoreSlim` 锁能解决跨进程并发；当前默认锁仅保证单进程内串行。
7. 不要在 Provider 中引入业务逻辑；它只负责历史读取、合并、裁剪、持久化和缓存协调。
8. 不要把 ChatHistory 基础设施层直接改造成继承 `ServiceBase<T>` 的业务服务形态。

## 新增 Agent 的最短步骤

1. 创建继承 `AgentBase` 的类。
2. 构造函数注入 `AgentSessionContextFactory`，并通过 `: base(sessionContextFactory)` 传递。
3. 覆写 `AgentName`、`Instructions`、`CreateAgent`。
4. 如需记忆或会话切换，调用基类暴露的 Session API，不要自己维护 Session 状态。
5. 通过 DI 解析并在 Conhost 或调用入口验证。

## 新增 ChatHistory 基础设施的最短步骤

1. 先判断新增点属于 Store、Cache、Lock、Provider 还是 Factory。
2. 按现有接口抽象新增对应实现，而不是直接改具体类到失控。
3. 保持所有存储读取 API 为异步。
4. 如涉及消息落库，保持 `SessionId + Sequence + PayloadJson + Hash` 这种记录模式，并将实体放入 `SimpleEasy.Entity` 项目。
5. 如涉及数据读取与写入，优先复用 `DbContext.Queryable<T>()` 与注入式数据库访问习惯。
6. 如涉及缓存，遵循“先查缓存，miss 后读库，再回填缓存”，并优先使用注入的 `ISimpleCacheService`。
7. 如涉及并发，明确说明锁的作用域。