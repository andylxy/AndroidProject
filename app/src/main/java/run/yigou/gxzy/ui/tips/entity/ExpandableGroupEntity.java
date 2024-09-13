/*
 * 项目名: AndroidProject
 * 类名: ExpandableGroupEntity.java
 * 包名: run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:24:18
 * 上次修改时间: 2024年09月08日 11:56:47
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.entity;

import android.text.SpannableStringBuilder;

import java.util.ArrayList;

/**
 * 可展开收起的组数据的实体类 它比GroupEntity只是多了一个boolean类型的isExpand，用来表示展开和收起的状态。
 */
public class ExpandableGroupEntity {



    private SpannableStringBuilder spannableHeader;
    private String header;
    private String footer;
    private ArrayList<ChildEntity> children;
    private boolean isExpand;

    public ExpandableGroupEntity(String header, String footer, boolean isExpand,
                                 ArrayList<ChildEntity> children) {
        this.header = header;
        this.footer = footer;
        this.isExpand = isExpand;
        this.children = children;
    }
    public ExpandableGroupEntity(String header,SpannableStringBuilder spannableHeader, String footer, boolean isExpand,
                                 ArrayList<ChildEntity> children) {
        this.header = header;
        this.spannableHeader = spannableHeader;
        this.footer = footer;
        this.isExpand = isExpand;
        this.children = children;
    }
    public SpannableStringBuilder getSpannableHeader() {
        return spannableHeader;
    }

    public void setSpannableHeader(SpannableStringBuilder spannableHeader) {
        this.spannableHeader = spannableHeader;
    }
    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getFooter() {
        return footer;
    }

    public void setFooter(String footer) {
        this.footer = footer;
    }

    public boolean isExpand() {
        return isExpand;
    }

    public void setExpand(boolean expand) {
        isExpand = expand;
    }

    public ArrayList<ChildEntity> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<ChildEntity> children) {
        this.children = children;
    }
}
