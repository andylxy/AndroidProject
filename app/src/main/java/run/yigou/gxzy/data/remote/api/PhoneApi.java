package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ??????
 */
public final class PhoneApi implements IRequestApi {

    @Override
    public String getApi() {
        return "user/phone";
    }
    // ????????????
    public String getMethod() {
        return "POST"; // ??"POST", "PUT" ??
    }
    /** ?????????????????????????????*/
    private String preCode;

    /** ?????? */
    private String phone;
    /** ???????????*/
    private String code;

    public PhoneApi setPreCode(String preCode) {
        this.preCode = preCode;
        return this;
    }

    public PhoneApi setPhone(String phone) {
        this.phone = phone;
        return this;
    }

    public PhoneApi setCode(String code) {
        this.code = code;
        return this;
    }
}
