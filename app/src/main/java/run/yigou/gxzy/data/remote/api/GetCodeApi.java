package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ????????
 */
public final class GetCodeApi implements IRequestApi {

    @Override
    public String getApi() {
        return "code/get";
    }
    // ????????????
    public String getMethod() {
        return "POST"; // ??"POST", "PUT" ??
    }
    /** ?????*/
    private String phone;

    public GetCodeApi setPhone(String phone) {
        this.phone = phone;
        return this;
    }
}
