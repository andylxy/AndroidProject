package run.yigou.gxzy.http.entitymodel;

import java.io.Serializable;

/**
 * Author: Xavier
 * Created on 2023/6/26 16:02
 * Email:
 * Desc:
 */
public class ChapterList implements Serializable {
   private int Id ;
   private String mTitle;

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
}
