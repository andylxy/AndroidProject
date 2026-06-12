package run.yigou.gxzy.event;

/**
 * 登录事件通知
 * 用于通知登录状态变更，携带登录通知开关标志
 */
public class LoginEvent {


    private boolean loginNotification = false;

    public boolean getLoginNotification() {
        return loginNotification;
    }

    public LoginEvent setUpdateNotification(boolean flag) {
        this.loginNotification = flag;
        return this;
    }

    public LoginEvent() {
    }

    public LoginEvent(boolean flag) {
        this.loginNotification = flag;
    }
}
