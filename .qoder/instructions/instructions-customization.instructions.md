---
description: "涉及修改 Qoder 指令文件、AGENTS.md、.qoder/ 目录或其说明文档时加载本文件。覆盖指令一致性。"
name: "Instructions Customization Consistency (Qoder)"
applyTo: [".qoder/**", "AGENTS.md"]
---

# Instructions Customization Consistency

- 当改动涉及 `AGENTS.md`、`.qoder/instructions/` 或自定义指令文件时，必须同步检查引用链路和说明文档是否仍然准确。
- 若修改了指令文件的目录职责、触发条件或使用方式，必须同步检查 `AGENTS.md` 中的引用路径是否仍然正确。
- 若修改了专项规则文件的内容或文件名，必须同步更新 `AGENTS.md` 中对应的引用链接。
- 指令说明中的关键词、触发描述、目录路径和示例调用方式，必须与实际实现和目录结构保持一致。
- 不得只更新指令规则文件，而不更新与之直接相关的 `AGENTS.md` 引用或说明。
- 检查范围仅限于本次改动直接影响的指令文件和文档，不要求做无边界全量扫描。
