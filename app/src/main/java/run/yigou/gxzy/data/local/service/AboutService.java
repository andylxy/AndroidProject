package run.yigou.gxzy.data.local.service;


import run.yigou.gxzy.data.local.entity.About;
import run.yigou.gxzy.data.local.entity.ZhongYaoAlia;
import run.yigou.gxzy.data.local.gen.AboutDao;
import run.yigou.gxzy.data.local.gen.ZhongYaoAliaDao;
import run.yigou.gxzy.data.local.gen.ZhongYaoDao;

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
