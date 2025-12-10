/*
 * 项目名: AndroidProject
 * 类名: DataBinder.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.binder
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 数据绑定器接口 - 定义数据绑定规范
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.binder;

import androidx.annotation.NonNull;

/**
 * 数据绑定器接口
 * 将数据绑定到ViewHolder的通用接口
 *
 * @param <T>  数据类型
 * @param <VH> ViewHolder类型
 */
public interface DataBinder<T, VH> {

    /**
     * 绑定数据到ViewHolder
     *
     * @param data       数据对象
     * @param viewHolder ViewHolder对象
     * @param position   位置
     */
    void bind(@NonNull T data, @NonNull VH viewHolder, int position);

    /**
     * 解绑ViewHolder(可选,用于释放资源)
     *
     * @param viewHolder ViewHolder对象
     */
    default void unbind(@NonNull VH viewHolder) {
        // 默认空实现
    }
}
