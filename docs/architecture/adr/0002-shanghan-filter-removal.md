# ADR-0002：移除宋版伤寒章节截取，章节内容一律按书籍 ID 全量返回

- **状态**：待确认（proposed）— 已经过三轮评审（§9 第一轮清单完整性、§10 第二轮持久化/竞态、§11 第三轮删除区间与形态核查）
- **日期**：2026-10-08
- **决策者**：用户（产品决策）
- **影响域**：阅读页章节列表、阅读页内搜索、书内搜索页、阅读设置 UI
- **编号说明**：沿用全仓 ADR 序列（0001 在 `docs/database/adr/0001-dbservice-design.md`，属 database 域；本篇属 reader 域，故置于 `docs/architecture/adr/`）

---

## 1. 背景（Context）

### 1.1 现状：同一套截取规则被复制了三份

宋版伤寒（`AppConst.ShangHanNo = "10001"`）原有一套「按设置截取章节范围」的逻辑，规则是：

```java
int start = 0, end = size;
if (!isSong_JinKui()) {
    if (!isSong_ShangHan()) { start = 8; end = Math.min(18, size); }
    else                     { end = Math.min(26, size); }
} else {
    if (!isSong_ShangHan()) { start = 8; }
}
return new ArrayList<>(contentList.subList(start, end));
```

这套规则在**三个地方各写了一遍**（`Duplicated Code`），且触发条件的写法互不相同：

| # | 位置                                                        | 触发条件写法                                                                                            | 作用      |
| - | --------------------------------------------------------- | ------------------------------------------------------------------------------------------------- | ------- |
| 1 | `TipsBookReadPresenter.filterShanghanContent` (:877-913)  | 全局布尔 `isShanghanBook`（由 :298 处 `ShangHanNo.equals(bookId)` 调 `setupShanghanContentListener()` 置真） | 阅读页章节列表 |
| 2 | `SearchCoordinator.filterShangHanData` (:174-211)         | `ShangHanNo.equals(bookId)` 直接判断                                                                  | 阅读页内搜索  |
| 3 | `BookContentSearchActivity.filterShangHanData` (:808-848) | `SHANGHAN_BOOK_ID.equals(bookId)` 直接判断                                                            | 书内搜索页   |

> 已核实：全仓 `subList` 用于章节截取的只有这三处（`:909` / `:207` / `:844`），不存在第四份实现。

**注意 #1 的隐患**：它用「全局可变布尔字段」而非在 `getChapterContentList()` 里直接判 `bookId`。字段一旦置真不会自动复位，若换书时未走 :298 分支，残留的 `true` 会让**任意一本书**都被截取。这是三处里最脆弱的一环。

### 1.2 触发问题的直接原因

默认设置是 `setSong_JinKui(true); setSong_ShangHan(false);`（`ManagerSetting:41-42`）→ 命中 `else { if (!isSong_ShangHan()) start = 8; }` → **默认就返回 `subList(8, size)`**，即章节列表默认从第 9 章开始。

由此产生了第五轮 code-review 修掉的 P2 缺陷：**显示列表是全量章节的截断片段**，而搜索侧按 `allChapters`（全量）反查下标 → 两套坐标系 → 搜索态跳转用全量坐标滚到短列表，越界被静默忽略。

### 1.3 决策动因

截取功能本身不再需要：章节内容应**按书籍 ID 全量返回**，不再按设置截取任何区间。

---

## 2. 决策（Decision）

**移除宋版伤寒章节截取功能，三处实现全部删除；驱动它的两个设置项（开关 UI + 字段 + 默认值 + 事件字段）连同因此产生的死代码一并删除。**

删除后：`getChapterContentList()` 与两处搜索都按 `bookId` 返回/检索该书的全部章节，不做任何区间截取。

### 2.1 为什么必须三处全删（而不是只删阅读页）

只删阅读页会造成**功能不一致**：阅读页显示全部 26 章，但两处搜索仍只检索第 9–18 章 → 用户看得见第 1–8 章，却搜不到其中的内容。

### 2.2 为什么开关也要删

过滤删除后，「宋版伤寒」「宋版金匮」两个开关不再产生任何效果。**保留无效果的开关 = 用 UI 骗用户**，故连开关、字段、默认值、事件字段一并删除。

