

package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;


public final class AboutApi implements IRequestApi {

    @Override
    public String getApi() {
        return "DevConfig/getAboutInfo";
    }
}