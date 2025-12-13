/*
 * 项目名: AndroidProject
 * 类名: RefactoredSearchAdapter.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 重构后的搜索适配器 - 搜索模式专用
 */

package run.yigou.gxzy.ui.tips.adapter.refactor;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.action.ToastAction;
import run.yigou.gxzy.ui.tips.adapter.refactor.binder.ChildTextBinder;
import run.yigou.gxzy.ui.tips.adapter.refactor.binder.HeaderBinder;
import run.yigou.gxzy.ui.tips.adapter.refactor.event.SearchModeClickHandler;
import run.yigou.gxzy.ui.tips.adapter.refactor.event.SearchModeLongClickHandler;
import run.yigou.gxzy.ui.tips.adapter.refactor.model.DataAdapter;
import run.yigou.gxzy.ui.tips.adapter.refactor.model.GroupData;
import run.yigou.gxzy.ui.tips.adapter.refactor.model.ItemData;
import run.yigou.gxzy.ui.tips.adapter.refactor.viewholder.TipsChildViewHolder;
import run.yigou.gxzy.ui.tips.adapter.refactor.viewholder.TipsHeaderViewHolder;
import run.yigou.gxzy.ui.tips.adapter.refactor.viewholder.ViewHolderFactory;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * 重构后的搜索适配器
 * 
 * 职责:
 * - 搜索模式下的内容展示
 * - 搜索关键词高亮
 * - 委托事件处理给Handler
 * - 委托数据绑定给Binder
 * - 使用新的数据结构GroupData/ItemData
 */
