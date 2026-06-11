# 数据库迁移功能实现指南

## 概述

本指南介绍 GreenDAO 在本项目中的完整迁移方案，确保结构变化时能够安全迁移并保留数据。

## 当前实现的功能

### 1. 版本管理

- 使用 [DatabaseVersionManager](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/DatabaseVersionManager.java#L8-L31) 集中维护版本号
- [VersionedOpenHelper](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/VersionedOpenHelper.java) 让我们脱离 `DaoMaster.SCHEMA_VERSION`
- [SchemaHistoryRepository](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/SchemaHistoryRepository.java) 记录升级日志，便于排查

### 2. 智能迁移机制

- [GreenDaoUpgrade](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/GreenDaoUpgrade.java#L25-L258) 提供 `smartMigrate`、`migrateByVersion`、`migrateToAddNewTables`
- [MigrationOrchestrator](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/MigrationOrchestrator.java) 在应用启动时强制执行升级并输出诊断信息

### 3. 自动迁移机制

- [AutoMigrationHelper](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/AutoMigrationHelper.java#L15-L192) 自动比对表结构、补列、批量迁移

### 4. 辅助工具类

- [MigrationHelper](file:///D:/git/app/AndroidProject/app/src/main/java/run/yigou/gxzy/greendao/util/MigrationHelper.java#L17-L210) 提供临时表、列检测等辅助能力

## 实际案例：处理 USER_INFO 缺失列

### 问题描述

访问 `USER_INFO.ACCESS_KEY_ID` 时抛出 “no such column”。

### 解决方案

1. 将 `DatabaseVersionManager.CURRENT_VERSION` 从 1 调整为 2
2. 在 `migrateByVersion` 中调用自动迁移

```java
public void migrateByVersion(Database db, int oldVersion, int newVersion) {
    if (oldVersion < 2 && newVersion >= 2) {
        Class<? extends AbstractDao<?, ?>>[] allDaos = EntityRegistrationHelper.getAllDaos();
        AutoMigrationHelper.autoMigrateAllTables(db, allDaos);
    }
}
```

## 新增迁移逻辑流程

### 1. 增加版本号

在 `DatabaseVersionManager` 中 bump 版本：

```java
public static final int CURRENT_VERSION = 3;
```

### 2. 添加迁移代码

在 `GreenDaoUpgrade.migrateByVersion` 中描述结构变化：

```java
public void migrateByVersion(Database db, int oldVersion, int newVersion) {
    if (oldVersion < 3 && newVersion >= 3) {
        addColumnIfNotExists(db, "USER_INFO", "CREATED_TIME", "INTEGER");
    }
}
```

### 3. 注册新实体

在 `EntityRegistrationHelper` 的版本映射里登记新 Dao，保证自动扫描得到：

```java
static {
    List<Class<? extends AbstractDao<?, ?>>> v3Entities = new ArrayList<>();
    v3Entities.add(run.yigou.gxzy.greendao.gen.NewEntityDao.class);
    VERSION_ENTITY_MAP.put(3, v3Entities);
}
```

## 复杂迁移场景

### 1. 表结构重大调整

```java
MigrationHelper.createTempTable(db, UserInfoDao.class);
UserInfoDao.dropTable(db, true);
UserInfoDao.createTable(db, false);
MigrationHelper.restoreDataFromTempTable(db, UserInfoDao.class);
```

### 2. 数据格式转换

```java
public void migrateByVersion(Database db, int oldVersion, int newVersion) {
    if (oldVersion < 3 && newVersion >= 3) {
        addColumnIfNotExists(db, "USER_INFO", "CREATED_TIME", "INTEGER");
        String updateSql = "UPDATE USER_INFO SET CREATED_TIME = " + System.currentTimeMillis() +
                " WHERE CREATED_TIME IS NULL";
        db.execSQL(updateSql);
    }
}
```

## 测试迁移

### 1. 单元测试

```java
@Test
public void testMigrationFromV1ToV2() {
    // 1. 创建 V1 schema
    // 2. 执行迁移
    // 3. 校验结构和数据
}
```

### 2. 手动测试

1. 安装旧版本，写入典型数据
2. 安装新版本触发升级
3. 验证 UI 和数据完整性

## 最佳实践

- 每次结构变更都要 bump 版本
- 优先使用增量迁移，谨慎重建
- 迁移逻辑集中在 `migrateByVersion`，并写清注释

## 常见问题

- **no such column**：确认版本已提升并触发自动迁移
- **数据丢失**：检查是否误用了全量 `migrate`
- **迁移失败**：查看 `migration.log` 以及 `SCHEMA_HISTORY` 中的状态描述
