package run.yigou.gxzy.ui.tips.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.CycleInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.EditText;
import android.widget.TextView;

import run.yigou.gxzy.R;


@SuppressLint("AppCompatCustomView")
public class ClearEditText extends EditText implements View.OnFocusChangeListener, TextWatcher {
    private boolean hasFoucs;
    private Drawable mClearDrawable;
    private TextView numTips;

    @Override // android.text.TextWatcher
    public void afterTextChanged(Editable editable) {
    }

    @Override // android.text.TextWatcher
    public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
    }

    @Override // android.view.View.OnFocusChangeListener
    public void onFocusChange(View view, boolean z) {
    }

    public ClearEditText(Context context) {
        this(context, null);
    }

    public void setNumTips(TextView textView) {
        this.numTips = textView;
    }

    public ClearEditText(Context context, AttributeSet attributeSet) {
        this(context, attributeSet, android.R.attr.editTextStyle);
    }

    public ClearEditText(Context context, AttributeSet attributeSet, int i) {
        super(context, attributeSet, i);
        init();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void init() {
        this.mClearDrawable = getCompoundDrawables()[2];
        if (this.mClearDrawable == null) {
            this.mClearDrawable = getResources().getDrawable(R.drawable.delete_selector);
        }
        this.mClearDrawable.setBounds(0, 0, this.mClearDrawable.getIntrinsicWidth(), this.mClearDrawable.getIntrinsicHeight());
        setClearIconVisible(false);
        setOnFocusChangeListener(this);
        addTextChangedListener(this);
    }

    /**
     * 处理触摸事件，检测是否点击了清除图标区域。
     *
     * @param motionEvent 触摸事件。
     * @return 如果触摸事件在清除图标区域内，返回 false；否则，返回父类的处理结果。
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        // 检查触摸事件的动作是否为 ACTION_UP，并且右侧有清除图标。
        if (motionEvent.getAction() == MotionEvent.ACTION_UP && getCompoundDrawables()[2] != null) {
            // 获取清除图标的水平区域范围。
            float iconRightEdge = getWidth() - getTotalPaddingRight();
            float iconLeftEdge = iconRightEdge + 10.0f;

            // 判断触摸点是否在清除图标的区域内。
            if (motionEvent.getX() > iconRightEdge && motionEvent.getX() < iconLeftEdge) {
                setText(""); // 清空文本。
                return true; // 事件已处理，返回 true。
            }
        }
        return super.onTouchEvent(motionEvent); // 其他触摸事件交给父类处理。
    }


    /**
     * 设置清除图标的可见性，并根据需要更新 numTips 视图的显示状态。
     *
     * @param z 一个布尔值，指示清除图标是否应可见 (true) 或隐藏 (false)。
     */
    protected void setClearIconVisible(boolean z) {
        // 获取当前 TextView 的左、上、右、下四个 drawable。
        // 根据布尔值 z 设置右侧 drawable 为 mClearDrawable（如果 z 为 true），否则设置为 null。
        setCompoundDrawables(
                getCompoundDrawables()[0], // 左侧 drawable
                getCompoundDrawables()[1], // 上侧 drawable
                z ? this.mClearDrawable : null, // 右侧 drawable（清除图标）
                getCompoundDrawables()[3]  // 下侧 drawable
        );

        // 检查 numTips 视图是否不为 null。
        // 如果 z 为 true，则将 numTips 的可见性设置为 VISIBLE（0）；否则设置为 GONE（8）。
        if (this.numTips != null) {
            this.numTips.setVisibility(z ? View.VISIBLE : View.GONE);
        }
    }


    /**
     * 文本改变时的回调方法。
     *
     * @param charSequence 当前输入的文本。
     * @param start 变更的起始位置。
     * @param before 变更前文本的长度。
     * @param count 变更后文本的长度。
     */
    @Override
    public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        // 设置清除图标的可见性，如果当前文本长度大于 0，则显示图标，否则隐藏。
        setClearIconVisible(charSequence.length() > 0);
    }


    /**
     * 设置抖动动画。
     *
     * 调用此方法将为当前视图应用一个抖动动画。
     */
    public void setShakeAnimation() {
        // 设置视图的动画为调用 shakeAnimation 方法生成的抖动动画，参数 5 可能表示抖动的强度或持续时间。
        setAnimation(shakeAnimation(5));
    }

    /**
     * 创建一个抖动动画。
     *
     * @param cycles 抖动的周期数，决定了动画的振动频率。
     * @return 配置好的抖动动画对象。
     */
    public static Animation shakeAnimation(int cycles) {
        // 创建一个 TranslateAnimation 实例，定义从 x 轴的 0.0f 到 10.0f 的平移动画。
        // y 轴上的移动保持为 0.0f，即没有垂直移动。
        TranslateAnimation translateAnimation = new TranslateAnimation(0.0f, 10.0f, 0.0f, 0.0f);

        // 设置动画的插值器为 CycleInterpolator，参数 cycles 控制动画的振动周期数。
        translateAnimation.setInterpolator(new CycleInterpolator(cycles));

        // 设置动画的持续时间为 1000 毫秒（1 秒）。
        translateAnimation.setDuration(1000L);

        // 返回配置好的 TranslateAnimation 对象。
        return translateAnimation;
    }

}
