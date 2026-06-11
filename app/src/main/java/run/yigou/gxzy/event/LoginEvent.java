package run.yigou.gxzy.event;

/**
 *  ?????????????????
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
