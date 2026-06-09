package run.yigou.gxzy.ui.feature.reader.renderer;

import run.yigou.gxzy.model.DataItem;
import run.yigou.gxzy.model.HH2SectionData;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import run.yigou.gxzy.http.api.StyleConfigApi;
import run.yigou.gxzy.ui.feature.reader.listener.ClickLink;

/**
 * Tips 模块文本渲染器
 * 负责解析自定义标签（如 $r{}, $g{} 等）并生成 SpannableString
 */
public class TipsTextRenderer {

    // 定义样式配置类
    public static class StyleConfig {
        public int color;
        public boolean isSmallFont; // 是否使用相对小字体
        public int linkType; // 链接类型: 0:无, 1:Yao, 2:Fang, 3:MingCi

        public StyleConfig(int color, boolean isSmallFont, int linkType) {
            this.color = color;
            this.isSmallFont = isSmallFont;
            this.linkType = linkType;
        }
    }

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
        updateStyleConfig(newConfigs);
    }

    // 使用 HashMap 存储样式配置，支持动态更新
    private static final HashMap<String, StyleConfig> configMap = new HashMap<>();

    static {
        // 初始化默认配置，保持与原有 switch 逻辑一致
        configMap.put("r", new StyleConfig(Color.RED, true, 0));
        configMap.put("n", new StyleConfig(Color.BLUE, false, 0));
        configMap.put("f", new StyleConfig(Color.BLUE, false, ProxyClickableSpan.TYPE_FANG));
        configMap.put("a", new StyleConfig(Color.GRAY, true, 0));
        configMap.put("m", new StyleConfig(Color.RED, false, 0));
        configMap.put("g", new StyleConfig(Color.argb(230, 0, 128, 255), false, ProxyClickableSpan.TYPE_MINGCI));
        configMap.put("u", new StyleConfig(Color.BLUE, false, ProxyClickableSpan.TYPE_YAO));
        configMap.put("v", new StyleConfig(Color.BLUE, false, 0));
        configMap.put("w", new StyleConfig(Color.rgb(28, 181, 92), true, 0));
        configMap.put("q", new StyleConfig(Color.rgb(61, 200, 120), false, 0));
        configMap.put("h", new StyleConfig(Color.BLACK, false, 0));
        configMap.put("x", new StyleConfig(Color.parseColor("#EA8E3B"), false, 0));
        configMap.put("y", new StyleConfig(Color.parseColor("#9A764F"), false, 0));
    }

    /**
     * 更新样式配置
     * @param newConfigs 新的样式配置映射
     */
    public static void updateStyleConfig(HashMap<String, StyleConfig> newConfigs) {
        if (newConfigs != null) {
            configMap.putAll(newConfigs);
        }
    }

    /**
     * 根据输入的字符串获取对应的颜色值。
     *
     * @param s 输入的字符串，表示颜色的键
     * @return 对应的颜色值，如果找不到则返回黑色
     */
    public static int getColoredTextByStrClass(String s) {
        StyleConfig config = configMap.get(s);
        return config != null ? config.color : Color.BLACK;
    }

    // 创建SpannableStringBuilder对象
    public static SpannableStringBuilder createSpannable(String text) {
        return createSpannable(text, null);
    }
    
    // 创建SpannableStringBuilder对象，用于渲染DataItem的文本、注释和视频部分
    public static SpannableStringBuilder createSpannable(String text, final ClickLink clickLink) {
        // 当输入的文本为null时，返回一个包含空字符串的SpannableStringBuilder对象
        if (text == null) {
            return new SpannableStringBuilder("");
        }
        // 返回渲染后的文本的SpannableStringBuilder对象
        return renderText(text, clickLink);
    }

    //todo 所有的SpannableStringBuilder 都是在这里处理的.
    //如果要改变样式，需要修改这里，同时修改renderItemNumber()
    public static SpannableStringBuilder renderText(String str) {
        return renderText(str, null);
    }

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
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\$([a-zA-Z])\\{([^}]*)\\}");
        java.util.regex.Matcher matcher = pattern.matcher(str);
        
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
        
        StyleConfig config = configMap.get(marker);
        if (config == null) return;
        
        // 1. 设置相对字体大小
        if (config.isSmallFont) {
            spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        // 2. 设置点击事件处理
        if (config.linkType != 0 && clickLink != null) {
             spannableStringBuilder.setSpan(new ProxyClickableSpan(clickLink, config.linkType), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        // 3. 设置文本颜色
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(config.color);
        spannableStringBuilder.setSpan(foregroundColorSpan, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    
    /**
     * 静态内部类实现 ClickableSpan，减少匿名类创建开销
     */
    private static class ProxyClickableSpan extends ClickableSpan {
        static final int TYPE_YAO = 1;
        static final int TYPE_FANG = 2;
        static final int TYPE_MINGCI = 3;
        
        private final ClickLink clickLink;
        private final int type;
        
        ProxyClickableSpan(ClickLink clickLink, int type) {
            this.clickLink = clickLink;
            this.type = type;
        }
        
        @Override
        public void onClick(View view) {
            if (clickLink == null) return;
            switch (type) {
                case TYPE_YAO:
                    clickLink.clickYaoLink((TextView) view, this);
                    break;
                case TYPE_FANG:
                    clickLink.clickFangLink((TextView) view, this);
                    break;
                case TYPE_MINGCI:
                    clickLink.clickMingCiLink((TextView) view, this);
                    break;
            }
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
                    new ForegroundColorSpan(0xFF0000FF),  // 颜色为蓝色（十六进制表示）
                    0,  // Span的起始索引
                    delimiterIndex,  // Span的结束索引
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE  // Span标志，避免影响周围文本
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
        // 检查字符串是否为 null 或空字符串
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 从字符串的末尾开始检查每个字符
        for (int i = str.length() - 1; i >= 0; i--) {
            char charAt = str.charAt(i);
            // 如果字符不是数字，则返回 false
            if (charAt < '0' || charAt > '9') {
                return false;
            }
        }

        // 如果所有字符都是数字，则返回 true
        return true;
    }

    /**
     * 查找给定字符串 (str) 中所有子字符串 (str2) 的起始位置。
     *
     * @param str  主字符串。
     * @param str2 要查找的子字符串。
     * @return 包含子字符串在主字符串中所有起始位置的列表。
     */
    public static ArrayList<Integer> getAllSubStringPos(String str, String str2) {
        // 初始化一个ArrayList来存储所有匹配的位置
        ArrayList<Integer> positions = new ArrayList<>();

        // 确保主字符串和子字符串都不是null，并且子字符串不为空
        if (str == null || str2 == null || str2.isEmpty() || str.isEmpty()) {
            return positions;
        }

        // 获取主字符串的长度和子字符串的长度
        int strLength = str.length();
        int str2Length = str2.length();

        // 初始化搜索索引
        int index = 0;

        // 在主字符串中查找子字符串
        while (index <= strLength - str2Length) {
            // 从当前索引位置查找子字符串
            int foundIndex = str.indexOf(str2, index);

            // 如果没有找到子字符串，退出循环
            if (foundIndex == -1) {
                break;
            }

            // 将找到的位置添加到结果列表中
            positions.add(foundIndex);

            // 移动索引到子字符串末尾的下一个位置，继续查找
            index = foundIndex + str2Length;
        }

        return positions;
    }
    /**
     * 高亮匹配项
     * 此方法用于在SpannableStringBuilder中查找匹配项，并将它们高亮显示（黄色背景）
     * 保持与 TipsBookNetReadFragment (TextHighlighter) 一致
     *
     * @param matcher   用于查找匹配项的Matcher对象
     * @param spannable 要进行高亮显示的SpannableStringBuilder对象
     */
    public static void highlightMatches(java.util.regex.Matcher matcher, SpannableStringBuilder spannable) {
        // 定义高亮显示的颜色为黄色 (与 TextHighlighter 一致)
        int color = 0xFFFFFF00; 
        // 遍历所有匹配项并应用高亮
        while (matcher.find()) {
            // 设置文本高亮背景颜色
            spannable.setSpan(new android.text.style.BackgroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }
}
