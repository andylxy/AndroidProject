package run.yigou.gxzy.base.action;

import androidx.annotation.StringRes;

import com.hjq.toast.Toaster;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/08
 *    desc   : ??????
 */
public interface ToastAction {

    default void toast(CharSequence text) {
        Toaster.show(text);
    }

    default void toast(@StringRes int id) {
        Toaster.show(id);
    }

    default void toast(Object object) {
        Toaster.show(object);
    }
}