---

## 3. 删除清单（9 项，已逐条 grep 核实；另有 §3.3 注释清理与 §5.3 文档同步）

### 3.1 过滤实现与其直接依赖

| # | 文件                               | 删除内容                                                                                                                                                                                                    |
| - | -------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 1 | `TipsBookReadPresenter.java`     | `:68` `isShanghanBook` 字段（含行尾注释）；② `:848-854` `setupShanghanContentListener()` 整方法（含 javadoc）；③ `:297-300` **整个** `if (AppConst.ShangHanNo.equals(bookId)) { setupShanghanContentListener(); }` 包裹块（注释+if+调用+`}`，只删 `:299` 会留空 if）；④ `:869-872` `getChapterContentList()` 内 `if (isShanghanBook) { … }` 分支（**仅这 4 行，切勿扩到整个方法**）；⑤ `:877-913` `filterShanghanContent()` 整方法（含 javadoc，`:913` `}` 收尾）；⑥ `:22` `import ...AppConst;` 在 `:298` 删后变未使用，一并删                              |
| 2 | `SearchCoordinator.java`         | `:93-96` `if (ShangHanNo.equals(bookId)) { allContent = filterShangHanData(allContent); }`；② `:174-211` `filterShangHanData()` 整方法（含 javadoc，`:211` `}` 收尾）；③ `searchGlobal` 删 `// 2.` 后步骤编号 `1/3/4/5` 断裂，须把 `3/4/5` 重编号为 `2/3/4`（见 §3.3）；④ 本文件对 `FragmentSetting` 用全限定名（`:183`），随方法删除自然消失，无 import 需清理                                                                        |
| 3 | `BookContentSearchActivity.java` | `:752-755` `if (SHANGHAN_BOOK_ID.equals(bookId)) { … }`；`:808-848` `filterShangHanData()` 整方法（含 javadoc，`:848` `}` 收尾，**初版 `:812-845` 少算 3 行会截断方法**）；`:88` `SHANGHAN_BOOK_ID` 常量；`:93`/`:98`/`:103` 三个索引常量（仅被方法内 `:829/:830/:833/:838` 引用）；`:603` `fragmentSetting` 字段 + `:229` 赋值 + 对应 `import ...FragmentSetting;` |

> **#3 的两点订正**（初版写错，已核实修正）：
>
> - `SHANGHAN_BOOK_ID`（:88）全仓**只被 `:753` 引用**，应**无条件删除**。初版所写的「仅当 `:231` 之外无引用时」是个错误条件——`:231` 用的是 `AppConst.ShangHanNo`，与本常量无关。
> - `SHANGHAN_JINKUI_START_INDEX` / `SHANGHAN_JINKUI_END_INDEX` / `SHANGHAN_MAIN_END_INDEX`（:93/:98/:103）仅被 `:829/:830/:833/:838` 引用，全在待删方法内 → 随方法删除后成死常量，**必须一并删**。
> - `fragmentSetting`（:603/:229）读点仅 `:816-836`，全在待删方法内 → 整字段变死，一并删。

### 3.2 设置项（字段 / 默认值 / UI / 事件）

| # | 文件                                          | 删除内容                                                                                                                                                                                                                                                          |
| - | ------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 4 | `FragmentSetting.java`                      | `song_ShangHan` / `song_JinKui` 两个字段及其 getter/setter。**保留** `shuJie`                                                                                                                                                                                          |
| 5 | `ManagerSetting.java`                       | `:41-42` 两行默认值设置。**保留** `:43` `setShuJie(false)`                                                                                                                                                                                                              |
| 6 | `TipsSettingFragment.java`                  | `:23-24` `sb_setting_sh` / `sb_setting_jk` 字段；`:26-27` `sb_setting_sh_switch` / `sb_setting_jk_switch` 字段；`:63-66` 四处 `findViewById`；`:85-86` 两个 `setOnCheckedChangeListener`；`:105-123` 显隐 / 回填 / 文案；`:148-171` 变更处理。**保留** `sb_setting_shu_jie_switch` 全部逻辑 |
| 7 | `tips_setting_fragment.xml`                 | `:41-70` **两个 SettingBar 容器整块删**（含 `:70` 的 `</SettingBar>` 闭合；只删内部的 SwitchButton——只删 `:49`/`:64` 会留下两条空白 UI；初版 `:41-67` 少算 3 行会留 `:68-70` 无配对闭合标签 → XML 解析失败）                                                                                                                                         |
| 8 | `TipsSettingChangedEvent.java` | `shanghan_Notification` / `jinkui_Notification` 两个字段及其 getter/setter；并修正 `:4` 类注释（原文称「用于通知伤寒论、金匮要略、书解等模块」，删后实际只剩「术解 / 通用刷新信号」）                                                                                                                                |
| 9 | `TipsSettingFragment.java`                  | `:33` `bookId` 字段与 `:77-78` 赋值（`bookId` 仅 `:105` 使用，随 #6 删除后变死字段）                                                                                                                                                                                             |

