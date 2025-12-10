/*
 * 项目名: AndroidProject
 * 类名: LongClickEventHandler.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.event
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 长按事件处理器接口 - 定义长按事件处理规范
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.event;

import androidx.annotation.NonNull;

import run.yigou.gxzy.ui.tips.entity.ChildEntity;

/**
 * 长按事件处理器接口
 * 定义Child的长按事件处理规范
 */
public interface LongClickEventHandler {

    /**
     * Child长按事件
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @param entity        数据实体
     * @param text          当前显示的文本
     * @return true表示消费事件
     */
    boolean onChildLongClick(int groupPosition,
                              int childPosition,
                              @NonNull ChildEntity entity,
                              @NonNull CharSequence text);
}
