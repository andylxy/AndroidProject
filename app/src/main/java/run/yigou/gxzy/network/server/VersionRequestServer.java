package run.yigou.gxzy.network.server;

import com.hjq.http.config.IHttpPostBodyStrategy;
import com.hjq.http.config.IRequestServer;
import com.hjq.http.model.RequestBodyType;

import run.yigou.gxzy.app.AppConfig;

/**
 * 版本接口专用服务器前缀：{@code {host}/api/}。
 *
 * <p>默认 {@link RequestServer} 的前缀是 {@code /api/AppBookRequest/}，而
 * {@code /api/app/version}（spec §6.5）**不在**该命名空间下——用默认前缀会拼成
 * {@code /api/AppBookRequest/api/app/version}（404）。EasyHttp 支持按请求覆盖
 * server（{@code HttpRequest#server(IRequestServer)}），这里给它一个独立前缀。</p>
 */
public class VersionRequestServer implements IRequestServer {

    @Override
    public String getHost() {
        return AppConfig.getHostUrl() + "/api/";
    }

    @Override
    public IHttpPostBodyStrategy getBodyType() {
        return RequestBodyType.JSON;
    }
}
