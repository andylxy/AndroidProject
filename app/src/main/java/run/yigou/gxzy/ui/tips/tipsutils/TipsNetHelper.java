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
import android.graphics.Rect;
import android.os.Build;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import androidx.annotation.NonNull;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import run.yigou.gxzy.ui.dialog.MenuDialog;
import run.yigou.gxzy.ui.tips.entity.GroupEntity;
import run.yigou.gxzy.ui.tips.entity.GroupModel;
import run.yigou.gxzy.ui.tips.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.tips.DataBeans.Fang;
import run.yigou.gxzy.ui.tips.DataBeans.Yao;
import run.yigou.gxzy.ui.tips.widget.TipsLittleMingCiViewWindow;
import run.yigou.gxzy.ui.tips.widget.TipsLittleTableViewWindow;
import run.yigou.gxzy.ui.tips.widget.TipsLittleTextViewWindow;

public class TipsNetHelper {
    private static final List<String> validSearchTerms = new ArrayList<>();

    public static @NonNull ArrayList<HH2SectionData> getSearchHh2SectionData(SearchKeyEntity searchKeyEntity,
                                                                             SingletonNetData singletonNetData) {
        if (singletonNetData.getContent().size() == 1) {
            searchKeyEntity.getSearchKeyText().append(",未见");
        }

        // 将搜索词拆分并过滤掉空白项
        String[] searchTerms = searchKeyEntity.getSearchKeyText().toString().split("[,.。，]");
        validSearchTerms.clear();

        for (String term : searchTerms) {
            if (!term.isEmpty()) {
                validSearchTerms.add(term);
            }
        }
        ArrayList<HH2SectionData> filteredData = new ArrayList<>(); // 用于保存过滤后的结果

        // 从单例数据中获取必要的映射和列表
        Map<String, String> yaoAliasDict = singletonNetData.getYaoAliasDict();
        Map<String, String> fangAliasDict = singletonNetData.getFangAliasDict();

        // 遍历数据以进行过滤
        int index = 0;
        for (HH2SectionData sectionData : singletonNetData.getContent()) {
            List<DataItem> matchedItems = new ArrayList<>(); // 用于保存当前部分中的匹配项
            boolean sectionHasMatches = false;

            // 检查当前部分中的每一个数据项
            for (DataItem dataItem : sectionData.getData()) {
                boolean itemMatched = false;
                DataItem dataItem2 = dataItem.getCopy();
                dataItem2.setGroupPosition(index);

                // 检查每个搜索词
                for (String term : validSearchTerms) {
                    String sanitizedTerm = sanitizeTerm(term); // 清理搜索词
                    Pattern pattern = getPattern(sanitizedTerm);

                    // 检查数据项是否符合搜索条件
                    if (matchDataItem(dataItem2, pattern, yaoAliasDict, fangAliasDict)) {
                        itemMatched = true;
                        // 突出显示数据项中的匹配文本
                        createSingleDataCopy(dataItem2, pattern);
                        break; // 一旦匹配，继续下一个数据项
                    }
                }

                // 如果有任何搜索词匹配，则加入匹配项
                if (itemMatched) {
                    matchedItems.add(dataItem2);
                    sectionHasMatches = true;
                    searchKeyEntity.setSearchResTotalNum(searchKeyEntity.getSearchResTotalNum() + 1);
                }
            }
            // 更新索引
            index++;
            // 如果有匹配项，则将其添加到过滤后的结果中
            if (sectionHasMatches) {
                filteredData.add(new HH2SectionData(matchedItems, sectionData.getSection(), sectionData.getHeader()));
            }
        }
        return filteredData;
    }

    public static @NonNull Pattern getPattern(String sanitizedTerm) {
        Pattern pattern;

        // 从清理后的搜索词编译正则表达式
        try {
            pattern = Pattern.compile(sanitizedTerm);
        } catch (Exception e) {
            // 记录错误日志，并使用默认正则表达式
            EasyLog.print("Error compiling regex: " + sanitizedTerm + ". Fallback to default.");
            pattern = Pattern.compile(".");
        }
        return pattern;
    }


