package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

public class AiConfigApi implements IRequestApi     {

    /**
     * @return
     */
    @Override
    public String getApi() {
        return "Ai/GetAiConfig";
    }


    // ????????????
    public String getMethod() {
        return "GET"; // ??"POST", "PUT" ??
    }

}
