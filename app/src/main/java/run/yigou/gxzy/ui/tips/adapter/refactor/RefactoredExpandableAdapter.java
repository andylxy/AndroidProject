/*
 * 项目名: AndroidProject
 * 类名: RefactoredExpandableAdapter.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 重构后的可展开适配器 - 阅读模式专用
 */

package run.yigou.gxzy.ui.tips.adapter.refactor;

import android.content.Context;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;
import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.action.ToastAction;
import run.yigou.gxzy.ui.tips.adapter.refactor.binder.ChildTextBinder;
import run.yigou.gxzy.ui.tips.adapter.refactor.binder.HeaderBinder;
import run.yigou.gxzy.ui.tips.adapter.refactor.event.ReadModeClickHandler;
import run.yigou.gxzy.ui.tips.adapter.refactor.event.ReadModeLongClickHandler;
import run.yigou.gxzy.ui.tips.adapter.refactor.model.DataAdapter;
import run.yigou.gxzy.ui.tips.adapter.refactor.model.GroupData;
import run.yigou.gxzy.ui.tips.adapter.refactor.model.ItemData;
import run.yigou.gxzy.ui.tips.adapter.refactor.viewholder.TipsChildViewHolder;
import run.yigou.gxzy.ui.tips.adapter.refactor.viewholder.TipsHeaderViewHolder;
import run.yigou.gxzy.ui.tips.adapter.refactor.viewholder.ViewHolderFactory;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * 重构后的可展开适配器
 * 
 * 职责:
 * - 阅读模式下的章节展开/收起
 * - 委托事件处理给Handler
 * - 委托数据绑定给Binder
 * - 使用新的数据结构GroupData/ItemData
 */
