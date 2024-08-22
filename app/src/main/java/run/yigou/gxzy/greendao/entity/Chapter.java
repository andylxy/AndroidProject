package run.yigou.gxzy.greendao.entity;


import org.greenrobot.greendao.annotation.Entity;
import org.greenrobot.greendao.annotation.Generated;
import org.greenrobot.greendao.annotation.Id;

import run.yigou.gxzy.http.entitymodel.ChapterList;

/**
 * 章节
 * Created by zhao on 2017/7/24.
 */

@Entity
public class Chapter {
    @Id
    private String id;

    private String bookId;//章节所属书的ID
    private int number;//章节序号
    private String title;//章节标题
    private String url;//章节链接
    private String parentId; //卷章
    private String mTitleColor;
    private String mSection;//原文
    private String mNo ;

    private String mSectionNote;//原文注解

    private String mSectionVideoMemo;//视频原文讲解
    private String mFangJi;//方剂

    private String mFangJiZhujie;//方剂注解

    private String mSectionVideoUrl;//原文视频地址
    private String BieMing;
    private String JingMai;
    private String ZhuZhi;
    private String ZhenJiu;
    private String AiJiu;
    private String PeiWu;
    private String TeDian;
    private String ShiCi;
    private String ShiYi;
    private String Notes;
    @Generated(hash = 235744334)
    public Chapter(String id, String bookId, int number, String title, String url,
            String parentId, String mTitleColor, String mSection, String mNo,
            String mSectionNote, String mSectionVideoMemo, String mFangJi,
            String mFangJiZhujie, String mSectionVideoUrl, String BieMing,
            String JingMai, String ZhuZhi, String ZhenJiu, String AiJiu,
            String PeiWu, String TeDian, String ShiCi, String ShiYi, String Notes) {
        this.id = id;
        this.bookId = bookId;
        this.number = number;
        this.title = title;
        this.url = url;
        this.parentId = parentId;
        this.mTitleColor = mTitleColor;
        this.mSection = mSection;
        this.mNo = mNo;
        this.mSectionNote = mSectionNote;
        this.mSectionVideoMemo = mSectionVideoMemo;
        this.mFangJi = mFangJi;
        this.mFangJiZhujie = mFangJiZhujie;
        this.mSectionVideoUrl = mSectionVideoUrl;
        this.BieMing = BieMing;
        this.JingMai = JingMai;
        this.ZhuZhi = ZhuZhi;
        this.ZhenJiu = ZhenJiu;
        this.AiJiu = AiJiu;
        this.PeiWu = PeiWu;
        this.TeDian = TeDian;
        this.ShiCi = ShiCi;
        this.ShiYi = ShiYi;
        this.Notes = Notes;
    }
    @Generated(hash = 393170288)
    public Chapter() {
    }
    public String getId() {
        return this.id;
    }
    public void setId(String id) {
        this.id = id;
    }
    public String getBookId() {
        return this.bookId;
    }
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    public int getNumber() {
        return this.number;
    }
    public void setNumber(int number) {
        this.number = number;
    }
    public String getTitle() {
        return this.title;
    }
    public void setTitle(String title) {
        this.title = title;
    }
    public String getUrl() {
        return this.url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getParentId() {
        return this.parentId;
    }
    public void setParentId(String parentId) {
        this.parentId = parentId;
    }
    public String getMTitleColor() {
        return this.mTitleColor;
    }
    public void setMTitleColor(String mTitleColor) {
        this.mTitleColor = mTitleColor;
    }
    public String getMSection() {
        return this.mSection;
    }
    public void setMSection(String mSection) {
        this.mSection = mSection;
    }
    public String getMNo() {
        return this.mNo;
    }
    public void setMNo(String mNo) {
        this.mNo = mNo;
    }
    public String getMSectionNote() {
        return this.mSectionNote;
    }
    public void setMSectionNote(String mSectionNote) {
        this.mSectionNote = mSectionNote;
    }
    public String getMSectionVideoMemo() {
        return this.mSectionVideoMemo;
    }
    public void setMSectionVideoMemo(String mSectionVideoMemo) {
        this.mSectionVideoMemo = mSectionVideoMemo;
    }
    public String getMFangJi() {
        return this.mFangJi;
    }
    public void setMFangJi(String mFangJi) {
        this.mFangJi = mFangJi;
    }
    public String getMFangJiZhujie() {
        return this.mFangJiZhujie;
    }
    public void setMFangJiZhujie(String mFangJiZhujie) {
        this.mFangJiZhujie = mFangJiZhujie;
    }
    public String getMSectionVideoUrl() {
        return this.mSectionVideoUrl;
    }
    public void setMSectionVideoUrl(String mSectionVideoUrl) {
        this.mSectionVideoUrl = mSectionVideoUrl;
    }
    public String getBieMing() {
        return this.BieMing;
    }
    public void setBieMing(String BieMing) {
        this.BieMing = BieMing;
    }
    public String getJingMai() {
        return this.JingMai;
    }
    public void setJingMai(String JingMai) {
        this.JingMai = JingMai;
    }
    public String getZhuZhi() {
        return this.ZhuZhi;
    }
    public void setZhuZhi(String ZhuZhi) {
        this.ZhuZhi = ZhuZhi;
    }
    public String getZhenJiu() {
        return this.ZhenJiu;
    }
    public void setZhenJiu(String ZhenJiu) {
        this.ZhenJiu = ZhenJiu;
    }
    public String getAiJiu() {
        return this.AiJiu;
    }
    public void setAiJiu(String AiJiu) {
        this.AiJiu = AiJiu;
    }
    public String getPeiWu() {
        return this.PeiWu;
    }
    public void setPeiWu(String PeiWu) {
        this.PeiWu = PeiWu;
    }
    public String getTeDian() {
        return this.TeDian;
    }
    public void setTeDian(String TeDian) {
        this.TeDian = TeDian;
    }
    public String getShiCi() {
        return this.ShiCi;
    }
    public void setShiCi(String ShiCi) {
        this.ShiCi = ShiCi;
    }
    public String getShiYi() {
        return this.ShiYi;
    }
    public void setShiYi(String ShiYi) {
        this.ShiYi = ShiYi;
    }
    public String getNotes() {
        return this.Notes;
    }
    public void setNotes(String Notes) {
        this.Notes = Notes;
    }

}
