package run.yigou.gxzy.greendao.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.DatabaseOpenHelper;

import run.yigou.gxzy.greendao.gen.DaoMaster;

/**
 * DatabaseOpenHelper variant that uses the custom version provided by DatabaseVersionManager.
 * It mirrors DaoMaster.OpenHelper but keeps the schema version in project code so upgrades can be bumped freely.
 */
abstract class VersionedOpenHelper extends DatabaseOpenHelper {

    VersionedOpenHelper(Context context, String name) {
        super(context, name, DatabaseVersionManager.getCurrentVersion());
    }

    VersionedOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
        super(context, name, factory, DatabaseVersionManager.getCurrentVersion());
    }

    @Override
    public void onCreate(Database db) {
        DaoMaster.createAllTables(db, false);
    }
}
