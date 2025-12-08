package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : 验证码校验
 */
public final class VerifyCodeApi implements IRequestApi {

    @Override
    public String getApi() {
        return "code/checkout";
    }
    // 明确指定请求方法
    public String getMethod() {
        return "POST"; // 或 "POST", "PUT" 等
    }
    /** 手机号 */
    private String phone;
    /** 验证码 */
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