package run.yigou.gxzy.utils;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;

import androidx.core.text.HtmlCompat;

/**
 * 作者:  zhs
 * 时间:  2023-07-07 23:25:18
 * 包名:  run.yigou.gxzy.utils
 * 类名:  SpannableStringHelper
 * 版本:  1.0
 * 描述:
 */
public class SpannableStringHelper {
    /**
     * searchKey 搜索关键字
     * contentText 搜索结果
     * colorResId  颜色参数
     */
    public static Spanned getSpannableString(String searchKey, String contentText, String colorResId) {


        // 构造带有背景色的 HTML 标签
        String highlightedText = createBackgroundColorSpan(colorResId, searchKey);
        // 将所有子串替换为带有背景色的 HTML 标签
        String modifiedText = contentText .replaceAll(searchKey, highlightedText);
        // 根据示例文本创建 SpannableString 对象
        return  HtmlCompat.fromHtml(modifiedText, HtmlCompat.FROM_HTML_MODE_COMPACT);

    }

    private static String createBackgroundColorSpan(String colorstring, String searchString) {

        // 默认的颜色字符串，可以根据需要进行修改
        String defaultColorString = "#E0E00B";
        // 如果传入的颜色字符串为空，则使用默认的颜色字符串
        if (!StringHelper.isEmpty(colorstring)) {
            defaultColorString = colorstring;
        }
        // 构造带有背景色的 HTML 标签
        return "<span style=\"background-color: " + defaultColorString + ";\">" + searchString + "</span>";
    }
}
