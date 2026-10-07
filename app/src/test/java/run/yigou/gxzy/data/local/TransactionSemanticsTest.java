package run.yigou.gxzy.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.database.StandardDatabase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;

import run.yigou.gxzy.data.local.gen.DaoMaster;
import run.yigou.gxzy.data.local.gen.DaoSession;
import run.yigou.gxzy.data.local.gen.SearchHistoryDao;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.local.service.BaseService;
import run.yigou.gxzy.data.local.service.SearchHistoryService;
import run.yigou.gxzy.data.local.entity.SearchHistory;

// 注意：本测试刻意不碰 GreenDaoManager（其私有构造会去 AppApplication.getContext()，单测里为 null 会 NPE）。
// 改用 Unsafe 直接 allocate 出 DbService / SearchHistoryService 实例并注入内存库，绕开一切生产构造器。

/**
 * 事务语义门禁（票 12 的 A2）。
 *
 * <p>背景：{@code DbService.runInTransaction} 与 {@code BaseService.replaceWhereInTx} 把
 * 「删 + 写」包在同一个 SQLite 事务里，不变量是「要么都提交、要么都回滚」。这类「中途抛异常必须回滚」
 * 的语义，靠 adb 脚本（A1）测不了——脚本无法在事务中途注入异常、也不该去污染真机库。
 *
 * <p>本测试用 Robolectric 在 JVM 上提供**真实可用的 SQLite**（mockable android.jar 里
 * {@code Database}/{@code Cursor} 只是抛 Stub! 的壳，Robolectric 会用真实实现替换），并把它接到
 * {@code GreenDaoManager} 上，从而直接打真实的两个方法：
 * <ul>
 *   <li>提交路径：task 成功 → 数据落地；</li>
 *   <li>回滚路径：task 中途抛异常 / 插入触发主键冲突 → 已做的删除一并回滚，库保持原样；</li>
 *   <li>异常透传：{@code runInTransaction} 必须把 task 内的异常向上抛，不能静默吞掉。</li>
 * </ul>
 *
 * <p>几个关键约束（都是 Robolectric / 单例坑踩出来的）：
 * <ol>
 *   <li>SQLite 的初始化必须在 Robolectric 的「sandbox 线程」上发生（即 {@code @Before}/测试方法里），
 *       不能在 {@code @BeforeClass} 里——后者跑在 JUnit 主线程，Robolectric 的
 *       {@code ConfigurationRegistry}（ThreadLocal）还没建立，{@code SQLiteDatabase.create} 会 NPE。</li>
 *   <li>{@code AppApplication} 是 final 且 onCreate 做重度 DB 初始化，单测里 boot 不了；
 *       所以 {@code @Config(application = NoOpApplication.class)} 换掉它，避免 Robolectric 去 boot
 *       真 Application。{@code DbService}/{@code SearchHistoryService} 的私有构造也会碰
 *       {@code GreenDaoManager}→{@code AppApplication}，故这里用 Unsafe 跳过构造、直接注入内存库，
 *       不调用任何生产构造器。</li>
 *   <li>每个测试都用 Unsafe 重新 allocate 出全新的 service 实例并注入同一个内存库，避免跨测试共享
 *       JVM 单例导致的「服务指向旧库」错乱——这是测试确定性的保证，不依赖 Robolectric 是否帮你重置静态。</li>
 * </ol>
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 30, manifest = Config.NONE, application = TransactionSemanticsTest.NoOpApplication.class)
public class TransactionSemanticsTest {

    /** Robolectric 会实例化它而不是真 AppApplication，从而跳过一切启动期 DB 初始化。 */
    public static class NoOpApplication extends Application {
        @Override
        public void onCreate() {
            // 故意留空：单测不需要 Application 的初始化
        }
    }

    private Database mDatabase;
    private DbService mDbService;
    private SearchHistoryService mSearchHistoryService;
    private DaoSession mSession;

    @Before
    public void setUp() throws Exception {
        // 真实可用的内存 SQLite（Robolectric 提供实现；此刻在 sandbox 线程上，ConfigurationRegistry 已就绪）
        SQLiteDatabase sqlite = SQLiteDatabase.create(null);
        StandardDatabase db = new StandardDatabase(sqlite);
        DaoMaster.createAllTables(db, true);
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession session = daoMaster.newSession();
        mSession = session;

        mDatabase = db;

        // 用 Unsafe 跳过私有构造（构造会碰 AppApplication / GreenDaoManager 真构造），直接注入内存库
        mDbService = allocateInstance(DbService.class);
        setField(DbService.class, mDbService, "mDatabase", db);

        mSearchHistoryService = allocateInstance(SearchHistoryService.class);
        setField(BaseService.class, mSearchHistoryService, "daoSession", session);
        setField(BaseService.class, mSearchHistoryService, "mDatabase", db);
        setField(BaseService.class, mSearchHistoryService, "tableName", SearchHistoryDao.TABLENAME);
        setField(BaseService.class, mSearchHistoryService, "daoConn", session.getSearchHistoryDao());

        clearTable(SearchHistoryDao.TABLENAME);
        mDatabase.execSQL("CREATE TABLE IF NOT EXISTS txn_probe (id INTEGER PRIMARY KEY, v TEXT)");
        clearTable("txn_probe");
    }

    // ---- DbService.runInTransaction：提交 / 回滚 / 异常透传 ----

    @Test
    public void runInTransaction_commitsWhenTaskSucceeds() {
        mDbService.runInTransaction(new Runnable() {
            @Override
            public void run() {
                mDatabase.execSQL("INSERT INTO txn_probe (id, v) VALUES (1, 'a')");
            }
        });
        assertEquals(1, countRows("txn_probe"));
    }

    @Test
    public void runInTransaction_rollsBackAndRethrowsWhenTaskThrows() {
        mDbService.runInTransaction(new Runnable() {
            @Override
            public void run() {
                mDatabase.execSQL("INSERT INTO txn_probe (id, v) VALUES (1, 'a')");
            }
        });
        assertEquals(1, countRows("txn_probe"));

        try {
            mDbService.runInTransaction(new Runnable() {
                @Override
                public void run() {
                    mDatabase.execSQL("INSERT INTO txn_probe (id, v) VALUES (2, 'b')");
                    throw new RuntimeException("boom");
                }
            });
            fail("runInTransaction 应在 task 抛异常时把异常向上抛");
        } catch (RuntimeException e) {
            assertEquals("boom", e.getMessage());
        }
        // 第 2 条插入必须被回滚：仍只有第 1 行
        assertEquals(1, countRows("txn_probe"));
    }

    // ---- BaseService.replaceWhereInTx：原子提交 / 回滚 ----

    @Test
    public void replaceWhereInTx_commitsDeleteAndInsertAtomically() {
        seedSearchHistory("1", "2", "3");
        List<SearchHistory> next = Arrays.asList(
                new SearchHistory("10", "c10", "2026-01-01"),
                new SearchHistory("11", "c11", "2026-01-01"));

        mSearchHistoryService.replaceWhereInTx(next, SearchHistoryDao.Properties.Id.in("1", "2", "3"));

        Set<String> ids = currentSearchHistoryIds();
        assertEquals(2, ids.size());
        assertTrue(ids.contains("10"));
        assertTrue(ids.contains("11"));
    }

    @Test
    public void replaceWhereInTx_rollsBackWhenInsertFails() {
        // 预置 5 行；条件只删 1/2/3，但新数据里含已存在的 4/5（主键冲突）→ insertInTx 抛异常
        seedSearchHistory("1", "2", "3", "4", "5");
        List<SearchHistory> next = Arrays.asList(
                new SearchHistory("4", "c4", "2026-01-01"),
                new SearchHistory("5", "c5", "2026-01-01"),
                new SearchHistory("6", "c6", "2026-01-01"));

        try {
            mSearchHistoryService.replaceWhereInTx(next, SearchHistoryDao.Properties.Id.in("1", "2", "3"));
            fail("replaceWhereInTx 应在插入主键冲突时抛出异常");
        } catch (Exception ignored) {
            // 主键冲突：事务应回滚
        }

        // 删除（1/2/3）必须与插入同事务回滚：5 行原封不动
        Set<String> ids = currentSearchHistoryIds();
        assertEquals(5, ids.size());
        for (String id : new String[]{"1", "2", "3", "4", "5"}) {
            assertTrue("缺失应被回滚保留的行: " + id, ids.contains(id));
        }
    }

    // ---- 辅助 ----

    private void seedSearchHistory(String... ids) {
        List<SearchHistory> list = new ArrayList<>();
        for (String id : ids) {
            list.add(new SearchHistory(id, "content-" + id, "2026-01-01"));
        }
        mSession.getSearchHistoryDao().insertInTx(list);
    }

    private Set<String> currentSearchHistoryIds() {
        Set<String> ids = new HashSet<>();
        for (SearchHistory h : mSearchHistoryService.findAll()) {
            ids.add(h.getId());
        }
        return ids;
    }

    private int countRows(String table) {
        android.database.Cursor c = mDatabase.rawQuery("SELECT COUNT(*) FROM " + table, null);
        c.moveToFirst();
        int n = c.getInt(0);
        c.close();
        return n;
    }

    private void clearTable(String table) {
        mDatabase.execSQL("DELETE FROM " + table);
    }

    @SuppressWarnings("unchecked")
    private static <T> T allocateInstance(Class<T> clazz) throws Exception {
        // 优先 sun.misc.Unsafe（JDK 8~16），回退 jdk.internal.misc.Unsafe（JDK 9+）
        try {
            Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            return (T) unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, clazz);
        } catch (Throwable t) {
            Class<?> unsafeClass = Class.forName("jdk.internal.misc.Unsafe");
            Field theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Object unsafe = theUnsafe.get(null);
            return (T) unsafeClass.getMethod("allocateInstance", Class.class).invoke(unsafe, clazz);
        }
    }

    private static void setField(Class<?> clazz, Object target, String name, Object value) throws Exception {
        Field f = clazz.getDeclaredField(name);
        f.setAccessible(true);
        // 允许写入 final 字段（如 DbService.mDatabase），便于不调构造器直接注入
        if (Modifier.isFinal(f.getModifiers())) {
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        }
        f.set(target, value);
    }
}
