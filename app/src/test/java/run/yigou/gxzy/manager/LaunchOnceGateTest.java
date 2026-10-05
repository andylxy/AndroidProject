package run.yigou.gxzy.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

/**
 * 一次性启动闸门 {@link LaunchOnceGate} 的单元测试。
 *
 * <p>纯 JVM：被测类刻意不引用 {@code android.app.Activity}，只接收 {@code Object}
 * 并用 {@code instanceof LifecycleOwner} 判定宿主，所以 JVM 单测能直接跑。</p>
 *
 * <p>锁的是 2026-10-05 adb 实测发现的那个 bug：冷启动首个应用级前台回调拿到的
 * Activity <b>往往不是 LifecycleOwner</b>，而早期实现「先置位闸门、再判宿主」，
 * 于是「本次启动唯一一次」机会被白白烧掉 → 版本检查/公告拉取<b>一次都不执行</b>。
 * 判据 1（宿主不可用）必须<b>不消耗</b>机会。</p>
 */
public class LaunchOnceGateTest {

    /** 假宿主：实现 LifecycleOwner 但什么都不做。 */
    private static final class FakeHost implements LifecycleOwner {
        @Override
        public Lifecycle getLifecycle() {
            throw new UnsupportedOperationException("not needed for this test");
        }
    }

    private LaunchOnceGate gate;

    @Before
    public void setUp() {
        gate = new LaunchOnceGate();
    }

    @Test
    public void runsOnceWhenHostIsUsable() {
        AtomicInteger calls = new AtomicInteger();
        assertTrue(gate.runOnceIfHostUsable(new FakeHost(), calls::incrementAndGet));
        assertTrue(calls.get() == 1);
        assertTrue(gate.isDone());
    }

    @Test
    public void doesNotRunTwiceOnLaterCalls() {
        AtomicInteger calls = new AtomicInteger();
        gate.runOnceIfHostUsable(new FakeHost(), calls::incrementAndGet);
        assertFalse("第二次必须被闸门挡住", gate.runOnceIfHostUsable(new FakeHost(), calls::incrementAndGet));
        assertTrue(calls.get() == 1);
    }

    @Test
    public void doesNotConsumeTheGateWhenHostIsNotUsable() {
        // 回归防护：这里的断言顺序就是那个 bug 的全部要害——
        // 先传不可用宿主，此时**不能**置位；随后传可用宿主必须仍能执行。
        AtomicInteger calls = new AtomicInteger();
        assertFalse(gate.runOnceIfHostUsable("not-a-lifecycle-owner", calls::incrementAndGet));
        assertFalse("宿主不可用时不得置位", gate.isDone());
        assertTrue(calls.get() == 0);

        // 下一个可用宿主（现实中来自 registerActivityResumeCallback）必须能补上。
        assertTrue(gate.runOnceIfHostUsable(new FakeHost(), calls::incrementAndGet));
        assertTrue(calls.get() == 1);
    }

    @Test
    public void treatsNullHostAsUnusable() {
        AtomicInteger calls = new AtomicInteger();
        assertFalse(gate.runOnceIfHostUsable(null, calls::incrementAndGet));
        assertFalse(gate.isDone());
    }

    @Test
    public void repeatedUnusableHostsNeverConsumeTheGate() {
        // 模拟冷启动：连着几个不可用宿主（启动页等），机会必须一直留着。
        AtomicInteger calls = new AtomicInteger();
        for (int i = 0; i < 5; i++) {
            assertFalse(gate.runOnceIfHostUsable(new Object(), calls::incrementAndGet));
        }
        assertFalse(gate.isDone());
        assertTrue(gate.runOnceIfHostUsable(new FakeHost(), calls::incrementAndGet));
        assertTrue(calls.get() == 1);
    }

    /**
     * 闸门与弹窗闸门的组合语义：公告框在屏时，强制升级必须**排队**而不是被丢弃
     * （DESIGN §6.5「升级框优先」）。这条锁的是 2026-10-05 code-review 的 P0：
     * 原实现闸门忙时直接 return，公告既不置 pending 也不重排 → 强制升级永久丢失，
     * 用户被 426 内容门拦死却只看到一个可关掉的公告框。
     */
    @Test
    public void forceUpgradeQueuesBehindAnnouncementInsteadOfBeingDropped() {
        AppModalGate.resetForTest();
        try {
            // 公告框先抢到弹窗闸门。
            assertTrue(AppModalGate.tryAcquire());

            // 强制升级来排队（模拟 UpdateManager 的闸门忙分支）。
            final List<String> shown = new ArrayList<>();
            AppModalGate.runWhenClear(new Runnable() {
                @Override
                public void run() {
                    if (AppModalGate.tryAcquire()) {
                        shown.add("force-upgrade");
                    }
                }
            });
            assertTrue("公告框在屏时强制升级必须还在排队", shown.isEmpty());

            // 公告框关闭 → 闸门释放 → 强制升级必须真的弹出来。
            AppModalGate.release();
            assertTrue("公告框关掉后强制升级必须补弹", shown.contains("force-upgrade"));
        } finally {
            AppModalGate.resetForTest();
        }
    }
}
