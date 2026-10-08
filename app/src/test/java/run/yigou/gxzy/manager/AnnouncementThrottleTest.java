package run.yigou.gxzy.manager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Calendar;
import java.util.Random;
import java.util.TimeZone;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import run.yigou.gxzy.data.remote.model.Announcement;
import run.yigou.gxzy.manager.announcement.AnnouncementStore;
import run.yigou.gxzy.manager.concurrency.FetchClock;
import run.yigou.gxzy.manager.concurrency.PopDelay;

/**
 * 公告的「拉取节流 / 缓存裁剪 / 弹窗延迟」判据测试（纯 JVM）。
 *
 * <p>被测的 {@code shouldFetchNow}、{@code pruneExpired}、{@code isExpired}、
 * {@link PopDelay#nextMs} 全部不碰 MMKV / Android，可直接跑。</p>
 *
 * <p>这些判据错了的后果：节流失效会让 App 每次启动都打服务器（用户要求「每天一次」）；
 * 过期不裁会让用户看到早已失效的公告；延迟区间写错会让弹窗永远不来。</p>
 */
public class AnnouncementThrottleTest {

    private static final Gson GSON = new Gson();
    private static final long HOUR = 3600_000L;
    private static final TimeZone ORIGINAL_TZ = TimeZone.getDefault();

