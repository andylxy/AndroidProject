package run.yigou.gxzy.data.remote.api.mingci;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.data.remote.api.announcement.AnnouncementApi;

/**
 * {@code GET /api/app/mingci-permission} —— App 冷启动时拉取「当前账号是否有名词解释查看权限」。
 *
 * <p>严格镜像 {@link run.yigou.gxzy.data.remote.api.search.SearchPermissionApi}：本特性与搜索权限
 * 同构（一个全局布尔开关，G3），只是码与路由不同。</p>
 *
 * <p>鉴权走与 {@link AnnouncementApi} 相同的 {@code VersionRequestServer} 前缀
 * （{@code {host}/api/}），由 {@code InterceptorHelper} 自动注入登录用户凭证
 * （{@code mflc_}），<b>无需</b>在本类写任何 Header 逻辑。</p>
 */
public final class MingCiPermissionApi implements IRequestApi {

    @Override
    public String getApi() {
        return "app/mingci-permission";
    }

    /**
     * 刻意<b>不加</b> {@code @Override}：{@link IRequestApi} 并未声明该方法，
     * 它只是本项目约定的可选覆盖点（{@code AnnouncementApi} / {@code UpdateApi} 同理，
     * 写上注解会编译失败）。EasyHttp 通过反射查找它，存在即生效。
     */
    public String getMethod() {
        return "GET";
    }
}
