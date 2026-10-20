package run.yigou.gxzy.data.remote.api.style;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.text.StyleConfigApiBean;

/**
 * 样式配置 API
 * 用于获取服务端下发的 Tips 文本渲染样式配置
 *
 * <p><b>这个接口没有版本协商，也没有增量下发。</b> {@link #setVersion} 传的是
 * {@link TipsTextRenderConfig#getConfigVersion()} 的<b>本地自增计数器</b>，
 * 服务端 {@code GetTipsStyleConfig} 既不读它也不回它 —— 每次请求都返回全量目录。
 * 要识别「服务端配置变了」，得靠响应里携带的服务端版本号（待实现，见 ADR-0028），
 * 不是这个参数。把 {@code version} 当增量游标用是错的。
 *
 * <p>GET 请求（轻量级），响应自动映射到 StyleConfigApiBean。
 *
 * <p>鉴权：服务端对 {@code /api/AppBookRequest/*} 一律要求登录凭证 Bearer
 * （{@code mflc_…}），<b>没有硬编码兜底 key</b>（microfeed {@code src/middleware.ts}
 * 的原注释即此意）。历史上 {@code AppConst.AllowAnonymous_Token} 那个匿名 JWT
 * 已过期，且发送它的代码被注释掉了 —— 不要指望它。
 */
public final class StyleConfigApi implements IRequestApi {
    
    /** 配置版本号（用于增量更新） */
    private int version = 0;
    
    @Override
    public String getApi() {
        return "GetTipsStyleConfig";
    }
    
    public String getMethod() {
        return "GET";
    }
    
    /**
     * 设置版本号
     * @param version 配置版本号（0=首次请求，>0=增量请求）
     * @return 当前实例（支持链式调用）
     */
    public StyleConfigApi setVersion(int version) {
        this.version = version;
        return this;
    }
    
    /**
     * 获取版本号
     * @return 配置版本号
     */
    public int getVersion() {
        return version;
    }
    
    /**
     * API 响应数据类
     * 继承 library 模块的 StyleConfigApiBean
     */
    public static class StyleConfigApiBean extends run.yigou.gxzy.text.StyleConfigApiBean {
        // 继承父类字段：
        // - List<StyleItem> styles
        // - StyleItem { marker, color, isSmallFont, linkType }
    }
}
