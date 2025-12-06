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

/**
 * 自动迁移助手类
 * 提供类似ORM框架的自动迁移功能
 * 能够自动比较实体类定义和数据库表结构，自动生成迁移脚本
 */
public class AutoMigrationHelper {
    private static final String TAG = "AutoMigrationHelper";
    
    /**
     * 自动迁移表结构
     * 比较实体类定义和数据库表结构，自动添加缺失的列
     * 
     * @param db 数据库对象
     * @param daoClass Dao类
     */
    public static void autoMigrateTable(Database db, Class<? extends AbstractDao<?, ?>> daoClass) {
        try {
            // 获取Dao配置
            DaoConfig daoConfig = new DaoConfig(db, daoClass);
            
            // 获取表名
            String tableName = daoConfig.tablename;
            
            // 检查表是否存在
            if (!isTableExists(db, tableName)) {
                // 表不存在，直接创建
                createTable(db, daoClass);
                Log.i(TAG, "Table " + tableName + " does not exist, created directly");
                return;
            }
            
            // 获取数据库中现有的列
            List<String> existingColumns = getColumns(db, tableName);
            
            // 获取实体类定义的列
            List<ColumnInfo> entityColumns = getEntityColumns(daoConfig);
            
            // 比较并添加缺失的列
            for (ColumnInfo columnInfo : entityColumns) {
                if (!existingColumns.contains(columnInfo.columnName)) {
                    // 添加缺失的列
                    addColumn(db, tableName, columnInfo);
                    Log.i(TAG, "Added missing column " + columnInfo.columnName + " to table " + tableName);
                }
            }
            
            Log.i(TAG, "Auto migration completed for table " + tableName);
        } catch (Exception e) {
            Log.e(TAG, "Auto migration failed for " + daoClass.getSimpleName(), e);
        }
    }
    
    /**
     * 批量自动迁移所有表
     * 
     * @param db 数据库对象
     * @param daoClasses Dao类数组
     */
    @SafeVarargs
    public static void autoMigrateAllTables(Database db, Class<? extends AbstractDao<?, ?>>... daoClasses) {
        Log.i(TAG, "Starting auto migration for " + daoClasses.length + " tables");
        for (Class<? extends AbstractDao<?, ?>> daoClass : daoClasses) {
            autoMigrateTable(db, daoClass);
        }
        Log.i(TAG, "Auto migration completed for all tables");
    }
    
    /**
     * 列信息类
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
     * 检查表是否存在
     */
    private static boolean isTableExists(Database db, String tableName) {
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }
    
    /**
     * 获取表的所有列名
     */
    private static List<String> getColumns(Database db, String tableName) {
        List<String> columns = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + tableName + " limit 0", null);
            if (cursor != null) {
                columns = new ArrayList<>(Arrays.asList(cursor.getColumnNames()));
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return columns;
    }
    
    /**
     * 获取实体类定义的列信息
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
     * 根据Java类型获取SQLite类型
     */
    private static String getTypeByClass(Class<?> type) {
        if (type.equals(String.class)) {
            return "TEXT";
        }
        if (type.equals(Long.class) || type.equals(Integer.class) || type.equals(long.class) || type.equals(int.class)) {
            return "INTEGER";
        }
        if (type.equals(Boolean.class) || type.equals(boolean.class)) {
            return "BOOLEAN";
        }
        if (type.equals(Double.class) || type.equals(Float.class) || type.equals(double.class) || type.equals(float.class)) {
            return "REAL";
        }
        
        // 默认返回TEXT类型
        return "TEXT";
    }
    
    /**
     * 创建表
     */
    private static void createTable(Database db, Class<? extends AbstractDao<?, ?>> daoClass) {
        try {
            java.lang.reflect.Method createTableMethod = daoClass.getDeclaredMethod("createTable", Database.class, boolean.class);
            createTableMethod.invoke(null, db, false);
        } catch (Exception e) {
            Log.e(TAG, "Failed to create table for " + daoClass.getSimpleName(), e);
        }
    }
    
    /**
     * 添加列
     */
    private static void addColumn(Database db, String tableName, ColumnInfo columnInfo) {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnInfo.columnName + " " + columnInfo.columnType;
        // 注意：SQLite中不能通过ALTER TABLE语句添加PRIMARY KEY约束
        // PRIMARY KEY应该在创建表时定义
        db.execSQL(sql);
    }
}