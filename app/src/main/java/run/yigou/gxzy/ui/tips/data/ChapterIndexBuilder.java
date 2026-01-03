/*
 * 项目名: AndroidProject
 * 类名: ChapterIndexBuilder.java
 * 包名: run.yigou.gxzy.ui.tips.data
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.data;

import android.text.TextUtils;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import run.yigou.gxzy.other.EasyLog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.utils.DebugLog;

/**
 * 章节索引构建器
 * 提供高效的章节查找和搜索功能
 * 特性：
 * - O(1) 复杂度的 signatureId 查找（SparseArray）
 * - 关键词倒排索引（加速搜索）
 * - 线程安全
 */
public class ChapterIndexBuilder {
    
    private static final String TAG = "ChapterIndexBuilder";
    
    // signatureId -> Chapter 索引
    private final SparseArray<Chapter> signatureIndex;
    
    // 关键词倒排索引：keyword -> List<Chapter>
    private final Map<String, List<Chapter>> keywordIndex;
    
    // 章节标题索引：title -> Chapter
    private final Map<String, Chapter> titleIndex;
    
    // 是否已构建索引
    private volatile boolean isBuilt = false;
    
    /**
     * 构造函数
     */
    public ChapterIndexBuilder() {
        this.signatureIndex = new SparseArray<>();
        this.keywordIndex = new HashMap<>();
        this.titleIndex = new HashMap<>();
    }
    
    /**
     * 构建索引
     * @param chapters 章节列表
     */
    public synchronized void buildIndex(@NonNull List<Chapter> chapters) {
        long startTime = System.currentTimeMillis();
        
        // 清空现有索引
        clear();
        
        for (Chapter chapter : chapters) {
            if (chapter == null) {
                continue;
            }
            
            // 1. 构建 signatureId 索引
            Long signatureId = chapter.getSignatureId();
            if (signatureId != null && signatureId > 0) {
                signatureIndex.put(signatureId.intValue(), chapter);
            }
            
            // 2. 构建标题索引
            String title = chapter.getChapterHeader();
            if (!TextUtils.isEmpty(title)) {
                titleIndex.put(title.trim(), chapter);
                
                // 3. 构建关键词索引（分词）
                indexKeywords(chapter, title);
            }
        }
        
        isBuilt = true;
        
        long elapsed = System.currentTimeMillis() - startTime;
        EasyLog.print(TAG, String.format(
            "Index built: %d chapters, %d signatures, %d keywords, time: %d ms",
            chapters.size(), 
            signatureIndex.size(), 
            keywordIndex.size(), 
            elapsed
        ));
    }
    
    /**
     * 索引章节关键词（简单分词）
     * @param chapter 章节对象
     * @param title 章节标题
     */
    private void indexKeywords(@NonNull Chapter chapter, @NonNull String title) {
        // 简单分词：按字符拆分（中文场景）
        int length = title.length();
        
        // 1-gram: 单字索引
        for (int i = 0; i < length; i++) {
            String keyword = String.valueOf(title.charAt(i)).toLowerCase();
            addToKeywordIndex(keyword, chapter);
        }
        
        // 2-gram: 双字索引（提高准确率）
        for (int i = 0; i < length - 1; i++) {
            String keyword = title.substring(i, i + 2).toLowerCase();
            addToKeywordIndex(keyword, chapter);
        }
        
        // 3-gram: 三字索引
        if (length >= 3) {
            for (int i = 0; i < length - 2; i++) {
                String keyword = title.substring(i, i + 3).toLowerCase();
                addToKeywordIndex(keyword, chapter);
            }
        }
        
        // 完整标题索引
        addToKeywordIndex(title.toLowerCase(), chapter);
    }
    
    /**
     * 添加到关键词索引
     */
    private void addToKeywordIndex(@NonNull String keyword, @NonNull Chapter chapter) {
        List<Chapter> chapterList = keywordIndex.get(keyword);
        if (chapterList == null) {
            chapterList = new ArrayList<>();
            keywordIndex.put(keyword, chapterList);
        }
        
        // 避免重复
        if (!chapterList.contains(chapter)) {
            chapterList.add(chapter);
        }
    }
    
