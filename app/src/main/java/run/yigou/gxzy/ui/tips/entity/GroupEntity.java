/*
 * 项目名: AndroidProject
 * 类名: GroupEntity.java
 * 包名: run.yigou.gxzy.ui.tips.entity.GroupEntity
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:24:18
 * 上次修改时间: 2024年09月08日 11:56:47
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.entity;

import java.util.ArrayList;

/**
 * 组数据的实体类
 */
public class GroupEntity {

    private String header;
    private String footer;
    private ArrayList<ChildEntity> children;

    public GroupEntity(String header, String footer, ArrayList<ChildEntity> children) {
        this.header = header;
        this.footer = footer;
        this.children = children;
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

    public ArrayList<ChildEntity> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<ChildEntity> children) {
        this.children = children;
    }
}
