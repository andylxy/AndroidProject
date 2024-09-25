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

import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Yao;

public class DataItem {
    /**
     * 记录序号
     */
    private int ID;
    private SpannableStringBuilder attributedText;
    private List<String> fangList;
    /**
     * 内容
     */
    private String text;
    private List<String> yaoList;
    /**
     * 内容注解
     */
    private String note;
    /**
     * 内容笔记
     */
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

    // 空值检查后设置 pureText
    if (this.text != null) {
        dataItem.setPureText(this.text);
    }

    // 空值检查后设置 attributedText
    if (this.attributedText != null) {
        dataItem.setAttributedText(new SpannableStringBuilder(this.attributedText));
    }

    // 深拷贝 fangList
    if (this.fangList != null) {
        List<String> fangListCopy = new ArrayList<>(this.fangList);
        dataItem.setFangList(fangListCopy);
    }

    // 深拷贝 yaoList
    if (this.yaoList != null) {
        List<String> yaoListCopy = new ArrayList<>(this.yaoList);
        dataItem.setYaoList(yaoListCopy);
    }

    // 空值检查后设置 note
    if (this.note != null) {
        dataItem.setNote(this.note);
    }

    // 空值检查后设置 sectionvideo
    if (this.sectionvideo != null) {
        dataItem.setSectionvideo(this.sectionvideo);
    }

    // 空值检查后设置 attributedNote
    if (this.attributedNote != null) {
        dataItem.setAttributedNote(new SpannableStringBuilder(this.attributedNote));
    }

    // 空值检查后设置 attributedSectionVideo
    if (this.attributedSectionVideo != null) {
        dataItem.setAttributedSectionVideo(new SpannableStringBuilder(this.attributedSectionVideo));
    }

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
        return this.fangList == null ? new ArrayList<>() : this.fangList;
    }

    public List<String> getYaoList() {
        return this.yaoList == null ? new ArrayList<>() : this.yaoList;
    }

    protected int getItemIndex(String str) {
        return Integer.parseInt(str.substring(0, str.indexOf("、"))) - 1;
    }

//    public static String[] getFangNameList(String str) {
//        ArrayList<Integer> allSubStringPos = TipsNetHelper.getAllSubStringPos(str, "$f");
//        String[] strArr = new String[allSubStringPos.size()];
//        Iterator<Integer> it = allSubStringPos.iterator();
//        int i = 0;
//        while (it.hasNext()) {
//            int intValue = it.next().intValue();
//            strArr[i] = str.substring(intValue + 3, intValue + str.substring(intValue).indexOf("}"));
//            i++;
//        }
//        return strArr;
//    }
//
//    public static String[] getYaoNameList(String str) {
//        ArrayList<Integer> allSubStringPos = TipsNetHelper.getAllSubStringPos(str, "$u");
//        String[] strArr = new String[allSubStringPos.size()];
//        Iterator<Integer> it = allSubStringPos.iterator();
//        int i = 0;
//        while (it.hasNext()) {
//            int intValue = it.next().intValue();
//            strArr[i] = str.substring(intValue + 3, intValue + str.substring(intValue).indexOf("}"));
//            i++;
//        }
//        return strArr;
//    }
}
