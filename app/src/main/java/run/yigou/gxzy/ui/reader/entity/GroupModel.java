/*
 * 项目名: AndroidProject
 * 类名: GroupModel.java
 * 包名: run.yigou.gxzy.ui.reader.entity.GroupModel
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:24:18
 * 上次修改时间: 2024年09月09日 17:32:00
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.entity;


import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.HH2SectionData;

import run.yigou.gxzy.ui.reader.helper.TipsClickHandler;


public class GroupModel {

    private static final String TAG = "GroupModel";

    private static @NonNull ChildEntity getChildEntity(DataItem dataItem) {
        ChildEntity child = new ChildEntity();
        if (dataItem.getAttributedText() != null)
            child.setAttributed_child_section_text(dataItem.getAttributedText());
        if (dataItem.getAttributedNote() != null)
            child.setAttributed_child_section_note(dataItem.getAttributedNote());
        if (dataItem.getAttributedSectionVideo() != null)
            child.setAttributed_child_section_video(dataItem.getAttributedSectionVideo());
        child.setGroupPosition(dataItem.getGroupPosition());
        if (dataItem.getImageUrl() != null)
            child.setChild_section_image(dataItem.getImageUrl());
        if (dataItem.getName() != null)
            child.setName(dataItem.getName());
        return child;
    }


    /**
     * 根据给定的分段数据生成可展开的分组列表
     *
     * @param hh2SectionData 分段数据，包含多个分段及其对应的条目
     * @param isExpand       标志位，表示分组是否展开
     * @return 返回一个包含可展开分组的ArrayList，每个分组包含标题和子项列表
     */
    public static ArrayList<ExpandableGroupEntity> getExpandableGroups(ArrayList<HH2SectionData> hh2SectionData, boolean isExpand) {
        // 初始化存储可展开分组的列表
        ArrayList<ExpandableGroupEntity> groups = new ArrayList<>();
        // 如果输入的分段数据为空，则直接返回空的分组列表
        if (hh2SectionData == null) return groups;

//        // 定义一个空字符串常量，用于后续操作
//        String EMPTY_STRING = "";

        // 遍历每个分段数据
        for (HH2SectionData sectionData : hh2SectionData) {
            ExpandableGroupEntity group = getExpandableGroupEntity(isExpand, sectionData);
            groups.add(group);
        }
        // 返回构造好的分组列表
        return groups;
    }

    public static @Nullable ExpandableGroupEntity getExpandableGroupEntity(boolean isExpand, HH2SectionData sectionData) {
        // 获取当前分段的数据列表
        List<DataItem> dataItems = (List<DataItem>) sectionData.getData();
        // 如果数据列表为空，则跳过当前分段
        if (dataItems == null) return null;
        // 初始化当前分组的子项列表
        ArrayList<ChildEntity> children = new ArrayList<>();
        // 遍历当前分段的每个数据项
        for (DataItem dataItem : dataItems) {
            if (dataItem != null) {
                ChildEntity child = getChildEntity(dataItem);
                children.add(child);
            }
        }
        // 使用辅助类渲染分组头部文本
        SpannableStringBuilder spannableHeader = TipsClickHandler.renderText(sectionData.getHeader());
        // 构造可展开分组实体并添加到分组列表中
        ExpandableGroupEntity group = new ExpandableGroupEntity(sectionData.getHeader(), spannableHeader, "", isExpand, children);
        return group;
    }


}
