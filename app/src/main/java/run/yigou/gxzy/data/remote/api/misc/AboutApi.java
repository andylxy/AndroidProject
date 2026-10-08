

package run.yigou.gxzy.data.remote.api.misc;

import com.hjq.http.config.IRequestApi;


public final class AboutApi implements IRequestApi {

    @Override
    public String getApi() {
        return "getAboutInfo";
    }
    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }
}
