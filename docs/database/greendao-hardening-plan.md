# GreenDAO 加固方案（AndroidProject-old · 本地关系库）

> **本文是本地关系库的唯一设计文档**：现状事实、风险登记、加固方案、决策记录、验证方法与实施边界全在这里。
> 面向开发者的**操作指南**（怎么新增实体、怎么排障）在 `greendao-guide.md`；两者各自可独立阅读，
> 交叉引用只作定位补充。
>
> **决策基线**：本方案**只做 GreenDAO 加固**——不换 ORM、不引入新依赖、不改库文件名与版本号机制。
>
> **⚠️ 行号是取证快照（2026-10-06）**：`DataRepository.java`、`AppDataManager.java`、`DbService.java`、
> `BaseService.java` 当时**存在未提交改动**，行号已随之漂移（`DataRepository` 后移约 18 行）。
> 引用这些文件时**以符号名（类 / 方法）为准**，行号仅作定位提示。
> 重新测量：`grep -rn "<符号名>" app/src/main/java/run/yigou/gxzy/`。
>
> **目录**
> §1 决策 · §2 现状事实基线 · §3 风险登记与加固项 · §4 错误处理 · §5 依赖管理 ·
> §6 验证方法 · §7 实施清单 · §8 实施边界 · §9 附录 A 术语表 · §10 附录 B 变更历史

---

## 1. 决策

### 1.1 背景：为什么现在加固

- **构建链是冻结的**：不上 Google Play、近期无升级 AGP 计划（当前 `compileSdk/targetSdk = 34` +
  `AGP 7.0.4`，`common.gradle:5,10`、`build.gradle:18`）。
  ⇒ 任何修复都必须**原地完成**：不动依赖、不换 ORM、不重建工程结构。
- **而既有缺陷已经在生产路径上**（§3）：
  1. 🔴 **启动失败隐患**——升级路径的表集合不一致，任何一次 `onUpgrade` 都会崩（§3.1）；
  2. 🔴 **数据完整性漏洞**——`BOOK_CHAPTER_BODY` 无主键 / 无 UNIQUE / 无索引（§3.2）；
  3. 🔴 **ANR 隐患**——9 个类、28 处 DB 调用在主线程（§3.3）；
  4. 🟡 **升级路径从未被验证**——真机 `SCHEMA_HISTORY` 只有 1 条 `create` 记录（§3.4）。
- ⇒ 这四条**与用哪个 ORM 无关**，原地就能修，且修完无论将来怎么演进都有效。

**本文的判断依据**：§2 的全部事实均已用代码取证（每条带 `文件:行`），§2.7 为真机库只读实测。

### 1.2 范围与明确不做的事

**范围：P0 + P1。P2 延后**（原 P2-2 并入 P1，因为它与 P1-2 是同一条升级路径的两面）。

| 优先级 | 项 | 状态 |
|---|---|---|
| P0-1 | 修迁移框架的"表集合不一致" | ✅ 本次 |
| P0-2 | 给 `BOOK_CHAPTER_BODY` 加唯一约束 | ✅ 本次 |
| P1-1 | 主线程 DB 调用迁后台 | ✅ 本次 |
| P1-2 | 让升级路径可被验证 | ✅ 本次 |
| P1-3 | 升级失败必须可见（原 P2-2） | ✅ 本次 |
| P2-1 | 补索引 | ⏸ 延后（纯增强，需先量） |

**目标**

1. 消除**已上线的启动失败隐患**（升级路径的表集合不一致）。
2. 消除**数据完整性漏洞**（`BOOK_CHAPTER_BODY` 无任何约束）。
3. 把**主线程 DB 访问**收敛到后台（消除 ANR 隐患）。
4. 让**升级路径可被验证**（现在完全没被验证过）且**失败可见**。

**明确不做**

- 不换 ORM、不引入任何新依赖（含 KV 存储、加密库）。
- 不改 `AppConst.dbName`、不改库文件名、不改 `CURRENT_VERSION` 机制。
- **不重构 `BaseService` 的 API 签名**（按 `addEntity` / `updateEntity` / `deleteEntity` /
  `findAll` / `deleteAll` / `find` 六项统计，全仓约 84 处调用点；改签名会引发大范围连带改动）。
- 不改 `DataRepository` / `ConvertEntity` 的职责。
- **不改 `data/local/gen/`**（生成目录，会被 codegen 覆盖）。
- **不做真机库破坏性实验**（升级路径由测试复现，见 §6.1）。

### 1.3 设计决定记录

| # | 决定 | 结论 | 理由摘要 |
|---|---|---|---|
| 1 | 加固的完成判据 / 范围 | **P0 + P1**；P2 延后，原 P2-2 并入 P1 | P0 不修可能崩/丢数据；P1 决定加固是否算真正完成；P2 是纯增强且需先量 |
| 2 | P0-1 修复深度 | **补登记 + `createAllTables(db, true)` 兜底**；不改"反射自动发现" | 自动发现会把"漏登记"变成"悄悄多登记"，方向反了；一致性测试已能拦住 |
| 3 | 是否先真机复现 P0-1 | **不改真机库**，用 `DbUpgradeDrillTest` 复现 | 测试即复现，更安全、可重复、能进 CI |
| 4 | 加固期间是否冻结新增实体 | **不冻结**；但递增 `CURRENT_VERSION` 前必须先做 P0-1 的第 1、2 步 | 那两步各一行，冻结没有收益；把"冻结"换成"先花两行" |
| 5 | P0-2 加 UNIQUE 还是只加索引 | **加 UNIQUE + 运行时防御** | 值是 `UUID.randomUUID()`，代码层面保证唯一；有重复则告警跳过，兼容历史数据 |
| 6 | P1-1 用什么机制 | **复用 `DbService.runInBackgroundSerial` / `runInTransaction`**，不新增封装 | 两者语义正确（串行 + 无界队列 + 事务异常必须传出）；新增封装是重复造轮子 |
| 7 | P1-1 完成判据 | **`StrictMode` 判定"主流程无主线程 IO"**；9 个类全迁是目标 | 判据必须可跑出证据；但 16 处写操作必须全迁（硬性子集） |
| 8 | 是否为"表集合一致"立 ADR | **不立** | 缺"真实权衡"这一条（它是修 bug + 加约束）；约束改为落在三处：`EntityRegistrationHelper` 类注释 + 本文 §3.1 + `greendao-guide.md` §6 |
| 9 | 文档形态 | 一份**自足**设计文档（本文）+ 一份操作指南 | 此前 7 份互相引用的文档难以维护——改一处事实要同步 3–4 处 |

> **关于 ADR**：本项目曾为本地库决策写过 ADR 文件。2026-10-06 合并文档时**放弃 ADR 文件形式**，
> 但**决策与理由完整保留在本节**。若将来决策数量增多、需要独立引用，从本表拆出即可。

---

## 2. 现状事实基线

### 2.1 命名规则

GreenDAO 默认命名规则（用 `gen/*Dao.java` 的建表语句核对）：
- **表名** = 类名转 `UPPER_SNAKE_CASE`：`Book`→`BOOK`、`BookChapter`→`BOOK_CHAPTER`、
  `ChatMessageBean`→`CHAT_MESSAGE_BEAN`、`YaoFang`→`YAO_FANG`、`TabNav`→`TAB_NAV`、
  `ZhongYaoAlia`→`ZHONG_YAO_ALIA`。
- **列名** = 字段名转 `UPPER_SNAKE_CASE`：`sessionId`→`SESSION_ID`、`createDate`→`CREATE_DATE`、
  `isDelete`→`IS_DELETE`、`bookChapterId`→`BOOK_CHAPTER_ID`；`pic_url`→`PIC_URL`；
  `sectionvideo`→`SECTIONVIDEO`（无大写字母则不插下划线）。
- **自增主键列名固定为 `_id`**（`@Id(autoincrement=true)` 时）。
- **字符串主键列名 = 字段名大写**：`bookId`→`BOOK_ID`、`yaoFangID`→`YAO_FANG_ID`、`id`→`ID`。

> 手写 SQL（迁移脚本、索引维护）**必须用真实名**。写成 camelCase 会直接报错。

### 2.2 19 张表清单（PK 全部取自 `gen/*Dao.java` 建表语句，无推导）

