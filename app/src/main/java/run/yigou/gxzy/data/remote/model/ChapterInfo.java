package run.yigou.gxzy.data.remote.model;

import java.io.Serializable;
import java.util.List;

/**
 * ????  zhs
 * ???:  2023-07-10 11:47:39
 * ???:  run.yigou.gxzy.http.model
 * ???:  ChapterInfo
 * ???:  1.0
 * ???:
 */
public class ChapterInfo implements Serializable {
    private int Id;
    private String mTitle;

    private String mTitleColor;

    public String getTitleColor() {
        return mTitleColor;
    }

    private String mParentId;

    public String getParentId() {
        return mParentId;
    }

    public ChapterInfo setParentId(String parentId) {
        mParentId = parentId;
        return this;
    }


    private List<ChapterInfoBody> ChapterInfoBody;
    private String Creator;

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }


    public List<ChapterInfoBody> getChapterInfoBody() {
        return ChapterInfoBody;
    }

    public void setChapterInfoBody(List<ChapterInfoBody> chapterInfoBody) {
        ChapterInfoBody = chapterInfoBody;
    }

    public String getCreator() {
        return Creator;
    }

    public void setCreator(String creator) {
        Creator = creator;
    }
}
