package run.yigou.gxzy.text;

import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

/**
 * 项编号渲染器
 * 
 * <p>职责：
 * <ul>
 *   <li>识别 "1、" "2、" "3、" 格式的列表编号</li>
 *   <li>应用特殊颜色高亮（蓝色 #0000FF）</li>
 * </ul>
 * 
 * <p>注意：这是业务规则，可根据需要启用/禁用
 * 
 * <p>使用示例：
 * <pre>
 * // 启用项编号渲染
 * SpannableStringBuilder ssb = TipsTextRenderer.renderText(text, clickLink);
 * ItemNumberRenderer.render(ssb);
 * 
 * // 不启用项编号渲染
 * SpannableStringBuilder ssb = TipsTextRenderer.renderText(text, clickLink);
 * // 不调用 ItemNumberRenderer
 * </pre>
 */
public class ItemNumberRenderer {
    
    /** 项编号高亮颜色：蓝色 */
    private static final int ITEM_NUMBER_COLOR = 0xFF0000FF;
    
    /** 列表分隔符 */
    private static final String DELIMITER = "、";
    
    /**
     * 渲染项编号
     * 
     * @param ssb 文本对象
     * @return true=成功渲染编号，false=未找到编号格式
     */
    public static boolean render(SpannableStringBuilder ssb) {
        if (ssb == null || ssb.length() == 0) {
            return false;
        }
        
        String text = ssb.toString();
        int delimiterIndex = text.indexOf(DELIMITER);
        
        // 检查是否包含分隔符，且分隔符前为纯数字
        if (delimiterIndex == -1 || !isNumeric(text.substring(0, delimiterIndex))) {
            return false;
        }
        
        // 应用颜色高亮
        ssb.setSpan(
            new ForegroundColorSpan(ITEM_NUMBER_COLOR),
            0,
            delimiterIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        
        return true;
    }
    
    /**
     * 检查字符串是否为纯数字
     * 
     * @param str 待检查字符串
     * @return true=纯数字，false=非纯数字
     */
    private static boolean isNumeric(String str) {
        if (str == null || str.isEmpty()) {
            return false;
        }
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isDigit(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