| 表（真实名） | 主键列 | 主键形态 | 外键列（真实列名） | 备注 |
|---|---|---|---|---|
| `ABOUT` | `_id` | 自增 Long | — | |
| `AI_CONFIG` | `AI_CONFIG_ID` | String | — | |
| `AI_CONFIG_BODY` | `AI_CONFIG_BODY_ID` | String | `AI_CONFIG_ID` | 关系子表 |
| `BEI_MING_CI` | `_id` | 自增 Long | — | 另有业务列 `ID INTEGER NOT NULL` |
| `BOOK` | `BOOK_ID` | String | — | |
| `BOOK_CHAPTER` | `BOOK_CHAPTER_ID` | String | `BOOK_ID` | |
| `BOOK_CHAPTER_BODY` | **（无主键）** | — | `BOOK_CHAPTER_ID` | ⚠️ 见 §3.2 |
| `CHAPTER` | `_id` | 自增 Long | `BOOK_ID` | |
| `CHAT_MESSAGE_BEAN` | `_id` | 自增 Long | `SESSION_ID`(Long, 可空) | RC4 密文在 `CONTENT` |
| `CHAT_SESSION_BEAN` | `_id` | 自增 Long | — | |
| `CHAT_SUMMARY_BEAN` | `_id` | 自增 Long | `SESSION_ID`(Long, NOT NULL) | ⚠️ **未登记进 `getAllDaos()`**，见 §3.1 |
| `SEARCH_HISTORY` | `ID` | String | — | 字段名 `id`，值由 `BaseService.getUUID()` 生成 |
| `TAB_NAV` | `TAB_NAV_ID` | String | `CASE_ID` | 有保留字列 `ORDER` |
| `TAB_NAV_BODY` | `TAB_NAV_BODY_ID` | String | `TAB_NAV_ID` | 有保留字列 `DESC` |
| `USER_INFO` | `ID` | String | — | 按 `USER_LOGIN_ACCOUNT` 查询 |
| `YAO_FANG` | `YAO_FANG_ID` | String | `BOOK_ID` | |
| `YAO_FANG_BODY` | `YAO_FANG_BODY_ID` | String | `YAO_FANG_ID` | |
| `ZHONG_YAO` | `_id` | 自增 Long | — | 另有业务列 `ID INTEGER NOT NULL` |
| `ZHONG_YAO_ALIA` | `_id` | 自增 Long | — | 另有业务列 `ID INTEGER NOT NULL` |

> 旧库中**不止这 19 张表**：`SchemaHistoryRepository` 会额外建 `SCHEMA_HISTORY`
> （`helper/SchemaHistoryRepository.java:12-20`），SQLite 自身还有 `sqlite_sequence` / `android_metadata`
> （§2.7 真机实测确认共 22 张）。**任何"遍历所有表"的脚本都要显式排除这三张。**

### 2.3 关系层现状（5 个 `@ToMany`，全部在被使用）

扫描确认：**只有 `@ToMany`**，没有 `@ToOne` / `@JoinEntity` / `@JoinProperty` / `@OrderBy` /
`@Property` / `@Index` / `@Unique` / `@Convert`。

| 父实体 | 关系注解 | 子表 | 子表外键列 | 托管访问器调用面 |
|---|---|---|---|---|
| `AiConfig` | `@ToMany(referencedJoinProperty="AiConfigId")` | `AI_CONFIG_BODY` | `AI_CONFIG_ID` | `getModelList()` — 2 处 |
| `BookChapter` | `@ToMany(referencedJoinProperty="bookChapterId")` | `BOOK_CHAPTER_BODY` | `BOOK_CHAPTER_ID` | `getData()` — 4 处 |
| `ChatSessionBean` | `@ToMany(referencedJoinProperty="sessionId")` | `CHAT_MESSAGE_BEAN` | `SESSION_ID` | `getMessages()` — 2 处 |
| `TabNav` | `@ToMany(referencedJoinProperty="tabNavId")` | `TAB_NAV_BODY` | `TAB_NAV_ID` | `getNavList()` — 8 行 / 11 次 |
| `YaoFang` | `@ToMany(referencedJoinProperty="yaoFangID")` | `YAO_FANG_BODY` | `YAO_FANG_ID` | `getStandardYaoList()` — 3 处 |

合计 **19 处（按行计）**。调用点（`DataRepository` / `AppDataManager` 为快照行号）：

- `BookChapter.getData()`：`DataRepository.java:750,755,802,807`
- `TabNav.getNavList()`：`app/AppDataInitializer.java:298,299`、`DataRepository.java:163,174`、
  `manager/AppDataManager.java:620,622`、`ui/main/HomeFragment.java:375,376`
- `AiConfig.getModelList()`：`DataRepository.java:539,540`
- `ChatSessionBean.getMessages()`：`manager/ai/ChatSessionManager.java:160`、
  `ui/reader/ai/helper/ChatSidebarHelper.java:114`
- `YaoFang.getStandardYaoList()`：`DataRepository.java:464`、`helper/ConvertEntity.java:253,254`

> **对加固的含义**：这 19 处调用点**当前可用**，加固**不需要**改它们——
> 但它们的可用性有前提（§2.8）。若将来调整实体（例如给 `BookChapterBody` 补主键），必须回归验证这 19 处。

### 2.4 字段类型、可序列化与 `@Transient`

- **实际持久化字段类型**：`Long` / `String` / `int` / `float` / **`boolean`**（`Chapter.isDownload`，
  列 `IS_DOWNLOAD INTEGER NOT NULL`）。日期存为 `String`（`CREATE_DATE` / `CREATE_TIME` / `UPDATE_TIME`）。
  **没有** `Date` / `byte[]` / `double` / `BigDecimal` / `enum` / `List`（作为列）。
- **`Serializable`**：16/19 实现（含 `serialVersionUID`）；
  `ChatSessionBean`、`ChatSummaryBean`、`UserInfo` 未实现。无实际影响，不必统一。
- **`@Transient` 分两类**：
  - `Book` / `Chapter` / `SearchHistory` 的 `@Transient` 打在 `static final long serialVersionUID` 上；
  - `ChatMessageBean` 的 5 个 `TYPE_*` / `IS_Delete_*` 常量也是 `static final`；
    **另有 7 个实例布尔运行态字段**（`isMore` / `isGreyMode` / `simpleQa` / `save` /
    `continuesListen` / `isThinkingCollapsed` / `isStreaming`，
    `entity/ChatMessageBean.java:68,73,95,100,105,110,116`）标了 `@Transient`。
    ⇒ GreenDAO **不持久化**它们，但 **UI 依赖这些运行态，加固时不要误删**。
- **全部 19 个实体都有无参构造**（`public Xxx()`），同时保留全参构造
  （调用方如 `new ChatMessageBean(...)` 在用，**不可删**）。

### 2.5 查询复杂度（8 个文件用到 `QueryBuilder`）

`service/` 下共 **20 个类**（19 个实体 Service + `BaseService`）。
**使用 `QueryBuilder` 的只有 8 个文件**：

| 文件 | `QueryBuilder` | `unique()` | `orderAsc/Desc` | `buildDelete` / `executeDelete` |
|---|---|---|---|---|
| `BaseService`（泛型基类） | 6 | 0 | 0 | 4（各 2 处：1 组注释 + 1 组实码） |
| `BookService` | 2 | 0 | 0 | 0 |
| `ChatMessageBeanService` | 2 | 0 | 1 | 0 |
| `ChatSessionBeanService` | 4 | **1** | 2 | 0 |
| `ChatSummaryBeanService` | 4 | **1** | 2 | 0 |
| `SearchHistoryService` | 3 | 0 | 0 | 1 |
| `TabNavService` | 3 | 0 | 1 | 0 |
| `UserInfoService` | 5 | 0 | 0 | 0 |

其余 12 个 Service 是纯 CRUD 委托，无查询构造。

**高级查询能力实测**（排除 `gen/`）：`like` / `between` / `in` / `or` / `and` / `groupBy` /
`limit` / `offset` / `notEq` / `lt` / `isNull` 全部 **0 命中**；`.gt(` 仅 1 处且**在注释里**
（`BaseService.java:134`，快照行号）。⇒ 加固**不需要**扩展查询能力。

**一处裸 SQL**：`service/SearchHistoryService.java:93-94` →
`select * from search_history where content = ?`，经 `selectBySql` 执行。
它用的是**小写表名**（SQLite 标识符大小写不敏感，当前能跑）；
**手写 SQL 时统一写真实大写表名 `SEARCH_HISTORY`**，不要依赖这个宽容性。

### 2.6 构建与运行时前提

