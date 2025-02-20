package run.yigou.gxzy.http.api;

import androidx.annotation.NonNull;

import com.hjq.http.EasyConfig;
import com.hjq.http.config.IRequestApi;
import com.hjq.http.config.IRequestClient;
import com.hjq.http.config.IRequestServer;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;


import run.yigou.gxzy.ui.tips.aimsg.AiConfig;

public class AiMsgApi implements IRequestApi  , IRequestServer   {
   // String url = AiConfig.getProxyAddress() + "/v1/chat/completions";
    /**
     * @return
     */
    @Override
    public String getApi() {
        return "/v1/chat/completions";
    }

    /**
     * @return
     */
    @Override
    public String getHost() {
        return AiConfig.getProxyAddress();
    }


}
