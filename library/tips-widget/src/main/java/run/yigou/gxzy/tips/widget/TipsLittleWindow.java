package run.yigou.gxzy.tips.widget;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import run.yigou.gxzy.log.EasyLog;

/**
 * LittleWindow类是一个Fragment的子类，用于实现一个小窗口的功能
 * 它主要提供了搜索文本的管理以及自身的显示和隐藏功能
 * 使用模板方法模式，子类通过实现抽象方法来定制弹窗行为
 */
public abstract class TipsLittleWindow extends Fragment {

    /**
     * 位置信息封装类
     * 用于封装弹窗位置计算的相关参数
     */
    protected static class PositionInfo {
        public final int centerX;       // 点击位置的中心X坐标
        public final int centerY;       // 点击位置的中心Y坐标
        public final int screenWidth;   // 屏幕宽度
        public final int screenHeight;  // 屏幕高度
        public final int midHeight;     // 屏幕中线高度
        public final boolean isUp;      // 是否向上显示

        public PositionInfo(int centerX, int centerY, int screenWidth, int screenHeight) {
            this.centerX = centerX;
            this.centerY = centerY;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.midHeight = screenHeight / 2;
            this.isUp = centerY < midHeight;
        }
    }

    // 成员变量
    protected ViewGroup mGroup;
    protected Rect rect;
    protected View view;

    // 宿主接口（用于解耦 app 模块依赖）
    private ITipsWindowHost mHost;
    // 箭头布局参数（替代 View Tag 存储，避免对 R.id 的依赖）
    private FrameLayout.LayoutParams mArrowParams;

    /**
     * 设置宿主接口
     * @param host 宿主实现
     */
    public void setHost(@NonNull ITipsWindowHost host) {
        this.mHost = host;
    }

    /**
     * 获取Fragment的标签
     *
     * @return 返回Fragment的标签，用于标识这个Fragment
     */
    public String getTagName() {
        return "littleWindow";
    }

    /**
     * 设置点击位置矩形
     */
    public void setRect(Rect rect) {
        this.rect = rect;
    }

    /**
     * 显示这个Fragment
     *
     * @param fragmentManager FragmentManager对象，用于管理Fragment的事务
     *                        通过这个方法，Fragment可以被添加到活动的Fragment列表中
     */
    public void show(FragmentManager fragmentManager) {
        // 开始一个新的事务
        FragmentTransaction transaction = fragmentManager.beginTransaction();

        // 检查事务是否创建成功
        if (transaction == null) {
            throw new IllegalStateException("无法创建FragmentTransaction");
        }

        String tag = getTagName();
        transaction.add(this, tag);
        transaction.addToBackStack(tag);

        // 使用commitAllowingStateLoss来避免状态丢失问题
        transaction.commitAllowingStateLoss();
    }

