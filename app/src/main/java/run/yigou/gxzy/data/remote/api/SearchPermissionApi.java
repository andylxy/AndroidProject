package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

/**
 * {@code GET /api/app/search-permission} —— App 冷启动时拉取「当前账号是否有搜索权限」。
 *
 * <p>设计依据在 <b>microfeed 仓</b>：{@code .scratch/search-permission/DESIGN.md} §5.2。</p>
 *
 * <p>鉴权走与 {@link AnnouncementApi} 相同的 {@code VersionRequestServer} 前缀
 * （{@code {host}/api/}），由 {@code InterceptorHelper} 自动注入登录用户凭证
 * （{@code mflc_}），<b>无需</b>在本类写任何 Header 逻辑。</p>
 */
public final class SearchPermissionApi implements IRequestApi {

    @Override
    public String getApi() {
        return "app/search-permission";
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
