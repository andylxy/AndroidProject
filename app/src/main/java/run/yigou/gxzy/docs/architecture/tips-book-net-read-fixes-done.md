# TipsBookNetRead 已完成修复记录（D1、D2、D2.1、D3、D4.1、D6、D7、D8）

> 本文件只记录**已落地**的修复，作为变更说明 / 验收依据。
> 仍未做的事项（D4 索引真相【阻塞待决策】、D9【部分成立】）见 `tips-book-net-read-design-analysis.md`；工单与逐条验收证据见 `tips-book-net-read-tickets.md`。
> 分析对象：`ui/reader/bookread/TipsBookNetReadFragment.java` + `ui/reader/adapter/RefactoredExpandableAdapter.java`。

## 状态总览

| 条目 | 状态 | 落点 |
|---|---|---|
| **D1** | 【已提交】`dfca6da` | `isSearchActive()` + 删除适配器 `setSearch/getSearch` 死桩 |
| **D2** | 【代码已写-待提交】 | `updateChapterContent` 守卫 + `setSearchData` 同步 `groups` 镜像（含一致性日志） |
| **D2.1** | 【代码已写-待提交】 | `updateDownloadStatus` 搜索态守卫 |
| **D3** | 【代码已写-待提交】 | 搜索统一走 `Presenter.search()`；Contract 回调改走新结构；删除 96 行注释死块 |
| **D4.1** | 【代码已写-待提交】 | 删 `currentIndex`、`findChapterIndexByTitle()`、`onHeaderClick` 搜索态死块 |
| **D6** | 【代码已写-待提交】 | 死路径清理（随 D3 一并实施） |
| **D7** | 【代码已写-待提交】 | EventBus 注册/注销移到 `onStart`/`onStop`；`bookDataLoadSeq` 在途序号 |
| **D8** | 【代码已写-待提交】 | 检索移出主线程 + `searchSeq` 在途序号 + `cancelSearch()` 联动 + 进行中提示 |

---

## D1. 搜索态判断走死桩，长按「重新下载」在搜索模式下索引错乱

### 根因
- `RefactoredExpandableAdapter.setSearch(boolean)` / `getSearch()` 是空桩，`getSearch()` 恒返回 `false`。
- `TipsBookNetReadFragment` 用 `if (adapter.getSearch()) return true;` 作为长按守卫 → 守卫永远不触发；在搜索结果上长按「重新下本章节」会用**过滤后的 groupPosition** 直接下标 `chapterList`，越界或命中错误章节。

### 修复（已提交 `dfca6da`）
- 删除适配器死桩 `setSearch/getSearch` 及其 5 处 Fragment 调用（`initView`/`initData`/`onClick`）。
- 长按守卫改为 `if (isSearchActive()) return true;`，新增私有 `isSearchActive()` = `searchText != null && !searchText.trim().isEmpty()`，与 `onHeaderClick` 的搜索态判定同源。
- 搜索态现在只由 Fragment 的 `searchText` 单一真相维护，消除「适配器空桩 + Fragment 字段」双轨。

### 附带清理（同批 `dfca6da`）
- 删除 `TipsBookNetReadFragment.onTrimMemory` 的空 `if (adapter != null) { /* 可加清理 */ }` 分支。
- 删除 `GroupModel.getExpandableGroups` 注释掉的 `// String EMPTY_STRING = ""`。
- 删除 `TipsBookNetReadFragment` 4 处空 `/**  */` Javadoc 块。

### 验证
- `:app:compileDebugJavaWithJavac` BUILD SUCCESSFUL。
- 行为：非搜索态下载 / 展开 / 点击逻辑零变化；搜索态长按不再误触「重新下载」。

---

## D2. `setSearchData` 后 `groups` 与 `groupDataList` 分离，下载回调污染搜索结果

