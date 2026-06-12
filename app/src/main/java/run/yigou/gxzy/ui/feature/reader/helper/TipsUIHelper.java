package run.yigou.gxzy.ui.feature.reader.helper;

import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.HH2SectionData;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.text.Layout;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.DisplayMetrics;
import android.widget.TextView;
import android.widget.Toast;

import run.yigou.gxzy.log.EasyLog;

/**
 * Tips 模块 UI 辅助类
 * 负责处理 View 相关的计算、交互等逻辑
 */
public class TipsUIHelper {

    /**
     * 获取ClickableSpan在TextView中的矩形区域
     * 此方法用于确定可点击文本在TextView中的准确位置，以便于处理点击事件时能够知道用户是否点击了可点击区域
     *
     * @param clickableSpan 可点击的Span，用于确定可点击文本的范围
     * @param textView      TextView组件，用于获取文本及其布局信息
     * @return Rect对象，表示可点击文本在TextView中的矩形区域。如果发生异常，返回一个空Rect。
     */
    public static Rect getTextRect(ClickableSpan clickableSpan, TextView textView) {
        // 检查参数是否为null
        if (textView == null || clickableSpan == null) {
            // 避免抛出异常导致崩溃，返回空Rect
            EasyLog.print("getTextRect: textView or clickableSpan is null");
            return new Rect();
        }

        // 1. 优先尝试获取精确的点击坐标 (修复弹窗位置偏移问题)
        run.yigou.gxzy.tips.widget.LocalLinkMovementMethod method = run.yigou.gxzy.tips.widget.LocalLinkMovementMethod.getInstance();
        if (method != null) {
            android.graphics.Point touchPoint = method.getLastTouchPoint();
            if (touchPoint != null) {
                // 返还一个以点击点为中心的微小矩形，TipsLittleWindow 会基于此计算偏移
                return new Rect(
                        touchPoint.x,
                        touchPoint.y,
                        touchPoint.x + 1,
                        touchPoint.y + 1
                );
            }
        }

        // 2. Fallback 到原有的基于文本布局的计算逻辑

        // 检查文本类型，避免 ClassCastException
        CharSequence text = textView.getText();
        if (!(text instanceof Spanned)) {
            EasyLog.print("getTextRect: text is not Spanned");
            return new Rect();
        }
        Spanned spannableString = (Spanned) text;
        
        // 初始化矩形
        Rect textRect = new Rect();
        
        // 获取文本布局
        Layout textLayout = textView.getLayout();
        if (textLayout == null) {
            return new Rect();
        }
        
        // 获取可点击区域的起始位置
        int spanStartIndex = Math.max(0, spannableString.getSpanStart(clickableSpan));
        // 获取可点击区域的结束位置
        int spanEndIndex = Math.min(spannableString.length(), spannableString.getSpanEnd(clickableSpan));
        
        if (spanStartIndex > spanEndIndex) {
             return new Rect();
        }

        // 获取起始位置的水平坐标
        float startX = textLayout.getPrimaryHorizontal(spanStartIndex);
        // 更准确地获取结束位置的水平坐标
        float endX = textLayout.getSecondaryHorizontal(spanEndIndex);
        // 获取起始位置所在行
        int startLine = textLayout.getLineForOffset(spanStartIndex);
        // 获取结束位置所在行
        int endLine = textLayout.getLineForOffset(spanEndIndex);
        // 判断起始和结束是否在同一行
        boolean isMultiLine = startLine != endLine;
        // 获取起始行的边界
        textLayout.getLineBounds(startLine, textRect);

        // 初始化数组用于存储位置
        int[] textViewPosition = {0, 0};
        // 尝试获取TextView在屏幕上的位置
        try {
            textView.getLocationOnScreen(textViewPosition);
        } catch (Exception e) {
            // 如果获取位置失败，打印日志并返回空Rect，防止崩溃
            EasyLog.print("Failed to get location on screen: " + e.getMessage());
            return new Rect();
        }

        // 计算Y轴的偏移
        int scrollY = calculateScrollY(textView, textViewPosition);
        // 更新矩形的上边界
        textRect.top += scrollY;
        // 更新矩形的下边界
        textRect.bottom += scrollY;

        // 如果起始和结束位置不在同一行，需要特殊处理
        if (isMultiLine) {
            Rect endLineRect = new Rect();
            // 获取结束行的边界
            textLayout.getLineBounds(endLine, endLineRect);
            // 更新结束行矩形的上边界
            endLineRect.top += scrollY;
            // 更新结束行矩形的下边界
            endLineRect.bottom += scrollY;

            // 获取屏幕高度，使用更稳健的方式
            int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

            // 根据显示情况调整起始X坐标
            if (textRect.top > screenHeight - textRect.bottom) {
                startX = textLayout.getLineRight(startLine);
            } else {
                startX = textLayout.getLineLeft(endLine);
            }
        }

        // 更新矩形的左边界
        textRect.left += (textViewPosition[0] + startX + textView.getCompoundPaddingLeft() - textView.getScrollX());
        // 更新矩形的右边界
        textRect.right = (int) (textRect.left + (endX - startX));
        // 返回计算后的矩形
        return textRect;
    }


    public static int calculateScrollY(TextView textView, int[] textViewPosition) {
        if (textView == null) {
            return 0; 
        }
        if (textViewPosition == null || textViewPosition.length < 2) {
            return 0; 
        }
        // 计算滚动偏移量
        int positionY = textViewPosition[1]; // 文本视图的位置 Y 坐标
        int scrollY = textView.getScrollY(); // 当前滚动的 Y 偏移量
        int paddingTop = textView.getCompoundPaddingTop(); // 上方内边距
        return positionY - scrollY + paddingTop;
    }

    // 复制内容到剪贴板
    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }
}
