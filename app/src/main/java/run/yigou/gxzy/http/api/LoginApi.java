package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.http.entitymodel.UserInfoToken;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : 用户登录
 */
public final class LoginApi implements IRequestApi {

    @Override
    public String getApi() {
        return "login";
    }
    // 明确指定请求方法
    public String getMethod() {
        return "POST"; // 或 "POST", "PUT" 等
    }
    /** 手机号 */
    private String userName;
    /** 登录密码 */
    private String passWord;
    public String verificationCode;
    public String uUID;

    public LoginApi setUserName(String user) {
        this.userName = user;
        return this;
    }

    public LoginApi setPassword(String pass) {
        this.passWord = pass;
        return this;
    }

    public LoginApi setVerificationCode(String verificationCode) {
        this.verificationCode = verificationCode;
        return this;
    }

    public LoginApi setUUID(String uUID) {
        this.uUID = uUID;
        return this;
    }

    public static final class Bean extends UserInfo {

    }

}