# GreenDAO 使用与升级指南（本地关系库）

> **适用对象**：需要新增 / 修改实体、或排查数据库升级问题的开发者。
> **配套**：`greendao-hardening-plan.md`——本地关系库的**唯一设计文档**
> （现状事实、风险登记、加固方案、决策记录、术语表都在那里）。本文件只管**怎么操作**。
> 两者**各自可独立阅读**；交叉引用只作定位补充。
>
> **本文件来历**：由原 4 份文档（`GreenDaoQuickUsage` / `GreenDaoStepByStepGuide` /
> `GreenDaoUpgradeGuide` / `DatabaseMigrationImplementationGuide`）**合并重写**。
> 原 4 份里描述的 `AutoMigrationHelper`、`MigrationHelper`、`migrateByVersion`、
> `migrateToAddNewTables`、`VERSION_ENTITY_MAP`、`greendaoGenerate`、`checkGreenDaoEntities`、
> `migration.log` 在本仓库**均不存在**（已并入 `GreenDaoUpgrade` 或从未落地），相关内容已删除。

---

## 1. 启动与初始化链

```
AppApplication.initBasicConfig()                       // AppApplication.java:205
  └─ MigrationOrchestrator.ensureUpToDate(context)     // AppApplication.java:209 调用
       ├─ GreenDaoManager.getInstance()                // 触发建库/升级（见下）
       ├─ SchemaHistoryRepository.ensureTable(db)      // 确保 SCHEMA_HISTORY 存在
       └─ 读 PRAGMA user_version 并打日志              // MigrationOrchestrator.java:40-42
  └─ DbService.getInstance().mUserInfoService          // 构造即打开库（见 §6 注意）
```

`GreenDaoManager` 的私有构造（`GreenDaoManager.java:44-54`）：

```
new MySQLiteOpenHelper(context, AppConst.dbName, null, AppConfig.isDebug())
  → getWritableDatabase()          // 此处触发 onCreate 或 onUpgrade
  → new DaoMaster(db) → daoMaster.newSession()
```

- **库文件名**：`AppConst.dbName = "myzhongyi.db"`（`base/constant/AppConst.java:23`）。
- `MySQLiteOpenHelper extends VersionedOpenHelper`，而 `VersionedOpenHelper` 把
  **`DatabaseVersionManager.getCurrentVersion()` 作为版本号传给 `SQLiteOpenHelper`**
  （`VersionedOpenHelper.java:18,22`）——这就是"何时触发 `onUpgrade`"的唯一判据。

## 2. 三个"版本号"不要混

| 名称 | 位置 | 当前值 | 含义 | 改动它会发生什么 |
|---|---|---|---|---|
| `schemaVersion` | `app/build.gradle` 的 `greendao{}` 块 | `1` | GreenDAO **生成器**版本。它决定生成代码里的 `DaoMaster.SCHEMA_VERSION`（`gen/DaoMaster.java:20`） | **对本项目运行时没有影响**——本项目不用 `DaoMaster.OpenHelper`，数据库版本走 `DatabaseVersionManager`（见下）。一般不需要改它 |
| `CURRENT_VERSION` | `data/local/helper/DatabaseVersionManager.java:18` | `2` | 项目自定义"用户版本"，**决定 `onUpgrade` 是否触发** | **递增会触发 `smartMigrate` 的破坏性重建**（见 §3）。新增/修改实体时**改的是它** |
| `PRAGMA user_version` | 库文件本身 | `2` | SQLite 实际存储值，由 `SQLiteOpenHelper` 写入 | 由上面两者间接决定，**不要手改** |

> `user_version = 2` 是真机实测值（见 `greendao-hardening-plan.md` §2.7），与 `CURRENT_VERSION` 一致。
> 详细释义见 `greendao-hardening-plan.md` §9（附录 A 术语表）。

**一句话**：加实体 / 改字段 → 动 `CURRENT_VERSION`；`schemaVersion` 基本不用动。

