package run.yigou.gxzy.greendao.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;


import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.QueryBuilder;

/**
 * 自定义数据库OpenHelper
 * 处理数据库创建和版本升级
 * 
 * 数据库升级策略：
 * 1. 默认情况下使用安全的数据迁移方式，保留所有现有数据
 * 2. 当添加新表时，使用增量升级方式只创建新表
 * 3. 当修改现有表结构时，需要实现特定的迁移逻辑
 */
public class MySQLiteOpenHelper extends VersionedOpenHelper {

    private Context mContext;


    public MySQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,boolean logDebug) {
        super(context, name, factory);
        mContext = context;
        // 启用 SQL 日志
        enableSqlLogging(logDebug);

    }
    
    private void enableSqlLogging( boolean logDebug ) {
        // 启用 SQL 日志
        QueryBuilder.LOG_SQL = logDebug;      // 打印 SQL 查询
        QueryBuilder.LOG_VALUES = logDebug;   // 打印 SQL 参数值
    }

    @Override
    public void onCreate(Database db) {
        super.onCreate(db);
        SchemaHistoryRepository.ensureTable(db);
        SchemaHistoryRepository.recordUpgrade(db, 0, DatabaseVersionManager.getCurrentVersion(), "create", "Initial create");
    }
    
    /**
     * 数据库升级方法
     * 根据不同版本差异执行相应的升级逻辑
     * 
     * @param db 数据库对象
     * @param oldVersion 旧版本号
     * @param newVersion 新版本号
     */
    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        SchemaHistoryRepository.ensureTable(db);
        GreenDaoUpgrade upgradeHelper = GreenDaoUpgrade.getInstance();
        try {
            upgradeHelper.migrateByVersion(db, oldVersion, newVersion);
            Class<? extends AbstractDao<?, ?>>[] allDaos = EntityRegistrationHelper.getAllDaos();
            upgradeHelper.smartMigrate(db, allDaos);
            SchemaHistoryRepository.recordUpgrade(db, oldVersion, newVersion, "success", "smartMigrate");
        } catch (Exception upgradeError) {
            SchemaHistoryRepository.recordUpgrade(db, oldVersion, newVersion, "failed", upgradeError.getMessage());
            throw upgradeError;
        }
    }
    
    /**
     * 获取当前数据库版本号
     * @return 当前数据库版本号
     */
    public static int getDatabaseVersion() {
        return DatabaseVersionManager.getCurrentVersion();
    }
}