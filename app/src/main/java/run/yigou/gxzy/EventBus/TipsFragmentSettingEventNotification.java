package run.yigou.gxzy.EventBus;

/**
 *  宋版伤寒金匮通知界面刷新
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
