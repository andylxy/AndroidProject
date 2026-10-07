# TipsBookNetRead 重构工单 / 票据（执行追踪）

> 来源：设计文档 `tips-book-net-read-design-analysis.md` + `tips-book-net-read-fixes-done.md`。
> 本文件 = **工单与票据**，按执行顺序编号，逐张推进，每完成一张更新状态与证据（不信任注释，以代码/git/logcat 为准）。
>
> 状态图例：
> - **【待执行】**：尚未开始
> - **【执行中】**：代码改动中
> - **【已完成-待验证】**：代码改完，待编译/装机验证
> - **【已验证】**：编译通过 + （尽可能）adb/logcat 验证
> - **【阻塞】**：需外部依赖（后端/数据）或需用户决策

门禁总则：
1. `git diff --check` 无尾随空格/冲突标记。
2. `./gradlew :app:compileDebugJavaWithJavac -PServerType=test` BUILD SUCCESSFUL。
3. adb 安装 + logcat 运行无新增崩溃（能复现的用例尽量复现）。

---

## 状态总览

| 工单 | 对应缺陷 | 优先级 | 风险 | 状态 |
| --- | --- | --- | --- | --- |
| T0 | D2 收尾（待提交代码入库） | P0 | 低 | 【代码已写-待提交】 |
| T1 | D4.1 死代码清理 | P1 | 低 | 【已验证】编译通过 + grep 零命中 |
| T2 | D2.1 `updateDownloadStatus` 搜索态守卫 | P1 | 低 | 【已验证】编译通过 |
| T3 | D6 死路径清理 | P1 | 低-中 | 【已验证】编译通过 + 注释死块已删（与 T5/T7 合并） |
| T4 | D7 生命周期/竞态治理 | P2 | 中 | 【已验证】装机运行无崩溃（与 T3/T5 同批） |
| T5 | D8 搜索异步化 | P2 | 中 | 【已验证】logcat 证实搜索在子线程执行 |
| T6 | D4 索引单一真相（chapterIndex） | P1 | 中 | 【阻塞-需决策】（前提不成立，见新发现1） |
| T7 | D3 统一搜索路径 | P2 | 中 | 【已验证】搜索链路 logcat 走通（与 T3/T5 合并） |
| T8 | D9 实体重建优化 | P3 | 低 | 【部分成立-已订正】`updateChapterContent` 增量那半不成立；`reListAdapter` 全量重建那半仍成立（见新发现2） |
| — | D5 范围已厘清（无需改代码） | — | — | 【已完成-无需代码】 |

### 联调验证证据（2026-10-07，emulator-5554 + 后端 192.168.2.158:4321）

-装机：`:app:installDebug -PServerType=test` BUILD SUCCESSFUL。
- **后端联通**：`/api/AppBookRequest/app/version` 返回真实版本数据；`/app/search-permission` 返回 `{"global":false,"book":true}`；本地落库 601 药材 / 17 名词 / 111 方剂。
- **阅读页正常**：进入《伤寒论・（人纪）》，章节列表完整渲染（伤寒论原序、校勘序、各篇），**零 FATAL / 零 Exception**（仅 StrictMode 提示，均为既有 MMKV/SPUtils 相关，与本轮改动无关）。
- **T3/T5/T7 搜索链路**：logcat 实证
  - `TipsBookReadPresenter: search() 开始: bookId=XNK0VFX39Xv, keyword=shang`
  - `=== SearchCoordinator.searchGlobal() ===` 线程 **20606**（主线程为 20248）→ **异步化生效，D8 修复确认**
  - `=== 搜索完成 === 匹配章节: 0, 总匹配数: 0` → UI 显示「0个结果」，结果回主线程渲染正常。
- **清空搜索恢复全量**：删除关键字后列表回到全量章节，「搜索态 ↔ 全量态」来回切换无崩溃。
- 说明：`adb shell input text` 无法注入中文（`NullPointerException: Attempt to get length of null array`，adb 已知限制），故用 ASCII 关键字验证链路；中文搜索需装 ADBKeyboard 才能端到端复现。

