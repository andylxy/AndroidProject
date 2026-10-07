# ADR-0001：DbService 的职责边界与并发正确性

- **状态**：已实施并通过联调验证（Accepted / Implemented / Verified）
- **日期**：2026-10-07
- **范围**：`app/src/main/java/run/yigou/gxzy/data/local/helper/DbService.java` 及其调用方
- **来源**：grill-with-docs 对「DbService 这个类设计是否合理」的逐条质询（Q1–Q4）

---

## 背景（Context）

`DbService` 当前是「三合一」结构，同时承担三种变化理由完全不同的职责：

1. **服务定位器**：19 个 `mXxxService` 公有字段（L48–66），构造时一次性 `getInstance()` 拉起全部 service（L73–91）。
2. **异步执行门面**：`runInBackgroundSerial(...)` + `readInBackground(...)`。
3. **跨表事务协调器**：`runInTransaction(...)`。

逐条质询中暴露了若干**真实正确性问题**（不是风格偏好）：

- `mDatabase` 在构造时 `final` 捕获（L124），存在「初始化顺序地雷」与「未来重开库时悬空句柄」两类脆弱性。
- `runInTransaction` 是**同步**跑在调用线程（L207–218），**没提交到串行执行器**。其原子性声明只在「也跑在 `mf-db-serial` 上」时才成立；同步调用方（如 `DataRepository.clearAndSaveNavTabs` 的 `public static` 路径）从其它线程调时，会与串行写**并发访问同一 SQLite 连接**。
- `runInBackgroundSerial`（L185–190）裸 `execute`，任务异常直接丢失（不像 `readInBackground` 那样 `EasyLog.print`）。
- 这 app 未启用 WAL（仅 `getWritableDatabase()`），默认 rollback-journal 模式下写事务会阻塞读——所以"全部 DB 操作 funnel 到单线程"的约束是正确的，问题只在于 `runInTransaction` 漏网。

---

## 决策（Decision）

| 编号 | 议题 | 结论 |
|---|---|---|
| **Q1** | 类身份 / 职责边界 | **(C)** 拆成两个：`DbService` 只保留「执行器 + 事务」；19 个字段搬进独立的 `LocalServices` 定位器 |
| **Q2** | `mDatabase` 捕获时机 | **(B)** 删除 `final` 字段；`runInTransaction` 每次调用现取 `GreenDaoManager.getDatabase()` |
| **Q3** | `runInTransaction` 是否自我串行化 | **(B)** 自我串行化：不在 `mf-db-serial` 线程时整段提交到串行执行器并阻塞等结果、异常透传；已在串行线程时内联短路（防自我死锁） |
| **Q4** | `runInBackgroundSerial` 失败可见性 | **(B)** 包一层 try/catch，任务异常统一 `EasyLog.print`（与 `readInBackground` 对齐） |

### 收敛后的不变量

> **`DbService` 层内的事务与异步写是串行化的**：它拥有串行执行器，经`DbService` 提交的
> 写任务与跨表事务都 funnel 到同一条 `mf-db-serial` 线程；读经 `readInBackground` 也走这条线程。
> 于是「经 `DbService` 的任意两个操作不会并发访问同一连接」由架构硬保证，而非靠调用约定。
>
> **范围限定（重要）**：该保证**不覆盖** `BaseService.replaceAllInTx` / `replaceWhereInTx` ——
> 它们自行调 `beginTransaction`（pre-existing，非本次引入）。故「全库所有写都走串行线程」
> 目前仍是约定而非硬保证，收口需独立议题。见「遗留 / 未决」。

---

## 实施要点（Implementation Notes）

### Q1 — 拆出 `LocalServices`（定位器）
- 新建单例 `LocalServices`，持有原 19 个 service 字段，构造时一次性 `getInstance()` 填充（或懒加载）。
- 从 `DbService` 删掉 L48–66 字段与 L73–91 赋值。
- 调用方改动（纯机械改名，16 个文件共 80 处）：
  - `DbService.getInstance().mChatSessionBeanService...` → `LocalServices.getInstance().mChatSessionBeanService...` 或干脆 `ChatSessionBeanService.getInstance()`。
  - 涉及 `ChatSummaryListDialog`、`ChatSessionManager`（~19 处）、`SettingActivity`、`ChapterContentManager`、`AppApplication`（L276）、`TipsBookNetReadFragment`、`AppDataInitializer`、`DataRepository`（L228–229 的 `db.mTabNavService` / `db.mTabNavBodyService`）。
