package run.yigou.gxzy.greendao.service;


import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.gen.TabNavDao;

public class TabNavService  extends BaseService<TabNav, TabNavDao>{
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
        tableName=TabNavDao.TABLENAME;
        return daoSession.getTabNavDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        TabNavDao.createTable(mDatabase,true);
    }
}
