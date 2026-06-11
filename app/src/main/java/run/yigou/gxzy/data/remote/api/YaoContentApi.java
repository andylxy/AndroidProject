

package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;


public final class YaoContentApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetAllZhongYao";
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }
}
