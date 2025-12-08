package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

public class AiConfigApi implements IRequestApi     {

    /**
     * @return
     */
    @Override
    public String getApi() {
        return "Ai/GetAiConfig";
    }


    // 明确指定请求方法
    public String getMethod() {
        return "GET"; // 或 "POST", "PUT" 等
    }

}
