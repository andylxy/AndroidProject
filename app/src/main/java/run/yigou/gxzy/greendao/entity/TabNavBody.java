package run.yigou.gxzy.greendao.entity;

import com.google.gson.annotations.SerializedName;
import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Generated;

import java.io.Serializable;

@Entity
public class TabNavBody  implements Serializable {
    private static final long serialVersionUID = 555454L;
    @Id
    private String tabNavBodyId;
    private String tabNavId;
    private Long Id;
    private int bookNo;
    private String imageUrl;
    private String bookName;
    private String chengShu;
    private String author;
    private String desc;
    private int caseTag;
    private int chapterCount;
    @Generated(hash = 528953577)
    public TabNavBody(String tabNavBodyId, String tabNavId, Long Id, int bookNo,
            String imageUrl, String bookName, String chengShu, String author,
            String desc, int caseTag, int chapterCount) {
        this.tabNavBodyId = tabNavBodyId;
        this.tabNavId = tabNavId;
        this.Id = Id;
        this.bookNo = bookNo;
        this.imageUrl = imageUrl;
        this.bookName = bookName;
        this.chengShu = chengShu;
        this.author = author;
        this.desc = desc;
        this.caseTag = caseTag;
        this.chapterCount = chapterCount;
    }
    @Generated(hash = 1918260576)
    public TabNavBody() {
    }

    /**
    *
    * 1	针灸
    * 2	内经
    * 3	本草
    *
    */
    public int getCaseTag() {
        return caseTag;
    }

    public void setCaseTag(int caseTag) {
        this.caseTag = caseTag;
    }

    public String getTabNavBodyId() {
        return this.tabNavBodyId;
    }
    public void setTabNavBodyId(String tabNavBodyId) {
        this.tabNavBodyId = tabNavBodyId;
    }
    public String getTabNavId() {
        return this.tabNavId;
    }
    public void setTabNavId(String tabNavId) {
        this.tabNavId = tabNavId;
    }
    public Long getId() {
        return this.Id;
    }
    public void setId(Long Id) {
        this.Id = Id;
    }
    public int getBookNo() {
        return this.bookNo;
    }
    public void setBookNo(int bookNo) {
        this.bookNo = bookNo;
    }
    public String getImageUrl() {
        return this.imageUrl;
    }
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    public String getBookName() {
        return this.bookName;
    }
    public void setBookName(String bookName) {
        this.bookName = bookName;
    }
    public String getChengShu() {
        return this.chengShu;
    }
    public void setChengShu(String chengShu) {
        this.chengShu = chengShu;
    }
    public String getAuthor() {
        return this.author;
    }
    public void setAuthor(String author) {
        this.author = author;
    }
    public String getDesc() {
        return this.desc;
    }
    public void setDesc(String desc) {
        this.desc = desc;
    }
    public int getChapterCount() {
        return this.chapterCount;
    }
    public void setChapterCount(int chapterCount) {
        this.chapterCount = chapterCount;
    }

}