## 3. 升级执行流程

`MySQLiteOpenHelper.onUpgrade`（`MySQLiteOpenHelper.java:55-67`）：

```
SchemaHistoryRepository.ensureTable(db)
GreenDaoUpgrade.getInstance().smartMigrate(db, EntityRegistrationHelper.getAllDaos())
  ├─ 成功 → recordUpgrade(old, new, "success", "smartMigrate")
  └─ 失败 → recordUpgrade(old, new, "failed", msg) 然后**重新抛出**
```

`smartMigrate`（`GreenDaoUpgrade.java:312-361`）的实际行为——**这是破坏性重建**：

1. 用 `getAllDaos()` 把表分成「新表」与「已存在表」；
2. 新表：`createTable(db, false)`；
3. 已存在表：`generateTempTables`（建 `X_TEMP` 并把数据整表拷进去）
   → **`dropTable` 掉原表** → `DaoMaster.createAllTables(db, false)` 重建
   → `restoreData` 从 `X_TEMP` 回填；
4. 新增的 `INTEGER`/`REAL`/`BOOLEAN` 列补 `0`，`TEXT` 列补 `NULL`。

> ⚠️ **不要为了"加索引 / 加约束"而递增 `CURRENT_VERSION`**——那会付一次全表重建的代价。
> 索引类改动请走启动时 `CREATE ... IF NOT EXISTS`（见 `greendao-hardening-plan.md` §3.2）。

## 4. 新增实体（Step by Step）

**⚠️ 硬前置：递增 `CURRENT_VERSION` 之前，必须先做掉 `greendao-hardening-plan.md` §3.1 那两步修复。**
未修复时递增版本会直接导致启动失败——`smartMigrate` 只 DROP `getAllDaos()` 里的表，
却用 `createAllTables(db, false)` 重建**全部**表，漏登记的表会被"重复创建"而抛
`table ... already exists`。现状 `getAllDaos()` 漏了 `ChatSummaryBeanDao`，所以**任何一次
`onUpgrade` 都会崩**。详见 `greendao-hardening-plan.md` §3.1。
（**不需要冻结开发**——那两步各一行，先做掉即可。）

1. **写实体类**：在 `data/local/entity/` 新建 `@Entity` 类。
   **不要手改 `data/local/gen/`**——那是 GreenDAO 插件生成目录，构建时会被覆盖。
2. **构建一次**：插件在构建期生成 `gen/<Entity>Dao.java`（没有独立的"生成实体"Gradle 任务）。
3. **登记 Dao（必做，漏了就踩 `greendao-hardening-plan.md` §3.1）**：在 `data/local/helper/EntityRegistrationHelper.java`
   对应分类方法里 `daos.add(run.yigou.gxzy.data.local.gen.XxxDao.class)`。
4. **跑一致性测试**：`EntityRegistrationHelperTest` 会断言
   「`getAllDaos()` 的表集合 == `gen/*Dao` 声明的表集合」，漏登记会**红**。
   ⚠️ 该测试类**随 `greendao-hardening-plan.md` §3.1 的修复一起新增**（该文 §7 第 4 步）——
   它落地前还不存在，此时**只能靠人工核对第 3 步**。
5. **递增版本**：`DatabaseVersionManager.CURRENT_VERSION` +1，让已有用户也能建出新表
   （不递增的话，只有全新安装的用户会通过 `onCreate` 拿到新表，老用户永远缺表 → `no such table`）。
6. **验证**：启动应用 → 看 logcat 的 `MigrationOrchestrator` 日志
   （格式为 `Database user version=<旧版本>, target=<CURRENT_VERSION>`）→ 查 `SCHEMA_HISTORY`
   最新一条应为 `success`。

> **若新表需要预置数据**：`smartMigrate` 不会插数据。可在 `onUpgrade` 之后追加一段受版本判断保护的
> 插入逻辑（见 §5 第 4 条），或在业务首次读到空表时懒加载。

## 5. 修改既有实体

