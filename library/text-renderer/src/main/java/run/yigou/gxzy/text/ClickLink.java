package run.yigou.gxzy.text;

import android.text.style.ClickableSpan;
import android.widget.TextView;

/**
 * Tips 模块点击链接回调接口
 * 定义文本中可点击区域的统一点击事件回调
 * 
 * <p>设计原则：
 * <ul>
 *   <li>单一入口：仅保留 onClickLink 方法</li>
 *   <li>配置驱动：linkType 由配置中心 StyleConfig.linkType 定义</li>
 *   <li>易于扩展：新增类型只需在实现类中处理新 linkType</li>
 * </ul>
 * 
 * <p>使用示例：
 * <pre>
 * ClickLink customClickLink = new ClickLink() {
 *     &#64;Override
 *     public void onClickLink(int linkType, TextView tv, ClickableSpan span) {
 *         switch (linkType) {
 *             case 1:  // 药物
 *                 handleYaoClick(tv, span);
 *                 break;
 *             case 2:  // 方剂
 *                 handleFangClick(tv, span);
 *                 break;
 *             case 5:  // 视频（新增类型）
 *                 handleVideoClick(tv);
 *                 break;
 *         }
 *     }
 * };
 * </pre>
 */
public interface ClickLink {
    /**
     * 统一链接点击处理器（配置驱动）
     * 
     * @param linkType 链接类型（由 StyleConfig.linkType 定义）
     *                 <ul>
     *                   <li>1 = 药物</li>
     *                   <li>2 = 方剂</li>
     *                   <li>3 = 名词</li>
     *                   <li>4 = 汉制单位</li>
     *                   <li>5+ = 预留扩展（视频/音频/图片等）</li>
     *                 </ul>
     * @param textView 被点击的 TextView
     * @param span 被点击的 ClickableSpan
     */
    void onClickLink(int linkType, TextView textView, ClickableSpan span);
}