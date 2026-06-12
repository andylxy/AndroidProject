package run.yigou.gxzy.event;

/**
 * Tips 片段设置事件通知
 * 用于通知伤寒论、金匮要略、书解等模块的设置变更
 */
public class TipsFragmentSettingEventNotification {


    private boolean shanghan_Notification = false;
    private boolean jinkui_Notification = false;
    private boolean shuJie_Notification = false;

    public boolean isShuJie_Notification() {
        return shuJie_Notification;
    }

    public void setShuJie_Notification(boolean shuJie_Notification) {
        this.shuJie_Notification = shuJie_Notification;
    }

    public boolean isShanghan_Notification() {
        return shanghan_Notification;
    }

    public void setShanghan_Notification(boolean shanghan_Notification) {
        this.shanghan_Notification = shanghan_Notification;
    }

    public boolean isJinkui_Notification() {
        return jinkui_Notification;
    }

    public void setJinkui_Notification(boolean jinkui_Notification) {
        this.jinkui_Notification = jinkui_Notification;
    }

    public TipsFragmentSettingEventNotification() {
    }


}
