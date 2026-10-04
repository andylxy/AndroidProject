package run.yigou.gxzy.manager;

import android.app.Activity;

/**
 * 取「当前可用的前台 Activity」——升级弹窗与设备禁用提示共用，避免两处各写一遍
 * 「拿到 Activity 后还要判断 isFinishing / isDestroyed」的逻辑。
 */
final class ForegroundActivities {

    private ForegroundActivities() {
    }

    /** 没有前台 Activity、或它正在结束 / 已销毁时返回 null。 */
    static Activity topIfUsable() {
        Activity activity = ActivityManager.getInstance().getTopActivity();
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            return null;
        }
        return activity;
    }
}
