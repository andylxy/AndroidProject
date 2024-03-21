package run.yigou.gxzy.greendao.service;

import android.database.Cursor;

import org.greenrobot.greendao.AbstractDao;
import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.UUID;

import run.yigou.gxzy.greendao.GreenDaoManager;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.greendao.gen.DaoSession;
import run.yigou.gxzy.greendao.gen.UserInfoDao;

public abstract class BaseService<T, TDao extends AbstractDao<T, ?>> {

    public DaoSession daoSession = GreenDaoManager.getInstance().getSession();
    protected QueryBuilder<T> mQueryBuilder = daoSession.queryBuilder(getEntityClass());
    protected Database mDatabase;
    protected String tableName;
    protected String getUuid= UUID.randomUUID().toString();
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
    public void updateEntity(T entity) {
        if (entity!=null)
            daoConn.update(entity);
    }
    public void deleteEntity(T entity) {
        if (entity!=null)
            daoConn.delete(entity);
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
            e.printStackTrace();
            return null;
        }
        return cursor;
    }

    /**
     * 执行SQL进行增删改
     *
     * @param sql
     * @param selectionArgs
     */
    public void rawQuery(String sql, String[] selectionArgs) {
        Cursor cursor = daoSession.getDatabase().rawQuery(sql, selectionArgs);
    }


}
