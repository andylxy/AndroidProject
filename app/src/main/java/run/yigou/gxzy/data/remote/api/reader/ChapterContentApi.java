
package run.yigou.gxzy.data.remote.api.reader;

import com.hjq.http.config.IRequestApi;

/**
 *  ???:  1.0
 *  ???:
 *
 */
public final class ChapterContentApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetChapterContent";
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }
    public int getContentId() {
        return contentId;
    }

    public ChapterContentApi setContentId(int contentId) {
        this.contentId = contentId;
        return this;
    }
    private String signatureId;

    public String getSignatureId() {
        return signatureId;
    }

    public ChapterContentApi setSignatureId(String signatureId) {
        this.signatureId = signatureId;
        return this;
    }
    private String bookId;
    private int contentId;

    public String getBookId() {
        return bookId;
    }

    public ChapterContentApi setBookId(String bookId) {
        this.bookId = bookId;
        return this;
    }
}
