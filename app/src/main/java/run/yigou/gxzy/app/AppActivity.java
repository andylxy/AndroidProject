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
 *    desc   : Activity ????
 */
public abstract class AppActivity extends BaseActivity
        implements ToastAction, TitleBarAction, OnHttpListener<Object> {

    /** ????? */
    private TitleBar mTitleBar;
    /** ????? */
    private ImmersionBar mImmersionBar;

    /** ????? */
    private BaseDialog mDialog;
    /** ????? */
    private int mDialogCount;

    /**
     * ?????????????
     */
    public boolean isShowDialog() {
        return mDialog != null && mDialog.isShowing();
    }

    /**
     * ???????
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
     * ???????
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

        // ?????????
        if (isStatusBarEnabled()) {
            getStatusBarConfig().init();

            // ???????
            if (getTitleBar() != null) {
                ImmersionBar.setTitleBar(this, getTitleBar());
            }
        }
    }

    /**
     * ??????????
     */
    protected boolean isStatusBarEnabled() {
        return true;
    }

    /**
     * ?????????
     */
    protected boolean isStatusBarDarkFont() {
        return true;
    }

    /**
     * ????????????
     */
    @NonNull
    public ImmersionBar getStatusBarConfig() {
        if (mImmersionBar == null) {
            mImmersionBar = createStatusBarConfig();
        }
        return mImmersionBar;
    }

    /**
     * ?????????
     */
    @NonNull
    protected ImmersionBar createStatusBarConfig() {
        return ImmersionBar.with(this)
                // ????????????
                .statusBarDarkFont(isStatusBarDarkFont())
                // ?????????
                .navigationBarColor(R.color.white)
                // ??????????????????????????????????????
                .autoDarkModeEnable(true, 0.2f);
    }

    /**
     * ????????
     */
    @Override
    public void setTitle(@StringRes int id) {
        setTitle(getString(id));
    }

    /**
     * ????????
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

    // ????????? override ???
    public void onStart(Call call) {
        // showDialog(); // ??? onHttpStart ??
    }

    public void onSucceed(Object result) {
        // default impl
    }

    public void onFail(Exception e) {
        // default impl
    }

    public void onEnd(Call call) {
        // hideDialog(); // ??? onHttpEnd ??
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