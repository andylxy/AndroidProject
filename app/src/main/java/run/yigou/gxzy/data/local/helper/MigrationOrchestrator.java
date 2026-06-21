package run.yigou.gxzy.data.local.helper;

import android.content.Context;
import android.database.Cursor;

import org.greenrobot.greendao.database.Database;

import java.util.concurrent.atomic.AtomicBoolean;

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
                GreenDaoManager manager = GreenDaoManager.getInstance();
                Database database = manager.getDaoMaster().getDatabase();
                SchemaHistoryRepository.ensureTable(database);
                int currentVersion = readUserVersion(database);
                int targetVersion = DatabaseVersionManager.getCurrentVersion();
                EasyLog.print(TAG, "Database user version=" + currentVersion + ", target=" + targetVersion);
                INITIALIZED.set(true);
            } catch (Exception e) {
                EasyLog.print(TAG, "Failed to prepare database: " + e.getMessage());
                throw new IllegalStateException("GreenDAO database initialization failed", e);
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
