/*
 * 项目名: AndroidProject
 * 类名: AppDataManager.java
 * 包名: run.yigou.gxzy.manager
 * 作者 : AI Assistant
 * 当前修改时间 : 2026年01月22日
 * Copyright (c) 2026, Inc. All Rights Reserved
 */

package run.yigou.gxzy.manager.data;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.hjq.http.listener.OnHttpListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.data.local.entity.TabNav;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.entity.ZhongYaoAlia;
import run.yigou.gxzy.data.local.helper.DataRepository;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.data.model.YaoAlia;
import run.yigou.gxzy.data.remote.api.reader.BookInfoNav;
import run.yigou.gxzy.data.remote.api.reader.MingCiContentApi;
import run.yigou.gxzy.data.remote.api.reader.YaoAliaApi;
import run.yigou.gxzy.data.remote.api.reader.YaoContentApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.app.DataPreferences;
import run.yigou.gxzy.app.DataUpdateFrequency;
import run.yigou.gxzy.utils.ThreadUtil;

/**
 * 应用数据管理器
 * 
 * <p>职责：
 * <ul>
 *   <li>统一管理所有业务数据的加载（本地 + 网络）</li>
 *   <li>实现本地优先策略（缓存命中则不请求网络）</li>
 *   <li>保证启动时只加载一次（后续屏幕翻转不重复加载）</li>
 *   <li>支持缓存过期自动更新</li>
 *   <li>提供异步回调机制</li>
 * </ul>
 * 
 * <p>设计原则：
 * <ul>
 *   <li>不持有 LifecycleOwner 引用（按需传递，用完释放）</li>
 *   <li>使用 volatile 标记保证线程安全</li>
 *   <li>支持 Fragment 重建时快速恢复（从 GlobalDataHolder 读取）</li>
 * </ul>
 * 
 * <p>加载策略：
 * <ul>
 *   <li>首次启动：本地缓存 → 网络请求</li>
 *   <li>屏幕翻转：从 GlobalDataHolder 直接读取（不重新加载）</li>
 *   <li>进程重启：重新执行完整加载流程</li>
 *   <li>缓存过期：根据用户配置自动更新</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * // 首次加载
 * AppDataManager.getInstance().loadAllDataIfNeeded(this, new Callback&lt;Void&gt;() {
 *     &#64;Override
 *     public void onSuccess(Void data) {
 *         // 数据加载完成，更新 UI
 *     }
 *     
 *     &#64;Override
 *     public void onError(Exception e) {
 *         // 加载失败
 *     }
 * });
 * 
 * // 检查是否已加载
 * if (AppDataManager.getInstance().isAllDataLoaded()) {
 *     // 直接使用 GlobalDataHolder 中的数据
 * }
 * </pre>
 */
public class AppDataManager {
    
    private static final String TAG = "AppDataManager";
    
    // 单例
    private static volatile AppDataManager instance;
    
    // 数据加载状态标记（进程级，应用退出后失效）
    private volatile boolean isInitialized = false;
    private volatile boolean isLoading = false;  // 防并发加载
    
    // 加载时间戳（用于调试）
    private long loadCompleteTime = 0;
    
    /**
     * 私有构造方法
     */
    private AppDataManager() {
    }
    
    /**
     * 获取单例实例
     * 
     * @return AppDataManager 单例
     */
    public static AppDataManager getInstance() {
        if (instance == null) {
            synchronized (AppDataManager.class) {
                if (instance == null) {
                    instance = new AppDataManager();
                }
            }
        }
        return instance;
    }
    
    /**
     * 检查数据是否已加载完成
     * 
     * @return true=已加载，false=未加载
     */
    public boolean isAllDataLoaded() {
        boolean loaded = isInitialized && !GlobalDataHolder.getInstance().isEmpty();
        EasyLog.print(TAG, "🔍 [isAllDataLoaded] isInitialized=" + isInitialized + 
            ", GlobalDataHolder.isEmpty=" + GlobalDataHolder.getInstance().isEmpty() + 
            ", 结果=" + loaded);
        return loaded;
    }
    
