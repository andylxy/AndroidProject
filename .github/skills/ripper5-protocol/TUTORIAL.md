# Hermes Agent 与 RIPER-5 协议使用教程

> 备忘文档：Hermes Agent 的行为准则、模式协议、文件职责划分及使用方法。

---

## 一、核心概念速查

### 1.1 两个关键文件

| 文件 | 角色 | 加载方式 | 作用域 | 类比 |
|------|------|----------|--------|------|
| `SOUL.md` | 智能体的"性格" | **自动加载**（会话启动时） | 全局（所有项目） | 一个人的性格和价值观 |
| `AGENTS.md` | 项目的"操作手册" | **手动加载**（用户要求时） | 项目级（特定项目） | 一家公司的员工手册 |

### 1.2 位置

| 文件 | 路径 |
|------|------|
| `SOUL.md` | `~/.hermes/profiles/<profile>/SOUL.md` |
| `AGENTS.md` | 项目根目录 `/path/to/project/AGENTS.md` |

### 1.3 内容分工

| 规则类型 | 定义位置 | 说明 |
|----------|----------|------|
| 模式定义（RESEARCH/PLAN/EXECUTE 等） | `SOUL.md` | 全局通用，自动加载 |
| 开发流程协议（五阶段流程） | `SOUL.md` | 全局通用，自动加载 |
| 查证与澄清规则 | `SOUL.md` | 全局通用，自动加载 |
| 禁止行为（通用） | `SOUL.md` | 全局通用，自动加载 |
| 项目目录约定 | `AGENTS.md` | 项目特定，需显式引用 |
| 模块特定规则（如 MAF 日志规则） | `AGENTS.md` | 项目特定，需显式引用 |

---

## 二、SOUL.md — 智能体角色定义

### 2.1 定位

SOUL.md 定义了 AI 助手的**身份、性格和工作方式**，是全局通用的行为准则。

### 2.2 核心内容

```
角色定位 → 资深后端开发专家
开发流程 → 五阶段流程（RESEARCH → INNOVATE → PLAN → EXECUTE → REVIEW）
模式定义 → 5 种模式的允许/禁止/要求
查证规则 → 来源查证优先级、查证原则
行为准则 → 先研究再改动、边界控制、注释要求、日志规范
禁止行为 → 不得使用未经验证的依赖、不得修改不相关代码等
```

### 2.3 加载机制

```
会话启动 → SOUL.md 自动加载到系统提示（system prompt）
→ AI 知道"我是谁、怎么工作"
→ 无需用户额外操作
```

---

## 三、AGENTS.md — 项目特定约束

### 3.1 定位

AGENTS.md 定义了**特定项目的操作规范**，包括目录约定、编码规范、模块特定规则等。

### 3.2 核心内容

```
项目目录约定 → 哪里放什么代码
文件落位规则 → 新增文件放哪里
编码规范 → 日志用哪个入口、命名约定
模块特定规则 → 如 MAF 模块必须用 MAFLogProvider
技能目录规则 → .github/skills/ 的维护规则
```

### 3.3 加载机制

```
会话启动 → SOUL.md 自动加载（全局规则）
用户说"遵循项目 AGENTS.md 规则" → 读取 AGENTS.md（项目特定规则）
→ 合并两套规则 → 按规则执行
```

> **重要**：AGENTS.md 不会自动加载，需要用户显式要求或技能引用。

---

## 四、RIPER-5 模式协议

### 4.1 五大模式

| 模式 | 目的 | 允许 | 禁止 |
|------|------|------|------|
| `RESEARCH` | 信息收集和深入理解 | 阅读文件、提问、分析 | 建议、规划、实施 |
| `INNOVATE` | 探索候选方案与权衡 | 讨论多路径、比较权衡 | 具体规划、写代码 |
| `PLAN` | 创建技术规范 | 精确路径、签名、验证策略 | 实施、示例代码 |
| `EXECUTE` | 实施已批准计划 | 严格按清单执行 | 偏离计划、创造性添加 |
| `REVIEW` | 验证实施一致性 | 逐项核对、标记偏差 | — |

### 4.2 模式切换规则

```
只有明确的切换信号才能切换模式：
- "进入研究模式" → [MODE: RESEARCH]
- "进入创新模式" → [MODE: INNOVATE]
- "进入计划模式" → [MODE: PLAN]
- "进入执行模式" → [MODE: EXECUTE]
- "进入审查模式" → [MODE: REVIEW]

无明确切换信号时，保持当前模式。
新对话默认处于 RESEARCH 模式。
```

