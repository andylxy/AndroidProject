/*
 * 项目名: AndroidProject
 * 类名: ShowFanYao.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.DataBeans.ShowFanYao
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:38
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils.DataBeans;

import android.text.SpannableStringBuilder;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData;
import run.yigou.gxzy.ui.tips.tipsutils.Singleton_Net_Data;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.Tips_Single_Data;


public class Show_Fan_Yao_MingCi {

   // 写本类单对象
    private static Show_Fan_Yao_MingCi instance;

    /**
     * 获取  Show_Fan_Yao_MingCi 单例对象
     *
     * @return 单例对象
     */
    public static Show_Fan_Yao_MingCi getInstance() {
        if (instance == null) {
            instance = new Show_Fan_Yao_MingCi();
        }
        return instance;
    }

    private List<DataItem> data = new ArrayList<>();

    public List<DataItem> getData() {
        return data;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    private String header;

    private Tips_Single_Data tipsSingleData;
    private Singleton_Net_Data singletonData;

    private Show_Fan_Yao_MingCi() {
        tipsSingleData = Tips_Single_Data.getInstance();
        // 获取 SingletonData 实例和数据结构
        singletonData = tipsSingleData.getBookIdContent(tipsSingleData.getCurBookId());
    }

    private final ArrayList<Show_Fan_Yao_MingCi> showFanYaoMingCiList = new ArrayList<>();

    /**
     * 根据方药名称展示相关信息
     * 该方法首先尝试根据输入的方药名称查找匹配的方剂数据
     * 如果找不到匹配的方剂，则添加默认的"伤寒金匮方"数据
     * 最后，处理与输入方药相关的其他内容，并将其添加到返回列表中
     *
     * @param fanyao 方药名称，用于查找方剂数据
     * @return 包含Show_Fan_Yao_MingCi对象的ArrayList，这些对象包含方剂及其相关信息
     */
    public ArrayList<Show_Fan_Yao_MingCi> showFang(String fanyao) {
    // 清空现有的展示列表
    showFanYaoMingCiList.clear();
    //Show_Fan_Yao_MingCi showFanYaoMingCi =  Show_Fan_Yao_MingCi.getInstance();
    // 获取方名别名映射
    Map<String, String> fangAliasDict = singletonData.getFangAliasDict();
    String aliasName = getOrDefault(fangAliasDict, fanyao);

    boolean found = false;
    ArrayList<HH2SectionData> fangList = singletonData.getFang();

    // 遍历方剂列表，查找匹配的方剂
    for (HH2SectionData sectionData : fangList) {
        for (DataItem dataItem : sectionData.getData()) {
            String originalName = dataItem.getFangList().get(0);
            String actualName = getOrDefault(fangAliasDict, originalName);
            List<DataItem> data = new ArrayList<>();
            // 如果找到匹配的方剂，创建并添加显示对象到列表
            if (actualName != null && actualName.equals(aliasName)) {
                Show_Fan_Yao_MingCi showFanYaoMingCi = new Show_Fan_Yao_MingCi();
                showFanYaoMingCi.setHeader(sectionData.getHeader());
                showFanYaoMingCi.getData().add(dataItem);
                showFanYaoMingCiList.add(showFanYaoMingCi);
                found = true;
                break;  // 跳出当前循环
            }
        }
        if (found) break;  // 如果已找到匹配项，则跳出外层循环
    }

    // 如果未找到匹配项，添加默认的 "伤寒金匮方" 数据
    if (!found) {
        Show_Fan_Yao_MingCi showFanYaoMingCi = new Show_Fan_Yao_MingCi();
        showFanYaoMingCi.setHeader("伤寒金匮方");
        DataItem dataItem = new DataItem();
        dataItem.setText("$m{未见方。}");
        showFanYaoMingCi.getData().add(dataItem);
        showFanYaoMingCiList.add(showFanYaoMingCi);
    }

    // 处理其他内容
    for (HH2SectionData sectionData : singletonData.getContent()) {
        ArrayList<DataItem> sectionDataList = null;

        for (DataItem dataItem : sectionData.getData()) {
            boolean matchFound = false;

            // 遍历数据项中的方名列表，查找匹配项
            for (String fangNameInList : dataItem.getFangList()) {
                String actualName = getOrDefault(fangAliasDict, fangNameInList);

                // 如果找到匹配项，将其添加到列表
                if (actualName != null && actualName.equals(aliasName)) {
                    matchFound = true;
                    break;  // 找到匹配项，退出循环
                }
            }

            if (matchFound) {
                if (sectionDataList == null) {
                    sectionDataList = new ArrayList<>();
                }
                sectionDataList.add(dataItem);
            }
        }

        // 如果有数据，则添加到 data 和 headers
        if (sectionDataList != null) {
            Show_Fan_Yao_MingCi showFanYaoMingCi = new Show_Fan_Yao_MingCi();
            showFanYaoMingCi.setHeader(sectionData.getHeader());
            showFanYaoMingCi.getData().addAll(sectionDataList);
            showFanYaoMingCiList.add(showFanYaoMingCi);
        }
    }

    // 返回包含方剂信息的列表
    return showFanYaoMingCiList;
}


private String getOrDefault(Map<String, String> map, String key) {
    // 空值检查
    if (map == null) {
        return key;
    }

    // 兼容不同版本的 Android SDK
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
        return map.getOrDefault(key, key);
    } else {
        return map.containsKey(key) ? map.get(key) : key;
    }
}


    private static final String DRUG_NOT_FOUND = "$r{药物未找到资料}";
    private static final String SECTION_FORMAT = "$m{%s}-$m{含“$v{%s}”凡%d方：}";

    /**
 * 根据输入的字符串生成包含药物信息的可显示SpannableStringBuilder
 * 该方法主要用于展示与指定字符串匹配的药物信息，包括药物别名处理、药物存在性检查以及相关配方信息
 *
 * @param str 用户输入的字符串，用于查找匹配的药物
 * @return 包含药物信息的SpannableStringBuilder，用于UI展示
 */
