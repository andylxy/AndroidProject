/*
 * 项目名: AndroidProject
 * 类名: RefactoredSearchAdapter.java
 * 包名: run.yigou.gxzy.ui.reader.adapter
 * 描述: 重构后的搜索适配器 - 搜索模式专用
 */

package run.yigou.gxzy.ui.reader.adapter;

import android.content.Context;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.util.ArrayList;

import run.yigou.gxzy.base.action.ToastAction;
import run.yigou.gxzy.ui.reader.adapter.event.SearchModeClickHandler;
import run.yigou.gxzy.ui.reader.adapter.event.SearchModeLongClickHandler;
import run.yigou.gxzy.ui.reader.adapter.model.DataAdapter;
import run.yigou.gxzy.ui.reader.adapter.model.GroupData;
import run.yigou.gxzy.ui.reader.adapter.model.ItemData;
import run.yigou.gxzy.ui.reader.entity.ExpandableGroupEntity;

/**
 * 重构后的搜索适配器 - 搜索模式
 * <p>
 * 职责:
 * - 搜索模式下的内容展示（默认全部展开）
 * - 委托事件处理给SearchModeClickHandler/SearchModeLongClickHandler
 * - 委托数据绑定给Binder
 * <p>
 * 公共功能(展开/收起/布局/兼容接口)已上提到BaseRefactoredAdapter
 */
public class RefactoredSearchAdapter extends BaseRefactoredAdapter
        implements ToastAction,
                   SearchModeClickHandler.OnExpandToggleListener,
                   SearchModeLongClickHandler.OnToastListener {

    /** 兼容旧接口的跳转监听器 */
    public interface OnJumpSpecifiedItemListener extends BaseRefactoredAdapter.OnJumpSpecifiedItemListener {
    }

    // 事件处理器
    private final SearchModeLongClickHandler longClickHandler;

    public RefactoredSearchAdapter(@NonNull Context context) {
        super(context);
        this.longClickHandler = new SearchModeLongClickHandler(context, this);
        // 搜索模式默认开启
        searchStateManager.enterSearchMode("");
    }

    // ============ 数据管理 ============

    /**
     * 设置数据 - 旧数据结构
     * 搜索模式下默认展开所有组
     */
    @Override
    public void setGroups(@NonNull ArrayList<ExpandableGroupEntity> groups) {
        this.groups = groups;
        this.groupDataList = DataAdapter.convertListGeneric(groups);
        // 搜索模式默认全部展开
        expandStateManager.reset();
        for (int i = 0; i < groupDataList.size(); i++) {
            expandStateManager.setExpandState(i, true);
        }
        notifyDataSetChanged();
    }

    /**
     * 设置搜索关键词
     */
    public void setSearchKeyword(@NonNull String keyword) {
        searchStateManager.enterSearchMode(keyword);
        notifyDataSetChanged();
    }

    // ============ 数据绑定 ============

    @Override
    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
        // 搜索模式只需公共绑定（ViewHolder + Binder + 长按监听），无额外点击监听
        performChildBindingSetup(holder, groupPosition, childPosition);
    }

    /**
     * 子项长按事件 - 委托给SearchModeLongClickHandler
     */
    @Override
    protected boolean onChildLongClick(int groupPosition, int childPosition,
                                        @NonNull ItemData itemData,
                                        @NonNull CharSequence text) {
        return longClickHandler.onChildLongClick(groupPosition, childPosition, itemData, text);
    }

    // ============ 跳转监听器 ============

    /**
     * 设置跳转监听器（同时注册到长按处理器）
     */
    @Override
    public void setOnJumpSpecifiedItemListener(BaseRefactoredAdapter.OnJumpSpecifiedItemListener listener) {
        super.setOnJumpSpecifiedItemListener(listener);
        longClickHandler.setJumpListener(listener);
    }

    // ============ 兼容旧接口 ============

    /**
     * 设置是否搜索模式（兼容旧代码）
     */
    public void setSearch(boolean isSearch) {
        // 搜索适配器默认就是搜索模式
    }

    /**
     * 获取是否搜索模式（兼容旧代码）
     */
    public boolean getSearch() {
        return true;
    }

    // ============ 实现 SearchModeClickHandler.OnExpandToggleListener ============

    @Override
    public void onExpandRequested(int groupPosition) {
        expandGroup(groupPosition, true);
    }

    @Override
    public void onCollapseRequested(int groupPosition) {
        collapseGroup(groupPosition, true);
    }

    // ============ 实现 SearchModeLongClickHandler.OnToastListener ============

    @Override
    public void showToast(String message) {
        toast(message);
    }
}
