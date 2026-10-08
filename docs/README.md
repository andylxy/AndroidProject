# 项目文档索引

本目录包含 AndroidProject 项目的所有技术文档，按主题分类组织。

## 📂 文档分类

### 🏗️ architecture/ - 架构设计文档

| 文档 | 说明 |
|------|------|
| [ui-module-migration-plan.md](architecture/archive/ui-module-migration-plan.md) | UI 模块迁移计划（914 行） |
| [package-restructure-plan.md](architecture/package-restructure-plan.md) | 包结构重构计划（176 行） |

---

### 💾 database/ - 数据库相关文档

| 文档 | 说明 |
|------|------|
| [greendao-hardening-plan.md](database/greendao-hardening-plan.md) | **GreenDAO 加固方案（唯一设计文档）**——现状事实基线、风险登记与加固项、决策记录、验证方法、实施清单与边界、术语表、变更历史。**只讲 GreenDAO**，不含任何其他存储方案的评估 |
| [greendao-guide.md](database/greendao-guide.md) | **GreenDAO 使用与升级指南**——初始化链、三个版本号、新增/修改实体、升级流程、排障速查（原 4 份文档合并重写） |

> **两份文档的分工**：`greendao-guide.md` 回答"**怎么操作**"（面向日常开发）；
> `greendao-hardening-plan.md` 回答"**为什么这么定 + 要改什么**"（设计文档）。
> 两者**各自可独立阅读**；交叉引用只作定位补充。

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

- **本索引收录文档数**：13 个
- **分类数**：6 个

> 注：本索引只收录"需要外部维护者理解"的文档；各模块内还有零散的说明文件未逐一登记。

---

## 🔍 快速查找

### 按主题查找

- **架构重构** → `architecture/`
- **数据库 / GreenDAO** → `database/`（设计文档 + 操作指南各一份）
- **安全/加密** → `security/`
- **组件使用** → `component/`
- **性能优化** → `optimization/`
- **开发计划** → `plans/`

### 按关键词查找

| 关键词 | 文档位置 |
|--------|----------|
| GreenDAO 用法 / 升级排障 / 新增实体 | `database/greendao-guide.md` |
| 加固方案 / 表集合一致性 / 启动失败隐患 | `database/greendao-hardening-plan.md` §3.1 |
| 本地库现状 / 风险登记 / 事实基线 | `database/greendao-hardening-plan.md` §2、§3 |
| 术语（版本号 / RC4 / smartMigrate / 门面 / 后台入口） | `database/greendao-hardening-plan.md` §9 |
| 防重放攻击 | `security/防重放*.md` |
| SM2 加密 | `security/SM2使用详细教程.md` |
| 配置中心 | `component/TipsTextRenderConfig配置中心使用指南.md` |
| UI 迁移 | `architecture/archive/ui-module-migration-plan.md` |
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

### 一致性要求

- **不得描述不存在的类 / 方法 / Gradle 任务**：写文档前先确认该类确实存在于
  `app/src/main/java/run/yigou/gxzy/` 下（`database/` 下曾出现 `AutoMigrationHelper`、
  `MigrationHelper`、`migrateByVersion`、`greendaoGenerate` 等已不存在的写法，2026-10-06 已清理）。
- **文件链接必须指向真实路径**：统一用相对路径，不要写绝对路径（曾出现
  `D:/git/app/AndroidProject/...` 这类漏了 `-old` 的失效链接）。
- **`文件:行` 引用要标注快照**：被引用的源文件若有未提交改动，行号会漂——
  这类引用应同时给出**符号名**（类 / 方法），行号只作定位提示。
- **不要为同一主题建多份互相引用的文档**：设计文档必须**自足**。
  需要拆分时按"读者与用途"拆（例如"设计"与"操作指南"），而不是按章节拆。
- **只写当前方向的内容**：不把**其他技术方案**的评估塞进设计文档。
  若某方案未被采纳且无推进计划，就不要在文档里保留它的评估、对比与触发条件
  （2026-10-06 已据此清理 `database/` 下的一份此类内容）。
  **注意区分**：这不禁止写"已排除项"（防止后人重复调研）与"记录在案但不修"（防止误以为遗漏）——
  它们讲的是**本方案范围内**的取舍，不是别的方案。

---

## 🔄 更新日志

| 日期 | 说明 |
|------|------|
| 2026-10-06 | `database/greendao-hardening-plan.md` **移除全部非 GreenDAO 内容**（此前保留的另一种 ORM 的迁移评估、对比与触发条件整节删除），收敛为纯 GreenDAO 加固方案 |
| 2026-10-06 | `database/` 收敛为**两份**：原 7 份互相引用的文档（子目录索引 1 份 + 加固方案 + 现状与风险 + 决策记录 2 份 + 预研 1 份 + 术语表 1 份）**合并为一份自足的设计文档** `greendao-hardening-plan.md`；操作指南保留为 `greendao-guide.md` |
| 2026-10-06 | `database/` 原 4 份文档（QuickUsage / StepByStep / UpgradeGuide / MigrationImplementationGuide）**合并重写**为 `greendao-guide.md` |
| 2026-06-23 | 新增内容类型系统优化总结文档 |
| 2026-06-23 | 更新配置中心文档：添加 ContentTypes 类型系统章节 |
| 2026-06-23 | 初始版本，整理所有文档到统一目录 |
