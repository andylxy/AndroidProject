/*
 * 项目名: AndroidProject
 * 类名: DbDao.java
 * 包名: run.yigou.gxzy.greendao.util.DbDao
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月11日 22:22:07
 * 上次修改时间: 2024年03月11日 22:22:06
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.greendao.util;

import run.yigou.gxzy.greendao.service.BookService;
import run.yigou.gxzy.greendao.service.ChapterService;
import run.yigou.gxzy.greendao.service.SearchHistoryService;
import run.yigou.gxzy.greendao.service.UserInfoService;

/**
 * 版本:  1.0
 * 描述: 统一创建数据Dao.需要的时候直接引用
 */
public class DbService {
    public UserInfoService mUserInfoService;
    public BookService mBookService;
    public ChapterService mChapterService;
    public SearchHistoryService mSearchHistoryService;
    private static DbService instance;
    public  static DbService getInstance() {
        if (instance == null) {
            instance = new DbService();
        }
        return instance;
    }
    private DbService() {
        mUserInfoService = new UserInfoService();
        mBookService = new BookService();
        mChapterService = new ChapterService();
        mSearchHistoryService = new SearchHistoryService();
    }
}
