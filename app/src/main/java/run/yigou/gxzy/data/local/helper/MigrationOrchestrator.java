package run.yigou.gxzy.data.local.helper;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.StandardDatabase;

import java.util.concurrent.atomic.AtomicBoolean;

import run.yigou.gxzy.base.constant.AppConst;
import run.yigou.gxzy.data.local.GreenDaoManager;
import run.yigou.gxzy.log.EasyLog;

/**
 * Central entry point that guarantees database initialization happens exactly once on app start.
 * It triggers GreenDaoManager creation, records basic diagnostics, and surfaces fatal issues early.
 */
public final class MigrationOrchestrator {

    private static final String TAG = "MigrationOrchestrator";
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    private MigrationOrchestrator() {
    }

    public static void ensureUpToDate(Context context) {
        if (context == null) {
            return;
        }
        if (INITIALIZED.get()) {
            return;
        }
        synchronized (MigrationOrchestrator.class) {
            if (INITIALIZED.get()) {
                return;
            }
            try {
                // 整个"进程一次性前置"在一个豁免窗口里完成，窗口之外照常被严格模式记录：
                //   ① 打开库（它是所有数据操作的前提）
                //   ② 建升级历史表 / 首次建索引 / 读版本号——各只在进程启动时发生一次，
                //      且都以"库刚打开、连接可用"为前提（升级当天首次建索引可能耗时较久）。
                // 用数组把值带出来，是因为匿名 Runnable 不能直接持有局部变量；
                // 不拆成两个紧邻的窗口——那既不会更窄，又多出两个容器。
                final Database[] databaseHolder = new Database[1];
                final int[] versions = new int[2];
                StartupIoExemption.runExempted(new Runnable() {
                    @Override
                    public void run() {
                        GreenDaoManager manager = GreenDaoManager.getInstance();
                        databaseHolder[0] = manager.getDaoMaster().getDatabase();
                        SchemaHistoryRepository.ensureTable(databaseHolder[0]);
                        // DbIndexMaintenance 内部保证"失败只记录、不抛"，不会把数据问题升级成启动失败。
                        DbIndexMaintenance.ensureIndexes(databaseHolder[0]);
                        versions[0] = readUserVersion(databaseHolder[0]);
                        versions[1] = DatabaseVersionManager.getCurrentVersion();
                    }
                });
                int currentVersion = versions[0];
                int targetVersion = versions[1];
                EasyLog.print(TAG, "Database user version=" + currentVersion + ", target=" + targetVersion);
                INITIALIZED.set(true);
            } catch (Exception e) {
                EasyLog.print(TAG, "Failed to prepare database: " + e.getMessage());
                EasyLog.print(e);
                recordUpgradeFailure(context, e);
                throw new IllegalStateException("GreenDAO database initialization failed", e);
            }
        }
    }

    /**
     * 用**独立连接**补写升级失败记录。
     *
     * <p>修补（2026-10-06）。为什么不能写在 {@code MySQLiteOpenHelper.onUpgrade} 的 catch 里：
     * 那里仍在 SQLiteOpenHelper 的事务内，异常一抛整个事务回滚，刚写的 failed 记录会一起被滚掉。
     * 实测证据：升级崩溃后 {@code SCHEMA_HISTORY} 里只有旧的 create 行，**没有任何 failed**。
     * 走到这里时失败的连接已关闭、事务已回滚，新开一个连接才写得进去。
     *
     * <p>自身失败不得影响原有异常：整段包在 try/catch 里，只记录。
     */
    private static void recordUpgradeFailure(Context context, Exception cause) {
        SQLiteDatabase raw = null;
        try {
            raw = SQLiteDatabase.openDatabase(
                    context.getDatabasePath(AppConst.dbName).getPath(),
                    null,
                    SQLiteDatabase.OPEN_READWRITE);
            Database outOfBand = new StandardDatabase(raw);
            SchemaHistoryRepository.ensureTable(outOfBand);
            int currentVersion = readUserVersion(outOfBand);
            int targetVersion = DatabaseVersionManager.getCurrentVersion();
            String reason = cause.getClass().getSimpleName()
                    + (cause.getMessage() == null ? "" : ": " + cause.getMessage());
            SchemaHistoryRepository.recordUpgrade(
                    outOfBand, currentVersion, targetVersion, "failed", reason);
            EasyLog.print(TAG, "已用独立连接补写升级失败记录：" + reason);
        } catch (Throwable t) {
            EasyLog.print(TAG, "补写升级失败记录也失败了（忽略）：" + t.getMessage());
            EasyLog.print(t);
        } finally {
            if (raw != null) {
                try {
                    raw.close();
                } catch (Throwable ignored) {
                    // 关闭失败无需处理
                }
            }
        }
    }

    private static int readUserVersion(Database db) {
        try (Cursor cursor = db.rawQuery("PRAGMA user_version", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }
}
