package run.yigou.gxzy.ui.tips.entity;

/**
 * 组数据类
 * 封装Group级别的数据
 */
public class GroupData {
    private String title;
    private boolean isExpanded;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}