| 项 | 实测值 | 位置 |
|---|---|---|
| GreenDAO | `3.3.0`（插件 + 运行时） | `app/build.gradle:296`、`build.gradle:21` |
| `greendao{}` 生成器版本 | `schemaVersion 1` | `app/build.gradle:316-317` |
| **旧库真实 `user_version`** | **2**（`CURRENT_VERSION = 2`） | `helper/DatabaseVersionManager.java:18` |
| 库文件名 | `myzhongyi.db` | `base/constant/AppConst.java:23` |
| compileSdk / minSdk / targetSdk | 34 / 23 / 34 | `common.gradle:5,8,10` |
| AGP / Gradle wrapper | 7.0.4 / 7.4 | `build.gradle:18`、`gradle-wrapper.properties` |
| Java | source/target `1.8` | `common.gradle` `compileOptions` |
| SQLCipher | **无**（旧库未加密） | `grep sqlcipher *.gradle` = 0 |
| 旧库二级索引 | **无** | `gen/` 中 `CREATE INDEX` 命中 0；§2.7 实测确认 |

**三个"版本"不要混**（完整释义见 §9）：

| 名称 | 位置 | 实测值 | 含义 |
|---|---|---|---|
| `schemaVersion` | `app/build.gradle` 的 `greendao{}` 块 | `1` | GreenDAO **生成器**版本，决定 `DaoMaster.SCHEMA_VERSION`（`gen/DaoMaster.java:20`）；**本项目不用它**（版本走 `CURRENT_VERSION`） |
| `CURRENT_VERSION` | `DatabaseVersionManager.java:18` | `2` | 项目自定义"用户版本"，决定 `onUpgrade` 是否触发 |
| `PRAGMA user_version` | 旧库文件本身 | **2** | SQLite 实际存储值，由 `SQLiteOpenHelper` 写入 |

> ⚠️ **递增 `CURRENT_VERSION` 会触发 `smartMigrate` 的破坏性重建**（§3.1）。
> 本方案刻意**不递增它**，改用启动时 `CREATE ... IF NOT EXISTS` 幂等补齐。

### 2.7 真机库实测（2026-10-06，`emulator-5554`）

> **这是只读取数**（`adb exec-out run-as … cat databases/myzhongyi.db` 后本地 sqlite 直读），
> **不是** §1.3 决定 3 所禁止的"改真机库做破坏性复现"。只读拉取不影响设备上的库。

| 项 | 实测结果 | 影响 |
|---|---|---|
| `PRAGMA user_version` | **2** | 与 `CURRENT_VERSION` 一致；证实 `schemaVersion 1` 与运行时版本无关 |
| 表数 | **22** = 19 实体表 + `SCHEMA_HISTORY` + `android_metadata` + `sqlite_sequence` | 脚本必须显式枚举 19 表（§2.2） |
| 索引 | **10 个，全部是 `sqlite_autoindex_*`**（TEXT 主键的隐式唯一索引） | **零二级索引**，见 §3.6 |
| `BOOK_CHAPTER_BODY` 主键 | **无**（该表既无 PK 列，也无任何 autoindex） | 实测确认 §3.2 |
| `SCHEMA_HISTORY` 记录 | 仅 1 条：`FROM=0 → TO=2`、`STATUS='create'`、`NOTE='Initial create'` | 本机库是**新建**的（当天 17:23），**没有任何 `onUpgrade` 记录** ⇒ 升级路径在这台设备上从未被验证过 |
| `BOOK_CHAPTER_BODY` 行数 | **1 行** | 样本太小，不足以判定线上唯一性（但 §3.2 已由代码构造证明唯一） |

> **本机数据的代表性有限**：该库被 `pm clear` 过，**不代表线上分布**。
> "线上是否真有停在 V1 的用户"**仍未证实**（只能通过线上 `SCHEMA_HISTORY` 分布或升级统计判断）。

### 2.8 关系层语义要点（加固必须知道的行为）

1. **托管关联是"懒加载 + 需要挂载"**：`getData()` / `getNavList()` 等方法首次调用时才查库，
   且要求实体挂在 `DaoSession` 上，否则抛 `DaoException("Entity is detached from DAO context")`
   （`entity/BookChapter.java:117` 的 `getData()`）。
   ⇒ 这 19 处调用点的**可用性依赖"实体来自 DaoSession 查询"**这个隐式前提；
   加固改动实体/查询路径后必须回归验证。
2. **`unique()` 命中多行会抛异常**（GreenDAO 语义，比"返回首行"严格）。
   全工程只有 2 处（`ChatSessionBeanService.java:114`、`ChatSummaryBeanService.java:123`），
   都作用在唯一列上。
3. **RC4 加解密在 Service 层，范围是两个实体三个字段**（详见 §9）：
   - `ChatMessageBean.content`（`ChatMessageBeanService.java:90-111`）
   - `ChatSummaryBean.title` **与** `content`（`ChatSummaryBeanService.java:148-159`）

   语义：`updateEntity` = 加密 → 写库 → **把明文改回实体**；`find*` = 查出后**就地解密**。
   ⇒ 任何触碰这两个 Service 的改动都要保持这个"就地改回 / 就地解密"行为，否则 UI 会显示密文。
4. **`BaseService.daoSession` 是字段初始化器**（`BaseService.java:24`：
   `= GreenDaoManager.getInstance().getSession()`）⇒ **构造任何 Service 都会打开可写库**。
   这也是为什么升级入口只要 `GreenDaoManager.getInstance()` 就会触发 `onUpgrade`（§3.1）。
5. **实体里有 5 组 GreenDAO 反向引用字段**（`transient DaoSession daoSession` /
   `transient XxxDao myDao`），存在于 `AiConfig` / `BookChapter` / `ChatSessionBean` /
   `TabNav` / `YaoFang`——与 §2.3 的 5 个 `@ToMany` 一一对应，是 GreenDAO 为托管关联生成的。
   **不要删**（删了托管关联就失效）。

---

## 3. 风险登记与加固项

| 项 | 现状 | 可行性 | 主要风险 |
|---|---|---|---|
| **P0-1 表集合一致性** | 18/19 张表不一致 → 升级即崩 | ✅ 高：改 2 处手写文件 + 1 个测试 | **不得改 `gen/DaoMaster.java`**；补登记后该表**首次**进入重建路径，需先跑升级演练 |
| **P0-2 唯一性约束** | 无主键/无 UNIQUE/无索引 | ✅ 高：值是 `UUID.randomUUID()`，代码层面保证唯一 | 历史数据无法用当前代码证明；有重复则**只告警、不建索引** |
| **P1-1 主线程迁后台** | 9 个类 28 处调用 | ✅ 高：`DbService` 已有串行入口 | 不改 `BaseService` 签名；不动 `ChapterContentManager` 自有线程池 |
| **P1-2 升级演练** | 从未被验证过 | ✅ 高：`src/androidTest` | CI 若无 Android 环境则只能本地跑 |
| **P1-3 失败可见** | 异常被静默吞 | ✅ 高：只改 catch 与日志 | 不改公开方法签名 |
| **P2-1 索引** | 零二级索引 | ✅ 高：`CREATE INDEX IF NOT EXISTS` 幂等 | `TAB_NAV."ORDER"` 是保留字必须加引号 |

> **2026-10-07 状态**：P0-1 / P0-2 / P1-1 / P1-2 / P1-3 **已加固**（P0-1、P0-2、P1-3 在前一轮会话完成，
> P1-1 与 P2-1 的索引维护在本轮完成并有设备证据）。
> **P1-2 的实现方式有变更**：原定 `src/androidTest` 的 `DbUpgradeDrillTest`，
> 实际落地为 adb 脚本 `.scratch/greendao-hardening/drill-upgrade.sh`（零依赖、可重复、能改 `user_version`）。
> **P2-1 仍延后**，但通道已就绪：`DbIndexMaintenance` 现在会建 `ux_book_chapter_body`，
> 其余候选列见 §3.6，加之前先 `EXPLAIN QUERY PLAN`。
>
> 本轮联调还发现一条**清单没覆盖到**的主线程链与两项非 DB 主线程 IO，均已修复，
> 记在 §3.3.1 / §3.3.2 与 `greendao-guide.md` §10。

### 3.0 改动点签名清单

下面 §3.1–§3.6 只描述"改什么、为什么"；**确切签名以本表为准**。

**新增符号**

