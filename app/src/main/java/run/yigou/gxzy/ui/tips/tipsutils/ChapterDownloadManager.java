/*
 * 项目名: AndroidProject
 * 类名: ChapterDownloadManager.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils;

import androidx.lifecycle.LifecycleOwner;
import com.hjq.http.EasyHttp;
import com.hjq.http.EasyLog;
import com.hjq.http.listener.HttpCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import run.yigou.gxzy.greendao.entity.Chapter;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.http.api.ChapterContentApi;
import run.yigou.gxzy.http.model.HttpData;
import run.yigou.gxzy.utils.DebugLog;

/**
 * 章节下载管理器
 * 
 * 核心功能：
 * 1. 按需下载：用户点击章节时，检查并下载未下载章节
 * 2. 智能预加载：下载完成后，自动预加载前后5个章节
 * 3. 避免重复：通过缓存 + 队列防止重复下载
 * 4. 优先级管理：高优先级（用户点击）+ 低优先级（预加载）
 * 5. 生命周期管理：Fragment 销毁时自动取消所有任务
 */
public class ChapterDownloadManager {

    // 高优先级线程池（单线程，用户点击章节立即下载）
    private final ExecutorService highPriorityExecutor;
    
    // 低优先级线程池（3线程池，后台预加载）
    private final ExecutorService lowPriorityExecutor;
    
    // 定时调度器（用于后台慢速下载）
    private final ScheduledExecutorService scheduler;
    
    // 下载中的章节 ID 集合（避免重复下载）
    private final Set<Long> downloadingChapters = new HashSet<>();
    
    // 已下载章节 ID 缓存（避免频繁查询数据库）
    private final Set<Long> downloadedChapters = new HashSet<>();

    /**
     * 下载回调接口
     */
    public interface DownloadCallback {
        /**
         * 下载成功回调
         * @param chapter 章节对象
         * @param sectionData 章节内容数据
         */
        void onSuccess(Chapter chapter, HH2SectionData sectionData);
        
        /**
         * 下载失败回调
         * @param chapter 章节对象
         * @param e 异常信息
         */
        void onFailure(Chapter chapter, Exception e);
    }

    public ChapterDownloadManager() {
        // 高优先级：单线程，确保用户点击的章节优先下载
        this.highPriorityExecutor = Executors.newSingleThreadExecutor();
        
        // 低优先级：3线程池，并发预加载
        this.lowPriorityExecutor = Executors.newFixedThreadPool(3);
        
        // 定时调度器：单线程，用于后台慢速下载调度
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        DebugLog.print("ChapterDownloadManager", "下载管理器初始化完成");
    }

    /**
     * 初始化已下载章节缓存
     * 从数据库加载已下载状态到内存缓存
     * 
     * @param chapters 所有章节列表
     */
    public void initDownloadedCache(List<Chapter> chapters) {
        downloadedChapters.clear();
        if (chapters != null) {
            for (Chapter chapter : chapters) {
                if (chapter != null && chapter.getIsDownload()) {
                    downloadedChapters.add(chapter.getSignatureId());
                }
            }
        }
        DebugLog.print("ChapterDownloadManager", "初始化缓存: " + downloadedChapters.size() + " 个已下载章节");
    }

    /**
     * 检查章节是否已下载
     * 
     * @param chapter 章节对象
     * @return true-已下载, false-未下载
     */
    public boolean isChapterDownloaded(Chapter chapter) {
        return chapter != null && downloadedChapters.contains(chapter.getSignatureId());
    }

    /**
     * 检查章节是否正在下载
     * 
     * @param chapter 章节对象
     * @return true-下载中, false-未下载
     */
    public boolean isChapterDownloading(Chapter chapter) {
        return chapter != null && downloadingChapters.contains(chapter.getSignatureId());
    }

    /**
     * 下载单个章节（高优先级）
     * 用户点击章节时调用
     * 
     * 流程：
     * 1. 检查是否已下载 → 跳过
     * 2. 检查是否下载中 → 跳过
     * 3. 提交到高优先级队列立即下载
     * 4. 下载完成后回调通知
     * 
     * @param lifecycleOwner Fragment/Activity 生命周期管理器
     * @param chapter 要下载的章节
     * @param callback 下载回调
     */
    public void downloadChapter(LifecycleOwner lifecycleOwner, Chapter chapter, DownloadCallback callback) {
        if (chapter == null) {
            DebugLog.print("ChapterDownloadManager", "章节为空，跳过下载");
            return;
        }

        long signatureId = chapter.getSignatureId();

        // 检查是否已下载
        if (downloadedChapters.contains(signatureId)) {
            DebugLog.print("ChapterDownloadManager", "章节已下载，跳过: " + chapter.getChapterHeader());
            return;
        }

        // 检查是否正在下载（线程安全）
        synchronized (downloadingChapters) {
            if (downloadingChapters.contains(signatureId)) {
                DebugLog.print("ChapterDownloadManager", "章节正在下载，跳过: " + chapter.getChapterHeader());
                return;
            }
            downloadingChapters.add(signatureId);
        }

        DebugLog.print("ChapterDownloadManager", "开始下载章节（高优先级）: " + chapter.getChapterHeader());

        // 提交到高优先级线程池
        highPriorityExecutor.execute(() -> {
            executeDownload(lifecycleOwner, chapter, callback, true);
        });
    }

