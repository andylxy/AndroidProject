/*
 * 项目名: AndroidProject
 * 类名: ViewHolderFactory.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.viewholder
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: ViewHolder工厂 - 统一创建和管理ViewHolder
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.viewholder;

import androidx.annotation.NonNull;

import com.donkingliang.groupedadapter.holder.BaseViewHolder;

/**
 * ViewHolder工厂
 * 统一创建和管理各种类型的ViewHolder
 */
public class ViewHolderFactory {

    /**
     * ViewHolder类型枚举
     */
    public enum ViewHolderType {
        HEADER,   // 头部ViewHolder
        CHILD     // 子项ViewHolder
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

    /**
     * 根据类型创建ViewHolder
     *
     * @param type   ViewHolder类型
     * @param holder BaseViewHolder
     * @return 对应类型的ViewHolder
     */
    @NonNull
    public static Object createViewHolder(@NonNull ViewHolderType type,
                                           @NonNull BaseViewHolder holder) {
        switch (type) {
            case HEADER:
                return createHeaderViewHolder(holder);
            case CHILD:
                return createChildViewHolder(holder);
            default:
                throw new IllegalArgumentException("Unknown ViewHolder type: " + type);
        }
    }

    /**
     * 判断ViewHolder是否为Header类型
     *
     * @param viewHolder ViewHolder对象
     * @return true表示是Header ViewHolder
     */
    public static boolean isHeaderViewHolder(@NonNull Object viewHolder) {
        return viewHolder instanceof TipsHeaderViewHolder;
    }

    /**
     * 判断ViewHolder是否为Child类型
     *
     * @param viewHolder ViewHolder对象
     * @return true表示是Child ViewHolder
     */
    public static boolean isChildViewHolder(@NonNull Object viewHolder) {
        return viewHolder instanceof TipsChildViewHolder;
    }
}
