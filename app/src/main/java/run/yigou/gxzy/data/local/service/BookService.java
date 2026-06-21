package run.yigou.gxzy.data.local.service;

import org.greenrobot.greendao.query.QueryBuilder;

import java.util.List;

import run.yigou.gxzy.data.local.entity.Book;
import run.yigou.gxzy.data.local.gen.BookDao;
import run.yigou.gxzy.log.EasyLog;

/**
 * 书籍数据服务
 */
public class BookService extends BaseService<Book,BookDao> {
    private static final String TAG = "BookService";

    private BookService() {
        if (BookService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final BookService INSTANCE = new BookService();
    }

    public static BookService getInstance() {
        return BookService.SingletonHolder.INSTANCE;
    }

    /**
     * 获取所有的书
     */
    public List<Book> getAllBooks() {
        return getQueryBuilder().list();
    }

    @Override
    protected Class<Book> getEntityClass() {
        return Book.class;
    }

    @Override
    protected BookDao getDao() {
        tableName = BookDao.TABLENAME;
        return daoSession.getBookDao();
    }

    @Override
    protected void createTable() {
        BookDao.createTable(mDatabase, true);
    }
}
