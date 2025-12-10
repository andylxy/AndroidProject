/*
 * 项目名: AndroidProject
 * 类名: ItemData.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.model
 * 作者 : AI Refactor
 * 创建时间 : 2025年12月10日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.model;

import android.text.SpannableStringBuilder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 子项数据模型 - 全新设计,不依赖旧的ChildEntity
 * 
 * 职责:
 * - 存储子项的三段式内容(正文/笺注/视频)
 * - 存储对应的富文本版本
 * - 提供不可变访问接口
 */
public class ItemData {
    
    private final String text;                           // 正文文本
    private final String note;                           // 笺注文本
    private final String videoUrl;                       // 视频URL
    private final String imageUrl;                       // 图片URL
    
    private final SpannableStringBuilder textSpan;       // 富文本正文
    private final SpannableStringBuilder noteSpan;       // 富文本笺注
    private final SpannableStringBuilder videoSpan;      // 富文本视频标签
    
    /**
     * 构造函数 - 仅使用纯文本
     */
    public ItemData(@NonNull String text, 
                    @Nullable String note, 
                    @Nullable String videoUrl) {
        this(text, note, videoUrl, null, null, null, null);
    }
    
    /**
     * 构造函数 - 包含图片URL
     */
    public ItemData(@NonNull String text, 
                    @Nullable String note, 
                    @Nullable String videoUrl,
                    @Nullable String imageUrl) {
        this(text, note, videoUrl, imageUrl, null, null, null);
    }
    
    /**
     * 完整构造函数 - 包含富文本版本
     */
    public ItemData(@NonNull String text, 
                    @Nullable String note, 
                    @Nullable String videoUrl,
                    @Nullable String imageUrl,
                    @Nullable SpannableStringBuilder textSpan,
                    @Nullable SpannableStringBuilder noteSpan,
                    @Nullable SpannableStringBuilder videoSpan) {
        this.text = text;
        this.note = note;
        this.videoUrl = videoUrl;
        this.imageUrl = imageUrl;
        this.textSpan = textSpan;
        this.noteSpan = noteSpan;
        this.videoSpan = videoSpan;
    }
    
    /**
     * 获取正文文本
     */
    @NonNull
    public String getText() {
        return text;
    }
    
    /**
     * 获取笺注文本
     */
    @Nullable
    public String getNote() {
        return note;
    }
    
    /**
     * 获取视频URL
     */
    @Nullable
    public String getVideoUrl() {
        return videoUrl;
    }
    
    /**
     * 获取图片URL
     */
    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }
    
    /**
     * 获取富文本正文
     */
    @Nullable
    public SpannableStringBuilder getTextSpan() {
        return textSpan;
    }
    
    /**
     * 获取富文本笺注
     */
    @Nullable
    public SpannableStringBuilder getNoteSpan() {
        return noteSpan;
    }
    
    /**
     * 获取富文本视频标签
     */
    @Nullable
    public SpannableStringBuilder getVideoSpan() {
        return videoSpan;
    }
    
    /**
     * 判断是否有笺注
     */
    public boolean hasNote() {
        return note != null && !note.isEmpty();
    }
    
    /**
     * 判断是否有视频
     */
    public boolean hasVideo() {
        return videoUrl != null && !videoUrl.isEmpty();
    }
    
    /**
     * 判断是否有图片
     */
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }
    
    /**
     * 判断是否有富文本正文
     */
    public boolean hasTextSpan() {
        return textSpan != null;
    }
    
    /**
     * 判断是否有富文本笺注
     */
    public boolean hasNoteSpan() {
        return noteSpan != null;
    }
    
    /**
     * 判断是否有富文本视频标签
     */
    public boolean hasVideoSpan() {
        return videoSpan != null;
    }
    
    @Override
    public String toString() {
        return "ItemData{" +
                "text='" + (text.length() > 20 ? text.substring(0, 20) + "..." : text) + "'" +
                ", hasNote=" + hasNote() +
                ", hasVideo=" + hasVideo() +
                ", hasImage=" + hasImage() +
                "}";
    }
}
