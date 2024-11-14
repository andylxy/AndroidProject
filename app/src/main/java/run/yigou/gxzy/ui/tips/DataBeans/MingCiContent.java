/*
 * 项目名: AndroidProject
 * 类名: Item.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Item
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:38
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.DataBeans;


import android.text.SpannableStringBuilder;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.ui.tips.tipsutils.DataItem;

public class MingCiContent extends DataItem {
    /**
     * 名词列表
     */
    private List<String> mingCiList;

    public List<String> getMingCiList() {
        return mingCiList == null ? new ArrayList<>() : this.mingCiList;
    }

    public void setMingCiList(List<String> mingCiList) {
        this.mingCiList = mingCiList;
    }




    @Override
    public MingCiContent getCopy() {
        MingCiContent dataItem = new MingCiContent();

        // 空值检查后设置 pureText
        if (getText() != null) {
            dataItem.setText(this.getText());
        }

        // 空值检查后设置 attributedText
        if (this.getAttributedText() != null) {
            dataItem.setAttributedText(new SpannableStringBuilder(this.getAttributedText()));
        }

        // 深拷贝 fangList
        if (getFangList() != null) {
            List<String> fangListCopy = new ArrayList<>(this.getFangList());
            dataItem.setFangList(fangListCopy);
        }

        // 深拷贝 yaoList
        if (getYaoList() != null) {
            List<String> yaoListCopy = new ArrayList<>(this.getYaoList());
            dataItem.setYaoList(yaoListCopy);
        }

        // 空值检查后设置 note
        if (getNote() != null) {
            dataItem.setNote(this.getNote());
        }

        // 空值检查后设置 sectionvideo
        if (this.getSectionvideo() != null) {
            dataItem.setSectionvideo(this.getSectionvideo());
        }

        // 空值检查后设置 attributedNote
        if (this.getAttributedNote() != null) {
            dataItem.setAttributedNote(new SpannableStringBuilder(this.getAttributedNote()));
        }

        // 空值检查后设置 attributedSectionVideo
        if (this.getAttributedSectionVideo() != null) {
            dataItem.setAttributedSectionVideo(new SpannableStringBuilder(this.getAttributedSectionVideo()));
        }

        // 处理 getName() 可能返回 null 的情况
        String name = getName();
        if (name != null) {
            dataItem.setName(name);
        }

        // 处理 getMingCiList() 可能返回 null 的情况
        List<String> mingCiList = getMingCiList();
        if (mingCiList != null) {
            dataItem.setMingCiList(new ArrayList<>(mingCiList)); // 防止外部修改原始列表
        }

        if (getImageUrl() != null)
            dataItem.setImageUrl(getImageUrl());
        return dataItem;
    }


}
