/*
 * 项目名: AndroidProject
 * 类名: NSIndexPath.java
 * 包名: run.yigou.gxzy.ui.tips.tipsutils.NSIndexPath
 * 作者 : Zhs (xiaoyang_02@qq.com)
 * 当前修改时间 : 2024年09月12日 09:47:06
 * 上次修改时间: 2024年09月12日 09:44:39
 * Copyright (c) 2024 Zhs, Inc. All Rights Reserved
 */

package run.yigou.gxzy.ui.tips.tipsutils;

/* loaded from: classes.dex */
public class NSIndexPath {
    private int mRow;
    private int mSection;

    private NSIndexPath(int i, int i2) {
        this.mRow = i;
        this.mSection = i2;
    }

    public static NSIndexPath indexPathForRowInSection(int i, int i2) {
        return new NSIndexPath(i, i2);
    }

    public int getSection() {
        return this.mSection;
    }

    public int getRow() {
        return this.mRow;
    }

    public String toString() {
        return "[" + this.mRow + ", " + this.mSection + "]";
    }

    public boolean equals(Object obj) {
        NSIndexPath nSIndexPath = (NSIndexPath) obj;
        return this.mRow == nSIndexPath.getRow() && this.mSection == nSIndexPath.getSection();
    }

    public int hashCode() {
        return (this.mSection + "," + this.mRow).hashCode();
    }
}
