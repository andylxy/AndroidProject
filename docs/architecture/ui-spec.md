# UI 设计规范

- **状态**：已接受（accepted）
- **日期**：2026-10-22
- **适用范围**：`app` 主模块的全部用户界面；`library/*` 中的通用组件改动同样受本规范约束
- **关联决策**：[ADR-0003：UI 基线](adr/0003-ui-baseline.md)
- **关联文档**：[数据模型](data-model.md)、[HTTP 契约](http-api.md)、[组件与依赖清单](components.md)

---

## 1. 定位与边界

本应用是《伤寒杂病论·桂本》的**学习与研究辅助查阅工具**（见 `res/values/strings.xml` 的应用定位文案），不是内容社区、资讯流或电商应用。UI 设计以「找书 → 读书 → 查词 → 搜索 → 提问」为主线，优先保证阅读与检索效率。

本文件是**规范级**设计说明，回答“页面由哪些区域组成、有哪些状态、遵循什么交互规则”。它不提供高保真视觉稿、不规定像素级间距，也不替代布局 XML；具体视觉以现有主题、样式和资源为准。

本轮明确**不做**：整体视觉重设计、Kotlin/Compose 迁移、引入 ViewModel/Room、平板/折叠屏专属布局、完整暗色主题。以上事项如要推进，须先新增 ADR。

---

## 2. 技术基线

| 项目 | 现状（必须沿用） |
| --- | --- |
| 语言与 UI 框架 | Java + Android View 体系；不使用 Kotlin、Compose、DataBinding |
| 主题 | `AppTheme`（`Theme.AppCompat.DayNight.NoActionBar`），见 `res/values/styles.xml` |
| 页面基类 | `BaseActivity` / `BaseFragment`（`library/base`）→ `AppActivity` / `AppFragment` / `TitleBarFragment`（`app`） |
| 列表 | `WrapRecyclerView` + `AppAdapter`；需要下拉刷新时使用 `SmartRefreshLayout` |
| 状态占位 | `StatusLayout`（`library/widget`）；项目内已有页面也使用等价的自定义空态布局 |
| 图片 | Glide 4.12（`GlideApp` / `Glide.with`），不引入第二套图片加载库 |
| 弹窗 | 通用弹窗使用 `library/ui-dialog`；Tips 文本浮窗使用 `library/tips-widget` |
| 文本渲染 | Tips 内容统一走 `TipsTextRenderer` / `TipsTextRenderConfig`，不得在页面内另写标记解析 |
| 状态栏 | `ImmersionBar`；页面按现有基类能力处理，不手写 `Window` flag 兼容代码 |
| 日志 | `EasyLog`；不得在 UI 层扩散 `android.util.Log` 或自建日志入口 |

> **尺寸资源**：统一使用 `@dimen/dp_*`、`@dimen/sp_*`、`@color/common_*` 等既有资源。当前仓库**没有** `values-swXXXdp` 断点目录，新增页面不得假设存在该适配体系；需要响应式布局时用 `match_parent`、权重和最小宽度约束实现。
>
> **组件版本与官方地址**：上表所列 UI 组件的具体版本、接入入口与官方文档见 [组件与依赖清单](components.md) §3.1；新增 UI 依赖前先查该清单，避免引入同类第二套库。

---

## 3. 全局框架

### 3.1 主壳与导航

`HomeActivity` 是唯一主壳，结构为：

```
RecyclerView（底部导航，NavigationAdapter）
        +
ViewPager（FragmentPagerAdapter）
```

- 底部导航顺序固定为：**书架（0）→ 发现（1）→ [AI 对话（仅登录，2）] → 我的（3）**。
- AI 标签由 `AppApplication.application.isLogin` 决定是否插入；登录状态变化后必须重建导航与 Pager，禁止留下索引错位的半更新状态。
- 默认启动页为「发现」（`HomeActivity.start(context)` → `HomeFragment`）。
- 主壳支持双击返回退出；退出延迟与提示沿用 `HomeActivity` 现有实现。

### 3.2 页面层级

