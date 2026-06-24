package run.yigou.gxzy.text;

/**
 * 文本渲染选项（Builder 模式）
 * 
 * <p>用于控制 TipsTextRenderer 的渲染行为
 * 
 * <p>使用示例：
 * <pre>
 * // 默认选项（启用项编号渲染）
 * RenderOptions.defaults()
 * 
 * // 禁用项编号渲染
 * RenderOptions.defaults().withItemNumber(false)
 * 
 * // 完整示例
 * SpannableStringBuilder ssb = TipsTextRenderer.renderText(
 *     text, 
 *     clickLink, 
 *     RenderOptions.defaults().withItemNumber(false)
 * );
 * </pre>
 */
public class RenderOptions {
    
    /** 是否启用项编号渲染（默认启用） */
    private boolean enableItemNumber = true;
    
    /**
     * 私有构造函数，强制使用 Builder 模式
     */
    private RenderOptions() {}
    
    /**
     * 创建默认选项
     * @return 默认选项（启用项编号渲染）
     */
    public static RenderOptions defaults() {
        return new RenderOptions();
    }
    
    /**
     * 设置是否启用项编号渲染
     * 
     * @param enabled true=启用，false=禁用
     * @return 当前实例（支持链式调用）
     */
    public RenderOptions withItemNumber(boolean enabled) {
        this.enableItemNumber = enabled;
        return this;
    }
    
    /**
     * 是否启用项编号渲染
     * @return true=启用，false=禁用
     */
    public boolean isItemNumberEnabled() {
        return enableItemNumber;
    }
}
