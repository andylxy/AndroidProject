/*
 * 项目名: AndroidProject
 * 类名: ClipboardHelper.java
 * 包名: run.yigou.gxzy.ui.tips.adapter.refactor.utils
 * 作者: Refactor Team
 * 创建时间: 2025年12月10日
 * 描述: 剪贴板工具类 - 统一管理文本复制功能
 */

package run.yigou.gxzy.ui.tips.adapter.refactor.utils;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * 剪贴板工具类
 * 统一管理文本复制到剪贴板的功能
 */
public class ClipboardHelper {

    /**
     * 复制文本到剪贴板
     *
     * @param context 上下文
     * @param text    要复制的文本
     * @return true表示复制成功
     */
    public static boolean copyText(@NonNull Context context, @Nullable String text) {
        return copyText(context, text, true);
    }

    /**
     * 复制文本到剪贴板
     *
     * @param context   上下文
     * @param text      要复制的文本
     * @param showToast 是否显示Toast提示
     * @return true表示复制成功
     */
    public static boolean copyText(@NonNull Context context,
                                    @Nullable String text,
                                    boolean showToast) {
        if (text == null || text.isEmpty()) {
            if (showToast) {
                Toast.makeText(context, "没有可复制的内容", Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                if (showToast) {
                    Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            ClipData clip = ClipData.newPlainText("text", text);
            clipboard.setPrimaryClip(clip);

            if (showToast) {
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (showToast) {
                Toast.makeText(context, "复制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    /**
     * 复制文本到剪贴板 - 带标签
     *
     * @param context 上下文
     * @param label   剪贴板标签
     * @param text    要复制的文本
     * @return true表示复制成功
     */
    public static boolean copyTextWithLabel(@NonNull Context context,
                                             @NonNull String label,
                                             @Nullable String text) {
        return copyTextWithLabel(context, label, text, true);
    }

    /**
     * 复制文本到剪贴板 - 带标签
     *
     * @param context   上下文
     * @param label     剪贴板标签
     * @param text      要复制的文本
     * @param showToast 是否显示Toast提示
     * @return true表示复制成功
     */
    public static boolean copyTextWithLabel(@NonNull Context context,
                                             @NonNull String label,
                                             @Nullable String text,
                                             boolean showToast) {
        if (text == null || text.isEmpty()) {
            if (showToast) {
                Toast.makeText(context, "没有可复制的内容", Toast.LENGTH_SHORT).show();
            }
            return false;
        }

        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                if (showToast) {
                    Toast.makeText(context, "复制失败", Toast.LENGTH_SHORT).show();
                }
                return false;
            }

            ClipData clip = ClipData.newPlainText(label, text);
            clipboard.setPrimaryClip(clip);

            if (showToast) {
                Toast.makeText(context, "已复制: " + label, Toast.LENGTH_SHORT).show();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            if (showToast) {
                Toast.makeText(context, "复制失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    /**
     * 从剪贴板获取文本
     *
     * @param context 上下文
     * @return 剪贴板中的文本,如果为空返回null
     */
    @Nullable
    public static String getText(@NonNull Context context) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                return null;
            }

            ClipData clipData = clipboard.getPrimaryClip();
            if (clipData == null || clipData.getItemCount() == 0) {
                return null;
            }

            ClipData.Item item = clipData.getItemAt(0);
            CharSequence text = item.getText();
            return text != null ? text.toString() : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 清空剪贴板
     *
     * @param context 上下文
     * @return true表示清空成功
     */
    public static boolean clear(@NonNull Context context) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                return false;
            }

            ClipData clipData = ClipData.newPlainText("", "");
            clipboard.setPrimaryClip(clipData);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 判断剪贴板是否有内容
     *
     * @param context 上下文
     * @return true表示有内容
     */
    public static boolean hasText(@NonNull Context context) {
        try {
            ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard == null) {
                return false;
            }

            ClipData clipData = clipboard.getPrimaryClip();
            return clipData != null && clipData.getItemCount() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
