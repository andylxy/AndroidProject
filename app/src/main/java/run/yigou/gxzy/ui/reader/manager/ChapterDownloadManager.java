/*
 * 项目名: AndroidProject
 * 类名: ChapterDownloadManager.java
 * 包名: run.yigou.gxzy.ui.reader.manager
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.reader.manager;

import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.HH2SectionData;

import androidx.lifecycle.LifecycleOwner;
import com.hjq.http.EasyHttp;
import run.yigou.gxzy.log.EasyLog;
import com.hjq.http.listener.HttpCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import run.yigou.gxzy.data.local.entity.Chapter;
import run.yigou.gxzy.data.local.helper.DataRepository;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.remote.api.ChapterContentApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.utils.DebugLog;

/**
 * 章节下载管理器
 * 
 * 核心功能:
 * 1. 优先级双线程池：高优先级（用户主动）+ 低优先级（预加载/批量）
 * 2. 并发控制：最多同时下载5个章节
 * 3. 状态管理：下载中 + 已下载双重去重
 * 4. 智能预加载：当前章节前后各5章 + 限速策略
 * 5. 生命周期感知：Fragment 销毁时自动取消下载
 */
public class ChapterDownloadManager {

    // 高优先级下载线程池（用户主动下载）
    private final ExecutorService highPriorityExecutor;
    
    // 低优先级下载线程池（预加载/批量，3个线程）
    private final ExecutorService lowPriorityExecutor;
    
    // 定时调度线程池（限速调度）
    private final ScheduledExecutorService scheduler;
    
    // 下载中章节 ID 集合（线程安全）
    private final Set<Long> downloadingChapters = new HashSet<>();
    
    // 已下载章节 ID 集合（内存缓存）
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
        // 高优先级：单线程，保证用户主动下载的顺序性
        this.highPriorityExecutor = Executors.newSingleThreadExecutor();
        
        // 低优先级：3个线程，用于预加载和批量下载
        this.lowPriorityExecutor = Executors.newFixedThreadPool(3);
        