### 3.3 失效注释与形态清理（必须同步清理）

`code-governance` 要求「旧注释失真必须同步修正」。以下注释/形态在删除后**与实现矛盾**，尤其前 3 条断言了「显示列表可能是截断片段」——这正是本次要消除的前提：

| 文件:行                                                      | 现状                                                                                                                                  | 处置                               |
| --------------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------- | -------------------------------- |
| `SearchCoordinator.java:39-43`                            | 「会经 `filterShanghanContent` 返回 `subList(start, end)`」「显示列表只是全量章节的一个截断片段」「被过滤掉的章节反查不到」                                               | 改为「显示列表恒等于该书全量章节」                |
| `SearchCoordinator.java:155`                              | 「两者长度可能不同（宋版伤寒过滤）」                                                                                                                  | 删除该前提说明                          |
| `GroupData.java:16`（entity 版）                             | 「可能是全量章节的过滤片段，如宋版伤寒的 `subList`」                                                                                                     | 删除「如宋版伤寒的 subList」               |
| `TipsBookNetReadFragment.java:378`                        | 「显示列表即 `presenter.getChapterContentList()`，可能是全量章节的过滤片段」                                                                            | 同上                               |
| `TipsBookReadPresenter.java:849-850`、`:859`、`:869`、`:878` | 「宋版伤寒内容监听器」「支持宋版伤寒过滤」等                                                                                                              | 随 #1 一并清                         |
| `TipsBookNetReadFragment.java:196`                        | 「✅ 宋版伤寒逻辑已移至 Presenter」                                                                                                             | 删（改为说明 `fragmentSetting` 现仅用于术解） |
| `TipsBookNetReadFragment.java:239-240`、`:245`            | 「✅ 宋版伤寒监听器已移至 Presenter / 不需要 Fragment 中设置」两处注释                                                                                      | **仅改注释**，保留 `:242` `@Subscribe` 与 `:243-250` `onEvent` 方法体（误删会破坏刷新机制 + 编译失败） |
| `BookContentSearchActivity.java:228`、`:752`、`:808-811`    | 「获取伤寒论过滤设置」「伤寒论特殊过滤逻辑」                                                                                                              | 随 #3 一并清                         |
| **`AppApplication.java:213-218`**                         | **「为什么敢改成异步：`fragmentSetting` 的两个使用方（`BookContentSearchActivity` 与 `TipsBookReadPresenter`）都有 null 分支兜底」——这两个使用方都在删除清单里，删后该论据完全失真** | **必须改**（本轮新增，见 §5.2 冷启动竞态）       |
| `TipsSettingChangedEvent.java:4`             | 类注释                                                                                                                                 | 见 #8                             |
| `SearchCoordinator.java:82`/`:98`/`:104`/`:116`           | `searchGlobal` 的 `// 1.` `// 3.` `// 4.` `// 5.` 步骤编号（删 `// 2.` 后断裂）                                                                           | 把 `3/4/5` 重编号为 `2/3/4`              |
| `TipsBookReadPresenter.java:22`、`TipsSettingFragment.java:14` | `import ...AppConst;`（两文件删完后 `AppConst` 仅在待删 `:298`/`:105` 使用，变未使用 import）                                                                  | 一并删 import（否则 checkstyle/编译告警）       |
| `TipsSettingFragment.java:105-124`                        | `showSettingSwitch()` 内依据 `AppConst.ShangHanNo` 控制 sh/jk 开关的可见性/勾选/文案                                                                           | 随 #6 一并删（仅留 `:112` shuJie 勾选）        |

