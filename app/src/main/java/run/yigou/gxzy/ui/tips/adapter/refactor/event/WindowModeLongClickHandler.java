/*
 * 项目名: AndroidProject
 * 类名: WindowModeLongClickHandler.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.event
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 弹窗模式长按处理器 - 处理弹窗界面的长按事件(简单复制功能)
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.event;

import android.content.Context;

import androidx.annotation.NonNull;

import run.yigou.gxzy.ui.tips.adapter.refactor.model.ItemData;
import run.yigou.gxzy.ui.tips.adapter.refactor.utils.ClipboardHelper;
import run.yigou.gxzy.ui.tips.entity.ChildEntity;

/**
 * 弹窗模式长按处理器
 * 处理弹窗界面的长按事件(直接复制,无菜单)
 */
public class WindowModeLongClickHandler implements LongClickEventHandler {

    private final Context context;
    private final OnToastListener toastListener;

    /**
     * Toast监听器
     */
    public interface OnToastListener {
        /**
         * 显示Toast消息
         *
         * @param message 消息内容
         */
        void showToast(String message);
    }

    /**
     * 构造函数
     *
     * @param context       上下文
     * @param toastListener Toast监听器
     */
    public WindowModeLongClickHandler(@NonNull Context context,
                                       @NonNull OnToastListener toastListener) {
        this.context = context;
        this.toastListener = toastListener;
    }

    /**
     * Child长按事件 - 直接复制文本
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @param entity        数据实体
     * @param text          当前显示的文本
     * @return true表示消费事件
     */
    @Override
    public boolean onChildLongClick(int groupPosition,
                                     int childPosition,
                                     @NonNull ChildEntity entity,
                                     @NonNull CharSequence text) {
        // 直接复制文本,无菜单
        if (text == null || text.length() == 0) {
            toastListener.showToast("内容为空，无法拷贝");
            return true;
        }

        boolean success = ClipboardHelper.copyText(context, text.toString(), false);
        if (success) {
            toastListener.showToast("已复制到剪贴板");
        } else {
            toastListener.showToast("复制失败");
        }

        return true;
    }
    
    /**
     * Child长按事件 - 使用新数据结构ItemData
     *
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @param itemData      数据实体(新结构)
     * @param text          当前显示的文本
     * @return true表示消费事件
     */
    @Override
    public boolean onChildLongClick(int groupPosition,
                                     int childPosition,
                                     @NonNull ItemData itemData,
                                     @NonNull CharSequence text) {
        // 直接使用文本处理，无需entity信息
        return onChildLongClick(groupPosition, childPosition, (ChildEntity) null, text);
    }
}
