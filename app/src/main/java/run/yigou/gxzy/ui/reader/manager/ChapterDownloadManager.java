/*
 * ???: AndroidProject
 * ??: ChapterDownloadManager.java
 * ??: run.yigou.gxzy.ui.reader.manager
 * ?? : AI Assistant
 * ?????? : 2025?12?09?
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
import run.yigou.gxzy.data.local.helper.ConvertEntity;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.remote.api.ChapterContentApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.utils.DebugLog;

/**
 * ???????
 * 
 * ?????
 * 1. ???????????????????????
 * 2. ???????????????????5???
 * 3. ????????? + ????????
 * 4. ????????????????+ ?????????
 * 5. ???????Fragment ???????????
 */
public class ChapterDownloadManager {

    // ???????????????????????
    private final ExecutorService highPriorityExecutor;
    
    // ????????3??????????
    private final ExecutorService lowPriorityExecutor;
    
    // ???????????????
    private final ScheduledExecutorService scheduler;
    
    // ?????? ID ??????????
    private final Set<Long> downloadingChapters = new HashSet<>();
    
    // ????? ID ?????????????
    private final Set<Long> downloadedChapters = new HashSet<>();

    /**
     * ??????
     */
    public interface DownloadCallback {
        /**
         * ??????
         * @param chapter ????
         * @param sectionData ??????
         */
        void onSuccess(Chapter chapter, HH2SectionData sectionData);
        
        /**
         * ??????
         * @param chapter ????
         * @param e ????
         */
        void onFailure(Chapter chapter, Exception e);
    }

    public ChapterDownloadManager() {
        // ??????????????????????
        this.highPriorityExecutor = Executors.newSingleThreadExecutor();
        
        // ?????3?????????
        this.lowPriorityExecutor = Executors.newFixedThreadPool(3);
        
        // ????????????????????
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        
        EasyLog.print("ChapterDownloadManager", "??????????");
    }

    /**
     * ??????????
     * ????????????????
     * 
     * @param chapters ??????
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
        EasyLog.print("ChapterDownloadManager", "?????: " + downloadedChapters.size() + " ??????");
    }

    /**
     * ?????????
     * 
     * @param chapter ????
     * @return true-???, false-???
     */
    public boolean isChapterDownloaded(Chapter chapter) {
        return chapter != null && downloadedChapters.contains(chapter.getSignatureId());
    }

    /**
     * ??????????
     * 
     * @param chapter ????
     * @return true-???, false-???
     */
    public boolean isChapterDownloading(Chapter chapter) {
        return chapter != null && downloadingChapters.contains(chapter.getSignatureId());
    }

    /**
     * ????????????
     * ?????????
     * 
     * ???
     * 1. ??????? ? ??
     * 2. ??????? ? ??
     * 3. ?????????????
     * 4. ?????????
     * 
     * @param lifecycleOwner Fragment/Activity ???????
     * @param chapter ??????
     * @param callback ????
     */
    public void downloadChapter(LifecycleOwner lifecycleOwner, Chapter chapter, DownloadCallback callback) {
        if (chapter == null) {
            EasyLog.print("ChapterDownloadManager", "?????????");
            return;
        }

        long signatureId = chapter.getSignatureId();

        // ???????
        if (downloadedChapters.contains(signatureId)) {
            EasyLog.print("ChapterDownloadManager", "????????: " + chapter.getChapterHeader());
            return;
        }

        // ??????????????
        synchronized (downloadingChapters) {
            if (downloadingChapters.contains(signatureId)) {
                EasyLog.print("ChapterDownloadManager", "?????????: " + chapter.getChapterHeader());
                return;
            }
            downloadingChapters.add(signatureId);
        }

        EasyLog.print("ChapterDownloadManager", "????????????: " + chapter.getChapterHeader());

        // ??????????
        highPriorityExecutor.execute(() -> {
            executeDownload(lifecycleOwner, chapter, callback, true);
        });
    }

