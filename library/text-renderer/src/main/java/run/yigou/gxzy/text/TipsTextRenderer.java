package run.yigou.gxzy.text;

import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;

/**
 * Tips 模块文本渲染器
 * <pre>
 * 负责解析自定义标签（如 $r{}, $g{} 等）并生成 SpannableString
 * 
 * 注意：本类仅负责渲染逻辑，样式配置由 TipsTextRenderConfig 统一管理
 * 
 * 使用示例：
 *
 * // 1. 应用启动时加载服务端配置
 * List&lt;StyleConfigApiBean.StyleItem&gt; serverConfigs = ...; // 从 API 获取
 * TipsTextRenderConfig.getInstance().applyServerConfig(serverConfigs);
 * 
 * // 2. 渲染文本（默认选项）
 * SpannableStringBuilder ssb = TipsTextRenderer.renderText("$r{红色文本}$u{药链接}", clickLink);
 * 
 * // 3. 渲染文本（自定义选项）
 * SpannableStringBuilder ssb = TipsTextRenderer.renderText(text, clickLink, 
 *     RenderOptions.defaults().withItemNumber(false));
 * </pre>
 */
public class TipsTextRenderer {

    /** 自定义标签正则：匹配 $marker{content} 格式，Group1=标记(marker)，Group2=内容(content)
     *  
     * 支持的 marker 格式：
     * - 单字母：$r{红色文本}
     * - 多字母：$yykd{自定义内容}
     * - 长度限制：1-10 个字母
     */
    private static final java.util.regex.Pattern TAG_PATTERN =
        java.util.regex.Pattern.compile("\\$([a-zA-Z]{1,10})\\{([^}]*)\\}");

    /**
     * 渲染文本（使用默认选项）
     * 
     * @param str 原始文本字符串
     * @param clickLink 点击回调接口
     * @return 渲染后的 SpannableStringBuilder
     */
    public static SpannableStringBuilder renderText(String str, ClickLink clickLink) {
        return renderText(str, clickLink, RenderOptions.defaults());
    }

    /**
     * 渲染文本（支持渲染选项）
     * 
     * @param str 原始文本字符串
     * @param clickLink 点击回调接口
     * @param options 渲染选项（可为 null，使用默认选项）
     * @return 渲染后的 SpannableStringBuilder
     */
    public static SpannableStringBuilder renderText(
            String str, 
            ClickLink clickLink,
            RenderOptions options) {
        
        // 空值处理
        if (str == null || str.isEmpty()) {
            return new SpannableStringBuilder();
        }
        
        // 使用默认选项
        if (options == null) {
            options = RenderOptions.defaults();
        }
        
        // 正则匹配
        java.util.regex.Matcher matcher = TAG_PATTERN.matcher(str);
        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int lastAppendPosition = 0;
        
        while (matcher.find()) {
            ssb.append(str, lastAppendPosition, matcher.start());
            
            String marker = matcher.group(1);
            String content = matcher.group(2);
            int start = ssb.length();
            
            ssb.append(content);
            int end = ssb.length();
            
            // 应用样式
            applyStyle(ssb, marker, start, end, clickLink);
            
            lastAppendPosition = matcher.end();
        }
        
        // 追加剩余文本
        if (lastAppendPosition < str.length()) {
            ssb.append(str, lastAppendPosition, str.length());
        }
        
        // 可选：项编号渲染
        if (options.isItemNumberEnabled()) {
            ItemNumberRenderer.render(ssb);
        }
        
        return ssb;
    }

    /**
     * 根据标记应用样式
     * 
     * @param spannableStringBuilder 文本对象
     * @param marker 标记
     * @param start 起始位置
     * @param end 结束位置
     * @param clickLink 点击回调接口
     */
    private static void applyStyle(SpannableStringBuilder spannableStringBuilder, 
                                   String marker, 
                                   int start, 
                                   int end, 
                                   ClickLink clickLink) {
        if (marker == null) return;

        // 从配置中心获取配置
        TipsTextRenderConfig.StyleConfig config = TipsTextRenderConfig.getInstance().getStyleConfig(marker);

        // 1. 设置相对字体大小
        if (config.isSmallFont) {
            spannableStringBuilder.setSpan(
                new RelativeSizeSpan(0.7f), 
                start, end, 
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        // 2. 设置点击事件处理（策略模式）
        if (config.linkType != 0 && clickLink != null) {
            spannableStringBuilder.setSpan(
                new ProxyClickableSpan(clickLink, config.linkType), 
                start, end, 
                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        // 3. 设置文本颜色
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(config.color);
        spannableStringBuilder.setSpan(
            foregroundColorSpan, 
            start, end, 
            SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
        );
    }
}
