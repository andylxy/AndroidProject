package run.yigou.gxzy.event;

/**
 * Tips 片段设置事件通知
 * 阅读设置（如术解开关）变更后发出的刷新信号，触发阅读页重新加载列表
 */
public class TipsFragmentSettingEventNotification {


    private boolean shuJie_Notification = false;

    public boolean isShuJie_Notification() {
        return shuJie_Notification;
    }

    public void setShuJie_Notification(boolean shuJie_Notification) {
        this.shuJie_Notification = shuJie_Notification;
    }

    public TipsFragmentSettingEventNotification() {
    }


}
