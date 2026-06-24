package run.yigou.gxzy.text;

import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

/**
 * 代理 ClickableSpan，将点击事件路由到 ClickLink 的通用处理器
 * 
 * <p>设计意图：
 * <ul>
 *   <li>解耦 library 和 app 模块：library 仅负责创建代理，app 实现业务逻辑</li>
 *   <li>支持动态 linkType：通过构造函数注入，避免为每种类型创建子类</li>
 *   <li>防御性编程：空值检查和类型检查，避免崩溃</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * // 在 TipsTextRenderer.applyStyle() 中创建
 * spannableStringBuilder.setSpan(
 *     new ProxyClickableSpan(clickLink, config.linkType),
 *     start, end,
 *     Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
 * );
 * </pre>
 * 
 * @see ClickLink
 * @see TipsTextRenderer
 */
public class ProxyClickableSpan extends ClickableSpan {
    
    private final ClickLink clickLink;
    private final int linkType;
    
    /**
     * 创建代理 ClickableSpan
     * 
     * @param clickLink 点击回调接口（由 app 模块实现）
     * @param linkType 链接类型（由配置中心 StyleConfig.linkType 定义）
     */
    public ProxyClickableSpan(ClickLink clickLink, int linkType) {
        this.clickLink = clickLink;
        this.linkType = linkType;
    }
    
    @Override
    public void onClick(View view) {
        // 防御：clickLink 为空时记录日志并跳过
        if (clickLink == null) {
            android.util.Log.w("ProxyClickableSpan", 
                "clickLink 为空，linkType=" + linkType + " 的点击事件被忽略");
            return;
        }
        
        // 防御：非 TextView 安全跳过，避免 ClassCastException
        if (!(view instanceof TextView)) {
            android.util.Log.w("ProxyClickableSpan", 
                "view 不是 TextView 类型，点击事件被忽略");
            return;
        }
        
        TextView textView = (TextView) view;
        
        // 路由到通用处理器（支持动态扩展）
        clickLink.onClickLink(linkType, textView, this);
    }
    
    /**
     * 获取链接类型
     * @return linkType 值
     */
    public int getLinkType() {
        return linkType;
    }
}
