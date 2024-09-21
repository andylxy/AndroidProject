/*
 * 项目名: AndroidProject
 * 类名: Helper.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.Helper
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:38
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.hjq.http.EasyLog;
import com.lxj.xpopup.XPopup;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import run.yigou.gxzy.ui.dialog.MenuDialog;
import run.yigou.gxzy.ui.tips.TipsWindow_Fang_BubbleAttachPopup;
import run.yigou.gxzy.ui.tips.TipsWindow_MingCi_BubbleAttachPopup;
import run.yigou.gxzy.ui.tips.TipsWindow_Yao_BubbleAttachPopup;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;


public class TipsNetHelper {

    public static @NonNull ArrayList<HH2SectionData> getSearchHh2SectionData(SearchKeyEntity searchKeyEntity,Singleton_Net_Data singletonNetData) {
        // 将搜索词拆分并过滤掉空白项
        String[] searchTerms = searchKeyEntity.getSearchKeyText().split(" ");
        List<String> validSearchTerms = new ArrayList<>();
        for (String term : searchTerms) {
            if (!term.isEmpty()) {
                validSearchTerms.add(term);
            }
        }
        ArrayList<HH2SectionData> filteredData = new ArrayList<>(); // 用于保存过滤后的结果

        // 从单例数据中获取必要的映射和列表
        Map<String, String> yaoAliasDict = singletonNetData.getYaoAliasDict();
        Map<String, String> fangAliasDict = singletonNetData.getFangAliasDict();
//            List<String> allYao = SingletonData.getInstance().getAllYao();
//            List<String> allFang = SingletonData.getInstance().getAllFang();

        // 遍历数据以进行过滤
        for (HH2SectionData sectionData : singletonNetData.getContent()) {
            List<DataItem> matchedItems = new ArrayList<>(); // 用于保存当前部分中的匹配项
            boolean sectionHasMatches = false;

            // 检查当前部分中的每一个数据项
            for (DataItem dataItem : sectionData.getData()) {
                boolean itemMatched = false;

                // 检查每个搜索词
                for (String term : validSearchTerms) {
                    String sanitizedTerm = sanitizeTerm(term); // 清理搜索词
                    Pattern pattern;

                    // 从清理后的搜索词编译正则表达式
                    try {
                        pattern = Pattern.compile(sanitizedTerm);
                    } catch (Exception e) {
                        // 如果正则表达式失败，则回退到基本模式
                        pattern = Pattern.compile(".");
                    }

                    // 检查数据项是否符合搜索条件
                    if (matchDataItem(dataItem, pattern, sanitizedTerm, yaoAliasDict, fangAliasDict)) {
                        itemMatched = true;
                        // 突出显示数据项中的匹配文本
                        highlightMatchingText(dataItem, pattern);
                        break; // 一旦匹配，继续下一个数据项
                    }
                }

                // 如果有任何搜索词匹配，则加入匹配项
                if (itemMatched) {
                    matchedItems.add(dataItem.getCopy());
                    sectionHasMatches = true;
                    searchKeyEntity.setSearchResTotalNum(searchKeyEntity.getSearchResTotalNum()+1);

                }
            }

            // 如果有匹配项，则将其添加到过滤后的结果中
            if (sectionHasMatches) {
                filteredData.add(new HH2SectionData(matchedItems, sectionData.getSection(), sectionData.getHeader()));
            }
        }
        return filteredData;
    }

    /**
     * 清理搜索词，去除不必要的字符。
     */
    private static String sanitizeTerm(String term) {
        // 去除前后破折号，并替换特殊字符（如果需要）
        return term.replace("-", "").replace("#", ".");
    }

    /**
     * 根据指定的模式匹配数据项。
     */
    private static boolean matchDataItem(DataItem dataItem, Pattern pattern, String term,
                                  Map<String, String> yaoAliasDict, Map<String, String> fangAliasDict) {
        // 检查数据项的属性是否与搜索词和正则匹配
        String attributeText = dataItem.getAttributedText().toString();
        // 这里可以进一步处理别名

        // 检查主属性文本是否匹配
        return pattern.matcher(attributeText).find() || checkAliases(dataItem, term, pattern, yaoAliasDict, fangAliasDict);
    }

    /**
     * 在数据项中突出显示匹配文本。
     */
    private static void highlightMatchingText(DataItem dataItem, Pattern pattern) {
        SpannableStringBuilder spannableText = new SpannableStringBuilder(renderText(dataItem.getText()));
        Matcher matcher = pattern.matcher(spannableText);
        // todo 需要更换突出颜色变更位置点 new ForegroundColorSpan(0xFFFF0000)
        // 在 Spannable 文本中突出显示所有匹配项 0xFFFF0000 为红色
        while (matcher.find()) {
            spannableText.setSpan(new ForegroundColorSpan(0xFFFF0000), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        dataItem.setAttributedText(spannableText);
    }

    /**
     * 检查别名以寻找额外的匹配项。
     */
    private static  boolean checkAliases(DataItem dataItem, String term, Pattern pattern,
                                 Map<String, String> yaoAliasDict, Map<String, String> fangAliasDict) {
        // 检查 YaoList 和 FangList 是否与模式匹配
        for (String yao : dataItem.getYaoList()) {
            String alias = yaoAliasDict.get(yao);
            if (alias != null && pattern.matcher(alias).find()) {
                return true;
            }
        }

        for (String fang : dataItem.getFangList()) {
            String alias = fangAliasDict.get(fang);
            if (alias != null && pattern.matcher(alias).find()) {
                return true;
            }
        }

        return false;
    }





    /**
     * 查找给定字符串 (str) 中所有子字符串 (str2) 的起始位置。
     *
     * @param str  主字符串。
     * @param str2 要查找的子字符串。
     * @return 包含子字符串在主字符串中所有起始位置的列表。
     */
    public static ArrayList<Integer> getAllSubStringPos(String str, String str2) {
        // 初始化一个ArrayList来存储所有匹配的位置
        ArrayList<Integer> positions = new ArrayList<>();

        // 确保主字符串和子字符串都不是null，并且子字符串不为空
        if (str == null || str2 == null || str2.isEmpty() || str.isEmpty()) {
            return positions;
        }

        // 获取主字符串的长度和子字符串的长度
        int strLength = str.length();
        int str2Length = str2.length();

        // 初始化搜索索引
        int index = 0;

        // 在主字符串中查找子字符串
        while (index <= strLength - str2Length) {
            // 从当前索引位置查找子字符串
            int foundIndex = str.indexOf(str2, index);

            // 如果没有找到子字符串，退出循环
            if (foundIndex == -1) {
                break;
            }

            // 将找到的位置添加到结果列表中
            positions.add(foundIndex);

            // 移动索引到子字符串末尾的下一个位置，继续查找
            index = foundIndex + str2Length;
        }

        return positions;
    }



    /**
     * 检查给定的字符串是否为数字（仅由数字组成）。
     *
     * @param str 要检查的字符串。
     * @return 如果字符串是数字，则返回 true；否则返回 false。
     */
    public static boolean isNumeric(String str) {
        // 检查字符串是否为 null 或空字符串
        if (str == null || str.isEmpty()) {
            return false;
        }

        // 从字符串的末尾开始检查每个字符
        for (int i = str.length() - 1; i >= 0; i--) {
            char charAt = str.charAt(i);
            // 如果字符不是数字，则返回 false
            if (charAt < '0' || charAt > '9') {
                return false;
            }
        }

        // 如果所有字符都是数字，则返回 true
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
//    public static SpannableStringBuilder renderText(String str, final ClickLink clickLink) {
//        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);
//
//        while (true) {
//            // 查找下一个"$"符号的位置
//            int indexOf = str.indexOf("$");
//            if (indexOf >= 0) {
//                // 查找"{"符号的位置
//                int indexOf2 = str.indexOf("}");
//                // 计算"$"的数量以确定匹配的结束位置
//                int size = getAllSubStringPos(str.substring(indexOf, indexOf2), "$").size();
//                int i = indexOf2;
//                int i2 = 1;
//
//                // 根据找到的"$"数量来调整结束位置
//                while (size > i2) {
//                    for (int i3 = 0; i3 < size - i2; i3++) {
//                        i += str.substring(i + 1).indexOf("}") + 1;
//                    }
//                    int i4 = size;
//                    size = getAllSubStringPos(str.substring(indexOf, i), "$").size();
//                    i2 = i4;
//                }
//
//                // 提取"$"后面的第一个字符作为标记
//                String substring = str.substring(indexOf + 1, indexOf + 2);
//
//                // 根据标记设置不同的样式
//                if (substring.equals("a") || substring.equals("w")||  substring.equals("r")) {
//                    spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                }
//                if (substring.equals("u")) {
//                    spannableStringBuilder.setSpan(new ClickableSpan() {
//                        @Override
//                        public void onClick(View view) {
//                            clickLink.clickYaoLink((TextView) view, this);
//                        }
//                    }, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                }
//                if (substring.equals("f")) {
//                    spannableStringBuilder.setSpan(new ClickableSpan() {
//                        @Override
//                        public void onClick(View view) {
//                            clickLink.clickFangLink((TextView) view, this);
//                        }
//                    }, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                }
//                if (substring.equals("g")) {
//                    spannableStringBuilder.setSpan(new ClickableSpan() {
//                        @Override
//                        public void onClick(View view) {
//                            clickLink.clickMingCiLink((TextView) view, this);
//                        }
//                    }, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//                }
//                // 设置文本颜色
//                ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getColoredTextByStrClass(substring));
//                spannableStringBuilder.setSpan(foregroundColorSpan, indexOf + 3, i, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//
//                // 替换处理过的部分
//                spannableStringBuilder.replace(i, i + 1, "");
//                spannableStringBuilder.replace(indexOf, indexOf + 3, "");
//
//                // 更新原始字符串为处理后的字符串
//                str = spannableStringBuilder.toString();
//            } else {
//                // 处理完所有"$"后，进行额外的渲染
//                renderItemNumber(spannableStringBuilder);
//                return spannableStringBuilder;
//            }
//        }
//    }



    //todo 所有的SpannableStringBuilder 都是在这里处理的.
    //如果要改变样式，需要修改这里，同时修改renderItemNumber()
    public static SpannableStringBuilder renderText(String str, final ClickLink clickLink) {
        // 创建一个可变的SpannableStringBuilder用于渲染文本
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(str);

        while (true) {
            // 找到下一个"$"符号的位置
            int indexOf = str.indexOf("$");
            if (indexOf >= 0) {
                // 找到下一个"}"符号的位置
                int indexOf2 = str.indexOf("}", indexOf);
                if (indexOf2 == -1) break; // 如果没有找到"}"，则退出循环

                // 计算"$"符号的数量
                int size = getAllSubStringPos(str.substring(indexOf, indexOf2), "$").size();
                int i = indexOf2; // 初始化i为"}"符号的位置
                int i2 = 1; // 初始化i2为1，用于跟踪"$"数量

                // 根据"$"的数量调整结束位置
                while (size > i2) {
                    for (int j = 0; j < size - i2; j++) {
                        // 更新i为下一个"}"符号的位置
                        i += str.substring(i + 1).indexOf("}") + 1;
                    }
                    int currentSize = size; // 保存当前的大小
                    size = getAllSubStringPos(str.substring(indexOf, i), "$").size(); // 更新size
                    i2 = currentSize; // 更新i2为当前大小
                }

                // 提取"$"后面的字符
                String marker = str.substring(indexOf + 1, indexOf + 2);

                // 根据标记应用不同的样式
                applyStyle(spannableStringBuilder, marker, indexOf, i, clickLink);

                // 将处理过的部分替换为空字符串
                spannableStringBuilder.replace(i, i + 1, "");
                spannableStringBuilder.replace(indexOf, indexOf + 3, "");

                // 更新原始字符串为修改后的字符串
                str = spannableStringBuilder.toString();
            } else {
                // 处理完所有"$"后执行其他渲染
                renderItemNumber(spannableStringBuilder);
                break; // 跳出循环
            }
        }

        return spannableStringBuilder; // 返回最终的SpannableStringBuilder
    }


    // 根据标记应用样式的方法
    private static void applyStyle(SpannableStringBuilder spannableStringBuilder, String marker, int start, int end, final ClickLink clickLink) {
        // 根据不同的标记应用样式
        switch (marker) {
            case "a":
            case "w":
            case "r":
                // 设置相对字体大小
                spannableStringBuilder.setSpan(new RelativeSizeSpan(0.7f), start + 3, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case "u":
                // 设置点击事件处理
                spannableStringBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        clickLink.clickYaoLink((TextView) view, this); // 处理点击
                    }
                }, start + 3, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case "f":
                // 设置另一个点击事件处理
                spannableStringBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        clickLink.clickFangLink((TextView) view, this); // 处理点击
                    }
                }, start + 3, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case "g":
                // 设置另一个点击事件处理
                spannableStringBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        clickLink.clickMingCiLink((TextView) view, this); // 处理点击
                    }
                }, start + 3, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
        }
        // 设置文本颜色
        ForegroundColorSpan foregroundColorSpan = new ForegroundColorSpan(getColoredTextByStrClass(marker));
        spannableStringBuilder.setSpan(foregroundColorSpan, start + 3, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    // 复制内容到剪贴板
    public static void copyToClipboard(Context context, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "已复制到剪贴板" , Toast.LENGTH_SHORT).show();
    }
    // 创建 MenuDialog 实例
    public static MenuDialog.Builder menuDialogBuilder;
    // 同时初始化数据
    private static List<String> data = Arrays.asList("拷贝本条"/*, "拷贝本章全部内容", "拷贝全部结果"*/);

    // 初始化方法
    public static void initDialog(Context context) {
        if (menuDialogBuilder == null) {
            menuDialogBuilder = new MenuDialog.Builder(context).setList(data);
        }
    }

    public static SpannableStringBuilder renderText(String str) {
        return renderText(str, new ClickLink() {
            @Override
            public void clickYaoLink(TextView textView, ClickableSpan clickableSpan) {

                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                EasyLog.print("tapped:" + charSequence);
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

                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                EasyLog.print("tapped:" + charSequence);
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

            /**
             * @param textView
             * @param clickableSpan
             */
            @Override
            public void clickMingCiLink(TextView textView, ClickableSpan clickableSpan) {

                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                EasyLog.print("tapped:" + charSequence);
                new XPopup.Builder(textView.getContext())
                        .isTouchThrough(true)
                        // .popupPosition(PopupPosition.Right)
                        .positionByWindowCenter(true)
                        .isDestroyOnDismiss(true) //对于只使用一次的弹窗，推荐设置这个
                        .isViewMode(true)
                        .atView(textView)
                        //.atPoint(pointF)
                        //.hasShadowBg(false) // 去掉半透明背景
                        .asCustom(new TipsWindow_MingCi_BubbleAttachPopup(textView.getContext(),charSequence))
                        .show();
            }

        });

    }


    private static final HashMap<String, Integer> colorMap = new HashMap<>();

    static {
// 初始化颜色映射
        colorMap.put("r", Color.RED); // 红色
        colorMap.put("n", Color.BLUE); // 蓝色
        colorMap.put("f", Color.BLUE); // 蓝色
        colorMap.put("a", Color.GRAY); // 灰色
        colorMap.put("m", Color.RED); // 红色
        colorMap.put("g", Color.argb(230, 0, 128, 255)); // 半透明蓝色
        colorMap.put("u", Color.BLUE); // 蓝色
        colorMap.put("v", Color.BLUE); // 蓝色
        colorMap.put("w", Color.rgb(28,181,92)); // 绿色
        colorMap.put("q", Color.rgb(61, 200, 120)); // 自定义绿色
        colorMap.put("h", Color.BLACK); // 黑色
        colorMap.put("x", Color.parseColor("#EA8E3B")); // 自定义橙色
        colorMap.put("y", Color.parseColor("#9A764F")); // 自定义棕色

    }

    /**
     * 根据输入的字符串获取对应的颜色值。
     *
     * @param s 输入的字符串，表示颜色的键
     * @return 对应的颜色值，如果找不到则返回黑色
     */
    public static int getColoredTextByStrClass(String s) {
        Integer colorValue = colorMap.get(s);
        return colorValue != null ? colorValue : Color.BLACK;
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
