package run.yigou.gxzy.greendao.service;

import run.yigou.gxzy.greendao.entity.BookChapter;
import run.yigou.gxzy.greendao.gen.BookChapterDao;

public class BookChapterService  extends BaseService<BookChapter, BookChapterDao>{
    /**
     * @return
     */
    @Override
    protected Class<BookChapter> getEntityClass() {
        return BookChapter.class;
    }

    /**
     * @return
     */
    @Override
    protected BookChapterDao getDao() {
        tableName = BookChapterDao.TABLENAME;
        return daoSession.getBookChapterDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        BookChapterDao.createTable(mDatabase,true);
    }
}
