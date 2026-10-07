package run.yigou.gxzy.data.local.service;

import android.database.Cursor;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.database.Database;

import org.greenrobot.greendao.query.DeleteQuery;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import run.yigou.gxzy.data.local.GreenDaoManager;
import run.yigou.gxzy.data.local.gen.DaoSession;
import run.yigou.gxzy.log.EasyLog;

public abstract class BaseService<T, TDao extends AbstractDao<T, ?>> {

    private static final String TAG = "BaseService";

    /** 进程内缓存的"库中已有哪些表"（见 {@code existingTableNames}）。 */
    private static final Object TABLE_NAMES_LOCK = new Object();
    private static volatile Set<String> sExistingTables;

    public DaoSession daoSession = GreenDaoManager.getInstance().getSession();
    protected Database mDatabase;
    protected String tableName;

    // 定义一个抽象方法，用于在子类中实现获取实体类的 Class 对象
    protected abstract Class<T> getEntityClass();

    // 在子类中实现 getDao 方法来获取具体的 DAO 对象
    protected abstract TDao getDao();
    protected TDao daoConn = getDao();
    protected abstract  void  createTable();
    public BaseService() {
        mDatabase = GreenDaoManager.getDaoMaster().getDatabase();
        initTable();
    }

    /**
     * 确保本 Service 对应的表存在；不存在就交给子类 {@link #createTable()} 补建。
     *
     * <p>为什么把"已有哪些表"缓存成一张进程内的表集合（见 greendao 加固票 11 B 项）：本方法是构造器末尾调用的，
     * 而 {@code DbService} 的构造器会逐个 new 出 19 个 Service ⇒ 一次 DbService 建立 =
     * 19 次 {@code sqlite_master} 查询，全部落在这条线程上。改成"整机只查一次、之后复用"，
     * 查询次数从 19 降到 1，行为完全一致（查的还是同一张 sqlite_master）。
     *
     * <p>缓存安全性：{@code sqlite_master} 在进程运行期间只有本 App 自己会改，而任何建表动作
     * 都发生在启动迁移阶段（早于 DbService 建立）。补建成功后也会把表名加进集合，
     * 避免同一张表被反复 createTable（子类的 createTable 都带 IF NOT EXISTS，仍幂等）。
     */
    private void initTable() {
        if (!existingTableNames(mDatabase).contains(tableName)) {
            createTable();
            existingTableNames(mDatabase).add(tableName);
        }
    }

