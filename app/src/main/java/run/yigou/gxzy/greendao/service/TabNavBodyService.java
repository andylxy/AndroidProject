package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.gen.TabNavBodyDao;

public class TabNavBodyService extends BaseService<TabNavBody, TabNavBodyDao>{


    private TabNavBodyService() {
        if (TabNavBodyService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final TabNavBodyService INSTANCE = new TabNavBodyService();
    }

    public static TabNavBodyService getInstance() {
        return TabNavBodyService.SingletonHolder.INSTANCE;
    }


    /**
     * @return
     */
    @Override
    protected Class<TabNavBody> getEntityClass() {
        return TabNavBody.class;
    }

    /**
     * @return
     */
    @Override
    protected TabNavBodyDao getDao() {
        tableName=TabNavBodyDao.TABLENAME;
        return daoSession.getTabNavBodyDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        TabNavBodyDao.createTable(mDatabase,true);
    }
}
