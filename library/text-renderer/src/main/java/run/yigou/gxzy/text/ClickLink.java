package run.yigou.gxzy.text;

import android.text.style.ClickableSpan;
import android.widget.TextView;

/**
 * Tips 模块点击链接回调接口
 * 定义文本中可点击区域的点击事件回调方法
 * 
 * 扩展新链接类型示例：
 * <pre>
 * ClickLink customClickLink = new ClickLink() {
 *     &#64;Override
 *     public void onClickLink(int linkType, TextView tv, ClickableSpan span) {
 *         if (linkType == 4) {
 *             // 处理新类型（如：打开视频）
 *             openVideo(tv.getText().toString());
 *         } else {
 *             // 调用默认实现（兼容原有 3 种类型）
 *             ClickLink.super.onClickLink(linkType, tv, span);
 *         }
 *     }
 *     // ... 其他方法实现
 * };
 * </pre>
 */
public interface ClickLink {
    void clickFangLink(TextView textView, ClickableSpan clickableSpan);

    void clickYaoLink(TextView textView, ClickableSpan clickableSpan);

    void clickMingCiLink(TextView textView, ClickableSpan clickableSpan);
    
    /**
     * 通用链接点击处理器（支持动态扩展）
     * 
     * 默认实现：路由到原有 3 个方法（向后兼容）
     * 调用方可重写此方法以支持新的链接类型
     * 
     * @param linkType 链接类型（由 StyleConfig.linkType 定义）
     * @param textView 被点击的 TextView
     * @param span 被点击的 ClickableSpan
     */
    default void onClickLink(int linkType, TextView textView, ClickableSpan span) {
        // 默认实现：路由到原有方法（向后兼容）
        switch (linkType) {
            case 1: clickYaoLink(textView, span); break;
            case 2: clickFangLink(textView, span); break;
            case 3: clickMingCiLink(textView, span); break;
            // 新增类型由调用方重写此方法处理
        }
    }
}