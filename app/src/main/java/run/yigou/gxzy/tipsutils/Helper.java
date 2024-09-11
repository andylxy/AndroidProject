package run.yigou.gxzy.tipsutils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.Rect;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.core.internal.view.SupportMenu;
import androidx.core.view.ViewCompat;

import com.lxj.xpopup.XPopup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.app.AppApplication;
import run.yigou.gxzy.ui.tips.TipsWindow_Yao_BubbleAttachPopup;
import run.yigou.gxzy.ui.tips.TipsWindow_Fang_BubbleAttachPopup;


public class Helper {


    /**
     * Finds all starting positions of a substring (str2) within a given string (str).
     *
     * @param str  The main string to search within.
     * @param str2 The substring to find.
     * @return A list of starting positions of the substring within the main string.
     */
    public static ArrayList<Integer> getAllSubStringPos(String str, String str2) {
        // 初始化一个ArrayList来存储所有匹配的位置
        ArrayList<Integer> positions = new ArrayList<>();

        // 确保子串不是空字符串且主字符串不是空字符串
        if (str == null || str2 == null || str2.isEmpty() || str.isEmpty()) {
            return positions;
        }

        // 获取主字符串的长度和子串的长度
        int strLength = str.length();
        int str2Length = str2.length();

        // 从主字符串的开始位置进行查找
        int index = 0;
        while (index <= strLength - str2Length) {
            // 查找子串在当前索引位置的出现位置
            int foundIndex = str.indexOf(str2, index);

            // 如果子串没有找到，则退出循环
            if (foundIndex == -1) {
                break;
            }

            // 将找到的位置添加到结果列表中
            positions.add(foundIndex);

            // 移动索引到子串末尾的下一个字符位置，准备进行下一个查找
            index = foundIndex + str2Length;
        }

        return positions;
    }


    /**
     * Checks if a given string is numeric (consists only of digits).
     *
     * @param str The string to check.
     * @return true if the string is numeric, false otherwise.
     */
    public static boolean isNumeric(String str) {
        // 检查字符串是否为null或空
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 从字符串的末尾开始检查每个字符
        for (int i = str.length() - 1; i >= 0; i--) {
            char charAt = str.charAt(i);
            // 如果字符不是数字，则返回false
            if (charAt < '0' || charAt > '9') {
                return false;
            }
        }

        // 如果所有字符都是数字，则返回true
        return true;
    }

