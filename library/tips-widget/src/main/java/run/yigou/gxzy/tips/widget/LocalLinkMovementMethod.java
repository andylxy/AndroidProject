package run.yigou.gxzy.tips.widget;

import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.MotionEvent;
import android.widget.TextView;

import run.yigou.gxzy.log.EasyLog;

/**
 * 支持 ClickableSpan 点击的 LinkMovementMethod 子类。
 *
 * 内部通过 setTag 机制在 ACTION_DOWN 阶段标记"正在处理 span 点击"，
 * 用于阻止父容器在本次触摸序列中误触发 toggle 逻辑。
 * 外部调用方通过 {@link #isHandlingClick(TextView)} 判断当前是否正在点击链接。
 *
 * 作为单例使用，通过 {@link #getInstance()} 获取全局实例。
 */
public class LocalLinkMovementMethod extends LinkMovementMethod {
    private static final LocalLinkMovementMethod INSTANCE = new LocalLinkMovementMethod();

    public static LocalLinkMovementMethod getInstance() {
        return INSTANCE;
    }

    /**
     * 检查指定 TextView 当前是否正在处理 ClickableSpan 点击。
     * 用于父容器在触摸事件中判断是否应跳过自身的 toggle 逻辑。
     *
     * @param textView 目标 TextView
     * @return true 表示正在处理 span 点击
     */
    public static boolean isHandlingClick(TextView textView) {
        Boolean isClick = (Boolean) textView.getTag(R.id.tag_is_clicking_link);
        return isClick != null && isClick;
    }

    @Override
    public boolean onTouchEvent(TextView textView, Spannable spannable, MotionEvent motionEvent) {
        // 获取触摸事件的动作类型
        int action = motionEvent.getAction();

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
            // 获取相对于 TextView 的触摸坐标
            int x = (int) motionEvent.getX();
            int y = (int) motionEvent.getY();

            // 计算去除内边距并考虑滚动偏移后的内容区域坐标
            int scrollX = x - textView.getTotalPaddingLeft() + textView.getScrollX();
            int scrollY = y - textView.getTotalPaddingTop() + textView.getScrollY();

            // 获取 TextView 的布局对象
            Layout layout = textView.getLayout();

            // 根据 Y 坐标计算文本行号，然后根据 X 坐标获取该行中点击位置的偏移量
            int offsetForHorizontal = layout.getOffsetForHorizontal(layout.getLineForVertical(scrollY), scrollX);

            // 获取在该位置的可点击文本对象
            ClickableSpan[] clickableSpanArr = (ClickableSpan[]) spannable.getSpans(offsetForHorizontal, offsetForHorizontal, ClickableSpan.class);

            if (clickableSpanArr.length != 0) {
                // 在 DOWN 阶段标记"正在处理 span 点击"，阻止父容器在本次触摸序列中误触发 toggle
                textView.setTag(R.id.tag_is_clicking_link, true);
                if (action == MotionEvent.ACTION_UP) {
                    // 抬起事件：直接触发点击回调
                    clickableSpanArr[0].onClick(textView);
                } else if (action == MotionEvent.ACTION_DOWN) {
                    // 按下事件：高亮显示可点击的文本
                    Selection.setSelection(spannable, spannable.getSpanStart(clickableSpanArr[0]), spannable.getSpanEnd(clickableSpanArr[0]));
                }
                return true;
            }
            textView.setTag(R.id.tag_is_clicking_link, false);
            // 仅在非 span 点击且当前有选区时清除
            if (Selection.getSelectionStart(spannable) >= 0) {
                Selection.removeSelection(spannable);
            }
        }
        return super.onTouchEvent(textView, spannable, motionEvent);
    }

}
