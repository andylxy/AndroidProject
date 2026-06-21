package run.yigou.gxzy.data.local.helper;

import android.database.Cursor;
import android.text.TextUtils;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.internal.DaoConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.data.local.gen.DaoMaster;
import run.yigou.gxzy.log.EasyLog;

/**
 * GreenDao数据库升级工具类（合并版）
 *
 * 整合了原 AutoMigrationHelper、MigrationHelper 的全部功能：
 * - 安全数据迁移（临时表备份/恢复）
 * - 自动表结构比较与补列
 * - 智能迁移（区分新表/已存在表）
 * - 临时表创建与恢复
 * - 列管理（检查、添加）
 *
 * @see EntityRegistrationHelper Dao注册中心
 * @see DatabaseVersionManager 版本号管理
 * @since 合并自 GreenDaoUpgrade + AutoMigrationHelper + MigrationHelper
 */
public class GreenDaoUpgrade {
    private static final String TAG = "GreenDaoUpgrade";
    private static GreenDaoUpgrade instance;

    public static GreenDaoUpgrade getInstance() {
        if (instance == null) {
            instance = new GreenDaoUpgrade();
        }
        return instance;
    }

    // ==================== 列信息查询工具 ====================

    /**
     * 获取表的所有列名
     */
    public static List<String> getColumns(Database db, String tableName) {
        List<String> columns = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null)) {
            if (cursor != null) {
                columns = new ArrayList<>(Arrays.asList(cursor.getColumnNames()));
            }
        } catch (Exception e) {
            EasyLog.print(TAG, "获取表列名失败 [" + tableName + "]: " + e.getMessage());
        }
        return columns;
    }

    /**
     * 检查表是否存在
     */
    public static boolean isTableExists(Database db, String tableName) {
        Cursor cursor = db.rawQuery(
            "SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = '" + tableName + "'", null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 根据Java类型获取SQLite列类型
     * 支持 String、Long/Integer、Boolean、Double/Float
     */
    public static String getTypeByClass(Class<?> type) {
        if (type.equals(String.class)) {
            return "TEXT";
        }
        if (type.equals(Long.class) || type.equals(Integer.class)
                || type.equals(long.class) || type.equals(int.class)) {
            return "INTEGER";
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return "BOOLEAN";
        }
        if (type.equals(Double.class) || type.equals(Float.class)
                || type.equals(double.class) || type.equals(float.class)) {
            return "REAL";
        }
        // 未知类型默认 TEXT
        EasyLog.print(TAG, "未识别的列类型 " + type + "，默认使用 TEXT");
        return "TEXT";
    }

    /**
     * 安全地为表添加列（如果列不存在）
     */
    public static void addColumnIfNotExists(Database db, String tableName,
                                             String columnName, String columnType) {
        List<String> columns = getColumns(db, tableName);
        if (!columns.contains(columnName)) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType;
            db.execSQL(sql);
            EasyLog.print(TAG, "已添加列 " + columnName + " 到表 " + tableName);
        }
    }

    /**
     * 为表添加列（不检查是否已存在）
     */
    public static void addColumn(Database db, String tableName,
                                  String columnName, String type) {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + type;
        db.execSQL(sql);
        EasyLog.print(TAG, "已添加列 " + columnName + " 到表 " + tableName);
    }

    // ==================== 自动迁移（来自 AutoMigrationHelper） ====================

    /**
     * 自动迁移单个表：比较实体类定义和数据库表结构，自动添加缺失的列
     *
     * @param db        数据库对象
     * @param daoClass  Dao类
     */
    public static void autoMigrateTable(Database db, Class<? extends AbstractDao<?, ?>> daoClass) {
        try {
            DaoConfig daoConfig = new DaoConfig(db, daoClass);
            String tableName = daoConfig.tablename;

            if (!isTableExists(db, tableName)) {
                createTableByReflection(db, daoClass);
                EasyLog.print(TAG, "表 " + tableName + " 不存在，已自动创建");
                return;
            }

            List<String> existingColumns = getColumns(db, tableName);
            List<ColumnInfo> entityColumns = getEntityColumns(daoConfig);

            for (ColumnInfo columnInfo : entityColumns) {
                if (!existingColumns.contains(columnInfo.columnName)) {
                    addColumn(db, tableName, columnInfo.columnName, columnInfo.columnType);
                    EasyLog.print(TAG, "已补列 " + columnInfo.columnName + " 到表 " + tableName);
                }
            }

            EasyLog.print(TAG, "自动迁移完成: " + tableName);
        } catch (Exception e) {
            EasyLog.print(TAG, "自动迁移失败: " + daoClass.getSimpleName() + " - " + e.getMessage());
        }
    }

    /**
     * 批量自动迁移所有表
     *
     * @param db         数据库对象
     * @param daoClasses Dao类数组
     */
    @SafeVarargs
    public static void autoMigrateAllTables(Database db,
                                             Class<? extends AbstractDao<?, ?>>... daoClasses) {
        EasyLog.print(TAG, "开始自动迁移 " + daoClasses.length + " 个表");
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            autoMigrateTable(db, daoClass);
        }
        EasyLog.print(TAG, "全部自动迁移完成");
    }

    /**
     * 列信息
     */
    private static class ColumnInfo {
        String columnName;
        String columnType;
        boolean isPrimaryKey;

        ColumnInfo(String columnName, String columnType, boolean isPrimaryKey) {
            this.columnName = columnName;
            this.columnType = columnType;
            this.isPrimaryKey = isPrimaryKey;
        }
    }

    /**
     * 从 DaoConfig 获取实体类定义的列信息
     */
    private static List<ColumnInfo> getEntityColumns(DaoConfig daoConfig) {
        List<ColumnInfo> columns = new ArrayList<>();
        for (int i = 0; i < daoConfig.properties.length; i++) {
            org.greenrobot.greendao.Property property = daoConfig.properties[i];
            String columnName = property.columnName;
            String columnType = getTypeByClass(property.type);
            boolean isPrimaryKey = property.primaryKey;
            columns.add(new ColumnInfo(columnName, columnType, isPrimaryKey));
        }
        return columns;
    }

    /**
     * 通过反射调用 Dao 类的 createTable 方法创建表
     */
    private static void createTableByReflection(Database db,
                                                 Class<? extends AbstractDao<?, ?>> daoClass) {
        try {
            java.lang.reflect.Method createTableMethod =
                daoClass.getDeclaredMethod("createTable", Database.class, boolean.class);
            createTableMethod.invoke(null, db, false);
        } catch (Exception e) {
            EasyLog.print(TAG, "反射创建表失败: " + daoClass.getSimpleName() + " - " + e.getMessage());
        }
    }

    // ==================== 临时表操作（来自 MigrationHelper） ====================

    /**
     * 创建临时表用于数据备份（全列使用 TEXT 类型）
     *
     * @param db       数据库对象
     * @param daoClass Dao类
     */
    public static void createTempTable(Database db, Class<? extends AbstractDao<?, ?>> daoClass) {
        DaoConfig daoConfig = new DaoConfig(db, daoClass);
        String tableName = daoConfig.tablename;
        String tempTableName = tableName.concat("_TEMP");

        // 删除可能存在的旧临时表
        db.execSQL("DROP TABLE IF EXISTS " + tempTableName);

        // 创建临时表（所有列使用 TEXT 类型）
        List<String> columns = getColumns(db, tableName);
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("CREATE TABLE ").append(tempTableName).append(" (");
        String divider = "";
        for (String column : columns) {
            createTableSql.append(divider).append("\"").append(column).append("\"").append(" TEXT");
            divider = ",";
        }
        createTableSql.append(");");
        db.execSQL(createTableSql.toString());

        // 复制数据到临时表
        String insertSql = "INSERT INTO " + tempTableName + " SELECT * FROM " + tableName + ";";
        db.execSQL(insertSql);
    }

    /**
     * 从临时表恢复数据到新表（处理列增减）
     *
     * @param db       数据库对象
     * @param daoClass Dao类
     */
    public static void restoreDataFromTempTable(Database db,
                                                 Class<? extends AbstractDao<?, ?>> daoClass) {
        DaoConfig daoConfig = new DaoConfig(db, daoClass);
        String tableName = daoConfig.tablename;
        String tempTableName = tableName.concat("_TEMP");

        // 获取新表和临时表的列
        List<String> newTableColumns = new ArrayList<>();
        for (int i = 0; i < daoConfig.properties.length; i++) {
            newTableColumns.add(daoConfig.properties[i].columnName);
        }
        List<String> tempTableColumns = getColumns(db, tempTableName);

        // 构建列映射
        List<String> intersectionColumns = new ArrayList<>();
        List<String> newColumnsWithDefaults = new ArrayList<>();

        for (String column : newTableColumns) {
            if (tempTableColumns.contains(column)) {
                intersectionColumns.add(column);
            } else {
                newColumnsWithDefaults.add("0 AS " + column);
            }
        }

        // 构建 INSERT 语句
        StringBuilder insertSql = new StringBuilder();
        insertSql.append("INSERT INTO ").append(tableName).append(" (");
        insertSql.append(TextUtils.join(",", intersectionColumns));
        if (!newColumnsWithDefaults.isEmpty()) {
            insertSql.append(",").append(TextUtils.join(",", newColumnsWithDefaults));
        }
        insertSql.append(") SELECT ");
        insertSql.append(TextUtils.join(",", intersectionColumns));
        if (!newColumnsWithDefaults.isEmpty()) {
            insertSql.append(",0");
        }
        insertSql.append(" FROM ").append(tempTableName).append(";");

        db.execSQL(insertSql.toString());
        db.execSQL("DROP TABLE " + tempTableName);
    }

    // ==================== 核心迁移方法 ====================

    /**
     * 默认迁移方法 - 保存旧数据并重新创建表结构
     * 适用于添加新表或不关心旧数据的情况
     */
    @SafeVarargs
    public final void migrate(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        generateTempTables(db, daoClasses);
        DaoMaster.dropAllTables(db, true);
        DaoMaster.createAllTables(db, false);
        restoreData(db, daoClasses);
    }

    /**
     * 智能迁移方法 - 根据表的存在情况决定是创建新表还是迁移现有表
     */
    @SafeVarargs
    public final void smartMigrate(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        List<Class<? extends AbstractDao<?, ?>>> newTables = new ArrayList<>();
        List<Class<? extends AbstractDao<?, ?>>> existingTables = new ArrayList<>();

        // 分离新表和已存在的表
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            try {
                java.lang.reflect.Field tableField = daoClass.getDeclaredField("TABLENAME");
                tableField.setAccessible(true);
                String tableName = (String) tableField.get(null);

                if (isTableExists(db, tableName)) {
                    existingTables.add(daoClass);
                } else {
                    newTables.add(daoClass);
                }
            } catch (Exception e) {
                EasyLog.print(TAG, "检查表存在失败: " + daoClass.getSimpleName() + " - " + e.getMessage());
                // 出错时保守处理，加入existingTables
                existingTables.add(daoClass);
            }
        }

        // 为新表创建表结构
        for (Class<? extends AbstractDao<?, ?>> daoClass : newTables) {
            createTableByReflection(db, daoClass);
            EasyLog.print(TAG, "已创建新表: " + daoClass.getSimpleName());
        }

        // 为已存在的表进行数据迁移
        if (!existingTables.isEmpty()) {
            Class<? extends AbstractDao<?, ?>>[] existingArray =
                existingTables.toArray(new Class[0]);
            generateTempTables(db, existingArray);

            // 只删除已存在表，不删除新表
            for (Class<? extends AbstractDao<?, ?>> daoClass : existingTables) {
                try {
                    java.lang.reflect.Method dropTableMethod =
                        daoClass.getDeclaredMethod("dropTable", Database.class, boolean.class);
                    dropTableMethod.invoke(null, db, true);
                } catch (Exception e) {
                    EasyLog.print(TAG, "删除旧表失败: " + daoClass.getSimpleName() + " - " + e.getMessage());
                }
            }
            DaoMaster.createAllTables(db, false);
            restoreData(db, existingArray);
            EasyLog.print(TAG, "已迁移 " + existingTables.size() + " 个已存在的表");
        }
    }

    // ==================== 内部辅助方法 ====================

    @SafeVarargs
    private final void generateTempTables(Database db,
                                           Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            DaoConfig daoConfig = new DaoConfig(db, daoClass);

            String divider = "";
            String tableName = daoConfig.tablename;
            String tempTableName = tableName.concat("_TEMP");
            ArrayList<String> properties = new ArrayList<>();

            StringBuilder createTableStringBuilder = new StringBuilder();
            createTableStringBuilder.append("CREATE TABLE ").append(tempTableName).append(" (");

            List<String> originalColumns = getColumns(db, tableName);
            for (int j = 0; j < daoConfig.properties.length; j++) {
                String columnName = daoConfig.properties[j].columnName;

                if (originalColumns.contains(columnName)) {
                    properties.add(columnName);
                    String type = getTypeByClass(daoConfig.properties[j].type);

                    createTableStringBuilder.append(divider)
                        .append(quoteColumn(columnName)).append(" ").append(type);

                    if (daoConfig.properties[j].primaryKey) {
                        createTableStringBuilder.append(" PRIMARY KEY");
                    }

                    divider = ",";
                }
            }
            createTableStringBuilder.append(");");
            db.execSQL(createTableStringBuilder.toString());

            String wrappedColumns = TextUtils.join(",", quoteColumns(properties));
            String insertSql = "INSERT INTO " + tempTableName + " (" +
                wrappedColumns + ") SELECT " + wrappedColumns + " FROM " + tableName + ";";
            db.execSQL(insertSql);
        }
    }

    @SafeVarargs
    private final void restoreData(Database db,
                                    Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            DaoConfig daoConfig = new DaoConfig(db, daoClass);

            String tableName = daoConfig.tablename;
            String tempTableName = tableName.concat("_TEMP");
            ArrayList<String> properties = new ArrayList<>();
            ArrayList<String> selectClauses = new ArrayList<>();
            List<String> tempColumns = getColumns(db, tempTableName);

            for (int j = 0; j < daoConfig.properties.length; j++) {
                String columnName = daoConfig.properties[j].columnName;

                if (tempColumns.contains(columnName)) {
                    properties.add(columnName);
                    selectClauses.add(quoteColumn(columnName));
                } else {
                    String type = getTypeByClass(daoConfig.properties[j].type);
                    if ("INTEGER".equals(type)) {
                        selectClauses.add("0 AS " + quoteColumn(columnName));
                        properties.add(columnName);
                    } else if ("REAL".equals(type)) {
                        selectClauses.add("0.0 AS " + quoteColumn(columnName));
                        properties.add(columnName);
                    } else if ("BOOLEAN".equals(type)) {
                        selectClauses.add("0 AS " + quoteColumn(columnName));
                        properties.add(columnName);
                    }
                    // TEXT 类型新增列使用 NULL 默认值，不需要特殊处理
                }
            }

            String insertColumns = TextUtils.join(",", quoteColumns(properties));
            String selectColumns = TextUtils.join(",", selectClauses);
            String insertSql = "INSERT INTO " + tableName + " (" +
                insertColumns + ") SELECT " + selectColumns + " FROM " + tempTableName + ";";
            db.execSQL(insertSql);
            db.execSQL("DROP TABLE " + tempTableName);
        }
    }

    private String quoteColumn(String columnName) {
        return "\"" + columnName + "\"";
    }

    private List<String> quoteColumns(List<String> columns) {
        List<String> quoted = new ArrayList<>(columns.size());
        for (String column : columns) {
            quoted.add(quoteColumn(column));
        }
        return quoted;
    }
}
