package run.yigou.gxzy.EventBus;

public class ShowUpdateNotificationEvent {


    private boolean updateNotification = false;

    public boolean isUpdateNotification() {
        return updateNotification;
    }

    public ShowUpdateNotificationEvent setUpdateNotification(boolean flag) {
        this.updateNotification = flag;
        return this;
    }
}
