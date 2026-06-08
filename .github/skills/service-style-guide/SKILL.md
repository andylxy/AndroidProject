---
name: service-style-guide
description: "Use when creating, updating, or reviewing service-layer database code in vol.api.sqlsugar/ZhongYi/Services or vol.api.sqlsugar/SimpleEasy.System/Services. 适用于业务服务类、Partial Service 扩展、数据库查询与写入、缓存一致性、事务处理、树形查询。关键词：ServiceBase、Partial Service、DbContext.Queryable、SimpleCacheService、DbContextBeginTransaction、ZhongYi Services、SimpleEasy.System Services。"
argument-hint: "描述你要新增或修改的服务、所属模块、数据库读写逻辑、缓存逻辑、事务逻辑或 Partial 拆分方式"
user-invocable: true
---
# Service Style Guide

## 这个 Skill 解决什么问题

这个 Skill 用于统一处理 `vol.api.sqlsugar/ZhongYi/Services` 与 `vol.api.sqlsugar/SimpleEasy.System/Services` 下的服务层数据库实现。两者业务落位不同，但核心写法一致，都是围绕 `ServiceBase<T>`、`Partial` 拆分、SqlSugar 查询、缓存回填与写后清缓存展开。

重点覆盖：

1. 新增或修改业务服务类。
2. 在 `Partial` 文件中扩展查询、聚合、分页、树形与写入逻辑。
3. 基于 `DbContext.Queryable<T>()`、`FindAsIQueryable`、`GetListAsync` 编写数据库读取。
4. 按现有风格处理缓存命中、缓存回填、缓存失效。
5. 在跨表写入、同步、差异更新时使用事务提交。

## 项目现有统一风格摘要

1. 服务主类通常很薄，只保留 `ServiceBase<T>` 继承、接口实现、`IDependency` 和少量静态入口。
2. 复杂业务逻辑优先拆分到 `Partial` 文件，而不是堆积在主类中。
3. 读取逻辑普遍直接使用 `DbContext.Queryable<T>()`，并混合使用 `Find`、`FindAsIQueryable`、`WhereIF`、`Includes`、匿名投影。
4. 高频或代价较高的查询普遍采用“先查 `SimpleCacheService`，未命中再查库，最后回填缓存”。
5. 写操作后的缓存一致性通常通过覆写 `Add`、`Update`、设置 `AddOnExecuting`、`UpdateOnExecuting`、`UpdateOnExecuted` 或专门 `ClearAllCaches` 方法保证。
6. 如果写操作跨多表、包含同步或差异比对，优先通过 `DbContextBeginTransaction` 统一提交，再 `SaveChanges()`、清缓存、必要时发布事件。
7. 查询返回给前端时，仓库允许直接投影成匿名对象或轻量 DTO，不强制每一步都新建类型。

## ZhongYi 与 System 的局部差异

1. `ZhongYi/Services` 更偏业务聚合，允许更直接的前端数据拼装和多表匿名投影。
2. `SimpleEasy.System/Services` 更偏基础能力，通常更强调构造函数注入、缓存一致性、事务边界和职责拆分。
3. 合并 skill 后，判断标准不再是“用不同 skill”，而是“在同一 skill 中根据模块类型套用对应约束”。

## 什么时候使用

当用户的问题包含这些关键词或意图时，应优先使用这个 Skill：

1. 新增服务类
2. 新增 Partial Service
3. 新增数据库查询服务
4. 查询后加缓存
5. 写后清缓存
6. ServiceBase 扩展
7. 事务型数据库写入
8. ZhongYi Services
9. SimpleEasy.System Services
10. 树形接口
11. 分页扩展
12. 资源同步或批量写入

## 标准处理流程

1. 先判断服务属于业务域模块还是系统基础模块。
2. 创建或确认主服务类存在，主类保持极薄。
3. 业务实现优先放到 `Partial/<ServiceName>.cs` 或职责化 partial 文件中。
4. 读操作优先决定是否需要缓存；如果需要，先查缓存，miss 后查库，再回填缓存。
5. 查询优先使用 `DbContext.Queryable<T>()`、`FindAsIQueryable`、`GetListAsync`。
6. 如果方法直接服务前端返回，允许直接匿名投影。
7. 写操作如涉及多表或同步，优先走 `DbContextBeginTransaction`。
8. 写成功后统一处理 `SaveChanges()`、清缓存、必要时发事件。

## 按模块使用时的附加约束

### 用于 ZhongYi/Services 时

1. 允许更务实的聚合查询与前端数据拼装。
2. 不要为了简单查询强行引入过度抽象的 Repository-only 写法。
3. 常见缓存键与业务字段拼装可直接保留在服务中。

### 用于 SimpleEasy.System/Services 时

1. 更强调构造函数注入和职责化 partial 拆分，如 `Auth`、`Crud`、`QueryCache`、`Sync`。
2. 更强调缓存一致性，新增读缓存时必须同步考虑清缓存路径。
3. 跨多表写入或同步逻辑不要绕开 `DbContextBeginTransaction`。

## 本项目中的关键定位点

1. ZhongYi 主服务模板参考 `vol.api.sqlsugar/ZhongYi/Services/ZhongYao/ZhongYaoService.cs`。
2. ZhongYi 聚合查询参考 `vol.api.sqlsugar/ZhongYi/Services/ZhongYao/Partial/ZhongYaoService.cs`。
3. ZhongYi 树形与缓存失效参考 `vol.api.sqlsugar/ZhongYi/Services/BookInfo/Partial/BookInfoService.cs`。
4. System 权限缓存与事务参考 `vol.api.sqlsugar/SimpleEasy.System/Services/Sys_ApiPermissions/Partial/Sys_ApiPermissionsService.cs`。
5. System 资源同步事务参考 `vol.api.sqlsugar/SimpleEasy.System/Services/Sys_ApiPermissions/Partial/Sys_ApiPermissionsService.ResourceSync.cs`。
6. System 用户缓存参考 `vol.api.sqlsugar/SimpleEasy.System/Services/System/Partial/SysUserService.QueryCache.cs`。
7. System 登录缓存参考 `vol.api.sqlsugar/SimpleEasy.System/Services/System/Partial/SysUserService.Auth.cs`。

## 具体约束

1. 不要把复杂服务实现写成一个超长单文件，优先 partial 拆分。
2. 不要新增读缓存却忘记写路径的清缓存。
3. 不要跨多表写入却绕开事务包装。
4. 不要把控制器职责塞进服务，但允许服务保留面向前端的必要数据拼装。
5. 不要在服务层重复发明通用 SqlSugar 或缓存基础能力，优先复用既有基础设施。
6. 该风格也可作为 `SimpleEasy.MAF.AI` 中数据/缓存基础设施实现的参考风格，但不意味着基础设施层应继承 `ServiceBase`。

## 新增服务实现的最短步骤

1. 新建服务主类，继承 `ServiceBase<TEntity>`，实现接口与 `IDependency`。
2. 新建一个或多个 `Partial` 文件承载具体业务逻辑。
3. 查询优先用 `DbContext.Queryable<T>()` 与 `FindAsIQueryable`。
4. 高频读取加 `SimpleCacheService.Get/Set/HashAdd` 等缓存访问。
5. 写操作后通过覆写钩子或专门方法清缓存。
6. 如果涉及同步、差异更新或多表写入，放进 `DbContextBeginTransaction`。
7. 构建并验证返回结构、缓存键、事务边界和模块落位是否符合当前仓库习惯。
