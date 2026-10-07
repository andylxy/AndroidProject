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

> **DbService 成为数据库工作的唯一串行化权威**：它拥有串行执行器，所有写操作与跨表事务都经它 funnel 到同一条 `mf-db-serial` 线程；读经 `readInBackground` 也走这条线程。于是「任意两个 DB 操作不会并发访问同一连接」由架构硬保证，而非靠调用约定。

---

## 实施要点（Implementation Notes，待执行）

### Q1 — 拆出 `LocalServices`（定位器）
- 新建单例 `LocalServices`，持有原 19 个 service 字段，构造时一次性 `getInstance()` 填充（或懒加载）。
- 从 `DbService` 删掉 L48–66 字段与 L73–91 赋值。
- 调用方改动（纯机械改名，约 30+ 处）：
  - `DbService.getInstance().mChatSessionBeanService...` → `LocalServices.getInstance().mChatSessionBeanService...` 或干脆 `ChatSessionBeanService.getInstance()`。
  - 涉及 `ChatSessionManager`（~19 处）、`SettingActivity`、`ChapterContentManager`、`AppApplication`（L276）、`TipsBookNetReadFragment`、`AppDataInitializer`、`DataRepository`（L228–229 的 `db.mTabNavService` / `db.mTabNavBodyService`）。
- 副作用（通常是好事）：首次 `DbService.getInstance()` 不再顺带构建 19 个 service 单例，启动成本下降。

### Q2 — 去掉 `mDatabase` 字段
- 删 L124 `private final Database mDatabase = GreenDaoManager.getDaoMaster().getDatabase();`。
- `runInTransaction` 内改为：`Database db = GreenDaoManager.getDatabase();` 再 `db.beginTransaction()...`。
- `GreenDaoManager.getDatabase()`（GreenDaoManager.java:77）已有 null 兜底，直接复用。

### Q3 — `runInTransaction` 自我串行化
- 区分"当前是否已在串行线程"：可用一个 `AtomicReference<Thread>` 记录执行器线程，或在提交任务的 Runnable 里把"自己是否在串行线程"标记。
- 典型实现：
  ```java
  public void runInTransaction(Runnable task) {
      if (task == null) return;
      Runnable tx = () -> {
          Database db = GreenDaoManager.getDatabase();
          db.beginTransaction();
          try {
              task.run();
              db.setTransactionSuccessful();
          } finally {
              db.endTransaction();
          }
      };
      if (isOnSerialThread()) {
          tx.run();                       // 内联短路，避免自我死锁
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
- 并发正确性从"约定"变"硬保证"——`runInTransaction` 不再可能因调用线程不同而和写并发。
- 写失败可见，排障不再黑盒。
- 消除"首次 `DbService.getInstance()` 顺带拉起 19 个单例"的隐含启动成本。

**负向 / 风险**
- Q1 是纯机械但面广的改名（~30+ 处），需编译 + 全量 `:app:testDebugUnitTest` 回归。
- Q3 引入"是否在串行线程"判断，需保证 `isOnSerialThread()` 判定可靠（建议用执行器线程身份而非线程名字符串比较）。
- Q3 的同步等待（`future.get()`）会让调用线程阻塞；仅当调用方本就在后台线程时才安全——同步版 `clearAndSaveNavTabs` 若被主线程直接调，会阻塞主线程。因此**仍建议**业务入口统一走 `clearAndSaveNavTabsAsync` 这类异步包装（Q3 解决的是"即使同步调也不会破坏原子性"，并不鼓励主线程同步调）。

---

## 遗留 / 未决（记录但不阻塞本 ADR）

- **命名**：`DbService` 实为"数据库执行协调器"，并非领域 `*Service` 那种数据访问对象；改名 `DbExecutor` / `DbCoordinator` 更贴切，但属纯命名优化，不在本 ADR 强制范围。
- **防反射攻击守卫**（L69–72，`if (instance != null) throw ...`）：因 `instance` 在构造完成后才赋值，该守卫实际永不触发，是无效代码；建议随 Q1 重构一并删除（非功能缺陷，仅冗余）。
- **表名缓存"读失败不写缓存"分支**：`BaseService.existingTableNames` 无法从外部构造读失败，仍无测试（见票 12 已知保留项）。
