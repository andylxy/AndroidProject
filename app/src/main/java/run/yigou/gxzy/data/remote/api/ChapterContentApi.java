
package run.yigou.gxzy.data.remote.api;

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
    private Long signatureId;

    public Long getSignatureId() {
        return signatureId;
    }

    public ChapterContentApi setSignatureId(Long signatureId) {
        this.signatureId = signatureId;
        return this;
    }
    private int bookId;
    private int contentId;

    public int getBookId() {
        return bookId;
    }

    public ChapterContentApi setBookId(int bookId) {
        this.bookId = bookId;
        return this;
    }
}
