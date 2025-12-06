package run.yigou.gxzy.greendao.util;

import android.database.Cursor;

import org.greenrobot.greendao.database.Database;

/**
 * Persists schema upgrade information for diagnostics and quick health checks.
 */
public final class SchemaHistoryRepository {

    private static final String TABLE_NAME = "SCHEMA_HISTORY";
    private static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
            "ID INTEGER PRIMARY KEY AUTOINCREMENT," +
            "FROM_VERSION INTEGER NOT NULL," +
            "TO_VERSION INTEGER NOT NULL," +
            "EXECUTED_AT INTEGER NOT NULL," +
            "STATUS TEXT NOT NULL," +
            "NOTE TEXT" +
            ")";

    private SchemaHistoryRepository() {
    }

    public static void ensureTable(Database db) {
        db.execSQL(CREATE_TABLE_SQL);
    }

    public static void recordUpgrade(Database db, int fromVersion, int toVersion, String status, String note) {
        ensureTable(db);
        Object[] bindArgs = new Object[]{fromVersion, toVersion, System.currentTimeMillis(), status, note};
        db.execSQL("INSERT INTO " + TABLE_NAME + " (FROM_VERSION, TO_VERSION, EXECUTED_AT, STATUS, NOTE) VALUES (?,?,?,?,?)", bindArgs);
    }

    public static int queryLatestToVersion(Database db) {
        ensureTable(db);
        try (Cursor cursor = db.rawQuery("SELECT TO_VERSION FROM " + TABLE_NAME + " ORDER BY ID DESC LIMIT 1", null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }
}
