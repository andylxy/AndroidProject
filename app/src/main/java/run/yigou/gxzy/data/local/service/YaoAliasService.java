package run.yigou.gxzy.data.local.service;


import run.yigou.gxzy.data.local.entity.ZhongYaoAlia;
import run.yigou.gxzy.data.local.gen.YaoAliaDao;
import run.yigou.gxzy.data.local.gen.ZhongYaoAliaDao;
import run.yigou.gxzy.data.local.gen.ZhongYaoDao;

public class YaoAliasService extends BaseService<ZhongYaoAlia, ZhongYaoAliaDao> {
    private YaoAliasService() {
        if (YaoAliasService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final YaoAliasService INSTANCE = new YaoAliasService();
    }

    public static YaoAliasService getInstance() {
        return YaoAliasService.SingletonHolder.INSTANCE;
    }

    /**
     * @return
     */
    @Override
    protected Class<ZhongYaoAlia> getEntityClass() {
        return ZhongYaoAlia.class ;
    }

    /**
     * @return
     */
    @Override
    protected ZhongYaoAliaDao getDao() {
        tableName = ZhongYaoAliaDao.TABLENAME;
        return  daoSession.getZhongYaoAliaDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        ZhongYaoAliaDao.createTable(mDatabase, true);
    }
}
