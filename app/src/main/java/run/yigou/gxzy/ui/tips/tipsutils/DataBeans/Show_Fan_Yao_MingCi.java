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

    public Show_Fan_Yao_MingCi() {
        tipsSingleData = Tips_Single_Data.getInstance();
        // 获取 SingletonData 实例和数据结构
        singletonData = tipsSingleData.getBookIdContent(tipsSingleData.getCurBookId());
    }

    private final ArrayList<Show_Fan_Yao_MingCi> showFanYaoMingCiList = new ArrayList<>();

    public ArrayList<Show_Fan_Yao_MingCi> showFang(String fanyao) {
        showFanYaoMingCiList.clear();
        // 获取方名别名映射
        Map<String, String> fangAliasDict = singletonData.getFangAliasDict();
        String aliasName = null;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            aliasName = fangAliasDict.getOrDefault(fanyao, fanyao);
        } else {
            // 兼容 Android 7.0 以下版本
            aliasName = fangAliasDict.containsKey(fanyao) ? fangAliasDict.get(fanyao) : fanyao;
        }
        boolean found = false;
        ArrayList<HH2SectionData> fangList = singletonData.getFang();
        for (HH2SectionData sectionData : fangList) {
            for (DataItem dataItem : sectionData.getData()) {
                String originalName = dataItem.getFangList().get(0);
                String actualName = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    actualName = fangAliasDict.getOrDefault(originalName, originalName);
                } else {
                    // 对于 Android 7.0 以下版本使用 get 方法并手动处理默认值
                    actualName = fangAliasDict.containsKey(originalName) ? fangAliasDict.get(originalName) : originalName;
                }

                if (actualName != null && actualName.equals(aliasName)) {
                    Show_Fan_Yao_MingCi showFanYaoMingCi = new Show_Fan_Yao_MingCi();
                    // 找到匹配项，添加到 data 和 headers
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

                for (String fangNameInList : dataItem.getFangList()) {
                    String actualName = null;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        actualName = fangAliasDict.getOrDefault(fangNameInList, fangNameInList);
                    } else {
                        // 对于 Android 7.0 以下版本
                        actualName = fangAliasDict.containsKey(fangNameInList) ? fangAliasDict.get(fangNameInList) : fangNameInList;
                    }

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

        return showFanYaoMingCiList;
    }

    /**
     * 根据给定的药物名称生成一个 spannable 字符串，其中包含药物相关的信息。
     *
     * @param str 药物名称
     * @return 包含药物信息的 SpannableStringBuilder
     */
    public SpannableStringBuilder getShowYaoSpanString(String str) {
        // 创建一个用于拼接 spannable 文本的对象
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder();

        // 获取药物别名字典
        Map<String, String> yaoAliasDict = singletonData.getYaoAliasDict();
        Map<String, Yao> yaoMap = tipsSingleData.getYaoMap();
        // 获取药物的实际名称（考虑别名）
        String actualName = yaoAliasDict.get(str);
        if (actualName != null) {
            str = actualName;
        }
        Yao yao = yaoMap.get(str);
        // 如果不只显示相关方的信息，则开始处理药物数据

        List<DataItem> dataItems = new ArrayList<>();
        // 遍历所有的药物数据
        if (yao.getName().equals(str)) {
            dataItems.add(yao);
        }

        // 处理数据项并拼接结果
        if (!dataItems.isEmpty()) {
            for (DataItem dataItem : dataItems) {
                spannableStringBuilder.append((CharSequence) dataItem.getAttributedText());
            }
        } else {
            spannableStringBuilder.append((CharSequence) TipsNetHelper.renderText("$r{药物未找到资料}"));
        }
        spannableStringBuilder.append((CharSequence) "\n\n");

        // 遍历所有的方数据
        List<HH2SectionData> fangData = singletonData.getFang();
        int sectionCount = 0; // 记录方数据的数量

        for (HH2SectionData sectionData : fangData) {
            List<DataItem> filteredItems = new ArrayList<>();

            for (DataItem dataItem : sectionData.getData()) {
                if (((Fang) dataItem).hasYao(str)) {
                    filteredItems.add(dataItem);
                }
            }

            // 对方数据进行排序
            String finalStr = str;
            Collections.sort(filteredItems, new Comparator<DataItem>() {
                @Override
                public int compare(DataItem item1, DataItem item2) {
                    return ((Fang) item1).compare((Fang) item2, finalStr);
                }
            });

            int matchedCount = filteredItems.size(); // 记录匹配的方数量
            if (matchedCount > 0) {
                if (sectionCount > 0) {
                    spannableStringBuilder.append((CharSequence) "\n\n");
                }
                spannableStringBuilder.append((CharSequence) TipsNetHelper.renderText(
                        String.format("$m{%s}-$m{含“$v{%s}”凡%d方：}",
                                sectionData.getHeader(), str, matchedCount)
                ));
                spannableStringBuilder.append((CharSequence) "\n");

                for (DataItem dataItem : filteredItems) {
                    spannableStringBuilder.append((CharSequence) TipsNetHelper.renderText(((Fang) dataItem).getFangNameLinkWithYaoWeight(str)));
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
        if (str == null) return spannableStringBuilder;
        // 获取药物别名字典
        Map<String, MingCiContent> mingCiContentMap = tipsSingleData.getMingCiContentMap();
        MingCiContent mingCiContent = mingCiContentMap.get(str);
        if (mingCiContent != null)
            spannableStringBuilder.append(TipsNetHelper.renderText(mingCiContent.getText()));
        return spannableStringBuilder;
    }

}
