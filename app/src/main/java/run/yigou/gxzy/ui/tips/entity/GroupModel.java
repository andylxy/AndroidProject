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


import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import com.hjq.http.EasyLog;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.SingletonNetData;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;


public class GroupModel {

    /**
     * 获取组列表数据
     */
    public static ArrayList<GroupEntity> getGroups(List<HH2SectionData> showMingCiList, String charSequence, boolean isFang ) {
        EasyLog.print("=== GroupModel.getGroups() 调用 ===");
        EasyLog.print("showMingCiList: " + (showMingCiList != null ? showMingCiList.size() + " items" : "null"));
        EasyLog.print("charSequence: " + charSequence);
        EasyLog.print("isFang: " + isFang);

        try {
            // 获取高亮匹配文本
            SingletonNetData singletonNetData = TipsNetHelper.createSingleDataCopy(showMingCiList);

            // 创建搜索关键字实体
            SearchKeyEntity searchKeyEntity = new SearchKeyEntity(new StringBuilder(charSequence));

            // 获取过滤后的数据
            ArrayList<HH2SectionData> filteredData = TipsNetHelper.getSearchHh2SectionData(searchKeyEntity, singletonNetData);

            // 初始化组列表
            ArrayList<GroupEntity> groups = new ArrayList<>();
            boolean isFirst = true;
            EasyLog.print("filteredData size: " + filteredData.size());
            // 遍历过滤后的数据
            for (HH2SectionData sectionData : filteredData) {
                EasyLog.print("--- 处理sectionData: " + (sectionData != null ? sectionData.getHeader() : "null"));
                if (sectionData == null) {
                    continue; // 跳过空的数据
                }


                List<DataItem> dataList = (List<DataItem>) sectionData.getData();
                if (dataList == null) {
                    continue; // 数据为空，跳过
                }
                // 初始化子项列表
                ArrayList<ChildEntity> children = new ArrayList<>();
               if (isFang){
                for (int i = 0; i < dataList.size(); i++) {
                    DataItem dataItem = dataList.get(i);
                    ChildEntity child = getChildEntity(dataItem);
                    if (i == 0 && isFirst) {
                        isFirst = false;
                        SpannableStringBuilder spannable = TipsNetHelper.renderText("$x{" + child.getName() + "}" + "\n");
                        child.setAttributed_child_section_text(spannable.append(child.getAttributed_child_section_text()));
                    }
                    children.add(child);

                }}else {
                   for (DataItem dataItem : dataList) {
                       if (dataItem != null) {
                           ChildEntity child = getChildEntity(dataItem);
                           children.add(child);
                       }
                   }
                }




                // 创建组实体并添加到组列表
                String header = sectionData.getHeader();
                if (header == null) {
                    header = ""; // 防止空指针异常
                }
                String footer = "第尾部"; // 可以改为配置文件或动态生成
                EasyLog.print("创建GroupEntity - header: " + header + ", children size: " + children.size());
                groups.add(new GroupEntity(header, footer, children));
            }

            EasyLog.print("=== GroupModel.getGroups() 完成 ===");
            EasyLog.print("返回groups数量: " + groups.size());
            for (int i = 0; i < groups.size(); i++) {
                GroupEntity g = groups.get(i);
                EasyLog.print("  Group[" + i + "]: header=" + g.getHeader() + ", children=" + (g.getChildren() != null ? g.getChildren().size() : "null"));
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
        SpannableStringBuilder spannableHeader = TipsNetHelper.renderText(sectionData.getHeader());
        // 构造可展开分组实体并添加到分组列表中
        ExpandableGroupEntity group = new ExpandableGroupEntity(sectionData.getHeader(), spannableHeader, "", isExpand, children);
        return group;
    }


}
