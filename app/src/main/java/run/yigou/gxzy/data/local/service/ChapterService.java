package run.yigou.gxzy.data.local.service;


import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.entity.ZhongYao;
import run.yigou.gxzy.data.local.gen.ChapterDao;
import run.yigou.gxzy.data.local.gen.ZhongYaoDao;

public class ChapterService extends BaseService<Chapter, ChapterDao> {
    private ChapterService() {
        if (ChapterService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final ChapterService INSTANCE = new ChapterService();
    }

    public static ChapterService getInstance() {
        return ChapterService.SingletonHolder.INSTANCE;
    }

    /**
     * @return
     */
    @Override
    protected Class<Chapter> getEntityClass() {
        return Chapter.class ;
    }

    /**
     * @return
     */
    @Override
    protected ChapterDao getDao() {
        tableName = ChapterDao.TABLENAME;
        return  daoSession.getChapterDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        ChapterDao.createTable(mDatabase, true);
    }
}
