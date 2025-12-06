# GreenDAO 快速使用指南

下述说明全部基于当前已落地的改造能力：应用启动即由 `MigrationOrchestrator` 触发数据库升级、`SchemaHistoryRepository` 自动记录历史、`GreenDaoManager` 继续对外提供统一的 `DaoSession`。日常开发只需遵循以下步骤即可。

## 1. 初始化

- `AppApplication.onCreate` 会调用 `MigrationOrchestrator.ensureUpToDate`，在任何 DAO 被访问前自动创建或升级 SQLite 数据库。
- `GreenDaoManager.getInstance()` 暴露出来的 `DaoSession` 与改造前保持一致，业务模块无需做额外适配。

## 2. 新增实体

1. 使用现有脚本或命令（如 `./gradlew greendaoGenerate -Pentity=Foo`）生成实体与 Dao。
2. 运行 `./gradlew checkGreenDaoEntities`（已提供的校验任务），确保 Dao 已被自动注册。
3. 将 `DatabaseVersionManager.CURRENT_VERSION` 加一，以触发升级。
4. 若新表需要预置数据，可在迁移计划中补充描述。
5. 启动应用即可自动建表，无需手动执行 SQL。

## 3. 修改既有实体

1. 按需调整实体/Dao 字段并重新生成代码。
2. 同样提升 `DatabaseVersionManager.CURRENT_VERSION`。
3. 在对应迁移计划中描述需要新增的列、列重命名或默认值策略。
4. 业务 Service/DAO 接口保持不变，升级完成后即可继续使用。

## 4. 排障速查

- **未触发升级**：检查 `CURRENT_VERSION` 是否大于本地记录，可在 `SchemaHistoryRepository` 中查看最近一次执行结果。
- **缺少列**：确认迁移计划中已声明对应列，或依赖 `AutoMigrationHelper` 自动补列。
- **需要重跑迁移**：清除应用数据或再次提升版本号，冷启动时脚本会自动重跑。

建议将本指南与 `GreenDaoUpgradeGuide.md` 搭配使用，方便在迭代中快速回顾流程。