| 层级 | 页面 | 说明 |
| --- | --- | --- |
| L1 主壳 | `HomeActivity` | 四个一级入口，负责底部导航与页面切换 |
| L2 列表/容器 | `HomeFragment`、`TipsWindowNetFragment`、`BookCollectCaseFragment`、`AiMsgFragment`、`MyFragmentPersonal` | 一级入口内容 |
| L2 阅读容器 | `TipsFragmentActivity` | ViewPager2 + 动态 Fragment：阅读、方药、设置 |
| L2 搜索 | `BookContentSearchActivity` | 全局搜索与书内搜索共用的结果页 |
| L2 功能页 | `SettingActivity`、`LoginActivity`、`RegisterActivity`、`AboutActivity` 等 | 设置、账号、关于 |
| L3 浮层 | Tips 文本浮窗、阅读设置弹窗、章节列表浮层、通用 Dialog | 不单独占用页面栈 |

### 3.3 生命周期与请求

- 页面内网络请求必须绑定 `LifecycleOwner`（`EasyHttp.get(this)` / `EasyHttp.post(this)`），页面销毁后不得再回调 UI。
- Fragment 的 `getAttachActivity()` 可能为空；异步回调中必须先判空再触碰 Activity。
- 读取数据库不得在主线程直接 `findAll()`；统一经 `DbService.readInBackground` / `runInBackgroundSerial`（详见[数据模型](data-model.md)）。
- 页面销毁时必须取消进行中的搜索、下载、SSE 与动画；不得留下静态引用持有 View。

---

## 4. 通用状态与列表规则

### 4.1 四态要求

每个依赖网络或本地异步数据的页面/区块，必须显式设计以下状态：

| 状态 | 要求 |
| --- | --- |
| 加载中 | 有可见反馈；禁止白屏等待；禁止重复触发同一请求 |
| 有内容 | 正常渲染；列表滚动位置在数据刷新后尽量保持 |
| 空数据 | 说明“为什么空 + 下一步做什么”；不得只显示空白页 |
| 失败 | 给出可理解的原因与重试入口；全局错误（426/1001/设备吊销）交由统一处理器，页面不重复弹通用错误 |

本地优先页面（书架、发现、阅读）应**先展示本地已有数据**，再按刷新策略更新；不得为了等待网络而阻塞首屏。

### 4.2 列表与分页

- 列表统一使用 `WrapRecyclerView` + `AppAdapter`；需要下拉刷新时用 `SmartRefreshLayout`。
- 只有接口明确返回分页数据时才启用“加载更多”；未分页的接口必须关闭上拉加载（参考 `TipsWindowNetFragment` 关闭 `setEnableLoadMore`）。
- 分页请求参数与响应结构遵循 [HTTP 契约](http-api.md) 的列表约定（`page` 从 1 开始，默认 20，最大 100）。
- 列表项点击必须有防重复点击保护（项目已有 `@SingleClick` / `DoubleClickHelper` 机制）。
- 列表为空时隐藏刷新头/加载更多，展示空态；不要保留“加载中”骨架无限循环。

### 4.3 文案与资源

- 所有用户可见文案放入 `res/values/strings.xml`；禁止在 Java/XML 中硬编码中文。
- 颜色、尺寸、圆角、间距必须引用资源；禁止在布局中散落魔法值。
- 文案使用简体中文，语气简洁、可执行；错误提示说明“发生了什么 + 可以怎么做”，不暴露堆栈、URL、Token 或内部类名。
- 无障碍：可点击图标必须提供 `contentDescription`；触控目标不小于 48dp；文字随系统字体缩放，不得用固定高度裁切。

---

## 5. 信息架构与页面规范

### 5.1 书架（`BookCollectCaseFragment`）

**目标**：快速回到正在读的书。

