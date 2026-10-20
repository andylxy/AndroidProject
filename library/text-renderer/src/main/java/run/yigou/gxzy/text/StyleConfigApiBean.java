package run.yigou.gxzy.text;

import java.util.List;

/**
 * 服务端样式配置数据类
 * 用于接收服务端下发的样式配置
 * 
 * 服务端返回格式示例：
 * <pre>
 * {
 *   "styles": [
 *     { "marker": "r", "color": "#FF0000", "isSmallFont": true, "linkType": 0 },
 *     { "marker": "u", "color": "#0000FF", "isSmallFont": false, "linkType": 1 }
 *   ]
 * }
 * </pre>
 * 
 * 使用方式：
 * <pre>
 * // 1. 从服务端 API 获取
 * StyleConfigApi api = new StyleConfigApi();
 * EasyHttp.post(api).request(new EasyCallBack<StyleConfigApiBean>() {
 *     @Override
 *     public void onSuccess(StyleConfigApiBean response) {
 *         TipsTextRenderConfig.getInstance().applyServerConfig(response.getStyles());
 *     }
 * });
 * 
 * // 2. 或直接构造（用于测试/本地配置）
 * StyleConfigApiBean bean = new StyleConfigApiBean();
 * List<StyleItem> items = new ArrayList<>();
 * StyleItem item = new StyleItem();
 * item.setMarker("r");
 * item.setColor("#FF0000");
 * item.setSmallFont(true);
 * item.setLinkType(0);
 * items.add(item);
 * bean.setStyles(items);
 * TipsTextRenderConfig.getInstance().applyServerConfig(bean.getStyles());
 * </pre>
 */
public class StyleConfigApiBean {

    /**
     * 服务端配置版本号。
     *
     * <p>取值 = 服务端 {@code ext_annotation_markers} 的 {@code MAX(updated_at_ms)}，
     * 即「目录最后一次被改动的时间」。客户端把它连同缓存一起落盘，下次启动据此判断
     * 手里的配置是否最新；0 表示服务端没给（老版本响应），按「未知」处理。
     *
     * <p><b>注意</b>：请求参数里的 {@code version} 不是这个 —— 那个是客户端本地的
     * 自增计数器，服务端不读它。别把两者混为一谈（见 StyleConfigApi 的类注释）。
     */
    private long version;

    /** 样式配置列表 */
    private List<StyleItem> styles;

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public List<StyleItem> getStyles() {
        return styles;
    }

    public void setStyles(List<StyleItem> styles) {
        this.styles = styles;
    }

    /**
     * 单个样式配置项
     */
    public static class StyleItem {
        /** 标记（单个字母，如 "r", "u", "f"） */
        private String marker;
        
        /** 颜色值（如 "#FF0000"） */
        private String color;
        
        /** 是否使用小字体 */
        private boolean isSmallFont;
        
        /** 链接类型（0=无链接，>=1 由调用方自定义） */
        private int linkType;

        public String getMarker() { 
            return marker; 
        }
        
        public void setMarker(String marker) { 
            this.marker = marker; 
        }

        public String getColor() { 
            return color; 
        }
        
        public void setColor(String color) { 
            this.color = color; 
        }

        public boolean isSmallFont() { 
            return isSmallFont; 
        }
        
        public void setSmallFont(boolean smallFont) { 
            isSmallFont = smallFont; 
        }

        public int getLinkType() { 
            return linkType; 
        }
        
        public void setLinkType(int linkType) {
            this.linkType = linkType;
        }

        /** 小字号相对比例（服务端迁移 0109 起可配）。未下发时为 0，
         *  StyleConfig 的构造函数会回落默认 0.7，与老包行为一致。 */
        private float smallFontScale;

        public float getSmallFontScale() {
            return smallFontScale;
        }

        public void setSmallFontScale(float smallFontScale) {
            this.smallFontScale = smallFontScale;
        }
    }
}
