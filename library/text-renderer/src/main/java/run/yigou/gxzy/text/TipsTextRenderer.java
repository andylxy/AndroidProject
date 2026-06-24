package run.yigou.gxzy.text;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

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
 * // 2. 渲染文本
 * SpannableStringBuilder ssb = TipsTextRenderer.renderText("$r{红色文本}$u{药链接}", clickLink);
 * </pre>
 */
public class TipsTextRenderer {

    /** 自定义标签正则：匹配 $x{content} 格式，Group1=标记(marker)，Group2=内容(content) */
    private static final java.util.regex.Pattern TAG_PATTERN =
        java.util.regex.Pattern.compile("\\$([a-zA-Z])\\{([^}]*)\\}");

    /** 项编号高亮颜色：蓝色 */
    private static final int ITEM_NUMBER_COLOR = 0xFF0000FF;

    // 创建SpannableStringBuilder对象，用于渲染DataItem的文本、注释和视频部分

    public static SpannableStringBuilder renderText(String str, final ClickLink clickLink) {
        // 如果输入为 null，返回一个带有默认内容的 SpannableStringBuilder
        if (str == null || str.isEmpty()) {
            return new SpannableStringBuilder();
        }

        // 使用 StringBuilder 预处理，去除所有标签，计算最终文本长度（优化内存）
        // 但由于需要保留样式位置，直接操作 SpannableStringBuilder 可能更直观，但效率较低。
        // 这里采用正则匹配方式，避免复杂的 substring 和 indexOf 逻辑。

        // 正则匹配 $x{content} 格式
        // \$([a-zA-Z])\{([^}]*)\}
        // Group 1: 标记 (marker)
        // Group 2: 内容 (content)

        java.util.regex.Matcher matcher = TAG_PATTERN.matcher(str);

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        int lastAppendPosition = 0;

        while (matcher.find()) {
            // 追加前一段普通文本
            ssb.append(str, lastAppendPosition, matcher.start());

            String marker = matcher.group(1);
            String content = matcher.group(2);
            int start = ssb.length();

            // 追加标签内的内容
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

        // 处理项编号特殊渲染
        renderItemNumber(ssb);

        return ssb;
    }

    // 根据标记应用样式的方法
    private static void applyStyle(SpannableStringBuilder spannableStringBuilder, String marker, int start, int end, final ClickLink clickLink) {
        if (marker == null) return;

        // 从配置中心获取配置
        TipsTextRenderConfig.StyleConfig config = TipsTextRenderConfig.getInstance().getStyleConfig(marker);

        // 1. 设置相对字体大小
        if (config.isSmallFont) {
            spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 2. 设置点击事件处理（策略模式）
        if (config.linkType != 0 && clickLink != null) {
            spannableStringBuilder.setSpan(new ProxyClickableSpan(clickLink, config.linkType), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 3. 设置文本颜色
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(config.color);
        spannableStringBuilder.setSpan(foregroundColorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    /**
     * 代理 ClickableSpan，将点击事件路由到 ClickLink 的通用处理器
     * 支持动态扩展链接类型，无需修改此类
     */
    private static class ProxyClickableSpan extends ClickableSpan {
        private final ClickLink clickLink;
        private final int linkType;
        
        ProxyClickableSpan(ClickLink clickLink, int linkType) {
            this.clickLink = clickLink;
            this.linkType = linkType;
        }
        
        @Override
        public void onClick(View view) {
            if (clickLink == null) return;
            // 防御：非 TextView 安全跳过，避免 ClassCastException
            if (!(view instanceof TextView)) return;
            TextView textView = (TextView) view;
            // 路由到通用处理器（支持动态扩展）
            clickLink.onClickLink(linkType, textView, this);
        }
    }

    /**
     * 在SpannableStringBuilder中渲染项编号，通过对特殊字符（"、"）之前的数字前缀设置颜色跨度。
     *
     * @param spannableStringBuilder 要修改的SpannableStringBuilder对象。
     */
    public static void renderItemNumber(SpannableStringBuilder spannableStringBuilder) {
        // 将SpannableStringBuilder转换为String，以便于操作
        String text = spannableStringBuilder.toString();

        // 定义作为分隔符的特殊字符
        final String DELIMITER = "、";

        // 检查文本是否包含分隔符，并且分隔符之前的子字符串是否为数字
        int delimiterIndex = text.indexOf(DELIMITER);
        if (delimiterIndex != -1 && isNumeric(text.substring(0, delimiterIndex))) {
            // 对数字前缀设置前景色跨度
            spannableStringBuilder.setSpan(
                    new ForegroundColorSpan(ITEM_NUMBER_COLOR),  // 项编号高亮颜色
                    0,  // Span的起始索引
                    delimiterIndex,  // Span的结束索引
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE  // Span标志，避免影响周围文本
            );
        }
    }

    /**
     * 检查给定的字符串是否为数字（仅由数字组成）。
     *
     * @param str 要检查的字符串。
     * @return 如果字符串是数字，则返回 true；否则返回 false。
     */
    public static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }



    /**
     * 高亮匹配项
     * 此方法用于在SpannableStringBuilder中查找匹配项，并将它们高亮显示（黄色背景）
     * 保持与 TextHighlighter 一致
     *
     * @param matcher   用于查找匹配项的Matcher对象
     * @param spannable 要进行高亮显示的SpannableStringBuilder对象
     */
    public static void highlightMatches(java.util.regex.Matcher matcher, SpannableStringBuilder spannable) {
        // 重置 Matcher 位置，确保无论调用方是否已消费，行为一致
        matcher.reset();
        // 定义高亮显示的颜色为黄色 (与 TextHighlighter 一致)
        int color = 0xFFFFFF00;
        // 遍历所有匹配项并应用高亮
        while (matcher.find()) {
            // 设置文本高亮背景颜色
            spannable.setSpan(new BackgroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}