package run.yigou.gxzy.utils;

import android.text.Spanned;
import androidx.core.text.HtmlCompat;

/**
 * SpannableString工具类
 * 作者: zhs
 * 时间: 2023-07-07 23:25:18
 * 包名: run.yigou.gxzy.utils
 * 类名: SpannableStringHelper
 * 版本: 1.0
 * 描述: 处理SpannableString相关的工具方法
 */
public class SpannableStringHelper {

    /**
     * 将HTML文本转换为Spanned对象
     *
     * @param contentText HTML文本内容
     * @return Spanned对象，如果输入为空则返回null
     */
    public static Spanned getSpannableString(String contentText) {
        if (StringHelper.isEmpty(contentText)) {
            return null;
        }
        return HtmlCompat.fromHtml(contentText, HtmlCompat.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH);
    }

    /**
     * 将HTML文本转换为Spanned对象，并高亮搜索关键词
     *
     * @param searchKey   搜索关键字
     * @param contentText 搜索结果文本
     * @param colorResId  颜色参数
     * @return Spanned对象
     */
    public static Spanned getSpannableString(String searchKey, String contentText, String colorResId) {
        if (StringHelper.isEmpty(contentText)) {
            return null;
        }
        
        if (StringHelper.isEmpty(searchKey)) {
            return HtmlCompat.fromHtml(contentText, HtmlCompat.FROM_HTML_MODE_COMPACT);
        }
        
        // 构造带有背景色的 HTML 标签
        String highlightedText = createBackgroundColorSpan(colorResId, searchKey);
        // 将所有子串替换为带有背景色的 HTML 标签
        String modifiedText = contentText.replaceAll(searchKey, highlightedText);
        // 根据示例文本创建 SpannableString 对象
        return HtmlCompat.fromHtml(modifiedText, HtmlCompat.FROM_HTML_MODE_COMPACT);
    }

    /**
     * 创建带背景色的HTML标签
     *
     * @param colorString 颜色字符串
     * @param searchString 搜索字符串
     * @return 带背景色的HTML标签
     */
    private static String createBackgroundColorSpan(String colorString, String searchString) {
        // 默认的颜色字符串
        String defaultColorString = "#E0E00B";
        // 如果传入的颜色字符串不为空，则使用传入的颜色字符串
        if (!StringHelper.isEmpty(colorString)) {
            defaultColorString = colorString;
        }
        // 构造带有背景色的 HTML 标签
        return "<span style=\"background-color: " + defaultColorString + ";\">" + searchString + "</span>";
    }
}