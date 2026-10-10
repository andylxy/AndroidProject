package run.yigou.gxzy.manager.mingci;

import com.google.gson.Gson;
import com.tencent.mmkv.MMKV;

import run.yigou.gxzy.data.remote.model.MingCiPermissionState;
import run.yigou.gxzy.log.EasyLog;
import run.yigou.gxzy.manager.concurrency.FetchClock;
import run.yigou.gxzy.manager.concurrency.FetchClockStore;

/**
 * 名词解释权限的落盘存储：<b>权限状态缓存</b> 与 <b>拉取节流时钟</b>。
 *
 * <p>严格镜像 {@link run.yigou.gxzy.manager.search.SearchPermissionStore}：G3 单开关只有
 * 一个布尔，结构完全同构。进程级单例 + 方法内获取 MMKV（不持 Context），Gson 默认字段名映射。</p>
 *
 * <p><b>注意</b>：网关 {@code MingCiPermissionManager.isAllowed} 只读进程内的
 * {@code sState}（拉取成功后持有），不读这里落盘的缓存——这是 INV-2「先判 fetchOk、
 * 为假直接返回 false、不读缓存」的硬要求，且能避免每次按键都打一次磁盘。本类的缓存是
 * <b>持久化 / 诊断</b>层：保留最近一次成功的判定，便于排查。</p>
 */
public final class MingCiPermissionStore {

    private static final String TAG = "MingCiPermissionStore";

    /** 拉取节流：失败退避 30 分钟（INV-5）。取值见 {@link FetchClockStore#RETRY_INTERVAL_MS}。 */
    public static final long RETRY_INTERVAL_MS = FetchClockStore.RETRY_INTERVAL_MS;

    private static final String MMKV_ID = "mingci_permission_state";
    /** 最近一次拉到的权限状态（只保存最新一份，覆盖写入）。 */
    private static final String KEY_CACHE = "cache";

    private static final Gson GSON = new Gson();

    private MingCiPermissionStore() {
    }

    /**
     * 取本类的 MMKV 实例。方法内获取（与 {@code SearchPermissionStore} 同一写法）而不是静态
     * 字段或 Holder：静态初始化会先于任何调用执行，而 MMKV 需要 native 库，JVM 单测里一触发
     * 就 {@code UnsatisfiedLinkError}。方法内获取天然把加载推迟到真正读写时。
     */
    private static MMKV kv() {
        return MMKV.mmkvWithID(MMKV_ID);
    }

    // ==================== 纯逻辑（不碰 MMKV，可 JVM 单测） ====================

    /**
     * 现在是否该去远程拉一次（纯逻辑，可 JVM 单测）。
     *
     * <p><b>不设每日节流</b>（与 search-permission 同构：不设每日节流，每次启动只拉一次）：
     * 只有<b>上一次尝试失败</b>才退避 {@link #RETRY_INTERVAL_MS}，一旦成功过就立刻恢复放行。
     * 「本次启动只拉一次」由 {@code MingCiPermissionManager} 的进程级闸门
     * {@code LaunchOnceGate} 负责，此处不做第二道约束。「次日 resume 重验」由<b>下次冷启动</b>
     * （新进程 → 闸门重置 → 前台回调重发）自然达成，无需进程内每日时钟。</p>
     *
     * <p>时钟回拨（用户把系统时间往前调）会让「距上次 &lt; 间隔」恒为真 → 永久饿死拉取；
     * 退避判断里用 {@code now &lt; lastAttemptAt} 放行回拨。</p>
     */
    public static boolean shouldFetchNow(long now, FetchClock clock) {
        // 只有「最近一次尝试失败」才退避：lastAttempt 晚于 lastSuccess 即表示上次失败。
        // 成功过（lastSuccess >= lastAttempt）说明上次拿到了有效答案，立即放行下次启动的拉取。
        boolean lastAttemptFailed = clock.lastAttemptAt > 0
                && clock.lastAttemptAt > clock.lastSuccessAt;
        if (lastAttemptFailed && now - clock.lastAttemptAt < RETRY_INTERVAL_MS) {
            return now < clock.lastAttemptAt; // 时钟回拨放行，避免永久退避
        }
        return true;
    }

    // ==================== 权限状态缓存（只存最新） ====================

    /**
     * 覆盖写入本地缓存：<b>只保存最近一次拉到的内容</b>。
     *
     * <p><b>返回是否真的落盘成功</b>：调用方据此走「成功」或「失败 + 退避重试」两条路
     * （字段缺失/写盘失败一律按失败处理，不允许「写盘失败但标记成功」——那会让退避时钟停摆）。</p>
     */
    public static boolean writeCache(MingCiPermissionState state, long now) {
        try {
            boolean ok = kv().encode(KEY_CACHE, GSON.toJson(state));
            EasyLog.print(TAG, "已写入名词解释权限缓存 allowed=" + state.allowed + " ok=" + ok);
            return ok;
        } catch (Throwable error) {
            // 写盘异常不能让它冒到调用方（会打断「拉取成功」这条主路径的语义），
            // 但必须如实返回 false，让调用方按失败处理。
            EasyLog.print(TAG, "写入名词解释权限缓存失败（按拉取失败处理）");
            EasyLog.print(error);
            return false;
        }
    }

    /** 读出本地缓存的权限状态（最坏降级为 false = 默认关闭）。 */
    public static MingCiPermissionState readCache() {
        try {
            String raw = kv().decodeString(KEY_CACHE);
            if (raw == null || raw.isEmpty()) {
                return new MingCiPermissionState(false);
            }
            MingCiPermissionState state = GSON.fromJson(raw, MingCiPermissionState.class);
            return state != null ? state : new MingCiPermissionState(false);
        } catch (Throwable error) {
            EasyLog.print(TAG, "读取名词解释权限缓存失败，降级为关闭");
            EasyLog.print(error);
            return new MingCiPermissionState(false);
        }
    }

    // ==================== 拉取节流 ====================

    /** 现在该不该拉远程（读盘 + 判定；{@link #shouldFetchNow} 是其中的纯逻辑）。 */
    public static boolean shouldFetchNow() {
        return shouldFetchNow(System.currentTimeMillis(), readClock());
    }

    /** 拉取成功：同时推进「上次尝试」与「上次成功」。 */
    public static void markFetchSuccess(long now) {
        writeClock(FetchClock.afterSuccess(now));
    }

    /**
     * 拉取失败：只推进「上次尝试」，让 {@link #RETRY_INTERVAL_MS} 的退避生效，
     * 而「上次成功」保持不变——否则一次失败会把「成功过」这个事实抹掉，
     * 下次启动的拉取将被误判成「从未成功」，退避与放行都失去依据。
     */
    public static void markFetchFailure(long now) {
        writeClock(readClock().afterFailure(now));
    }

    private static FetchClock readClock() {
        return FetchClockStore.read(MMKV_ID, TAG);
    }

    private static void writeClock(FetchClock clock) {
        FetchClockStore.write(MMKV_ID, TAG, clock);
    }
}
