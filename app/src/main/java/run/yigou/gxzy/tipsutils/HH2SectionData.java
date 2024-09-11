package run.yigou.gxzy.tipsutils;

import java.util.List;

/* loaded from: classes.dex */
public class HH2SectionData {
    private List<? extends DataItem> data;
    private String header;
    private int section;

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
