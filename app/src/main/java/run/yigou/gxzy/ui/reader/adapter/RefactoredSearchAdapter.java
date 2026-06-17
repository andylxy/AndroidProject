/*
 * 项目名: AndroidProject
 * 类名: RefactoredSearchAdapter.java
 * 包名: run.yigou.gxzy.ui.reader.adapter
 * 描述: 重构后的搜索适配器 - 搜索模式专用
 */

package run.yigou.gxzy.ui.reader.adapter;

import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.util.ArrayList;

import run.yigou.gxzy.R;
import run.yigou.gxzy.base.action.ToastAction;
import run.yigou.gxzy.ui.reader.adapter.binder.ChildTextBinder;
import run.yigou.gxzy.ui.reader.adapter.binder.HeaderBinder;
import run.yigou.gxzy.ui.reader.adapter.event.SearchModeClickHandler;
import run.yigou.gxzy.ui.reader.adapter.event.SearchModeLongClickHandler;
import run.yigou.gxzy.ui.reader.adapter.model.DataAdapter;
import run.yigou.gxzy.ui.reader.adapter.model.GroupData;
import run.yigou.gxzy.ui.reader.adapter.model.ItemData;
import run.yigou.gxzy.ui.reader.adapter.viewholder.TipsChildViewHolder;
import run.yigou.gxzy.ui.reader.adapter.viewholder.TipsHeaderViewHolder;
import run.yigou.gxzy.ui.reader.adapter.viewholder.ViewHolderFactory;
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
    private final SearchModeClickHandler clickHandler;
    private final SearchModeLongClickHandler longClickHandler;

    public RefactoredSearchAdapter(@NonNull Context context) {
        super(context);
        this.clickHandler = new SearchModeClickHandler(context, this);
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
    public void onBindHeaderViewHolder(BaseViewHolder holder, int groupPosition) {
        GroupData groupData = groupDataList.get(groupPosition);

        TipsHeaderViewHolder headerVH = ViewHolderFactory.createHeaderViewHolder(holder);
        HeaderBinder binder = binderFactory.createHeaderBinder();
        binder.bind(groupData, headerVH, groupPosition);

        ImageView ivState = holder.get(R.id.iv_state);
        if (ivState != null) {
            ivState.setRotation(expandStateManager.isExpanded(groupPosition) ? 90f : 0f);
        }
    }

    @Override
    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
        GroupData groupData = groupDataList.get(groupPosition);
        ItemData itemData = groupData.getItem(childPosition);

        TipsChildViewHolder childVH = ViewHolderFactory.createChildViewHolder(holder);
        ChildTextBinder binder = binderFactory.createChildTextBinder();
        binder.bind(itemData, childVH, childPosition);

        // 搜索模式下注释掉setOnClickListener，避免与LocalLinkMovementMethod冲突
        TextView sectionText = holder.get(R.id.tv_sectiontext);
        TextView sectionNote = holder.get(R.id.tv_sectionnote);
        TextView sectionVideo = holder.get(R.id.tv_sectionvideo);

        if (sectionText != null) {
            sectionText.setOnLongClickListener(v -> {
                CharSequence text = itemData.hasTextSpan()
                        ? itemData.getTextSpan() : sectionText.getText();
                return longClickHandler.onChildLongClick(groupPosition, childPosition, itemData, text);
            });
        }

        if (sectionNote != null) {
            sectionNote.setOnLongClickListener(v -> {
                CharSequence text = itemData.hasNoteSpan()
                        ? itemData.getNoteSpan() : sectionNote.getText();
                return longClickHandler.onChildLongClick(groupPosition, childPosition, itemData, text);
            });
        }

        if (sectionVideo != null) {
            sectionVideo.setOnLongClickListener(v -> {
                CharSequence text = itemData.hasVideoSpan()
                        ? itemData.getVideoSpan() : sectionVideo.getText();
                return longClickHandler.onChildLongClick(groupPosition, childPosition, itemData, text);
            });
        }
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
