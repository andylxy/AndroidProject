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
 * 数据库迁移助手类
 * 专门处理数据库表结构变更时的数据迁移
 * 
 * 使用说明：
 * 1. 当需要修改现有表结构时，在此类中添加相应的迁移方法
 * 2. 在MySQLiteOpenHelper的onUpgrade方法中调用相应的迁移方法
 * 3. 确保迁移过程中数据不会丢失
 */
public class MigrationHelper {

    private static final String CONVERSION_CLASS_NOT_FOUND_EXCEPTION = "MIGRATION HELPER - CLASS DOESN'T MATCH WITH THE CURRENT PARAMETERS";
    private static final String TAG = "MigrationHelper";

    /**
     * 获取表的所有列名
     *
     * @param db        数据库对象
     * @param tableName 表名
     * @return 列名列表
     */
    private static List<String> getColumns(Database db, String tableName) {
        List<String> columns = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + tableName + " limit 0", null);
            if (null != cursor && cursor.getColumnCount() > 0) {
                columns = Arrays.asList(cursor.getColumnNames());
            }
        } catch (Exception e) {
            Log.v(tableName, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return columns;
    }

    /**
     * 为表添加新列
     *
     * @param db         数据库对象
     * @param tableName  表名
     * @param columnName 列名
     * @param type       列类型
     */
    public static void addColumn(Database db, String tableName, String columnName, String type) {
        String sql = "ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + type;
        db.execSQL(sql);
        Log.i(TAG, "Added column " + columnName + " to table " + tableName);
    }

    /**
     * 检查表是否存在
     *
     * @param db        数据库对象
     * @param tableName 表名
     * @return 如果表存在返回true，否则返回false
     */
    public static boolean isTableExists(Database db, String tableName) {
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        boolean exists = cursor.getCount() > 0;
        cursor.close();
        return exists;
    }

    /**
     * 安全地为表添加列（如果列不存在）
     *
     * @param db         数据库对象
     * @param tableName  表名
     * @param columnName 列名
     * @param type       列类型
     */
    public static void addColumnIfNotExists(Database db, String tableName, String columnName, String type) {
        List<String> columns = getColumns(db, tableName);
        if (!columns.contains(columnName)) {
            addColumn(db, tableName, columnName, type);
        } else {
            Log.i(TAG, "Column " + columnName + " already exists in table " + tableName);
        }
    }

    /**
     * 创建临时表用于数据备份
     *
     * @param db       数据库对象
     * @param daoClass Dao类
     */
    public static void createTempTable(Database db, Class<? extends AbstractDao<?, ?>> daoClass) {
        DaoConfig daoConfig = new DaoConfig(db, daoClass);
        String tableName = daoConfig.tablename;
        String tempTableName = daoConfig.tablename.concat("_TEMP");

        // 删除可能存在的临时表
        db.execSQL("DROP TABLE IF EXISTS " + tempTableName);

        // 创建临时表
        StringBuilder createTableSql = new StringBuilder();
        createTableSql.append("CREATE TABLE ").append(tempTableName).append(" (");

        List<String> columns = getColumns(db, tableName);
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
     * 从临时表恢复数据到新表
     *
     * @param db       数据库对象
     * @param daoClass Dao类
     */
    public static void restoreDataFromTempTable(Database db, Class<? extends AbstractDao<?, ?>> daoClass) {
        DaoConfig daoConfig = new DaoConfig(db, daoClass);
        String tableName = daoConfig.tablename;
        String tempTableName = daoConfig.tablename.concat("_TEMP");

        // 获取新表和临时表的列
        List<String> newTableColumns = getNewTableColumns(daoConfig);
        List<String> tempTableColumns = getColumns(db, tempTableName);

        // 构建列映射
        List<String> intersectionColumns = new ArrayList<>();
        List<String> newColumnsWithDefaults = new ArrayList<>();

        for (String column : newTableColumns) {
            if (tempTableColumns.contains(column)) {
                intersectionColumns.add(column);
            } else {
                // 新增的列使用默认值
                newColumnsWithDefaults.add(getDefaultColumnValue(column, daoConfig));
            }
        }

        // 构建INSERT语句
        StringBuilder insertSql = new StringBuilder();
        insertSql.append("INSERT INTO ").append(tableName).append(" (");
        insertSql.append(TextUtils.join(",", intersectionColumns));
        if (!newColumnsWithDefaults.isEmpty()) {
            insertSql.append(",").append(TextUtils.join(",", newColumnsWithDefaults));
        }
        insertSql.append(") SELECT ");
        insertSql.append(TextUtils.join(",", intersectionColumns));
        if (!newColumnsWithDefaults.isEmpty()) {
            insertSql.append(",0"); // 默认值为0
        }
        insertSql.append(" FROM ").append(tempTableName).append(";");

        db.execSQL(insertSql.toString());

        // 删除临时表
        db.execSQL("DROP TABLE " + tempTableName);
    }

    /**
     * 获取新表的列定义
     *
     * @param daoConfig Dao配置
     * @return 列名列表
     */
    private static List<String> getNewTableColumns(DaoConfig daoConfig) {
        List<String> columns = new ArrayList<>();
        for (int i = 0; i < daoConfig.properties.length; i++) {
            columns.add(daoConfig.properties[i].columnName);
        }
        return columns;
    }

    /**
     * 获取列的默认值
     *
     * @param columnName 列名
     * @param daoConfig  Dao配置
     * @return 默认值表达式
     */
    private static String getDefaultColumnValue(String columnName, DaoConfig daoConfig) {
        // 根据实际情况返回合适的默认值
        return "0 AS " + columnName; // 默认返回0作为数值类型的默认值
    }
}