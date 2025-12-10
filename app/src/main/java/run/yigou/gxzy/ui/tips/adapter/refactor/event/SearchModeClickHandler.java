/*
 * 项目名: AndroidProject
 * 类名: SearchModeClickHandler.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.event
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 搜索模式点击处理器 - 处理搜索界面的点击事件
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.event;

import android.content.Context;

import androidx.annotation.NonNull;

import run.yigou.gxzy.ui.tips.entity.ChildEntity;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * 搜索模式点击处理器
 * 处理搜索界面的Header和Child点击事件
 */
public class SearchModeClickHandler implements ClickEventHandler {

    private final Context context;
    private final OnExpandToggleListener expandToggleListener;

    /**
     * 展开/收起监听器
     */
    public interface OnExpandToggleListener {
        /**
         * 请求展开组
         *
         * @param groupPosition 组位置
         */
        void onExpandRequested(int groupPosition);

        /**
         * 请求收起组
         *
         * @param groupPosition 组位置
         */
        void onCollapseRequested(int groupPosition);
    }

    /**
     * 构造函数
     *
     * @param context               上下文
     * @param expandToggleListener  展开/收起监听器
     */
    public SearchModeClickHandler(@NonNull Context context,
                                    @NonNull OnExpandToggleListener expandToggleListener) {
        this.context = context;
        this.expandToggleListener = expandToggleListener;
    }

    /**
     * Header点击事件 - 展开/收起搜索结果组
     *
     * @param groupPosition 组位置
     * @param entity        数据实体
     */
    @Override
    public void onHeaderClick(int groupPosition, @NonNull ExpandableGroupEntity entity) {
        // 切换展开/收起状态
        if (entity.isExpand()) {
            expandToggleListener.onCollapseRequested(groupPosition);
        } else {
            expandToggleListener.onExpandRequested(groupPosition);
        }
    }

    /**
     * Child点击事件 - 暂不处理
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @param entity        数据实体
     */
    @Override
    public void onChildClick(int groupPosition, int childPosition, @NonNull ChildEntity entity) {
        // 搜索模式下Child点击不处理
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