### 根因
- `RefactoredExpandableAdapter.setSearchData` 只写 `groupDataList`，`groups`（旧结构）保持全量章节不变。
- 若搜索后某章下载完成，`TipsBookReadPresenter` 以**真实章节索引**回调 `updateChapterContent`，它读 `adapter.getmGroups()`（旧章节）构造 entity，再 `updateGroupFromEntity(groupPosition, ...)` 写回 `groupDataList` 的**同一 position**——而此时 `groupDataList` 是搜索结果。索引对不上 → 渲染错位 / 数据被覆盖（潜伏型，难以复现）。

### 修复（代码已写，待提交）
1. **主修复（拦截污染入口）**：`TipsBookNetReadFragment.updateChapterContent` 入口增加搜索态守卫：
   ```java
   // ✅ D2 修复：搜索态下列表展示的是搜索结果（groupDataList 已是过滤集），
   //    此时 Presenter 回调的 position 是真实章节索引，若继续写入会覆盖搜索结果同位置的项，
   //    造成数据污染。与 onHeaderClick 的搜索态守卫（"防止数据被覆盖"）保持同一口径：
   //    搜索态下不把章节下载结果应用到当前列表。非搜索态行为与修复前完全一致。
   if (isSearchActive()) {
       return;
   }
   ```
2. **根因加固（消除双源分离）**：`RefactoredExpandableAdapter.setSearchData` 在保留 `groupDataList` 原有构建（保留 ClickableSpan）不变的前提下，按搜索结果构建与 `groupDataList` 1:1 对应的 `groups` 镜像并赋值 `this.groups`：
   ```java
   // ✅ D2 修复：setSearchData 此前只写 groupDataList，导致 groups（旧结构）停留在全量章节，
   //    与 groupDataList 分离；后续读取 getmGroups() 会得到错误数据，引发索引错位。
   //    此处同步构建与 groupDataList 1:1 对应的 groups，使两个数据源保持一致。
   //    注意：仅补全 groups 的"结构镜像"（标题/展开态/空 children），
   //          groupDataList 的原有构建（保留 ClickableSpan）完全不动，搜索结果展示行为不变。
   //    绑定始终走 groupDataList，groups 仅用于 size/header 一致性，故 children 用空列表即可。
   ArrayList<ExpandableGroupEntity> mirrorGroups = new ArrayList<>();
   for (int i = 0; i < entityGroupList.size() && i < entityItemList.size(); i++) {
       run.yigou.gxzy.ui.reader.entity.GroupData srcGroup = entityGroupList.get(i);
       mirrorGroups.add(new ExpandableGroupEntity(
               srcGroup.getTitle() != null ? srcGroup.getTitle() : "",
               "",
               srcGroup.isExpanded(),
               new ArrayList<>()));
   }
   this.groups = mirrorGroups;
   EasyLog.print("RefactoredExpandableAdapter",
           "setSearchData 同步 groups 镜像: groups=" + mirrorGroups.size()
                   + ", groupDataList=" + groupDataList.size()
                   ```
使 `getmGroups().size()` 与 `groupDataList` 索引对齐。

> 原先此处还有一个「数量不一致打 ⚠️」的告警分支，但构建 `groups` 与 `groupDataList` 的循环条件完全相同、两个 size 恒等，该分支永不可能触发，已作为死分支删除。

### 边界确认
- `setSearchData` 仅被 `TipsBookNetReadFragment` 调用；`BookContentSearchActivity` 用另一个适配器（`RefactoredSearchAdapter`，走 `setmGroups`），故本次修复**无需触碰搜索书详情页**，爆炸半径仅限阅读页适配器。
- D2.1 已一并修复：`updateDownloadStatus` 入口加了同样的搜索态守卫（此前其调用方在搜索态已被 `return` 拦死、不可达，加守卫是为后续放开搜索态下载预留正确性）。

### 验证
- `:app:compileDebugJavaWithJavac` 通过。
- 非搜索态下载 / 展开 / 点击回归无变化。
- 装机联调：后端 `192.168.2.158:4321` 联通，阅读页章节列表完整渲染，全程零 FATAL / 零 Exception。

> ⚠️ D2 尚未提交：当前改动在工作区，需走安全提交后入仓。

---

## D3 / D8. 两套搜索路径 + 主线程同步检索（合并实施）

