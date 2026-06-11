package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ???????
 */
public final class LogoutApi implements IRequestApi {

    @Override
    public String getApi() {
        return "user/logout";
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }
}
