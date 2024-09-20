/*
 * 项目名: AndroidProject
 * 类名: ChildEntity.java
 * 包名: run.yigou.gxzy.ui.tips.entity.ChildEntity
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:24:18
 * 上次修改时间: 2024年09月08日 11:56:47
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.entity;

import android.text.SpannableStringBuilder;

/**
 * 子项数据的实体类
 */
public class ChildEntity {

    private String child_sectiontext;
    private String child_sectionnote;
    private String child_sectionvideo;
    private SpannableStringBuilder spannableChild;
    public ChildEntity(String child_sectiontext) {
        this.child_sectiontext = child_sectiontext;
    }
    public ChildEntity(String child_sectiontext, String child_sectionnote, String child_sectionvideo, SpannableStringBuilder spannableChild) {
        this.child_sectiontext = child_sectiontext;
        this.child_sectionnote = child_sectionnote;
        this.child_sectionvideo = child_sectionvideo;
        this.spannableChild = spannableChild;
    }
    public ChildEntity(String child_sectiontext, SpannableStringBuilder spannableChild) {
        this.child_sectiontext = child_sectiontext;
        this.spannableChild = spannableChild;
    }

    public SpannableStringBuilder getSpannableChild() {
        return spannableChild;
    }

    public void setSpannableChild(SpannableStringBuilder spannableChild) {
        this.spannableChild = spannableChild;
    }

    public String getChild_sectiontext() {
        return child_sectiontext;
    }

    public void setChild_sectiontext(String child_sectiontext) {
        this.child_sectiontext = child_sectiontext;
    }

    public String getChild_sectionnote() {
        return child_sectionnote;
    }

    public void setChild_sectionnote(String child_sectionnote) {
        this.child_sectionnote = child_sectionnote;
    }

    public String getChild_sectionvideo() {
        return child_sectionvideo;
    }

    public void setChild_sectionvideo(String child_sectionvideo) {
        this.child_sectionvideo = child_sectionvideo;
    }
}
