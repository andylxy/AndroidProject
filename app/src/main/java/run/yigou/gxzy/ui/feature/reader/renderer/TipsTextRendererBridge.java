package run.yigou.gxzy.ui.feature.reader.renderer;

import android.graphics.Color;

import java.util.HashMap;
import java.util.List;

import run.yigou.gxzy.http.api.StyleConfigApi;
import run.yigou.gxzy.text.TipsTextRenderer;
import run.yigou.gxzy.text.TipsTextRenderer.StyleConfig;

/**
 * TipsTextRenderer 桥接层
 * 负责将 app 模块专属的 StyleConfigApi 类型转换为 library 模块的 StyleConfig
 * updateConfigFromApi() 的唯一落位点
 */
public final class TipsTextRendererBridge {

    /**
     * 将 API 返回的配置列表转换为内部样式映射表，并更新配置
     *
     * @param apiStyles API 返回的样式列表
     */
    public static void updateConfigFromApi(List<StyleConfigApi.StyleConfigApiBean.StyleItem> apiStyles) {
        if (apiStyles == null || apiStyles.isEmpty()) {
            return;
        }

        HashMap<String, StyleConfig> newConfigs = new HashMap<>();
        for (StyleConfigApi.StyleConfigApiBean.StyleItem item : apiStyles) {
            try {
                // 解析颜色，支持 #RRGGBB 格式，如果解析失败则捕获异常
                int color = Color.parseColor(item.getColor());

                // 构建内部配置对象
                newConfigs.put(item.getMarker(), new StyleConfig(
                        color,
                        item.isSmallFont(),
                        item.getLinkType()
                ));
            } catch (Exception e) {
                // 仅记录错误，跳过当前出错的项，不影响其他项的加载
                e.printStackTrace();
            }
        }

        // 更新全局配置
        TipsTextRenderer.updateStyleConfig(newConfigs);
    }
}