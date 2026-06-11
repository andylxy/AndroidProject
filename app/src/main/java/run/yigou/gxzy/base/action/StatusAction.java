package run.yigou.gxzy.base.action;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.annotation.DrawableRes;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import run.yigou.gxzy.R;
import com.hjq.widget.layout.StatusLayout;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/08
 *    desc   : ??????????
 */
public interface StatusAction {

    /**
     * ??????????
     */
    StatusLayout getStatusLayout();

    /**
     * ????????
     */
    default void showLoading() {
        showLoading(R.raw.loading);
    }

    default void showLoading(@RawRes int id) {
        StatusLayout layout = getStatusLayout();
        layout.show();
        layout.setAnimResource(id);
        layout.setHint("");
        layout.setOnRetryListener(null);
    }

    /**
     * ?????????
     */
    default void showComplete() {
        StatusLayout layout = getStatusLayout();
        if (layout == null || !layout.isShow()) {
            return;
        }
        layout.hide();
    }

    /**
     * ????????
     */
    default void showEmpty() {
        showLayout(R.drawable.status_empty_ic, R.string.status_layout_no_data, null);
    }

    /**
     * ?????????
     */
    default void showError(StatusLayout.OnRetryListener listener) {
        StatusLayout layout = getStatusLayout();
        Context context = layout.getContext();
        ConnectivityManager manager = ContextCompat.getSystemService(context, ConnectivityManager.class);
        if (manager != null) {
            NetworkInfo info = manager.getActiveNetworkInfo();
            // ????????????
            if (info == null || !info.isConnected()) {
                showLayout(R.drawable.status_network_ic, R.string.status_layout_error_network, listener);
                return;
            }
        }
        showLayout(R.drawable.status_error_ic, R.string.status_layout_error_request, listener);
    }

    /**
     * ???????????
     */
    default void showLayout(@DrawableRes int drawableId, @StringRes int stringId, StatusLayout.OnRetryListener listener) {
        StatusLayout layout = getStatusLayout();
        Context context = layout.getContext();
        showLayout(ContextCompat.getDrawable(context, drawableId), context.getString(stringId), listener);
    }

    default void showLayout(Drawable drawable, CharSequence hint, StatusLayout.OnRetryListener listener) {
        StatusLayout layout = getStatusLayout();
        layout.show();
        layout.setIcon(drawable);
        layout.setHint(hint);
        layout.setOnRetryListener(listener);
    }
}
