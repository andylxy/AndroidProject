/*
 * 项目名: AndroidProject
 * 类名: SearchResultBuilder.java
 * 包名: run.yigou.gxzy.ui.reader.search
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search;

import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.util.Pair;

import run.yigou.gxzy.ui.reader.entity.GroupData;
import run.yigou.gxzy.ui.reader.entity.ItemData;
import run.yigou.gxzy.ui.reader.helper.TipsClickHandler;
import run.yigou.gxzy.data.model.DataItem;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索结果构建器
 * 
 * <p>职责：统一构建 GroupData / ItemData 结果结构
 * <ul>
 *   <li>构建分组数据（GroupData）</li>
 *   <li>转换 DataItem 为 ItemData（支持方剂配方特殊处理）</li>
 *   <li>构建"未找到"结果（方剂/药物/名词）</li>
 * </ul>
 * 
 * <p>设计意图：
 * <ul>
 *   <li>将结果构建逻辑从搜索策略中提取，避免重复代码</li>
 *   <li>统一处理方剂配方的橙色标签样式</li>
 *   <li>提供便捷的"未找到"结果工厂方法</li>
 * </ul>
 */
public class SearchResultBuilder {
    
    /**
     * 构建分组数据
     * 
     * @param title 分组标题
     * @param expanded 是否展开
     * @return GroupData 实例
     */
    public GroupData buildGroup(String title, boolean expanded) {
        GroupData group = new GroupData();
        group.setTitle(title);
        group.setExpanded(expanded);
        return group;
    }
    
    /**
     * 转换 DataItem 为 ItemData
     * 
     * <p>特殊处理：
     * <ul>
     *   <li>方剂配方（isFangRecipe=true）：添加橙色标签【方剂名称】</li>
     *   <li>普通条目（isFangRecipe=false）：直接复制 attributedText</li>
     * </ul>
     * 
     * @param source 源数据项
     * @param isFangRecipe 是否为方剂配方
     * @return 转换后的 ItemData
     */
    public ItemData convertDataItem(DataItem source, boolean isFangRecipe) {
        ItemData item = new ItemData();
        
        if (isFangRecipe && source.getAttributedText() != null) {
            // 方剂配方：添加橙色标签
            item.setAttributedText(buildFangRecipeHint(source));
        } else if (source.getAttributedText() != null) {
            // 普通条目：直接复制
            item.setAttributedText(source.getAttributedText());
        }
        
        // 复制其他字段
        if (source.getAttributedNote() != null) {
            item.setAttributedNote(source.getAttributedNote());
        }
        if (source.getAttributedSectionVideo() != null) {
            item.setAttributedVideo(source.getAttributedSectionVideo());
        }
        if (source.getImageUrl() != null) {
            item.setImageUrl(source.getImageUrl());
        }
        item.setGroupPosition(source.getGroupPosition());
        
        return item;
    }
    
    /**
     * 构建"未找到方剂"结果
     * 
     * @return Pair<groups, items> 包含"未见方。"提示的结果
     */
    public Pair<List<GroupData>, List<List<ItemData>>> notFoundFang() {
        List<GroupData> groups = new ArrayList<>();
        List<List<ItemData>> items = new ArrayList<>();
        
        GroupData group = buildGroup("伤寒金匮方", true);
        groups.add(group);
        
        List<ItemData> itemList = new ArrayList<>();
        ItemData item = new ItemData();
        item.setAttributedText(TipsClickHandler.renderText("$m{未见方。}"));
        itemList.add(item);
        items.add(itemList);
        
        return new Pair<>(groups, items);
    }
    
    /**
     * 构建"未找到药物"结果
     * 
     * @return Pair<groups, items> 包含"未见此药。"提示的结果
     */
    public Pair<List<GroupData>, List<List<ItemData>>> notFoundYao() {
        List<GroupData> groups = new ArrayList<>();
        List<List<ItemData>> items = new ArrayList<>();
        
        GroupData group = buildGroup("药物信息", true);
        groups.add(group);
        
        List<ItemData> itemList = new ArrayList<>();
        ItemData item = new ItemData();
        item.setAttributedText(TipsClickHandler.renderText("$m{未见此药。}"));
        itemList.add(item);
        items.add(itemList);
        
        return new Pair<>(groups, items);
    }
    
    /**
     * 构建"未找到名词"结果
     * 
     * @return Pair<groups, items> 包含"未见此名词。"提示的结果
     */
    public Pair<List<GroupData>, List<List<ItemData>>> notFoundMingCi() {
        List<GroupData> groups = new ArrayList<>();
        List<List<ItemData>> items = new ArrayList<>();
        
        GroupData group = buildGroup("名词解释", true);
        groups.add(group);
        
        List<ItemData> itemList = new ArrayList<>();
        ItemData item = new ItemData();
        item.setAttributedText(TipsClickHandler.renderText("$m{未见此名词。}"));
        itemList.add(item);
        items.add(itemList);
        
        return new Pair<>(groups, items);
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 构建方剂配方提示文本（带橙色标签）
     * 
     * <p>样式：
     * <ul>
     *   <li>标签颜色：淡橙色 #FFB74D</li>
     *   <li>标签样式：粗体</li>
     *   <li>格式：【方剂名称】\n\n[正文内容]</li>
     * </ul>
     */
    private SpannableStringBuilder buildFangRecipeHint(DataItem dataItem) {
        SpannableStringBuilder enhancedText = new SpannableStringBuilder();
        
        String hint = "【" + dataItem.getName() + "】";
        SpannableString hintSpan = new SpannableString(hint);
        
        // 淡橙色 #FFB74D
        hintSpan.setSpan(new ForegroundColorSpan(0xFFFFB74D), 0, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        // 粗体
        hintSpan.setSpan(new StyleSpan(Typeface.BOLD), 0, hint.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        
        enhancedText.append(hintSpan);
        enhancedText.append("\n\n");
        enhancedText.append(dataItem.getAttributedText());
        
        return enhancedText;
    }
}
