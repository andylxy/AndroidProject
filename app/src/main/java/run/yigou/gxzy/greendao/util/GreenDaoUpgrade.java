package run.yigou.gxzy.greendao.util;

import android.database.Cursor;
import android.text.TextUtils;
import android.util.Log;


import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.internal.DaoConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import run.yigou.gxzy.greendao.gen.DaoMaster;

/**
 * GreenDao数据库升级工具类
 * 提供安全的数据库迁移功能，避免数据丢失
 * 
 * 使用说明：
 * 1. 当添加新的实体类时，在onUpgrade方法中调用migrate方法并传入新的Dao类
 * 2. 当修改现有实体结构时，需要编写特定的迁移逻辑
 * 3. 始终保证向后兼容性
 */
public class GreenDaoUpgrade {
    private static final String CONVERSION_CLASS_NOT_FOUND_EXCEPTION = "MIGRATION HELPER - CLASS DOESN'T MATCH WITH THE CURRENT PARAMETERS";
    private static GreenDaoUpgrade instance;

    public static GreenDaoUpgrade getInstance() {
        if (instance == null) {
            instance = new GreenDaoUpgrade();
        }
        return instance;
    }

    private static List<String> getColumns(Database db, String tableName) {
        List<String> columns = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("SELECT * FROM " + tableName + " limit 1", null)) {
            if (cursor != null) {
                columns = new ArrayList<>(Arrays.asList(cursor.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        }
        return columns;
    }

    /**
     * 默认迁移方法 - 保存旧数据并重新创建表结构
     * 适用于添加新表或不关心旧数据的情况
     * 
     * @param db 数据库对象
     * @param daoClasses 需要迁移的Dao类列表
     */
    @SafeVarargs
    public final void migrate(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        generateTempTables(db, daoClasses);
        DaoMaster.dropAllTables(db, true);
        DaoMaster.createAllTables(db, false);
        restoreData(db, daoClasses);
    }

    /**
     * 增量迁移方法 - 只创建新表，保留所有现有数据
     * 适用于仅添加新实体类的情况
     * 
     * @param db 数据库对象
     * @param daoClasses 新增的Dao类列表
     */
    @SafeVarargs
    public final void migrateToAddNewTables(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        // 只创建新表，不删除已有表
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            try {
                // 通过反射获取Dao类中的createTable方法
                java.lang.reflect.Method createTableMethod = daoClass.getDeclaredMethod("createTable", Database.class, boolean.class);
                createTableMethod.invoke(null, db, false);
            } catch (Exception e) {
                Log.e("GreenDaoUpgrade", "Failed to create table for " + daoClass.getSimpleName(), e);
            }
        }
    }

    /**
     * 特定版本迁移方法
     * 适用于需要特殊处理的版本升级
     * 
     * 示例使用场景：
     * 1. 修改现有表结构（添加/删除字段）
     * 2. 迁移数据格式
     * 3. 复杂的数据转换逻辑
     * 
     * @param db 数据库对象
     * @param oldVersion 旧版本号
     * @param newVersion 新版本号
     */
    public void migrateByVersion(Database db, int oldVersion, int newVersion) {
        // 根据不同的版本差异执行不同的迁移逻辑
        // 当从版本1升级到版本2时，自动迁移所有表结构
        if (oldVersion < 2 && newVersion >= 2) {
            // 使用自动迁移功能处理所有表
            Class<? extends AbstractDao<?, ?>>[] allDaos = EntityRegistrationHelper.getAllDaos();
            AutoMigrationHelper.autoMigrateAllTables(db, allDaos);
        }
        
        // 可以继续添加其他版本的迁移逻辑
        // 例如从版本2升级到版本3:
        // if (oldVersion < 3 && newVersion >= 3) {
        //     // 添加版本3的迁移逻辑
        // }
    }

    /**
     * 检查表是否存在
     * 
     * @param db 数据库对象
     * @param tableName 表名
     * @return 如果表存在返回true，否则返回false
     */
    private boolean isTableExists(Database db, String tableName) {
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 为指定表添加新列（如果列不存在）
     * 
     * @param db 数据库对象
     * @param tableName 表名
     * @param columnName 列名
     * @param columnType 列类型（如TEXT, INTEGER等）
     */
    private void addColumnIfNotExists(Database db, String tableName, String columnName, String columnType) {
        // 检查列是否已存在，避免重复添加
        List<String> columns = getColumns(db, tableName);
        if (!columns.contains(columnName)) {
            String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + columnType;
            db.execSQL(sql);
            Log.i("GreenDaoUpgrade", "Added column " + columnName + " to table " + tableName);
        }
    }

    /**
     * 智能迁移方法 - 根据表的存在情况决定是创建新表还是迁移现有表
     * 
     * @param db 数据库对象
     * @param daoClasses 所有Dao类列表
     */
    @SafeVarargs
    public final void smartMigrate(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        List<Class<? extends AbstractDao<?, ?>>> newTables = new ArrayList<>();
        List<Class<? extends AbstractDao<?, ?>>> existingTables = new ArrayList<>();
        
        // 分离新表和已存在的表
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            try {
                // 获取表名
                java.lang.reflect.Field tableField = daoClass.getDeclaredField("TABLENAME");
                tableField.setAccessible(true);
                String tableName = (String) tableField.get(null);
                
                if (isTableExists(db, tableName)) {
                    existingTables.add(daoClass);
                } else {
                    newTables.add(daoClass);
                }
            } catch (Exception e) {
                Log.e("GreenDaoUpgrade", "Error checking table existence for " + daoClass.getSimpleName(), e);
                // 出错时保守处理，加入existingTables
                existingTables.add(daoClass);
            }
        }
        
        // 为新表创建表结构
        for (Class<? extends AbstractDao<?, ?>> daoClass : newTables) {
            try {
                java.lang.reflect.Method createTableMethod = daoClass.getDeclaredMethod("createTable", Database.class, boolean.class);
                createTableMethod.invoke(null, db, false);
                Log.i("GreenDaoUpgrade", "Created new table for " + daoClass.getSimpleName());
            } catch (Exception e) {
                Log.e("GreenDaoUpgrade", "Failed to create table for " + daoClass.getSimpleName(), e);
            }
        }
        
        // 为已存在的表进行数据迁移
        if (!existingTables.isEmpty()) {
            Class<? extends AbstractDao<?, ?>>[] existingArray = new Class[existingTables.size()];
            existingArray = existingTables.toArray(existingArray);
            generateTempTables(db, existingArray);
            // 只删除已存在表，不删除新表
            for (Class<? extends AbstractDao<?, ?>> daoClass : existingTables) {
                try {
                    java.lang.reflect.Field tableField = daoClass.getDeclaredField("TABLENAME");
                    tableField.setAccessible(true);
                    String tableName = (String) tableField.get(null);
                    
                    java.lang.reflect.Method dropTableMethod = daoClass.getDeclaredMethod("dropTable", Database.class, boolean.class);
                    dropTableMethod.invoke(null, db, true);
                } catch (Exception e) {
                    Log.e("GreenDaoUpgrade", "Failed to drop table for " + daoClass.getSimpleName(), e);
                }
            }
            DaoMaster.createAllTables(db, false);
            restoreData(db, existingArray);
            Log.i("GreenDaoUpgrade", "Migrated existing tables: " + existingTables.size());
        }
    }

    @SafeVarargs
    private final void generateTempTables(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            DaoConfig daoConfig = new DaoConfig(db, daoClass);

            String divider = "";
            String tableName = daoConfig.tablename;
            String tempTableName = daoConfig.tablename.concat("_TEMP");
            ArrayList<String> properties = new ArrayList<>();

            StringBuilder createTableStringBuilder = new StringBuilder();

            createTableStringBuilder.append("CREATE TABLE ").append(tempTableName).append(" (");

            for (int j = 0; j < daoConfig.properties.length; j++) {
                String columnName = daoConfig.properties[j].columnName;

                if (getColumns(db, tableName).contains(columnName)) {
                    properties.add(columnName);

                    String type = null;

                    try {
                        type = getTypeByClass(daoConfig.properties[j].type);
                    } catch (Exception exception) {
                        exception.printStackTrace();
                    }

                    createTableStringBuilder.append(divider).append(quoteColumn(columnName)).append(" ").append(type);

                    if (daoConfig.properties[j].primaryKey) {
                        createTableStringBuilder.append(" PRIMARY KEY");
                    }

                    divider = ",";
                }
            }
            createTableStringBuilder.append(");");

            db.execSQL(createTableStringBuilder.toString());

            String wrappedColumns = TextUtils.join(",", quoteColumns(properties));
            String insertTableStringBuilder = "INSERT INTO " + tempTableName + " (" +
                    wrappedColumns +
                    ") SELECT " +
                    wrappedColumns +
                    " FROM " + tableName + ";";
            db.execSQL(insertTableStringBuilder);
        }
    }

