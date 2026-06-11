package run.yigou.gxzy.app;

import com.hjq.base.BaseFragment;
import run.yigou.gxzy.base.action.ToastAction;
import run.yigou.gxzy.data.remote.model.HttpData;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.listener.OnHttpListener;

import okhttp3.Call;

/**
 *    author : Android ???
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2018/10/18
 *    desc   : Fragment ????
 */
public abstract class AppFragment<A extends AppActivity> extends BaseFragment<A>
        implements ToastAction, OnHttpListener<Object> {

    /**
     * ?????????????
     */
    public boolean isShowDialog() {
        A activity = getAttachActivity();
        if (activity == null) {
            return false;
        }
        return activity.isShowDialog();
    }

    /**
     * ???????
     */
    public void showDialog() {
        A activity = getAttachActivity();
        if (activity == null) {
            return;
        }
        activity.showDialog();
    }

    /**
     * ???????
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
        toast(e.getMessage());
    }

    @Override
    public void onHttpEnd(IRequestApi api) {
        hideDialog();
    }
}