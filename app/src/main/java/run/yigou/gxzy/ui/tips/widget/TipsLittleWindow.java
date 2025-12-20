package run.yigou.gxzy.ui.tips.widget;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.hjq.http.EasyLog;

import run.yigou.gxzy.R;
import run.yigou.gxzy.ui.activity.TipsFragmentActivity;

import run.yigou.gxzy.utils.DebugLog;
import timber.log.Timber;

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

    // 静态窗口栈，替代 TipsSingleData 中的管理
    public static final java.util.List<TipsLittleWindow> windowStack = new java.util.concurrent.CopyOnWriteArrayList<>();

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

        // 添加到弹窗栈
        windowStack.add(this);
    }

    /**
     * 隐藏（移除）这个Fragment
     * <p>
     * 通过移除事务将Fragment从当前的活动列表中移除，并且退回到之前的BackStack状态
     */
    public void dismiss() {
        // 从弹窗栈移除
        windowStack.remove(this);

        // 获取FragmentManager
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager == null) {
            Timber.tag("Fragment").e("FragmentManager is null, cannot dismiss fragment.");
            return;
        }

        // 异步提交事务
        fragmentManager.popBackStack();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.remove(this);

        try {
            // 异步提交事务，兼容不同版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    transaction.commitNow();
                    Timber.tag("TransactionManager").i("事务提交成功，使用 commitNow()。");
                } catch (IllegalStateException e) {
                    Timber.tag("TransactionManager").e(e, "使用 commitNow() 提交事务失败。");
                    // 尝试使用 commit() 方法
                    try {
                        transaction.commit();
                        Timber.tag("TransactionManager").i("事务提交成功，使用 commit()。");
                    } catch (IllegalStateException ex) {
                        Timber.tag("TransactionManager").e(ex, "使用 commit() 提交事务失败。");
                    }
                } catch (Exception e) {
                    Timber.tag("TransactionManager").e(e, "提交事务时发生意外错误。");
                }
            } else {
                try {
                    transaction.commit();
                    Timber.tag("TransactionManager").i("事务提交成功，使用 commit()。");
                } catch (IllegalStateException e) {
                    Timber.tag("TransactionManager").e(e, "使用 commit() 提交事务失败。");
                } catch (Exception e) {
                    Timber.tag("TransactionManager").e(e, "提交事务时发生意外错误。");
                }
            }

            // 处理外部捕获的 IllegalStateException
        } catch (IllegalStateException e) {
            Timber.tag("Fragment").e(e, "提交事务失败。");
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
        DisplayMetrics metrics = this.mGroup.getResources().getDisplayMetrics();
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
        DebugLog.print("=== TipsLittleWindow.onCreateView() ===");
        DebugLog.print("步骤1: 调用createContentView()");
        View contentView = createContentView(inflater, container);
        DebugLog.print("步骤2: createContentView()完成");

        // 绑定数据（子类实现）
        DebugLog.print("步骤3: 调用bindData()");
        bindData();
        DebugLog.print("步骤4: bindData()完成");

        // 将视图添加到根视图中
        this.mGroup.addView(this.view);
        DebugLog.print("步骤5: 视图已添加到根视图");

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
        int minSize = Math.min(50, positionInfo.screenWidth / 18);

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
                    visibleRect.top + 8,
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

        // 配置Wrapper布局参数
        LinearLayout wrapper = this.view.findViewById(R.id.wrapper);
        wrapper.setLayoutParams(largeLayoutParams);

        // 保存smallLayoutParams供箭头使用
        this.view.setTag(R.id.arrow, smallLayoutParams);
    }

    /**
     * 设置箭头
     */
    protected void setupArrow(PositionInfo positionInfo, WindowConfig config) {
        TipsArrowView arrowView = this.view.findViewById(config.getArrowViewId());
        if (arrowView != null) {
            arrowView.setDirection(positionInfo.isUp ? TipsArrowView.UP : TipsArrowView.DOWN);
            FrameLayout.LayoutParams arrowParams = (FrameLayout.LayoutParams) this.view.getTag(R.id.arrow);
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
                    Intent intent = new Intent(activity, TipsFragmentActivity.class);
                    onMoreButtonClick(intent);
                    activity.startActivity(intent);
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
