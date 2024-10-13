package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.BeiMingCi;
import run.yigou.gxzy.greendao.gen.BeiMingCiDao;

public class BeiMingCiService extends BaseService<BeiMingCi, BeiMingCiDao>{
    /**
     * @return
     */
    @Override
    protected Class<BeiMingCi> getEntityClass() {
        return BeiMingCi.class;
    }

    /**
     * @return
     */
    @Override
    protected BeiMingCiDao getDao() {
        tableName = BeiMingCiDao.TABLENAME;
        return daoSession.getBeiMingCiDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        BeiMingCiDao.createTable(mDatabase,true);
    }
}
