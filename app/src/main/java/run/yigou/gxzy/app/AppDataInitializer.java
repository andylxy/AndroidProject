/*
 * 项目名: AndroidProject
 * 类名: AppDataInitializer.java
 * 包名: run.yigou.gxzy.app
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月13日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.app;

import android.content.Context;

import run.yigou.gxzy.log.EasyLog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.data.local.entity.TabNav;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.local.entity.ZhongYaoAlia;
import run.yigou.gxzy.data.local.helper.DataRepository;
import run.yigou.gxzy.data.local.helper.DbService;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.data.model.Yao;
import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.base.GlobalDataHolder;

/**
 * 应用数据初始化器
 * 特性：
 * - 程序启动时只执行一次
 * - 优先使用本地数据
 * - 线程安全
 */
public class AppDataInitializer {
    
    private static final String TAG = "AppDataInitializer";
    
    // 初始化状态标志
    private static volatile boolean isInitialized = false;
    
    // 防止外部实例化
    private AppDataInitializer() {}
    
    /**
     * 检查并执行初始化（如果需要）
     * 此方法应在 Application.onCreate() 中异步调用
     * 
     * @param context 应用上下文
     */
    public static void initializeIfNeeded(Context context) {
        EasyLog.print(TAG, "🔍 [initializeIfNeeded] isInitialized=" + isInitialized);
        
        if (isInitialized) {
            EasyLog.print(TAG, "Already initialized, skipping");
            return;
        }
        
        synchronized (AppDataInitializer.class) {
            if (isInitialized) {
                return;
            }
            
            try {
                EasyLog.print(TAG, "Starting data initialization...");
                long startTime = System.currentTimeMillis();
                
                // 检查本地是否已有数据
                boolean hasData = hasLocalData();
                EasyLog.print(TAG, "🔍 [initializeIfNeeded] hasLocalData()=" + hasData);
                
                if (hasData) {
                    EasyLog.print(TAG, "Local data found, loading from database");
                    loadFromLocalDatabase();
                } else {
                    EasyLog.print(TAG, "No local data, will load from network later");
                }
                
                isInitialized = true;
                long duration = System.currentTimeMillis() - startTime;
                EasyLog.print(TAG, "Initialization completed in " + duration + "ms");
                
            } catch (Exception e) {
                EasyLog.print(TAG, "Initialization error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * 检查本地是否有数据
     * 使用 TabNav 表非空作为判断标准
     */
    public static boolean hasLocalData() {
        try {
            DbService dbService = DbService.getInstance();
            if (dbService == null || dbService.mTabNavService == null) {
                return false;
            }
            
            ArrayList<TabNav> navList = dbService.mTabNavService.findAll();
            return navList != null && !navList.isEmpty();
        } catch (Exception e) {
            EasyLog.print(TAG, "Error checking local data: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 从本地数据库加载数据到 GlobalDataHolder
     * 
     * <p>加载顺序遵循依赖关系：
     * <ol>
     *   <li>导航数据（其他数据可能依赖书籍信息）</li>
     *   <li>药物数据</li>
     *   <li>名词数据</li>
     *   <li>药物别名</li>
     *   <li>方剂别名（依赖导航数据中的书籍信息）</li>
     * </ol>
     */
    private static void loadFromLocalDatabase() {
        try {
            // 数据源校验
            DbService dbService = DbService.getInstance();
            if (dbService == null) {
                EasyLog.print(TAG, "❌ DbService 未初始化，中止加载");
                return;
            }
            
            GlobalDataHolder globalData = GlobalDataHolder.getInstance();
            
            // 预检查数据可用性
            long startTime = System.currentTimeMillis();
            
            EasyLog.print(TAG, "📊 开始加载本地数据...");
            
            // 1. 加载导航数据（必须先加载，方剂别名依赖此数据）
            loadNavigationData(dbService, globalData);
            
            // 2. 加载药物数据
            loadYaoData(globalData);
            
            // 3. 加载名词数据
            loadMingCiData(globalData);
            
            // 4. 加载药物别名
            loadYaoAliasData(globalData);
            
            // 5. 加载方剂别名（依赖导航数据中的书籍信息）
            loadFangAliasData(globalData);
            
            // 加载结果统计
            long duration = System.currentTimeMillis() - startTime;
            
            EasyLog.print(TAG, "📈 加载统计: " +
                "导航=" + globalData.getNavTabMap().size() + 
                ", 书籍=" + globalData.getNavTabBodyMap().size() +
                ", 药物=" + globalData.getYaoMap().size() +
                ", 名词=" + globalData.getMingCiContentMap().size() +
                ", 方剂别名=" + globalData.getFangAliasDict().size() +
                ", 耗时=" + duration + "ms");
            
            EasyLog.print(TAG, "✅ 本地数据加载完成");
            
        } catch (Exception e) {
            EasyLog.print(TAG, "❌ 加载本地数据失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载药物数据
     */
    private static void loadYaoData(GlobalDataHolder globalData) {
        ArrayList<Yao> yaoList = DataRepository.getYaoData();
        if (yaoList != null && !yaoList.isEmpty()) {
            for (Yao yao : yaoList) {
                globalData.putYao(yao.getName(), yao);
                // 同时添加别名映射
                if (yao.getYaoList() != null) {
                    for (String alias : yao.getYaoList()) {
                        globalData.putYao(alias, yao);
                    }
                }
            }
            EasyLog.print(TAG, "Loaded " + yaoList.size() + " Yao items");
        }
    }
    
    /**
     * 加载名词数据
     */
    private static void loadMingCiData(GlobalDataHolder globalData) {
        ArrayList<MingCiContent> mingCiList = DataRepository.getMingCi();
        if (mingCiList != null && !mingCiList.isEmpty()) {
            for (MingCiContent mingCi : mingCiList) {
                globalData.putMingCiContent(mingCi.getName(), mingCi);
            }
            EasyLog.print(TAG, "Loaded " + mingCiList.size() + " MingCi items");
        }
    }
    
    /**
     * 加载药物别名数据
     */
    private static void loadYaoAliasData(GlobalDataHolder globalData) {
        java.util.List<ZhongYaoAlia> aliasList = DataRepository.getYaoAlia();
        if (aliasList != null && !aliasList.isEmpty()) {
            Map<String, String> yaoAliasDict = new java.util.HashMap<>();
            for (ZhongYaoAlia alias : aliasList) {
                yaoAliasDict.put(alias.getBieming(), alias.getName());
            }
            globalData.putAllYaoAlias(yaoAliasDict);
            EasyLog.print(TAG, "Loaded " + aliasList.size() + " Yao aliases");
        }
    }
    
    /**
     * 加载方剂别名数据（从现有方剂数据中提取别名）
     * 
     * <p>依赖关系：
     * <ul>
     *   <li>必须在 {@link #loadNavigationData(DbService, GlobalDataHolder)} 之后调用</li>
     *   <li>依赖 {@code globalData.getNavTabBodyMap()} 中的书籍信息</li>
     * </ul>
     * 
     * <p>由于当前系统没有专门的方剂别名实体，我们从方剂名称中提取可能的别名。
     * 
     * @param globalData 全局数据持有者
     */
    private static void loadFangAliasData(GlobalDataHolder globalData) {
        try {
            EasyLog.print(TAG, "🔍 [loadFangAliasData] 开始加载...");
            
            // 从所有书籍的方剂数据中提取别名信息
            List<TabNavBody> bookInfos = globalData.getAllBookInfos();
            EasyLog.print(TAG, "🔍 [loadFangAliasData] bookInfos.size() = " + bookInfos.size());
            
            Map<String, String> fangAliasDict = new java.util.HashMap<>();
            
            int aliasCount = 0;
            int bookIndex = 0;
            for (TabNavBody bookInfo : bookInfos) {
                bookIndex++;
                int bookId = bookInfo.getBookNo();
                EasyLog.print(TAG, "🔍 [loadFangAliasData] 处理第 " + bookIndex + " 本书, bookId=" + bookId);
                
                ArrayList<Fang> fangList = DataRepository.getFangDetailList(bookId);
                EasyLog.print(TAG, "🔍 [loadFangAliasData] bookId=" + bookId + ", fangList.size()=" + 
                    (fangList != null ? fangList.size() : "null"));
                
                if (fangList != null && !fangList.isEmpty()) {
                    for (Fang fang : fangList) {
                        String fangName = fang.getName();
                        if (fangName != null && !fangName.trim().isEmpty()) {
                            // 添加标准方剂名称
                            fangAliasDict.put(fangName.trim(), fangName.trim());
                            aliasCount++;
                        }
                    }
                }
            }
            
            EasyLog.print(TAG, "🔍 [loadFangAliasData] 收集到 " + aliasCount + " 条别名，准备调用 putAllFangAlias");
            
            // 使用 putAllFangAlias 触发加载状态标记
            globalData.putAllFangAlias(fangAliasDict);
            
            EasyLog.print(TAG, "🔍 [loadFangAliasData] putAllFangAlias 完成，isFangAliasLoaded=" + globalData.isFangAliasLoaded());
            
            // 校验加载结果
            if (aliasCount == 0 && bookInfos.size() > 0) {
                EasyLog.print(TAG, "⚠️ 方剂别名为 0，但导航数据有 " + bookInfos.size() + " 本书");
            }
            
            EasyLog.print(TAG, "✅ 加载 " + aliasCount + " 条方剂别名（来自 " + bookInfos.size() + " 本书）");
            
        } catch (Exception e) {
            EasyLog.print(TAG, "Error loading Fang aliases: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载导航数据
     */
    private static void loadNavigationData(DbService dbService, GlobalDataHolder globalData) {
        ArrayList<TabNav> navList = dbService.mTabNavService.findAll();
        if (navList == null || navList.isEmpty()) {
            return;
        }
        
        for (TabNav nav : navList) {
            globalData.putNavTab(nav.getOrder(), nav);
            
            if (nav.getNavList() != null) {
                for (TabNavBody item : nav.getNavList()) {
                    if (item.getBookNo() > 0) {
                        globalData.putBookInfo(item.getBookNo(), item);
                    }
                }
            }
        }
        
        EasyLog.print(TAG, "Loaded " + navList.size() + " navigation tabs");
    }
    
    /**
     * 强制重新初始化（用于手动刷新）
     */
    public static void forceReinitialize() {
        synchronized (AppDataInitializer.class) {
            isInitialized = false;
            // 清空现有数据
            GlobalDataHolder.getInstance().clearAll();
        }
    }
    
    /**
     * 检查是否已初始化
     */
    public static boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * 标记初始化完成（网络加载后调用）
     */
    public static void markInitialized() {
        isInitialized = true;
    }
}