### 3.4 明确保留（勿误删 —— 已逐条核实有真实用途）

| 项                                                                       | 保留理由                                                                              |
| ----------------------------------------------------------------------- | --------------------------------------------------------------------------------- |
| `FragmentSetting.shuJie` 与 `sb_setting_shu_jie_switch`                  | 仍被 `TipsBookNetReadFragment:200` 与 `:254` 真实读取，与截取无关                              |
| `TipsSettingFragment.fragmentSetting`（:36/:103）                         | 仍被 `:112` `isShuJie()` 与 `:145` `setShuJie()` 使用                                  |
| `TipsBookNetReadFragment.onEvent(TipsSettingChangedEvent)` | 术解开关切换后依赖它 `refreshData()` 刷新列表，仍需保留                                              |
| `AppConst.ShangHanNo` 常量                                                | `BookContentSearchActivity:231` 仍有用途。（初版举的 `TipsSettingFragment:105` 是错的——那行本身要删） |
| `TipsSettingFragment.newInstance(BookArgs)` 签名                          | `TipsFragmentActivity:487` 调用，属外部门面，签名保留                                          |
| `AppApplication.fragmentSetting` 与 `ManagerSetting` 存取                  | 仍承载 `shuJie`，不是截取专用                                                               |
| `TipsFragmentActivity:424` `CASE_TAG_SHANGHAN = 5`                      | 用于「单位标签显隐」，与章节截取无关，勿误删                                                            |

---

## 4. 被否决的替代方案

| 方案                       | 否决理由                                        |
| ------------------------ | ------------------------------------------- |
| 只删阅读页那一处                 | 阅读页显示全部、搜索仍只搜 8–18 章，用户看得见却搜不到，功能不一致        |
| 保留过滤，改为「显示列表与搜索统一按过滤后坐标」 | 治标不治本：仍依赖 `subList` 与全局布尔字段，且产品上已确认该截取功能不需要 |
| 保留开关但改作标注/高亮             | 用户未提出新语义，属凭空发明需求                            |
| 保留开关不动，只删过滤              | 留下完全无效果的 UI，等同欺骗用户                          |

---

## 5. 后果（Consequences）

### 5.1 正面

- 章节列表与两处搜索**坐标系天然统一**（都等于全量），第五轮 P2 的根因（截断片段）彻底消失——`chapterIndex` 不再有「显示列表 ≠ 全量」的可能。
- 消除三份重复的 `Duplicated Code`、三处魔法数（`8` / `18` / `26`）、以及第 #1 处「全局布尔字段不复位」的隐患。
- 伤寒论（10001）默认可见全部章节，不再从第 9 章开始。

### 5.2 负面 / 风险

| 风险                                                                                                                                                                                                                                                                                                                       | 评估                                                                                                                      |
| ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------------------------------------------------------------------------------------- |
| **🔴 持久化兼容（初版评估错误，已订正）**：`FragmentSetting implements Serializable` 但**没有显式 `serialVersionUID`**（已核实：全文只有 3 个字段，无 UID 常量）。删字段会改变 JVM 默认计算的 UID → 读旧缓存抛 `InvalidClassException` → `CacheHelper.readObject`（`:59-68`）**删除缓存文件**并返回 null → `ManagerSetting.getFragmentSetting()` 重建默认值 → **用户已保存的 `shuJie=true` 被重置为 false** | **中**。初版写的「缺失字段取默认值、不会抛异常」仅在**有显式 UID** 时成立，本类没有，故该评估错误。**修法见 §6 步骤 0**：删字段前先固化当前 UID，即可回落到「缺失字段取默认值」的安全路径，`shuJie` 零丢失 |
| **🔴 冷启动空值竞态（初版完全未评）**：`AppApplication:139` 初值为 `null`，`:219-231` 后台读文件、主线程稍后赋值。`:213-218` 的注释称「两个使用方都有 null 兜底」，而这两个使用方（`BookContentSearchActivity`、`TipsBookReadPresenter`）**都在删除清单里** —— 安全网随删除一起消失。剩余读取点 `TipsBookNetReadFragment:200`/`:254` 与 `TipsSettingFragment:103`/`:112` 均**无判空**                            | **中**。严格说这是**既有竞态**（非删除首次制造），但删除后注释失真且失去兜底说明。处置：`:213-218` 注释**本轮必改**（列入 §3.3）；**空值治理另开票**，不混入本次删除提交（避免扩大范围）            |
| **行为变更**：伤寒论默认显示范围变大                                                                                                                                                                                                                                                                                                     | 这是本次决策的目标，非缺陷                                                                                                           |
| **搜索结果变多**：两处搜索不再截取，命中范围扩大                                                                                                                                                                                                                                                                                               | 符合预期；若大书搜索耗时明显，需另行评估（不在本轮）                                                                                              |
| **删除面较大**（4 个业务类 + 3 个设置类 + 1 个布局 + 1 个事件类 + 注释若干）                                                                                                                                                                                                                                                                       | 已逐处 grep 定位；一次性提交，便于整体回滚                                                                                                |
| **编译风险**：删 XML 后 Java 侧 `findViewById` 会失效                                                                                                                                                                                                                                                                               | 已由 #6 覆盖；两者必须同批删                                                                                                        |

