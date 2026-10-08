package run.yigou.gxzy.data.remote.api.update;

import com.hjq.http.config.IRequestApi;

/**
 * {@code GET /api/app/version} —— 后端下发的版本/升级信息（spec §6.5）。
 *
 * <p>公共端点：未登录也能调用（老客户端靠它拿到下载地址去重装）。
 * 必须配合 {@code VersionRequestServer} 使用（前缀 {@code {host}/api/}），
 * 否则会被拼到 AppBookRequest 命名空间下；调用入口见 {@code UpdateManager}。</p>
 */
public final class UpdateApi implements IRequestApi {

    @Override
    public String getApi() {
        return "app/version";
    }

    public String getMethod() {
        return "GET";
    }
}
