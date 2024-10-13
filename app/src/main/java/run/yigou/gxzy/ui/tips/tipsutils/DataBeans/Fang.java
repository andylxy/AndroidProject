/*
 * 项目名: AndroidProject
 * 类名: Fang.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Fang
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:38
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils.DataBeans;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import run.yigou.gxzy.ui.tips.tipsutils.DataItem;
import run.yigou.gxzy.ui.tips.tipsutils.Singleton_Net_Data;
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;
import run.yigou.gxzy.ui.tips.tipsutils.Tips_Single_Data;


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
        standardYaoList .add(standardYao);
    }

    List<YaoUse> standardYaoList;
    int yaoCount;

    public boolean hasYao( String str) {
        if (this.standardYaoList == null) {
            this.standardYaoList = new LinkedList();
        }
        if (this.extraYaoList == null) {
            this.extraYaoList = new LinkedList();
        }
        if (this.helpYaoList == null) {
            this.helpYaoList = new LinkedList();
        }
        TipsNetHelper.Condition<YaoUse> iBool = new TipsNetHelper.Condition<YaoUse>() {
            @Override
            public boolean test(YaoUse yaoUse) {
                return Fang.this.isYaoEqual(yaoUse.getShowName(), str);
            }
        };
        return TipsNetHelper.some(this.standardYaoList, iBool) || TipsNetHelper.some(this.extraYaoList, iBool) || TipsNetHelper.some(this.helpYaoList, iBool);
    }

    public int compare(Fang fang, String str) {
        YaoUse yaoUseByYao = getYaoUseByYao(str);
        YaoUse yaoUseByYao2 = fang.getYaoUseByYao(str);
        if (yaoUseByYao == null) {
            return 1;
        }
        if (yaoUseByYao2 == null) {
            return -1;
        }
        float max = Math.max(yaoUseByYao.getWeight(), yaoUseByYao.getMaxWeight()) / this.drinkNum;
        float max2 = Math.max(yaoUseByYao2.getWeight(), yaoUseByYao2.getMaxWeight()) / fang.drinkNum;
        if (max < max2) {
            return 1;
        }
        return max > max2 ? -1 : 0;
    }

    public YaoUse getYaoUseByYao(String str) {
        if (this.standardYaoList != null) {
            for (YaoUse yaoUse : this.standardYaoList) {
                if (isYaoEqual(yaoUse.getShowName(), str)) {
                    return yaoUse;
                }
            }
        }
        if (this.extraYaoList != null) {
            for (YaoUse yaoUse2 : this.extraYaoList) {
                if (isYaoEqual(yaoUse2.getShowName(), str)) {
                    return yaoUse2;
                }
            }
        }
        if (this.helpYaoList == null) {
            return null;
        }
        for (YaoUse yaoUse3 : this.helpYaoList) {
            if (isYaoEqual(yaoUse3.getShowName(), str)) {
                return yaoUse3;
            }
        }
        return null;
    }

    public String getFangNameLinkWithYaoWeight(String str) {
        if (this.standardYaoList != null) {
            for (YaoUse yaoUse : this.standardYaoList) {
                if (isYaoEqual(yaoUse.getShowName(), str)) {
                    return String.format(Locale.CHINA, "$f{%s}$w{(%s%.0f%s服)}，", this.name, yaoUse.getAmount(), Float.valueOf(this.drinkNum), def(yaoUse.getSuffix(), ""));
                }
            }
        }
        if (this.extraYaoList != null) {
            for (YaoUse yaoUse2 : this.extraYaoList) {
                if (isYaoEqual(yaoUse2.getShowName(), str)) {
                    return String.format(Locale.CHINA, "$f{%s}$w{(%s%.0f%s服)}，", this.name, yaoUse2.getAmount(), Float.valueOf(this.drinkNum), def(yaoUse2.getSuffix(), ""));
                }
            }
        }
        if (this.helpYaoList == null) {
            return "";
        }
        for (YaoUse yaoUse3 : this.helpYaoList) {
            if (isYaoEqual(yaoUse3.getShowName(), str)) {
                return String.format(Locale.CHINA, "$f{%s}$w{(%s%.0f%s服)}，", this.name, yaoUse3.getAmount(), Float.valueOf(this.drinkNum), def(yaoUse3.getSuffix(), ""));
            }
        }
        return "";
    }
    public static <T> T def(T t, T t2) {
        return t == null ? t2 : t;
    }
    public boolean isYaoEqual(String str, String str2) {
        return getStandardYaoName(str).equals(getStandardYaoName(str2));
    }

    public String getStandardYaoName(String str) {
        String str2 =  Tips_Single_Data.getInstance().getBookIdContent(Tips_Single_Data.getInstance().getCurBookId()).getYaoAliasDict().get(str);
        return str2 == null ? str : str2;
    }


}
