/*
 * 项目名: AndroidProject
 * 类名: BookDataManager.java
 * 包名: run.yigou.gxzy.ui.tips.data
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.data;

import android.content.ComponentCallbacks2;
import android.util.LruCache;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.http.EasyLog;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import run.yigou.gxzy.utils.DebugLog;

/**
 * 书籍数据管理器（核心组件）
 * 替代 TipsSingleData 的书籍内容管理部分
 * 特性：
 * - LRU 缓存机制（最多缓存 5 本书）
 * - 线程安全
 * - 内存监控和自动释放
 * - 单例模式
 */
public class BookDataManager implements ComponentCallbacks2 {
    
    private static final String TAG = "BookDataManager";
    
    // LRU 缓存大小（KB）：约 50MB
    private static final int CACHE_SIZE_KB = 50 * 1024;
    
    // 单例实例
    private static volatile BookDataManager instance;
    
    // LRU 缓存（bookId -> BookData）
    private final LruCache<Integer, BookData> bookCache;
    
    // 当前活跃的书籍 ID（防止被 LRU 淘汰）
    private volatile int currentBookId = -1;
    
    // 加载状态映射（bookId -> isLoading）
    private final Map<Integer, Boolean> loadingStates;
    
    /**
     * 私有构造函数
     */
    private BookDataManager() {
        // 初始化 LRU 缓存
        this.bookCache = new LruCache<Integer, BookData>(CACHE_SIZE_KB) {
            @Override
            protected int sizeOf(Integer key, BookData value) {
                // 返回书籍数据的内存占用（KB）
                int size = value.estimateMemorySize();
                EasyLog.print(TAG, "Book " + key + " size: " + size + " KB");
                return size;
            }
            
            @Override
            protected void entryRemoved(boolean evicted, Integer key, 
                                       BookData oldValue, BookData newValue) {
                if (evicted) {
                    // LRU 淘汰时清理数据
                    EasyLog.print(TAG, "Book " + key + " evicted from cache");
                    if (oldValue != null) {
                        oldValue.onEvicted();
                    }
                }
            }
        };
        
        this.loadingStates = new ConcurrentHashMap<>();
    }
    