    /**
     * ???????????????????
     * Fragment ??????
     * 
     * @param allChapters ??????
     * @param lifecycleOwner ??????????? null ?????????
     */
    public void batchDownloadAllChapters(List<Chapter> allChapters, LifecycleOwner lifecycleOwner) {
        if (allChapters == null || allChapters.isEmpty()) {
            return;
        }

        List<Chapter> chaptersToDownload = new ArrayList<>();
        
        // ???????
        synchronized (downloadingChapters) {
            for (Chapter chapter : allChapters) {
                if (chapter == null) continue;
                
                long signatureId = chapter.getSignatureId();
                
                // ???????????????
                if (!downloadedChapters.contains(signatureId) && !downloadingChapters.contains(signatureId)) {
                    chaptersToDownload.add(chapter);
                    downloadingChapters.add(signatureId);
                }
            }
        }

        if (chaptersToDownload.isEmpty()) {
            EasyLog.print("ChapterDownloadManager", "??????????????????");
            return;
        }

        // ????10???????????
        int totalChapters = chaptersToDownload.size();
        long totalDelayMillis = 10 * 60 * 1000; // 10?? = 600,000??
        long intervalMillis = totalChapters > 0 ? totalDelayMillis / totalChapters : 1000;
        
        EasyLog.print("ChapterDownloadManager", "???????? " + totalChapters + " ??????");
        EasyLog.print("ChapterDownloadManager", "????: 10???????? " + (intervalMillis / 1000.0) + " ?/??");

        // ????????
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
     * ???????
     * ?????????
     * 
     * ???
     * 1. ???????????5??
     * 2. ????????????
     * 3. ???????????
     * 
     * @param allChapters ??????
     * @param currentIndex ??????
     * @param lifecycleOwner ?????????,? null ?????????
     */
    public void preloadChapters(List<Chapter> allChapters, int currentIndex, LifecycleOwner lifecycleOwner) {
        if (allChapters == null || allChapters.isEmpty()) {
            return;
        }

        if (currentIndex < 0 || currentIndex >= allChapters.size()) {
            EasyLog.print("ChapterDownloadManager", "??????: " + currentIndex);
            return;
        }

        // ??????????? 5 ???
        List<Integer> preloadIndices = calculatePreloadRange(currentIndex, allChapters.size());

        List<Chapter> chaptersToPreload = new ArrayList<>();
        for (int index : preloadIndices) {
            Chapter chapter = allChapters.get(index);
            long signatureId = chapter.getSignatureId();

            // ????????????
            synchronized (downloadingChapters) {
                if (!downloadedChapters.contains(signatureId) && !downloadingChapters.contains(signatureId)) {
                    chaptersToPreload.add(chapter);
                    downloadingChapters.add(signatureId);
                }
            }
        }

        if (chaptersToPreload.isEmpty()) {
            EasyLog.print("ChapterDownloadManager", "?????????????????");
            return;
        }

        EasyLog.print("ChapterDownloadManager", "????? " + chaptersToPreload.size() + " ???");

        // ????????????
        for (Chapter chapter : chaptersToPreload) {
            lowPriorityExecutor.execute(() -> {
                // ???????? lifecycleOwner,??? null ??????(???????)
                executeDownload(lifecycleOwner, chapter, null, false);
            });
        }
    }

    /**
     * ???????
     * 
     * ???
     * currentIndex = 10, totalCount = 100
     * ??: [5,6,7,8,9,11,12,13,14,15] (?5? + ?5?)
     * 
     * @param currentIndex ??????
     * @param totalCount ????
     * @return ????????????
     */
    private List<Integer> calculatePreloadRange(int currentIndex, int totalCount) {
        List<Integer> indices = new ArrayList<>();

        // ?? 5 ???
        int startIndex = Math.max(0, currentIndex - 5);
        for (int i = startIndex; i < currentIndex; i++) {
            indices.add(i);
        }

        // ?? 5 ???
        int endIndex = Math.min(totalCount - 1, currentIndex + 5);
        for (int i = currentIndex + 1; i <= endIndex; i++) {
            indices.add(i);
        }

        return indices;
    }

    /**
     * ????????????
     * 
     * ???
     * 1. ?? HTTP API ??????
     * 2. ??????
     * 3. ????????
     * 4. ?????????
     * 
     * @param lifecycleOwner Fragment/Activity ???????
     * @param chapter ??????
     * @param callback ????????
     * @param isHighPriority ????????
     */
    private void executeDownload(LifecycleOwner lifecycleOwner, Chapter chapter, DownloadCallback callback, boolean isHighPriority) {
        try {
            // EasyHttp.get(lifecycleOwner) ??????,Fragment ?????????
            // HttpCallback ?????? null ??,??????
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
                                // ??????
                                ConvertEntity.saveBookChapterDetailList(chapter, data.getData());
                                chapter.setIsDownload(true);
                                DbService.getInstance().mChapterService.updateEntity(chapter);

                                // ??????????
                                synchronized (downloadingChapters) {
                                    downloadingChapters.remove(chapter.getSignatureId());
                                    downloadedChapters.add(chapter.getSignatureId());
                                }

                                String priority = isHighPriority ? "????" : "????";
                                EasyLog.print("ChapterDownloadManager", "?????" + priority + "?: " + chapter.getChapterHeader());

                                // ????
                                if (callback != null) {
                                    callback.onSuccess(chapter, sectionData);
                                }

                            } catch (Exception e) {
                                EasyLog.print("ChapterDownloadManager", "??????: " + e.getMessage());
                                handleDownloadFailure(chapter, e, callback);
                            }
                        } else {
                            Exception e = new Exception("??????");
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
     * ??????
     * 
     * @param chapter ????
     * @param e ????
     * @param callback ????
     */
    private void handleDownloadFailure(Chapter chapter, Exception e, DownloadCallback callback) {
        synchronized (downloadingChapters) {
            downloadingChapters.remove(chapter.getSignatureId());
        }

        EasyLog.print("ChapterDownloadManager", "????: " + chapter.getChapterHeader() + " - " + e.getMessage());

        if (callback != null) {
            callback.onFailure(chapter, e);
        }
    }

    /**
     * ????????
     * Fragment onDestroy ???
     */
    public void cancelAll() {
        EasyLog.print("ChapterDownloadManager", "????????");

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
     * ????????
     * 
     * @return ???????
     */
    public String getStatistics() {
        return String.format("???: %d, ???: %d", 
            downloadedChapters.size(), 
            downloadingChapters.size());
    }
}
