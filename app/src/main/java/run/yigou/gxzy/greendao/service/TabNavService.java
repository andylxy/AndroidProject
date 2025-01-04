package run.yigou.gxzy.greendao.service;


import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;

import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.gen.TabNavDao;

public class TabNavService extends BaseService<TabNav, TabNavDao> {


    private TabNavService() {
        if (TabNavService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final TabNavService INSTANCE = new TabNavService();
    }

    public static TabNavService getInstance() {
        return TabNavService.SingletonHolder.INSTANCE;
    }

    /**
     * @return
     */
    @Override
    protected Class<TabNav> getEntityClass() {
        return TabNav.class;
    }

    /**
     * @return
     */
    @Override
    protected TabNavDao getDao() {
        tableName = TabNavDao.TABLENAME;
        return daoSession.getTabNavDao();
    }

    @Override
    public ArrayList<TabNav> findAll() {
        QueryBuilder<TabNav> queryBuilder = getQueryBuilder();
        //  QueryBuilder<TabNav> queryBuilder =  daoSession.getTabNavDao().queryBuilder();
        // 按照 'Order' 字段升序排序
        queryBuilder.orderAsc(TabNavDao.Properties.Order);
        return (ArrayList<TabNav>) queryBuilder.list();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        TabNavDao.createTable(mDatabase, true);
    }
}
