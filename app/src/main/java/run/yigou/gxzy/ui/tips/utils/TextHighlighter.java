package run.yigou.gxzy.ui.tips.utils;

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
}
