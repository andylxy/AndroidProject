package run.yigou.gxzy.manager.concurrency;
import run.yigou.gxzy.manager.lifecycle.ForegroundActivities;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 「失败退避后再试一次」的排程器 —— <b>单飞</b> + <b>主线程定时</b> 这一件事的唯一实现。
 *
 * <p>公告刷新与搜索权限拉取都需要同一件事：失败后在 {@code RETRY_INTERVAL_MS} 到点时再试，
 * 且一次失败只能排出<b>一个</b>定时器。此前两处各抄了一份 {@code AtomicBoolean} +
 * {@code postDelayed}，结构体相同而生命周期各自漂移（其中一个还得记得做惰性初始化，
 * 否则 JVM 单测加载类时静态初始化即崩）。</p>
 *
 * <p><b>为什么定时器必须在主线程</b>：到期执行的回调会走网络请求，而请求宿主取的是
 * {@code ForegroundActivities.topIfUsable()} 且要 {@code LifecycleOwner}，主线程能保证
 * 这一个 Looper 队列上的顺序与 UI 状态一致。</p>
 */
public final class RetryScheduler {

    /** 到点要执行的动作。刻意做成 SAM 接口而不是抽象方法，让调用方保持在自己的类里。 */
    public interface Task {
        void run();
    }

    /**
     * 主线程 Handler：<b>惰性初始化</b>。
     *
     * <p>不能在静态字段里直接 {@code new Handler(Looper.getMainLooper())}：JVM 单测环境没有
     * Android 主线程，一旦有测试加载了持有它的类，静态初始化失败会让整批用例报
     * {@code ExceptionInInitializerError}。惰性化之后，只有真正排程（只发生在设备上）才会取。</p>
     */
    private static Handler sMainHandler;

    private final long delayMs;
    private final Task task;
    private final AtomicBoolean pending = new AtomicBoolean(false);

    public RetryScheduler(long delayMs, Task task) {
        this.delayMs = delayMs;
        this.task = task;
    }

    /**
     * 排一次。已有一次在途则直接返回 —— 一次失败只该排出一个定时器。
     *
     * <p>定时到点后<b>先放开单飞标志再执行任务</b>：任务多半会再次失败并重新排程，
     * 若标志此时仍为真，那条新排的任务会被自己拦掉，重试链就此断掉。</p>
     */
    public void schedule() {
        if (!pending.compareAndSet(false, true)) {
            return;
        }
        mainHandler().postDelayed(() -> {
            pending.set(false);
            task.run();
        }, delayMs);
    }

    private static synchronized Handler mainHandler() {
        if (sMainHandler == null) {
            sMainHandler = new Handler(Looper.getMainLooper());
        }
        return sMainHandler;
    }
}
