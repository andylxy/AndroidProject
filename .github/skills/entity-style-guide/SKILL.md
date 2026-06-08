---
name: entity-style-guide
description: "Use when creating, updating, or reviewing SimpleEasy.Entity code. 适用于新增 DomainModels、partial 实体、MesFlow 扩展模型、SqlSugar 注解映射、Tenant/Entity 特性、BaseEntityTemplate 继承、导航属性、展示扩展类。关键词：SimpleEasy.Entity、实体类、DomainModels、SugarTable、Entity 特性、Tenant、MesFlow Ext。"
argument-hint: "描述你要新增或修改的实体、所属模块、是否为主表/明细表/扩展展示模型"
user-invocable: true
---

# Entity Style Guide

## 这个 Skill 解决什么问题

这个 Skill 用于处理当前仓库 `SimpleEasy.Entity` 项目中的实体建模与扩展对象创建，重点覆盖以下场景：

1. 新增 `DomainModels` 下的数据库实体。
2. 新增 `partial` 目录中的主表/明细表模型。
3. 新增 `MesFlow` 风格的展示扩展对象或参数对象。
4. 补充 `Entity`、`SugarTable`、`Tenant`、`Navigate` 等特性映射。
5. 保持实体注解、命名、注释、继承体系与仓库现有风格一致。

## 项目现有风格摘要

1. 实体普遍放在 `SimpleEasy.Entity/DomainModels/**`，并使用 `Entity` + `SugarTable` + `Tenant` 组合描述表信息。
2. 主实体通常继承 `BaseEntityTemplate<T>`，部分属性会 `override` 基类字段，如 `OrgId`、`ParentId`、`IsEnable`。
3. 属性上大量使用 `[Display]`、`[Editable]`、`[Required]`、`[MaxLength]`、`[Column]`、`[SugarColumn]`，中英文元数据都偏完整。
4. 明细关系一般通过 `DetailTable` 元数据、`[ForeignKey]` 和 `[Navigate]` 组合定义。
5. `MesFlow` 目录下存在大量“扩展类/参数类”，用于业务展示或服务方法参数，不一定直接映射数据库表。
6. 注释风格以 XML 注释为主，说明偏业务中文，不追求极简。

## 什么时候使用

当用户的问题包含下面这些关键词或意图时，应优先使用这个 Skill：

1. 新建实体类
2. 新增 DomainModels
3. 新增 SqlSugar 映射
4. 新增主表明细表
5. 新增 Tenant 实体
6. 新增 MesFlow 扩展类
7. 新增参数对象 DTO 但要保持 Entity 项目风格
8. 调整 `SugarColumn` / `Display` / `Entity` 特性
9. 为 Entity 项目补字段、补导航属性

## 标准处理流程

1. 先确认目标是数据库实体，还是业务扩展对象。
2. 如果是数据库实体，优先放入 `DomainModels/<模块>` 或 `DomainModels/<模块>/partial`。
3. 先确定继承基类，优先复用 `BaseEntityTemplate<T>` 或现有实体父类。
4. 按现有模式补齐 `Entity`、`SugarTable`、`Tenant` 特性。
5. 为每个字段补业务中文注释和 `SugarColumn` 元数据。
6. 如有关联子表，使用 `[ForeignKey]` + `[Navigate]`。
7. 如只是页面展示扩展，不要强行加表映射特性，优先参考 `MesFlow/*Ext.cs`。

## 本项目中的关键定位点

1. 全局 using 风格参考 `SimpleEasy.Entity/GlobalUsing.cs`。
2. 标准实体参考 `SimpleEasy.Entity/DomainModels/ZhongYi/BookInfo.cs`。
3. 带明细导航的实体参考 `SimpleEasy.Entity/DomainModels/ZhongYi/partial/AiConfig.cs`。
4. MesFlow 展示扩展参考 `SimpleEasy.Entity/MesFlow/MocTaInvExt.cs`。
5. 参数对象参考 `SimpleEasy.Entity/MesFlow/ValidateCheckParameter.cs`。

## 具体约束

1. 不要把数据库实体写成纯 POCO 而缺失 `SugarTable` / `SugarColumn` 元数据，除非它明确不是表映射对象。
2. 不要把服务层逻辑放进实体类。
3. 不要随意改动基类继承体系；若已有通用字段在基类中，优先 `override`，不要重复发明新字段名。
4. 不要把 `MesFlow` 扩展类错误放进 `DomainModels` 表映射目录。
5. 字段注释、`Display(Name = ...)`、`SugarColumn(ColumnDescription = ...)` 尽量保持一致，避免名称漂移。

## 创建数据库实体的最短步骤

1. 选择所属模块目录，如 `DomainModels/ZhongYi/partial`。
2. 定义类并继承合适基类，通常为 `BaseEntityTemplate<T>`。
3. 添加 `[Entity]`、`[SugarTable]`、`[Tenant]`。
4. 逐个字段补 `Display`、`Editable`、`SugarColumn`。
5. 如有字符串长度约束，补 `MaxLength` 与 `Column(TypeName = ...)`。
6. 如有导航关系，补 `ForeignKey` 和 `Navigate`。
7. 构建依赖项目并检查生成表、导航、命名是否与仓库一致。

## 新增 MesFlow 扩展类的最短步骤

1. 放到 `SimpleEasy.Entity/MesFlow/`。
2. 如果它是某实体的展示扩展，直接继承原实体。
3. 只补业务展示字段，不要附加数据库映射特性。
4. 保持中文 XML 注释与当前 MesFlow 文件风格一致。
5. 如果是参数对象，允许直接组合多个实体对象作为属性。