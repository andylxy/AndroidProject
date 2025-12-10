/*
 * 项目名: AndroidProject
 * 类名: TextViewHelper.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.utils
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: TextView显示切换工具类 - 统一管理TextView的text/note/video三种模式切换
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.utils;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

/**
 * TextView显示切换工具类
 * 用于管理三段式TextView的显示逻辑(text/note/video)
 */
public class TextViewHelper {

    /**
     * 显示模式枚举
     */
    public enum DisplayMode {
        TEXT,    // 显示正文
        NOTE,    // 显示注释
        VIDEO    // 显示视频
    }

    /**
     * 切换TextView显示模式
     *
     * @param textView   正文TextView
     * @param noteView   注释TextView
     * @param videoView  视频TextView
     * @param mode       目标显示模式
     */
    public static void switchDisplayMode(@NonNull TextView textView,
                                          @NonNull TextView noteView,
                                          @NonNull TextView videoView,
                                          @NonNull DisplayMode mode) {
        switch (mode) {
            case TEXT:
                showText(textView, noteView, videoView);
                break;
            case NOTE:
                showNote(textView, noteView, videoView);
                break;
            case VIDEO:
                showVideo(textView, noteView, videoView);
                break;
        }
    }

    /**
     * 显示正文模式
     */
    public static void showText(@NonNull TextView textView,
                                 @NonNull TextView noteView,
                                 @NonNull TextView videoView) {
        textView.setVisibility(View.VISIBLE);
        noteView.setVisibility(View.GONE);
        videoView.setVisibility(View.GONE);
    }

    /**
     * 显示注释模式
     */
    public static void showNote(@NonNull TextView textView,
                                 @NonNull TextView noteView,
                                 @NonNull TextView videoView) {
        textView.setVisibility(View.GONE);
        noteView.setVisibility(View.VISIBLE);
        videoView.setVisibility(View.GONE);
    }

    /**
     * 显示视频模式
     */
    public static void showVideo(@NonNull TextView textView,
                                  @NonNull TextView noteView,
                                  @NonNull TextView videoView) {
        textView.setVisibility(View.GONE);
        noteView.setVisibility(View.GONE);
        videoView.setVisibility(View.VISIBLE);
    }

    /**
     * 循环切换显示模式
     * text -> note -> video -> text
     *
     * @param textView   正文TextView
     * @param noteView   注释TextView
     * @param videoView  视频TextView
     * @return 切换后的模式
     */
    public static DisplayMode toggleVisibility(@NonNull TextView textView,
                                                @NonNull TextView noteView,
                                                @NonNull TextView videoView) {
        if (textView.getVisibility() == View.VISIBLE) {
            showNote(textView, noteView, videoView);
            return DisplayMode.NOTE;
        } else if (noteView.getVisibility() == View.VISIBLE) {
            showVideo(textView, noteView, videoView);
            return DisplayMode.VIDEO;
        } else {
            showText(textView, noteView, videoView);
            return DisplayMode.TEXT;
        }
    }

    /**
     * 获取当前显示模式
     *
     * @param textView   正文TextView
     * @param noteView   注释TextView
     * @param videoView  视频TextView
     * @return 当前显示模式
     */
    public static DisplayMode getCurrentMode(@NonNull TextView textView,
                                              @NonNull TextView noteView,
                                              @NonNull TextView videoView) {
        if (textView.getVisibility() == View.VISIBLE) {
            return DisplayMode.TEXT;
        } else if (noteView.getVisibility() == View.VISIBLE) {
            return DisplayMode.NOTE;
        } else {
            return DisplayMode.VIDEO;
        }
    }

    /**
     * 判断某个模式是否可用(TextView是否有内容)
     *
     * @param textView TextView对象
     * @return true表示可用
     */
    public static boolean isModeAvailable(@NonNull TextView textView) {
        CharSequence text = textView.getText();
        return text != null && text.length() > 0;
    }
}
