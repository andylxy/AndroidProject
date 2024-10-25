package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.BookChapterBody;
import run.yigou.gxzy.greendao.gen.BookChapterBodyDao;

public class BookChapterBodyService  extends BaseService<BookChapterBody, BookChapterBodyDao>{



    private BookChapterBodyService() {
        if (BookChapterBodyService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final BookChapterBodyService INSTANCE = new BookChapterBodyService();
    }

    public static BookChapterBodyService getInstance() {
        return BookChapterBodyService.SingletonHolder.INSTANCE;
    }


    /**
     * @return
     */
    @Override
    protected Class<BookChapterBody> getEntityClass() {
        return BookChapterBody.class;
    }

    /**
     * @return
     */
    @Override
    protected BookChapterBodyDao getDao() {
        tableName = BookChapterBodyDao.TABLENAME;
        return daoSession.getBookChapterBodyDao() ;
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        BookChapterBodyDao.createTable(mDatabase,true);
    }
}