### 5.3 与既有文档的关系（属同一次变更的一部分）

`docs-sync.instructions.md` 要求「同步更新文档须视为同一次变更的一部分」。以下文档须**与代码同批**订正：

- `tips-book-net-read-design-analysis.md` — D4 条目、§5 阶段 2.5 中「显示列表可能是过滤片段（宋版伤寒）」的论述，改为「过滤已移除，显示列表恒等于全量」。
- `tips-book-net-read-tickets.md` — 「T6 实施记录」的「前提验证」「已知局限」两节中涉及截断片段的表述，同上订正。
- `SearchCoordinator` / `GroupData` / `TipsBookNetReadFragment` 里的「显示列表」措辞**本身仍正确**（全量也是一种显示列表），但支撑它的「可能是截断片段」理由失效，须按 §3.3 回改。

---

## 6. 实施顺序（避免中间态编译不过）

**步骤 0（必须先做，否则会丢用户设置）**：在**删除任何字段之前**，先给 `FragmentSetting` 固化 `serialVersionUID`。  
原因是该类当前没有显式 UID，删字段会改变 JVM 默认计算的 UID，导致旧缓存反序列化失败并被 `CacheHelper` 删除（详见 §5.2）。取值方式（在改动前的代码上执行其一）：

- `serialver run.yigou.gxzy.base.args.FragmentSetting`（JDK 工具）
- 或在 App 内临时打印 `java.io.ObjectStreamClass.lookup(FragmentSetting.class).getSerialVersionUID()`

拿到值后写成 `private static final long serialVersionUID = <该值>L;`，再执行下面的删除。**不做这一步，`shuJie` 的用户设置会被静默重置。**

1. **先删 Java 侧引用**（#6 `#9` 的字段 / findViewById / 监听器 / 变更处理），再删 XML（#7）—— 反过来会留下 `findViewById` 找不到 id 的中间态。
2. 删三处过滤实现（#1 #2 #3）及其死常量、死字段。
3. 删设置项字段与默认值（#4 #5 #8）。
4. 清理失效注释（§3.3）。
5. 同步订正三份 md 文档（§5.3）。
6. 编译 → 定向单测 → grep 零残留 → 门禁检查 → 装机验证。

---

## 7. 回滚方式

一次性提交，直接 `git revert <commit>` 即可完整回退。无需数据迁移（旧设置文件即使残留缺失字段也能正常反序列化）。

---

## 8. 验证门禁

1. `./gradlew.bat :app:compileDebugJavaWithJavac -PServerType=test`
2. 定向单测 `:app:testDebugUnitTest --tests "run.yigou.gxzy.manager.*" -PServerType=test`
3. **零残留 grep**（应全部无输出）：
   ```bash
   grep -rn "filterShanghanContent\|filterShangHanData\|isSong_ShangHan\|isSong_JinKui\|song_ShangHan\|song_JinKui\|isShanghanBook\|SHANGHAN_BOOK_ID\|SHANGHAN_JINKUI_START_INDEX\|SHANGHAN_JINKUI_END_INDEX\|SHANGHAN_MAIN_END_INDEX\|sb_setting_sh\|sb_setting_jk" app/src/ --include=*.java --include=*.xml
   ```