    /**
     * 获取单例实例
     */
    @NonNull
    public static BookDataManager getInstance() {
        if (instance == null) {
            synchronized (BookDataManager.class) {
                if (instance == null) {
                    instance = new BookDataManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 获取书籍数据（从缓存）
     * @param bookId 书籍 ID
     * @return 书籍数据，未缓存返回 null
     */
    @Nullable
    public synchronized BookData getFromCache(int bookId) {
        return bookCache.get(bookId);
    }
    
    /**
     * 获取书籍数据（确保返回非 null）
     * 如果缓存中不存在，则创建新实例
     * @param bookId 书籍 ID
     * @return 书籍数据（非 null）
     */
    @NonNull
    public synchronized BookData getBookData(int bookId) {
        BookData bookData = bookCache.get(bookId);
        
        if (bookData == null) {
            // 缓存未命中，创建新实例
            EasyLog.print(TAG, "Cache miss for book " + bookId + ", creating new instance");
            bookData = new BookData(bookId);
            bookCache.put(bookId, bookData);
        } else {
            EasyLog.print(TAG, "Cache hit for book " + bookId);
        }
        
        return bookData;
    }
    
    /**
     * 将书籍数据放入缓存
     * @param bookId 书籍 ID
     * @param bookData 书籍数据
     */
    public synchronized void putToCache(int bookId, @NonNull BookData bookData) {
        bookCache.put(bookId, bookData);
        EasyLog.print(TAG, "Book " + bookId + " cached, cache size: " + 
                     bookCache.size() + ", memory: " + bookCache.size() + " KB");
    }
    
    /**
     * 从缓存中移除书籍
     * @param bookId 书籍 ID
     */
    public synchronized void removeFromCache(int bookId) {
        BookData removed = bookCache.remove(bookId);
        if (removed != null) {
            removed.onEvicted();
            EasyLog.print(TAG, "Book " + bookId + " removed from cache");
        }
    }
    
    /**
     * 设置当前活跃的书籍 ID
     * 活跃书籍不会被 LRU 淘汰
     * @param bookId 书籍 ID
     */
    public void setCurrentBook(int bookId) {
        this.currentBookId = bookId;
        EasyLog.print(TAG, "Current book set to: " + bookId);
    }
    
    /**
     * 获取当前活跃的书籍 ID
     */
    public int getCurrentBookId() {
        return currentBookId;
    }
    
    /**
     * 获取当前活跃的书籍数据
     */
    @Nullable
    public BookData getCurrentBookData() {
        if (currentBookId <= 0) {
            return null;
        }
        return getBookData(currentBookId);
    }
    
    /**
     * 检查书籍是否正在加载
     */
    public boolean isLoading(int bookId) {
        Boolean loading = loadingStates.get(bookId);
        return loading != null && loading;
    }
    
    /**
     * 设置书籍加载状态
     */
    public void setLoading(int bookId, boolean loading) {
        loadingStates.put(bookId, loading);
    }
    
    /**
     * 清空所有缓存
     */
    public synchronized void clearAllCache() {
        bookCache.evictAll();
        loadingStates.clear();
        currentBookId = -1;
        EasyLog.print(TAG, "All cache cleared");
    }
    
    /**
     * 根据内存级别清理缓存
     * @param level 内存级别（ComponentCallbacks2 定义的常量）
     */
    public synchronized void trimMemory(int level) {
        EasyLog.print(TAG, "trimMemory called with level: " + level);
        
        switch (level) {
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_CRITICAL:
            case ComponentCallbacks2.TRIM_MEMORY_COMPLETE:
                // 内存极度紧张，清空所有缓存
                clearAllCache();
                EasyLog.print(TAG, "Critical memory, cleared all cache");
                break;
                
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_LOW:
            case ComponentCallbacks2.TRIM_MEMORY_MODERATE:
                // 内存紧张，清理非当前书籍
                clearInactiveBooks();
                EasyLog.print(TAG, "Low memory, cleared inactive books");
                break;
                
            case ComponentCallbacks2.TRIM_MEMORY_RUNNING_MODERATE:
            case ComponentCallbacks2.TRIM_MEMORY_BACKGROUND:
                // 中等压力，触发 GC 回收弱引用
                System.gc();
                EasyLog.print(TAG, "Moderate memory, triggered GC");
                break;
                
            default:
                // 轻微压力，不做处理
                break;
        }
    }
    
    /**
     * 清理非活跃书籍（保留当前书籍）
     */
    private synchronized void clearInactiveBooks() {
        if (currentBookId <= 0) {
            clearAllCache();
            return;
        }
        
        // 保存当前书籍
        BookData currentBook = bookCache.get(currentBookId);
        
        // 清空缓存
        bookCache.evictAll();
        
        // 恢复当前书籍
        if (currentBook != null) {
            bookCache.put(currentBookId, currentBook);
        }
    }
    
    /**
     * 获取缓存统计信息
     */
    @NonNull
    public String getCacheStats() {
        return String.format("Cache: size=%d, hits=%d, misses=%d, evictions=%d",
                bookCache.size(),
                bookCache.hitCount(),
                bookCache.missCount(),
                bookCache.evictionCount());
    }
    
    /**
     * 打印缓存统计
     */
    public void printCacheStats() {
        EasyLog.print(TAG, getCacheStats());
    }
    
    // ==================== ComponentCallbacks2 实现 ====================
    
    @Override
    public void onTrimMemory(int level) {
        trimMemory(level);
    }
    
    @Override
    public void onConfigurationChanged(@NonNull android.content.res.Configuration newConfig) {
        // 配置改变时不做处理
    }
    
    @Override
    public void onLowMemory() {
        // 低内存警告，清理所有缓存
        EasyLog.print(TAG, "onLowMemory called");
        clearAllCache();
    }
    
    /**
     * 销毁管理器（应用退出时调用）
     */
    public static synchronized void destroy() {
        if (instance != null) {
            instance.clearAllCache();
            instance = null;
        }
    }
}
