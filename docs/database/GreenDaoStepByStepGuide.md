# GreenDAO 功能使用逐步指南

> 适用对象：对数据库改造了解不多、需要一步一步操作指引的同学。按照下面的顺序执行即可完成增删实体、触发迁移和排查问题。

---

## 一、首次确认（只做一次）

1. **确定版本号来源**
   - 打开 `run/yigou/gxzy/greendao/util/DatabaseVersionManager.java`，记下 `CURRENT_VERSION` 的数值。
2. **确认迁移已被启动**
   - 在 `AppApplication.onCreate` 中可以看到 `MigrationOrchestrator.ensureUpToDate(this);`，说明应用启动时一定会自动检查数据库。
3. **了解历史记录位置**
   - 数据库中存在 `SCHEMA_HISTORY` 表，由 `SchemaHistoryRepository` 自动写入。排查问题时会用到。

> 完成以上三点后，就可以进行日常的新增、修改操作。

---

## 二、新增实体（Step by Step）

1. **生成实体与 Dao**
    - 执行你常用的 GreenDAO 生成命令，例如：

       ```bash
       ./gradlew greendaoGenerate -Pentity=Foo
       ```

    - 确保 `entity/` 和 `gen/` 目录下出现新的类。
2. **检查 Dao 是否已注册**
   - 运行 `./gradlew checkGreenDaoEntities`（任务已在工程中提供）。
   - 终端输出成功即表示 `EntityRegistrationHelper` 已包含该 Dao。
3. **提升数据库版本**
   - 打开 `DatabaseVersionManager` 将 `CURRENT_VERSION` + 1，例如从 `3` 改为 `4`。
4. **（可选）编写迁移计划**
   - 如果新表需要预置数据或索引，可在 `GreenDaoUpgrade.migrateByVersion` 中添加对应逻辑。
5. **执行验证**
   - 启动应用或运行单元测试。首次启动会创建新表。
   - 在日志中搜索 `MigrationOrchestrator`，确认升级已执行。

### 示例：新增 ChatDraft 表

1. 运行 `./gradlew greendaoGenerate -Pentity=ChatDraft`，生成 `ChatDraft` 实体与 `ChatDraftDao`。
2. 执行 `./gradlew checkGreenDaoEntities`，终端会显示 `ChatDraftDao registered`（或类似成功信息）。
3. 将 `CURRENT_VERSION` 从 `4` 调整为 `5`。
4. 如果需要默认数据，可在 `migrateByVersion` 的 `if (oldVersion < 5 && newVersion >= 5)` 中插入 `INSERT` 语句。
5. 安装/启动应用后，打开数据库浏览器即可看到全新的 `CHAT_DRAFT` 表。

---

## 三、修改已有实体（Step by Step）

1. **更新实体/Dao 代码**
   - 按照业务需求调整字段并重新生成 Dao。
2. **同步版本号**
   - 仍然在 `DatabaseVersionManager` 中将 `CURRENT_VERSION` + 1。
3. **描述迁移逻辑**
   - 在 `GreenDaoUpgrade.migrateByVersion` 中判断旧版本并调用：
     - `AutoMigrationHelper.autoMigrateAllTables`（自动补列）
     - 或 `MigrationHelper.addColumnIfNotExists` / 临时表方案（复杂场景）。
4. **运行应用验证**
   - 观察日志或查看数据库 `SCHEMA_HISTORY`，确认状态为 `success`。
5. **功能自测**
   - 关注受影响的 Service/页面，确保读写正常。

### 示例：为 UserInfo 增加 avatarUrl 列

1. 在 `UserInfo` 实体中新增 `private String avatarUrl;` 并重新生成 Dao。
2. 将版本号 `CURRENT_VERSION` 从 `5` 提升到 `6`。
3. 在 `GreenDaoUpgrade.migrateByVersion` 中添加：

   ```java
   if (oldVersion < 6 && newVersion >= 6) {
       addColumnIfNotExists(db, "USER_INFO", "AVATAR_URL", "TEXT");
   }
   ```

