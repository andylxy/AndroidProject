/*
 * 项目名: AndroidProject
 * 类名: DataAdapter.java
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

import run.yigou.gxzy.ui.reader.entity.ChildEntity;
import run.yigou.gxzy.ui.reader.entity.ExpandableGroupEntity;

/**
 * 数据转换器 - 旧数据结构转换为新数据结构
 * 
 * 职责:
 * - 将ExpandableGroupEntity转换为GroupData
 * - 将ChildEntity转换为ItemData
 * - 处理批量转换
 * - 保持数据完整性
 */
public class DataAdapter {
    
    /**
     * 将旧的ExpandableGroupEntity转换为新的GroupData
     */
    @NonNull
    public static GroupData fromExpandableGroupEntity(@NonNull ExpandableGroupEntity old) {
        // 转换子项列表
        List<ItemData> items = new ArrayList<>();
        ArrayList<ChildEntity> oldChildren = old.getChildren();
        
        if (oldChildren != null) {
            for (ChildEntity child : oldChildren) {
                items.add(fromChildEntity(child));
            }
        }
        
        // 创建GroupData,优先使用富文本标题
        if (old.getSpannableHeader() != null) {
            return new GroupData(
                old.getHeader() != null ? old.getHeader() : "",
                old.getSpannableHeader(),
                items
            );
        } else {
            return new GroupData(
                old.getHeader() != null ? old.getHeader() : "",
                items
            );
        }
    }
    
    /**
     * 将旧的ChildEntity转换为新的ItemData
     */
    @NonNull
    public static ItemData fromChildEntity(@NonNull ChildEntity old) {
        // 获取基础文本字段
        String text = old.getChild_section_text() != null ? old.getChild_section_text() : "";
        String note = old.getChild_section_note();
        String videoUrl = old.getChild_section_video();
        String imageUrl = old.getChild_section_image();

        // 获取富文本版本（如已渲染过则保留ClickableSpan等信息）
        SpannableStringBuilder textSpan = old.getAttributed_child_section_text();
        SpannableStringBuilder noteSpan = old.getAttributed_child_section_note();
        SpannableStringBuilder videoSpan = old.getAttributed_child_section_video();

        return new ItemData(
            text,
            note,
            videoUrl,
            imageUrl,
            textSpan,   // 可能为null
            noteSpan,   // 可能为null
            videoSpan   // 可能为null
        );
    }
    
    /**
     * 批量转换 - List版本
     */
    @NonNull
    public static List<GroupData> convertListGeneric(@NonNull List<ExpandableGroupEntity> oldList) {
        List<GroupData> newList = new ArrayList<>(oldList.size());
        for (ExpandableGroupEntity old : oldList) {
            newList.add(fromExpandableGroupEntity(old));
        }
        return newList;
    }
    
    /**
     * 私有构造函数 - 工具类不应被实例化
     */
    private DataAdapter() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}
