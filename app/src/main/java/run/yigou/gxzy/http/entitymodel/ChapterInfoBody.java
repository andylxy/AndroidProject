package run.yigou.gxzy.http.entitymodel;

import java.io.Serializable;

/**
 * 作者:  zhs
 * 时间:  2023-07-10 11:47:44
 * 包名:  run.yigou.gxzy.http.entitymodel
 * 类名:  ChapterInfoBody
 * 版本:  1.0
 * 描述:
 */
public class ChapterInfoBody implements Serializable {
    private static final long serialVersionUID = -3499982078639650258L;

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
    private int mNo;

    public int getNo() {
        return mNo;
    }

    private String mSection;
    private String mSectionNote;//原文注解

    private String mSectionVideoMemo;//视频原文讲解

    private String mFangJi;//方剂

    private String mFangJiZhujie;//方剂注解

    private String mSectionVideoUrl;//原文视频地址

    public String getSection() {
        return mSection;
    }

    public String getSectionNote() {
        return mSectionNote;
    }

    public ChapterInfoBody setSectionNote(String sectionNote) {
        mSectionNote = sectionNote;
        return this;
    }

    public String getSectionVideoMemo() {
        return mSectionVideoMemo;
    }

    public ChapterInfoBody setSectionVideoMemo(String sectionVideoMemo) {
        mSectionVideoMemo = sectionVideoMemo;
        return this;
    }

    public String getFangJi() {
        return mFangJi;
    }

    public ChapterInfoBody setFangJi(String fangJi) {
        mFangJi = fangJi;
        return this;
    }

    public String getFangJiZhujie() {
        return mFangJiZhujie;
    }

    public ChapterInfoBody setFangJiZhujie(String fangJiZhujie) {
        mFangJiZhujie = fangJiZhujie;
        return this;
    }

    public String getSectionVideoUrl() {
        return mSectionVideoUrl;
    }

    public ChapterInfoBody setSectionVideoUrl(String sectionVideoUrl) {
        mSectionVideoUrl = sectionVideoUrl;
        return this;
    }


    public String getBieMing() {
        return BieMing;
    }

    public void setBieMing(String bieMing) {
        BieMing = bieMing;
    }

    public String getJingMai() {
        return JingMai;
    }

    public void setJingMai(String jingMai) {
        JingMai = jingMai;
    }

    public String getTeDian() {
        return TeDian;
    }

    public void setTeDian(String teDian) {
        TeDian = teDian;
    }

    public String getShiCi() {
        return ShiCi;
    }

    public void setShiCi(String shiCi) {
        ShiCi = shiCi;
    }

    public String getShiYi() {
        return ShiYi;
    }

    public void setShiYi(String shiYi) {
        ShiYi = shiYi;
    }

    public String getNotes() {
        return Notes;
    }

    public void setNotes(String notes) {
        Notes = notes;
    }

    public String getZhuZhi() {
        return ZhuZhi;
    }

    public void setZhuZhi(String zhuZhi) {
        ZhuZhi = zhuZhi;
    }

    public String getZhenJiu() {
        return ZhenJiu;
    }

    public void setZhenJiu(String zhenJiu) {
        ZhenJiu = zhenJiu;
    }

    public String getAiJiu() {
        return AiJiu;
    }

    public void setAiJiu(String aiJiu) {
        AiJiu = aiJiu;
    }

    public String getPeiWu() {
        return PeiWu;
    }

    public void setPeiWu(String peiWu) {
        PeiWu = peiWu;
    }


}
