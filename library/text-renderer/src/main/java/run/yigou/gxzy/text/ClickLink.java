package run.yigou.gxzy.text;

import android.text.style.ClickableSpan;
import android.widget.TextView;

/**
 * Tips 模块点击链接回调接口
 * 定义文本中可点击区域的点击事件回调方法
 */
public interface ClickLink {
    void clickFangLink(TextView textView, ClickableSpan clickableSpan);

    void clickYaoLink(TextView textView, ClickableSpan clickableSpan);

    void clickMingCiLink(TextView textView, ClickableSpan clickableSpan);
}