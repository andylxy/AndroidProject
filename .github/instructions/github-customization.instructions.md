---
description: "Use when modifying .github skills, agents, prompts, instructions, template assets, or their documentation and references. Covers customization consistency and synchronized updates."
name: "GitHub Customization Consistency"
applyTo: [".github/**"]
---

# GitHub Customization Consistency

- 当改动涉及 `.github/skills/`、`.github/agents/`、模板资产、示例入口、脚手架、技能引用链路或自定义指令文件时，必须同步检查对应说明文档是否仍然准确。
- 若修改了技能触发条件、技能职责、目录结构、模板输入输出、示例代码或约定命名，必须同步更新对应的 `SKILL.md`、同目录 `assets/README.md`、`references/*.md` 以及任何被直接引用的模板或说明文件。
- 若修改了 agents、instructions、prompts 的目录职责、发现规则或使用方式，也必须同步检查相邻说明文档是否与当前实现一致。
- 技能说明中的关键词、触发描述、目录路径、模板名称和示例调用方式，必须与实际代码和目录结构保持一致。
- 不得只更新运行时代码、模板或规则文件，而不更新与之直接相关的技能说明、模板说明或示例文档。
- 若某个技能目录已包含模板、引用文档或可复用资产，但缺少必要说明文档，则在本次改动直接影响该技能时，应补充最小说明，覆盖用途、触发条件、资产职责和维护边界。
- 检查范围仅限于本次改动直接影响的技能、模板和文档，不要求做无边界全量扫描。