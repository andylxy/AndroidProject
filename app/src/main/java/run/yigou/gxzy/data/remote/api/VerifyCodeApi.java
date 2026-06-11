package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ????????
 */
public final class VerifyCodeApi implements IRequestApi {

    @Override
    public String getApi() {
        return "code/checkout";
    }
    // ????????????
    public String getMethod() {
        return "POST"; // ??"POST", "PUT" ??
    }
    /** ?????*/
    private String phone;
    /** ?????*/
    private String code;

    public VerifyCodeApi setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public VerifyCodeApi setCode(String code) {
        this.code = code;
        return this;
    }
}
