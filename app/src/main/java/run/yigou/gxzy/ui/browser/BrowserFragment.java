package run.yigou.gxzy.ui.browser;

import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.annotation.NonNull;

import run.yigou.gxzy.R;
import run.yigou.gxzy.base.action.StatusAction;
import run.yigou.gxzy.aop.CheckNet;
import run.yigou.gxzy.aop.Log;
import run.yigou.gxzy.app.AppActivity;
import run.yigou.gxzy.app.AppFragment;
import run.yigou.gxzy.widget.BrowserView;
import com.hjq.widget.layout.StatusLayout;
import com.scwang.smart.refresh.layout.SmartRefreshLayout;
import com.scwang.smart.refresh.layout.api.RefreshLayout;
import com.scwang.smart.refresh.layout.listener.OnRefreshListener;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2020/10/24
 *    desc   : 浏览器 Fragment
 */
public final class BrowserFragment extends AppFragment<AppActivity>
        implements StatusAction, OnRefreshListener {

    private static final String INTENT_KEY_IN_URL = "url";

    @Log
    public static BrowserFragment newInstance(String url) {
        BrowserFragment fragment = new BrowserFragment();
        Bundle bundle = new Bundle();
        bundle.putString(INTENT_KEY_IN_URL, url);
        fragment.setArguments(bundle);
        return fragment;
    }

    private StatusLayout mStatusLayout;
    private SmartRefreshLayout mRefreshLayout;
    private BrowserView mBrowserView;

    @Override
    protected int getLayoutId() {
        return R.layout.browser_fragment;
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
        mBrowserView.loadUrl(getString(INTENT_KEY_IN_URL));
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
    public void onRefresh(@NonNull RefreshLayout refreshLayout) {
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