package run.yigou.gxzy.ui.browser;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.action.StatusAction;
import run.yigou.gxzy.aop.CheckNet;
import run.yigou.gxzy.aop.Log;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.widget.BrowserView;
import com.hjq.widget.layout.StatusLayout;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : 浏览器界面
 */
public final class BrowserActivity extends AppActivity
        implements StatusAction, OnRefreshListener {

    private static final String INTENT_KEY_IN_URL = "url";

    @CheckNet
    @Log
    public static void start(Context context, String url) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        Intent intent = new Intent(context, BrowserActivity.class);
        intent.putExtra(INTENT_KEY_IN_URL, url);
        if (!(context instanceof Activity)) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    private StatusLayout mStatusLayout;
    private SmartRefreshLayout mRefreshLayout;
    private BrowserView mBrowserView;

    @Override
    protected int getLayoutId() {
        return R.layout.browser_activity;
    }

    @Override
    protected void initView() {
        mStatusLayout = findViewById(R.id.hl_browser_hint);
        mRefreshLayout = findViewById(R.id.sl_browser_refresh);
        mBrowserView = findViewById(R.id.wv_browser_view);

        mBrowserView.setLifecycleOwner(this);
        mRefreshLayout.setOnRefreshListener(this);
    }

    @Override
    protected void initData() {
        mBrowserView.setBrowserViewClient(new AppBrowserViewClient());
        mBrowserView.setBrowserChromeClient(new BrowserView.BrowserChromeClient(mBrowserView));
        mBrowserView.loadUrl(getIntent().getStringExtra(INTENT_KEY_IN_URL));
        showLoading();
    }

    @Override
    public StatusLayout getStatusLayout() {
        return mStatusLayout;
    }

    /**
     * 重新加载当前页
     */
    @CheckNet
    public void reload() {
        mBrowserView.reload();
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (mBrowserView.canGoBack()) {
                mBrowserView.goBack();
                return true;
            }
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
        // 这里应该执行刷新操作，但是为了演示，这里延迟两秒后隐藏刷新布局
        postDelayed(() -> {
            mBrowserView.reload();
            mRefreshLayout.finishRefresh();
        }, 1000);
    }

    private class AppBrowserViewClient extends BrowserView.BrowserViewClient {

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            showLoading();
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (mRefreshLayout != null && mRefreshLayout.isRefreshing()) {
                mRefreshLayout.finishRefresh();
            }
            showComplete();
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            if (mRefreshLayout != null && mRefreshLayout.isRefreshing()) {
                mRefreshLayout.finishRefresh(false);
            }
            showError((v) -> reload());
        }

        public void onProgressChanged(WebView view, int newProgress) {

        }
    }
}