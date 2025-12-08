

package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;


public final class YaoAliaApi implements IRequestApi {

    @Override
    public String getApi() {
        return "GetAliaZhongYao";
    }
    // 明确指定请求方法
    public String getMethod() {
        return "POST"; // 或 "POST", "PUT" 等
    }
}