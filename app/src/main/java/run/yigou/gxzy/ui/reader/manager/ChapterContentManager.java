/*
 * 项目名: AndroidProject
 * 类名: ChapterContentManager.java
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
import com.hjq.http.listener.OnHttpListener;

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
 * 章节内容管理器
 * 
 * 核心功能:
 * 1. 优先级双线程池：高优先级（用户主动）+ 低优先级（预加载/批量）
 * 2. 并发控制：最多同时获取5个章节
 * 3. 状态管理：获取中 + 已获取双重去重
 * 4. 智能预加载：当前章节前后各5章 + 限速策略
 * 5. 生命周期感知：Fragment 销毁时自动取消请求
 */
public class ChapterContentManager {

    // 单例实例
    private static volatile ChapterContentManager instance;

    // 高优先级获取线程池（用户主动触发）
    private final ExecutorService highPriorityExecutor;
    
    // 低优先级获取线程池（预加载/批量，3个线程）
    private final ExecutorService lowPriorityExecutor;
    
    // 定时调度线程池（限速调度）
    private final ScheduledExecutorService scheduler;
    
    // 获取中章节 ID 集合（线程安全）
    private final Set<Long> fetchingChapters = new HashSet<>();
    
    // 已获取章节 ID 集合（内存缓存）
    private final Set<Long> readyChapters = new HashSet<>();

    /**
     * 内容回调接口
     */
    public interface ContentCallback {
        /**
         * 获取成功回调
         * @param chapter 章节对象
         * @param sectionData 章节内容数据
         */
        void onSuccess(Chapter chapter, HH2SectionData sectionData);
        
        /**
         * 获取失败回调
         * @param chapter 章节对象
         * @param e 异常信息
         */
        void onFailure(Chapter chapter, Exception e);
    }

    /**
     * 获取单例实例
     * 
     * @return ChapterContentManager 实例
     */
    public static ChapterContentManager getInstance() {
        if (instance == null) {
            synchronized (ChapterContentManager.class) {
                if (instance == null) {
                    instance = new ChapterContentManager();
                }
            }
        }
        return instance;
    }

    public ChapterContentManager() {
        // 高优先级：单线程，保证用户主动获取的顺序性
        this.highPriorityExecutor = Executors.newSingleThreadExecutor();
        
        // 低优先级：3个线程，用于预加载和批量获取
        this.lowPriorityExecutor = Executors.newFixedThreadPool(3);
        
        // 定时调度器：用于限速策略
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        EasyLog.print("ChapterContentManager", "ChapterContentManager 初始化完成");
    }

    /**
     * 初始化已获取章节缓存
     * 从数据库读取已获取的章节，构建内存缓存
     * 
     * @param chapters 章节列表
     */
    public void initContentCache(List<Chapter> chapters) {
        readyChapters.clear();
        if (chapters != null) {
            for (Chapter chapter : chapters) {
                if (chapter != null && chapter.getIsDownload()) {
                    readyChapters.add(chapter.getSignatureId());
                }
            }
        }
        EasyLog.print("ChapterContentManager", "已获取缓存初始化: " + readyChapters.size() + " 个章节");
    }

    /**
     * 检查章节内容是否已就绪
     * 
     * @param chapter 章节对象
     * @return true-已获取，false-未获取
     */
    public boolean isContentReady(Chapter chapter) {
        return chapter != null && readyChapters.contains(chapter.getSignatureId());
    }

    /**
     * 检查章节是否正在获取中
     * 
     * @param chapter 章节对象
     * @return true-获取中，false-未获取或已完成
     */
    public boolean isContentFetching(Chapter chapter) {
        return chapter != null && fetchingChapters.contains(chapter.getSignatureId());
    }

    /**
     * 获取单个章节内容
     * 使用高优先级线程池，用户主动触发
     * 
     * 流程:
     * 1. 参数合法性校验
     * 2. 检查是否已获取
     * 3. 检查是否正在获取（防重复）
     * 4. 提交到高优先级线程池
     * 
     * @param lifecycleOwner Fragment/Activity 生命周期所有者
     * @param chapter 章节对象
     * @param callback 内容回调
     */
    public void fetchChapterContent(LifecycleOwner lifecycleOwner, Chapter chapter, ContentCallback callback) {
        if (chapter == null) {
            EasyLog.print("ChapterContentManager", "错误：章节对象为空");
            return;
        }

        long signatureId = chapter.getSignatureId();

        // 检查是否已获取
        if (readyChapters.contains(signatureId)) {
            EasyLog.print("ChapterContentManager", "章节已获取，跳过: " + chapter.getChapterHeader());
            return;
        }

        // 检查是否正在获取，防止重复提交
        synchronized (fetchingChapters) {
            if (fetchingChapters.contains(signatureId)) {
                EasyLog.print("ChapterContentManager", "章节正在获取中，跳过: " + chapter.getChapterHeader());
                return;
            }
            fetchingChapters.add(signatureId);
        }

        EasyLog.print("ChapterContentManager", "开始获取章节: " + chapter.getChapterHeader());

        // 提交到高优先级线程池
        highPriorityExecutor.execute(() -> {
            executeFetch(lifecycleOwner, chapter, callback, true);
        });
    }

