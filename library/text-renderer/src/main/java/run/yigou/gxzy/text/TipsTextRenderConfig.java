package run.yigou.gxzy.text;

import android.graphics.Color;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Tips 文本渲染配置中心
 * 负责管理样式配置的生命周期，支持：
 * - 服务端动态下发
 * - 运行时动态修改
 * - 本地缓存加载
 * - 配置校验与降级
 * - 线程安全（读写锁）
 * - 版本管理
 * 
 * 配置加载流程：
 * 1. 应用启动：configMap 预加载 13 个默认配置
 * 2. 首次渲染：checkConfigBeforeUse() 返回 false（未加载），返回触底配置 #CCCCCC
 * 3. 触发加载：异步请求服务端配置
 * 4. 配置加载中：checkConfigBeforeUse() 返回 false（isLoading=true），返回触底配置 #CCCCCC
 * 5. 服务端配置返回：应用到 configMap，替换默认配置
 * 6. 后续渲染：checkConfigBeforeUse() 返回 true，返回服务端配置
 * 7. 加载失败：configMap 保持默认配置，后续渲染返回默认配置
 * 
 * 使用示例：
 * <pre>
 * // 1. 应用启动时加载服务端配置
 * List&lt;StyleConfigApiBean.StyleItem&gt; serverConfigs = ...; // 从 API 获取
 * TipsTextRenderConfig.getInstance().applyServerConfig(serverConfigs);
 * 
 * // 2. 或使用本地缓存配置
 * Map&lt;String, StyleConfig&gt; cachedConfigs = ...; // 从 SharedPreferences 加载
 * TipsTextRenderConfig.getInstance().updateStyleConfig(cachedConfigs);
 * 
 * // 3. 增量合并配置
 * Map&lt;String, StyleConfig&gt; additionalConfigs = ...;
 * TipsTextRenderConfig.getInstance().mergeStyleConfig(additionalConfigs);
 * 
 * // 4. 获取配置版本号（检测配置是否更新）
 * int version = TipsTextRenderConfig.getInstance().getConfigVersion();
 * </pre>
 */
public class TipsTextRenderConfig {
    
    private static final String TAG = "TipsTextRenderConfig";
    
    /**
     * 打印日志（使用 Android Log）
     */
    private static void log(String message) {
        android.util.Log.d(TAG, message);
    }
    
    // ==================== 单例 ====================
    
    private static volatile TipsTextRenderConfig instance;
    
    /**
     * 获取配置中心单例
     */
    public static TipsTextRenderConfig getInstance() {
        if (instance == null) {
            synchronized (TipsTextRenderConfig.class) {
                if (instance == null) {
                    instance = new TipsTextRenderConfig();
                }
            }
        }
        return instance;
    }
    
    // ==================== 配置提供者 ====================
    
    /** 配置提供者（由 app 模块设置） */
    private volatile IStyleConfigProvider provider;
    
    /** 是否正在加载配置（防止重复触发） */
    private volatile boolean isLoading = false;
    
    /**
     * 设置配置提供者（由 app 模块在 Application.onCreate() 中调用）
     * 
     * 使用示例：
     * <pre>
     * // Application.onCreate()
     * TipsTextRenderConfig.getInstance().setProvider(new AppStyleConfigProvider());
     * </pre>
     * 
     * @param provider 配置提供者实现
     */
    public void setProvider(IStyleConfigProvider provider) {
        this.provider = provider;
        log("已设置配置提供者: " + provider.getClass().getSimpleName());
    }
    
    // ==================== 配置存储 ====================
    
    /** 样式配置映射（marker -> StyleConfig） */
    private final ConcurrentHashMap<String, StyleConfig> configMap = new ConcurrentHashMap<>();
    
    /** 读写锁（保证并发安全） */
    private final ReadWriteLock configLock = new ReentrantReadWriteLock();
    
    /** 配置版本号（每次成功更新时递增） */
    private final AtomicInteger configVersion = new AtomicInteger(0);
    
    /**
     * 私有构造函数，初始化默认配置
     */
    private TipsTextRenderConfig() {
        loadDefaultConfigs();
    }
    
    // ==================== 公共 API ====================
    