    /**
     * 检查数据是否已过期，需要更新
     * 
     * <p>根据用户配置的更新频率判断缓存是否过期
     * 
     * @return true=需要更新，false=未过期
     */
    private boolean shouldUpdate() {
        DataUpdateFrequency frequency = DataPreferences.getUpdateFrequency();
        
        // 手动更新模式：不自动更新
        if (frequency == DataUpdateFrequency.MANUAL) {
            EasyLog.print(TAG, "📊 当前为手动更新模式，不自动更新");
            return false;
        }
        
        // 每次启动模式：总是更新
        if (frequency == DataUpdateFrequency.EVERY_START) {
            EasyLog.print(TAG, "📊 当前为每次启动模式，强制更新");
            return true;
        }
        
        // 定时更新模式：检查时间间隔
        long lastUpdateTime = DataPreferences.getLastUpdateTime();
        if (lastUpdateTime == 0) {
            EasyLog.print(TAG, "📊 从未更新过，需要加载");
            return true;
        }
        
        long now = System.currentTimeMillis();
        long elapsed = now - lastUpdateTime;
        long interval = frequency.getIntervalMillis();
        boolean expired = elapsed > interval;
        
        EasyLog.print(TAG, "📊 缓存检查：已耗时=" + elapsed + "ms, 间隔=" + interval + 
            "ms, 是否过期=" + expired);
        
        return expired;
    }
    
