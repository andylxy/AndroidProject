package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.YaoFang;
import run.yigou.gxzy.greendao.gen.YaoFangDao;

public class YaoFangService extends BaseService<YaoFang, YaoFangDao>{

    private YaoFangService() {
        if (YaoFangBodyService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final YaoFangService INSTANCE = new YaoFangService();
    }

    public static YaoFangService getInstance() {
        return YaoFangService.SingletonHolder.INSTANCE;
    }
    /**
     * @return
     */
    @Override
    protected Class<YaoFang> getEntityClass() {
        return YaoFang.class;
    }

    /**
     * @return
     */
    @Override
    protected YaoFangDao getDao() {
        tableName = YaoFangDao.TABLENAME;
        return daoSession.getYaoFangDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {

        YaoFangDao.createTable(mDatabase,true);
    }
}