    /**
     * 完全替换配置
     * @param newConfigs 新的样式配置映射（会完全替换现有配置）
     */
    public void updateStyleConfig(Map<String, StyleConfig> newConfigs) {
        Map<String, StyleConfig> validated = validateConfigs(newConfigs);
        
        configLock.writeLock().lock();
        try {
            configMap.clear();
            configMap.putAll(validated);
            int newVersion = configVersion.incrementAndGet();
            log("配置已完全替换，版本: " + newVersion + ", 共 " + configMap.size() + " 项");
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * 合并配置（增量更新）
     * @param additionalConfigs 要追加的配置
     */
    public void mergeStyleConfig(Map<String, StyleConfig> additionalConfigs) {
        if (additionalConfigs == null || additionalConfigs.isEmpty()) {
            return;
        }
        
        Map<String, StyleConfig> validated = validateConfigs(additionalConfigs);
        
        configLock.writeLock().lock();
        try {
            configMap.putAll(validated);
            int newVersion = configVersion.incrementAndGet();
            log("配置已合并，版本: " + newVersion + ", 共 " + configMap.size() + " 项");
        } finally {
            configLock.writeLock().unlock();
        }
    }
    
    /**
     * 应用服务端下发的样式配置
     * @param serverConfigs 服务端返回的样式列表
     */
    public void applyServerConfig(List<StyleConfigApiBean.StyleItem> serverConfigs) {
        if (serverConfigs == null || serverConfigs.isEmpty()) {
            log("服务端配置为空，保持当前配置");
            return;
        }
        
        log("开始应用服务端配置，共 " + serverConfigs.size() + " 项");
        
        Map<String, StyleConfig> configs = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < serverConfigs.size(); i++) {
            StyleConfigApiBean.StyleItem item = serverConfigs.get(i);
            try {
                // 校验 item
                if (item == null || item.getMarker() == null) {
                    log("跳过第 " + i + " 项：null item 或 marker");
                    failCount++;
                    continue;
                }
                
                // 安全解析颜色
                int color = parseColorSafe(item.getColor());
                
                // 限制 linkType 范围（0-100）
                int linkType = Math.max(0, Math.min(item.getLinkType(), 100));
                
                configs.put(item.getMarker(), new StyleConfig(
                    color,
                    item.isSmallFont(),
                    linkType
                ));
                successCount++;
            } catch (Exception e) {
                log("解析第 " + i + " 项失败: " + e.getMessage());
                failCount++;
            }
        }
        
        log("配置解析完成：成功 " + successCount + " 项，失败 " + failCount + " 项");
        
        // 应用校验后的配置
        if (!configs.isEmpty()) {
            updateStyleConfig(configs);
            isLoading = false; // 标记加载完成
            log("✅ 服务端配置已应用");
        } else {
            log("无有效配置项，保持当前配置");
        }
    }
    
    /**
     * 应用服务端下发的样式配置（通用接口，支持任意 StyleItem 实现）
     * 
     * 使用示例：
     * <pre>
     * // app 模块调用（使用 app 模块的 StyleConfigApiBean）
     * EasyHttp.post(new StyleConfigApi())
     *     .request(new EasyCallBack<StyleConfigApi.StyleConfigApiBean>() {
     *         @Override
     *         public void onSuccess(StyleConfigApi.StyleConfigApiBean response) {
     *             TipsTextRenderConfig.getInstance().applyServerConfigGeneric(
     *                 response.getStyles(),
     *                 new ItemExtractor<StyleConfigApi.StyleConfigApiBean.StyleItem>() {
     *                     @Override
     *                     public String getMarker(StyleConfigApi.StyleConfigApiBean.StyleItem item) {
     *                         return item.getMarker();
     *                     }
     *                     @Override
     *                     public String getColor(StyleConfigApi.StyleConfigApiBean.StyleItem item) {
     *                         return item.getColor();
     *                     }
     *                     @Override
     *                     public boolean isSmallFont(StyleConfigApi.StyleConfigApiBean.StyleItem item) {
     *                         return item.isSmallFont();
     *                     }
     *                     @Override
     *                     public int getLinkType(StyleConfigApi.StyleConfigApiBean.StyleItem item) {
     *                         return item.getLinkType();
     *                     }
     *                 }
     *             );
     *         }
     *     });
     * </pre>
     * 
     * @param items 服务端返回的样式列表（任意类型）
     * @param extractor 样式项提取器
     * @param <T> 样式项类型
     */
    public <T> void applyServerConfigGeneric(List<T> items, ItemExtractor<T> extractor) {
        if (items == null || items.isEmpty()) {
            log("服务端配置为空，保持当前配置");
            return;
        }
        
        log("开始应用服务端配置（通用接口），共 " + items.size() + " 项");
        
        Map<String, StyleConfig> configs = new HashMap<>();
        int successCount = 0;
        int failCount = 0;
        
        for (int i = 0; i < items.size(); i++) {
            T item = items.get(i);
            try {
                // 校验 item
                if (item == null) {
                    log("跳过第 " + i + " 项：null item");
                    failCount++;
                    continue;
                }
                
                String marker = extractor.getMarker(item);
                if (marker == null) {
                    log("跳过第 " + i + " 项：null marker");
                    failCount++;
                    continue;
                }
                
                // 安全解析颜色
                String colorStr = extractor.getColor(item);
                int color = parseColorSafe(colorStr);
                
                // 限制 linkType 范围（0-100）
                int linkType = Math.max(0, Math.min(extractor.getLinkType(item), 100));
                
                configs.put(marker, new StyleConfig(
                    color,
                    extractor.isSmallFont(item),
                    linkType
                ));
                successCount++;
            } catch (Exception e) {
                log("解析第 " + i + " 项失败: " + e.getMessage());
                failCount++;
            }
        }
        
        log("配置解析完成：成功 " + successCount + " 项，失败 " + failCount + " 项");
        
        // 应用校验后的配置
        if (!configs.isEmpty()) {
            updateStyleConfig(configs);
            isLoading = false; // 标记加载完成
            log("✅ 服务端配置已应用（通用接口）");
        } else {
            log("无有效配置项，保持当前配置");
        }
    }
    
    /**
     * 样式项提取器接口（用于适配不同类型的 StyleItem）
     * @param <T> 样式项类型
     */
    public interface ItemExtractor<T> {
        String getMarker(T item);
        String getColor(T item);
        boolean isSmallFont(T item);
        int getLinkType(T item);
    }
    
    /**
     * 检查配置是否已加载，未加载时自动触发加载
     * 
     * 调用时机：首次渲染前调用
     * 行为：
     * - 如果已加载：直接返回 true
     * - 如果未加载且未在加载中：触发异步加载，返回 false
     * - 如果正在加载中：返回 false
     * 
     * @return true=配置已就绪，false=配置未就绪（使用默认配置）
     */
    public boolean checkConfigBeforeUse() {
        if (provider == null) {
            log("⚠️ 未设置 IStyleConfigProvider，使用默认配置");
            return false;
        }
        
        if (provider.isConfigLoaded()) {
            return true;
        }
        
        // 配置未加载，尝试触发加载
        synchronized (this) {
            if (isLoading) {
                log("配置正在加载中，跳过重复触发");
                return false;
            }
            
            isLoading = true;
            log("配置未加载，触发自动加载...");
            
            boolean triggered = provider.loadConfig();
            if (!triggered) {
                isLoading = false;
                log("⚠️ 配置加载触发失败");
            } else {
                log("✅ 已触发配置加载，等待异步完成");
            }
            return false;
        }
    }
    
    /**
     * 获取样式配置（供 TipsTextRenderer 调用）
     * 
     * 执行流程：
     * 1. 检查配置是否就绪（checkConfigBeforeUse）
     * 2. 未就绪（未加载/正在加载）→ 返回触底配置 #CCCCCC
     * 3. 已就绪（服务端配置/默认配置）→ 从 configMap 返回
     * 
     * @param marker 标记（如 "r", "u", "f" 等）
     * @return 样式配置（未就绪返回触底配置，未知标记返回默认配置）
     */
    public StyleConfig getStyleConfig(String marker) {
        // 检查配置是否已加载
        boolean configReady = checkConfigBeforeUse();
        
        // 配置未就绪（未加载或正在加载），返回触底配置
        if (!configReady) {
            return getFallbackStyleConfig();
        }
        
        // 配置已就绪（服务端配置加载成功或失败），从 configMap 返回
        configLock.readLock().lock();
        try {
            StyleConfig config = configMap.get(marker);
            return config != null ? config : getDefaultStyleConfig();
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    /**
     * 获取当前配置版本号
     * 调用方可用于检测配置是否已更新
     * @return 配置版本号
     */
    public int getConfigVersion() {
        return configVersion.get();
    }
    
    /**
     * 获取当前所有配置（用于缓存保存）
     * 
     * @return 配置映射的副本
     */
    public Map<String, StyleConfig> getAllConfig() {
        configLock.readLock().lock();
        try {
            return new HashMap<>(configMap);
        } finally {
            configLock.readLock().unlock();
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 校验并过滤非法配置项
     * @param rawConfigs 原始配置（可能包含非法数据）
     * @return 校验后的安全配置
     */
    private Map<String, StyleConfig> validateConfigs(Map<String, StyleConfig> rawConfigs) {
        if (rawConfigs == null || rawConfigs.isEmpty()) {
            log("配置为空，使用默认配置");
            return loadDefaultConfigs();
        }
        
        Map<String, StyleConfig> safeConfigs = new HashMap<>();
        for (Map.Entry<String, StyleConfig> entry : rawConfigs.entrySet()) {
            String marker = entry.getKey();
            StyleConfig config = entry.getValue();
            
            // 校验 marker（必须是单个字母）
            if (marker == null || marker.length() != 1 || !Character.isLetter(marker.charAt(0))) {
                log("跳过非法 marker: " + marker);
                continue;
            }
            
            // 校验 config
            if (config == null) {
                log("跳过 null config: " + marker);
                continue;
            }
            
            // 校验 linkType 范围（0-100，预留扩展空间）
            if (config.linkType < 0 || config.linkType > 100) {
                log("linkType 超出范围: " + config.linkType + ", marker=" + marker);
                continue;
            }
            
            safeConfigs.put(marker, config);
        }
        
        // 如果所有配置都非法，降级到默认配置
        if (safeConfigs.isEmpty()) {
            log("所有配置项均非法，使用默认配置");
            return loadDefaultConfigs();
        }
        
        return safeConfigs;
    }
    
    /**
     * 加载默认配置（兜底方案）
     * @return 默认配置映射
     */
    private Map<String, StyleConfig> loadDefaultConfigs() {
        Map<String, StyleConfig> defaults = new HashMap<>();
        defaults.put("r", new StyleConfig(Color.RED, true, 0));
        defaults.put("n", new StyleConfig(Color.BLUE, false, 0));
        defaults.put("f", new StyleConfig(Color.BLUE, false, 2));
        defaults.put("a", new StyleConfig(Color.GRAY, true, 0));
        defaults.put("m", new StyleConfig(Color.RED, false, 0));
        defaults.put("g", new StyleConfig(Color.argb(230, 0, 128, 255), false, 3));
        defaults.put("u", new StyleConfig(Color.BLUE, false, 1));
        defaults.put("v", new StyleConfig(Color.BLUE, false, 0));
        defaults.put("w", new StyleConfig(Color.rgb(28, 181, 92), true, 0));
        defaults.put("q", new StyleConfig(Color.rgb(61, 200, 120), false, 0));
        defaults.put("h", new StyleConfig(Color.BLACK, false, 0));
        defaults.put("x", new StyleConfig(Color.parseColor("#EA8E3B"), false, 0));
        defaults.put("y", new StyleConfig(Color.parseColor("#9A764F"), false, 0));
        
        configMap.clear();
        configMap.putAll(defaults);
        log("已加载默认配置，共 " + defaults.size() + " 项");
        return defaults;
    }
    
    /**
     * 安全解析颜色值（异常兜底为黑色）
     * @param colorStr 颜色字符串（如 "#FF0000"）
     * @return 解析后的颜色值
     */
    private int parseColorSafe(String colorStr) {
        if (colorStr == null || colorStr.isEmpty()) {
            return Color.BLACK;
        }
        try {
            return Color.parseColor(colorStr);
        } catch (IllegalArgumentException e) {
            log("颜色解析失败: " + colorStr);
            return Color.BLACK;
        }
    }
    
    /**
     * 获取默认样式配置（未知标记兜底）
     * @return 默认配置（灰色 #808080，正常字体，无链接）
     */
    private StyleConfig getDefaultStyleConfig() {
        return new StyleConfig(Color.GRAY, false, 0);
    }
    
    /**
     * 获取触底样式配置（配置未加载时的临时占位）
     * 
     * 与 getDefaultStyleConfig() 的区别：
     * - getFallbackStyleConfig()：配置未就绪时的触底配置（浅灰色 #CCCCCC，小字体）
     * - getDefaultStyleConfig()：未知标记的默认配置（灰色 #808080，正常字体）
     * 
     * 触发时机：
     * - 首次渲染（配置未加载）
     * - 配置正在加载中
     * 
     * @return 触底配置（浅灰色 #CCCCCC，小字体，无链接）
     */
    private StyleConfig getFallbackStyleConfig() {
        return new StyleConfig(Color.parseColor("#CCCCCC"), true, 0);
    }
    
    // ==================== 数据类 ====================
    
    /**
     * 样式配置类
     * 支持服务端动态下发，通过 updateStyleConfig() 或 applyServerConfig() 更新
     */
    public static class StyleConfig {
        /** 文本颜色 */
        public final int color;
        
        /** 是否使用相对小字体（0.7x） */
        public final boolean isSmallFont;
        
        /** 链接类型：0=无链接，>=1 由调用方自定义 */
        public final int linkType;
        
        public StyleConfig(int color, boolean isSmallFont, int linkType) {
            this.color = color;
            this.isSmallFont = isSmallFont;
            this.linkType = linkType;
        }
    }
}