    /**
     * 加载所有数据（本地优先，网络兜底）
     * 
     * <p>重要：此方法在应用生命周期内只执行一次！
     * <p>后续屏幕翻转、Fragment 重建都不会重复加载。
     * <p>支持缓存过期自动更新。
     * 
     * @param lifecycleOwner LifecycleOwner（用于网络请求生命周期绑定，用完后自动释放）
     * @param callback 加载完成回调
     */
    public void loadAllDataIfNeeded(@NonNull LifecycleOwner lifecycleOwner, 
                                    @NonNull Callback<Void> callback) {
        // 1. 检查是否已加载且未过期
        if (isInitialized && !shouldUpdate()) {
            EasyLog.print(TAG, "✅ 数据未过期，跳过重复加载（已加载于 " + 
                loadCompleteTime + "）");
            callback.onSuccess(null);
            return;
        }
        
        // 2. 检查是否正在加载（防并发）
        if (isLoading) {
            EasyLog.print(TAG, "⚠️ 数据正在加载中，跳过重复请求");
            callback.onSuccess(null);  // 等待第一次加载完成
            return;
        }
        
        // 3. 开始加载
        isLoading = true;
        // 记下起点，供完成时打印真实耗时。此前完成日志直接打印 loadCompleteTime
        // 这个时间戳，标签却写「总耗时」，日志里会出现十几位的 epoch 值
        // （如 1791362992457ms），排查启动耗时时会被误导。
        final long loadStartTime = System.currentTimeMillis();
        EasyLog.print(TAG, "🚀 开始加载所有数据...");
        
        // 4. 执行加载流程
        executeLoadSequence(lifecycleOwner, new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                isInitialized = true;
                isLoading = false;
                loadCompleteTime = System.currentTimeMillis();
                
                // 记录更新时间
                DataPreferences.setLastUpdateTime(loadCompleteTime);
                
                EasyLog.print(TAG, "🎉 所有数据加载完成（总耗时 " + 
                    (loadCompleteTime - loadStartTime) + "ms）");
                callback.onSuccess(null);
            }
            
            @Override
            public void onError(Exception e) {
                isLoading = false;
                EasyLog.print(TAG, "❌ 数据加载失败: " + e.getMessage());
                callback.onError(e);
            }
        });
    }
    
    /**
     * 执行加载序列（依赖顺序）
     * 
     * <p>加载顺序：
     * <ol>
     *   <li>导航数据（独立）</li>
     *   <li>药物数据 + 名词数据（并行加载）</li>
     *   <li>方剂别名（依赖导航数据）</li>
     * </ol>
     */
    private void executeLoadSequence(LifecycleOwner lifecycleOwner, 
                                     Callback<Void> callback) {
        EasyLog.print(TAG, "📋 加载序列：导航 → [药物 + 名词] → 方剂别名");
        
        // 1. 加载导航数据（独立）
        loadNavigationData(lifecycleOwner, new Callback<List<TabNav>>() {
            @Override
            public void onSuccess(List<TabNav> data) {
                EasyLog.print(TAG, "✅ 步骤1：导航数据加载完成");
                
                // 2. 并行加载药物和名词数据（独立）
                loadYaoAndMingCiDataParallel(lifecycleOwner, new Callback<Void>() {
                    @Override
                    public void onSuccess(Void data) {
                        EasyLog.print(TAG, "✅ 步骤2：药物 + 名词数据加载完成");
                        
                        // 3. 加载方剂别名（依赖导航数据）
                        loadFangAliasData(() -> {
                            EasyLog.print(TAG, "✅ 步骤3：方剂别名加载完成");
                            callback.onSuccess(null);
                        });
                    }
                    
                    @Override
                    public void onError(Exception e) {
                        callback.onError(e);
                    }
                });
            }
            
            @Override
            public void onError(Exception e) {
                EasyLog.print(TAG, "❌ 导航数据加载失败");
                callback.onError(e);
            }
        });
    }
    
    /**
     * 加载导航数据（本地优先）
     * 
     * @param lifecycleOwner LifecycleOwner（用完后不持有）
     * @param callback 加载完成回调
     */
    private void loadNavigationData(LifecycleOwner lifecycleOwner, 
                                    Callback<List<TabNav>> callback) {
        // 本地读取走统一入口：原来这里在主线程直读导航表，严格模式下实测为
        // DiskReadViolation（全 navigations 一次性 loadAll）。失败按"本地无数据"处理，
        // 于是自动落到网络分支——与迁移前的行为一致。
        DbService.getInstance().readInBackground(
                () -> DataRepository.getNavigationData(),
                new Callback<List<TabNav>>() {
                    @Override
                    public void onSuccess(List<TabNav> localData) {
                        useLocalNavigation(localData, lifecycleOwner, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        useLocalNavigation(null, lifecycleOwner, callback);
                    }
                });
    }

    /** 有本地数据就用它并回调；没有就发网络请求（主线程）。 */
    private void useLocalNavigation(List<TabNav> localData, LifecycleOwner lifecycleOwner,
                                    Callback<List<TabNav>> callback) {
        if (localData != null && !localData.isEmpty()) {
            EasyLog.print(TAG, "📦 使用本地缓存：导航数据 " + localData.size() + " 条");
            syncNavigationToGlobalDataHolder(localData);
            callback.onSuccess(localData);
            return;
        }
        requestNavigationDataFromNetwork(lifecycleOwner, callback);
    }

    /** 本地无数据时的网络分支（主线程）。 */
    private void requestNavigationDataFromNetwork(LifecycleOwner lifecycleOwner,
                                                  Callback<List<TabNav>> callback) {
        // 请求网络
        EasyLog.print(TAG, "🌐 本地无缓存，请求网络：导航数据");
        EasyHttp.get(lifecycleOwner)
                .api(new BookInfoNav())
                .request(new HttpCallback<HttpData<List<TabNav>>>((OnHttpListener) lifecycleOwner) {
                    @Override
                    public void onSucceed(HttpData<List<TabNav>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            List<TabNav> networkData = data.getData();
                            
                            // 清空 GlobalDataHolder 旧数据（内存操作，主线程即时完成）
                            GlobalDataHolder.getInstance().reloadNavigationData();
                            
                            // 同步到 GlobalDataHolder（内存操作，主线程即时完成，保证 UI 立即可用）
                            syncNavigationToGlobalDataHolder(networkData);
                            
                            // 落库交给 DataRepository 的异步入口（内部走 DbService 的串行后台线程）：
                            // clearAndSaveNavTabs 会清空导航表 + 逐项查询/写入，还会为每本书触发
                            // 章节列表下载，同步执行会阻塞主线程（实测约 0.4s）。
                            // 边界：内存态已先行同步、UI 立即可用；步骤3 读的是 GlobalDataHolder
                            // 而不是库，因此不依赖此处落库完成。
                            DataRepository.clearAndSaveNavTabsAsync(networkData, lifecycleOwner);

                            EasyLog.print(TAG, "✅ 网络请求成功：导航数据 " + 
                                networkData.size() + " 条");
                            callback.onSuccess(networkData);
                        }
                    }
                    
                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "❌ 网络请求失败：导航数据 - " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 并行加载药物和名词数据
     * 
     * <p>使用计数器模式等待两个异步任务完成。
     */
    private void loadYaoAndMingCiDataParallel(LifecycleOwner lifecycleOwner, 
                                              Callback<Void> callback) {
        final int[] pendingTasks = {2};  // 等待 2 个任务
        final boolean[] hasError = {false};
        
        // 加载药物数据（含别名）
        loadYaoDataWithAlias(lifecycleOwner, new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                if (--pendingTasks[0] == 0 && !hasError[0]) {
                    callback.onSuccess(null);
                }
            }
            
            @Override
            public void onError(Exception e) {
                hasError[0] = true;
                callback.onError(e);
            }
        });
        
        // 加载名词数据
        loadMingCiData(lifecycleOwner, new Callback<Void>() {
            @Override
            public void onSuccess(Void data) {
                if (--pendingTasks[0] == 0 && !hasError[0]) {
                    callback.onSuccess(null);
                }
            }
            
            @Override
            public void onError(Exception e) {
                hasError[0] = true;
                callback.onError(e);
            }
        });
    }
    
    /**
     * 加载药物数据（含别名）
     */
    private void loadYaoDataWithAlias(LifecycleOwner lifecycleOwner, 
                                      Callback<Void> callback) {
        // 本地读取走统一入口（原主线程直读药材表）；失败按"本地无数据"处理
        DbService.getInstance().readInBackground(
                () -> DataRepository.getYaoData(),
                new Callback<List<Yao>>() {
                    @Override
                    public void onSuccess(List<Yao> localYaoData) {
                        useLocalYao(localYaoData, lifecycleOwner, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        useLocalYao(null, lifecycleOwner, callback);
                    }
                });
    }

    /** 有本地药材就用它并继续加载别名；没有就发网络请求（主线程）。 */
    private void useLocalYao(List<Yao> localYaoData, LifecycleOwner lifecycleOwner,
                             Callback<Void> callback) {
        if (localYaoData != null && !localYaoData.isEmpty()) {
            EasyLog.print(TAG, "📦 使用本地缓存：药物数据 " + localYaoData.size() + " 条");
            syncYaoToGlobalDataHolder(localYaoData);

            // 加载药物别名
            loadYaoAliasData(lifecycleOwner, callback);
            return;
        }
        requestYaoDataFromNetwork(lifecycleOwner, callback);
    }

    /** 本地无数据时的网络分支（主线程）。 */
    private void requestYaoDataFromNetwork(LifecycleOwner lifecycleOwner,
                                           Callback<Void> callback) {
        // 请求网络
        EasyLog.print(TAG, "🌐 本地无缓存，请求网络：药物数据");
        EasyHttp.get(lifecycleOwner)
                .api(new YaoContentApi())
                .request(new HttpCallback<HttpData<List<Yao>>>((OnHttpListener) lifecycleOwner) {
                    @Override
                    public void onSucceed(HttpData<List<Yao>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            List<Yao> networkData = data.getData();
                            
                            // 清空 GlobalDataHolder 旧数据（内存操作，主线程即时完成）
                            GlobalDataHolder.getInstance().reloadYaoData();
                            
                            // 同步到 GlobalDataHolder（内存操作，主线程即时完成，保证 UI 立即可用）
                            syncYaoToGlobalDataHolder(networkData);
                            
                            EasyLog.print(TAG, "✅ 网络请求成功：药物数据 " + 
                                networkData.size() + " 条");
                            
                            // 落库提交到 DbService 的串行后台线程：hjq 成功回调跑在主线程，
                            // saveYaoData 会清表+批量写库，同步执行会阻塞主线程造成 ANR
                            // （HomeActivity 曾因此 Input dispatching timed out）。
                            // 走串行执行器而不是通用缓存池，是为了让本 App 的所有落库共用同一条线程，
                            // 避免两条线程来源并发写同一个 SQLite 库互相抢锁。
                            // 边界：内存态已在上面同步完成、UI 立即可用；此处的落库是 fire-and-forget，
                            // 失败只记录日志并保留旧数据（事务已回滚），不重试、不回滚内存态。
                            DbService.getInstance().runInBackgroundSerial(
                                () -> DataRepository.saveYaoData(networkData));

                            // 加载药物别名（读的是别名表，与上面药材表的落库无依赖，无需等待）
                            loadYaoAliasData(lifecycleOwner, callback);
                        }
                    }
                    
                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "❌ 网络请求失败：药物数据 - " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 加载药物别名
     */
    private void loadYaoAliasData(LifecycleOwner lifecycleOwner, 
                                  Callback<Void> callback) {
        // 本地读取走统一入口（原主线程直读别名表）；失败按"本地无数据"处理
        DbService.getInstance().readInBackground(
                () -> DataRepository.getYaoAlia(),
                new Callback<List<ZhongYaoAlia>>() {
                    @Override
                    public void onSuccess(List<ZhongYaoAlia> localAliasData) {
                        useLocalYaoAlias(localAliasData, lifecycleOwner, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        useLocalYaoAlias(null, lifecycleOwner, callback);
                    }
                });
    }

    /** 有本地别名就用它并回调完成；没有就发网络请求（主线程）。 */
    private void useLocalYaoAlias(List<ZhongYaoAlia> localAliasData, LifecycleOwner lifecycleOwner,
                                  Callback<Void> callback) {
        if (localAliasData != null && !localAliasData.isEmpty()) {
            EasyLog.print(TAG, "📦 使用本地缓存：药物别名 " + localAliasData.size() + " 条");
            syncYaoAliasToGlobalDataHolder(localAliasData);
            callback.onSuccess(null);
            return;
        }
        requestYaoAliasDataFromNetwork(lifecycleOwner, callback);
    }

    /** 本地无数据时的网络分支（主线程）。 */
    private void requestYaoAliasDataFromNetwork(LifecycleOwner lifecycleOwner,
                                                Callback<Void> callback) {
        // 请求网络
        EasyLog.print(TAG, "🌐 本地无缓存，请求网络：药物别名");
        EasyHttp.get(lifecycleOwner)
                .api(new YaoAliaApi())
                .request(new HttpCallback<HttpData<List<YaoAlia>>>((OnHttpListener) lifecycleOwner) {
                    @Override
                    public void onSucceed(HttpData<List<YaoAlia>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            List<YaoAlia> networkData = data.getData();
                            
                            // 清空 GlobalDataHolder 旧数据（内存操作，主线程即时完成）
                            GlobalDataHolder.getInstance().reloadYaoAlias();
                            
                            // 同步到 GlobalDataHolder（内存操作，主线程即时完成）
                            syncYaoAliasToGlobalDataHolderFromNetwork(networkData);
                            
                            EasyLog.print(TAG, "✅ 网络请求成功：药物别名 " + 
                                networkData.size() + " 条");

                            // 落库提交到 DbService 串行后台线程：同 loadYaoDataWithAlias，
                            // 避免主线程同步写库造成 ANR，并与其它落库共用同一条线程。
                            // 边界：内存态已先行同步、UI 立即可用；落库失败只记录日志，不影响本次回调结果。
                            DbService.getInstance().runInBackgroundSerial(
                                () -> DataRepository.saveYaoAlia(networkData));

                            callback.onSuccess(null);
                        }
                    }
                    
                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "❌ 网络请求失败：药物别名 - " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 加载名词数据
     */
    private void loadMingCiData(LifecycleOwner lifecycleOwner, 
                                Callback<Void> callback) {
        // 本地读取走统一入口（原主线程直读名词表）；失败按"本地无数据"处理
        DbService.getInstance().readInBackground(
                () -> DataRepository.getMingCi(),
                new Callback<List<MingCiContent>>() {
                    @Override
                    public void onSuccess(List<MingCiContent> localData) {
                        useLocalMingCi(localData, lifecycleOwner, callback);
                    }

                    @Override
                    public void onError(Exception e) {
                        useLocalMingCi(null, lifecycleOwner, callback);
                    }
                });
    }

    /** 有本地名词就用它并回调完成；没有就发网络请求（主线程）。 */
    private void useLocalMingCi(List<MingCiContent> localData, LifecycleOwner lifecycleOwner,
                                Callback<Void> callback) {
        if (localData != null && !localData.isEmpty()) {
            EasyLog.print(TAG, "📦 使用本地缓存：名词数据 " + localData.size() + " 条");
            syncMingCiToGlobalDataHolder(localData);
            callback.onSuccess(null);
            return;
        }
        requestMingCiDataFromNetwork(lifecycleOwner, callback);
    }

    /** 本地无数据时的网络分支（主线程）。 */
    private void requestMingCiDataFromNetwork(LifecycleOwner lifecycleOwner,
                                              Callback<Void> callback) {
        // 请求网络
        EasyLog.print(TAG, "🌐 本地无缓存，请求网络：名词数据");
        EasyHttp.get(lifecycleOwner)
                .api(new MingCiContentApi())
                .request(new HttpCallback<HttpData<List<MingCiContent>>>((OnHttpListener) lifecycleOwner) {
                    @Override
                    public void onSucceed(HttpData<List<MingCiContent>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            List<MingCiContent> networkData = data.getData();
                            
                            // 清空 GlobalDataHolder 旧数据（内存操作，主线程即时完成）
                            GlobalDataHolder.getInstance().reloadMingCiData();
                            
                            // 同步到 GlobalDataHolder（内存操作，主线程即时完成）
                            syncMingCiToGlobalDataHolder(networkData);
                            
                            EasyLog.print(TAG, "✅ 网络请求成功：名词数据 " + 
                                networkData.size() + " 条");

                            // 落库提交到 DbService 串行后台线程：同 loadYaoDataWithAlias，
                            // 避免主线程同步写库造成 ANR，并与其它落库共用同一条线程。
                            // 边界：内存态已先行同步、UI 立即可用；落库失败只记录日志，不影响本次回调结果。
                            DbService.getInstance().runInBackgroundSerial(
                                () -> DataRepository.saveMingCiContent(networkData));

                            callback.onSuccess(null);
                        }
                    }
                    
                    @Override
                    public void onFail(Exception e) {
                        EasyLog.print(TAG, "❌ 网络请求失败：名词数据 - " + e.getMessage());
                        callback.onError(e);
                    }
                });
    }
    
    /**
     * 加载方剂别名（仅本地，从方剂数据提取）
     * 
     * <p>依赖关系：必须在导航数据加载完成后调用（读的是 GlobalDataHolder 里的书籍列表，
     * 不是数据库，所以不依赖导航数据的落库）。
     *
     * <p>执行线程：逐本书读库提取方剂名，实测 13 本书约 1.4s，同步跑在主线程会卡顿，
     * 因此整体提交到 {@link DbService#runInBackgroundSerial} 的串行后台线程（与落库同一条线程），
     * 完成后回主线程写 GlobalDataHolder 并回调。
     *
     * <p>为什么参数是 {@link Runnable} 而不是 {@code Callback<Void>}：本方法内部消化所有异常，
     * 不存在失败分支，用一个永不触发的 onError 只会制造死代码。
     *
     * @param onDone 加载完成回调；一定会被调用，且在主线程
     */
    private void loadFangAliasData(Runnable onDone) {
        EasyLog.print(TAG, "🔍 开始加载方剂别名（依赖导航数据）...");
        
        // 在主线程取一次书籍列表快照：getAllBookInfos() 本身返回副本，
        // 且不让后台线程直接读 GlobalDataHolder 的内存态，避免并发读写。
        final List<TabNavBody> bookInfos = GlobalDataHolder.getInstance().getAllBookInfos();

        // 逐本书读库属于数据库操作，统一提交到 DbService 的串行后台执行器：
        // ① 与落库共用同一条线程，不再有「缓存池 + 串行池」两种线程来源；
        // ② 该执行器是无界队列、提交即受理，不存在缓存池满载抛 RejectedExecutionException
        //    导致「回调永不触发、isLoading 永久卡住」的风险，故不需要额外的失败兜底。
        DbService.getInstance().runInBackgroundSerial(() -> {
            Map<String, String> fangAliasDict = new HashMap<>();
            int aliasCount = 0;

            try {
                for (TabNavBody bookInfo : bookInfos) {
                    String bookId = bookInfo.getBookNo();
                    ArrayList<Fang> fangList = DataRepository.getFangDetailList(bookId);

                    if (fangList != null && !fangList.isEmpty()) {
                        for (Fang fang : fangList) {
                            String fangName = fang.getName();
                            if (fangName != null && !fangName.trim().isEmpty()) {
                                fangAliasDict.put(fangName.trim(), fangName.trim());
                                aliasCount++;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                EasyLog.print(TAG, "❌ 方剂别名加载失败: " + e.getMessage());
                // 这个任务体跑在 DbService 的串行线程上：异常必须由自己记录并带堆栈，
                // 否则日志里只有一句话，排查时拿不到现场（设计文档 §4）。
                EasyLog.print(e);
            }
            
            final int total = aliasCount;
            ThreadUtil.runOnUiThread(() -> {
                try {
                    GlobalDataHolder.getInstance().putAllFangAlias(fangAliasDict);
                    EasyLog.print(TAG, "✅ 方剂别名加载完成：" + total + " 条（来自 " +
                        bookInfos.size() + " 本书）");
                } catch (Exception e) {
                    EasyLog.print(TAG, "❌ 方剂别名字典写入失败: " + e.getMessage());
                    EasyLog.print(e);
                } finally {
                    // 回调必须一定触发：否则 isLoading 永远为 true，之后每次 loadAllDataIfNeeded
                    // 都会在「正在加载」分支直接返回，App 会静默地再也不加载数据。
                    onDone.run();
                }
            });
        });
    }
    
    // ========== 同步方法（数据 → GlobalDataHolder） ==========
    
    /**
     * 同步导航数据到 GlobalDataHolder
     */
    private void syncNavigationToGlobalDataHolder(List<TabNav> navList) {
        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        int order = 0;
        
        for (TabNav nav : navList) {
            if (nav.getNavList() != null && !nav.getNavList().isEmpty()) {
                globalData.putNavTab(order, nav);
                for (TabNavBody item : nav.getNavList()) {
                    if (item.getBookNo() != null && !item.getBookNo().isEmpty()) {
                        globalData.putBookInfo(item.getBookNo(), item);
                    }
                }
                order++;
            }
        }
    }
    
    /**
     * 同步药物数据到 GlobalDataHolder
     */
    private void syncYaoToGlobalDataHolder(List<Yao> yaoList) {
        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        for (Yao yao : yaoList) {
            globalData.putYao(yao.getName(), yao);
            if (yao.getYaoList() != null) {
                for (String alias : yao.getYaoList()) {
                    globalData.putYao(alias, yao);
                }
            }
        }
    }
    
    /**
     * 同步药物别名到 GlobalDataHolder（本地数据库类型）
     */
    private void syncYaoAliasToGlobalDataHolder(List<ZhongYaoAlia> aliasList) {
        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        Map<String, String> yaoAliasDict = new HashMap<>();
        
        for (ZhongYaoAlia alias : aliasList) {
            yaoAliasDict.put(alias.getBieming(), alias.getName());
        }
        
        globalData.putAllYaoAlias(yaoAliasDict);
    }
    
    /**
     * 同步药物别名到 GlobalDataHolder（网络返回类型）
     */
    private void syncYaoAliasToGlobalDataHolderFromNetwork(List<YaoAlia> aliasList) {
        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        Map<String, String> yaoAliasDict = new HashMap<>();
        
        for (YaoAlia alias : aliasList) {
            yaoAliasDict.put(alias.getBieming(), alias.getName());
        }
        
        globalData.putAllYaoAlias(yaoAliasDict);
    }
    
    /**
     * 同步名词数据到 GlobalDataHolder
     */
    private void syncMingCiToGlobalDataHolder(List<MingCiContent> mingCiList) {
        GlobalDataHolder globalData = GlobalDataHolder.getInstance();
        for (MingCiContent mingCi : mingCiList) {
            globalData.putMingCiContent(mingCi.getName(), mingCi);
        }
    }
    
    // ========== 调试方法 ==========
    
    /**
     * 重置加载状态（用于手动刷新）
     */
    public void reset() {
        EasyLog.print(TAG, "🔄 重置加载状态");
        isInitialized = false;
        isLoading = false;
        loadCompleteTime = 0;
    }
    
    /**
     * 获取加载状态信息（调试用）
     * 
     * @return 状态信息字符串
     */
    public String getLoadStatus() {
        return "isInitialized=" + isInitialized + 
            ", isLoading=" + isLoading + 
            ", loadCompleteTime=" + loadCompleteTime;
    }
}
