package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 *    author : Android ?????
 *    github : https://github.com/getActivity/AndroidProject
 *    time   : 2019/12/07
 *    desc   : ????????
 */
public final class VierCode implements IRequestApi {

    @Override
    public String getApi() {
        return "getPicCaptcha";
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
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
