package run.yigou.gxzy.greendao.service;

import android.database.Cursor;

import org.greenrobot.greendao.query.DeleteQuery;
import org.greenrobot.greendao.query.QueryBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import run.yigou.gxzy.greendao.GreenDaoManager;
import run.yigou.gxzy.greendao.entity.Book;
import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.gen.BookDao;
import run.yigou.gxzy.greendao.gen.ChapterDao;
import run.yigou.gxzy.utils.StringHelper;

/**
 * Created by zhao on 2017/7/24.
 */

public class ChapterService extends BaseService<Chapter> {

    public QueryBuilder<Chapter> mChapterQueryBuilder = daoSession.queryBuilder(Chapter.class);
    ChapterDao daoConn = daoSession.getChapterDao();
    private List<Chapter> findChapters(String sql, String[] selectionArgs) {
        ArrayList<Chapter> chapters = new ArrayList<>();
        try {
            Cursor cursor = selectBySql(sql, selectionArgs);
            if (cursor == null) return chapters;
            while (cursor.moveToNext()) {
                Chapter chapter = new Chapter();
                chapter.setId(cursor.getString(0));
                chapter.setBookId(cursor.getString(1));
                chapter.setNumber(cursor.getInt(2));
                chapter.setTitle(cursor.getString(3));
                chapter.setUrl(cursor.getString(4));
                chapter.setContent(cursor.getString(5));
                chapters.add(chapter);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return chapters;
        }
        return chapters;
    }

    /**
     * 通过ID查章节
     *
     * @param id
     * @return
     */
    public Chapter getChapterById(String id) {
        ChapterDao chapterDao = GreenDaoManager.getInstance().getSession().getChapterDao();
        return chapterDao.load(id);
    }

    /**
     * 获取书的所有章节
     *
     * @return
     */
    public List<Chapter> findBookAllChapterByBookId(String bookId) {
        if (StringHelper.isEmpty(bookId)) return new ArrayList<>();
        //String sql = "select * from chapter where book_id = ? order by number";
        // return findChapters(sql, new String[]{bookId});
        QueryBuilder<Chapter> where  = mChapterQueryBuilder.where(ChapterDao.Properties.BookId.eq(bookId));
        return    where.list();
    }

    /**
     * 新增章节
     *
     * @param chapter
     */
    public void addChapter(Chapter chapter) {
        chapter.setId(UUID.randomUUID().toString());
        addEntity(chapter);
    }

    /**
     * 查找章节
     *
     * @param bookId
     * @param title
     * @return
     */
    public Chapter findChapterByBookIdAndTitle(String bookId, String title) {
        Chapter chapter = null;
        try {
            String sql = "select id from chapter where book_id = ? and title = ?";
            Cursor cursor = selectBySql(sql, new String[]{bookId, title});
            if (cursor == null) return null;
            if (cursor.moveToNext()) {
                String id = cursor.getString(0);
                chapter = getChapterById(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return chapter;
    }

    /**
     * 删除书的所有章节
     *
     * @param bookId
     */
    public void deleteBookALLChapterById(String bookId) {
       // String sel = "delete from chapter where book_id = ?";
       // rawQuery(sel, new String[]{bookId});

        QueryBuilder<Chapter> where  = mChapterQueryBuilder.where(ChapterDao.Properties.BookId.eq(bookId));
        DeleteQuery<Chapter> deleteQuery = where.buildDelete();
        deleteQuery.executeDeleteWithoutDetachingEntities();

    }

    /**
     * 更新章节
     */
    public void updateChapter(Chapter chapter) {
       // ChapterDao chapterDao = GreenDaoManager.getInstance().getSession().getChapterDao();
        daoConn.update(chapter);
    }

    /**
     * 分段查找章节
     *
     * @param bookId
     * @param from
     * @param to
     * @return
     */
    public List<Chapter> findChapter(String bookId, int from, int to) {
        String sql = "select * from " +
                "(select row_number()over(order by number)rownumber,* from chapter where bookId = ? order by number)a " +
                "where rownumber >= ? and rownumber <= ?";

        return findChapters(sql, new String[]{bookId, String.valueOf(from), String.valueOf(to)});
    }


    /**
     * 保存或更新章节
     *
     * @param chapter
     */
    public void saveOrUpdateChapter(Chapter chapter) {
        if (!StringHelper.isEmpty(chapter.getId())) {
            updateEntity(chapter);
        } else {
            addChapter(chapter);
        }
    }

    /**
     * 批量添加章节
     */
    public void addChapters(List<Chapter> chapters) {
       // ChapterDao chapterDao = daoSession.getChapterDao();
        daoConn.insertInTx(chapters);

    }

    @Override
    public void addEntity(Chapter entity) {
        daoConn.insert(entity);
    }

    @Override
    public void updateEntity(Chapter entity) {
        daoConn .update(entity);
    }

    @Override
    public void deleteEntity(Chapter entity) {
        daoConn.delete(entity);
    }

}
