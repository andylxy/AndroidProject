package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.text.StyleConfigApiBean;

/**
 * 样式配置 API
 * 用于获取服务端下发的 Tips 文本渲染样式配置
 * 
 * 支持特性：
 * - 版本控制（增量更新）
 * - GET 请求（轻量级）
 * - 自动映射到 StyleConfigApiBean
 * 
 * 使用示例：
 * <pre>
 * // 首次请求（获取完整配置）
 * EasyHttp.get(this).api(new StyleConfigApi())
 * 
 * // 增量请求（仅获取变更配置）
 * EasyHttp.get(this).api(new StyleConfigApi().setVersion(5))
 * </pre>
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