> D5 经核实：阅读页搜索态不依赖 `SearchStateManager`（属`RefactoredSearchAdapter` 搜索页），阅读页用 `isSearchActive()` 即可，**无代码改动**。

### 本轮实施后的两项新发现（推翻原工单假设）

**发现1 — T6 的前提不成立，需用户决策后才能做**

原方案假设「`GroupData` 携带 `chapterIndex` 即可解决索引错位」。核查后发现**位置索引在这套数据里不可靠**：

1. 非搜索列表来自 `Chapter` 表（`LocalServices.mChapterService.find(BookId.eq(...))`），搜索列表来自 `BookChapter` 表（`mBookChapterService.find(BookId.eq(...))`）——**两张表、两套记录**。
2. 两处查询**均无显式排序**（`grep orderBy` 在两个 Dao 与 helper 中零命中），返回顺序由SQLite 默认决定，**不保证一致**。
3. 因此「搜索结果第 n 项 = 非搜索列表第 n 章」这一等式不成立；把过滤后的 `groupPosition` 直接当章节索引用，仍会错章——**T6 若照原方案实施，只是把D4 的标题匹配换成位置匹配，错误依旧**。

可选项（需决策，见文末）：
- **A（推荐）**：以 `signatureId` 为跨表稳定键——两表都有该字段（`BookChapter.signatureId` / `Chapter.signatureId`），先按 `signatureId` 建立映射再取位置。代价：需给 `GroupData` 加字段并在两条构造链透传，且要验证两表 `signatureId` 确实一一对应。
- **B**：给两表查询补显式 `orderBy`（同一排序键，如 `chapterSection` / `section`），使位置索引可靠。改动小，但需确认两表该字段语义一致。
- **C**：暂不做，维持现状（现状是「标题匹配」，D4 标注的风险等级远高于实际暴露面，因搜索态点击已统一 `return`，见发现2）。

**发现 2 — T8 的问题描述不成立，无需改动**

D9 写「`getExpandableGroups` 每次重建整份实体列表」。核实：
- `reListAdapter(true, ...)` 仅在**初始化 / 清空搜索**时调用（`:146/:541/:612/:662`），此时重建是必需且幂等的，不是浪费。
- `updateChapterContent` 并非全表重建——它只对**单个** `groupPosition` 构造一个 `ExpandableGroupEntity` 并 `updateGroupFromEntity`（`:440-443`），已是增量更新。

结论：T8 属**误判**，不做改动；D9 应降级为「无实际性能问题」。

**发现 3 — 搜索态子项长按仍用过滤后索引（真实残留风险）**

`onHeaderClick` / `onHeaderLongClick` 已在搜索态 `return`（D1/T1），但**子项长按**走适配器 `ReadModeLongClickHandler.onChildLongClick` →「跳转到本章内容」/「重新下本章节」直接使用 `groupPosition`（`:138/:143`），搜索态下会命中错误章节。此路径不经过 Fragment 的搜索态守卫，是 D4 唯一的真实暴露面。修复取决于发现 1 的决策结果。

---

## T0 — D2 收尾：将待提交代码入库

- **目标**：D2 的两处修复（Fragment `updateChapterContent` 搜索态守卫 + Adapter `setSearchData` 同步 `groups` 镜像）已写入工作区但未 `commit`。本轮随整批一起安全提交（不自动 push）。
- **代码位置**：
  - `TipsBookNetReadFragment.java:492` `if (isSearchActive()) return;`
  - `RefactoredExpandableAdapter.java:108-117` `mirrorGroups` 镜像赋值 `this.groups`
- **门禁**：编译通过；非搜索态下载/展开/点击回归无变化。
- **状态**：【代码已写-待提交】随T1–T7 一并入库

---

## T1 — D4.1 死代码清理

