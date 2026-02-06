package com.hjq.http.listener;

import com.hjq.http.config.IRequestApi;
import okhttp3.Call;

/**
 * HttpCallback 适配类，适配 EasyHttp 13.5+
 * 桥接旧版 API (Call) 到新版 API (IRequestApi)
 */
public abstract class HttpCallback<T> implements OnHttpListener<T> {

    private final OnHttpListener<? super T> mListener;

    public HttpCallback(OnHttpListener<? super T> listener) {
        this.mListener = listener;
    }

    @Override
    public void onHttpStart(IRequestApi api) {
        if (mListener != null) {
            mListener.onHttpStart(api);
        }
        onStart((Call) null);
    }

    @Override
    public void onHttpSuccess(T result) {
        if (mListener != null) {
            mListener.onHttpSuccess(result);
        }
        onSucceed(result);
    }

    @Override
    public void onHttpFail(Throwable throwable) {
        if (mListener != null) {
            mListener.onHttpFail(throwable);
        }
        onFail(Exception.class.isInstance(throwable) ? (Exception) throwable : new Exception(throwable));
    }

    @Override
    public void onHttpEnd(IRequestApi api) {
        if (mListener != null) {
            mListener.onHttpEnd(api);
        }
        onEnd((Call) null);
    }

    // Old API methods with Call
    public void onStart(Call call) {}
    public void onSucceed(T result) {}
    public void onFail(Exception e) {}
    public void onEnd(Call call) {}
}
