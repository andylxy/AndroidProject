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
 * 1. App 启动：AppApplication.onCreate() 调用 loadStyleConfigOnInit()
 *    - 有缓存：从缓存加载配置 → isLoaded = true → 返回缓存配置
 *    - 无缓存：加载默认配置（13个预定义）→ isLoaded = true → 返回默认配置
 * 2. HomeFragment 初始化：HomeFragment.initData() 调用 loadStyleConfig()
 *    - 先检查缓存：有缓存 → 使用缓存配置，不再请求后端
 *    - 无缓存：请求服务端配置 → 应用到 configMap → 保存到缓存 → 更新配置列表
 * 3. 首次渲染：checkConfigBeforeUse() 返回 true → 从 configMap 返回配置
 *    - 后端已返回 → 返回服务端配置
 *    - 后端未返回 → 返回默认配置（#FF0000）
 * 4. 点击刷新：请求服务端配置 → 应用到 configMap → 保存到缓存
 * 5. 下次启动：AppApplication 从缓存加载 → HomeFragment 检查缓存存在 → 不再请求后端
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
     * 检查配置是否已加载（纯检查，不触发加载）
     * 
     * 职责：
     * - 仅检查配置状态，不发起网络请求
     * - 返回配置是否就绪
     * 
     * @return true=配置已就绪，false=配置未就绪
     */
    public boolean checkConfigBeforeUse() {
        if (provider == null) {
            log("⚠️ 未设置 IStyleConfigProvider，使用默认配置");
            return false;
        }
        
        return provider.isConfigLoaded();
    }
    
    /**
     * 获取样式配置（供 TipsTextRenderer 调用）
     * 
     * 执行流程：
     * 1. 检查配置状态（checkConfigBeforeUse）
     * 2. 从 configMap 返回配置（默认配置或服务端配置）
     * 
     * @param marker 标记（如 "r", "u", "f" 等）
     * @return 样式配置（从 configMap 返回，未知标记返回默认配置）
     */
    public StyleConfig getStyleConfig(String marker) {
        // 检查配置状态（仅检查，不触发加载）
        checkConfigBeforeUse();
        
        // 始终从 configMap 返回配置（默认配置或服务端配置）
        configLock.readLock().lock();
        try {
            StyleConfig config = configMap.get(marker);
            if (config == null) {
                log("⚠️ 标记 " + marker + " 不存在，返回默认配置，configMap.size=" + configMap.size());
            } else {
                log("✅ 获取配置 marker=" + marker + ", color=" + config.color + ", linkType=" + config.linkType);
            }
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
     * 
     * 调用时机：
     * - 缓存无数据时
     * - 服务端配置加载失败时
     * 
     * @return 默认配置映射
     */
    public Map<String, StyleConfig> loadDefaultConfigs() {
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