- 数据源：本地 `Book` 表（`BookService.getAllBooks()`），不直接请求网络。
- 标题固定为「书架」。
- 列表项至少展示**书名 + 作者**；封面、简介、阅读进度属于后续增强，只有在数据可用时才展示，不得用占位假数据。
- 空态：提示“书架还没有书”，提供跳转「发现」的入口（现有 `ll_no_data_tips` / `ll_no_book` 交互）。
- 点击书籍：先执行强制升级检查（`UpdateManager.checkForceOnReading`），通过后按 `bookNo` 查书并进入阅读容器；书籍信息缺失时给出明确提示，不得静默无响应。
- 书架页不承担搜索入口；全局搜索统一放在「发现」。

### 5.2 发现（`HomeFragment`）

**目标**：找书与全局搜索。

页面自上而下：

1. 可折叠头部（`XCollapsingToolbarLayout`）与搜索框；
2. 搜索历史浮层（`include_book_content_search_ll_history_view`，本地 `SearchHistory` 表）；
3. 分类标签（`TabNav.name`，`rv_home_tab`）；
4. 分类下的书籍列表（`NestedViewPager` + `TipsWindowNetFragment`）。

规则：

- 搜索框支持输入、键盘搜索、点击搜索图标、点击历史记录三种触发方式；触发后保存历史并进入 `BookContentSearchActivity`。
- 全局搜索必须先判定 `SearchPermissionManager.isSearchAllowed(SearchEntry.GLOBAL)`；未授权或尚未拉到权限时提示无权限并中止（fail-closed），不得放行后由接口报错。
- 分类与书籍来自 `GlobalDataHolder` 中已加载的 `TabNav` / `TabNavBody`；数据未就绪时展示加载态或空态，不得显示空白标签栏。
- 书本条目点击：先做强更检查，再进入 `TipsFragmentActivity`；条目至少展示书名与作者，`imageUrl` 可用时可展示封面。
- 头部刷新按钮触发 `AppDataManager.reset()` + 重新加载，并刷新样式配置；刷新期间按钮应有禁用/加载反馈，避免连续点击。
- 发现页依赖的样式配置必须走 `TipsTextRenderConfig` 的 provider 机制；页面不得自行解析样式 JSON。

### 5.3 阅读容器（`TipsFragmentActivity` + `TipsBookNetReadFragment`）

**目标**：稳定、低干扰地阅读与查证。

容器：

- `TipsFragmentActivity` 用 ViewPager2 承载动态 Fragment 列表，底部导航标签数量与 Fragment 数量一致。
- 书籍阅读场景至少包含：阅读正文、方药、阅读设置；不同书籍类型可按现有 `setupFragments` 规则增减，但**标签与 Fragment 必须同源生成**，禁止两套列表各自维护顺序。
- 打开书籍时传入 `bookId`（即内容书号 `bookNo`）；无效 ID 或书籍信息缺失时展示错误页并允许返回，不得崩溃或停在空白容器。

阅读正文：

- 章节列表使用分组列表（`WrapRecyclerView` + 分组适配器），支持展开/收起、跳转指定章节。
- 正文由 `TipsTextRenderer` 渲染；`$g{}` 等标记的展示与点击行为由渲染配置决定，页面不得硬编码样式或自行解析标记。
- 书内搜索入口必须先判定 `SearchPermissionManager.isSearchAllowed(SearchEntry.BOOK)`；无权限时提示并中止。
- 名词解释点击受 `MingCiPermissionManager.isAllowed()` 约束；无权限时保持现有“未加载”表现，不弹自定义权限说明。
- 章节内容按需下载/缓存；加载失败时在对应章节或页面给出重试入口，不得把失败章节显示成空正文。
- 阅读位置通过 `Book.lastReadPosition` 与 `historiographerNumb` 持久化；退出再进入应回到上次位置，越界位置必须夹取到合法范围。

阅读设置：

- 设置项变更后必须能立即反馈到正文（字号、术解等现有能力）。
- 设置项不得出现“开关存在但无效果”的状态；功能移除时同步移除 UI（参照 ADR-0002 的处置原则）。

### 5.4 搜索（`BookContentSearchActivity`）

**目标**：一次输入，按内容类型给出可解释的结果。

