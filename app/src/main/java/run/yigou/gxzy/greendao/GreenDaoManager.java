package run.yigou.gxzy.greendao;


import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.greendao.gen.DaoMaster;
import run.yigou.gxzy.greendao.gen.DaoSession;
import run.yigou.gxzy.greendao.util.MySQLiteOpenHelper;

/**
 *  作者:  zhs
 *  时间:  2023-07-12 10:40:35
 *  包名:  run.yigou.gxzy.greendao
 *  类名:  GreenDaoManager
 *  版本:  1.0
 *  描述: 数据库操作类
 *
*/
public class GreenDaoManager {
    private static GreenDaoManager instance;
    private static DaoMaster daoMaster;
    private static MySQLiteOpenHelper mySQLiteOpenHelper;
    private DaoSession daoSession;
    public static GreenDaoManager getInstance() {
        if (instance == null) {
            instance = new GreenDaoManager();
        }
        return instance;
    }

    public GreenDaoManager(){
        mySQLiteOpenHelper = new MySQLiteOpenHelper(AppApplication.getmContext(), "read" , null);
        daoMaster = new DaoMaster(mySQLiteOpenHelper.getWritableDatabase());
        daoSession = daoMaster.newSession();
    }

    public static DaoMaster getDaoMaster() {
        return daoMaster;
    }

    public DaoSession getSession(){
       return daoSession;
    }

}
