---
name: sqlsugar-style-guide
description: "Use when creating, updating, or reviewing SimpleEasy.SqlSugar code. 适用于新增 SqlSugar 注册、DbManger 连接管理、AOP/日志、SqlSugar 扩展方法、Repository/DbContext 风格代码、CodeFirst/DbFirst 初始化、事务封装、多租户连接切换。关键词：SimpleEasy.SqlSugar、DbManger、SqlSugarRegister、DbRepository、IRepository、SqlSugarExtension、AOP、事务、IOC、多租户。"
argument-hint: "描述你要新增或修改的 ORM 能力，例如连接管理、仓储、事务、扩展方法、表初始化"
user-invocable: true
---

# SqlSugar Style Guide

## 这个 Skill 解决什么问题

这个 Skill 用于处理当前仓库 `SimpleEasy.SqlSugar` 项目的数据库访问基础设施，重点覆盖以下场景：

1. 新增或修改 SqlSugar 注册逻辑。
2. 新增 `DbManger` 连接管理、AOP、外部服务配置。
3. 新增 `ISqlSugarClient` 扩展方法。
4. 新增或修改仓储接口与通用仓储实现。
5. 处理 CodeFirst 初始化、种子数据初始化、多租户连接切换。
6. 对照 SqlSugar 官方文档检查当前仓库实现是否符合既有能力模型，如 AOP、事务、IOC 注入、仓储与多租户约定。

## 与官网文档的对齐范围

SqlSugar 官网文档覆盖面很广，包括查询、增删改、导航、原生 SQL、AOP&日志、事务锁、DbFirst、CodeFirst、仓储、IOC 注入、多租户、跨库查询、分表和大数据写入等能力。

这个 Skill 不是 SqlSugar 官网的完整替代，而是“当前仓库里的 SqlSugar 落地风格指南”。

因此使用时要区分两层：

1. 如果问题是“SqlSugar 官方能力怎么用”，应先以官网文档能力模型为准。
2. 如果问题是“本仓库里这类 SqlSugar 能力应该写到哪里、沿用什么风格”，应以本 Skill 为准。

## 项目现有风格摘要

1. DI 入口集中在 `SqlSugarRegister.UseSqlSugar()`。
2. 连接管理由静态 `DbManger` 负责，包含懒加载单例、请求级回退、连接配置、外部服务配置与初始化逻辑。
3. 通用仓储围绕 `IRepository<TEntity>` 和 `DbRepository<TEntity>` 展开，偏向“通用 CRUD + 事务包装 + 队列保存”。
4. 扩展方法集中在 `SqlSugarExtension`，命名直接贴近使用场景，如 `Add`、`UpdateRange`、`SaveChangesAsync`。
5. 表初始化与种子数据逻辑集中在 `SqlSugarUtils`，通过反射扫描 `SimpleEasy.Entity`。
6. 代码注释相对完整，强调行为说明与兼容旧逻辑的原因。
7. 当前仓库已实际采用 SqlSugar 官网文档强调的几类核心能力：AOP、事务包装、IOC 注入、仓储风格、多库/多租户连接切换、CodeFirst 初始化。

## 什么时候使用

当用户的问题包含下面这些关键词或意图时，应优先使用这个 Skill：

1. 新增仓储
2. 新增 Repository 方法
3. 新增 SqlSugar 扩展
4. 修改 `DbManger`
5. 修改 `SqlSugarRegister`
6. 多租户连接切换
7. CodeFirst 初始化
8. 种子数据初始化
9. 数据库事务封装
10. `ISqlSugarClient` 工具方法
11. AOP 与 SQL 日志
12. DbContext 风格接入
13. 对照 SqlSugar 官网检查仓库实现是否偏离官方能力模型

## 标准处理流程

1. 先确认改动属于连接管理、仓储层还是扩展方法层。
2. 连接和注册相关改动优先落在 `DBManager`。
3. 通用查询/保存语义优先落在 `Repository` 或 `SqlSugarExtension`，避免在业务项目重复实现。
4. 如果改动影响实体扫描、建表、种子数据，优先修改 `SqlSugarUtils`。
5. 保持事务与 `SaveQueues()` 风格一致，不要混入完全不同的持久化节奏。
6. 如涉及多库/多租户，优先复用 `Tenant`、`GetConnection`、`GetConnectionScopeWithAttr` 等既有机制。
7. 如果用户问题带有明显的“官网能力核对”意图，应先对照官网文档的大类能力，再判断当前仓库是否已经有约定实现。