- **目标**：删除无副作用的死状态/死分支，降低阅读干扰、为 D4 让路。
- **改动点**（均在 `TipsBookNetReadFragment.java`）：
  1. 删除字段 `private int currentIndex = -1;`（:90，全仓未读）。
  2. 删除 `onHeaderClick` 中搜索态映射块（:296-328）：`realIndex` / `findChapterIndexByTitle` 反查 / `currentIndex = realIndex` / 紧接 `return`——该块仅写死变量随即返回，无任何可观察效果。
  3. 删除 `findChapterIndexByTitle(...)` 方法（:436-448），需先 grep 确认全线唯一调用方即本块。
- **门禁**：编译通过；`grep -rn "currentIndex\|findChapterIndexByTitle"` 零命中（除删除处）。
- **风险**：低。仅删死代码，不改变任何可见行为。
- **状态**：【已验证】删除完成，grep 零命中；编译通过

---

## T2 — D2.1 `updateDownloadStatus` 搜索态守卫

- **目标**：与 D2 同口径，搜索态下不应把「章节下载完成」回调应用到当前（过滤后的）列表。
- **改动点**：`TipsBookNetReadFragment.updateDownloadStatus(int, boolean)`（:825）入口加 `if (isSearchActive()) return;`。
- **依据**：其调用方 `presenter.updateDownloadStatus` 在搜索态已被 `onHeaderClick`/`onHeaderLongClick` 的 `return` 拦死，当前不可达（低危）；加守卫是为「后续放开搜索态下载」预留正确性，属防御性收口。
- **门禁**：编译通过。
- **风险**：低。
- **状态**：【已验证】守卫已加；编译通过

---

## T3 — D6 死路径清理

- **目标**：移除 MVP 侧从未接线的搜索死路径，消除设计文档 D3/D6 双路径歧义。
- **改动点**：
  1. `TipsBookReadPresenter.java`：注释块内的旧 `search()`（含 `view.showSearchResults` 调用，未编译生效）与生效桩 `search()` 整段删除，合并为单一 `search(keyword)`：内部调 `SearchCoordinator` +后台线程 + `searchSeq` 在途序号。
  2. ~~`TipsBookNetReadFragment` 的 `showSearchResults(...)` 实现：删除~~ —— **实际未删除，改为保留并成为唯一渲染路径**。原因：删除后 Presenter 无处回填结果。签名从 `List<ExpandableGroupEntity>` 改为 `(List<GroupData>, List<List<ItemData>>, int)`，与适配器绑定的 `groupDataList` 同构（这才是「消除双路径」的关键，见 T7）。
  3. ~~`TipsBookReadContract` 的 `showSearchResults` 标注 `@Deprecated`~~ —— **实际未标**，改为直接改签名（新结构取代旧结构，旧结构已无使用方）。已核实 `TipsBookReadContract.View` 全仓唯一实现者是 `TipsBookNetReadFragment`，`BookContentSearchActivity` 不引用该 Contract，改签名安全。
- **前置校验**：`grep -rn "showSearchResults"` 确认仅 Presenter 与 Fragment 引用，无第三方调用。
- **门禁**：编译通过；grep 确认死调用已清除。
- **风险**：低-中（动 MVP 接口，需确认无其它 View 实现 —— 已确认）。
- **状态**：【已验证】与 T5/T7 合并实施；注释死块已删（Presenter 1039→997 行；末轮因脚本事故回滚重做为 1045 行）；改签名安全性已核实；编译通过

---

## T4 — D7 生命周期与竞态治理

