/*
 * 项目名: AndroidProject
 * 类名: GlobalDataHolder.java
 * 包名: run.yigou.gxzy.ui.tips.data
 * 作者 : AI Assistant
 * 当前修改时间 : 2025年12月09日
 * Copyright (c) 2025, Inc. All Rights Reserved
 */

package run.yigou.gxzy.base;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import run.yigou.gxzy.data.local.entity.AiConfig;
import run.yigou.gxzy.data.local.entity.TabNav;
import run.yigou.gxzy.data.local.entity.TabNavBody;
import run.yigou.gxzy.data.model.MingCiContent;
import run.yigou.gxzy.data.model.Yao;

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
    
    // AI 配置列表（读多写少场景，使用 CopyOnWriteArrayList）
    private final CopyOnWriteArrayList<AiConfig> aiConfigList;
    
    // 数据加载状态标记（volatile 保证多线程可见性）
    private volatile boolean navDataLoaded = false;
    private volatile boolean yaoAliasLoaded = false;
    private volatile boolean fangAliasLoaded = false;
    private volatile boolean yaoDataLoaded = false;
    private volatile boolean mingCiDataLoaded = false;
    private volatile boolean aiConfigLoaded = false;
    
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
        this.aiConfigList = new CopyOnWriteArrayList<>();
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
        navDataLoaded = true;
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
        navDataLoaded = true;
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
        yaoAliasLoaded = true;
    }
    
    /**
     * 批量设置药物别名
     */
    public void putAllYaoAlias(@NonNull Map<String, String> aliases) {
        yaoAliasDict.putAll(aliases);
        yaoAliasLoaded = true;
        android.util.Log.i("GlobalDataHolder", "药物别名字典更新: +" + aliases.size() + " 条");
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
        fangAliasLoaded = true;
    }
    
    /**
     * 批量设置方剂别名
     */
    public void putAllFangAlias(@NonNull Map<String, String> aliases) {
        fangAliasDict.putAll(aliases);
        fangAliasLoaded = true;
        android.util.Log.i("GlobalDataHolder", "方剂别名字典更新: +" + aliases.size() + " 条");
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
        yaoDataLoaded = true;
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
        mingCiDataLoaded = true;
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
    public List<AiConfig> getAiConfigList() {
        return new ArrayList<>(aiConfigList);
    }
    
    /**
     * 设置 AI 配置列表
     */
    public void setAiConfigList(@Nullable List<AiConfig> configs) {
        aiConfigList.clear();
        
        if (configs != null) {
            aiConfigList.addAll(configs);
        }
        aiConfigLoaded = true;
    }
    
    // ==================== 数据加载状态 ====================
    
    /**
     * 导航数据是否已加载
     */
    public boolean isNavDataLoaded() {
        return navDataLoaded;
    }
    
    /**
     * 药物别名字典是否已加载
     */
    public boolean isYaoAliasLoaded() {
        return yaoAliasLoaded;
    }
    
    /**
     * 方剂别名字典是否已加载
     */
    public boolean isFangAliasLoaded() {
        return fangAliasLoaded;
    }
    
    /**
     * 药物详细信息是否已加载
     */
    public boolean isYaoDataLoaded() {
        return yaoDataLoaded;
    }
    
    /**
     * 名词内容是否已加载
     */
    public boolean isMingCiDataLoaded() {
        return mingCiDataLoaded;
    }
    
    /**
     * AI 配置是否已加载
     */
    public boolean isAiConfigLoaded() {
        return aiConfigLoaded;
    }
    
    // ==================== 数据刷新方法 ====================
    
    /**
     * 清空导航数据（用于刷新）
     * 
     * <p>调用后需重新从数据库加载导航数据。
     */
    public synchronized void reloadNavigationData() {
        navTabMap.clear();
        navTabBodyMap.clear();
        navDataLoaded = false;
        android.util.Log.i("GlobalDataHolder", "导航数据已清空，待重新加载");
    }
    
    /**
     * 清空药物别名字典
     */
    public synchronized void reloadYaoAlias() {
        yaoAliasDict.clear();
        yaoAliasLoaded = false;
        android.util.Log.i("GlobalDataHolder", "药物别名字典已清空，待重新加载");
    }
    
    /**
     * 清空方剂别名字典
     */
    public synchronized void reloadFangAlias() {
        fangAliasDict.clear();
        fangAliasLoaded = false;
        android.util.Log.i("GlobalDataHolder", "方剂别名字典已清空，待重新加载");
    }
    
    /**
     * 清空药物详细信息
     */
    public synchronized void reloadYaoData() {
        yaoMap.clear();
        yaoDataLoaded = false;
        android.util.Log.i("GlobalDataHolder", "药物详细信息已清空，待重新加载");
    }
    
    /**
     * 清空名词内容
     */
    public synchronized void reloadMingCiData() {
        mingCiContentMap.clear();
        mingCiDataLoaded = false;
        android.util.Log.i("GlobalDataHolder", "名词内容已清空，待重新加载");
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
     * 
     * @deprecated 此方法使用硬编码估算（如假设每个 Yao 对象固定 500 字节），
     *             但实际对象大小差异巨大（100 字节 ~ 10 KB），结果完全不可靠。
     *             <p>
     *             请使用以下工具进行准确的内存分析：
     *             <ul>
     *               <li>Android Studio Profiler - Memory 标签页</li>
     *               <li>LeakCanary - 内存泄漏检测</li>
     *               <li>Debug.getNativeHeapAllocatedSize() - 原生堆内存</li>
     *             </ul>
     *             <p>
     *             将于 v2.0 移除此方法。
     * 
     * @return 估算的内存占用（KB），仅供参考，不建议用于业务逻辑判断
     */
    @Deprecated
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
