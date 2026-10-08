/*
 * 项目名: AndroidProject
 * 类名: ItemData.java
 * 包名: run.yigou.gxzy.ui.reader.adapter.model
 * 作者 : AI Refactor
 * 创建时间 : 2025年12月10日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.adapter.model;

import android.text.SpannableStringBuilder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 子项数据模型 —— 全局唯一的 ItemData 类。
 *
 * <p>由 Q7C 合并自「新架构 ItemData」（AI Refactor 2025-12-10）与「旧 entity.ItemData」：
 * 保留新架构的 text/note/videoUrl/imageUrl + textSpan/noteSpan/videoSpan，
 * 从旧类并入 groupPosition，合并后旧 `ui/reader/entity/ItemData` 已删除。
 *
 * <p>关于可变性：
 * <ul>
 *   <li>内容字段（text/note/videoUrl/imageUrl）一律 final，构造后只读；</li>
 *   <li>富文本字段（textSpan/noteSpan/videoSpan）<b>未</b>声明 final，
 *       是为支持 SearchCoordinator/SearchResultBuilder 里「{@code new ItemData()} + 多次 set 构建高亮 Span」
 *       的既有调用模式。调用方必须传入独立 Span 对象，<b>不得原地修改 Span 内容</b>
 *       （否则跨引用同步影响所有持有者）；</li>
 *   <li>{@code groupPosition} 由数据装载方在构造后绑定，允许 setter。</li>
 * </ul>
 *
 * <p>富文本字段通过 canonical {@code getTextSpan}/{@code setTextSpan}、
 * {@code getNoteSpan}/{@code setNoteSpan}、{@code getVideoSpan}/{@code setVideoSpan} 访问；
 * 历史 {@code attributedText/Note/Video} 兼容别名与 no-op {@code setImageUrl} 已随 Q7C 后续清理移除
 * （调用点已改为直接使用 {@code XSpan} API，语义等价）。
 */
public class ItemData {

    private final String text;                           // 正文文本
    private final String note;                           // 笺注文本
    private final String videoUrl;                       // 视频 URL
    private final String imageUrl;                       // 图片 URL

    // 富文本字段：未 final 化，为支持既有「new + set」构建模式
    private SpannableStringBuilder textSpan;             // 富文本正文
    private SpannableStringBuilder noteSpan;             // 富文本笺注
    private SpannableStringBuilder videoSpan;            // 富文本视频标签

    /** 所属分组下标（由数据装载方在构造后绑定，合并自旧 entity.ItemData.groupPosition） */
    private int groupPosition;

    /** 无参构造（合并自旧 entity.ItemData，供 SearchCoordinator 等「new + 多次 set」模式使用） */
    public ItemData() {
        this("", null, null, null, null, null, null);
    }

    /** 构造函数 —— 仅使用纯文本 */
    public ItemData(@NonNull String text,
                    @Nullable String note,
                    @Nullable String videoUrl) {
        this(text, note, videoUrl, null, null, null, null);
    }

    /** 构造函数 —— 包含图片 URL */
    public ItemData(@NonNull String text,
                    @Nullable String note,
                    @Nullable String videoUrl,
                    @Nullable String imageUrl) {
        this(text, note, videoUrl, imageUrl, null, null, null);
    }

    /** 完整构造函数 —— 包含富文本版本 */
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
        this.groupPosition = 0;
    }

    /** 获取正文文本 */
    @NonNull
    public String getText() {
        return text;
    }

    /** 获取笺注文本 */
    @Nullable
    public String getNote() {
        return note;
    }

    /** 获取视频 URL */
    @Nullable
    public String getVideoUrl() {
        return videoUrl;
    }

    /** 获取图片 URL */
    @Nullable
    public String getImageUrl() {
        return imageUrl;
    }

    /** 获取富文本正文 */
    @Nullable
    public SpannableStringBuilder getTextSpan() {
        return textSpan;
    }

    /** 获取富文本笺注 */
    @Nullable
    public SpannableStringBuilder getNoteSpan() {
        return noteSpan;
    }

    /** 获取富文本视频标签 */
    @Nullable
    public SpannableStringBuilder getVideoSpan() {
        return videoSpan;
    }

    /** 设置富文本正文（引用替换；禁止原地修改传入的 Span 内容） */
    public void setTextSpan(@Nullable SpannableStringBuilder textSpan) {
        this.textSpan = textSpan;
    }

    /** 设置富文本笺注 */
    public void setNoteSpan(@Nullable SpannableStringBuilder noteSpan) {
        this.noteSpan = noteSpan;
    }

    /** 设置富文本视频标签 */
    public void setVideoSpan(@Nullable SpannableStringBuilder videoSpan) {
        this.videoSpan = videoSpan;
    }


    /** 获取所属分组下标 */
    public int getGroupPosition() {
        return groupPosition;
    }

    /** 设置所属分组下标（由数据装载方调用） */
    public void setGroupPosition(int groupPosition) {
        this.groupPosition = groupPosition;
    }

    /** 判断是否有笺注（纯文本版） */
    public boolean hasNote() {
        return note != null && !note.isEmpty();
    }

    /** 判断是否有视频（URL 非空） */
    public boolean hasVideo() {
        return videoUrl != null && !videoUrl.isEmpty();
    }

    /** 判断是否有图片 */
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }


    /** 判断是否有富文本正文 */
    public boolean hasTextSpan() {
        return textSpan != null;
    }

    /** 判断是否有富文本笺注 */
    public boolean hasNoteSpan() {
        return noteSpan != null;
    }

    /** 判断是否有富文本视频标签 */
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
                ", groupPosition=" + groupPosition +
                "}";
    }
}