### 根因
- `TipsBookReadPresenter` 有两套 `search()`：一处在 `/* */` 注释块内（含 `view.showSearchResults` 调用，未编译生效），生效的那个是**空实现桩**。
- Fragment 侧自行持有 `SearchCoordinator` 并**在主线程同步**调用 `searchGlobal`（整本书 DB 读 + 过滤 + 高亮）→ 章节多的书卡 UI。
- 两条路分别走「新结构」和「旧结构」，是D2 索引错位的直接来源。

### 修复
1. **单一入口**：`Presenter.search(keyword)` 成为唯一搜索入口；`Contract.showSearchResults` 签名从 `List<ExpandableGroupEntity>` 改为 `(List<GroupData>, List<List<ItemData>>, int)`——与适配器绑定的 `groupDataList` **同构**，不再经旧结构中转。
2. **异步化（D8）**：检索放进 `ThreadUtil.runInBackground`，结果 `ThreadUtil.runOnUiThread` 回主线程渲染。
3. **在途序号**：`searchSeq` 保证只认最后一次结果；新增 `Contract.cancelSearch()`，用户清空搜索框时递增序号作废在途结果。
4. **双保险**：`showSearchResults` 入口再判`!isSearchActive()` 就丢弃 —— 防止漏掉 `cancelSearch` 的路径把搜索结果写进已恢复的全量列表。
5. **进行中提示**：`view.showLoading(true)`。`showLoading` 原为空壳，本页布局无 loading 控件、项目内无可复用组件，故复用既有 `numTips` 结果提示位显示 `搜索中…`（新增 `strings.xml` 的 `search_in_progress`），**不新增控件、不改布局**。
6. **异常处理**：子线程内`try/catch` 就地捕获并回主线程 `showError`，避免静默吞掉DB 读异常；入口/完成/失败/丢弃均有 `EasyLog`。
7. **清理**：删除 Fragment 的 `searchCoordinator` 字段与直调、Presenter 里 96 行注释死块（1039 → 997 行）、死字段 `isSearchMode`（唯一写入点是注释行）。

### 为什么第3、4 条必要（code-review 发现的真缺陷）
清空搜索时列表已恢复成全量章节，若在途结果仍回填，会覆盖全量列表；更麻烦的是清空后 `isSearchActive()` 为 `false`，`updateChapterContent` / `updateDownloadStatus` 的搜索态守卫**一并失效**，等于重新打开了 D2/D2.1 想关闭的污染路径。

### 验证
- `:app:compileDebugJavaWithJavac` / `:app:testDebugUnitTest` 通过。
- logcat 实证：`TipsBookReadPresenter: search() 开始…` → `=== SearchCoordinator.searchGlobal() ===` 跑在**线程 20606**（主线程 20248）→ `搜索完成` → UI 显示结果数。**异步化生效。**
- 清空搜索可恢复全量章节，「搜索态 ↔ 全量态」来回切换无崩溃。

---

## 末轮复审追加的修复（2026-10-07）

第二轮 code-review 后的追加修复，**前三项是上一轮修复自身引入的**：

### 1. loading 只有开始没有结束（搜索失败时「搜索中…」永久残留）

`showLoading(true)` 把「搜索中…」写进 `numTips`，但失败路径只调 `showError`（只 toast，不碰 `numTips`）→ 提示永不消失，列表也不被替换。

修复：
- `showLoading` 补 `false` 分支清空 `numTips`；
- `showError` 先复位 `numTips` 再 toast；
- `Presenter.search()` 的「结果为 null」与catch 两条失败路径显式 `showLoading(false)`。

成功路径由 `showSearchResults` 写「N个结果」覆盖，与失败路径互斥，不会互相抹除。

### 2. `e.printStackTrace()` → `EasyLog.print(e)`

`code-governance` 要求异常统一走 `EasyLog.print(Throwable)`。

### 3. `isSearchActive()` 补 `trim()`（真实缺陷）

