package run.yigou.gxzy.data.remote.api;

import com.hjq.http.config.IRequestApi;

import java.util.List;

/**
 * ?????? API ???
 * ???????????Tips ???????????????????????????????????
 * 
 * ??????????????
 * {
 *   "code": 200,
 *   "msg": "???",
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
        // TODO: ???????????????????????"config/tipsStyle"
        return "GetTipsStyleConfig"; 
    }
    
    // ????????????
    public String getMethod() {
        return "GET"; 
    }

    /**
     * API ?????????????????
     * ???????????JSON ?????? styles ?????
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
            /** ????????? "r", "u" */
            private String marker;
            
            /** ????????"#FF0000" */
            private String color;
            
            /** ???????????*/
            private boolean isSmallFont;
            
            /** ????????=???1=?????=?????=??? */
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