    /** 进程内缓存的"库中已有哪些表"；首次访问时查一次 sqlite_master。 */
    private static Set<String> existingTableNames(Database db) {
        Set<String> cached = sExistingTables;
        if (cached != null) {
            return cached;
        }
        synchronized (TABLE_NAMES_LOCK) {
            if (sExistingTables != null) {
                return sExistingTables;
            }
            Set<String> names = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
            Cursor cursor = null;
            boolean readable = false;
            try {
                cursor = db.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE type = 'table'", null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        names.add(cursor.getString(0));
                    }
                }
                readable = true;
            } catch (Exception e) {
                EasyLog.print(TAG, "读取表清单失败: " + e.getMessage());
                EasyLog.print(e);
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
            if (readable) {
                // 只有真读到了才缓存：把"读失败"当成"库里没有表"会让每个 Service 都去 createTable。
                sExistingTables = names;
            }
            return names;
        }
    }

    public long addEntity(T entity) {
        if (entity==null) return 0;
      return   daoConn.insert(entity);
    }

    /**
     * 全量替换表内容：清空旧数据后写入新数据，整体包在同一个数据库事务里。
     *
     * <p>不变量：{@code deleteAll()} 与批量插入同处一个事务，二者要么都提交、要么都回滚。
     * 若把两步拆成各自独立的事务，先提交的清空会在后续插入失败时留下空表（数据丢失），
     * 且两步之间的窗口期其它线程可能读到空表。因此全量替换必须走本方法。
     *
     * <p>适用边界：仅用于「整表内容一次性替换」且允许失败时保留旧数据的场景；
     * 需要「部分成功」语义的逐条写入请用 {@code addEntity}。
     *
     * <p>失败时抛出异常（由调用方决定重试或降级），不会静默吞掉。
     *
     * @param entities 待写入的新数据；为空时只清空、不写入
     */
    public void replaceAllInTx(List<T> entities) {
        mDatabase.beginTransaction();
        try {
            daoConn.deleteAll();
            if (entities != null && !entities.isEmpty()) {
                daoConn.insertInTx(entities);
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    /**
     * 全量替换某个子集：按条件删掉旧数据后批量写入新数据，整体包在同一个数据库事务里。
     *
     * <p>与 {@link #replaceAllInTx(List)} 是同一契约，区别只在删除范围——整表替换用那个，
     * 只替换其中一部分（例如「某本书的章节」）用这个。
     *
     * <p>不变量：删除与批量插入同处一个事务，二者要么都提交、要么都回滚。若把两步拆成各自独立
     * 执行，先落地的删除会在插入失败后留下空洞（数据丢失），且两步之间的窗口期其它线程会读到
     * 不完整的表。因此「先删后插」的替换必须走本方法。
     *
     * <p>失败时抛出异常（由调用方决定重试或降级），不会静默吞掉。
     *
     * @param entities        待写入的新数据；为空时只删除、不写入
     * @param deleteCondition 删除条件，不可为 null——整表替换请用 {@link #replaceAllInTx(List)}
     */
    public void replaceWhereInTx(List<T> entities, WhereCondition deleteCondition) {
        if (deleteCondition == null) {
            throw new IllegalArgumentException(
                "replaceWhereInTx 需要删除条件；整表替换请用 replaceAllInTx");
        }
        mDatabase.beginTransaction();
        try {
            deleteAll(deleteCondition);
            if (entities != null && !entities.isEmpty()) {
                daoConn.insertInTx(entities);
            }
            mDatabase.setTransactionSuccessful();
        } finally {
            mDatabase.endTransaction();
        }
    }

    public void updateEntity(T entity) {
        if (entity!=null)
            daoConn.update(entity);
    }
    public void deleteEntity(T entity) {
        if (entity!=null)
            daoConn.delete(entity);
    }
    public void deleteAll(WhereCondition cond, WhereCondition... condMore) {

       // DaoSession daoSession = ((AserbaoApplication) getApplication()).getDaoSession();
       // QueryBuilder<Student> where = daoSession.queryBuilder(Student.class).where(StudentDao.Properties.Id.gt(5));
      //  DeleteQuery<Student> deleteQuery = where.buildDelete();
      //  deleteQuery.executeDeleteWithoutDetachingEntities();

        QueryBuilder<T> where=  daoConn.queryBuilder().where(cond, condMore);
        DeleteQuery<T> deleteQuery = where.buildDelete();
        deleteQuery.executeDeleteWithoutDetachingEntities();

    }
    public void deleteAll() {

        daoConn.deleteAll();
    }
    /**
     * 返回所有记录
     *
     * @return
     */
    public ArrayList<T> findAll() {
        //  String sql = "select * from table order by create_date desc";
        return  (ArrayList<T>) getQueryBuilder().list();

    }

    /**
     * 返回指定条件的记录
     *
     * @return
     */
    public ArrayList<T> find(WhereCondition cond, WhereCondition... condMore) {
        //  String sql = "select * from table order by create_date desc";
        // 清空查询条件
        return  (ArrayList<T>) getQueryBuilder().where(cond, condMore).list();
    }
    /**
     * 通过SQL查找
     *
     * @param sql
     * @param selectionArgs
     * @return
     */
    public Cursor selectBySql(String sql, String[] selectionArgs) {

        Cursor cursor = null;
        try {
            cursor = daoSession.getDatabase().rawQuery(sql, selectionArgs);
        } catch (Exception e) {
            EasyLog.print(TAG, "SQL查询失败: " + e.getMessage());
            return null;
        }
        return cursor;
    }




    public String getUUID() {
        return  UUID.randomUUID().toString();
    }

    public QueryBuilder<T> getQueryBuilder() {
        return daoSession.queryBuilder(getEntityClass());
    }

}
