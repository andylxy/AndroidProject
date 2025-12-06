# GreenDAO 升级与实体管理指南

## 概述

本文面向需要维护/扩展数据库的同学，给出最新版的 GreenDAO 升级策略、实体注册方式以及常见问题定位手段。

## 主要能力

### 1. 版本管理

- [DatabaseVersionManager](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/DatabaseVersionManager.java#L8-L31) 统一维护版本号
- [MySQLiteOpenHelper](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/MySQLiteOpenHelper.java) 基于 `VersionedOpenHelper`，可自由 bump 版本
- `SCHEMA_HISTORY` 由 [SchemaHistoryRepository](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/SchemaHistoryRepository.java) 记录升级记录

### 2. 升级策略

- [GreenDaoUpgrade](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/GreenDaoUpgrade.java) 提供 `migrate`、`migrateToAddNewTables`、`smartMigrate`、`migrateByVersion`
- [MigrationOrchestrator](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/MigrationOrchestrator.java) 在 `AppApplication.onCreate` 中执行，保证冷启动即完成迁移

### 3. 自动迁移

- [AutoMigrationHelper](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/AutoMigrationHelper.java#L15-L192) 比对列差异并补齐缺失列

### 4. 实体注册

- [EntityRegistrationHelper](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/EntityRegistrationHelper.java#L14-L173) 维护 Dao 清单与版本映射

## 使用指南

### 添加新实体

1. 通过 GreenDAO Generator 生成实体/Dao
2. 确认 Dao 自动出现在 `EntityRegistrationHelper` 中（或手动登记）
3. 将 `DatabaseVersionManager.CURRENT_VERSION` +1
4. 如需种子数据或额外列处理，则在 `migrateByVersion` 中补充逻辑

### 修改已有实体

1. 更新实体字段并重新生成 Dao
2. bump 版本号
3. 在 `migrateByVersion` 描述列变化或数据修复策略

### 升级执行流程

1. `AppApplication` 调用 `MigrationOrchestrator.ensureUpToDate`
2. `MySQLiteOpenHelper.onUpgrade` 触发 `GreenDaoUpgrade`
3. 结果写入 `SCHEMA_HISTORY`，日志输出到 `migration.log`

## 最佳实践

- 版本号只能上升，且需配套迁移脚本
- 优先使用增量迁移，减少全表重建
- 所有 Dao 必须由 `EntityRegistrationHelper` 统一管理
- 迁移前后务必运行回归测试（含数据库读写）

## 注意事项

- 不修改 `DaoMaster` 等生成文件
- 复杂迁移前备份用户数据或提供回滚方案
- 观察 `SchemaHistoryRepository` 结果以确认升级成功

## 常见问题

### “no such column”

原因：实体字段与表结构不匹配。

处理：提升版本号并在 `migrateByVersion` 里补列，或依赖 `AutoMigrationHelper`。

```java
public void migrateByVersion(Database db, int oldVersion, int newVersion) {
  if (oldVersion < 2 && newVersion >= 2) {
    Class<? extends AbstractDao<?, ?>>[] allDaos = EntityRegistrationHelper.getAllDaos();
    AutoMigrationHelper.autoMigrateAllTables(db, allDaos);
  }
}
```

更多细节见 [数据库迁移功能实现指南](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/DatabaseMigrationImplementationGuide.md)。