- 入口：发现页全局搜索；阅读页内搜索。两个入口分别使用 `SearchEntry.GLOBAL` 与 `SearchEntry.BOOK` 做权限判定。
- 搜索范围由现有策略决定：正文内容、药物、方剂、名词解释（`ContentSearchStrategy` / `YaoSearchStrategy` / `FangSearchStrategy` / `MingCiSearchStrategy`）。
- 结果项必须标明类型与来源书籍/章节；命中词使用 `library/text-utils` 的 `SearchMatcher` / `TextHighlighter` 高亮，禁止各页面自写高亮算法。
- 搜索历史写入本地 `SearchHistory`，支持单条点击回填与清空；去重与排序沿用 `SearchHistoryService` 现有行为。
- 状态：输入为空时不搜索；搜索中禁用重复提交；无结果时提示“没有找到相关内容”并保留关键词；失败时展示重试。
- 结果点击跳转到对应阅读位置；无法定位时给出提示，不得静默失败（章节坐标约定见 ADR-0002）。

### 5.5 AI 对话（`AiMsgFragment`）

**目标**：在登录前提下提供基于书籍内容的问答。

- 仅登录用户显示 AI 标签；未登录不显示入口，也不允许通过深链绕过。
- 页面结构：会话区（消息列表）+ 输入区（`ChatInputHelper`）；空会话展示引导文案，不展示伪造消息。
- 消息类型沿用 `ChatMessageBean`：发送（1）、接收（2）、系统（3）、思考过程（4）、会话总结（5）。
- 流式状态必须可见且可区分：连接中、思考中、接收中、完成、失败。失败时保留已接收内容并提供重试/重新发送。
- SSE 仅用于 AI 对话；请求与生命周期绑定，页面销毁或会话切换时必须取消上一条流。
- 消息落库、会话切换与历史加载遵循[数据模型](data-model.md)；UI 不得直接操作数据库。

### 5.6 我的（`MyFragmentPersonal`）

- 未登录：展示登录/注册入口，说明登录后可用的能力（AI 对话、权限相关能力）。
- 已登录：展示用户信息、数据更新频率入口、设置入口、退出登录。
- 退出登录必须二次确认；退出后清除登录态并回到主壳默认页，AI 标签随之移除。
- 数据更新频率选项文案与 `DataUpdateFrequency` 保持一致（每次启动/每天/每周/每月/手动）；修改后立即生效于下一次加载判定。

### 5.7 设置、账号与关于

- 沿用现有 `TitleBarFragment` + 通用设置条样式，不引入新的页面骨架。
- 表单页（登录、注册、找回/重置密码）必须提供：输入校验、提交中禁用、失败提示、成功后的明确去向。
- 关于页展示版本信息；不得展示内部环境地址、密钥或调试开关。

---

## 6. 交互与状态规则

### 6.1 导航与返回

- 主壳内切换标签不重建已缓存的 Fragment；阅读容器内部切换标签不得丢失阅读位置。
- 阅读页返回应回到进入前的页面；从书架/发现进入的行为保持一致。
- 主壳返回：两次返回间隔内再次返回才退出，首次返回给出提示。
- 任何“无反应”的点击都是缺陷：要么执行，要么提示原因。

### 6.2 权限闸门

| 能力 | 判定入口 | 无权限表现 |
| --- | --- | --- |
| 首页全局搜索 | `SearchPermissionManager.isSearchAllowed(SearchEntry.GLOBAL)` | Toast 提示，停留在原页 |
| 书内搜索 | `SearchPermissionManager.isSearchAllowed(SearchEntry.BOOK)` | Toast 提示，不发起搜索 |
| 名词解释 | `MingCiPermissionManager.isAllowed()` | 维持现有“未加载”行为，不展示内容 |

权限判定是**进程内 fail-closed**：冷启动尚未拉到、拉取失败、响应字段缺失时一律视为无权限。UI 不得读取 MMKV 缓存自行放行，也不得在权限未知时先展示受限内容再回滚。

### 6.3 刷新与数据新鲜度

