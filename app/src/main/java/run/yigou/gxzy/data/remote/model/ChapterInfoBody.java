package run.yigou.gxzy.data.remote.model;

import java.io.Serializable;

/**
 * ????  zhs
 * ???:  2023-07-10 11:47:44
 * ???:  run.yigou.gxzy.http.model
 * ???:  ChapterInfoBody
 * ???:  1.0
 * ???:
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
    private String mSectionNote;//??????

    private String mSectionVideoMemo;//?????????

    private String mFangJi;//???

    private String mFangJiZhujie;//??????

    private String mSectionVideoUrl;//?????????

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