public class RefactoredSearchAdapter extends BaseRefactoredAdapter 
        implements ToastAction, 
                   SearchModeClickHandler.OnExpandToggleListener,
                   SearchModeLongClickHandler.OnToastListener {

    // 新数据结构
    private List<GroupData> groupDataList;
    
    // 事件处理器
    private final SearchModeClickHandler clickHandler;
    private final SearchModeLongClickHandler longClickHandler;
    
    // 跳转监听器(兼容旧接口)
    private OnJumpSpecifiedItemListener jumpListener;

    public RefactoredSearchAdapter(@NonNull Context context) {
        super(context);
        
        // 初始化事件处理器
        this.clickHandler = new SearchModeClickHandler(context, this);
        this.longClickHandler = new SearchModeLongClickHandler(context, this);
        
        this.groupDataList = new ArrayList<>();
        
        // 搜索模式默认开启
        searchStateManager.enterSearchMode("");
    }

    /**
     * 设置数据 - 使用新数据结构
     */
    public void setGroupDataList(@NonNull List<GroupData> groupDataList) {
        this.groupDataList = new ArrayList<>(groupDataList);
        // 搜索模式下默认全部展开
        expandStateManager.reset();
        for (int i = 0; i < groupDataList.size(); i++) {
            expandStateManager.setExpandState(i, true);
        }
        notifyDataSetChanged();
    }
    
    /**
     * 兼容旧接口 - 设置旧数据结构(自动转换)
     */
    @Override
    public void setGroups(@NonNull ArrayList<ExpandableGroupEntity> groups) {
        super.setGroups(groups);
        // 转换为新数据结构
        this.groupDataList = DataAdapter.convertList(groups);
        // 搜索模式下默认全部展开
        for (int i = 0; i < groups.size(); i++) {
            expandStateManager.setExpandState(i, groups.get(i).isExpand());
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

    @Override
    public int getGroupCount() {
        return groupDataList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // 搜索模式下默认展开所有
        if (!expandStateManager.isExpanded(groupPosition)) {
            return 0;
        }
        return groupDataList.get(groupPosition).getItemCount();
    }

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
        // 不使用Footer
    }

    @Override
    public void onBindHeaderViewHolder(BaseViewHolder holder, int groupPosition) {
        GroupData groupData = groupDataList.get(groupPosition);
        
        // 使用ViewHolder封装
        TipsHeaderViewHolder headerVH = ViewHolderFactory.createHeaderViewHolder(holder);
        
        // 使用Binder绑定数据
        HeaderBinder binder = binderFactory.createHeaderBinder();
        binder.bind(groupData, headerVH, groupPosition);
        
        // 设置展开/收起图标(使用旋转实现)
        ImageView ivState = holder.get(R.id.iv_state);
        if (ivState != null) {
            ivState.setRotation(
                expandStateManager.isExpanded(groupPosition) 
                    ? 90f 
                    : 0f
            );
        }
        
        // 注意: Header点击事件通过setOnHeaderClickListener设置，不在此处设置
    }

    @Override
    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
        GroupData groupData = groupDataList.get(groupPosition);
        ItemData itemData = groupData.getItem(childPosition);
        
        // 使用ViewHolder封装
        TipsChildViewHolder childVH = ViewHolderFactory.createChildViewHolder(holder);
        
        // 使用Binder绑定数据(搜索模式会应用高亮)
        ChildTextBinder binder = binderFactory.createChildTextBinder();
        binder.bind(itemData, childVH, childPosition);
        
        // 设置点击事件(委托给Handler)
        TextView sectionText = holder.get(R.id.tv_sectiontext);
        TextView sectionNote = holder.get(R.id.tv_sectionnote);
        TextView sectionVideo = holder.get(R.id.tv_sectionvideo);
        
        if (sectionText != null) {
            sectionText.setOnClickListener(v -> 
                childVH.toggleTextVisibility(itemData.getNoteSpan())
            );
            
            sectionText.setOnLongClickListener(v -> {
                // 优先使用SpannableStringBuilder以保留格式
                CharSequence text = itemData.hasTextSpan() 
                    ? itemData.getTextSpan() 
                    : sectionText.getText();
                return longClickHandler.onChildLongClick(
                    groupPosition, 
                    childPosition, 
                    itemData, 
                    text
                );
            });
        }
        
        if (sectionNote != null) {
            sectionNote.setOnClickListener(v -> 
                childVH.toggleNoteVisibility(itemData.getVideoSpan())
            );
            
            sectionNote.setOnLongClickListener(v -> {
                // 优先使用SpannableStringBuilder以保留格式
                CharSequence text = itemData.hasNoteSpan() 
                    ? itemData.getNoteSpan() 
                    : sectionNote.getText();
                return longClickHandler.onChildLongClick(
                    groupPosition, 
                    childPosition, 
                    itemData, 
                    text
                );
            });
        }
        
        if (sectionVideo != null) {
            sectionVideo.setOnClickListener(v -> 
                childVH.toggleVideoVisibility()
            );
            
            sectionVideo.setOnLongClickListener(v -> {
                // 优先使用SpannableStringBuilder以保留格式
                CharSequence text = itemData.hasVideoSpan() 
                    ? itemData.getVideoSpan() 
                    : sectionVideo.getText();
                return longClickHandler.onChildLongClick(
                    groupPosition, 
                    childPosition, 
                    itemData, 
                    text
                );
            });
        }
    }

    /**
     * 判断当前组是否展开
     */
    public boolean isExpand(int groupPosition) {
        return expandStateManager.isExpanded(groupPosition);
    }

    /**
     * 展开一个组
     */
    public void expandGroup(int groupPosition) {
        expandGroup(groupPosition, false);
    }

    /**
     * 展开一个组(带动画)
     */
    public void expandGroup(int groupPosition, boolean animate) {
        expandStateManager.setExpandState(groupPosition, true);
        if (animate) {
            notifyChildrenInserted(groupPosition);
        } else {
            notifyDataChanged();
        }
    }

    /**
     * 收起一个组
     */
    public void collapseGroup(int groupPosition) {
        collapseGroup(groupPosition, false);
    }

    /**
     * 收起一个组(带动画)
     */
    public void collapseGroup(int groupPosition, boolean animate) {
        expandStateManager.setExpandState(groupPosition, false);
        if (animate) {
            notifyChildrenRemoved(groupPosition);
        } else {
            notifyDataChanged();
        }
    }

    /**
     * 跳转监听器接口(兼容旧代码)
     */
    public interface OnJumpSpecifiedItemListener {
        void onJumpSpecifiedItem(int groupPosition, int childPosition);
    }

    /**
     * 设置跳转监听器
     */
    public void setOnJumpSpecifiedItemListener(OnJumpSpecifiedItemListener listener) {
        this.jumpListener = listener;
        longClickHandler.setJumpListener(listener);
    }
    
    /**
     * 获取跳转监听器
     */
    public OnJumpSpecifiedItemListener getJumpListener() {
        return jumpListener;
    }
    
    // ============ 兼容旧代码的方法 ============
    
    /**
     * 设置是否搜索模式(兼容旧代码)
     */
    public void setSearch(boolean isSearch) {
        // 搜索适配器默认就是搜索模式,此方法仅为兼容保留
    }
    
    /**
     * 获取是否搜索模式(兼容旧代码)
     */
    public boolean getSearch() {
        return true;  // 搜索适配器始终为true
    }
    
    /**
     * 获取旧数据结构(兼容旧代码)
     */
    public ArrayList<ExpandableGroupEntity> getmGroups() {
        return new ArrayList<>();  // 返回空列表,新数据在groupDataList中
    }
    
    /**
     * 设置旧数据结构(兼容旧代码)
     */
    public void setmGroups(ArrayList<ExpandableGroupEntity> groups) {
        setGroups(groups);  // 委托给setGroups
    }
    
    // ============ 实现接口方法 ============
    
    @Override
    public void onExpandRequested(int groupPosition) {
        expandGroup(groupPosition, true);
    }
    
    @Override
    public void onCollapseRequested(int groupPosition) {
        collapseGroup(groupPosition, true);
    }
    
    @Override
    public void showToast(String message) {
        toast(message);
    }
}
