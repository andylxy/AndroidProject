package run.yigou.gxzy.greendao.service;


import run.yigou.gxzy.greendao.entity.ZhongYao;
import run.yigou.gxzy.greendao.gen.ZhongYaoDao;

public class YaoService extends BaseService<ZhongYao, ZhongYaoDao> {
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