    /**
     * 批量后台下载所有未下载章节（低优先级）
     * Fragment 初始化时调用
     * 
     * @param allChapters 所有章节列表
     * @param lifecycleOwner 生命周期对象（可选，传 null 则不绑定生命周期）
     */
    public void batchDownloadAllChapters(List<Chapter> allChapters, LifecycleOwner lifecycleOwner) {
        if (allChapters == null || allChapters.isEmpty()) {
            return;
        }

        List<Chapter> chaptersToDownload = new ArrayList<>();
        
        // 过滤未下载章节
        synchronized (downloadingChapters) {
            for (Chapter chapter : allChapters) {
                if (chapter == null) continue;
                
                long signatureId = chapter.getSignatureId();
                
                // 只添加未下载且未在下载中的章节
                if (!downloadedChapters.contains(signatureId) && !downloadingChapters.contains(signatureId)) {
                    chaptersToDownload.add(chapter);
                    downloadingChapters.add(signatureId);
                }
            }
        }

        if (chaptersToDownload.isEmpty()) {
            DebugLog.print("ChapterDownloadManager", "所有章节已下载或下载中，无需后台下载");
            return;
        }

        // 【优化】10分钟内均匀下载所有章节
        int totalChapters = chaptersToDownload.size();
        long totalDelayMillis = 10 * 60 * 1000; // 10分钟 = 600,000毫秒
        long intervalMillis = totalChapters > 0 ? totalDelayMillis / totalChapters : 1000;
        
        DebugLog.print("ChapterDownloadManager", "开始后台批量下载 " + totalChapters + " 个未下载章节");
        DebugLog.print("ChapterDownloadManager", "下载策略: 10分钟内完成，间隔 " + (intervalMillis / 1000.0) + " 秒/章节");

        // 定时调度下载任务
        for (int i = 0; i < totalChapters; i++) {
            final Chapter chapter = chaptersToDownload.get(i);
            final long delay = i * intervalMillis;
            
            scheduler.schedule(() -> {
                lowPriorityExecutor.execute(() -> {
                    executeDownload(lifecycleOwner, chapter, null, false);
                });
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 智能预加载章节
     * 下载完成后自动调用
     * 
     * 流程：
     * 1. 计算预加载范围（前后各5个）
     * 2. 过滤已下载和下载中的章节
     * 3. 批量提交到低优先级队列
     * 
     * @param allChapters 所有章节列表
     * @param currentIndex 当前章节索引
     * @param lifecycleOwner 生命周期对象（可选,传 null 则不绑定生命周期）
     */
    public void preloadChapters(List<Chapter> allChapters, int currentIndex, LifecycleOwner lifecycleOwner) {
        if (allChapters == null || allChapters.isEmpty()) {
            return;
        }

        if (currentIndex < 0 || currentIndex >= allChapters.size()) {
            DebugLog.print("ChapterDownloadManager", "当前索引越界: " + currentIndex);
            return;
        }

        // 计算预加载范围：前后各 5 个章节
        List<Integer> preloadIndices = calculatePreloadRange(currentIndex, allChapters.size());

        List<Chapter> chaptersToPreload = new ArrayList<>();
        for (int index : preloadIndices) {
            Chapter chapter = allChapters.get(index);
            long signatureId = chapter.getSignatureId();

            // 过滤已下载和下载中的章节
            synchronized (downloadingChapters) {
                if (!downloadedChapters.contains(signatureId) && !downloadingChapters.contains(signatureId)) {
                    chaptersToPreload.add(chapter);
                    downloadingChapters.add(signatureId);
                }
            }
        }

        if (chaptersToPreload.isEmpty()) {
            DebugLog.print("ChapterDownloadManager", "预加载范围内所有章节已下载或下载中");
            return;
        }

        DebugLog.print("ChapterDownloadManager", "开始预加载 " + chaptersToPreload.size() + " 个章节");

        // 批量提交到低优先级线程池
        for (Chapter chapter : chaptersToPreload) {
            lowPriorityExecutor.execute(() -> {
                // 预加载使用传入的 lifecycleOwner,如果为 null 则在后台执行(不绑定生命周期)
                executeDownload(lifecycleOwner, chapter, null, false);
            });
        }
    }

    /**
     * 计算预加载范围
     * 
     * 示例：
     * currentIndex = 10, totalCount = 100
     * 返回: [5,6,7,8,9,11,12,13,14,15] (前5个 + 后5个)
     * 
     * @param currentIndex 当前章节索引
     * @param totalCount 总章节数
     * @return 需要预加载的章节索引列表
     */
    private List<Integer> calculatePreloadRange(int currentIndex, int totalCount) {
        List<Integer> indices = new ArrayList<>();

        // 向前 5 个章节
        int startIndex = Math.max(0, currentIndex - 5);
        for (int i = startIndex; i < currentIndex; i++) {
            indices.add(i);
        }

        // 向后 5 个章节
        int endIndex = Math.min(totalCount - 1, currentIndex + 5);
        for (int i = currentIndex + 1; i <= endIndex; i++) {
            indices.add(i);
        }

        return indices;
    }

    /**
     * 执行下载任务（内部方法）
     * 
     * 流程：
     * 1. 调用 HTTP API 下载章节内容
     * 2. 保存到数据库
     * 3. 更新下载状态缓存
     * 4. 回调通知（如果有）
     * 
     * @param lifecycleOwner Fragment/Activity 生命周期管理器
     * @param chapter 要下载的章节
     * @param callback 下载回调（可选）
     * @param isHighPriority 是否高优先级下载
     */
    private void executeDownload(LifecycleOwner lifecycleOwner, Chapter chapter, DownloadCallback callback, boolean isHighPriority) {
        try {
            // EasyHttp.get(lifecycleOwner) 绑定生命周期,Fragment 销毁时自动取消请求
            // HttpCallback 构造函数传递 null 即可,无需重复绑定
            EasyHttp.get(lifecycleOwner)
                .api(new ChapterContentApi()
                    .setContentId(chapter.getChapterSection())
                    .setSignatureId(chapter.getSignatureId())
                    .setBookId(chapter.getBookId()))
                .request(new HttpCallback<HttpData<List<HH2SectionData>>>(null) {
                    @Override
                    public void onSucceed(HttpData<List<HH2SectionData>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            HH2SectionData sectionData = data.getData().get(0);

                            try {
                                // 保存到数据库
                                ConvertEntity.saveBookChapterDetailList(chapter, data.getData());
                                chapter.setIsDownload(true);
                                DbService.getInstance().mChapterService.updateEntity(chapter);

                                // 更新缓存（线程安全）
                                synchronized (downloadingChapters) {
                                    downloadingChapters.remove(chapter.getSignatureId());
                                    downloadedChapters.add(chapter.getSignatureId());
                                }

                                String priority = isHighPriority ? "高优先级" : "低优先级";
                                EasyLog.print("ChapterDownloadManager", "下载成功（" + priority + "）: " + chapter.getChapterHeader());

                                // 回调通知
                                if (callback != null) {
                                    callback.onSuccess(chapter, sectionData);
                                }

                            } catch (Exception e) {
                                EasyLog.print("ChapterDownloadManager", "保存章节失败: " + e.getMessage());
                                handleDownloadFailure(chapter, e, callback);
                            }
                        } else {
                            Exception e = new Exception("章节数据为空");
                            handleDownloadFailure(chapter, e, callback);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        handleDownloadFailure(chapter, e, callback);
                    }
                });

        } catch (Exception e) {
            handleDownloadFailure(chapter, e, callback);
        }
    }

    /**
     * 处理下载失败
     * 
     * @param chapter 章节对象
     * @param e 异常信息
     * @param callback 下载回调
     */
    private void handleDownloadFailure(Chapter chapter, Exception e, DownloadCallback callback) {
        synchronized (downloadingChapters) {
            downloadingChapters.remove(chapter.getSignatureId());
        }

        EasyLog.print("ChapterDownloadManager", "下载失败: " + chapter.getChapterHeader() + " - " + e.getMessage());

        if (callback != null) {
            callback.onFailure(chapter, e);
        }
    }

    /**
     * 取消所有下载任务
     * Fragment onDestroy 时调用
     */
    public void cancelAll() {
        EasyLog.print("ChapterDownloadManager", "取消所有下载任务");

        if (highPriorityExecutor != null && !highPriorityExecutor.isShutdown()) {
            highPriorityExecutor.shutdownNow();
        }

        if (lowPriorityExecutor != null && !lowPriorityExecutor.isShutdown()) {
            lowPriorityExecutor.shutdownNow();
        }
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        synchronized (downloadingChapters) {
            downloadingChapters.clear();
        }
    }

    /**
     * 获取下载统计信息
     * 
     * @return 统计信息字符串
     */
    public String getStatistics() {
        return String.format("已下载: %d, 下载中: %d", 
            downloadedChapters.size(), 
            downloadingChapters.size());
    }
}
