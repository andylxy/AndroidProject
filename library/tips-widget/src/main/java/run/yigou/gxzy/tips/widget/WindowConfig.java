package run.yigou.gxzy.tips.widget;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;

/**
 * 弹窗配置类
 * 使用Builder模式封装TipsLittleWindow子类的配置参数
 */
public class WindowConfig {
    // 布局资源
    @LayoutRes
    private final int upLayoutRes;      // 向上显示的布局
    @LayoutRes
    private final int downLayoutRes;    // 向下显示的布局

    // 按钮ID
    @IdRes
    private final int closeButtonId;    // 关闭按钮ID
    @IdRes
    private final int copyButtonId;     // 复制按钮ID
    @IdRes
    private final int moreButtonId;     // 更多按钮ID

    // 箭头相关
    @IdRes
    private final int arrowViewId;      // 箭头View的ID
    private final int arrowWidth;       // 箭头宽度
    private final int arrowHeight;      // 箭头高度

    // 边距配置
    private final int horizontalMargin; // 水平边距
    private final int verticalMargin;   // 垂直边距

    // 功能开关
    private final boolean enableCopy;   // 是否启用复制功能
    private final boolean enableMore;   // 是否启用更多功能

    // wrapper 容器 View ID（由宿主提供，替代硬编码 R.id.wrapper）
    @IdRes
    private final int wrapperViewId;

    // margin 是否被自定义（用于 TipsLittleWindow 判断是否覆盖默认动态计算）
    private final boolean marginCustomized;

    private WindowConfig(Builder builder) {
        this.upLayoutRes = builder.upLayoutRes;
        this.downLayoutRes = builder.downLayoutRes;
        this.closeButtonId = builder.closeButtonId;
        this.copyButtonId = builder.copyButtonId;
        this.moreButtonId = builder.moreButtonId;
        this.arrowViewId = builder.arrowViewId;
        this.arrowWidth = builder.arrowWidth;
        this.arrowHeight = builder.arrowHeight;
        this.horizontalMargin = builder.horizontalMargin;
        this.verticalMargin = builder.verticalMargin;
        this.enableCopy = builder.enableCopy;
        this.enableMore = builder.enableMore;
        this.wrapperViewId = builder.wrapperViewId;
        this.marginCustomized = builder.mMarginCustomized;
    }

    // Getters
    public int getUpLayoutRes() { return upLayoutRes; }
    public int getDownLayoutRes() { return downLayoutRes; }
    public int getCloseButtonId() { return closeButtonId; }
    public int getCopyButtonId() { return copyButtonId; }
    public int getMoreButtonId() { return moreButtonId; }
    public int getArrowViewId() { return arrowViewId; }
    public int getArrowWidth() { return arrowWidth; }
    public int getArrowHeight() { return arrowHeight; }
    public int getHorizontalMargin() { return horizontalMargin; }
    public int getVerticalMargin() { return verticalMargin; }
    public boolean isEnableCopy() { return enableCopy; }
    public boolean isEnableMore() { return enableMore; }
    public int getWrapperViewId() { return wrapperViewId; }
    public boolean isMarginCustomized() { return marginCustomized; }

    /**
     * Builder模式构建器
     */
    public static class Builder {
        private int upLayoutRes;
        private int downLayoutRes;
        private int closeButtonId;
        private int copyButtonId;
        private int moreButtonId;
        private int arrowViewId;
        private int arrowWidth = 30;        // 默认箭头宽度
        private int arrowHeight = 20;       // 默认箭头高度
        private int horizontalMargin = 20;  // 默认水平边距
        private int verticalMargin = 10;    // 默认垂直边距
        private boolean enableCopy = true;  // 默认启用复制
        private boolean enableMore = true;  // 默认启用更多
        private int wrapperViewId = -1;     // 默认未配置
        private boolean mMarginCustomized = false;  // margin 是否被自定义

        public Builder upLayout(@LayoutRes int layoutRes) {
            this.upLayoutRes = layoutRes;
            return this;
        }

        public Builder downLayout(@LayoutRes int layoutRes) {
            this.downLayoutRes = layoutRes;
            return this;
        }

        public Builder closeButton(@IdRes int buttonId) {
            this.closeButtonId = buttonId;
            return this;
        }

        public Builder copyButton(@IdRes int buttonId) {
            this.copyButtonId = buttonId;
            return this;
        }

        public Builder moreButton(@IdRes int buttonId) {
            this.moreButtonId = buttonId;
            return this;
        }

        public Builder arrow(@IdRes int viewId, int width, int height) {
            this.arrowViewId = viewId;
            this.arrowWidth = width;
            this.arrowHeight = height;
            return this;
        }

        public Builder margins(int horizontal, int vertical) {
            this.horizontalMargin = horizontal;
            this.verticalMargin = vertical;
            this.mMarginCustomized = true;
            return this;
        }

        public Builder enableCopy(boolean enable) {
            this.enableCopy = enable;
            return this;
        }

        public Builder enableMore(boolean enable) {
            this.enableMore = enable;
            return this;
        }

        public Builder wrapperView(@IdRes int viewId) {
            this.wrapperViewId = viewId;
            return this;
        }

        public WindowConfig build() {
            return new WindowConfig(this);
        }
    }
}
