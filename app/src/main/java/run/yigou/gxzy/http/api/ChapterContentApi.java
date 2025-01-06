
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
        return "BookChapter/GetChapterContent";
    }

    public int getContentId() {
        return contentId;
    }

    public ChapterContentApi setContentId(int contentId) {
        this.contentId = contentId;
        return this;
    }
    private Long sgnatureId;

    public Long getSgnatureId() {
        return sgnatureId;
    }

    public ChapterContentApi setSgnatureId(Long sgnatureId) {
        this.sgnatureId = sgnatureId;
        return this;
    }

    private int contentId;

}