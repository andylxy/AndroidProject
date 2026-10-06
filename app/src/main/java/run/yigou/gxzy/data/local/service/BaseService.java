package run.yigou.gxzy.data.local.service;

import android.database.Cursor;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.database.Database;

import org.greenrobot.greendao.query.DeleteQuery;
import org.greenrobot.greendao.query.QueryBuilder;
import org.greenrobot.greendao.query.WhereCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import run.yigou.gxzy.data.local.GreenDaoManager;
import run.yigou.gxzy.data.local.gen.DaoSession;
import run.yigou.gxzy.log.EasyLog;

public abstract class BaseService<T, TDao extends AbstractDao<T, ?>> {

    private static final String TAG = "BaseService";

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

    private void initTable() {
        boolean isTableExists = false;
        Cursor cursor = null;
        try {
            cursor = mDatabase.rawQuery("SELECT DISTINCT tbl_name FROM sqlite_master WHERE tbl_name = '" + tableName + "'", null);
            if (cursor != null && cursor.getCount() > 0) {
                isTableExists = true;
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        if (!isTableExists) {
            createTable();
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