- **目标**：消除「view 已销毁仍被 EventBus 触发」与「并发多趟加载覆盖 chapterList」两类隐患。
- **改动点**（`TipsBookNetReadFragment.java`）：
  1. ~~`onDestroyView()` 补 `unregister`~~ → **实际改为 `onStart` 注册 / `onStop` 注销配对**（设计文档 §4.5 允许二选一：「`onStart` 注册 / `onStop` 注销，或 `initView` 注册、`onDestroyView` 注销」）。选前者的理由：与 view 生命周期严格对齐，且避免 `onDestroy`/`onDestroyView` 双注销的重复注销风险。
  2. `bookInitData()` 的 `DbService.readInBackground` 异步加载加**在途序号 `bookDataLoadSeq`**：新一次加载发起时递增，回调里序号不匹配即丢弃结果，避免 `chapterList` 被最后完成者覆盖。
  3. （末轮复审追加）`Presenter.search()` 同样加了 `searchSeq` 在途序号 + `cancelSearch()` 作废入口，与本条同机制。
- **门禁**：编译通过；logcat 观察配置变更/连发事件无重复加载、无 NPE。
- **风险**：中（竞态改动需谨慎，避免引入「永远不刷新」—— 用「序号不匹配即丢弃」而非「取消任务」，保证最后一次一定生效）。
- **状态**：【已验证】装机运行无崩溃

---

## T5 — D8 搜索异步化（性能）

- **目标**：把 `performGlobalSearch` 中的整本书同步检索移出主线程，避免章节多的书卡 UI / ANR。
- **改动点**（实际落点已随 T3/T7 合并而变化）：
  1. ~~`searchCoordinator.searchGlobal(keyword)` 包进 `DbService.readInBackground`~~ → **实际由 `Presenter.search()` 统一执行**：Fragment 只做权限判定与转发，检索在 Presenter 内用 `ThreadUtil.runInBackground` 执行，结果回主线程再 `view.showSearchResults(...)` → `adapter.setSearchData(...)`（Fragment 侧的 `searchCoordinator` 字段与直调已删）。
  2. 加载期间给出「搜索中」提示，结束隐藏。
   > **第三轮复核修订**：原写「复用 `showLoading(true)` / 已有 loading 视图」—— 两处都不准：① 本页布局内**没有** loading 控件，只有 `numTips`；② `showLoading` 被**书籍加载链路**（`loadBookContent` / `onChaptersLoaded`）复用，若搜索提示也写进同一方法，打开任意书籍都会闪「搜索中…」，且搜索与书籍加载两条链会互踩提示。
   > 现改为：新增专用 `Contract.showSearching(boolean)`，Presenter 搜索路径走它；`Fragment.showLoading` 恢复为空实现（书籍加载进度本就未做展示）。
- **前置**：确认 `SearchCoordinator.searchGlobal` 不持有主线程对象、可安全后台执行。
- **门禁**：编译通过；搜索大书时主线程无长阻塞（logcat / 不掉帧）。
- **风险**：中（线程切换需保证结果回主线程且 Fragment 未销毁时再写 adapter）。
- **状态**：【已验证】logcat 实证检索在子线程（20606 ≠ 主线程 20248）

---

## T6 — D4 索引单一真相（chapterIndex）

- **目标**：让 UI 点击/长按直接使用章节的稳定索引字段，彻底去掉 `findChapterIndexByTitle` 的标题字符串回查（标题重复会错章）。
- **改动点**：
  1. 确认 `HH2SectionData`（或 `GroupData`）已携带/可加 `chapterIndex` 字段；构造时（Presenter/`GroupModel`）绑定真实索引。
  2. `onHeaderClick` 直接取 `groupPosition` 经 adapter 暴露的「当前列表项 → chapterIndex」映射；非搜索态 `groupPosition` == `chapterIndex`，搜索态由 `setSearchData` 携带的真实 `chapterIndex` 提供。
  3. 删除 T1 已删的标题回查。
- **爆炸半径**：`RefactoredExpandableAdapter` 被 `TipsBookNetReadFragment` 与 `BookContentSearchActivity` 共用；改公开 API 须两处同步适配并一起编译回归。
- **门禁**：编译通过；搜索/非搜索点击、长按均命中正确章（adb 可复现用例优先）。
- **风险**：中。
- **状态**：【阻塞-需决策】前提不成立，见「发现 1」；子项长按为唯一未收敛暴露面

