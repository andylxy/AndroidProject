package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ??????
 */
public final class RegisterApi implements IRequestApi {

    @Override
    public String getApi() {
        return "user/register";
    }
    // ????????????
    public String getMethod() {
        return "POST"; // ??"POST", "PUT" ??
    }
    /** ?????*/
    private String phone;
    /** ?????*/
    private String code;
    /** ??? */
    private String password;

    public RegisterApi setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public RegisterApi setCode(String code) {
        this.code = code;
        return this;
    }

    public RegisterApi setPassword(String password) {
        this.password = password;
        return this;
    }

    public final static class Bean {

    }
}
