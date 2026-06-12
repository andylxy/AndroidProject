package run.yigou.gxzy.data.local.service;


import run.yigou.gxzy.data.local.entity.ZhongYao;
import run.yigou.gxzy.data.local.gen.ZhongYaoDao;

public class YaoService extends BaseService<ZhongYao, ZhongYaoDao> {
    private YaoService() {
        if (YaoService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final YaoService INSTANCE = new YaoService();
    }

    public static YaoService getInstance() {
        return YaoService.SingletonHolder.INSTANCE;
    }

    /**
     * @return
     */
    @Override
    protected Class<ZhongYao> getEntityClass() {
        return ZhongYao.class ;
    }

    /**
     * @return
     */
    @Override
    protected ZhongYaoDao getDao() {
        tableName = ZhongYaoDao.TABLENAME;
        return  daoSession.getZhongYaoDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        ZhongYaoDao.createTable(mDatabase, true);
    }
}
