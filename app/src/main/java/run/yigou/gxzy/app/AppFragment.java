package run.yigou.gxzy.app;

import com.hjq.base.BaseFragment;
import run.yigou.gxzy.base.action.ToastAction;
import run.yigou.gxzy.data.remote.model.HttpData;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.listener.OnHttpListener;
import run.yigou.gxzy.network.exception.HandledHttpFailure;

import okhttp3.Call;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : Fragment 基类
 */
public abstract class AppFragment<A extends AppActivity> extends BaseFragment<A>
        implements ToastAction, OnHttpListener<Object> {

    /**
     * 是否显示加载对话框
     */
    public boolean isShowDialog() {
        A activity = getAttachActivity();
        if (activity == null) {
            return false;
        }
        return activity.isShowDialog();
    }

    /**
     * 显示加载对话框
     */
    public void showDialog() {
        A activity = getAttachActivity();
        if (activity == null) {
            return;
        }
        activity.showDialog();
    }

    /**
     * 显示加载对话框
     */
    public void hideDialog() {
        A activity = getAttachActivity();
        if (activity == null) {
            return;
        }
        activity.hideDialog();
    }

    /**
     * {@link OnHttpListener}
     */

    @Override
    public void onHttpStart(IRequestApi api) {
        showDialog();
    }

    @Override
    public void onHttpSuccess(Object result) {
        if (!(result instanceof HttpData)) {
            return;
        }
       // toast(((HttpData<?>) result).getMessage());
    }

    @Override
    public void onHttpFail(Throwable e) {
        // 426 / 设备吊销已有专用提示，不再补通用 toast（票据 21）。
        if (!HandledHttpFailure.shouldSilence(e)) {
            toast(e.getMessage());
        }
    }

    @Override
    public void onHttpEnd(IRequestApi api) {
        hideDialog();
    }
}