4. `git diff --check` 干净；逐文件比对 `--numstat` 与 `--ignore-cr-at-eol --numstat` 确认零行尾噪音
5. 新增行缩进扫描（**注意**：`git diff --check` 查不出缩进，须另行扫描前导空格）
6. 装机联调：
   - 打开伤寒论（10001），确认**第 1 章可见**（此前被截掉）
   - 阅读页内搜索与书内搜索页结果一致
   - 设置页不再显示两个宋版开关（且不留空白条），术解开关仍可用且切换后列表刷新

---

## 9. 清单完整性评审记录（2026-10-08）

对初版 8 项清单做过一轮双轴评审，补出以下遗漏（均已 grep 核实后并入 §3）：

| 遗漏                                                                    | 初版问题                              |
| --------------------------------------------------------------------- | --------------------------------- |
| `BookContentSearchActivity` 三个索引常量（:93/:98/:103）                      | 完全未列                              |
| `BookContentSearchActivity.fragmentSetting`（:603/:229）+ import        | 未进清单，只在备注里泛泛提醒                    |
| `TipsSettingFragment` 字段声明 / findViewById / 监听器（:23-27、:63-66、:85-86） | 只列了逻辑段，漏了声明与绑定 → **删 XML 后会编译失败** |
| `TipsSettingFragment.bookId`（:33、:77-78）                              | 未列                                |


| XML 应删 `:41-67` 整块 SettingBar | 只列了 `:49`/`:64` 两个 SwitchButton → 会留两条空白 UI |  
| `SHANGHAN_BOOK_ID` 的删除条件写错 | 误把 `:231`（实为 `AppConst.ShangHanNo`）当作本常量引用 |  
| §3.3 失效注释清理 | 初版完全没有 |  
| 实施顺序 | 初版没有（现为 §6） |

**灰色地带（原本轮不擅自决定；已在 08 号清理票解决）**：

- `TipsSettingFragment.bookArgs`（:41）在删 `bookId` 后变成「只写不读」，但 `newInstance(BookArgs)` 是 `TipsFragmentActivity:487` 依赖的外部门面 → **保留签名与字段**，不因内部清理破坏外部契约。
- `TipsSettingChangedEvent`（原 `TipsFragmentSettingEventNotification`）删掉 sh/jk 两字段后，事件对象零信息量、仅作「触发 refreshData」的信号；`shuJie_Notification` 字段全仓从未被赋值（既有的死字段）。**已在 08 号清理票解决**：删除死字段、类名改为 `TipsSettingChangedEvent`、退化为零载荷标记事件（接收方仍直接重读 `fragmentSetting` 刷新，零行为变化）。

---

## 10. 第二轮评审记录（2026-10-08）

核验前两轮已并入的修复是否落在代码里，结果：① 持久化兼容评估被推翻——`FragmentSetting` 无显式 `serialVersionUID`（已 grep 确认全文无此常量），删字段会改变 JVM 默认 UID → 读旧缓存抛 `InvalidClassException` → `CacheHelper.readObject` 删缓存文件 → `ManagerSetting` 重建默认值 → 用户 `shuJie=true` 被静默重置；修法为 §6 步骤 0（删字段前先固化 UID）。② 冷启动空值竞态：`:213-218` 注释以「两个使用方都有 null 兜底」为异步加载论据，而这两个使用方都在删除清单里，删后论据失真；注释列入 §3.3 必改，空值治理另开票。③ 补遗漏注释 `TipsBookNetReadFragment:196`；§5.3 定位订正为 `design-analysis.md:131` 与 `tickets.md:409`。上述 ①②③ 已分别落实到 §5.2 / §3.3 / §5.3，本节仅作记录。

## 11. 第三轮评审记录（2026-10-08）—— 修正 8 处删除区间错误（含 7 处会编译失败）

本轮 Spec 轴发现初版与二版清单的**删除区间普遍少算行**，直接按原行号删会留下孤立 `}` 或误删活代码；Standards 轴由主代理自跑（子代理因限频未返回），补充形态清理项。

### 11.1 编译失败级错误（必须订正）

