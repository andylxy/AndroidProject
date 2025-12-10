package run.yigou.gxzy.ui.tips.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索匹配工具类
 * 负责在文本中查找关键字的所有匹配位置
 */
public class SearchMatcher {
    
    /**
     * 查找所有匹配位置（不区分大小写）
     * 
     * @param text 要搜索的文本
     * @param keyword 关键字
     * @return 匹配结果列表
     */
    public static List<MatchResult> findMatches(String text, String keyword) {
        List<MatchResult> results = new ArrayList<>();
        
        if (text == null || keyword == null || keyword.isEmpty()) {
            return results;
        }
        
        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        
        int index = 0;
        while ((index = lowerText.indexOf(lowerKeyword, index)) != -1) {
            MatchResult result = new MatchResult();
            result.start = index;
            result.end = index + keyword.length();
            result.matchedText = text.substring(index, result.end);
            results.add(result);
            
            index += keyword.length();
        }
        
        return results;
    }
    
    /**
     * 检查文本是否包含关键字（不区分大小写）
     * 
     * @param text 要检查的文本
     * @param keyword 关键字
     * @return 是否包含
     */
    public static boolean contains(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return false;
        }
        
        return text.toLowerCase().contains(keyword.toLowerCase());
    }
    
    /**
     * 匹配结果类
     */
    public static class MatchResult {
        /** 匹配开始位置 */
        public int start;
        
        /** 匹配结束位置 */
        public int end;
        
        /** 匹配的文本 */
        public String matchedText;
    }
}
