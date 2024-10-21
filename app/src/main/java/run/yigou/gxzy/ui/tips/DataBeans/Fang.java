/*
 * 项目名: AndroidProject
 * 类名: Fang.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Fang
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:38
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.DataBeans;

import android.os.Build;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.TipsSingleData;


public class Fang extends DataItem {
    float drinkNum;
    List<YaoUse> extraYaoList;
    List<YaoUse> helpYaoList;
    String makeWay;
    public String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getYaoCount() {
        return yaoCount;
    }

    public void setYaoCount(int yaoCount) {
        this.yaoCount = yaoCount;
    }

    public float getDrinkNum() {
        return drinkNum;
    }

    public void setDrinkNum(float drinkNum) {
        this.drinkNum = drinkNum;
    }

    public List<YaoUse> getStandardYaoList() {
        return standardYaoList;
    }

    public void setStandardYaoList(YaoUse standardYao) {
        if (standardYaoList == null) {
            standardYaoList = new ArrayList<>();
        }
        standardYaoList.add(standardYao);
    }

    List<YaoUse> standardYaoList;
    int yaoCount;

    /**
     * 检查给定字符串是否与任何 Yao 的名称匹配
     * 此方法用于确定在标准 Yao 列表、额外 Yao 列表或辅助 Yao 列表中是否存在与给定字符串相等的 Yao
     *
     * @param str 要检查的字符串，代表 Yao 的名称
     * @return 如果找到匹配的 Yao，则返回 true；否则返回 false
     */
    public boolean hasYao(String str) {
        // 初始化标准 Yao 列表，如果尚未初始化的话
        if (this.standardYaoList == null) {
            this.standardYaoList = new LinkedList();
        }
        // 初始化额外 Yao 列表，如果尚未初始化的话
        if (this.extraYaoList == null) {
            this.extraYaoList = new LinkedList();
        }
        // 初始化辅助 Yao 列表，如果尚未初始化的话
        if (this.helpYaoList == null) {
            this.helpYaoList = new LinkedList();
        }
        // 创建一个条件对象，用于测试 Yao 的名称是否与给定字符串相等
        TipsNetHelper.Condition<YaoUse> iBool = new TipsNetHelper.Condition<YaoUse>() {
            @Override
            public boolean test(YaoUse yaoUse) {
                // 重写 test 方法，实现名称比较逻辑
                return Fang.this.isYaoEqual(yaoUse.getShowName(), str);
            }
        };
        // 使用 TipsNetHelper 的 some 方法检查标准、额外和辅助 Yao 列表中是否存在满足条件的 Yao
        // 如果任一列表中存在与给定字符串相等的 Yao 名称，则返回 true
        return TipsNetHelper.some(this.standardYaoList, iBool) || TipsNetHelper.some(this.extraYaoList, iBool) || TipsNetHelper.some(this.helpYaoList, iBool);
    }

    /**
     * 比较两个方剂在特定条件下的用药权重
     * 此方法用于评估当前方剂与另一个方剂在使用特定药材时的用药强度
     *
     * @param fang 方剂对象，用于比较的另一个方剂
     * @param str 字符串，可能代表药材名称或特定条件
     * @return int 返回比较结果，1表示当前方剂用药权重较大，-1表示较小，0表示相等
     */
    public int compare(Fang fang, String str) {
        // 根据给定的字符串获取当前方剂中的药材使用情况
        YaoUse yaoUseByYao = getYaoUseByYao(str);
        // 根据给定的字符串获取比较方剂中的药材使用情况
        YaoUse yaoUseByYao2 = fang.getYaoUseByYao(str);

        // 如果当前方剂中没有该药材的使用记录，则认为用药权重较大
        if (yaoUseByYao == null) {
            return 1;
        }
        // 如果比较方剂中没有该药材的使用记录，则认为用药权重较小
        if (yaoUseByYao2 == null) {
            return -1;
        }

        // 计算当前方剂中该药材的平均用药权重
        float max = Math.max(yaoUseByYao.getWeight(), yaoUseByYao.getMaxWeight()) / this.drinkNum;
        // 计算比较方剂中该药材的平均用药权重
        float max2 = Math.max(yaoUseByYao2.getWeight(), yaoUseByYao2.getMaxWeight()) / fang.drinkNum;

        // 比较两个方剂的平均用药权重，决定返回值
        if (max < max2) {
            return 1;
        }
        return max > max2 ? -1 : 0;
    }

    /**
     * 根据给定的字符串在不同的药用列表中查找药用信息
     * 此方法优先在标准药列表中查找，如果找不到，则尝试在额外药列表和帮助药列表中查找
     * 如果在所有列表中都找不到匹配的药用信息，则返回null
     *
     * @param str 要查找的药用字符串
     * @return 匹配的药用信息对象，如果找不到则返回null
     */
    public YaoUse getYaoUseByYao(String str) {
        // 在标准药列表中查找药用信息
        YaoUse result = findYaoUse(this.standardYaoList, str);
        if (result != null) {
            return result;
        }

        // 在额外药列表中查找药用信息
        result = findYaoUse(this.extraYaoList, str);
        if (result != null) {
            return result;
        }

        // 在帮助药列表中查找药用信息
        result = findYaoUse(this.helpYaoList, str);
        if (result != null) {
            return result;
        }

        // 如果在所有列表中都没有找到匹配的药用信息，返回null
        return null;
    }

    /**
     * 从给定的列表中查找匹配的YaoUse对象
     *
     * @param yaoList YaoUse对象的列表，用于查找
     * @param str     需要匹配的字符串
     * @return 如果找到匹配的YaoUse对象，则返回该对象；否则返回null
     */
    private YaoUse findYaoUse(List<YaoUse> yaoList, String str) {
        // 检查列表是否为空或为null，如果是，则直接返回null
        if (yaoList == null || yaoList.isEmpty()) {
            return null;
        }

        try {
            // 从Android N版本开始，可以使用Java 8的流特性
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                // 使用流和过滤器来查找列表中匹配特定条件的YaoUse对象
                return yaoList.stream()
                        .filter(yaoUse -> isYaoEqual(yaoUse.getShowName(), str))
                        .findFirst()
                        .orElse(null);
            }
        } catch (Exception e) {
            // 处理异常，例如记录日志
            System.err.println("Error in findYaoUse: " + e.getMessage());
            return null;
        }
        // 如果没有找到匹配的YaoUse对象，或者版本低于Android N，则返回null
        return null;
    }


    /**
     * 根据给定的字符串获取方名链接带药味的信息
     * 该方法主要用于在不同的药材列表中查找与给定字符串匹配的药材信息，并返回其方名链接带药味的详细信息
     *
     * @param str 用于匹配药材信息的查询字符串
     * @return 匹配到的方名链接带药味的信息，如果没有匹配到则返回空字符串
     */
    public String getFangNameLinkWithYaoWeight(String str) {
        // 提前计算常量
        float drinkNum = Float.valueOf(this.drinkNum);

        // 统一处理空指针
        List<YaoUse> standardYaoList = this.standardYaoList != null ? this.standardYaoList : Collections.emptyList();
        List<YaoUse> extraYaoList = this.extraYaoList != null ? this.extraYaoList : Collections.emptyList();
        List<YaoUse> helpYaoList = this.helpYaoList != null ? this.helpYaoList : Collections.emptyList();

        // 提取公共代码
        String result = findYaoUse(standardYaoList, str, drinkNum);
        if (result != null) {
            return result;
        }

        result = findYaoUse(extraYaoList, str, drinkNum);
        if (result != null) {
            return result;
        }

        result = findYaoUse(helpYaoList, str, drinkNum);
        if (result != null) {
            return result;
        }

        return "";
    }

    /**
     * 在给定的药用列表中查找匹配的药用信息
     *
     * @param yaoList 药用信息列表，用于查找匹配的药用信息
     * @param str 需要匹配的字符串，通常为药用的名称或标识
     * @param drinkNum 服用数量，用于计算或显示药量
     * @return 如果找到匹配的药用信息，则返回格式化的药用信息字符串；否则返回null
     */
    private String findYaoUse(List<YaoUse> yaoList, String str, float drinkNum) {
        // 遍历药用列表，寻找匹配的药用信息
        for (YaoUse yaoUse : yaoList) {
            // 检查药用信息是否非空且名称匹配
            if (yaoUse != null && isYaoEqual(yaoUse.getShowName(), str)) {
                // 获取药用量，如果为空则使用空字符串
                String amount = yaoUse.getAmount() != null ? yaoUse.getAmount() : "";
                // 获取后缀，如果为空则使用空字符串
                String suffix = def(yaoUse.getSuffix(), "");
                // 格式化并返回药用信息字符串
                return String.format(Locale.CHINA, "$f{%s}$w{(%s%.0f%s服)}，", this.name, amount, drinkNum, suffix);
            }
        }
        // 如果没有找到匹配的药用信息，返回null
        return null;
    }

    //    public String getFangNameLinkWithYaoWeight(String str) {