        // 定时调度器：用于限速策略
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        EasyLog.print("ChapterDownloadManager", "ChapterDownloadManager 初始化完成");
    }

    /**
     * 初始化已下载章节缓存
     * 从数据库读取已下载的章节，构建内存缓存
     * 
     * @param chapters 章节列表
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
        EasyLog.print("ChapterDownloadManager", "已下载缓存初始化: " + downloadedChapters.size() + " 个章节");
    }

    /**
     * 检查章节是否已下载
     * 
     * @param chapter 章节对象
     * @return true-已下载，false-未下载
     */
    public boolean isChapterDownloaded(Chapter chapter) {
        return chapter != null && downloadedChapters.contains(chapter.getSignatureId());
    }

    /**
     * 检查章节是否正在下载中
     * 
     * @param chapter 章节对象
     * @return true-下载中，false-未下载或已完成
     */
    public boolean isChapterDownloading(Chapter chapter) {
        return chapter != null && downloadingChapters.contains(chapter.getSignatureId());
    }

    /**
     * 下载单个章节内容
     * 使用高优先级线程池，用户主动触发
     * 
     * 流程:
     * 1. 参数合法性校验
     * 2. 检查是否已下载
     * 3. 检查是否正在下载（防重复）
     * 4. 提交到高优先级线程池
     * 
     * @param lifecycleOwner Fragment/Activity 生命周期所有者
     * @param chapter 章节对象
     * @param callback 下载回调
     */
    public void downloadChapter(LifecycleOwner lifecycleOwner, Chapter chapter, DownloadCallback callback) {
        if (chapter == null) {
            EasyLog.print("ChapterDownloadManager", "错误：章节对象为空");
            return;
        }

        long signatureId = chapter.getSignatureId();

        // 检查是否已下载
        if (downloadedChapters.contains(signatureId)) {
            EasyLog.print("ChapterDownloadManager", "章节已下载，跳过: " + chapter.getChapterHeader());
            return;
        }

        // 检查是否正在下载，防止重复提交
        synchronized (downloadingChapters) {
            if (downloadingChapters.contains(signatureId)) {
                EasyLog.print("ChapterDownloadManager", "章节正在下载中，跳过: " + chapter.getChapterHeader());
                return;
            }
            downloadingChapters.add(signatureId);
        }

        EasyLog.print("ChapterDownloadManager", "开始下载章节: " + chapter.getChapterHeader());

        // 提交到高优先级线程池
        highPriorityExecutor.execute(() -> {
            executeDownload(lifecycleOwner, chapter, callback, true);
        });
    }

    /**
     * 批量下载所有章节内容
     * 使用低优先级线程池 + 限速策略
     * 适合应用启动时预加载
     * 
     * @param allChapters 所有章节列表
     * @param lifecycleOwner 生命周期所有者，传 null 则不感知生命周期
     */
    public void batchDownloadAllChapters(List<Chapter> allChapters, LifecycleOwner lifecycleOwner) {
        if (allChapters == null || allChapters.isEmpty()) {
            return;
        }

        List<Chapter> chaptersToDownload = new ArrayList<>();
        
        // 收集需要下载的章节
        synchronized (downloadingChapters) {
            for (Chapter chapter : allChapters) {
                if (chapter == null) continue;
                
                long signatureId = chapter.getSignatureId();
                
                // 排除已下载和正在下载的章节
                if (!downloadedChapters.contains(signatureId) && !downloadingChapters.contains(signatureId)) {
                    chaptersToDownload.add(chapter);
                    downloadingChapters.add(signatureId);
                }
            }
        }

        if (chaptersToDownload.isEmpty()) {
            EasyLog.print("ChapterDownloadManager", "所有章节已下载或正在下载，跳过批量下载");
            return;
        }

        // 限速策略：10分钟内下载完所有章节
        int totalChapters = chaptersToDownload.size();
        long totalDelayMillis = 10 * 60 * 1000; // 10分钟 = 600,000毫秒
        long intervalMillis = totalChapters > 0 ? totalDelayMillis / totalChapters : 1000;
        
        EasyLog.print("ChapterDownloadManager", "开始批量下载 " + totalChapters + " 个章节");
        EasyLog.print("ChapterDownloadManager", "限速策略：10分钟内下载完成，间隔 " + (intervalMillis / 1000.0) + " 秒/章");

        // 定时提交到低优先级线程池
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
     * 预加载当前章节前后各5章
     * 
     * 流程:
     * 1. 计算预加载范围（前后各5章）
     * 2. 排除已下载/下载中的章节
     * 3. 提交到低优先级线程池
     * 
     * @param allChapters 所有章节
     * @param currentIndex 当前阅读章节索引
     * @param lifecycleOwner 生命周期所有者，传 null 则不感知生命周期
     */
    public void preloadChapters(List<Chapter> allChapters, int currentIndex, LifecycleOwner lifecycleOwner) {
        if (allChapters == null || allChapters.isEmpty()) {
            return;
        }

        if (currentIndex < 0 || currentIndex >= allChapters.size()) {
            EasyLog.print("ChapterDownloadManager", "无效索引: " + currentIndex);
            return;
        }

        // 计算预加载范围（前后各5章）
        List<Integer> preloadIndices = calculatePreloadRange(currentIndex, allChapters.size());

        List<Chapter> chaptersToPreload = new ArrayList<>();
        for (int index : preloadIndices) {
            Chapter chapter = allChapters.get(index);
            long signatureId = chapter.getSignatureId();

            // 排除已下载和正在下载的章节
            synchronized (downloadingChapters) {
                if (!downloadedChapters.contains(signatureId) && !downloadingChapters.contains(signatureId)) {
                    chaptersToPreload.add(chapter);
                    downloadingChapters.add(signatureId);
                }
            }
        }

        if (chaptersToPreload.isEmpty()) {
            EasyLog.print("ChapterDownloadManager", "所有预加载章节已下载或正在下载");
            return;
        }

        EasyLog.print("ChapterDownloadManager", "开始预加载 " + chaptersToPreload.size() + " 个章节");

        // 提交到低优先级线程池异步预加载
        for (Chapter chapter : chaptersToPreload) {
            lowPriorityExecutor.execute(() -> {
                // 注意：这里 lifecycleOwner 可能为 null，但不影响下载(异步执行)
                executeDownload(lifecycleOwner, chapter, null, false);
            });
        }
    }

    /**
     * 计算预加载章节范围
     * 
     * 示例:
     * currentIndex = 10, totalCount = 100
     * 返回: [5,6,7,8,9,11,12,13,14,15] (前5章 + 后5章)
     * 
     * @param currentIndex 当前章节索引
     * @param totalCount 章节总数
     * @return 预加载章节索引列表
     */
    private List<Integer> calculatePreloadRange(int currentIndex, int totalCount) {
        List<Integer> indices = new ArrayList<>();

        // 添加前5章
        int startIndex = Math.max(0, currentIndex - 5);
        for (int i = startIndex; i < currentIndex; i++) {
            indices.add(i);
        }

        // 添加后5章
        int endIndex = Math.min(totalCount - 1, currentIndex + 5);
        for (int i = currentIndex + 1; i <= endIndex; i++) {
            indices.add(i);
        }

        return indices;
    }

    /**
     * 执行章节下载
     * 核心下载逻辑
     * 
     * 流程:
     * 1. 调用 HTTP API 获取章节内容
     * 2. 保存到数据库
     * 3. 更新状态
     * 4. 回调通知
     * 
     * @param lifecycleOwner Fragment/Activity 生命周期所有者
     * @param chapter 章节对象
     * @param callback 下载回调（批量下载时可能为 null）
     * @param isHighPriority 是否高优先级
     */
    private void executeDownload(LifecycleOwner lifecycleOwner, Chapter chapter, DownloadCallback callback, boolean isHighPriority) {
        try {
            // EasyHttp.get(lifecycleOwner) 感知生命周期，Fragment 销毁时自动取消请求
            // HttpCallback 即使为 null 也不会崩溃
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
                                DataRepository.saveBookChapterDetailList(chapter, data.getData());
                                chapter.setIsDownload(true);
                                DbService.getInstance().mChapterService.updateEntity(chapter);

                                // 更新下载状态
                                synchronized (downloadingChapters) {
                                    downloadingChapters.remove(chapter.getSignatureId());
                                    downloadedChapters.add(chapter.getSignatureId());
                                }

                                String priority = isHighPriority ? "高优先级" : "低优先级";
                                EasyLog.print("ChapterDownloadManager", priority + "下载成功: " + chapter.getChapterHeader());

                                // 回调通知
                                if (callback != null) {
                                    callback.onSuccess(chapter, sectionData);
                                }

                            } catch (Exception e) {
                                EasyLog.print("ChapterDownloadManager", "保存数据失败: " + e.getMessage());
                                handleDownloadFailure(chapter, e, callback);
                            }
                        } else {
                            Exception e = new Exception("章节内容为空");
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
     * 取消所有下载
     * 在 Fragment/Activity onDestroy 时调用
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
