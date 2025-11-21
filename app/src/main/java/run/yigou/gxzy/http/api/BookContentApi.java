

package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

import java.io.Serializable;
import java.util.List;

import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;



public final class BookContentApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetBookIdContent";
    }

    public int getBookId() {
        return bookId;
    }

    public BookContentApi setBookId(int bookId) {
        this.bookId = bookId;
        return this;
    }

    private int bookId;
    public final static class Bean extends HH2SectionData{

    }
}