    /** 固定时区：自然日判定依赖本地时区，不固定的话断言会随机器跑偏。 */
    @BeforeClass
    public static void fixTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
    }

    @After
    public void restoreTimeZone() {
        TimeZone.setDefault(ORIGINAL_TZ);
    }

    /** 造某个本地时刻的毫秒时间戳。 */
    private static long at(int year, int month, int day, int hour, int minute, int second) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day, hour, minute, second);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    /** 造一条公告；validTo 为 null 表示无结束时间。 */
    private static Announcement item(int id, Long validTo) {
        String to = validTo == null ? "null" : String.valueOf(validTo);
        return GSON.fromJson(
                "{\"id\":" + id + ",\"title\":\"公告" + id + "\",\"body\":\"正文\","
                        + "\"priority\":0,\"version\":1,\"validFrom\":null,\"validTo\":" + to + "}",
                Announcement.class);
    }

    /** 固定返回值的 Random，让 PopDelay.nextMs 可确定地断言。 */
    private static Random fixedRandom(final int value) {
        return new Random() {
            @Override
            public int nextInt(int bound) {
                return value % bound;
            }
        };
    }

    // ---------- 每天一次 ----------

    @Test
    public void fetchesWhenNeverSucceeded() {
        // 从未成功过（0 表示无记录）→ 应当拉一次。
        assertTrue(AnnouncementStore.shouldFetchNow(1000L, new FetchClock(0L, 0L)));
    }

    @Test
    public void skipsLaterOnTheSameLocalDay() {
        // 今天 10:00 成功过，晚上 20:00 不该再拉。
        long success = at(2026, 10, 5, 10, 0, 0);
        assertFalse(AnnouncementStore.shouldFetchNow(
                at(2026, 10, 5, 20, 0, 0), new FetchClock(0L, success)));
    }

    @Test
    public void fetchesAgainJustAfterMidnight() {
        // 需求原文：超过 23:59:59 就算第二天。23:59:59 还属当天（跳过），
        // 00:00:00 起算第二天（允许再拉）——边界必须精确到这一秒。
        long success = at(2026, 10, 5, 23, 0, 0);
        assertFalse("23:59:59 仍算当天，不该再拉",
                AnnouncementStore.shouldFetchNow(
                        at(2026, 10, 5, 23, 59, 59), new FetchClock(0L, success)));
        assertTrue("00:00:00 起算第二天，应当再拉",
                AnnouncementStore.shouldFetchNow(
                        at(2026, 10, 6, 0, 0, 0), new FetchClock(0L, success)));
    }

    @Test
    public void skipsOnlyOneSecondBeforeMidnight() {
        long success = at(2026, 10, 5, 23, 0, 0);
        assertFalse(AnnouncementStore.shouldFetchNow(
                at(2026, 10, 5, 23, 59, 58), new FetchClock(0L, success)));
    }

    @Test
    public void sameDayCheckUsesDeviceLocalTimeZone() {
        long morning = at(2026, 10, 5, 1, 0, 0);
        long evening = at(2026, 10, 5, 23, 0, 0);
        long nextDay = at(2026, 10, 6, 1, 0, 0);
        assertTrue(AnnouncementStore.isSameLocalDay(morning, evening));
        assertFalse(AnnouncementStore.isSameLocalDay(evening, nextDay));
    }

    // ---------- 失败退避 ----------

    @Test
    public void backsOffAfterFailure() {
        long attempt = 1000L * HOUR;
        // 失败后 1 分钟内不再打（避免离线时每次启动都请求）。
        assertFalse(AnnouncementStore.shouldFetchNow(attempt + 60_000L, new FetchClock(attempt, 0L)));
    }

    @Test
    public void retriesAfterBackoffWindow() {
        long attempt = 1000L * HOUR;
        // 退避期过后仍要再试，否则一次失败就再也拿不到公告。
        assertTrue(AnnouncementStore.shouldFetchNow(attempt + AnnouncementStore.RETRY_INTERVAL_MS, new FetchClock(attempt, 0L)));
    }

    @Test
    public void backoffDoesNotCountAsSuccess() {
        // 关键：失败只推进 lastAttempt，**不能**把 lastSuccess 也推进。
        // 若实现错误地一并推进，两者会相等，now - success 就只有退避期那么长，
        // 于是被「每天一次」规则否决 → true 变 false，本条正好抓到这个 bug。
        long success = 1000L * HOUR;
        long attempt = success + 25 * HOUR;          // 距上次成功已超 24 小时
        long now = attempt + AnnouncementStore.RETRY_INTERVAL_MS;
        assertTrue(AnnouncementStore.shouldFetchNow(now, new FetchClock(attempt, success)));
    }

    @Test
    public void newDayBeatsYesterdayBackoff() {
        // 需求原文「超过 23:59:59 就算第二天」：23:55 成功、00:20 才打开 App 时，
        // 距上次尝试只有 25 分钟。若先判退避就会被拦下，用户得多等 5 分钟，
        // 而当天确实已经换日了。跨日优先于退避。
        long success = at(2026, 10, 5, 23, 55, 0);
        long now = at(2026, 10, 6, 0, 20, 0);
        assertTrue("跨过零点即应再拉，不受昨日退避期阻挡",
                AnnouncementStore.shouldFetchNow(now, new FetchClock(success, success)));
    }

    @Test
    public void sameDayBackoffStillAppliesWithinTheDay() {
        // 反向确认：跨日优先不能做过头——同一天内 25 分钟时仍要退避。
        long attempt = at(2026, 10, 5, 10, 0, 0);
        long now = at(2026, 10, 5, 10, 25, 0);
        assertFalse(AnnouncementStore.shouldFetchNow(now, new FetchClock(attempt, 0L)));
    }

    @Test
    public void dailyThrottleBeatsBackoffWhenBothApply() {
        // 今天已经成功过：即便退避期也过了，也不该再拉。
        long success = 1000L * HOUR;
        long now = success + 2 * HOUR;
        assertFalse(AnnouncementStore.shouldFetchNow(now, new FetchClock(0L, success)));
    }

    // ---------- 时钟回拨 ----------

    @Test
    public void clockSkewedBackwardsDoesNotStarveFetchingForever() {
        // 用户把系统时间往前调一天：now < lastSuccessAt，差值为负，
        // 会被「< 24h」判成「刚拉过」而永远不再拉 → 公告再也不更新。必须放行。
        long lastSuccess = 100_000_000L;
        long now = lastSuccess - 24 * HOUR;
        assertTrue(AnnouncementStore.shouldFetchNow(now, new FetchClock(0L, lastSuccess)));
    }

    @Test
    public void clockSkewedBackwardsAlsoClearsTheRetryBackoff() {
        // 回拨同样会让退避期判成「刚失败过」而永久退避。两道都要放行。
        long lastAttempt = 100_000_000L;
        long now = lastAttempt - HOUR;
        assertTrue(AnnouncementStore.shouldFetchNow(now, new FetchClock(lastAttempt, 0L)));
    }

    @Test
    public void normalClockStillThrottles() {
        // 反向确认：时钟正常时「今天拉过」依然被节流（别把上面的放行做过头）。
        long lastSuccess = 100_000_000L;
        assertFalse(AnnouncementStore.shouldFetchNow(lastSuccess + HOUR, new FetchClock(0L, lastSuccess)));
    }

    @Test
    public void crossesMidnightButStillBacksOffAfterFailure() {
        // 修复回归（第四轮评审「跨日只放行一次」）：此前「跨日」是无条件首条放行，
        // 导致「昨天成功 + 今天持续失败」时每次调用都直接返回 true、完全绕过 30 分钟退避
        // （一天可拉数十次）。正确语义是跨日只放行一次；今天首次尝试失败后，退避期内的
        // 后续调用必须被拦下，而不是因为「不是同一天」就又放行。
        long yesterdaySuccess = at(2026, 10, 4, 23, 0, 0);
        // 今天 09:00 首次（跨日）应放行。
        assertTrue(AnnouncementStore.shouldFetchNow(
                at(2026, 10, 5, 9, 0, 0), new FetchClock(0L, yesterdaySuccess)));
        // 模拟这次拉取失败：lastAttemptAt 推进到今天 09:05（lastSuccess 仍是昨天）。
        FetchClock afterFail = new FetchClock(at(2026, 10, 5, 9, 5, 0), yesterdaySuccess);
        // 09:30（退避期内，距上次尝试仅 25 分钟）应被拦下，不得无视跨日而狂拉。
        assertFalse("退避期内不得因跨日而绕过退避连续拉取",
                AnnouncementStore.shouldFetchNow(at(2026, 10, 5, 9, 30, 0), afterFail));
    }

    // ---------- 过期裁剪 ----------

    @Test
    public void keepsAnnouncementWithoutEndTime() {
        // validTo 为空 = 无结束时间，永不过期。
        assertFalse(AnnouncementStore.isExpired(item(1, null), 999_999L));
    }

    @Test
    public void treatsEndTimeEqualToNowAsStillValid() {
        // 与后端 SQL 的 valid_to >= now 口径一致：等于 now 仍算有效。
        long now = 1000L;
        assertFalse(AnnouncementStore.isExpired(item(1, now), now));
    }

    @Test
    public void detectsExpiredAnnouncement() {
        assertTrue(AnnouncementStore.isExpired(item(1, 999L), 1000L));
    }

    @Test
    public void prunesExpiredAndKeepsAlive() {
        long now = 1000L * HOUR;
        List<Announcement> input = Arrays.asList(
                item(1, now - 1),      // 过期
                item(2, now + 1),      // 仍有效
                item(3, null));        // 无结束时间
        List<Announcement> alive = AnnouncementStore.pruneExpired(input, now);
        assertEquals(2, alive.size());
        assertEquals(2, alive.get(0).getId());
        assertEquals(3, alive.get(1).getId());
    }

    @Test
    public void pruneHandlesNullAndNullElements() {
        assertEquals(0, AnnouncementStore.pruneExpired(null, 1000L).size());
        assertEquals(0, AnnouncementStore.pruneExpired(
                Collections.<Announcement>emptyList(), 1000L).size());
        List<Announcement> withNull = new ArrayList<>();
        withNull.add(null);
        assertEquals(0, AnnouncementStore.pruneExpired(withNull, 1000L).size());
    }

    // ---------- 弹窗延迟 ----------

    @Test
    public void delayStaysWithinFifteenToTwentyFiveSeconds() {
        // 需求：启动完成后等 15~25 秒。随机取值必须落在这个区间内（含端点）。
        for (int i = 0; i < 200; i++) {
            int delay = PopDelay.nextMs(new Random(i));
            assertTrue("delay=" + delay, delay >= PopDelay.MIN_MS);
            assertTrue("delay=" + delay, delay <= PopDelay.MAX_MS);
        }
    }

    @Test
    public void delayCoversBothEnds() {
        // 区间两端都要取得到，否则 15 秒下界形同虚设。
        assertEquals(15_000, PopDelay.nextMs(fixedRandom(0)));
        assertEquals(25_000,
                PopDelay.nextMs(fixedRandom(10_000)));
    }

    @Test
    public void delayConstantsMatchTheRequirement() {
        assertEquals(15_000, PopDelay.MIN_MS);
        assertEquals(25_000, PopDelay.MAX_MS);
    }

    @Test
    public void retryIntervalIsThirtyMinutes() {
        // 失败退避是 30 分钟，与「每天一次」的自然日判定互相独立。
        assertEquals(30L * 60L * 1000L, AnnouncementStore.RETRY_INTERVAL_MS);
    }
}
