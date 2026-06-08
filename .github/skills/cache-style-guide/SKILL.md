---
name: cache-style-guide
description: "Use when creating, updating, or reviewing SimpleEasy.Cache code. 适用于新增缓存接口、Memory/Redis 缓存实现、AddCache 注入扩展、Hash/Queue/Set 能力、缓存键约定、缓存服务封装。关键词：SimpleEasy.Cache、ISimpleCacheService、MemoryCacheService、RedisCacheService、AddCache。"
argument-hint: "描述你要新增或修改的缓存能力，例如普通缓存、Hash、队列、分布式锁、注入扩展"
user-invocable: true
---

# Cache Style Guide

## 这个 Skill 解决什么问题

这个 Skill 用于处理当前仓库 `SimpleEasy.Cache` 项目的统一缓存抽象与具体实现，重点覆盖以下场景：

1. 新增缓存接口方法。
2. 为 `MemoryCacheService` 和 `RedisCacheService` 增加对应实现。
3. 新增 Hash、Queue、Set 等分部接口能力。
4. 调整 `AddCache` 注入逻辑。
5. 保持缓存项目的接口分层、命名和实现风格一致。

## 项目现有风格摘要

1. 入口是 `ISimpleCacheService` 的 partial 接口拆分，按基础操作、集合操作、高级操作、Hash、Queue 等能力分文件组织。
2. 具体实现使用 partial class 拆分到多个文件中，避免单文件过大。
3. 依赖注入入口集中在 `CacheCollectionExtensions.AddCache()`，按配置切换 Redis 与内存实现。
4. `MemoryCacheService` 与 `RedisCacheService` 尽量保持 API 对齐，方法签名一致。
5. 缓存实现偏“薄封装”，不在这一层引入复杂业务规则。
6. 常通过 Base 项目的序列化扩展 `ToJson` / `ToObject<T>` 进行对象转换。

## 什么时候使用

当用户的问题包含下面这些关键词或意图时，应优先使用这个 Skill：

1. 新增缓存方法
2. 新增 Redis 缓存能力
3. 新增 Memory 缓存能力
4. 修改 `ISimpleCacheService`
5. 新增 Hash/Queue/Stack/Set 接口
6. 修改 `AddCache`
7. 缓存封装统一风格
8. 需要在 Cache 项目里补实现

## 标准处理流程

1. 先确认新增能力属于基础操作、集合操作还是高级操作。
2. 在 `Interface` 下对应 partial 接口文件增加签名。
3. 同步在 `MemoryCacheService` 与 `RedisCacheService` 的对应 partial 文件中补实现。
4. 尽量直接代理底层缓存实例，不在这一层写业务逻辑。
5. 如涉及注入与实现切换，更新 `CacheCollectionExtensions`。
6. 保持 XML 注释、方法命名、泛型签名一致。

## 本项目中的关键定位点

1. DI 入口参考 `SimpleEasy.Cache/CacheCollectionExtensions.cs`。
2. 主接口参考 `SimpleEasy.Cache/Interface/ISimpleCacheService.cs`。
3. Hash 扩展接口参考 `SimpleEasy.Cache/Interface/ISimpleCacheHashService.cs`。
4. 内存实现参考 `SimpleEasy.Cache/Service/MemoryCacheService.cs`。
5. Redis 实现参考 `SimpleEasy.Cache/Service/RedisCacheService.cs`。

## 具体约束

1. 不要只改接口不改双实现，除非目标能力明确只支持某一后端。
2. 不要在缓存层直接耦合业务实体或业务流程。
3. 不要引入与现有 `ISimpleCacheService` 风格完全不同的新命名体系。
4. 同一能力优先保证 Redis 与 Memory 行为尽量一致。
5. 如果底层库本身已有能力，优先做转发封装而不是二次发明。

## 新增缓存能力的最短步骤

1. 选择对应接口分组文件，给 `ISimpleCacheService` partial 增加方法签名。
2. 在 `MemoryCacheService` 对应 partial 文件中补实现。
3. 在 `RedisCacheService` 对应 partial 文件中补实现。
4. 如涉及序列化，复用 `ToJson` / `ToObject<T>`。
5. 如涉及依赖注入切换，更新 `AddCache`。
6. 构建并验证两个实现都可编译。

## 设计倾向

1. 接口先行，双实现对齐。
2. 包装底层能力，而不是在缓存层写策略引擎。
3. 保持扩展方法与注入入口简单可读。