4. 启动应用，`SCHEMA_HISTORY` 中最新记录的 `TO_VERSION` 应为 `6` 且状态 `success`。
5. 打开依赖用户头像的页面，确认新增字段读取正常（默认值为空字符串）。

---

## 四、排查问题（Step by Step）

1. **升级没有触发？**
   - 用 `adb shell` 或数据库管理工具查看 `SCHEMA_HISTORY` 最后一条记录。
   - 若 `TO_VERSION` 小于 `CURRENT_VERSION`，说明升级尚未执行，可以清除应用数据后重启。
2. **提示缺少列？**
   - 检查 `GreenDaoUpgrade.migrateByVersion` 是否漏写该列，或确认 `AutoMigrationHelper` 是否覆盖到对应 Dao。
3. **需要强制重跑迁移？**
   - 增加一次版本号（即使只是 +1），重新启动应用即可。
4. **定位详细错误日志**
   - 搜索 `MigrationOrchestrator`、`SchemaHistoryRepository`、`GreenDaoUpgrade` 的日志输出；失败时会打印异常堆栈。

### 示例：升级未生效的排查流程

1. 发现新列未创建，首先执行 `adb shell run-as run.yigou.gxzy cat databases/gxzy.db` 查看表结构（或使用数据库工具）。
2. 再查询 `SCHEMA_HISTORY`：`SELECT * FROM SCHEMA_HISTORY ORDER BY ID DESC LIMIT 1;` 如果 `TO_VERSION` 仍为旧版本，说明升级未触发。
3. 将 `CURRENT_VERSION` 再加 1（如从 6 到 7），重新启动应用，观察 Logcat 中 `MigrationOrchestrator` 日志“Database user version=6, target=7”。
4. 若依旧失败，查看相同时间段的异常堆栈定位 SQL 语法或数据问题。

---

## 五、常用命令速查

| 场景 | 命令/位置 |
| --- | --- |
| 生成新实体 | `./gradlew greendaoGenerate -Pentity=Foo` |
| 校验 Dao 注册情况 | `./gradlew checkGreenDaoEntities` |
| 查看版本号 | `DatabaseVersionManager.CURRENT_VERSION` |
| 查看迁移日志 | `Logcat` 中检索 `MigrationOrchestrator` 或查询表 `SCHEMA_HISTORY` |

### 命令示例

- **生成实体**：`./gradlew greendaoGenerate -Pentity=ChatDraft` → 成功后会在 `app/build/generated/source/greendao/` 下看到新 Dao。
- **查看版本号**：使用 IDE 搜索 `CURRENT_VERSION` 并直接修改，或在代码中临时 `Log.d("DB", "version=" + DatabaseVersionManager.getCurrentVersion());`。
- **查看迁移记录**：在 Android Studio 的 Database Inspector 中执行 `SELECT * FROM SCHEMA_HISTORY;`，即可确认每次升级的 `STATUS`、时间戳。

---

## 六、常见问答（Q&A）

**Q1：已经在 IDE 中 Run/Debug，一样会生成 `ChatDraftDao`，和执行 `./gradlew greendaoGenerate -Pentity=ChatDraft` 有区别吗？**

A1：最终生成的 Dao 文件完全一致；差别只在触发方式。独立运行 `greendaoGenerate` 可以单独生成代码、无需编译整个 App，适合提前 review；Run/Debug 时是构建流程顺带执行生成任务，相当于多跑了一次完整 build。

**Q2：既然有 `AutoMigrationHelper`，为什么还要手写 `addColumnIfNotExists(db, "USER_INFO", "AVATAR_URL", "TEXT")`？**

A2：自动迁移仅能处理基础的新增列，并不会推测复杂逻辑。涉及列重命名、默认值填充、索引等操作必须显式写在 `migrateByVersion` 里，否则 ORM 不会自动处理。手写 `addColumnIfNotExists` 可以精确控制升级过程，并让代码阅读者清楚本次版本需要的变更。

---

按照以上步骤操作，即使对 GreenDAO 改造细节不熟悉，也能顺利完成日常的新增/修改，并在出现问题时快速定位。遇到特殊场景，可再参考 `GreenDaoUpgradeGuide.md` 获取更多范例。
