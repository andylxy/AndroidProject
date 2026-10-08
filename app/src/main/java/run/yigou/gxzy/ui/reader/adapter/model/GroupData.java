/*
 * 项目名: AndroidProject
 * 类名: GroupData.java
 * 包名: run.yigou.gxzy.ui.reader.adapter.model
 * 作者 : AI Refactor
 * 创建时间 : 2025年12月10日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.adapter.model;

import android.text.SpannableStringBuilder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分组数据模型 —— 全局唯一的 GroupData 类。
 *
 * <p>由 Q7C 合并自「新架构 GroupData」（AI Refactor 2025-12-10）与「旧 entity.GroupData」：
 * 保留新架构的 title/titleSpan/items + chapterIndex，从旧类并入 isExpanded（展开状态），
 * 合并后旧 `ui/reader/entity/GroupData` 已删除。
 *
 * <p>关于可变性：
 * <ul>
 *   <li>{@code title}/{@code titleSpan}/{@code items} 一律 final，构造后只读；</li>
 *   <li>{@code chapterIndex} 由数据装载方按 signatureId 反查绑定，允许 setter；</li>
 *   <li>{@code expanded} 展开状态由 ExpandStateManager 通过 setter 更新。</li>
 * </ul>
 */
public class GroupData {

    /** 章节未绑定真实位置时的哨兵值（列表下标从 0 开始，故 -1 可安全表示「无」） */
    public static final int NO_CHAPTER_INDEX = -1;

    private final String title;                          // 标题文本
    private final SpannableStringBuilder titleSpan;      // 富文本标题
    private final List<ItemData> items;                  // 子项列表（可能为 null）

    /**
     * 本章在「当前显示列表」中的下标（T6）。
     *
     * <p>列表有两套数据源且顺序不保证一致：非搜索列表来自 {@code CHAPTER} 表、
     * 搜索结果来自 {@code BOOK_CHAPTER} 表，两处查询都没有 {@code orderBy}。
     * 故搜索结果的过滤后下标不能当章节真实下标用，跳转会命中错章。
     *
     * <p>非搜索态由 {@code setGroups} 按位置绑定；搜索态由 {@code setSearchData}
     * 按 {@code signatureId} 反查绑定。查不到时保持 {@link #NO_CHAPTER_INDEX}，
     * UI 层必须据此拦截动作，<b>不得退化为用过滤后下标兜底</b>。
     */
    private int chapterIndex = NO_CHAPTER_INDEX;

    /** 展开状态（由 ExpandStateManager 管理，合并自旧 entity.GroupData.isExpanded） */
    private boolean expanded = false;

    /**
     * 构造函数 —— 仅有标题（无 items 的搜索态构造，替代旧 entity.GroupData 的无参构造 + setTitle）
     *
     * <p>用于 {@code SearchResultBuilder.buildGroup()} 与 {@code SearchCoordinator.searchGlobal()}：
     * 先构造空组挂标题，后续 item 通过外层 List<ItemData> 结构附加。
     */
    public GroupData(@NonNull String title) {
        this(title, null);
    }

    /** 构造函数 —— 纯文本标题 + items 列表 */
    public GroupData(@NonNull String title, @Nullable List<ItemData> items) {
        this.title = title;
        this.titleSpan = null;
        this.items = items != null ? new ArrayList<>(items) : null;  // 防御性拷贝
    }

    /** 构造函数 —— 纯文本标题 + 富文本标题 + items 列表 */
    public GroupData(@NonNull String title,
                     @Nullable SpannableStringBuilder titleSpan,
                     @Nullable List<ItemData> items) {
        this.title = title;
        this.titleSpan = titleSpan;
        this.items = items != null ? new ArrayList<>(items) : null;
    }

    /** 获取标题文本 */
    @NonNull
    public String getTitle() {
        return title;
    }

    /** 获取富文本标题（可能为 null） */
    @Nullable
    public SpannableStringBuilder getTitleSpan() {
        return titleSpan;
    }

    /** 判断是否有富文本标题 */
    public boolean hasTitleSpan() {
        return titleSpan != null;
    }

    /** 获取本章在当前显示列表中的下标（T6） */
    public int getChapterIndex() {
        return chapterIndex;
    }

    /**
     * 绑定本章在当前显示列表中的下标（T6）
     *
     * <p>由数据装载方（{@code setGroups} 按位置 / {@code setSearchData} 按 signatureId 反查）
     * 调用，不应由 UI 层或事件处理器调用。
     */
    public void setChapterIndex(int chapterIndex) {
        this.chapterIndex = chapterIndex;
    }

    /** 是否处于展开状态（合并自旧 entity.GroupData.isExpanded） */
    public boolean isExpanded() {
        return expanded;
    }

    /** 设置展开状态 */
    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    /** 获取子项数量（items 为 null 时返回 0） */
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    /** 获取指定位置的子项（越界或 items 为 null 时返回 null） */
    @Nullable
    public ItemData getItem(int position) {
        if (items == null || position < 0 || position >= items.size()) {
            return null;
        }
        return items.get(position);
    }

    @Override
    public String toString() {
        int count = items != null ? items.size() : 0;
        return "GroupData{title='" + title + "', itemCount=" + count
                + ", chapterIndex=" + chapterIndex
                + ", expanded=" + expanded + "}";
    }
}
