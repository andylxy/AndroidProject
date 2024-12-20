package run.yigou.gxzy.greendao.service;


import run.yigou.gxzy.greendao.entity.About;
import run.yigou.gxzy.greendao.entity.ZhongYaoAlia;
import run.yigou.gxzy.greendao.gen.AboutDao;
import run.yigou.gxzy.greendao.gen.ZhongYaoAliaDao;
import run.yigou.gxzy.greendao.gen.ZhongYaoDao;

public class AboutService extends BaseService<About, AboutDao> {
    private AboutService() {
        if (AboutService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final AboutService INSTANCE = new AboutService();
    }

    public static AboutService getInstance() {
        return AboutService.SingletonHolder.INSTANCE;
    }

    /**
     * @return
     */
    @Override
    protected Class<About> getEntityClass() {
        return About.class ;
    }

    /**
     * @return
     */
    @Override
    protected AboutDao getDao() {
        tableName = AboutDao.TABLENAME;
        return  daoSession.getAboutDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        AboutDao.createTable(mDatabase, true);
    }
}
