package run.yigou.gxzy.ui.reader.search;

import run.yigou.gxzy.data.model.DataItem;
import run.yigou.gxzy.data.model.HH2SectionData;

import android.text.SpannableStringBuilder;

import androidx.annotation.NonNull;

import run.yigou.gxzy.log.EasyLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import run.yigou.gxzy.data.model.Fang;
import run.yigou.gxzy.ui.reader.entity.SearchKeyEntity;
import run.yigou.gxzy.ui.reader.helper.TipsClickHandler;
import run.yigou.gxzy.text.TipsTextRenderer;

/**
 * Tips 模块搜索引擎
 * 负责处理搜索逻辑、正则匹配、数据过滤等
 */
public class TipsSearchEngine {

    private TipsSearchEngine() {
    }

    public static @NonNull ArrayList<HH2SectionData> getSearchHh2SectionData(SearchKeyEntity searchKeyEntity,
                                                                             List<HH2SectionData> contentList,
                                                                             Map<String, String> yaoAliasDict,
                                                                             Map<String, String> fangAliasDict) {
        if (contentList.size() == 1) {
            searchKeyEntity.getSearchKeyText().append(",未见");
        }

        // 将搜索词拆分并过滤掉空白项
        String[] searchTerms = searchKeyEntity.getSearchKeyText().toString().split("[,.。，]");
        List<Pattern> validPatterns = new ArrayList<>();

        for (String term : searchTerms) {
            if (!term.isEmpty()) {
                // 预编译 Pattern，避免在循环中重复编译
                validPatterns.add(getPattern(sanitizeTerm(term)));
            }
        }
        ArrayList<HH2SectionData> filteredData = new ArrayList<>(); // 用于保存过滤后的结果

        // 遍历数据以进行过滤
        int index = 0;
        for (HH2SectionData sectionData : contentList) {
            List<DataItem> matchedItems = new ArrayList<>(); // 用于保存当前部分中的匹配项
            boolean sectionHasMatches = false;

            // 检查当前部分中的每一个数据项
            if (sectionData.getData() != null) {
                for (DataItem dataItem : sectionData.getData()) {
                    boolean itemMatched = false;
                    DataItem dataItem2 = dataItem.getCopy();
                    dataItem2.setGroupPosition(index);

                    // 检查每个搜索词 Pattern
                    for (Pattern pattern : validPatterns) {
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
     * 清理搜索词，去除不必要的字符。
     */
    private static String sanitizeTerm(String term) {
        // 去除前后破折号，并替换特殊字符（如果需要）
        return term.replace("-", "").replace("#", ".");
    }

    /**
     * 匹配数据项是否符合给定的模式，考虑Yao和Fang的别名字典。
     */
    private static boolean matchDataItem(DataItem dataItem, Pattern pattern,
                                         Map<String, String> yaoAliasDict, Map<String, String> fangAliasDict) {
        // 验证输入的模式不能为空
        if (pattern == null) {
            throw new IllegalArgumentException("Pattern cannot be null");
        }

        // 获取数据项的属性文本，用于后续匹配
        String attributeText = getAttributeText(dataItem);
        Matcher matcher = pattern.matcher(attributeText);

        // 检查模式是否匹配Fang列表或Yao列表中的任何项
        if (matchInList(dataItem.getFangList(), matcher) || matchInList(dataItem.getYaoList(), matcher)) {
            return true;
        }

        // 如果数据项有名称，尝试匹配名称或属性文本，或检查别名
        if (dataItem.getName() != null) {
            return matcher.reset(dataItem.getName()).find() || matcher.reset(attributeText).find() || checkAliases(dataItem, pattern, yaoAliasDict, fangAliasDict);
        }

        // 如果数据项没有名称，匹配属性文本或检查别名
        return matcher.reset(attributeText).find() || checkAliases(dataItem, pattern, yaoAliasDict, fangAliasDict);
    }

    /**
     * 从数据项中获取属性文本。
     */
    private static String getAttributeText(DataItem dataItem) {
        return (dataItem.getAttributedText() != null) ? dataItem.getAttributedText().toString() : "";
    }

    /**
     * 检查列表中的任何项是否匹配给定的模式。
     */
    private static boolean matchInList(List<String> list, Matcher matcher) {
        if (list == null) return false;
        for (String item : list) {
            if (item != null && matcher.reset(item).find()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 高亮匹配的文本
     */
    public static void createSingleDataCopy(DataItem dataItem, Pattern pattern) {
        if (pattern == null) {
            return;
        }

        SpannableStringBuilder spannableText = TipsClickHandler.renderText(dataItem.getText());
        SpannableStringBuilder spannableNote = TipsClickHandler.renderText(dataItem.getNote());
        SpannableStringBuilder spannableSectionVideo = TipsClickHandler.renderText(dataItem.getSectionvideo());

        // 创建Matcher对象，用于在各个部分中匹配模式
        Matcher matcherText = pattern.matcher(spannableText);
        Matcher matcherNote = pattern.matcher(spannableNote);
        Matcher matcherSectionVideo = pattern.matcher(spannableSectionVideo);

        // 对匹配到的文本应用高亮显示 - 调用 TipsTextRenderer
        TipsTextRenderer.highlightMatches(matcherText, spannableText);
        TipsTextRenderer.highlightMatches(matcherNote, spannableNote);
        TipsTextRenderer.highlightMatches(matcherSectionVideo, spannableSectionVideo);

        // 将高亮显示后的文本设置回DataItem对象
        dataItem.setAttributedText(spannableText);
        dataItem.setAttributedNote(spannableNote);
        dataItem.setAttributedSectionVideo(spannableSectionVideo);
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
     * 过滤出符合条件的方剂信息
     */
    public static List<? extends DataItem> filterFang(List<? extends DataItem> sectionData, String finalStr1) {
        if (sectionData == null || finalStr1 == null) {
            return Collections.emptyList();
        }

        List<DataItem> result = new ArrayList<>();
        for (DataItem item : sectionData) {
            if (item instanceof Fang) {
                Fang fang = (Fang) item;
                if (fang.hasYao(finalStr1)) {
                    result.add(fang);
                }
            }
        }
        return result;
    }
}
