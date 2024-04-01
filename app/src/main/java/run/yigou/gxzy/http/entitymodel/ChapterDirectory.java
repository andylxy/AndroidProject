package run.yigou.gxzy.http.entitymodel;

import java.util.List;

/**
 * Author: Xavier
 * Created on 2023/6/26 16:01
 * Email:
 * Desc:
 */
public class ChapterDirectory extends ChapterList {
  private  int  mBookId;
  private String  mComment ;
    private String mParentId;
    private String mTitleColor;


    public String getTitleColor() {
        return mTitleColor;
    }

    public ChapterDirectory setTitleColor(String titleColor) {
        mTitleColor = titleColor;
        return this;
    }

    public ChapterList setParentId(String parentId) {
        mParentId = parentId;
        return this;
    }

    public String getParentId() {
        return mParentId;
    }
  private List<ChapterList> mChapterLists;

    public int getBookId() {
        return mBookId;
    }

    public void setBookId(int bookId) {
        mBookId = bookId;
    }

    public String getComment() {
        return mComment;
    }

    public void setComment(String comment) {
        mComment = comment;
    }

    public List<ChapterList> getChapterLists() {
        return mChapterLists;
    }

    public void setChapterLists(List<ChapterList> chapterLists) {
        mChapterLists = chapterLists;
    }
}