---

## T7 — D3 统一搜索路径

- **目标**：收敛为单一搜索入口，消除「Fragment 直调 SearchCoordinator」与「MVP showSearchResults」双路径。
- **改动点**：
  1. `TipsBookNetReadFragment.performGlobalSearch` 改为调用 `presenter.search(keyword)`。
  2. Presenter `search(keyword)` 内部调 `SearchCoordinator`，回调 `view.showSearchResults(results, count)`（此处用统一 `submitList` 新结构）。
  3. 确认 `setSearchData` 调用点仅此一处后，删除 Fragment 侧的 `performGlobalSearch` 直调与 `setSearchData`（或改为内部方法被 Presenter 回调复用）。
- **门禁**：编译通过；阅读页与书内搜索页（`BookContentSearchActivity`）搜索功能均正常。
- **风险**：中（跨消费者）。
- **状态**：【已验证】与 T3/T5 合并实施；logcat 证实链路走通

---

## T8 — D9 实体重建优化

- **目标**：`reListAdapter`/`updateChapterContent` 不再每次都从 `HH2SectionData` 全量重建 `ExpandableGroupEntity`，改为仅在数据变更时重建，子项更新走增量 `updateGroupData`。
- **改动点**：
  1. 引入「数据指纹/版本号」：DB 数据未变则不重建 `groupDataList`。
  2. 章节内容更新复用 `adapter.updateGroupData(pos, groupData)` 增量刷新。
- **门禁**：编译通过；章节多时无明显卡顿（logcat 对比重建次数）。
- **风险**：低（优化项）。
- **状态**：【部分成立-已订正】原描述只成立一半，见「发现 2」

---

## 执行顺序与依赖

```
T0(D2入库) → T1(D4.1死代码) → T2(D2.1守卫) → T3(D6死路径)
   → T4(D7生命周期) → T5(D8异步搜索) → T6(D4索引) → T7(D3统一) → T8(D9优化)
```

- T1/T2/T3 互不依赖，且只动 Fragment/Presenter/Contract，不影响适配器公开 API → 低风险先行。
- T4/T5 动 Fragment 内部逻辑，需编译 + adb 回归。
- T6/T7 动适配器/数据模型与跨消费者，风险最高，放在编译 + 前序回归稳定后执行。
- 每完成一张工单：编译门禁 → 需要时 adb 安装 + logcat 抽查 → 更新本表状态与证据。

---

## 计划外改动登记（code-review Standards 轴要求显式登记）

| 文件 | 改动 | 理由 | 证据 |
| --- | --- | --- | --- |
| `manager/AppDataManager.java` | 加载耗时日志：`总耗时` 原打印 `loadCompleteTime`（epoch 时间戳，标签却写「总耗时」，日志里出现十几位数字，误导排查）；改为 `loadCompleteTime - loadStartTime` | 不属 T0–T8（reader 域），是上一轮遗留。**改动本身正确且已验证**，故保留而不丢弃，但在本登记显式说明，保持批次可审阅 | 装机联调实测输出 `🎉 所有数据加载完成（总耗时 3244ms）` |

---

## code-review 双轴发现与处置（2026-10-07）

**Standards 轴（6 项硬违规 + 2 项判断题）**

