package run.yigou.gxzy.greendao.entity;



import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;
import org.greenrobot.greendao.annotation.Transient;

import java.io.Serializable;

/**
 * 书
 * Created by zhao on 2017/7/24.
 */

@Entity
public class Chapter implements Serializable {
    @Transient
    private static final long serialVersionUID = 1525487L;
    @Id(autoincrement = true)
    private Long  id ;
    // 书籍id
    private int bookId;
    // 章节序号
    private int chapterSection;
    // 章节标题
    private String chapterHeader;
    // 书签id
    private Long signatureId;
    // 是否下载
    private boolean isDownload=false;



    @Generated(hash = 443104719)
    public Chapter(Long id, int bookId, int chapterSection, String chapterHeader,
            Long signatureId, boolean isDownload) {
        this.id = id;
        this.bookId = bookId;
        this.chapterSection = chapterSection;
        this.chapterHeader = chapterHeader;
        this.signatureId = signatureId;
        this.isDownload = isDownload;
    }
    @Generated(hash = 393170288)
    public Chapter() {
    }



    public Long getId() {
        return this.id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public int getBookId() {
        return this.bookId;
    }
    public void setBookId(int bookId) {
        this.bookId = bookId;
    }
    public int getChapterSection() {
        return this.chapterSection;
    }
    public void setChapterSection(int chapterSection) {
        this.chapterSection = chapterSection;
    }
    public String getChapterHeader() {
        return this.chapterHeader;
    }
    public void setChapterHeader(String chapterHeader) {
        this.chapterHeader = chapterHeader;
    }

    public boolean getIsDownload() {
        return this.isDownload;
    }
    public void setIsDownload(boolean isDownload) {
        this.isDownload = isDownload;
    }
    public Long getSignatureId() {
        return this.signatureId;
    }
    public void setSignatureId(Long signatureId) {
        this.signatureId = signatureId;
    }


}
