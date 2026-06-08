---
description: "Use when creating, moving, or renaming files and directories, or when deciding where new code, docs, templates, tests, or examples should live."
name: "File Placement"
---

# File Placement

- 新建文件前，先判断其属于业务代码、基础设施代码、配置、说明文档、技能文档、模板资产、测试还是示例。
- 新文件优先放入职责匹配的现有目录，不得随意创建语义模糊的新目录或散落到无关位置。
- 若新增的是模块级共享基础设施能力文件，应优先放入该模块既有 `Shared/` 目录，并保持模块归属清晰。
- 若现有目录已有明确归类和命名约定，必须优先复用，例如 `.github/skills/<name>/`、`.github/agents/`、`docs/`、模块目录 README 等。
- 只有现有结构无法合理承载时，才允许新增目录，并应明确该目录的职责边界。
- 文件名必须与职责、目录和现有命名风格一致，不得使用临时名、歧义名、泛化名或随意缩写。
- 文件名应能直接表达职责；对于桥接层、共享入口、适配层等基础设施文件，必须与现有命名风格保持一致。
- 对 skills、agents、prompts、instructions、README、模板资产等约定文件，优先使用现有约定名称，不得自创平行命名。
- 若目录中已存在同职责文档或模板说明，应优先更新现有文件，而不是新建近似重复文件。
- 不得在模块内部为同一职责额外创建平行日志入口、平行共享目录或重复封装。