| 符号 | 签名 | 详见 |
|---|---|---|
| `DbIndexMaintenance` | `public final class DbIndexMaintenance`（工具类，私有构造） | §3.2 / §3.6 |
| ↳ 方法 | `public static void ensureIndexes(org.greenrobot.greendao.database.Database db)` —— 幂等；内部先跑重复检查，通过才 `CREATE UNIQUE INDEX IF NOT EXISTS`；**失败只 `EasyLog.print(Throwable)`，不抛** | §3.2 |
| `EntityRegistrationHelperTest` | `public class EntityRegistrationHelperTest`（JVM 单测，`src/test/java/.../data/local/helper/`） | §3.1 |
| ↳ 方法 | `@Test public void getAllDaosCoversEveryGeneratedDao()` —— 用反射读 `getAllDaos()` 中每个 Dao 的 `TABLENAME`，与 `gen/` 下全部 `*Dao` 的 `TABLENAME` 集合比对（不相等即失败） | §3.1 |
| `DbUpgradeDrillTest` | `public class DbUpgradeDrillTest`（`@RunWith(AndroidJUnit4.class)`，`src/androidTest/java/.../data/local/helper/`） | §3.4 |
| ↳ 方法 | `@Test public void upgradeFromV1DoesNotThrowAndRecordsSuccess()` | §3.4 |

**修改的现有符号**

| 符号 | 现有签名 | 改动 |
|---|---|---|
| `EntityRegistrationHelper.getChatDaos()` | `private static List<Class<? extends AbstractDao<?, ?>>> getChatDaos()` | 加一行 `daos.add(run.yigou.gxzy.data.local.gen.ChatSummaryBeanDao.class);` |
| `EntityRegistrationHelper` 类注释 | — | 写入"表集合一致性"约束（决定 8 的落点之一） |
| `GreenDaoUpgrade.smartMigrate(...)` | `public final void smartMigrate(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses)` | 把体内的 `DaoMaster.createAllTables(db, false)` 改为 `(db, true)` |
| `GreenDaoUpgrade.generateTempTables(...)` | `private final void generateTempTables(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses)` | 对不存在的表**显式跳过并记录**，不生成空列临时表；异常一律走 `EasyLog.print(Throwable)` |
| `GreenDaoUpgrade.autoMigrateTable(...)` | `public static void autoMigrateTable(Database db, Class<? extends AbstractDao<?, ?>> daoClass)` | 同上：catch 分支补 `EasyLog.print(Throwable)`，不再只打 tag+msg |
| `MigrationOrchestrator.ensureUpToDate(Context)` | `public static void ensureUpToDate(Context context)` | 在 `SchemaHistoryRepository.ensureTable(database)` 之后插入一行 `DbIndexMaintenance.ensureIndexes(database);`（局部变量 `database` 此刻已就绪） |
| `StartupIoExemption` | `public final class StartupIoExemption`（工具类，私有构造） | 2026-10-07 新增。`public static void runExempted(Runnable action)` —— 只包住"进程一次性前置"（开库 / 建升级历史表 / 首次建索引 / 读版本号 / 建立 LocalServices 单例 / 启动读登录记录）；debug 之外直接执行原动作。**调用方必须逐段显式声明**，不允许整段包住自己的业务 |
| `DbService.readInBackground` | `public <T> void readInBackground(Callable<T> reader, Callback<T> callback)` | 2026-10-07 新增（票 13）。"读后回 UI"的统一入口：异常自动 `EasyLog.print(Throwable)` 后转 `callback.onError`，**"读失败算什么"由调用方决定** |

**三种调用范式**（§3.3 迁移时照此写，避免各写各的）

- **写（fire-and-forget）**：`DbService.getInstance().runInBackgroundSerial(...)`，任务体内自己 try/catch 并
  `EasyLog.print(Throwable)`——`execute` 提交的异常不会进 Future，只会杀掉 worker 线程。
- **读后回 UI（返回值型同步读）**：同样走 `runInBackgroundSerial`；在任务体内读值，再用
  `ThreadUtil.runOnUiThread(...)` 回主线程应用结果（**注意被捕获变量需为 final / 有效 final**）。
  适用：`HomeFragment` / `BookRepository` / `TipsBookNetReadFragment` 这类"读出来赋给局部变量再渲染"的调用点。
- **跨表原子操作**：`DbService.getInstance().runInTransaction(...)`；任务内异常**不要**吞，
  让它传出去（否则事务照常提交，留下删一半/写一半的表）。

### 3.1 🔴 P0-1：`smartMigrate` 的表集合不一致（已上线的启动失败隐患）

**现状**

- `GreenDaoUpgrade.smartMigrate` 用 `EntityRegistrationHelper.getAllDaos()` 决定"哪些表已存在、需要 DROP"，
  只 DROP `existingTables`，随后调 `DaoMaster.createAllTables(db, false)` 重建**全部 19 张**
  （`GreenDaoUpgrade.java:342-360`）。
- `getAllDaos()` 只登记 **18** 个 Dao —— **缺 `ChatSummaryBeanDao`**
  （`EntityRegistrationHelper.java:24-38,52-57`）。
- `createTable(db, false)` 生成的 SQL **不带 `IF NOT EXISTS`**
  （`gen/ChatSummaryBeanDao.java:46`：`constraint = ifNotExists ? "IF NOT EXISTS " : ""`）。

**根因与影响**

`CHAT_SUMMARY_BEAN` **永不被 DROP，却总被无条件重建** ⇒ **一旦 `onUpgrade` 触发即抛
`table CHAT_SUMMARY_BEAN already exists`**；异常在 `MySQLiteOpenHelper.onUpgrade` 中被记为 `failed`
后**重新抛出**（`MySQLiteOpenHelper.java:55` 的 `onUpgrade`），经 `GreenDaoManager` 构造函数包成
`RuntimeException`（`GreenDaoManager.java:51-53`），最终由 `MigrationOrchestrator` 包成
`IllegalStateException`（`MigrationOrchestrator.java:44-47`）→ **启动失败**。

**升级入口（为什么会被触发）**

`AppApplication.java:209` → `MigrationOrchestrator.ensureUpToDate(this)` →
`GreenDaoManager.getInstance()`（`MigrationOrchestrator.java:37`）→ 打开可写库 → 触发
`MySQLiteOpenHelper.onUpgrade` → `smartMigrate`。
**触发条件是 `user_version != 2`**（即"从 V1 直升"的用户）。

**⚠️ 状态**：以上为**代码路径推演，尚未在真机复现**——但**不需要**改真机库来复现：
`DbUpgradeDrillTest`（§3.4）本身就会构造 `user_version = 1` 的库再打开，**修复前应红、修复后应绿**。

**⚠️ 硬前置（措辞按"不冻结"）**：**不要求冻结新增实体**——但**递增 `CURRENT_VERSION` 之前
必须先做掉本项的第 1、2 步改动**（各一行）。原因：在 P0-1 未修的状态下递增版本 = 主动触发上面那条崩溃路径。
"要不要冻结"取决于"愿不愿意先花两行改动"。**这条硬前置**同时写入 `greendao-guide.md` §4
（"表集合一致性"约束本身写入其 §6）。

**改动点**

| 文件 | 改动 |
|---|---|
| `data/local/helper/EntityRegistrationHelper.java` | ① `getChatDaos()` 补 `ChatSummaryBeanDao.class`（**根因修复**）；② **类注释写入"表集合一致性"约束**（决定 8 的落点之一） |
| `data/local/helper/GreenDaoUpgrade.java` | `smartMigrate` 里的 `DaoMaster.createAllTables(db, false)` → **`(db, true)`**（加 `IF NOT EXISTS` 兜底；**不要改 `gen/DaoMaster.java`**，那是生成文件） |
| `app/src/test/java/.../EntityRegistrationHelperTest.java`（新增） | 断言"`getAllDaos()` 覆盖的表集合 == `gen/*Dao` 声明的表集合"，防止再次漏登记 |

**为什么两处都改**：`getAllDaos()` 补全修根因；`createAllTables(db, true)` 保证"将来再漏登记一次也不会崩"。
两者互补，缺一仍留隐患。

**为什么不改成"自动发现"（不采纳）**：把 `getAllDaos()` 改成反射扫描 `gen/` 看似根除漏登记，
但方向是反的——它会把"忘记登记"变成"**悄悄多登记**"；而 `gen/` 是生成目录、无法在其内部加钩子，
运行时枚举 dex 又很脏。**一致性测试已经能拦住漏登记**，够用。

**一致性测试放哪（已查证）**：放 `app/src/test/java`（JVM 单测）。
依据：`TABLENAME` 是 `public static final String`（反射无需 `setAccessible`）；`AbstractDao`
无静态字段与 `<clinit>`，类初始化不触碰 Android；AGP 默认给 `testDebugUnitTest` 提供 mockable android.jar。
**兜底**：若实测类加载失败（`NoClassDefFoundError`），就挪到 `app/src/androidTest`。

