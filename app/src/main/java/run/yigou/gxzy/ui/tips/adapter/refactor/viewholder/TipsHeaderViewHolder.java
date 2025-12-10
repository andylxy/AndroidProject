/*
 * 项目名: AndroidProject
 * 类名: TipsHeaderViewHolder.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.viewholder
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 头部ViewHolder - 封装Header绑定逻辑
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.viewholder;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.tips.entity.ExpandableGroupEntity;

/**
 * Tips头部ViewHolder
 * 封装Header数据绑定逻辑
 */
public class TipsHeaderViewHolder {

    private final TextView tvHeader;
    private final ImageView ivState;

    /**
     * 构造函数
     *
     * @param holder BaseViewHolder
     */
    public TipsHeaderViewHolder(@NonNull BaseViewHolder holder) {
        this.tvHeader = holder.get(R.id.tv_expandable_header);
        this.ivState = holder.get(R.id.iv_state);
    }

    /**
     * 绑定Header数据
     *
     * @param entity   数据实体
     * @param isExpand 是否展开
     */
    public void bind(@NonNull ExpandableGroupEntity entity, boolean isExpand) {
        // 设置Header文本(使用SpannableString)
        if (entity.getSpannableHeader() != null) {
            tvHeader.setText(entity.getSpannableHeader());
        } else {
            tvHeader.setText(entity.getHeader());
        }

        // 设置展开/收起图标旋转角度
        updateExpandState(isExpand);
    }

    /**
     * 更新展开状态
     *
     * @param isExpand 是否展开
     */
    public void updateExpandState(boolean isExpand) {
        if (ivState != null) {
            ivState.setRotation(isExpand ? 90 : 0);
        }
    }

    /**
     * 设置点击事件
     *
     * @param holder          BaseViewHolder
     * @param clickListener   点击监听器
     */
    public static void setClickListener(@NonNull BaseViewHolder holder,
                                         @NonNull View.OnClickListener clickListener) {
        holder.itemView.setOnClickListener(clickListener);
    }

    /**
     * 获取TextView控件
     *
     * @return TextView
     */
    public TextView getTextView() {
        return tvHeader;
    }

    /**
     * 获取ImageView控件
     *
     * @return ImageView
     */
    public ImageView getImageView() {
        return ivState;
    }
}
