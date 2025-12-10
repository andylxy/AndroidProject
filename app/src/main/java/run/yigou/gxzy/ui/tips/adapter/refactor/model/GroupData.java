/*
 * 项目名: AndroidProject
 * 类名: GroupData.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.model
 * 作者 : AI Refactor
 * 创建时间 : 2025年12月10日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.model;

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
 */
public class GroupData {
    
    private final String title;                          // 标题文本
    private final SpannableStringBuilder titleSpan;      // 富文本标题
    private final List<ItemData> items;                  // 子项列表
    
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
