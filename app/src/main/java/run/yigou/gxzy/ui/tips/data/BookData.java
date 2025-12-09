/*
 * 项目名: AndroidProject
 * 类名: BookData.java
 * 包名: run.yigou.gxzy.ui.tips.data
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.data;

import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 书籍数据容器（优化版）
 * 替代 SingletonNetData，支持多书籍实例化
 * 特性：
 * - 线程安全
 * - 懒加载支持
 * - 内存占用可估算
 */
public class BookData {
    
    // 书籍 ID
    private final int bookId;
    
    // 章节索引映射（signatureId -> ChapterData）用于 O(1) 查找
    private final SparseArray<ChapterData> chapterMap;
    
    // 章节列表（顺序）
    private final List<ChapterData> chapterList;
    
    // 搜索结果列表（临时）
    private List<ChapterData> searchResultList;
    
    // 是否已完整加载所有章节
    private final AtomicBoolean isFullyLoaded;
    
    // 方剂数据
    private ChapterData fangData;
    
    /**
     * 构造函数
     * @param bookId 书籍 ID
     */
    public BookData(int bookId) {
        this.bookId = bookId;
        this.chapterMap = new SparseArray<>();
        this.chapterList = new CopyOnWriteArrayList<>();
        this.isFullyLoaded = new AtomicBoolean(false);
    }
    
    /**
     * 获取书籍 ID
     */
    public int getBookId() {
        return bookId;
    }
    
    /**
     * 添加章节
     * @param chapter 章节数据
     */
    public synchronized void addChapter(@NonNull ChapterData chapter) {
        chapterList.add(chapter);
        if (chapter.getSignatureId() > 0) {
            chapterMap.put((int) chapter.getSignatureId(), chapter);
        }
    }
    
    /**
     * 批量设置章节列表
     * @param chapters 章节列表
     */
    public synchronized void setChapters(@NonNull List<ChapterData> chapters) {
        chapterList.clear();
        chapterMap.clear();
        
        for (ChapterData chapter : chapters) {
            chapterList.add(chapter);
            if (chapter.getSignatureId() > 0) {
                chapterMap.put((int) chapter.getSignatureId(), chapter);
            }
        }
        
        isFullyLoaded.set(true);
    }
    
    /**
     * 根据索引获取章节
     * @param position 章节位置
     * @return 章节数据，未找到返回 null
     */
    @Nullable
    public ChapterData getChapter(int position) {
        if (position < 0 || position >= chapterList.size()) {
            return null;
        }
        return chapterList.get(position);
    }
    
    /**
     * 根据 signatureId 查找章节（O(1) 复杂度）
     * @param signatureId 章节签名 ID
     * @return 章节数据，未找到返回 null
     */
    @Nullable
    public ChapterData findChapterBySignature(long signatureId) {
        return chapterMap.get((int) signatureId);
    }
    
    /**
     * 获取所有章节列表
     * @return 章节列表副本（防止外部修改）
     */
    @NonNull
    public List<ChapterData> getAllChapters() {
        return new ArrayList<>(chapterList);
    }
    
    /**
     * 获取章节数量
     */
    public int getChapterCount() {
        return chapterList.size();
    }
    
    /**
     * 设置搜索结果
     * @param results 搜索结果列表
     */
    public synchronized void setSearchResults(@Nullable List<ChapterData> results) {
        if (searchResultList == null) {
            searchResultList = new ArrayList<>();
        }
        searchResultList.clear();
        
        if (results != null) {
            searchResultList.addAll(results);
        }
    }
    
    /**
     * 获取搜索结果
     */
    @NonNull
    public List<ChapterData> getSearchResults() {
        if (searchResultList == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(searchResultList);
    }
    
    /**
     * 清空搜索结果
     */
    public synchronized void clearSearchResults() {
        if (searchResultList != null) {
            searchResultList.clear();
        }
    }
    
    /**
     * 设置方剂数据
     */
    public void setFangData(@Nullable ChapterData fangData) {
        this.fangData = fangData;
    }
    
    /**
     * 获取方剂数据
     */
    @Nullable
    public ChapterData getFangData() {
        return fangData;
    }
    
    /**
     * 是否已完整加载
     */
    public boolean isFullyLoaded() {
        return isFullyLoaded.get();
    }
    
    /**
     * 标记为已完整加载
     */
    public void markAsFullyLoaded() {
        isFullyLoaded.set(true);
    }
    
    /**
     * 估算内存占用（KB）
     * 用于 LruCache 的 sizeOf 计算
     */
    public int estimateMemorySize() {
        int size = 0;
        
        // 基础对象开销
        size += 100; // 对象头 + 字段
        
        // 章节列表占用
        for (ChapterData chapter : chapterList) {
            size += chapter.estimateMemorySize();
        }
        
        // SparseArray 占用（近似）
        size += chapterMap.size() * 50;
        
        // 搜索结果占用
        if (searchResultList != null) {
            size += searchResultList.size() * 20;
        }
        
        return size / 1024; // 转换为 KB
    }
    
    /**
     * 清理缓存数据
     * 在内存紧张时调用
     */
    public synchronized void onEvicted() {
        // 清空搜索结果
        if (searchResultList != null) {
            searchResultList.clear();
            searchResultList = null;
        }
        
        // 清理章节缓存内容
        for (ChapterData chapter : chapterList) {
            chapter.clearCache();
        }
    }
    
    /**
     * 异步加载所有章节内容
     * @param callback 加载完成回调
     */
    public void loadAllChaptersAsync(@Nullable final LoadCallback callback) {
        // 在后台线程执行
        new Thread(() -> {
            try {
                int loadedCount = 0;
                for (ChapterData chapter : chapterList) {
                    if (!chapter.isContentLoaded()) {
                        // 触发内容加载
                        chapter.ensureContentLoaded();
                        loadedCount++;
                    }
                }
                
                isFullyLoaded.set(true);
                
                if (callback != null) {
                    final int count = loadedCount;
                    callback.onSuccess(count);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        }).start();
    }
    
    /**
     * 加载回调接口
     */
    public interface LoadCallback {
        /**
         * 加载成功
         * @param loadedCount 实际加载的章节数
         */
        void onSuccess(int loadedCount);
        
        /**
         * 加载失败
         * @param error 异常信息
         */
        void onError(Exception error);
    }
}