**副作用评估**：补 `ChatSummaryBeanDao` 后，**下一次** `onUpgrade` 会把 `CHAT_SUMMARY_BEAN` 纳入
"DROP → 重建 → 从临时表回填"流程（`generateTempTables`/`restoreData` 会保留数据，`_id` 原值照抄）。
这是期望行为，但**该表是第一次走重建路径**——必须先由 §3.4 的升级演练验证通过。

### 3.2 🔴 P0-2：`BOOK_CHAPTER_BODY` 无任何约束（数据完整性）

**现状**

- 该表**无主键、无 UNIQUE、无任何索引**（`gen/BookChapterBodyDao.java:21,55-65`；§2.7 实测确认）。
- 实体 `BookChapterBody` **没有 `@Id`**，`BookChapterBodyDao extends AbstractDao<BookChapterBody, Void>`
  ⇒ GreenDAO 层面就承认它**没有主键**，**无法按主键 update/delete**。
- 写入路径靠"先按外键列删、再整批插"维持幂等：`DataRepository.saveBookChapterData` 里
  `deleteAll(BookChapterBodyDao.Properties.BookChapterId.eq(...))` 后 `addEntity(...)`
  （快照行号 `DataRepository.java:622-623` 与 `:663`；以符号名为准）。

**影响**：幂等性**只靠应用层自觉**，数据库层没有唯一性保证；异常重试或并发写会产生重复行。

**唯一性依据（已查实，撤销了原先"必须先查线上数据"的前置）**

- `bookChapterBodyId = StringHelper.getUuid()` = `UUID.randomUUID().toString()`
  （`ConvertEntity.java:195`、`StringHelper.java:22`）——**全工程唯一赋值点**就是这一处，
  **唯一写入点**是 `DataRepository.saveBookChapterData` 里的 `mBookChapterBodyService.addEntity(...)`
  （快照行号 `:663`）。
- 正常路径**不可能为空**（`createBookChapterBody` 仅在 `chapterId`/`content` 为 null 时返回 null，
  且那种情况不会插入）。
- 服务端**没有**这个概念（服务端 `signatureId = String(row.id)` 落到 `SIGNATURE_ID` 列，与 body id 无关）。
- ⇒ **代码层面保证唯一**。

**改动点**

| 文件 | 改动 |
|---|---|
| `data/local/helper/DbIndexMaintenance.java`（新增） | 幂等索引维护。建索引**前**先跑重复检查：`SELECT COUNT(*), COUNT(DISTINCT BOOK_CHAPTER_BODY_ID) FROM BOOK_CHAPTER_BODY`；相等才执行 `CREATE UNIQUE INDEX IF NOT EXISTS ux_book_chapter_body ON BOOK_CHAPTER_BODY(BOOK_CHAPTER_BODY_ID)`；不等则**只告警不建索引** |
| `data/local/helper/MigrationOrchestrator.java` | `ensureUpToDate` 中在库打开后调用索引维护；**失败只记录不抛**（`EasyLog.print(Throwable)`），避免把数据问题升级成启动失败 |

**为什么要先查重复**：值是 UUID，理论上不会重复，但**历史版本写下的数据**无法用当前代码证明。
"有重复则告警并跳过"让这条改动**在任何数据状态下都安全**（幂等、可重入、不阻断启动）。

**为什么不走 `@Index` 注解 + 版本号递增**：递增 `CURRENT_VERSION` 会触发 `smartMigrate` 的
**破坏性重建**（DROP 全部已存在表再重建），为加一个索引付这个代价不值得。
用 `CREATE ... IF NOT EXISTS` 在启动时幂等补齐：不动版本号、不重建表、无数据风险。

### 3.3 🔴 P1-1：主线程 DB 访问（ANR 隐患）

**现状**：9 个类在主线程直连 DB。GreenDAO 允许主线程读写，所以**这些 IO 今天就在主线程发生**——
这正是上一轮 ANR 的成因类别（`AppDataManager$6.onSucceed → saveYaoData → endTransaction`
阻塞主线程 >5s）。**原统计 28 处（16 写 / 12 读）经后会修正为 24 处（13 写 / 11 读）**，见下表后说明。

| 类 | 写 | 读 |
|---|---|---|
| `ui/account/LoginActivity.java` | `:528,530,533,589,591,594` | `:522,583` |
| `ui/reader/repository/BookRepository.java` | `:214,230` | `:95,199`（`:262` 是 `getUUID()`，不访问库） |
| `ui/main/HomeFragment.java` | `:257,296` | `:158,272` |
| `ui/activity/BookContentSearchActivity.java` | `:377,746` | `:505` |
| `ui/reader/fragment/BookCollectCaseFragment.java` | `:210` | `:119,151,208` |
| `ui/dialog/ChatSummaryListDialog.java` | `:129` | `:111` |
| `ui/account/MyFragmentPersonal.java` | `:263` | — |
| `ui/setting/SettingActivity.java` | `:149` | — |
| `ui/reader/bookread/TipsBookNetReadFragment.java` | — | `:539` |

> 统计口径：`addEntity` / `updateEntity` / `deleteEntity` / `deleteAll` / `addOrUpadteHistory` /
> `clearHistory` / `find` / `findAll` / `selectBySql` / `findBySessionId` / `findAllSearchHistory` /
> `getAllBooks` / `findUserInfoByLoginAccount`。复现：对上述 9 个文件跑该口径的 `grep … | wc -l` → **28**。
> 这 9 个文件当时**无未提交改动**，故其行号稳定。
>
> **⚠️ 计数修正（2026-10-06，联调取证后回填）**：上述 28 处里有 4 处不成立——
> `LoginActivity` 的 522/528/530/533 位于 `login()` 开头被 `/* … */` 整体注释掉的旧回调块
> （行 497–559），不参与编译；`BookCollectCaseFragment` 的 119 处，其宿主 `loadData()`
> 整个包在 `ThreadUtil.runInBackground` 里，本来就不在主线程。
> ⇒ **真实生效的是 24 处（13 写 / 11 读）**，本轮的迁移按 24 处执行。

### 3.3.1 统计口径没覆盖到的另一条链（2026-10-06 补）

上面的清单是"哪个文件里出现了 CRUD 调用"。严格模式实测显示还有一条**更粗**的主线程链，
因为它把 DB 调用包在 lambda 里（`DataRepository` 内部 + `ConvertEntity.executeDatabaseOperation`），
没被这个口径统计到：

```
HomeFragment.initData → loadDataWithLifecycle
  → AppDataManager.loadAllDataIfNeeded → executeLoadSequence
  → loadNavigationData / loadYaoDataWithAlias / loadMingCiData / loadYaoAliasData
  → DataRepository.getNavigationData / getYaoData / getMingCi / getYaoAlia（各一次 findAll）
```

这条链正是历史 ANR 的那条（其后段的批量落库曾阻塞主线程 >5s）。已在本次加固中一并迁入后台
（每个 `loadXxxData` 改为"后台读本地 → 回主线程判定 → 没有再发网络请求"）。

### 3.3.2 `BaseService.initTable` 的构造器副作用（2026-10-06 补）

`DbService` 构造器会逐个 new 出 19 个 Service，而 `BaseService.<init>` 末尾的
`initTable()` 会对每张表查一次 `sqlite_master` ⇒ 一次 DbService 构造 = 19 次读。
发生位置是进程启动时的 `AppApplication.initBasicConfig`，无法挪走（任何数据操作都以它为前置），
故归入"进程一次性前置"窗口处理：在这段窄范围内放开严格模式后立即恢复原策略。
长期方案见票 11 的 B 项。

**改动点：复用既有基建，不新造封装**

| 已有设施 | 语义 | 用法 |
|---|---|---|
| `DbService.runInBackgroundSerial(Runnable)` | 单线程串行（线程名 `mf-db-serial`）+ **无界队列**，"提交即受理，不会因池满抛 `RejectedExecutionException`" | **所有 DB 调用的统一入口** |
| `DbService.runInTransaction(Runnable)` | 跨 service（跨表）的原子操作；**任务内异常必须传出** | 需要"删+插"原子性的场景（如全量覆盖导航） |
| `ThreadUtil.runOnUiThread(Runnable)` | 回主线程（已在主线程则直接执行） | 结果回 UI |

> **⚠️ 前置确认**：这两个 `DbService` 方法在**当前工作树**中存在，但**尚未提交**
> （`HEAD` = `f93c845` 里还没有）。动手前先确认它们已随提交落地，否则先把这段基建补上。

