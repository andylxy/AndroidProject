/*
 * 项目名: AndroidProject
 * 类名: BaseRefactoredAdapter.java
 * 包名: run.yigou.gxzy.ui.reader.adapter
 * 描述: 重构基类Adapter - 提供公共功能，统一管理数据源和展开/收起逻辑
 */

package run.yigou.gxzy.ui.reader.adapter;

import android.content.Context;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.adapter.GroupedRecyclerViewAdapter;
import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.media.image.GlideImageLoader;
import run.yigou.gxzy.ui.media.image.ImageLoader;
import run.yigou.gxzy.ui.reader.adapter.binder.BinderFactory;
import run.yigou.gxzy.ui.reader.adapter.model.DataAdapter;
import run.yigou.gxzy.ui.reader.adapter.model.GroupData;
import run.yigou.gxzy.ui.reader.adapter.state.ExpandStateManager;
import run.yigou.gxzy.ui.reader.adapter.state.SearchStateManager;
import run.yigou.gxzy.ui.reader.entity.ExpandableGroupEntity;
import run.yigou.gxzy.utils.SpannableStringCache;

/**
 * 重构基类Adapter
 * <p>
 * 提供所有重构Adapter的公共功能:
 * - 统一管理双数据源(groups + groupDataList)
 * - 展开/收起逻辑及空指针保护
 * - 公共布局和Footer绑定
 * - 跳转监听器管理
 * - 兼容旧接口方法
 */
public abstract class BaseRefactoredAdapter extends GroupedRecyclerViewAdapter {

    protected final Context context;

    /** 旧数据结构（兼容层，可能为null） */
    protected ArrayList<ExpandableGroupEntity> groups;

    /** 新数据结构（主要数据源） */
    protected List<GroupData> groupDataList;

    // 状态管理器
    protected final ExpandStateManager expandStateManager;
    protected final SearchStateManager searchStateManager;

    // 工具类
    protected final SpannableStringCache spannableStringCache;
    protected final ImageLoader imageLoader;

    // Binder工厂
    protected final BinderFactory binderFactory;

    // 跳转监听器
    protected OnJumpSpecifiedItemListener jumpListener;

    /**
     * 跳转监听器接口（兼容旧代码）
     */
    public interface OnJumpSpecifiedItemListener {
        void onJumpSpecifiedItem(int groupPosition, int childPosition);
    }

    /**
     * 构造函数
     *
     * @param context 上下文
     */
    public BaseRefactoredAdapter(@NonNull Context context) {
        super(context);
        this.context = context;
        this.groupDataList = new ArrayList<>();

        // 初始化状态管理器
        this.expandStateManager = new ExpandStateManager();
        this.searchStateManager = new SearchStateManager();

        // 初始化工具类
        this.spannableStringCache = new SpannableStringCache();
        this.imageLoader = new GlideImageLoader();

        // 初始化Binder工厂
        this.binderFactory = new BinderFactory(
                expandStateManager,
                spannableStringCache,
                imageLoader
        );
    }

    // ============ 数据管理 ============

    /**
     * 设置数据 - 旧数据结构
     * <p>
     * 自动转换为新数据结构并同步展开状态。
     * 子类可重写此方法以自定义展开行为（如搜索模式默认全部展开）。
     *
     * @param groups 数据列表
     */
    public void setGroups(@NonNull ArrayList<ExpandableGroupEntity> groups) {
        this.groups = groups;
        this.groupDataList = DataAdapter.convertListGeneric(groups);
        expandStateManager.syncFromData(groups);
        notifyDataSetChanged();
    }

    /**
     * 设置数据 - 新数据结构
     * <p>
     * 注意：此方法不会设置 groups（旧数据结构），
     * 展开/收起方法已做空指针保护。
     *
     * @param groupDataList 分组数据列表
     */
    public void setGroupDataList(@NonNull List<GroupData> groupDataList) {
        this.groupDataList = new ArrayList<>(groupDataList);
        expandStateManager.reset();
        for (int i = 0; i < groupDataList.size(); i++) {
            expandStateManager.setExpandState(i, false);
        }
        notifyDataSetChanged();
    }

    /**
     * 获取旧数据结构
     *
     * @return 数据列表，可能为null
     */
    public ArrayList<ExpandableGroupEntity> getGroups() {
        return groups;
    }

    /**
     * 获取新数据结构
     *
     * @return 分组数据列表
     */
    public List<GroupData> getGroupDataList() {
        return groupDataList;
    }

