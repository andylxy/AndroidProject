/*
 * 项目名: AndroidProject
 * 类名: WindowModeClickHandler.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.event
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 弹窗模式点击处理器 - 处理弹窗界面的点击事件
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.event;

import android.content.Context;

import androidx.annotation.NonNull;

import run.yigou.gxzy.ui.tips.entity.ChildEntity;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * 弹窗模式点击处理器
 * 处理弹窗界面的点击事件(通常不需要特殊处理)
 */
public class WindowModeClickHandler implements ClickEventHandler {

    private final Context context;

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public WindowModeClickHandler(@NonNull Context context) {
        this.context = context;
    }

    /**
     * Header点击事件 - 不处理
     *
     * @param groupPosition 组位置
     * @param entity        数据实体
     */
    @Override
    public void onHeaderClick(int groupPosition, @NonNull ExpandableGroupEntity entity) {
        // 弹窗模式下Header点击不处理
    }

    /**
     * Child点击事件 - 不处理
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @param entity        数据实体
     */
    @Override
    public void onChildClick(int groupPosition, int childPosition, @NonNull ChildEntity entity) {
        // 弹窗模式下Child点击不处理
    }

    /**
     * Child文本点击事件 - 切换显示模式
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @param entity        数据实体
     */
    @Override
    public void onChildTextClick(int groupPosition, int childPosition, @NonNull ChildEntity entity) {
        // 在ViewHolder中处理显示切换逻辑
    }
}
