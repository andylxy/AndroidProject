/*
 * 项目名: AndroidProject
 * 类名: ClickEventHandler.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.event
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 点击事件处理器接口 - 定义点击事件处理规范
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.event;

import androidx.annotation.NonNull;

import run.yigou.gxzy.ui.tips.entity.ChildEntity;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * 点击事件处理器接口
 * 定义Header和Child的点击事件处理规范
 */
public interface ClickEventHandler {

    /**
     * Header点击事件
     *
     * @param groupPosition 组位置
     * @param entity        数据实体
     */
    void onHeaderClick(int groupPosition, @NonNull ExpandableGroupEntity entity);

    /**
     * Child点击事件
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @param entity        数据实体
     */
    void onChildClick(int groupPosition, int childPosition, @NonNull ChildEntity entity);

    /**
     * Child文本点击事件(点击TextView切换显示模式)
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @param entity        数据实体
     */
    void onChildTextClick(int groupPosition, int childPosition, @NonNull ChildEntity entity);
}
