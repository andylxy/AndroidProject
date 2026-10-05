package run.yigou.gxzy.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * 全局模态闸门 {@link AppModalGate} 的单元测试（DESIGN §6.5）。
 *
 * <p>纯 JVM：闸门刻意不依赖 Android（除日志外），所以可直接测。</p>
 *
 * <p>这里锁的是升级框/公告框的先后协调，而它错起来的表现极难在真机上复现：
 * 两个 Dialog 叠在一起、或**谁都弹不出来**。最阴的一条是「闸门自己锁死」——
 * {@code release()} 若在执行补弹任务<b>之后</b>才把标志置回 false，补弹任务里的
 * {@code tryAcquire} 就会被自己刚放开的闸门挡回去，于是永远弹不出来（自锁）。</p>
 */
public class AppModalGateTest {

    @Before
    public void setUp() {
        // 静态状态必须逐例复位：否则一个用例占住闸门后，后续用例的
        // 「应立即执行」全变成「排队等待」，表现为莫名其妙的连锁失败。
        AppModalGate.resetForTest();
    }

    @Test
    public void startsFree() {
        assertFalse(AppModalGate.isShowing());
    }

    @Test
    public void firstAcquireWins() {
        assertTrue(AppModalGate.tryAcquire());
        assertTrue(AppModalGate.isShowing());
    }

    @Test
    public void secondAcquireIsRefusedWhileHeld() {
        assertTrue(AppModalGate.tryAcquire());
        // 升级框在屏时公告拿不到闸门 → 只能排队等（这就是 §6.5 的协调点）。
        assertFalse(AppModalGate.tryAcquire());
    }

    @Test
    public void releaseFreesTheGate() {
        AppModalGate.tryAcquire();
        AppModalGate.release();
        assertFalse(AppModalGate.isShowing());
        assertTrue(AppModalGate.tryAcquire());
    }

    @Test
    public void runWhenClearRunsImmediatelyWhenFree() {
        // 闸门空着时必须立刻执行，否则公告在升级框之前先到会被延后甚至丢失。
        final List<String> log = new ArrayList<>();
        AppModalGate.runWhenClear(new Runnable() {
            @Override
            public void run() {
                log.add("ran");
            }
        });
        assertEquals(1, log.size());
    }

    @Test
    public void runWhenClearQueuesWhileHeldAndRunsOnRelease() {
        AppModalGate.tryAcquire();
        final List<String> log = new ArrayList<>();
        AppModalGate.runWhenClear(new Runnable() {
            @Override
            public void run() {
                log.add("announcement");
            }
        });
        // 还在排队：闸门被升级框占着。
        assertEquals(0, log.size());
        AppModalGate.release();
        assertEquals(1, log.size());
        assertEquals("announcement", log.get(0));
    }

    @Test
    public void queuedTaskCanAcquireTheGateItJustWaitedFor() {
        // 回归防护（自锁）：release() 必须**先**把 showing 置 false 再执行补弹，
        // 否则补弹任务里的 tryAcquire 会被自己刚放开的闸门挡回去 → 永远弹不出来。
        AppModalGate.tryAcquire();
        final boolean[] acquired = new boolean[1];
        AppModalGate.runWhenClear(new Runnable() {
            @Override
            public void run() {
                acquired[0] = AppModalGate.tryAcquire();
            }
        });
        // 闸门被升级框占着时任务只在排队，必须等 release() 才会跑。
        assertFalse("补弹任务此刻不该执行", acquired[0]);
        AppModalGate.release();
        assertTrue("补弹任务必须能拿到刚释放的闸门", acquired[0]);
        assertTrue(AppModalGate.isShowing());
    }

    @Test
    public void releasesAllQueuedTasksInOrder() {
        AppModalGate.tryAcquire();
        final List<String> log = new ArrayList<>();
        AppModalGate.runWhenClear(new Runnable() {
            @Override
            public void run() {
                log.add("first");
            }
        });
        AppModalGate.runWhenClear(new Runnable() {
            @Override
            public void run() {
                log.add("second");
            }
        });
        AppModalGate.release();
        assertEquals(2, log.size());
        assertEquals("first", log.get(0));
        assertEquals("second", log.get(1));
    }

    @Test
    public void oneFailingTaskDoesNotStarveTheRest() {
        // 一个补弹任务崩了不能连累其余的，否则后面几条公告全丢。
        AppModalGate.tryAcquire();
        final List<String> log = new ArrayList<>();
        AppModalGate.runWhenClear(new Runnable() {
            @Override
            public void run() {
                throw new RuntimeException("boom");
            }
        });
        AppModalGate.runWhenClear(new Runnable() {
            @Override
            public void run() {
                log.add("survivor");
            }
        });
        AppModalGate.release();
        assertEquals(1, log.size());
        assertEquals("survivor", log.get(0));
    }

    @Test
    public void runWhenClearIgnoresNullTask() {
        AppModalGate.runWhenClear(null);
        assertFalse(AppModalGate.isShowing());
    }
}
