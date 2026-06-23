package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

import run.yigou.gxzy.text.StyleConfigApiBean;

/**
 * 样式配置 API 请求类
 * 用于获取服务端下发的 Tips 文本渲染样式配置
 * 
 * 服务端返回格式示例：
 * {
 *   "code": 200,
 *   "msg": "成功",
 *   "data": {
 *     "styles": [
 *       { "marker": "r", "color": "#FF0000", "isSmallFont": true, "linkType": 0 },
 *       { "marker": "u", "color": "#0000FF", "isSmallFont": false, "linkType": 1 }
 *     ]
 *   }
 * }
 * 
 * 复用策略：
 * - StyleConfigApiBean 继承 library 模块的类（字段完全相同）
 * - StyleItem 直接使用 library 模块的内部类
 * - 避免在 app 模块重复定义，保持单一数据源
 */
public final class StyleConfigApi implements IRequestApi {

    @Override
    public String getApi() {
        // TODO: 替换为实际的服务端接口地址，例如 "config/tipsStyle"
        return "GetTipsStyleConfig"; 
    }
    
    // 请求方法（默认 POST）
    public String getMethod() {
        return "GET"; 
    }

    /**
     * API 响应数据类
     * 继承 library 模块的 StyleConfigApiBean，复用字段定义
     * 服务端返回 JSON 中的 styles 数组自动映射到此类
     */
    public static class StyleConfigApiBean extends run.yigou.gxzy.text.StyleConfigApiBean {
        // 继承父类的所有字段和方法：
        // - List<StyleItem> styles
        // - StyleItem { marker, color, isSmallFont, linkType }
    }
}
