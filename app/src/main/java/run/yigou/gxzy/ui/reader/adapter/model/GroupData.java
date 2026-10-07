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
import java.util.ArrayList;
import java.util.List;

/**
 * 分组数据模型 - 全新设计,不依赖旧的ExpandableGroupEntity
 * 
 * 职责:
 * - 存储分组基本信息(标题、富文本)
 * - 管理子项列表
 * - 提供不可变访问接口
 *
 * 关于可变性：标题 / 富文本 / 子项列表一律 final，构造后只读。
 * 唯一的例外是 chapterIndex——它由「构造之后」才知道的信息决定（搜索结果的真实
 * 章节下标要按 signatureId 反查，见 SearchCoordinator），且绑定发生在构造链之外
 * （BaseRefactoredAdapter#bindChapterIndexByPosition 与
 * RefactoredExpandableAdapter#setSearchData）。故该字段允许 setter。
 * 实际写入只发生在列表重建 / 单章更新时，且都在主线程，不存在并发问题。
 */
public class GroupData {

    /** 章节未绑定真实位置时的哨兵值（列表下标从 0 开始，故-1 可安全表示「无」） */
    public static final int NO_CHAPTER_INDEX = -1;

    private final String title;                          // 标题文本
    private final SpannableStringBuilder titleSpan;      // 富文本标题
    private final List<ItemData> items;                  // 子项列表

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
    
    /**
     * 构造函数 - 使用纯文本标题
     */
    public GroupData(@NonNull String title, @NonNull List<ItemData> items) {
        this.title = title;
        this.titleSpan = null;
        this.items = new ArrayList<>(items);  // 防御性拷贝
    }
    
    /**
     * 构造函数 - 使用富文本标题
     */
    public GroupData(@NonNull String title, 
                     SpannableStringBuilder titleSpan,
                     @NonNull List<ItemData> items) {
        this.title = title;
        this.titleSpan = titleSpan;
        this.items = new ArrayList<>(items);  // 防御性拷贝
    }
    
    /**
     * 获取标题文本
     */
    @NonNull
    public String getTitle() {
        return title;
    }
    
    /**
     * 获取富文本标题(可能为null)
     */
    public SpannableStringBuilder getTitleSpan() {
        return titleSpan;
    }
    
    /**
     * 判断是否有富文本标题
     */
    public boolean hasTitleSpan() {
        return titleSpan != null;
    }

    /**
     * 获取本章在当前显示列表中的下标（T6）
     */
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

    
    /**
     * 获取子项数量
     */
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * 获取指定位置的子项
     */
    @NonNull
    public ItemData getItem(int position) {
        return items.get(position);
    }
    
    /**
     * 获取所有子项(不可变列表)
     */
    @NonNull
    public List<ItemData> getItems() {
        return new ArrayList<>(items);  // 返回拷贝,防止外部修改
    }
    
    /**
     * 判断是否为空组
     */
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    @Override
    public String toString() {
        return "GroupData{title='" + title + "', itemCount=" + items.size() + "}";
    }
}