原判据是「`searchText` 非空」。用户输入**纯空格**时，`setSearchText` 会因 `trim().isEmpty()` 走清空分支、把列表恢复成全量章节，但 `searchText` 仍是非空串 → `isSearchActive()` 为 true → `updateChapterContent` / `updateDownloadStatus` / `onHeaderClick` 的搜索态守卫**全部持续失效**，正是 D2/D2.1 想关闭的污染路径。

改为 `searchText != null && !searchText.trim().isEmpty()`，与 `setSearchText` 的分支判据一致 —— 搜索态判定与实际列表状态由此对齐。

### 4. 清空搜索收敛为单一入口

`TextWatcher` 原本走 `charSequenceIsEmpty(text)` → 直接 `reListAdapter`，**不经过 `setSearchText`**，导致 `presenter.cancelSearch()` 在用户真实退格路径上被绕过（只靠 `showSearchResults` 的双保险兜住），且与 `Contract`/`Presenter` 里「清空搜索框时必须调用」的注释断言不符。

修复：`TextWatcher` 一律调 `setSearchText`（空 → `setSearchText(null)`）；`charSequenceIsEmpty` 去掉副作用（不再写 `searchText`），并在 Javadoc 里写明「不要在这里做状态恢复，否则会与 setSearchText 形成双入口」。

### 5. 清理两处死代码

- 镜像数量的 `⚠️` 告警是**死分支**：循环条件与构建 `modelGroupList` 的条件完全相同，两个 size 恒等，永不可能触发 —— 删除。
- `clearSearch()` 退化为 `cancelSearch()` 的转发壳且全仓无调用方 —— 连同 `Contract` 声明一并删除。

### 验证

`:app:compileDebugJavaWithJavac` BUILD SUCCESSFUL；`:app:testDebugUnitTest` BUILD SUCCESSFUL；`git diff --check` 干净；新增行缩进全量扫描合规。

---

## 第三轮复审追加的修复

### 1. `showSearching` 与 `showLoading` 分离（唯一有行为影响的一项）

上一轮把 `Fragment.showLoading` 从空壳改成写「搜索中…」，但 `showLoading` **被两条链路复用**：

- 书籍加载：`Presenter.loadBookContent`（:164 / :198）、`onChaptersLoaded`（:314）
- 搜索：`Presenter.search`（:578）

结果：**打开任意书籍都会在结果提示位闪「搜索中…」**，且两条链会互相覆盖 / 清空对方的提示。

修复：新增专用 `Contract.showSearching(boolean)`，Presenter 搜索路径改用它；`Fragment.showLoading` 恢复为空实现，并在 Javadoc 里写明「与 showSearching 不可混用，本方法被 loadBookContent / onChaptersLoaded 复用」。spec §4.8 只授权搜索用 loading，故这是收敛到 spec 而非扩张。

### 2. `setSearchData` 两个循环合并为一个

`groups` 镜像与 `groupDataList` 的构建循环条件完全相同（这是上一轮判定「⚠️ 告警永不可能触发」的依据），故合并为一次遍历，同时产出 `GroupData` 与 `ExpandableGroupEntity`，少一次遍历。原先那个恒不可能触发的 `⚠️` 告警分支已删除。

### 3. `Adapter` 的 `this.groups = mirrorGroups;` 缩进由顶格 0 修正为 8 空格

同轮一并把 `Fragment` 内 3 处 `e.printStackTrace()` 统一为 `EasyLog.print(e)`（`code-governance` 要求异常统一走 `EasyLog.print(Throwable)`），消除「Presenter 改了、Fragment 没改」的口径不一致。

### 4. `countMatches()` 的语义说明（不构成行为变更）

现实现统计的是 `ItemData` 条目数。旧实现（在已删除的注释死块里）用的是 `SearchKeyEntity.getSearchResTotalNum()`（匹配次数），但 **Fragment 原实现同样是 `items.size()` 求和** —— 也就是说本轮重构前后用户可见行为一致（「N个结果」一直是「命中条目数」），只是把散在 Fragment 的统计收敛到 Presenter 单点。故不构成 spec 偏离，此处登记以免日后误判为行为变更。
