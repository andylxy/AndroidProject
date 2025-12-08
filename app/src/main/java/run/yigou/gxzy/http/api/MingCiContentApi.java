

package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;


public final class MingCiContentApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetAllMingCi";
    }
    // 明确指定请求方法
    public String getMethod() {
        return "GET"; // 或 "POST", "PUT" 等
    }
}