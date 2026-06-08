# Hermes Agent 使用 RIPER-5 协议教程

> 本教程说明如何在 Hermes Agent 中实现与 GitHub Copilot 等效的 RIPER-5 模式协议。

## 一、GitHub Copilot 的 RIPER-5 协议

GitHub Copilot 在 VS Code 中通过 `.github/copilot-instructions.md` 定义行为准则，核心是 **5 种模式**：

| 模式 | 目的 | 关键约束 |
|------|------|----------|
| `RESEARCH` | 信息收集 | 只读不写，不猜测 |
| `INNOVATE` | 方案探索 | 讨论多路径，不承诺 |
| `PLAN` | 技术规范 | 精确路径签名，不实施 |
| `EXECUTE` | 实施计划 | 100% 忠实执行 |
| `REVIEW` | 验证一致性 | 逐项核对，标记偏差 |

## 二、Hermes Agent 的对应能力

Hermes 没有内置模式系统，但通过以下方式实现等效功能：

### 2.1 项目级协议 — `AGENTS.md`

在项目根目录创建 `AGENTS.md`，定义行为准则。Hermes 启动时会自动读取项目根目录的 `AGENTS.md` 或 `.github/copilot-instructions.md` 作为系统提示的一部分。

**已创建文件**：`/mnt/d/git/web/Vue.NetCore/AGENTS.md`

### 2.2 技能系统 — `ripper5-protocol`

将 RIPER-5 协议封装为可复用技能，让 Hermes 自动加载协议规则。

**已创建技能**：`~/.hermes/profiles/coder/skills/ripper5-protocol/SKILL.md`

### 2.3 配置项 — `approvals.mode`

Hermes 内置安全审批机制，与 RIPER-5 的"未获授权不得实施"原则对应：

```bash
# 查看当前审批模式
hermes config get approvals.mode

# 设置为手动审批（推荐，与 RIPER-5 一致）
hermes config set approvals.mode manual

# smart 模式：低风险自动通过，高风险手动审批
hermes config set approvals.mode smart

# off 模式：跳过所有审批（不推荐）
hermes config set approvals.mode off
```

### 2.3 模式切换机制

Hermes 通过以下方式实现模式切换：

| 方式 | 命令 | 说明 |
|------|------|------|
| 会话内 | `/goal [text]` | 设定目标，Hermes 自动按目标工作 |
| 会话内 | 用户指令 | 用户直接说"进入研究模式"等 |
| CLI 启动 | `hermes -s ripper5-protocol` | 启动时加载技能 |
| CLI 启动 | `hermes --continue` | 恢复会话，保持上下文 |

### 2.4 AGENTS.md 内容规范

AGENTS.md 只包含**项目特定规范**，不包含 SOUL.md 中的角色定义或通用行为准则。

| 包含内容 | 说明 |
|----------|------|
| 技术栈 | .NET 8.0/9.0、SqlSugar、Autofac、Vue 3.x、Element Plus |
| 项目目录结构 | 后端/前端/MAF SDK 的目录约定 |
| 代码安全区域 | `*/Partial/*.cs` 和 `extension/**/*.jsx` 不被覆盖 |
| 编码规范 | Entity、Service Partial、Controller Partial、Vue 组件的模板 |
| API 规范 | 标准 CRUD 路由格式 |
| 开发规则 | 文件落位、日志、代码风格、文档同步 |

| 不包含内容 | 说明 |
|------------|------|
| 角色定义 | 这些在 SOUL.md 中定义 |
| 模式协议 | 这些在 SOUL.md 中定义 |
| 通用行为准则 | 这些在 SOUL.md 中定义 |

---

## 三、使用教程

```bash
# 1. 确保 AGENTS.md 存在（项目根目录）
#    已创建：/mnt/d/git/web/Vue.NetCore/AGENTS.md

# 2. 确保 RIPER-5 技能已安装
hermes skills install ripper5-protocol

# 3. 验证配置
hermes config check

# 4. 设置审批模式为 manual（与 RIPER-5 一致）
hermes config set approvals.mode manual

# 5. 启动 Hermes 并加载技能
hermes -s ripper5-protocol
```

### 3.2 日常使用流程

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

### 3.3 关键命令速查

| 命令 | 作用 | 对应模式 |
|------|------|----------|
| `/goal [text]` | 设定长期目标 | 所有模式 |
| `/reset` | 新建会话 | 重置为 RESEARCH |
| `/undo` | 撤销上一步 | 执行模式回退 |
| `/rollback [N]` | 回滚文件系统 | 执行模式回退 |
| `/status` | 查看当前状态 | 所有模式 |
| `/help` | 查看所有命令 | — |

### 3.4 配置参考

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

## 四、Hermes vs GitHub Copilot 功能对比

| 功能 | GitHub Copilot | Hermes Agent | 实现方式 |
|------|---------------|-------------|----------|
| 模式声明 | ✅ RIPER-5 | ⚠️ 需约定 | `AGENTS.md` + 用户指令 |
| 项目级准则 | ✅ `copilot-instructions.md` | ✅ `AGENTS.md` | 自动读取 |
| 可复用流程 | ✅ `.github/skills/` | ✅ `skills/` | `hermes skills` |
| 持久记忆 | ❌ | ✅ `memory` | `hermes memory` |
| 子代理协作 | ❌ | ✅ `delegate_task` | 内置工具 |
| 定时任务 | ❌ | ✅ `cronjob` | `hermes cron` |
| 多模型支持 | ❌ | ✅ 20+ 提供商 | `hermes model` |
| 多平台 | ❌ VS Code 独占 | ✅ CLI + 10+ 平台 | `gateway` |
| 安全审批 | ⚠️ 基础 | ✅ `approvals.mode` | 配置项 |
| 自进化 | ❌ | ✅ 自动保存技能 | `curator` |

## 五、最佳实践

### 5.1 模式使用建议

| 任务类型 | 推荐模式 | 理由 |
|----------|----------|------|
| 简单问答 | `RESEARCH` | 直接回应，无需完整流程 |
| 架构设计 | `INNOVATE` → `PLAN` | 先探索方案，再制定规范 |
| 代码修改 | `PLAN` → `EXECUTE` → `REVIEW` | 严格遵循计划，验证一致性 |
| 缺陷修复 | `RESEARCH` → `PLAN` → `EXECUTE` | 先定位根因，再修复 |
| 代码审查 | `REVIEW` | 逐项核对 |

### 5.2 避免常见错误

1. **不要跳过 PLAN 直接进入 EXECUTE** — 即使任务简单，也先输出简要计划
2. **不要在中途改变模式** — 只有明确的切换信号才能切换
3. **不要自行扩大修改范围** — 用户限定边界时严格遵守
4. **不要遗漏文档同步** — 代码改动必须同步更新文档
5. **不要忽略偏差** — REVIEW 中发现任何偏差都要标记

### 5.3 技能管理

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

## 六、故障排查

### 问题：模式不生效

**原因**：AGENTS.md 未被正确读取
**解决**：
```bash
# 1. 确认 AGENTS.md 在项目根目录
ls /mnt/d/git/web/Vue.NetCore/AGENTS.md

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

---

## 附录：RIPER-5 模式切换信号

以下指令可触发模式切换：

```
进入研究模式    → [MODE: RESEARCH]
进入创新模式    → [MODE: INNOVATE]
进入计划模式    → [MODE: PLAN]
进入执行模式    → [MODE: EXECUTE]
进入审查模式    → [MODE: REVIEW]
```

无明确切换信号时，Hermes 保持当前模式。新对话默认处于 `RESEARCH` 模式。
