/*
 * 项目名: AndroidProject
 * 类名: ExpandStateManager.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.state
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 展开状态管理器 - 管理章节展开/收起状态
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.state;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * 展开状态管理器
 * 集中管理所有组的展开/收起状态
 */
public class ExpandStateManager {

    // 存储展开组的位置
    private final Set<Integer> expandedGroups = new HashSet<>();

    // 状态观察者列表
    private final List<StateObserver> observers = new ArrayList<>();

    /**
     * 判断指定组是否展开
     *
     * @param groupPosition 组位置
     * @return true表示已展开
     */
    public boolean isExpanded(int groupPosition) {
        return expandedGroups.contains(groupPosition);
    }

    /**
     * 展开指定组
     *
     * @param groupPosition 组位置
     */
    public void expand(int groupPosition) {
        if (!expandedGroups.contains(groupPosition)) {
            expandedGroups.add(groupPosition);
            notifyExpandStateChanged(groupPosition, true);
        }
    }

    /**
     * 收起指定组
     *
     * @param groupPosition 组位置
     */
    public void collapse(int groupPosition) {
        if (expandedGroups.contains(groupPosition)) {
            expandedGroups.remove(groupPosition);
            notifyExpandStateChanged(groupPosition, false);
        }
    }

    /**
     * 切换指定组的展开状态
     *
     * @param groupPosition 组位置
     * @return 切换后的状态(true表示展开)
     */
    public boolean toggleExpand(int groupPosition) {
        if (isExpanded(groupPosition)) {
            collapse(groupPosition);
            return false;
        } else {
            expand(groupPosition);
            return true;
        }
    }

    /**
     * 展开所有组
     *
     * @param groupCount 总组数
     */
    public void expandAll(int groupCount) {
        for (int i = 0; i < groupCount; i++) {
            expand(i);
        }
    }

    /**
     * 收起所有组
     */
    public void collapseAll() {
        List<Integer> expandedList = new ArrayList<>(expandedGroups);
        for (Integer position : expandedList) {
            collapse(position);
        }
    }

    /**
     * 获取所有展开的组位置
     *
     * @return 展开组位置列表
     */
    @NonNull
    public List<Integer> getExpandedGroups() {
        return new ArrayList<>(expandedGroups);
    }

    /**
     * 获取展开组数量
     *
     * @return 展开组数量
     */
    public int getExpandedCount() {
        return expandedGroups.size();
    }

    /**
     * 从数据实体同步状态
     *
     * @param groups 数据实体列表
     */
    public void syncFromData(@NonNull List<ExpandableGroupEntity> groups) {
        expandedGroups.clear();
        for (int i = 0; i < groups.size(); i++) {
            if (groups.get(i).isExpand()) {
                expandedGroups.add(i);
            }
        }
    }

    /**
     * 将状态同步到数据实体
     *
     * @param groups 数据实体列表
     */
    public void syncToData(@NonNull List<ExpandableGroupEntity> groups) {
        for (int i = 0; i < groups.size(); i++) {
            groups.get(i).setExpand(isExpanded(i));
        }
    }

    /**
     * 清空所有状态
     */
    public void clear() {
        expandedGroups.clear();
    }
    
    /**
     * 重置状态(清空所有展开状态)
     */
    public void reset() {
        clear();
    }
    
    /**
     * 设置展开状态
     *
     * @param groupPosition 组位置
     * @param expanded      是否展开
     */
    public void setExpandState(int groupPosition, boolean expanded) {
        if (expanded) {
            expand(groupPosition);
        } else {
            collapse(groupPosition);
        }
    }

    /**
     * 添加状态观察者
     *
     * @param observer 观察者
     */
    public void addObserver(@NonNull StateObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /**
     * 移除状态观察者
     *
     * @param observer 观察者
     */
    public void removeObserver(@NonNull StateObserver observer) {
        observers.remove(observer);
    }

    /**
     * 通知展开状态变化
     *
     * @param groupPosition 组位置
     * @param isExpanded    是否展开
     */
    private void notifyExpandStateChanged(int groupPosition, boolean isExpanded) {
        for (StateObserver observer : observers) {
            observer.onExpandStateChanged(groupPosition, isExpanded);
        }
    }
}
