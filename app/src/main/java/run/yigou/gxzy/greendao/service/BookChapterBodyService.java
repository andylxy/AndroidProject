package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.BookChapterBody;
import run.yigou.gxzy.greendao.gen.BookChapterBodyDao;

public class BookChapterBodyService  extends BaseService<BookChapterBody, BookChapterBodyDao>{
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
