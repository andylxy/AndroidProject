package run.yigou.gxzy.manager;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import run.yigou.gxzy.manager.concurrency.FetchClock;
import run.yigou.gxzy.manager.mingci.MingCiPermissionStore;

/**
 * {@link MingCiPermissionStore} 的「拉取节流」纯 JVM 单测。
 *
 * <p>严格镜像 {@link SearchPermissionStoreTest}：结构、判据完全一致。被测的
 * {@code shouldFetchNow} 只做时间算术，不碰 MMKV / Android，时钟由外部传入
 * {@link FetchClock}，因此可直接跑、也不需要 Robolectric。</p>
 *
 * <p>与 search 同构：<b>不设每日节流</b>（每日节流已按 ADR 移除），只有「上次尝试失败」才退避，
 * 成功过立即恢复放行；「一次启动一条链」由 {@code LaunchOnceGate} 负责。
 * 「次日 resume 重验」由<b>下次冷启动</b>（新进程 → 闸门重置）自然达成。</p>
 *
 * <p>这些判据错了后果严重：节流失效会让 App 每次启动都打服务器，
 * 或（反向）让已授权用户整天看不到名词解释。</p>
 */
public final class MingCiPermissionStoreTest {

    private static final long MINUTE = 60_000L;
    private static final long HOUR = 3_600_000L;

    /**
     * 固定时间锚点（任意非零毫秒值即可，纯算术用）。
     *
     * <p><b>刻意不锚定「某天的某时刻」</b>：本功能的节流只比较「距上次尝试多久」，
     * 不看自然日（每日节流已按 ADR 移除），所以断言与时区/日期无关。</p>
     */
    private static final long T0 = 1_700_000_000_000L;

    @Test
    public void shouldFetchWhenNeverAttempted() {
        assertTrue(MingCiPermissionStore.shouldFetchNow(T0, new FetchClock(0L, 0L)));
    }

    @Test
    public void shouldBackoffWithinRetryWindowAfterFailure() {
        long attempt = T0;
        long now = attempt + 10 * MINUTE; // 失败后 10 分钟
        FetchClock clock = new FetchClock(attempt, 0L); // 尝试过、未成功
        assertFalse(MingCiPermissionStore.shouldFetchNow(now, clock));
    }

    @Test
    public void shouldRetryAfterBackoffWindow() {
        long attempt = T0;
        long now = attempt + 31 * MINUTE; // 退避期已过
        FetchClock clock = new FetchClock(attempt, 0L);
        assertTrue(MingCiPermissionStore.shouldFetchNow(now, clock));
    }

    /**
     * 回归：上次成功后<b>当天再次启动必须仍会拉取</b>。
     *
     * <p>曾经的 bug：节流按「今天已成功过」否决，而时钟是 MMKV 持久化、{@code sFetchOk}
     * 是进程内 → 当天第二次冷启动整程不请求，已授权用户名词解释被禁用到跨午夜。
     * 现在成功后立即恢复放行，「一次启动一条链」由 {@code LaunchOnceGate} 负责。</p>
     */
    @Test
    public void shouldFetchAgainOnSecondLaunchSameDay() {
        long firstSuccess = T0;
        long secondLaunch = (T0 + 5 * MINUTE); // 同一天、5 分钟后重新启动
        FetchClock clock = new FetchClock(firstSuccess, firstSuccess);
        assertTrue(MingCiPermissionStore.shouldFetchNow(secondLaunch, clock));
    }

    /** 回归：成功后跨天启动同样放行（不再有「同一天」这种自然日概念）。 */
    @Test
    public void shouldFetchOnLaterDayAfterSuccess() {
        long firstSuccess = T0;
        long nextDay = (T0 + 26 * HOUR);
        FetchClock clock = new FetchClock(firstSuccess, firstSuccess);
        assertTrue(MingCiPermissionStore.shouldFetchNow(nextDay, clock));
    }

    /** 时钟回拨：距上次为负 → 放行，避免永久饿死拉取。 */
    @Test
    public void shouldFetchWhenClockMovedBackwards() {
        long attempt = T0;
        long now = attempt - 5 * MINUTE;
        FetchClock clock = new FetchClock(attempt, 0L);
        assertTrue(MingCiPermissionStore.shouldFetchNow(now, clock));
    }
}
