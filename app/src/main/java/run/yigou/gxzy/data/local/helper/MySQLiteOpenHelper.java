package run.yigou.gxzy.data.local.helper;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;


import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.QueryBuilder;

import run.yigou.gxzy.log.EasyLog;

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

    private static final String TAG = "MySQLiteOpenHelper";

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
            // smartMigrate 统一处理：新表自动创建，已有表自动迁移结构
            Class<? extends AbstractDao<?, ?>>[] allDaos = EntityRegistrationHelper.getAllDaos();
            upgradeHelper.smartMigrate(db, allDaos);
            SchemaHistoryRepository.recordUpgrade(db, oldVersion, newVersion, "success", "smartMigrate");
        } catch (Exception upgradeError) {
            // 修补（2026-10-06）：这里原先在本连接上写 "failed" 记录，但那条记录是**无效的**——
            // onUpgrade 抛异常时 SQLiteOpenHelper 会回滚整个事务，刚写的记录被一起滚掉。
            // 实测证据：升级崩溃后 SCHEMA_HISTORY 里只有旧的 create 行，没有任何 failed。
            // 失败记录改由 MigrationOrchestrator 在"连接已关闭、事务已回滚"之后用独立连接补写；
            // 这里只把原因与堆栈打进日志，保留现场。
            EasyLog.print(TAG, "升级失败 old=" + oldVersion + " new=" + newVersion
                    + "：" + upgradeError.getMessage());
            EasyLog.print(upgradeError);
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