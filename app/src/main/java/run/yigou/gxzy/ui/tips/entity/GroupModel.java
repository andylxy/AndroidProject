/*
 * 项目名: AndroidProject
 * 类名: GroupModel.java
 * 包名: run.yigou.gxzy.ui.tips.entity.GroupModel
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:24:18
 * 上次修改时间: 2024年09月09日 17:32:00
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.entity;

import static run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper.highlightMatchingText;

import android.text.SpannableStringBuilder;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Show_Fan_Yao_MingCi;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.Singleton_Net_Data;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;


public class GroupModel {

    /**
 * 获取组列表数据
 */
public static ArrayList<GroupEntity> getGroups(ArrayList<Show_Fan_Yao_MingCi> showFanYaoMingCiList, String charSequence) {

    try {
        // 获取高亮匹配文本
        Singleton_Net_Data singletonNetData = highlightMatchingText(showFanYaoMingCiList);
        if (singletonNetData == null) {
            throw new NullPointerException("Singleton_Net_Data is null");
        }

        // 创建搜索关键字实体
        SearchKeyEntity searchKeyEntity = new SearchKeyEntity(charSequence);

        // 获取过滤后的数据
        ArrayList<HH2SectionData> filteredData = TipsNetHelper.getSearchHh2SectionData(searchKeyEntity, singletonNetData);
        if (filteredData == null) {
            throw new NullPointerException("FilteredData is null");
        }

        // 初始化组列表
        ArrayList<GroupEntity> groups = new ArrayList<>();

        // 遍历过滤后的数据
        for (HH2SectionData sectionData : filteredData) {
            if (sectionData == null) {
                continue; // 跳过空的数据
            }

            // 初始化子项列表
            ArrayList<ChildEntity> children = new ArrayList<>();
            for (DataItem dataItem : sectionData.getData()) {
                if (dataItem != null) {
                    children.add(new ChildEntity(dataItem.getAttributedText(), dataItem.getNote(), dataItem.getSectionvideo()));
                }
            }

            // 创建组实体并添加到组列表
            String header = sectionData.getHeader();
            if (header == null) {
                header = ""; // 防止空指针异常
            }
            groups.add(new GroupEntity(header, "第尾部", children));
        }

        return groups;
    } catch (NullPointerException e) {
        // 处理空指针异常
        System.err.println("发生空指针异常: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>();
    } catch (Exception e) {
        // 其他异常处理
        System.err.println("发生异常: " + e.getMessage());
        e.printStackTrace();
        return new ArrayList<>();
    }
}

    /**
     * 获取可展开收起的组列表数据
     *
     * @param groupCount    组数量
     * @param childrenCount 每个组里的子项数量
     * @param isExpand      是否展开:false 收缩,true 展开
     * @return groups 列表数据
     */
    public static ArrayList<ExpandableGroupEntity> getExpandableGroups(int groupCount, int childrenCount, boolean isExpand) {
        ArrayList<ExpandableGroupEntity> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            ArrayList<ChildEntity> children = new ArrayList<>();
            for (int j = 0; j < childrenCount; j++) {
                children.add(new ChildEntity("第" + (i + 1) + "组第" + (j + 1) + "项"));
            }
            groups.add(new ExpandableGroupEntity("第" + (i + 1) + "组头部",
                    "第" + (i + 1) + "组尾部", isExpand, children));
        }
        return groups;
    }

    /**
     * 获取可展开收起的组列表数据(默认收缩)
     *
     * @param groupCount    组数量
     * @param childrenCount 每个组里的子项数量
     * @return groups 列表数据
     */
    public static ArrayList<ExpandableGroupEntity> getExpandableGroups(int groupCount, int childrenCount) {
        return getExpandableGroups(groupCount, childrenCount, false);
    }

/**
 * 根据给定的分段数据生成可展开的分组列表
 *
 * @param hh2SectionData 分段数据，包含多个分段及其对应的条目
 * @param isExpand       标志位，表示分组是否展开
 * @param isSearch       标志位，表示当前操作是否为搜索模式
 * @return 返回一个包含可展开分组的ArrayList，每个分组包含标题和子项列表
 */
public static ArrayList<ExpandableGroupEntity> getExpandableGroups(ArrayList<HH2SectionData> hh2SectionData, boolean isExpand, boolean isSearch) {
    // 初始化存储可展开分组的列表
    ArrayList<ExpandableGroupEntity> groups = new ArrayList<>();
    // 如果输入的分段数据为空，则直接返回空的分组列表
    if (hh2SectionData == null) return groups;

    // 定义一个空字符串常量，用于后续操作
    String EMPTY_STRING = "";

    // 遍历每个分段数据
    for (HH2SectionData sectionData : hh2SectionData) {
        // 初始化当前分组的子项列表
        ArrayList<ChildEntity> children = new ArrayList<>();
        // 获取当前分段的数据列表
        List<DataItem> dataItems = (List<DataItem>) sectionData.getData();
        // 如果数据列表为空，则跳过当前分段
        if (dataItems == null) continue;

        // 遍历当前分段的每个数据项
        for (DataItem dataItem : dataItems) {
            // 根据是否为搜索模式来决定使用不同的文本内容构造子项实体
            if (isSearch) {
                children.add(new ChildEntity(dataItem.getAttributedText(), dataItem.getNote(), dataItem.getSectionvideo()));
            } else {
                children.add(new ChildEntity(dataItem.getText(), dataItem.getNote(), dataItem.getSectionvideo()));
            }
        }

        // 使用辅助类渲染分组头部文本
        SpannableStringBuilder spannableHeader = TipsNetHelper.renderText(sectionData.getHeader());
        // 构造可展开分组实体并添加到分组列表中
        groups.add(new ExpandableGroupEntity(sectionData.getHeader(), spannableHeader, EMPTY_STRING, isExpand, children));
    }
    // 返回构造好的分组列表
    return groups;
}



}
