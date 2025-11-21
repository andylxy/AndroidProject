

package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;




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

    private int bookId;
    public final static class Bean extends HH2SectionData{

    }
}