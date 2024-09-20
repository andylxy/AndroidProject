/*
 * 项目名: AndroidProject
 * 类名: Item.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Item
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:38
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils.DataBeans;


import android.text.SpannableStringBuilder;

import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;

public class ShuContent extends DataItem {
    private String note;
    private String sectionvideo;
    private SpannableStringBuilder attributedNote;
    private SpannableStringBuilder attributedSectionVideo;

    public SpannableStringBuilder getAttributedSectionVideo() {
        if (this.attributedSectionVideo != null) {
            return this.attributedSectionVideo;
        }
        if (this.sectionvideo!=null)
            this.attributedSectionVideo = TipsNetHelper.renderText(this.sectionvideo);
        else  return null;
        return attributedSectionVideo;
    }

    public void setAttributedSectionVideo(SpannableStringBuilder attributedSectionVideo) {
        this.attributedSectionVideo = attributedSectionVideo;
    }

    public SpannableStringBuilder getAttributedNote() {
        if (this.attributedNote != null) {
            return this.attributedNote;
        }
        if (this.note!=null)
            this.attributedNote = TipsNetHelper.renderText(this.note);
        else  return null;
        return attributedNote;
    }

    public void setAttributedNote(SpannableStringBuilder attributedNote) {
        this.attributedNote = attributedNote;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
        this.attributedNote = TipsNetHelper.renderText(this.note);
    }

    public String getSectionvideo() {
        return sectionvideo;
    }

    public void setSectionvideo(String sectionvideo) {
        this.sectionvideo = sectionvideo;
        this.attributedSectionVideo = TipsNetHelper.renderText(this.sectionvideo);
    }
}
