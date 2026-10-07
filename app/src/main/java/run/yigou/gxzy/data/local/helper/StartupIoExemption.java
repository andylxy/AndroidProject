package run.yigou.gxzy.data.local.helper;

import android.os.StrictMode;

import run.yigou.gxzy.app.AppConfig;

/**
 * 进程启动期"一次性前置 IO"的严格模式豁免窗口。
 *
 * <p><b>为什么需要它</b>：进程一启动就必须做几件不可推迟的事——打开本地库
 * （{@code SQLiteOpenHelper.getWritableDatabase()}）、建升级历史表、首次建索引、读版本号、
 * 建立 {@code DbService} 单例。它们都以"库刚打开、连接可用"为前提，
 * 推迟到后台只会让同一件事在更晚的时刻仍然跑在主线程。所以这段被显式标记为<b>已知例外</b>，
 * 好让严格模式剩下的信号里只剩真正需要修的东西。
 *
 * <p><b>豁免范围就是上面这几件事，全部由调用方逐段声明</b>；窗口之外的一切
 * （包括后续所有主线程 IO）照常被记录。调用方应当把连续的启动动作放在**一次**调用里——
 * 拆成两个紧邻的窗口既不会更窄，也只是徒增容器。
 *
 * <p><b>debug 之外</b>：不做任何策略切换，直接执行原动作（发布构建没有严格模式可豁免）。
 *
 * <p>用法（与全仓风格一致，用匿名 Runnable）：
 * <pre>
 * StartupIoExemption.runExempted(new Runnable() {
 *     &#64;Override public void run() {
 *         GreenDaoManager.getInstance();
 *     }
 * });
 * </pre>
 */
public final class StartupIoExemption {

    private StartupIoExemption() {
    }

    /** 在豁免窗口内执行一次启动期前置动作；异常原样传出（窗口内不吞异常）。 */
    public static void runExempted(Runnable action) {
        if (action == null) {
            return;
        }
        if (!AppConfig.isDebug()) {
            action.run();
            return;
        }
        StrictMode.ThreadPolicy original = StrictMode.getThreadPolicy();
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(original)
                .permitDiskReads()
                .permitDiskWrites()
                .build());
        try {
            action.run();
        } finally {
            StrictMode.setThreadPolicy(original);
        }
    }
}