- 副作用：`DbService` 的构造器不再顺带构建 19 个 service 单例（变为无状态协调器）。
  - **注意：启动成本并未因此下降** —— 19 个 service 仍被急切构建，只是构建点从
    `DbService.getInstance()` 移到了 `LocalServices.getInstance()`。首次构建必须留在
    `AppApplication.initDatabaseOnStartup` 的 `StartupIoExemption` 窗口内，否则主线程 DB IO 会
    触发 StrictMode 违例（即 `dc48c18` 当初治的那 10 条）。改动此窗口时须一并核对。
  - 之所以仍要拆：**触发时机可分离**。原先「谁先调 `DbService.getInstance()`，谁就承担开库副作用」，
    现在可以把这件事显式放进指定窗口，而不是依赖调用顺序的巧合。

#### 为什么保留定位器，而不是让调用方直连 `XxxService.getInstance()`（2026-10-07 复核补充）

评审时本条被质疑为 Middle Man —— 19 个字段全部指向已存在的单例，调用点
`LocalServices.getInstance().mXxxService` 比直连多一跳。**结论是保留**，理由两条（2026-10-07 复核时
删掉了一条不成立的，见下）：

1. **替换边界**。19 个 service 的持有方式若要整体更换（换 ORM、换存储、加一层缓存），
   改定位器一处即可；散落在 16 个文件 80 处调用点直连则要改 80 处。
2. **字段已声明 `final`**，构造后不可改写，定位器不会退化成可变容器 ——
   这是把它当 Middle Man 的主要担忧，已从设计上消除。

> **已删除的理由（复核时发现不成立）**：初版写「调用点是『一个入口拿全本地数据』的真实语义」。
> 实测全仓**没有任何调用点**从同一次 `getInstance()` 取 2 个以上 service
> （唯一样本是 `AppDataInitializer:99`，也只取 `mTabNavService` 一个），故该理由无代码证据支撑，
> 不作为决策依据。真正站得住的是上面两条。

**代价**：多一跳间接。判断标准是「间接是否换来可替换性」——本例换来，故接受。
这条间接目前是**纯成本**（无调用点因定位器而变得更简洁），如实记录之。
若将来出现第三种持有方式，或 service 数量大幅变化，此结论需重新评估。

### Q2 — 去掉 `mDatabase` 字段
- 删 L124 `private final Database mDatabase = GreenDaoManager.getDaoMaster().getDatabase();`。
- `runInTransaction` 内改为：`Database db = GreenDaoManager.getDatabase();` 再 `db.beginTransaction()...`。
- `GreenDaoManager.getDatabase()`（GreenDaoManager.java:77）在 `daoMaster` 未就绪时**返回 null**（它不是"兜底"，只是把 null 原样传出），故 `runInTransaction` 内须先判空再 `beginTransaction()`，否则 NPE 且 `endTransaction` 不执行。

### Q3 — `runInTransaction` 自我串行化
- 区分"当前是否已在串行线程"：可用一个 `AtomicReference<Thread>` 记录执行器线程，或在提交任务的 Runnable 里把"自己是否在串行线程"标记。
- 典型实现：
  ```java
  public void runInTransaction(Runnable task) {
      if (task == null) return;
      Runnable tx = () -> {
          Database db = GreenDaoManager.getDatabase();
          // getDatabase() 在 daoMaster 未就绪时返回 null，必须判空：
          // beginTransaction 在 try 外，NPE 时 endTransaction 也不会执行。
          if (db == null) throw new IllegalStateException("数据库尚未打开，无法开启事务");
          db.beginTransaction();
          try {
              task.run();
              db.setTransactionSuccessful();
          } finally {
              db.endTransaction();
          }
      };
      // 实际实现内联此判断（不单独抽方法）：
      // Thread.currentThread() == mSerialThread.get()
      if (Thread.currentThread() == mSerialThread.get()) {
          tx.run();// 内联短路，避免自我死锁
      } else {
          Future<?> f = mSerialExecutor.submit(tx);
          try { f.get(); }                // 阻塞等结果，保留同步语义
          catch (ExecutionException e) {
              Throwable cause = e.getCause();
              if (cause instanceof RuntimeException) throw (RuntimeException) cause;
              throw new RuntimeException(cause);
          } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
      }
  }
  ```