    /**
     * 预加载所有章节内容
     * 使用低优先级线程池 + 限速策略
     * 适合应用启动时预加载
     * 
     * @param allChapters 所有章节列表
     * @param lifecycleOwner 生命周期所有者，传 null 则不感知生命周期
     */
    public void preloadAllChapters(List<Chapter> allChapters, LifecycleOwner lifecycleOwner) {
        if (allChapters == null || allChapters.isEmpty()) {
            return;
        }

        List<Chapter> chaptersToFetch = new ArrayList<>();
        
        // 收集需要获取的章节
        synchronized (fetchingChapters) {
            for (Chapter chapter : allChapters) {
                if (chapter == null) continue;
                
                long signatureId = chapter.getSignatureId();
                
                // 排除已获取和正在获取的章节
                if (!readyChapters.contains(signatureId) && !fetchingChapters.contains(signatureId)) {
                    chaptersToFetch.add(chapter);
                    fetchingChapters.add(signatureId);
                }
            }
        }

        if (chaptersToFetch.isEmpty()) {
            EasyLog.print("ChapterContentManager", "所有章节已获取或正在获取，跳过批量预加载");
            return;
        }

        // 限速策略：10分钟内获取完所有章节
        int totalChapters = chaptersToFetch.size();
        long totalDelayMillis = 10 * 60 * 1000; // 10分钟 = 600,000毫秒
        long intervalMillis = totalChapters > 0 ? totalDelayMillis / totalChapters : 1000;
        
        EasyLog.print("ChapterContentManager", "开始批量预加载 " + totalChapters + " 个章节");
        EasyLog.print("ChapterContentManager", "限速策略：10分钟内完成，间隔 " + (intervalMillis / 1000.0) + " 秒/章");

        // 定时提交到低优先级线程池
        for (int i = 0; i < totalChapters; i++) {
            final Chapter chapter = chaptersToFetch.get(i);
            final long delay = i * intervalMillis;
            
            scheduler.schedule(() -> {
                lowPriorityExecutor.execute(() -> {
                    executeFetch(lifecycleOwner, chapter, null, false);
                });
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * 智能预加载邻近章节
     * 预加载当前章节前后各5章
     * 
     * 流程:
     * 1. 计算预加载范围（前后各5章）
     * 2. 排除已获取/获取中的章节
     * 3. 提交到低优先级线程池
     * 
     * @param allChapters 所有章节
     * @param currentIndex 当前阅读章节索引
     * @param lifecycleOwner 生命周期所有者，传 null 则不感知生命周期
     */
    public void preloadNearbyChapters(List<Chapter> allChapters, int currentIndex, LifecycleOwner lifecycleOwner) {
        if (allChapters == null || allChapters.isEmpty()) {
            return;
        }

        if (currentIndex < 0 || currentIndex >= allChapters.size()) {
            EasyLog.print("ChapterContentManager", "无效索引: " + currentIndex);
            return;
        }

        // 计算预加载范围（前后各5章）
        List<Integer> preloadIndices = calculateNearbyRange(currentIndex, allChapters.size());

        List<Chapter> chaptersToPreload = new ArrayList<>();
        for (int index : preloadIndices) {
            Chapter chapter = allChapters.get(index);
            long signatureId = chapter.getSignatureId();

            // 排除已获取和正在获取的章节
            synchronized (fetchingChapters) {
                if (!readyChapters.contains(signatureId) && !fetchingChapters.contains(signatureId)) {
                    chaptersToPreload.add(chapter);
                    fetchingChapters.add(signatureId);
                }
            }
        }

        if (chaptersToPreload.isEmpty()) {
            EasyLog.print("ChapterContentManager", "所有预加载章节已获取或正在获取");
            return;
        }

        EasyLog.print("ChapterContentManager", "开始预加载 " + chaptersToPreload.size() + " 个章节");

        // 提交到低优先级线程池异步预加载
        for (Chapter chapter : chaptersToPreload) {
            lowPriorityExecutor.execute(() -> {
                // 注意：这里 lifecycleOwner 可能为 null，但不影响获取(异步执行)
                executeFetch(lifecycleOwner, chapter, null, false);
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
    private List<Integer> calculateNearbyRange(int currentIndex, int totalCount) {
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
     * 执行章节内容获取
     * 核心获取逻辑
     * 
     * 流程:
     * 1. 调用 HTTP API 获取章节内容
     * 2. 保存到数据库
     * 3. 更新状态
     * 4. 回调通知
     * 
     * @param lifecycleOwner Fragment/Activity 生命周期所有者
     * @param chapter 章节对象
     * @param callback 内容回调（批量获取时可能为 null）
     * @param isHighPriority 是否高优先级
     */
    private void executeFetch(LifecycleOwner lifecycleOwner, Chapter chapter, ContentCallback callback, boolean isHighPriority) {
        if (lifecycleOwner == null) {
            EasyLog.print("ChapterContentManager", "警告：lifecycleOwner 为 null，不感知生命周期");
        }

        try {
            // EasyHttp.get(lifecycleOwner) 感知生命周期，Fragment 销毁时自动取消请求
            // 使用 (OnHttpListener) lifecycleOwner 实现生命周期绑定
            EasyHttp.get(lifecycleOwner)
                .api(new ChapterContentApi()
                    .setContentId(chapter.getChapterSection())
                    .setSignatureId(chapter.getSignatureId())
                    .setBookId(chapter.getBookId()))
                .request(new HttpCallback<HttpData<List<HH2SectionData>>>((OnHttpListener) lifecycleOwner) {
                    @Override
                    public void onSucceed(HttpData<List<HH2SectionData>> data) {
                        if (data != null && !data.getData().isEmpty()) {
                            HH2SectionData sectionData = data.getData().get(0);

                            try {
                                // 保存到数据库
                                DataRepository.saveBookChapterDetailList(chapter, data.getData());
                                chapter.setIsDownload(true);
                                DbService.getInstance().mChapterService.updateEntity(chapter);

                                // 更新获取状态
                                synchronized (fetchingChapters) {
                                    fetchingChapters.remove(chapter.getSignatureId());
                                    readyChapters.add(chapter.getSignatureId());
                                }

                                String priority = isHighPriority ? "高优先级" : "低优先级";
                                EasyLog.print("ChapterContentManager", priority + "获取成功: " + chapter.getChapterHeader());

                                // 回调通知
                                if (callback != null) {
                                    callback.onSuccess(chapter, sectionData);
                                }

                            } catch (Exception e) {
                                EasyLog.print("ChapterContentManager", "保存数据失败: " + e.getMessage());
                                handleFetchFailure(chapter, e, callback);
                            }
                        } else {
                            Exception e = new Exception("章节内容为空");
                            handleFetchFailure(chapter, e, callback);
                        }
                    }

                    @Override
                    public void onFail(Exception e) {
                        handleFetchFailure(chapter, e, callback);
                    }
                });

        } catch (Exception e) {
            handleFetchFailure(chapter, e, callback);
        }
    }

    /**
     * 处理获取失败
     * 
     * @param chapter 章节对象
     * @param e 异常信息
     * @param callback 内容回调
     */
    private void handleFetchFailure(Chapter chapter, Exception e, ContentCallback callback) {
        synchronized (fetchingChapters) {
            fetchingChapters.remove(chapter.getSignatureId());
        }

        EasyLog.print("ChapterContentManager", "获取失败: " + chapter.getChapterHeader() + " - " + e.getMessage());

        if (callback != null) {
            callback.onFailure(chapter, e);
        }
    }

    /**
     * 取消所有获取
     * 在 Fragment/Activity onDestroy 时调用
     */
    public void cancelAll() {
        EasyLog.print("ChapterContentManager", "取消所有获取任务");

        if (highPriorityExecutor != null && !highPriorityExecutor.isShutdown()) {
            highPriorityExecutor.shutdownNow();
        }

        if (lowPriorityExecutor != null && !lowPriorityExecutor.isShutdown()) {
            lowPriorityExecutor.shutdownNow();
        }
        
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        synchronized (fetchingChapters) {
            fetchingChapters.clear();
        }
    }

    /**
     * 获取统计信息
     * 
     * @return 统计信息字符串
     */
    public String getStatistics() {
        return String.format("已获取: %d, 获取中: %d", 
            readyChapters.size(), 
            fetchingChapters.size());
    }
}
