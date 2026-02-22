package run.yigou.gxzy.http.api;

import com.hjq.http.config.IRequestApi;

import java.util.List;

/**
 * 样式配置 API 接口
 * 用于从后端获取 Tips 模块的文本渲染样式（颜色、字体大小、点击行为）
 * 
 * 返回数据结构预期：
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
 */
public final class StyleConfigApi implements IRequestApi {

    @Override
    public String getApi() {
        // TODO: 替换为实际的后端接口路径，例如 "config/tipsStyle"
        return "GetTipsStyleConfig"; 
    }
    
    // 明确指定请求方法
    public String getMethod() {
        return "GET"; 
    }

    /**
     * API 响应数据模型：样式配置
     * 对应后端返回的 JSON 结构中的 styles 数组项
     */
    public static class StyleConfigApiBean {
        private List<StyleItem> styles;

        public List<StyleItem> getStyles() {
            return styles;
        }

        public void setStyles(List<StyleItem> styles) {
            this.styles = styles;
        }

        public static class StyleItem {
            /** 标签标记，如 "r", "u" */
            private String marker;
            
            /** 颜色值，如 "#FF0000" */
            private String color;
            
            /** 是否使用小字体 */
            private boolean isSmallFont;
            
            /** 链接类型：0=无，1=药物，2=方剂，3=名词 */
            private int linkType;

            public String getMarker() { return marker; }
            public void setMarker(String marker) { this.marker = marker; }

            public String getColor() { return color; }
            public void setColor(String color) { this.color = color; }

            public boolean isSmallFont() { return isSmallFont; }
            public void setSmallFont(boolean smallFont) { isSmallFont = smallFont; }

            public int getLinkType() { return linkType; }
            public void setLinkType(int linkType) { this.linkType = linkType; }
        }
    }
}