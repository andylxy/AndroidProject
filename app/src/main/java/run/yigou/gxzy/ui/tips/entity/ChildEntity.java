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

import com.hjq.http.EasyLog;

import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;

/**
 * 子项数据的实体类
 */
public class ChildEntity {


    private String child_section_text;
    private String child_section_note;
    private String child_section_video;
    private SpannableStringBuilder attributed_child_section_text;
    private SpannableStringBuilder attributed_child_section_note;
    private SpannableStringBuilder attributed_child_section_video;
    private SpannableStringBuilder spannableChild;
    private int groupPosition;

    private String child_section_image;

    public String getChild_section_image() {
        return child_section_image;
    }

    public void setChild_section_image(String child_section_image) {
        this.child_section_image = child_section_image;
    }

    public int getGroupPosition() {
        return groupPosition;
    }

    public void setGroupPosition(int groupPosition) {
        this.groupPosition = groupPosition;
    }
    public ChildEntity() {
    }


    public synchronized SpannableStringBuilder getAttributed_child_section_text() {
        if (attributed_child_section_text == null) {
            if (child_section_text == null) {
                attributed_child_section_text = new SpannableStringBuilder();
            } else {
                attributed_child_section_text = TipsNetHelper.renderText(child_section_text);
            }
        }
        return attributed_child_section_text;
    }

    public void setAttributed_child_section_text(SpannableStringBuilder attributed_child_section_text) {
        this.attributed_child_section_text = attributed_child_section_text;
    }

    public synchronized SpannableStringBuilder getAttributed_child_section_video() {
        if (attributed_child_section_video == null) {
            if (child_section_video != null) {
                attributed_child_section_video = TipsNetHelper.renderText(child_section_video);
            } else {
                // 处理 child_sectionvideo 为 null 的情况
                attributed_child_section_video = new SpannableStringBuilder(); // 或者返回一个默认值
            }
        }
        return attributed_child_section_video;
    }


    public void setAttributed_child_section_video(SpannableStringBuilder attributed_child_section_video) {
        this.attributed_child_section_video = attributed_child_section_video;
    }

    public synchronized SpannableStringBuilder getAttributed_child_section_note() {
        if (attributed_child_section_note == null) {
            if (child_section_note == null) {
                // 防止空指针异常
                attributed_child_section_note = new SpannableStringBuilder();
            } else {
                attributed_child_section_note = TipsNetHelper.renderText(child_section_note);
            }

            // 记录日志
            EasyLog.print("ChildEntity", "attributed_child_sectionnote initialized");
        }

        return attributed_child_section_note;
    }


    public void setAttributed_child_section_note(SpannableStringBuilder attributed_child_section_note) {
        this.attributed_child_section_note = attributed_child_section_note;
    }


    public SpannableStringBuilder getSpannableChild() {
        return spannableChild;
    }

    public void setSpannableChild(SpannableStringBuilder spannableChild) {
        this.spannableChild = spannableChild;
    }

    public String getChild_section_text() {
        return child_section_text;
    }

    public void setChild_section_text(String child_section_text) {
        this.child_section_text = child_section_text;
    }

    public String getChild_section_note() {
        return child_section_note;
    }

    public void setChild_section_note(String child_section_note) {
        this.child_section_note = child_section_note;
    }

    public String getChild_section_video() {
        return child_section_video;
    }

    public void setChild_section_video(String child_section_video) {
        this.child_section_video = child_section_video;
    }
}