public SpannableStringBuilder getShowYaoSpanString(String str) {
    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

    // 从单例数据中获取药物别名字典和药物映射
    Map<String, String> yaoAliasDict = singletonData.getYaoAliasDict();
    Map<String, Yao> yaoMap = tipsSingleData.getYaoMap();

    // 如果输入字符串是药物别名，替换为实际名称
    String actualName = yaoAliasDict.get(str);

    if (actualName != null) {
        str = actualName;
    }

    // 从药物映射中获取药物对象，若不存在则初始化一个空的Yao对象
    Yao yao = yaoMap.get(str);
    if (yao == null) {
        yao = new Yao(); // 或者返回默认值
    }

    // 初始化数据项列表，用于存储匹配的药物信息
    List<DataItem> dataItems = new ArrayList<>();

    // 如果药物名称与输入字符串匹配，添加到数据项列表
    if (yao.getName().equals(str)) {
        dataItems.add(yao);
    }

    // 如果数据项列表不为空，将各数据项的属性文本添加到SpannableStringBuilder
    // 否则，添加药物未找到的提示文本
    if (!dataItems.isEmpty()) {
        for (DataItem dataItem : dataItems) {
            spannableStringBuilder.append((CharSequence) dataItem.getAttributedText());
        }
    } else {
        spannableStringBuilder.append(TipsNetHelper.renderText(DRUG_NOT_FOUND));
    }

    // 从单例数据中获取配方数据，并初始化节计数器
    List<HH2SectionData> fangData = singletonData.getFang();
    int sectionCount = 0;

    // 遍历配方数据，查找包含目标药物的配方
    for (HH2SectionData sectionData : fangData) {
        List<DataItem> filteredItems = new ArrayList<>();

        // 筛选包含目标药物的配方数据项
        for (DataItem dataItem : sectionData.getData()) {
            if (dataItem instanceof Fang && ((Fang) dataItem).hasYao(str)) {
                filteredItems.add(dataItem);
            }
        }
        // 将 str 复制到 final 临时变量中
        final String finalStr = str;
        // 对匹配的配方数据项按特定规则排序
        Collections.sort(filteredItems, (item1, item2) -> {
            return ((Fang) item1).compare((Fang) item2, finalStr);
        });

        int matchedCount = filteredItems.size();
        // 如果有匹配的配方，则添加到SpannableStringBuilder中
        if (matchedCount > 0) {
            // 如果不是第一个节，则添加换行符
            if (sectionCount > 0) {
                spannableStringBuilder.append("\n\n");
            }
            // 格式化并添加节标题和匹配数量
            spannableStringBuilder.append(TipsNetHelper.renderText(
                    String.format(SECTION_FORMAT, sectionData.getHeader(), str, matchedCount)
            ));
            spannableStringBuilder.append("\n");

            // 添加匹配的配方名称和药物权重信息
            for (DataItem dataItem : filteredItems) {
                spannableStringBuilder.append(TipsNetHelper.renderText(((Fang) dataItem).getFangNameLinkWithYaoWeight(str)));
            }

            sectionCount++;
        }
    }

    return spannableStringBuilder;
}



    /**
 * 根据给定的药物名称生成一个 spannable 字符串，其中包含药物相关的信息。
 *
 * @param str 药物名称
 * @return 包含药物信息的 SpannableStringBuilder
 */
public SpannableStringBuilder getShowMingCiSpanString(String str) {

    // 创建一个用于拼接 spannable 文本的对象
    SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

    // 空值检查
    if (str == null) {
        return spannableStringBuilder;
    }

    try {
        // 获取药物别名字典
        Map<String, MingCiContent> mingCiContentMap = tipsSingleData.getMingCiContentMap();

        // 空值检查
        if (mingCiContentMap == null) {
            return spannableStringBuilder;
        }

        MingCiContent mingCiContent = mingCiContentMap.get(str);
        if (mingCiContent != null) {
            spannableStringBuilder.append(TipsNetHelper.renderText(mingCiContent.getText()));
        } else {
            // 日志记录
            EasyLog.print("DrugInfo", "No information found for drug: " + str);
        }
    } catch (NullPointerException e) {
        // 异常处理
        EasyLog.print("DrugInfo", "NullPointerException occurred: " + e.getMessage());
        e.printStackTrace();
    } catch (Exception e) {
        // 其他异常处理
        EasyLog.print("DrugInfo", "An unexpected error occurred: " + e.getMessage());
        e.printStackTrace();
    }

    return spannableStringBuilder;
}


}
