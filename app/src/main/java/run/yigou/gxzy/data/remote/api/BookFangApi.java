

package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.data.model.HH2SectionData;




public final class BookFangApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetBookIdFang";
    }

    public String getBookId() {
        return bookId;
    }

    public BookFangApi setBookId(String bookId) {
        this.bookId = bookId;
        return this;
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }
    private String bookId;
    public final static class Bean extends HH2SectionData{

    }
}