### 4.3 模式切换信号

**用户指令是自然语言指令，不是 Hermes 内置命令。**

| 指令 | 作用 | 示例 |
|------|------|------|
| 模式切换 | 触发模式切换 | "进入计划模式" |
| 目标设定 | 设定长期目标 | `/goal [text]` |
| 会话控制 | 新建/恢复会话 | `/reset`, `/resume` |

### 4.4 工作流程

```
用户说"进入计划模式"
    ↓
AI 读取 SOUL.md 中 PLAN 模式的规则
    ↓
切换到 [MODE: PLAN]
    ↓
只输出规范，不实施，不写代码
    ↓
结尾给出实施清单
```

---

## 五、GitHub Copilot vs Hermes Agent

### 5.1 协议对比

| 功能 | GitHub Copilot | Hermes Agent | 实现方式 |
|------|---------------|-------------|----------|
| 模式声明 | ✅ RIPER-5 内置 | ⚠️ 需约定 | `SOUL.md` + 用户指令 |
| 项目级准则 | ✅ `copilot-instructions.md` | ✅ `AGENTS.md` | 自动/手动加载 |
| 可复用流程 | ✅ `.github/skills/` | ✅ `skills/` | `hermes skills` |
| 持久记忆 | ❌ | ✅ `memory` | `hermes memory` |
| 子代理协作 | ❌ | ✅ `delegate_task` | 内置工具 |
| 定时任务 | ❌ | ✅ `cronjob` | `hermes cron` |
| 多模型支持 | ❌ | ✅ 20+ 提供商 | `hermes model` |
| 多平台 | ❌ VS Code 独占 | ✅ CLI + 10+ 平台 | `gateway` |
| 安全审批 | ⚠️ 基础 | ✅ `approvals.mode` | 配置项 |
| 自进化 | ❌ | ✅ 自动保存技能 | `curator` |

### 5.2 Hermes 的优势

1. **技能自进化**：自动将成功的工作流保存为 skill
2. **跨平台**：CLI、Telegram、Discord、Slack 等多平台
3. **多模型支持**：20+ LLM 提供商，随时切换
4. **多 Profile**：每个 profile 独立配置、技能、记忆
5. **MCP 集成**：支持 Model Context Protocol 扩展工具

---

## 六、使用教程

### 6.1 首次设置

```bash
# 1. 确保 SOUL.md 存在（~/.hermes/profiles/coder/SOUL.md）
#    这是全局角色定义，自动加载

# 2. 确保 AGENTS.md 存在（项目根目录）
#    这是项目特定规则，需显式引用

# 3. 确保 RIPER-5 技能已安装
hermes skills install ripper5-protocol

# 4. 设置审批模式为 manual（与 RIPER-5 一致）
hermes config set approvals.mode manual

# 5. 启动 Hermes 并加载技能
hermes -s ripper5-protocol
```

### 6.2 日常使用流程

#### 场景 A：分析项目结构

```
用户：分析 Vue.NetCore 项目的 Agent 模块架构

→ Hermes 自动进入 [MODE: RESEARCH]
→ 读取文件、分析结构、输出观察
→ 结尾提示：进入创新模式
```

#### 场景 B：制定技术方案

```
用户：进入创新模式，为 Agent 模块设计一个新的技能系统

→ Hermes 进入 [MODE: INNOVATE]
→ 输出多种方案、比较优缺点
→ 结尾提示：方案收敛则进入计划模式
```

#### 场景 C：实施代码修改

```
用户：进入计划模式，实施方案 A

→ Hermes 进入 [MODE: PLAN]
→ 输出精确文件路径、函数签名、实施清单
→ 结尾提示：如确认方案，输入"进入执行模式"

用户：进入执行模式

→ Hermes 进入 [MODE: EXECUTE]
→ 按清单顺序执行，标记完成状态
→ 发现偏差自动回退到 PLAN

用户：进入审查模式

→ Hermes 进入 [MODE: REVIEW]
→ 逐项核对，输出偏差报告
```

### 6.3 关键命令速查

| 命令 | 作用 | 对应模式 |
|------|------|----------|
| `/goal [text]` | 设定长期目标 | 所有模式 |
| `/reset` | 新建会话 | 重置为 RESEARCH |
| `/undo` | 撤销上一步 | 执行模式回退 |
| `/rollback [N]` | 回滚文件系统 | 执行模式回退 |
| `/status` | 查看当前状态 | 所有模式 |
| `/help` | 查看所有命令 | — |
| `/skill [name]` | 加载技能 | — |
| `/curator [sub]` | 技能维护 | — |

