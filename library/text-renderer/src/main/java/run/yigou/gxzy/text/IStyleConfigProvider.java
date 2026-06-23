package run.yigou.gxzy.text;

import androidx.lifecycle.LifecycleOwner;

/**
 * 样式配置提供者接口
 * 由 app 模块实现，实现依赖倒置原则
 * 
 * 架构设计：
 * - library 模块定义接口（不依赖 app）
 * - app 模块实现接口（使用 StyleConfigApi 请求服务端）
 * - TipsTextRenderConfig 通过接口调用（运行时绑定）
 * 
 * 使用示例：
 * <pre>
 * // 1. app 模块实现此接口
 * public class AppStyleConfigProvider implements IStyleConfigProvider {
 *     @Override
 *     public boolean loadConfig() {
 *         // 异步请求 StyleConfigApi
 *         EasyHttp.post(new StyleConfigApi())
 *             .request(new EasyCallBack&lt;StyleConfigApi.StyleConfigApiBean&gt;() {
 *                 @Override
 *                 public void onSuccess(StyleConfigApi.StyleConfigApiBean response) {
 *                     TipsTextRenderConfig.getInstance().applyServerConfigGeneric(...);
 *                 }
 *             });
 *         return true;
 *     }
 *     
 *     @Override
 *     public boolean isConfigLoaded() { return isLoaded; }
 *     
 *     @Override
 *     public int getConfigVersion() { return version; }
 * }
 * 
 * // 2. Application.onCreate() 中注册
 * TipsTextRenderConfig.getInstance().setProvider(new AppStyleConfigProvider());
 * 
 * // 3. 首次渲染时自动触发加载
 * TipsTextRenderer.renderText("$r{文本}", clickLink);
 * </pre>
 */
public interface IStyleConfigProvider {
    /**
     * 加载配置（从服务端或本地缓存）
     * 
     * 此方法应为异步操作，立即返回 true
     * 配置加载完成后通过 TipsTextRenderConfig.applyServerConfig() 更新
     * 
     * @return true=已触发加载流程，false=加载失败
     */
    boolean loadConfig(LifecycleOwner lifecycleOwner);
    
    /**
     * 检查配置是否已加载
     * @return true=已加载（可使用服务端配置），false=未加载（将使用默认配置）
     */
    boolean isConfigLoaded();
    
    /**
     * 获取配置版本号
     * @return 配置版本号（0=未加载）
     */
    int getConfigVersion();
}
