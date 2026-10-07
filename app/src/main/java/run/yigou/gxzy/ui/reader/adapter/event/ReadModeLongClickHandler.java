/*
 * 项目名: AndroidProject
 * 类名: ReadModeLongClickHandler.java
 * 包名: run.yigou.gxzy.ui.reader.adapter.event
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 阅读模式长按处理器 - 处理阅读界面的长按事件(复制/跳转/重新下载)
 */

package run.yigou.gxzy.ui.reader.adapter.event;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.utils.ClipboardHelper;
import run.yigou.gxzy.ui.reader.adapter.BaseRefactoredAdapter;
import run.yigou.gxzy.ui.reader.adapter.model.GroupData;
import run.yigou.gxzy.ui.reader.adapter.model.ItemData;
import run.yigou.gxzy.ui.reader.entity.ChildEntity;
import run.yigou.gxzy.ui.reader.helper.TipsDialogHelper;

import java.util.List;

/**
 * 阅读模式长按处理器
 * 处理阅读界面的长按菜单(复制/跳转到本章内容/重新下载)
 */
public class ReadModeLongClickHandler implements LongClickEventHandler {

    private final Context context;
    private final OnMenuActionListener menuActionListener;

    /**
     * 当前列表数据源，用于按 {@code groupPosition} 取显示列表下标（T6）。
     *
     * <p>类型用 {@code BaseRefactoredAdapter} 而非具体适配器：它已暴露
     * {@code getGroupDataList()}，本类只需读这一个方法，无需依赖具体子类。
     */
    private final BaseRefactoredAdapter dataSource;

    @Nullable
    private BaseRefactoredAdapter.OnJumpSpecifiedItemListener jumpListener;

    /**
     * 菜单动作监听器
     */
    public interface OnMenuActionListener {
        /**
         * 请求跳转到指定章节
         *
         * @param chapterIndex 章节在显示列表中的下标（T6；由 resolveChapterIndex 换算而来）
         * @param childPosition 子项位置(-1表示章节头部)
         */
        void onJumpRequested(int chapterIndex, int childPosition);

        /**
         * 请求重新下载全部数据
         */
        void onRedownloadAllRequested();

        /**
         * 请求重新下载本章节
         *
         * @param chapterIndex 章节在显示列表中的下标（T6；由 resolveChapterIndex 换算而来）
         */
        void onRedownloadChapterRequested(int chapterIndex);

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
     * @param dataSource 当前列表数据源，用于 T6 的显示列表下标反查；由适配器传入自身
     */
    public ReadModeLongClickHandler(@NonNull Context context,
            @NonNull OnMenuActionListener menuActionListener,
            @NonNull BaseRefactoredAdapter dataSource) {
        this.context = context;
        this.menuActionListener = menuActionListener;
        this.dataSource = dataSource;
    }

    /**
     * 取指定分组对应的显示列表下标（T6）。
     *
     * <p>搜索态下列表是过滤后的结果，{@code groupPosition} 不是章节真实下标，
     * 必须用数据装载时绑定的 {@code chapterIndex}。查不到时返回
     * {@link GroupData#NO_CHAPTER_INDEX}，由调用方拦截动作 ——
     * <b>不得回退到用 groupPosition 兜底</b>，那正是本方法要消除的错章路径。
     *
     * @param groupPosition 分组下标
     * @return 显示列表下标；未绑定返回 {@link GroupData#NO_CHAPTER_INDEX}
     */
    private int resolveChapterIndex(int groupPosition) {
        List<GroupData> groupDataList = dataSource.getGroupDataList();
        if (groupDataList == null || groupPosition < 0 || groupPosition >= groupDataList.size()) {
            return GroupData.NO_CHAPTER_INDEX;
        }
        GroupData groupData = groupDataList.get(groupPosition);
        if (groupData == null) {
            return GroupData.NO_CHAPTER_INDEX;
        }
        return groupData.getChapterIndex();
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
        TipsDialogHelper.showListDialog(context, TipsDialogHelper.DIALOG_TYPE_COPY)
                .setListener((dialog, position, string) -> {
                    handleMenuAction(String.valueOf(string), groupPosition, childPosition, text);
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
     * 处理菜单动作（统一入口，同时处理旧版和新版跳转逻辑）
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
                handleJumpAction(resolveChapterIndex(groupPosition));
                break;

            case "重新下载全部数据":
                handleRedownloadAllAction();
                break;

            case "重新下本章节":
                handleRedownloadChapterAction(resolveChapterIndex(groupPosition));
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
     * 处理跳转动作（统一使用类型安全的jumpListener，若未设置则回退到menuActionListener）
     *
     * @param chapterIndex 章节在当前显示列表中的下标（T6）；{@link GroupData#NO_CHAPTER_INDEX} 表示未绑定
     */
    private void handleJumpAction(int chapterIndex) {
        // T6：未绑定（含越界）一律toast 拦截。条件是「< 0」而非「== -1」，
        // 这样即使将来出现其它负值也走同一条失败路径，不会静默执行。
        if (chapterIndex < 0) {
            EasyLog.print("ReadModeLongClickHandler", "跳转中断：chapterIndex=" + chapterIndex);
            menuActionListener.showToast("无法定位该章节，请重新进入本书");
            return;
        }
        // 第 0 章是合法章节，必须能跳转 —— 此前用「> 0」把第 0 章静默拦掉了，
        // 与 handleRedownloadChapterAction 的判定不一致（T6 修正为 >= 0）。
        if (jumpListener != null) {
            jumpListener.onJumpSpecifiedItem(chapterIndex, -1);
        } else {
            menuActionListener.onJumpRequested(chapterIndex, -1);
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
     * @param chapterIndex 章节在当前显示列表中的下标（T6）；{@link GroupData#NO_CHAPTER_INDEX} 表示未绑定
     */
    private void handleRedownloadChapterAction(int chapterIndex) {
        // 与 handleJumpAction 同一判定口径（T6）：「< 0」而非「== -1」，失败提示语也一致
        if (chapterIndex < 0) {
        EasyLog.print("ReadModeLongClickHandler", "重新下载中断：chapterIndex=" + chapterIndex);
            menuActionListener.showToast("无法定位该章节，请重新进入本书");
            return;
        }
        menuActionListener.onRedownloadChapterRequested(chapterIndex);
    }
    
    /**
     * 设置跳转监听器（类型安全，替代旧版Object参数）
     *
     * @param listener 跳转监听器
     */
    public void setJumpListener(@Nullable BaseRefactoredAdapter.OnJumpSpecifiedItemListener listener) {
        this.jumpListener = listener;
    }
}
