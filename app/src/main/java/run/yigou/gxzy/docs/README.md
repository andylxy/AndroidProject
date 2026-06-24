# 项目文档索引

本目录包含 AndroidProject 项目的所有技术文档，按主题分类组织。

## 📂 文档分类

### 🏗️ architecture/ - 架构设计文档

| 文档 | 说明 |
|------|------|
| [ui-module-migration-plan.md](architecture/ui-module-migration-plan.md) | UI 模块迁移计划（914 行） |
| [package-restructure-plan.md](architecture/package-restructure-plan.md) | 包结构重构计划（176 行） |

---

### 💾 database/ - 数据库相关文档

| 文档 | 说明 |
|------|------|
| [DatabaseMigrationImplementationGuide.md](database/DatabaseMigrationImplementationGuide.md) | 数据库迁移实施指南（135 行） |
| [GreenDaoStepByStepGuide.md](database/GreenDaoStepByStepGuide.md) | GreenDao 逐步使用指南（133 行） |
| [GreenDaoUpgradeGuide.md](database/GreenDaoUpgradeGuide.md) | GreenDao 升级指南（79 行） |
| [GreenDaoQuickUsage.md](database/GreenDaoQuickUsage.md) | GreenDao 快速使用（31 行） |

---

### 🔒 security/ - 安全相关文档

| 文档 | 说明 |
|------|------|
| [防重放攻击集成指南.md](security/防重放攻击集成指南.md) | 防重放攻击集成指南（244 行） |
| [anti-replay-client-guide.md](security/anti-replay-client-guide.md) | 防重放攻击客户端指南（146 行） |
| [防重放攻击升级说明.md](security/防重放攻击升级说明.md) | 防重放攻击升级说明（59 行） |
| [SM2使用详细教程.md](security/SM2使用详细教程.md) | SM2 加密使用教程（263 行） |

---

### 🧩 component/ - 组件使用指南

| 文档 | 说明 |
|------|------|
| [TipsTextRenderConfig配置中心使用指南.md](component/TipsTextRenderConfig配置中心使用指南.md) | TipsTextRenderConfig 配置中心完整使用指南（含 ContentTypes 类型系统）（800 行） |

---

### ⚡ optimization/ - 优化总结

| 文档 | 说明 |
|------|------|
| [TipsSingleData模块优化总结.md](optimization/TipsSingleData模块优化总结.md) | TipsSingleData 模块优化总结（643 行） |
| [TitleBar优化指南.md](optimization/TitleBar优化指南.md) | TitleBar 优化指南（82 行） |
| [内容类型系统统一优化总结.md](optimization/内容类型系统统一优化总结.md) | ContentTypes 类型系统统一优化总结（290 行） |

---

### 📋 plans/ - 计划/方案

| 文档 | 说明 |
|------|------|
| [comment-repair-plan.md](plans/comment-repair-plan.md) | 注释修复计划（76 行） |

---

## 📊 统计信息

- **文档总数**：15 个
- **总行数**：约 4,100+ 行
- **分类数**：6 个

---

## 🔍 快速查找

### 按主题查找

- **架构重构** → `architecture/`
- **数据库/GreenDao** → `database/`
- **安全/加密** → `security/`
- **组件使用** → `component/`
- **性能优化** → `optimization/`
- **开发计划** → `plans/`

### 按关键词查找

| 关键词 | 文档位置 |
|--------|----------|
| GreenDao | `database/GreenDao*.md` |
| 防重放攻击 | `security/防重放*.md` |
| SM2 加密 | `security/SM2使用详细教程.md` |
| 配置中心 | `component/TipsTextRenderConfig配置中心使用指南.md` |
| UI 迁移 | `architecture/ui-module-migration-plan.md` |
| TitleBar | `optimization/TitleBar优化指南.md` |

---

## 📝 文档规范

### 命名规范

- 使用英文文件名（除特定中文主题）
- 使用驼峰命名或连字符分隔
- 文件名应清晰表达文档内容

### 分类原则

- **architecture/**：架构设计、模块迁移、包结构重构
- **database/**：数据库相关、ORM 框架使用
- **security/**：安全机制、加密算法、防攻击
- **component/**：组件使用指南、API 文档
- **optimization/**：性能优化、代码重构总结
- **plans/**：开发计划、修复方案

### 新增文档

新增文档时，请：
1. 选择合适的分类目录
2. 更新本索引文件
3. 遵循命名规范

---

## 🔄 更新日志

| 日期 | 说明 |
|------|------|
| 2026-06-23 | 新增内容类型系统优化总结文档 |
| 2026-06-23 | 更新配置中心文档：添加 ContentTypes 类型系统章节 |
| 2026-06-23 | 初始版本，整理所有文档到统一目录 |

---

**维护者**：开发团队  
**最后更新**：2026-06-23
