package run.yigou.gxzy.manager.launch;
import run.yigou.gxzy.manager.announcement.AnnouncementManager;
import run.yigou.gxzy.manager.update.UpdateManager;

import java.util.concurrent.atomic.AtomicBoolean;

import androidx.lifecycle.LifecycleOwner;

/**
 * 「一次启动只做一件事」的一次性闸门（进程级）。
 *
 * <p>抽出来是因为它被两个 Manager 以**完全相同的形状**各写了一份
 * （{@code UpdateManager.tryLaunchCheck} 与 {@code AnnouncementManager.tryLaunchFetch}）：
 * 都是「没做过 → 宿主可用 → CAS 置位 → 执行」。重复的闸门迟早会只改一处，
 * 于是两个功能的「一次启动一次」语义悄悄分叉。</p>
 *
 * <p><b>关键顺序：先判宿主、再置位</b>（2026-10-05 adb 实测）。冷启动时首个应用级前台
 * 回调拿到的 Activity 往往**不是** {@link LifecycleOwner}；若先 CAS 置位、再发现宿主
 * 不可用而放弃，「本次启动唯一一次」的机会就被白白烧掉 → 该功能本次启动**永远不执行**
 * （版本检查一次都不做 / 公告一条都不拉）。失败时**不消耗**闸门，等下一个触发点重来。</p>
 *
 * <p>刻意不接收 {@code Activity}：那会让本类在 JVM 单测里加载不到 android 框架。
 * 传 {@code Object} 并在内部用 {@code instanceof LifecycleOwner} 判定，
 * 单测可传一个实现了该接口的假宿主。</p>
 */
public final class LaunchOnceGate {

    private final AtomicBoolean mDone = new AtomicBoolean(false);

    /**
     * 闸门未消耗且宿主可用时执行一次 {@code action}。
     *
     * @param host   候选宿主；非 {@link LifecycleOwner} 一律视为不可用
     * @param action 真正要执行的动作
     * @return {@code true} 表示本次真的执行了；{@code false} 表示已做过或宿主不可用
     */
    public boolean runOnceIfHostUsable(Object host, Runnable action) {
        if (mDone.get()) {
            return false;
        }
        if (!(host instanceof LifecycleOwner)) {
            // 刻意**不**置位：等下一个触发点（每个 Activity resume 都会来，
            // 且 resume 必定带可用宿主）。这里置位就等于本次启动再也没机会了。
            return false;
        }
        if (!mDone.compareAndSet(false, true)) {
            return false;
        }
        action.run();
        return true;
    }

    /** 本次启动是否已经执行过（仅供诊断与单测）。 */
    public boolean isDone() {
        return mDone.get();
    }
}
