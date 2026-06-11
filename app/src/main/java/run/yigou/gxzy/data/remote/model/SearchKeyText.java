/*
 * ????? AndroidProject
 * ???: SearchKeyText.java
 * ???: run.yigou.gxzy.http.model.SearchKeyText
 * ????: Zhs (xiaoyang_02@qq.com)
 * ????????? : 2024??3??7??23:00:37
 * ?????????: 2024??3??7??23:00:37
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.data.remote.model;

import java.io.Serializable;
import java.util.List;

/**
 * ???:  1.0
 * ???:
 */
public class SearchKeyText implements Serializable {
    public String getBookCaseName() {
        return mBookCaseName;
    }

    public SearchKeyText setBookCaseName(String bookCaseName) {
        mBookCaseName = bookCaseName;
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

    public int getSearchTextResCount() {
        return mSearcTextResCount;
    }

    public SearchKeyText setSearchTextResCount(int searchTextResCount) {
        mSearcTextResCount = searchTextResCount;
        return this;
    }

    private int mSearcTextResCount ;

   private List<ChapterSearchRes> mChapterList;
}
