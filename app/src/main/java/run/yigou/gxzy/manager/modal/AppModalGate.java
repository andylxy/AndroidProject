package run.yigou.gxzy.manager.modal;
import run.yigou.gxzy.manager.announcement.AnnouncementManager;
import run.yigou.gxzy.manager.update.UpdateManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 全局「模态框闸门」：保证同一时刻**只有一个**模态框在屏，并让被挡住的框能在闸门
 * 释放后自动补上（DESIGN §6.5）。
 *
 * <p><b>为什么需要它</b>：升级框（{@code UpdateManager}）与公告框
 * （{@code AnnouncementManager}）各自独立触发，启动完成时可能同时到达。若各弹各的，
 * 两个 {@code Dialog} 会叠在一起，用户看到的是错位的两层界面。规则是
 * <b>升级框优先</b>（阻断性更强，必须先被看到），公告框等它关掉再弹。</p>
 *
 * <p><b>为什么不能靠 Activity</b>：两个 Manager 都在弹窗层去重
 * （各自的 {@code sDialogShowing}），但那两个标志互不相识，跨 Manager 就失效了。
 * 本类就是那份共享认知。</p>
 *
 * <p><b>本类刻意不打任何日志</b>：它因此可以是被纯 JVM 单测完整覆盖的状态机
 * （见 {@code AppModalGateTest}）。加了 {@code EasyLog} 就等于在每个状态转移里引入
 * {@code android.util.Log}，JVM 单测会直接抛
 * {@code RuntimeException: Method i in android.util.Log not mocked} ——
 * 正是本项目已踩过的那类坑。需要日志的调用点自己打（两个 Manager 都已打）。</p>
 */
public final class AppModalGate {

    /** 当前是否有模态框在屏。同一 monitor 上用 synchronized 保证并发只有一个赢家。 */
    private static boolean sShowing = false;

    /** 等待闸门释放后补弹的任务（先入先出）。 */
    private static final Deque<Runnable> PENDING = new ArrayDeque<>();

    /**
     * 闸门释放时的订阅者（后注册先不通知，只在后续释放时触发）。
     *
     * <p>存在的理由：某些提示走的是「置 pending 标志 + 等Activity resume 补弹」的路
     * （如强制升级的 {@code sPendingForceUpgrade}），而公告框弹着的时候**不会**再有
     * resume 事件，那个补弹就永远等不到 → 用户被内容门拦死却看不到提示。
     * 让「闸门空了」本身成为一个可订阅的事件，这条路才闭合。</p>
     */
    private static final List<Runnable> RELEASE_LISTENERS = new ArrayList<>();

    private AppModalGate() {
    }

    /**
     * 订阅「闸门变为空闲」事件。每次 {@link #release()} 都会通知。
     *
     * <p>回调在锁外执行，异常被吞掉（一个订阅者崩了不影响其余订阅者与弹窗状态）。</p>
     */
    public static synchronized void addOnReleasedListener(Runnable listener) {
        if (listener != null) {
            RELEASE_LISTENERS.add(listener);
        }
    }

    /**
     * 尝试占住闸门。
     *
     * @return {@code true} 表示占用成功，可以弹；{@code false} 表示已有模态框在屏
     */
    public static synchronized boolean tryAcquire() {
        if (sShowing) {
            return false;
        }
        sShowing = true;
        return true;
    }

    /**
     * 释放闸门，并依次执行等待中的补弹任务。
     *
     * <p><b>执行前先把 {@code sShowing} 置回 false</b>：补弹任务会再次调用
     * {@link #tryAcquire}，若此时标志仍为 true，它会被自己刚放开的闸门挡回去，
     * 于是永远弹不出来（自锁）。这条由
     * {@code AppModalGateTest#queuedTaskCanAcquireTheGateItJustWaitedFor} 钉住。</p>
     */
    public static void release() {
        Deque<Runnable> queued;
        List<Runnable> listeners;
        synchronized (AppModalGate.class) {
            sShowing = false;
            listeners = new ArrayList<>(RELEASE_LISTENERS);
            if (PENDING.isEmpty()) {
                // 仍要通知订阅者：pending 队列空 ≠ 没有等待弹窗的提示
                // （强制升级走的是 sPendingForceUpgrade 标志，不进这个队列）。
                notifyListeners(listeners);
                return;
            }
            // 取出后先清空队列再执行：任务里若又注册了新的补弹（极端情况），
            // 那是新一轮等待，不该在本轮就被执行。
            queued = new ArrayDeque<>(PENDING);
            PENDING.clear();
        }
        // 在锁外执行：任务本身可能再调 tryAcquire/runWhenClear（重入）。
        // 放在锁内执行会因 synchronized 可重入而侥幸通过，但一旦任务里
        // 走到别的 synchronized 方法就可能与别的线程互相等待。
        for (Runnable task : queued) {
            try {
                task.run();
            } catch (RuntimeException error) {
                // 一个补弹任务崩了不能连累其余的（否则后面几条公告都丢了）。
                // 异常不在这里吞掉日志——调用方拿不到；但闸门已释放，状态一致。
                continue;
            }
        }
        notifyListeners(listeners);
    }

    /** 通知订阅者，异常逐个隔离。 */
    private static void notifyListeners(List<Runnable> listeners) {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (RuntimeException error) {
                // 订阅者崩了不影响弹窗状态，也不影响其他订阅者。
                continue;
            }
        }
    }

    /** 当前是否有模态框在屏。 */
    public static synchronized boolean isShowing() {
        return sShowing;
    }

    /**
     * 登记一个「等闸门空出来再执行」的任务（弹窗协调，DESIGN §6.5）。
     *
     * <p>闸门当前为空就<b>立刻</b>执行——否则公告在升级框之前先到时会被延后到
     * 无关的后续时刻，或干脆丢失。</p>
     */
    public static void runWhenClear(Runnable task) {
        if (task == null) {
            return;
        }
        boolean runNow;
        synchronized (AppModalGate.class) {
            runNow = !sShowing;
            if (!runNow) {
                PENDING.addLast(task);
            }
        }
        if (runNow) {
            task.run();
        }
    }

    /**
     * 仅供单测复位：清空闸门状态与等待队列。
     *
     * <p>进程内静态状态在单测之间会互相污染，必须显式复位——否则一个用例占住闸门
     * 后，后续用例的「应立即执行」全部变成「排队等待」，表现为莫名其妙的连锁失败。</p>
     */
    public static synchronized void resetForTest() {
        sShowing = false;
        PENDING.clear();
        RELEASE_LISTENERS.clear();
    }
}