1. 改 `data/local/entity/` 下的字段（**不要改 `gen/`**），构建生成新的 Dao。
2. 递增 `DatabaseVersionManager.CURRENT_VERSION`。
3. 启动应用：`smartMigrate` 会重建该表并从临时表回填，新增列按类型补默认值（`0` / `NULL`）。
4. **数据修复类逻辑**（重命名列、填默认值、去重）**没有现成的"按版本分派"入口**——
   `GreenDaoUpgrade` 里没有 `migrateByVersion` 这类方法。
   但它**提供了可复用的静态助手**，手写迁移时直接用：

   | 助手 | 位置 | 用途 |
   |---|---|---|
   | `addColumnIfNotExists(db, table, column, type)` | `GreenDaoUpgrade.java:97` | 安全补列（已存在则跳过） |
   | `autoMigrateAllTables(db, daoClasses…)` | `:159` | 批量补列（比对实体定义与表结构） |
   | `createTempTable(db, daoClass)` | `:220` | 建临时表备份（全列 TEXT） |
   | `restoreDataFromTempTable(db, daoClass)` | `:251` | 从临时表回填（处理列增减） |

   需要"按版本分派"时，在 `GreenDaoUpgrade` 内新增一个受版本判断保护的方法，
   并从 `smartMigrate` 或 `MySQLiteOpenHelper.onUpgrade` 调用；
   **同时补注释说明适用边界与何时可移除**。
5. 验证同 §4 第 6 步。

## 6. 强制约束：`getAllDaos()` 与 `createAllTables()` 的表集合必须一致

- `EntityRegistrationHelper.getAllDaos()`（**手工登记**）决定 `smartMigrate` 会 DROP 哪些表；
- `DaoMaster.createAllTables()`（**生成代码，19 张全量**）决定会重建哪些表。

**两者不一致时**：漏登记的表**永不被 DROP，却总被无条件重建**
（`createTable(db, false)` 生成的 SQL 不带 `IF NOT EXISTS`）
⇒ 一旦 `onUpgrade` 触发即抛 `table ... already exists` → **启动失败**。

**现有反例**：`ChatSummaryBeanDao` 在 `DaoMaster`（19 张）里，却漏登记 `getAllDaos()`（18 张）。
失效链与风险分析见 `greendao-hardening-plan.md` §3.1。

**因此**：新增实体时必须同时完成 §4 的第 3、4 步。
该约束的落地位置是 `greendao-hardening-plan.md` §3.1 的修复步骤（一致性测试）与
`EntityRegistrationHelper` 的类注释——**两处都随修复落地，当前尚未存在**。

> **另一个容易踩的点**：`BaseService` 的 `daoSession` 是**字段初始化器**
> （`BaseService.java:24`），所以**构造任何 Service 都会打开可写库**。
> 这意味着升级会在"第一次拿到 Service"时就发生，而不是等你显式调用。

## 7. 排障

| 现象 | 先查什么 |
|---|---|
| **启动崩溃 / `GreenDAO database initialization failed`** | logcat 搜 `table .* already exists`。命中即 §6 的表集合不一致 |
| **升级没触发** | 查 `SCHEMA_HISTORY` 最后一条的 `TO_VERSION` 是否小于 `CURRENT_VERSION`；`CURRENT_VERSION` 是否真的改了 |
| **`no such table`** | 新增了实体但没递增 `CURRENT_VERSION`（老用户不会走 `onCreate`） |
| **`no such column`** | 改了实体但没递增 `CURRENT_VERSION`，或新列没被 `restoreData` 覆盖 |
| **升级记录是 `failed`** | `SCHEMA_HISTORY.NOTE` 里有原因；结合 logcat 的 `GreenDaoUpgrade` 输出 |
| **主线程卡顿 / ANR** | 见 `greendao-hardening-plan.md` §3.3 与 §3.3.1（真实生效的是 24 处，另有一条更粗的加载链）；debug 包开严格模式后 grep logcat 的 `StrictMode policy violation` |
| **主线程有 DB 调用** | 用 `DbService.getInstance().runInBackgroundSerial(...)`（写/读统一入口），跨表原子操作用 `runInTransaction(...)`；详见 §10 |

