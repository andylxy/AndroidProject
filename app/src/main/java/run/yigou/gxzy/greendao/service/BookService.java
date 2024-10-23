package run.yigou.gxzy.greendao.service;

import android.database.Cursor;


import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.gen.BookDao;
import run.yigou.gxzy.utils.StringHelper;

/**
 * Created by zhao on 2017/7/24.
 */

public class BookService extends BaseService<Book,BookDao> {

    //public QueryBuilder<Book> mBookQueryBuilder = daoSession.queryBuilder(Book.class);
    //BookDao daoConn = daoSession.getBookDao();
    private ChapterService mChapterService;

    public BookService() {
        mChapterService = new ChapterService();
    }




    /**
     * 获取所有的书
     *
     * @return
     */
    public List<Book> getAllBooks() {
        //查出当前对应的数据
       // List<Book> bookList = mBookQueryBuilder.list();
        return getQueryBuilder().list();
    }










    /**
     * 查询书籍总数
     *
     * @return
     */
    public int countBookTotalNum() {
        int num = 0;
        try {
            // 查出所有的数据
            List<Book> bookList = getQueryBuilder().list();
            num = bookList.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return num;
    }

    /**
     * 更新书
     *
     * @param books
     */
    public void updateBooks(List<Book> books) {

        daoConn.updateInTx(books);
    }

    @Override
    protected Class<Book> getEntityClass() {
        return Book.class;
    }

    @Override
    protected BookDao getDao() {
        tableName =BookDao.TABLENAME;
        return daoSession.getBookDao();
    }

    @Override
    protected void createTable() {
        BookDao.createTable(mDatabase,true);
    }

//    @Override
//    public void addEntity(Book entity) {
//        daoConn.insert(entity);
//    }
//
//    @Override
//    public void updateEntity(Book entity) {
//        daoConn .update(entity);
//    }
//
//    @Override
//    public void deleteEntity(Book entity) {
//        daoConn.delete(entity);
//    }


}
