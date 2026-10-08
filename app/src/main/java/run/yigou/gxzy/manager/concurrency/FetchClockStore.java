package run.yigou.gxzy.manager.concurrency;

import com.tencent.mmkv.MMKV;

import run.yigou.gxzy.log.EasyLog;

/**
 * {@link FetchClock} 的 MMKV 读写 —— 「两个时间戳怎么持久化」这一件事的唯一实现。
 *
 * <p>公告与搜索权限两套拉动都用同一组键名、同一套降级策略（读失败当「从未拉过」、
 * 写失败只记日志），此前是两份逐字相同的私有方法。抽到这里后，<b>记录至 critical 日志的
 * TAG 与命名空间由调用方传入</b>，语义一字未改，只是不再有两份会各自漂移的副本。</p>
 *
 * <p>键名常量没有搬进来：两个 store 的 MMKV 命名空间不同（各自的 {@code MMKV_ID}），
 * 但键名字符串是一致的两组词面量；保留在各自 store 里能让「这个文件存了什么」就地可见，
 * 也不必为一个纯组合操作再引入一层映射配置。</p>
 */
public final class FetchClockStore {

    /** 最近一次**成功**拉到内容的时刻（unix 毫秒），0=从未成功。 */
    static final String KEY_LAST_SUCCESS = "last_success_at";
    /** 最近一次**尝试**拉取的时刻（成功失败都记），用于失败退避。 */
    static final String KEY_LAST_ATTEMPT = "last_attempt_at";

    /** 退避间隔：公告与搜索权限同为一处改动即可调整。 */
    public static final long RETRY_INTERVAL_MS = 30L * 60L * 1000L;

    private FetchClockStore() {
    }

    /**
     * 读时间戳。任何异常都降级为「从未拉过」 —— 最坏是多发一次请求，
     * 绝不能让读盘异常连累 App 启动或卡死拉取节流。
     */
    public static FetchClock read(String mmkvId, String tag) {
        try {
            MMKV kv = MMKV.mmkvWithID(mmkvId);
            return new FetchClock(
                    kv.decodeLong(KEY_LAST_ATTEMPT, 0L),
                    kv.decodeLong(KEY_LAST_SUCCESS, 0L));
        } catch (Throwable error) {
            EasyLog.print(tag, "读取拉取时间戳失败，降级为从未拉过");
            EasyLog.print(error);
            return new FetchClock(0L, 0L);
        }
    }

    /** 写时间戳。写失败不抛：后果只是「下次多拉一次」，不该打断拉取成功的语义。 */
    public static void write(String mmkvId, String tag, FetchClock clock) {
        try {
            MMKV kv = MMKV.mmkvWithID(mmkvId);
            kv.encode(KEY_LAST_ATTEMPT, clock.lastAttemptAt);
            kv.encode(KEY_LAST_SUCCESS, clock.lastSuccessAt);
        } catch (Throwable error) {
            EasyLog.print(tag, "写入拉取时间戳失败（会导致下次多拉一次）");
            EasyLog.print(error);
        }
    }
}