public class RefactoredExpandableAdapter extends BaseRefactoredAdapter 
        implements ToastAction, 
                   ReadModeClickHandler.OnExpandToggleListener,
                   ReadModeLongClickHandler.OnMenuActionListener {

    // 新数据结构
    private List<GroupData> groupDataList;
    
    // 事件处理器
    private final ReadModeClickHandler clickHandler;
    private final ReadModeLongClickHandler longClickHandler;
    
    // 跳转监听器(兼容旧接口)
    private OnJumpSpecifiedItemListener jumpListener;

    public RefactoredExpandableAdapter(@NonNull Context context) {
        super(context);
        
        EasyLog.print("========== RefactoredExpandableAdapter 构造函数 ==========");
        EasyLog.print("实例创建: " + this);
        
        // 初始化事件处理器
        this.clickHandler = new ReadModeClickHandler(context, this);
        this.longClickHandler = new ReadModeLongClickHandler(context, this);
        
        this.groupDataList = new ArrayList<>();
        
        EasyLog.print("RefactoredExpandableAdapter 初始化完成");
    }

    /**
     * 设置数据 - 使用新数据结构
     */
    public void setGroupDataList(@NonNull List<GroupData> groupDataList) {
        this.groupDataList = new ArrayList<>(groupDataList);
        // 同步展开状态
        expandStateManager.reset();
        for (int i = 0; i < groupDataList.size(); i++) {
            expandStateManager.setExpandState(i, false);  // 默认收起
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
        // 同步展开状态
        expandStateManager.syncFromData(groups);
        notifyDataSetChanged();
    }

    @Override
    public int getGroupCount() {
        return groupDataList.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        // 如果当前组收起,返回0(关键:实现展开收起效果)
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
        return false;  // 去掉footer,更像ExpandableListView
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
        // 不使用Footer,留空实现
    }

    @Override
    public void onBindHeaderViewHolder(BaseViewHolder holder, int groupPosition) {
        EasyLog.print("=== RefactoredExpandableAdapter.onBindHeaderViewHolder 调用 ===");
        EasyLog.print("groupPosition: " + groupPosition);
        
        GroupData groupData = groupDataList.get(groupPosition);
        
        EasyLog.print("GroupData: title=" + groupData.getTitle() + ", itemCount=" + groupData.getItemCount());
        
        // 使用ViewHolder封装
        TipsHeaderViewHolder headerVH = ViewHolderFactory.createHeaderViewHolder(holder);
        
        // 使用Binder绑定数据
        HeaderBinder binder = binderFactory.createHeaderBinder();
        binder.bind(groupData, headerVH, groupPosition);
        
        // 设置展开/收起图标(使用旋转实现)
        ImageView ivState = holder.get(R.id.iv_state);
        if (ivState != null) {
            // 使用旋转角度实现箭头方向
            ivState.setRotation(
                expandStateManager.isExpanded(groupPosition) 
                    ? 90f   // 展开:箭头向下
                    : 0f    // 收起:箭头向右
            );
        }
        
        // 注意: Header点击事件通过setOnHeaderClickListener设置，不在此处设置
    }

    @Override
    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
        EasyLog.print("=== RefactoredExpandableAdapter.onBindChildViewHolder 调用 ===");
        EasyLog.print("groupPosition: " + groupPosition + ", childPosition: " + childPosition);
        
        GroupData groupData = groupDataList.get(groupPosition);
        ItemData itemData = groupData.getItem(childPosition);
        
        EasyLog.print("ItemData: hasTextSpan=" + itemData.hasTextSpan());
        
        // 使用ViewHolder封装
        TipsChildViewHolder childVH = ViewHolderFactory.createChildViewHolder(holder);
        
        // 使用Binder绑定数据
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
        
        // ✅ 同步entity状态 - 保证状态管理器和entity一致
        if (groupPosition >= 0 && groupPosition < groups.size()) {
            groups.get(groupPosition).setExpand(true);
        }
        
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
        
        // ✅ 同步entity状态 - 保证状态管理器和entity一致
        if (groupPosition >= 0 && groupPosition < groups.size()) {
            groups.get(groupPosition).setExpand(false);
        }
        
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
            // ✅ 同步entity状态
            if (i < groups.size()) {
                groups.get(i).setExpand(true);
            }
        }
        notifyDataSetChanged();
    }

    /**
     * 收起所有组
     */
    public void collapseAll() {
        for (int i = 0; i < getGroupCount(); i++) {
            expandStateManager.setExpandState(i, false);
            // ✅ 同步entity状态
            if (i < groups.size()) {
                groups.get(i).setExpand(false);
            }
        }
        notifyDataSetChanged();
    }

    /**
     * 更新指定位置的组数据（使用新的GroupData结构）
     * 这是重构后的核心数据更新方法
     * 
     * @param position 组位置
     * @param groupData 新的组数据
     */
    public void updateGroupData(int position, @NonNull GroupData groupData) {
        if (position < 0 || position >= groupDataList.size()) {
            com.hjq.http.EasyLog.print("RefactoredExpandableAdapter", 
                "updateGroupData: 位置越界 position=" + position + ", size=" + groupDataList.size());
            return;
        }
        
        // 更新groupDataList（唯一数据源）
        groupDataList.set(position, groupData);
        
        // 同步更新groups（兼容层 - 反向转换）
        if (groups != null && position < groups.size()) {
            // TODO: 如果需要反向转换GroupData -> ExpandableGroupEntity
            // groups.set(position, convertToEntity(groupData));
        }
        
        com.hjq.http.EasyLog.print("RefactoredExpandableAdapter", 
            "updateGroupData: 已更新位置=" + position + ", 标题=" + groupData.getTitle());
        
        // 注意：不自动刷新界面，由调用者决定刷新策略
    }

    /**
     * 更新指定位置的组数据（从旧的ExpandableGroupEntity转换）
     * 兼容旧代码调用方式，自动转换为新的GroupData结构
     * 
     * @param position 组位置
     * @param entity 旧的实体数据
     */
    public void updateGroupFromEntity(int position, @NonNull ExpandableGroupEntity entity) {
        if (position < 0 || position >= groupDataList.size()) {
            com.hjq.http.EasyLog.print("RefactoredExpandableAdapter", 
                "updateGroupFromEntity: 位置越界 position=" + position + ", size=" + groupDataList.size());
            return;
        }
        
        // 1. 转换为新数据结构
        GroupData groupData = DataAdapter.fromExpandableGroupEntity(entity);
        
        // 2. 更新groupDataList
        groupDataList.set(position, groupData);
        
        // 3. 同步entity到groups（兼容层）
        if (groups != null && position < groups.size()) {
            groups.set(position, entity);
        }
        
        // 4. 同步展开状态到状态管理器
        expandStateManager.setExpandState(position, entity.isExpand());
        
        com.hjq.http.EasyLog.print("RefactoredExpandableAdapter", 
            "updateGroupFromEntity: 已更新位置=" + position + 
            ", 标题=" + entity.getHeader() + 
            ", 展开=" + entity.isExpand() +
            ", 子项数=" + (entity.getChildren() != null ? entity.getChildren().size() : 0));
        
        // 注意：不自动刷新界面，由调用者决定刷新策略
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
    
    // ============ 兼容旧接口 ============
    
    /**
     * 设置搜索模式(兼容旧代码,实际不使用)
     */
    public void setSearch(boolean search) {
        // RefactoredExpandableAdapter不使用搜索模式
        // 搜索功能在RefactoredSearchAdapter中
    }
    
    /**
     * 获取搜索模式(兼容旧代码)
     */
    public boolean getSearch() {
        return false;  // 始终返回false
    }
    
    /**
     * 获取旧数据结构(兼容旧代码)
     */
    public ArrayList<ExpandableGroupEntity> getmGroups() {
        return groups;  // 返回BaseRefactoredAdapter中的groups
    }
    
    /**
     * 设置旧数据结构(兼容旧代码)
     */
    public void setmGroups(ArrayList<ExpandableGroupEntity> groups) {
        setGroups(groups);  // 委托给setGroups，会自动同步状态
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
    public void onJumpRequested(int groupPosition, int childPosition) {
        if (jumpListener != null) {
            jumpListener.onJumpSpecifiedItem(groupPosition, childPosition);
        }
    }
    
    @Override
    public void onRedownloadAllRequested() {
        toast("重新下载全部数据功能暂未实现");
    }
    
    @Override
    public void onRedownloadChapterRequested(int groupPosition) {
        toast("重新下载章节功能暂未实现");
    }
    
    @Override
    public void showToast(String message) {
        toast(message);
    }
}
