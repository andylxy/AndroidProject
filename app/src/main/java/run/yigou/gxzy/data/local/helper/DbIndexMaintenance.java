package run.yigou.gxzy.data.local.helper;

import android.database.Cursor;

import org.greenrobot.greendao.database.Database;

import run.yigou.gxzy.log.EasyLog;

/**
 * 启动时的索引维护。
 *
 * <p>为什么需要：旧库是**零二级索引**的——GreenDAO 只在实体声明 {@code @Index}/{@code @Unique}
 * 时才建索引，本项目一个都没声明。而 {@code BOOK_CHAPTER_BODY} 更彻底：它连主键都没有，
 * 章节正文的幂等性只靠应用层"先按外键列删、再整批插"自觉维持，数据库层没有任何唯一性保证。
 *
 * <p>为什么不走 {@code @Index} 注解 + 递增版本号：递增 {@code CURRENT_VERSION} 会触发
 * {@link GreenDaoUpgrade#smartMigrate} 的**破坏性重建**（DROP 全部已存在表再重建），
 * 为加一个索引付这个代价不值得。这里用 {@code CREATE ... IF NOT EXISTS} 在启动时幂等补齐：
 * 不动版本号、不重建表、无数据风险。
 *
 * <p>安全边界：索引属于增强项，**任何失败都只记录、不抛出**——数据问题绝不能升级成启动失败。
 * 唯一约束还多一道防御：建索引前先查重。值是 {@code UUID.randomUUID()}，代码层面保证唯一，
 * 但历史版本写下的数据无法用当前代码证明；有重复就只告警、不建索引，留给人工清理。
 */
public final class DbIndexMaintenance {

    private static final String TAG = "DbIndexMaintenance";

    private static final String TABLE = "BOOK_CHAPTER_BODY";
    private static final String COLUMN = "BOOK_CHAPTER_BODY_ID";
    private static final String INDEX = "ux_book_chapter_body";

    private DbIndexMaintenance() {
    }

    /** 幂等入口：可重复调用；失败只记录，不抛。 */
    public static void ensureIndexes(Database db) {
        if (db == null) {
            return;
        }
        ensureUniqueBookChapterBodyId(db);
    }

    private static void ensureUniqueBookChapterBodyId(Database db) {
        try {
            long total = scalar(db, "SELECT COUNT(*) FROM " + TABLE);
            long distinct = scalar(db, "SELECT COUNT(DISTINCT " + COLUMN + ") FROM " + TABLE);
            if (total < 0 || distinct < 0) {
                return; // 表读不到，scalar 里已记录
            }
            if (total != distinct) {
                EasyLog.print(TAG, "检测到 " + TABLE + "." + COLUMN + " 存在重复（" + total
                        + " 行 / 去重后 " + distinct + " 行），跳过建唯一索引；请人工清理后重启");
                return;
            }
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS " + INDEX
                    + " ON " + TABLE + "(" + COLUMN + ")");
            EasyLog.print(TAG, "唯一索引就绪：" + INDEX);
        } catch (Throwable t) {
            EasyLog.print(TAG, "唯一索引维护失败（已忽略，不影响启动）：" + t.getMessage());
            EasyLog.print(t);
        }
    }

    /** 取单个标量；失败返回 -1 并记录。 */
    private static long scalar(Database db, String sql) {
        try (Cursor cursor = db.rawQuery(sql, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getLong(0);
            }
        } catch (Throwable t) {
            EasyLog.print(TAG, "查询失败：" + sql);
            EasyLog.print(t);
        }
        return -1;
    }
}
