# ADR-0003：UI 基线——沿用 Java View 体系，以规范约束增量

- **状态**：已接受（accepted）
- **日期**：2026-10-22
- **决策者**：用户
- **影响域**：主壳导航、发现/书架/阅读/搜索/AI/我的页面，以及后续所有 UI 变更
- **编号说明**：本编号属本仓 `docs/architecture/adr/` 序列。`app/.../manager/device`、`network/exception`、`network/server` 注释中的 `ADR-0003` 指 **microfeed 仓**的设备吊销决策，不在本仓，阅读代码注释时勿与本篇混淆。

---

## 1. 背景（Context）

当前 UI 是完全的 Java + Android View 体系：

- `HomeActivity` 用自定义底部导航 `RecyclerView` + `ViewPager` 承载「书架 / 发现 / AI / 我的」，AI 标签随登录态动态插入；
- 阅读容器 `TipsFragmentActivity` 用 ViewPager2 动态装配阅读、方药、设置等 Fragment；
- 列表统一 `WrapRecyclerView` + `AppAdapter`，下拉刷新用 `SmartRefreshLayout`；
- 主题为 `AppTheme`（`Theme.AppCompat.DayNight.NoActionBar`），尺寸/颜色走 `dp_*`、`sp_*`、`common_*` 资源；
- 通用能力已下沉到 `library/base`、`library/widget`、`library/ui-dialog`、`library/tips-widget`、`library/text-renderer`、`library/text-utils`。

但仓库此前没有一份**UI 规范**：新增页面靠模仿旧页面，容易产生以下问题：

1. 每个页面各写一套加载/空/错误态；
2. 直接 `new AlertDialog.Builder` 或自造 Popup，绕过通用弹窗；
3. 页面内硬编码颜色、尺寸、中文文案；
4. 在页面内重复实现搜索高亮、Tips 标记解析；
5. 新页面不知道哪些组件必须复用、哪些行为（权限、刷新、返回）必须接入。

同时，全面改版或框架迁移的成本与风险远高于当前收益。

---

## 2. 决策（Decision）

**保持 Java + Android View 技术栈不变；以规范而非重写来约束 UI 增量。**

具体：

1. 新增 `docs/architecture/ui-spec.md`，作为页面设计、评审与验收的基线，定义：
   - 全局框架（主壳、页面层级、生命周期）；
   - 通用四态（加载/内容/空/失败）与列表分页规则；
   - 书架、发现、阅读、搜索、AI、我的、设置/账号的页面规范；
   - 权限闸门、刷新、错误分层、组件复用矩阵、新页面检查清单。
2. 新增页面/改版必须复用既有基类、列表、状态、弹窗、文本渲染与图片加载能力；确需新增通用组件时放入对应 `library/*`，不在 `app` 内建平行实现。
3. 本轮**不做**：高保真视觉重设计、Kotlin/Compose 迁移、引入 ViewModel/Room、平板/折叠屏专属布局、完整暗色主题。
4. 规范采用“最小约束”原则：只规定结构、状态、交互与复用，不规定像素级间距与具体视觉稿；具体视觉以现有主题/资源为准。
5. M1 以名词解释链路作为首个按规范落地的端到端样板；M2 再覆盖发现→阅读→搜索主链路。

---

## 3. 被否决的替代方案

| 方案 | 否决理由 |
| --- | --- |
| 迁移 Kotlin/Compose 后再统一 UI | 迁移面覆盖全部页面、构建链与团队习惯，收益与风险不匹配；当前没有必须依赖 Compose 的产品需求 |
| 先做高保真视觉重设计 | 缺少设计资源与验收标准，容易把功能票拖成长期改版；当前更需要的是状态与复用的一致性 |
| 新建独立 design system 模块 | 现有 `library/widget` / `ui-dialog` / `tips-widget` 已覆盖主要能力；再建一套会产生平行实现 |
| 不写规范，仅靠 code review | 已出现同类页面实现不一致；口头约定不可检索、不可验收 |
| 为每个页面写独立设计文档 | 文档碎片化、互相引用、难以维护；应由一份规范统一约束 |

---

## 4. 后果（Consequences）

### 4.1 正面

- 新页面有明确的结构、状态与复用清单，评审可逐项核对。
- 避免重复造轮子，减少弹窗/空态/高亮等实现分叉。
- 不承担框架迁移成本，M1 可以在不改行为的前提下落地。
- 规范与 ADR 分离：规范讲“怎么做”，ADR 讲“为什么这样定”。

### 4.2 负面 / 风险

| 风险 | 评估与处置 |
| --- | --- |
| 规范落后于实现 | 每次 UI 变更必须同步更新 `ui-spec.md`；M1/M2 验收时检查 |
| 现有页面不完全符合规范 | 规范约束**新增与改动**；存量页面按 M2/M3 逐步收敛，不在功能票中顺手大改 |
| 规范被理解为“视觉稿” | 文档开头明确规范级定位；视觉细节仍以主题/资源为准 |
| DayNight 声明与暗色资源缺失 | 当前无 `values-night` 覆盖；新增颜色必须同时提供暗色资源或明确不支持暗色，不得只硬编码亮色 |
| 过度约束拖慢小改动 | 复用矩阵与检查清单只覆盖结构/状态/安全；纯文案修正不强制全套流程 |

### 4.3 对现有代码的影响

本 ADR 本身**不修改任何代码**。它约束的是后续变更：

- 新页面/改版的布局、状态、导航、权限与日志；
- 通用组件的新增位置；
- UI 相关文档的同步义务。

---

## 5. 落地与验证

1. `docs/architecture/ui-spec.md` 与本 ADR 同批提交。
2. `.qoder/skills/project-architecture/SKILL.md` 将本规范列为 UI 变更的必读入口。
3. M1 名词解释样板按规范验收：权限闸门、四态、文案资源、无硬编码、日志脱敏。
4. UI 变更至少通过 `gradlew.bat testDebugUnitTest` 与 `gradlew.bat lintDebug`；涉及界面时附截图或录屏。

---

## 6. 关联文档

- [UI 设计规范](../ui-spec.md)
- [数据模型与缓存策略](../data-model.md)
- [HTTP 请求契约](../http-api.md)
- [ADR-0002：移除宋版伤寒章节截取](0002-shanghan-filter-removal.md)