    @Override
    public int getGroupCount() {
        return groupDataList != null ? groupDataList.size() : 0;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (!expandStateManager.isExpanded(groupPosition)) {
            return 0;
        }
        if (groupDataList == null || groupPosition >= groupDataList.size()) {
            return 0;
        }
        return groupDataList.get(groupPosition).getItemCount();
    }

    // ============ 展开/收起逻辑 ============

    /**
     * 判断指定组是否展开
     */
    public boolean isExpand(int groupPosition) {
        return expandStateManager.isExpanded(groupPosition);
    }

    /**
     * 展开指定组
     */
    public void expandGroup(int groupPosition) {
        expandGroup(groupPosition, false);
    }

    /**
     * 展开指定组（可选动画）
     *
     * @param groupPosition 组位置
     * @param animate       是否使用插入动画
     */
    public void expandGroup(int groupPosition, boolean animate) {
        expandStateManager.setExpandState(groupPosition, true);
        syncEntityExpandState(groupPosition, true);
        if (animate) {
            notifyChildrenInserted(groupPosition);
        } else {
            notifyDataChanged();
        }
    }

    /**
     * 收起指定组
     */
    public void collapseGroup(int groupPosition) {
        collapseGroup(groupPosition, false);
    }

    /**
     * 收起指定组（可选动画）
     *
     * @param groupPosition 组位置
     * @param animate       是否使用删除动画
     */
    public void collapseGroup(int groupPosition, boolean animate) {
        expandStateManager.setExpandState(groupPosition, false);
        syncEntityExpandState(groupPosition, false);
        if (animate) {
            notifyChildrenRemoved(groupPosition);
        } else {
            notifyDataChanged();
        }
    }

    /**
     * 展开所有组
     */
    public void expandAll() {
        for (int i = 0; i < getGroupCount(); i++) {
            expandStateManager.setExpandState(i, true);
            syncEntityExpandState(i, true);
        }
        notifyDataSetChanged();
    }

    /**
     * 收起所有组
     */
    public void collapseAll() {
        for (int i = 0; i < getGroupCount(); i++) {
            expandStateManager.setExpandState(i, false);
            syncEntityExpandState(i, false);
        }
        notifyDataSetChanged();
    }

    /**
     * 安全地同步entity的展开状态（groups可能为null）
     *
     * @param position 组位置
     * @param expanded 是否展开
     */
    protected void syncEntityExpandState(int position, boolean expanded) {
        if (groups != null && position >= 0 && position < groups.size()) {
            groups.get(position).setExpand(expanded);
        }
    }

    // ============ 公共布局方法 ============

    @Override
    public boolean hasHeader(int groupPosition) {
        return true;
    }

    @Override
    public boolean hasFooter(int groupPosition) {
        return false;
    }

    @Override
    public int getHeaderLayout(int viewType) {
        return R.layout.adapter_expandable_header;
    }

    @Override
    public int getFooterLayout(int viewType) {
        return 0;
    }

    @Override
    public int getChildLayout(int viewType) {
        return R.layout.adapter_child;
    }

    @Override
    public void onBindFooterViewHolder(BaseViewHolder holder, int groupPosition) {
        // 不使用Footer，留空实现
    }

    // ============ 跳转监听器 ============

    /**
     * 设置跳转监听器
     */
    public void setOnJumpSpecifiedItemListener(OnJumpSpecifiedItemListener listener) {
        this.jumpListener = listener;
    }

    /**
     * 获取跳转监听器
     */
    public OnJumpSpecifiedItemListener getJumpListener() {
        return jumpListener;
    }

    // ============ 兼容旧接口 ============

    /**
     * 获取旧数据结构（兼容旧代码）
     *
     * @return 数据列表
     */
    public ArrayList<ExpandableGroupEntity> getmGroups() {
        return groups != null ? groups : new ArrayList<>();
    }

    /**
     * 设置旧数据结构（兼容旧代码）
     */
    public void setmGroups(ArrayList<ExpandableGroupEntity> groups) {
        setGroups(groups);
    }

    // ============ 状态管理器访问 ============

    public ExpandStateManager getExpandStateManager() {
        return expandStateManager;
    }

    public SearchStateManager getSearchStateManager() {
        return searchStateManager;
    }

    public SpannableStringCache getSpannableStringCache() {
        return spannableStringCache;
    }

    public ImageLoader getImageLoader() {
        return imageLoader;
    }

    // ============ 资源管理 ============

    /**
     * 清理资源（Activity/Fragment销毁时调用）
     */
    public void cleanup() {
        spannableStringCache.clear();
        imageLoader.clearCache();
        expandStateManager.clear();
        searchStateManager.reset();
    }
}
