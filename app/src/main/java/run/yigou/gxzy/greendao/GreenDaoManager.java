package run.yigou.gxzy.greendao;


import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.common.AppConst;
import org.greenrobot.greendao.database.Database;

import run.yigou.gxzy.greendao.gen.DaoMaster;
import run.yigou.gxzy.greendao.gen.DaoSession;
import run.yigou.gxzy.greendao.util.MySQLiteOpenHelper;
import run.yigou.gxzy.other.AppConfig;

/**
 * 作者:  zhs
 * 时间:  2023-07-12 10:40:35
 * 包名:  run.yigou.gxzy.greendao
 * 类名:  GreenDaoManager
 * 版本:  1.0
 * 描述: 数据库操作类
 * 
 * 使用说明：
 * 1. 通过getInstance()获取单例实例
 * 2. 通过getSession()获取DaoSession进行数据库操作
 * 3. 数据库版本管理由DatabaseVersionManager负责
 * 4. 实体注册由EntityRegistrationHelper负责
 */
public class GreenDaoManager {
    private static GreenDaoManager instance;
    private static DaoMaster daoMaster;
    private static MySQLiteOpenHelper mySQLiteOpenHelper;
    private DaoSession daoSession;

//    public static GreenDaoManager getInstance() {
//        if (instance == null) {
//            instance = new GreenDaoManager();
//        }
//        return instance;
//    }

    /**
     * 私有构造函数，防止外部实例化
     * 初始化GreenDao相关组件
     */
    private GreenDaoManager() {
        try {
            // 构造函数中的初始化代码
           // boolean isDebug = AppConfig.isDebug();
            mySQLiteOpenHelper = new MySQLiteOpenHelper(AppApplication.getContext(), AppConst.dbName, null, AppConfig.isDebug());
            daoMaster = new DaoMaster(mySQLiteOpenHelper.getWritableDatabase());
            daoSession = daoMaster.newSession();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize GreenDaoManager", e);
        }
    }

    /**
     * 静态内部类，用于延迟初始化单例实例
     * 确保线程安全且懒加载
     */
    private static class SingletonHolder {
        private static final GreenDaoManager INSTANCE = new GreenDaoManager();
    }

    /**
     * 获取单例实例
     *
     * @return 单例实例
     */
    public static GreenDaoManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public static DaoMaster getDaoMaster() {
        return daoMaster;
    }

    public static Database getDatabase() {
        return daoMaster != null ? daoMaster.getDatabase() : null;
    }

    static MySQLiteOpenHelper getOpenHelper() {
        return mySQLiteOpenHelper;
    }

    public DaoSession getSession() {
        return daoSession;
    }

}