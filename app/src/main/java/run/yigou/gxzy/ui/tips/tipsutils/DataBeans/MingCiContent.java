/*
 * 项目名: AndroidProject
 * 类名: Item.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.DataBeans.Item
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
import run.yigou.gxzy.ui.tips.tipsutils.TipsNetHelper;

public class MingCiContent extends DataItem {
    /**
     * 名词列表
     */
    private List<String> mingCiList;
    private String name;

    public List<String> getMingCiList() {
        return mingCiList == null ? new ArrayList<>() : this.mingCiList;
    }

    public void setMingCiList(List<String> mingCiList) {
        this.mingCiList = mingCiList;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


}
