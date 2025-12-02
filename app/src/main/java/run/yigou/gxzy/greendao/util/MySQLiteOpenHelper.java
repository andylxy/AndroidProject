package run.yigou.gxzy.greendao.util;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;


import org.greenrobot.greendao.database.Database;
import org.greenrobot.greendao.query.QueryBuilder;

import run.yigou.gxzy.greendao.gen.BookDao;
import run.yigou.gxzy.greendao.gen.ChatMessageBeanDao;
import run.yigou.gxzy.greendao.gen.ChatSessionBeanDao;
import run.yigou.gxzy.greendao.gen.DaoMaster;
import run.yigou.gxzy.greendao.gen.SearchHistoryDao;
import run.yigou.gxzy.greendao.gen.UserInfoDao;

/**
 * Created by zhao on 2017/3/15.
 */

public class MySQLiteOpenHelper extends DaoMaster.OpenHelper {

    private Context mContext;


    public MySQLiteOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory,boolean logDebug) {
        super(context, name, factory);
        mContext = context;
        // 启用 SQL 日志
        enableSqlLogging(logDebug);

    }
    private void enableSqlLogging( boolean logDebug ) {
        // 启用 SQL 日志
        QueryBuilder.LOG_SQL = logDebug;      // 打印 SQL 查询
        QueryBuilder.LOG_VALUES = logDebug;   // 打印 SQL 参数值
    }
    @Override
    public void onUpgrade(Database db, int oldVersion, int newVersion) {
        //加入你要新建的或者修改的表的信息
        GreenDaoUpgrade.getInstance().migrate(db, BookDao.class, ChatMessageBeanDao.class, ChatSessionBeanDao.class, SearchHistoryDao.class, UserInfoDao.class);

    }



}