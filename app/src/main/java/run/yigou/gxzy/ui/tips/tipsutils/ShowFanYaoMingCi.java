/*
 * 项目名: AndroidProject
 * 类名: ShowFanYao.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.DataBeans.ShowFanYao
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:38
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils;

import com.hjq.http.EasyLog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import run.yigou.gxzy.ui.tips.DataBeans.MingCiContent;


public class ShowFanYaoMingCi {
    // 写本类单对象
    private static volatile ShowFanYaoMingCi instance;

    /**
     * 获取  Show_Fan_Yao_MingCi 单例对象
     *
     * @return 单例对象
     */
    public static ShowFanYaoMingCi getInstance() {
        if (instance == null) {
            synchronized (ShowFanYaoMingCi.class) {
                if (instance == null) {
                    instance = new ShowFanYaoMingCi();
                }
            }
        }
        instance.getSingletonData();
        return instance;
    }

    private  void getSingletonData() {
        // 获取 SingletonData 实例和数据结构
        singletonData = tipsSingleData.getMapBookContent(tipsSingleData.getCurBookId());
    }

    private TipsSingleData tipsSingleData;
    private SingletonNetData singletonData;

    private ShowFanYaoMingCi() {
        tipsSingleData = TipsSingleData.getInstance();

    }

    /**
     * 根据方药名称展示相关信息
     * 该方法首先尝试根据输入的方药名称查找匹配的方剂数据
     * 如果找不到匹配的方剂，则添加默认的"伤寒金匮方"数据
     * 最后，处理与输入方药相关的其他内容，并将其添加到返回列表中
     *
     * @param fanyao 方药名称，用于查找方剂数据
     * @return 包含HH2SectionData对象的List，这些对象包含方剂及其相关信息
     */
    public List<HH2SectionData> showFangTwo(String fanyao) {
        // 清空现有的展示列表

        List<HH2SectionData> hh2SectionDataList = new ArrayList<>();

        // 获取方名别名映射
        Map<String, String> fangAliasDict = singletonData.getFangAliasDict();
        String aliasName = getOrDefault(fangAliasDict, fanyao);

        boolean found = false;
        ArrayList<HH2SectionData> fangList = singletonData.getFang();

        // 遍历方剂列表，查找匹配的方剂
        for (HH2SectionData sectionData : fangList) {
            for (DataItem dataItem : sectionData.getData()) {
               // if (dataItem.getFangList().isEmpty()) continue;
                String originalName = dataItem.getName();
                String actualName = getOrDefault(fangAliasDict, originalName);
                // 如果找到匹配的方剂，创建并添加显示对象到列表
                if (actualName != null && actualName.equals(aliasName)) {
                    // 创建对象
                    DataItem fangCiContent;
                    try {
                        fangCiContent = dataItem.getCopy();
                        // 减少冗余对象创建：通过 Collections.singletonList 创建单元素列表
                        List<DataItem> mingCiContentList = Collections.singletonList(fangCiContent);
                        String name = sectionData.getHeader() != null ? sectionData.getHeader().trim() : "";
                        hh2SectionDataList.add(new HH2SectionData(mingCiContentList, 0, name));
                    } catch (NullPointerException e) {
                        // 处理 getCopy 或 getName 抛出的 NullPointerException
                        EasyLog.print("Error: mingCiMap.getCopy() or mingCiMap.getName() returned null.");
                    }
                    found = true;
                    break;  // 跳出当前循环
                }
            }
            if (found) break;  // 如果已找到匹配项，则跳出外层循环
        }
        // 如果未找到匹配项，添加默认的 "伤寒金匮方" 数据
        if (!found) {
            DataItem dataItem = new DataItem();
            dataItem.setText("$m{未见方。}");
            List<DataItem> dataItemList = Collections.singletonList(dataItem);
            hh2SectionDataList.add(new HH2SectionData(dataItemList, 0, "伤寒金匮方"));
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
                    sectionDataList.add(dataItem.getCopy());
                }
            }

            // 如果有数据，则添加到 data 和 headers
            if (sectionDataList != null && !sectionDataList.isEmpty()) {
                try {
                    String name = sectionData.getHeader() != null ? sectionData.getHeader().trim() : "";
                    hh2SectionDataList.add(new HH2SectionData(sectionDataList, 0, name));
                } catch (NullPointerException e) {
                    EasyLog.print("Error: sectionData.getHeader()  returned null.");
                }
            }
        }

        // 返回包含方剂信息的列表
        return hh2SectionDataList;
    }

    public List<HH2SectionData> showMingCiTwo(String mingCi) {

        // 获取方名别名映射
        Map<String, MingCiContent> mingCiContentMap = tipsSingleData.getMingCiContentMap();

        List<HH2SectionData> hh2SectionDataList = new ArrayList<>();

        // 兼容低版本 Android
        MingCiContent mingCiMap;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            mingCiMap = mingCiContentMap.getOrDefault(mingCi, null);
        } else {
            // 兼容低版本 Android 的处理方式
            mingCiMap = mingCiContentMap.get(mingCi);
        }

        // 空指针检查
        if (mingCiMap != null) {
            // 使用 mingCiMap
            // 创建对象
            // 添加数据
            MingCiContent mingCiContent;
            try {
                mingCiContent = mingCiMap.getCopy();
                // 减少冗余对象创建：通过 Collections.singletonList 创建单元素列表
                List<MingCiContent> mingCiContentList = Collections.singletonList(mingCiContent);
                String name = mingCiMap.getName() != null ? mingCiMap.getName().trim() : "";
                hh2SectionDataList.add(new HH2SectionData(mingCiContentList, 0, name));
            } catch (NullPointerException e) {
                // 处理 getCopy 或 getName 抛出的 NullPointerException
                EasyLog.print("Error: mingCiMap.getCopy() or mingCiMap.getName() returned null.");
            }

        } else {
            // 处理实际名称不存在的情况
            DataItem dataItem = new DataItem();
            dataItem.setText("$m{未见此文字解释。}");
            // 减少冗余对象创建：通过 Collections.singletonList 创建单元素列表
            List<DataItem> dataItemList = Collections.singletonList(dataItem);
            hh2SectionDataList.add(new HH2SectionData(dataItemList, 0, "当前书籍"));
        }


