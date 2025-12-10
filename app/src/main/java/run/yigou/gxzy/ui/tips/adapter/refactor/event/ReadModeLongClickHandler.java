/*
 * 项目名: AndroidProject
 * 类名: ReadModeLongClickHandler.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.event
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 阅读模式长按处理器 - 处理阅读界面的长按事件(复制/跳转/重新下载)
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.event;

import android.content.Context;

import androidx.annotation.NonNull;

import com.hjq.http.EasyLog;

import run.yigou.gxzy.common.AppConst;
import run.yigou.gxzy.ui.tips.adapter.refactor.utils.ClipboardHelper;
import run.yigou.gxzy.ui.tips.adapter.refactor.model.ItemData;
import run.yigou.gxzy.ui.tips.entity.ChildEntity;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;

/**
 * 阅读模式长按处理器
 * 处理阅读界面的长按菜单(复制/跳转到本章内容/重新下载)
 */
public class ReadModeLongClickHandler implements LongClickEventHandler {

    private final Context context;
    private final OnMenuActionListener menuActionListener;
    private Object jumpListener;  // 跳转监听器(动态类型,兼容不同接口)

    /**
     * 菜单动作监听器
     */
    public interface OnMenuActionListener {
        /**
         * 请求跳转到指定章节
         *
         * @param groupPosition 组位置
         * @param childPosition 子项位置(-1表示章节头部)
         */
        void onJumpRequested(int groupPosition, int childPosition);

        /**
         * 请求重新下载全部数据
         */
        void onRedownloadAllRequested();

        /**
         * 请求重新下载本章节
         *
         * @param groupPosition 组位置
         */
        void onRedownloadChapterRequested(int groupPosition);

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
     * @param context            上下文
     * @param menuActionListener 菜单动作监听器
     */
    public ReadModeLongClickHandler(@NonNull Context context,
                                     @NonNull OnMenuActionListener menuActionListener) {
        this.context = context;
        this.menuActionListener = menuActionListener;
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
        // 显示菜单(阅读模式菜单类型)
        TipsNetHelper.showListDialog(context, AppConst.data_Type)
                .setListener((dialog, position, string) -> {
                    handleMenuAction(String.valueOf(string), groupPosition, childPosition, text);
                })
                .show();

        return true;
    }

    /**
     * 处理菜单动作
     *
     * @param action        菜单项文本
     * @param groupPosition 组位置
     * @param childPosition 子项位置
     * @param text          当前显示的文本
     */
    private void handleMenuAction(@NonNull String action,
                                    int groupPosition,
                                    int childPosition,
                                    @NonNull CharSequence text) {
        switch (action) {
            case "拷贝内容":
                handleCopyAction(text);
                break;

            case "跳转到本章内容":
                handleJumpAction(groupPosition);
                break;

            case "重新下载全部数据":
                handleRedownloadAllAction();
                break;

            case "重新下本章节":
                handleRedownloadChapterAction(groupPosition);
                break;

            default:
                EasyLog.print("Unknown menu action: " + action);
                break;
        }
    }

    /**
     * 处理复制动作
     *
     * @param text 要复制的文本
     */
    private void handleCopyAction(@NonNull CharSequence text) {
        if (text == null || text.length() == 0) {
            menuActionListener.showToast("内容为空，无法拷贝");
            return;
        }

        boolean success = ClipboardHelper.copyText(context, text.toString(), false);
        if (success) {
            menuActionListener.showToast("已复制到剪贴板");
        } else {
            menuActionListener.showToast("复制失败");
        }
    }

    /**
     * 处理跳转动作
     *
     * @param groupPosition 组位置
     */
    private void handleJumpAction(int groupPosition) {
        if (groupPosition > 0) {
            menuActionListener.onJumpRequested(groupPosition, -1);
        }
    }

    /**
     * 处理重新下载全部数据动作
     */
    private void handleRedownloadAllAction() {
        menuActionListener.onRedownloadAllRequested();
    }

    /**
     * 处理重新下载本章节动作
     *
     * @param groupPosition 组位置
     */
    private void handleRedownloadChapterAction(int groupPosition) {
        menuActionListener.onRedownloadChapterRequested(groupPosition);
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
        // 显示菜单(阅读模式菜单类型)
        TipsNetHelper.showListDialog(context, AppConst.data_Type)
                .setListener((dialog, position, string) -> {
                    handleMenuActionNew(String.valueOf(string), groupPosition, childPosition, text);
                })
                .show();

        return true;
    }
    
    /**
     * 处理菜单动作 - 新版本使用jumpListener
     */
    private void handleMenuActionNew(@NonNull String action,
                                       int groupPosition,
                                       int childPosition,
                                       @NonNull CharSequence text) {
        switch (action) {
            case "拷贝内容":
                handleCopyAction(text);
                break;

            case "跳转到本章内容":
                handleJumpActionNew(groupPosition);
                break;

            case "重新下载全部数据":
                handleRedownloadAllAction();
                break;

            case "重新下本章节":
                handleRedownloadChapterAction(groupPosition);
                break;

            default:
                EasyLog.print("Unknown menu action: " + action);
                break;
        }
    }
    
    /**
     * 处理跳转动作 - 新版本
     */
    private void handleJumpActionNew(int groupPosition) {
        if (jumpListener != null && groupPosition > 0) {
            try {
                // 使用反射调用,兼容不同接口
                jumpListener.getClass()
                    .getMethod("onJumpSpecifiedItem", int.class, int.class)
                    .invoke(jumpListener, groupPosition, -1);
            } catch (Exception e) {
                EasyLog.print("Jump failed: " + e.getMessage());
            }
        }
    }
}