- 注意：事务体**不得**再往 `mSerialExecutor` 排任务（单线程会被自己占着 → 死锁）；现有 `DataRepository` 路径未触发此情况，但文档/评审须拦住。
  - 现状：`DataRepository` 事务体内用的是 `ThreadPoolManager`（独立池），不是串行池，故未触发。
- **判据是线程身份而非线程名**：`mSerialThread` 是执行器线程对象引用，线程死亡后残留的旧引用永不与活线程相等，故无「陈旧引用误判」；执行器尚未建线程时为 null，同样不会误判。
- 测试门禁：`TransactionSemanticsTest` 用三条用例分别锁住内联短路（重入不死锁）、提交路径（事务体线程名必须是 `mf-db-serial`）、异常逐层透传（内层抛异常须整段回滚且传到最外层）。

### Q4 — `runInBackgroundSerial` 失败可见性
- 改为：
  ```java
  public void runInBackgroundSerial(Runnable task) {
      if (task == null) return;
      mSerialExecutor.execute(() -> {
          try { task.run(); }
          catch (Throwable t) { EasyLog.print(t); }
      });
  }
  ```
- 与 `readInBackground` 的异常纪律对齐；写失败进统一日志，可被发现。

---

## 后果（Consequences）

**正向**
- 职责单一：`DbService` = 执行/事务协调器；`LocalServices` = 定位器，二者变化理由分离。
- `runInTransaction` 的并发正确性从"约定"变"硬保证"——不再可能因调用线程不同而和写并发
  （限定：`BaseService` 的两个 `*InTx` 方法不在此范围内，见上）。
- 写失败可见，排障不再黑盒。
- **副作用的触发时机可分离**：原先「谁先调 `DbService.getInstance()`，谁就承担开库副作用」，
  现在可显式放进 `StartupIoExemption` 窗口。（启动**总**成本未下降——19 个 service 仍被急切构建，
  只是换了构建点。）

**负向 / 风险**
- Q1 是纯机械但面广的改名（16 文件 80 处），需编译 + 全量 `:app:testDebugUnitTest` 回归。
- Q3 引入"是否在串行线程"判断，需保证 `isOnSerialThread()` 判定可靠（建议用执行器线程身份而非线程名字符串比较）。
- Q3 的同步等待（`future.get()`）会让调用线程阻塞；仅当调用方本就在后台线程时才安全——同步版 `clearAndSaveNavTabs` 若被主线程直接调，会阻塞主线程。因此**仍建议**业务入口统一走 `clearAndSaveNavTabsAsync` 这类异步包装（Q3 解决的是"即使同步调也不会破坏原子性"，并不鼓励主线程同步调）。

---

## 遗留 / 未决（记录但不阻塞本 ADR）

- **命名**：`DbService` 实为"数据库执行协调器"，并非领域 `*Service` 那种数据访问对象；改名 `DbExecutor` / `DbCoordinator` 更贴切，但属纯命名优化，不在本 ADR 强制范围。
- **表名缓存"读失败不写缓存"分支**：`BaseService.existingTableNames` 无法从外部构造读失败，仍无测试（见票 12 已知保留项）。
- **Q2 只覆盖了 `DbService` 一处缓存**：`BaseService`（`mDatabase` / `daoSession`）等19 个 service 仍在构造时缓存库句柄。ADR 的"消除悬空句柄"理由同样适用于它们，但当前无"重开库"需求，故本次不改；若将来引入重开库，须连同 service 层一起改。
- **唯一串行化权威只在 `DbService` 层**：`BaseService.replaceAllInTx` / `replaceWhereInTx` 自行调`beginTransaction`，不经 `DbService`（pre-existing，非本次引入）。要真正做到"所有写操作 funnel 到 `mf-db-serial`"，需把这层也收口，属独立议题。
