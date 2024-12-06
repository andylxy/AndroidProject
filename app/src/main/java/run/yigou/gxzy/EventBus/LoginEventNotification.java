package run.yigou.gxzy.EventBus;

/**
 *  登陆成功后通知界面刷新
 */
public class LoginEventNotification {


    private boolean loginNotification = false;

    public boolean getLoginNotification() {
        return loginNotification;
    }

    public LoginEventNotification setUpdateNotification(boolean flag) {
        this.loginNotification = flag;
        return this;
    }

    public LoginEventNotification() {
    }

    public LoginEventNotification(boolean flag) {
        this.loginNotification = flag;
    }
}
