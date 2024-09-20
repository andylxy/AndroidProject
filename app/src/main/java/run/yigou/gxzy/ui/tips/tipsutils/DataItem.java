/*
 * 项目名: AndroidProject
 * 类名: DataItem.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.DataItem
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:38
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils;

import android.text.SpannableStringBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataItem {
    private int ID;
    private SpannableStringBuilder attributedText;
    private List<String> fangList;
    private String text;
    private List<String> yaoList;
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

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public void setText(String str) {
        this.text = str;
        this.attributedText = TipsNetHelper.renderText(this.text);
    }

    public DataItem getCopy() {
        DataItem dataItem = new DataItem();
        dataItem.setPureText(this.text);
        dataItem.setAttributedText(new SpannableStringBuilder(this.attributedText));
        dataItem.setFangList(this.fangList);
        dataItem.setYaoList(this.yaoList);
        if (this.attributedNote != null)
            dataItem.setAttributedNote(new SpannableStringBuilder(this.attributedNote));
        if (this.attributedSectionVideo != null)
            dataItem.setAttributedSectionVideo(new SpannableStringBuilder(this.attributedSectionVideo));
        return dataItem;
    }

    private void setPureText(String str) {
        this.text = str;
    }

    public void setFangList(List<String> list) {
        this.fangList = list;
    }

    public void setYaoList(List<String> list) {
        this.yaoList = list;
    }

    public String getText() {
        return this.text;
    }

    public SpannableStringBuilder getAttributedText() {
        if (this.attributedText != null) {
            return this.attributedText;
        }
        this.attributedText = TipsNetHelper.renderText(this.text);
        return this.attributedText;
    }

    public void setAttributedText(SpannableStringBuilder spannableStringBuilder) {
        this.attributedText = spannableStringBuilder;
    }


    public List<String> getFangList() {
        return this.fangList == null ? new ArrayList() : this.fangList;
    }

    public List<String> getYaoList() {
        return this.yaoList == null ? new ArrayList() : this.yaoList;
    }

    protected int getItemIndex(String str) {
        return Integer.parseInt(str.substring(0, str.indexOf("、"))) - 1;
    }

    public static String[] getFangNameList(String str) {
        ArrayList<Integer> allSubStringPos = TipsNetHelper.getAllSubStringPos(str, "$f");
        String[] strArr = new String[allSubStringPos.size()];
        Iterator<Integer> it = allSubStringPos.iterator();
        int i = 0;
        while (it.hasNext()) {
            int intValue = it.next().intValue();
            strArr[i] = str.substring(intValue + 3, intValue + str.substring(intValue).indexOf("}"));
            i++;
        }
        return strArr;
    }

    public static String[] getYaoNameList(String str) {
        ArrayList<Integer> allSubStringPos = TipsNetHelper.getAllSubStringPos(str, "$u");
        String[] strArr = new String[allSubStringPos.size()];
        Iterator<Integer> it = allSubStringPos.iterator();
        int i = 0;
        while (it.hasNext()) {
            int intValue = it.next().intValue();
            strArr[i] = str.substring(intValue + 3, intValue + str.substring(intValue).indexOf("}"));
            i++;
        }
        return strArr;
    }
}