    /**
     * 在SpannableStringBuilder中渲染项编号，通过对特殊字符（"、"）之前的数字前缀设置颜色跨度。
     *
     * @param spannableStringBuilder 要修改的SpannableStringBuilder对象。
     */
    public static void renderItemNumber(SpannableStringBuilder spannableStringBuilder) {
        // 将SpannableStringBuilder转换为String，以便于操作
        String text = spannableStringBuilder.toString();

        // 定义作为分隔符的特殊字符
        final String DELIMITER = "、";

        // 检查文本是否包含分隔符，并且分隔符之前的子字符串是否为数字
        int delimiterIndex = text.indexOf(DELIMITER);
        if (delimiterIndex != -1 && isNumeric(text.substring(0, delimiterIndex))) {
            // 对数字前缀设置前景色跨度
            spannableStringBuilder.setSpan(
                    new ForegroundColorSpan(0xFF0000FF),  // 颜色为蓝色（十六进制表示）
                    0,  // Span的起始索引
                    delimiterIndex,  // Span的结束索引
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE  // Span标志，避免影响周围文本
            );
        }
    }
    public static SpannableStringBuilder renderText(String str, final ClickLink clickLink) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);

        while (true) {
            // 查找下一个"$"符号的位置
            int indexOf = str.indexOf("$");
            if (indexOf >= 0) {
                // 查找"{"符号的位置
                int indexOf2 = str.indexOf("}");
                // 计算"$"的数量以确定匹配的结束位置
                int size = getAllSubStringPos(str.substring(indexOf, indexOf2), "$").size();
                int i = indexOf2;
                int i2 = 1;

                // 根据找到的"$"数量来调整结束位置
                while (size > i2) {
                    for (int i3 = 0; i3 < size - i2; i3++) {
                        i += str.substring(i + 1).indexOf("}") + 1;
                    }
                    int i4 = size;
                    size = getAllSubStringPos(str.substring(indexOf, i), "$").size();
                    i2 = i4;
                }

                // 提取"$"后面的第一个字符作为标记
                String substring = str.substring(indexOf + 1, indexOf + 2);

                // 根据标记设置不同的样式
                if (substring.equals("a") || substring.equals("w")) {
                    spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (substring.equals("u")) {
                    spannableStringBuilder.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View view) {
                            clickLink.clickYaoLink((TextView) view, this);
                        }
                    }, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                if (substring.equals("f")) {
                    spannableStringBuilder.setSpan(new ClickableSpan() {
                        @Override
                        public void onClick(View view) {
                            clickLink.clickFangLink((TextView) view, this);
                        }
                    }, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                // 设置文本颜色
                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getColoredTextByStrClass(substring));
                spannableStringBuilder.setSpan(foregroundColorSpan, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                // 替换处理过的部分
                spannableStringBuilder.replace(i, i + 1, "");
                spannableStringBuilder.replace(indexOf, indexOf + 3, "");

                // 更新原始字符串为处理后的字符串
                str = spannableStringBuilder.toString();
            } else {
                // 处理完所有"$"后，进行额外的渲染
                renderItemNumber(spannableStringBuilder);
                return spannableStringBuilder;
            }
        }
    }


    public static SpannableStringBuilder renderText(String str) {
        return renderText(str, new ClickLink() {
            @Override
            public void clickYaoLink(TextView textView, ClickableSpan clickableSpan) {
              //  Helper.closeKeyboard(SingletonData.getInstance().curActivity);
                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                System.out.println("tapped:" + charSequence);
                PointF pointF =(PointF) textView.getTag();
                new XPopup.Builder(textView.getContext())
                        .isTouchThrough(true)
                       // .popupPosition(PopupPosition.Right)
                        .positionByWindowCenter(true)
                        .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
                        .isViewMode(true)
                        .atView(textView)
                        //.atPoint(pointF)
                        //.hasShadowBg(false) // 去掉半透明背景
                        .asCustom(new TipsWindow_Yao_BubbleAttachPopup(textView.getContext(),charSequence))
                        .show();
            }

            @Override
            public void clickFangLink(TextView textView, ClickableSpan clickableSpan) {
               // Helper.closeKeyboard(SingletonData.getInstance().curActivity);
                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                System.out.println("tapped:" + charSequence);
//                MotionEvent org_event =(MotionEvent) textView.getTag();
//                if (org_event!=null){
//                    Log.e("Helper-->--XPopup", "可点击的文本 (按下或抬起) mRawX : "+org_event.getRawX() +" mRawY: "+org_event.getRawY());
//                }
                new XPopup.Builder(textView.getContext())
                        .isTouchThrough(true)
                        .positionByWindowCenter(true)
                        .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
                        .isViewMode(true)
                        .atView(textView)
                        //.offsetX((int) org_event.getRawX())
                       // .offsetY((int) org_event.getRawY())
                        // .atPoint(pointF)
                        //.hasShadowBg(false) // 去掉半透明背景
                        .asCustom(
                                new TipsWindow_Fang_BubbleAttachPopup(textView.getContext(),charSequence)
                                       // .setArrowHeight(XPopupUtils.dp2px(textView.getContext(), 6f) )
                        )
                        .show();
                }

        });
    }


    private static Rect getTextRect(ClickableSpan clickableSpan, TextView textView) {
        Rect rect = new Rect(); // 用于存储文本的矩形区域
        SpannableString spannableString = (SpannableString) textView.getText(); // 获取 TextView 的文本
        Layout layout = textView.getLayout(); // 获取文本布局
        int start = spannableString.getSpanStart(clickableSpan); // 获取可点击 span 的起始位置
        int end = spannableString.getSpanEnd(clickableSpan); // 获取可点击 span 的结束位置

        int startLine = layout.getLineForOffset(start); // 获取起始位置所在的行号
        int endLine = layout.getLineForOffset(end); // 获取结束位置所在的行号
        boolean isMultiLine = startLine != endLine; // 判断 span 是否跨越多行

        layout.getLineBounds(startLine, rect); // 获取起始行的边界
        int[] location = new int[2]; // 存储 TextView 在屏幕上的位置
        textView.getLocationOnScreen(location); // 获取 TextView 在屏幕上的位置
        int scrollY = textView.getScrollY(); // 获取 TextView 的垂直滚动量
        rect.offset(0, location[1] - scrollY + textView.getCompoundPaddingTop()); // 调整矩形区域的顶部位置

        if (isMultiLine) {
            // 处理 span 跨越多行的情况
            if (rect.top > AppApplication.getmContext().getResources().getDisplayMetrics().heightPixels - rect.bottom) {
                // 如果矩形区域的底部超出屏幕高度，使用起始行的右边界
                rect.right = (int) layout.getLineRight(startLine);
            } else {
                // 否则，处理结束行的边界
                layout.getLineBounds(endLine, rect);
                rect.offset(0, location[1] - scrollY + textView.getCompoundPaddingTop()); // 调整矩形区域的顶部位置
                rect.left = (int) layout.getLineLeft(endLine); // 设置矩形区域的左边界
            }
        } else {
            // 处理 span 不跨越多行的情况
            rect.right = (int) (layout.getPrimaryHorizontal(end) - layout.getPrimaryHorizontal(start)) + rect.left; // 计算右边界
        }

        rect.left += textView.getCompoundPaddingLeft() - textView.getScrollX(); // 调整矩形区域的左边界
        return rect; // 返回最终计算的矩形区域
    }

    @SuppressLint("RestrictedApi")
    public static int getColoredTextByStrClass(String str) {
        HashMap<String, Integer> hashMap = new HashMap<>();
        hashMap.put("r", Integer.valueOf(SupportMenu.CATEGORY_MASK));
        hashMap.put("n", Color.BLUE); // 蓝色
        hashMap.put("f", Color.BLUE); // 蓝色
        hashMap.put("a", Color.parseColor("#C0C0C0")); // 浅灰色
        hashMap.put("m", Color.rgb(255, 0, 0)); // 红色
        hashMap.put("s", Color.argb(128, 0, 0, 255)); // 半透明蓝色
        hashMap.put("u", Color.rgb(77, 0, 255)); // 紫色
        hashMap.put("v", Color.rgb(77, 0, 255)); // 紫色
        hashMap.put("w", Color.rgb(0, 128, 0)); // 绿色
        hashMap.put("q", Color.rgb(61, 200, 120)); // 浅绿色
        hashMap.put("h", Color.BLACK); // 默认颜色，黑色
        hashMap.put("x", Color.parseColor("#EA8E3B")); // 橙色
        hashMap.put("y", Color.parseColor("#9A764F")); // 棕色

        Integer num = hashMap.get(str);
        return num == null ? ViewCompat.MEASURED_STATE_MASK : num.intValue();
    }

    /**
     * 根据给定的比较器在数据列表中搜索符合条件的项，并返回一个新的列表，其中包含符合条件的部分数据。
     *
     * @param list 要搜索的HH2SectionData列表。
     * @param dataItemCompare 用于比较的DataItemCompare对象。
     * @return 包含符合条件数据的HH2SectionData列表。
     */
    public static List<HH2SectionData> searchText(List<HH2SectionData> list, DataItemCompare dataItemCompare) {
        // 创建一个新的ArrayList来存储符合条件的HH2SectionData对象
        List<HH2SectionData> resultList = new ArrayList<>();

        // 遍历输入的HH2SectionData列表
        for (HH2SectionData sectionData : list) {
            // 创建一个临时ArrayList用于存储符合条件的DataItem
            List<DataItem> filteredItems = null;

            // 遍历每个HH2SectionData对象中的DataItem
            for (DataItem dataItem : sectionData.getData()) {
                // 使用比较器判断DataItem是否符合条件
                if (dataItemCompare.useThisItem(dataItem)) {
                    // 如果临时列表为空，初始化它
                    if (filteredItems == null) {
                        filteredItems = new ArrayList<>();
                    }
                    // 将符合条件的DataItem添加到临时列表中
                    filteredItems.add(dataItem);
                }
            }

            // 如果临时列表中有符合条件的数据，创建新的HH2SectionData对象并添加到结果列表中
            if (filteredItems != null) {
                resultList.add(new HH2SectionData(filteredItems, sectionData.getSection(), sectionData.getHeader()));
            }
        }

        // 返回包含符合条件数据的HH2SectionData列表
        return resultList;
    }

    /**
     * 表示一个接受类型为 T 的对象并返回布尔值的条件接口。
     *
     * @param <T> 要检查的对象的类型。
     */
    @FunctionalInterface
    public interface Condition<T> {
        /**
         * 判断给定的对象是否满足条件。
         *
         * @param t 要检查的对象。
         * @return 如果对象满足条件，则返回 true；否则返回 false。
         */
        boolean test(T t);
    }
    /**
     * 检查列表中是否有至少一个元素满足给定的条件。
     *
     * @param list 要检查的列表，包含类型为 T 的元素。
     * @param condition 判断元素是否满足条件的函数接口。
     * @param <T> 列表中元素的类型。
     * @return 如果列表中至少有一个元素满足条件，则返回 true；否则返回 false。
     */
    public static <T> boolean some(List<T> list, Condition<T> condition) {
        if (list == null) {
            return false;
        }

        // 遍历列表中的每个元素
        for (T element : list) {
            if (condition.test(element)) {
                return true;
            }
        }

        return false;
    }
    /**
     * 将 dp（密度无关像素）值转换为 px（像素）值。
     *
     * @param context 用于获取屏幕密度的上下文。
     * @param dpValue 要转换的dp值。
     * @return 转换后的像素值。
     */
    public static int dip2px(Context context, float dpValue) {
        // 获取屏幕的显示密度
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        // dp值转换为px值，并四舍五入到最接近的整数
        return (int) ((dpValue * displayMetrics.density) + 0.5f);
    }

    /**
     * 将 px（像素）值转换为 dp（密度无关像素）值。
     *
     * @param context 用于获取屏幕密度的上下文。
     * @param pxValue 要转换的像素值。
     * @return 转换后的dp值。
     */
    public static int px2dip(Context context, float pxValue) {
        // 获取屏幕的显示密度
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        // px值转换为dp值，并四舍五入到最接近的整数
        return (int) ((pxValue / displayMetrics.density) + 0.5f);
    }


    public static int getScreenWidth(Activity activity) {
        return activity.getWindow().getDecorView().getWidth();
    }

    public static int getScreenHeight(Activity activity) {
        return activity.getWindow().getDecorView().getHeight();
    }

    public static String getDateString(int i) {
        return new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis() - (((i * 24) * 3600) * 1000)));
    }

}
