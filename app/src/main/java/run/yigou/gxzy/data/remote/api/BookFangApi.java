

package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.model.HH2SectionData;




public final class BookFangApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetBookIdFang";
    }

    public int getBookId() {
        return bookId;
    }

    public BookFangApi setBookId(int bookId) {
        this.bookId = bookId;
        return this;
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }
    private int bookId;
    public final static class Bean extends HH2SectionData{

    }
}