- 页面首屏遵循本地优先：先渲染本地/内存数据，再按 `DataUpdateFrequency` 策略决定是否更新。
- 手动刷新必须有明确入口与结果反馈；失败时保留旧数据并提示，不得清空页面。
- 禁止页面自行定时轮询；周期刷新统一由数据层策略控制。
- 样式配置、权限状态属于独立缓存，不参与内容数据的“全量刷新”语义（详见[数据模型](data-model.md)）。

### 6.4 错误分层

- 426（版本过低）、1001（Token 失效）、401 + `X-Device-Revoked: 1`（设备吊销）由 `RequestHandler` / `UpdateManager` / `DeviceNoticeManager` 统一处理；页面只负责不重复弹通用错误。
- 页面级错误只处理本页可恢复的问题（空数据、单章下载失败、搜索失败），并提供就地重试。
- 禁止把异常堆栈、请求 URL、响应原文直接展示给用户；调试信息只进 `EasyLog`。

---

## 7. 组件复用清单

| 需求 | 必须使用 | 禁止 |
| --- | --- | --- |
| 页面骨架 | `AppActivity` / `AppFragment` / `TitleBarFragment` | 新写一套 Base 类 |
| 列表 | `WrapRecyclerView` + `AppAdapter` | 直接裸用 `RecyclerView.Adapter` 复制粘贴现有逻辑 |
| 下拉/加载更多 | `SmartRefreshLayout` | 自写手势监听 |
| 空/错/加载 | `StatusLayout` 或页面既有等价布局 | 每个页面各写一套互不一致的空态 |
| 弹窗 | `library/ui-dialog` | 直接 `new AlertDialog.Builder` 扩散样式 |
| Tips 浮窗 | `library/tips-widget`（`TipsLittleWindow` / `WindowConfig`） | 自定义 PopupWindow 重造 |
| 文本渲染 | `TipsTextRenderer` + `TipsTextRenderConfig` | 页面内解析 `$g{}` 等标记 |
| 搜索高亮 | `SearchMatcher` / `TextHighlighter` | 自写正则高亮 |
| 图片 | Glide | 新增图片库 |
| 事件 | 现有 `XEventBus` / 局部回调 | 新增事件总线 |

新增通用组件前先确认 `library/base`、`library/widget`、`library/ui-dialog`、`library/tips-widget` 中是否已有等价能力；确需新增时，放入对应共享模块，不在 `app` 内建平行实现。

---

## 8. 新页面/改版验收清单

- [ ] 页面归属层级明确，使用现有基类与主题，无新框架。
- [ ] 完成加载/内容/空/失败四态，并有可执行的重试或下一步。
- [ ] 网络请求绑定生命周期，异步回调判空，页面销毁可取消。
- [ ] 列表使用 `WrapRecyclerView` + `AppAdapter`；分页符合 HTTP 契约。
- [ ] 文案、颜色、尺寸全部走资源；无硬编码中文与魔法值。
- [ ] 受权限约束的入口已接入对应 Manager，且为 fail-closed。
- [ ] 点击均有反馈；重复点击有保护；返回行为符合预期。
- [ ] 关键路径有 `EasyLog` 日志，不记录 Token、口令、密钥或用户隐私。
- [ ] 同步更新本文件、[数据模型](data-model.md) 或 [HTTP 契约](http-api.md) 中受影响的部分。
- [ ] 至少通过 `gradlew.bat testDebugUnitTest` 与 `gradlew.bat lintDebug`；UI 变更附截图或录屏。

---

## 9. 与路线图的关系

- **M1**：本规范作为评审基线；不进行视觉改版。名词解释链路作为首个按规范落地的端到端样板。
- **M2**：发现 → 书籍 → 阅读 → 搜索主链路按本规范补齐状态、分页与错误处理；AI 仅完善容器与流式状态。
- **M3**：启动与页面性能、可观测性、暗色/大屏等体验治理；任何视觉体系升级须先新增 ADR。

超出本规范范围的改动（新导航模型、新设计语言、跨端一致性）不得在功能票中顺手实施。
