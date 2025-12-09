/*
 * 项目名: AndroidProject
 * 类名: GlobalDataHolder.java
 * 包名: run.yigou.gxzy.ui.tips.data
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import run.yigou.gxzy.greendao.entity.AiConfig;
import run.yigou.gxzy.greendao.entity.TabNav;
import run.yigou.gxzy.greendao.entity.TabNavBody;
import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;

/**
 * 全局数据持有者（单例）
 * 从 TipsSingleData 提取真正全局的数据
 * 特性：
 * - 线程安全（ConcurrentHashMap）
 * - 单例模式（双重检查锁）
 * - 职责明确（只存全局数据，不管理书籍内容）
 */
public class GlobalDataHolder {
    
    private static volatile GlobalDataHolder instance;
    
    // 导航分类映射（tabId -> TabNav）
    private final Map<Integer, TabNav> navTabMap;
    
    // 书籍信息映射（bookId -> TabNavBody）
    private final Map<Integer, TabNavBody> navTabBodyMap;
    
    // 药物别名字典（全局唯一）
    private final Map<String, String> yaoAliasDict;
    
    // 方剂别名字典（全局唯一）
    private final Map<String, String> fangAliasDict;
    
    // 药物详细信息映射（yaoName -> Yao）
    private final Map<String, Yao> yaoMap;
    
    // 名词内容映射（name -> MingCiContent）
    private final Map<String, MingCiContent> mingCiContentMap;
    
    // AI 配置列表
    private List<AiConfig> aiConfigList;
    
    /**
     * 私有构造函数
     */
    private GlobalDataHolder() {
        this.navTabMap = new ConcurrentHashMap<>();
        this.navTabBodyMap = new ConcurrentHashMap<>();
        this.yaoAliasDict = new ConcurrentHashMap<>();
        this.fangAliasDict = new ConcurrentHashMap<>();
        this.yaoMap = new ConcurrentHashMap<>();
        this.mingCiContentMap = new ConcurrentHashMap<>();
        this.aiConfigList = new ArrayList<>();
    }
    
    /**
     * 获取单例实例
     */
    @NonNull
    public static GlobalDataHolder getInstance() {
        if (instance == null) {
            synchronized (GlobalDataHolder.class) {
                if (instance == null) {
                    instance = new GlobalDataHolder();
                }
            }
        }
        return instance;
    }
    
    // ==================== 导航数据 ====================
    
    /**
     * 获取导航映射
     */
    @NonNull
    public Map<Integer, TabNav> getNavTabMap() {
        return navTabMap;
    }
    
    /**
     * 添加导航分类
     */
    public void putNavTab(int tabId, @NonNull TabNav nav) {
        navTabMap.put(tabId, nav);
    }
    
    /**
     * 获取导航分类
     */
    @Nullable
    public TabNav getNavTab(int tabId) {
        return navTabMap.get(tabId);
    }
    
    // ==================== 书籍信息 ====================
    
    /**
     * 获取书籍信息映射
     */
    @NonNull
    public Map<Integer, TabNavBody> getNavTabBodyMap() {
        return navTabBodyMap;
    }
    
    /**
     * 添加书籍信息
     */
    public void putBookInfo(int bookId, @NonNull TabNavBody bookInfo) {
        navTabBodyMap.put(bookId, bookInfo);
    }
    
    /**
     * 获取书籍信息
     */
    @Nullable
    public TabNavBody getBookInfo(int bookId) {
        return navTabBodyMap.get(bookId);
    }
    
    // ==================== 别名字典 ====================
    
    /**
     * 获取药物别名字典
     */
    @NonNull
    public Map<String, String> getYaoAliasDict() {
        return yaoAliasDict;
    }
    
    /**
     * 设置药物别名
     */
    public void putYaoAlias(@NonNull String alias, @NonNull String realName) {
        yaoAliasDict.put(alias, realName);
    }
    
    /**
     * 批量设置药物别名
     */
    public void putAllYaoAlias(@NonNull Map<String, String> aliases) {
        yaoAliasDict.putAll(aliases);
    }
    
    /**
     * 获取方剂别名字典
     */
    @NonNull
    public Map<String, String> getFangAliasDict() {
        return fangAliasDict;
    }
    
    /**
     * 设置方剂别名
     */
    public void putFangAlias(@NonNull String alias, @NonNull String realName) {
        fangAliasDict.put(alias, realName);
    }
    
    /**
     * 批量设置方剂别名
     */
    public void putAllFangAlias(@NonNull Map<String, String> aliases) {
        fangAliasDict.putAll(aliases);
    }
    
    // ==================== 药物信息 ====================
    
    /**
     * 获取药物映射
     */
    @NonNull
    public Map<String, Yao> getYaoMap() {
        return yaoMap;
    }
    
    /**
     * 添加药物信息
     */
    public void putYao(@NonNull String name, @NonNull Yao yao) {
        yaoMap.put(name, yao);
    }
    
    /**
     * 获取药物信息
     */
    @Nullable
    public Yao getYao(@NonNull String name) {
        return yaoMap.get(name);
    }
    
    /**
     * 获取所有药物名称
     */
    @NonNull
    public List<String> getAllYaoNames() {
        return new ArrayList<>(yaoMap.keySet());
    }
    
    // ==================== 名词内容 ====================
    
    /**
     * 获取名词内容映射
     */
    @NonNull
    public Map<String, MingCiContent> getMingCiContentMap() {
        return mingCiContentMap;
    }
    
    /**
     * 添加名词内容
     */
    public void putMingCiContent(@NonNull String name, @NonNull MingCiContent content) {
        mingCiContentMap.put(name, content);
    }
    
    /**
     * 获取名词内容
     */
    @Nullable
    public MingCiContent getMingCiContent(@NonNull String name) {
        return mingCiContentMap.get(name);
    }
    
    // ==================== AI 配置 ====================
    
    /**
     * 获取 AI 配置列表
     */
    @NonNull
    public synchronized List<AiConfig> getAiConfigList() {
        return new ArrayList<>(aiConfigList);
    }
    
    /**
     * 设置 AI 配置列表
     */
    public synchronized void setAiConfigList(@Nullable List<AiConfig> configs) {
        if (this.aiConfigList == null) {
            this.aiConfigList = new ArrayList<>();
        }
        this.aiConfigList.clear();
        
        if (configs != null) {
            this.aiConfigList.addAll(configs);
        }
    }
    
    // ==================== 清理方法 ====================
    
    /**
     * 清空所有数据（谨慎使用）
     */
    public synchronized void clearAll() {
        navTabMap.clear();
        navTabBodyMap.clear();
        yaoAliasDict.clear();
        fangAliasDict.clear();
        yaoMap.clear();
        mingCiContentMap.clear();
        if (aiConfigList != null) {
            aiConfigList.clear();
        }
    }
    
    /**
     * 销毁单例（应用退出时调用）
     */
    public static synchronized void destroy() {
        if (instance != null) {
            instance.clearAll();
            instance = null;
        }
    }
    
    /**
     * 估算内存占用（KB）
     */
    public int estimateMemorySize() {
        int size = 0;
        
        // 导航数据
        size += navTabMap.size() * 200;
        size += navTabBodyMap.size() * 300;
        
        // 别名字典
        size += yaoAliasDict.size() * 50;
        size += fangAliasDict.size() * 50;
        
        // 药物和名词
        size += yaoMap.size() * 500;
        size += mingCiContentMap.size() * 400;
        
        // AI 配置
        if (aiConfigList != null) {
            size += aiConfigList.size() * 100;
        }
        
        return size / 1024; // 转换为 KB
    }
}
