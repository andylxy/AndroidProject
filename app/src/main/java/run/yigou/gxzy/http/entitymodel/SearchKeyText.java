/*
 * 项目名: AndroidProject
 * 类名: SearchKeyText.java
 * 包名: run.yigou.gxzy.http.entitymodel.SearchKeyText
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年03月07日 23:00:37
 * 上次修改时间: 2024年03月07日 23:00:37
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.http.entitymodel;

import java.util.List;

import run.yigou.gxzy.greendao.entity.Chapter;

/**
 * 版本:  1.0
 * 描述:
 */
public class SearchKeyText   {
    public String getBookCaseName() {
        return mBookCaseName;
    }

    public SearchKeyText setBookCaseName(String bookCaseName) {
        mBookCaseName = bookCaseName;
        return this;
    }

    public int getSearcTextResCount() {
        return mSearcTextResCount;
    }

    public SearchKeyText setSearcTextResCount(int searcTextResCount) {
        mSearcTextResCount = searcTextResCount;
        return this;
    }

    public List<ChapterSearchRes> getChapterList() {
        return mChapterList;
    }

    public SearchKeyText setChapterList(List<ChapterSearchRes> chapterList) {
        mChapterList = chapterList;
        return this;
    }

    private String mBookCaseName ;
    private int mSearcTextResCount ;

   private List<ChapterSearchRes> mChapterList;
}
