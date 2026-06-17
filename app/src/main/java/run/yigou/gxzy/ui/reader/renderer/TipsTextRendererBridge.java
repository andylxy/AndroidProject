package run.yigou.gxzy.ui.reader.renderer;

import android.graphics.Color;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

import run.yigou.gxzy.data.remote.api.StyleConfigApi;
import run.yigou.gxzy.text.TipsTextRenderer;
import run.yigou.gxzy.text.TipsTextRenderer.StyleConfig;

/**
 * TipsTextRenderer ???
 * ??? app ????? StyleConfigApi ????? library ??? StyleConfig
 * updateConfigFromApi() ??????
 */
public final class TipsTextRendererBridge {

    /**
     * ? API ???????????????????????
     *
     * @param apiStyles API ???????
     */
    public static void updateConfigFromApi(List<StyleConfigApi.StyleConfigApiBean.StyleItem> apiStyles) {
        if (apiStyles == null || apiStyles.isEmpty()) {
            return;
        }

        ConcurrentHashMap<String, StyleConfig> newConfigs = new ConcurrentHashMap<>();
        for (StyleConfigApi.StyleConfigApiBean.StyleItem item : apiStyles) {
            try {
                // ??????? #RRGGBB ??????????????
                int color = Color.parseColor(item.getColor());

                // ????????
                newConfigs.put(item.getMarker(), new StyleConfig(
                        color,
                        item.isSmallFont(),
                        item.getLinkType()
                ));
            } catch (Exception e) {
                // ????????????????????????
                e.printStackTrace();
            }
        }

        // ??????
        TipsTextRenderer.updateStyleConfig(newConfigs);
    }
}