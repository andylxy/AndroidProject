package run.yigou.gxzy.ui.reader.renderer;

import android.graphics.Color;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;

import run.yigou.gxzy.data.remote.api.StyleConfigApi;
import run.yigou.gxzy.text.TipsTextRenderer;
import run.yigou.gxzy.text.TipsTextRenderer.StyleConfig;

/**
 * TipsTextRenderer 桥接类
 * 用于 app 层将 StyleConfigApi 转换为 library 层 StyleConfig
 * updateConfigFromApi() 方法完成转换
 */
public final class TipsTextRendererBridge {

    /**
     * 从 API 返回的样式列表更新渲染器配置
     *
     * @param apiStyles API 返回的样式列表
     */
    public static void updateConfigFromApi(List<StyleConfigApi.StyleConfigApiBean.StyleItem> apiStyles) {
        if (apiStyles == null || apiStyles.isEmpty()) {
            return;
        }

        ConcurrentHashMap<String, StyleConfig> newConfigs = new ConcurrentHashMap<>();
        for (StyleConfigApi.StyleConfigApiBean.StyleItem item : apiStyles) {
            try {
                // 解析颜色字符串 #RRGGBB 格式为整型颜色值
                int color = Color.parseColor(item.getColor());

                // 存入配置表
                newConfigs.put(item.getMarker(), new StyleConfig(
                        color,
                        item.isSmallFont(),
                        item.getLinkType()
                ));
            } catch (Exception e) {
                // 忽略无法解析的样式项，不影响其他样式
                e.printStackTrace();
            }
        }

        // 应用到全局渲染器
        TipsTextRenderer.updateStyleConfig(newConfigs);
    }
}