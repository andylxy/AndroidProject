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

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.Map;

import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.greendao.entity.ZhongYaoAlia;
import run.yigou.gxzy.greendao.util.ConvertEntity;
import run.yigou.gxzy.greendao.util.DbService;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.data.GlobalDataHolder;
import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;

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
                if (hasLocalData()) {
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
     */
    private static void loadFromLocalDatabase() {
        try {
            DbService dbService = DbService.getInstance();
            GlobalDataHolder globalData = GlobalDataHolder.getInstance();
            
            // 1. 加载药物数据
            loadYaoData(globalData);
            
            // 2. 加载名词数据
            loadMingCiData(globalData);
            
            // 3. 加载药物别名
            loadYaoAliasData(globalData);
            
            // 4. 加载导航数据
            loadNavigationData(dbService, globalData);
            
            EasyLog.print(TAG, "Local data loaded successfully");
            
        } catch (Exception e) {
            EasyLog.print(TAG, "Error loading local data: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 加载药物数据
     */
    private static void loadYaoData(GlobalDataHolder globalData) {
        ArrayList<Yao> yaoList = ConvertEntity.getYaoData();
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
        ArrayList<MingCiContent> mingCiList = ConvertEntity.getMingCi();
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
        java.util.List<ZhongYaoAlia> aliasList = ConvertEntity.getYaoAlia();
        if (aliasList != null && !aliasList.isEmpty()) {
            Map<String, String> yaoAliasDict = globalData.getYaoAliasDict();
            for (ZhongYaoAlia alias : aliasList) {
                yaoAliasDict.put(alias.getBieming(), alias.getName());
            }
            EasyLog.print(TAG, "Loaded " + aliasList.size() + " Yao aliases");
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
        
        Map<Integer, TabNav> navTabMap = globalData.getNavTabMap();
        Map<Integer, TabNavBody> navTabBodyMap = globalData.getNavTabBodyMap();
        
        for (TabNav nav : navList) {
            navTabMap.put(nav.getOrder(), nav);
            
            if (nav.getNavList() != null) {
                for (TabNavBody item : nav.getNavList()) {
                    if (item.getBookNo() > 0) {
                        navTabBodyMap.put(item.getBookNo(), item);
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