### 取真机库来查

```bash
# debug 包名带 .debug 后缀；库文件名是 myzhongyi.db（不是 gxzy.db）
adb exec-out run-as run.yigou.gxzy.debug cat databases/myzhongyi.db > /tmp/myzhongyi.db
# 然后用任意 sqlite 客户端：
#   PRAGMA user_version;
#   SELECT * FROM SCHEMA_HISTORY ORDER BY ID DESC LIMIT 1;
#   SELECT type, name FROM sqlite_master WHERE type='index';
```

> 注意：真机库被 `pm clear` 后会是**全新库**，`SCHEMA_HISTORY` 里只有一条 `create` 记录——
> 看不到历史升级记录，不要据此判断"升级没跑过"。

## 8. 升级相关的最佳实践

- **回归测试**：每次结构变更后，跑一遍受影响模块的读写。
  升级路径的自动化验证已落地为**脚本**：`.scratch/greendao-hardening/drill-upgrade.sh`
  （自动备份 → 置 `user_version=1` → 回推 → 冷启 → 抓 logcat → 查 `SCHEMA_HISTORY` → 自动恢复），
  只对模拟器执行、全程只改 `user_version` 一个整数；执行报告落在 `.scratch/greendao-hardening/drill/`。
  设计文档 §3.4 原定的是 `src/androidTest` 里的 `DbUpgradeDrillTest`，
  实际选择了脚本方案（零依赖、可在无Android 环境的机器上重复执行），这是 §3.4 的实现方式变更。
- **表集合一致性有门禁**：`app/src/test/java/run/yigou/gxzy/data/local/helper/EntityRegistrationHelperTest.java`
  断言 `getAllDaos()` 的表集合等于 `gen/` 声明的表集合，漏登记即红。**新增实体后必跑**
  `./gradlew.bat testDebugUnitTest`（见 §6）。
- **手工验证升级**：装旧版本 → 写入典型数据 → 装新版本触发升级 → 核对 UI 与数据完整性。
  这是**目前唯一能端到端验证升级路径的手段**。
- **复杂迁移前先备份**：升级前把 `databases/myzhongyi.db` 拷出来（见 §7 的命令），
  或确保改动可回滚（`smartMigrate` 会 DROP 原表，回填失败即丢数据）。
- **版本号只升不降**；优先让 `smartMigrate` 走增量（补列）而不是全量重建。
- **改动前先确认 `CURRENT_VERSION` 现值**，不要在不知当前值的情况下 +1。

## 9. 速查表

| 场景 | 位置 / 命令 |
|---|---|
| 库文件名 | `AppConst.dbName` = `myzhongyi.db` |
| 版本号（触发升级的） | `DatabaseVersionManager.CURRENT_VERSION` |
| 生成器版本（与运行时无关） | `app/build.gradle` 的 `greendao { schemaVersion }` |
| 升级入口 | `AppApplication.java:205` → `MigrationOrchestrator.ensureUpToDate`（`:209` 调用） |
| 升级实现 | `GreenDaoUpgrade.smartMigrate` |
| 可复用迁移助手 | `GreenDaoUpgrade.addColumnIfNotExists` / `autoMigrateAllTables` / `createTempTable` / `restoreDataFromTempTable` |
| Dao 登记表 | `EntityRegistrationHelper.getAllDaos()` |
| 升级历史表 | `SCHEMA_HISTORY`（`SchemaHistoryRepository`） |
| 单测 | `gradlew.bat testDebugUnitTest` |
| 构建 | `gradlew.bat assembleDebug` |
| 主线程 IO 的统一入口 | `DbService.runInBackgroundSerial(Runnable)` / `DbService.runInTransaction(Runnable)`（见 §10） |
| 启动期一次性 IO 豁免 | `StartupIoExemption.runExempted(Runnable)`（只包住开库、建升级历史表、首次建索引、读版本号） |
| 升级演练脚本 | `.scratch/greendao-hardening/drill-upgrade.sh`（升级路径）、`.scratch/greendao-hardening/drill-persistence.sh`（索引 + 表名缓存 + 重复行防御） |
| 加固票据与证据 | `.scratch/greendao-hardening/issues/` 与 `.scratch/greendao-hardening/verify/` |

