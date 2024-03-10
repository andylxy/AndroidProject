package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

/**
 *    author : Android 轮子哥
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : 验证码校验
 */
public final class VierCode implements IRequestApi {

    @Override
    public String getApi() {
        return "User/getVierificationCode";
    }


    private String img;

    public String getImg() {
        return img;
    }

    public String getUuid() {
        return uuid;
    }

    private String uuid;


}