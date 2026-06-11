package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ??????
 */
public final class PasswordApi implements IRequestApi {

    @Override
    public String getApi() {
        return "user/password";
    }
    // ????????????
    public String getMethod() {
        return "POST"; // ??"POST", "PUT" ??
    }
    /** ?????????????????*/
    private String phone;
    /** ?????*/
    private String code;
    /** ??? */
    private String password;

    public PasswordApi setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public PasswordApi setCode(String code) {
        this.code = code;
        return this;
    }

    public PasswordApi setPassword(String password) {
        this.password = password;
        return this;
    }
}
