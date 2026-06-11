package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.greendao.entity.UserInfo;
import run.yigou.gxzy.data.remote.model.UserInfoToken;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ??????
 */
public final class LoginApi implements IRequestApi {

    @Override
    public String getApi() {
        return "login";
    }
    // ????????????
    public String getMethod() {
        return "POST"; // ??"POST", "PUT" ??
    }
    /** ?????*/
    private String userName;
    /** ?????? */
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