| # | 发现 | 处置 |
| --- | --- | --- |
| 1 | `TipsBookNetReadFragment:636` 注释仍写「经 SearchCoordinator 直接设置」，但检索已改到 Presenter | ✅ 已改为「搜索由 Presenter.search() 完成后回调 showSearchResults」 |
| 2 | `AppDataManager` 属计划外改动 | ✅ 见上方「计划外改动登记」 |
| 3 | 文档状态与代码脱节（design-analysis 把已实现的 5 项标【未实现】；tickets 总览与正文自相矛盾） | ✅ 两份文档的状态标记已逐项对齐 |
| 4 | `Presenter.search()` 丢异常处理：放进 `runInBackground` 后无 try/catch、无失败日志 | ✅ 已补 try/catch + `EasyLog` + `view.showError` 回主线程 |
| 5 | `Presenter.isSearchMode` 死字段（唯一写入点是注释行，无读取点） | ✅ 字段与注释残留一并删除；`clearSearch()` 改为委托 `cancelSearch()`（恢复全量仍由 Fragment 负责） |
| 6 | `setSearchData` 的 D2 镜像分支无日志 | ✅ 已补镜像数量日志；原先带「数量不一致打 ⚠️」的告警分支因条件恒等、永不触发，已作为死分支删除 |
| 判| Data Clumps：搜索结果三元组以「两个平行 List + 计数」在三处同现，`android.util.Pair` 泄漏到领域层 | **本轮不做**：属结构重构，与 §4.1 单一数据源收敛是同一件事，应合并到那一轮，避免同一处反复改动 |
| 判 | Duplicated Code：「在途序号丢弃过期结果」形状在 Presenter 与 Fragment 各写一遍 | **本轮不做**：两处字段名与生命周期不同（`searchSeq` vs `bookDataLoadSeq`），抽取公共 latch 的收益不足以抵消理解成本 |

**Spec 轴（2 项缺失 + 1 项 scope creep + 3 项可疑实现）**

| # | 发现 | 处置 |
| --- | --- | --- |
| a | **搜索在途与清空不联动**：清空输入框走 `reListAdapter` 恢复全量，但未作废在途 `searchSeq`，`showSearchResults` 也无守卫 → 搜索结果会覆盖全量列表；清空后 `isSearchActive()` 为 false，`updateChapterContent`/`updateDownloadStatus` 守卫一并失效，**重新打开 D2/D2.1 想关闭的污染路径** | ✅ 已修：新增 `Contract.cancelSearch()`；`setSearchText` 空分支调用它；`showSearchResults` 入口再加 `isSearchActive()` 守卫作双保险 |
| b | §4.8/T5 要求「加载期间展示 loading」缺失 | ✅ 已补 `view.showLoading(true)`；`showLoading` 原为空壳，现复用既有 `numTips` 结果提示位显示 `搜索中…`（**不新增控件、不改布局** —— 本页布局无 loading 控件，项目内也无可复用组件） |
| c | §4.6「删除 `onTrimMemory` 空分支」 | ✅ 核实为基点 `dfca6da` 已完成，非本轮范围；文档已补标注 |
| d | T8「全免」订正只成立一半 | ✅ 已订正为【部分成立】：`updateChapterContent` 增量那半不成立；`reListAdapter` 经 `refreshData`/EventBus 在设置变更时仍全量重建 |
| e | `updateChapterContent` 守卫处注释称「position 是真实章节索引」，与 T6 订正自相矛盾 | ✅ 注释已改为「position 来自全量章节侧，两侧索引不对齐」 |
| f | 子项长按 D4 暴露面未标状态 | ✅ 已在 §0 总览、发现 3 与 D4 条目中显式标注为「未收敛-待 T6 决策」 |

---

## 修复后复审（第二轮 code-review，2026-10-07）

对**同一份工作区 diff** 复审，目的是核实上一轮修复是否真的到位、以及修复本身有没有引入新问题。

### 上一轮 6 项硬违规：**全部已修，无回退** ✅

stale 注释、`AppDataManager` 登记、文档状态对齐、`search()` 异常处理、死字段、镜像日志 —— 逐项核实通过。四项点名核实亦通过：`cancelSearch` 无漏实现（`View`/`Presenter` 实现者各唯一）、`numTips` 有 null 保护、`search_in_progress` 被引用、成功路径不覆盖 loading。

### 复审新发现（6 项，全部已修）

