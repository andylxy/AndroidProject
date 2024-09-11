package run.yigou.gxzy.ui.tips;

import android.text.SpannableStringBuilder;

import java.util.ArrayList;

import run.yigou.gxzy.tipsutils.DataBeans.ShowFanYao;
import run.yigou.gxzy.tipsutils.DataItem;
import run.yigou.gxzy.tipsutils.HH2SectionData;
import run.yigou.gxzy.tipsutils.SingletonData;

/**
 * Depiction:
 * Author: teach
 * Date: 2017/3/20 15:51
 */
public class GroupModel {


    /**
     * 获取组列表数据
     *
     * @param groupCount    组数量
     * @param childrenCount 每个组里的子项数量
     * @return
     */
    public static ArrayList<GroupEntity> getGroups(int groupCount, int childrenCount) {
        ArrayList<GroupEntity> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            ArrayList<ChildEntity> children = new ArrayList<>();
            for (int j = 0; j < childrenCount; j++) {
                children.add(new ChildEntity("第" + (i + 1) + "组第" + (j + 1) + "项"));
            }
            groups.add(new GroupEntity("第" + (i + 1) + "组头部",
                    "第" + (i + 1) + "组尾部", children));
        }
        return groups;
    }

    /**
     * 获取组列表数据
     */
    public static ArrayList<GroupEntity> getGroups(SingletonData singletonData) {
        ArrayList<GroupEntity> groups = new ArrayList<>();
        for (HH2SectionData sectionData : singletonData.getContent()) {
            ArrayList<ChildEntity> children = new ArrayList<>();
            for (DataItem dataItem : sectionData.getData()) {
                children.add(new ChildEntity(dataItem.getText()));
            }
            //String header = sectionData.getHeader();
            groups.add(new GroupEntity(sectionData.getHeader(), "第尾部", children));
        }
        return groups;
    }

    /**
     * 获取组列表数据
     */
    public static ArrayList<GroupEntity> getGroups(ArrayList<ShowFanYao> showFanYaoList) {
        ArrayList<GroupEntity> groups = new ArrayList<>();

        for (ShowFanYao sectionData :showFanYaoList) {
            ArrayList<ChildEntity> children = new ArrayList<>();
            for (DataItem dataItem : sectionData.getData()) {
                children.add(new ChildEntity(dataItem.getText()));
            }
            //String header = sectionData.getHeader();
            groups.add(new GroupEntity(sectionData.getHeader(), "第尾部", children));
        }
        return groups;
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

    public static ArrayList<ExpandableGroupEntity> getExpandableGroups(SingletonData singletonData, boolean isExpand) {
        ArrayList<ExpandableGroupEntity> groups = new ArrayList<>();
        for (HH2SectionData sectionData : singletonData.getContent()) {
            ArrayList<ChildEntity> children = new ArrayList<>();
            for (DataItem dataItem : sectionData.getData()) {
                children.add(new ChildEntity(dataItem.getText()));
            }
            //String header = sectionData.getHeader();
            groups.add(new ExpandableGroupEntity(sectionData.getHeader(), "第尾部", isExpand, children));
        }
        return groups;
    }

}
