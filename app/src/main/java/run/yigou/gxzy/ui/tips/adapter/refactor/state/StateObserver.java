/*
 * 项目名: AndroidProject
 * 类名: StateObserver.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.state
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 状态观察者接口 - 监听状态变化
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.state;

/**
 * 状态观察者接口
 * 用于监听展开状态和搜索模式的变化
 */
public interface StateObserver {

    /**
     * 展开状态变化回调
     *
     * @param groupPosition 组位置
     * @param isExpanded    是否展开
     */
    void onExpandStateChanged(int groupPosition, boolean isExpanded);

    /**
     * 搜索模式变化回调
     *
     * @param isSearchMode 是否为搜索模式
     */
    void onSearchModeChanged(boolean isSearchMode);
}
