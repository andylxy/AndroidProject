
package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

/**
 *  版本:  1.0
 *  描述:
 *
 */
public final class ChapterListApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetBookChapter";
    }
    public int getBookId() {
        return bookId;
    }
    public ChapterListApi setBookId(int bookId) {
        this.bookId = bookId;
        return this;
    }
    private int bookId;

}