package run.yigou.gxzy.ui.tips.tipsutils;

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

/**
 * Tips 模块文本渲染器
 * 负责解析自定义标签（如 $r{}, $g{} 等）并生成 SpannableString
 */
public class TipsTextRenderer {

    private static final HashMap<String, Integer> colorMap = new HashMap<>();

    static {
        // 初始化颜色映射
        colorMap.put("r", Color.RED); // 红色
        colorMap.put("n", Color.BLUE); // 蓝色
        colorMap.put("f", Color.BLUE); // 蓝色
        colorMap.put("a", Color.GRAY); // 灰色
        colorMap.put("m", Color.RED); // 红色
        colorMap.put("g", Color.argb(230, 0, 128, 255)); // 半透明蓝色
        colorMap.put("u", Color.BLUE); // 蓝色
        colorMap.put("v", Color.BLUE); // 蓝色
        colorMap.put("w", Color.rgb(28, 181, 92)); // 绿色
        colorMap.put("q", Color.rgb(61, 200, 120)); // 自定义绿色
        colorMap.put("h", Color.BLACK); // 黑色
        colorMap.put("x", Color.parseColor("#EA8E3B")); // 自定义橙色
        colorMap.put("y", Color.parseColor("#9A764F")); // 自定义棕色
    }

    /**
     * 根据输入的字符串获取对应的颜色值。
     *
     * @param s 输入的字符串，表示颜色的键
     * @return 对应的颜色值，如果找不到则返回黑色
     */
    public static int getColoredTextByStrClass(String s) {
        Integer colorValue = colorMap.get(s);
        return colorValue != null ? colorValue : Color.BLACK;
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
        
        // 根据不同的标记应用样式
        switch (marker) {
            case "a":
            case "w":
            case "r":
                // 设置相对字体大小
                spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case "u":
                // 设置点击事件处理
                if (clickLink != null) {
                    spannableStringBuilder.setSpan(new ProxyClickableSpan(clickLink, ProxyClickableSpan.TYPE_YAO), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                break;
            case "f":
                // 设置另一个点击事件处理
                if (clickLink != null) {
                    spannableStringBuilder.setSpan(new ProxyClickableSpan(clickLink, ProxyClickableSpan.TYPE_FANG), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                break;
            case "g":
                // 设置另一个点击事件处理
                if (clickLink != null) {
                    spannableStringBuilder.setSpan(new ProxyClickableSpan(clickLink, ProxyClickableSpan.TYPE_MINGCI), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                break;
        }
        // 设置文本颜色
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getColoredTextByStrClass(marker));
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
