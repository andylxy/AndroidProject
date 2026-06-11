

package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;


public final class MingCiContentApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetAllMingCi";
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }
}
