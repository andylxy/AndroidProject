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
    
    /** 样式配置列表 */
    private List<StyleItem> styles;

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
    }
}
