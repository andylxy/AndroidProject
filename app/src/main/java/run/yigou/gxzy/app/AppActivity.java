package run.yigou.gxzy.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.gyf.immersionbar.ImmersionBar;
import com.hjq.bar.TitleBar;
import com.hjq.base.BaseActivity;
import com.hjq.base.BaseDialog;
import run.yigou.gxzy.R;
import run.yigou.gxzy.base.action.TitleBarAction;
import run.yigou.gxzy.base.action.ToastAction;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.ui.dialog.WaitDialog;
import com.hjq.http.listener.OnHttpListener;

import okhttp3.Call;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : Activity 基类
 */
public abstract class AppActivity extends BaseActivity
        implements ToastAction, TitleBarAction, OnHttpListener<Object> {

    /** 标题栏 */
    private TitleBar mTitleBar;
    /** 沉浸式状态栏 */
    private ImmersionBar mImmersionBar;

    /** 等待对话框 */
    private BaseDialog mDialog;
    /** 加载对话框 */
    private int mDialogCount;

    /**
     * 是否显示加载对话框
     */
    public boolean isShowDialog() {
        return mDialog != null && mDialog.isShowing();
    }

    /**
     * 显示加载对话框
     */
    public void showDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        mDialogCount++;
        postDelayed(() -> {
            if (mDialogCount <= 0 || isFinishing() || isDestroyed()) {
                return;
            }

            if (mDialog == null) {
                mDialog = new WaitDialog.Builder(this)
                        .setCancelable(false)
                        .create();
            }
            if (!mDialog.isShowing()) {
                mDialog.show();
            }
        }, 300);
    }

    /**
     * 显示加载对话框
     */
    public void hideDialog() {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        if (mDialogCount > 0) {
            mDialogCount--;
        }

        if (mDialogCount != 0 || mDialog == null || !mDialog.isShowing()) {
            return;
        }

        mDialog.dismiss();
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        if (getTitleBar() != null) {
            getTitleBar().setOnTitleBarListener(this);
        }

        // ，延迟?
        if (isStatusBarEnabled()) {
            getStatusBarConfig().init();

            // 显示加载对话框
            if (getTitleBar() != null) {
                ImmersionBar.setTitleBar(this, getTitleBar());
            }
        }
    }

    /**
     * ，延迟??
     */
    protected boolean isStatusBarEnabled() {
        return true;
    }

    /**
     * 状态栏字体是否为深色
     */
    protected boolean isStatusBarDarkFont() {
        return true;
    }

    /**
     * 获取状态栏配置
     */
    @NonNull
    public ImmersionBar getStatusBarConfig() {
        if (mImmersionBar == null) {
            mImmersionBar = createStatusBarConfig();
        }
        return mImmersionBar;
    }

    /**
     * 创建状态栏配置
     */
    @NonNull
    protected ImmersionBar createStatusBarConfig() {
        return ImmersionBar.with(this)
                // 默认使用深色字体
                .statusBarDarkFont(isStatusBarDarkFont())
                // 设置导航栏颜色为白色
                .navigationBarColor(R.color.white)
                // 自动适配深色模式
                .autoDarkModeEnable(true, 0.2f);
    }

    /**
     * 设置标题（字符串资源）
     */
    @Override
    public void setTitle(@StringRes int id) {
        setTitle(getString(id));
    }

    /**
     * 设置标题（字符串文本）
     */
    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (getTitleBar() != null) {
            getTitleBar().setTitle(title);
        }
    }

    @Override
    @Nullable
    public TitleBar getTitleBar() {
        if (mTitleBar == null) {
            mTitleBar = obtainTitleBar(getContentView());
        }
        return mTitleBar;
    }

    @Override
    public void onLeftClick(View view) {
        onBackPressed();
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode, @Nullable Bundle options) {
        super.startActivityForResult(intent, requestCode, options);
        overridePendingTransition(R.anim.right_in_activity, R.anim.right_out_activity);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(R.anim.left_in_activity, R.anim.left_out_activity);
    }

    /**
     * {@link OnHttpListener}
     */

    @Override
    public void onHttpStart(com.hjq.http.config.IRequestApi api) {
        showDialog();
        onStart((Call) null);
    }

    @Override
    public void onHttpSuccess(Object result) {
        //if (result instanceof HttpData) {
        //    toast(((HttpData<?>) result).getMessage());
        //}
        onSucceed(result);
    }

    @Override
    public void onHttpFail(Throwable throwable) {
        toast(throwable.getMessage());
        onFail(throwable instanceof Exception ? (Exception) throwable : new Exception(throwable));
    }

    @Override
    public void onHttpEnd(com.hjq.http.config.IRequestApi api) {
        hideDialog();
        onEnd((Call) null);
    }

    // ，延迟? override ???
    public void onStart(Call call) {
        // showDialog(); // 已在 onHttpStart 中调用
    }

    public void onSucceed(Object result) {
        // default impl
    }

    public void onFail(Exception e) {
        // default impl
    }

    public void onEnd(Call call) {
        // hideDialog(); // 已在 onHttpEnd 中调用
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isShowDialog()) {
            hideDialog();
        }
        mDialog = null;
    }
}