## 本项目中的关键定位点

1. 注册入口参考 `SimpleEasy.SqlSugar/DBManager/SqlSugarRegister.cs`。
2. 连接与配置核心参考 `SimpleEasy.SqlSugar/DBManager/DbManger.cs`。
3. 扩展方法参考 `SimpleEasy.SqlSugar/DBManager/SqlSugarExtension.cs`。
4. 仓储接口参考 `SimpleEasy.SqlSugar/Repository/IRepository.cs`。
5. 通用仓储参考 `SimpleEasy.SqlSugar/Repository/DbRepository.cs`。
6. 表与种子初始化参考 `SimpleEasy.SqlSugar/SqlSugarUtils/SqlSugarUtils.cs`。
7. 如果需要核对官网能力范围，优先查看官网文档中的 AOP&日志、数据事务、IOC 注入、使用仓储、多租户基础、迁移/创建表 等章节。

## 具体约束

1. 不要在业务服务里重复发明通用仓储能力，优先下沉到 `DbRepository` 或扩展方法。
2. 不要直接绕开 `DbManger` 新建孤立连接对象，除非用户明确要求隔离连接。
3. 不要破坏现有 `SaveQueues()` / `AddQueue()` 的提交节奏。
4. 不要在仓储层写过多业务判断，仓储应保持通用性。
5. 修改种子或建表逻辑时，注意不要把扩展展示类误识别成实体表。
6. 不要把“官网支持某能力”直接等同于“本仓库已经采用该能力”；需要先核对仓库现状再落地。

## 官网能力与本仓库映射

1. 官网的 `AOP&日志` 在本仓库主要落在 `DBManager` 及其协作类。
2. 官网的 `数据事务` 与 `仓储` 在本仓库主要映射到 `Repository`、`DbContextBeginTransaction` 和 `SaveQueues()` 风格。
3. 官网的 `IOC注入` 在本仓库主要映射到 `SqlSugarRegister.UseSqlSugar()`、请求级 `ISqlSugarClient` 解析和 `DbManger.Db` 回退逻辑。
4. 官网的 `CodeFirst/迁移创建表` 在本仓库主要映射到 `SqlSugarUtils`。
5. 官网的 `多租户基础/跨库查询` 在本仓库主要映射到 `Tenant`、`GetConnection(...)`、`GetConnectionScopeWithAttr(...)`。
6. 官网还有大量导航查询、原生 SQL、动态建类、分表、大数据写入等能力；除非仓库里已有明确落点，否则不要在本 Skill 中擅自扩展为“统一规范”。

## 新增仓储能力的最短步骤

1. 确认能力是通用 CRUD 还是特定实体查询。
2. 通用能力优先加到 `IRepository<TEntity>` 或 `DbRepository<TEntity>`。
3. 如果本质是 `ISqlSugarClient` 便利封装，优先加到 `SqlSugarExtension`。
4. 事务相关逻辑复用 `DbContextBeginTransaction` 和 `Tenant.BeginTran/CommitTran/RollbackTran`。
5. 构建并检查对 net8.0/net9.0 双目标是否一致。

## 新增初始化能力的最短步骤

1. 如果涉及注册，改 `SqlSugarRegister`。
2. 如果涉及连接列表、AOP、命名规则、多库行为，改 `DbManger`。
3. 如果涉及表扫描、建表、种子数据，改 `SqlSugarUtils`。
4. 保持日志输出、异常捕获与现有风格一致。

## 本 Skill 当前判断

基于官网文档核对，这个 Skill 的主方向是正确的：它确实覆盖了当前仓库最核心的 SqlSugar 落点，如连接管理、注册、仓储、事务、CodeFirst、多租户。

但它原先的问题是：

1. 对官方能力面的描述偏窄，没有明确 AOP/日志、IOC 注入、DbContext 这些官网核心章节。
2. 没有强调“这是仓库风格指南，不是 SqlSugar 官网完整说明书”。
3. 没有提醒在“官网能力”和“仓库已落地能力”之间做区分。

以上几点已在本次修正中补齐。