/*
 * 项目名: AndroidProject
 * 类名: LocalLinkMovementMethod.java
 * 包名: run.yigou.gxzy.ui.tips.widget.LocalLinkMovementMethod
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月14日 09:36:40
 * 上次修改时间: 2024年09月12日 09:47:26
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.widget;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;


public class LocalLinkMovementMethod extends LinkMovementMethod {
    private static final long CLICK_DELAY = 1000;
    static LocalLinkMovementMethod sInstance;
    private long lastClickTime;

    public static LocalLinkMovementMethod getInstance() {
        if (sInstance == null) {
            sInstance = new LocalLinkMovementMethod();
        }
        return sInstance;
    }

    /**
        获取触摸事件的动作: 通过 motionEvent.getAction() 获取当前事件的类型（按下或抬起）。
        获取触摸坐标: 使用 motionEvent.getX() 和 motionEvent.getY() 获取触摸点的坐标。
        计算内边距坐标: 触摸坐标减去 TextView 的内边距，得到相对于内容区域的坐标。
        计算滚动偏移: 将触摸坐标考虑滚动位置，获取实际的内容区域坐标。
        获取布局对象: textView.getLayout() 获取 TextView 的布局，用于计算文本行和偏移量。
        获取偏移量: getOffsetForHorizontal 用于将触摸坐标转换为文本中的偏移量。
        获取可点击文本: spannable.getSpans() 用于获取指定位置的 ClickableSpan 对象。
        处理点击事件: 根据触摸事件的类型（按下或抬起）执行相应的操作，如高亮显示文本或执行点击事件。
        打印调试日志: 输出日志以便于调试和分析触摸事件的处理情况。
        交给父类处理: 如果事件未处理，则调用父类的 onTouchEvent 方法继续处理。
    */
    @Override
    public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent motionEvent) {
        // 获取触摸事件的动作类型
        int action = motionEvent.getAction();
        // ACTION_DOWN = 0: 触摸按下事件
        // ACTION_UP = 1: 触摸抬起事件

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            // 获取相对于 TextView 的触摸坐标
            int x = (int) motionEvent.getX(); // 获取触摸事件的 X 坐标
            int y = (int) motionEvent.getY(); // 获取触摸事件的 Y 坐标

            // 计算相对于 TextView 内边距的坐标
            int totalPaddingLeft = x - textView.getTotalPaddingLeft(); // 减去左侧内边距
            int totalPaddingTop = y - textView.getTotalPaddingTop(); // 减去顶部内边距

            // 考虑滚动偏移量，计算相对于 TextView 内容区域的坐标
            int scrollX = totalPaddingLeft + textView.getScrollX(); // 加上水平滚动偏移
            int scrollY = totalPaddingTop + textView.getScrollY(); // 加上垂直滚动偏移

            // 获取 TextView 的布局对象
            Layout layout = textView.getLayout();

            // 根据 Y 坐标计算文本行号，然后根据 X 坐标获取该行中点击位置的偏移量
            int offsetForHorizontal = layout.getOffsetForHorizontal(layout.getLineForVertical(scrollY), scrollX);

            // 获取在该位置的可点击文本对象
            ClickableSpan[] clickableSpanArr = (ClickableSpan[]) spannable.getSpans(offsetForHorizontal, offsetForHorizontal, ClickableSpan.class);

            if (clickableSpanArr.length != 0) {
                // 如果触摸位置存在可点击文本
                if (action == MotionEvent.ACTION_UP) {
                    // 如果是抬起事件，检查是否在双击时间内处理点击事件
                    if (System.currentTimeMillis() - this.lastClickTime < CLICK_DELAY) {
                        clickableSpanArr[0].onClick(textView); // 执行点击事件
                    }
                } else if (action == MotionEvent.ACTION_DOWN) {
                    // 如果是按下事件，高亮显示可点击的文本
                    Selection.setSelection(spannable, spannable.getSpanStart(clickableSpanArr[0]), spannable.getSpanEnd(clickableSpanArr[0]));
                    this.lastClickTime = System.currentTimeMillis(); // 记录当前点击时间
                }
           // 打印调试日志
                Log.e("--->>", "触摸事件处理完成 (按下或抬起) x : "+x  +" y: "+y +" scrollX :"+scrollX+" scrollY: "+scrollY +" offsetForHorizontal: "+offsetForHorizontal);
                return true; // 事件被处理
            }
            // 如果没有点击到可点击的文本，则移除选中状态
            Log.e("--->>", "未点击到可点击的文本 (按下或抬起) x : "+x  +" y: "+y+" scrollX :"+scrollX+" scrollY: "+scrollY +" offsetForHorizontal: "+offsetForHorizontal);
            Selection.removeSelection(spannable);
        }
        // 如果事件未处理，交给父类处理
        Log.e("--->>", "触摸事件结束");
        return super.onTouchEvent(textView, spannable, motionEvent);
    }


}
