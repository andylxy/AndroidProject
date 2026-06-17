/*
 * 项目名: AndroidProject
 * 类名: RefactoredExpandableAdapter.java
 * 包名: run.yigou.gxzy.ui.reader.adapter
 * 描述: 重构后的可展开适配器 - 阅读模式专用
 */

package run.yigou.gxzy.ui.reader.adapter;

import android.content.Context;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import java.util.ArrayList;
import java.util.List;

import run.yigou.gxzy.R;
import run.yigou.gxzy.base.action.ToastAction;
import run.yigou.gxzy.ui.reader.adapter.event.ReadModeClickHandler;
import run.yigou.gxzy.ui.reader.adapter.event.ReadModeLongClickHandler;
import run.yigou.gxzy.ui.reader.adapter.model.DataAdapter;
import run.yigou.gxzy.ui.reader.adapter.model.GroupData;
import run.yigou.gxzy.ui.reader.adapter.model.ItemData;
import run.yigou.gxzy.ui.reader.adapter.viewholder.TipsChildViewHolder;
import run.yigou.gxzy.ui.reader.entity.ExpandableGroupEntity;

/**
 * 重构后的可展开适配器 - 阅读模式
 * <p>
 * 职责:
 * - 阅读模式下的章节展开/收起
 * - 委托事件处理给ReadModeClickHandler/ReadModeLongClickHandler
 * - 委托数据绑定给Binder
 * - 使用GroupData/ItemData数据结构
 * <p>
 * 公共功能(展开/收起/布局/兼容接口)已上提到BaseRefactoredAdapter
 */
public class RefactoredExpandableAdapter extends BaseRefactoredAdapter
        implements ToastAction,
                   ReadModeClickHandler.OnExpandToggleListener,
                   ReadModeLongClickHandler.OnMenuActionListener {

    /** 兼容旧接口的跳转监听器（继承基类接口以保持类型兼容） */
    public interface OnJumpSpecifiedItemListener extends BaseRefactoredAdapter.OnJumpSpecifiedItemListener {
    }

    // 事件处理器
    private final ReadModeLongClickHandler longClickHandler;

    public RefactoredExpandableAdapter(@NonNull Context context) {
        super(context);
        this.longClickHandler = new ReadModeLongClickHandler(context, this);
    }

    // ============ 数据管理 ============

    /**
     * 设置搜索结果数据（专用于全局搜索）
     * <p>
     * 直接转换为 model.GroupData/ItemData，保留ClickableSpan，
     * 避免 DataAdapter.convertList 的二次转换。
     *
     * @param entityGroupList 分组数据列表 (entity.GroupData)
     * @param entityItemList  条目数据列表 (entity.ItemData)
     */
    public void setSearchData(
            @NonNull List<run.yigou.gxzy.ui.reader.entity.GroupData> entityGroupList,
            @NonNull List<List<run.yigou.gxzy.ui.reader.entity.ItemData>> entityItemList) {

        List<GroupData> modelGroupList = new ArrayList<>();

        for (int i = 0; i < entityGroupList.size() && i < entityItemList.size(); i++) {
            run.yigou.gxzy.ui.reader.entity.GroupData sourceGroup = entityGroupList.get(i);
            List<run.yigou.gxzy.ui.reader.entity.ItemData> sourceItems = entityItemList.get(i);

            // 转换 entity.ItemData -> model.ItemData（保留ClickableSpan）
            List<ItemData> modelItems = new ArrayList<>();
            if (sourceItems != null) {
                for (run.yigou.gxzy.ui.reader.entity.ItemData src : sourceItems) {
                    android.text.SpannableStringBuilder textSpan = src.getAttributedText();
                    android.text.SpannableStringBuilder noteSpan = src.getAttributedNote();
                    android.text.SpannableStringBuilder videoSpan = src.getAttributedVideo();

                    modelItems.add(new ItemData(
                            textSpan != null ? textSpan.toString() : "",
                            noteSpan != null ? noteSpan.toString() : null,
                            videoSpan != null ? videoSpan.toString() : null,
                            src.getImageUrl(),
                            textSpan, noteSpan, videoSpan
                    ));
                }
            }

            modelGroupList.add(new GroupData(sourceGroup.getTitle(), modelItems));
        }

        this.groupDataList = new ArrayList<>(modelGroupList);

        // 同步展开状态（使用entity的展开状态）
        expandStateManager.reset();
        for (int i = 0; i < entityGroupList.size() && i < modelGroupList.size(); i++) {
            expandStateManager.setExpandState(i, entityGroupList.get(i).isExpanded());
        }

        notifyDataSetChanged();
    }

    // ============ 数据更新 ============

    /**
     * 更新指定位置的组数据（GroupData结构）
     *
     * @param position  组位置
     * @param groupData 新的组数据
     */
    public void updateGroupData(int position, @NonNull GroupData groupData) {
        if (position < 0 || position >= groupDataList.size()) {
            return;
        }
        groupDataList.set(position, groupData);
        // 不自动刷新界面，由调用者决定刷新策略
    }

    /**
     * 更新指定位置的组数据（从ExpandableGroupEntity转换）
     *
     * @param position 组位置
     * @param entity   旧的实体数据
     */
    public void updateGroupFromEntity(int position, @NonNull ExpandableGroupEntity entity) {
        if (position < 0 || position >= groupDataList.size()) {
            return;
        }

        // 转换为新数据结构并更新
        GroupData groupData = DataAdapter.fromExpandableGroupEntity(entity);
        groupDataList.set(position, groupData);

        // 同步entity到groups（兼容层）
        if (groups != null && position < groups.size()) {
            groups.set(position, entity);
        }

        // 同步展开状态
        expandStateManager.setExpandState(position, entity.isExpand());
        // 不自动刷新界面，由调用者决定刷新策略
    }

    // ============ 数据绑定 ============

    @Override
    public void onBindChildViewHolder(BaseViewHolder holder, int groupPosition, int childPosition) {
        // 公共绑定（ViewHolder + Binder + 长按监听）
        TipsChildViewHolder childVH = performChildBindingSetup(holder, groupPosition, childPosition);
        GroupData groupData = groupDataList.get(groupPosition);
        ItemData itemData = groupData.getItem(childPosition);

        // 阅读模式特有：设置点击切换显示模式（text/note/video）
        TextView sectionText = holder.get(R.id.tv_sectiontext);
        TextView sectionNote = holder.get(R.id.tv_sectionnote);
        TextView sectionVideo = holder.get(R.id.tv_sectionvideo);

        if (sectionText != null) {
            sectionText.setOnClickListener(v ->
                    childVH.toggleTextVisibility(itemData.getNoteSpan()));
        }

        if (sectionNote != null) {
            sectionNote.setOnClickListener(v ->
                    childVH.toggleNoteVisibility(itemData.getVideoSpan()));
        }

        if (sectionVideo != null) {
            sectionVideo.setOnClickListener(v ->
                    childVH.toggleVideoVisibility());
        }
    }

    /**
     * 子项长按事件 - 委托给ReadModeLongClickHandler
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
     * 设置搜索模式（兼容旧代码，阅读模式不使用）
     */
    public void setSearch(boolean search) {
        // 阅读模式不使用搜索模式
    }

    /**
     * 获取搜索模式（兼容旧代码）
     */
    public boolean getSearch() {
        return false;
    }

    // ============ 实现 ReadModeClickHandler.OnExpandToggleListener ============

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

    // ============ 实现 ReadModeLongClickHandler.OnMenuActionListener ============

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
