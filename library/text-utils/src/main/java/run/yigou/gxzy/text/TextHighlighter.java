package run.yigou.gxzy.text;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;

import java.util.List;

/**
 * 文本高亮工具类
 * 负责在 SpannableStringBuilder 中高亮显示关键字
 */
public class TextHighlighter {

    /** 默认高亮颜色：黄色 */
    private static final int DEFAULT_HIGHLIGHT_COLOR = 0xFFFFFF00;

    /**
     * 高亮显示关键字（使用默认颜色）
     *
     * @param text 要高亮的文本
     * @param keyword 关键字
     * @return 高亮后的文本
     */
    public static SpannableStringBuilder highlight(SpannableStringBuilder text, String keyword) {
        return highlight(text, keyword, DEFAULT_HIGHLIGHT_COLOR);
    }

    /**
     * 高亮显示关键字
     *
     * @param text 要高亮的文本
     * @param keyword 关键字
     * @param highlightColor 高亮颜色
     * @return 高亮后的文本
     */
    public static SpannableStringBuilder highlight(
            SpannableStringBuilder text,
            String keyword,
            int highlightColor) {

        if (text == null || keyword == null || keyword.isEmpty()) {
            return text;
        }

        String plainText = text.toString();
        List<SearchMatcher.MatchResult> matches = SearchMatcher.findMatches(plainText, keyword);

        for (SearchMatcher.MatchResult match : matches) {
            text.setSpan(
                new BackgroundColorSpan(highlightColor),
                match.start,
                match.end,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        return text;
    }

    /**
     * 创建高亮文本的副本
     *
     * @param originalText 原始文本
     * @param keyword 关键字
     * @return 高亮后的新文本
     */
    public static SpannableStringBuilder createHighlighted(
            SpannableStringBuilder originalText,
            String keyword) {

        if (originalText == null) {
            return new SpannableStringBuilder();
        }

        // 创建副本
        SpannableStringBuilder copy = new SpannableStringBuilder(originalText);
        return highlight(copy, keyword);
    }

    /**
     * 高亮 Matcher 匹配项（使用默认黄色）
     * 
     * <p>使用场景：
     * <ul>
     *   <li>调用方已经创建了 Matcher 对象</li>
     *   <li>需要复用 Matcher 的匹配结果</li>
     * </ul>
     * 
     * @param matcher 已创建的 Matcher 对象（会自动 reset）
     * @param spannable 要应用高亮的 SpannableStringBuilder
     */
    public static void highlightMatches(
            java.util.regex.Matcher matcher, 
            SpannableStringBuilder spannable) {
        highlightMatches(matcher, spannable, DEFAULT_HIGHLIGHT_COLOR);
    }

    /**
     * 高亮 Matcher 匹配项（支持自定义颜色）
     * 
     * <p>使用场景：
     * <ul>
     *   <li>调用方已经创建了 Matcher 对象</li>
     *   <li>需要复用 Matcher 的匹配结果</li>
     *   <li>需要自定义高亮颜色</li>
     * </ul>
     * 
     * @param matcher 已创建的 Matcher 对象（会自动 reset）
     * @param spannable 要应用高亮的 SpannableStringBuilder
     * @param highlightColor 高亮颜色
     */
    public static void highlightMatches(
            java.util.regex.Matcher matcher, 
            SpannableStringBuilder spannable,
            int highlightColor) {
        
        if (matcher == null || spannable == null) {
            return;
        }
        
        // 重置 Matcher 位置，确保无论调用方是否已消费，行为一致
        matcher.reset();
        
        // 遍历所有匹配项并应用高亮
        while (matcher.find()) {
            spannable.setSpan(
                new BackgroundColorSpan(highlightColor),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }
    }
}
