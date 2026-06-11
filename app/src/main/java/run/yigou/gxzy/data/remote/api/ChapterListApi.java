
package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 *  ???:  1.0
 *  ???:
 *
 */
public final class ChapterListApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetBookChapter";
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
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
