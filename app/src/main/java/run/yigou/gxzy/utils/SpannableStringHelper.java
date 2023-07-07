package run.yigou.gxzy.utils;

import android.graphics.Color;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;

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
     *  searchKey 搜索关键字
     *  contentText 搜索结果
     *  colorResId  颜色参数
     */
    public static SpannableString getSpannableString(String searchKey, String contentText, int colorResId) {
        // 根据示例文本创建 SpannableString 对象
        SpannableString spannableText = new SpannableString(contentText);
        // 查找并设置背景颜色
        int startIndex = 0;
        BackgroundColorSpan backgroundColorSpan;
        while (startIndex < contentText.length()) {
            int nextIndex = contentText.indexOf(searchKey, startIndex);
            if (nextIndex == -1) {
                break;
            }
            // 如果传入的颜色为0，则使用默认的颜色字符串
            backgroundColorSpan = createBackgroundColorSpan(colorResId);
            spannableText.setSpan(backgroundColorSpan, nextIndex, nextIndex + searchKey.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            startIndex = nextIndex + searchKey.length();
        }
        return spannableText;
    }

    private static BackgroundColorSpan createBackgroundColorSpan(int colorResId) {
        int color = colorResId;
        // 默认的颜色字符串，可以根据需要进行修改
        String defaultColorString = "#ffef9903";
        // 如果传入的颜色字符串为空，则使用默认的颜色字符串
        if (colorResId == 0) {
            color = Color.parseColor(defaultColorString);
        }
        // 解析颜色字符串并创建颜色值

        // 创建并返回 BackgroundColorSpan 对象
        return new BackgroundColorSpan(color);
    }
}
