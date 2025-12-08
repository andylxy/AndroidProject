
package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

/**
 *  版本:  1.0
 *  描述:
 *
 */
public final class ChapterContentApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetChapterContent";
    }
    // 明确指定请求方法
    public String getMethod() {
        return "GET"; // 或 "POST", "PUT" 等
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