package run.yigou.gxzy.greendao.entity;

import org.greenrobot.greendao.annotation.Entity;

import java.io.Serializable;
import org.greenrobot.greendao.annotation.Generated;
@Entity
public class BookChapterBody implements Serializable {
    private static final long serialVersionUID = 4L;

    private String bookChapterBodyId;
    private String bookChapterId;
    private int ID;
    private String text;
    private String note;
    private String sectionvideo;
    private int height;
    private String fangList;

    private long signatureId;
    private String signature;

    @Generated(hash = 861768021)
    public BookChapterBody(String bookChapterBodyId, String bookChapterId, int ID,
            String text, String note, String sectionvideo, int height,
            String fangList, long signatureId, String signature) {
        this.bookChapterBodyId = bookChapterBodyId;
        this.bookChapterId = bookChapterId;
        this.ID = ID;
        this.text = text;
        this.note = note;
        this.sectionvideo = sectionvideo;
        this.height = height;
        this.fangList = fangList;
        this.signatureId = signatureId;
        this.signature = signature;
    }

    @Generated(hash = 1488279804)
    public BookChapterBody() {
    }

    public long getSignatureId() {
        return signatureId;
    }

    public void setSignatureId(long signatureId) {
        this.signatureId = signatureId;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }







    public void setID(int ID) {
        this.ID = ID;
    }

    public int getID() {
        return ID;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getNote() {
        return note;
    }

    public void setSectionvideo(String sectionvideo) {
        this.sectionvideo = sectionvideo;
    }

    public String getSectionvideo() {
        return sectionvideo;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getHeight() {
        return height;
    }



    public String getFangList() {
        return this.fangList;
    }

    public void setFangList(String fangList) {
        this.fangList = fangList;
    }

    public String getBookChapterBodyId() {
        return this.bookChapterBodyId;
    }

    public void setBookChapterBodyId(String bookChapterBodyId) {
        this.bookChapterBodyId = bookChapterBodyId;
    }

    public String getBookChapterId() {
        return this.bookChapterId;
    }

    public void setBookChapterId(String bookChapterId) {
        this.bookChapterId = bookChapterId;
    }




}