    /**
     * 获取ClickableSpan在TextView中的矩形区域
     * 此方法用于确定可点击文本在TextView中的准确位置，以便于处理点击事件时能够知道用户是否点击了可点击区域
     *
     * @param clickableSpan 可点击的Span，用于确定可点击文本的范围
     * @param textView      TextView组件，用于获取文本及其布局信息
     * @return Rect对象，表示可点击文本在TextView中的矩形区域
     * @throws IllegalArgumentException 如果传入的textView或clickableSpan为null，则抛出此异常
     */
    public static Rect getTextRect(ClickableSpan clickableSpan, TextView textView) {
        // 检查参数是否为null
        if (textView == null || clickableSpan == null) {
            throw new IllegalArgumentException("textView and clickableSpan cannot be null");
        }

        // 初始化矩形
        Rect textRect = new Rect();
        // 获取TextView的文本
        SpannableString spannableString = (SpannableString) textView.getText();
        // 获取文本布局
        Layout textLayout = textView.getLayout();
        // 获取可点击区域的起始位置
        int spanStartIndex = Math.max(0, spannableString.getSpanStart(clickableSpan));
        // 获取可点击区域的结束位置
        int spanEndIndex = Math.min(spannableString.length(), spannableString.getSpanEnd(clickableSpan));
        // 获取起始位置的水平坐标
        float startX = textLayout.getPrimaryHorizontal(spanStartIndex);
        // 更准确地获取结束位置的水平坐标
        float endX = textLayout.getSecondaryHorizontal(spanEndIndex);
        // 获取起始位置所在行
        int startLine = textLayout.getLineForOffset(spanStartIndex);
        // 获取结束位置所在行
        int endLine = textLayout.getLineForOffset(spanEndIndex);
        // 判断起始和结束是否在同一行
        boolean isMultiLine = startLine != endLine;
        // 获取起始行的边界
        textLayout.getLineBounds(startLine, textRect);

        // 初始化数组用于存储位置
        int[] textViewPosition = {0, 0};
        // 尝试获取TextView在屏幕上的位置
        try {
            textView.getLocationOnScreen(textViewPosition);
        } catch (Exception e) {
            // 如果获取位置失败，抛出运行时异常
            throw new RuntimeException("Failed to get location on screen", e);
        }

        // 计算Y轴的偏移
        float scrollY = calculateScrollY(textView, textViewPosition);
        // 更新矩形的上边界
        textRect.top += scrollY;
        // 更新矩形的下边界
        textRect.bottom += scrollY;

        // 如果起始和结束位置不在同一行，需要特殊处理
        if (isMultiLine) {
            Rect endLineRect = new Rect();
            // 获取结束行的边界
            textLayout.getLineBounds(endLine, endLineRect);
            // 更新结束行矩形的上边界
            endLineRect.top += scrollY;
            // 更新结束行矩形的下边界
            endLineRect.bottom += scrollY;

            // 根据显示情况调整起始X坐标
            if (textRect.top > ((WindowManager) textView.getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight() - textRect.bottom) {
                startX = textLayout.getLineRight(startLine);
            } else {
                startX = textLayout.getLineLeft(endLine);
            }
        }

        // 更新矩形的左边界
        textRect.left += (textViewPosition[0] + startX + textView.getCompoundPaddingLeft() - textView.getScrollX());
        // 更新矩形的右边界
        textRect.right = (int) (textRect.left + (endX - startX));
        // 返回计算后的矩形
        return textRect;
    }


    private static float calculateScrollY(TextView textView, int[] textViewPosition) {
        if (textView == null) {
            return Float.NaN; // 返回 NaN 表示无效值
        }
        if (textViewPosition == null || textViewPosition.length < 2) {
            return Float.NaN; // 返回 NaN 表示无效值
        }
        // 计算滚动偏移量
        int positionY = textViewPosition[1]; // 文本视图的位置 Y 坐标
        int scrollY = textView.getScrollY(); // 当前滚动的 Y 偏移量
        int paddingTop = textView.getCompoundPaddingTop(); // 上方内边距
        return positionY - scrollY + paddingTop;
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
    private static boolean matchDataItem(DataItem dataItem, Pattern pattern,
                                         Map<String, String> yaoAliasDict, Map<String, String> fangAliasDict) {
        // 检查数据项的属性是否与搜索词和正则匹配
        String attributeText = dataItem.getAttributedText().toString();
        // 这里可以进一步处理别名
        if(dataItem.getName()!=null)  return  pattern.matcher(dataItem.getName()).find()|| pattern.matcher(attributeText).find() || checkAliases(dataItem, pattern, yaoAliasDict, fangAliasDict);
                // 检查主属性文本是否匹配
        return pattern.matcher(attributeText).find() || checkAliases(dataItem, pattern, yaoAliasDict, fangAliasDict);
    }

    /**
     * 高亮匹配的文本
     * 此方法用于在DataItem的文本、注释和视频部分中高亮显示符合给定模式的文本
     * 它通过使用正则表达式匹配来查找需要高亮显示的部分，并应用格式化使其突出显示
     *
     * @param dataItem 要处理的DataItem对象，包含文本、注释和视频部分
     * @param pattern  用于匹配的Pattern对象，如果为null将被处理而不是抛出异常
     */
    public static void createSingleDataCopy(DataItem dataItem, Pattern pattern) {
        // 检查模式是否为null，如果是null则直接返回
        if (pattern == null) {
            return;
        }

        // 使用修改后的方法
        SpannableStringBuilder spannableText = createSpannable(dataItem.getText());
        SpannableStringBuilder spannableNote = createSpannable(dataItem.getNote());
        SpannableStringBuilder spannableSectionVideo = createSpannable(dataItem.getSectionvideo());

        // 创建Matcher对象，用于在各个部分中匹配模式
        Matcher matcherText = pattern.matcher(spannableText);
        Matcher matcherNote = pattern.matcher(spannableNote);
        Matcher matcherSectionVideo = pattern.matcher(spannableSectionVideo);

        // 对匹配到的文本应用高亮显示
        highlightMatches(matcherText, spannableText);
        highlightMatches(matcherNote, spannableNote);
        highlightMatches(matcherSectionVideo, spannableSectionVideo);

        // 将高亮显示后的文本设置回DataItem对象
        dataItem.setAttributedText(spannableText);
        dataItem.setAttributedNote(spannableNote);
        dataItem.setAttributedSectionVideo(spannableSectionVideo);
    }

    // 创建SpannableStringBuilder对象，用于渲染DataItem的文本、注释和视频部分
    public static SpannableStringBuilder createSpannable(String text) {
        // 当输入的文本为null时，返回一个包含空字符串的SpannableStringBuilder对象
        if (text == null) {
            return new SpannableStringBuilder("");
        }
        // 返回渲染后的文本的SpannableStringBuilder对象
        return new SpannableStringBuilder(renderText(text));
    }


    /**
     * 高亮匹配项
     * 此方法用于在SpannableStringBuilder中查找匹配项，并将它们高亮显示为红色
     *
     * @param matcher   用于查找匹配项的Matcher对象
     * @param spannable 要进行高亮显示的SpannableStringBuilder对象
     */
    private static void highlightMatches(Matcher matcher, SpannableStringBuilder spannable) {
        // 定义高亮显示的颜色为红色
        int color = 0xFFFF0000; // 红色
        // 遍历所有匹配项并应用高亮
        while (matcher.find()) {
            // 设置文本高亮颜色，从匹配项开始位置到结束位置（不包含结束位置）
            spannable.setSpan(new ForegroundColorSpan(color), matcher.start(), matcher.end(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    /**
     * 检查别名以寻找额外的匹配项。
     */

    private static boolean checkAliases(DataItem dataItem, Pattern pattern,
                                        Map<String, String> yaoAliasDict, Map<String, String> fangAliasDict) {
        // 空指针检查
        if (yaoAliasDict == null || fangAliasDict == null) {
            return false;
        }

        // 提取公共方法
        return checkList(dataItem.getYaoList(), yaoAliasDict, pattern) ||
                checkList(dataItem.getFangList(), fangAliasDict, pattern);
    }

    private static boolean checkList(Iterable<String> list, Map<String, String> aliasDict, Pattern pattern) {
        if (list == null) {
            return false;
        }

        for (String item : list) {
            String alias = aliasDict.get(item);
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

    //todo 所有的SpannableStringBuilder 都是在这里处理的.
    //如果要改变样式，需要修改这里，同时修改renderItemNumber()
    public static SpannableStringBuilder renderText(String str, final ClickLink clickLink) {
        // 如果输入为 null，返回一个带有默认内容的 SpannableStringBuilder
        if (str == null) {
            // EasyLog.print("renderText default content: Null ");
            return new SpannableStringBuilder();
        }
        // 创建 SpannableStringBuilder 并初始化
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
        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }


    // 同时初始化数据
    private static List<String> data = Arrays.asList("拷贝内容", "跳转到本章内容"/*, "拷贝全部结果"*/);

    // 初始化方法
    public static MenuDialog.Builder showListDialog(Context context) {

        return new MenuDialog.Builder(context).setList(data);

    }

    /**
     * 创建并返回一个单例数据副本，该副本包含明词列表的引用
     * 此方法用于在不直接修改原始单例数据的情况下，获取一个含有指定明词列表的新单例数据实例
     * 它通过深拷贝当前单例数据的别名字典，以及设置新的明词列表内容来实现
     *
     * @param mingCiList 明词列表，用于设置到新的单例数据副本中，不能为空或空列表
     * @return 返回一个包含明词列表的新单例数据副本
     * @throws IllegalArgumentException 如果提供的明词列表为空或为null
     * @throws IllegalStateException    如果无法获取单例数据实例
     */
    public static SingletonNetData createSingleDataCopy(List<HH2SectionData> mingCiList) {
        // 检查输入的明词列表是否为空或为null，如果满足条件，则抛出异常
        if (mingCiList == null || mingCiList.isEmpty()) {
            throw new IllegalArgumentException("mingCiList cannot be null or empty");
        }

        // 生成一个书本数据实例
        SingletonNetData singletonData = SingletonNetData.getInstance();

        // 深拷贝当前的书籍的药和药方数据
        SingletonNetData singletonDataInstance = TipsSingleData.getInstance().getCurSingletonData();

        // 如果当前单例数据实例不为null，则复制别名字典
        if (singletonDataInstance != null) {
            copyAliasDictionaries(singletonData, singletonDataInstance);
            EasyLog.print("实例别名字典复制成功");
        } else {
            // 如果当前单例数据实例为null，则打印日志，表示无法设置别名字典
            EasyLog.print("实例为null，无法设置别名字典");
        }

        // 设置新的明词列表内容到单例数据实例中
        singletonData.setContent(mingCiList);

        // 成功设置内容后，打印日志
        // EasyLog.print("成功设置内容");

        // 返回包含新明词列表的单例数据副本
        return singletonData;
    }

    private static void copyAliasDictionaries(SingletonNetData target, SingletonNetData source) {
        target.setYaoAliasDict(new HashMap<>(source.getYaoAliasDict()));
        target.setFangAliasDict(new HashMap<>(source.getFangAliasDict()));
    }

    /**
     * 过滤出符合条件的房产信息
     * 该方法用于从一个数据列表中筛选出特定类型的房产（Fang），并进一步根据提供的字符串参数筛选出包含该字符串的房产信息
     *
     * @param sectionData 一个包含DataItem类型数据的列表，该列表可能包含各种类型的DataItem，包括房产信息
     * @param finalStr1   用于筛选房产信息的字符串，通常代表某个关键词或标识
     * @return 返回一个包含符合条件的房产信息的列表如果输入为空或没有找到符合条件的房产信息，返回空列表
     * <p>
     * 注意：该方法在处理数据时，会根据系统的Android版本选择不同的处理方式，以利用Java 8及更高版本的流式处理特性
     */
    public static List<? extends DataItem> filterFang(List<? extends DataItem> sectionData, String finalStr1) {
        // 检查输入参数是否为空，如果任一参数为空，则返回空列表
        if (sectionData == null || finalStr1 == null) {
            return Collections.emptyList();
        }

        // 初始化数据列表，根据Android版本使用不同的方式初始化
        List<? extends DataItem> dataItems = null;
        // 当Android版本为N或更高时，使用Optional进行空值检查，并返回空列表作为默认值
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            dataItems = Optional.ofNullable(sectionData)
                    .orElse(Collections.emptyList());
        }

        // 再次检查Android版本，以确定是否可以使用Java 8的流式处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // 使用流式处理过滤和转换数据列表，筛选出符合条件的房产信息
            return dataItems.stream()
                    .filter(dataItem -> dataItem instanceof Fang)
                    .map(Fang.class::cast)
                    .filter(fang -> fang.hasYao(finalStr1))
                    .collect(Collectors.toList());
        }
        // 如果Android版本不满足条件，返回null
        return null;
    }


    /**
     * 根据给定的药物名称获取SpannableStringBuilder对象，该对象包含了与药物相关的文本信息。
     * 这些信息包括药物自身的详细描述和包含该药物的方剂信息。
     *
     * @param str 药物名称，用于查询和生成相关信息。
     * @return SpannableStringBuilder 包含药物详细信息和相关方剂信息的SpannableStringBuilder对象。
     */
    public static SpannableStringBuilder getSpanString(String str) {
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

        Map<String, String> yaoAliasDict = TipsSingleData.getInstance().getYaoAliasDict();

        // 获取药物的别名，若无则保留原名
        String alias = yaoAliasDict.get(str);
        str = alias != null ? alias : str;

        // 仅在不限制方剂显示时进行处理
        //   if (!onlyShowRelatedFang()) {
        if (true) {
            // 遍历药物数据
            // 空值检查
            if (TipsSingleData.getInstance().getYaoMap() == null) {
                throw new IllegalArgumentException("tipsSingleData or yaoMap is null");
            }
            Map<String, Yao> yaoMap = TipsSingleData.getInstance().getYaoMap();
            Yao yaoData = yaoMap.get(str);
            if (yaoData == null) {
                // 处理 yaoData 为 null 的情况
                return spannableStringBuilder.append((CharSequence) TipsNetHelper.renderText("$r{药物未找到资料}"));
            }

            // 获取药物名称并查找别名
            String yaoName = yaoData.getName();
            String yaoAlias = yaoAliasDict.get(yaoName);
            yaoName = yaoAlias != null ? yaoAlias : yaoName;

            // 如果药物名称匹配，则添加其相关信息
            if (Objects.equals(yaoName, str)) {
                spannableStringBuilder.append((CharSequence) yaoData.getAttributedText());
            }

            // 如果没有找到相关药物，添加提示信息
            if (spannableStringBuilder.length() == 0) {
                spannableStringBuilder.append((CharSequence) TipsNetHelper.renderText("$r{药物未找到资料}"));
            }
            spannableStringBuilder.append("\n\n");
        }

        // 处理方剂数据
        int sectionCount = 0;
        for (HH2SectionData sectionData : TipsSingleData.getInstance().getCurSingletonData().getFang()) {
            SpannableStringBuilder fangBuilder = new SpannableStringBuilder();

            // 筛选与药物相关的方剂
            String finalStr1 = str;
            // 假设 TipsNetHelper.filter 方法已知返回类型为 List<DataItem>
            List<? extends DataItem> filteredFang = filterFang(sectionData.getData(), finalStr1);
            String finalStr = str;
            // 对筛选结果进行排序
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                if (filteredFang != null) {
                    filteredFang.sort(new Comparator<DataItem>() {
                        @Override
                        public int compare(DataItem dataItem1, DataItem dataItem2) {
                            return ((Fang) dataItem1).compare((Fang) dataItem2, finalStr);
                        }
                    });
                }
            } else {
                Collections.sort(filteredFang, new Comparator<DataItem>() {
                    @Override
                    public int compare(DataItem dataItem1, DataItem dataItem2) {
                        return ((Fang) dataItem1).compare((Fang) dataItem2, finalStr);
                    }
                });
            }

            int matchedCount = 0;

            // 遍历筛选后的方剂
            if (filteredFang != null) {
                for (DataItem dataItem : filteredFang) {
                    for (String yaoName : dataItem.getYaoList()) {
                        // 查找药物的别名
                        String yaoAlias = yaoAliasDict.get(yaoName);
                        yaoName = yaoAlias != null ? yaoAlias : yaoName;

                        // 匹配方剂中的药物
                        if (yaoName.equals(str)) {
                            matchedCount++;
                            // 添加方剂名称及其相关药物信息
                            fangBuilder.append((CharSequence) TipsNetHelper.renderText(((Fang) dataItem).getFangNameLinkWithYaoWeight(str)));
                            break; // 匹配后退出内层循环
                        }
                    }
                }
            }

            // 添加方剂标题和匹配药物数量
            if (matchedCount > 0) {
                if (sectionCount > 0) {
                    spannableStringBuilder.append("\n\n"); // 分隔不同方剂
                }
                spannableStringBuilder.append((CharSequence) TipsNetHelper.renderText(
                        String.format("$m{%s}-$m{含“$v{%s}”凡%d方：}", sectionData.getHeader(), str, matchedCount)));
                spannableStringBuilder.append("\n").append(fangBuilder);
                sectionCount++;
            }
        }

        return spannableStringBuilder; // 返回最终构建的字符串
    }


    public static SpannableStringBuilder renderText(String str) {
        return renderText(str, new ClickLink() {

            @Override
            public void clickYaoLink(TextView textView, ClickableSpan clickableSpan) {
                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                 EasyLog.print("Yao--tapped:" + charSequence);
                Rect textRect = TipsNetHelper.getTextRect(clickableSpan, textView);
                TipsLittleTextViewWindow tipsLittleTextViewWindow = new TipsLittleTextViewWindow();
                tipsLittleTextViewWindow.setYao(charSequence.trim());
                tipsLittleTextViewWindow.setRect(textRect);
                tipsLittleTextViewWindow.show(((Activity) textView.getContext()).getFragmentManager());
            }

            @Override
            public void clickFangLink(TextView textView, ClickableSpan clickableSpan) {

                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                EasyLog.print("Fang--tapped:" + charSequence);
                List<HH2SectionData> mingCiList = ShowFanYaoMingCi.getInstance().showFangTwo(charSequence.trim());
                ArrayList<GroupEntity> groups = GroupModel.getGroups(mingCiList, charSequence);
                Rect textRect = TipsNetHelper.getTextRect(clickableSpan, textView);
                TipsLittleTableViewWindow tipsLittleTableViewWindow = new TipsLittleTableViewWindow();
                tipsLittleTableViewWindow.setAdapterSource(textView.getContext(), groups);
                tipsLittleTableViewWindow.setFang(charSequence);
                tipsLittleTableViewWindow.setRect(textRect);
                tipsLittleTableViewWindow.show(((Activity) textView.getContext()).getFragmentManager());
            }

            /**
             * @param textView
             * @param clickableSpan
             */
            @Override
            public void clickMingCiLink(TextView textView, ClickableSpan clickableSpan) {

                String charSequence = textView.getText().subSequence(textView.getSelectionStart(), textView.getSelectionEnd()).toString();
                EasyLog.print("MingCi---tapped:" + charSequence);
                List<HH2SectionData> mingCiList = ShowFanYaoMingCi.getInstance().showMingCiTwo(charSequence.trim());

                ArrayList<GroupEntity> groups = GroupModel.getGroups(mingCiList, charSequence);
                Rect textRect = TipsNetHelper.getTextRect(clickableSpan, textView);
                TipsLittleMingCiViewWindow tipsLittleTableViewWindow = new TipsLittleMingCiViewWindow();
                tipsLittleTableViewWindow.setAdapterSource(textView.getContext(), groups);
               // tipsLittleTableViewWindow.setFang(charSequence);
                tipsLittleTableViewWindow.setRect(textRect);
                tipsLittleTableViewWindow.show(((Activity) textView.getContext()).getFragmentManager());

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
        colorMap.put("w", Color.rgb(28, 181, 92)); // 绿色
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
     * @param list      要检查的列表，包含类型为 T 的元素。
     * @param condition 判断元素是否满足条件的函数接口。
     * @param <T>       列表中元素的类型。
     * @return 如果列表中至少有一个元素满足条件，则返回 true；否则返回 false。
     */
    public static <T> boolean some(List<T> list, Condition<T> condition) {
        // 检查 list 和 condition 是否为 null
        if (list == null || condition == null) {
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


}
