package run.yigou.gxzy.greendao.service;

import android.database.Cursor;

import org.greenrobot.greendao.query.QueryBuilder;

import run.yigou.gxzy.greendao.GreenDaoManager;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.gen.DaoSession;

public abstract class BaseService<T>  {

    public DaoSession daoSession= GreenDaoManager.getInstance().getSession();
    public abstract void addEntity(T entity) ;

    public abstract void updateEntity(T entity);

    public abstract void deleteEntity(T entity);

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