| 位置 | 原 ADR 行号 | 正确行号 | 后果 |
| --- | --- | --- | --- |
| `tips_setting_fragment.xml` 两块 SettingBar | `:41-67` | `:41-70`（`:70` 才是 `</SettingBar>` 闭合） | 只删到 `:67` 会留 `:68-70` 的 `</SettingBar>` 无配对开标签 → XML 解析失败 |
| `TipsBookReadPresenter.filterShanghanContent` | `:880-911` | `:877-913`（含 javadoc，`:913` 是方法 `}`） | `:912-913` 被孤立 → 编译失败 |
| `SearchCoordinator.filterShangHanData` | `:177-210` | `:174-211`（含 javadoc，`:211` 是方法 `}`） | `:211` `}` 被孤立 → 编译失败 |
| `BookContentSearchActivity.filterShangHanData` | `:812-845` | `:808-848`（含 javadoc，`:848` 是方法 `}`） | `:846-848` 被孤立 → 编译失败 |
| `getChapterContentList` 内 if 分支 | `:861-873` | `:869-872`（仅 if 块） | `:861-873` 会连同「null 守卫 + 转换调用」一起删掉 → 方法体被破坏 |
| `:297-300` 包裹 if | 只列 `:299` 调用 | 整个 `:297-300` | 只删调用留空 `if (...) {}` → 死代码 |
| `TipsBookNetReadFragment:239-245` | 标「删」 | **只改注释 `:239-240`/`:245`，保留 `:242` `@Subscribe`+`:243-250` `onEvent`** | 按原表删会删掉 `onEvent` 方法头 → 刷新机制失效 + 编译失败 |

### 11.2 形态清理（非编译阻断，但属同次变更）

- `searchGlobal` 删 `// 2.` 后步骤编号 `1/3/4/5` 断裂 → 重编号为 `2/3/4`（§3.3）。
- 两个文件（`TipsBookReadPresenter:22`、`TipsSettingFragment:14`）的 `import ...AppConst;` 在删完后变未使用 → 一并删。
- `TipsSettingFragment.showSettingSwitch()` 的 `:105-124`（sh/jk 可见性/勾选/文案）随 #6 删除；方法名 `showSettingSwitch`（复数）删后仅服务单个术解开关，可顺手改名 `showShuJieSwitch`（可选，不阻断）。

### 11.3 已确认无遗漏的维度

- `TipsSettingFragment.onCheckedChanged` 删 sh/jk 分支后仅留 shuJie 分支 + 保存 + post 事件；事件对象零字段但仍触发 `onEvent→refreshData`，刷新链路 intact。
- `BookContentSearchActivity.searchInBook` 删 `:752-755` 后 `contentList` 直接供 `:757` 搜索，无对「过滤后长度」的依赖；`:766` 空结果守卫仍有效。
- `FragmentSetting` 删字段后仅剩 `shuJie` 单布尔，仍是 CacheHelper 持久化的设置包，保留（不在本轮重构）。
- `TipsSettingChangedEvent`（原 `TipsFragmentSettingEventNotification`）删 sh/jk 字段后为零字段信号对象，仍能触发刷新；死字段与类名失配问题已在 08 号清理票完成（§9 灰色地带已记，现已解决）。

## 12. 术语表（Glossary）

| 术语                   | 定义                                                                             |
| -------------------- | ------------------------------------------------------------------------------ |
| **显示列表**             | 适配器当前绑定的章节列表，即 `presenter.getChapterContentList()` 的返回值。本 ADR 落地后恒等于该书全量章节     |
| **宋版伤寒截取**           | 按 `song_ShangHan` / `song_JinKui` 两个设置对伤寒论章节列表做 `subList(start, end)` 的旧逻辑。已移除 |
| **chapterIndex**     | 分组对应的章节下标。基准是显示列表；本 ADR 落地后与全量章节下标恒等                                           |
| **ShangHanNo**       | `AppConst.ShangHanNo = "10001"`，伤寒论的书籍 ID。常量保留，仅不再用于截取                         |
| **SHANGHAN_BOOK_ID** | `BookContentSearchActivity` 内部对 `AppConst.ShangHanNo` 的别名常量，随截取逻辑一并删除          |
| **shuJie（术解）**       | 与截取无关的另一项阅读设置，本轮保留                                                             |
