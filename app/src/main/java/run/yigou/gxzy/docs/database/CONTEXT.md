# 领域术语表（CONTEXT）：本地数据库层

> 本文件是 `data/local` 包的统一语言（ubiquitous language）速查表，配合 `adr/0001-dbservice-design.md` 使用。
> 凡涉及「DbService 设计」「主线程 DB 访问」「跨表事务」的讨论，以本表术语为准，避免各文档各说各话。

---

## 核心概念

| 术语 | 含义 | 备注 |
|---|---|---|
| **DbService** | 数据库**执行协调器**（非领域 service）。持有串行执行器，提供 `runInBackgroundSerial` / `readInBackground` / `runInTransaction` 三个入口。 | ADR-0001 后只保留"执行器 + 事务"两职；不再充当服务定位器。 |
| **LocalServices** | 服务**定位器**。单例，持有 19 个 `*Service` 引用，供"一个 import 拿全本地数据"的调用方使用。 | ADR-0001 Q1=C 的结论；替代原 `DbService` 的 19 字段。 |
| **串行执行器** | `DbService.mSerialExecutor`：`Executors.newSingleThreadExecutor`，线程名 `mf-db-serial`，优先级 `NORM_PRIORITY-1`，无界队列。 | 所有写操作与跨表事务 funnel 到这条线程，保证不与彼此并发访问同一 SQLite 连接。 |
| **跨表事务** | `DbService.runInTransaction(Runnable)`：把跨多个 service（多张表）的"删+插"包进同一个 SQLite 事务，要么都提交、要么都回滚。 | 例：`clearAndSaveNavTabs` 同时改 `TabNav` 与 `TabNavBody`。 |
| **主线程 DB 访问** | 在主线程直接 `findAll()` / `insert()` 等。 | 被禁止（见 greendao-hardening 票 04–07）；一律经 `runInBackgroundSerial` / `readInBackground`。 |
| **无界队列** | 串行执行器用 `LinkedBlockingQueue`（无 capacity）。提交即受理，不会因池满抛 `RejectedExecutionException`。 | 调用方可依赖"任务一定会被执行"——例如当作完成回调的兜底通道。 |

---

## 并发与异常术语

| 术语 | 含义 |
|---|---|
| **SQLiteDatabaseLockedException / SQLITE_BUSY** | 两个操作并发抢同一 SQLite 连接时抛出。串行执行器 + ADR-0001 Q3 的"自我串行化"就是要根除它。 |
| **rollback-journal 模式** | 本 app 未启用 WAL（仅 `getWritableDatabase()`），故写事务进行中会阻塞读。这是"全部 DB 操作 funnel 到单线程"约束成立的前提。 |
| **WAL（Write-Ahead Logging）** | SQLite 的另一种日志模式，允许一个写者 + 多个并发读者。本 app 未启用；启用前不要假设"读不阻塞写"。 |
| **事务自我死锁** | 单线程执行器上，若某任务持有线程期间又往同一条线程排了新任务 → 新任务永远排不上 → 死锁。ADR-0001 Q3 用"已在串行线程则内联短路"规避。 |
| **异常透传** | `runInTransaction` 的任务内异常必须向上抛（不能吞），否则事务会照常提交、留下删一半/写一半的表。对应纪律：任务内不要套 `ConvertEntity.executeDatabaseOperation`（它吞异常）。 |
| **失败可见性** | `readInBackground` 走 `EasyLog.print` + `onError`；ADR-0001 Q4 要求 `runInBackgroundSerial` 也 `EasyLog.print`，消除写失败静默丢失。 |

---

## 角色与边界（一句话）

- **DbService**："怎么执行"——串行、后台、事务、失败日志。
- **LocalServices**："有哪些本地数据"——定位 19 个 service。
- **各 `*Service`（如 `SearchHistoryService`）**："怎么读写某张表"——GreenDAO 实体访问，单例。
- **GreenDaoManager**："连接从哪来"——持有 `DaoMaster`/`DaoSession`/`Database`，进程内只构建一次。
- **DataRepository**："业务级组合"——把多个 service 组合成"全量覆盖导航"等用例，跨表时借 `DbService.runInTransaction`。

---

## 关联文档

- `adr/0001-dbservice-design.md` — 本术语表对应的决策记录。
- `greendao-guide.md` / `greendao-hardening-plan.md` — 主线程 DB 访问迁移与防火规范（票 04–07、13）。
- 票 12 — 持久化测试覆盖（A2 已交付：事务语义 Robolectric 单测 4/4）。
