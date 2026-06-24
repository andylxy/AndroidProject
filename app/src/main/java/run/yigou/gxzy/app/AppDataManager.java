/*
 * 项目名: AndroidProject
 * 类名: AppDataManager.java
 * 包名: run.yigou.gxzy.app
 * 作者 : AI Assistant
 * 当前修改时间 : 2026年01月22日
 * Copyright (c) 2026, Inc. All Rights Reserved
 */

package run.yigou.gxzy.app;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;

import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.base.GlobalDataHolder;
import run.yigou.gxzy.data.local.entity.TabNav;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.entity.ZhongYaoAlia;
import run.yigou.gxzy.data.local.helper.DataRepository;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.data.model.YaoAlia;
import run.yigou.gxzy.data.remote.api.BookInfoNav;
import run.yigou.gxzy.data.remote.api.MingCiContentApi;
import run.yigou.gxzy.data.remote.api.YaoAliaApi;
import run.yigou.gxzy.data.remote.api.YaoContentApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.log.EasyLog;

/**
 * 应用数据管理器
 * 
 * <p>职责：
 * <ul>
 *   <li>统一管理所有业务数据的加载（本地 + 网络）</li>
 *   <li>实现本地优先策略（缓存命中则不请求网络）</li>
 *   <li>保证启动时只加载一次（后续屏幕翻转不重复加载）</li>
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
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * // 首次加载
 * AppDataManager.getInstance().loadAllDataIfNeeded(this, new DataLoadCallback() {
 *     @Override
 *     public void onComplete() {
 *         // 数据加载完成，更新 UI
 *     }
 *     
 *     @Override
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
     * 加载所有数据（本地优先，网络兜底）
     * 
     * <p>重要：此方法在应用生命周期内只执行一次！
     * <p>后续屏幕翻转、Fragment 重建都不会重复加载。
     * 
     * @param lifecycleOwner LifecycleOwner（用于网络请求生命周期绑定，用完后自动释放）
     * @param callback 加载完成回调
     */
    public void loadAllDataIfNeeded(@NonNull LifecycleOwner lifecycleOwner, 
                                    @NonNull DataLoadCallback callback) {
        // 1. 检查是否已加载
        if (isInitialized) {
            EasyLog.print(TAG, "✅ 数据已加载，跳过重复加载（已耗时 " + 
                (System.currentTimeMillis() - loadCompleteTime) + "ms）");
            callback.onComplete();
            return;
        }
        
        // 2. 检查是否正在加载（防并发）
        if (isLoading) {
            EasyLog.print(TAG, "⚠️ 数据正在加载中，跳过重复请求");
            callback.onComplete();  // 等待第一次加载完成
            return;
        }
        
        // 3. 开始加载
        isLoading = true;
        EasyLog.print(TAG, "🚀 开始加载所有数据...");
        
        // 4. 执行加载流程
        executeLoadSequence(lifecycleOwner, new DataLoadCallback() {
            @Override
            public void onComplete() {
                isInitialized = true;
                isLoading = false;
                loadCompleteTime = System.currentTimeMillis();
                
                EasyLog.print(TAG, "🎉 所有数据加载完成（总耗时 " + 
                    loadCompleteTime + "ms）");
                callback.onComplete();
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
                                     DataLoadCallback callback) {
        EasyLog.print(TAG, "📋 加载序列：导航 → [药物 + 名词] → 方剂别名");
        
        // 1. 加载导航数据（独立）
        loadNavigationData(lifecycleOwner, new DataCallback<List<TabNav>>() {
            @Override
            public void onSuccess(List<TabNav> data) {
                EasyLog.print(TAG, "✅ 步骤1：导航数据加载完成");
                
                // 2. 并行加载药物和名词数据（独立）
                loadYaoAndMingCiDataParallel(lifecycleOwner, new SimpleCallback() {
                    @Override
                    public void onSuccess() {
                        EasyLog.print(TAG, "✅ 步骤2：药物 + 名词数据加载完成");
                        
                        // 3. 加载方剂别名（依赖导航数据）
                        loadFangAliasData();
                        EasyLog.print(TAG, "✅ 步骤3：方剂别名加载完成");
                        
                        callback.onComplete();
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
                                    DataCallback<List<TabNav>> callback) {
        // 1. 尝试本地加载
        List<TabNav> localData = DataRepository.getNavigationData();
        if (localData != null && !localData.isEmpty()) {
            EasyLog.print(TAG, "📦 使用本地缓存：导航数据 " + localData.size() + " 条");
            syncNavigationToGlobalDataHolder(localData);
            callback.onSuccess(localData);
            return;
        }
        
        // 2. 本地无数据，请求网络
        EasyLog.print(TAG, "🌐 本地无缓存，请求网络：导航数据");
        EasyHttp.get(lifecycleOwner)
                .api(new BookInfoNav())
                .request(new HttpCallback<HttpData<List<TabNav>>>(null) {
                    @Override
                    public void onSucceed(HttpData<List<TabNav>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            List<TabNav> networkData = data.getData();
                            
                            // 保存到本地（下次启动可用）
                            DataRepository.saveTabNvaInDb(networkData, lifecycleOwner);
                            
                            // 同步到 GlobalDataHolder
                            syncNavigationToGlobalDataHolder(networkData);
                            
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
                                              SimpleCallback callback) {
        final int[] pendingTasks = {2};  // 等待 2 个任务
        final boolean[] hasError = {false};
        
        // 加载药物数据（含别名）
        loadYaoDataWithAlias(lifecycleOwner, new SimpleCallback() {
            @Override
            public void onSuccess() {
                if (--pendingTasks[0] == 0 && !hasError[0]) {
                    callback.onSuccess();
                }
            }
            
            @Override
            public void onError(Exception e) {
                hasError[0] = true;
                callback.onError(e);
            }
        });
        
        // 加载名词数据
        loadMingCiData(lifecycleOwner, new SimpleCallback() {
            @Override
            public void onSuccess() {
                if (--pendingTasks[0] == 0 && !hasError[0]) {
                    callback.onSuccess();
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
                                      SimpleCallback callback) {
        // 1. 检查本地缓存
        List<Yao> localYaoData = DataRepository.getYaoData();
        if (localYaoData != null && !localYaoData.isEmpty()) {
            EasyLog.print(TAG, "📦 使用本地缓存：药物数据 " + localYaoData.size() + " 条");
            syncYaoToGlobalDataHolder(localYaoData);
            
            // 加载药物别名
            loadYaoAliasData(lifecycleOwner, callback);
            return;
        }
        
        // 2. 请求网络
        EasyLog.print(TAG, "🌐 本地无缓存，请求网络：药物数据");
        EasyHttp.get(lifecycleOwner)
                .api(new YaoContentApi())
                .request(new HttpCallback<HttpData<List<Yao>>>(null) {
                    @Override
                    public void onSucceed(HttpData<List<Yao>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            List<Yao> networkData = data.getData();
                            
                            // 保存到本地
                            DataRepository.saveYaoData(networkData);
                            
                            // 同步到 GlobalDataHolder
                            syncYaoToGlobalDataHolder(networkData);
                            
                            EasyLog.print(TAG, "✅ 网络请求成功：药物数据 " + 
                                networkData.size() + " 条");
                            
                            // 加载药物别名
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
                                  SimpleCallback callback) {
        // 1. 检查本地缓存
        List<ZhongYaoAlia> localAliasData = DataRepository.getYaoAlia();
        if (localAliasData != null && !localAliasData.isEmpty()) {
            EasyLog.print(TAG, "📦 使用本地缓存：药物别名 " + localAliasData.size() + " 条");
            syncYaoAliasToGlobalDataHolder(localAliasData);
            callback.onSuccess();
            return;
        }
        
        // 2. 请求网络
        EasyLog.print(TAG, "🌐 本地无缓存，请求网络：药物别名");
        EasyHttp.get(lifecycleOwner)
                .api(new YaoAliaApi())
                .request(new HttpCallback<HttpData<List<YaoAlia>>>(null) {
                    @Override
                    public void onSucceed(HttpData<List<YaoAlia>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            List<YaoAlia> networkData = data.getData();
                            
                            // 保存到本地
                            DataRepository.saveYaoAlia(networkData);
                            
                            // 同步到 GlobalDataHolder
                            syncYaoAliasToGlobalDataHolderFromNetwork(networkData);
                            
                            EasyLog.print(TAG, "✅ 网络请求成功：药物别名 " + 
                                networkData.size() + " 条");
                            callback.onSuccess();
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
                                SimpleCallback callback) {
        // 1. 检查本地缓存
        List<MingCiContent> localData = DataRepository.getMingCi();
        if (localData != null && !localData.isEmpty()) {
            EasyLog.print(TAG, "📦 使用本地缓存：名词数据 " + localData.size() + " 条");
            syncMingCiToGlobalDataHolder(localData);
            callback.onSuccess();
            return;
        }
        
        // 2. 请求网络
        EasyLog.print(TAG, "🌐 本地无缓存，请求网络：名词数据");
        EasyHttp.get(lifecycleOwner)
                .api(new MingCiContentApi())
                .request(new HttpCallback<HttpData<List<MingCiContent>>>(null) {
                    @Override
                    public void onSucceed(HttpData<List<MingCiContent>> data) {
                        if (data != null && data.getData() != null && !data.getData().isEmpty()) {
                            List<MingCiContent> networkData = data.getData();
                            
                            // 保存到本地
                            DataRepository.saveMingCiContent(networkData);
                            
                            // 同步到 GlobalDataHolder
                            syncMingCiToGlobalDataHolder(networkData);
                            
                            EasyLog.print(TAG, "✅ 网络请求成功：名词数据 " + 
                                networkData.size() + " 条");
                            callback.onSuccess();
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
     * <p>依赖关系：必须在导航数据加载完成后调用。
     */
    private void loadFangAliasData() {
        EasyLog.print(TAG, "🔍 开始加载方剂别名（依赖导航数据）...");
        
        try {
            GlobalDataHolder globalData = GlobalDataHolder.getInstance();
            List<TabNavBody> bookInfos = globalData.getAllBookInfos();
            Map<String, String> fangAliasDict = new HashMap<>();
            
            int aliasCount = 0;
            int bookIndex = 0;
            for (TabNavBody bookInfo : bookInfos) {
                bookIndex++;
                int bookId = bookInfo.getBookNo();
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
            
            globalData.putAllFangAlias(fangAliasDict);
            EasyLog.print(TAG, "✅ 方剂别名加载完成：" + aliasCount + " 条（来自 " + 
                bookInfos.size() + " 本书）");
            
        } catch (Exception e) {
            EasyLog.print(TAG, "❌ 方剂别名加载失败: " + e.getMessage());
            e.printStackTrace();
        }
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
                    if (item.getBookNo() > 0) {
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
    
    // ========== 回调接口 ==========
    
    /**
     * 数据加载完成回调
     */
    public interface DataLoadCallback {
        void onComplete();
        void onError(Exception e);
    }
    
    /**
     * 带数据的回调
     */
    public interface DataCallback<T> {
        void onSuccess(T data);
        void onError(Exception e);
    }
    
    /**
     * 简单回调（无数据）
     */
    public interface SimpleCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