    @SafeVarargs
    private final void restoreData(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            DaoConfig daoConfig = new DaoConfig(db, daoClass);

            String tableName = daoConfig.tablename;
            String tempTableName = daoConfig.tablename.concat("_TEMP");
            ArrayList<String> properties = new ArrayList<>();
            ArrayList<String> selectClauses = new ArrayList<>();
            List<String> tempColumns = getColumns(db, tempTableName);
            for (int j = 0; j < daoConfig.properties.length; j++) {
                String columnName = daoConfig.properties[j].columnName;

                if (tempColumns.contains(columnName)) {
                    properties.add(columnName);
                    selectClauses.add(quoteColumn(columnName));
                } else {
                    try {
                        if (getTypeByClass(daoConfig.properties[j].type).equals("INTEGER")) {
                            selectClauses.add("0 AS " + quoteColumn(columnName));
                            properties.add(columnName);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            String insertColumns = TextUtils.join(",", quoteColumns(properties));
            String selectColumns = TextUtils.join(",", selectClauses);
            String insertTableStringBuilder = "INSERT INTO " + tableName + " (" +
                    insertColumns +
                    ") SELECT " +
                    selectColumns +
                    " FROM " + tempTableName + ";";
            db.execSQL(insertTableStringBuilder);
            db.execSQL("DROP TABLE " + tempTableName);
        }
    }

    private String getTypeByClass(Class<?> type) throws Exception {
        if (type.equals(String.class)) {
            return "TEXT";
        }
        if (type.equals(Long.class) || type.equals(Integer.class) || type.equals(long.class) || type.equals(int.class)) {
            return "INTEGER";
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return "BOOLEAN";
        }

        Exception exception = new Exception(CONVERSION_CLASS_NOT_FOUND_EXCEPTION.concat(" - Class: ").concat(type.toString()));
        exception.printStackTrace();
        throw exception;
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