/*
 * 项目名: AndroidProject
 * 类名: SearchModeLongClickHandler.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.event
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 搜索模式长按处理器 - 处理搜索界面的长按事件(仅复制功能)
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.event;

import android.content.Context;

import androidx.annotation.NonNull;

import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.ui.tips.adapter.refactor.utils.ClipboardHelper;
import run.yigou.gxzy.ui.tips.adapter.refactor.model.ItemData;
import run.yigou.gxzy.ui.tips.entity.ChildEntity;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;

/**
 * 搜索模式长按处理器
 * 处理搜索界面的长按菜单(仅复制功能)
 */
public class SearchModeLongClickHandler implements LongClickEventHandler {

    private final Context context;
    private final OnToastListener toastListener;
    private Object jumpListener;  // 跳转监听器

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
    public SearchModeLongClickHandler(@NonNull Context context,
                                       @NonNull OnToastListener toastListener) {
        this.context = context;
        this.toastListener = toastListener;
    }

    /**
     * Child长按事件
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
        // 显示菜单(搜索模式菜单类型,仅复制功能)
        TipsNetHelper.showListDialog(context, AppConst.noFooter_Type)
                .setListener((dialog, position, string) -> {
                    if ("拷贝内容".equals(string)) {
                        handleCopyAction(text);
                    }
                })
                .show();

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

    /**
     * 处理复制动作
     *
     * @param text 要复制的文本
     */
    private void handleCopyAction(@NonNull CharSequence text) {
        if (text == null || text.length() == 0) {
            toastListener.showToast("内容为空，无法拷贝");
            return;
        }

        boolean success = ClipboardHelper.copyText(context, text.toString(), false);
        if (success) {
            toastListener.showToast("已复制到剪贴板");
        } else {
            toastListener.showToast("复制失败");
        }
    }
    
    /**
     * 设置跳转监听器(兼容旧接口)
     */
    public void setJumpListener(Object listener) {
        this.jumpListener = listener;
    }
    
    /**
     * Child长按事件 - 使用新数据结构ItemData
     */
    public boolean onChildLongClick(int groupPosition,
                                     int childPosition,
                                     @NonNull ItemData itemData,
                                     @NonNull String text) {
        // 显示菜单(搜索模式菜单类型,仅复制功能)
        TipsNetHelper.showListDialog(context, AppConst.noFooter_Type)
                .setListener((dialog, position, string) -> {
                    if ("拷贝内容".equals(string)) {
                        handleCopyAction(text);
                    }
                })
                .show();

        return true;
    }
}
