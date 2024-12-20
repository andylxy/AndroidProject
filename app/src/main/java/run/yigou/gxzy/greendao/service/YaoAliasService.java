package run.yigou.gxzy.greendao.service;


import run.yigou.gxzy.greendao.entity.ZhongYaoAlia;
import run.yigou.gxzy.greendao.gen.YaoAliaDao;
import run.yigou.gxzy.greendao.gen.ZhongYaoAliaDao;
import run.yigou.gxzy.greendao.gen.ZhongYaoDao;

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
