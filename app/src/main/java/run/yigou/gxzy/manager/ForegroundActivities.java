package run.yigou.gxzy.manager;

import android.app.Activity;

/**
 * 取「当前可用的前台 Activity」——升级弹窗与设备禁用提示共用，避免两处各写一遍
 * 「拿到 Activity 后还要判断 isFinishing / isDestroyed」的逻辑。
 *
 * <p><b>「可用」的判定比「存在」严格（票据 24）</b>：Activity 存在不等于能承载 Dialog。
 * 冷启动时 {@code SplashActivity} 会持到版本检查的结果，但它随即 {@code finish()}，
 * 挂在它身上的 Dialog 会跟着一起消失 —— 后端明明下了 {@code force=true}，
 * 用户屏幕上却什么都不会看到。故这里排除「自己会立刻结束」的宿主。</p>
 */
final class ForegroundActivities {

    private ForegroundActivities() {
    }

    /** 没有前台 Activity、或它不能承载 Dialog 时返回 null。 */
    static Activity topIfUsable() {
        final Activity activity = ActivityManager.getInstance().getTopActivity();
        return isUsableHost(activity) ? activity : null;
    }

    /**
     * 这个 Activity 能否承载升级 / 禁用弹窗。
     *
     * <p>两类不可用：</p>
     * <ol>
     *   <li>硬性不可用：已销毁或正在结束 —— 弹窗挂了也显示不出来；</li>
     *   <li>短命宿主：SplashActivity 这类「显示完就 finish」的启动页。
     *       它在冷启动时必然是第一个 resume 的 Activity，等版本检查的往返返回时通常已被销毁；
     *       即使还没销毁，把不可关闭的强制升级框挂在启动页上，用户也来不及反应。</li>
     * </ol>
     */
    static boolean isUsableHost(Activity activity) {
        return activity != null
                && !activity.isFinishing()
                && !activity.isDestroyed()
                && !isEphemeral(activity);
    }

    /**
     * 是否是「显示完即结束」的启动页宿主。
     *
     * <p>判据是类名含 {@code Splash}。本项目的启动页在 AndroidManifest 里就是
     * {@code .ui.main.SplashActivity}（无 alias），所以类名判定足够。
     * 刻意不用「Activity 是不是 taskRoot」这类间接推断：它在正常页面里也会命中，
     * 反而误伤。改名后这里会静默失效——真出问题时把启动页的类名补进来即可。</p>
     */
    private static boolean isEphemeral(Activity activity) {
        return activity.getClass().getSimpleName().contains("Splash");
    }
}
