---
name: business-module-workflow
description: "Use when creating a new business module end-to-end in this repository. 适用于新增业务模块、从实体到服务到缓存到 SqlSugar 到 Agent/ChatHistory 的统一工作流，串联 Entity、Cache、SqlSugar、ZhongYi Services、SimpleEasy.System Services、SimpleEasy.MAF.AI 技能。关键词：新增业务模块、统一工作流、模块脚手架、端到端实现、数据库+缓存+服务+Agent。"
argument-hint: "描述你要新增的业务模块，包含实体、服务、缓存、数据库操作、是否需要 Agent 或 ChatHistory"
user-invocable: true
---

# Business Module Workflow

## 这个 Skill 解决什么问题

这个 Skill 是仓库级总入口技能，用于在新增业务模块时，把相关 skill 串成统一工作流，避免实现风格分裂。它不替代各专项 skill，而是负责判断应该依次加载哪些 skill，并规定整体落地顺序。

## 它会串联哪些 Skill

1. `entity-style-guide`：负责实体、DomainModels、扩展对象。
2. `cache-style-guide`：负责统一缓存抽象和双实现能力。
3. `sqlsugar-style-guide`：负责 SqlSugar 注册、仓储、扩展和初始化。
4. `service-style-guide`：负责 ZhongYi 与 System 两类服务的数据库实现、缓存一致性、事务与 Partial 拆分。
5. `agent-di-template-guide`：负责 Agent DI、Agent 模板与 Demo 创建。
6. `maf-agent-chat-history-style-guide`：负责 `SimpleEasy.MAF.AI` 中 Agent 和 ChatHistory 的实现约束。

## 什么时候使用

当用户的问题包含这些关键词或意图时，应优先使用这个总入口 Skill：

1. 新增业务模块
2. 从实体开始做一整套实现
3. 新增数据库表对应后端能力
4. 新增服务 + 缓存 + 仓储
5. 新增业务功能并带 Agent
6. 新增带消息历史的 Agent 模块
7. 统一模块脚手架
8. 端到端模块实现

## 统一工作流

1. 先判断模块属于业务域还是系统域。
2. 先建实体层，加载 `entity-style-guide`。
3. 如果涉及缓存或高频读取，加载 `cache-style-guide`。
4. 如果涉及数据库基础设施或通用仓储/扩展，加载 `sqlsugar-style-guide`。
5. 如果涉及服务层数据库实现，不区分 ZhongYi 还是 System，统一加载 `service-style-guide`，再按模块类型套用附加约束。
6. 如果模块包含 Agent、会话、多轮记忆或消息历史，先加载 `agent-di-template-guide`，再加载 `maf-agent-chat-history-style-guide`。
7. 实施顺序必须优先保持“实体 -> 基础设施 -> 服务 -> Agent/ChatHistory -> 验证”。

## 统一实施约束

1. 不要跳过实体层直接在服务里发明数据库结构。
2. 不要在业务服务里复制通用 SqlSugar/缓存能力，优先复用现有基础设施。
3. 不要把 ZhongYi 风格和 System 风格混写在同一个服务实现中。
4. 不要把 Agent 业务逻辑和 ChatHistory 基础设施混成一个类。
5. 新增模块时，所有读缓存都必须有对应写路径的失效方案。
6. 若新增 Agent 多轮记忆，必须默认采用数据库 + 缓存的历史方案，不要只保留内存态。
7. 若新增 Agent/ChatHistory 持久化表，实体应落在 `SimpleEasy.Entity/DomainModels` 的合适通用域目录中，不要把表实体留在 `SimpleEasy.MAF.AI` 基础设施项目里。
8. 若新增 Agent/ChatHistory 的数据和缓存实现，代码风格应参考仓库 Service 层的 `DbContext.Queryable<T>()` 与 `SimpleCacheService` 注入习惯，但基础设施层不应继承 `ServiceBase`。

## 使用这个 Skill 时的输出要求

1. 先明确本次模块涉及哪些层。
2. 明确要调用哪些下游 skill。
3. 按顺序实施，不要跨层乱改。
4. 最终输出时要说明：新增了哪些实体、哪些服务、哪些缓存、哪些数据库操作、是否包含 Agent/ChatHistory。

## 新增业务模块的最短步骤

1. 定义模块边界，确认是 ZhongYi 还是 System，是否涉及 Agent。
2. 创建或调整实体，遵循 `entity-style-guide`。
3. 补缓存抽象或缓存使用，遵循 `cache-style-guide`。
4. 补 SqlSugar 基础设施或通用数据能力，遵循 `sqlsugar-style-guide`。
5. 落业务服务实现，统一遵循 `service-style-guide`，再按模块位置套用 ZhongYi 或 System 的附加规则。
6. 如有 Agent/多轮记忆，再补 `agent-di-template-guide` 和 `maf-agent-chat-history-style-guide`。
7. 如有 Agent/ChatHistory 持久化表，先把实体落到 `SimpleEasy.Entity/DomainModels` 的正确目录，再实现 Store/Cache/Provider。
8. 构建并验证缓存一致性、数据库读写、事务边界和消息历史行为。