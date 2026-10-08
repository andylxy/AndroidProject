

package run.yigou.gxzy.data.remote.api.reader;

import com.hjq.http.config.IRequestApi;


public final class YaoAliaApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetAliaZhongYao";
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }
}