**⚠️ 异常处理不能想当然**：`runInBackgroundSerial` 用 `Executor.execute`（不是 `submit`），
任务内抛出的异常**不会进 Future**，会走到线程的未捕获异常处理并**杀掉该 worker 线程**
（执行器随后重建它）。所以 **task 内部必须自己 try/catch 并用 `EasyLog.print(Throwable)` 记录**——
否则写失败既没日志也没提示。
跨表事务场景则相反：**异常必须让它传出去**（走 `runInTransaction`），
**不要用 `ConvertEntity.executeDatabaseOperation` 包**——它吞异常并返回 `null`，
会让事务照常提交，留下删了一半/写了一半的表（`DbService.runInTransaction` 的 javadoc 已写明这一点）。

**迁移顺序**（按"写优先、密集优先"）：

| 顺序 | 类 | 理由 |
|---|---|---|
| 1 | `LoginActivity`（6 写 + 2 读） | 写最密集，且在登录回调路径上 |
| 2 | `BookRepository`（2 写 + 2 读）、`HomeFragment`（2 写 + 2 读） | 读写混合，阅读主路径 |
| 3 | `BookContentSearchActivity`（2 写 + 1 读）、`ChatSummaryListDialog`（1 写 + 1 读） | |
| 4 | `MyFragmentPersonal`、`SettingActivity`（各 1 写）、`BookCollectCaseFragment`（1 写 + 3 读）、`TipsBookNetReadFragment`（1 读） | 零散 |

**完成判据（不是"9 个类全迁"）**：**主流程（冷启 → 首页 → 阅读 → 聊天）在 `StrictMode` 下无
`DiskReadViolation` / `DiskWriteViolation`**。"9 个类全迁"是目标，`StrictMode` 才是可判定的判据。
**硬性子集：16 处写操作必须全迁**（写阻塞主线程的代价最高，正是上一轮 ANR 的成因）。

**边界**：不改 `BaseService` 签名；不动 `ChapterContentManager` 自有的 high/low 线程池
（它承担 `cancelAll()` 取消语义，换全局池会破坏取消）。

### 3.4 🟡 P1-2：让升级路径可被验证

**现状**：真机 `SCHEMA_HISTORY` **只有 1 条 `create` 记录**，没有任何 `onUpgrade` 记录
⇒ 这套自建迁移框架的正确性**从未被任何一次真实升级检验过**。§3.1 能潜伏至今，正因为没人走过这条路。

**改动点**

| 文件 | 改动 |
|---|---|
| `app/src/androidTest/java/.../DbUpgradeDrillTest.java`（新增，`AndroidJUnit4`） | ① 用当前 schema 建库；② `PRAGMA user_version = 1`；③ 重新经 `MySQLiteOpenHelper` 打开；④ 断言**不抛异常**且 `SCHEMA_HISTORY` 出现一条 `success`；⑤ 断言各表行数与升级前一致（数据不丢） |

**这条同时是 §3.1 的验收手段**：修复前该测试应**红**（复现 `already exists`），修复后应**绿**。

**为什么不先改真机库复现**：这个测试**本身就是复现**（它构造 `user_version=1` 再打开库），
而且在测试里复现**更安全、可重复、能进 CI**。真机复现的剩余价值只有"证明线上真会触发"，
而那取决于"是否存在 V1 用户"——与修不修无关。**不改真机库。**

**为什么用 androidTest 而不是 JVM 单测**：`GreenDaoUpgrade` 依赖
`org.greenrobot.greendao.database.Database`（Android `SQLiteDatabase` 包装），JVM 侧无法构造。
仓库 `AGENTS.md` 也约定仪器测试放 `src/androidTest`。

### 3.5 🟢 P1-3：升级失败必须可见（原 P2-2）

**现状**：`GreenDaoUpgrade` 里多处 `catch` 只 `EasyLog.print(tag, msg)` 后继续
（`autoMigrateTable`、`getColumns`、`isTableExists` 的调用方等），
且 `getColumns` 在表不存在时返回**空列表**，会让 `generateTempTables` 生成
`CREATE TABLE X_TEMP ();` 这类非法 SQL，把真实错误掩盖成 SQL 语法错。

**改动点**

| 文件 | 改动 |
|---|---|
| `data/local/helper/GreenDaoUpgrade.java` | ① `generateTempTables` 对不存在的表**显式跳过并记录**，不生成空列临时表；② 升级路径的异常一律用 `EasyLog.print(Throwable)`（符合仓库日志规范），并在 `SchemaHistoryRepository` 记录 `failed` 时带上可读原因 |

**为什么并入 P1**：它与 §3.4 是同一件事的两面——§3.4 让升级**可验证**，本项让失败**可见**。
拆开做会重复改 `GreenDaoUpgrade` 两次。

**边界**：不改 `GreenDaoUpgrade` 的公开方法签名（`MySQLiteOpenHelper` 依赖 `smartMigrate`）。

### 3.6 🟢 P2-1：补索引（延后）

旧库零二级索引（§2.7）。这是**纯增强、不是回归修复**——不加索引 = 维持现状，不是性能退化。
候选列（对应实际查询）：`CHAT_MESSAGE_BEAN(SESSION_ID/IS_DELETE)`、
`CHAT_SESSION_BEAN(UPDATE_TIME)`、`CHAT_SUMMARY_BEAN(SESSION_ID)`、`USER_INFO(USER_LOGIN_ACCOUNT)`、
`BOOK_CHAPTER(BOOK_ID)`、`BOOK_CHAPTER_BODY(BOOK_CHAPTER_ID)`、`TAB_NAV("ORDER")`（保留字，需引号）。

**延后理由**：本机数据量太小（`YAO_FANG_BODY` 541 行、`ZHONG_YAO` 601 行），
`EXPLAIN QUERY PLAN` 很可能量不出全表扫的问题；等 P0/P1 落地、数据量或聊天表增长后再按需加。
届时复用 §3.2 已建好的 `DbIndexMaintenance` 通道（`CREATE INDEX IF NOT EXISTS`，不递增版本号）。
**加之前先 `EXPLAIN QUERY PLAN` 确认是 `SCAN TABLE`。**

### 3.7 🟢 已排除（不必再查）

| 项 | 证据 |
|---|---|
| **SQLCipher 不需要** | `grep sqlcipher *.gradle` = 0；旧库未加密 |
| **无隐藏复杂 SQL** | `like/between/in/or/groupBy/limit/offset` 全 0 命中；`.gt(` 仅在注释里；唯一裸 SQL 是 `SearchHistoryService` 的一句 `where content = ?` |
| **无多对多 / 嵌套关系 / 复合 join** | 只有 5 个 `@ToMany` |
| **类型转换问题不存在** | 持久化类型仅 `Long/String/int/float/boolean`，SQLite 原生支持 |

### 3.8 🔵 记录在案但不修（加固范围外）

这些是真实存在的既有问题，但**修它们会引发连带改动**（改 API 签名 / 动数据层职责），
与"加固"目标相悖。登记在此，将来若重构再处理：

| 项 | 现状 | 为什么不修 |
|---|---|---|
| `BaseService.daoSession` 字段初始化器 | 构造任何 Service 即打开可写库 | 改它要动 `BaseService` 的初始化模型，波及 20 个 Service |
| RC4 的"就地改回明文 / 就地解密" | `updateEntity` 改实体内容再改回 | 语义上可行，只是不优雅；改它要动两个 Service 的读写路径 |
| `SearchHistoryService` 裸 SQL 用小写表名 | 当前能跑 | 不影响行为；仅在将来写 SQL 时注意 |
| `Serializable` 不统一（3 个实体未实现） | 无实际影响 | 补齐无收益 |
| `BaseService` 的通用条件 API（`find` / `deleteAll(WhereCondition…)`） | 约 84 处调用点（六项 CRUD 方法合计）直接构造条件对象 | 改签名会波及全部调用方；当前无收益 |

---

## 4. 错误处理策略

- **升级路径**：任何表结构操作的失败必须**可见**（`SCHEMA_HISTORY` + `EasyLog.print(Throwable)`），
  不允许静默吞异常。
- **后台 DB 任务**：`runInBackgroundSerial` 的任务内**必须自己 try/catch + `EasyLog.print(Throwable)`**
  （`execute` 提交的异常不会进 Future，只会杀掉 worker 线程）；
  跨表事务用 `runInTransaction` 且**让异常传出去**，**不得**套 `executeDatabaseOperation`（它吞异常）。
