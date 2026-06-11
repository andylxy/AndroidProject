package run.yigou.gxzy.tips.widget;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import run.yigou.gxzy.log.EasyLog;

public class LocalLinkMovementMethod extends LinkMovementMethod {
    private static final long CLICK_DELAY = 1000;
    static LocalLinkMovementMethod sInstance;
    private long lastClickTime;

    // 用于弹窗定位修正
    private float lastRawX;
    private float lastRawY;
    private long lastTouchTimestamp;

    public static LocalLinkMovementMethod getInstance() {
        if (sInstance == null) {
            sInstance = new LocalLinkMovementMethod();
        }
        return sInstance;
    }

    @Override
    public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent motionEvent) {
        // 获取触摸事件的动作类型
        int action = motionEvent.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            // 捕获原始坐标用于优化弹窗位置
            this.lastRawX = motionEvent.getRawX();
            this.lastRawY = motionEvent.getRawY();
            this.lastTouchTimestamp = System.currentTimeMillis();

            // 获取相对于 TextView 的触摸坐标
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();

            // 计算相对于 TextView 内边距的坐标
            int totalPaddingLeft = x - textView.getTotalPaddingLeft();
            int totalPaddingTop = y - textView.getTotalPaddingTop();

            // 考虑滚动偏移量，计算相对于 TextView 内容区域的坐标
            int scrollX = totalPaddingLeft + textView.getScrollX();
            int scrollY = totalPaddingTop + textView.getScrollY();

            // 获取 TextView 的布局对象
            Layout layout = textView.getLayout();

            // 根据 Y 坐标计算文本行号，然后根据 X 坐标获取该行中点击位置的偏移量
            int offsetForHorizontal = layout.getOffsetForHorizontal(layout.getLineForVertical(scrollY), scrollX);

            // 获取在该位置的可点击文本对象
            ClickableSpan[] clickableSpanArr = (ClickableSpan[]) spannable.getSpans(offsetForHorizontal, offsetForHorizontal, ClickableSpan.class);

            if (clickableSpanArr.length != 0) {
                // 标记当前触摸操作是 点击动作 => 点击文本spannable对象
                textView.setTag(true);
                if (action == MotionEvent.ACTION_UP) {
                    // 如果是抬起事件，检查是否在双击时间内处理点击事件
                    if (System.currentTimeMillis() - this.lastClickTime < CLICK_DELAY) {
                        clickableSpanArr[0].onClick(textView);
                    }
                } else if (action == MotionEvent.ACTION_DOWN) {
                    // 如果是按下事件，高亮显示可点击的文本
                    Selection.setSelection(spannable, spannable.getSpanStart(clickableSpanArr[0]), spannable.getSpanEnd(clickableSpanArr[0]));
                    this.lastClickTime = System.currentTimeMillis();
                }
                return true;
            }
            textView.setTag(false);
            Selection.removeSelection(spannable);
        }
        return super.onTouchEvent(textView, spannable, motionEvent);
    }

    /**
     * 获取最近一次触摸的屏幕绝对坐标
     * 有效期 500ms，避免使用陈旧数据
     * @return Point 对象包含 x, y 坐标，如果无效则返回 null
     */
    public android.graphics.Point getLastTouchPoint() {
        if (System.currentTimeMillis() - lastTouchTimestamp < 500) {
            return new android.graphics.Point((int) lastRawX, (int) lastRawY);
        }
        return null;
    }
}
