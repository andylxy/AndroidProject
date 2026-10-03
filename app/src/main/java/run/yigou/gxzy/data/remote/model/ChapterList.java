package run.yigou.gxzy.data.remote.model;

import java.io.Serializable;

/**
 * Author: Xavier
 * Created on 2023/6/26 16:02
 * Email:
 * Desc:
 */
public class ChapterList implements Serializable {
   private String Id ;
   private String mTitle;
    private String mNo ;

    public String getNo() {
        return mNo;
    }

    public ChapterList setNo(String no) {
        mNo = no;
        return this;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }
}