## 10. 主线程 DB 访问：唯一入口与写法

**规则：任何 DB 调用都不许写在主线程。**统一走 `DbService` 的两个入口，不要自己起线程
（更不要用 `ThreadUtil.runInBackground`——它走 `ThreadPoolManager`，无串行保证、无并发上限，
对本仓的 SQLite 库会并发抢锁）。

| 场景 | 写法 | 关键约束 |
|---|---|---|
| 写（不关心结果） | `DbService.getInstance().runInBackgroundSerial(task)` | task 内部抛出的异常由 `DbService` 统一接住并 `EasyLog.print(Throwable)` 记录，**不会**冒泡、也不会杀掉串行线程（ADR-0001 Q4）。仍建议任务内自行 `try/catch` 以便按业务语义降级（读失败按空结果、写失败回滚局部） |
| 读后回 UI | **首选** `DbService.getInstance().readInBackground(reader, callback)`：`reader` 在后台读，`callback` 在主线程收到结果或错误 | task 内异常会被自动记录并转成 `callback.onError`；**"读失败算什么"由调用方自己决定**（章节读失败按空列表、设置缓存读失败只记日志……） |
| 读后回 UI（需要自己控制线程切换时） | `runInBackgroundSerial` + `ThreadUtil.runOnUiThread` | 被捕获变量必须 final / 有效 final；**读失败按"空结果"回调**，与同步方法的失败语义保持一致 |
| 跨表原子操作 | `DbService.getInstance().runInTransaction(task)` | task 内异常**必须传出去**；不要套 `ConvertEntity.executeDatabaseOperation`（它吞异常并返回 null，会让事务照常提交，留下删一半/写一半的表） |

**文件缓存（不是数据库）不要走 DB 队列**：像样式配置、片段设置这类 `CacheHelper` 文件读写，
应该用 `ThreadUtil.runInBackground` / `runOnUiThread`，不要塞进 `DbService` 的串行线程——
那会把 DB 队列的"单线程"语义稀释成"什么都串行"。

**为什么不能把同步方法直接改成异步**：形如 `getChapters(String): List<Chapter>` 这类返回值型 API
一旦挪到后台就会破坏返回值约定。做法是**新增 `*Async` 方法**（`getChaptersAsync` / `addToBookshelfAsync` …），
调用方显式改写；旧的同步方法保留给后台调用方。改名不是洁癖——返回值语义变了，名字必须讲清楚。

**已知例外（不可移走的那部分）**：进程启动时的开库、建立 `LocalServices` 单例（它会构建 19 个
service，各自确认表是否存在）、建升级历史表、
首次建索引、读版本号。这些都以"库刚打开、连接可用"为前提，由`StartupIoExemption` 显式豁免，
并**只在 debug 构建**生效（`AppApplication.enableStrictModeForDebug()` + `AppConfig.isDebug()`）。

**顺带一条语义坑（别再踩）**：`UserInfoService.addEntity` 会给新增行生成随机 UUID 主键，
所以服务端返回的实体**不能直接 `updateEntity`**（主键对不上，静默无更新）。
二次登录的正确写法是`data.setId(userInfo.getId())` 沿用本地已有行的主键再更新；
否则 `AppApplication.initUserLogin()` 在下次启动读不到行，会判定为未登录。
| 本机库路径 | `databases/myzhongyi.db`（应用私有目录） |
| 设计与决策 | `greendao-hardening-plan.md` |
