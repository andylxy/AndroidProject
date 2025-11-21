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
        return "getPicCaptcha";
    }

    public final static class Bean {
        private String validCodeBase64;
        private boolean isCode;
        private String validCodeReqNo;

        public boolean isCode() {
            return isCode;
        }
        public String getImg() {
            return validCodeBase64;
        }

        public String getUuid() {
            return validCodeReqNo;
        }
    }

}