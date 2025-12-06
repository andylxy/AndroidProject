package run.yigou.gxzy.greendao.util;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.greenrobot.greendao.database.Database;

import java.util.List;

/**
 * 迁移测试辅助类
 * 用于验证数据库迁移是否正确执行
 */
public class MigrationTestHelper {

    /**
     * 验证表是否存在
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
     * 验证表中是否存在指定列
     *
     * @param db         数据库对象
     * @param tableName  表名
     * @param columnName 列名
     * @return 如果列存在返回true，否则返回false
     */
    public static boolean isColumnExists(Database db, String tableName, String columnName) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null);
            int columnIndex = cursor.getColumnIndex(columnName);
            return columnIndex != -1;
        } catch (Exception e) {
            return false;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 获取表中的所有列名
     *
     * @param db        数据库对象
     * @param tableName 表名
     * @return 列名列表
     */
    public static String[] getColumnNames(Database db, String tableName) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT * FROM " + tableName + " LIMIT 0", null);
            return cursor.getColumnNames();
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * 验证表中的数据行数
     *
     * @param db        数据库对象
     * @param tableName 表名
     * @return 数据行数
     */
    public static int getRowCount(Database db, String tableName) {
        Cursor cursor = null;
        try {
            cursor = db.rawQuery("SELECT COUNT(*) FROM " + tableName, null);
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
            return 0;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}