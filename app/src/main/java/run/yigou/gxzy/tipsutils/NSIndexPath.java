package run.yigou.gxzy.tipsutils;

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