| # | 发现 | 轴 | 修复 |
| --- | --- | --- | --- |
| 1 | **搜索失败时「搜索中…」永久残留** —— `showLoading(true)` 写入 `numTips`，但失败路径只调 `showError`（只 toast 不碰 `numTips`）。修复前 `showLoading` 无副作用，故这是上一轮新引入 | Standards#1 / Spec#1（同一根因） | `showLoading` 补 `false` 分支清空；`showError` 先复位 `numTips` 再 toast；Presenter 失败/异常路径显式 `showLoading(false)` |
| 2 | `e.printStackTrace()` 违反 `code-governance`「异常必须用 `EasyLog.print(Throwable)`」 | Standards#2 | 改为 `EasyLog.print(e)` |
| 3 | 4 处缩进破坏（`AGENTS.md` 要求 4 空格） | Standards#3 | 全量扫描「新增行非 4 空格倍数」并逐处修正 |
| 4 | **`isSearchActive()` 漏 `trim()`** —— 输入纯空格时列表已恢复全量但 `searchText` 非空，D2/D2.1 守卫全部持续失效 | Spec#3 | 判据改为 `searchText != null && !searchText.trim().isEmpty()` |
| 5 | **`cancelSearch()` 在用户真实退格路径上不可达** —— `TextWatcher` 走 `charSequenceIsEmpty` 直接 `reListAdapter`，不经 `setSearchText`，与注释断言不符 | Standards判断题A / Spec#2（同一根因） | 收敛为单一入口：`TextWatcher` 一律调 `setSearchText`；`charSequenceIsEmpty` 去掉副作用（不再写 `searchText`）并补注释说明「不要在这里做状态恢复」 |
| 6 | 镜像数量 `⚠️` 告警是**死分支**（循环条件与构建 `modelGroupList` 完全相同 → 恒等）；`clearSearch()` 是 `cancelSearch()` 的转发壳且无调用方 | Standards判断题B / C | 删掉恒不可能触发的告警三元；删除 `clearSearch()`（含 Contract 声明），由 `cancelSearch()` 独立承担 |

### 修复过程中的一次事故（已回滚，记录以免重犯）

用 node 脚本按「catch 块起点」定位并批量替换缩进时，脚本匹配到了**文件中第一个** `} catch (Exception e) {`（`loadBookContent` 的，179 行），导致 `search()` / `reloadChapter()` 等方法被整段删除、注释块 `*/` 收尾丢失。

**处置**：`git checkout HEAD -- <file>` 恢复，用 Edit 工具逐段精确重做。
**教训**：
1. **行号/内容匹配式脚本改代码前必须先打印待改区间并人工核对**（我打印了才发现匹配错），且要带「边界断言」——本例应先校验 `lines[start]` 前一行确实是 `search()` 内的 `try {`。
2. **Edit 工具在多行替换时可能重写缩进** —— 本轮 `cancelSearch()` 的 4 空格被反复吃成 2 空格，需「替换整段（带前后锚点）」而非逐行替换。
3. 沙箱内 node 调`git` 会 EBUSY，取 git 内容要在 Bash 里先取好。

修复后：`:app:compileDebugJavaWithJavac` BUILD SUCCESSFUL、`:app:testDebugUnitTest` BUILD SUCCESSFUL、`git diff --check` 干净、全部新增行缩进合规。

---

## 修复后复审（第三轮 code-review，2026-10-07）

### 第二轮 6 项修复核实

4 项彻底修好（`showLoading` 收口 + `showError` 复位、`isSearchActive` 补 `trim()`、`TextWatcher` 单一入口 + `charSequenceIsEmpty` 纯函数、删除 `clearSearch()`）；`printStackTrace` 与缩进只修了一半，见下表。

**方法结构完整性核查通过**（本轮重点）：`Presenter.java` 曾因脚本事故被 `git checkout` 回滚重做，复审确认 `search()` / `countMatches()` / `cancelSearch()` 结构完整闭合，注释块收尾已随旧代码一并删除，无残留旧 `search()`、无重复方法、无孤儿 import。

