/*
 * 项目名: AndroidProject
 * 类名: HH2SectionData.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.HH2SectionData
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:39
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils;

import java.io.Serializable;
import java.util.List;

public class HH2SectionData implements Serializable {
    private List<? extends DataItem> data;
    private String header;
    private int section;

    public HH2SectionData() {
    }

    public HH2SectionData(List<? extends DataItem> list, int i, String str) {
        this.section = i;
        this.header = str;
        this.data = list;
    }

    public List<? extends DataItem> getData() {
        return this.data;
    }

    public String getHeader() {
        return this.header;
    }

    public int getSection() {
        return this.section;
    }

    public void setSection(int i) {
        this.section = i;
    }
}
