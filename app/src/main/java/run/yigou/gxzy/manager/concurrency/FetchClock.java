package run.yigou.gxzy.manager.concurrency;

/**
 * 拉取节流用的两个时间戳（纯值对象，可 JVM 单测）。
 *
 * <p>它们总是一起出现、一起读写，且顺序容易写反 —— 收成一个类型后，
 * 「读出来 → 改一个 → 写回去」不会错位，也不用在注释里解释两个 long 谁是谁。</p>
 */
public final class FetchClock {

    /** 最近一次**尝试**拉取的时刻（成功失败都记）；0 表示从未尝试。 */
    public final long lastAttemptAt;

    /** 最近一次**成功**拉到内容的时刻；0 表示从未成功。 */
    public final long lastSuccessAt;

    public FetchClock(long lastAttemptAt, long lastSuccessAt) {
        this.lastAttemptAt = lastAttemptAt;
        this.lastSuccessAt = lastSuccessAt;
    }

    /** 成功一次：两个时刻一起推进到 `now`。 */
    public static FetchClock afterSuccess(long now) {
        return new FetchClock(now, now);
    }

    /**
     * 失败一次：只推进「上次尝试」。
     *
     * <p>绝不能顺手推进「上次成功」—— 那样退避期一过，若当天已成功过就再也不会拉，
     * 一次失败会永久停掉当天的更新。</p>
     */
    public FetchClock afterFailure(long now) {
        return new FetchClock(now, lastSuccessAt);
    }
}
