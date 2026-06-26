package run.yigou.gxzy.ui.reader.ai.helper;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import run.yigou.gxzy.data.local.entity.ChatMessageBean;
import run.yigou.gxzy.utils.MarkdownUtils;

/**
 * 消息操作菜单助手：管理消息长按弹出菜单（重发、删除、复制、采用）
 *
 * <p>职责：
 * <ul>
 *   <li>构建 PopupWindow 菜单布局</li>
 *   <li>根据消息类型动态展示菜单项</li>
 *   <li>复制消息内容到剪贴板</li>
 *   <li>通过回调接口将操作委托给 Presenter</li>
 * </ul>
 */
public class ChatMessageMenuHelper {

    // PopupWindow 样式常量
    private static final int POPUP_CORNER_RADIUS = 8;
    private static final int POPUP_STROKE_WIDTH = 1;
    private static final int POPUP_LAYOUT_PADDING = 4;
    private static final int POPUP_ITEM_PADDING_HORIZONTAL = 24;
    private static final int POPUP_ITEM_PADDING_VERTICAL = 16;
    private static final float POPUP_ITEM_TEXT_SIZE = 14;
    private static final int POPUP_ELEVATION = 4;

    private final Activity activity;
    private final OnMessageMenuActionListener actionListener;

    public interface OnMessageMenuActionListener {
        void onResendMessage(String content);
        void onDeleteMessage(ChatMessageBean message);
        void onAdoptSummary(ChatMessageBean message);
    }

    public ChatMessageMenuHelper(Activity activity, OnMessageMenuActionListener listener) {
        this.activity = activity;
        this.actionListener = listener;
    }

    /**
     * 在指定位置显示消息操作菜单
     *
     * @param anchor  触发菜单的 View
     * @param message 目标消息
     * @param x       触摸 X 坐标（屏幕绝对坐标）
     * @param y       触摸 Y 坐标（屏幕绝对坐标）
     */
    public void showMenu(View anchor, ChatMessageBean message, float x, float y) {
        if (message == null || anchor == null || activity == null) return;

        String[] items;
        switch (message.getType()) {
            case ChatMessageBean.TYPE_SEND:
                items = new String[]{"重发", "删除", "复制"};
                break;
            case ChatMessageBean.TYPE_RECEIVED:
            case ChatMessageBean.TYPE_THINKING:
                items = new String[]{"删除", "复制"};
                break;
            case ChatMessageBean.TYPE_SUMMARY:
                items = new String[]{"复制", "删除", "采用"};
                break;
            default:
                return;
        }

        LinearLayout menuLayout = new LinearLayout(activity);
        menuLayout.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.WHITE);
        background.setCornerRadius(POPUP_CORNER_RADIUS);
        background.setStroke(POPUP_STROKE_WIDTH, Color.LTGRAY);
        menuLayout.setBackground(background);
        menuLayout.setPadding(POPUP_LAYOUT_PADDING, POPUP_LAYOUT_PADDING,
                POPUP_LAYOUT_PADDING, POPUP_LAYOUT_PADDING);

        final String[] menuItems = items;
        for (int i = 0; i < items.length; i++) {
            TextView menuItem = new TextView(activity);
            menuItem.setText(items[i]);
            menuItem.setPadding(POPUP_ITEM_PADDING_HORIZONTAL, POPUP_ITEM_PADDING_VERTICAL,
                    POPUP_ITEM_PADDING_HORIZONTAL, POPUP_ITEM_PADDING_VERTICAL);
            menuItem.setTextSize(POPUP_ITEM_TEXT_SIZE);
            menuItem.setTextColor(Color.BLACK);
            menuItem.setBackgroundResource(android.R.drawable.list_selector_background);
            menuLayout.addView(menuItem);
        }

        final PopupWindow popupWindow = new PopupWindow(
                menuLayout,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setElevation(POPUP_ELEVATION);

        for (int i = 0; i < menuLayout.getChildCount(); i++) {
            final int index = i;
            menuLayout.getChildAt(i).setOnClickListener(v -> {
                popupWindow.dismiss();
                String clickedItem = menuItems[index];

                if ("重发".equals(clickedItem)) {
                    actionListener.onResendMessage(message.getContent());
                } else if ("删除".equals(clickedItem)) {
                    actionListener.onDeleteMessage(message);
                } else if ("复制".equals(clickedItem)) {
                    copyToClipboard(message.getContent());
                } else if ("采用".equals(clickedItem)) {
                    actionListener.onAdoptSummary(message);
                }
            });
        }

        View decorView = activity.getWindow().getDecorView();
        int[] location = new int[2];
        decorView.getLocationOnScreen(location);
        int popupX = (int) x - location[0];
        int popupY = (int) y - location[1];
        popupWindow.showAtLocation(decorView, Gravity.NO_GRAVITY, popupX, popupY);
    }

    /**
     * 复制消息内容到系统剪贴板（Markdown 转纯文本后复制）
     */
    private void copyToClipboard(String content) {
        String plainText = MarkdownUtils.convertMarkdownToPlainText(content);
        ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("聊天内容", plainText);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(activity, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }
}