package run.yigou.gxzy.config;

import androidx.lifecycle.LifecycleOwner;

import com.hjq.http.EasyHttp;
import com.hjq.http.listener.HttpCallback;

import run.yigou.gxzy.data.remote.api.StyleConfigApi;
import run.yigou.gxzy.data.remote.model.HttpData;
import run.yigou.gxzy.text.IStyleConfigProvider;
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
    
    @Override
    public boolean loadConfig(LifecycleOwner lifecycleOwner) {
        // 异步请求服务端配置（Application 不是 LifecycleOwner，传 null 表示不感知生命周期）
        EasyHttp.post(lifecycleOwner)
            .api(new StyleConfigApi())
            .request(new HttpCallback<HttpData<StyleConfigApi.StyleConfigApiBean>>(null) {
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
                    TipsTextRenderConfig.getInstance().applyServerConfig(response.getStyles());
                    
                    // 保存到缓存（供下次启动使用）
                    saveCacheConfig(TipsTextRenderConfig.getInstance().getAllConfig());
                    
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
     * 从本地缓存加载配置
     * 
     * 参考 loadBookNavigation() 模式：
     * - 优先使用本地缓存
     * - 缓存无数据时不主动请求网络，等待后续触发
     * 
     * @return true=缓存加载成功，false=无缓存或加载失败
     */
    public boolean loadCacheConfig() {
        try {
            // 从缓存文件读取配置
            Serializable cachedData = run.yigou.gxzy.utils.CacheHelper.readObject("style_config_cache");
            if (cachedData == null) {
                return false;
            }
            
            Map<String, TipsTextRenderConfig.StyleConfig> cachedConfigs = 
                (Map<String, TipsTextRenderConfig.StyleConfig>) cachedData;
            
            if (cachedConfigs.isEmpty()) {
                return false;
            }
            
            // 应用到配置中心
            TipsTextRenderConfig.getInstance().updateStyleConfig(cachedConfigs);
            
            // 标记配置已加载
            isLoaded = true;
            version++;
            
            return true;
        } catch (Exception e) {
            // 缓存加载失败，静默处理
            return false;
        }
    }
    
    /**
     * 保存配置到缓存
     * 
     * @param configs 样式配置
     */
    public void saveCacheConfig(Map<String, TipsTextRenderConfig.StyleConfig> configs) {
        try {
            run.yigou.gxzy.utils.CacheHelper.saveObject((java.io.Serializable) configs, "style_config_cache");
        } catch (Exception e) {
            // 缓存保存失败，静默处理
        }
    }
}
