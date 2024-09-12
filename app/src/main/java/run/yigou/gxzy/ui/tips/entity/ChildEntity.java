/*
 * 项目名: AndroidProject
 * 类名: ChildEntity.java
 * 包名: run.yigou.gxzy.ui.tips.entity.ChildEntity
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:24:18
 * 上次修改时间: 2024年09月08日 11:56:47
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.entity;

/**
 * 子项数据的实体类
 */
public class ChildEntity {

    private String child;

    public ChildEntity(String child) {
        this.child = child;
    }

    public String getChild() {
        return child;
    }

    public void setChild(String child) {
        this.child = child;
    }
}
