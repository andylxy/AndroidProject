package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.YaoFangBody;
import run.yigou.gxzy.greendao.gen.YaoFangBodyDao;

public class YaoFangBodyService extends BaseService<YaoFangBody, YaoFangBodyDao>{

    private YaoFangBodyService() {
        if (YaoFangBodyService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final YaoFangBodyService INSTANCE = new YaoFangBodyService();
    }

    public static YaoFangBodyService getInstance() {
        return YaoFangBodyService.SingletonHolder.INSTANCE;
    }
    /**
     * @return
     */
    @Override
    protected Class<YaoFangBody> getEntityClass() {
        return YaoFangBody.class;
    }

    /**
     * @return
     */
    @Override
    protected YaoFangBodyDao getDao() {
        tableName = YaoFangBodyDao.TABLENAME;
        return daoSession.getYaoFangBodyDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
         YaoFangBodyDao.createTable(mDatabase,true);
    }
}
