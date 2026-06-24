/*
 * 项目名: AndroidProject
 * 类名: FangNameMatcher.java
 * 包名: run.yigou.gxzy.ui.reader.search
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2026年06月22日
 * Copyright (c) 2026 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.search;

import java.util.Map;

/**
 * 方剂名称匹配器
 * 
 * <p>职责：封装方剂名称匹配逻辑（5 种策略）
 * <ul>
 *   <li>直接名称匹配</li>
 *   <li>别名匹配</li>
 *   <li>基础名称匹配（去掉剂型后缀）</li>
 *   <li>包含匹配</li>
 *   <li>反向别名检查</li>
 * </ul>
 * 
 * <p>设计意图：
 * <ul>
 *   <li>将复杂的匹配逻辑从 SearchDataAdapter 中提取，单一职责</li>
 *   <li>支持独立单元测试</li>
 *   <li>可复用于方剂/药物搜索策略</li>
 * </ul>
 */
public class FangNameMatcher {
    
    /**
     * 剂型后缀列表（按优先级排序）
     */
    private static final String[] FANG_SUFFIXES = {
        "汤", "散", "丸", "膏", "丹", "片", "胶囊", "颗粒", "口服液", "注射液"
    };
    
    /**
     * 检查方剂名称是否匹配
     * 
     * <p>支持 5 种匹配策略（按优先级）：
     * <ol>
     *   <li>直接匹配</li>
     *   <li>别名匹配</li>
     *   <li>基础名称匹配（去掉剂型后缀）</li>
     *   <li>包含匹配（关键词长度 >= 2）</li>
     *   <li>反向别名检查</li>
     * </ol>
     * 
     * @param itemName 数据项名称
     * @param keyword 搜索关键词
     * @param aliasDict 别名字典
     * @return 是否匹配
     */
    public boolean isMatch(String itemName, String keyword, Map<String, String> aliasDict) {
        if (itemName == null || keyword == null) {
            return false;
        }
        
        // 1. 直接匹配
        if (itemName.equals(keyword)) {
            return true;
        }
        
        // 2. 别名匹配
        String aliasName = resolveAlias(aliasDict, keyword);
        if (itemName.equals(aliasName)) {
            return true;
        }
        
        // 3. 基础名称匹配（去掉剂型后缀）
        String itemBase = extractBaseName(itemName);
        String keywordBase = extractBaseName(keyword);
        if (!itemBase.isEmpty() && itemBase.equals(keywordBase)) {
            return true;
        }
        
        // 4. 包含匹配（关键词长度 >= 2）
        if (keyword.length() >= 2 && itemName.contains(keyword)) {
            return true;
        }
        
        // 5. 反向别名检查
        return reverseAliasMatch(itemName, keyword, aliasDict);
    }
    
    /**
     * 提取方剂基础名称（去掉剂型后缀）
     * 
     * <p>示例：
     * <ul>
     *   <li>"桂枝汤" → "桂枝"</li>
     *   <li>"麻黄散" → "麻黄"</li>
     *   <li>"六味地黄丸" → "六味地黄"</li>
     * </ul>
     * 
     * @param fangName 方剂名称
     * @return 基础名称（去掉剂型后缀）
     */
    public String extractBaseName(String fangName) {
        if (fangName == null) {
            return "";
        }
        
        String baseName = fangName.trim();
        
        // 遍历后缀列表，匹配并移除
        for (String suffix : FANG_SUFFIXES) {
            if (baseName.endsWith(suffix) && baseName.length() > suffix.length()) {
                return baseName.substring(0, baseName.length() - suffix.length()).trim();
            }
        }
        
        return baseName;
    }
    
    /**
     * 宽松匹配（用于章节备选搜索）
     * 
     * <p>比 isMatch() 更宽松的匹配策略，用于在章节内容中查找潜在方剂引用
     * 
     * @param itemName 数据项名称
     * @param keyword 搜索关键词
     * @param aliasName 解析后的别名
     * @return 是否潜在匹配
     */
    public boolean isPotentialMatch(String itemName, String keyword, String aliasName) {
        if (itemName == null || keyword == null) {
            return false;
        }
        
        // 1. 直接包含
        if (itemName.contains(keyword)) {
            return true;
        }
        
        // 2. 别名包含
        if (!keyword.equals(aliasName) && itemName.contains(aliasName)) {
            return true;
        }
        
        // 3. 基础名称匹配
        String itemBase = extractBaseName(itemName);
        String keywordBase = extractBaseName(keyword);
        if (!itemBase.isEmpty() && itemBase.equals(keywordBase)) {
            return true;
        }
        
        // 4. 常见方剂关键词匹配
        if (keyword.length() >= 2 && itemName.length() > keyword.length()) {
            String[] commonKeywords = {"汤", "散", "丸", "膏", "丹"};
            for (String common : commonKeywords) {
                if (itemName.contains(keyword) && itemName.contains(common)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * 解析别名
     * 
     * @param aliasDict 别名字典
     * @param key 原始关键词
     * @return 解析后的别名（如无映射则返回原词）
     */
    public String resolveAlias(Map<String, String> aliasDict, String key) {
        if (aliasDict == null || key == null) {
            return key;
        }
        // 兼容 API 21：手动实现 getOrDefault
        return aliasDict.containsKey(key) ? aliasDict.get(key) : key;
    }
    
    // ========== 私有辅助方法 ==========
    
    /**
     * 反向别名检查：检查关键词是否是 itemName 的别名
     */
    private boolean reverseAliasMatch(String itemName, String keyword, Map<String, String> aliasDict) {
        if (aliasDict == null) {
            return false;
        }
        
        for (Map.Entry<String, String> entry : aliasDict.entrySet()) {
            if (entry.getValue().equals(itemName) && entry.getKey().equals(keyword)) {
                return true;
            }
        }
        
        return false;
    }
}
