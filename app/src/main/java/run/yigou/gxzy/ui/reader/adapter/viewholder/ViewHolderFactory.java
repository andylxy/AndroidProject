/*
 * 项目名: AndroidProject
 * 类名: ViewHolderFactory.java
 * 包名: run.yigou.gxzy.ui.reader.adapter.viewholder
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: ViewHolder工厂 - 统一创建和管理ViewHolder
 */

package run.yigou.gxzy.ui.reader.adapter.viewholder;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;

/**
 * ViewHolder工厂
 * 统一创建和管理各种类型的ViewHolder
 */
public class ViewHolderFactory {

    private ViewHolderFactory() {
    }

    /**
     * 创建Header ViewHolder
     *
     * @param holder BaseViewHolder
     * @return TipsHeaderViewHolder
     */
    @NonNull
    public static TipsHeaderViewHolder createHeaderViewHolder(@NonNull BaseViewHolder holder) {
        return new TipsHeaderViewHolder(holder);
    }

    /**
     * 创建Child ViewHolder
     *
     * @param holder BaseViewHolder
     * @return TipsChildViewHolder
     */
    @NonNull
    public static TipsChildViewHolder createChildViewHolder(@NonNull BaseViewHolder holder) {
        return new TipsChildViewHolder(holder);
    }
}