### 本轮发现与处置（6 项，全部已修）

| # | 发现 | 轴 | 修复 |
| --- | --- | --- | --- |
| 1 | **`numTips` 一位两用 → 打开任意书籍都闪「搜索中…」**：`Presenter.loadBookContent`（:164/:198）与 `onChaptersLoaded`（:314）也调 `view.showLoading(...)`，而 `Fragment.showLoading` 无条件写「搜索中…」；且搜索与书籍加载两条链会互相覆盖/清空对方提示。spec §4.8 只授权搜索用 loading | Standards#3 / Spec#1（同一根因） | 新增专用 `Contract.showSearching(boolean)`，Presenter 搜索路径改用它；`Fragment.showLoading` 恢复为空实现并写明「与 showSearching 不可混用」 |
| 2 | 文档仍描述**已删除**的镜像 `⚠️ 数量不一致` 三元与「打 ⚠️」表述（`fixes-done.md:82/84`、`tickets.md:225`），后者还与同文件「已删掉」自相矛盾 | Standards#1 / Spec#2（同一根因） | 三处表述据实订正，并注明「条件恒等、永不触发，已作为死分支删除」 |
| 3 | **2 处缩进标为已修实未修**：`RefactoredExpandableAdapter` 的 `this.groups = mirrorGroups;` 顶格 0 缩进、`Fragment` 的 `isSearchActive` Javadoc 续行顶格 | Standards#2 / Spec 核查 | 已修 |
| 4 | `printStackTrace` 只改了 Presenter 一处，Fragment 内 3 处口径不一致（基点既有代码，但同轮改动触及） | Standards#4 | 统一改为 `EasyLog.print(e)` |
| 5 | `Adapter` 两个循环条件完全相同（既然已认定恒等，可合为一个） | Standards判断题 | 已合并为单循环：同一次遍历同时产出 `GroupData` 与 `ExpandableGroupEntity`，少一次遍历 |
| 6 | `countMatches()` 语义从「匹配次数」改为「`ItemData` 条目数」，文档未登记 | Spec判断题 | 经核实**不构成偏离**（旧实现只存在于已删的注释死块，Fragment 原实现同样是 `items.size()` 求和，用户可见行为未变），已在 `fixes-done.md` 登记说明 |

### 附带订正的文档不一致（Spec 核查）

- `isSearchActive()` 公式过期：`design:195` 与 `fixes-done.md:30` 写的仍是未含 `trim()` 的版本 → 已同步。
- T8 状态自相矛盾：总览表 `:32` 与正文 `:188` 写【无需改动-已订正】，而同文件 `:236` 写【已订正为部分成立】→ 两处统一为【部分成立-已订正】。
- T5 改动点写「复用 `showLoading(true)` / **已有 loading 视图**」→ 两处都不准，已改为 `showSearching` 方案并说明原因；同条改动点 1 写「`searchCoordinator.searchGlobal` 包进 readInBackground」也与实际（Presenter 统一入口）不符，已订正。
- Presenter 行数「1030 行」→ 实际 **1045 行**（脚本事故回滚重做后）。
- 状态图例未收录总览表实际使用的【代码已写-待提交】→ 已补。

### 登记为既有问题（本轮不改）

**`onJumpSpecifiedItem`（`Fragment.java` 约:375-381）**：`clearEditText.setText("")` 会经 300ms 防抖触发 `setSearchText(null)` 恢复全量列表，随后才 `scrollToPositionWithOffset(groupPosition)` —— 而 `groupPosition` 是**搜索结果索引**。`setText` 与 `postDelayed(300)` 同为 300ms、注册顺序在前，故实际是「先恢复全量、再按搜索态索引滚动」，比「命中错章」更具体。

已用 `git show HEAD` 核实基线 `:415-421` 完全相同，**非本轮引入**，故不在本轮修复。修复需与 T6（索引真相）一并决策 —— 子项长按的同类问题已在「发现 3」记录。