//        // 处理其他章节是否有此文字出现
//        for (HH2SectionData sectionData : singletonData.getContent()) {
//            ArrayList<DataItem> sectionDataList = null;
//
//            for (DataItem dataItem : sectionData.getData()) {
//                boolean matchFound = false;
//
//                Pattern pattern = TipsNetHelper.getPattern(mingCi);
//
//                // 创建一个 Matcher 对象，并重用它
//                Matcher matcher = pattern.matcher("");
//
//                // 空指针检查
//                String text = dataItem.getText();
//                String note = dataItem.getNote();
//                String video = dataItem.getSectionvideo();
//
//                if (text != null && matcher.reset(text).find() ||
//                        note != null && matcher.reset(note).find() ||
//                        video != null && matcher.reset(video).find()) {
//                    matchFound = true;
//                }
//                // 遍历数据项，查找匹配项
//                // 突出显示数据项中的匹配文本
//                if (matchFound) {
//                    if (sectionDataList == null) {
//                        sectionDataList = new ArrayList<>();
//                    }
//                    TipsNetHelper.createSingleDataCopy(dataItem, pattern);
//                    sectionDataList.add(dataItem.getCopy());
//                }
//            }
//
//            // 如果有数据，则添加到 data 和 headers
//            if (sectionDataList != null) {
//
//                try {
//                    String name = sectionData.getHeader() != null ? sectionData.getHeader().trim() : "";
//                    hh2SectionDataList.add(new HH2SectionData(sectionDataList, 0, name));
//                } catch (NullPointerException e) {
//                    EasyLog.print("Error: sectionData.getHeader()  returned null.");
//                }
//            }
//        }

        // 返回包含方剂信息的列表
        return hh2SectionDataList;
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


}
