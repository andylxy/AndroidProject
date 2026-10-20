package run.yigou.gxzy.config;

import androidx.lifecycle.LifecycleOwner;

import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;
import com.hjq.http.listener.OnHttpListener;

import run.yigou.gxzy.data.remote.api.style.StyleConfigApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.data.Callback;
import run.yigou.gxzy.text.IStyleConfigProvider;
import run.yigou.gxzy.utils.ThreadUtil;
import run.yigou.gxzy.text.StyleConfigApiBean;
import run.yigou.gxzy.text.TipsTextRenderConfig;

import java.io.Serializable;
import java.util.Map;

/**
 * app 模块样式配置提供者
 * 负责从服务端拉取配置并应用到 TipsTextRenderConfig
 * 
 * 工作流程：
 * 1. Application.onCreate() 中注册到 TipsTextRenderConfig
 * 2. 首次渲染时自动触发 loadConfig()
 * 3. 异步请求 StyleConfigApi 获取服务端配置
 * 4. 配置返回后通过 applyServerConfig() 直接应用（StyleConfigApiBean 已继承 library 模块）
 * 5. 下次渲染时使用服务端配置
 * 
 * 复用策略：
 * - StyleConfigApi.StyleConfigApiBean 继承 library 模块的 StyleConfigApiBean
 * - StyleItem 直接使用 library 模块的内部类
 * - 无需 ItemExtractor 适配，直接调用 applyServerConfig()
 */
public class AppStyleConfigProvider implements IStyleConfigProvider {
    
    /** 配置是否已加载 */
    private volatile boolean isLoaded = false;
    
    /** 配置版本号 */
    private volatile int version = 0;

    /** 最近一次生效的服务端版本号（0 = 还没从服务端拿到过）。 */
    private volatile long serverVersion = 0;
    
    @Override
    public boolean loadConfig(LifecycleOwner lifecycleOwner) {
        // 检查全局开关
        if (run.yigou.gxzy.app.AppApplication.application != null 
            && !run.yigou.gxzy.app.AppApplication.application.enableStyleConfig) {
            android.util.Log.i("AppStyleConfigProvider", "样式配置中心已禁用（全局开关），使用默认配置");
            return false;
        }
        
        // 这只是**本地**自增计数器，服务端并不认识它、也不会据此做增量下发
        // （每次请求都返回全量目录 —— 见 StyleConfigApi 的类注释）。
        // 保留传参只为兼容既有请求形态，别把它当增量游标用。
        int currentVersion = TipsTextRenderConfig.getInstance().getConfigVersion();
        
        // 异步请求服务端配置（使用 GET 请求，轻量级）
        // Application 不是 LifecycleOwner，传 null 表示不感知生命周期
        EasyHttp.get(lifecycleOwner)
            .api(new StyleConfigApi().setVersion(currentVersion))
            .request(new HttpCallback<HttpData<StyleConfigApi.StyleConfigApiBean>>((OnHttpListener) lifecycleOwner) {
                @Override
                public void onSucceed(HttpData<StyleConfigApi.StyleConfigApiBean> data) {
                    if (data == null || data.getData() == null) {
                        return;
                    }
                    
                    StyleConfigApi.StyleConfigApiBean response = data.getData();
                    if (response.getStyles() == null || response.getStyles().isEmpty()) {
                        return;
                    }
                    
                    // 直接使用 applyServerConfig()，无需 ItemExtractor 适配
                    // 因为 StyleConfigApiBean 已继承 library 模块的类
                    // 服务端版本号没变就什么都不做 —— 应用与落盘都省掉。
                    // 这是 Q6a 的「比较」那一半：少了它，缓存一旦存在就永远命中，
                    // 后台新增的标记永远到不了 App（ADR-0028 D3）。
                    long serverVer = response.getVersion();
                    if (serverVer != 0 && serverVer == serverVersion) {
                        isLoaded = true;
                        return;
                    }

                    TipsTextRenderConfig.getInstance().applyServerConfig(response.getStyles());
                    serverVersion = serverVer;
                    saveCacheConfigAsync(serverVer, TipsTextRenderConfig.getInstance().getAllConfig());

                    // 标记加载成功
                    isLoaded = true;
                    version++;
                }
                
                @Override
                public void onFail(Exception e) {
                    // 加载失败，下次渲染时会重试
                    // 静默处理，不打印日志（避免频繁失败导致日志过多）
                }
            });
        
        return true; // 已触发加载流程
    }
    
    @Override
    public boolean isConfigLoaded() {
        return isLoaded;
    }
    
    @Override
    public int getConfigVersion() {
        return version;
    }
    