- **索引维护**（§3.2 / 将来的 §3.6）：`CREATE ... IF NOT EXISTS` 幂等；**失败只记录，不阻断启动**——
  数据问题不能升级成启动失败。
- **数据修复**：不做自动去重。发现重复只记录 + 告警，清理动作交人工确认。

## 5. 依赖管理

**不新增任何依赖**——不换 ORM、不引入 KV 存储或加密库。仅改动 `app/src/main/java/.../data/local/`
下的手写文件、9 个 UI/repository 类的调用点、`app/src/test`、`app/src/androidTest`。
**不改 `data/local/gen/`（生成目录）**。

## 6. 验证方法

### 6.1 §3.1 由测试复现（不碰真机库）

跑 `DbUpgradeDrillTest`：**修复前应红**（复现 `table CHAT_SUMMARY_BEAN already exists`），
**修复后应绿**（`SCHEMA_HISTORY` 出现 `1→2 / success`，且各表行数不变）。

### 6.2 §3.2 约束生效

启动一次后查真机库：
`SELECT type, name FROM sqlite_master WHERE type='index' AND name='ux_book_chapter_body'`。
另外造一份"有重复行"的库，确认 `DbIndexMaintenance` **只告警不建索引、且不影响启动**。

### 6.3 §3.3 主线程

debug 构建下开 `StrictMode`（`penaltyLog`），跑主流程（冷启 → 首页 → 阅读 → 聊天），
确认无 `DiskReadViolation` / `DiskWriteViolation`；并复核上一轮 ANR 场景
（`pm clear` 后冷启全量加载）无 `ANR in`。

### 6.4 一致性门禁

`EntityRegistrationHelperTest` 通过（表集合一致）。

### 6.5 静态门禁

`gradlew.bat testDebugUnitTest` 通过；`gradlew.bat assembleDebug` 通过。

## 7. 实施清单（原子化、按顺序）

**Phase 1 — P0（阻断级；递增 `CURRENT_VERSION` 前必须先完成第 1、2 步）**

1. `EntityRegistrationHelper.getChatDaos()` 补 `ChatSummaryBeanDao.class`。
2. `GreenDaoUpgrade.smartMigrate` 的 `DaoMaster.createAllTables(db, false)` → `(db, true)`。
3. 在 `EntityRegistrationHelper` 类注释写入"表集合一致性"约束（决定 8 的落点之一）。
4. 新增 `EntityRegistrationHelperTest`（`src/test`，表集合一致性断言）。跑通；
   若类加载失败（`NoClassDefFoundError`）则挪到 `androidTest`。
5. 新增 `DbUpgradeDrillTest`（`src/androidTest`），确认第 1/2 步后**由红转绿**。
6. 新增 `DbIndexMaintenance`（含重复检查 + `CREATE UNIQUE INDEX IF NOT EXISTS`），
   在 `MigrationOrchestrator.ensureUpToDate` 接入；失败只记录不抛。

**Phase 2 — P1**

7. 按 §3.3 的顺序表逐类迁移（`LoginActivity` 优先），每类迁完跑一次主流程冒烟。
   写操作走 `DbService.runInBackgroundSerial`，任务内 try/catch + `EasyLog.print(Throwable)`；
   跨表原子操作走 `DbService.runInTransaction`。
   动手前先确认这两个方法已提交（见 §3.3 的前置确认）。
8. 加固 `GreenDaoUpgrade` 的异常可见性（§3.5）。
9. 补 `StrictMode` 验证（debug 构建），按 §6.3 判定 §3.3 完成。

**Phase 3 — 收尾**

10. 复核本文 §3 的风险登记，把已消项标注为"已加固"。
11. 复核 `greendao-guide.md`：确认其 §4 的硬前置与 §6 的约束表述与代码一致
    （含"一致性测试已存在"这一状态）。

## 8. 实施边界

- **改动范围**：`app/src/main/java/run/yigou/gxzy/data/local/`（手写部分）、
  9 个 UI/repository 类的主线程调用点、`app/src/test`、`app/src/androidTest`。
- **排除范围**：`data/local/gen/`（生成目录，禁止手改）；ORM 替换；网络层；服务端；
  `BaseService` API 签名；`DataRepository` / `ConvertEntity` 的职责；**真机库文件**（不做破坏性实验）。
- **允许新增文件**（实际落地清单，2026-10-07 更新）：
  `data/local/helper/DbIndexMaintenance.java`（P0-2 索引维护）、
  `data/local/helper/StartupIoExemption.java`（启动期一次性 IO 的豁免窗口，见 §3.3.2）、
  `src/test/java/.../EntityRegistrationHelperTest.java`（表集合一致性门禁）。
  **升级演练没有落成 `src/androidTest/.../DbUpgradeDrillTest.java`**，改用 adb 脚本
  `.scratch/greendao-hardening/drill-upgrade.sh` 与 `drill-persistence.sh`（§3.4 与票 12）。
- **`DbService` 的异步封装**：原边界是"不再新增"。2026-10-07 的票 13 追加了一个
  `readInBackground(Callable<T>, Callback<T>)`——它不是新机制，只是把
  "后台读 → 回主线程交付、异常自动记录"这段已经在 5 处手抄的形状收敛成一个入口；
  `runInBackgroundSerial` / `runInTransaction` 的语义与签名未变。
- **回滚方式**：§3.1 与 §3.2 均为幂等改动，回退即删除对应代码；索引用 `DROP INDEX IF EXISTS` 撤销；
  库文件不受影响（不重建表、不递增版本号）。

---

## 9. 附录 A：术语表

> 只收**本方案正文没有完整展开**的词；正文已详述的（表集合一致性、RC4 语义、三个版本号）
> 不在此重复，见对应章节。

### GreenDAO 机制

- **`gen/` 生成目录**：`data/local/gen/`，插件生成的 `*Dao` / `DaoMaster` / `DaoSession`。
  **禁止手改**——下次 codegen 会覆盖。行为改动一律写在 `data/local/helper/` 下的手写文件里。
- **`TABLENAME`**：GreenDAO 在每个生成的 `*Dao` 上声明的表名常量
  （`public static final String TABLENAME`）。`smartMigrate` 靠反射读它判断"表是否存在"。
- **`AbstractDao<E, K>`**：生成的 DAO 基类，`K` 是**主键类型**。`K = Void` 表示该实体**没有主键**。
- **QueryBuilder**：GreenDAO 的查询构造器（`where()` / `list()` / `unique()` / `orderAsc()` /
  `buildDelete()`）。
- **`WhereCondition`**：GreenDAO 的条件对象（`XxxDao.Properties.Yyy.eq(v)` 的产物）。
  `BaseService.find(WhereCondition…)` / `deleteAll(WhereCondition…)` 直接暴露它，
  导致调用方必须持有 `gen/` 里的 `Properties` 类。
- **`smartMigrate`**：`GreenDaoUpgrade` 的"智能迁移"，按 `getAllDaos()` 把表分成"新表 / 已存在表"，
  对已存在表执行**建临时表 → DROP 原表 → 重建 → 从临时表回填**。
- **破坏性重建**：`smartMigrate` 的本质。**会 DROP 并重建全部已存在表**，数据靠临时表回填保留。
  任何"想加索引 / 加约束"的需求都**不要**走这条路。
- **升级演练**：把 `PRAGMA user_version` 置为 `1` 后重新打开库，验证升级路径不崩且
  `SCHEMA_HISTORY` 出现 `success`。**这是验证 `smartMigrate` 的唯一手段**
  （JVM 单测无法构造 Android `SQLiteDatabase`）。
- **`SCHEMA_HISTORY`**：`SchemaHistoryRepository` 在旧库额外建的表，记录每次升级的
  `FROM_VERSION / TO_VERSION / STATUS / NOTE`。

### 三个"版本"（详见 §2.6）

- **`schemaVersion`**：`app/build.gradle` 的 `greendao{}` 块里的**生成器**版本，
  决定生成代码的 `DaoMaster.SCHEMA_VERSION`。**本项目不用它**。
- **`CURRENT_VERSION`**：`DatabaseVersionManager` 里的"用户版本"，**决定 `onUpgrade` 是否触发**。
  递增它会触发 `smartMigrate` 的破坏性重建。
- **`PRAGMA user_version`**：SQLite 实际存储值，由 `SQLiteOpenHelper` 写入，不要手改。

### RC4 加密（范围与语义见 §2.8 第 3 条）

- **实现**：`SecurityUtils.rc4Encrypt / rc4Decrypt`；包装在 `ConvertEntity.encryptIfNotEmpty /
  decryptIfNotEmpty`。