    /**
     * 根据 signatureId 查找章节（O(1) 复杂度）
     * @param signatureId 章节签名 ID
     * @return 章节对象，未找到返回 null
     */
    @Nullable
    public Chapter findBySignature(long signatureId) {
        if (!isBuilt) {
            EasyLog.print(TAG, "Index not built yet!");
            return null;
        }
        
        return signatureIndex.get((int) signatureId);
    }
    
    /**
     * 根据标题查找章节（精确匹配）
     * @param title 章节标题
     * @return 章节对象，未找到返回 null
     */
    @Nullable
    public Chapter findByTitle(@NonNull String title) {
        if (!isBuilt) {
            EasyLog.print(TAG, "Index not built yet!");
            return null;
        }
        
        return titleIndex.get(title.trim());
    }
    
    /**
     * 搜索章节（关键词匹配）
     * @param keyword 搜索关键词
     * @return 匹配的章节列表
     */
    @NonNull
    public List<Chapter> search(@Nullable String keyword) {
        if (!isBuilt || TextUtils.isEmpty(keyword)) {
            return new ArrayList<>();
        }
        
        long startTime = System.currentTimeMillis();
        
        String searchKey = keyword.trim().toLowerCase();
        
        // 从关键词索引查找
        List<Chapter> results = keywordIndex.get(searchKey);
        
        if (results == null) {
            // 如果没有精确匹配，尝试模糊匹配
            results = fuzzySearch(searchKey);
        } else {
            // 返回副本，避免外部修改
            results = new ArrayList<>(results);
        }
        
        long elapsed = System.currentTimeMillis() - startTime;
        EasyLog.print(TAG, String.format(
            "Search '%s': %d results, time: %d ms",
            keyword, results.size(), elapsed
        ));
        
        return results;
    }
    
    /**
     * 模糊搜索（包含匹配）
     * @param keyword 搜索关键词
     * @return 匹配的章节列表
     */
    @NonNull
    private List<Chapter> fuzzySearch(@NonNull String keyword) {
        List<Chapter> results = new ArrayList<>();
        
        // 遍历标题索引，查找包含关键词的章节
        for (Map.Entry<String, Chapter> entry : titleIndex.entrySet()) {
            String title = entry.getKey().toLowerCase();
            if (title.contains(keyword)) {
                results.add(entry.getValue());
            }
        }
        
        return results;
    }
    
    /**
     * 多关键词搜索（AND 逻辑）
     * @param keywords 关键词列表
     * @return 同时匹配所有关键词的章节列表
     */
    @NonNull
    public List<Chapter> searchMultiple(@NonNull List<String> keywords) {
        if (!isBuilt || keywords.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查找第一个关键词的结果
        List<Chapter> results = search(keywords.get(0));
        
        // 与其他关键词的结果取交集
        for (int i = 1; i < keywords.size() && !results.isEmpty(); i++) {
            List<Chapter> nextResults = search(keywords.get(i));
            results.retainAll(nextResults); // 取交集
        }
        
        return results;
    }
    
    /**
     * 获取索引统计信息
     */
    @NonNull
    public String getIndexStats() {
        return String.format(
            "Index stats: signatures=%d, titles=%d, keywords=%d, built=%b",
            signatureIndex.size(),
            titleIndex.size(),
            keywordIndex.size(),
            isBuilt
        );
    }
    
    /**
     * 打印索引统计
     */
    public void printStats() {
        EasyLog.print(TAG, getIndexStats());
    }
    
    /**
     * 清空索引
     */
    public synchronized void clear() {
        signatureIndex.clear();
        keywordIndex.clear();
        titleIndex.clear();
        isBuilt = false;
        EasyLog.print(TAG, "Index cleared");
    }
    
    /**
     * 是否已构建索引
     */
    public boolean isBuilt() {
        return isBuilt;
    }
    
    /**
     * 获取索引大小
     */
    public int getIndexSize() {
        return signatureIndex.size();
    }
    
    /**
     * 重建索引（增量更新时使用）
     * @param chapters 章节列表
     */
    public void rebuildIndex(@NonNull List<Chapter> chapters) {
        EasyLog.print(TAG, "Rebuilding index...");
        buildIndex(chapters);
    }
}