//        if (this.standardYaoList != null) {
//            for (YaoUse yaoUse : this.standardYaoList) {
//                if (isYaoEqual(yaoUse.getShowName(), str)) {
//                    return String.format(Locale.CHINA, "$f{%s}$w{(%s%.0f%s服)}，", this.name, yaoUse.getAmount(), Float.valueOf(this.drinkNum), def(yaoUse.getSuffix(), ""));
//                }
//            }
//        }
//        if (this.extraYaoList != null) {
//            for (YaoUse yaoUse2 : this.extraYaoList) {
//                if (isYaoEqual(yaoUse2.getShowName(), str)) {
//                    return String.format(Locale.CHINA, "$f{%s}$w{(%s%.0f%s服)}，", this.name, yaoUse2.getAmount(), Float.valueOf(this.drinkNum), def(yaoUse2.getSuffix(), ""));
//                }
//            }
//        }
//        if (this.helpYaoList == null) {
//            return "";
//        }
//        for (YaoUse yaoUse3 : this.helpYaoList) {
//            if (isYaoEqual(yaoUse3.getShowName(), str)) {
//                return String.format(Locale.CHINA, "$f{%s}$w{(%s%.0f%s服)}，", this.name, yaoUse3.getAmount(), Float.valueOf(this.drinkNum), def(yaoUse3.getSuffix(), ""));
//            }
//        }
//        return "";
//    }
//
    public static <T> T def(T t, T t2) {
        return t == null ? t2 : t;
    }

    public boolean isYaoEqual(String str, String str2) {
        return getStandardYaoName(str).equals(getStandardYaoName(str2));
    }

    public String getStandardYaoName(String str) {
        String str2 = TipsSingleData.getInstance().getMapBookContent(TipsSingleData.getInstance().getCurBookId()).getYaoAliasDict().get(str);
        return str2 == null ? str : str2;
    }


}
