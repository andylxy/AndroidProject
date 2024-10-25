package run.yigou.gxzy.greendao.service;

import android.database.Cursor;


import java.util.ArrayList;
import java.util.Date;

import run.yigou.gxzy.greendao.entity.SearchHistory;
import run.yigou.gxzy.greendao.gen.SearchHistoryDao;
import run.yigou.gxzy.utils.DateHelper;


/**
 * 作者:  zhs
 * 时间:  2023-07-12 22:30:01
 * 包名:  run.yigou.gxzy.greendao.service
 * 类名:  SearchHistoryService
 * 版本:  1.0
 * 描述:
 */

public class SearchHistoryService extends BaseService<SearchHistory,SearchHistoryDao> {
//    public QueryBuilder<SearchHistory> mSearchHistoryQueryBuilder = daoSession.queryBuilder(SearchHistory.class);
//    SearchHistoryDao daoConn = daoSession.getSearchHistoryDao();


    private SearchHistoryService() {
        if (SearchHistoryService.class.desiredAssertionStatus()) {
            throw new AssertionError("No instances allowed");
        }
    }

    private static class SingletonHolder {
        private static final SearchHistoryService INSTANCE = new SearchHistoryService();
    }

    public static SearchHistoryService getInstance() {
        return SearchHistoryService.SingletonHolder.INSTANCE;
    }


    private ArrayList<SearchHistory> findSearchHistorys(String sql, String[] selectionArgs) {
        ArrayList<SearchHistory> searchHistories = new ArrayList<>();
        try {
            Cursor cursor = selectBySql(sql, selectionArgs);
            if (cursor == null) return searchHistories;
            while (cursor.moveToNext()) {
                SearchHistory searchHistory = new SearchHistory();
                searchHistory.setId(cursor.getString(0));
                searchHistory.setContent(cursor.getString(1));
                searchHistory.setCreateDate(cursor.getString(2));
                searchHistories.add(searchHistory);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return searchHistories;
        }
        return searchHistories;
    }

    /**
     * 返回所有历史记录（按时间从大到小）
     *
     * @return
     */
    public ArrayList<SearchHistory> findAllSearchHistory() {
        //  String sql = "select * from search_history order by create_date desc";
        // return findSearchHistorys(sql, null);
        return  (ArrayList<SearchHistory>) getQueryBuilder().list();

    }


    /**
     * 添加历史记录
     *
     * @param searchHistory
     */
    public void addSearchHistory(SearchHistory searchHistory) {
        searchHistory.setId(getUUID());
        searchHistory.setCreateDate(DateHelper.longToTime(new Date().getTime()));
        addEntity(searchHistory);
    }

    /**
     * 删除历史记录
     *
     * @param searchHistory
     */
    public void deleteHistory(SearchHistory searchHistory) {
        deleteEntity(searchHistory);
    }

    /**
     * 清空历史记录
     */
    public void clearHistory() {
         getQueryBuilder().buildDelete().executeDeleteWithoutDetachingEntities();
    }

    /**
     * 根据内容查找历史记录
     *
     * @param content
     * @return
     */
    public SearchHistory findHistoryByContent(String content) {
        SearchHistory searchHistory = null;
        String sql = "select * from search_history where content = ?";
        Cursor cursor = selectBySql(sql, new String[]{content});
        if (cursor == null) return searchHistory;
        if (cursor.moveToNext()) {
            searchHistory = new SearchHistory();
            searchHistory.setId(cursor.getString(0));
            searchHistory.setContent(cursor.getString(1));
            searchHistory.setCreateDate(cursor.getString(2));
        }
        return searchHistory;
    }

    /**
     * 添加或更新历史记录
     *
     * @param history
     */
    public void addOrUpadteHistory(String history) {
        SearchHistory searchHistory = findHistoryByContent(history);
        if (searchHistory == null) {
            searchHistory = new SearchHistory();
            searchHistory.setContent(history);
            addSearchHistory(searchHistory);
        } else {
            searchHistory.setCreateDate(DateHelper.longToTime(new Date().getTime()));
            updateEntity(searchHistory);
        }
    }

    @Override
    protected Class<SearchHistory> getEntityClass() {
        return SearchHistory.class;
    }

    @Override
    protected SearchHistoryDao getDao() {
        tableName=SearchHistoryDao.TABLENAME;
        return daoSession.getSearchHistoryDao();
    }

    /**
     *
     */
    @Override
    protected void createTable() {
        SearchHistoryDao.createTable(mDatabase,true);
    }

//    @Override
//    public void addEntity(SearchHistory entity) {
//        daoConn.insert(entity);
//    }
//
//    @Override
//    public void updateEntity(SearchHistory entity) {
//        daoConn.update(entity);
//    }
//
//    @Override
//    public void deleteEntity(SearchHistory entity) {
//        daoConn.delete(entity);
//    }
}
