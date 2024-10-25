package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.BeiMingCi;
import run.yigou.gxzy.greendao.gen.BeiMingCiDao;

public class BeiMingCiService extends BaseService<BeiMingCi, BeiMingCiDao>{


    private BeiMingCiService() {
        if (BeiMingCiService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final BeiMingCiService INSTANCE = new BeiMingCiService();
    }

    public static BeiMingCiService getInstance() {
        return BeiMingCiService.SingletonHolder.INSTANCE;
    }



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
