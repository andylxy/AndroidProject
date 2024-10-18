package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.gen.TabNavBodyDao;

public class TabNavBodyService extends BaseService<TabNavBody, TabNavBodyDao>{





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