### 6.4 配置参考

```yaml
# ~/.hermes/config.yaml

approvals:
  mode: manual          # manual/smart/off — 与 RIPER-5 授权原则对应

agent:
  max_turns: 90         # 单次会话最大轮数

display:
  show_reasoning: true  # 显示推理过程，便于 REVIEW 阶段核对

security:
  redact_secrets: true  # 工具输出中隐藏敏感信息
```

---

## 七、最佳实践

### 7.1 模式使用建议

| 任务类型 | 推荐模式 | 理由 |
|----------|----------|------|
| 简单问答 | `RESEARCH` | 直接回应，无需完整流程 |
| 架构设计 | `INNOVATE` → `PLAN` | 先探索方案，再制定规范 |
| 代码修改 | `PLAN` → `EXECUTE` → `REVIEW` | 严格遵循计划，验证一致性 |
| 缺陷修复 | `RESEARCH` → `PLAN` → `EXECUTE` | 先定位根因，再修复 |
| 代码审查 | `REVIEW` | 逐项核对 |

### 7.2 避免常见错误

| 错误 | 正确做法 |
|------|----------|
| 跳过 PLAN 直接进入 EXECUTE | 即使任务简单，也先输出简要计划 |
| 中途改变模式 | 只有明确的切换信号才能切换 |
| 自行扩大修改范围 | 用户限定边界时严格遵守 |
| 遗漏文档同步 | 代码改动必须同步更新文档 |
| 忽略偏差 | REVIEW 中发现任何偏差都要标记 |
| 认为 AGENTS.md 会自动加载 | 需要用户显式要求或技能引用 |

### 7.3 技能管理

```bash
# 查看已安装技能
hermes skills list

# 搜索技能
hermes skills search ripper5

# 加载技能到当前会话
/skill ripper5-protocol

# 更新技能
hermes skills update

# 发布自定义技能
hermes skills publish /path/to/skill
```

---

## 八、故障排查

### 问题：模式不生效

**原因**：SOUL.md 未被正确加载或用户指令未触发
**解决**：
```bash
# 1. 确认 SOUL.md 存在
ls ~/.hermes/profiles/coder/SOUL.md

# 2. 重启 Hermes 会话
/reset

# 3. 手动加载技能
/skill ripper5-protocol
```

### 问题：审批总是通过

**原因**：`approvals.mode` 设置为 `off`
**解决**：
```bash
hermes config set approvals.mode manual
```

### 问题：技能未自动加载

**原因**：技能未安装或路径错误
**解决**：
```bash
hermes skills install ripper5-protocol
hermes skills list | grep ripper5
```

### 问题：AGENTS.md 规则未生效

**原因**：AGENTS.md 不会被自动加载
**解决**：
```
用户在对话中说："请遵循项目根目录的 AGENTS.md 规则"
→ AI 读取 AGENTS.md → 应用项目特定规则
```

---

## 九、文件清单

| 文件 | 路径 | 用途 |
|------|------|------|
| `SOUL.md` | `~/.hermes/profiles/coder/SOUL.md` | 全局角色定义（自动加载） |
| `AGENTS.md` | `/mnt/d/git/web/Vue.NetCore/AGENTS.md` | 项目特定规则（需显式引用） |
| `SKILL.md` | `~/.hermes/profiles/coder/skills/ripper5-protocol/SKILL.md` | RIPER-5 协议技能 |
| `README.md` | `.github/skills/ripper5-protocol/README.md` | 完整使用教程 |

---

## 十、快速参考卡

### 模式切换信号

```
进入研究模式    → [MODE: RESEARCH]
进入创新模式    → [MODE: INNOVATE]
进入计划模式    → [MODE: PLAN]
进入执行模式    → [MODE: EXECUTE]
进入审查模式    → [MODE: REVIEW]
```

### 文件职责速查

```
SOUL.md  → 全局角色（自动加载）→ "我是谁、怎么工作"
AGENTS.md → 项目规则（手动加载）→ "这个项目有什么特殊规则"
```

### 工作流程速查

```
会话启动 → SOUL.md 自动加载
用户说"进入计划模式" → 切换到 [MODE: PLAN]
用户说"遵循项目 AGENTS.md 规则" → 读取 AGENTS.md
→ 合并两套规则 → 按规则执行
```

---

*文档版本：1.0 | 最后更新：2026-05-08*