- **密钥**：`AppConst.rc4_SecretKey`。
- **要点**：旧库存的是**密文**；`updateEntity` 加密后写库再把明文改回实体；`find*` 查出后**就地解密**。
  任何数据搬运必须原样搬密文，不得二次加密。

### 门面与后台入口

- **Facade**：中央数据门面是 **`data/local/helper/DbService.java`**（19 个 `mXxxService` 字段），
  **不是**各 `XxxService.getInstance()`。
- **注意**：`DbService` 只能挡住"取 Service 实例"这一步；它**挡不住**门面后面的
  `find(WhereCondition…)` / `deleteAll(WhereCondition…)` 与调用方直接引用的 `XxxDao.Properties`。
- **DB 后台入口（不要另造线程池）**：
  - `runInBackgroundSerial(Runnable)`：单线程串行 + **无界队列**，不会抛 `RejectedExecutionException`。
    ⚠️ 用 `execute`（非 `submit`），任务内异常**不进 Future**，只会杀掉 worker 线程
    ⇒ **任务内必须自己 try/catch + `EasyLog.print(Throwable)`**。
  - `runInTransaction(Runnable)`：跨表原子操作；**任务内异常必须传出去**，
    **不得**套 `ConvertEntity.executeDatabaseOperation`（它吞异常返回 `null`）。
  - `readInBackground(Callable<T>, Callback<T>)`：**"读后回 UI"的统一入口**（2026-10-07 新增）。
    与 `runInBackgroundSerial` 的分工：它替你做三件事——切后台、执行、回主线程交付结果；
    异常由它 `EasyLog.print(Throwable)` 记录后转成 `onError`。**"读失败算什么"由调用方自己决定**
    （章节读失败按空列表、设置缓存读失败只记日志……）。**写操作不要用它**——写没有"结果可交付"。
  - `StartupIoExemption.runExempted(Runnable)`：严格模式的**窄粒度豁免窗口**（2026-10-07 新增）。
    有些启动期动作（开库、建升级历史表、首次建索引、读版本号、建立 LocalServices 单例、读登录记录）
    **无法移走**，显式标记为已知例外，好让剩余信号里只剩真正要修的东西。
    调用方必须**逐段显式声明**豁免范围，不允许整段把自己的业务包进去。
  - `ThreadUtil.runInBackground(Runnable)`：走 `ThreadPoolManager`（core=0 / max=200 /
    SynchronousQueue）⇒ **无并发上限、无串行保证、满载抛 `RejectedExecutionException`**。
    DB 操作**不要**用它。

---

## 10. 附录 B：变更历史

| 日期 | 变更 |
|---|---|
| 2026-10-07 | **第二轮双轴 review 后的修正**：① **撤销登录态异步化**——`HomeActivity` 建导航时只读一次 `isLogin`（`setupNavigation → addAiChatNavigationItemIfNeeded` / `getMaxFragmentIndex`）且之后不重建，异步恢复会让已登录用户整个会话看不到 AI 聊天入口；改回同步，并把这读放进 `StartupIoExemption` 窗口（与开库同属一次性前置）。② 修正三处**注释与实现矛盾**（豁免窗口范围、`debug 之外是空操作`、`编译期常量短路`）。③ 合并 `MigrationOrchestrator` 里两个紧邻的豁免窗口（拆开并不会更窄）。④ `AppDataManager` 的 3 处 `e.printStackTrace()` 换成 `EasyLog.print(Throwable)`（§4 要求串行任务体自己记堆栈）。⑤ 4 处异步 UI 回调补生命周期守卫（`isAdded` / `isFinishing` / `isViewActive`）。⑥ `AppDataManager` 4 处「后台读→回主线程」改用 `DbService.readInBackground`。⑦ 样式缓存写失败改为记录日志（不再静默吞）。⑧ 验证脚本加**防假绿断言**并把 SDK/ADB/PY 改为可配置。⑨ §3.0 / §8 / §9 补齐新符号与实施边界。 |
| 2026-10-07 | **票 12 / 13 收尾**：新增 `.scratch/greendao-hardening/drill-persistence.sh`（票 12 的 A1 方案）——设备侧验证三件事并**8/8 通过**：唯一索引建出、有重复行时只告警不建索引且 App 不崩、删掉整张表后冷启能被 `createTable` 建回（证明表名缓存没破坏自愈）。票 13 新增 `DbService.readInBackground(Callable, Callback)` 作为"读后回 UI"的统一入口，5 个读调用点收敛（`HomeFragment` / `BookContentSearchActivity` / `ChatSummaryListDialog` / `TipsBookNetReadFragment` / `BookCollectCaseFragment`）；**写操作刻意不收敛**（各自的任务体本来就不同，抽模板只会掩盖差异），文件缓存刻意不走 DB 队列。回归实测：无 `FATAL`/`ANR`，严格模式仍是 12 条（SDK 初始化 10 + 第三方 2），本包业务代码 0 条。 |
| 2026-10-07 | **双轴 code-review 后的修正**：① 豁免窗口收窄并显式化——新增 `StartupIoExemption.runExempted`，只包住"开库 / 建升级历史表 / 首次建索引 / 读版本号 / 建立 LocalServices 单例"这五段一次性启动工作（原先整个 `ensureUpToDate` 被豁免，把索引维护的两次全表 `COUNT(*)` 也盖住了）；② 升级路径 5 处 catch 补`EasyLog.print(Throwable)`（§3.5 的"日志带堆栈"此前未落地）；③ `BaseService` 表名缓存**读失败不再缓存**，避免"读不到"被当成"库里没表"而让每个 Service 都去建表；④ `BookRepository` 两个异步读失败改为**按空结果回调**（与同步版语义一致），并删掉因此不再有调用者的 `deliverErrorOnUi`；⑤ 聊天摘要弹窗的异步回调补 `isShowing()` 判断。**另修一个真 bug**：登录二次写入原本是 `deleteEntity`（删掉登录信息 ⇒ 每次冷启动都判未登录），改为"沿用本地已有行主键后 update"。**遗留**：持久化改动缺测试、"后台读→回主线程"样板重复 13 处（Standards 轴发现，已开票 12 / 13）。 |
| 2026-10-06 | **联调取证后的回填（票 08/11）**：§3.3 计数 28 → 24（剔除注释内 4 处与本来就在后台的 1 处）；新增 §3.3.1（口径没覆盖到的 `AppDataManager` 全量加载链，已迁后台）与 §3.3.2（`BaseService.initTable` 的构造器副作用与处理）。P1-1 的可判定判据（主流程无主线程 DB IO）已在模拟器上实测达标。 |
| 2026-10-06 | **补 §3.0「改动点签名清单」**——此前新增文件只给了类名、没有方法签名，不满足计划类文档"精确的函数名称与签名"的要求；同时把 §3.3 的迁移写法固化为三种调用范式（写 / 读后回 UI / 跨表原子），消除"读了不知道怎么写"的缺口。§3.1 明确区分两处 guide 落点（**硬前置 → §4**、**表集合一致性约束 → §6**）。 |——此前新增文件只给了类名、没有方法签名，不满足计划类文档"精确的函数名称与签名"的要求；同时把 §3.3 的迁移写法固化为三种调用范式（写 / 读后回 UI / 跨表原子），消除"读了不知道怎么写"的缺口。§3.1 明确区分两处 guide 落点（**硬前置 → §4**、**表集合一致性约束 → §6**）。 |
| 2026-10-06 | **移除全部非 GreenDAO 内容**（此前保留的另一种 ORM 的迁移评估、对比与触发条件整节删除），本文收敛为纯 GreenDAO 加固方案。**删节后章节号顺移**：术语表 → §9，变更历史 → §10；原「何时再评估迁移」整节及其摘要已删除，旧编号不再存在。同时修正：`buildDelete` 列的标签（含 `executeDelete`）、"约 78 处调用点"→ 按六项口径实测 84 处、`BookChapter.java` 与 `MySQLiteOpenHelper.java` 的行号引用改为符号名 + 声明行。 |
| 2026-10-06 | **本文合并成型**：把此前 7 份互相引用的文档合并为**一份自足设计文档**；ADR 文件形式放弃，决策与理由保留在 §1.3。 |
| 2026-10-06 | 两轮两轴 code-review 后的修正：计数口径统一为 28 处（16 写 / 12 读）；`schemaVersion` 的作用更正为"决定 `DaoMaster.SCHEMA_VERSION`，本项目不用它"；`DataRepository` / `BaseService` 行号按工作树更新并改为"符号名 + 快照行号"双写。 |
