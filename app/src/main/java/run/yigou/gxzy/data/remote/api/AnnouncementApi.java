package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 * {@code GET /api/app/announcements} —— 后端下发的运营/系统消息（DESIGN §5.3）。
 *
 * <p>公共端点：未登录也能调用。与 {@link UpdateApi} 同理，必须配合
 * {@code VersionRequestServer} 使用（前缀 {@code {host}/api/}），否则会被拼到
 * AppBookRequest 命名空间下；调用入口见 {@code AnnouncementManager}。</p>
 */
public final class AnnouncementApi implements IRequestApi {

    @Override
    public String getApi() {
        return "app/announcements";
    }

    /**
     * 刻意<b>不加</b> {@code @Override}：{@link IRequestApi} 并未声明该方法，
     * 它只是本项目约定的可选覆盖点（{@code UpdateApi} 同样如此，写上注解会编译失败：
     * 「方法不会覆盖或实现超类型的方法」）。EasyHttp 通过反射查找它，存在即生效。
     */
    public String getMethod() {
        return "GET";
    }
}
