/*
 * 项目名: AndroidProject
 * 类名: HeaderBinder.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.binder
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: Header绑定器 - 绑定Header数据到ViewHolder
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.binder;

import androidx.annotation.NonNull;

import run.yigou.gxzy.ui.tips.adapter.refactor.model.GroupData;
import run.yigou.gxzy.ui.tips.adapter.refactor.state.ExpandStateManager;
import run.yigou.gxzy.ui.tips.adapter.refactor.viewholder.TipsHeaderViewHolder;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * Header绑定器
 * 负责将ExpandableGroupEntity绑定到TipsHeaderViewHolder
 */
public class HeaderBinder implements DataBinder<ExpandableGroupEntity, TipsHeaderViewHolder> {

    private final ExpandStateManager expandStateManager;

    /**
     * 构造函数
     *
     * @param expandStateManager 展开状态管理器
     */
    public HeaderBinder(@NonNull ExpandStateManager expandStateManager) {
        this.expandStateManager = expandStateManager;
    }

    /**
     * 绑定Header数据
     *
     * @param data       数据实体
     * @param viewHolder ViewHolder对象
     * @param position   位置(groupPosition)
     */
    @Override
    public void bind(@NonNull ExpandableGroupEntity data,
                     @NonNull TipsHeaderViewHolder viewHolder,
                     int position) {
        // 获取展开状态
        boolean isExpanded = expandStateManager.isExpanded(position);

        // 绑定数据
        viewHolder.bind(data, isExpanded);
    }

    /**
     * 解绑ViewHolder
     *
     * @param viewHolder ViewHolder对象
     */
    @Override
    public void unbind(@NonNull TipsHeaderViewHolder viewHolder) {
        // Header不需要特殊的解绑操作
    }
    
    /**
     * 绑定Header数据 - 使用新数据结构GroupData
     *
     * @param data       数据实体(GroupData)
     * @param viewHolder ViewHolder对象
     * @param position   位置(groupPosition)
     */
    public void bind(@NonNull GroupData data,
                     @NonNull TipsHeaderViewHolder viewHolder,
                     int position) {
        // 使用GroupData的富文本或纯文本
        if (data.hasTitleSpan()) {
            viewHolder.getTextView().setText(data.getTitleSpan());
        } else {
            viewHolder.getTextView().setText(data.getTitle());
        }
    }
}
