package run.yigou.gxzy.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewParent;
import android.widget.ScrollView;

/**
 * 自定义 ScrollView，解决在 RecyclerView 中嵌套滚动的问题
 * 当触摸到该 ScrollView 时，会请求父布局不要拦截触摸事件
 */
public class InterceptableScrollView extends ScrollView {

    public InterceptableScrollView(Context context) {
        super(context);
    }

    public InterceptableScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InterceptableScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // 当触摸事件发生时，请求所有父布局不要拦截
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            requestParentDisallowInterceptTouchEvent(true);
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // 在触摸过程中持续请求不拦截
        int action = ev.getAction();
        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            requestParentDisallowInterceptTouchEvent(true);
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            requestParentDisallowInterceptTouchEvent(false);
        }
        return super.onTouchEvent(ev);
    }

    /**
     * 递归请求所有父布局不要拦截触摸事件
     */
    private void requestParentDisallowInterceptTouchEvent(boolean disallow) {
        ViewParent parent = getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
            parent = parent.getParent();
        }
    }
}
