

package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;


public final class AboutApi implements IRequestApi {

    @Override
    public String getApi() {
        return "getAboutInfo";
    }
    // 明确指定请求方法
    public String getMethod() {
        return "GET"; // 或 "POST", "PUT" 等
    }
}