    /**
     * 隐藏（移除）这个Fragment
     * <p>
     * 通过移除事务将Fragment从当前的活动列表中移除，并且退回到之前的BackStack状态
     */
    public void dismiss() {
        // 获取FragmentManager
        FragmentManager fragmentManager = getParentFragmentManager();
        if (fragmentManager == null) {
            EasyLog.print("TipsLittleWindow", "FragmentManager is null, cannot dismiss fragment.");
            return;
        }

        // 异步提交事务
        fragmentManager.popBackStack();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(this);

        // 提交事务，兼容不同版本
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    transaction.commitNow();
                } catch (IllegalStateException e) {
                    // commitNow 失败，降级为 commit
                    transaction.commit();
                }
            } else {
                transaction.commit();
            }
        } catch (IllegalStateException e) {
            EasyLog.print(e);
        } catch (Exception e) {
            EasyLog.print(e);
        }
    }

    /**
     * 模板方法：创建视图
     * 子类可以重写此方法来自定义布局创建流程
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Activity activity = getActivity();
        if (activity == null) {
            throw new IllegalStateException("Activity cannot be null.");
        }

        // 获取窗口的根视图和屏幕信息
        this.mGroup = (ViewGroup) activity.getWindow().getDecorView();
        int screenHeight = this.mGroup.getHeight();
        int screenWidth = this.mGroup.getWidth();

        // 初始化矩形区域
        if (this.rect == null) {
            this.rect = new Rect();
        }

        // 计算位置信息
        int centerX = this.rect.left + (this.rect.width() / 2);
        int centerY = this.rect.top + (this.rect.height() / 2);
        PositionInfo positionInfo = new PositionInfo(centerX, centerY, screenWidth, screenHeight);

        // 获取窗口配置
        WindowConfig config = getWindowConfig();

        // 创建并配置视图
        this.view = createWindowView(inflater, positionInfo, config);

        // 设置位置和布局参数
        setupWindowPosition(positionInfo, config);

        // 设置箭头
        setupArrow(positionInfo, config);

        // 设置按钮
        setupButtons(config);

        // 创建内容视图（子类实现）
        createContentView(inflater, container);

        // 绑定数据（子类实现）
        bindData();

        // 将视图添加到根视图中
        this.mGroup.addView(this.view);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    /**
     * 创建窗口视图
     */
    protected View createWindowView(LayoutInflater inflater, PositionInfo positionInfo, WindowConfig config) {
        int layoutRes = positionInfo.isUp ? config.getUpLayoutRes() : config.getDownLayoutRes();
        return inflater.inflate(layoutRes, null);
    }

    /**
     * 设置窗口位置和布局参数
     */
    protected void setupWindowPosition(PositionInfo positionInfo, WindowConfig config) {
        // 默认动态计算 minSize，如果子类自定义了 margin 则使用自定义值
        int minSize = config.isMarginCustomized()
                ? config.getHorizontalMargin()
                : Math.min(50, positionInfo.screenWidth / 18);

        FrameLayout.LayoutParams smallLayoutParams = new FrameLayout.LayoutParams(minSize, minSize);
        FrameLayout.LayoutParams largeLayoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        if (positionInfo.isUp) {
            // 向上布局
            largeLayoutParams.setMargins(
                    minSize,
                    this.rect.top + this.rect.height() + minSize,
                    minSize,
                    minSize
            );
            smallLayoutParams.setMargins(
                    positionInfo.centerX - (minSize / 2),
                    this.rect.top + this.rect.height(),
                    (positionInfo.screenWidth - positionInfo.centerX) - (minSize / 2),
                    (positionInfo.screenHeight - this.rect.top - this.rect.height()) - minSize
            );
        } else {
            // 向下布局
            largeLayoutParams.gravity = Gravity.BOTTOM;
            Rect visibleRect = new Rect();
            this.mGroup.getWindowVisibleDisplayFrame(visibleRect);
            largeLayoutParams.setMargins(
                    minSize,
                    visibleRect.top + config.getVerticalMargin(),
                    minSize,
                    (positionInfo.screenHeight - this.rect.top) + minSize
            );
            smallLayoutParams.setMargins(
                    positionInfo.centerX - (minSize / 2),
                    (this.rect.top - minSize) - 1,
                    (positionInfo.screenWidth - positionInfo.centerX) - (minSize / 2),
                    (positionInfo.screenHeight - this.rect.top) + 1
            );
        }

        // 配置Wrapper布局参数（通过接口解耦，避免硬编码 R.id.wrapper）
        int wrapperId = config.getWrapperViewId();
        if (wrapperId == -1) {
            EasyLog.print("TipsLittleWindow", "wrapperViewId 未配置，跳过位置设置");
        } else {
            LinearLayout wrapper = this.view.findViewById(wrapperId);
            if (wrapper != null) {
                wrapper.setLayoutParams(largeLayoutParams);
            }
        }

        // 保存smallLayoutParams供箭头使用
        this.mArrowParams = smallLayoutParams;
    }

    /**
     * 设置箭头
     */
    protected void setupArrow(PositionInfo positionInfo, WindowConfig config) {
        TipsArrowView arrowView = this.view.findViewById(config.getArrowViewId());
        if (arrowView != null) {
            arrowView.setDirection(positionInfo.isUp ? TipsArrowView.UP : TipsArrowView.DOWN);
            // 使用 mArrowParams（包含动态计算的 minSize 和位置 margin）
            FrameLayout.LayoutParams arrowParams = this.mArrowParams;
            if (arrowParams != null) {
                arrowView.setLayoutParams(arrowParams);
            }
        }
    }

    /**
     * 设置按钮
     */
    protected void setupButtons(WindowConfig config) {
        Activity activity = getActivity();
        if (activity == null) return;

        // 关闭按钮
        Button closeButton = this.view.findViewById(config.getCloseButtonId());
        if (closeButton != null) {
            closeButton.setOnClickListener(v -> dismiss());
        }

        // 复制按钮
        if (config.isEnableCopy()) {
            Button copyButton = this.view.findViewById(config.getCopyButtonId());
            if (copyButton != null) {
                copyButton.setOnClickListener(v -> {
                    onCopyButtonClick();
                    Toast.makeText(activity, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
                });
            }
        }

        // 更多按钮
        if (config.isEnableMore()) {
            Button moreButton = this.view.findViewById(config.getMoreButtonId());
            if (moreButton != null) {
                moreButton.setOnClickListener(v -> {
                    Intent intent = new Intent();
                    onMoreButtonClick(intent);
                    if (mHost != null) {
                        mHost.navigateToDetail(intent);
                    } else {
                        EasyLog.print("TipsLittleWindow", "ITipsWindowHost 未设置，无法导航");
                    }
                });
            }
        }
    }

    /**
     * 清理资源
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (this.view != null && this.mGroup != null) {
            this.mGroup.removeView(this.view);
        }
    }

    // ========== 抽象方法，子类必须实现 ==========

    /**
     * 获取窗口配置
     * 子类必须实现此方法返回窗口配置
     */
    protected abstract WindowConfig getWindowConfig();

    /**
     * 创建内容视图
     * 子类实现此方法创建具体的内容视图（如RecyclerView、TextView等）
     */
    protected abstract View createContentView(LayoutInflater inflater, ViewGroup container);

    /**
     * 绑定数据
     * 子类实现此方法将数据绑定到视图
     */
    protected abstract void bindData();

    // ========== 钩子方法，子类可选择性重写 ==========

    /**
     * 复制按钮点击事件
     * 子类可以重写此方法实现具体的复制逻辑
     */
    protected void onCopyButtonClick() {
        // 默认空实现，子类重写
    }

    /**
     * 更多按钮点击事件
     * 子类可以重写此方法定制Intent参数
     */
    protected void onMoreButtonClick(Intent intent) {
        // 默认实现
        intent.putExtra("isFang", "false");
    }

}