    /**
     * 本地缓存读取的三种结果。
     *
     * <p>为什么必须有这个区分：{@link #loadCacheConfig()} 原来只返回 boolean，
     * 而且**两条分支都返回 true** —— 命中缓存 true，「无缓存 → 加载内置默认值」也 true。
     * 调用方（HomeFragment）于是把「回落默认值」当成「缓存命中」，直接不再请求后端，
     * 服务端样式一次都拉不到（实测日志：`样式配置已从缓存加载（不请求后端）`，
     * 而磁盘上根本没有缓存文件）。三种结果分开，调用方才能自己做决定。
     */
    public enum CacheState {
        /** 磁盘上真有缓存，已应用。 */
        HIT,
        /** 没有缓存，已回落内置默认值 —— 这时应该去请求后端。 */
        MISS,
        /** 读缓存过程抛异常。 */
        FAILED,
    }

    /**
     * 落盘的缓存内容：服务端版本号 + 配置表。
     *
     * <p>版本号由服务端下发（{@code MAX(updated_at_ms)}），用来判断手里的配置是否最新，
     * 以及让「增量未变更」这条路径成立。必须是 Serializable —— {@link StyleConfig} 现在也是，
     * 否则 {@code CacheHelper.saveObject} 会在运行期抛 NotSerializableException
     * （此前就一直如此，缓存从未真正写盘）。
     */
    public static class StyleConfigCache implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public long version;
        public java.util.Map<String, TipsTextRenderConfig.StyleConfig> configs;
    }

    /**
     * 从本地缓存加载配置。
     *
     * @return {@link CacheState#HIT} 已应用缓存；{@link CacheState#MISS} 无缓存、
     *         已加载内置默认值（调用方应据此去请求后端）；{@link CacheState#FAILED} 读盘异常。
     */
    public CacheState loadCacheConfig() {
        try {
            // 1. 尝试从缓存文件读取配置
            java.io.Serializable cachedData = run.yigou.gxzy.utils.CacheHelper.readObject("style_config_cache");

            if (cachedData instanceof StyleConfigCache) {
                StyleConfigCache cache = (StyleConfigCache) cachedData;
                if (cache.configs != null && !cache.configs.isEmpty()) {
                    // 2. 命中缓存 → 应用
                    TipsTextRenderConfig.getInstance().updateStyleConfig(cache.configs);
                    isLoaded = true;
                    version++;
                    serverVersion = cache.version;
                    return CacheState.HIT;
                }
            }

            // 3. 没有可用缓存 → 回落内置默认值（保证配置就绪），**并告知调用方去请求后端**
            TipsTextRenderConfig.getInstance().loadDefaultConfigs();
            isLoaded = true;
            version++;
            return CacheState.MISS;
        } catch (Exception e) {
            // 读盘失败不当成命中：调用方仍应尝试请求后端（有默认值兜底）。
            return CacheState.FAILED;
        }
    }
    
    /**
     * 异步从本地缓存加载配置（文件读写在后台线程，结果回主线程）。
     *
     * <p>为什么要有异步版本：{@link #loadCacheConfig()} 会读缓存文件，同步调用发生在
     * {@code AppApplication.onCreate} 与 {@code HomeFragment.initData}，严格模式下实测为
     * DiskReadViolation（见 greendao 加固票 11 A 项）。样式配置本身有内置默认值兜底，晚一帧应用不影响功能。
     *
     * @param callback 主线程回调：{@link CacheState#HIT} 已应用磁盘缓存；
     *                 {@link CacheState#MISS} 无缓存、已回落内置默认值；
     *                 {@link CacheState#FAILED} 读盘异常。后两者都应去请求后端。
     */
    public void loadCacheConfigAsync(final Callback<CacheState> callback) {
        ThreadUtil.runInBackground(new Runnable() {
            @Override
            public void run() {
                final CacheState loaded = loadCacheConfig();
                ThreadUtil.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (callback != null) {
                            callback.onSuccess(loaded);
                        }
                    }
                });
            }
        });
    }

    /**
     * 保存配置到缓存（写文件在后台线程）。
     *
     * <p>为什么改名带 Async：写入不再同步发生在调用线程上，调用方拿不到"写没写完"，
     * 方法名必须把这点讲清楚（见 greendao 加固票 11 A 项）。
     *
     * <p>与 {@link StyleConfigCache} 一起落盘的还有服务端版本号 —— 下次启动用它判断
     * 手里的配置是否最新（ADR-0028）。版本号缺失时传 0，等价于「未知」。
     *
     * @param version 服务端下发的配置版本号（0 = 未知）
     * @param configs 样式配置
     */
    public void saveCacheConfigAsync(final long version, final Map<String, TipsTextRenderConfig.StyleConfig> configs) {
        ThreadUtil.runInBackground(new Runnable() {
            @Override
            public void run() {
                try {
                    StyleConfigCache cache = new StyleConfigCache();
                    cache.version = version;
                    cache.configs = new java.util.HashMap<>(configs);
                    run.yigou.gxzy.utils.CacheHelper.saveObject(cache, "style_config_cache");
                } catch (Throwable t) {
                    // 缓存写失败不影响本次运行（有默认值兜底），但必须留下记录：
                    // 静默吞掉会让"样式配置怎么没生效"变成查不出来的问题。
                    EasyLog.print("AppStyleConfigProvider", "样式配置缓存写入失败: " + t.getMessage());
                    EasyLog.print(t);
                }
            }
        });
    }
}
