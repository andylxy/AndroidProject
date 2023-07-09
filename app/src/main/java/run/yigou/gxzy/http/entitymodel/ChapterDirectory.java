package run.yigou.gxzy.http.entitymodel;

import java.util.List;

/**
 * Author: Xavier
 * Created on 2023/6/26 16:01
 * Email:
 * Desc:
 */
public class ChapterDirectory extends ChapterList {
  private  int  BookId;
  private String  Comment ;
  private List<ChapterList>  ChapterList;

    public int getBookId() {
        return BookId;
    }

    public void setBookId(int bookId) {
        BookId = bookId;
    }

    public String getComment() {
        return Comment;
    }

    public void setComment(String comment) {
        Comment = comment;
    }

    public List<ChapterList> getChapterList() {
        return ChapterList;
    }

    public void setChapterList(List<ChapterList> chapterList) {
        ChapterList = chapterList;
    }
}
